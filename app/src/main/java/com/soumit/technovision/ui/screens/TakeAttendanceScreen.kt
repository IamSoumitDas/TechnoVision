package com.soumit.technovision.ui.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
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
import com.soumit.technovision.ui.screens.student.StudentModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAttendanceScreen(
    onBack: () -> Unit,
    onNavigateToHistory: () -> Unit // Callback to open attendance history
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    val studentList = remember { mutableStateListOf<StudentModel>() }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Map to track attendance: Student ID -> Boolean (true = Present, false = Absent)
    val attendanceStates = remember { mutableStateMapOf<String, Boolean>() }

    // Current Date formatted as DD-MM-YYYY
    val currentDate = remember {
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    }

    // Time slot options for the current batch session
    val timeSlots = listOf(
        "9:00 AM - 11:00 AM",
        "11:00 AM - 1:00 PM",
        "1:00 PM - 3:00 PM",
        "3:00 PM - 5:00 PM",
        "5:00 PM - 7:00 PM",
        "7:00 PM - 9:00 PM",
        "General / Other"
    )
    var selectedTimeSlot by remember { mutableStateOf(timeSlots[0]) }
    var timeSlotExpanded by remember { mutableStateOf(false) }

    // Fetch all students on load
    LaunchedEffect(Unit) {
        if (isInPreview) {
            val mockStudents = listOf(
                StudentModel(
                    id = "1",
                    fullName = "Aarav Sharma",
                    studentClass = "X",
                    school = "St. Xavier's School"
                ),
                StudentModel(
                    id = "2",
                    fullName = "Priya Das",
                    studentClass = "XII",
                    school = "Alipurduar Girls High School"
                ),
                StudentModel(
                    id = "3",
                    fullName = "Rahul Roy",
                    studentClass = "VIII",
                    school = "Kendriya Vidyalaya"
                )
            )
            studentList.clear()
            studentList.addAll(mockStudents)
            // Default everyone to Absent initially
            mockStudents.forEach { attendanceStates[it.id] = false }
            isLoading = false
        } else {
            FirebaseFirestore.getInstance().collection("students")
                .orderBy("fullName")
                .get()
                .addOnSuccessListener { result ->
                    val list = result.documents.map { doc ->
                        StudentModel(
                            id = doc.id,
                            fullName = doc.getString("fullName") ?: "",
                            studentClass = doc.getString("studentClass") ?: "",
                            school = doc.getString("school") ?: ""
                        )
                    }
                    studentList.clear()
                    studentList.addAll(list)
                    // Default all students to unchecked (Absent)
                    list.forEach { attendanceStates[it.id] = false }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    Toast.makeText(context, "Error loading students: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Take Attendance ($currentDate)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // History Action Button
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Attendance History"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        Button(
                            onClick = {
                                if (isInPreview) {
                                    Toast.makeText(context, "Preview: Attendance Saved!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                    return@Button
                                }

                                isSaving = true
                                val db = FirebaseFirestore.getInstance()

                                val presentIds = attendanceStates.filter { it.value }.keys.toList()
                                val absentIds = attendanceStates.filter { !it.value }.keys.toList()

                                val slotIdTag = selectedTimeSlot.replace(Regex("[^A-Za-z0-9]"), "_")
                                val attendanceDocId = "${currentDate}_$slotIdTag"

                                val attendanceRecord = hashMapOf(
                                    "date" to currentDate,
                                    "timeSlot" to selectedTimeSlot,
                                    "presentStudentIds" to presentIds,
                                    "absentStudentIds" to absentIds,
                                    "totalPresent" to presentIds.size,
                                    "totalAbsent" to absentIds.size,
                                    "timestamp" to com.google.firebase.Timestamp.now()
                                )

                                db.collection("attendance").document(attendanceDocId).set(attendanceRecord)
                                    .addOnSuccessListener {
                                        isSaving = false
                                        Toast.makeText(context, "Attendance saved successfully!", Toast.LENGTH_SHORT).show()

                                        // Log attendance submission action
                                        SystemLogger.log(
                                            action = "Batch Attendance Saved",
                                            details = "Recorded attendance for $currentDate [$selectedTimeSlot]. Present: ${presentIds.size}, Absent: ${absentIds.size}"
                                        )

                                        onBack()
                                    }
                                    .addOnFailureListener { e ->
                                        isSaving = false
                                        Toast.makeText(context, "Failed to save: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Batch Attendance")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Select Current Batch Time Slot:",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ExposedDropdownMenuBox(
                expanded = timeSlotExpanded,
                onExpandedChange = { timeSlotExpanded = !timeSlotExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = selectedTimeSlot,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Active Time Slot") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeSlotExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = timeSlotExpanded,
                    onDismissRequest = { timeSlotExpanded = false }
                ) {
                    timeSlots.forEach { slot ->
                        DropdownMenuItem(
                            text = { Text(slot) },
                            onClick = {
                                selectedTimeSlot = slot
                                timeSlotExpanded = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Students List (${studentList.size} Total)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = {
                    studentList.forEach { attendanceStates[it.id] = true }
                }) {
                    Text("Mark All Present")
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (studentList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No students enrolled yet. Enroll students first.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(studentList) { student ->
                        val isPresent = attendanceStates[student.id] == true

                        AttendanceStudentCard(
                            student = student,
                            isPresent = isPresent,
                            onToggle = { checked ->
                                attendanceStates[student.id] = checked
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceStudentCard(
    student: StudentModel,
    isPresent: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        onClick = { onToggle(!isPresent) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPresent) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.fullName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Class: ${student.studentClass} | School: ${student.school}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Checkbox(
                checked = isPresent,
                onCheckedChange = { checked ->
                    onToggle(checked)
                }
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun TakeAttendanceScreenPreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        TakeAttendanceScreen(onBack = {}, onNavigateToHistory = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TakeAttendanceScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        TakeAttendanceScreen(onBack = {}, onNavigateToHistory = {})
    }
}