package com.psrm.forms.psbdx.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Holds the site URL, WP username, and Application Password.
 *
 * Application Passwords are WordPress core (5.6+) — no OAuth app-registration
 * step needed, which keeps this F-Droid-clean (no proprietary auth SDK). The
 * value entered here is the space-separated password WP itself generates
 * under Users → Profile → Application Passwords, NOT the user's normal
 * login password.
 *
 * Stored via EncryptedSharedPreferences (Jetpack Security, AndroidX/AOSP —
 * not a proprietary dependency) rather than plain SharedPreferences, since
 * this credential grants full REST API access as that WP user.
 */
class CredentialStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "psrm_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var siteUrl: String?
        get() = prefs.getString(KEY_SITE_URL, null)
        set(value) = prefs.edit().putString(KEY_SITE_URL, value).apply()

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var applicationPassword: String?
        get() = prefs.getString(KEY_APP_PASSWORD, null)
        set(value) = prefs.edit().putString(KEY_APP_PASSWORD, value).apply()

    val isLoggedIn: Boolean
        get() = !siteUrl.isNullOrBlank() && !username.isNullOrBlank() && !applicationPassword.isNullOrBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_SITE_URL = "site_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_APP_PASSWORD = "app_password"
    }
}
