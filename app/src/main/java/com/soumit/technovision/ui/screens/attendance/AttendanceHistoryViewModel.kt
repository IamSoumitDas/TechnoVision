package com.soumit.technovision.ui.screens.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.repository.AttendanceRepository
import com.soumit.technovision.data.repository.StaffRepository
import com.soumit.technovision.data.repository.StudentRepository
import com.soumit.technovision.util.AttendanceUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AttendanceHistoryUiState(
    val attendanceRecords: List<AttendanceRecordModel> = emptyList(),
    val studentsMap: Map<String, StudentModel> = emptyMap(),
    val userMap: Map<String, String> = emptyMap(),
    val teacherAttendanceMap: Map<String, String> = emptyMap(),
    val fallbackTeacherMap: Map<String, String> = emptyMap(),
    val filteredGroupedRecords: List<Pair<String, List<AttendanceRecordModel>>> = emptyList(),
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
    val teacherAttendanceMap: Map<String, String>,
    val fallbackTeacherMap: Map<String, String>
)

@dagger.hilt.android.lifecycle.HiltViewModel
class AttendanceHistoryViewModel @javax.inject.Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val studentRepository: StudentRepository,
    private val staffRepository: StaffRepository,
    private val auditLogRepository: com.soumit.technovision.data.repository.AuditLogRepository? = null,
    private val defaultDispatcher: CoroutineDispatcher? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceHistoryUiState())
    val uiState: StateFlow<AttendanceHistoryUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        val logsFlow = auditLogRepository?.getLogsStream()?.catch { emit(emptyList()) }
            ?: kotlinx.coroutines.flow.flowOf(emptyList())

        viewModelScope.launch {
            combine(
                attendanceRepository.getAttendanceRecordsStream(),
                studentRepository.getStudentsStream(),
                staffRepository.getUsersMapStream(),
                attendanceRepository.getTeacherAttendanceRequestsStream(teacherId = null),
                logsFlow
            ) { records, students, users, teacherRequests, logs ->
                val sMap = students.associateBy { it.id }

                // 1. Augment userMap with teacherId -> teacherName from teacher attendance requests
                val augmentedUserMap = users.toMutableMap()
                teacherRequests
                    .filter { !it.status.equals("Rejected", ignoreCase = true) }
                    .forEach { req ->
                        val tId = req.teacherId.trim()
                        val tName = req.teacherName.trim()
                        if (tId.isNotBlank() && tName.isNotBlank() && !AttendanceUtils.isPlaceholderName(tName)) {
                            augmentedUserMap.putIfAbsent(tId, tName)
                        }
                    }

                // 2. Build teacherAttendanceMap for (date, slot) -> teacherName from strictly verified requests
                val teacherMap = mutableMapOf<String, String>()
                val verifiedRequests = teacherRequests
                    .filter {
                        it.status.equals("Verified", ignoreCase = true)
                    }

                for (req in verifiedRequests) {
                    val resolvedTeacherName = req.teacherName.takeIf { name -> name.isNotBlank() && !AttendanceUtils.isPlaceholderName(name) }
                        ?: augmentedUserMap[req.teacherId]
                        ?: augmentedUserMap[req.teacherId.lowercase()]
                        ?: ""

                    if (resolvedTeacherName.isNotBlank()) {
                        val normKey = AttendanceUtils.normalizeSlotKey(req.date, req.timeSlot)
                        val rawKey = "${req.date}_${req.timeSlot}"
                        val cleanDate = req.date.trim().replace('/', '-')
                        val cleanSlot = req.timeSlot.replace(Regex("\\s+"), " ").trim().lowercase()
                        val legacyKey = "${cleanDate}_${cleanSlot}"

                        teacherMap[normKey] = resolvedTeacherName
                        teacherMap[rawKey] = resolvedTeacherName
                        teacherMap[legacyKey] = resolvedTeacherName

                        AttendanceUtils.parseRecordDate(req.date)?.let { parsed ->
                            val canonicalDate = String.format(java.util.Locale.US, "%02d-%02d-%04d", parsed.third, parsed.second + 1, parsed.first)
                            val canonicalKey = AttendanceUtils.normalizeSlotKey(canonicalDate, req.timeSlot)
                            teacherMap[canonicalKey] = resolvedTeacherName
                        }
                    }
                }

                // 3. Build fallbackTeacherMap for (date, slot) -> teacherName from:
                // a) Audit logs in system_logs ("Batch Attendance Saved")
                // b) Pending teacher attendance requests in teacher_attendance
                val fallbackMap = mutableMapOf<String, String>()

                // Audit logs from system_logs
                logs.filter { it.action.contains("Attendance", ignoreCase = true) }.forEach { log ->
                    val pair = AttendanceUtils.extractDateAndSlotFromLog(log.details)
                    if (pair != null) {
                        val (logDate, logSlot) = pair
                        val performer = log.performedBy.trim()
                        val name = augmentedUserMap[performer]
                            ?: augmentedUserMap[performer.lowercase()]
                            ?: (if (performer.contains("@")) AttendanceUtils.formatEmailPrefixToName(performer) else performer)

                        if (name.isNotBlank() && !AttendanceUtils.isPlaceholderName(name)) {
                            val normKey = AttendanceUtils.normalizeSlotKey(logDate, logSlot)
                            val rawKey = "${logDate}_${logSlot}"
                            fallbackMap[normKey] = name
                            fallbackMap[rawKey] = name
                            AttendanceUtils.parseRecordDate(logDate)?.let { parsed ->
                                val canonicalDate = String.format(java.util.Locale.US, "%02d-%02d-%04d", parsed.third, parsed.second + 1, parsed.first)
                                fallbackMap[AttendanceUtils.normalizeSlotKey(canonicalDate, logSlot)] = name
                            }
                        }
                    }
                }

                // Pending requests take priority over system logs
                teacherRequests
                    .filter {
                        !it.status.equals("Rejected", ignoreCase = true) &&
                        !it.status.equals("Verified", ignoreCase = true)
                    }
                    .forEach { req ->
                        val resolvedTeacherName = req.teacherName.takeIf { name -> name.isNotBlank() && !AttendanceUtils.isPlaceholderName(name) }
                            ?: augmentedUserMap[req.teacherId]
                            ?: augmentedUserMap[req.teacherId.lowercase()]
                            ?: ""

                        if (resolvedTeacherName.isNotBlank()) {
                            val normKey = AttendanceUtils.normalizeSlotKey(req.date, req.timeSlot)
                            val rawKey = "${req.date}_${req.timeSlot}"
                            val cleanDate = req.date.trim().replace('/', '-')
                            val cleanSlot = req.timeSlot.replace(Regex("\\s+"), " ").trim().lowercase()
                            val legacyKey = "${cleanDate}_${cleanSlot}"

                            fallbackMap[normKey] = resolvedTeacherName
                            fallbackMap[rawKey] = resolvedTeacherName
                            fallbackMap[legacyKey] = resolvedTeacherName

                            AttendanceUtils.parseRecordDate(req.date)?.let { parsed ->
                                val canonicalDate = String.format(java.util.Locale.US, "%02d-%02d-%04d", parsed.third, parsed.second + 1, parsed.first)
                                val canonicalKey = AttendanceUtils.normalizeSlotKey(canonicalDate, req.timeSlot)
                                fallbackMap[canonicalKey] = resolvedTeacherName
                            }
                        }
                    }

                AttendanceData(records, sMap, augmentedUserMap, teacherMap, fallbackMap)
            }.let { flow ->
                if (defaultDispatcher != null) flow.flowOn(defaultDispatcher) else flow
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }.collect { data ->
                val grouped = if (defaultDispatcher != null) {
                    withContext(defaultDispatcher) {
                        filterAndGroup(
                            records = data.records,
                            query = _uiState.value.searchQuery,
                            studentsMap = data.studentsMap,
                            userMap = data.userMap,
                            teacherAttendanceMap = data.teacherAttendanceMap,
                            fallbackTeacherMap = data.fallbackTeacherMap
                        )
                    }
                } else {
                    filterAndGroup(
                        records = data.records,
                        query = _uiState.value.searchQuery,
                        studentsMap = data.studentsMap,
                        userMap = data.userMap,
                        teacherAttendanceMap = data.teacherAttendanceMap,
                        fallbackTeacherMap = data.fallbackTeacherMap
                    )
                }
                _uiState.update {
                    it.copy(
                        attendanceRecords = data.records,
                        studentsMap = data.studentsMap,
                        userMap = data.userMap,
                        teacherAttendanceMap = data.teacherAttendanceMap,
                        fallbackTeacherMap = data.fallbackTeacherMap,
                        filteredGroupedRecords = grouped,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun filterAndGroup(
        records: List<AttendanceRecordModel>,
        query: String,
        studentsMap: Map<String, StudentModel>,
        userMap: Map<String, String>,
        teacherAttendanceMap: Map<String, String>,
        fallbackTeacherMap: Map<String, String>
    ): List<Pair<String, List<AttendanceRecordModel>>> {
        val filtered = if (query.isBlank()) {
            records
        } else {
            val q = query.trim().lowercase()
            records.filter { record ->
                record.date.lowercase().contains(q) ||
                record.timeSlot.lowercase().contains(q) ||
                AttendanceUtils.resolveTeacherName(record, userMap, teacherAttendanceMap, fallbackTeacherMap).lowercase().contains(q) ||
                record.presentStudentIds.any { id ->
                    studentsMap[id]?.fullName?.lowercase()?.contains(q) == true
                } ||
                record.absentStudentIds.any { id ->
                    studentsMap[id]?.fullName?.lowercase()?.contains(q) == true
                } ||
                record.additionalStudentIds.any { id ->
                    studentsMap[id]?.fullName?.lowercase()?.contains(q) == true
                }
            }
        }
        return filtered.groupBy { it.date }
            .toList()
            .sortedByDescending { (dateStr, _) ->
                AttendanceUtils.parseDateForSorting(dateStr)
            }
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            if (query.isNotBlank()) {
                kotlinx.coroutines.delay(120L)
            }
            val state = _uiState.value
            val grouped = filterAndGroup(
                records = state.attendanceRecords,
                query = query,
                studentsMap = state.studentsMap,
                userMap = state.userMap,
                teacherAttendanceMap = state.teacherAttendanceMap,
                fallbackTeacherMap = state.fallbackTeacherMap
            )
            _uiState.update { it.copy(filteredGroupedRecords = grouped) }
        }
    }

    fun openDetail(record: AttendanceRecordModel) {
        _uiState.update { it.copy(selectedRecord = record, isDetailOpen = true) }
    }

    fun closeDetail() {
        _uiState.update { it.copy(selectedRecord = null, isDetailOpen = false) }
    }
}
