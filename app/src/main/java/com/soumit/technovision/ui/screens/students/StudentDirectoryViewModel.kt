package com.soumit.technovision.ui.screens.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.repository.AuthRepository
import com.soumit.technovision.data.repository.FeeRepository
import com.soumit.technovision.data.repository.StudentRepository
import com.soumit.technovision.domain.usecase.CalculateBillingDuesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
    private val authRepository: AuthRepository,
    private val calculateBillingDuesUseCase: CalculateBillingDuesUseCase = CalculateBillingDuesUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentDirectoryUiState())
    val uiState: StateFlow<StudentDirectoryUiState> = _uiState.asStateFlow()

    init {
        loadUserRole()
        observeStudentsAndBalances()
        viewModelScope.launch {
            // One-off sync in the background so legacy students have totalPaid populated
            feeRepository.syncAllStudentsTotalPaid()
        }
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
            studentRepository.getStudentsStream()
                .catch { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.localizedMessage) }
                }
                .collect { students ->
                    val balances = mutableMapOf<String, Long>()
                    for (student in students) {
                        val billingDues = calculateBillingDuesUseCase(
                            admissionDate = student.admissionDate,
                            monthlyInstallment = student.monthlyInstallment,
                            totalPaid = student.totalPaid
                        )
                        balances[student.id] = billingDues.outstandingBalance
                    }
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
