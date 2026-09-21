package com.derived.campusdesk.networking.analytics

import okhttp3.Interceptor
import okhttp3.Response

/** Emits `api_call` analytics for every OkHttp request (Traject parity). */
class ArcherApiInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startedAt = System.currentTimeMillis()
        return try {
            val response = chain.proceed(request)
            ArcherAnalytics.api(
                method = request.method,
                path = request.url.encodedPath,
                statusCode = response.code,
                durationMs = (System.currentTimeMillis() - startedAt).toInt(),
                outcome = outcomeFor(response.code),
            )
            response
        } catch (t: Throwable) {
            ArcherAnalytics.api(
                method = request.method,
                path = request.url.encodedPath,
                statusCode = null,
                durationMs = (System.currentTimeMillis() - startedAt).toInt(),
                outcome = "error",
            )
            throw t
        }
    }

    private fun outcomeFor(code: Int): String = when (code) {
        in 200..299 -> "success"
        in 400..499 -> "client_error"
        in 500..599 -> "server_error"
        else -> "other"
    }
}
