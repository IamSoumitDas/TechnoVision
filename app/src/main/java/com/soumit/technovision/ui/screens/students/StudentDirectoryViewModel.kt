package com.soumit.technovision.ui.screens.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.repository.AuthRepository
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

data class StudentDirectoryUiState(
    val students: List<StudentModel> = emptyList(),
    val outstandingBalances: Map<String, Long> = emptyMap(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val userRole: String = "owner",
    val errorMessage: String? = null
) {
    val filteredStudents: List<StudentModel>
        get() {
            if (searchQuery.isBlank()) return students
            return students.filter {
                it.fullName.contains(searchQuery, ignoreCase = true) ||
                        it.school.contains(searchQuery, ignoreCase = true) ||
                        it.studentClass.contains(searchQuery, ignoreCase = true) ||
                        it.phone.contains(searchQuery, ignoreCase = true)
            }
        }
}

class StudentDirectoryViewModel(
    private val studentRepository: StudentRepository,
    private val feeRepository: FeeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentDirectoryUiState())
    val uiState: StateFlow<StudentDirectoryUiState> = _uiState.asStateFlow()

    init {
        loadUserRole()
        observeStudentsAndBalances()
    }

    private fun loadUserRole() {
        viewModelScope.launch {
            val currentUserId = authRepository.currentUser?.uid
            if (currentUserId != null) {
                val roleResult = authRepository.getUserRole(currentUserId)
                if (roleResult.isSuccess) {
                    _uiState.update { it.copy(userRole = roleResult.getOrThrow().trim().lowercase()) }
                }
            }
        }
    }

    private fun observeStudentsAndBalances() {
        viewModelScope.launch {
            combine(
                studentRepository.getStudentsStream(),
                feeRepository.getFeePaymentsStream()
            ) { students, payments ->
                val paymentsMap = mutableMapOf<String, Long>()
                for (payment in payments) {
                    if (payment.studentId.isNotBlank()) {
                        paymentsMap[payment.studentId] = (paymentsMap[payment.studentId] ?: 0L) + payment.amountPaid
                    }
                }
                val balances = mutableMapOf<String, Long>()
                for (student in students) {
                    val expected = BillingUtils.calculateBillingMonths(
                        student.admissionDate,
                        student.monthlyInstallment
                    ).second
                    val paid = paymentsMap[student.id] ?: 0L
                    val due = expected - paid
                    balances[student.id] = if (due > 0) due else 0L
                }
                Pair(students, balances)
            }.catch { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.localizedMessage) }
            }.collect { (students, balances) ->
                _uiState.update {
                    it.copy(
                        students = students,
                        outstandingBalances = balances,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}
