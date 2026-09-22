package com.psrm.forms.psbdx.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds a WordPressApi bound to whatever site URL is currently stored.
 * Re-created on login (site URL is only known once the user enters it),
 * so this is a factory rather than a singleton constructed at app start.
 */
object WordPressApiClient {

    fun create(credentialStore: CredentialStore): WordPressApi {
        val baseUrl = credentialStore.siteUrl
            ?.trimEnd('/')
            ?.let { "$it/" }
            ?: error("Site URL not set — complete login first")

        val logging = HttpLoggingInterceptor().apply {
            // BASIC, not BODY: an Application Password rides in the
            // Authorization header on every request, so full-body logging
            // is avoided even in debug builds to keep credentials/PII out
            // of logcat.
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(WordPressAuthInterceptor(credentialStore))
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WordPressApi::class.java)
    }
}
