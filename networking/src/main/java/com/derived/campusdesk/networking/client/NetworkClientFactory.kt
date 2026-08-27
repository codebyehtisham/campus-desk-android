package com.derived.campusdesk.networking.client

import com.derived.campusdesk.networking.api.ApiConfigProvider
import com.derived.campusdesk.networking.api.CampusDeskApi
import com.derived.campusdesk.networking.storage.TokenStore
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class NetworkClientFactory(
    private val apiConfigProvider: ApiConfigProvider,
    private val tokenStore: TokenStore,
    private val debugInterceptor: Interceptor? = null,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun createApi(): CampusDeskApi {
        val config = apiConfigProvider.current()
        val clientBuilder = OkHttpClient.Builder()
            .dns(ResilientDns)
            .connectTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                    .header("Accept", "application/json")
                if (original.body != null) {
                    builder.header("Content-Type", "application/json")
                }
                tokenStore.token()?.takeIf { it.isNotBlank() }?.let { token ->
                    if (!original.url.encodedPath.contains("/auth/login") &&
                        !original.url.encodedPath.contains("/auth/register") &&
                        !original.url.encodedPath.contains("/auth/forgot-password") &&
                        !original.url.encodedPath.contains("/api/health") &&
                        !original.url.encodedPath.contains("/api/settings") &&
                        !original.url.encodedPath.contains("/api/news") &&
                        !original.url.encodedPath.contains("/api/faculty") &&
                        !original.url.encodedPath.contains("/api/courses")
                    ) {
                        builder.header("Authorization", "Bearer $token")
                    }
                }
                chain.proceed(builder.build())
            }

        debugInterceptor?.let { clientBuilder.addInterceptor(it) }

        val retrofit = Retrofit.Builder()
            .baseUrl(ensureTrailingSlash(config.baseUrl))
            .client(clientBuilder.build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(CampusDeskApi::class.java)
    }

    val campusJson: Json get() = json

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}
