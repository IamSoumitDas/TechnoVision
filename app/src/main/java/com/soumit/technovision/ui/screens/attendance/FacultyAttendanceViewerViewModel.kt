package com.soumit.technovision.ui.screens.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel
import com.soumit.technovision.data.repository.AttendanceRepository
import com.soumit.technovision.data.repository.StaffRepository
import com.soumit.technovision.util.AttendanceUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class TeacherSummary(
    val teacherName: String,
    val monthClassesCount: Int,
    val allTimeClassesCount: Int,
    val monthHours: Double,
    val allTimeHours: Double
)

data class FacultyAttendanceViewerUiState(
    val requests: List<TeacherAttendanceRequestModel> = emptyList(),
    val userMap: Map<String, String> = emptyMap(),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val isLoading: Boolean = true,
    val userMessage: String? = null
) {
    val verifiedRequests: List<TeacherAttendanceRequestModel>
        get() = requests.filter { it.status.equals("Verified", ignoreCase = true) }

    val teacherSummaries: List<TeacherSummary>
        get() {
            val byTeacher = verifiedRequests.groupBy {
                userMap[it.teacherId] ?: userMap[it.teacherName] ?: it.teacherName.ifBlank { "Faculty Member" }
            }
            return byTeacher.map { (name, list) ->
                val monthList = list.filter { req ->
                    val parsed = AttendanceUtils.parseRecordDate(req.date)
                    if (parsed != null) {
                        parsed.first == selectedYear && parsed.second == selectedMonth
                    } else if (req.timestamp != null) {
                        val c = Calendar.getInstance().apply { time = req.timestamp.toDate() }
                        c.get(Calendar.YEAR) == selectedYear && c.get(Calendar.MONTH) == selectedMonth
                    } else false
                }
                val monthHours = monthList.sumOf { AttendanceUtils.parseSlotDurationHours(it.timeSlot) }
                val allTimeHours = list.sumOf { AttendanceUtils.parseSlotDurationHours(it.timeSlot) }
                TeacherSummary(
                    teacherName = name,
                    monthClassesCount = monthList.size,
                    allTimeClassesCount = list.size,
                    monthHours = monthHours,
                    allTimeHours = allTimeHours
                )
            }.sortedByDescending { it.monthClassesCount }
        }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class FacultyAttendanceViewerViewModel @javax.inject.Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val staffRepository: StaffRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacultyAttendanceViewerUiState())
    val uiState: StateFlow<FacultyAttendanceViewerUiState> = _uiState.asStateFlow()

    init {
        observeData()
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
                _uiState.update {
                    it.copy(
                        requests = reqs,
                        userMap = users,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onMonthChanged(year: Int, month: Int) {
        _uiState.update { it.copy(selectedYear = year, selectedMonth = month) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
