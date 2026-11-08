package com.soumit.technovision.ui.screens.logs

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soumit.technovision.data.model.LogModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SystemLogsScreen(onBack: () -> Unit) {
    val isInPreview = LocalInspectionMode.current

    val todayDateStr = remember {
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    }
    val yesterdayDateStr = remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(cal.time)
    }

    val previewLogs = remember {
        listOf(
            LogModel(
                id = "1",
                action = "Attendance Verified",
                details = "Approved time slot [9:00 AM - 11:00 AM] for teacher Amitabha Teacher on date $todayDateStr (Verified by Soumit Das)",
                performedBy = "owner.preview@technovision.com",
                timestamp = Timestamp.now()
            ),
            LogModel(
                id = "2",
                action = "Attendance Rejected",
                details = "Rejected time slot [1:00 PM - 3:00 PM] for teacher Ramesh Teacher on date $todayDateStr (Rejected by Soumit Das). Slot is now open for re-request.",
                performedBy = "owner.preview@technovision.com",
                timestamp = Timestamp(Date(System.currentTimeMillis() - 1800000))
            ),
            LogModel(
                id = "3",
                action = "Student Enrolled",
                details = "Enrolled student Aarav Sharma (Class X)",
                performedBy = "owner.preview@technovision.com",
                timestamp = Timestamp(Date(System.currentTimeMillis() - 3600000))
            ),
            LogModel(
                id = "4",
                action = "Attendance Saved",
                details = "Saved attendance for 11:00 AM - 1:00 PM batch",
                performedBy = "faculty@technovision.com",
                timestamp = Timestamp(Date(System.currentTimeMillis() - 86400000))
            ),
            LogModel(
                id = "5",
                action = "Staff Added",
                details = "Added receptionist account for Rina",
                performedBy = "owner.preview@technovision.com",
                timestamp = Timestamp(Date(System.currentTimeMillis() - 172800000))
            )
        )
    }

    val previewUserMap = remember {
        mapOf(
            "owner.preview@technovision.com" to "Soumit Das",
            "faculty@technovision.com" to "Amitabha Teacher"
        )
    }

    var logList by remember { mutableStateOf(if (isInPreview) previewLogs else emptyList()) }
    var userMap by remember { mutableStateOf(if (isInPreview) previewUserMap else emptyMap()) }
    var isLoading by remember { mutableStateOf(!isInPreview) }

    // Filter states
    var showAllHistory by remember { mutableStateOf(false) }
    var selectedCustomDate by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val classTimeSlots = listOf(
        "All Slots",
        "9:00 AM - 11:00 AM",
        "11:00 AM - 1:00 PM",
        "1:00 PM - 3:00 PM",
        "3:00 PM - 5:00 PM",
        "5:00 PM - 7:00 PM",
        "7:00 PM - 9:00 PM"
    )
    var selectedTimeSlot by remember { mutableStateOf("All Slots") }

    val categories = listOf("All", "Attendance", "Students", "Fees", "Staff")
    var selectedCategory by remember { mutableStateOf("All") }

    DisposableEffect(Unit) {
        if (isInPreview) {
            onDispose { }
        } else {
            val db = FirebaseFirestore.getInstance()

            db.collection("users").get().addOnSuccessListener { userSnapshot ->
                val map = mutableMapOf<String, String>()
                for (doc in userSnapshot.documents) {
                    val fullName = doc.getString("fullName") ?: continue
                    val email = doc.getString("email") ?: ""
                    val uid = doc.id
                    if (email.isNotBlank()) map[email] = fullName
                    map[uid] = fullName
                }
                userMap = map
            }

            val registration = db.collection("system_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val list = snapshot.documents.map { doc ->
                            LogModel(
                                id = doc.id,
                                action = doc.getString("action") ?: "",
                                details = doc.getString("details") ?: "",
                                performedBy = doc.getString("performedBy") ?: "",
                                timestamp = doc.getTimestamp("timestamp")
                            )
                        }
                        logList = list
                        isLoading = false
                    }
                }
            onDispose {
                registration.remove()
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                            }
                            selectedCustomDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(localCal.time)
                            showAllHistory = false
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Filter computation
    val filteredLogs = remember(logList, showAllHistory, selectedCustomDate, selectedTimeSlot, selectedCategory, todayDateStr) {
        logList.filter { log ->
            val logDate = if (log.timestamp != null) {
                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(log.timestamp.toDate())
            } else {
                ""
            }

            // 1. Date Filter:
            val matchesDate = when {
                selectedCustomDate != null -> logDate == selectedCustomDate
                showAllHistory -> true
                else -> logDate == todayDateStr
            }

            // 2. Class Time Slot Filter:
            val matchesSlot = selectedTimeSlot == "All Slots" ||
                log.details.contains(selectedTimeSlot, ignoreCase = true) ||
                log.action.contains(selectedTimeSlot, ignoreCase = true)

            // 3. Category Filter:
            val matchesCategory = when (selectedCategory) {
                "Attendance" -> log.action.contains("Attendance", ignoreCase = true)
                "Students" -> log.action.contains("Student", ignoreCase = true)
                "Fees" -> log.action.contains("Payment", ignoreCase = true) || log.action.contains("Fee", ignoreCase = true)
                "Staff" -> log.action.contains("Staff", ignoreCase = true)
                else -> true
            }

            matchesDate && matchesSlot && matchesCategory
        }
    }

    // Count of older logs hidden by the default "Today" view
    val olderLogsCount = remember(logList, todayDateStr) {
        logList.count { log ->
            val logDate = if (log.timestamp != null) {
                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(log.timestamp.toDate())
            } else ""
            logDate.isNotBlank() && logDate != todayDateStr
        }
    }

    val todayLogsCount = remember(logList, todayDateStr) {
        logList.count { log ->
            val logDate = if (log.timestamp != null) {
                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(log.timestamp.toDate())
            } else ""
            logDate == todayDateStr
        }
    }

    // Grouping by date when showing all history
    val groupedLogs = remember(filteredLogs, showAllHistory, selectedCustomDate) {
        if (showAllHistory && selectedCustomDate == null) {
            filteredLogs.groupBy { log ->
                if (log.timestamp != null) {
                    SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(log.timestamp.toDate())
                } else {
                    "Other"
                }
            }
        } else {
            emptyMap()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity History") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Date Scope Chips & Date Picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Today Chip
                FilterChip(
                    selected = !showAllHistory && selectedCustomDate == null,
                    onClick = {
                        showAllHistory = false
                        selectedCustomDate = null
                    },
                    label = { Text("Today ($todayLogsCount)") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Today,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                // Show All History Chip
                FilterChip(
                    selected = showAllHistory && selectedCustomDate == null,
                    onClick = {
                        showAllHistory = true
                        selectedCustomDate = null
                    },
                    label = { Text("All History (${logList.size})") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                // Custom Date Chip / Picker
                if (selectedCustomDate != null) {
                    InputChip(
                        selected = true,
                        onClick = { showDatePicker = true },
                        label = { Text(selectedCustomDate!!) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { selectedCustomDate = null },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear Custom Date",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    )
                } else {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Date", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Class Time Slot Filter Chips
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Filter by Class Slot",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    classTimeSlots.forEach { slot ->
                        FilterChip(
                            selected = selectedTimeSlot == slot,
                            onClick = { selectedTimeSlot = slot },
                            label = { Text(slot, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }

            // Filter Active Status Banner
            val isFilterActive = selectedTimeSlot != "All Slots" || selectedCategory != "All" || selectedCustomDate != null
            if (isFilterActive) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Showing ${filteredLogs.size} filtered log${if (filteredLogs.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        TextButton(
                            onClick = {
                                selectedTimeSlot = "All Slots"
                                selectedCategory = "All"
                                selectedCustomDate = null
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Main Content Area
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = when {
                                selectedCustomDate != null -> "No logs recorded for $selectedCustomDate."
                                !showAllHistory -> "No activity logs recorded for today."
                                else -> "No logs match the selected filters."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (!showAllHistory && olderLogsCount > 0 && selectedCustomDate == null) {
                            Button(
                                onClick = { showAllHistory = true }
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Show all History ($olderLogsCount older events)")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (showAllHistory && selectedCustomDate == null) {
                        groupedLogs.forEach { (dateStr, logsForDate) ->
                            item(key = "header_$dateStr") {
                                val friendlyHeader = when (dateStr) {
                                    todayDateStr -> "Today ($dateStr)"
                                    yesterdayDateStr -> "Yesterday ($dateStr)"
                                    else -> dateStr
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = friendlyHeader,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${logsForDate.size} event${if (logsForDate.size > 1) "s" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                            items(logsForDate, key = { it.id }) { log ->
                                LogCard(log = log, userMap = userMap)
                            }
                        }
                    } else {
                        items(filteredLogs, key = { it.id }) { log ->
                            LogCard(log = log, userMap = userMap)
                        }

                        // Prominent "Show all History" button at the bottom of Today's view
                        if (!showAllHistory && selectedCustomDate == null && olderLogsCount > 0) {
                            item(key = "show_all_history_bottom_btn") {
                                OutlinedCard(
                                    onClick = { showAllHistory = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Show all History ($olderLogsCount older events)",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
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
}

@Composable
fun LogCard(log: LogModel, userMap: Map<String, String>) {
    val formattedTime = remember(log.timestamp) {
        if (log.timestamp != null) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(log.timestamp.toDate())
        } else {
            "Just now"
        }
    }

    val displayName = userMap[log.performedBy] ?: log.performedBy

    val timeSlotRegex = remember {
        Regex("""(\d{1,2}:\d{2}\s*(?:AM|PM)\s*-\s*\d{1,2}:\d{2}\s*(?:AM|PM))""", RegexOption.IGNORE_CASE)
    }
    val matchedSlot = remember(log.details) {
        timeSlotRegex.find(log.details)?.value
    }

    val actionIcon = when {
        log.action.contains("Verified", ignoreCase = true) -> Icons.Default.CheckCircle
        log.action.contains("Reject", ignoreCase = true) -> Icons.Default.Cancel
        log.action.contains("Attendance", ignoreCase = true) -> Icons.Default.Schedule
        log.action.contains("Student", ignoreCase = true) -> Icons.Default.Person
        log.action.contains("Payment", ignoreCase = true) || log.action.contains("Fee", ignoreCase = true) -> Icons.Default.Payments
        log.action.contains("Staff", ignoreCase = true) -> Icons.Default.Badge
        else -> Icons.Default.Info
    }

    val actionColor = when {
        log.action.contains("Reject", ignoreCase = true) || log.action.contains("Delete", ignoreCase = true) ->
            MaterialTheme.colorScheme.error
        log.action.contains("Verified", ignoreCase = true) ->
            MaterialTheme.colorScheme.primary
        log.action.contains("Payment", ignoreCase = true) ->
            MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = actionColor.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = actionColor
                        )
                        Text(
                            text = log.action,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = actionColor
                        )
                    }
                }

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Detected Class Time Slot Pill
            if (matchedSlot != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = matchedSlot,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Text(
                text = log.details,
                style = MaterialTheme.typography.bodyMedium
            )

            if (log.performedBy.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "By: $displayName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun SystemLogsScreenPreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        SystemLogsScreen(onBack = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SystemLogsScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        SystemLogsScreen(onBack = {})
    }
}
