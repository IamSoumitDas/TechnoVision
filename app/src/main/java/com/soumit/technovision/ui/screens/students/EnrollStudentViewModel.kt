package com.soumit.technovision.ui.screens.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.repository.AuditLogRepository
import com.soumit.technovision.data.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EnrollStudentUiState(
    val fullName: String = "",
    val selectedClass: String = "",
    val selectedSchool: String = "",
    val selectedGender: String = "",
    val dob: String = "",
    val admissionDate: String = "",
    val fatherName: String = "",
    val motherName: String = "",
    val monthlyInstallment: String = "500",
    val phone: String = "",
    val altPhone: String = "",
    val daySchedules: Map<String, String> = mapOf(
        "Sunday" to "Off",
        "Monday" to "Off",
        "Tuesday" to "Off",
        "Wednesday" to "Off",
        "Thursday" to "Off",
        "Friday" to "Off",
        "Saturday" to "Off"
    ),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val attemptedSubmit: Boolean = false,
    val enrolledStudentId: String? = null,
    val isOnline: Boolean = true
) {
    val activeBatchDaysCount: Int
        get() = daySchedules.values.count { it != "Off" }
}

@HiltViewModel
class EnrollStudentViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val auditLogRepository: AuditLogRepository,
    private val networkMonitor: com.soumit.technovision.util.NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnrollStudentUiState())
    val uiState: StateFlow<EnrollStudentUiState> = _uiState.asStateFlow()

    init {
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

    fun onFullNameChanged(value: String) = _uiState.update { it.copy(fullName = value, errorMessage = null) }
    fun onClassChanged(value: String) = _uiState.update { it.copy(selectedClass = value, errorMessage = null) }
    fun onSchoolChanged(value: String) = _uiState.update { it.copy(selectedSchool = value, errorMessage = null) }
    fun onGenderChanged(value: String) = _uiState.update { it.copy(selectedGender = value, errorMessage = null) }
    fun onDobChanged(value: String) = _uiState.update { it.copy(dob = value, errorMessage = null) }
    fun onAdmissionDateChanged(value: String) = _uiState.update { it.copy(admissionDate = value, errorMessage = null) }
    fun onFatherNameChanged(value: String) = _uiState.update { it.copy(fatherName = value, errorMessage = null) }
    fun onMotherNameChanged(value: String) = _uiState.update { it.copy(motherName = value, errorMessage = null) }
    fun onMonthlyInstallmentChanged(value: String) = _uiState.update { it.copy(monthlyInstallment = value, errorMessage = null) }
    fun onPhoneChanged(value: String) = _uiState.update { it.copy(phone = value, errorMessage = null) }
    fun onAltPhoneChanged(value: String) = _uiState.update { it.copy(altPhone = value, errorMessage = null) }

    fun onDayScheduleChanged(day: String, slot: String) {
        _uiState.update { state ->
            val updated = state.daySchedules.toMutableMap()
            updated[day] = slot
            state.copy(daySchedules = updated, errorMessage = null)
        }
    }

    fun clearErrorMessage() = _uiState.update { it.copy(errorMessage = null) }

    fun enrollStudent(onSuccess: (String) -> Unit) {
        val s = _uiState.value
        if (!s.isOnline) {
            _uiState.update { it.copy(errorMessage = "Student enrollment requires an active internet connection.") }
            return
        }
        _uiState.update { it.copy(attemptedSubmit = true) }

        if (s.fullName.isBlank() || s.selectedClass.isBlank() || s.selectedSchool.isBlank() ||
            s.selectedGender.isBlank() || s.dob.isBlank() || s.admissionDate.isBlank() ||
            s.fatherName.isBlank() || s.motherName.isBlank() || s.monthlyInstallment.isBlank() || s.phone.isBlank()
        ) {
            _uiState.update { it.copy(errorMessage = "Please fill in all required (*) fields highlighted in red.") }
            return
        }

        if (s.activeBatchDaysCount < 2) {
            _uiState.update { it.copy(errorMessage = "Student must have at least 2 batch days scheduled.") }
            return
        }

        val installmentValue = s.monthlyInstallment.trim().toLongOrNull()
        if (installmentValue == null || installmentValue <= 0L) {
            _uiState.update { it.copy(errorMessage = "Monthly installment must be a valid positive amount.") }
            return
        }

        val cleanPhone = s.phone.replace(Regex("\\s+"), "")
        val studentModel = StudentModel(
            fullName = s.fullName.trim(),
            studentClass = s.selectedClass.trim(),
            school = s.selectedSchool.trim(),
            dob = s.dob.trim(),
            gender = s.selectedGender.trim(),
            admissionDate = s.admissionDate.trim(),
            fatherName = s.fatherName.trim(),
            motherName = s.motherName.trim(),
            monthlyInstallment = installmentValue,
            phone = cleanPhone,
            altPhone = s.altPhone.trim(),
            weeklySchedule = s.daySchedules
        )

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = studentRepository.enrollStudent(studentModel)
            result.fold(
                onSuccess = { newId ->
                    _uiState.update { it.copy(isLoading = false, enrolledStudentId = newId) }
                    auditLogRepository.log(
                        action = "Student Enrolled",
                        details = "New student registered: ${s.fullName.trim()} (ID: $newId)"
                    )
                    onSuccess(newId)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Error enrolling student: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }
}
