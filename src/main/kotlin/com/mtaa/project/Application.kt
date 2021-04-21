package com.mtaa.project

import com.mtaa.project.routing.*
import io.ktor.routing.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.cio.websocket.*
import io.ktor.network.tls.certificates.*
import io.ktor.websocket.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.exposed.sql.Database
import java.io.File
import java.text.DateFormat

fun main(args: Array<String>) {
    val jksFile = File("build/temporary.jks").apply {
        parentFile.mkdirs()
    }

    if (!jksFile.exists()) {
        generateCertificate(jksFile) // Generates the SSL certificate
    }

    io.ktor.server.netty.EngineMain.main(args)
}

@ExperimentalCoroutinesApi
fun Application.module() {

    //Set up http app
    install(ContentNegotiation) {
        gson {
            setDateFormat(DateFormat.LONG)
            setPrettyPrinting()
        }
    }

    //Set up websocket connection
    install(WebSockets) {}

    routing {
        //Set rest api points
        userRouting()
        brandRouting()
        categoryRouting()
        productRouting()
        reviewRouting()
        photoRouting()

        //Set websocket routing
        websocketRouting()
    }

    //Set up database connection
    Database.connect(
        "jdbc:postgresql://localhost:5433/mtaa?currentSchema=mtaa", driver = "org.postgresql.Driver",
        user = "techtalk", password = "mtaaTechTalk0120"
    )

    //createFakeData()
}


