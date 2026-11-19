package com.soumit.technovision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.repository.AuthRepository
import com.soumit.technovision.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val startDestination: Any? = null,
    val isLoadingRole: Boolean = true,
    val isOnline: Boolean = true
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val networkMonitor: com.soumit.technovision.util.NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        determineStartDestination()
        observeNetwork()
    }

    private fun observeNetwork() {
        if (networkMonitor == null) return
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }

    private fun determineStartDestination() {
        val currentUser = authRepository.currentUser
        if (currentUser == null) {
            _uiState.update { it.copy(startDestination = Screen.Auth, isLoadingRole = false) }
            return
        }

        viewModelScope.launch {
            val roleResult = authRepository.getUserRole(currentUser.uid)
            val destination: Any = if (roleResult.isSuccess) {
                when (roleResult.getOrThrow().lowercase()) {
                    "owner" -> Screen.OwnerHome
                    "faculty" -> Screen.FacultyHome
                    "receptionist" -> Screen.ReceptionistHome
                    else -> Screen.Auth
                }
            } else {
                Screen.Auth
            }
            _uiState.update { it.copy(startDestination = destination, isLoadingRole = false) }
        }
    }
}
