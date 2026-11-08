package com.soumit.technovision.ui.screens.attendance

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * Calculates duration hours from a batch time slot string (e.g. "9:00 AM - 11:00 AM" -> 2.0 hrs).
 */
fun parseSlotDurationHours(timeSlot: String): Double {
    return try {
        val parts = timeSlot.split("-").map { it.trim() }
        if (parts.size == 2) {
            val format = SimpleDateFormat("h:mm a", Locale.US)
            val start = format.parse(parts[0])
            val end = format.parse(parts[1])
            if (start != null && end != null) {
                var diff = end.time - start.time
                if (diff < 0) {
                    diff += 12 * 60 * 60 * 1000L
                }
                val hours = diff.toDouble() / (1000 * 60 * 60)
                if (hours > 0) hours else 2.0
            } else 2.0
        } else 2.0
    } catch (_: Exception) {
        2.0
    }
}

/**
 * Parses "dd-MM-yyyy" into (year, month (0-11), dayOfMonth (1-31))
 */
fun parseRecordDate(dateStr: String): Triple<Int, Int, Int>? {
    val parts = dateStr.trim().split("-")
    if (parts.size == 3) {
        val d = parts[0].toIntOrNull()
        val m = parts[1].toIntOrNull()
        val y = parts[2].toIntOrNull()
        if (d != null && m != null && y != null) {
            return Triple(y, m - 1, d)
        }
    }
    return null
}

