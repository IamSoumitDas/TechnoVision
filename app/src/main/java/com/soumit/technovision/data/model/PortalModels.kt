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
                        PortalAction("Take Attendance", "Mark session attendance", Icons.Default.CheckCircle) { onActionClick("Take Attendance") },
                        PortalAction("Attendance History", "Past attendance sessions", Icons.AutoMirrored.Filled.Assignment) { onActionClick("Attendance History") },
                        PortalAction("All Students", "Directory and profiles", Icons.Default.People) { onActionClick("All Students") },
                        PortalAction("Batch Schedules", "Timings and day assignments", Icons.Default.School) { onActionClick("Class Timings") },
                        PortalAction("Birthday Calendar", "Upcoming student birthdays", Icons.Default.Cake) { onActionClick("Birthday Calendar") }
                    )
                ),
                PortalSectionModel(
                    title = "Faculty Management",
                    actions = listOf(
                        PortalAction("Faculty Attendance", "Submit attendance request", Icons.AutoMirrored.Filled.Assignment) { onActionClick("Faculty Attendance") }
                    )
                ),
                PortalSectionModel(
                    title = "System",
                    actions = listOf(
                        PortalAction("Activity Logs", "Center activity & audit trail", Icons.Default.History) { onActionClick("Activity History") }
                    )
                )
            )
            "receptionist" -> listOf(
                PortalSectionModel(
                    title = "Student Management",
                    actions = listOf(
                        PortalAction("Add Student", "Register a new student", Icons.Default.AppRegistration) { onActionClick("Add Student") },
                        PortalAction("All Students", "Directory and profiles", Icons.Default.People) { onActionClick("All Students") },
                        PortalAction("Attendance History", "Past attendance sessions", Icons.AutoMirrored.Filled.Assignment) { onActionClick("Attendance History") },
                        PortalAction("Batch Schedules", "Timings and day assignments", Icons.Default.School) { onActionClick("Class Timings") },
                        PortalAction("Collect Payment", "Record payments & dues", Icons.Default.Payment) { onActionClick("Payments") },
                        PortalAction("Payment History", "View payment receipts", Icons.Default.History) { onActionClick("Global History") },
                        PortalAction("Birthday Calendar", "Upcoming student birthdays", Icons.Default.Cake) { onActionClick("Birthday Calendar") }
                    )
                ),
                PortalSectionModel(
                    title = "Staff Management",
                    actions = listOf(
                        PortalAction("Verify Attendance", "Review faculty requests", Icons.AutoMirrored.Filled.FactCheck) { onActionClick("Verify Attendance") }
                    )
                ),
                PortalSectionModel(
                    title = "System",
                    actions = listOf(
                        PortalAction("Activity Logs", "Center activity & audit trail", Icons.Default.History) { onActionClick("Activity History") }
                    )
                )
            )
            else -> listOf( // "owner" default
                PortalSectionModel(
                    title = "Student Management",
                    actions = listOf(
                        PortalAction("Add Student", "Register a new student", Icons.Default.AppRegistration) { onActionClick("Add Student") },
                        PortalAction("All Students", "Directory and profiles", Icons.Default.People) { onActionClick("All Students") },
                        PortalAction("Take Attendance", "Mark session attendance", Icons.Default.CheckCircle) { onActionClick("Take Attendance") },
                        PortalAction("Attendance History", "Past attendance sessions", Icons.AutoMirrored.Filled.Assignment) { onActionClick("Attendance History") },
                        PortalAction("Batch Schedules", "Timings and day assignments", Icons.Default.School) { onActionClick("Class Timings") },
                        PortalAction("Collect Payment", "Record payments & dues", Icons.Default.Payment) { onActionClick("Payments") },
                        PortalAction("Payment History", "View payment receipts", Icons.Default.History) { onActionClick("Global History") },
                        PortalAction("Birthday Calendar", "Upcoming student birthdays", Icons.Default.Cake) { onActionClick("Birthday Calendar") }
                    )
                ),
                PortalSectionModel(
                    title = "Staff Management",
                    actions = listOf(
                        PortalAction("Manage Staff", "Staff accounts & permissions", Icons.Default.Badge) { onActionClick("Manage Staff") },
                        PortalAction("Verify Attendance", "Review faculty requests", Icons.AutoMirrored.Filled.FactCheck) { onActionClick("Verify Attendance") }
                    )
                ),
                PortalSectionModel(
                    title = "System",
                    actions = listOf(
                        PortalAction("Activity Logs", "Center activity & audit trail", Icons.Default.History) { onActionClick("Activity History") }
                    )
                )
            )
        }
    }
}
