package com.soumit.technovision.ui.screens.student

data class StudentModel(
    val id: String = "",
    val fullName: String = "",
    val studentClass: String = "",
    val school: String = "",
    val dob: String = "",
    val gender: String = "",
    val fatherName: String = "",
    val motherName: String = "",
    val monthlyInstallment: Long = 500L,
    val phone: String = "",
    val altPhone: String = "",
    val email: String = "",
    val batchTimeSlot: String = "",
    val weeklySchedule: Map<String, String> = emptyMap(),
    val admissionDate: String = "",
)