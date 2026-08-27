package com.derived.campusdesk.networking.debug

import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer

class NetworkResponseLogger(
    private val devToolsConfig: DevToolsConfig,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!devToolsConfig.isDevToolsEnabled) return chain.proceed(chain.request())

        val request = chain.request()
        val start = System.currentTimeMillis()
        val requestBody = request.body?.let { body ->
            Buffer().also { body.writeTo(it) }.readUtf8()
        }

        val response = chain.proceed(request)
        val duration = System.currentTimeMillis() - start
        val responseBody = response.peekBody(1024 * 256).string()

        NetworkResponseStore.add(
            ApiResponseLog(
                method = request.method,
                url = request.url.toString(),
                requestBody = requestBody,
                responseBody = responseBody,
                statusCode = response.code,
                durationMs = duration,
            ),
        )
        return response
    }
}
