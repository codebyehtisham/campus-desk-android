package com.derived.campusdesk.networking.api

enum class AppEnvironment {
    DEVELOPMENT,
    PRODUCTION,
}

data class ApiConfiguration(
    val baseUrl: String,
    val timeoutSeconds: Long,
    val environment: AppEnvironment,
) {
    val isDevelopment: Boolean get() = environment == AppEnvironment.DEVELOPMENT

    companion object {
        const val PRODUCTION_URL = "https://campusdesk-production-9ab3.up.railway.app"
        const val DEVELOPMENT_PRESET_URL = "https://adequate-success-production-39da.up.railway.app"
        const val LOCAL_URL = "http://localhost:5050"
        const val DEFAULT_INSTITUTE_SLUG = "explore"

        val production = ApiConfiguration(
            baseUrl = PRODUCTION_URL,
            timeoutSeconds = 30,
            environment = AppEnvironment.PRODUCTION,
        )

        val developmentPreset = ApiConfiguration(
            baseUrl = DEVELOPMENT_PRESET_URL,
            timeoutSeconds = 30,
            environment = AppEnvironment.DEVELOPMENT,
        )

        val local = ApiConfiguration(
            baseUrl = LOCAL_URL,
            timeoutSeconds = 8,
            environment = AppEnvironment.DEVELOPMENT,
        )

        fun normalizedBaseUrl(raw: String): String {
            var url = raw.trim().trimEnd('/')
            if (url.endsWith("/api")) {
                url = url.removeSuffix("/api")
            }
            return url
        }
    }
}

interface ApiConfigProvider {
    fun current(): ApiConfiguration
    fun defaultInstituteSlug(): String
}
