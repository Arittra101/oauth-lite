
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import com.example.oauthlite.AuthConfig
import com.example.oauthlite.AuthResult
import com.example.oauthlite.OAuthLite

sealed interface XAuthResult {
    data class Success(
        val code: String,
        val codeVerifier: String,
        val redirectUri: String
    ) : XAuthResult
    data object Cancelled : XAuthResult
    data class Error(val message: String) : XAuthResult
}

class XAuthManager(context: Context) {

    private val appContext = context.applicationContext

    private val config = AuthConfig(
        authEndpoint = XAuthConfig.AUTH_ENDPOINT,
        clientId = XAuthConfig.CLIENT_ID,
        redirectUri = Uri.parse(XAuthConfig.REDIRECT_URI),
        scopes = XAuthConfig.SCOPES.toList()
    )

    fun createLoginIntent(): Intent = OAuthLite.createLoginIntent(appContext, config)

    fun parseResult(result: ActivityResult): XAuthResult =
        when (val r = OAuthLite.parseResult(result.resultCode, result.data)) {
            is AuthResult.Success -> XAuthResult.Success(
                code = r.code,
                codeVerifier = r.codeVerifier,
                redirectUri = r.redirectUri
            )
            AuthResult.Cancelled -> XAuthResult.Cancelled

            is AuthResult.Error ->
                if (r.error == "access_denied") XAuthResult.Cancelled
                else XAuthResult.Error(r.description ?: r.error)
        }

    fun dispose() = Unit
}
