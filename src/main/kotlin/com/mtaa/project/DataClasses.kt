package com.mtaa.project

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.jodatime.datetime
import org.joda.time.DateTime

/**
 * Enumerations
 */
enum class Status {
    UNAUTHORIZED, NOT_FOUND, OK, BAD_REQUEST
}

/**
 * Constants
 */

const val MIN_NAME_LENGHT = 1 //Minimal character length for a name
const val PAGE_LIMIT = 5 //Number of products loading for one page
const val DEFAULT_MIN = 0 //Minimal price
const val MIN_LOGIN_LENGTH = 8 //Minimal character length for login email and password
const val MAX_SCORE = 100 //Maximum score value

/**
 * REST data
 */

data class RegisterInfo(val name: String, val password: String, val email: String)
data class LoginInfo(val password: String, val email: String)
data class UserInfo(val name: String, val trust_score: Int, val reviews: MutableList<ReviewInfoItem>)
data class AuthInfo(val key: String)
data class NameInfo(val name: String)

/**
 * Categories
 * */
data class CategoriesInfo(val categories: MutableList<CategoryInfo>)
data class CategoryInfo(val name: String, val category_id: Int)

/**
 * Brands
 * */
data class BrandInfo(val name: String, val brand_id: Int)
data class BrandsInfo(val brands: MutableList<BrandInfo>)

/**
 * Products
 * */
data class ProductInfo(val name: String, val score: Int, val price: Int, val product_id: Int)
data class AddedProduct(val name: String, val price: Int, val category_id: Int, val brand_id: Int)
data class ProductsInfo(val products: MutableList<ProductInfo>)

/**
 * Reviews GET
 * */
//Single review
data class ReviewInfo(val text: String, val attributes: MutableList<ReviewAttributeInfo>,
                      val photos: MutableList<PhotoInfo>, val likes:Int, val dislikes:Int,
                      val product_id: Int, val score: Int, val user_id: Int,
                      val created_at: String)
//When returning array of reviews
data class ReviewInfoItem(val text: String, val attributes: MutableList<ReviewAttributeInfo>,
                      val photos: MutableList<PhotoInfo>, val likes:Int, val dislikes:Int,
                      val product_id: Int, val score: Int, val user_id: Int,
                      val review_id: Int,val created_at: String)
data class ReviewsInfo(val reviews: MutableList<ReviewInfoItem>)

data class ReviewAttributeInfo(val text: String, val is_positive: Boolean, val review_id: Int)
data class PhotoInfo(val photo_id: Int)

/**
 * Reviews POST
 * */
data class ReviewPostInfo(val text: String, val attributes: MutableList<ReviewAttributePostPutInfo>,
                          val product_id: Int, val score: Int)

data class ReviewAttributePostPutInfo(val text: String, val is_positive: Boolean)

/**
 * Reviews other
 */

data class ReviewPutInfo(val text: String, val attributes: MutableList<ReviewAttributePostPutInfo>, val score: Int)
data class ReviewVotesInfo(val likes:Int, val dislikes:Int)


/**
 * SQL tables for ORM
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

object Brands: IntIdTable(){
    val name: Column<String> = varchar("name",255)
}

class Brand(id:EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Brand>(Brands)
    var name by Brands.name
}

object Categories: IntIdTable(){
    val name: Column<String> = varchar("name",255)
}

class Category(id:EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Category>(Categories)
    var name by Categories.name
}

object CategoriesBrands: IntIdTable(){
    val brand = reference("brand_id",Brands)
    val category = reference("category_id",Categories)
}

class CategoryBrand(id:EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CategoryBrand>(CategoriesBrands)
    var brand by Brand referencedOn CategoriesBrands.brand
    var category by Category referencedOn  CategoriesBrands.category
}

object Products : IntIdTable() {
    val name: Column<String> = varchar("name", 255)
    val price: Column<Int> = integer("price")
    val score: Column<Int> = integer("score")
    val brand = reference("brand_id",Brands)
    val category = reference("category_id",Categories)
}

class Product(id:EntityID<Int>) : IntEntity(id) {
    companion object: IntEntityClass<Product>(Products)
    var name by Products.name
    var price by Products.price
    var score by Products.score
    var brand by Brand referencedOn Products.brand
    var category by Category referencedOn Products.category
}

object Reviews : IntIdTable() {
    val text: Column<String> = varchar("text", 4095)
    val product = reference("product_id", Products)
    val score: Column<Int> = integer("score")
    val user = reference("user_id", Users)
    val created_at: Column<DateTime> = datetime("created_at")
}

class Review(id:EntityID<Int>) : IntEntity(id) {
    companion object: IntEntityClass<Review>(Reviews)
    var text by Reviews.text
    var product by Product referencedOn Reviews.product
    var score by Reviews.score
    var user by User referencedOn Reviews.user
    var created_at by Reviews.created_at
}

object ReviewAttributes : IntIdTable() {
    val text: Column<String> = varchar("text", 255)
    val is_positive: Column<Boolean> = bool("is_positive")
    val review = reference("review_id", Reviews)
}

class ReviewAttribute(id:EntityID<Int>) : IntEntity(id) {
    companion object: IntEntityClass<ReviewAttribute>(ReviewAttributes)
    var text by ReviewAttributes.text
    var is_positive by ReviewAttributes.is_positive
    var review by Review referencedOn ReviewAttributes.review
}

object ReviewVotes : IntIdTable() {
    val user = reference("user_id", Users)
    val is_positive: Column<Boolean> = bool("is_positive")
    val review = reference("review_id", Reviews)
}

class ReviewVote(id:EntityID<Int>) : IntEntity(id) {
    companion object: IntEntityClass<ReviewVote>(ReviewVotes)
    var user by User referencedOn ReviewVotes.user
    var is_positive by ReviewVotes.is_positive
    var review by Review referencedOn ReviewVotes.review
}

object Photos : IntIdTable() {
    val src: Column<String> = varchar("source", 255)
    val review = reference("review_id", Reviews)
}

class Photo(id:EntityID<Int>) : IntEntity(id) {
    companion object: IntEntityClass<Photo>(Photos)
    var src by Photos.src
    var review by Review referencedOn Photos.review
}