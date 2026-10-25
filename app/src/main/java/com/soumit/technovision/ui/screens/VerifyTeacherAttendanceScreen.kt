package com.soumit.technovision.ui.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soumit.technovision.ui.screens.faculty.TeacherAttendanceRequestModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyTeacherAttendanceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    var requestsList by remember { mutableStateOf<List<TeacherAttendanceRequestModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (isInPreview) {
            requestsList = listOf(
                TeacherAttendanceRequestModel(
                    id = "1",
                    teacherName = "Amitabha Teacher",
                    date = "24-10-2026",
                    timeSlot = "9:00 AM - 11:00 AM",
                    status = "Pending"
                ),
                TeacherAttendanceRequestModel(
                    id = "2",
                    teacherName = "Priya Teacher",
                    date = "24-10-2026",
                    timeSlot = "11:00 AM - 1:00 PM",
                    status = "Verified"
                )
            )
            isLoading = false
        } else {
            FirebaseFirestore.getInstance().collection("teacher_attendance")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        isLoading = false
                        Toast.makeText(context, "Error loading requests: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val list = snapshot.documents.map { doc ->
                            TeacherAttendanceRequestModel(
                                id = doc.id,
                                teacherId = doc.getString("teacherId") ?: "",
                                teacherName = doc.getString("teacherName") ?: "",
                                date = doc.getString("date") ?: "",
                                timeSlot = doc.getString("timeSlot") ?: "",
                                status = doc.getString("status") ?: "Pending",
                                timestamp = doc.getTimestamp("timestamp")
                            )
                        }
                        requestsList = list
                        isLoading = false
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Faculty Attendance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Pending & Verified Faculty Requests",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (requestsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No teacher attendance requests found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(requestsList) { request ->
                        AdminVerifyRequestCard(
                            request = request,
                            onVerify = {
                                if (isInPreview) {
                                    Toast.makeText(context, "Preview: Request Verified!", Toast.LENGTH_SHORT).show()
                                    return@AdminVerifyRequestCard
                                }

                                FirebaseFirestore.getInstance().collection("teacher_attendance")
                                    .document(request.id)
                                    .update("status", "Verified")
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Attendance verified successfully!", Toast.LENGTH_SHORT).show()

                                        // Log attendance verification action
                                        SystemLogger.log(
                                            action = "Attendance Verified",
                                            details = "Approved time slot [${request.timeSlot}] for teacher ${request.teacherName} on date ${request.date}"
                                        )
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Failed to verify: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminVerifyRequestCard(
    request: TeacherAttendanceRequestModel,
    onVerify: () -> Unit
) {
    val isVerified = request.status == "Verified"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isVerified) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = request.teacherName.ifBlank { "Faculty Member" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Slot: ${request.timeSlot}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Date: ${request.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Surface(
                    color = if (isVerified) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isVerified) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = request.status,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isVerified) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (!isVerified) {
                Button(
                    onClick = onVerify,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Verify & Lock Attendance")
                }
            } else {
                Text(
                    text = "Locked & Verified",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun VerifyTeacherAttendanceScreenPreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        VerifyTeacherAttendanceScreen(onBack = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun VerifyTeacherAttendanceScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        VerifyTeacherAttendanceScreen(onBack = {})
    }
}