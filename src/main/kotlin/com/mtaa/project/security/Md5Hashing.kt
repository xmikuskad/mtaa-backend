package com.mtaa.project.security

import java.security.NoSuchAlgorithmException

import java.security.MessageDigest

//MD5 hashing bol prevzany zo stranky https://howtodoinjava.com/security/how-to-generate-secure-password-hash-md5-sha-pbkdf2-bcrypt-examples/
fun getSecurePassword(passwordToHash: String): String? {
    val generatedPassword: String?
    try {
        val md = MessageDigest.getInstance("MD5")
        md.update(passwordToHash.toByteArray())
        val bytes = md.digest()
        val sb = StringBuilder()
        for (aByte in bytes) {
            sb.append(((aByte.toInt() and 0xff) + 0x100).toString(16).substring(1))
        }
        generatedPassword = sb.toString()
    } catch (e: NoSuchAlgorithmException) { //Shouldnt happen
        e.printStackTrace()
        return null
    }
    return generatedPassword
}
