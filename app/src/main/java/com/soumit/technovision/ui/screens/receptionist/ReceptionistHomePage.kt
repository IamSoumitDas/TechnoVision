package com.soumit.technovision.ui.screens.receptionist

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
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

data class ReceptionAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceptionistHomePage(
    onNavigateToAddStudent: () -> Unit = {},
    onNavigateToDirectory: () -> Unit = {},
    onNavigateToFees: () -> Unit = {},
    onNavigateToVerifyAttendance: () -> Unit = {},
    onNavigateToBatches: () -> Unit = {}, // Added callback for Class Timings
    onNavigateToLogs: () -> Unit = {},
    onLogout: () -> Unit
) {
    val isInPreview = LocalInspectionMode.current
    var receptionName by remember { mutableStateOf("Receptionist") }
    var receptionEmail by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (isInPreview) {
            receptionName = "Rina Reception"
            receptionEmail = "reception@technovision.com"
        } else {
            val currentUser = FirebaseAuth.getInstance().currentUser
            receptionEmail = currentUser?.email ?: ""
            val userId = currentUser?.uid

            if (userId != null) {
                FirebaseFirestore.getInstance().collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            receptionName = document.getString("fullName") ?: "Receptionist"
                        }
                    }
            }
        }
    }

    val studentActions = listOf(
        ReceptionAction(
            title = "Add Student",
            subtitle = "Register a new student",
            icon = Icons.Default.AppRegistration,
            onClick = onNavigateToAddStudent
        ),
        ReceptionAction(
            title = "Payments",
            subtitle = "Collect fees & view ledger",
            icon = Icons.Default.Payment,
            onClick = onNavigateToFees
        ),
        ReceptionAction(
            title = "All Students",
            subtitle = "List of all the students",
            icon = Icons.Default.People,
            onClick = onNavigateToDirectory
        ),
        ReceptionAction(
            title = "Class Timings",
            subtitle = "Set batches & schedules",
            icon = Icons.Default.School,
            onClick = onNavigateToBatches
        )
    )

    val facultyAction = listOf(
        ReceptionAction(
            title = "Verify Attendance",
            subtitle = "Approve teacher attendance",
            icon = Icons.AutoMirrored.Filled.FactCheck,
            onClick = onNavigateToVerifyAttendance
        )
    )

    val systemActions = listOf(
        ReceptionAction(
            title = "Activity History",
            subtitle = "View center logs & events",
            icon = Icons.Default.History,
            onClick = onNavigateToLogs
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TechnoVision Portal") },
                actions = {
                    IconButton(onClick = {
                        if (!isInPreview) {
                            FirebaseAuth.getInstance().signOut()
                        }
                        onLogout()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Log Out"
                        )
                    }
                },
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Welcome back,",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = receptionName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (receptionName.isNotBlank()) {
                                val parts = receptionName.trim().split("\\s+".toRegex())
                                if (parts.size > 1) "${parts.first().take(1)}${parts.last().take(1)}".uppercase()
                                else parts.first().take(1).uppercase()
                            } else "R",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            DashboardSection(title = "Student Management", actions = studentActions)
            DashboardSection(title = "Staff Management", actions = facultyAction)
            DashboardSection(title = "System", actions = systemActions)
        }
    }
}

@Composable
fun DashboardSection(title: String, actions: List<ReceptionAction>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        actions.chunked(2).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (rowActions.size == 1) {
                    Box(modifier = Modifier.weight(1f)) {
                        ReceptionActionCard(action = rowActions[0])
                    }
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    for (action in rowActions) {
                        Box(modifier = Modifier.weight(1f)) {
                            ReceptionActionCard(action = action)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ReceptionActionCard(action: ReceptionAction) {
    Card(
        onClick = action.onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = action.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun ReceptionistHomePagePreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        ReceptionistHomePage(onLogout = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ReceptionistHomePagePreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        ReceptionistHomePage(onLogout = {})
    }
}