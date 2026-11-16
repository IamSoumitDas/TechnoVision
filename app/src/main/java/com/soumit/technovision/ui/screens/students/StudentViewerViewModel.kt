package com.soumit.technovision.ui.screens.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.repository.FeeRepository
import com.soumit.technovision.data.repository.StudentRepository
import com.soumit.technovision.util.BillingUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentViewerUiState(
    val student: StudentModel? = null,
    val weeklySchedule: Map<String, String> = emptyMap(),
    val totalPaid: Long = 0L,
    val calculatedMonthsDue: Int = 0,
    val expectedTotalFees: Long = 0L,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val outstandingDue: Long
        get() = (expectedTotalFees - totalPaid).coerceAtLeast(0L)
}

class StudentViewerViewModel(
    private val studentId: String,
    private val studentRepository: StudentRepository,
    private val feeRepository: FeeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentViewerUiState())
    val uiState: StateFlow<StudentViewerUiState> = _uiState.asStateFlow()

    init {
        loadStudentData()
    }

    private fun loadStudentData() {
        if (studentId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid student ID") }
            return
        }

        viewModelScope.launch {
            combine(
                studentRepository.getStudentStream(studentId),
                feeRepository.getPaymentsForStudentStream(studentId)
            ) { student, payments ->
                if (student != null) {
                    val (months, expected) = BillingUtils.calculateBillingMonths(
                        student.admissionDate,
                        student.monthlyInstallment
                    )
                    val paid = payments.sumOf { it.amountPaid }
                    StudentViewerUiState(
                        student = student,
                        weeklySchedule = student.weeklySchedule,
                        totalPaid = paid,
                        calculatedMonthsDue = months,
                        expectedTotalFees = expected,
                        isLoading = false
                    )
                } else {
                    StudentViewerUiState(
                        student = null,
                        isLoading = false,
                        errorMessage = "Student not found"
                    )
                }
            }.catch { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.localizedMessage) }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
}
