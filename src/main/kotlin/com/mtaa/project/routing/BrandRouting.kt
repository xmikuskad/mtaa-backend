package com.mtaa.project.routing

import com.mtaa.project.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.NullPointerException

fun Route.brandRouting() {
    route("/brands") {
        post{
            if (!isAdmin(call)) { //Check if user is admin
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }

            try {
                val data = call.receive<NameInfo>()
                transaction {
                    createBrand(data.name)
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

            val id = call.parameters["id"]?.toInt()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@delete
            }

            var found = false
            transaction {
                found = deleteBrand(id)
            }

            if (found) { //Record succesfully deleted
                call.respond(HttpStatusCode.OK)
            } else { //If we didnt find any record to delete
                call.respond(HttpStatusCode.NotFound)
            }

        }
    }
}
