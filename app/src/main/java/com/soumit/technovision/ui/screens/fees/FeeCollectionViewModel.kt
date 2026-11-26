package com.soumit.technovision.ui.screens.fees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.soumit.technovision.data.model.FeePaymentModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.repository.AuditLogRepository
import com.soumit.technovision.data.repository.AuthRepository
import com.soumit.technovision.data.repository.FeeRepository
import com.soumit.technovision.data.repository.StaffRepository
import com.soumit.technovision.data.repository.StudentRepository
import com.soumit.technovision.util.BillingUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FeeCollectionUiState(
    val studentList: List<StudentModel> = emptyList(),
    val selectedStudent: StudentModel? = null,
    val searchQuery: String = "",
    val currentUserRole: String = "",
    val expectedTotalFees: Long = 0L,
    val totalPaid: Long = 0L,
    val amountPaid: String = "",
    val selectedMethod: String = "Cash",
    val remarks: String = "",
    val paymentDateStr: String = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()),
    val paymentHistory: List<FeePaymentModel> = emptyList(),
    val isLoadingStudents: Boolean = true,
    val isSaving: Boolean = false,
    val isDeletingPayment: Boolean = false,
    val paymentToDelete: FeePaymentModel? = null,
    val showDatePicker: Boolean = false,
    val methodExpanded: Boolean = false,
    val isOnline: Boolean = true,
    val userMessage: String? = null,
    val filteredStudents: List<StudentModel> = emptyList()
) {
    val balanceDue: Long
        get() = expectedTotalFees - totalPaid
}

