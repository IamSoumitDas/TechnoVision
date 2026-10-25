package com.soumit.technovision.ui.screens.faculty

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.soumit.technovision.ui.screens.SystemLogger
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import java.text.SimpleDateFormat
import java.util.*

data class TeacherAttendanceRequestModel(
    val id: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val date: String = "",
    val timeSlot: String = "",
    val status: String = "Pending",
    val timestamp: Timestamp? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAttendanceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    // Safely bridge Compose locale to Java Locale without non-existent extensions
    val composeLocale = androidx.compose.ui.text.intl.Locale.current
    val javaLocale = Locale.forLanguageTag(composeLocale.toLanguageTag())
    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy", javaLocale) }
    val todayDateStr = remember { dateFormat.format(Date()) }

    var selectedDateStr by remember { mutableStateOf(todayDateStr) }

    val calendarInstance = remember { Calendar.getInstance() }
    var displayedYear by remember { mutableStateOf(calendarInstance.get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableStateOf(calendarInstance.get(Calendar.MONTH)) }

    val timeSlots = listOf(
        "9:00 AM - 11:00 AM",
        "11:00 AM - 1:00 PM",
        "1:00 PM - 3:00 PM",
        "3:00 PM - 5:00 PM",
        "5:00 PM - 7:00 PM",
        "7:00 PM - 9:00 PM"
    )

    var selectedTimeSlot by remember { mutableStateOf(timeSlots[0]) }
    var timeSlotExpanded by remember { mutableStateOf(false) }

    var allMyRequests by remember { mutableStateOf<List<TeacherAttendanceRequestModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    val currentUserId = if (!isInPreview) {
        FirebaseAuth.getInstance().currentUser?.uid ?: ""
    } else {
        "preview_teacher_id"
    }

    LaunchedEffect(currentUserId) {
        if (isInPreview) {
            allMyRequests = listOf(
                TeacherAttendanceRequestModel(
                    id = "1",
                    teacherName = "Amitabha Teacher",
                    date = todayDateStr,
                    timeSlot = "9:00 AM - 11:00 AM",
                    status = "Verified"
                ),
                TeacherAttendanceRequestModel(
                    id = "2",
                    teacherName = "Amitabha Teacher",
                    date = todayDateStr,
                    timeSlot = "5:00 PM - 7:00 PM",
                    status = "Pending"
                )
            )
            isLoading = false
        } else {
            FirebaseFirestore.getInstance().collection("teacher_attendance")
                .whereEqualTo("teacherId", currentUserId)
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
                        allMyRequests = list
                        isLoading = false
                    }
                }
        }
    }

    val requestsForSelectedDate = remember(allMyRequests, selectedDateStr) {
        allMyRequests.filter { it.date == selectedDateStr }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Batch Attendance Request") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Selected Request Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                Text(text = selectedDateStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Surface(
                                color = if (selectedDateStr == todayDateStr) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = if (selectedDateStr == todayDateStr) "Same-Day Active" else "Viewing Past Log",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = if (selectedDateStr == todayDateStr) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        Text(
                            text = "Select Completed Batch Time Slot",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )

                        ExposedDropdownMenuBox(
                            expanded = timeSlotExpanded,
                            onExpandedChange = { timeSlotExpanded = !timeSlotExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedTimeSlot,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Batch Time Slot") },
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

                        if (isSubmitting) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (selectedDateStr != todayDateStr) {
                                        Toast.makeText(context, "Attendance can only be requested for today's date before midnight.", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }

                                    val alreadyRequested = requestsForSelectedDate.any { it.timeSlot == selectedTimeSlot }
                                    if (alreadyRequested) {
                                        Toast.makeText(context, "You have already requested attendance for this slot today.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    if (isInPreview) {
                                        Toast.makeText(context, "Preview: Request submitted successfully!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    isSubmitting = true
                                    val db = FirebaseFirestore.getInstance()

                                    db.collection("users").document(currentUserId).get()
                                        .addOnSuccessListener { userDoc ->
                                            val teacherName = if (userDoc.exists()) userDoc.getString("fullName") ?: "Faculty" else "Faculty"

                                            val requestData = hashMapOf(
                                                "teacherId" to currentUserId,
                                                "teacherName" to teacherName,
                                                "date" to selectedDateStr,
                                                "timeSlot" to selectedTimeSlot,
                                                "status" to "Pending",
                                                "timestamp" to Timestamp.now()
                                            )

                                            db.collection("teacher_attendance").add(requestData)
                                                .addOnSuccessListener {
                                                    isSubmitting = false
                                                    Toast.makeText(context, "Attendance request sent to admin!", Toast.LENGTH_SHORT).show()

                                                    // Log the attendance request action
                                                    SystemLogger.log(
                                                        action = "Attendance Requested",
                                                        details = "Teacher $teacherName submitted time slot [$selectedTimeSlot] for date $selectedDateStr"
                                                    )
                                                }
                                                .addOnFailureListener { e ->
                                                    isSubmitting = false
                                                    Toast.makeText(context, "Failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                Text("Request Attendance for Slot")
                            }
                        }
                    }
                }
            }

            item {
                MonthlyCalendarWidget(
                    displayedYear = displayedYear,
                    displayedMonth = displayedMonth,
                    selectedDateStr = selectedDateStr,
                    todayDateStr = todayDateStr,
                    allRequests = allMyRequests,
                    onPrevMonth = {
                        if (displayedMonth == 0) {
                            displayedMonth = 11
                            displayedYear -= 1
                        } else {
                            displayedMonth -= 1
                        }
                    },
                    onNextMonth = {
                        if (displayedMonth == 11) {
                            displayedMonth = 0
                            displayedYear += 1
                        } else {
                            displayedMonth += 1
                        }
                    },
                    onDateSelected = { newDate ->
                        selectedDateStr = newDate
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Requests for $selectedDateStr (${requestsForSelectedDate.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (requestsForSelectedDate.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No attendance requests found for this date.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(requestsForSelectedDate) { req ->
                    TeacherRequestCard(request = req)
                }
            }
        }
    }
}

@Composable
fun MonthlyCalendarWidget(
    displayedYear: Int,
    displayedMonth: Int,
    selectedDateStr: String,
    todayDateStr: String,
    allRequests: List<TeacherAttendanceRequestModel>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val monthNames = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val daysOfWeekHeaders = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${monthNames[displayedMonth]} $displayedYear",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row {
                    IconButton(onClick = onPrevMonth) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                    }
                    IconButton(onClick = onNextMonth) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                daysOfWeekHeaders.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            val calendar = Calendar.getInstance().apply {
                set(displayedYear, displayedMonth, 1)
            }
            val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val startDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1

            val totalCells = startDayOfWeek + maxDays
            val rows = (totalCells + 6) / 7

            val composeLocale = androidx.compose.ui.text.intl.Locale.current
            val javaLocale = Locale.forLanguageTag(composeLocale.toLanguageTag())
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", javaLocale)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (col in 0 until 7) {
                            val dayIndex = row * 7 + col
                            val dayNumber = dayIndex - startDayOfWeek + 1

                            if (dayNumber in 1..maxDays) {
                                val cellCal = Calendar.getInstance().apply {
                                    set(displayedYear, displayedMonth, dayNumber)
                                }
                                val cellDateStr = dateFormat.format(cellCal.time)

                                val isSelected = cellDateStr == selectedDateStr
                                val isToday = cellDateStr == todayDateStr

                                val dayRequests = allRequests.filter { it.date == cellDateStr }
                                val hasVerified = dayRequests.any { it.status == "Verified" }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { onDateSelected(cellDateStr) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        if (dayRequests.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (hasVerified) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.tertiary
                                                    )
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(5.dp))
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherRequestCard(request: TeacherAttendanceRequestModel) {
    val isVerified = request.status == "Verified"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isVerified) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = request.timeSlot,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
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
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun TeacherAttendanceScreenPreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        TeacherAttendanceScreen(onBack = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TeacherAttendanceScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        TeacherAttendanceScreen(onBack = {})
    }
}