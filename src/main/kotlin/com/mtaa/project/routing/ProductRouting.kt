package com.mtaa.project.routing

import com.mtaa.project.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.NullPointerException

fun Route.productRouting() {
    route("/products") {
        get("{id}") {
            val id = parseInt(call,"id")
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
                val result = transaction {
                    addProduct(data.name, data.price, data.category_ID, data.brand_ID)
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

            val id = parseInt(call,"id")
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
    }
}
