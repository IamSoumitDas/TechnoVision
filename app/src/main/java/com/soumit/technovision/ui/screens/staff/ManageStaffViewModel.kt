package com.soumit.technovision.ui.screens.staff

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.model.StaffModel
import com.soumit.technovision.data.repository.AuditLogRepository
import com.soumit.technovision.data.repository.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ManageStaffUiState(
    val staffList: List<StaffModel> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
    val userMessage: String? = null,
    val errorMessage: String? = null,
    val isOnline: Boolean = true
)

@dagger.hilt.android.lifecycle.HiltViewModel
class ManageStaffViewModel @javax.inject.Inject constructor(
    private val staffRepository: StaffRepository,
    private val auditLogRepository: AuditLogRepository,
    private val networkMonitor: com.soumit.technovision.util.NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageStaffUiState())
    val uiState: StateFlow<ManageStaffUiState> = _uiState.asStateFlow()

    init {
        observeStaff()
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

    private fun observeStaff() {
        viewModelScope.launch {
            staffRepository.getStaffStream()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Error loading staff: ${e.localizedMessage}") }
                }
                .collect { list ->
                    _uiState.update { it.copy(staffList = list, isLoading = false) }
                }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(errorMessage = null, userMessage = null) }

    fun createStaff(
        fullName: String,
        email: String,
        password: String,
        phone: String,
        role: String,
        onSuccess: () -> Unit
    ) {
        if (!_uiState.value.isOnline) {
            _uiState.update { it.copy(errorMessage = "Connect to the internet to manage staff accounts") }
            return
        }

        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Fill in all required fields") }
            return
        }

        if (!isEmailValid(email)) {
            _uiState.update { it.copy(errorMessage = "Enter a valid email address") }
            return
        }

        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val result = staffRepository.createStaff(
                fullName = fullName.trim(),
                email = email.trim(),
                password = password,
                phone = phone.trim(),
                role = role.trim()
            )

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            userMessage = "Staff account created"
                        )
                    }
                    auditLogRepository.log(
                        action = "Staff Added",
                        details = "Registered new $role: ${fullName.trim()} (${email.trim()})"
                    )
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.localizedMessage ?: "Failed to create staff account"
                        )
                    }
                }
            )
        }
    }

    fun updateStaff(
        id: String,
        fullName: String,
        phone: String,
        role: String,
        onSuccess: () -> Unit
    ) {
        if (!_uiState.value.isOnline) {
            _uiState.update { it.copy(errorMessage = "Connect to the internet to manage staff accounts") }
            return
        }

        if (fullName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter a full name") }
            return
        }

        _uiState.update { it.copy(isUpdating = true, errorMessage = null) }

        viewModelScope.launch {
            val result = staffRepository.updateStaff(id, fullName.trim(), phone.trim(), role.trim())
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            userMessage = "Staff profile updated"
                        )
                    }
                    auditLogRepository.log(
                        action = "Staff Updated",
                        details = "Updated staff record: ${fullName.trim()} ($role)"
                    )
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            errorMessage = "Update failed: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun deleteStaff(staff: StaffModel, onSuccess: () -> Unit) {
        if (!_uiState.value.isOnline) {
            _uiState.update { it.copy(errorMessage = "Connect to the internet to manage staff accounts") }
            return
        }

        _uiState.update { it.copy(isDeleting = true, errorMessage = null) }

        viewModelScope.launch {
            val result = staffRepository.deleteStaff(staff.id)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            userMessage = "Staff account removed"
                        )
                    }
                    auditLogRepository.log(
                        action = "Staff Deleted",
                        details = "Deleted staff record: ${staff.fullName} (${staff.email})"
                    )
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = "Delete failed: ${error.localizedMessage}"
                        )
                    }
                }
            )
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
