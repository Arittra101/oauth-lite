package com.example.oauthlite

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent

internal class AuthManagementActivity : Activity() {

    private var authUri: String? = null
    private var expectedState: String? = null
    private var verifier: String? = null
    private var redirectUri: String? = null
    private var started = false
    private var blockedBrowsers: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val src = savedInstanceState ?: intent.extras
        if (src == null) {
            finishWithError("invalid_request", "No auth request")
            return
        }
        authUri = src.getString(Keys.AUTH_URI)
        expectedState = src.getString(Keys.STATE)
        verifier = src.getString(Keys.VERIFIER)
        redirectUri = src.getString(Keys.REDIRECT_URI)
        started = src.getBoolean(Keys.STARTED, false)
        blockedBrowsers = src.getStringArrayList(Keys.BLOCKED_BROWSERS) ?: emptyList()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (isFinishing) return

        if (!started) {
            openLoginPage()
            return
        }

        val responseUri = intent.data
        if (responseUri != null) {
            handleRedirect(responseUri)
        } else {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(Keys.AUTH_URI, authUri)
        outState.putString(Keys.STATE, expectedState)
        outState.putString(Keys.VERIFIER, verifier)
        outState.putString(Keys.REDIRECT_URI, redirectUri)
        outState.putBoolean(Keys.STARTED, started)
        outState.putStringArrayList(Keys.BLOCKED_BROWSERS, ArrayList(blockedBrowsers))
    }

    private fun openLoginPage() {
        val uri = authUri ?: return finishWithError("invalid_request", "No auth uri")
        started = true
        try {
            launchInBrowser(Uri.parse(uri))
        } catch (e: ActivityNotFoundException) {
            finishWithError("no_browser", "No browser found on this device")
        }
    }

    private fun launchInBrowser(uri: Uri) {
        val browser = BrowserSelector.select(packageManager, blockedBrowsers)
        when {
            browser == null ->
                CustomTabsIntent.Builder().build().launchUrl(this, uri)

            browser.supportsCustomTabs ->
                CustomTabsIntent.Builder().build()
                    .also { it.intent.setPackage(browser.packageName) }
                    .launchUrl(this, uri)

            else ->
                startActivity(
                    Intent(Intent.ACTION_VIEW, uri)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .setPackage(browser.packageName)
                )
        }
    }

    private fun handleRedirect(uri: Uri) {
        uri.getQueryParameter("error")?.let { error ->
            finishWithError(error, uri.getQueryParameter("error_description"))
            return
        }

        val state = uri.getQueryParameter("state")
        if (state == null || state != expectedState) {
            finishWithError("state_mismatch", "State does not match")
            return
        }

        val code = uri.getQueryParameter("code")
        if (code.isNullOrEmpty()) {
            finishWithError("invalid_response", "No code in redirect")
            return
        }

        setResult(RESULT_OK, Intent().apply {
            putExtra(Keys.RESULT_CODE, code)
            putExtra(Keys.RESULT_VERIFIER, verifier)
            putExtra(Keys.RESULT_REDIRECT_URI, redirectUri)
            putExtra(Keys.RESULT_STATE, state)
        })
        finish()
    }

    private fun finishWithError(error: String, description: String?) {
        setResult(RESULT_CANCELED, Intent().apply {
            putExtra(Keys.RESULT_ERROR, error)
            putExtra(Keys.RESULT_ERROR_DESC, description)
        })
        finish()
    }
}
