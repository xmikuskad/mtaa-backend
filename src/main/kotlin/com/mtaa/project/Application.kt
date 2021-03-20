package com.mtaa.project

import com.mtaa.project.routing.userRouting
import io.ktor.routing.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.request.*
import org.jetbrains.exposed.sql.Database
import java.text.DateFormat

fun main(args: Array<String>): Unit =
        io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {

    //Set up app
    install(ContentNegotiation) {
        gson {
            setDateFormat(DateFormat.LONG)
            setPrettyPrinting()
        }
    }

    //Set rest api points
    routing {
        userRouting()
    }

    //Set up database connection
    Database.connect(
        "jdbc:postgresql://localhost:5433/mtaa?currentSchema=mtaa", driver = "org.postgresql.Driver",
        user = "postgres", password = "root"
    )

}

//Auth methods TODO move to other file?

fun isAuthenticated(call:ApplicationCall):Boolean {
    val header: String? = call.request.header("auth")
    return if(header!=null) {
        //TODO check token
        print("Pog");
        true;
    }
    else {
        false;
    }
}

fun isAdmin(call:ApplicationCall):Boolean {
    val header: String? = call.request.header("auth")
    return if(header!=null) {
        //TODO check token
        print("Pog");
        true;
    }
    else {
        false;
    }
}

