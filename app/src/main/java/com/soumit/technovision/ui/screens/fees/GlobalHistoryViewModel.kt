package com.soumit.technovision.ui.screens.fees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.model.FeePaymentModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.repository.AuditLogRepository
import com.soumit.technovision.data.repository.AuthRepository
import com.soumit.technovision.data.repository.FeeRepository
import com.soumit.technovision.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
                        (student?.studentClass?.contains(searchQuery, ignoreCase = true) == true) ||
                        (student?.school?.contains(searchQuery, ignoreCase = true) == true)
                matchesMethod && matchesQuery
            }
        }
}

class GlobalHistoryViewModel(
    private val feeRepository: FeeRepository,
    private val studentRepository: StudentRepository,
    private val authRepository: AuthRepository,
    private val auditLogRepository: AuditLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalHistoryUiState())
    val uiState: StateFlow<GlobalHistoryUiState> = _uiState.asStateFlow()

    init {
        loadUserRole()
        observeData()
    }

    private fun loadUserRole() {
        viewModelScope.launch {
            val user = authRepository.currentUser
            if (user != null) {
                val roleResult = authRepository.getUserRole(user.uid)
                if (roleResult.isSuccess) {
                    _uiState.update { it.copy(currentUserRole = roleResult.getOrThrow().lowercase()) }
                }
            }
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
            }.catch { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "Error loading history: ${e.localizedMessage}"
                    )
                }
            }.collect { (payments, sMap) ->
                _uiState.update {
                    it.copy(
                        paymentHistory = payments,
                        studentsMap = sMap,
                        isLoading = false
                    )
                }
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
        val target = _uiState.value.paymentToDelete ?: return
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
