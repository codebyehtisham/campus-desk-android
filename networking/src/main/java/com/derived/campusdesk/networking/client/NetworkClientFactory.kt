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
    private val analyticsInterceptor: Interceptor? = null,
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
                val path = original.url.encodedPath
                val builder = original.newBuilder()
                    .header("Accept", "application/json")
                if (original.body != null) {
                    builder.header("Content-Type", "application/json")
                }
                tokenStore.token()?.takeIf { it.isNotBlank() }?.let { token ->
                    if (!isPublicAuthPath(path)) {
                        builder.header("Authorization", "Bearer $token")
                    }
                }
                val response = chain.proceed(builder.build())
                if (response.code == 401 && !isLoginStylePath(path)) {
                    tokenStore.clear()
                    SessionEvents.notifyUnauthorized()
                }
                response
            }

        analyticsInterceptor?.let { clientBuilder.addInterceptor(it) }
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

    private fun isPublicAuthPath(path: String): Boolean =
        path.contains("/auth/login") ||
            path.contains("/auth/register") ||
            path.contains("/auth/forgot-password") ||
            path.contains("/auth/password-key") ||
            path.contains("/api/health") ||
            path.contains("/api/settings") ||
            path.contains("/api/news") ||
            path.contains("/api/faculty") ||
            path.contains("/api/courses")

    private fun isLoginStylePath(path: String): Boolean =
        path.contains("/auth/login") ||
            path.contains("/auth/register") ||
            path.contains("/auth/forgot-password") ||
            path.contains("/auth/password-key")
}
