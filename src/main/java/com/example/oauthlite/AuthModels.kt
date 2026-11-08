package com.example.oauthlite

import android.net.Uri

data class AuthConfig(
    val authEndpoint: Uri,
    val clientId: String,
    val redirectUri: Uri,
    val scopes: List<String>,
    val extraParams: Map<String, String> = emptyMap(),
    val blockedBrowsers: Set<String> = emptySet()
)

object KnownBrowsers {
    const val CHROME = "com.android.chrome"
    const val FIREFOX = "org.mozilla.firefox"
    const val SAMSUNG_INTERNET = "com.sec.android.app.sbrowser"
    const val EDGE = "com.microsoft.emmx"
    const val OPERA = "com.opera.browser"
    const val OPERA_MINI = "com.opera.mini.native"
    const val BRAVE = "com.brave.browser"
    const val UC_BROWSER = "com.UCMobile.intl"
}

sealed interface AuthResult {

    data class Success(
        val code: String,
        val codeVerifier: String,
        val redirectUri: String,
        val state: String
    ) : AuthResult

    data object Cancelled : AuthResult

    data class Error(
        val error: String,
        val description: String? = null
    ) : AuthResult
}

internal object Keys {
    const val AUTH_URI = "oauthlite.auth_uri"
    const val STATE = "oauthlite.state"
    const val VERIFIER = "oauthlite.verifier"
    const val REDIRECT_URI = "oauthlite.redirect_uri"
    const val STARTED = "oauthlite.started"
    const val BLOCKED_BROWSERS = "oauthlite.blocked_browsers"

    const val RESULT_CODE = "oauthlite.result.code"
    const val RESULT_VERIFIER = "oauthlite.result.verifier"
    const val RESULT_REDIRECT_URI = "oauthlite.result.redirect_uri"
    const val RESULT_STATE = "oauthlite.result.state"
    const val RESULT_ERROR = "oauthlite.result.error"
    const val RESULT_ERROR_DESC = "oauthlite.result.error_desc"
}
