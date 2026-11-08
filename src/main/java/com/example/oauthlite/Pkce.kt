package com.example.oauthlite

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

internal object Pkce {

    private val random = SecureRandom()

    private const val FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

    fun newCodeVerifier(): String = randomString(64)

    fun newState(): String = randomString(16)

    fun challengeS256(verifier: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(hash, FLAGS)
    }

    private fun randomString(byteCount: Int): String {
        val bytes = ByteArray(byteCount).also { random.nextBytes(it) }
        return Base64.encodeToString(bytes, FLAGS)
    }
}
