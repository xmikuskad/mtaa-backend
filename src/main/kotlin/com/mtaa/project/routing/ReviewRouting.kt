package com.mtaa.project.routing

import com.mtaa.project.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.NullPointerException

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

            var product_id = 0
            var user_id = 0
            transaction {
                product_id = review.product.id.toString().toInt()
                user_id = review.user.id.toString().toInt()
            }

            // Review found
            call.respond(ReviewInfo(review.text, attributesInfo, photosInfo,
                votesInfo, product_id, review.score,
                user_id, review.created_at.toString()))
        }
        post {
            try {
                val data = call.receive<ReviewInfo>()
                println(data) // debug
                println(data.photos) // debug
                call.respond(HttpStatusCode.OK)
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
        put("{id}") {

        }
        put("{id}/like") {

        }
        put("{id}/dislike") {

        }
        delete("{id}") {

        }
    }
}
