package com.soumit.technovision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.soumit.technovision.ui.screens.faculty.FacultyHomePage
import com.soumit.technovision.ui.screens.faculty.TeacherAttendanceScreen
import com.soumit.technovision.ui.screens.owner.ManageStaffScreen
import com.soumit.technovision.ui.screens.owner.OwnerHomePage
import com.soumit.technovision.ui.screens.receptionist.ReceptionistHomePage
import com.soumit.technovision.ui.screens.BatchesScreen
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

                    // Check user role on startup if a session already exists
                    LaunchedEffect(currentUser) {
                        if (currentUser != null) {
                            val userId = currentUser.uid
                            FirebaseFirestore.getInstance().collection("users").document(userId).get()
                                .addOnSuccessListener { document ->
                                    val role = document.getString("role") ?: "auth"
                                    startDestination = when (role) {
                                        "owner" -> "owner_home"
                                        "faculty" -> "faculty_home"
                                        "receptionist" -> "receptionist_home"
                                        else -> "auth" // Fallback to auth if role is unknown or missing
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
                                        val destination = when (userRole) {
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

                            // Owner Portal & Sub-screens
                            composable("owner_home") {
                                OwnerHomePage(
                                    onNavigateToAddStudent = { navController.navigate("enroll_student") },
                                    onNavigateToFeeCollection = { navController.navigate("fee_collection") },
                                    onNavigateToManageStaff = { navController.navigate("manage_staff") },
                                    onNavigateToAttendance = { navController.navigate("attendance") },
                                    onNavigateToStudentDirectory = { navController.navigate("student_directory") },
                                    onNavigateToBatches = { navController.navigate("batches") },
                                    onNavigateToLogs = { navController.navigate("system_logs") },
                                    onNavigateToVerifyAttendance = { navController.navigate("verify_teacher_attendance") },
                                    onLogout = {
                                        navController.navigate("auth") {
                                            popUpTo("owner_home") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // Updated Enroll Student Route with navigation to viewer
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

                            // Student Info Viewer Route
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

                            // Updated Student Directory Route with Callback
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

                            // New Update / Delete Route with Student ID Argument
                            composable("update_delete_student/{studentId}") { backStackEntry ->
                                val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
                                UpdateDeleteStudentScreen(
                                    studentId = studentId,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("batches") { BatchesScreen(onBack = { navController.popBackStack() }) }
                            composable("system_logs") { SystemLogsScreen(onBack = { navController.popBackStack() }) }

                            // Faculty Portal
                            composable("faculty_home") {
                                FacultyHomePage(
                                    onNavigateToAttendance = { navController.navigate("attendance") },
                                    onNavigateToStudentDirectory = { navController.navigate("student_directory") },
                                    onNavigateToTeacherAttendance = { navController.navigate("teacher_attendance") },
                                    onNavigateToLogs = { navController.navigate("system_logs") }, // <-- Wired here
                                    onLogout = {
                                        navController.navigate("auth") {
                                            popUpTo("faculty_home") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // Teacher Attendance Request Route
                            composable("teacher_attendance") {
                                TeacherAttendanceScreen(onBack = { navController.popBackStack() })
                            }

                            // Admin Attendance Verification Route (Shared by Owner & Receptionist)
                            composable("verify_teacher_attendance") {
                                VerifyTeacherAttendanceScreen(onBack = { navController.popBackStack() })
                            }

                            // Receptionist Portal
                            composable("receptionist_home") {
                                ReceptionistHomePage(
                                    onNavigateToAddStudent = { navController.navigate("enroll_student") },
                                    onNavigateToDirectory = { navController.navigate("student_directory") },
                                    onNavigateToFees = { navController.navigate("fee_collection") },
                                    onNavigateToVerifyAttendance = { navController.navigate("verify_teacher_attendance") },
                                    onNavigateToLogs = { navController.navigate("system_logs") }, // <-- Wired here
                                    onLogout = {
                                        navController.navigate("auth") {
                                            popUpTo("receptionist_home") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            // Fee Collection Route
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