package com.mtaa.project

import org.jetbrains.exposed.sql.*
import org.joda.time.DateTime

enum class Status {
    UNAUTHORIZED, NOT_FOUND, OK
}

/**
 * User queries
 */
const val PAGE_LIMIT = 5 //number of products loading in one batch

fun createUser(_name:String, _password:String, _email:String, _trust_score:Int) {
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
    Products.deleteWhere { Products.brand eq brand.id }

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
    Products.deleteWhere { Products.category eq category.id }

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

fun getProductInfo(_id: Int): Product? {
    return Product.find { Products.id eq _id }.firstOrNull()
}

//TODO: produkt znacky, ktora po vymazani v kategorii nebude, tak vymazem spojenie danej znacky s tou kategoriou
fun deleteProduct(_id: Int): Boolean {
    val product = Product.find { Products.id eq _id }.firstOrNull() ?: return false

    val reviews: Query = Reviews.select { Reviews.product eq product.id }
    val reviewsList = Review.wrapRows(reviews).toList()

    for (review in reviewsList) {
        Photos.deleteWhere { Photos.review eq review.id }
        ReviewAttributes.deleteWhere { ReviewAttributes.review eq review.id }
        ReviewVotes.deleteWhere { ReviewVotes.review eq review.id }
        review.delete()
    }

    product.delete()
    return true
}

fun addProduct(_name: String, _price: Int, category_ID: Int, brand_ID: Int): Boolean {
    val category = Category.findById(category_ID) ?: return false
    val brand =  Brand.findById(brand_ID) ?: return false
    Product.new {
        name = _name
        price = _price
        score = 0
        this.category = category
        this.brand = brand
    }
    return true
}

fun searchProducts(_name: String): List<Product> {
    val products: Query = Products.select { Products.name like "%$_name%"}
    return Product.wrapRows(products).toList()
}

fun getReviewInfo(_id: Int): Review? {
    return Review.find { Reviews.id eq _id }.firstOrNull()
}

fun getReviewAttributes(review_id: Int): List<ReviewAttribute> {
    val attributes: Query = ReviewAttributes.select { ReviewAttributes.review eq review_id }
    return ReviewAttribute.wrapRows(attributes).toList()
}

fun getReviewVotes(review_id: Int): List<ReviewVote> {
    val votes: Query = ReviewVotes.select { ReviewVotes.review eq review_id }
    return ReviewVote.wrapRows(votes).toList()
}

fun getPhotos(review_id: Int): List<Photo> {
    val photos: Query = Photos.select { Photos.review eq review_id }
    return Photo.wrapRows(photos).toList()
}

fun createReview(reviewPostInfo: ReviewPostInfo, auth: Int): Status {
    val product = Product.findById(reviewPostInfo.product_id) ?: return Status.NOT_FOUND
    val user = User.findById(auth) ?: return Status.UNAUTHORIZED

    val newReview = Review.new {
        text = reviewPostInfo.text
        this.product = product
        created_at = DateTime.now()
        this.user = user
        score = reviewPostInfo.score
    }

    for (attribute in reviewPostInfo.attributes) {
        ReviewAttribute.new {
            text = attribute.text
            is_positive = attribute.is_positive
            review = newReview
        }
    }

    for (photo in reviewPostInfo.photos) {
        Photo.new {
            src = photo.source
            review = newReview
        }
    }

    return Status.OK
}

fun updateReview(reviewPutInfo: ReviewPutInfo, review_id: Int, auth: Int): Status {
    val review = Review.findById(review_id) ?: return Status.NOT_FOUND
    val user = User.findById(auth) ?: return Status.UNAUTHORIZED

    if (review.user != user) {
        return Status.UNAUTHORIZED
    }

    review.text = reviewPutInfo.text
    review.score = reviewPutInfo.score

    ReviewAttributes.deleteWhere { ReviewAttributes.review eq review_id }

    for (attribute in reviewPutInfo.attributes) {
        ReviewAttribute.new {
            text = attribute.text
            is_positive = attribute.is_positive
            this.review = review
        }
    }

    return Status.OK
}

fun voteOnReview(auth: Int, review_id: Int, is_positive: Boolean): Status {
    val review = Review.findById(review_id) ?: return Status.NOT_FOUND
    val user = User.findById(auth) ?: return Status.UNAUTHORIZED

    val sameVote = ReviewVote.find {
                (ReviewVotes.user eq user.id) and
                (ReviewVotes.review eq review.id) and
                (ReviewVotes.is_positive eq is_positive)
            }.firstOrNull()

    if (sameVote != null) {
        sameVote.delete()
        return Status.OK
    }

    val vote = ReviewVote.find { (ReviewVotes.user eq user.id) and (ReviewVotes.review eq review.id) }.firstOrNull()
    if (vote == null) {
        ReviewVote.new {
            this.user = user
            this.review = review
            this.is_positive = is_positive
        }
    }
    else {
        vote.is_positive = is_positive
    }

    return Status.OK
}

fun deleteReview(auth: Int, review_id: Int): Status {
    val review = Review.findById(review_id) ?: return Status.NOT_FOUND
    val user = User.findById(auth) ?: return Status.UNAUTHORIZED

    ReviewAttributes.deleteWhere { ReviewAttributes.review eq review.id }
    ReviewVotes.deleteWhere { ReviewVotes.review eq review.id }
    Photos.deleteWhere { Photos.review eq review.id }
    Reviews.deleteWhere { (Reviews.id eq review.id) and (Reviews.user eq user.id) }

    return Status.OK
}

fun getReviews(product_id: Int, paging: Int, _orderBy: String?, _orderType: String?, user_id: Int? = null) {
    val orderType:SortOrder = when (_orderType) {
        "asc" -> SortOrder.ASC
        "desc" -> SortOrder.DESC
        else -> SortOrder.DESC
    }

    when (_orderBy) {
        "created_at" -> {
            // Tu budu query pre typ Column<DateTime>
            var orderBy = Reviews.created_at
        }
        "score" -> {
            // Tu budu query pre typ Column<Int>
            var orderBy = Reviews.score
        }
        else -> {
            // Tu budu query pre default order_by
        }
    }
}