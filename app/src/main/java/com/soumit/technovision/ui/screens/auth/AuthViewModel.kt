package com.soumit.technovision.ui.screens.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun login(onSuccess: (String) -> Unit) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update {
                it.copy(
                    statusMessage = "Please fill in all fields.",
                    isError = true
                )
            }
            return
        }

        if (!isEmailValid(state.email)) {
            _uiState.update {
                it.copy(
                    statusMessage = "Please enter a valid email structure (e.g. name@example.com).",
                    isError = true
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, statusMessage = null, isError = false) }

        viewModelScope.launch {
            val result = authRepository.signIn(state.email.trim(), state.password)
            if (result.isSuccess) {
                val role = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = "Welcome! Logged in successfully.",
                        isError = false
                    )
                }
                onSuccess(role)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = result.exceptionOrNull()?.localizedMessage ?: "Login failed",
                        isError = true
                    )
                }
            }
        }
    }

    fun forgotPassword() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.update {
                it.copy(
                    statusMessage = "Please enter your registered email address first.",
                    isError = true
                )
            }
            return
        }

        if (!isEmailValid(email)) {
            _uiState.update {
                it.copy(
                    statusMessage = "Please enter a valid email address.",
                    isError = true
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, statusMessage = null) }

        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(email)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = "Password reset instructions sent to $email.",
                        isError = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = "Reset Error: ${result.exceptionOrNull()?.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }

    private fun isEmailValid(email: String): Boolean {
        val clean = email.trim()
        if (clean.isBlank()) return false
        val fallbackRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return try {
            Patterns.EMAIL_ADDRESS?.matcher(clean)?.matches() ?: fallbackRegex.matches(clean)
        } catch (_: Throwable) {
            fallbackRegex.matches(clean)
        }
    }
}
