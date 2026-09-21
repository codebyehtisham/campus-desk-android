package com.derived.campusdesk.courses.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derived.campusdesk.networking.models.StudentNotification
import com.derived.campusdesk.networking.services.StudentService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val studentService: StudentService,
) : ViewModel() {
    private val _notifications = MutableStateFlow<List<StudentNotification>>(emptyList())
    val notifications: StateFlow<List<StudentNotification>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _notifications.value = studentService.notifications()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not load notifications."
            } finally {
                _isLoading.value = false
            }
        }
    }
}
