package com.derived.campusdesk.networking.debug

data class ApiResponseLog(
    val id: Long = System.currentTimeMillis(),
    val method: String,
    val url: String,
    val requestBody: String?,
    val responseBody: String?,
    val statusCode: Int,
    val durationMs: Long,
)

object NetworkResponseStore {
    private const val MAX_LOGS = 100
    private val logs = mutableListOf<ApiResponseLog>()
    private val lock = Any()

    fun add(log: ApiResponseLog) = synchronized(lock) {
        logs.add(0, log)
        while (logs.size > MAX_LOGS) logs.removeLast()
    }

    fun all(): List<ApiResponseLog> = synchronized(lock) { logs.toList() }

    fun clear() = synchronized(lock) { logs.clear() }
}

interface DevToolsConfig {
    val isDevToolsEnabled: Boolean
}
