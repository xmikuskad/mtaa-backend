package com.mtaa.project.routing

import com.mtaa.project.Product
import com.mtaa.project.ReviewInfo
import com.mtaa.project.User
import com.mtaa.project.getReviewInfo
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.reviewRouting() {
    route("/reviews") {
        get {

        }
        get("{id}") {
            val id = parseInt(call,"id")
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
            val review = transaction {
                getReviewInfo(id)
            }
            if (review == null) { //Product not found
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            // TODO Neda sa spravit review.product.id.toString().toInt(). Pada to. WHY??
            val product = transaction {
                Product.findById(review.product.id)
            }
            val user = transaction {
                User.findById(review.user.id)
            }
            if (product == null || user == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            // Review found
            call.respond(ReviewInfo(review.text, product.id.toString().toInt(), review.score, user.id.toString().toInt(), review.created_at.toString()))
        }
        post {

        }
        put {

        }
        put("{id}/like") {

        }
        put("{id}/dislike") {

        }
        delete("{id}") {

        }
    }
}
