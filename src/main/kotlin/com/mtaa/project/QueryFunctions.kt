package com.mtaa.project

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select

/**
 * User queries
 */

fun createUser(_name:String, _password:String, _email:String, _trust_score:Int,) {
    User.new {
        name = _name
        email = _email
        trust_score = _trust_score
        password = _password
    }
}

fun updateUser(user:User, _name:String, _password:String, _email:String) {
    user.password = _password
    user.email = _email
    user.name = _name
}

fun getUser(_email:String, _password: String): User? {
    return User.find {
        Users.email eq _email and
                (Users.password eq _password)
    }.firstOrNull()
}

fun getUserById(_id:Int): User? {
    return User.find {Users.id eq _id}.firstOrNull()
}

fun createBrand(_name:String) {
    Brand.new {
        name = _name
    }
}

fun deleteBrand(_id:Int):Boolean {
    val brand = Brand.find { Brands.id eq _id }.firstOrNull() ?: return false

    /*val query = CategoriesBrands.innerJoin(Brands)
        .slice(CategoriesBrands.columns)
        .select{CategoriesBrands.brand eq brand.id}

    query.forEach { println(it) }*/

    CategoriesBrands.deleteWhere { CategoriesBrands.brand eq brand.id }

    brand.delete()
    return true
}