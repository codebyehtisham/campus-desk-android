package com.derived.campusdesk.auth

import com.derived.campusdesk.networking.client.NetworkError
import com.derived.campusdesk.networking.client.SessionEvents
import com.derived.campusdesk.networking.analytics.ArcherAnalytics
import com.derived.campusdesk.networking.models.AuthResponse
import com.derived.campusdesk.networking.models.CampusLocation
import com.derived.campusdesk.networking.models.Organization
import com.derived.campusdesk.networking.models.User
import com.derived.campusdesk.networking.services.AuthService
import com.derived.campusdesk.networking.storage.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class SessionStore(
    private val authService: AuthService,
    private val tokenStore: TokenStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _phase = MutableStateFlow<SessionPhase>(
        if (tokenStore.token() == null) SessionPhase.SignedOut else SessionPhase.Launching,
    )
    val phase: StateFlow<SessionPhase> = _phase.asStateFlow()

    val isAuthenticated: Boolean get() = _phase.value is SessionPhase.SignedIn

    init {
        scope.launch {
            SessionEvents.unauthorized.collect {
                if (_phase.value is SessionPhase.SignedIn) {
                    logout()
                }
            }
        }
    }

    /**
     * Cold start: always verify with `/api/auth/me` when a token exists.
     * Never navigate from cached user data alone (iOS parity).
     */
    suspend fun restore() {
        if (tokenStore.token().isNullOrBlank()) {
            tokenStore.clear()
            _phase.value = SessionPhase.SignedOut
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
        ArcherAnalytics.clearUser()
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
        ArcherAnalytics.identify(
            userId = user.id.value,
            email = user.email,
            role = user.role,
            organization = organization?.name ?: organization?.slug,
        )
    }
}