package com.soumit.technovision.ui.screens.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.repository.AttendanceRepository
import com.soumit.technovision.data.repository.StaffRepository
import com.soumit.technovision.data.repository.StudentRepository
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
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val selectedRecord: AttendanceRecordModel? = null,
    val isDetailOpen: Boolean = false,
    val errorMessage: String? = null
)

class AttendanceHistoryViewModel(
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
                staffRepository.getUsersMapStream()
            ) { records, students, users ->
                val sMap = students.associateBy { it.id }
                Triple(records, sMap, users)
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }.collect { (records, sMap, users) ->
                _uiState.update {
                    it.copy(
                        attendanceRecords = records,
                        studentsMap = sMap,
                        userMap = users,
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
