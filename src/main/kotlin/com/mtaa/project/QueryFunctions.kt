package com.mtaa.project

import com.mtaa.project.Brands.name
import org.jetbrains.exposed.sql.*

/**
 * User queries
 */
const val PAGE_LIMIT = 5 //number of products loading in one batch

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

    CategoriesBrands.deleteWhere { CategoriesBrands.brand eq brand.id }

    brand.delete()
    return true
}

fun createCategory(_name:String) {
    Category.new {
        name = _name
    }
}

fun deleteCategory(_id:Int):Boolean {
    val category = Category.find { Categories.id eq _id }.firstOrNull() ?: return false

    CategoriesBrands.deleteWhere { CategoriesBrands.category eq category.id }

    category.delete()
    return true
}

fun getCategoryBrands(_id: Int): List<Brand> {
    val query = CategoriesBrands.innerJoin(Brands)
        .slice(Brands.columns)
        .select{CategoriesBrands.category eq _id}

    return Brand.wrapRows(query).toList()
}

fun getCategoryProducts(_id:Int, paging:Int, _orderBy:String?,_orderType:String?): List<Product> {

    var orderBy: Column<Int>? = null
    when (_orderBy) {
        "price" -> orderBy = Products.price
        "score" -> orderBy = Products.score
    }

    val orderType:SortOrder = when (_orderType) {
        "asc" -> SortOrder.ASC
        "desc" -> SortOrder.DESC
        else -> SortOrder.DESC
    }

    val products: Query = if (orderBy == null)
        Products.select { Products.category eq _id }.orderBy(Products.id, orderType)
            .limit(PAGE_LIMIT, ((paging - 1) * PAGE_LIMIT).toLong())
    else
        Products.select { Products.category eq _id }.orderBy(orderBy, orderType)
            .limit(PAGE_LIMIT, ((paging - 1) * PAGE_LIMIT).toLong())

    return Product.wrapRows(products).toList()
}
