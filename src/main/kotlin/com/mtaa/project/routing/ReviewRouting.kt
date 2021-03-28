package com.mtaa.project.routing

import com.mtaa.project.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.NullPointerException
import java.lang.NumberFormatException

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
            val photos = transaction {
                getPhotos(id)
            }
            val attributes = transaction {
                getReviewAttributes(id)
            }
            val votes = transaction {
                getReviewVotes(id)
            }
            val photosInfo: MutableList<PhotoInfo> = mutableListOf()
            for (photo in photos) {
                photosInfo.add(PhotoInfo(photo.src, id))
            }

            val attributesInfo: MutableList<ReviewAttributeInfo> = mutableListOf()
            for (attribute in attributes) {
                attributesInfo.add(ReviewAttributeInfo(attribute.text, attribute.is_positive, id))
            }

            val votesInfo: MutableList<ReviewVoteInfo> = mutableListOf()
            for (vote in votes) {
                val user = transaction {
                    User.findById(vote.user.id)
                }
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                votesInfo.add(ReviewVoteInfo(user.id.toString().toInt(), vote.is_positive, id))
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
            call.respond(ReviewInfo(review.text, attributesInfo, photosInfo,
                votesInfo, product.id.toString().toInt(), review.score,
                user.id.toString().toInt(), review.created_at.toString()))
        }
        post {
            try {
                val data = call.receive<ReviewInfo>()
                call.respond(HttpStatusCode.OK)
            } catch (e: Exception) {
                when (e) {
                    //Got null payload
                    is ContentTransformationException -> call.respond(HttpStatusCode.BadRequest)
                    //Some values missing in payload
                    is NullPointerException -> call.respond(HttpStatusCode.BadRequest)
                    //Fail to parse id
                    is NumberFormatException -> call.respond(HttpStatusCode.BadRequest)
                    else -> {
                        println(e.stackTraceToString())
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
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
