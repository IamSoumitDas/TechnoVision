package com.soumit.technovision.ui.screens.batches

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
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
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    var studentList by remember { mutableStateOf<List<StudentModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Toggle state: "Today" vs "This Week"
    var selectedView by remember { mutableStateOf("Today") }

    val calendar = Calendar.getInstance()
    val dayOfWeekIndex = calendar.get(Calendar.DAY_OF_WEEK)
    val daysArray = arrayOf("", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val todayName = if (dayOfWeekIndex in daysArray.indices) daysArray[dayOfWeekIndex] else "Sunday"

    val timeSlots = listOf(
        "9:00 AM - 11:00 AM",
        "11:00 AM - 1:00 PM",
        "1:00 PM - 3:00 PM",
        "3:00 PM - 5:00 PM",
        "5:00 PM - 7:00 PM",
        "7:00 PM - 9:00 PM"
    )

    val weekDays = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    LaunchedEffect(Unit) {
        if (isInPreview) {
            studentList = listOf(
                StudentModel(
                    id = "1",
                    fullName = "Aarav Sharma",
                    studentClass = "X",
                    school = "St. Xavier's School",
                    weeklySchedule = mapOf(
                        todayName to "9:00 AM - 11:00 AM",
                        "Tuesday" to "9:00 AM - 11:00 AM"
                    )
                ),
                StudentModel(
                    id = "2",
                    fullName = "Priya Das",
                    studentClass = "XII",
                    school = "Alipurduar Girls High School",
                    weeklySchedule = mapOf(
                        todayName to "5:00 PM - 7:00 PM",
                        "Wednesday" to "5:00 PM - 7:00 PM"
                    )
                ),
                StudentModel(
                    id = "3",
                    fullName = "Debayan Roy",
                    studentClass = "IX",
                    school = "Mc William High School (HS)",
                    weeklySchedule = mapOf(
                        "Sunday" to "9:00 AM - 11:00 AM",
                        "Friday" to "1:00 PM - 3:00 PM"
                    )
                )
            )
            isLoading = false
        } else {
            FirebaseFirestore.getInstance().collection("students")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        isLoading = false
                        Toast.makeText(context, "Error loading batch students: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val list = snapshot.documents.map { doc ->
                            @Suppress("UNCHECKED_CAST")
                            val scheduleMap = doc.get("weeklySchedule") as? Map<String, String> ?: emptyMap()

                            StudentModel(
                                id = doc.id,
                                fullName = doc.getString("fullName") ?: "",
                                studentClass = doc.getString("studentClass") ?: "",
                                school = doc.getString("school") ?: "",
                                weeklySchedule = scheduleMap
                            )
                        }
                        studentList = list
                        isLoading = false
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Batch-wise Student Rosters") },
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
            // Clean Segmented Toggle Bar
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedView == "Today",
                    onClick = { selectedView = "Today" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Today ($todayName)")
                }
                SegmentedButton(
                    selected = selectedView == "This Week",
                    onClick = { selectedView = "This Week" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("This Week")
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (selectedView == "Today") {
                    // TODAY VIEW: Group by Time Slots, hiding empty slots automatically
                    val activeTimeSlots = timeSlots.map { slot ->
                        slot to studentList.filter { it.weeklySchedule[todayName] == slot }
                    }.filter { (_, students) -> students.isNotEmpty() }

                    if (activeTimeSlots.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No classes scheduled for today.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(activeTimeSlots) { (slot, students) ->
                                TimeSlotBatchCard(timeSlot = slot, students = students)
                            }
                        }
                    }
                } else {
                    // THIS WEEK VIEW: Group by Days of the Week
                    val activeDays = weekDays.map { day ->
                        day to timeSlots.map { slot ->
                            slot to studentList.filter { it.weeklySchedule[day] == slot }
                        }.filter { (_, students) -> students.isNotEmpty() }
                    }.filter { (_, slotList) -> slotList.isNotEmpty() }

                    if (activeDays.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No weekly schedule found.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(activeDays) { (day, slotList) ->
                                DayScheduleCard(dayName = day, slotsWithStudents = slotList)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeSlotBatchCard(timeSlot: String, students: List<StudentModel>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = timeSlot,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "${students.size} Enrolled",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                students.forEach { student ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = student.fullName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = "Class ${student.studentClass}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayScheduleCard(dayName: String, slotsWithStudents: List<Pair<String, List<StudentModel>>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            slotsWithStudents.forEach { (slot, students) ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = slot,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    students.forEach { student ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = student.fullName, style = MaterialTheme.typography.bodyMedium)
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        text = "Class ${student.studentClass}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
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
fun BatchesScreenPreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        BatchesScreen(onBack = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BatchesScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        BatchesScreen(onBack = {})
    }
}
