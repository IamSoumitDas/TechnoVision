@file:Suppress("UNCHECKED_CAST")

package com.soumit.technovision.ui.screens.attendance

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Parses date string in "dd-MM-yyyy" format into a LocalDate for sorting.
 */
private fun parseDateForSorting(dateStr: String): LocalDate {
    return try {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
        } else {
            LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        }
    } catch (_: Exception) {
        LocalDate.MIN
    }
}

/**
 * Formats a date string nicely (e.g. "Today, 05 Nov 2026", "Thursday, 24 Oct 2026").
 */
private fun formatDateHeading(dateStr: String): String {
    return try {
        val inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val localDate = LocalDate.parse(dateStr, inputFormatter)
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val dayString = when (localDate) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> localDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        }
        val formattedDate = localDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        "$dayString, $formattedDate"
    } catch (_: Exception) {
        dateStr
    }
}

/**
 * Normalizes date and time slot strings into a consistent lookup key.
 */
private fun normalizeSlotKey(date: String, timeSlot: String): String {
    val cleanDate = date.trim()
    val cleanSlot = timeSlot.replace(Regex("\\s+"), " ").trim().lowercase()
    return "${cleanDate}_${cleanSlot}"
}

/**
 * Resolves the genuine teacher's full name using:
 * 1. teacherAttendanceMap (from teacher_attendance collection which explicitly records teacherName for date & slot)
 * 2. submittedBy from attendance document (if not placeholder "Faculty")
 * 3. userMap (from users collection by UID or email)
 * 4. logsMap (from system_logs audit logs details or performedBy)
 */
