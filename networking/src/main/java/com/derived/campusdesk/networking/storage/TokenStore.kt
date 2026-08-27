package com.derived.campusdesk.networking.storage

import com.derived.campusdesk.networking.models.CampusLocation
import com.derived.campusdesk.networking.models.Organization
import com.derived.campusdesk.networking.models.User

interface TokenStore {
    fun token(): String?
    fun save(token: String)
    fun clear()

    fun saveSession(
        user: User,
        organization: Organization?,
        attendanceLocationEnabled: Boolean,
        campusLocation: CampusLocation?,
    )

    fun loadSession(): PersistedSession?
}

data class PersistedSession(
    val user: User,
    val organization: Organization?,
    val attendanceLocationEnabled: Boolean,
    val campusLocation: CampusLocation?,
)
