package com.mtaa.project.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.ktor.application.*
import io.ktor.request.*
import java.lang.Exception

//This should probably be hidden
const val SECRET_KEY = "sO76akvMO54qFLVsB0Fxz4I7DAa3Mr21T87JTfYb180="
const val ADMIN_KEY = "admin123"

//Parse JWT token and return ID
fun getIdFromAuth(call: ApplicationCall): Int {
    val auth: String? = call.request.header("auth")
    return if (auth != null) {
        try {
            val claim = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY.toByteArray(charset("UTF-8"))))
                .build()
                .parseClaimsJws(auth)
            val id: String = claim.body["id"] as String

            id.toInt()
        } catch (e:Exception) { //When jwt is in wrong format
            -1
        }
    }
    else {
        -1
    }
}

//Check if header contains admin key
fun isAdmin(call: ApplicationCall):Boolean {
    val auth: String? = call.request.header("admin")
    return auth?.equals(ADMIN_KEY) ?: false
}

//Create auth key and return it
fun getAuthKey(id:String):String {
    return Jwts.builder()
        .claim("id", id)
        .signWith(Keys.hmacShaKeyFor(SECRET_KEY.toByteArray(charset("UTF-8"))))
        .compact()
}
