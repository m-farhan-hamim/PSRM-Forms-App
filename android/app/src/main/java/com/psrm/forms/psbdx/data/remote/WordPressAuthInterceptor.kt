package com.psrm.forms.psbdx.data.remote

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

/**
 * WordPress Application Passwords authenticate over standard HTTP Basic
 * auth: `Authorization: Basic base64(username:application_password)`. WP
 * core accepts this directly on wp-json routes — no token refresh, no
 * OAuth dance, which is exactly why Application Passwords are the right
 * fit for an F-Droid app with zero proprietary auth dependencies.
 */
class WordPressAuthInterceptor(
    private val credentialStore: CredentialStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val username = credentialStore.username
        val password = credentialStore.applicationPassword

        val request = if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            chain.request().newBuilder()
                .header("Authorization", Credentials.basic(username, password))
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}
