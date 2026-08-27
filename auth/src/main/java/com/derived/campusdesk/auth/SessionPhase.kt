package com.derived.campusdesk.auth

import com.derived.campusdesk.networking.models.CampusFence
import com.derived.campusdesk.networking.models.Organization
import com.derived.campusdesk.networking.models.User

sealed class SessionPhase {
    data object Launching : SessionPhase()
    data object SignedOut : SessionPhase()
    data class SignedIn(
        val user: User,
        val organization: Organization?,
        val attendanceLocationEnabled: Boolean,
        val campusFence: CampusFence?,
    ) : SessionPhase()
}
