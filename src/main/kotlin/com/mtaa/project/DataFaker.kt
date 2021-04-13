package com.mtaa.project

import com.mtaa.project.routing.DIRECTORY_NAME
import com.mtaa.project.security.getSecurePassword
import io.github.serpro69.kfaker.Faker
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import kotlin.random.Random

const val USER_COUNT = 30           //number of generated users
const val PRODUCTS_PER_CATEGORY = 25
const val MIN_PRICE = 10000         //max product price
const val MAX_PRICE = 500000        //min product price
const val MAX_PHOTOS = 4            //max photos per review
const val PHOTOS_COUNT = 12         //available photos for each category
const val MIN_REVIEW_SIZE = 60
const val MAX_REVIEW_SIZE = 1000
const val MAX_REVIEW_ATTRIBUTES = 5 //max attributes for review (real max is *2 => pos + neg)
const val USER_VOTES_COUNT = 1000   //how many votes can 1 user do -- there are about 9000 reviews

val categories = mutableListOf("PCs","Phones","Cameras","Laptops","Headphones","Printers",
    "Tablets","TVs","Consoles","Monitors","Smart watches","Routers")
val brands = mutableListOf("Huawei","Xiaomi","ZTE","Lenovo","Fujitsu","Canon","Nikon","Olympus",
    "Nintendo","Sony","Toshiba","LG","Hyundai","Asus","Acer","Realtek","HTC","Nokia","Siemens",
    "Philips","Dell","Google","Microsoft","Intel","HP","AMD","Bosch","Electrolux","Apple","IBM","Nvidia")


fun createFakeData() {
    val faker = Faker()
    faker.unique.clearAll()

    val reviewFile = File("faker/reviews.txt")
    val reviews = reviewFile.readLines()
    val posFile = File("faker/positive.txt")
    val positive = posFile.readLines()
    val negFile = File("faker/negative.txt")
    val negative = negFile.readLines()

    //Create users
    transaction {
        for (i in 1..USER_COUNT) {
            generateUser(faker)
        }
    }

    //Create categories
    transaction {
        for (i in 0 until categories.size) {
            generateCategory(categories[i])
        }
    }

    //Create brands
    transaction {
        for (i in 0 until brands.size) {
            generateBrand(brands[i])
        }
    }

    //Create products
    transaction {
        for (i in 1 until categories.size+1) {
            for (j in 1..PRODUCTS_PER_CATEGORY) {
                generateProduct(i, faker)
            }
            //Clear used values
            faker.unique.clear(faker::code)
        }
    }

    //create reviews - about 9000
    transaction {
        for (i in 1..USER_COUNT) {
            for (j in 1..(categories.size * PRODUCTS_PER_CATEGORY)) {
                generateReview(j, i, reviews, positive, negative)
            }
        }
    }

    //create votes
    val reviewsCount = USER_COUNT * categories.size * PRODUCTS_PER_CATEGORY
    transaction {
        for (i in 1..USER_COUNT) {
            for (j in 1..USER_VOTES_COUNT) {
                generateVote(i, reviewsCount)
            }
        }
    }

}

fun generateUser(faker:Faker)
{
    val pass = getSecurePassword(faker.eSport.players()) ?:"randomPass"
    val email = faker.name.unique.firstName().toLowerCase()+"@gmail.com"
    createUser(faker.name.unique.name(), pass,email,0)
}

fun generateCategory(name:String)
{
    createCategory(name)
}

fun generateBrand(name:String)
{
    createBrand(name)
}

fun generateProduct(categoryID:Int,faker:Faker) {

    val price = Random.nextInt(MIN_PRICE, MAX_PRICE)
    val brandIter = Random.nextInt(0, brands.size)
    val name = brands[brandIter] + " " + faker.code.unique.asin()

    createProduct(name, price, categoryID, brandIter+1)
}

fun generateReview(productID: Int, userID:Int,reviews:List<String>, positive:List<String>, negative:List<String>) {
    val score = Random.nextInt(0, 100)
    val photoCount = Random.nextInt(1, MAX_PHOTOS+1)
    val productInfo = getProductInfo(productID)
    val categoryID = productInfo?.category?.id.toString().toInt()

    val text :String
    val photos = mutableListOf<Int>()
    val attributes = mutableListOf<ReviewAttributePostPutInfo>()

    //Choose random photo ID
    while(photos.size < photoCount) {
        val photoID = Random.nextInt(0,PHOTOS_COUNT)
        if(photoID !in photos) {
            photos.add(photoID)
        }
    }

    //Choose random text review
    while(true){
        val randomID = Random.nextInt(0,reviews.size)
        if(reviews[randomID].length in MIN_REVIEW_SIZE until MAX_REVIEW_SIZE)
        {
            text = reviews[randomID]
            break
        }
    }

    //Random positive attributes
    val posCount = Random.nextInt(1, MAX_REVIEW_ATTRIBUTES+1)
    for(i in 1..posCount) {
        val index = Random.nextInt(0,positive.size)
        attributes.add(ReviewAttributePostPutInfo(positive[index],true))
    }

    //Random negative attibutes
    val negCount = Random.nextInt(1, MAX_REVIEW_ATTRIBUTES+1)
    for(i in 1..negCount) {
        val index = Random.nextInt(0,negative.size)
        attributes.add(ReviewAttributePostPutInfo(negative[index],false))
    }

    //Create review
    val reviewID = createReview(ReviewPostInfo(text,attributes,productID,score),userID)

    //Add photos
    for(photo in photos) {
        val photoName = categories[categoryID-1].toLowerCase().replace(' ','_')+photo.toString()
        createPhoto("$DIRECTORY_NAME/$photoName", reviewID)
    }

}

fun generateVote(userID:Int, reviewCount:Int){
    val reviewID = Random.nextInt(1,reviewCount+1)
    val isPositive = Random.nextBoolean()

    voteOnReview(userID,reviewID,isPositive)
}