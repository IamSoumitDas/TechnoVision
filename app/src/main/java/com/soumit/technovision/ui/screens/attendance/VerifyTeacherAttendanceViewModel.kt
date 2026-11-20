package com.soumit.technovision.ui.screens.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel
import com.soumit.technovision.data.repository.AttendanceRepository
import com.soumit.technovision.data.repository.AuditLogRepository
import com.soumit.technovision.data.repository.AuthRepository
import com.soumit.technovision.data.repository.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerifyTeacherAttendanceUiState(
    val requests: List<TeacherAttendanceRequestModel> = emptyList(),
    val userMap: Map<String, String> = emptyMap(),
    val currentAdminName: String = "Admin",
    val isLoading: Boolean = true,
    val processingRequestId: String? = null,
    val isOnline: Boolean = true,
    val userMessage: String? = null
) {
    val pendingRequests: List<TeacherAttendanceRequestModel>
        get() = requests.filter { it.status.equals("Pending", ignoreCase = true) }

    val resolvedRequests: List<TeacherAttendanceRequestModel>
        get() = requests.filter { !it.status.equals("Pending", ignoreCase = true) }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class VerifyTeacherAttendanceViewModel @javax.inject.Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val staffRepository: StaffRepository,
    private val authRepository: AuthRepository,
    private val auditLogRepository: AuditLogRepository,
    private val networkMonitor: com.soumit.technovision.util.NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerifyTeacherAttendanceUiState())
    val uiState: StateFlow<VerifyTeacherAttendanceUiState> = _uiState.asStateFlow()

    init {
        loadAdminName()
        observeData()
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

    private fun loadAdminName() {
        viewModelScope.launch {
            val user = authRepository.currentUser
            if (user != null) {
                val userMap = staffRepository.getUsersMap().getOrDefault(emptyMap())
                val resolved = userMap[user.uid]
                    ?: userMap[user.email]
                    ?: user.displayName?.takeIf { it.isNotBlank() }
                    ?: user.email?.substringBefore("@")
                    ?: "Admin"
                _uiState.update { it.copy(currentAdminName = resolved) }
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                attendanceRepository.getTeacherAttendanceRequestsStream(),
                staffRepository.getUsersMapStream()
            ) { reqs, users ->
                Pair(reqs, users)
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, userMessage = "Error: ${e.localizedMessage}") }
            }.collect { (reqs, users) ->
                val user = authRepository.currentUser
                val currentAdmin = _uiState.value.currentAdminName
                val resolvedAdmin = if (currentAdmin == "Admin" && user != null) {
                    users[user.uid] ?: users[user.email] ?: currentAdmin
                } else currentAdmin
                _uiState.update {
                    it.copy(
                        requests = reqs,
                        userMap = users,
                        currentAdminName = resolvedAdmin,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun verifyRequest(request: TeacherAttendanceRequestModel) {
        if (!_uiState.value.isOnline) {
            _uiState.update { it.copy(userMessage = "Verifying attendance requires an active internet connection.") }
            return
        }
        val adminName = _uiState.value.currentAdminName
        _uiState.update { it.copy(processingRequestId = request.id) }

        viewModelScope.launch {
            val result = attendanceRepository.verifyTeacherAttendance(request.id, adminName)
            if (result.isSuccess) {
                auditLogRepository.log(
                    action = "Attendance Verified",
                    details = "Approved time slot [${request.timeSlot}] for teacher ${request.teacherName} on date ${request.date} (Verified by $adminName)"
                )
                _uiState.update { state ->
                    val updatedList = state.requests.map {
                        if (it.id == request.id) it.copy(status = "Verified", verifiedBy = adminName) else it
                    }
                    state.copy(
                        requests = updatedList,
                        processingRequestId = null,
                        userMessage = "Verified attendance for ${request.teacherName}"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        processingRequestId = null,
                        userMessage = "Verification failed: ${result.exceptionOrNull()?.localizedMessage}"
                    )
                }
            }
        }
    }

    fun rejectRequest(request: TeacherAttendanceRequestModel) {
        if (!_uiState.value.isOnline) {
            _uiState.update { it.copy(userMessage = "Rejecting attendance requires an active internet connection.") }
            return
        }
        val adminName = _uiState.value.currentAdminName
        _uiState.update { it.copy(processingRequestId = request.id) }

        viewModelScope.launch {
            val result = attendanceRepository.rejectTeacherAttendance(request.id, adminName)
            if (result.isSuccess) {
                auditLogRepository.log(
                    action = "Attendance Rejected",
                    details = "Rejected time slot [${request.timeSlot}] for teacher ${request.teacherName} on date ${request.date} (Rejected by $adminName)"
                )
                _uiState.update { state ->
                    val updatedList = state.requests.map {
                        if (it.id == request.id) it.copy(status = "Rejected", rejectedBy = adminName) else it
                    }
                    state.copy(
                        requests = updatedList,
                        processingRequestId = null,
                        userMessage = "Rejected request for ${request.teacherName}"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        processingRequestId = null,
                        userMessage = "Rejection failed: ${result.exceptionOrNull()?.localizedMessage}"
                    )
                }
            }
        }
    }
}
