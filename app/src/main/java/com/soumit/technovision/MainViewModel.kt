package com.soumit.technovision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.repository.AuthRepository
import com.soumit.technovision.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val startDestination: String? = null,
    val isLoadingRole: Boolean = true
)

class MainViewModel(
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
            _uiState.value = MainUiState(startDestination = Routes.AUTH, isLoadingRole = false)
            return
        }

        viewModelScope.launch {
            val roleResult = authRepository.getUserRole(currentUser.uid)
            val destination = if (roleResult.isSuccess) {
                when (roleResult.getOrThrow().lowercase()) {
                    "owner" -> Routes.OWNER_HOME
                    "faculty" -> Routes.FACULTY_HOME
                    "receptionist" -> Routes.RECEPTIONIST_HOME
                    else -> Routes.AUTH
                }
            } else {
                Routes.AUTH
            }
            _uiState.value = MainUiState(startDestination = destination, isLoadingRole = false)
        }
    }
}
