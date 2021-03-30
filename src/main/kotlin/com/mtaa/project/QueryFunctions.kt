package com.mtaa.project

import com.mtaa.project.routing.ReviewListType
import org.jetbrains.exposed.sql.*
import org.joda.time.DateTime
import java.io.File

/**
 * User queries
 */

fun createUser(_name:String, _password:String, _email:String, _trust_score:Int):Boolean {
    //Check for email duplication
    val user = User.find { Users.email eq _email }.firstOrNull()
    if(user != null)
        return true

    User.new {
        name = _name
        email = _email
        trust_score = _trust_score
        password = _password
    }
    return false
}

fun updateUser(user:User, _name:String, _password:String, _email:String):Boolean {
    val found = User.find { Users.id neq user.id and (Users.email eq _email)}.firstOrNull() ?:return false

    user.password = _password
    user.email = _email
    user.name = _name

    return true
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

fun deleteUser(user_id: Int): Boolean {
    val user = User.findById(user_id) ?: return false

    val reviews: Query = Reviews.select { Reviews.user eq user.id }
    val reviewsList = Review.wrapRows(reviews).toList()

    ReviewVotes.deleteWhere { ReviewVotes.user eq user.id }

    //Delete all connections with user
    for (review in reviewsList) {
        deletePhotos(review.id.toString().toInt())
        ReviewAttributes.deleteWhere { ReviewAttributes.review eq review.id }
        ReviewVotes.deleteWhere { ReviewVotes.review eq review.id }
        review.delete()
    }

    user.delete()
    return true
}

/**
 * Brand queries
 */

fun createBrand(_name:String) {
    Brand.new {
        name = _name
    }
}

fun deleteBrand(_id:Int):Boolean {
    val brand = Brand.find { Brands.id eq _id }.firstOrNull() ?: return false

    val products = Product.wrapRows(Products.select{Products.brand eq brand.id}).toList()
    for(product in products) {
        deleteProduct(product.id.toString().toInt())
    }

    CategoriesBrands.deleteWhere { CategoriesBrands.brand eq brand.id }

    brand.delete()
    return true
}

/**
 * Category queries
 */

fun createCategory(_name:String) {
    Category.new {
        name = _name
    }
}

fun deleteCategory(_id:Int):Boolean {
    val category = Category.find { Categories.id eq _id }.firstOrNull() ?: return false

    val products = Product.wrapRows(Products.select{Products.category eq category.id}).toList()
    for(product in products) {
        deleteProduct(product.id.toString().toInt())
    }

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

fun getCategoryProducts(_id:Int, paging:Int, _orderBy:String?,_orderType:String?, _minPrice:String?,
                        _maxPrice:String?, _minScore:String?, _brands:String?): List<Product> {

    //Select attribute for query order
    val orderBy = when (_orderBy) {
        "price" -> Products.price
        "score" -> Products.score
        else -> {
            Products.id as Column<Int>
        }
    }

    //Order type - ASC or DESC
    val orderType: SortOrder = when (_orderType) {
        "asc" -> SortOrder.ASC
        else -> SortOrder.DESC
    }

    //This is option headers handling
    var maxPrice = DEFAULT_MIN
    var minPrice = DEFAULT_MIN
    var minScore = DEFAULT_MIN
    val products: Query
    val brands : List<Int>
    if (_minScore != null) {
        minScore = _minScore.toInt()
    }
    if(_minPrice != null){
        minPrice = _minPrice.toInt()
    }
    if(_maxPrice != null) {
        maxPrice = _maxPrice.toInt()
    }
    else {
        //Find the actual max value od product
        val product = Products.selectAll().orderBy(Products.price, SortOrder.DESC).firstOrNull()
        if (product != null) {
            maxPrice = Product.wrapRow(product).price
        }
    }

    //Filter by brands if we have them
    if(_brands != null) {
        brands = _brands.split(",").map { it.toInt() }.toList()

        products = Products.select {
            Products.category eq _id and (Products.price greaterEq minPrice) and (Products.price lessEq maxPrice) and
                    (Products.score greaterEq minScore) and (Products.brand inList brands)
        }
            .orderBy(orderBy, orderType)
            .limit(PAGE_LIMIT, ((paging - 1) * PAGE_LIMIT).toLong())
    }
    else {
        products = Products.select {
            Products.category eq _id and (Products.price greaterEq minPrice) and (Products.price lessEq maxPrice) and
                    (Products.score greaterEq minScore)
        }
            .orderBy(orderBy, orderType)
            .limit(PAGE_LIMIT, ((paging - 1) * PAGE_LIMIT).toLong())
    }

    return Product.wrapRows(products).toList()
}

/**
 * Product queries
 */

fun getProductInfo(_id: Int): Product? {
    return Product.find { Products.id eq _id }.firstOrNull()
}

fun deleteProduct(_id: Int): Boolean {
    val product = Product.find { Products.id eq _id }.firstOrNull() ?: return false

    val reviews: Query = Reviews.select { Reviews.product eq product.id }
    val reviewsList = Review.wrapRows(reviews).toList()

    for (review in reviewsList) {
        deletePhotos(review.id.toString().toInt())
        ReviewAttributes.deleteWhere { ReviewAttributes.review eq review.id }
        ReviewVotes.deleteWhere { ReviewVotes.review eq review.id }
        review.delete()
    }

    //Check if there are any products with this brand and category
    val test =
        Product.find { Products.brand eq product.brand.id and (Products.category eq product.category.id) and (Products.id neq product.id) }
            .firstOrNull()
    //If not then delete connection
    if (test == null) {
        CategoryBrand.find { CategoriesBrands.brand eq product.brand.id and (CategoriesBrands.category eq product.category.id) }
            .firstOrNull()?.delete()
    }

    product.delete()
    return true
}

fun addProduct(_name: String, _price: Int, category_ID: Int, brand_ID: Int): Boolean {
    val category = Category.findById(category_ID) ?: return false
    val brand =  Brand.findById(brand_ID) ?: return false

    //If this is the first time adding a brand for a category then create connection
    val catBrand =  CategoryBrand.find {CategoriesBrands.brand eq brand.id and (CategoriesBrands.category eq category.id)}.firstOrNull()
    if(catBrand == null) {
        CategoryBrand.new {
            this.brand = brand
            this.category =  category
        }
    }

    Product.new {
        name = _name
        price = _price
        score = 0
        this.category = category
        this.brand = brand
    }
    return true
}

fun searchProducts(_name: String, paging: Int): List<Product> {
    val products: Query = Products.select { Products.name like "%$_name%"}
        .limit(PAGE_LIMIT, ((paging - 1) * PAGE_LIMIT).toLong())
    return Product.wrapRows(products).toList()
}

/**
 * Review queries
 */

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

    //Create review
    val newReview = Review.new {
        text = reviewPostInfo.text
        this.product = product
        created_at = DateTime.now()
        this.user = user
        score = reviewPostInfo.score
    }

    //Create attributes for review
    for (attribute in reviewPostInfo.attributes) {
        ReviewAttribute.new {
            text = attribute.text
            is_positive = attribute.is_positive
            review = newReview
        }
    }

    //Recalculating product scora base on review score
    val numberOfReviews = Reviews.select { Reviews.product eq product.id }.count()
    val productScore = product.score

    val newProductScore = ((numberOfReviews * productScore) + reviewPostInfo.score) / (numberOfReviews + 1)
    product.score = newProductScore.toInt()

    return Status.OK
}

fun updateReview(reviewPutInfo: ReviewPutInfo, review_id: Int, auth: Int): Status {
    val review = Review.findById(review_id) ?: return Status.NOT_FOUND
    val user = User.findById(auth) ?: return Status.UNAUTHORIZED

    //If user is not creator of review
    if (review.user != user) {
        return Status.UNAUTHORIZED
    }

    //Recalculating product scora base on review score
    val product = review.product
    val numberOfReviews = Reviews.select { Reviews.product eq product.id }.count()
    val productScore = product.score

    val newProductScore = ((numberOfReviews * productScore) - review.score + reviewPutInfo.score) / (numberOfReviews)
    product.score = newProductScore.toInt()

    review.text = reviewPutInfo.text
    review.score = reviewPutInfo.score

    //Delete all review attributes and add new updated ones
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

    val reviewOwner = review.user

    //Check if vote is the same. If thats the case then delete it
    val sameVote = ReviewVote.find {
        (ReviewVotes.user eq user.id) and
                (ReviewVotes.review eq review.id) and
                (ReviewVotes.is_positive eq is_positive)
    }.firstOrNull()

    if (sameVote != null) {
        sameVote.delete()
        if (is_positive) {
            reviewOwner.trust_score--
        } else {
            reviewOwner.trust_score++
        }
        return Status.OK
    }

    //When we add the vote for first time or just change the vote
    val vote = ReviewVote.find { (ReviewVotes.user eq user.id) and (ReviewVotes.review eq review.id) }.firstOrNull()
    if (vote == null) {
        ReviewVote.new {
            this.user = user
            this.review = review
            this.is_positive = is_positive
        }
        if (is_positive) {
            reviewOwner.trust_score++
        } else {
            reviewOwner.trust_score--
        }
    } else {
        vote.is_positive = is_positive
        if (is_positive) {
            reviewOwner.trust_score += 2
        } else {
            reviewOwner.trust_score -= 2
        }
    }

    return Status.OK
}

fun deleteReview(auth: Int, review_id: Int): Status {
    val review = Review.findById(review_id) ?: return Status.NOT_FOUND
    val user = User.findById(auth) ?: return Status.UNAUTHORIZED

    //Check if user is a creator of the review
    if (review.user != user) {
        return Status.UNAUTHORIZED
    }

    ReviewAttributes.deleteWhere { ReviewAttributes.review eq review.id }
    ReviewVotes.deleteWhere { ReviewVotes.review eq review.id }
    deletePhotos(review_id)
    Reviews.deleteWhere { (Reviews.id eq review.id) and (Reviews.user eq user.id) }

    return Status.OK
}

fun getReviews(id: Int, paging: Int, _orderBy: String?, _orderType: String?, listType: ReviewListType): List<Review> {
    val orderType: SortOrder = when (_orderType) {
        "asc" -> SortOrder.ASC
        "desc" -> SortOrder.DESC
        else -> SortOrder.DESC
    }

    //Order by item has different types so we had to do this if else
    when (_orderBy) {
        "created_at" -> {
            // Query pre typ Column<DateTime>
            if (listType == ReviewListType.PRODUCT_REVIEWS) {
                val query = Reviews.select { Reviews.product eq id }.orderBy(Reviews.created_at, orderType)
                    .limit(PAGE_LIMIT, ((paging - 1) * PAGE_LIMIT).toLong())
                return Review.wrapRows(query).toList()
            }
            val query = Reviews.select { Reviews.user eq id }.orderBy(Reviews.created_at, orderType)
                .limit(PAGE_LIMIT, ((paging - 1) * PAGE_LIMIT).toLong())
            return Review.wrapRows(query).toList()
        }
        "score" -> {
            // Query pre typ Column<Int>
            if (listType == ReviewListType.PRODUCT_REVIEWS) {
                val query = Reviews.select { Reviews.product eq id }.orderBy(Reviews.score, orderType)
                    .limit(PAGE_LIMIT, ((paging - 1) * PAGE_LIMIT).toLong())
                return Review.wrapRows(query).toList()
            }
            val query = Reviews.select { Reviews.user eq id }.orderBy(Reviews.score, orderType)
                .limit(PAGE_LIMIT, ((paging - 1) * PAGE_LIMIT).toLong())
            return Review.wrapRows(query).toList()
        }
        else -> {
            // Query pre default order_by
            if (listType == ReviewListType.PRODUCT_REVIEWS) {
                val query = Reviews.select { Reviews.product eq id }.orderBy(Reviews.id, orderType)
                    .limit(PAGE_LIMIT, ((paging - 1) * PAGE_LIMIT).toLong())
                return Review.wrapRows(query).toList()
            }
            val query = Reviews.select { Reviews.user eq id }.orderBy(Reviews.score, orderType)
                .limit(PAGE_LIMIT, ((paging - 1) * PAGE_LIMIT).toLong())
            return Review.wrapRows(query).toList()
        }
    }
}

fun getRecentReview() :List<Review>{
    val query = Reviews.selectAll().orderBy(Reviews.id,SortOrder.DESC).limit(PAGE_LIMIT)
    return Review.wrapRows(query).toList()
}

fun getPhotoPath(review_id: Int, _id:Int): Photo? {
    val review = Review.find{Reviews.id eq review_id}.firstOrNull() ?: return null
    return Photo.find{Photos.id eq _id and (Photos.review eq review.id) }.firstOrNull()
}

fun createPhoto(_src:String, review_id: Int):Boolean {
    val _review = Review.find{Reviews.id eq review_id}.firstOrNull() ?: return false
    Photo.new {
        src=_src
        review=_review
    }
    return true
}

fun deletePhotos(_review_id:Int) {
    val photos = Photo.wrapRows(Photos.select{Photos.review eq _review_id}).toList()
    for(photo in photos) {
        try{
            File(photo.src).delete()
        }catch (e:Exception) {
            println(e.stackTraceToString())
        }
        photo.delete()
    }
}

fun checkPhotoOwnership(_user_id:Int, _review_id: Int,_photo_id:Int):Boolean {
    val photo = Photo.find { Photos.id eq _photo_id }.firstOrNull() ?: return false
    Review.find { Reviews.user eq _user_id and (Reviews.id eq photo.review.id) and (Reviews.id eq _review_id) }
        .firstOrNull() ?: return false
    return true
}

fun checkReviewOwnership(_user_id:Int, _review_id: Int):Boolean {
    Review.find{Reviews.user eq _user_id and (Reviews.id eq _review_id)}.firstOrNull()?:return false
    return true
}

fun deletePhoto(_id:Int):Boolean {
    val photo = Photo.find{Photos.id eq _id}.firstOrNull() ?: return false
    try{
        File(photo.src).delete()
    }catch (e:Exception) {
        println(e.stackTraceToString())
        return false
    }
    photo.delete()
    return true
}