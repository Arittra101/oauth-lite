package com.example.oauthlite

import android.app.Activity
import android.content.Context
import android.content.Intent

object OAuthLite {

    fun createLoginIntent(context: Context, config: AuthConfig): Intent {
        val state = Pkce.newState()
        val verifier = Pkce.newCodeVerifier()

        val authUri = config.authEndpoint.buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", config.clientId)
            .appendQueryParameter("redirect_uri", config.redirectUri.toString())
            .appendQueryParameter("scope", config.scopes.joinToString(" "))
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", Pkce.challengeS256(verifier))
            .appendQueryParameter("code_challenge_method", "S256")
            .apply { config.extraParams.forEach { (k, v) -> appendQueryParameter(k, v) } }
            .build()

        return Intent(context, AuthManagementActivity::class.java).apply {
            putExtra(Keys.AUTH_URI, authUri.toString())
            putExtra(Keys.STATE, state)
            putExtra(Keys.VERIFIER, verifier)
            putExtra(Keys.REDIRECT_URI, config.redirectUri.toString())
            putStringArrayListExtra(Keys.BLOCKED_BROWSERS, ArrayList(config.blockedBrowsers))
        }
    }

    fun parseResult(resultCode: Int, data: Intent?): AuthResult {
        if (data == null) return AuthResult.Cancelled

        data.getStringExtra(Keys.RESULT_ERROR)?.let { error ->
            return AuthResult.Error(error, data.getStringExtra(Keys.RESULT_ERROR_DESC))
        }

        if (resultCode != Activity.RESULT_OK) return AuthResult.Cancelled

        val code = data.getStringExtra(Keys.RESULT_CODE)
        val verifier = data.getStringExtra(Keys.RESULT_VERIFIER)
        val redirectUri = data.getStringExtra(Keys.RESULT_REDIRECT_URI)
        val state = data.getStringExtra(Keys.RESULT_STATE)

        return if (code != null && verifier != null && redirectUri != null && state != null) {
            AuthResult.Success(code, verifier, redirectUri, state)
        } else {
            AuthResult.Error("invalid_response", "Missing data in result")
        }
    }
}
