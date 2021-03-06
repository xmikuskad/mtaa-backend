package com.mtaa.project.routing

import com.mtaa.project.*
import com.mtaa.project.security.getIdFromAuth
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.*
import java.util.*

import java.io.FileOutputStream
import java.io.File

const val DIRECTORY_NAME = "uploads"

fun Route.photoRouting() {
    route("/reviews") {
        get("{id}/photo/{photo_id}") {
            val id = parseInt(call, "id")
            val photo_id = parseInt(call, "photo_id")
            if (id == null || photo_id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            //Get path of photo from DB
            val photo = transaction {
                getPhotoPath(id, photo_id)
            }

            if (photo == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            //Send image
            call.respondFile(File(photo.src))
        }

        delete("{id}/photo/{photo_id}") {
            val id = parseInt(call, "id")
            val photo_id = parseInt(call, "photo_id")
            val user_id = getIdFromAuth(call)

            if (user_id == -1) {
                call.respond(HttpStatusCode.Unauthorized)
                return@delete
            }
            if (id == null || photo_id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@delete
            }

            //Get path of photo from DB
            val result = transaction {
                if(checkPhotoOwnership(user_id,id,photo_id))    //Validate ownership
                    deletePhoto(photo_id)
                else
                    false
            }

            if (!result) {
                call.respond(HttpStatusCode.NotFound)
                return@delete
            }

            //Send image
            call.respond(HttpStatusCode.OK)
        }


        post("{id}/photo") {
            val id = parseInt(call, "id")
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            val user_id = getIdFromAuth(call)
            if (user_id == -1) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }

            val isOwner = transaction {
                checkReviewOwnership(user_id,id)
            }
            if(!isOwner){
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }

            //Create folder if it doesnt exist
            createFileDirectory()

            //Pseudo random name gen
            val name = UUID.randomUUID().toString()
            val file = File("$DIRECTORY_NAME/$name")

            //Working for image/png and multiparts also
            withContext(Dispatchers.IO) {
                call.receive<InputStream>().use {
                    try {
                        FileOutputStream(file).use { output -> it.transferTo(output) }
                    } catch (ioException: IOException) {
                        ioException.printStackTrace()
                    }
                }
            }

            //If the request was wrong
            if (file.length() <= 0) {
                file.delete()
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            //Save path to DB
            val status = transaction {
                createPhoto("$DIRECTORY_NAME/$name", id)
            }

            //If review wasn't found
            if (!status) {
                call.respond(HttpStatusCode.NotFound)
            }
            call.respond(HttpStatusCode.OK)
        }


    }
}

fun createFileDirectory() {
    val directory = File(DIRECTORY_NAME)

    if (!directory.exists()) {
        directory.mkdir()
    }
}