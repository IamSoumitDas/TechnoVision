package com.soumit.technovision.ui.screens.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.repository.AttendanceRepository
import com.soumit.technovision.data.repository.StaffRepository
import com.soumit.technovision.data.repository.StudentRepository
import com.soumit.technovision.util.AttendanceUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AttendanceHistoryUiState(
    val attendanceRecords: List<AttendanceRecordModel> = emptyList(),
    val studentsMap: Map<String, StudentModel> = emptyMap(),
    val userMap: Map<String, String> = emptyMap(),
    val teacherAttendanceMap: Map<String, String> = emptyMap(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val selectedRecord: AttendanceRecordModel? = null,
    val isDetailOpen: Boolean = false,
    val errorMessage: String? = null
)

private data class AttendanceData(
    val records: List<AttendanceRecordModel>,
    val studentsMap: Map<String, StudentModel>,
    val userMap: Map<String, String>,
    val teacherAttendanceMap: Map<String, String>
)

@dagger.hilt.android.lifecycle.HiltViewModel
class AttendanceHistoryViewModel @javax.inject.Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val studentRepository: StudentRepository,
    private val staffRepository: StaffRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceHistoryUiState())
    val uiState: StateFlow<AttendanceHistoryUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                attendanceRepository.getAttendanceRecordsStream(),
                studentRepository.getStudentsStream(),
                staffRepository.getUsersMapStream(),
                attendanceRepository.getTeacherAttendanceRequestsStream(teacherId = null)
            ) { records, students, users, teacherRequests ->
                val sMap = students.associateBy { it.id }
                val teacherMap = teacherRequests
                    .filter { it.teacherName.isNotBlank() }
                    .associate { req ->
                        val normKey = AttendanceUtils.normalizeSlotKey(req.date, req.timeSlot)
                        normKey to req.teacherName
                    }
                AttendanceData(records, sMap, users, teacherMap)
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }.collect { data ->
                _uiState.update {
                    it.copy(
                        attendanceRecords = data.records,
                        studentsMap = data.studentsMap,
                        userMap = data.userMap,
                        teacherAttendanceMap = data.teacherAttendanceMap,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openDetail(record: AttendanceRecordModel) {
        _uiState.update { it.copy(selectedRecord = record, isDetailOpen = true) }
    }

    fun closeDetail() {
        _uiState.update { it.copy(selectedRecord = null, isDetailOpen = false) }
    }
}
