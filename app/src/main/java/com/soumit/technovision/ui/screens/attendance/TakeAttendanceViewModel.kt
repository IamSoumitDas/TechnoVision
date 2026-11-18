package com.soumit.technovision.ui.screens.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.repository.AttendanceRepository
import com.soumit.technovision.data.repository.AuditLogRepository
import com.soumit.technovision.data.repository.AuthRepository
import com.soumit.technovision.data.repository.StaffRepository
import com.soumit.technovision.data.repository.StudentRepository
import com.soumit.technovision.util.AttendanceUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExistingAttendanceInfo(
    val id: String,
    val submittedBy: String,
    val timeFormatted: String,
    val totalPresent: Int,
    val totalAbsent: Int,
    val additionalStudentIds: List<String> = emptyList()
)

data class TakeAttendanceUiState(
    val selectedTimeSlot: String = AttendanceUtils.getAutomaticTimeSlot(),
    val currentDate: String = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date()),
    val todayName: String = AttendanceUtils.getTodayDayName(),
    val allStudents: List<StudentModel> = emptyList(),
    val attendanceStates: Map<String, Boolean> = emptyMap(),
    val additionalStudents: List<StudentModel> = emptyList(),
    val existingAttendanceInfo: ExistingAttendanceInfo? = null,
    val isCheckingExistingAttendance: Boolean = false,
    val isLoadingStudents: Boolean = true,
    val isSaving: Boolean = false,
    val searchQuery: String = "",
    val isSearchExpanded: Boolean = false,
    val userMap: Map<String, String> = emptyMap(),
    val currentUserName: String = "Faculty",
    val userMessage: String? = null
) {
    val isAttendanceLocked: Boolean
        get() = existingAttendanceInfo != null

    val scheduledStudents: List<StudentModel>
        get() = allStudents.filter { student ->
            student.weeklySchedule[todayName] == selectedTimeSlot
        }

    val displayedStudents: List<StudentModel>
        get() = scheduledStudents + additionalStudents

    val presentCount: Int
        get() = displayedStudents.count { attendanceStates[it.id] == true }

    val absentCount: Int
        get() = scheduledStudents.count { attendanceStates[it.id] != true }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class TakeAttendanceViewModel @javax.inject.Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val studentRepository: StudentRepository,
    private val staffRepository: StaffRepository,
    private val authRepository: AuthRepository,
    private val auditLogRepository: AuditLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TakeAttendanceUiState())
    val uiState: StateFlow<TakeAttendanceUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
        loadStudents()
        checkExistingAttendance(_uiState.value.selectedTimeSlot)
    }

    private fun loadUsers() {
        viewModelScope.launch {
            staffRepository.getUsersMapStream().collect { map ->
                val currentUser = authRepository.currentUser
                val resolvedName = map[currentUser?.uid]
                    ?: map[currentUser?.email]
                    ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
                    ?: currentUser?.email?.substringBefore("@")
                    ?: "Faculty"

                _uiState.update {
                    it.copy(
                        userMap = map,
                        currentUserName = resolvedName
                    )
                }
            }
        }
    }

    private fun loadStudents() {
        viewModelScope.launch {
            studentRepository.getStudentsStream()
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingStudents = false,
                            userMessage = "Error loading students: ${e.localizedMessage}"
                        )
                    }
                }
                .collect { students ->
                    _uiState.update { state ->
                        val scheduled = students.filter { it.weeklySchedule[state.todayName] == state.selectedTimeSlot }
                        val currentStates = state.attendanceStates.toMutableMap()
                        scheduled.forEach { s ->
                            if (!currentStates.containsKey(s.id)) {
                                currentStates[s.id] = false
                            }
                        }
                        val extras = state.existingAttendanceInfo?.let { info ->
                            students.filter { it.id in info.additionalStudentIds }
                        } ?: state.additionalStudents

                        state.copy(
                            allStudents = students,
                            attendanceStates = currentStates,
                            additionalStudents = extras,
                            isLoadingStudents = false
                        )
                    }
                }
        }
    }

    fun onSelectTimeSlot(slot: String) {
        _uiState.update {
            it.copy(
                selectedTimeSlot = slot,
                additionalStudents = emptyList(),
                attendanceStates = emptyMap(),
                isSearchExpanded = false,
                searchQuery = ""
            )
        }
        checkExistingAttendance(slot)
    }

    private fun checkExistingAttendance(slot: String) {
        val state = _uiState.value
        _uiState.update { it.copy(isCheckingExistingAttendance = true) }

        viewModelScope.launch {
            val result = attendanceRepository.getExistingAttendance(state.currentDate, slot)
            if (result.isSuccess) {
                val record = result.getOrNull()
                if (record != null) {
                    val recordedBy = AttendanceUtils.resolveTeacherName(record, state.userMap)
                    val formattedTime = record.timestamp?.let {
                        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(it.toDate())
                    } ?: "Earlier today"

                    val newStates = mutableMapOf<String, Boolean>()
                    record.presentStudentIds.forEach { newStates[it] = true }
                    record.absentStudentIds.forEach { newStates[it] = false }
                    record.additionalStudentIds.forEach { id ->
                        if (!newStates.containsKey(id)) {
                            newStates[id] = false
                        }
                    }

                    val extras = state.allStudents.filter { it.id in record.additionalStudentIds }

                    _uiState.update {
                        it.copy(
                            existingAttendanceInfo = ExistingAttendanceInfo(
                                id = record.id,
                                submittedBy = recordedBy,
                                timeFormatted = formattedTime,
                                totalPresent = record.totalPresent,
                                totalAbsent = record.totalAbsent,
                                additionalStudentIds = record.additionalStudentIds
                            ),
                            attendanceStates = newStates,
                            additionalStudents = extras,
                            isCheckingExistingAttendance = false
                        )
                    }
                } else {
                    _uiState.update { current ->
                        val defaultStates = current.scheduledStudents.associate { it.id to false }
                        val mergedStates = defaultStates.toMutableMap()
                        current.attendanceStates.forEach { (k, v) -> mergedStates[k] = v }
                        current.copy(
                            existingAttendanceInfo = null,
                            attendanceStates = mergedStates,
                            isCheckingExistingAttendance = false
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isCheckingExistingAttendance = false,
                        existingAttendanceInfo = null
                    )
                }
            }
        }
    }

    fun toggleStudentAttendance(studentId: String) {
        if (_uiState.value.isAttendanceLocked) return
        _uiState.update { state ->
            val updated = state.attendanceStates.toMutableMap()
            updated[studentId] = !(updated[studentId] ?: false)
            state.copy(attendanceStates = updated)
        }
    }

    fun setAllAttendance(present: Boolean) {
        if (_uiState.value.isAttendanceLocked) return
        _uiState.update { state ->
            val updated = state.attendanceStates.toMutableMap()
            state.displayedStudents.forEach { s ->
                updated[s.id] = present
            }
            state.copy(attendanceStates = updated)
        }
    }

    fun addAdditionalStudent(student: StudentModel) {
        if (_uiState.value.isAttendanceLocked) return
        _uiState.update { state ->
            if (state.displayedStudents.none { it.id == student.id }) {
                val updatedExtras = state.additionalStudents + student
                val updatedStates = state.attendanceStates.toMutableMap()
                updatedStates[student.id] = true
                state.copy(
                    additionalStudents = updatedExtras,
                    attendanceStates = updatedStates,
                    searchQuery = "",
                    isSearchExpanded = false
                )
            } else {
                state
            }
        }
    }

    fun removeAdditionalStudent(studentId: String) {
        if (_uiState.value.isAttendanceLocked) return
        _uiState.update { state ->
            val updatedExtras = state.additionalStudents.filter { it.id != studentId }
            val updatedStates = state.attendanceStates.toMutableMap().apply { remove(studentId) }
            state.copy(
                additionalStudents = updatedExtras,
                attendanceStates = updatedStates
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSearchExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isSearchExpanded = expanded) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun saveAttendance(onSuccess: () -> Unit) {
        val state = _uiState.value
        val displayed = state.displayedStudents
        if (displayed.isEmpty()) {
            _uiState.update { it.copy(userMessage = "No students in this session to save.") }
            return
        }

        val presentIds = displayed.filter { state.attendanceStates[it.id] == true }.map { it.id }
        val absentIds = state.scheduledStudents.filter { state.attendanceStates[it.id] != true }.map { it.id }
        val extraIds = state.additionalStudents.map { it.id }

        val currentUser = authRepository.currentUser
        val userUid = currentUser?.uid ?: ""
        val userEmail = currentUser?.email ?: ""
        val resolvedName = state.currentUserName

        val record = AttendanceRecordModel(
            date = state.currentDate,
            timeSlot = state.selectedTimeSlot,
            presentStudentIds = presentIds,
            absentStudentIds = absentIds,
            additionalStudentIds = extraIds,
            totalPresent = presentIds.size,
            totalAbsent = absentIds.size,
            submittedBy = resolvedName,
            submittedByUid = userUid,
            submittedByEmail = userEmail,
            timestamp = Timestamp.now()
        )

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val result = attendanceRepository.saveBatchAttendance(record)
            if (result.isSuccess) {
                val extraInfo = if (extraIds.isNotEmpty()) " (${extraIds.size} out of schedule)" else ""
                auditLogRepository.log(
                    action = "Batch Attendance Saved",
                    details = "Recorded attendance for ${state.currentDate} [${state.selectedTimeSlot}]. Present: ${presentIds.size}, Absent: ${absentIds.size}$extraInfo (by $resolvedName)"
                )
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        userMessage = "Attendance saved successfully!"
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        userMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to save attendance"
                    )
                }
                checkExistingAttendance(state.selectedTimeSlot)
            }
        }
    }
}
