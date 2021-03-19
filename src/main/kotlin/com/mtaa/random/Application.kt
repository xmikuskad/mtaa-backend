package com.mtaa.random

import io.ktor.routing.*
import io.ktor.application.*
import io.ktor.response.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>): Unit =
        io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {

    Database.connect("jdbc:postgresql://localhost:5433/mtaa", driver = "org.postgresql.Driver",
        user = "postgres", password = "root")

    transaction {
        val id = test.insert {
            it[name] = "Hello33"
        } get test.id;
        println(id)

        for(testik in test.selectAll()) {
            println("Result: ${testik[test.name]}")
        }
    }

    routing {
        userRouting()
    }
}

object test: IntIdTable() {
    val name = varchar("name", 50)
}


fun Route.userRouting() {
    route("/user") {
        get {
            call.respondText("Hello user");
        }
        get("{id}") {
            val id = call.parameters["id"];
            call.respondText("Hello $id");
        }
        post {

        }
        delete("{id}") {

        }
    }
}

