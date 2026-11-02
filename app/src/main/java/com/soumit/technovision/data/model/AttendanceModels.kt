package com.soumit.technovision.data.model

import com.google.firebase.Timestamp

data class AttendanceRecordModel(
    val id: String = "",
    val date: String = "",
    val timeSlot: String = "",
    val presentStudentIds: List<String> = emptyList(),
    val absentStudentIds: List<String> = emptyList(),
    val totalPresent: Int = 0,
    val totalAbsent: Int = 0,
    val timestamp: Timestamp? = null
)

data class TeacherAttendanceRequestModel(
    val id: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val date: String = "",
    val timeSlot: String = "",
    val status: String = "Pending", // "Pending", "Verified", "Rejected"
    val timestamp: Timestamp? = null
)
