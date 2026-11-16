package com.soumit.technovision.navigation

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
