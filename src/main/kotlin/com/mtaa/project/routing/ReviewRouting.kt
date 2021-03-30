package com.mtaa.project.routing

import com.mtaa.project.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.NullPointerException

enum class ReviewListType {
    USER_REVIEWS, PRODUCT_REVIEWS
}

fun Route.reviewRouting() {
    route("/reviews") {
        get("{id}") {
            val id = parseInt(call, "id")
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val reviewInfo = getReviewInfoData(id)
            if (reviewInfo == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            // Review found
            call.respond(reviewInfo)
        }
        get("recent") {

            val reviews = transaction {
                getRecentReview()
            }
            val reviewsInfo = mutableListOf<ReviewInfoItem>()
            for (review in reviews) {
                val data = getReviewInfoData(review.id.toString().toInt())
                if (data != null) {
                    reviewsInfo += ReviewInfoItem(
                        data.text,
                        data.attributes,
                        data.photos,
                        data.likes,
                        data.dislikes,
                        data.product_id,
                        data.score,
                        data.user_id,
                        review.id.toString().toInt(),
                        data.created_at
                    )
                }
            }

            call.respond(ReviewsInfo(reviewsInfo))
        }

        post {
            val auth = getIdFromAuth(call)
            if (auth == -1) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }

            try {
                val data = call.receive<ReviewPostInfo>()

                //Validation checks
                if (!validateReview(data.text, data.score, data.attributes)) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                val result = transaction {
                    createReview(data, auth)
                }
                when (result) {
                    Status.UNAUTHORIZED -> {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@post
                    }
                    Status.NOT_FOUND -> {
                        call.respond(HttpStatusCode.NotFound)
                        return@post
                    }
                    Status.OK -> {
                        call.respond(HttpStatusCode.OK)
                        return@post
                    }
                }
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
            val auth = getIdFromAuth(call)
            if (auth == -1) {
                call.respond(HttpStatusCode.Unauthorized)
                return@put
            }

            val id = parseInt(call, "id")
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@put
            }

            try {
                val data = call.receive<ReviewPutInfo>()

                //Validation checks
                if (!validateReview(data.text, data.score, data.attributes)) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@put
                }

                val result = transaction {
                    updateReview(data, id, auth)
                }
                when (result) {
                    Status.UNAUTHORIZED -> {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@put
                    }
                    Status.NOT_FOUND -> {
                        call.respond(HttpStatusCode.NotFound)
                        return@put
                    }
                    Status.OK -> {
                        call.respond(HttpStatusCode.OK)
                        return@put
                    }
                }
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
        put("{id}/like") {
            val result = addVoteToReview(call, true)
            when (result) {
                Status.UNAUTHORIZED -> {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@put
                }
                Status.NOT_FOUND -> {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }
                Status.OK -> {
                    parseInt(call, "id")?.let { it1 -> getVotes(it1) }?.let { it2 -> call.respond(it2) }
                        ?: call.respond(
                            HttpStatusCode.InternalServerError
                        )
                    return@put
                }
                Status.BAD_REQUEST -> {
                    call.respond(HttpStatusCode.BadRequest)
                    return@put
                }
            }
        }
        put("{id}/dislike") {
            val result = addVoteToReview(call, false)
            when (result) {
                Status.UNAUTHORIZED -> {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@put
                }
                Status.NOT_FOUND -> {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }
                Status.OK -> {
                    parseInt(call, "id")?.let { it1 -> getVotes(it1) }?.let { it2 -> call.respond(it2) }
                        ?: call.respond(
                            HttpStatusCode.InternalServerError
                        )
                    return@put
                }
                Status.BAD_REQUEST -> {
                    call.respond(HttpStatusCode.BadRequest)
                    return@put
                }
            }
        }
        delete("{id}") {
            val id = parseInt(call, "id")
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@delete
            }

            val auth = getIdFromAuth(call)
            if (auth == -1) {
                call.respond(HttpStatusCode.Unauthorized)
                return@delete
            }

            val result = transaction {
                deleteReview(auth, id)
            }
            when (result) {
                Status.UNAUTHORIZED -> {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@delete
                }
                Status.NOT_FOUND -> {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }
                Status.OK -> {
                    call.respond(HttpStatusCode.OK)
                    return@delete
                }
            }
        }
    }
}

fun getReviewInfoData(id:Int): ReviewInfo? {
    val review = transaction {
        getReviewInfo(id)
    } ?: return null
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
        photosInfo.add(PhotoInfo(photo.id.toString().toInt()))
    }

    val attributesInfo: MutableList<ReviewAttributeInfo> = mutableListOf()
    for (attribute in attributes) {
        attributesInfo.add(ReviewAttributeInfo(attribute.text, attribute.is_positive, id))
    }

    var likes = 0
    var dislikes = 0
    for (vote in votes) {
        if (vote.is_positive) {
            likes++
        } else {
            dislikes++
        }
    }

    var product_id = 0
    var user_id = 0
    transaction {
        product_id = review.product.id.toString().toInt()
        user_id = review.user.id.toString().toInt()
    }

    // Review found
    return ReviewInfo(
        review.text, attributesInfo, photosInfo,
        likes, dislikes, product_id, review.score,
        user_id, review.created_at.toString()
    )
}

fun getVotes(id:Int) :ReviewVotesInfo {
    val votes = transaction {
        getReviewVotes(id)
    }

    var likes = 0
    var dislikes = 0
    for (vote in votes) {
        if (vote.is_positive) {
            likes++
        } else {
            dislikes++
        }
    }

    return ReviewVotesInfo(likes,dislikes)
}

fun addVoteToReview(call: ApplicationCall, is_positive: Boolean): Status {
    val id = parseInt(call, "id") ?: return Status.BAD_REQUEST

    val auth = getIdFromAuth(call)
    if (auth == -1) {
        return Status.UNAUTHORIZED
    }

    return transaction {
        voteOnReview(auth, id, is_positive)
    }
}

fun validateReview(text:String,score:Int,attributes: MutableList<ReviewAttributePostPutInfo>):Boolean {
    if (attributes.equals(null) || text.length < MIN_NAME_LENGHT || score < DEFAULT_MIN || score > MAX_SCORE) {
        return false
    }
    for (attr in attributes) {
        if (attr.text.length < MIN_NAME_LENGHT) {
            return false
        }
    }
    return true
}

fun getReviewsInfoItems(call: ApplicationCall, id: Int, page: Int, listType: ReviewListType): MutableList<ReviewInfoItem> {
    val reviewsInfo = mutableListOf<ReviewInfoItem>()

    transaction {
        val reviews = getReviews(
            id,
            page,
            call.request.queryParameters["order_by"],
            call.request.queryParameters["order_type"],
            listType
        )
        for (review in reviews) {
            val data = getReviewInfoData(review.id.toString().toInt())
            if (data != null) {
                reviewsInfo += ReviewInfoItem(
                    data.text,
                    data.attributes,
                    data.photos,
                    data.likes,
                    data.dislikes,
                    data.product_id,
                    data.score,
                    data.user_id,
                    review.id.toString().toInt(),
                    data.created_at
                )
            }
        }
    }

    return reviewsInfo
}