package com.shahid.cents

import java.security.MessageDigest

object SmsHash {
    fun create(sender: String, timestamp: Long, body: String): String {
        val input = "$sender|$timestamp|${body.trim().lowercase()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