data class TeacherSummary(
    val teacherName: String,
    val monthClassesCount: Int,
    val allTimeClassesCount: Int,
    val monthHours: Double,
    val allTimeHours: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyAttendanceViewerScreen(
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToVerify: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    // Calendar month & year selection (Defaults to current month)
    val todayCal = remember { Calendar.getInstance() }
    val todayYear = remember { todayCal.get(Calendar.YEAR) }
    val todayMonth = remember { todayCal.get(Calendar.MONTH) } // 0-11

    var selectedYear by remember { mutableIntStateOf(todayYear) }
    var selectedMonth by remember { mutableIntStateOf(todayMonth) } // 0-11

    // Specific day selection within the month (null = full month)
    var selectedDayInMonth by remember { mutableStateOf<Int?>(null) }

    // Optional teacher filter
    var selectedTeacherFilter by remember { mutableStateOf<String?>(null) }

    // Status filter chip
    var selectedStatusFilter by remember { mutableStateOf("All") } // "All", "Verified", "Pending", "Rejected"

    // Mock data for preview mode
    val mockRecords = remember {
        val curYear = todayCal.get(Calendar.YEAR)
        val curMonth = todayCal.get(Calendar.MONTH) + 1
        fun fmt(day: Int): String = String.format(Locale.US, "%02d-%02d-%04d", day, curMonth, curYear)

        listOf(
            // Day 2: 1 teacher
            TeacherAttendanceRequestModel(
                id = "m1",
                teacherId = "t1",
                teacherName = "Amitabha Teacher",
                date = fmt(2),
                timeSlot = "9:00 AM - 11:00 AM",
                status = "Verified",
                verifiedBy = "Soumit Das (Owner)",
                timestamp = Timestamp.now()
            ),
            // Day 3: 2 teachers
            TeacherAttendanceRequestModel(
                id = "m2",
                teacherId = "t1",
                teacherName = "Amitabha Teacher",
                date = fmt(3),
                timeSlot = "9:00 AM - 11:00 AM",
                status = "Verified",
                verifiedBy = "Soumit Das (Owner)",
                timestamp = Timestamp.now()
            ),
            TeacherAttendanceRequestModel(
                id = "m3",
                teacherId = "t2",
                teacherName = "Priya Teacher",
                date = fmt(3),
                timeSlot = "11:00 AM - 1:00 PM",
                status = "Verified",
                verifiedBy = "Ananya (Receptionist)",
                timestamp = Timestamp.now()
            ),
            // Day 5: 3 teachers
            TeacherAttendanceRequestModel(
                id = "m4",
                teacherId = "t1",
                teacherName = "Amitabha Teacher",
                date = fmt(5),
                timeSlot = "9:00 AM - 11:00 AM",
                status = "Verified",
                verifiedBy = "Soumit Das (Owner)",
                timestamp = Timestamp.now()
            ),
            TeacherAttendanceRequestModel(
                id = "m5",
                teacherId = "t2",
                teacherName = "Priya Teacher",
                date = fmt(5),
                timeSlot = "1:00 PM - 3:00 PM",
                status = "Verified",
                verifiedBy = "Ananya (Receptionist)",
                timestamp = Timestamp.now()
            ),
            TeacherAttendanceRequestModel(
                id = "m6",
                teacherId = "t3",
                teacherName = "Ramesh Teacher",
                date = fmt(5),
                timeSlot = "5:00 PM - 7:00 PM",
                status = "Verified",
                verifiedBy = "Soumit Das (Owner)",
                timestamp = Timestamp.now()
            ),
            // Day 8: 1 teacher
            TeacherAttendanceRequestModel(
                id = "m7",
                teacherId = "t4",
                teacherName = "Tanmoy Teacher",
                date = fmt(8),
                timeSlot = "3:00 PM - 5:00 PM",
                status = "Verified",
                verifiedBy = "Ananya (Receptionist)",
                timestamp = Timestamp.now()
            ),
            // Day 10: 2 teachers
            TeacherAttendanceRequestModel(
                id = "m8",
                teacherId = "t2",
                teacherName = "Priya Teacher",
                date = fmt(10),
                timeSlot = "11:00 AM - 1:00 PM",
                status = "Verified",
                verifiedBy = "Soumit Das (Owner)",
                timestamp = Timestamp.now()
            ),
            TeacherAttendanceRequestModel(
                id = "m9",
                teacherId = "t4",
                teacherName = "Tanmoy Teacher",
                date = fmt(10),
                timeSlot = "5:00 PM - 7:00 PM",
                status = "Pending",
                timestamp = Timestamp.now()
            ),
            // Day 12: 1 teacher
            TeacherAttendanceRequestModel(
                id = "m10",
                teacherId = "t3",
                teacherName = "Ramesh Teacher",
                date = fmt(12),
                timeSlot = "1:00 PM - 3:00 PM",
                status = "Rejected",
                rejectedBy = "Soumit Das (Owner)",
                timestamp = Timestamp.now()
            )
        )
    }

    var recordsList by remember { mutableStateOf(if (isInPreview) mockRecords else emptyList()) }
    var isLoading by remember { mutableStateOf(!isInPreview) }

    // Fetch real-time records from Firestore with proper lifecycle disposal to prevent memory leaks
    DisposableEffect(Unit) {
        if (isInPreview) {
            onDispose { }
        } else {
            val db = FirebaseFirestore.getInstance()
            val registration = db.collection("teacher_attendance")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        isLoading = false
                        Toast.makeText(context, "Error loading records: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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
                                timestamp = doc.getTimestamp("timestamp"),
                                verifiedBy = doc.getString("verifiedBy") ?: "",
                                rejectedBy = doc.getString("rejectedBy") ?: ""
                            )
                        }
                        recordsList = list
                        isLoading = false
                    }
                }
            onDispose {
                registration.remove()
            }
        }
    }

    // Records filtered strictly for the selected Calendar Month & Year
    val monthRecords = remember(recordsList, selectedYear, selectedMonth) {
        recordsList.filter { record ->
            val parsed = parseRecordDate(record.date)
            if (parsed != null) {
                parsed.first == selectedYear && parsed.second == selectedMonth
            } else if (record.timestamp != null) {
                val c = Calendar.getInstance().apply { time = record.timestamp.toDate() }
                c.get(Calendar.YEAR) == selectedYear && c.get(Calendar.MONTH) == selectedMonth
            } else {
                false
            }
        }
    }

    // Teacher breakdown computation: (Which teacher did how much classes in this month vs all-time)
    val teacherSummaries = remember(recordsList, monthRecords) {
        val allTeacherNames = recordsList.map { it.teacherName.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        allTeacherNames.map { name ->
            val teacherAllRecords = recordsList.filter { it.teacherName.equals(name, ignoreCase = true) }
            val teacherMonthVerifiedRecords = monthRecords.filter {
                it.teacherName.equals(name, ignoreCase = true) &&
                    it.status.equals("Verified", ignoreCase = true)
            }

            val monthHours = teacherMonthVerifiedRecords.sumOf { parseSlotDurationHours(it.timeSlot) }
            val allHours = teacherAllRecords.sumOf { parseSlotDurationHours(it.timeSlot) }

            TeacherSummary(
                teacherName = name,
                monthClassesCount = teacherMonthVerifiedRecords.size,
                allTimeClassesCount = teacherAllRecords.size,
                monthHours = monthHours,
                allTimeHours = allHours
            )
        }.sortedByDescending { it.monthClassesCount }
    }

    // High level metrics for the selected month (verified only)
    val totalMonthClasses = remember(monthRecords) {
        monthRecords.count { it.status.equals("Verified", ignoreCase = true) }
    }

    // Records for the Attendance Log (taking into account selectedDayInMonth, selectedTeacherFilter, selectedStatusFilter)
    val filteredLogRecords = remember(monthRecords, selectedDayInMonth, selectedTeacherFilter, selectedStatusFilter, selectedYear, selectedMonth) {
        monthRecords.filter { record ->
            val matchesDay = if (selectedDayInMonth != null) {
                val parsed = parseRecordDate(record.date)
                val dayStr = String.format(Locale.US, "%02d-%02d-%04d", selectedDayInMonth!!, selectedMonth + 1, selectedYear)
                record.date == dayStr || (parsed != null && parsed.third == selectedDayInMonth)
            } else {
                true
            }

            val matchesTeacher = selectedTeacherFilter == null ||
                    record.teacherName.equals(selectedTeacherFilter, ignoreCase = true)

            val matchesStatus = when (selectedStatusFilter) {
                "Verified" -> record.status.equals("Verified", ignoreCase = true)
                "Pending" -> record.status.equals("Pending", ignoreCase = true)
                "Rejected" -> record.status.equals("Rejected", ignoreCase = true)
                else -> true
            }

            matchesDay && matchesTeacher && matchesStatus
        }
    }

    // Month display name formatted (e.g. "November 2026")
    val selectedMonthDisplayName = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    val isCurrentRealMonth = (selectedYear == todayYear && selectedMonth == todayMonth)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Faculty Attendance",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                // Removed all buttons from top app bar as requested
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Month Navigator Header
            item {
                MonthNavigatorCard(
                    monthTitle = selectedMonthDisplayName,
                    isCurrentMonth = isCurrentRealMonth,
                    onPreviousMonth = {
                        selectedDayInMonth = null
                        if (selectedMonth == 0) {
                            selectedMonth = 11
                            selectedYear -= 1
                        } else {
                            selectedMonth -= 1
                        }
                    },
                    onNextMonth = {
                        selectedDayInMonth = null
                        if (selectedMonth == 11) {
                            selectedMonth = 0
                            selectedYear += 1
                        } else {
                            selectedMonth += 1
                        }
                    },
                    onResetToCurrentMonth = {
                        selectedDayInMonth = null
                        selectedYear = todayYear
                        selectedMonth = todayMonth
                    }
                )
            }

            // 3. Teacher Breakdown Section ("Which teacher did how much classes")
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                            Column {
                                Text(
                                    text = "Faculty Class Distribution",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Classes taken by each teacher in $selectedMonthDisplayName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            if (selectedTeacherFilter != null) {
                                TextButton(
                                    onClick = { selectedTeacherFilter = null },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Show All")
                                }
                            }
                        }

                        if (teacherSummaries.isEmpty()) {
                            Text(
                                text = "No faculty data logged yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                teacherSummaries.forEach { summary ->
                                    val isSelected = selectedTeacherFilter.equals(summary.teacherName, ignoreCase = true)
                                    val progress = if (totalMonthClasses > 0) {
                                        summary.monthClassesCount.toFloat() / totalMonthClasses.toFloat()
                                    } else 0f

                                    Surface(
                                        onClick = {
                                            selectedTeacherFilter = if (isSelected) null else summary.teacherName
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    Text(
                                                        text = summary.teacherName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                Text(
                                                    text = "${summary.monthClassesCount} ${if (summary.monthClassesCount == 1) "class" else "classes"}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            if (totalMonthClasses > 0) {
                                                LinearProgressIndicator(
                                                    progress = { progress },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(4.dp)
                                                        .clip(RoundedCornerShape(2.dp)),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
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

            // 5. Attendance Log Section Header & Filter Controls
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Attendance Log (${filteredLogRecords.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Clear filter actions
                        if (selectedDayInMonth != null || selectedTeacherFilter != null || selectedStatusFilter != "All") {
                            TextButton(
                                onClick = {
                                    selectedDayInMonth = null
                                    selectedTeacherFilter = null
                                    selectedStatusFilter = "All"
                                }
                            ) {
                                Text("Reset Filters")
                            }
                        }
                    }

                    // Active Filter Indicator Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status Filter Chips
                        val statuses = listOf("All", "Verified", "Pending", "Rejected")
                        statuses.forEach { status ->
                            FilterChip(
                                selected = selectedStatusFilter == status,
                                onClick = { selectedStatusFilter = status },
                                label = { Text(status) }
                            )
                        }

                        // Day filter tag
                        if (selectedDayInMonth != null) {
                            SuggestionChip(
                                onClick = { selectedDayInMonth = null },
                                label = {
                                    Text("Day: $selectedDayInMonth $selectedMonthDisplayName ✕")
                                }
                            )
                        }

                        // Teacher filter tag
                        if (selectedTeacherFilter != null) {
                            SuggestionChip(
                                onClick = { selectedTeacherFilter = null },
                                label = {
                                    Text("Teacher: $selectedTeacherFilter ✕")
                                }
                            )
                        }
                    }
                }
            }

            // 6. Attendance Log Items (NO click menu/dialog, includes 'Verified by $who')
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    }
                }
            } else if (filteredLogRecords.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No Attendance Records",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "There are no faculty attendance sessions logged for the selected criteria.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredLogRecords, key = { it.id }) { record ->
                    AttendanceLogCard(record = record)
                }
            }
        }
    }
}

