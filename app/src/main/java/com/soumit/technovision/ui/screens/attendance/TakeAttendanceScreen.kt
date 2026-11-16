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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.ui.common.AppViewModelProvider
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import com.soumit.technovision.util.AttendanceUtils

@Composable
fun TakeAttendanceScreen(
    onBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: TakeAttendanceViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    TakeAttendanceContent(
        uiState = uiState,
        onBack = onBack,
        onNavigateToHistory = onNavigateToHistory,
        onSelectTimeSlot = viewModel::onSelectTimeSlot,
        onToggleAttendance = viewModel::toggleStudentAttendance,
        onSetAllAttendance = viewModel::setAllAttendance,
        onAddAdditionalStudent = viewModel::addAdditionalStudent,
        onRemoveAdditionalStudent = viewModel::removeAdditionalStudent,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onSetSearchExpanded = viewModel::setSearchExpanded,
        onSaveAttendance = { viewModel.saveAttendance { onBack() } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TakeAttendanceContent(
    uiState: TakeAttendanceUiState,
    onBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onSelectTimeSlot: (String) -> Unit,
    onToggleAttendance: (String) -> Unit,
    onSetAllAttendance: (Boolean) -> Unit,
    onAddAdditionalStudent: (StudentModel) -> Unit,
    onRemoveAdditionalStudent: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSetSearchExpanded: (Boolean) -> Unit,
    onSaveAttendance: () -> Unit
) {
    val context = LocalContext.current
    var timeSlotExpanded by remember { mutableStateOf(false) }

    val autoTimeSlot = remember { AttendanceUtils.getAutomaticTimeSlot() }
    val totalActiveStudents = uiState.displayedStudents.size
    val allActivePresent = totalActiveStudents > 0 && uiState.displayedStudents.all { uiState.attendanceStates[it.id] == true }

    val searchCandidates = remember(uiState.allStudents, uiState.displayedStudents, uiState.searchQuery) {
        val activeIds = uiState.displayedStudents.map { it.id }.toSet()
        val nonActive = uiState.allStudents.filter { it.id !in activeIds }
        if (uiState.searchQuery.isBlank()) {
            nonActive
        } else {
            nonActive.filter {
                it.fullName.contains(uiState.searchQuery, ignoreCase = true) ||
                        it.studentClass.contains(uiState.searchQuery, ignoreCase = true) ||
                        it.school.contains(uiState.searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Take Attendance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
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
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Present: ${uiState.presentCount}  |  Absent: ${uiState.absentCount}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = uiState.currentDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    if (uiState.isAttendanceLocked) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Locked: Attendance already recorded for this slot.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else if (uiState.isSaving) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    } else {
                        Button(
                            onClick = onSaveAttendance,
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
                    value = uiState.selectedTimeSlot,
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
                    AttendanceUtils.BATCH_TIME_SLOTS.forEach { slot ->
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
                                onSelectTimeSlot(slot)
                                timeSlotExpanded = false
                            }
                        )
                    }
                }
            }

            // Duplicate Attendance Warning Banner
            uiState.existingAttendanceInfo?.let { existing ->
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
                                text = "Attendance for this batch (${uiState.selectedTimeSlot}) was already submitted today at ${existing.timeFormatted} by ${existing.submittedBy} (${existing.totalPresent} Present, ${existing.totalAbsent} Absent).",
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

            // Summary Header
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

                if (totalActiveStudents > 0 && !uiState.isAttendanceLocked) {
                    TextButton(
                        onClick = { onSetAllAttendance(!allActivePresent) }
                    ) {
                        Text(if (allActivePresent) "Unmark All" else "Mark All Present")
                    }
                }
            }

            if (uiState.isLoadingStudents || uiState.isCheckingExistingAttendance) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Scheduled Students Section
                    if (uiState.scheduledStudents.isNotEmpty()) {
                        item {
                            Text(
                                text = "Scheduled for this Slot (${uiState.scheduledStudents.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(uiState.scheduledStudents, key = { it.id }) { student ->
                            val isPresent = uiState.attendanceStates[student.id] == true

                            AttendanceStudentCard(
                                student = student,
                                isPresent = isPresent,
                                isOutOfSchedule = false,
                                isReadOnly = uiState.isAttendanceLocked,
                                onToggle = { onToggleAttendance(student.id) }
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
                                        text = "No students scheduled for ${uiState.selectedTimeSlot} on ${uiState.todayName}.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    if (!uiState.isAttendanceLocked) {
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

                    // Out of schedule students
                    if (uiState.additionalStudents.isNotEmpty()) {
                        item {
                            Text(
                                text = "Attending Out of Schedule (${uiState.additionalStudents.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(uiState.additionalStudents, key = { it.id }) { student ->
                            val isPresent = uiState.attendanceStates[student.id] == true
                            val regularSchedule = student.weeklySchedule[uiState.todayName]?.let { "Today: $it" }
                                ?: student.weeklySchedule.entries.firstOrNull()?.let { "${it.key}: ${it.value}" }
                                ?: "No regular schedule"

                            AttendanceStudentCard(
                                student = student,
                                isPresent = isPresent,
                                isOutOfSchedule = true,
                                regularScheduleInfo = regularSchedule,
                                isReadOnly = uiState.isAttendanceLocked,
                                onToggle = { onToggleAttendance(student.id) },
                                onRemove = if (!uiState.isAttendanceLocked) {
                                    { onRemoveAdditionalStudent(student.id) }
                                } else null
                            )
                        }
                    }

                    // Add Student Out of Schedule Button / Search Form
                    if (!uiState.isAttendanceLocked) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            if (!uiState.isSearchExpanded) {
                                OutlinedButton(
                                    onClick = { onSetSearchExpanded(true) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Student Out of Scheduled Time")
                                }
                            } else {
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
                                                onClick = { onSetSearchExpanded(false) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Collapse Search"
                                                )
                                            }
                                        }

                                        OutlinedTextField(
                                            value = uiState.searchQuery,
                                            onValueChange = onSearchQueryChange,
                                            placeholder = { Text("Search by name, class, or school...") },
                                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                            trailingIcon = {
                                                if (uiState.searchQuery.isNotEmpty()) {
                                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                                    }
                                                }
                                            },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        if (searchCandidates.isEmpty()) {
                                            Text(
                                                text = if (uiState.searchQuery.isBlank()) "All enrolled students are already in this session."
                                                else "No matching students found for \"${uiState.searchQuery}\".",
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
                                                        todayName = uiState.todayName,
                                                        onAddStudent = {
                                                            onAddAdditionalStudent(candidate)
                                                            Toast.makeText(context, "Added ${candidate.fullName} out of scheduled time", Toast.LENGTH_SHORT).show()
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
        TakeAttendanceContent(
            uiState = TakeAttendanceUiState(isLoadingStudents = false),
            onBack = {},
            onNavigateToHistory = {},
            onSelectTimeSlot = {},
            onToggleAttendance = {},
            onSetAllAttendance = {},
            onAddAdditionalStudent = {},
            onRemoveAdditionalStudent = {},
            onSearchQueryChange = {},
            onSetSearchExpanded = {},
            onSaveAttendance = {}
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TakeAttendanceScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        TakeAttendanceContent(
            uiState = TakeAttendanceUiState(isLoadingStudents = false),
            onBack = {},
            onNavigateToHistory = {},
            onSelectTimeSlot = {},
            onToggleAttendance = {},
            onSetAllAttendance = {},
            onAddAdditionalStudent = {},
            onRemoveAdditionalStudent = {},
            onSearchQueryChange = {},
            onSetSearchExpanded = {},
            onSaveAttendance = {}
        )
    }
}
