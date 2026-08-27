package com.derived.campusdesk.networking.api

enum class ApiEnvironmentPreset(val label: String, val configuration: ApiConfiguration) {
    Production("Production", ApiConfiguration.production),
    Development("Development", ApiConfiguration.developmentPreset),
    ;

    val healthUrl: String
        get() = ApiConfiguration.normalizedBaseUrl(configuration.baseUrl) + "/api/health"

    val hostLabel: String
        get() = configuration.baseUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
}

interface ApiEnvironmentStore {
    val hasSavedSelection: Boolean
    var selected: ApiEnvironmentPreset
    fun clearSavedSelection()
}
