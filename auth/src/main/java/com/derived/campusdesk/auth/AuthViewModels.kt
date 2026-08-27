package com.derived.campusdesk.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derived.campusdesk.networking.client.NetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sessionStore: SessionStore,
) : ViewModel() {
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val canSubmit: Boolean
        get() = _email.value.contains("@") && _password.value.length >= 6

    fun onEmailChange(value: String) { _email.value = value }
    fun onPasswordChange(value: String) { _password.value = value }

    fun reset() {
        _email.value = ""
        _password.value = ""
        _isLoading.value = false
        _errorMessage.value = null
    }

    fun submit() {
        if (!canSubmit || _isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                sessionStore.login(_email.value.trim(), _password.value)
                // Drop credentials from memory once sign-in succeeds.
                _password.value = ""
            } catch (e: NetworkError) {
                _errorMessage.value = e.message
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Sign in failed."
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val sessionStore: SessionStore,
) : ViewModel() {
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _didSend = MutableStateFlow(false)
    val didSend: StateFlow<Boolean> = _didSend.asStateFlow()

    fun onEmailChange(value: String) { _email.value = value }

    fun reset() {
        _email.value = ""
        _isLoading.value = false
        _didSend.value = false
    }

    fun submit() {
        if (_email.value.isBlank() || _isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            sessionStore.requestPasswordReset(_email.value.trim())
            _didSend.value = true
            _isLoading.value = false
        }
    }
}