@dagger.hilt.android.lifecycle.HiltViewModel
class FeeCollectionViewModel @javax.inject.Inject constructor(
    private val studentRepository: StudentRepository,
    private val feeRepository: FeeRepository,
    private val authRepository: AuthRepository,
    private val auditLogRepository: AuditLogRepository,
    private val staffRepository: StaffRepository,
    private val networkMonitor: com.soumit.technovision.util.NetworkMonitor? = null,
    private val defaultDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeeCollectionUiState())
    val uiState: StateFlow<FeeCollectionUiState> = _uiState.asStateFlow()

    private var paymentsJob: Job? = null
    private var searchJob: Job? = null
    private var isStudentsLoaded = false
    private var isRoleLoaded = false

    init {
        loadUserRole()
        loadStudents()
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
        if (isStudentsLoaded && isRoleLoaded) {
            _uiState.update { it.copy(isLoadingStudents = false) }
        }
    }

    private fun loadStudents() {
        viewModelScope.launch {
            studentRepository.getStudentsStream()
                .catch { e ->
                    isStudentsLoaded = true
                    _uiState.update {
                        it.copy(
                            isLoadingStudents = false,
                            userMessage = "Error loading students: ${e.localizedMessage}"
                        )
                    }
                }
                .collect { list ->
                    isStudentsLoaded = true
                    _uiState.update { state ->
                        val currentSelected = state.selectedStudent
                        val updatedSelected = if (currentSelected != null) {
                            list.find { it.id == currentSelected.id } ?: currentSelected
                        } else null
                        val newExpected = if (updatedSelected != null) {
                            BillingUtils.calculateBillingMonths(updatedSelected.admissionDate, updatedSelected.monthlyInstallment).second
                        } else state.expectedTotalFees
                        state.copy(
                            studentList = list,
                            selectedStudent = updatedSelected,
                            expectedTotalFees = newExpected,
                            filteredStudents = filterStudentsList(list, state.searchQuery)
                        )
                    }
                    checkReady()
                }
        }
    }

    private fun filterStudentsList(list: List<StudentModel>, query: String): List<StudentModel> {
        val q = query.trim()
        if (q.isBlank()) return list
        return list.filter {
            it.fullName.contains(q, ignoreCase = true) ||
                    it.studentClass.contains(q, ignoreCase = true) ||
                    it.school.contains(q, ignoreCase = true) ||
                    it.phone.contains(q, ignoreCase = true)
        }
    }

    fun onSelectStudent(student: StudentModel) {
        val (_, expected) = BillingUtils.calculateBillingMonths(student.admissionDate, student.monthlyInstallment)
        _uiState.update {
            it.copy(
                selectedStudent = student,
                amountPaid = student.monthlyInstallment.toString(),
                expectedTotalFees = expected,
                paymentHistory = emptyList(),
                totalPaid = 0L
            )
        }

        paymentsJob?.cancel()
        paymentsJob = viewModelScope.launch {
            feeRepository.getPaymentsForStudentStream(student.id)
                .catch { e ->
                    _uiState.update { it.copy(userMessage = "Error loading payment history: ${e.localizedMessage}") }
                }
                .collect { history ->
                    val sum = history.sumOf { it.amountPaid }
                    _uiState.update {
                        it.copy(
                            paymentHistory = history,
                            totalPaid = sum
                        )
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch(defaultDispatcher) {
            if (query.isNotBlank()) {
                kotlinx.coroutines.delay(150L)
            }
            val filtered = filterStudentsList(_uiState.value.studentList, query)
            _uiState.update { it.copy(filteredStudents = filtered) }
        }
    }

    fun onAmountChanged(amount: String) {
        _uiState.update { it.copy(amountPaid = amount) }
    }

    fun onMethodChanged(method: String) {
        _uiState.update { it.copy(selectedMethod = method, methodExpanded = false) }
    }

    fun onRemarksChanged(remarks: String) {
        _uiState.update { it.copy(remarks = remarks) }
    }

    fun onPaymentDateChanged(dateStr: String) {
        _uiState.update { it.copy(paymentDateStr = dateStr, showDatePicker = false) }
    }

    fun setDatePickerVisible(visible: Boolean) {
        _uiState.update { it.copy(showDatePicker = visible) }
    }

    fun setMethodDropdownExpanded(expanded: Boolean) {
        _uiState.update { it.copy(methodExpanded = expanded) }
    }

    fun setPaymentToDelete(payment: FeePaymentModel?) {
        _uiState.update { it.copy(paymentToDelete = payment) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun submitPayment(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!state.isOnline) {
            _uiState.update { it.copy(userMessage = "Connect to the internet to record payments") }
            return
        }
        val student = state.selectedStudent ?: return
        val amount = state.amountPaid.toLongOrNull() ?: 0L

        if (amount <= 0) {
            _uiState.update { it.copy(userMessage = "Enter a valid payment amount") }
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val cleanDateStr = BillingUtils.normalizeDigits(state.paymentDateStr.trim())
            val parsedDate = try {
                SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(cleanDateStr)
            } catch (_: Exception) {
                try {
                    SimpleDateFormat("dd-MM-yyyy", Locale.US).parse(cleanDateStr)
                } catch (_: Exception) {
                    try {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(cleanDateStr)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            val dateObj = if (parsedDate != null) {
                val cal = java.util.Calendar.getInstance().apply {
                    val targetCal = java.util.Calendar.getInstance().apply { time = parsedDate }
                    set(java.util.Calendar.YEAR, targetCal.get(java.util.Calendar.YEAR))
                    set(java.util.Calendar.MONTH, targetCal.get(java.util.Calendar.MONTH))
                    set(java.util.Calendar.DAY_OF_MONTH, targetCal.get(java.util.Calendar.DAY_OF_MONTH))
                }
                cal.time
            } else {
                Date()
            }
            val paymentTimestamp = Timestamp(dateObj)

            val currentUser = authRepository.currentUser
            val currentEmail = currentUser?.email ?: ""
            val currentUid = currentUser?.uid ?: ""
            val usersMap = staffRepository.getUsersMap().getOrDefault(emptyMap())
            val receivedBy = usersMap[currentUid]
                ?: usersMap[currentEmail.lowercase()]
                ?: usersMap[currentEmail]
                ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: currentEmail.takeIf { it.isNotBlank() }
                ?: "Receptionist"

            val payment = FeePaymentModel(
                studentId = student.id,
                studentName = student.fullName,
                amountPaid = amount,
                paymentMethod = state.selectedMethod,
                remarks = state.remarks.trim(),
                receivedBy = receivedBy,
                timestamp = paymentTimestamp
            )

            val result = feeRepository.recordPayment(payment)
            if (result.isSuccess) {
                auditLogRepository.log(
                    action = "Payment Recorded",
                    details = "Recorded payment of ₹$amount (${state.selectedMethod}) for student: ${student.fullName}"
                )
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        remarks = "",
                        amountPaid = student.monthlyInstallment.toString(),
                        userMessage = "Payment of ₹$amount recorded"
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        userMessage = "Couldn't record payment: ${result.exceptionOrNull()?.localizedMessage}"
                    )
                }
            }
        }
    }

    fun deletePayment(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!state.isOnline) {
            _uiState.update { it.copy(userMessage = "Connect to the internet to delete payment receipts") }
            return
        }
        val target = state.paymentToDelete ?: return
        val studentName = target.studentName.ifBlank { _uiState.value.selectedStudent?.fullName ?: "Student" }

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
                        userMessage = "Couldn't delete receipt: ${result.exceptionOrNull()?.localizedMessage}"
                    )
                }
            }
        }
    }
}