/**
 * Month Navigation Component: previous, next, and current month label.
 */
@Composable
fun MonthNavigatorCard(
    monthTitle: String,
    isCurrentMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToCurrentMonth: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!isCurrentMonth) {
                    Text(
                        text = "Jump to Today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onResetToCurrentMonth)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                } else {
                    Text(
                        text = "Current Month",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
            }
        }
    }
}

/**
 * Clean Attendance Log Card (NO onclick menu/dialog; prominently shows 'Verified by $who')
 */
@Composable
fun AttendanceLogCard(
    record: TeacherAttendanceRequestModel
) {
    val isVerified = record.status.equals("Verified", ignoreCase = true)
    val isRejected = record.status.equals("Rejected", ignoreCase = true)

    val durationHours = remember(record.timeSlot) { parseSlotDurationHours(record.timeSlot) }
    val durationText = if (durationHours == durationHours.toInt().toDouble()) "${durationHours.toInt()}h class" else "${String.format(Locale.US, "%.1f", durationHours)}h class"

    val statusColor = when {
        isVerified -> Color(0xFF2E7D32)
        isRejected -> MaterialTheme.colorScheme.error
        else -> Color(0xFFF57C00)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            1.dp,
            when {
                isVerified -> Color(0xFF2E7D32).copy(alpha = 0.3f)
                isRejected -> MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Name & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = record.teacherName.ifBlank { "Faculty Member" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Faculty • $durationText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Status Badge
                Surface(
                    color = when {
                        isVerified -> Color(0xFFE8F5E9)
                        isRejected -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        else -> Color(0xFFFFF3E0)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isVerified -> Icons.Default.CheckCircle
                                isRejected -> Icons.Default.Cancel
                                else -> Icons.Default.HourglassTop
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = record.status,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Time & Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = record.timeSlot,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = record.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Verification Banner: Prominently displaying "Verified by $who" from Firebase
            /*Surface(
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    isVerified -> Color(0xFFE8F5E9)
                    isRejected -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    else -> Color(0xFFFFF8E1)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when {
                            isVerified -> Icons.Default.VerifiedUser
                            isRejected -> Icons.Default.Cancel
                            else -> Icons.Default.HourglassTop
                        },
                        contentDescription = null,
                        tint = when {
                            isVerified -> Color(0xFF1B5E20)
                            isRejected -> MaterialTheme.colorScheme.error
                            else -> Color(0xFFE65100)
                        },
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = when {
                            isVerified -> {
                                val verifier = record.verifiedBy.ifBlank { "Admin" }
                                "Verified by $verifier"
                            }
                            isRejected -> {
                                val rejector = record.rejectedBy.ifBlank { "Admin" }
                                "Rejected by $rejector"
                            }
                            else -> "Status: Pending verification"
                        },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isVerified -> Color(0xFF1B5E20)
                            isRejected -> MaterialTheme.colorScheme.error
                            else -> Color(0xFFE65100)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }*/
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun FacultyAttendanceViewerScreenPreview() {
    TechnoVisionTheme {
        FacultyAttendanceViewerScreen(onBack = {})
    }
}
