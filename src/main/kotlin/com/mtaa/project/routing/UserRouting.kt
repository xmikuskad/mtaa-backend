package com.mtaa.project.routing

import com.mtaa.project.*
import com.mtaa.project.security.getAuthKey
import com.mtaa.project.security.getIdFromAuth
import com.mtaa.project.security.getSecurePassword
import com.mtaa.project.security.isAdmin
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.NullPointerException
import java.lang.NumberFormatException

fun Route.userRouting() {

    post("/login") {
        try {
            val data = call.receive<LoginInfo>()
            var id = -1

            //Hash password
            val password = getSecurePassword(data.password)
            if(password==null){
                call.respond(HttpStatusCode.InternalServerError)
                return@post
            }

            transaction {
                id = (getUser(data.email, password)?.id ?: -1).toString().toInt()
            }

            if (id > 0) { //User found, return auth key
                call.respond(AuthInfo(getAuthKey(id.toString())))
            } else { //User not found in DB
                call.respond(HttpStatusCode.NotFound)
            }

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

    route("/users") {
        get("{page}") {
            val id = getIdFromAuth(call)
            if (id == -1) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }

            var page = parseInt(call, "page")
            if (page == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
            if (page <= 0) {
                page = 1
            }

            val user = transaction { getUserById(id) }
            if (user == null) { //User not found
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val reviews = transaction {
                getReviews(
                    id,
                    page!!,
                    call.request.queryParameters["order_by"],
                    call.request.queryParameters["order_type"],
                    ReviewListType.USER_REVIEWS
                )
            }
            val reviewsInfo = getReviewsInfoItems(reviews)

            call.respond(UserInfo(user.name, user.trust_score, reviewsInfo))
        }
        put {
            try {
                val id = getIdFromAuth(call)
                if (id < 0) { //Check if user is authenticated
                    call.respond(HttpStatusCode.Unauthorized)
                    return@put
                }

                val data = call.receive<RegisterInfo>()
                var found = false

                //Hash password
                val password = getSecurePassword(data.password)
                if(password==null){
                    call.respond(HttpStatusCode.InternalServerError)
                    return@put
                }

                var duplicate = false
                transaction {
                    getUserById(id)?.let {
                        duplicate = updateUser(it, data.name, password, data.email)
                        found = true
                    }
                }
                if(duplicate){
                    call.respond(HttpStatusCode.Conflict)
                    return@put
                }
                if (found) { //Change successful
                    call.respond(HttpStatusCode.OK)
                } else { //User not found
                    call.respond(HttpStatusCode.NotFound)
                }

            } catch (e: Exception) { //If body doesnt contain all variables
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

            val user = transaction { getUserById(id) }
            if (user == null) { //User not found
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val reviews = transaction {
                getReviews(
                    id,
                    page!!,
                    call.request.queryParameters["order_by"],
                    call.request.queryParameters["order_type"],
                    ReviewListType.USER_REVIEWS
                )
            }
            val reviewsInfo = getReviewsInfoItems(reviews)

            call.respond(UserInfo(user.name, user.trust_score, reviewsInfo))
        }

        post {
            try {
                val data = call.receive<RegisterInfo>()

                //Data validation
                if (data.name.length < MIN_NAME_LENGHT || data.email.length < MIN_LOGIN_LENGTH || data.password.length < MIN_LOGIN_LENGTH) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                //Password hash
                val password = getSecurePassword(data.password)
                if(password==null){
                    call.respond(HttpStatusCode.InternalServerError)
                    return@post
                }


                val duplicate = transaction {
                    createUser(data.name, password, data.email, 0)
                }
                //If email is already registered
                if(duplicate){
                    call.respond(HttpStatusCode.Conflict)
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

            val result = transaction {
                deleteUser(id)
            }

            if (!result) { // User not found
                call.respond(HttpStatusCode.NotFound)
                return@delete
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}

fun parseInt(call:ApplicationCall ,name:String):Int? {
    try {
        return call.parameters[name]?.toInt() ?: return null
    } catch (e: Exception) {             //Fail to parse id
        println(e.stackTraceToString())
        return null
    }
}