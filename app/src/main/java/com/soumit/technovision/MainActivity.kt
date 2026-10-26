package com.soumit.technovision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.soumit.technovision.ui.screens.*
import com.soumit.technovision.ui.screens.auth.AuthScreen
import com.soumit.technovision.ui.screens.faculty.TeacherAttendanceScreen
import com.soumit.technovision.ui.screens.owner.ManageStaffScreen
import com.soumit.technovision.ui.screens.student.StudentViewerScreen
import com.soumit.technovision.ui.theme.TechnoVisionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TechnoVisionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val auth = FirebaseAuth.getInstance()
                    val currentUser = auth.currentUser

                    var startDestination by remember { mutableStateOf<String?>(null) }
                    var isLoadingRole by remember { mutableStateOf(currentUser != null) }

                    LaunchedEffect(currentUser) {
                        if (currentUser != null) {
                            val userId = currentUser.uid
                            FirebaseFirestore.getInstance().collection("users").document(userId).get()
                                .addOnSuccessListener { document ->
                                    val role = document.getString("role") ?: "auth"
                                    startDestination = when (role.lowercase()) {
                                        "owner" -> "owner_home"
                                        "faculty" -> "faculty_home"
                                        "receptionist" -> "receptionist_home"
                                        else -> "auth"
                                    }
                                    isLoadingRole = false
                                }
                                .addOnFailureListener {
                                    startDestination = "auth"
                                    isLoadingRole = false
                                }
                        } else {
                            startDestination = "auth"
                            isLoadingRole = false
                        }
                    }

                    if (isLoadingRole || startDestination == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination!!
                        ) {
                            // Auth Screen
                            composable("auth") {
                                AuthScreen(
                                    onLoginSuccess = { userRole ->
                                        val destination = when (userRole.lowercase()) {
                                            "owner" -> "owner_home"
                                            "faculty" -> "faculty_home"
                                            "receptionist" -> "receptionist_home"
                                            else -> "auth"
                                        }
                                        navController.navigate(destination) {
                                            popUpTo("auth") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // 1. Owner Portal Route
                            composable("owner_home") {
                                PortalDashboardScreen(
                                    portalTitle = "Owner Portal",
                                    roleSubtitle = "TechnoVision Administration",
                                    sections = listOf(
                                        PortalSectionModel(
                                            title = "Student Management",
                                            actions = listOf(
                                                PortalAction("Add Student", "Register a new student", Icons.Default.AppRegistration) { navController.navigate("enroll_student") },
                                                PortalAction("All Students", "List of all the students", Icons.Default.People) { navController.navigate("student_directory") },
                                                PortalAction("Take Attendance", "Mark daily student presence", Icons.Default.CheckCircle) { navController.navigate("attendance") },
                                                PortalAction("Attendance History", "View past session records", Icons.AutoMirrored.Filled.Assignment) { navController.navigate("attendance_history") },
                                                PortalAction("Class Timings", "Set batches & schedules", Icons.Default.School) { navController.navigate("batches") },
                                                PortalAction("Payments", "Collect fees & view ledger", Icons.Default.Payment) { navController.navigate("fee_collection") }
                                            )
                                        ),
                                        PortalSectionModel(
                                            title = "Staff Management",
                                            actions = listOf(
                                                PortalAction("Manage Staff", "Add, edit & remove staffs", Icons.Default.Badge) { navController.navigate("manage_staff") },
                                                PortalAction("Verify Attendance", "Approve teacher attendance", Icons.AutoMirrored.Filled.FactCheck) { navController.navigate("verify_teacher_attendance") }
                                            )
                                        ),
                                        PortalSectionModel(
                                            title = "System",
                                            actions = listOf(
                                                PortalAction("Activity History", "View center logs & events", Icons.Default.History) { navController.navigate("system_logs") }
                                            )
                                        )
                                    ),
                                    onLogout = {
                                        navController.navigate("auth") {
                                            popUpTo(0) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            // 2. Faculty Portal Route
                            composable("faculty_home") {
                                PortalDashboardScreen(
                                    portalTitle = "Faculty Portal",
                                    roleSubtitle = "TechnoVision Teaching Staff",
                                    sections = listOf(
                                        PortalSectionModel(
                                            title = "Student Management",
                                            actions = listOf(
                                                PortalAction("Take Attendance", "Mark daily student presence", Icons.Default.CheckCircle) { navController.navigate("attendance") },
                                                PortalAction("Attendance History", "View past session records", Icons.AutoMirrored.Filled.Assignment) { navController.navigate("attendance_history") },
                                                PortalAction("All Students", "List of all the students", Icons.Default.People) { navController.navigate("student_directory") },
                                                PortalAction("Class Timings", "Set batches & schedules", Icons.Default.School) { navController.navigate("batches") }
                                            )
                                        ),
                                        PortalSectionModel(
                                            title = "Teacher Management",
                                            actions = listOf(
                                                PortalAction("Faculty Attendance", "Request attendance", Icons.AutoMirrored.Filled.Assignment) { navController.navigate("teacher_attendance") }
                                            )
                                        ),
                                        PortalSectionModel(
                                            title = "System",
                                            actions = listOf(
                                                PortalAction("Activity History", "View center logs & events", Icons.Default.History) { navController.navigate("system_logs") }
                                            )
                                        )
                                    ),
                                    onLogout = {
                                        navController.navigate("auth") {
                                            popUpTo(0) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            // 3. Receptionist Portal Route
                            composable("receptionist_home") {
                                PortalDashboardScreen(
                                    portalTitle = "Receptionist Portal",
                                    roleSubtitle = "TechnoVision Front Desk",
                                    sections = listOf(
                                        PortalSectionModel(
                                            title = "Student Management",
                                            actions = listOf(
                                                PortalAction("Add Student", "Register a new student", Icons.Default.AppRegistration) { navController.navigate("enroll_student") },
                                                PortalAction("Payments", "Collect fees & view ledger", Icons.Default.Payment) { navController.navigate("fee_collection") },
                                                PortalAction("All Students", "List of all the students", Icons.Default.People) { navController.navigate("student_directory") },
                                                PortalAction("Attendance History", "View past session records", Icons.AutoMirrored.Filled.Assignment) { navController.navigate("attendance_history") },
                                                PortalAction("Class Timings", "Set batches & schedules", Icons.Default.School) { navController.navigate("batches") }
                                            )
                                        ),
                                        PortalSectionModel(
                                            title = "Staff Management",
                                            actions = listOf(
                                                PortalAction("Verify Attendance", "Approve teacher attendance", Icons.AutoMirrored.Filled.FactCheck) { navController.navigate("verify_teacher_attendance") }
                                            )
                                        ),
                                        PortalSectionModel(
                                            title = "System",
                                            actions = listOf(
                                                PortalAction("Activity History", "View center logs & events", Icons.Default.History) { navController.navigate("system_logs") }
                                            )
                                        )
                                    ),
                                    onLogout = {
                                        navController.navigate("auth") {
                                            popUpTo(0) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            // Sub-screens & Functional Routes
                            composable("enroll_student") {
                                EnrollStudentScreen(
                                    onBack = { navController.popBackStack() },
                                    onStudentEnrolled = { studentId ->
                                        navController.navigate("student_viewer/$studentId") {
                                            popUpTo("enroll_student") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("student_viewer/{studentId}") { backStackEntry ->
                                val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
                                StudentViewerScreen(
                                    studentId = studentId,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("manage_staff") { ManageStaffScreen(onBack = { navController.popBackStack() }) }

                            composable("attendance") {
                                TakeAttendanceScreen(
                                    onBack = { navController.popBackStack() },
                                    onNavigateToHistory = { navController.navigate("attendance_history") }
                                )
                            }

                            composable("attendance_history") {
                                AttendanceHistoryScreen(onBack = { navController.popBackStack() })
                            }

                            composable("student_directory") {
                                StudentDirectoryScreen(
                                    onBack = { navController.popBackStack() },
                                    onNavigateToUpdateDelete = { studentId ->
                                        navController.navigate("update_delete_student/$studentId")
                                    },
                                    onNavigateToViewer = { studentId ->
                                        navController.navigate("student_viewer/$studentId")
                                    }
                                )
                            }

                            composable("update_delete_student/{studentId}") { backStackEntry ->
                                val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
                                UpdateDeleteStudentScreen(
                                    studentId = studentId,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("batches") { BatchesScreen(onBack = { navController.popBackStack() }) }
                            composable("system_logs") { SystemLogsScreen(onBack = { navController.popBackStack() }) }

                            composable("teacher_attendance") {
                                TeacherAttendanceScreen(onBack = { navController.popBackStack() })
                            }

                            composable("verify_teacher_attendance") {
                                VerifyTeacherAttendanceScreen(onBack = { navController.popBackStack() })
                            }

                            composable("fee_collection") {
                                FeeCollectionScreen(
                                    onBack = { navController.popBackStack() },
                                    onNavigateToGlobalHistory = { navController.navigate("global_history") }
                                )
                            }

                            composable("global_history") {
                                GlobalHistoryScreen(onBack = { navController.popBackStack() })
                            }
                        }
                    }
                }
            }
        }
    }
}