package com.derived.campusdesk.networking.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.derived.campusdesk.networking.models.CampusLocation
import com.derived.campusdesk.networking.models.Organization
import com.derived.campusdesk.networking.models.User
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SecureTokenStore(context: Context) : TokenStore {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val prefs: SharedPreferences = createPrefs(context)

    override fun token(): String? =
        prefs.getString(KEY_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() }

    override fun save(token: String) {
        require(token.isNotBlank()) { "Refusing to persist a blank auth token." }
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).commit()
    }

    override fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_SESSION)
            .commit()
    }

    override fun saveSession(
        user: User,
        organization: Organization?,
        attendanceLocationEnabled: Boolean,
        campusLocation: CampusLocation?,
    ) {
        val payload = SessionPayload(
            user = user,
            organization = organization,
            attendanceLocationEnabled = attendanceLocationEnabled,
            campusLocation = campusLocation,
        )
        prefs.edit().putString(KEY_SESSION, json.encodeToString(payload)).commit()
    }

    override fun loadSession(): PersistedSession? {
        val raw = prefs.getString(KEY_SESSION, null)?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            val payload = json.decodeFromString<SessionPayload>(raw)
            PersistedSession(
                user = payload.user,
                organization = payload.organization,
                attendanceLocationEnabled = payload.attendanceLocationEnabled,
                campusLocation = payload.campusLocation,
            )
        }.getOrNull()
    }

    @Serializable
    private data class SessionPayload(
        val user: User,
        val organization: Organization? = null,
        val attendanceLocationEnabled: Boolean = false,
        val campusLocation: CampusLocation? = null,
    )

    companion object {
        private const val PREFS_NAME = "com.derived.campusdesk.auth"
        private const val FALLBACK_PREFS = "com.derived.campusdesk.auth.fallback"
        private const val KEY_ACCESS_TOKEN = "accessToken"
        private const val KEY_SESSION = "sessionSnapshot"

        private fun createPrefs(context: Context): SharedPreferences {
            return try {
                EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            } catch (_: Exception) {
                // Emulator / corrupted master-key cases — keep the session usable.
                context.getSharedPreferences(FALLBACK_PREFS, Context.MODE_PRIVATE)
            }
        }
    }
}
