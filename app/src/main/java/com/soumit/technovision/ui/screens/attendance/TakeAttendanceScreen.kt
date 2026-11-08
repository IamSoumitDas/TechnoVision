package com.soumit.technovision.ui.screens.attendance

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
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
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import com.soumit.technovision.util.SystemLogger
import java.text.SimpleDateFormat
import java.util.*

private val BATCH_TIME_SLOTS = listOf(
    "9:00 AM - 11:00 AM",
    "11:00 AM - 1:00 PM",
    "1:00 PM - 3:00 PM",
    "3:00 PM - 5:00 PM",
    "5:00 PM - 7:00 PM",
    "7:00 PM - 9:00 PM",
    "General / Other"
)

/**
 * Data class to hold details of an existing attendance submission for the batch.
 */
data class ExistingAttendanceInfo(
    val id: String,
    val submittedBy: String,
    val timeFormatted: String,
    val totalPresent: Int,
    val totalAbsent: Int
)

/**
 * Automatically determines the active batch time slot based on current device time.
 */
private fun getAutomaticTimeSlot(): String {
    val cal = Calendar.getInstance()
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val totalMinutes = hour * 60 + minute

    return when {
        totalMinutes < 11 * 60 -> "9:00 AM - 11:00 AM"
        totalMinutes < 13 * 60 -> "11:00 AM - 1:00 PM"
        totalMinutes < 15 * 60 -> "1:00 PM - 3:00 PM"
        totalMinutes < 17 * 60 -> "3:00 PM - 5:00 PM"
        totalMinutes < 19 * 60 -> "5:00 PM - 7:00 PM"
        totalMinutes < 21 * 60 -> "7:00 PM - 9:00 PM"
        else -> "7:00 PM - 9:00 PM"
    }
}

/**
 * Returns the English name of the current day of the week matching weeklySchedule keys.
 */
