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
import kotlinx.coroutines.Dispatchers
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
    val userMessage: String? = null
) {
    val filteredPayments: List<FeePaymentModel>
        get() {
            return paymentHistory.filter { payment ->
                val matchesMethod = selectedMethodFilter == "All" || payment.paymentMethod.equals(selectedMethodFilter, ignoreCase = true)
                val student = studentsMap[payment.studentId]
                val matchesQuery = searchQuery.isBlank() ||
                        payment.studentName.contains(searchQuery, ignoreCase = true) ||
                        payment.remarks.contains(searchQuery, ignoreCase = true) ||
                        payment.receivedBy.contains(searchQuery, ignoreCase = true) ||
                        payment.paymentMethod.contains(searchQuery, ignoreCase = true) ||
                        payment.amountPaid.toString().contains(searchQuery) ||
                        (student?.fullName?.contains(searchQuery, ignoreCase = true) == true) ||
                        (student?.studentClass?.contains(searchQuery, ignoreCase = true) == true) ||
                        (student?.school?.contains(searchQuery, ignoreCase = true) == true)
                matchesMethod && matchesQuery
            }
        }
}

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
                _uiState.update {
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
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onMethodFilterChanged(method: String) {
        _uiState.update { it.copy(selectedMethodFilter = method) }
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
            _uiState.update { it.copy(userMessage = "Deleting payment records requires an active internet connection.") }
            return
        }
        val target = state.paymentToDelete ?: return
        val studentName = target.studentName.ifBlank { "Student" }

        _uiState.update { it.copy(isDeletingPayment = true) }

        viewModelScope.launch {
            val result = feeRepository.deletePayment(target.id)
            if (result.isSuccess) {
                auditLogRepository.log(
                    action = "Fee Payment Deleted",
                    details = "Deleted payment of ₹${target.amountPaid} (${target.paymentMethod}) for student: $studentName"
                )
                _uiState.update {
                    it.copy(
                        isDeletingPayment = false,
                        paymentToDelete = null,
                        userMessage = "Payment record deleted."
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
