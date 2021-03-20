package com.mtaa.project

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

/**
 * REST data
 */
data class RegisterInfo(val name:String, val password: String, val email:String);
data class LoginInfo(val password: String, val email:String);
data class UserInfo(val name:String, val trust_score: Int)
data class AuthInfo(val key:String)


/**
 * SQL data
 */
object Users: IntIdTable(){
    val password: Column<String> = varchar("password",255)
    val email: Column<String> = varchar("email",255)
    val name: Column<String> = varchar("name",255)
    val trust_score: Column<Int> = integer("trust_score")
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)
    var password by Users.password
    var name by Users.name
    var email by Users.email
    var trust_score by Users.trust_score
}