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

enum class StudentSortOption(val displayName: String) {
    NAME_ASC("Name (A-Z)"),
    HIGHEST_DUE("Highest Due"),
    NAME_DESC("Name (Z-A)")
}

data class StudentDirectoryUiState(
    val students: List<StudentModel> = emptyList(),
    val outstandingBalances: Map<String, Long> = emptyMap(),
    val searchQuery: String = "",
    val selectedClassFilter: String = "All",
    val duesOnlyFilter: Boolean = false,
    val sortOption: StudentSortOption = StudentSortOption.NAME_ASC,
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
    private val defaultDispatcher: CoroutineDispatcher,
    private val deltaSyncManager: com.soumit.technovision.data.local.DeltaSyncManager? = null
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

    private fun filterStudentsList(
        list: List<StudentModel>,
        query: String,
        classFilter: String,
        duesOnly: Boolean,
        balances: Map<String, Long>,
        sort: StudentSortOption
    ): List<StudentModel> {
        val q = query.trim()
        var result = list

        if (q.isNotBlank()) {
            result = result.filter {
                it.fullName.contains(q, ignoreCase = true) ||
                        it.school.contains(q, ignoreCase = true) ||
                        it.studentClass.contains(q, ignoreCase = true) ||
                        it.phone.contains(q, ignoreCase = true)
            }
        }

        if (classFilter != "All") {
            result = result.filter { it.studentClass.equals(classFilter, ignoreCase = true) }
        }

        if (duesOnly) {
            result = result.filter { (balances[it.id] ?: 0L) > 0L }
        }

        return when (sort) {
            StudentSortOption.NAME_ASC -> result.sortedBy { it.fullName.lowercase() }
            StudentSortOption.NAME_DESC -> result.sortedByDescending { it.fullName.lowercase() }
            StudentSortOption.HIGHEST_DUE -> result.sortedByDescending { balances[it.id] ?: 0L }
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
                _uiState.update { current ->
                    current.copy(
                        students = students,
                        outstandingBalances = balances,
                        filteredStudents = filterStudentsList(
                            list = students,
                            query = current.searchQuery,
                            classFilter = current.selectedClassFilter,
                            duesOnly = current.duesOnlyFilter,
                            balances = balances,
                            sort = current.sortOption
                        )
                    )
                }
                checkReady()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        reapplyFilters()
    }

    fun onClassFilterSelected(className: String) {
        _uiState.update { it.copy(selectedClassFilter = className) }
        reapplyFilters()
    }

    fun onDuesOnlyToggled(duesOnly: Boolean) {
        _uiState.update { it.copy(duesOnlyFilter = duesOnly) }
        reapplyFilters()
    }

    fun onSortOptionSelected(sort: StudentSortOption) {
        _uiState.update { it.copy(sortOption = sort) }
        reapplyFilters()
    }

    private fun reapplyFilters() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(defaultDispatcher) {
            val state = _uiState.value
            if (state.searchQuery.isNotBlank()) {
                delay(150L)
            }
            val filtered = filterStudentsList(
                list = state.students,
                query = state.searchQuery,
                classFilter = state.selectedClassFilter,
                duesOnly = state.duesOnlyFilter,
                balances = state.outstandingBalances,
                sort = state.sortOption
            )
            _uiState.update { it.copy(filteredStudents = filtered) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            runCatching {
                deltaSyncManager?.syncStudents(force = true)
                deltaSyncManager?.syncFees(force = true)
                feeRepository.syncAllStudentsTotalPaid()
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
}
