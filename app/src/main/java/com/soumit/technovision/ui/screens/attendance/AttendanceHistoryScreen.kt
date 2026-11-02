@file:Suppress("UNCHECKED_CAST")

package com.soumit.technovision.ui.screens.attendance

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
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
import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    var attendanceRecords by remember { mutableStateOf<List<AttendanceRecordModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Dialog & student name lookup states
    var selectedRecordForDetail by remember { mutableStateOf<AttendanceRecordModel?>(null) }
    var presentStudentNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var absentStudentNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingNames by remember { mutableStateOf(false) }

    // Fetch attendance logs ordered by newest first
    LaunchedEffect(Unit) {
        if (isInPreview) {
            attendanceRecords = listOf(
                AttendanceRecordModel(
                    id = "24-10-2026_9_00_AM",
                    date = "24-10-2026",
                    timeSlot = "9:00 AM - 11:00 AM",
                    presentStudentIds = listOf("s1", "s2", "s3"),
                    absentStudentIds = listOf("s4"),
                    totalPresent = 3,
                    totalAbsent = 1
                ),
                AttendanceRecordModel(
                    id = "23-10-2026_5_00_PM",
                    date = "23-10-2026",
                    timeSlot = "5:00 PM - 7:00 PM",
                    presentStudentIds = listOf("s1", "s2"),
                    absentStudentIds = listOf("s3", "s4"),
                    totalPresent = 2,
                    totalAbsent = 2
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

    // Effect to fetch student names when a record is selected
    LaunchedEffect(selectedRecordForDetail) {
        val record = selectedRecordForDetail
        if (record != null) {
            if (isInPreview) {
                presentStudentNames = listOf("Aarav Sharma", "Priya Das", "Rahul Roy")
                absentStudentNames = listOf("Sneha Gupta")
                isLoadingNames = false
            } else {
                isLoadingNames = true
                val db = FirebaseFirestore.getInstance()
                val idsToFetch = record.presentStudentIds + record.absentStudentIds

                if (idsToFetch.isEmpty()) {
                    presentStudentNames = emptyList()
                    absentStudentNames = emptyList()
                    isLoadingNames = false
                } else {
                    var fetchedCount = 0
                    val nameMap = mutableMapOf<String, String>()

                    for (id in idsToFetch) {
                        db.collection("students").document(id).get()
                            .addOnSuccessListener { doc ->
                                val name = doc.getString("fullName") ?: doc.getString("name") ?: id
                                nameMap[id] = name
                                fetchedCount++
                                if (fetchedCount == idsToFetch.size) {
                                    presentStudentNames = record.presentStudentIds.map { nameMap[it] ?: it }
                                    absentStudentNames = record.absentStudentIds.map { nameMap[it] ?: it }
                                    isLoadingNames = false
                                }
                            }
                            .addOnFailureListener {
                                nameMap[id] = id
                                fetchedCount++
                                if (fetchedCount == idsToFetch.size) {
                                    presentStudentNames = record.presentStudentIds.map { nameMap[it] ?: it }
                                    absentStudentNames = record.absentStudentIds.map { nameMap[it] ?: it }
                                    isLoadingNames = false
                                }
                            }
                    }
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

    // Enhanced Detail Dialog with Present & Absent Student Names
    if (selectedRecordForDetail != null) {
        val record = selectedRecordForDetail!!
        val formattedDate = remember(record.date) {
            try {
                val inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                val localDate = LocalDate.parse(record.date, inputFormatter)
                val outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy (EEE)")
                localDate.format(outputFormatter)
            } catch (e: Exception) {
                record.date
            }
        }

        AlertDialog(
            onDismissRequest = { selectedRecordForDetail = null },
            title = {
                Column {
                    Text(text = "Session Breakdown", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$formattedDate • ${record.timeSlot}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            text = {
                if (isLoadingNames) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "${record.totalPresent}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(text = "Present", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "${record.totalAbsent}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Text(text = "Absent", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        HorizontalDivider()

                        // Present Students Section
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text(text = "Present Students (${presentStudentNames.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            if (presentStudentNames.isEmpty()) {
                                Text(text = "None", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            } else {
                                presentStudentNames.forEach { name ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = name, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        // Absent Students Section
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                Text(text = "Absent Students (${absentStudentNames.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                            if (absentStudentNames.isEmpty()) {
                                Text(text = "None", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            } else {
                                absentStudentNames.forEach { name ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = name, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
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
    val formattedDate = remember(record.date) {
        try {
            val inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            val localDate = LocalDate.parse(record.date, inputFormatter)
            val outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy (EEE)")
            localDate.format(outputFormatter)
        } catch (e: Exception) {
            record.date
        }
    }

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
                        text = formattedDate,
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
