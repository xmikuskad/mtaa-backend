package com.mtaa.project.security

import java.security.NoSuchAlgorithmException

import java.security.MessageDigest

fun getSecurePassword(passwordToHash: String): String? {
    val generatedPassword: String?
    try {
        val md = MessageDigest.getInstance("MD5")
        md.update(passwordToHash.toByteArray())
        val bytes = md.digest()
        val sb = StringBuilder()
        for (aByte in bytes) {
            sb.append(Integer.toString((aByte.toInt() and 0xff) + 0x100, 16).substring(1))
        }
        generatedPassword = sb.toString()
    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
        return null
    }
    return generatedPassword
}
