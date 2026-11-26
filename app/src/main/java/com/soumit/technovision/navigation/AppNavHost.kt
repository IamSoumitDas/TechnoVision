package com.soumit.technovision.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import com.soumit.technovision.ui.screens.update.AppUpdateScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Any = Screen.Auth
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 300))
        },
        predictivePopEnterTransition = { _ ->
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 300))
        },
        predictivePopExitTransition = { _ ->
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 300))
        }
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
                            PortalAction("All Students", "Directory and profiles", Icons.Default.People) { navController.navigate(Screen.StudentDirectory) },
                            PortalAction("Take Attendance", "Mark session attendance", Icons.Default.CheckCircle) { navController.navigate(Screen.Attendance) },
                            PortalAction("Attendance History", "Past attendance sessions", Icons.AutoMirrored.Filled.Assignment) { navController.navigate(Screen.AttendanceHistory) },
                            PortalAction("Batch Schedules", "Timings and day assignments", Icons.Default.School) { navController.navigate(Screen.Batches) },
                            PortalAction("Collect Payment", "Record payments & dues", Icons.Default.Payment) { navController.navigate(Screen.FeeCollection) },
                            PortalAction("Payment History", "View payment receipts", Icons.Default.History) { navController.navigate(Screen.GlobalHistory) },
                            PortalAction("Birthday Calendar", "Upcoming student birthdays", Icons.Default.Cake) { navController.navigate(Screen.BirthdayCalendar) }
                        )
                    ),
                    PortalSectionModel(
                        title = "Staff Management",
                        actions = listOf(
                            PortalAction("Manage Staff", "Staff accounts & permissions", Icons.Default.Badge) { navController.navigate(Screen.ManageStaff) },
                            PortalAction("Verify Attendance", "Review faculty requests", Icons.AutoMirrored.Filled.FactCheck) { navController.navigate(Screen.VerifyTeacherAttendance) },
                            PortalAction("Faculty Records", "Approved faculty logs", Icons.Default.EventAvailable) { navController.navigate(Screen.FacultyAttendanceViewer) }
                        )
                    ),
                    PortalSectionModel(
                        title = "System",
                        actions = listOf(
                            PortalAction("Activity Logs", "Center activity & audit trail", Icons.Default.History) { navController.navigate(Screen.SystemLogs) },
                            PortalAction("Software Update", "Check & download releases", Icons.Default.SystemUpdate) { navController.navigate(Screen.AppUpdate) }
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
                            PortalAction("Take Attendance", "Mark session attendance", Icons.Default.CheckCircle) { navController.navigate(Screen.Attendance) },
                            PortalAction("Attendance History", "Past attendance sessions", Icons.AutoMirrored.Filled.Assignment) { navController.navigate(Screen.AttendanceHistory) },
                            PortalAction("All Students", "Directory and profiles", Icons.Default.People) { navController.navigate(Screen.StudentDirectory) },
                            PortalAction("Batch Schedules", "Timings and day assignments", Icons.Default.School) { navController.navigate(Screen.Batches) },
                            PortalAction("Birthday Calendar", "Upcoming student birthdays", Icons.Default.Cake) { navController.navigate(Screen.BirthdayCalendar) }
                        )
                    ),
                    PortalSectionModel(
                        title = "Faculty Management",
                        actions = listOf(
                            PortalAction("Faculty Attendance", "Submit attendance request", Icons.AutoMirrored.Filled.Assignment) { navController.navigate(Screen.TeacherAttendance) },
                            PortalAction("Faculty Records", "Approved faculty logs", Icons.Default.EventAvailable) { navController.navigate(Screen.FacultyAttendanceViewer) }
                        )
                    ),
                    PortalSectionModel(
                        title = "System",
                        actions = listOf(
                            PortalAction("Activity Logs", "Center activity & audit trail", Icons.Default.History) { navController.navigate(Screen.SystemLogs) },
                            PortalAction("Software Update", "Check & download releases", Icons.Default.SystemUpdate) { navController.navigate(Screen.AppUpdate) }
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
                            PortalAction("All Students", "Directory and profiles", Icons.Default.People) { navController.navigate(Screen.StudentDirectory) },
                            PortalAction("Attendance History", "Past attendance sessions", Icons.AutoMirrored.Filled.Assignment) { navController.navigate(Screen.AttendanceHistory) },
                            PortalAction("Batch Schedules", "Timings and day assignments", Icons.Default.School) { navController.navigate(Screen.Batches) },
                            PortalAction("Collect Payment", "Record payments & dues", Icons.Default.Payment) { navController.navigate(Screen.FeeCollection) },
                            PortalAction("Payment History", "View payment receipts", Icons.Default.History) { navController.navigate(Screen.GlobalHistory) },
                            PortalAction("Birthday Calendar", "Upcoming student birthdays", Icons.Default.Cake) { navController.navigate(Screen.BirthdayCalendar) }
                        )
                    ),
                    PortalSectionModel(
                        title = "Staff Management",
                        actions = listOf(
                            PortalAction("Verify Attendance", "Review faculty requests", Icons.AutoMirrored.Filled.FactCheck) { navController.navigate(Screen.VerifyTeacherAttendance) },
                            PortalAction("Faculty Records", "Approved faculty logs", Icons.Default.EventAvailable) { navController.navigate(Screen.FacultyAttendanceViewer) }
                        )
                    ),
                    PortalSectionModel(
                        title = "System",
                        actions = listOf(
                            PortalAction("Activity Logs", "Center activity & audit trail", Icons.Default.History) { navController.navigate(Screen.SystemLogs) },
                            PortalAction("Software Update", "Check & download releases", Icons.Default.SystemUpdate) { navController.navigate(Screen.AppUpdate) }
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

        composable<Screen.AppUpdate> {
            AppUpdateScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
