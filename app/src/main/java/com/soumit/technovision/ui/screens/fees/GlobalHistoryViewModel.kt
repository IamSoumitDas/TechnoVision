package com.soumit.technovision.ui.screens.fees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.model.FeePaymentModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.repository.AuditLogRepository
import com.soumit.technovision.data.repository.AuthRepository
import com.soumit.technovision.data.repository.FeeRepository
import com.soumit.technovision.data.repository.StudentRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GlobalHistoryUiState(
    val paymentHistory: List<FeePaymentModel> = emptyList(),
    val studentsMap: Map<String, StudentModel> = emptyMap(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedMethodFilter: String = "All",
    val currentUserRole: String = "",
    val paymentToDelete: FeePaymentModel? = null,
    val isDeletingPayment: Boolean = false,
    val isOnline: Boolean = true,
    val userMessage: String? = null,
    val filteredPayments: List<FeePaymentModel> = emptyList()
)

@dagger.hilt.android.lifecycle.HiltViewModel
class GlobalHistoryViewModel @javax.inject.Inject constructor(
    private val feeRepository: FeeRepository,
    private val studentRepository: StudentRepository,
    private val authRepository: AuthRepository,
    private val auditLogRepository: AuditLogRepository,
    private val defaultDispatcher: CoroutineDispatcher,
    private val networkMonitor: com.soumit.technovision.util.NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalHistoryUiState())
    val uiState: StateFlow<GlobalHistoryUiState> = _uiState.asStateFlow()

    private var isDataLoaded = false
    private var isRoleLoaded = false

    init {
        loadUserRole()
        observeData()
        observeNetwork()
    }

    private fun observeNetwork() {
        if (networkMonitor == null) return
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
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
            _uiState.update { it.copy(currentUserRole = role) }
            checkReady()
        }
    }

    private fun computeFilteredPayments(state: GlobalHistoryUiState): List<FeePaymentModel> {
        val methodFilter = state.selectedMethodFilter
        val q = state.searchQuery.trim()
        val sMap = state.studentsMap
        if (methodFilter == "All" && q.isBlank()) {
            return state.paymentHistory
        }
        return state.paymentHistory.filter { payment ->
            val matchesMethod = methodFilter == "All" || payment.paymentMethod.equals(methodFilter, ignoreCase = true)
            val student = sMap[payment.studentId]
            val matchesQuery = q.isBlank() ||
                    payment.studentName.contains(q, ignoreCase = true) ||
                    payment.remarks.contains(q, ignoreCase = true) ||
                    payment.receivedBy.contains(q, ignoreCase = true) ||
                    payment.paymentMethod.contains(q, ignoreCase = true) ||
                    payment.amountPaid.toString().contains(q) ||
                    (student?.fullName?.contains(q, ignoreCase = true) == true) ||
                    (student?.studentClass?.contains(q, ignoreCase = true) == true) ||
                    (student?.school?.contains(q, ignoreCase = true) == true)
            matchesMethod && matchesQuery
        }
    }

    private fun updateState(reducer: (GlobalHistoryUiState) -> GlobalHistoryUiState) {
        _uiState.update { current ->
            val updated = reducer(current)
            updated.copy(filteredPayments = computeFilteredPayments(updated))
        }
    }

    private fun checkReady() {
        if (isDataLoaded && isRoleLoaded) {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                feeRepository.getFeePaymentsStream(),
                studentRepository.getStudentsStream()
            ) { payments, students ->
                val sMap = students.associateBy { it.id }
                Pair(payments, sMap)
            }.flowOn(defaultDispatcher)
            .catch { e ->
                isDataLoaded = true
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "Error loading history: ${e.localizedMessage}"
                    )
                }
            }.collect { (payments, sMap) ->
                isDataLoaded = true
                updateState {
                    it.copy(
                        paymentHistory = payments,
                        studentsMap = sMap
                    )
                }
                checkReady()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        updateState { it.copy(searchQuery = query) }
    }

    fun onMethodFilterChanged(method: String) {
        updateState { it.copy(selectedMethodFilter = method) }
    }

    fun setPaymentToDelete(payment: FeePaymentModel?) {
        _uiState.update { it.copy(paymentToDelete = payment) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun deletePayment(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!state.isOnline) {
            _uiState.update { it.copy(userMessage = "Connect to the internet to delete payment receipts") }
            return
        }
        val target = state.paymentToDelete ?: return
        val studentName = target.studentName.ifBlank { "Student" }

        _uiState.update { it.copy(isDeletingPayment = true) }

        viewModelScope.launch {
            val result = feeRepository.deletePayment(target.id)
            if (result.isSuccess) {
                auditLogRepository.log(
                    action = "Payment Receipt Deleted",
                    details = "Deleted payment of ₹${target.amountPaid} (${target.paymentMethod}) for student: $studentName"
                )
                _uiState.update {
                    it.copy(
                        isDeletingPayment = false,
                        paymentToDelete = null,
                        userMessage = "Payment receipt deleted"
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isDeletingPayment = false,
                        userMessage = "Failed to delete: ${result.exceptionOrNull()?.localizedMessage}"
                    )
                }
            }
        }
    }
}
