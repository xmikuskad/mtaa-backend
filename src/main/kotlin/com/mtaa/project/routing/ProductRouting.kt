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

fun Route.productRouting() {
    route("/products") {
        post("/search/{page}") {
            try {
                val productInfoList: MutableList<ProductInfo> = mutableListOf()
                val data = call.receive<NameInfo>()
                var page = parseInt(call, "page")
                //Data validation
                if (data.name.length < MIN_NAME_LENGTH || page == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                if (page <= 0) {
                    page = 1
                }

                //Get search results
                val productList = transaction {
                    searchProducts(data.name, page!!)
                }
                for (product in productList) {
                    productInfoList.add(
                        ProductInfo(
                            product.name,
                            product.score,
                            product.price,
                            product.id.toString().toInt()
                        )
                    )
                }

                call.respond(ProductsInfo(productInfoList))
            } catch (e: Exception) {
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

        get("{id}") {
            val id = parseInt(call, "id")
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val product = transaction {
                getProductInfo(id)
            }
            if (product == null) { //Product not found
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            // Product found
            call.respond(ProductInfo(product.name, product.score, product.price, id))
        }

        post {
            if (!isAdmin(call)) { //Check if user is admin
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }

            try {
                val data = call.receive<AddedProduct>()
                // Data validation
                if (data.brand_id <= 0 || data.category_id <= 0 || data.price <= 0 || data.name.length < MIN_NAME_LENGTH) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                val result = transaction {
                    createProduct(data.name, data.price, data.category_id, data.brand_id)
                }
                if (!result) { // No category/brand found
                    call.respond(HttpStatusCode.NotFound)
                    return@post
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

            val result = transaction { deleteProduct(id) }

            if (!result) { // Product not found and not deleted
                call.respond(HttpStatusCode.NotFound)
                return@delete
            }
            // Product deleted
            call.respond(HttpStatusCode.OK)
        }

        get("{id}/{page}") {
            val id = parseInt(call, "id")
            var page = parseInt(call, "page")
            if (id == null || page == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
            if (page!! <= 0) {
                page = 1
            }

            val reviews = transaction {
               getReviews(
                    id,
                    page!!,
                    call.request.queryParameters["order_by"],
                    call.request.queryParameters["order_type"],
                    ReviewListType.PRODUCT_REVIEWS
               )
            }
            val reviewsInfo = getReviewsInfoItems(reviews)

            call.respond(ReviewsInfo(reviewsInfo))
        }
    }
}
