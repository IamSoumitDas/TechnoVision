package com.soumit.technovision.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class PortalAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

data class PortalSectionModel(
    val title: String,
    val actions: List<PortalAction>
)

object PortalRoleSections {
    fun getSectionsForRole(role: String, onActionClick: (String) -> Unit = {}): List<PortalSectionModel> {
        return when (role.lowercase()) {
            "faculty" -> listOf(
                PortalSectionModel(
                    title = "Student Management",
                    actions = listOf(
                        PortalAction("Take Attendance", "Mark daily student presence", Icons.Default.CheckCircle) { onActionClick("Take Attendance") },
                        PortalAction("Attendance History", "Students attendance history", Icons.AutoMirrored.Filled.Assignment) { onActionClick("Attendance History") },
                        PortalAction("All Students", "List of all the students", Icons.Default.People) { onActionClick("All Students") },
                        PortalAction("Class Timings", "Set batches & schedules", Icons.Default.School) { onActionClick("Class Timings") }
                    )
                ),
                PortalSectionModel(
                    title = "Teacher Management",
                    actions = listOf(
                        PortalAction("Faculty Attendance", "Request attendance", Icons.AutoMirrored.Filled.Assignment) { onActionClick("Faculty Attendance") }
                    )
                ),
                PortalSectionModel(
                    title = "System",
                    actions = listOf(
                        PortalAction("Activity History", "View center logs & events", Icons.Default.History) { onActionClick("Activity History") }
                    )
                )
            )
            "receptionist" -> listOf(
                PortalSectionModel(
                    title = "Student Management",
                    actions = listOf(
                        PortalAction("Add Student", "Register a new student", Icons.Default.AppRegistration) { onActionClick("Add Student") },
                        PortalAction("All Students", "List of all the students", Icons.Default.People) { onActionClick("All Students") },
                        PortalAction("Attendance History", "Students attendance history", Icons.AutoMirrored.Filled.Assignment) { onActionClick("Attendance History") },
                        PortalAction("Class Timings", "Set batches & schedules", Icons.Default.School) { onActionClick("Class Timings") },
                        PortalAction("Collect Payment", "Collect fees & view ledger", Icons.Default.Payment) { onActionClick("Payments") },
                        PortalAction("Payment History", "View all payment records", Icons.Default.History) { onActionClick("Global History") },
                    )
                ),
                PortalSectionModel(
                    title = "Staff Management",
                    actions = listOf(
                        PortalAction("Verify Attendance", "Approve teacher attendance", Icons.AutoMirrored.Filled.FactCheck) { onActionClick("Verify Attendance") }
                    )
                ),
                PortalSectionModel(
                    title = "System",
                    actions = listOf(
                        PortalAction("Activity History", "View center logs & events", Icons.Default.History) { onActionClick("Activity History") }
                    )
                )
            )
            else -> listOf( // "owner" default
                PortalSectionModel(
                    title = "Student Management",
                    actions = listOf(
                        PortalAction("Add Student", "Register a new student", Icons.Default.AppRegistration) { onActionClick("Add Student") },
                        PortalAction("All Students", "List of all the students", Icons.Default.People) { onActionClick("All Students") },
                        PortalAction("Take Attendance", "Mark daily student presence", Icons.Default.CheckCircle) { onActionClick("Take Attendance") },
                        PortalAction("Attendance History", "Students attendance history", Icons.AutoMirrored.Filled.Assignment) { onActionClick("Attendance History") },
                        PortalAction("Class Timings", "Set batches & schedules", Icons.Default.School) { onActionClick("Class Timings") },
                        PortalAction("Collect Payment", "Collect fees & view ledger", Icons.Default.Payment) { onActionClick("Payments") },
                        PortalAction("Payment History", "View all payment records", Icons.Default.History) { onActionClick("Global History") },
                    )
                ),
                PortalSectionModel(
                    title = "Staff Management",
                    actions = listOf(
                        PortalAction("Manage Staff", "Add, edit & remove staffs", Icons.Default.Badge) { onActionClick("Manage Staff") },
                        PortalAction("Verify Attendance", "Approve teacher attendance", Icons.AutoMirrored.Filled.FactCheck) { onActionClick("Verify Attendance") }
                    )
                ),
                PortalSectionModel(
                    title = "System",
                    actions = listOf(
                        PortalAction("Activity History", "View center logs & events", Icons.Default.History) { onActionClick("Activity History") }
                    )
                )
            )
        }
    }
}
