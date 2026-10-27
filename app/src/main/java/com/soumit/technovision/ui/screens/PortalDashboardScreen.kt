package com.soumit.technovision.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.soumit.technovision.ui.theme.TechnoVisionTheme

// ==========================================
// DATA MODELS
// ==========================================

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

// ==========================================
// CENTRALIZED ROLE SECTION PROVIDERS
// ==========================================

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
                        PortalAction("Class Timings", "Set batches & schedules", Icons.Default.School) { onActionClick("Class Timings") } ,
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

// ==========================================
// MAIN DASHBOARD SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalDashboardScreen(
    portalTitle: String,
    roleSubtitle: String,
    sections: List<PortalSectionModel>,
    onLogout: () -> Unit
) {
    val isInPreview = LocalInspectionMode.current
    var userName by remember { mutableStateOf("User") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (isInPreview) {
            userName = "Soumit Das"
        } else {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid
            if (userId != null) {
                FirebaseFirestore.getInstance().collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            userName = document.getString("fullName") ?: "User"
                        }
                    }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (userName.isNotBlank() && userName != "User") {
                                    val parts = userName.trim().split("\\s+".toRegex())
                                    if (parts.size > 1) "${parts.first().take(1)}${parts.last().take(1)}".uppercase()
                                    else parts.first().take(1).uppercase()
                                } else "U",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Techno Vision Portal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                /*actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Log Out",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },*/
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            sections.forEach { section ->
                PortalDashboardSection(section = section)
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        if (!isInPreview) {
                            FirebaseAuth.getInstance().signOut()
                        }
                        onLogout()
                    }
                ) {
                    Text("Log Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// SECTION & CARD COMPONENTS
// ==========================================

@Composable
fun PortalDashboardSection(section: PortalSectionModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        section.actions.chunked(2).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (action in rowActions) {
                    Box(modifier = Modifier.weight(1f)) {
                        PortalActionCard(action = action)
                    }
                }
                if (rowActions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun PortalActionCard(action: PortalAction) {
    Card(
        onClick = action.onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = action.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

// ==========================================
// PREVIEWS WITH SELF-CONTAINED ROLE SWITCHER
// ==========================================

@Preview(name = "Interactive Role Switcher Preview", showBackground = true)
@Composable
fun PortalDashboardInteractivePreview() {
    TechnoVisionTheme {
        var selectedRole by remember { mutableStateOf("Owner") }
        val sections = PortalRoleSections.getSectionsForRole(selectedRole)
        val portalTitle = when (selectedRole) {
            "Faculty" -> "Faculty Portal"
            "Receptionist" -> "Receptionist Portal"
            else -> "Owner Portal"
        }
        val roleSubtitle = when (selectedRole) {
            "Faculty" -> "TechnoVision Teaching Staff"
            "Receptionist" -> "TechnoVision Front Desk"
            else -> "TechnoVision Administration"
        }

        Box(modifier = Modifier.fillMaxSize()) {
            PortalDashboardScreen(
                portalTitle = portalTitle,
                roleSubtitle = roleSubtitle,
                sections = sections,
                onLogout = {}
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Owner", "Faculty", "Receptionist").forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            label = { Text(role) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Dark Mode Preview", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PortalDashboardDarkPreview() {
    TechnoVisionTheme(darkTheme = true) {
        PortalDashboardScreen(
            portalTitle = "Owner Portal",
            roleSubtitle = "TechnoVision Administration",
            sections = emptyList(),
            onLogout = {}
        )
    }
}