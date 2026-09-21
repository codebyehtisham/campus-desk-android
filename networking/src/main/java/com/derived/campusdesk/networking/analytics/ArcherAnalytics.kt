package com.derived.campusdesk.networking.analytics

import co.archer.sdk.Archer

/**
 * App-wide Archer event helpers. Prefer these over raw [Archer.analytics]
 * so events stay consistent (naming + current screen attribution).
 * Mirrors Traject `ArcherAnalytics` / iOS CampusAnalytics.
 */
object ArcherAnalytics {
    /** Current top screen id from the screen stack (last segment). */
    val currentScreen: String
        get() {
            val path = ArcherScreenAnalytics.currentScreenName
            if (path == "App") return "unknown"
            return path.substringAfterLast(" › ")
        }

    /** Generic event. Auto-attaches `screen` when not provided. */
    fun track(name: String, properties: Map<String, String> = emptyMap()) {
        val props = properties.toMutableMap()
        if (props["screen"] == null) {
            props["screen"] = currentScreen
        }
        Archer.analytics(name, props)
    }

    fun button(id: String, properties: Map<String, String> = emptyMap()) {
        val props = properties.toMutableMap()
        props["id"] = id
        track("button_tapped", props)
    }

    fun flow(name: String, step: String, properties: Map<String, String> = emptyMap()) {
        val props = properties.toMutableMap()
        props["flow"] = name
        props["step"] = step
        track("flow_step", props)
    }

    fun action(name: String, properties: Map<String, String> = emptyMap()) {
        track(name, properties)
    }

    fun permission(name: String, action: String, properties: Map<String, String> = emptyMap()) {
        val props = properties.toMutableMap()
        props["name"] = name
        props["action"] = action
        track("permission_event", props)
    }

    fun popup(name: String, action: String = "shown", properties: Map<String, String> = emptyMap()) {
        val props = properties.toMutableMap()
        props["name"] = name
        props["action"] = action
        track("popup_event", props)
    }

    fun api(
        method: String,
        path: String,
        statusCode: Int?,
        durationMs: Int,
        outcome: String,
    ) {
        track(
            "api_call",
            mapOf(
                "method" to method.uppercase(),
                "path" to sanitizeAPIPath(path),
                "status" to (statusCode?.toString().orEmpty()),
                "duration_ms" to durationMs.toString(),
                "outcome" to outcome,
            ),
        )
    }

    fun settingChanged(key: String, value: String) {
        track("setting_changed", mapOf("key" to key, "value" to value))
    }

    fun toggle(id: String, enabled: Boolean) {
        button(id, mapOf("enabled" to if (enabled) "true" else "false"))
    }

    fun identify(
        userId: String,
        email: String? = null,
        role: String? = null,
        organization: String? = null,
    ) {
        val props = mutableMapOf("user_id" to userId)
        email?.let { props["email"] = it }
        role?.let { props["role"] = it }
        organization?.let { props["organization"] = it }
        track("user_identified", props)
    }

    fun clearUser() {
        track("user_signed_out")
    }

    /** Drop query strings and collapse UUIDs / numeric ids for cardinality control. */
    fun sanitizeAPIPath(raw: String): String {
        var path = raw
        val q = path.indexOf('?')
        if (q >= 0) path = path.substring(0, q)
        if (path.startsWith("http://") || path.startsWith("https://")) {
            path = runCatching { java.net.URI(path).path }.getOrDefault(path)
        }
        val uuid =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        val scrubbed = path
            .split('/')
            .filter { it.isNotEmpty() }
            .joinToString("/") { segment ->
                when {
                    uuid.matches(segment) -> ":id"
                    segment.all { it.isDigit() } -> ":id"
                    else -> segment
                }
            }
        return "/$scrubbed".replace("//", "/")
    }
}
