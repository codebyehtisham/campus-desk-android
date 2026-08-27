package com.derived.campusdesk.networking.api

import com.derived.campusdesk.networking.client.ResilientDns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Probes a preset's `/api/health` URL directly (not the active Retrofit client).
 * Matches iOS `EnvironmentPickerSheet.checkHealth` — any HTTP 2xx means online.
 */
object EnvironmentHealthProbe {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .dns(ResilientDns)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun isOnline(preset: ApiEnvironmentPreset): Boolean = withContext(Dispatchers.IO) {
        isOnline(preset.healthUrl)
    }

    suspend fun isOnline(healthUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(healthUrl)
                .get()
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                response.code in 200..299
            }
        } catch (_: Exception) {
            false
        }
    }
}
