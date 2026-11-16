package com.soumit.technovision.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.soumit.technovision.data.model.PortalAction
import com.soumit.technovision.data.model.PortalSectionModel
import com.soumit.technovision.ui.screens.attendance.AttendanceHistoryScreen
import com.soumit.technovision.ui.screens.attendance.FacultyAttendanceViewerScreen
import com.soumit.technovision.ui.screens.attendance.TakeAttendanceScreen
import com.soumit.technovision.ui.screens.attendance.TeacherAttendanceScreen
import com.soumit.technovision.ui.screens.attendance.VerifyTeacherAttendanceScreen
import com.soumit.technovision.ui.screens.auth.AuthScreen
import com.soumit.technovision.ui.screens.batches.BatchesScreen
import com.soumit.technovision.ui.screens.dashboard.PortalDashboardScreen
import com.soumit.technovision.ui.screens.fees.FeeCollectionScreen
import com.soumit.technovision.ui.screens.fees.GlobalHistoryScreen
import com.soumit.technovision.ui.screens.logs.SystemLogsScreen
import com.soumit.technovision.ui.screens.staff.ManageStaffScreen
import com.soumit.technovision.ui.screens.students.EnrollStudentScreen
import com.soumit.technovision.ui.screens.students.StudentDirectoryScreen
import com.soumit.technovision.ui.screens.students.StudentViewerScreen
import com.soumit.technovision.ui.screens.students.UpdateDeleteStudentScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Auth Screen
        composable(Routes.AUTH) {
            AuthScreen(
                onLoginSuccess = { userRole ->
                    val destination = when (userRole.lowercase()) {
                        "owner" -> Routes.OWNER_HOME
                        "faculty" -> Routes.FACULTY_HOME
                        "receptionist" -> Routes.RECEPTIONIST_HOME
                        else -> Routes.AUTH
                    }
                    navController.navigate(destination) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        // 1. Owner Portal Route
        composable(Routes.OWNER_HOME) {
            PortalDashboardScreen(
                portalTitle = "Owner Portal",
                roleSubtitle = "TechnoVision Administration",
                sections = listOf(
                    PortalSectionModel(
                        title = "Student Management",
                        actions = listOf(
                            PortalAction("Add Student", "Register a new student", Icons.Default.AppRegistration) { navController.navigate(Routes.ENROLL_STUDENT) },
                            PortalAction("All Students", "List of all the students", Icons.Default.People) { navController.navigate(Routes.STUDENT_DIRECTORY) },
                            PortalAction("Take Attendance", "Mark daily student presence", Icons.Default.CheckCircle) { navController.navigate(Routes.ATTENDANCE) },
                            PortalAction("Attendance History", "View past session records", Icons.AutoMirrored.Filled.Assignment) { navController.navigate(Routes.ATTENDANCE_HISTORY) },
                            PortalAction("Class Timings", "Set batches & schedules", Icons.Default.School) { navController.navigate(Routes.BATCHES) },
                            PortalAction("Collect Payment", "Collect fees & view ledger", Icons.Default.Payment) { navController.navigate(Routes.FEE_COLLECTION) },
                            PortalAction("Payment History", "View all payment records", Icons.Default.History) { navController.navigate(Routes.GLOBAL_HISTORY) }
                        )
                    ),
                    PortalSectionModel(
                        title = "Staff Management",
                        actions = listOf(
                            PortalAction("Manage Staff", "Add, edit & remove staffs", Icons.Default.Badge) { navController.navigate(Routes.MANAGE_STAFF) },
                            PortalAction("Verify Attendance", "Approve teacher attendance", Icons.AutoMirrored.Filled.FactCheck) { navController.navigate(Routes.VERIFY_TEACHER_ATTENDANCE) },
                            PortalAction("Attendance Records", "View verified faculty logs", Icons.Default.EventAvailable) { navController.navigate(Routes.FACULTY_ATTENDANCE_VIEWER) }
                        )
                    ),
                    PortalSectionModel(
                        title = "System",
                        actions = listOf(
                            PortalAction("Activity History", "View center logs & events", Icons.Default.History) { navController.navigate(Routes.SYSTEM_LOGS) }
                        )
                    )
                )
            )
        }

        // 2. Faculty Portal Route
        composable(Routes.FACULTY_HOME) {
            PortalDashboardScreen(
                portalTitle = "Faculty Portal",
                roleSubtitle = "TechnoVision Teaching Staff",
                sections = listOf(
                    PortalSectionModel(
                        title = "Student Management",
                        actions = listOf(
                            PortalAction("Take Attendance", "Mark daily student presence", Icons.Default.CheckCircle) { navController.navigate(Routes.ATTENDANCE) },
                            PortalAction("Attendance History", "View past session records", Icons.AutoMirrored.Filled.Assignment) { navController.navigate(Routes.ATTENDANCE_HISTORY) },
                            PortalAction("All Students", "List of all the students", Icons.Default.People) { navController.navigate(Routes.STUDENT_DIRECTORY) },
                            PortalAction("Class Timings", "Set batches & schedules", Icons.Default.School) { navController.navigate(Routes.BATCHES) }
                        )
                    ),
                    PortalSectionModel(
                        title = "Teacher Management",
                        actions = listOf(
                            PortalAction("Faculty Attendance", "Request attendance", Icons.AutoMirrored.Filled.Assignment) { navController.navigate(Routes.TEACHER_ATTENDANCE) },
                            PortalAction("Attendance Records", "View verified faculty logs", Icons.Default.EventAvailable) { navController.navigate(Routes.FACULTY_ATTENDANCE_VIEWER) }
                        )
                    ),
                    PortalSectionModel(
                        title = "System",
                        actions = listOf(
                            PortalAction("Activity History", "View center logs & events", Icons.Default.History) { navController.navigate(Routes.SYSTEM_LOGS) }
                        )
                    )
                )
            )
        }

        // 3. Receptionist Portal Route
        composable(Routes.RECEPTIONIST_HOME) {
            PortalDashboardScreen(
                portalTitle = "Receptionist Portal",
                roleSubtitle = "TechnoVision Front Desk",
                sections = listOf(
                    PortalSectionModel(
                        title = "Student Management",
                        actions = listOf(
                            PortalAction("Add Student", "Register a new student", Icons.Default.AppRegistration) { navController.navigate(Routes.ENROLL_STUDENT) },
                            PortalAction("All Students", "List of all the students", Icons.Default.People) { navController.navigate(Routes.STUDENT_DIRECTORY) },
                            PortalAction("Attendance History", "View past session records", Icons.AutoMirrored.Filled.Assignment) { navController.navigate(Routes.ATTENDANCE_HISTORY) },
                            PortalAction("Class Timings", "Set batches & schedules", Icons.Default.School) { navController.navigate(Routes.BATCHES) },
                            PortalAction("Collect Payment", "Collect fees & view ledger", Icons.Default.Payment) { navController.navigate(Routes.FEE_COLLECTION) },
                            PortalAction("Payment History", "View all payment records", Icons.Default.History) { navController.navigate(Routes.GLOBAL_HISTORY) }
                        )
                    ),
                    PortalSectionModel(
                        title = "Staff Management",
                        actions = listOf(
                            PortalAction("Verify Attendance", "Approve teacher attendance", Icons.AutoMirrored.Filled.FactCheck) { navController.navigate(Routes.VERIFY_TEACHER_ATTENDANCE) },
                            PortalAction("Attendance Records", "View verified faculty logs", Icons.Default.EventAvailable) { navController.navigate(Routes.FACULTY_ATTENDANCE_VIEWER) }
                        )
                    ),
                    PortalSectionModel(
                        title = "System",
                        actions = listOf(
                            PortalAction("Activity History", "View center logs & events", Icons.Default.History) { navController.navigate(Routes.SYSTEM_LOGS) }
                        )
                    )
                )
            )
        }

        // Sub-screens & Functional Routes
        composable(Routes.ENROLL_STUDENT) {
            EnrollStudentScreen(
                onBack = { navController.popBackStack() },
                onStudentEnrolled = { studentId ->
                    navController.navigate(Routes.studentViewer(studentId)) {
                        popUpTo(Routes.ENROLL_STUDENT) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.STUDENT_VIEWER) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            StudentViewerScreen(
                studentId = studentId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MANAGE_STAFF) {
            ManageStaffScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ATTENDANCE) {
            TakeAttendanceScreen(
                onBack = { navController.popBackStack() },
                onNavigateToHistory = { navController.navigate(Routes.ATTENDANCE_HISTORY) }
            )
        }

        composable(Routes.ATTENDANCE_HISTORY) {
            AttendanceHistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.STUDENT_DIRECTORY) {
            StudentDirectoryScreen(
                onBack = { navController.popBackStack() },
                onNavigateToUpdateDelete = { studentId ->
                    navController.navigate(Routes.updateDeleteStudent(studentId))
                },
                onNavigateToViewer = { studentId ->
                    navController.navigate(Routes.studentViewer(studentId))
                }
            )
        }

        composable(Routes.UPDATE_DELETE_STUDENT) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            UpdateDeleteStudentScreen(
                studentId = studentId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.BATCHES) {
            BatchesScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SYSTEM_LOGS) {
            SystemLogsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.TEACHER_ATTENDANCE) {
            TeacherAttendanceScreen(
                onBack = { navController.popBackStack() },
                onNavigateToViewer = { navController.navigate(Routes.FACULTY_ATTENDANCE_VIEWER) }
            )
        }

        composable(Routes.VERIFY_TEACHER_ATTENDANCE) {
            VerifyTeacherAttendanceScreen(
                onBack = { navController.popBackStack() },
                onNavigateToViewer = { navController.navigate(Routes.FACULTY_ATTENDANCE_VIEWER) }
            )
        }

        composable(Routes.FACULTY_ATTENDANCE_VIEWER) {
            FacultyAttendanceViewerScreen(
                onBack = { navController.popBackStack() },
                onNavigateToVerify = { navController.navigate(Routes.VERIFY_TEACHER_ATTENDANCE) }
            )
        }

        composable(Routes.FEE_COLLECTION) {
            FeeCollectionScreen(
                onBack = { navController.popBackStack() },
                onNavigateToGlobalHistory = { navController.navigate(Routes.GLOBAL_HISTORY) }
            )
        }

        composable(Routes.GLOBAL_HISTORY) {
            GlobalHistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}
