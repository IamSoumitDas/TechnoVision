package com.soumit.technovision.ui.common

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.soumit.technovision.TechnoVisionApplication
import com.soumit.technovision.di.AppContainer
import com.soumit.technovision.di.DefaultAppContainer

import com.soumit.technovision.MainViewModel
import com.soumit.technovision.ui.screens.attendance.AttendanceHistoryViewModel
import com.soumit.technovision.ui.screens.attendance.TakeAttendanceViewModel
import com.soumit.technovision.ui.screens.auth.AuthViewModel
import com.soumit.technovision.ui.screens.fees.FeeCollectionViewModel
import com.soumit.technovision.ui.screens.fees.GlobalHistoryViewModel
import com.soumit.technovision.ui.screens.students.StudentDirectoryViewModel
import com.soumit.technovision.ui.screens.students.StudentViewerViewModel

import com.soumit.technovision.ui.screens.attendance.FacultyAttendanceViewerViewModel
import com.soumit.technovision.ui.screens.attendance.TeacherAttendanceViewModel
import com.soumit.technovision.ui.screens.attendance.VerifyTeacherAttendanceViewModel

object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            MainViewModel(
                authRepository = appContainer().authRepository
            )
        }
        initializer {
            AuthViewModel(
                authRepository = appContainer().authRepository
            )
        }
        initializer {
            TakeAttendanceViewModel(
                attendanceRepository = appContainer().attendanceRepository,
                studentRepository = appContainer().studentRepository,
                staffRepository = appContainer().staffRepository,
                authRepository = appContainer().authRepository,
                auditLogRepository = appContainer().auditLogRepository
            )
        }
        initializer {
            AttendanceHistoryViewModel(
                attendanceRepository = appContainer().attendanceRepository,
                studentRepository = appContainer().studentRepository,
                staffRepository = appContainer().staffRepository
            )
        }
        initializer {
            TeacherAttendanceViewModel(
                attendanceRepository = appContainer().attendanceRepository,
                authRepository = appContainer().authRepository,
                staffRepository = appContainer().staffRepository,
                auditLogRepository = appContainer().auditLogRepository
            )
        }
        initializer {
            VerifyTeacherAttendanceViewModel(
                attendanceRepository = appContainer().attendanceRepository,
                staffRepository = appContainer().staffRepository,
                authRepository = appContainer().authRepository,
                auditLogRepository = appContainer().auditLogRepository
            )
        }
        initializer {
            FacultyAttendanceViewerViewModel(
                attendanceRepository = appContainer().attendanceRepository,
                staffRepository = appContainer().staffRepository
            )
        }
        initializer {
            FeeCollectionViewModel(
                studentRepository = appContainer().studentRepository,
                feeRepository = appContainer().feeRepository,
                authRepository = appContainer().authRepository,
                auditLogRepository = appContainer().auditLogRepository,
                staffRepository = appContainer().staffRepository
            )
        }
        initializer {
            GlobalHistoryViewModel(
                feeRepository = appContainer().feeRepository,
                studentRepository = appContainer().studentRepository,
                authRepository = appContainer().authRepository,
                auditLogRepository = appContainer().auditLogRepository
            )
        }
        initializer {
            StudentDirectoryViewModel(
                studentRepository = appContainer().studentRepository,
                feeRepository = appContainer().feeRepository,
                authRepository = appContainer().authRepository,
                calculateBillingDuesUseCase = appContainer().calculateBillingDuesUseCase
            )
        }
        initializer {
            com.soumit.technovision.ui.screens.students.EnrollStudentViewModel(
                studentRepository = appContainer().studentRepository,
                auditLogRepository = appContainer().auditLogRepository
            )
        }
        initializer {
            com.soumit.technovision.ui.screens.staff.ManageStaffViewModel(
                staffRepository = appContainer().staffRepository,
                auditLogRepository = appContainer().auditLogRepository
            )
        }
        initializer {
            com.soumit.technovision.ui.screens.batches.BatchesViewModel(
                studentRepository = appContainer().studentRepository
            )
        }
        initializer {
            com.soumit.technovision.ui.screens.logs.SystemLogsViewModel(
                auditLogRepository = appContainer().auditLogRepository,
                staffRepository = appContainer().staffRepository
            )
        }
        initializer {
            com.soumit.technovision.ui.screens.dashboard.PortalDashboardViewModel(
                authRepository = appContainer().authRepository
            )
        }
        initializer {
            com.soumit.technovision.ui.screens.students.BirthdayCalendarViewModel(
                studentRepository = appContainer().studentRepository
            )
        }
    }

    fun studentViewerFactory(studentId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            StudentViewerViewModel(
                studentId = studentId,
                studentRepository = appContainer().studentRepository,
                feeRepository = appContainer().feeRepository
            )
        }
    }

    fun updateDeleteStudentFactory(studentId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            com.soumit.technovision.ui.screens.students.UpdateDeleteStudentViewModel(
                studentId = studentId,
                studentRepository = appContainer().studentRepository,
                auditLogRepository = appContainer().auditLogRepository
            )
        }
    }
}

fun CreationExtras.technoVisionApplication(): TechnoVisionApplication {
    return (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as? TechnoVisionApplication)
        ?: TechnoVisionApplication.instance
        ?: throw IllegalStateException("TechnoVisionApplication is not initialized")
}

fun CreationExtras.appContainer(): AppContainer {
    return technoVisionApplication().container
}