private fun resolveTeacherName(
    record: AttendanceRecordModel,
    userMap: Map<String, String>,
    logsMap: Map<String, String>,
    teacherAttendanceMap: Map<String, String>
): String {
    val normKey = normalizeSlotKey(record.date, record.timeSlot)
    val rawKey = "${record.date}_${record.timeSlot}"

    // 1. Direct match from teacher_attendance collection (highest priority for historical and current batch sessions)
    val fromTeacherAttendance = teacherAttendanceMap[normKey] ?: teacherAttendanceMap[rawKey]
    if (!fromTeacherAttendance.isNullOrBlank() && !fromTeacherAttendance.equals("Faculty", ignoreCase = true)) {
        return fromTeacherAttendance
    }

    // 2. Check if submittedBy on the record is already an authentic teacher name
    if (record.submittedBy.isNotBlank() &&
        !record.submittedBy.equals("Faculty", ignoreCase = true) &&
        !record.submittedBy.contains("@") &&
        record.submittedBy.length < 35
    ) {
        return record.submittedBy
    }

    // 3. Direct match in userMap by UID or Email
    if (record.submittedByUid.isNotBlank() && userMap.containsKey(record.submittedByUid)) {
        val name = userMap[record.submittedByUid]
        if (!name.isNullOrBlank()) return name
    }
    if (record.submittedByEmail.isNotBlank()) {
        val mapped = userMap[record.submittedByEmail] ?: userMap[record.submittedByEmail.lowercase()]
        if (!mapped.isNullOrBlank()) return mapped
    }
    if (record.submittedBy.isNotBlank()) {
        val mapped = userMap[record.submittedBy] ?: userMap[record.submittedBy.lowercase()]
        if (!mapped.isNullOrBlank()) return mapped
    }

    // 4. Match against system_logs where batch or teacher attendance was audited
    val loggedName = logsMap[normKey] ?: logsMap[rawKey]
    if (!loggedName.isNullOrBlank() && !loggedName.equals("Faculty", ignoreCase = true)) {
        val mapped = userMap[loggedName] ?: userMap[loggedName.lowercase()]
        if (!mapped.isNullOrBlank()) return mapped
        if (!loggedName.contains("@") && loggedName.length < 35) {
            return loggedName
        }
        if (loggedName.contains("@")) {
            val prefix = loggedName.substringBefore("@")
            if (!prefix.equals("faculty", ignoreCase = true)) {
                return prefix
                    .replace(".", " ")
                    .replace("_", " ")
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
        }
    }

    // 5. If submittedBy has an email format, format it into a clean display name if it's not generic "faculty"
    if (record.submittedBy.contains("@")) {
        val prefix = record.submittedBy.substringBefore("@")
        if (!prefix.equals("faculty", ignoreCase = true)) {
            return prefix
                .replace(".", " ")
                .replace("_", " ")
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    }

    // 6. If submittedByEmail is present and not generic "faculty"
    if (record.submittedByEmail.contains("@")) {
        val prefix = record.submittedByEmail.substringBefore("@")
        if (!prefix.equals("faculty", ignoreCase = true)) {
            return prefix
                .replace(".", " ")
                .replace("_", " ")
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    }

    // 7. Fallback to existing non-blank submittedBy if present
    if (record.submittedBy.isNotBlank()) {
        return record.submittedBy
    }

    return "Faculty"
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AttendanceHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    val mockPreviewRecords = remember {
        listOf(
            AttendanceRecordModel(
                id = "05-11-2026_9_00_AM",
                date = "05-11-2026",
                timeSlot = "9:00 AM - 11:00 AM",
                presentStudentIds = listOf("s1", "s2", "s3"),
                absentStudentIds = listOf("s4"),
                totalPresent = 3,
                totalAbsent = 1,
                submittedBy = "Soumit Sir"
            ),
            AttendanceRecordModel(
                id = "05-11-2026_5_00_PM",
                date = "05-11-2026",
                timeSlot = "5:00 PM - 7:00 PM",
                presentStudentIds = listOf("s1", "s2"),
                absentStudentIds = emptyList(),
                totalPresent = 2,
                totalAbsent = 0,
                submittedBy = "Priya Ma'am"
            ),
            AttendanceRecordModel(
                id = "04-11-2026_11_00_AM",
                date = "04-11-2026",
                timeSlot = "11:00 AM - 1:00 PM",
                presentStudentIds = listOf("s2", "s3"),
                absentStudentIds = emptyList(),
                totalPresent = 2,
                totalAbsent = 0,
                submittedBy = "Soumit Sir"
            ),
            AttendanceRecordModel(
                id = "01-11-2026_1_00_PM",
                date = "01-11-2026",
                timeSlot = "1:00 PM - 3:00 PM",
                presentStudentIds = listOf("s1", "s4"),
                absentStudentIds = emptyList(),
                totalPresent = 2,
                totalAbsent = 0,
                submittedBy = "Debayan Sir"
            )
        )
    }

    val previewUserMap = remember {
        mapOf(
            "owner.preview@technovision.com" to "Soumit Das",
            "faculty@technovision.com" to "Amitabha Teacher"
        )
    }

    val previewTeacherAttendanceMap = remember {
        mapOf(
            "05-11-2026_9:00 AM - 11:00 AM" to "Soumit Sir",
            "05-11-2026_5:00 PM - 7:00 PM" to "Priya Ma'am",
            "04-11-2026_11:00 AM - 1:00 PM" to "Soumit Sir",
            "01-11-2026_1:00 PM - 3:00 PM" to "Debayan Sir"
        )
    }

    var attendanceRecords by remember {
        mutableStateOf(if (isInPreview) mockPreviewRecords else emptyList())
    }
    // Map email/uid -> fullName from users collection (aligned with SystemLogsScreen)
    var userMap by remember { mutableStateOf(if (isInPreview) previewUserMap else emptyMap()) }
    // Map "${date}_${timeSlot}" -> performedBy or teacher name from system_logs
    var logsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    // Map "${date}_${timeSlot}" -> teacherName from teacher_attendance collection
    var teacherAttendanceMap by remember {
        mutableStateOf(if (isInPreview) previewTeacherAttendanceMap else emptyMap())
    }
    var isLoading by remember { mutableStateOf(!isInPreview) }

    // Dialog & student name lookup states
    var selectedRecordForDetail by remember { mutableStateOf<AttendanceRecordModel?>(null) }
    var presentStudentNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var absentStudentNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingNames by remember { mutableStateOf(false) }

    // Fetch attendance logs, users mapping, teacher_attendance, and system_logs with lifecycle cleanup
    DisposableEffect(Unit) {
        if (isInPreview) {
            onDispose { }
        } else {
            val db = FirebaseFirestore.getInstance()

            // 0. Direct lookup of currently authenticated user profile
            val curUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            if (curUid.isNotBlank()) {
                db.collection("users").document(curUid).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            val fullName = doc.getString("fullName")
                                ?: doc.getString("name")
                                ?: doc.getString("displayName")
                            val email = doc.getString("email") ?: ""
                            if (!fullName.isNullOrBlank()) {
                                userMap = userMap + buildMap {
                                    put(curUid, fullName)
                                    if (email.isNotBlank()) {
                                        put(email, fullName)
                                        put(email.lowercase(), fullName)
                                    }
                                }
                            }
                        }
                    }
            }

            // 1. Fetch users collection to resolve names (exact same logic as SystemLogsScreen)
            val usersRegistration = db.collection("users").addSnapshotListener { userSnapshot, _ ->
                if (userSnapshot != null) {
                    val map = mutableMapOf<String, String>()
                    for (doc in userSnapshot.documents) {
                        val fullName = doc.getString("fullName")
                            ?: doc.getString("name")
                            ?: doc.getString("displayName")
                            ?: continue
                        val email = doc.getString("email") ?: ""
                        val uid = doc.id
                        if (email.isNotBlank()) {
                            map[email] = fullName
                            map[email.lowercase()] = fullName
                        }
                        map[uid] = fullName
                    }
                    userMap = userMap + map
                }
            }

            // 2. Fetch teacher_attendance collection to map (date + timeSlot) directly to genuine teacherName
            val teacherAttendanceRegistration = db.collection("teacher_attendance")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val map = mutableMapOf<String, String>()
                        for (doc in snapshot.documents) {
                            val teacherName = doc.getString("teacherName") ?: ""
                            val date = doc.getString("date") ?: ""
                            val timeSlot = doc.getString("timeSlot") ?: ""
                            if (teacherName.isNotBlank() && date.isNotBlank() && timeSlot.isNotBlank()) {
                                val normKey = normalizeSlotKey(date, timeSlot)
                                val rawKey = "${date}_${timeSlot}"
                                map[normKey] = teacherName
                                map[rawKey] = teacherName
                            }
                        }
                        teacherAttendanceMap = map
                    }
                }

            // 3. Fetch system_logs for batch attendance events to resolve who recorded attendance
            val logsRegistration = db.collection("system_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { logsSnapshot, _ ->
                    if (logsSnapshot != null) {
                        val map = mutableMapOf<String, String>()
                        val dateRegex = Regex("""\b(\d{2}-\d{2}-\d{4})\b""")
                        val timeSlotRegex = Regex("""(\d{1,2}:\d{2}\s*(?:AM|PM)\s*-\s*\d{1,2}:\d{2}\s*(?:AM|PM)|General / Other)""", RegexOption.IGNORE_CASE)
                        val byRegex = Regex("""\(by\s+([^)]+)\)|by\s+([A-Za-z\s.']+?)(?:\s+on\s+|\s*$|\))|for teacher\s+([A-Za-z\s.']+?)\s+on date""", RegexOption.IGNORE_CASE)

                        for (doc in logsSnapshot.documents) {
                            val action = doc.getString("action") ?: ""
                            if (action.contains("Attendance", ignoreCase = true)) {
                                val details = doc.getString("details") ?: ""
                                val performedBy = doc.getString("performedBy") ?: ""
                                val dMatch = dateRegex.find(details)?.value
                                val sMatch = timeSlotRegex.find(details)?.value
                                if (dMatch != null && sMatch != null) {
                                    val normKey = normalizeSlotKey(dMatch, sMatch)
                                    val rawKey = "${dMatch}_${sMatch}"

                                    // Check if details contains explicit teacher name
                                    val byMatch = byRegex.find(details)
                                    val extractedName = byMatch?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }?.trim()
                                    val resolvedFromLog = if (!extractedName.isNullOrBlank() && !extractedName.equals("Faculty", ignoreCase = true)) {
                                        extractedName
                                    } else {
                                        performedBy.takeIf { it.isNotBlank() }
                                    }

                                    if (resolvedFromLog != null) {
                                        if (!map.containsKey(normKey)) map[normKey] = resolvedFromLog
                                        if (!map.containsKey(rawKey)) map[rawKey] = resolvedFromLog
                                    }
                                }
                            }
                        }
                        logsMap = map
                    }
                }

            // 4. Fetch attendance records
            val attendanceRegistration = db.collection("attendance")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        isLoading = false
                        Toast.makeText(context, "Error loading history: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val list = snapshot.documents.map { doc ->
                            val pIds = doc.get("presentStudentIds") as? List<String> ?: emptyList()
                            val aIds = doc.get("absentStudentIds") as? List<String> ?: emptyList()
                            val addIds = doc.get("additionalStudentIds") as? List<String> ?: emptyList()
                            val submittedBy = doc.getString("submittedBy")
                                ?: doc.getString("takenBy")
                                ?: doc.getString("teacherName")
                                ?: doc.getString("recordedBy")
                                ?: ""
                            val submittedByUid = doc.getString("submittedByUid") ?: ""
                            val submittedByEmail = doc.getString("submittedByEmail")
                                ?: doc.getString("performedBy")
                                ?: ""

                            AttendanceRecordModel(
                                id = doc.id,
                                date = doc.getString("date") ?: "",
                                timeSlot = doc.getString("timeSlot") ?: "",
                                presentStudentIds = pIds,
                                absentStudentIds = aIds,
                                additionalStudentIds = addIds,
                                totalPresent = (doc.getLong("totalPresent") ?: pIds.size.toLong()).toInt(),
                                totalAbsent = (doc.getLong("totalAbsent") ?: aIds.size.toLong()).toInt(),
                                submittedBy = submittedBy,
                                submittedByUid = submittedByUid,
                                submittedByEmail = submittedByEmail,
                                timestamp = doc.getTimestamp("timestamp")
                            )
                        }
                        attendanceRecords = list
                        isLoading = false
                    }
                }
            onDispose {
                usersRegistration.remove()
                teacherAttendanceRegistration.remove()
                logsRegistration.remove()
                attendanceRegistration.remove()
            }
        }
    }

    // Effect to fetch student names when an attendance record is clicked
    LaunchedEffect(selectedRecordForDetail) {
        val record = selectedRecordForDetail
        if (record != null) {
            if (isInPreview) {
                presentStudentNames = listOf("Aarav Sharma", "Priya Das", "Rahul Roy")
                absentStudentNames = if (record.absentStudentIds.isNotEmpty()) listOf("Sneha Gupta") else emptyList()
                isLoadingNames = false
            } else {
                isLoadingNames = true
                val db = FirebaseFirestore.getInstance()
                val isOldCorruptedAbsentList = record.absentStudentIds.size > 15
                val genuineAbsentIds = if (isOldCorruptedAbsentList) emptyList() else record.absentStudentIds
                val uniqueIds = (record.presentStudentIds + genuineAbsentIds).distinct()

                if (uniqueIds.isEmpty()) {
                    presentStudentNames = emptyList()
                    absentStudentNames = emptyList()
                    isLoadingNames = false
                } else {
                    val tasks = uniqueIds.map { id ->
                        db.collection("students").document(id).get()
                    }
                    com.google.android.gms.tasks.Tasks.whenAllComplete(tasks)
                        .addOnCompleteListener {
                            val nameMap = mutableMapOf<String, String>()
                            for (task in tasks) {
                                if (task.isSuccessful) {
                                    val doc = task.result
                                    if (doc != null && doc.exists()) {
                                        val name = doc.getString("fullName") ?: doc.getString("name") ?: doc.id
                                        nameMap[doc.id] = name
                                    }
                                }
                            }
                            presentStudentNames = record.presentStudentIds.map { nameMap[it] ?: it }
                            absentStudentNames = genuineAbsentIds.map { nameMap[it] ?: it }
                            isLoadingNames = false
                        }
                }
            }
        }
    }

    // Group records by Date and Sort Dates newest first
    val groupedRecordsByDate = remember(attendanceRecords) {
        attendanceRecords.groupBy { it.date }
            .toList()
            .sortedByDescending { (dateStr, _) ->
                parseDateForSorting(dateStr)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Past Session Records",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (attendanceRecords.isNotEmpty()) {
                    Text(
                        text = "${attendanceRecords.size} Sessions Logged",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(groupedRecordsByDate, key = { (date, _) -> date }) { (dateStr, dateRecords) ->
                        DateAttendanceGroupCard(
                            dateStr = dateStr,
                            records = dateRecords,
                            userMap = userMap,
                            logsMap = logsMap,
                            teacherAttendanceMap = teacherAttendanceMap,
                            onRecordClick = { record ->
                                selectedRecordForDetail = record
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Detail Menu / Dialog showing session details
    if (selectedRecordForDetail != null) {
        val record = selectedRecordForDetail!!
        val formattedDate = remember(record.date) { formatDateHeading(record.date) }
        val detailTeacherName = remember(record, userMap, logsMap, teacherAttendanceMap) {
            resolveTeacherName(record, userMap, logsMap, teacherAttendanceMap)
        }

        AlertDialog(
            onDismissRequest = { selectedRecordForDetail = null },
            title = {
                Column {
                    Text(text = "Batch Attendance Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$formattedDate • ${record.timeSlot} • By: $detailTeacherName",
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
                        LoadingIndicator()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Attendance Summary Pill
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Students Present",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "${record.totalPresent} Attended",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Present Students List
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Present Students (${presentStudentNames.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (presentStudentNames.isEmpty()) {
                                Text(
                                    text = "No students recorded as present.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            } else {
                                presentStudentNames.forEach { name ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Scheduled but Absent Students
                        if (absentStudentNames.isNotEmpty() && absentStudentNames.size <= 10) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Scheduled but Absent (${absentStudentNames.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )

                                absentStudentNames.forEach { name ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Cancel,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
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

/**
 * Big Card representing a specific Date, sorted newest first.
 * Inside this card lives the actual attendance card(s) for each batch on this date.
 */
@Composable
fun DateAttendanceGroupCard(
    dateStr: String,
    records: List<AttendanceRecordModel>,
    userMap: Map<String, String>,
    logsMap: Map<String, String>,
    teacherAttendanceMap: Map<String, String>,
    onRecordClick: (AttendanceRecordModel) -> Unit
) {
    val headingText = remember(dateStr) { formatDateHeading(dateStr) }
    val totalStudentsPresentOnDate = remember(records) { records.sumOf { it.totalPresent } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Big Card Header: Date & Batch Count Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp).size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = headingText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${records.size} ${if (records.size == 1) "Batch Session" else "Batch Sessions"} • $totalStudentsPresentOnDate Present",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Inner: Actual attendance cards living inside this big date card
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                records.forEach { record ->
                    BatchAttendanceItemCard(
                        record = record,
                        userMap = userMap,
                        logsMap = logsMap,
                        teacherAttendanceMap = teacherAttendanceMap,
                        onClick = { onRecordClick(record) }
                    )
                }
            }
        }
    }
}

/**
 * The actual attendance card that lives inside the big date card.
 * Displays batch time slot, the teacher name who took it (resolved from users/logs), and students present.
 */
@Composable
fun BatchAttendanceItemCard(
    record: AttendanceRecordModel,
    userMap: Map<String, String>,
    logsMap: Map<String, String>,
    teacherAttendanceMap: Map<String, String>,
    onClick: () -> Unit
) {
    val teacherName = remember(record, userMap, logsMap, teacherAttendanceMap) {
        resolveTeacherName(record, userMap, logsMap, teacherAttendanceMap)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(6.dp).size(18.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = record.timeSlot,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "By: $teacherName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${record.totalPresent} Present",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View details",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
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
