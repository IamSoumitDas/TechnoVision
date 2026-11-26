package com.soumit.technovision.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations matching Google's recommended standard for Jetpack Navigation Compose.
 */
sealed interface Screen {
    @Serializable
    data object Auth : Screen

    @Serializable
    data object OwnerHome : Screen

    @Serializable
    data object FacultyHome : Screen

    @Serializable
    data object ReceptionistHome : Screen

    @Serializable
    data object EnrollStudent : Screen

    @Serializable
    data class StudentViewer(val studentId: String) : Screen

    @Serializable
    data object ManageStaff : Screen

    @Serializable
    data object Attendance : Screen

    @Serializable
    data object AttendanceHistory : Screen

    @Serializable
    data object StudentDirectory : Screen

    @Serializable
    data class UpdateDeleteStudent(val studentId: String) : Screen

    @Serializable
    data object Batches : Screen

    @Serializable
    data object SystemLogs : Screen

    @Serializable
    data object TeacherAttendance : Screen

    @Serializable
    data object VerifyTeacherAttendance : Screen

    @Serializable
    data object FeeCollection : Screen

    @Serializable
    data object GlobalHistory : Screen

    @Serializable
    data object FacultyAttendanceViewer : Screen

    @Serializable
    data object BirthdayCalendar : Screen

    @Serializable
    data object AppUpdate : Screen
}

object Routes {
    const val AUTH = "auth"
    const val OWNER_HOME = "owner_home"
    const val FACULTY_HOME = "faculty_home"
    const val RECEPTIONIST_HOME = "receptionist_home"
    const val ENROLL_STUDENT = "enroll_student"
    const val STUDENT_VIEWER = "student_viewer/{studentId}"
    const val MANAGE_STAFF = "manage_staff"
    const val ATTENDANCE = "attendance"
    const val ATTENDANCE_HISTORY = "attendance_history"
    const val STUDENT_DIRECTORY = "student_directory"
    const val UPDATE_DELETE_STUDENT = "update_delete_student/{studentId}"
    const val BATCHES = "batches"
    const val SYSTEM_LOGS = "system_logs"
    const val TEACHER_ATTENDANCE = "teacher_attendance"
    const val VERIFY_TEACHER_ATTENDANCE = "verify_teacher_attendance"
    const val FEE_COLLECTION = "fee_collection"
    const val GLOBAL_HISTORY = "global_history"
    const val FACULTY_ATTENDANCE_VIEWER = "faculty_attendance_viewer"
    const val BIRTHDAY_CALENDAR = "birthday_calendar"

    fun studentViewer(studentId: String) = "student_viewer/$studentId"
    fun updateDeleteStudent(studentId: String) = "update_delete_student/$studentId"
}
