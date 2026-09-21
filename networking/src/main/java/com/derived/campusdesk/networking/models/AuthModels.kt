package com.derived.campusdesk.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class User(
    val id: FlexibleId,
    val name: String? = null,
    val email: String? = null,
    val role: String? = null,
    val title: String? = null,
    val phone: String? = null,
    @SerialName("avatarUrl") val avatarUrl: String? = null,
    val avatar: String? = null,
    val photo: String? = null,
    val image: String? = null,
    val blocked: Boolean? = null,
) {
    val displayName: String get() = name?.trim().orEmpty().ifBlank { email.orEmpty() }
    val resolvedAvatarUrl: String? get() = avatarUrl ?: avatar ?: photo ?: image
    val firstName: String
        get() = displayName.split(" ").firstOrNull().orEmpty()
}

@Serializable
data class Organization(
    val id: FlexibleId? = null,
    val name: String? = null,
    val title: String? = null,
    val slug: String? = null,
    val kind: String? = null,
    val modules: List<String>? = null,
    val status: String? = null,
)

@Serializable
data class AuthCredentials(
    val email: String,
    val password: String,
    val institute: String = "explore",
    val client: String = "mobile",
)

@Serializable
data class RegisterPayload(
    val name: String,
    val email: String,
    val password: String,
)

@Serializable
data class ForgotPasswordPayload(
    val email: String,
)

@Serializable
data class AuthResponse(
    val token: String? = null,
    @SerialName("accessToken") val accessToken: String? = null,
    val user: User? = null,
    val organization: Organization? = null,
    @SerialName("org") val org: Organization? = null,
    val attendanceLocationEnabled: Boolean? = null,
    val campusLocation: CampusLocation? = null,
) {
    val resolvedToken: String get() = token ?: accessToken.orEmpty()
    val resolvedOrganization: Organization? get() = organization ?: org
}

@Serializable
data class MeResponse(
    val user: User? = null,
    val organization: Organization? = null,
    val attendanceLocationEnabled: Boolean? = null,
    val campusLocation: CampusLocation? = null,
) {
    companion object {
        fun decode(json: Json, root: JsonObject): MeResponse {
            if (root.containsKey("email") || root.containsKey("id")) {
                val user = json.decodeFromJsonElement<User>(root)
                return MeResponse(user = user)
            }
            return json.decodeFromJsonElement<MeResponse>(root)
        }
    }
}
