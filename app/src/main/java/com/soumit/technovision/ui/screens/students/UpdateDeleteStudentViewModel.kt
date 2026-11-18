package com.soumit.technovision.ui.screens.students

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.repository.AuditLogRepository
import com.soumit.technovision.data.repository.StudentRepository
import com.soumit.technovision.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdateDeleteStudentUiState(
    val studentId: String = "",
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
    val email: String = "",
    val batchTimeSlot: String = "",
    val daySchedules: Map<String, String> = mapOf(
        "Sunday" to "Off",
        "Monday" to "Off",
        "Tuesday" to "Off",
        "Wednesday" to "Off",
        "Thursday" to "Off",
        "Friday" to "Off",
        "Saturday" to "Off"
    ),
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val userMessage: String? = null
) {
    val activeDaysCount: Int
        get() = daySchedules.values.count { it != "Off" }
}

@HiltViewModel
class UpdateDeleteStudentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val studentRepository: StudentRepository,
    private val auditLogRepository: AuditLogRepository
) : ViewModel() {

    private val studentId: String = savedStateHandle.get<String>("studentId")
        ?: runCatching { savedStateHandle.toRoute<Screen.UpdateDeleteStudent>().studentId }.getOrDefault("")

    private val _uiState = MutableStateFlow(UpdateDeleteStudentUiState(studentId = studentId))
    val uiState: StateFlow<UpdateDeleteStudentUiState> = _uiState.asStateFlow()

    init {
        loadStudent()
    }

    private fun loadStudent() {
        if (studentId.isBlank()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            val result = studentRepository.getStudent(studentId)
            result.fold(
                onSuccess = { student ->
                    val defaultSchedules = mapOf(
                        "Sunday" to "Off", "Monday" to "Off", "Tuesday" to "Off",
                        "Wednesday" to "Off", "Thursday" to "Off", "Friday" to "Off", "Saturday" to "Off"
                    ).toMutableMap()
                    defaultSchedules.putAll(student.weeklySchedule)

                    _uiState.update {
                        it.copy(
                            fullName = student.fullName,
                            selectedClass = student.studentClass,
                            selectedSchool = student.school,
                            selectedGender = student.gender,
                            dob = student.dob,
                            admissionDate = student.admissionDate,
                            fatherName = student.fatherName,
                            motherName = student.motherName,
                            monthlyInstallment = student.monthlyInstallment.toString(),
                            phone = student.phone,
                            altPhone = student.altPhone,
                            email = student.email,
                            batchTimeSlot = student.batchTimeSlot,
                            daySchedules = defaultSchedules,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load student: ${error.localizedMessage}"
                        )
                    }
                }
            )
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

    fun clearMessages() = _uiState.update { it.copy(errorMessage = null, userMessage = null) }

    fun updateStudent(onSuccess: () -> Unit) {
        val s = _uiState.value

        if (s.fullName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Full name cannot be blank.") }
            return
        }

        val installmentVal = s.monthlyInstallment.trim().toLongOrNull()
        if (installmentVal == null || installmentVal <= 0L) {
            _uiState.update { it.copy(errorMessage = "Monthly installment must be a valid positive amount.") }
            return
        }

        val studentModel = StudentModel(
            id = studentId,
            fullName = s.fullName.trim(),
            studentClass = s.selectedClass,
            school = s.selectedSchool,
            dob = s.dob,
            gender = s.selectedGender,
            admissionDate = s.admissionDate,
            fatherName = s.fatherName.trim(),
            motherName = s.motherName.trim(),
            monthlyInstallment = installmentVal,
            weeklySchedule = s.daySchedules,
            phone = s.phone.trim(),
            altPhone = s.altPhone.trim(),
            email = s.email,
            batchTimeSlot = s.batchTimeSlot
        )

        _uiState.update { it.copy(isUpdating = true, errorMessage = null) }

        viewModelScope.launch {
            val result = studentRepository.updateStudent(studentModel)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isUpdating = false, userMessage = "Student updated successfully!") }
                    auditLogRepository.log(
                        action = "Student Profile Updated",
                        details = "Updated profile record for student: ${s.fullName.trim()} (ID: $studentId)"
                    )
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isUpdating = false, errorMessage = "Update failed: ${error.localizedMessage}")
                    }
                }
            )
        }
    }

    fun deleteStudent(onSuccess: () -> Unit) {
        val s = _uiState.value
        _uiState.update { it.copy(isDeleting = true, errorMessage = null) }

        viewModelScope.launch {
            val result = studentRepository.deleteStudent(studentId, s.fullName)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isDeleting = false, userMessage = "Student deleted successfully.") }
                    auditLogRepository.log(
                        action = "Student Deleted",
                        details = "Deleted student record (ID: $studentId, Name: ${s.fullName})"
                    )
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isDeleting = false, errorMessage = "Delete failed: ${error.localizedMessage}")
                    }
                }
            )
        }
    }
}
