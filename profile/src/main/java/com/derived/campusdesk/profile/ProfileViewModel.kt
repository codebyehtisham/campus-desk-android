package com.derived.campusdesk.profile

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import co.archer.sdk.Archer
import com.derived.campusdesk.networking.analytics.ArcherAnalytics
import com.derived.campusdesk.networking.client.NetworkError
import com.derived.campusdesk.networking.models.StudentApplication
import com.derived.campusdesk.networking.services.CampusService
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    application: Application,
    private val campusService: CampusService,
) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(PREFS, Application.MODE_PRIVATE)

    private val _application = MutableStateFlow<StudentApplication?>(null)
    val application: StateFlow<StudentApplication?> = _application.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _pushEnabled = MutableStateFlow(readPushEnabled())
    val pushEnabled: StateFlow<Boolean> = _pushEnabled.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _isLoading.value = _application.value == null
            _errorMessage.value = null
            try {
                _application.value = campusService.myApplication()
            } catch (e: NetworkError.Client) {
                if (e.status == 404) {
                    _application.value = null
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = e.message
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
        refreshPushState()
    }

    fun refreshPushState() {
        val want = prefs.getBoolean(KEY_PUSH, true)
        val granted = notificationsGranted()
        _pushEnabled.value = want && granted
    }

    fun setPushEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PUSH, enabled).apply()
        _pushEnabled.value = enabled && notificationsGranted()
        ArcherAnalytics.toggle("push_notifications", enabled)
        ArcherAnalytics.settingChanged("push_notifications", if (enabled) "on" else "off")

        if (enabled) {
            Archer.activatePush(requestAuthorization = false)
            registerFcmToken()
        } else {
            runCatching {
                if (FirebaseApp.getApps(getApplication()).isNotEmpty()) {
                    FirebaseMessaging.getInstance().deleteToken()
                }
            }
        }
    }

    /** Call after the UI obtains POST_NOTIFICATIONS. */
    fun onNotificationPermissionResult(granted: Boolean) {
        ArcherAnalytics.permission(
            name = "post_notifications",
            action = if (granted) "granted" else "denied",
        )
        if (granted) {
            prefs.edit().putBoolean(KEY_PUSH, true).apply()
            _pushEnabled.value = true
            Archer.activatePush(requestAuthorization = false)
            registerFcmToken()
        } else {
            _pushEnabled.value = false
        }
    }

    fun needsNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        return ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
    }

    private fun registerFcmToken() {
        runCatching {
            val app = getApplication<Application>()
            if (FirebaseApp.getApps(app).isEmpty()) return
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (!token.isNullOrBlank()) Archer.setPushToken(token)
            }
        }
    }

    private fun notificationsGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            return ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
        return NotificationManagerCompat.from(getApplication()).areNotificationsEnabled()
    }

    private fun readPushEnabled(): Boolean {
        val want = prefs.getBoolean(KEY_PUSH, true)
        return want && notificationsGranted()
    }

    companion object {
        private const val PREFS = "campusdesk.push"
        private const val KEY_PUSH = "enabled"
    }
}
