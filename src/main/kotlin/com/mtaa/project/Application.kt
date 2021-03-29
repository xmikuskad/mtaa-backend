package com.mtaa.project

import com.mtaa.project.routing.*
import io.ktor.routing.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.network.tls.certificates.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
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
        brandRouting()
        categoryRouting()
        productRouting()
        reviewRouting()
        photoRouting()
    }

    //Set up database connection
    Database.connect(
        "jdbc:postgresql://localhost:5433/mtaa?currentSchema=mtaa", driver = "org.postgresql.Driver",
        user = "techtalk", password = "mtaaTechTalk0120"
    )
}


