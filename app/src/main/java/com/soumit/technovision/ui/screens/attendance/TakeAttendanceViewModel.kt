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
    val isOnline: Boolean = true,
    val userMessage: String? = null,
    val scheduledStudents: List<StudentModel> = emptyList(),
    val displayedStudents: List<StudentModel> = emptyList(),
    val presentCount: Int = 0,
    val absentCount: Int = 0
) {
    val isAttendanceLocked: Boolean
        get() = existingAttendanceInfo != null
}

@dagger.hilt.android.lifecycle.HiltViewModel
class TakeAttendanceViewModel @javax.inject.Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val studentRepository: StudentRepository,
    private val staffRepository: StaffRepository,
    private val authRepository: AuthRepository,
    private val auditLogRepository: AuditLogRepository,
    private val networkMonitor: com.soumit.technovision.util.NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(recomputeDerived(TakeAttendanceUiState()))
    val uiState: StateFlow<TakeAttendanceUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
        loadStudents()
        checkExistingAttendance(_uiState.value.selectedTimeSlot)
        observeNetwork()
    }

    private fun recomputeDerived(state: TakeAttendanceUiState): TakeAttendanceUiState {
        val targetSlot = AttendanceUtils.normalizeTimeSlot(state.selectedTimeSlot)
        val scheduled = state.allStudents.filter { student ->
            val slot = student.weeklySchedule[state.todayName]
            slot != null && AttendanceUtils.normalizeTimeSlot(slot) == targetSlot
        }
        val scheduledIds = scheduled.map { it.id }.toSet()
        val sanitizedExtras = state.additionalStudents.filter { it.id !in scheduledIds }
        val displayed = scheduled + sanitizedExtras
        val present = displayed.count { state.attendanceStates[it.id] == true }
        val absent = scheduled.count { state.attendanceStates[it.id] != true }
        return state.copy(
            scheduledStudents = scheduled,
            additionalStudents = sanitizedExtras,
            displayedStudents = displayed,
            presentCount = present,
            absentCount = absent
        )
    }

    private fun updateState(reducer: (TakeAttendanceUiState) -> TakeAttendanceUiState) {
        _uiState.update { recomputeDerived(reducer(it)) }
    }

    private fun observeNetwork() {
        if (networkMonitor == null) return
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                updateState { it.copy(isOnline = online) }
            }
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            // Pre-load current user's display name from auth / user profile immediately
            val currentNameRes = authRepository.getCurrentUserName()
            if (currentNameRes.isSuccess) {
                val name = currentNameRes.getOrNull()
                if (!name.isNullOrBlank() && !AttendanceUtils.isPlaceholderName(name)) {
                    updateState { it.copy(currentUserName = name) }
                }
            }

            staffRepository.getUsersMapStream().collect { map ->
                val currentUser = authRepository.currentUser
                val resolvedName = map[currentUser?.uid]?.takeIf { !AttendanceUtils.isPlaceholderName(it) }
                    ?: map[currentUser?.email]?.takeIf { !AttendanceUtils.isPlaceholderName(it) }
                    ?: _uiState.value.currentUserName.takeIf { !AttendanceUtils.isPlaceholderName(it) }
                    ?: currentUser?.displayName?.takeIf { it.isNotBlank() && !AttendanceUtils.isPlaceholderName(it) }
                    ?: currentUser?.email?.let { AttendanceUtils.formatEmailPrefixToName(it) }?.takeIf { it.isNotBlank() }
                    ?: "Faculty"

                updateState {
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
                    updateState {
                        it.copy(
                            isLoadingStudents = false,
                            userMessage = "Error loading students: ${e.localizedMessage}"
                        )
                    }
                }
                .collect { students ->
                    updateState { state ->
                        val targetSlot = AttendanceUtils.normalizeTimeSlot(state.selectedTimeSlot)
                        val scheduled = students.filter { s ->
                            val slot = s.weeklySchedule[state.todayName]
                            slot != null && AttendanceUtils.normalizeTimeSlot(slot) == targetSlot
                        }
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
        updateState {
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
        updateState { it.copy(isCheckingExistingAttendance = true) }

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

                    updateState {
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
                    updateState { current ->
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
                updateState {
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
        updateState { state ->
            val updated = state.attendanceStates.toMutableMap()
            updated[studentId] = !(updated[studentId] ?: false)
            state.copy(attendanceStates = updated)
        }
    }

    fun setAllAttendance(present: Boolean) {
        if (_uiState.value.isAttendanceLocked) return
        updateState { state ->
            val updated = state.attendanceStates.toMutableMap()
            state.displayedStudents.forEach { s ->
                updated[s.id] = present
            }
            state.copy(attendanceStates = updated)
        }
    }

    fun addAdditionalStudent(student: StudentModel) {
        if (_uiState.value.isAttendanceLocked) return
        updateState { state ->
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
        updateState { state ->
            val updatedExtras = state.additionalStudents.filter { it.id != studentId }
            val updatedStates = state.attendanceStates.toMutableMap().apply { remove(studentId) }
            state.copy(
                additionalStudents = updatedExtras,
                attendanceStates = updatedStates
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        updateState { it.copy(searchQuery = query) }
    }

    fun setSearchExpanded(expanded: Boolean) {
        updateState { it.copy(isSearchExpanded = expanded) }
    }

    fun clearUserMessage() {
        updateState { it.copy(userMessage = null) }
    }

    fun saveAttendance(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!state.isOnline) {
            updateState { it.copy(userMessage = "Connect to the internet to submit attendance") }
            return
        }
        val displayed = state.displayedStudents
        if (displayed.isEmpty()) {
            updateState { it.copy(userMessage = "No students in this session") }
            return
        }

        val presentIds = displayed.filter { state.attendanceStates[it.id] == true }.map { it.id }
        val absentIds = state.scheduledStudents.filter { state.attendanceStates[it.id] != true }.map { it.id }
        val extraIds = state.additionalStudents.map { it.id }

        val currentUser = authRepository.currentUser
        val userUid = currentUser?.uid ?: ""
        val userEmail = currentUser?.email ?: ""
        val formattedEmailName = currentUser?.email?.let { AttendanceUtils.formatEmailPrefixToName(it) }?.takeIf { it.isNotBlank() }

        val resolvedName = state.currentUserName.takeIf { it.isNotBlank() && !AttendanceUtils.isPlaceholderName(it) }
            ?: state.userMap[userUid]?.takeIf { !AttendanceUtils.isPlaceholderName(it) }
            ?: state.userMap[userEmail]?.takeIf { !AttendanceUtils.isPlaceholderName(it) }
            ?: currentUser?.displayName?.takeIf { it.isNotBlank() && !AttendanceUtils.isPlaceholderName(it) }
            ?: formattedEmailName
            ?: "Faculty"

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

        updateState { it.copy(isSaving = true) }

        viewModelScope.launch {
            val result = attendanceRepository.saveBatchAttendance(record)
            if (result.isSuccess) {
                val extraInfo = if (extraIds.isNotEmpty()) " (${extraIds.size} out of schedule)" else ""
                auditLogRepository.log(
                    action = "Batch Attendance Saved",
                    details = "Recorded attendance for ${state.currentDate} [${state.selectedTimeSlot}]. Present: ${presentIds.size}, Absent: ${absentIds.size}$extraInfo (by $resolvedName)"
                )
                updateState {
                    it.copy(
                        isSaving = false,
                        userMessage = "Attendance recorded"
                    )
                }
                onSuccess()
            } else {
                updateState {
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
