package com.soumit.technovision.ui.screens.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.repository.AuthRepository
import com.soumit.technovision.data.repository.FeeRepository
import com.soumit.technovision.data.repository.StudentRepository
import com.soumit.technovision.domain.usecase.CalculateBillingDuesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class StudentDirectoryUiState(
    val students: List<StudentModel> = emptyList(),
    val outstandingBalances: Map<String, Long> = emptyMap(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val userRole: String = "",
    val errorMessage: String? = null,
    val filteredStudents: List<StudentModel> = emptyList()
)

@HiltViewModel
class StudentDirectoryViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val feeRepository: FeeRepository,
    private val authRepository: AuthRepository,
    private val calculateBillingDuesUseCase: CalculateBillingDuesUseCase,
    private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentDirectoryUiState())
    val uiState: StateFlow<StudentDirectoryUiState> = _uiState.asStateFlow()

    private var isStudentsLoaded = false
    private var isRoleLoaded = false
    private var searchJob: Job? = null

    init {
        loadUserRole()
        observeStudentsAndBalances()
        viewModelScope.launch {
            // One-off sync in the background so legacy students have totalPaid populated
            feeRepository.syncAllStudentsTotalPaid()
        }
    }

    private fun loadUserRole() {
        val currentUserId = authRepository.currentUserId
        if (currentUserId == null) {
            isRoleLoaded = true
            checkReady()
            return
        }
        viewModelScope.launch {
            val roleResult = authRepository.getUserRole(currentUserId)
            val role = if (roleResult.isSuccess) roleResult.getOrThrow().trim().lowercase() else ""
            isRoleLoaded = true
            _uiState.update { it.copy(userRole = role) }
            checkReady()
        }
    }

    private fun checkReady() {
        if (isStudentsLoaded && isRoleLoaded) {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun filterStudentsList(list: List<StudentModel>, query: String): List<StudentModel> {
        val q = query.trim()
        if (q.isBlank()) return list
        return list.filter {
            it.fullName.contains(q, ignoreCase = true) ||
                    it.school.contains(q, ignoreCase = true) ||
                    it.studentClass.contains(q, ignoreCase = true) ||
                    it.phone.contains(q, ignoreCase = true)
        }
    }

    private fun observeStudentsAndBalances() {
        viewModelScope.launch {
            combine(
                studentRepository.getStudentsStream(),
                feeRepository.getFeePaymentsStream()
            ) { students, payments ->
                val paymentsByStudent = payments.groupBy { it.studentId }
                val calendar = Calendar.getInstance()
                val curYear = calendar.get(Calendar.YEAR)
                val curMonth = calendar.get(Calendar.MONTH) + 1

                val balances = mutableMapOf<String, Long>()
                val updatedStudents = students.map { student ->
                    val actualTotalPaid = paymentsByStudent[student.id]?.sumOf { it.amountPaid } ?: 0L
                    val billingDues = calculateBillingDuesUseCase(
                        admissionDate = student.admissionDate,
                        monthlyInstallment = student.monthlyInstallment,
                        totalPaid = actualTotalPaid,
                        currentYear = curYear,
                        currentMonth = curMonth
                    )
                    balances[student.id] = billingDues.outstandingBalance
                    student.copy(totalPaid = actualTotalPaid)
                }
                Pair(updatedStudents, balances)
            }.flowOn(defaultDispatcher)
            .catch { error ->
                isStudentsLoaded = true
                _uiState.update { it.copy(isLoading = false, errorMessage = error.localizedMessage) }
            }.collect { (students, balances) ->
                isStudentsLoaded = true
                _uiState.update {
                    it.copy(
                        students = students,
                        outstandingBalances = balances,
                        filteredStudents = filterStudentsList(students, it.searchQuery)
                    )
                }
                checkReady()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch(defaultDispatcher) {
            if (query.isNotBlank()) {
                delay(150L)
            }
            val filtered = filterStudentsList(_uiState.value.students, query)
            _uiState.update { it.copy(filteredStudents = filtered) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            runCatching {
                feeRepository.syncAllStudentsTotalPaid()
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
}
