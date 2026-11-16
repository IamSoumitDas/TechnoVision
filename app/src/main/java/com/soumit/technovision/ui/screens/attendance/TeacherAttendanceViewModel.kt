package com.soumit.technovision.ui.screens.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel
import com.soumit.technovision.data.repository.AttendanceRepository
import com.soumit.technovision.data.repository.AuditLogRepository
import com.soumit.technovision.data.repository.AuthRepository
import com.soumit.technovision.data.repository.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TeacherAttendanceUiState(
    val requests: List<TeacherAttendanceRequestModel> = emptyList(),
    val teacherName: String = "",
    val teacherId: String = "",
    val selectedDateStr: String = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date()),
    val selectedTimeSlot: String = "9:00 AM - 11:00 AM",
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val userMessage: String? = null
)

class TeacherAttendanceViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val authRepository: AuthRepository,
    private val staffRepository: StaffRepository,
    private val auditLogRepository: AuditLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherAttendanceUiState())
    val uiState: StateFlow<TeacherAttendanceUiState> = _uiState.asStateFlow()

    init {
        determineInitialSlot()
        observeTeacherAndRequests()
    }

    private fun determineInitialSlot() {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val initialSlot = when (currentHour) {
            in 9..10 -> "9:00 AM - 11:00 AM"
            in 11..12 -> "11:00 AM - 1:00 PM"
            in 13..14 -> "1:00 PM - 3:00 PM"
            in 15..16 -> "3:00 PM - 5:00 PM"
            in 17..18 -> "5:00 PM - 7:00 PM"
            in 19..20 -> "7:00 PM - 9:00 PM"
            in 21..23 -> "7:00 PM - 9:00 PM"
            else -> "9:00 AM - 11:00 AM"
        }
        _uiState.update { it.copy(selectedTimeSlot = initialSlot) }
    }

    private fun observeTeacherAndRequests() {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser
            val currentUid = currentUser?.uid ?: ""
            val currentEmail = currentUser?.email ?: ""
            val defaultName = currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: currentEmail.substringBefore("@").takeIf { it.isNotBlank() }
                ?: "Teacher"

            kotlinx.coroutines.flow.combine(
                attendanceRepository.getTeacherAttendanceRequestsStream(teacherId = currentUid.ifBlank { null }),
                staffRepository.getUsersMapStream()
            ) { allRequests, userMap ->
                val resolvedName = userMap[currentUid]
                    ?: userMap[currentEmail]
                    ?: defaultName

                val teacherRequests = if (currentUid.isNotBlank()) {
                    allRequests.filter { it.teacherId == currentUid || it.teacherName.equals(resolvedName, ignoreCase = true) }
                } else {
                    emptyList()
                }
                Triple(currentUid, resolvedName, teacherRequests)
            }.catch { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "Error loading requests: ${e.localizedMessage}"
                    )
                }
            }.collect { (uid, name, requests) ->
                _uiState.update {
                    it.copy(
                        teacherId = uid,
                        teacherName = name,
                        requests = requests,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onDateSelected(dateStr: String) {
        _uiState.update { it.copy(selectedDateStr = dateStr) }
    }

    fun onTimeSlotSelected(slot: String) {
        _uiState.update { it.copy(selectedTimeSlot = slot) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun submitRequest(onSuccess: () -> Unit) {
        val state = _uiState.value
        val alreadyRequested = state.requests.any {
            it.date == state.selectedDateStr &&
                    it.timeSlot == state.selectedTimeSlot &&
                    it.status != "Rejected"
        }

        if (alreadyRequested) {
            _uiState.update {
                it.copy(userMessage = "You already have a request submitted for this time slot.")
            }
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val currentUser = authRepository.currentUser
            val teacherId = state.teacherId.ifBlank { currentUser?.uid ?: "" }
            val teacherName = state.teacherName.ifBlank {
                currentUser?.displayName?.takeIf { it.isNotBlank() }
                    ?: currentUser?.email?.substringBefore("@")
                    ?: "Teacher"
            }

            val req = TeacherAttendanceRequestModel(
                teacherId = teacherId,
                teacherName = teacherName,
                date = state.selectedDateStr,
                timeSlot = state.selectedTimeSlot,
                status = "Pending",
                timestamp = Timestamp.now()
            )

            val result = attendanceRepository.submitTeacherAttendanceRequest(req)
            if (result.isSuccess) {
                auditLogRepository.log(
                    action = "Teacher Attendance Requested",
                    details = "Submitted verification request for [${state.selectedTimeSlot}] on ${state.selectedDateStr} (Teacher: $teacherName)"
                )
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        userMessage = "Attendance verification requested successfully!"
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        userMessage = "Submission failed: ${result.exceptionOrNull()?.localizedMessage}"
                    )
                }
            }
        }
    }
}
