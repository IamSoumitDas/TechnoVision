package com.soumit.technovision.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PortalDashboardUiState(
    val userName: String = "User",
    val isLoading: Boolean = true
)

class PortalDashboardViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortalDashboardUiState())
    val uiState: StateFlow<PortalDashboardUiState> = _uiState.asStateFlow()

    init {
        loadUserName()
    }

    private fun loadUserName() {
        viewModelScope.launch {
            val result = authRepository.getCurrentUserName()
            val name = result.getOrDefault("User")
            _uiState.update { it.copy(userName = name, isLoading = false) }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
