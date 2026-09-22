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
     * Verifies the given site URL + username + Application Password by
     * calling `wp/v2/users/me` — the same request the header display and
     * every later screen depends on, so a failure here means the app can't
     * do anything useful yet and the user needs to fix the credential.
     */
    suspend fun login(siteUrl: String, username: String, applicationPassword: String): LoginResult {
        credentialStore.siteUrl = siteUrl
        credentialStore.username = username
        credentialStore.applicationPassword = applicationPassword

        return try {
            val api = WordPressApiClient.create(credentialStore)
            val response = api.getCurrentUser()

            if (!response.isSuccessful || response.body() == null) {
                credentialStore.clear()
                LoginResult.Failure(
                    when (response.code()) {
                        401, 403 -> "That username/Application Password combination was rejected by the site."
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
