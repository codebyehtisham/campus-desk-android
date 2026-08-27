package com.derived.campusdesk.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derived.campusdesk.networking.client.NetworkError
import com.derived.campusdesk.networking.models.StudentApplication
import com.derived.campusdesk.networking.services.CampusService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val campusService: CampusService,
) : ViewModel() {
    private val _application = MutableStateFlow<StudentApplication?>(null)
    val application: StateFlow<StudentApplication?> = _application.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _application.value = campusService.myApplication()
            } catch (e: NetworkError.Client) {
                if (e.status != 404) _errorMessage.value = e.message
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}
