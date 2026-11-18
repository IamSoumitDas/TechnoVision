package com.soumit.technovision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.repository.AuthRepository
import com.soumit.technovision.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val startDestination: Any? = null,
    val isLoadingRole: Boolean = true
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        determineStartDestination()
    }

    private fun determineStartDestination() {
        val currentUser = authRepository.currentUser
        if (currentUser == null) {
            _uiState.value = MainUiState(startDestination = Screen.Auth, isLoadingRole = false)
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
            _uiState.value = MainUiState(startDestination = destination, isLoadingRole = false)
        }
    }
}
