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

                // 1. Augment userMap with teacherId -> teacherName from verified teacher attendance requests
                val augmentedUserMap = users.toMutableMap()
                teacherRequests
                    .filter { it.status.equals("Verified", ignoreCase = true) && !AttendanceUtils.isPlaceholderName(it.teacherName) }
                    .forEach { req ->
                        val tId = req.teacherId.trim()
                        val tName = req.teacherName.trim()
                        if (tId.isNotBlank() && tName.isNotBlank()) {
                            augmentedUserMap.putIfAbsent(tId, tName)
                        }
                    }

                // 2. Build teacherAttendanceMap for (date, slot) -> teacherName from verified requests
                val teacherMap = mutableMapOf<String, String>()
                val verifiedRequests = teacherRequests
                    .filter {
                        it.teacherName.isNotBlank() &&
                        it.status.equals("Verified", ignoreCase = true) &&
                        !AttendanceUtils.isPlaceholderName(it.teacherName)
                    }

                for (req in verifiedRequests) {
                    val normKey = AttendanceUtils.normalizeSlotKey(req.date, req.timeSlot)
                    val rawKey = "${req.date}_${req.timeSlot}"
                    val cleanDate = req.date.trim().replace('/', '-')
                    val cleanSlot = req.timeSlot.replace(Regex("\\s+"), " ").trim().lowercase()
                    val legacyKey = "${cleanDate}_${cleanSlot}"

                    teacherMap[normKey] = req.teacherName
                    teacherMap[rawKey] = req.teacherName
                    teacherMap[legacyKey] = req.teacherName

                    AttendanceUtils.parseRecordDate(req.date)?.let { parsed ->
                        val canonicalDate = "%02d-%02d-%04d".format(parsed.third, parsed.second + 1, parsed.first)
                        val canonicalKey = AttendanceUtils.normalizeSlotKey(canonicalDate, req.timeSlot)
                        teacherMap[canonicalKey] = req.teacherName
                    }
                }

                AttendanceData(records, sMap, augmentedUserMap, teacherMap)
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
