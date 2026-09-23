package com.psrm.forms.psbdx.data.repository

import com.psrm.forms.psbdx.data.remote.CredentialStore
import com.psrm.forms.psbdx.data.remote.WordPressApiClient
import com.psrm.forms.psbdx.domain.model.WpSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface LoginResult {
    data class Success(val session: WpSession) : LoginResult
    data class Failure(val message: String) : LoginResult
}

class AuthRepository(private val credentialStore: CredentialStore) {

    private val _session = MutableStateFlow<WpSession?>(null)
    val session: StateFlow<WpSession?> = _session.asStateFlow()

    /**
     * Verifies the given site URL + username + Application Password.
     *
     * Runs a pre-flight check against the unauthenticated REST index first
     * (see [com.psrm.forms.psbdx.data.remote.WordPressApi.getSiteIndexRaw])
     * so a failure can say *why* — unreachable REST API, HTTP vs HTTPS,
     * Application Passwords disabled site-wide or network-wide on
     * Multisite — rather than the generic 401 that's all `wp/v2/users/me`
     * alone can tell you, since WP returns the same rejected-auth response
     * whether the password is wrong or the whole feature is off.
     */
    suspend fun login(siteUrl: String, username: String, applicationPassword: String): LoginResult {
        credentialStore.siteUrl = siteUrl
        credentialStore.username = username
        credentialStore.applicationPassword = applicationPassword

        return try {
            val api = WordPressApiClient.create(credentialStore)

            val preflight = runCatching { api.getSiteIndexRaw() }
            val preflightBody = preflight.getOrNull()?.takeIf { it.isSuccessful }?.body()?.string()

            if (preflight.isFailure || preflightBody == null) {
                credentialStore.clear()
                return LoginResult.Failure(
                    "Couldn't reach the WordPress REST API at that URL. Check it's correct and reachable " +
                        "(on Multisite, use the specific site's URL, not the network's main domain), and " +
                        "that nothing — a firewall, security plugin, or maintenance mode — is blocking /wp-json/."
                )
            }

            if (!preflightBody.contains("application-passwords")) {
                credentialStore.clear()
                return LoginResult.Failure(
                    "This site doesn't have Application Passwords enabled. WordPress disables them by " +
                        "default over plain HTTP — the site needs HTTPS, or a filter override on " +
                        "`wp_is_application_passwords_available`. On Multisite, they can also be turned " +
                        "off network-wide by the network admin or a security plugin — check Network " +
                        "Settings and any hardening plugin's auth settings."
                )
            }

            val response = api.getCurrentUser()

            if (!response.isSuccessful || response.body() == null) {
                credentialStore.clear()
                LoginResult.Failure(
                    when (response.code()) {
                        401, 403 -> "That username/Application Password combination was rejected by the site. " +
                            "Double-check you generated it under Users → Profile → Application Passwords for " +
                            "this exact user, and pasted the full value (spaces included)."
                        404 -> "The WordPress REST API wasn't found at that site URL — check it's reachable and not blocked."
                        else -> "Login failed (HTTP ${response.code()})."
                    }
                )
            } else {
                val me = response.body()!!
                val session = WpSession(
                    siteUrl = siteUrl,
                    username = username,
                    displayName = me.name,
                    avatarUrl = me.avatarUrls?.values?.lastOrNull(),
                    roles = me.roles,
                    // Gate CRUD/response screens on real WP capabilities rather
                    // than guessing from role name, so custom role setups are
                    // respected too.
                    canManageForms = me.capabilities["manage_options"] == true ||
                        me.capabilities["edit_others_posts"] == true,
                    canViewResponses = me.capabilities["manage_options"] == true ||
                        me.capabilities["moderate_comments"] == true
                )
                _session.value = session
                LoginResult.Success(session)
            }
        } catch (e: Exception) {
            credentialStore.clear()
            LoginResult.Failure(e.message ?: "Couldn't reach the site — check the URL and your connection.")
        }
    }

    fun restoreSession(): WpSession? {
        // A lightweight "am I still logged in" check for app relaunch;
        // FormsListViewModel re-verifies against the server on load, since
        // a stored credential could have been revoked server-side since.
        if (!credentialStore.isLoggedIn) return null
        val cached = WpSession(
            siteUrl = credentialStore.siteUrl!!,
            username = credentialStore.username!!,
            displayName = credentialStore.username!!,
            avatarUrl = null,
            roles = emptyList(),
            canManageForms = true,
            canViewResponses = true
        )
        _session.value = cached
        return cached
    }

    fun logout() {
        credentialStore.clear()
        _session.value = null
    }
}
