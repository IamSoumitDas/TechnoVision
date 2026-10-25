package com.soumit.technovision.ui.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
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
import com.soumit.technovision.ui.theme.TechnoVisionTheme

data class AttendanceRecordModel(
    val id: String = "",
    val date: String = "",
    val timeSlot: String = "",
    val presentStudentIds: List<String> = emptyList(),
    val absentStudentIds: List<String> = emptyList(),
    val totalPresent: Int = 0,
    val totalAbsent: Int = 0,
    val timestamp: com.google.firebase.Timestamp? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    var attendanceRecords by remember { mutableStateOf<List<AttendanceRecordModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Dialog to view student details for a selected record
    var selectedRecordForDetail by remember { mutableStateOf<AttendanceRecordModel?>(null) }

    // Fetch attendance logs ordered by newest first
    LaunchedEffect(Unit) {
        if (isInPreview) {
            attendanceRecords = listOf(
                AttendanceRecordModel(
                    id = "24-10-2026_9_00_AM",
                    date = "24-10-2026",
                    timeSlot = "9:00 AM - 11:00 AM",
                    totalPresent = 12,
                    totalAbsent = 2
                ),
                AttendanceRecordModel(
                    id = "23-10-2026_5_00_PM",
                    date = "23-10-2026",
                    timeSlot = "5:00 PM - 7:00 PM",
                    totalPresent = 10,
                    totalAbsent = 4
                )
            )
            isLoading = false
        } else {
            FirebaseFirestore.getInstance().collection("attendance")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        isLoading = false
                        Toast.makeText(context, "Error loading history: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val list = snapshot.documents.map { doc ->
                            AttendanceRecordModel(
                                id = doc.id,
                                date = doc.getString("date") ?: "",
                                timeSlot = doc.getString("timeSlot") ?: "",
                                presentStudentIds = doc.get("presentStudentIds") as? List<String> ?: emptyList(),
                                absentStudentIds = doc.get("absentStudentIds") as? List<String> ?: emptyList(),
                                totalPresent = (doc.getLong("totalPresent") ?: 0L).toInt(),
                                totalAbsent = (doc.getLong("totalAbsent") ?: 0L).toInt(),
                                timestamp = doc.getTimestamp("timestamp")
                            )
                        }
                        attendanceRecords = list
                        isLoading = false
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance History Logs") },
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
                .padding(16.dp)
        ) {
            Text(
                text = "Past Session Records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (attendanceRecords.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No attendance history logged yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(attendanceRecords) { record ->
                        AttendanceHistoryCard(
                            record = record,
                            onClick = { selectedRecordForDetail = record }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog when a session card is clicked
    if (selectedRecordForDetail != null) {
        val record = selectedRecordForDetail!!
        AlertDialog(
            onDismissRequest = { selectedRecordForDetail = null },
            title = { Text("Session Breakdown") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Date: ${record.date}", fontWeight = FontWeight.Bold)
                    Text(text = "Time Slot: ${record.timeSlot}", style = MaterialTheme.typography.bodyMedium)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(text = "Present Count: ${record.totalPresent}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(text = "Absent Count: ${record.totalAbsent}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedRecordForDetail = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun AttendanceHistoryCard(record: AttendanceRecordModel, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp).size(24.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = record.date,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = record.timeSlot,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${record.totalPresent} Present",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${record.totalAbsent} Absent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun AttendanceHistoryScreenPreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        AttendanceHistoryScreen(onBack = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AttendanceHistoryScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        AttendanceHistoryScreen(onBack = {})
    }
}