package com.derived.campusdesk.auth

import com.derived.campusdesk.networking.client.NetworkError
import com.derived.campusdesk.networking.models.AuthResponse
import com.derived.campusdesk.networking.models.CampusLocation
import com.derived.campusdesk.networking.models.Organization
import com.derived.campusdesk.networking.models.User
import com.derived.campusdesk.networking.services.AuthService
import com.derived.campusdesk.networking.storage.PersistedSession
import com.derived.campusdesk.networking.storage.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout

class SessionStore(
    private val authService: AuthService,
    private val tokenStore: TokenStore,
) {
    private val _phase = MutableStateFlow<SessionPhase>(
        if (tokenStore.token() == null) SessionPhase.SignedOut else SessionPhase.Launching,
    )
    val phase: StateFlow<SessionPhase> = _phase.asStateFlow()

    val isAuthenticated: Boolean get() = _phase.value is SessionPhase.SignedIn

    /**
     * Cold start: prefer the locally persisted user snapshot.
     * Skips `/me` when a cached session exists so the dashboard opens offline-fast.
     * Only hits the network when a token exists without a local snapshot (upgrade path).
     */
    suspend fun restore() {
        if (tokenStore.token().isNullOrBlank()) {
            tokenStore.clear()
            _phase.value = SessionPhase.SignedOut
            return
        }

        val cached = tokenStore.loadSession()
        if (cached != null) {
            enterSignedIn(cached, persist = false)
            return
        }

        _phase.value = SessionPhase.Launching
        try {
            val me = withTimeout(8_000) { authService.me() }
            val user = me.user
            if (user == null) {
                tokenStore.clear()
                _phase.value = SessionPhase.SignedOut
                return
            }
            enterSignedIn(
                user = user,
                organization = me.organization,
                attendanceLocationEnabled = me.attendanceLocationEnabled == true,
                campusLocation = me.campusLocation,
            )
        } catch (e: NetworkError.Unauthorized) {
            tokenStore.clear()
            _phase.value = SessionPhase.SignedOut
        } catch (_: Exception) {
            tokenStore.clear()
            _phase.value = SessionPhase.SignedOut
        }
    }

    suspend fun login(email: String, password: String) {
        val response = authService.login(email, password)
        complete(response)
    }

    suspend fun requestPasswordReset(email: String) {
        authService.requestPasswordReset(email)
    }

    fun logout() {
        tokenStore.clear()
        _phase.value = SessionPhase.SignedOut
    }

    private suspend fun complete(response: AuthResponse) {
        val token = response.resolvedToken
        if (token.isBlank()) {
            throw NetworkError.Decoding("Login succeeded but no auth token was returned.")
        }
        tokenStore.save(token)

        val fenceLocation = response.campusLocation
        val attendanceEnabled = response.attendanceLocationEnabled == true

        response.user?.let { user ->
            enterSignedIn(
                user = user,
                organization = response.resolvedOrganization,
                attendanceLocationEnabled = attendanceEnabled,
                campusLocation = fenceLocation,
            )
            return
        }

        val me = authService.me()
        val user = me.user ?: throw NetworkError.Decoding("Missing user in auth response.")
        enterSignedIn(
            user = user,
            organization = me.organization ?: response.resolvedOrganization,
            attendanceLocationEnabled = me.attendanceLocationEnabled ?: attendanceEnabled,
            campusLocation = me.campusLocation ?: fenceLocation,
        )
    }

    private fun enterSignedIn(session: PersistedSession, persist: Boolean = true) {
        enterSignedIn(
            user = session.user,
            organization = session.organization,
            attendanceLocationEnabled = session.attendanceLocationEnabled,
            campusLocation = session.campusLocation,
            persist = persist,
        )
    }

    private fun enterSignedIn(
        user: User,
        organization: Organization?,
        attendanceLocationEnabled: Boolean,
        campusLocation: CampusLocation?,
        persist: Boolean = true,
    ) {
        if (persist) {
            tokenStore.saveSession(
                user = user,
                organization = organization,
                attendanceLocationEnabled = attendanceLocationEnabled,
                campusLocation = campusLocation,
            )
        }
        _phase.value = SessionPhase.SignedIn(
            user = user,
            organization = organization,
            attendanceLocationEnabled = attendanceLocationEnabled,
            campusFence = campusLocation?.toFence(),
        )
    }
}
