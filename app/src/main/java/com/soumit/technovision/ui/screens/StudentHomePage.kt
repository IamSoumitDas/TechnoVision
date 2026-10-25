package com.soumit.technovision.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.soumit.technovision.ui.theme.TechnoVisionTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomePage(onLogout: () -> Unit) {
    val isInPreview = LocalInspectionMode.current
    var studentName by remember { mutableStateOf("Student") }
    var studentEmail by remember { mutableStateOf("") }
    var studentClass by remember { mutableStateOf("") }
    var schoolName by remember { mutableStateOf("") }
    var monthlyInstallment by remember { mutableStateOf(500L) }
    var weeklySchedule by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(Unit) {
        if (isInPreview) {
            studentName = "Aarav Sharma"
            studentEmail = "aarav@example.com"
            studentClass = "X"
            schoolName = "St. Xavier's School"
            monthlyInstallment = 500L
            weeklySchedule = mapOf(
                "Sunday" to "9:00 AM - 11:00 AM",
                "Tuesday" to "5:00 PM - 7:00 PM",
                "Thursday" to "5:00 PM - 7:00 PM"
            )
        } else {
            val currentUser = FirebaseAuth.getInstance().currentUser
            studentEmail = currentUser?.email ?: ""
            val userId = currentUser?.uid

            if (userId != null) {
                FirebaseFirestore.getInstance().collection("students").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            studentName = document.getString("fullName") ?: "Student"
                            studentClass = document.getString("studentClass") ?: ""
                            schoolName = document.getString("school") ?: ""
                            monthlyInstallment = document.getLong("monthlyInstallment") ?: 500L

                            @Suppress("UNCHECKED_CAST")
                            weeklySchedule = document.get("weeklySchedule") as? Map<String, String> ?: emptyMap()
                        }
                    }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TechnoVision Student Portal") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Welcome Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "Welcome, $studentName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = studentEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Profile Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Academic Profile", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text = "Class: ${studentClass.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "School: ${schoolName.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Monthly Installment Due: ₹$monthlyInstallment", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            // Weekly Schedule Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Your Weekly Batch Schedule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    if (weeklySchedule.isEmpty()) {
                        Text(text = "No custom schedule assigned.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    } else {
                        weeklySchedule.forEach { (day, slot) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = day, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(text = slot, style = MaterialTheme.typography.bodyMedium, color = if (slot == "Off") MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.secondary)
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
fun StudentHomePagePreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        StudentHomePage(onLogout = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun StudentHomePagePreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        StudentHomePage(onLogout = {})
    }
}