private fun getTodayDayName(): String {
    val calendar = Calendar.getInstance()
    val dayOfWeekIndex = calendar.get(Calendar.DAY_OF_WEEK)
    val daysArray = arrayOf("", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    return if (dayOfWeekIndex in daysArray.indices) daysArray[dayOfWeekIndex] else "Sunday"
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TakeAttendanceScreen(
    onBack: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    // Today's day name (e.g. "Monday", "Thursday")
    val todayName = remember { getTodayDayName() }

    // Current Date formatted as DD-MM-YYYY
    val currentDate = remember {
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    }

    // 1. Time automatically selected based on current time
    val autoTimeSlot = remember { getAutomaticTimeSlot() }
    var selectedTimeSlot by remember { mutableStateOf(autoTimeSlot) }
    var timeSlotExpanded by remember { mutableStateOf(false) }

    // Master student list fetched from Firestore
    val allStudents = remember {
        mutableStateListOf<StudentModel>().apply {
            if (isInPreview) {
                addAll(
                    listOf(
                        StudentModel(
                            id = "1",
                            fullName = "Aarav Sharma",
                            studentClass = "X",
                            school = "St. Xavier's School",
                            weeklySchedule = mapOf(
                                todayName to autoTimeSlot,
                                "Tuesday" to "9:00 AM - 11:00 AM"
                            )
                        ),
                        StudentModel(
                            id = "2",
                            fullName = "Priya Das",
                            studentClass = "XII",
                            school = "Alipurduar Girls High School",
                            weeklySchedule = mapOf(
                                todayName to autoTimeSlot,
                                "Wednesday" to "5:00 PM - 7:00 PM"
                            )
                        ),
                        StudentModel(
                            id = "3",
                            fullName = "Rahul Roy",
                            studentClass = "VIII",
                            school = "Kendriya Vidyalaya",
                            weeklySchedule = mapOf(
                                "Sunday" to "9:00 AM - 11:00 AM",
                                "Friday" to "1:00 PM - 3:00 PM"
                            )
                        ),
                        StudentModel(
                            id = "4",
                            fullName = "Debayan Roy",
                            studentClass = "IX",
                            school = "Mc William High School (HS)",
                            weeklySchedule = mapOf(
                                "Sunday" to "11:00 AM - 1:00 PM",
                                "Saturday" to "3:00 PM - 5:00 PM"
                            )
                        )
                    )
                )
            }
        }
    }
    var isLoading by remember { mutableStateOf(!isInPreview) }
    var isSaving by remember { mutableStateOf(false) }

    // Map to track attendance: Student ID -> Boolean (true = Present, false = Absent)
    val attendanceStates = remember { mutableStateMapOf<String, Boolean>() }

    // 3. Extra students added who are attending out of their regular scheduled time
    val additionalStudents = remember { mutableStateListOf<StudentModel>() }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Strict duplicate check state: tracks if attendance has already been recorded for this batch
    var isCheckingExistingAttendance by remember { mutableStateOf(false) }
    var existingAttendanceInfo by remember { mutableStateOf<ExistingAttendanceInfo?>(null) }
    val isAttendanceLocked = existingAttendanceInfo != null

    // 2. Scheduled students for the selected day and time slot (matches BatchesScreen logic)
    val scheduledStudents = remember(allStudents.toList(), todayName, selectedTimeSlot) {
        allStudents.filter { student ->
            student.weeklySchedule[todayName] == selectedTimeSlot ||
                (student.weeklySchedule.isEmpty() && student.batchTimeSlot == selectedTimeSlot)
        }
    }

    // User map from users collection (aligned with SystemLogsScreen) to resolve authentic teacher names
    var userMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var currentUserName by remember { mutableStateOf("Faculty") }

    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    // Direct lookup of current user profile to ensure authentic teacher name is retrieved even if collection listing is restricted
    LaunchedEffect(currentUserId) {
        if (!isInPreview && currentUserId.isNotBlank()) {
            FirebaseFirestore.getInstance().collection("users").document(currentUserId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val name = doc.getString("fullName")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("name")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("displayName")?.takeIf { it.isNotBlank() }
                        if (name != null) {
                            currentUserName = name
                        }
                    }
                }
        }
    }

    // Fetch user map and fallback resolution from users collection
    LaunchedEffect(Unit) {
        if (!isInPreview) {
            FirebaseFirestore.getInstance().collection("users").get()
                .addOnSuccessListener { snapshot ->
                    val map = mutableMapOf<String, String>()
                    for (doc in snapshot.documents) {
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
                    userMap = map

                    val curUid = FirebaseAuth.getInstance().currentUser?.uid
                    val curEmail = FirebaseAuth.getInstance().currentUser?.email
                    val resolved = map[curUid] ?: map[curEmail]
                        ?: FirebaseAuth.getInstance().currentUser?.displayName?.takeIf { it.isNotBlank() }
                        ?: curEmail?.substringBefore("@")
                        ?: "Faculty"
                    if (currentUserName == "Faculty" && resolved != "Faculty") {
                        currentUserName = resolved
                    }
                }
        }
    }

    // Check if attendance already exists for this date and time slot to strictly prevent duplicates
    LaunchedEffect(selectedTimeSlot, currentDate, userMap) {
        additionalStudents.clear()
        isSearchExpanded = false
        searchQuery = ""

        if (isInPreview) {
            existingAttendanceInfo = null
            return@LaunchedEffect
        }

        isCheckingExistingAttendance = true
        val slotIdTag = selectedTimeSlot.replace(Regex("[^A-Za-z0-9]"), "_")
        val attendanceDocId = "${currentDate}_$slotIdTag"

        FirebaseFirestore.getInstance().collection("attendance").document(attendanceDocId)
            .get()
            .addOnSuccessListener { doc ->
                isCheckingExistingAttendance = false
                if (doc != null && doc.exists()) {
                    val rawSubmitted = doc.getString("submittedBy") ?: ""
                    val rawUid = doc.getString("submittedByUid") ?: ""
                    val rawEmail = doc.getString("submittedByEmail") ?: doc.getString("takenBy") ?: ""

                    val recordedBy = userMap[rawUid]
                        ?: userMap[rawEmail]
                        ?: userMap[rawSubmitted]
                        ?: rawSubmitted.takeIf { it.isNotBlank() && it != "Faculty" && !it.contains("@") }
                        ?: rawEmail.takeIf { it.contains("@") }?.substringBefore("@")?.replace(".", " ")?.replaceFirstChar { it.uppercase() }
                        ?: "Another Faculty/User"

                    val timestamp = doc.getTimestamp("timestamp")
                    val formattedTime = if (timestamp != null) {
                        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(timestamp.toDate())
                    } else {
                        "Earlier today"
                    }
                    val presentCount = (doc.getLong("totalPresent") ?: 0L).toInt()
                    val absentCount = (doc.getLong("totalAbsent") ?: 0L).toInt()

                    existingAttendanceInfo = ExistingAttendanceInfo(
                        id = doc.id,
                        submittedBy = recordedBy,
                        timeFormatted = formattedTime,
                        totalPresent = presentCount,
                        totalAbsent = absentCount
                    )

                    // Load recorded attendance states into UI for inspection
                    @Suppress("UNCHECKED_CAST")
                    val presentIds = doc.get("presentStudentIds") as? List<String> ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val absentIds = doc.get("absentStudentIds") as? List<String> ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val extraIds = doc.get("additionalStudentIds") as? List<String> ?: emptyList()

                    presentIds.forEach { attendanceStates[it] = true }
                    absentIds.forEach { attendanceStates[it] = false }

                    if (extraIds.isNotEmpty()) {
                        val extras = allStudents.filter { it.id in extraIds }
                        additionalStudents.clear()
                        additionalStudents.addAll(extras)
                    }
                } else {
                    existingAttendanceInfo = null
                }
            }
            .addOnFailureListener {
                isCheckingExistingAttendance = false
                existingAttendanceInfo = null
            }
    }

    // Ensure default attendance state (false) for scheduled students when not locked
    LaunchedEffect(scheduledStudents) {
        if (!isAttendanceLocked) {
            scheduledStudents.forEach { student ->
                if (!attendanceStates.containsKey(student.id)) {
                    attendanceStates[student.id] = false
                }
            }
        }
    }

    // Fetch all students on load with their weekly schedule
    LaunchedEffect(Unit) {
        if (!isInPreview) {
            FirebaseFirestore.getInstance().collection("students")
                .orderBy("fullName")
                .get()
                .addOnSuccessListener { result ->
                    val list = result.documents.map { doc ->
                        @Suppress("UNCHECKED_CAST")
                        val scheduleMap = doc.get("weeklySchedule") as? Map<String, String> ?: emptyMap()

                        StudentModel(
                            id = doc.id,
                            fullName = doc.getString("fullName") ?: "",
                            studentClass = doc.getString("studentClass") ?: "",
                            school = doc.getString("school") ?: "",
                            batchTimeSlot = doc.getString("batchTimeSlot") ?: "",
                            weeklySchedule = scheduleMap
                        )
                    }
                    allStudents.clear()
                    allStudents.addAll(list)
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    Toast.makeText(context, "Error loading students: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Filter available students for search (excluding already displayed ones)
    val searchCandidates = remember(allStudents.toList(), scheduledStudents, additionalStudents.toList(), searchQuery) {
        val currentIds = (scheduledStudents.map { it.id } + additionalStudents.map { it.id }).toSet()
        val unassigned = allStudents.filter { it.id !in currentIds }

        if (searchQuery.isBlank()) {
            unassigned
        } else {
            unassigned.filter { student ->
                student.fullName.contains(searchQuery, ignoreCase = true) ||
                    student.studentClass.contains(searchQuery, ignoreCase = true) ||
                    student.school.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val totalActiveStudents = scheduledStudents.size + additionalStudents.size
    val allActivePresent = totalActiveStudents > 0 &&
        (scheduledStudents + additionalStudents).all { attendanceStates[it.id] == true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Take Attendance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "$todayName, $currentDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Attendance History")
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
                        LoadingIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (isAttendanceLocked) {
                        // Strict lock indicator preventing submission
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContentColor = MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Attendance Locked (Already Recorded)")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (isInPreview) {
                                    Toast.makeText(context, "Preview: Attendance Saved!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                    return@Button
                                }

                                val displayedStudents = scheduledStudents + additionalStudents
                                if (displayedStudents.isEmpty()) {
                                    Toast.makeText(context, "No students in this session to save.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isSaving = true
                                val db = FirebaseFirestore.getInstance()

                                val presentIds = displayedStudents.filter { attendanceStates[it.id] == true }.map { it.id }
                                val absentIds = scheduledStudents.filter { attendanceStates[it.id] != true }.map { it.id }

                                val slotIdTag = selectedTimeSlot.replace(Regex("[^A-Za-z0-9]"), "_")
                                val attendanceDocId = "${currentDate}_$slotIdTag"
                                val docRef = db.collection("attendance").document(attendanceDocId)

                                val currentUser = FirebaseAuth.getInstance().currentUser
                                val currentUserUid = currentUser?.uid ?: ""
                                val currentUserEmail = currentUser?.email ?: ""
                                val resolvedUserName = currentUserName.takeIf { it.isNotBlank() && it != "Faculty" }
                                    ?: userMap[currentUserUid]
                                    ?: userMap[currentUserEmail]
                                    ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
                                    ?: currentUserEmail.takeIf { it.isNotBlank() }?.substringBefore("@")?.replace(".", " ")?.replaceFirstChar { it.uppercase() }
                                    ?: "Faculty"

                                // Strict transactional write: atomicity ensures no concurrent duplicate submissions
                                db.runTransaction { transaction ->
                                    val snapshot = transaction.get(docRef)
                                    if (snapshot.exists()) {
                                        val recorder = snapshot.getString("submittedBy")
                                            ?: snapshot.getString("takenBy")
                                            ?: "another user"
                                        throw IllegalStateException("Attendance for this batch has already been recorded by $recorder.")
                                    }

                                    val attendanceRecord = hashMapOf(
                                        "date" to currentDate,
                                        "timeSlot" to selectedTimeSlot,
                                        "presentStudentIds" to presentIds,
                                        "absentStudentIds" to absentIds,
                                        "additionalStudentIds" to additionalStudents.map { it.id },
                                        "totalPresent" to presentIds.size,
                                        "totalAbsent" to absentIds.size,
                                        "submittedBy" to resolvedUserName,
                                        "submittedByUid" to currentUserUid,
                                        "submittedByEmail" to currentUserEmail,
                                        "timestamp" to com.google.firebase.Timestamp.now()
                                    )

                                    transaction.set(docRef, attendanceRecord)
                                }
                                    .addOnSuccessListener {
                                        isSaving = false
                                        Toast.makeText(context, "Attendance saved successfully!", Toast.LENGTH_SHORT).show()

                                        val extraInfo = if (additionalStudents.isNotEmpty()) " (${additionalStudents.size} out of schedule)" else ""
                                        SystemLogger.log(
                                            action = "Batch Attendance Saved",
                                            details = "Recorded attendance for $currentDate [$selectedTimeSlot]. Present: ${presentIds.size}, Absent: ${absentIds.size}$extraInfo (by $resolvedUserName)"
                                        )

                                        onBack()
                                    }
                                    .addOnFailureListener { e ->
                                        isSaving = false
                                        val errorMsg = e.localizedMessage ?: "Failed to save attendance"
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()

                                        // Refresh the duplicate lock state in case another user just submitted
                                        docRef.get().addOnSuccessListener { freshDoc ->
                                            if (freshDoc != null && freshDoc.exists()) {
                                                val rec = freshDoc.getString("submittedBy") ?: freshDoc.getString("takenBy") ?: "Another User"
                                                val ts = freshDoc.getTimestamp("timestamp")
                                                val fTime = if (ts != null) {
                                                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(ts.toDate())
                                                } else "earlier today"

                                                existingAttendanceInfo = ExistingAttendanceInfo(
                                                    id = freshDoc.id,
                                                    submittedBy = rec,
                                                    timeFormatted = fTime,
                                                    totalPresent = (freshDoc.getLong("totalPresent") ?: 0L).toInt(),
                                                    totalAbsent = (freshDoc.getLong("totalAbsent") ?: 0L).toInt()
                                                )
                                            }
                                        }
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Time Slot Selector Header
            Text(
                text = "Batch Time Slot:",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            ExposedDropdownMenuBox(
                expanded = timeSlotExpanded,
                onExpandedChange = { timeSlotExpanded = !timeSlotExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                OutlinedTextField(
                    value = selectedTimeSlot,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Active Time Slot") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeSlotExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                )
                ExposedDropdownMenu(
                    expanded = timeSlotExpanded,
                    onDismissRequest = { timeSlotExpanded = false }
                ) {
                    BATCH_TIME_SLOTS.forEach { slot ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(slot)
                                    if (slot == autoTimeSlot) {
                                        Text(
                                            text = "Current",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            onClick = {
                                selectedTimeSlot = slot
                                timeSlotExpanded = false
                            }
                        )
                    }
                }
            }

            // Strict Duplicate Warning Banner
            existingAttendanceInfo?.let { existing ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(top = 2.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Attendance Already Recorded",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Attendance for this batch ($selectedTimeSlot) was already submitted today at ${existing.timeFormatted} by ${existing.submittedBy} (${existing.totalPresent} Present, ${existing.totalAbsent} Absent).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Multiple attendance submissions for the same batch are restricted to maintain record integrity.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = onNavigateToHistory,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Assignment,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View in Attendance History", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Summary & Quick Action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Students in Session ($totalActiveStudents)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (totalActiveStudents > 0 && !isAttendanceLocked) {
                    TextButton(
                        onClick = {
                            val targetState = !allActivePresent
                            (scheduledStudents + additionalStudents).forEach { student ->
                                attendanceStates[student.id] = targetState
                            }
                        }
                    ) {
                        Text(if (allActivePresent) "Unmark All" else "Mark All Present")
                    }
                }
            }

            if (isLoading || isCheckingExistingAttendance) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Scheduled Students Section
                    if (scheduledStudents.isNotEmpty()) {
                        item {
                            Text(
                                text = "Scheduled for this Slot (${scheduledStudents.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(scheduledStudents, key = { it.id }) { student ->
                            val isPresent = attendanceStates[student.id] == true

                            AttendanceStudentCard(
                                student = student,
                                isPresent = isPresent,
                                isOutOfSchedule = false,
                                isReadOnly = isAttendanceLocked,
                                onToggle = { checked ->
                                    if (!isAttendanceLocked) {
                                        attendanceStates[student.id] = checked
                                    }
                                }
                            )
                        }
                    } else {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No students scheduled for $selectedTimeSlot on $todayName.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    if (!isAttendanceLocked) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Use the '+' button below to add attending students.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Additional Out-of-Schedule Students Section
                    if (additionalStudents.isNotEmpty()) {
                        item {
                            Text(
                                text = "Attending Out of Schedule (${additionalStudents.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(additionalStudents, key = { it.id }) { student ->
                            val isPresent = attendanceStates[student.id] == true
                            val regularSchedule = student.weeklySchedule[todayName]?.let { "Today: $it" }
                                ?: student.weeklySchedule.entries.firstOrNull()?.let { "${it.key}: ${it.value}" }
                                ?: "No regular schedule"

                            AttendanceStudentCard(
                                student = student,
                                isPresent = isPresent,
                                isOutOfSchedule = true,
                                regularScheduleInfo = regularSchedule,
                                isReadOnly = isAttendanceLocked,
                                onToggle = { checked ->
                                    if (!isAttendanceLocked) {
                                        attendanceStates[student.id] = checked
                                    }
                                },
                                onRemove = if (!isAttendanceLocked) {
                                    {
                                        additionalStudents.remove(student)
                                        attendanceStates.remove(student.id)
                                    }
                                } else null
                            )
                        }
                    }

                    // 3. Plus button & Expandable Search Bar (only accessible when attendance is not locked)
                    if (!isAttendanceLocked) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            if (!isSearchExpanded) {
                                OutlinedButton(
                                    onClick = {
                                        isSearchExpanded = true
                                        searchQuery = ""
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Student Out of Scheduled Time")
                                }
                            } else {
                                // Expandable Search Container
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
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
                                                    imageVector = Icons.Default.PersonAdd,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "Add Student Out of Scheduled Time",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    isSearchExpanded = false
                                                    searchQuery = ""
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Collapse Search"
                                                )
                                            }
                                        }

                                        OutlinedTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            placeholder = { Text("Search by name, class, or school...") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Search, contentDescription = null)
                                            },
                                            trailingIcon = {
                                                if (searchQuery.isNotEmpty()) {
                                                    IconButton(onClick = { searchQuery = "" }) {
                                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                                    }
                                                }
                                            },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        if (searchCandidates.isEmpty()) {
                                            Text(
                                                text = if (searchQuery.isBlank()) "All enrolled students are already in this session."
                                                else "No matching students found for \"$searchQuery\".",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "Tap student or check box to add & mark present:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )

                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                searchCandidates.take(5).forEach { candidate ->
                                                    SearchStudentResultItem(
                                                        student = candidate,
                                                        todayName = todayName,
                                                        onAddStudent = { markPresent ->
                                                            if (!additionalStudents.any { it.id == candidate.id }) {
                                                                additionalStudents.add(candidate)
                                                                attendanceStates[candidate.id] = markPresent
                                                                Toast.makeText(
                                                                    context,
                                                                    "Added ${candidate.fullName} out of scheduled time",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                            isSearchExpanded = false
                                                            searchQuery = ""
                                                        }
                                                    )
                                                }

                                                if (searchCandidates.size > 5) {
                                                    Text(
                                                        text = "+ ${searchCandidates.size - 5} more matching students. Type to narrow search.",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.padding(vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Result row inside the expanded search container.
 * Shows student info and an attendance checkbox / add action.
 * Tapping or checking immediately adds the student and collapses the search container.
 */
@Composable
fun SearchStudentResultItem(
    student: StudentModel,
    todayName: String,
    onAddStudent: (Boolean) -> Unit
) {
    var isChecked by remember { mutableStateOf(false) }

    Surface(
        onClick = { onAddStudent(true) },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.fullName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                val scheduleInfo = student.weeklySchedule[todayName]?.let { "Today: $it" }
                    ?: student.weeklySchedule.entries.firstOrNull()?.let { "${it.key}: ${it.value}" }
                    ?: "No schedule set"

                Text(
                    text = "Class: ${student.studentClass} | Regular: $scheduleInfo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Checkbox(
                checked = isChecked,
                onCheckedChange = { checked ->
                    isChecked = checked
                    onAddStudent(checked)
                }
            )
        }
    }
}

@Composable
fun AttendanceStudentCard(
    student: StudentModel,
    isPresent: Boolean,
    isOutOfSchedule: Boolean = false,
    regularScheduleInfo: String? = null,
    isReadOnly: Boolean = false,
    onToggle: (Boolean) -> Unit,
    onRemove: (() -> Unit)? = null
) {
    Card(
        onClick = {
            if (!isReadOnly) {
                onToggle(!isPresent)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPresent) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = student.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (isOutOfSchedule) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "Out of Schedule",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Class: ${student.studentClass} | School: ${student.school}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                if (isOutOfSchedule && regularScheduleInfo != null) {
                    Text(
                        text = "Regular slot: $regularScheduleInfo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isOutOfSchedule && onRemove != null && !isReadOnly) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove student",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Checkbox(
                    checked = isPresent,
                    enabled = !isReadOnly,
                    onCheckedChange = { checked ->
                        if (!isReadOnly) {
                            onToggle(checked)
                        }
                    }
                )
            }
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
