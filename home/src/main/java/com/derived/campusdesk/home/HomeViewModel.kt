package com.derived.campusdesk.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derived.campusdesk.networking.client.NetworkError
import com.derived.campusdesk.networking.models.StudentDashboard
import com.derived.campusdesk.networking.models.StudentNotification
import com.derived.campusdesk.networking.services.StudentService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val studentService: StudentService,
) : ViewModel() {
    private val _dashboard = MutableStateFlow<StudentDashboard?>(null)
    val dashboard: StateFlow<StudentDashboard?> = _dashboard.asStateFlow()

    private val _notifications = MutableStateFlow<List<StudentNotification>>(emptyList())
    val notifications: StateFlow<List<StudentNotification>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isNotEnrolled = MutableStateFlow(false)
    val isNotEnrolled: StateFlow<Boolean> = _isNotEnrolled.asStateFlow()

    val unreadCount: Int
        get() = _notifications.value.count { it.read != true }

    fun load() {
        viewModelScope.launch {
            val firstLoad = _dashboard.value == null
            _isLoading.value = firstLoad
            _errorMessage.value = null
            _isNotEnrolled.value = false
            try {
                val dashboardDeferred = async { runCatching { studentService.dashboard() } }
                val notificationsDeferred = async { runCatching { studentService.notifications() } }
                val dashboardResult = dashboardDeferred.await()
                val notificationsResult = notificationsDeferred.await()

                dashboardResult.onSuccess { _dashboard.value = it }
                notificationsResult.onSuccess { _notifications.value = it }

                if (_dashboard.value == null) {
                    val failure = dashboardResult.exceptionOrNull()
                        ?: notificationsResult.exceptionOrNull()
                    handleError(failure)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun handleError(error: Throwable?) {
        when {
            error is NetworkError && error.isNotEnrolled -> {
                _isNotEnrolled.value = true
                _errorMessage.value = null
            }
            error?.message?.contains("enroll", ignoreCase = true) == true -> {
                _isNotEnrolled.value = true
                _errorMessage.value = null
            }
            else -> _errorMessage.value = error?.message ?: "Could not load dashboard."
        }
    }
}
