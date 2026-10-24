package com.soumit.technovision.ui.screens.receptionist

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
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
    onNavigateToLogs: () -> Unit = {}, // Added callback for System Logs
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

    val actions = listOf(
        ReceptionAction(
            title = "Enroll Student",
            subtitle = "Register new admissions",
            icon = Icons.Default.AppRegistration,
            onClick = onNavigateToAddStudent
        ),
        ReceptionAction(
            title = "Fee Collection",
            subtitle = "Record monthly installments",
            icon = Icons.Default.Payment,
            onClick = onNavigateToFees
        ),
        ReceptionAction(
            title = "Verify Attendance",
            subtitle = "Approve teacher requests",
            icon = Icons.Default.FactCheck,
            onClick = onNavigateToVerifyAttendance
        ),
        ReceptionAction(
            title = "Student Directory",
            subtitle = "View student records & contact info",
            icon = Icons.Default.People,
            onClick = onNavigateToDirectory
        ),
        ReceptionAction(
            title = "System Logs",
            subtitle = "View center audit trail",
            icon = Icons.Default.History,
            onClick = onNavigateToLogs
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TechnoVision Reception Portal") },
                actions = {
                    IconButton(onClick = {
                        if (!isInPreview) {
                            FirebaseAuth.getInstance().signOut()
                        }
                        onLogout()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
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
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "Welcome, $receptionName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = receptionEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(
                text = "Front Desk Operations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(actions) { action ->
                    Card(
                        onClick = action.onClick,
                        modifier = Modifier.fillMaxWidth().height(130.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(imageVector = action.icon, contentDescription = action.title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Column {
                                Text(text = action.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = action.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
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