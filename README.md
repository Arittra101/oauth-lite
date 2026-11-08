# oauth-lite

Small replacement for AppAuth. Only does what we need for X login:
open login page -> catch redirect -> check state -> give back `code` + `codeVerifier`.
Token exchange happens on the backend.

## Files

| File | Job |
|---|---|
| `Pkce.kt` | Makes `code_verifier`, `code_challenge` (S256) and `state` |
| `AuthModels.kt` | `AuthConfig` (input) and `AuthResult` (output) |
| `OAuthLite.kt` | Public API: `createLoginIntent()` and `parseResult()` |
| `AuthManagementActivity.kt` | Invisible activity: opens Custom Tab, waits, checks the result |
| `RedirectReceiverActivity.kt` | Catches the redirect and sends it to the activity above |
| `src/main/AndroidManifest.xml` | Registers both activities + redirect intent-filter |
| `BrowserSelector.kt` | Picks a Custom Tabs-capable browser, prefers the user's default |
| `sample/XAuthManager.kt` | Your X manager, now using oauth-lite (copy into app) |

## Setup in the app

**settings.gradle.kts**
```kotlin
include(":oauth-lite")
```

**app/build.gradle.kts**
```kotlin
android {
    defaultConfig {
        // Must match the scheme of your REDIRECT_URI.
        // e.g. REDIRECT_URI = "com.example.app://x-callback"  -> scheme = "com.example.app"
        manifestPlaceholders["oauthRedirectScheme"] = "com.example.app"
    }
}

dependencies {
    implementation(project(":oauth-lite"))
    // remove: implementation("net.openid:appauth:...")
}
```
If you forget the placeholder, the build fails at manifest merge. That is on purpose.

Remove any old `appAuthRedirectScheme` placeholder and any `RedirectUriReceiverActivity`
entry from your app manifest.

## Use in Fragment (same as before)
```kotlin
private val xAuth by lazy { XAuthManager(requireContext()) }

private val loginLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (val r = xAuth.parseResult(result)) {
            is XAuthResult.Success -> viewModel.sendToBackend(r.code, r.codeVerifier, r.redirectUri)
            XAuthResult.Cancelled -> { /* do nothing */ }
            is XAuthResult.Error -> showError(r.message)
        }
    }

binding.btnX.setOnClickListener { loginLauncher.launch(xAuth.createLoginIntent()) }
```

## Browser selection

Picked by `BrowserSelector`, in this order:

1. Default browser that supports Custom Tabs
2. Any browser that supports Custom Tabs
3. Default browser (plain `ACTION_VIEW`)
4. Any browser (plain `ACTION_VIEW`)

Preferring the user's default matters: they may already be signed in to X there,
so the login page can skip straight to the consent screen.

Only real browsers are considered - a candidate must declare `http` **and**
`https` with no host or path restrictions, which filters out apps that register
narrow deep links.

The `<queries>` block in `src/main/AndroidManifest.xml` is required. Without it,
Android 11+ package visibility makes `queryIntentActivities` return nothing and
no browser is ever found. It travels with the AAR, so the app needs no change.

To skip a browser that misbehaves:

```kotlin
AuthConfig(
    authEndpoint = ...,
    clientId = ...,
    redirectUri = ...,
    scopes = ...,
    blockedBrowsers = setOf(KnownBrowsers.UC_BROWSER)
)
```

The default is empty - nothing is blocked unless you say so. `KnownBrowsers`
holds package-name constants so you do not have to hardcode strings. If
selection finds nothing at all, the call falls back to a plain Custom Tabs
launch and lets the OS choose, so behaviour is never worse than before.

## What it handles
- PKCE S256 and random `state`, with `state` check on return
- Prefers a Custom Tabs browser, with an optional per-request blocklist
- User closes Custom Tab -> `Cancelled`
- Provider error (`access_denied`, etc.) -> `Error(error, description)`
- No browser on device -> `Error("no_browser")`
- Android kills the app while user is on the login page -> request is saved and restored

## What it does NOT do (on purpose)
- Token exchange / refresh (backend does it)
- OpenID discovery and ID token checks (X does not use OpenID Connect)
- HTTPS App Link redirects (only custom scheme). Add a second intent-filter if you need it.
