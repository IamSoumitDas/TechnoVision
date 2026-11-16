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
import androidx.navigation.toRoute
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
import com.soumit.technovision.ui.screens.students.BirthdayCalendarScreen
import com.soumit.technovision.ui.screens.students.EnrollStudentScreen
import com.soumit.technovision.ui.screens.students.StudentDirectoryScreen
import com.soumit.technovision.ui.screens.students.StudentViewerScreen
import com.soumit.technovision.ui.screens.students.UpdateDeleteStudentScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Any = Screen.Auth
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Auth Screen
        composable<Screen.Auth> {
            AuthScreen(
                onLoginSuccess = { userRole ->
                    val destination: Any = when (userRole.lowercase()) {
                        "owner" -> Screen.OwnerHome
                        "faculty" -> Screen.FacultyHome
                        "receptionist" -> Screen.ReceptionistHome
                        else -> Screen.Auth
                    }
                    navController.navigate(destination) {
                        popUpTo<Screen.Auth> { inclusive = true }
                    }
                }
            )
        }

        // 1. Owner Portal Route
        composable<Screen.OwnerHome> {
            PortalDashboardScreen(
                portalTitle = "Owner Portal",
                roleSubtitle = "Techno Vision Portal",
                sections = listOf(
                    PortalSectionModel(
                        title = "Student Management",
                        actions = listOf(
                            PortalAction("Add Student", "Register a new student", Icons.Default.AppRegistration) { navController.navigate(Screen.EnrollStudent) },
                            PortalAction("All Students", "List of all the students", Icons.Default.People) { navController.navigate(Screen.StudentDirectory) },
                            PortalAction("Take Attendance", "Mark daily student presence", Icons.Default.CheckCircle) { navController.navigate(Screen.Attendance) },
                            PortalAction("Attendance History", "View past session records", Icons.AutoMirrored.Filled.Assignment) { navController.navigate(Screen.AttendanceHistory) },
                            PortalAction("Class Timings", "Set batches & schedules", Icons.Default.School) { navController.navigate(Screen.Batches) },
                            PortalAction("Collect Payment", "Collect fees & view ledger", Icons.Default.Payment) { navController.navigate(Screen.FeeCollection) },
                            PortalAction("Payment History", "View all payment records", Icons.Default.History) { navController.navigate(Screen.GlobalHistory) },
                            PortalAction("Birthday Calendar", "View student birthdays & wishes", Icons.Default.Cake) { navController.navigate(Screen.BirthdayCalendar) }
                        )
                    ),
                    PortalSectionModel(
                        title = "Staff Management",
                        actions = listOf(
                            PortalAction("Manage Staff", "Add, edit & remove staffs", Icons.Default.Badge) { navController.navigate(Screen.ManageStaff) },
                            PortalAction("Verify Attendance", "Approve teacher attendance", Icons.AutoMirrored.Filled.FactCheck) { navController.navigate(Screen.VerifyTeacherAttendance) },
                            PortalAction("Attendance Records", "View verified faculty logs", Icons.Default.EventAvailable) { navController.navigate(Screen.FacultyAttendanceViewer) }
                        )
                    ),
                    PortalSectionModel(
                        title = "System",
                        actions = listOf(
                            PortalAction("Activity History", "View center logs & events", Icons.Default.History) { navController.navigate(Screen.SystemLogs) }
                        )
                    )
                )
            )
        }

        // 2. Faculty Portal Route
        composable<Screen.FacultyHome> {
            PortalDashboardScreen(
                portalTitle = "Faculty Portal",
                roleSubtitle = "Techno Vision Portal",
                sections = listOf(
                    PortalSectionModel(
                        title = "Student Management",
                        actions = listOf(
                            PortalAction("Take Attendance", "Mark daily student presence", Icons.Default.CheckCircle) { navController.navigate(Screen.Attendance) },
                            PortalAction("Attendance History", "View past session records", Icons.AutoMirrored.Filled.Assignment) { navController.navigate(Screen.AttendanceHistory) },
                            PortalAction("All Students", "List of all the students", Icons.Default.People) { navController.navigate(Screen.StudentDirectory) },
                            PortalAction("Class Timings", "Set batches & schedules", Icons.Default.School) { navController.navigate(Screen.Batches) },
                            PortalAction("Birthday Calendar", "View student birthdays & wishes", Icons.Default.Cake) { navController.navigate(Screen.BirthdayCalendar) }
                        )
                    ),
                    PortalSectionModel(
                        title = "Teacher Management",
                        actions = listOf(
                            PortalAction("Faculty Attendance", "Request attendance", Icons.AutoMirrored.Filled.Assignment) { navController.navigate(Screen.TeacherAttendance) },
                            PortalAction("Attendance Records", "View verified faculty logs", Icons.Default.EventAvailable) { navController.navigate(Screen.FacultyAttendanceViewer) }
                        )
                    ),
                    PortalSectionModel(
                        title = "System",
                        actions = listOf(
                            PortalAction("Activity History", "View center logs & events", Icons.Default.History) { navController.navigate(Screen.SystemLogs) }
                        )
                    )
                )
            )
        }

        // 3. Receptionist Portal Route
        composable<Screen.ReceptionistHome> {
            PortalDashboardScreen(
                portalTitle = "Receptionist Portal",
                roleSubtitle = "Techno Vision Portal",
                sections = listOf(
                    PortalSectionModel(
                        title = "Student Management",
                        actions = listOf(
                            PortalAction("Add Student", "Register a new student", Icons.Default.AppRegistration) { navController.navigate(Screen.EnrollStudent) },
                            PortalAction("All Students", "List of all the students", Icons.Default.People) { navController.navigate(Screen.StudentDirectory) },
                            PortalAction("Attendance History", "View past session records", Icons.AutoMirrored.Filled.Assignment) { navController.navigate(Screen.AttendanceHistory) },
                            PortalAction("Class Timings", "Set batches & schedules", Icons.Default.School) { navController.navigate(Screen.Batches) },
                            PortalAction("Collect Payment", "Collect fees & view ledger", Icons.Default.Payment) { navController.navigate(Screen.FeeCollection) },
                            PortalAction("Payment History", "View all payment records", Icons.Default.History) { navController.navigate(Screen.GlobalHistory) },
                            PortalAction("Birthday Calendar", "View student birthdays & wishes", Icons.Default.Cake) { navController.navigate(Screen.BirthdayCalendar) }
                        )
                    ),
                    PortalSectionModel(
                        title = "Staff Management",
                        actions = listOf(
                            PortalAction("Verify Attendance", "Approve teacher attendance", Icons.AutoMirrored.Filled.FactCheck) { navController.navigate(Screen.VerifyTeacherAttendance) },
                            PortalAction("Attendance Records", "View verified faculty logs", Icons.Default.EventAvailable) { navController.navigate(Screen.FacultyAttendanceViewer) }
                        )
                    ),
                    PortalSectionModel(
                        title = "System",
                        actions = listOf(
                            PortalAction("Activity History", "View center logs & events", Icons.Default.History) { navController.navigate(Screen.SystemLogs) }
                        )
                    )
                )
            )
        }

        // Sub-screens & Functional Routes
        composable<Screen.EnrollStudent> {
            EnrollStudentScreen(
                onBack = { navController.popBackStack() },
                onStudentEnrolled = { studentId ->
                    navController.navigate(Screen.StudentViewer(studentId)) {
                        popUpTo<Screen.EnrollStudent> { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.StudentViewer> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.StudentViewer>()
            StudentViewerScreen(
                studentId = route.studentId,
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.ManageStaff> {
            ManageStaffScreen(onBack = { navController.popBackStack() })
        }

        composable<Screen.Attendance> {
            TakeAttendanceScreen(
                onBack = { navController.popBackStack() },
                onNavigateToHistory = { navController.navigate(Screen.AttendanceHistory) }
            )
        }

        composable<Screen.AttendanceHistory> {
            AttendanceHistoryScreen(onBack = { navController.popBackStack() })
        }

        composable<Screen.StudentDirectory> {
            StudentDirectoryScreen(
                onBack = { navController.popBackStack() },
                onNavigateToUpdateDelete = { studentId ->
                    navController.navigate(Screen.UpdateDeleteStudent(studentId))
                },
                onNavigateToViewer = { studentId ->
                    navController.navigate(Screen.StudentViewer(studentId))
                }
            )
        }

        composable<Screen.UpdateDeleteStudent> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.UpdateDeleteStudent>()
            UpdateDeleteStudentScreen(
                studentId = route.studentId,
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Batches> {
            BatchesScreen(onBack = { navController.popBackStack() })
        }

        composable<Screen.SystemLogs> {
            SystemLogsScreen(onBack = { navController.popBackStack() })
        }

        composable<Screen.TeacherAttendance> {
            TeacherAttendanceScreen(
                onBack = { navController.popBackStack() },
                onNavigateToViewer = { navController.navigate(Screen.FacultyAttendanceViewer) }
            )
        }

        composable<Screen.VerifyTeacherAttendance> {
            VerifyTeacherAttendanceScreen(
                onBack = { navController.popBackStack() },
                onNavigateToViewer = { navController.navigate(Screen.FacultyAttendanceViewer) }
            )
        }

        composable<Screen.FacultyAttendanceViewer> {
            FacultyAttendanceViewerScreen(
                onBack = { navController.popBackStack() },
                onNavigateToVerify = { navController.navigate(Screen.VerifyTeacherAttendance) }
            )
        }

        composable<Screen.FeeCollection> {
            FeeCollectionScreen(
                onBack = { navController.popBackStack() },
                onNavigateToGlobalHistory = { navController.navigate(Screen.GlobalHistory) }
            )
        }

        composable<Screen.GlobalHistory> {
            GlobalHistoryScreen(onBack = { navController.popBackStack() })
        }

        composable<Screen.BirthdayCalendar> {
            BirthdayCalendarScreen(
                onBack = { navController.popBackStack() },
                onStudentClick = { studentId ->
                    navController.navigate(Screen.StudentViewer(studentId))
                }
            )
        }
    }
}
