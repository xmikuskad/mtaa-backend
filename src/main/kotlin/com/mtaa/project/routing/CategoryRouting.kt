package com.mtaa.project.routing

import com.mtaa.project.*
import com.mtaa.project.security.isAdmin
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.NullPointerException
import java.lang.NumberFormatException

fun Route.categoryRouting() {
    route("/categories") {

        get {
            val array: MutableList<CategoryInfo> = mutableListOf()
            transaction {
                val categories = Category.all()
                for (category in categories) {
                    array.add(CategoryInfo(category.name, category.id.toString().toInt()))
                }
            }

            call.respond(CategoriesInfo(array))
        }

        get("{id}/brands") {
            val id = parseInt(call, "id")
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            var brands: List<Brand> = listOf()
            transaction {
                brands = getCategoryBrands(id)
            }

            if (brands.isEmpty()) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val array: MutableList<BrandInfo> = mutableListOf()
            for (item in brands) {
                array.add(BrandInfo(item.name, item.id.toString().toInt()))
            }

            call.respond(BrandsInfo(array))
        }

        get("{categoryID}/{page}") {
            val categoryID = parseInt(call, "categoryID")
            var page = parseInt(call, "page")

            if (categoryID == null || page == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
            //To prevent negative pages
            if (page <= 0) {
                page = 1
            }

            val category = transaction { Category.findById(categoryID) }
            if (category == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            var products: List<Product> = listOf()
            try {
                transaction {
                    products = getCategoryProducts(
                        categoryID,
                        page,
                        call.request.queryParameters["order_by"],
                        call.request.queryParameters["order_type"],
                        call.request.queryParameters["min_price"],
                        call.request.queryParameters["max_price"],
                        call.request.queryParameters["min_score"],
                        call.request.queryParameters["brands"]
                    )
                }
            } catch (e: Exception) {
                when (e) {
                    //Fail to parse int
                    is NumberFormatException -> {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                    else -> {
                        println(e.stackTraceToString())
                        call.respond(HttpStatusCode.InternalServerError)
                        return@get
                    }
                }
            }

            val array: MutableList<ProductInfo> = mutableListOf()
            for (item in products) {
                array.add(ProductInfo(item.name, item.score, item.price, item.id.toString().toInt()))
            }

            call.respond(ProductsInfo(array))
        }

        post {
            if (!isAdmin(call)) { //Check if user is admin
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }

            try {
                val data = call.receive<NameInfo>()
                //Validation
                if (data.name.length < MIN_NAME_LENGTH) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                transaction {
                    createCategory(data.name)
                }
                call.respond(HttpStatusCode.OK)
            } catch (e: Exception) { //If body doesnt contain all variables
                when (e) {
                    //Got null payload
                    is ContentTransformationException -> call.respond(HttpStatusCode.BadRequest)
                    //Some values missing in payload
                    is NullPointerException -> call.respond(HttpStatusCode.BadRequest)
                    else -> {
                        println(e.stackTraceToString())
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }

        delete("{id}") {
            if (!isAdmin(call)) { //Check if user is admin
                call.respond(HttpStatusCode.Unauthorized)
                return@delete
            }

            val id = parseInt(call, "id")
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@delete
            }

            var found = false
            transaction {
                found = deleteCategory(id)
            }

            if (found) { //Record successfully deleted
                call.respond(HttpStatusCode.OK)
            } else { //If we didnt find any record to delete
                call.respond(HttpStatusCode.NotFound)
            }

        }
    }
}