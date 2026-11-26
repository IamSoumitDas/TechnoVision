@file:Suppress("UNCHECKED_CAST")

package com.soumit.technovision.ui.screens.attendance

import android.content.res.Configuration
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import com.soumit.technovision.util.AttendanceUtils
import java.time.LocalDate

private fun parseDateForSorting(dateStr: String): LocalDate = AttendanceUtils.parseDateForSorting(dateStr)
private fun formatDateHeading(dateStr: String): String = AttendanceUtils.formatDateHeading(dateStr)
internal fun resolveTeacherName(
    record: AttendanceRecordModel,
    userMap: Map<String, String>,
    teacherAttendanceMap: Map<String, String> = emptyMap(),
    fallbackTeacherMap: Map<String, String> = emptyMap()
): String = AttendanceUtils.resolveTeacherName(record, userMap, teacherAttendanceMap, fallbackTeacherMap)

@Composable
fun AttendanceHistoryScreen(
    onBack: () -> Unit,
    viewModel: AttendanceHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AttendanceHistoryContent(
        uiState = uiState,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onBack = onBack,
        onOpenDetail = viewModel::openDetail,
        onCloseDetail = viewModel::closeDetail
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AttendanceHistoryContent(
    uiState: AttendanceHistoryUiState,
    onSearchQueryChange: (String) -> Unit = {},
    onBack: () -> Unit,
    onOpenDetail: (AttendanceRecordModel) -> Unit,
    onCloseDetail: () -> Unit
) {
    val groupedRecordsByDate = remember(uiState.filteredGroupedRecords, uiState.attendanceRecords, uiState.searchQuery) {
        if (uiState.filteredGroupedRecords.isNotEmpty() || uiState.searchQuery.isNotBlank()) {
            uiState.filteredGroupedRecords
        } else {
            uiState.attendanceRecords.groupBy { it.date }
                .toList()
                .sortedByDescending { parseDateForSorting(it.first) }
        }
    }
    val totalFilteredCount = remember(groupedRecordsByDate) {
        groupedRecordsByDate.sumOf { it.second.size }
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
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search by date, slot, teacher, or student...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

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

                if (totalFilteredCount > 0) {
                    Text(
                        text = "$totalFilteredCount Sessions Logged",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (uiState.isLoading && uiState.attendanceRecords.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else if (totalFilteredCount == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (uiState.searchQuery.isNotBlank()) "No matching session logs found." else "No attendance history logged yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(groupedRecordsByDate, key = { (date, _) -> date }, contentType = { "attendance_group" }) { (dateStr, dateRecords) ->
                        DateAttendanceGroupCard(
                            dateStr = dateStr,
                            records = dateRecords,
                            userMap = uiState.userMap,
                            teacherAttendanceMap = uiState.teacherAttendanceMap,
                            fallbackTeacherMap = uiState.fallbackTeacherMap,
                            onRecordClick = onOpenDetail
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Detail Dialog
    if (uiState.selectedRecord != null) {
        val record = uiState.selectedRecord
        val formattedDate = remember(record.date) { formatDateHeading(record.date) }
        val detailTeacherName = remember(record, uiState.userMap, uiState.teacherAttendanceMap, uiState.fallbackTeacherMap) {
            resolveTeacherName(record, uiState.userMap, uiState.teacherAttendanceMap, uiState.fallbackTeacherMap)
        }

        val presentNames = remember(record, uiState.studentsMap) {
            record.presentStudentIds.map { id ->
                uiState.studentsMap[id]?.fullName ?: id
            }
        }
        val absentNames = remember(record.absentStudentIds, uiState.studentsMap) {
            record.absentStudentIds.map { id ->
                uiState.studentsMap[id]?.fullName ?: id
            }
        }

        AlertDialog(
            onDismissRequest = onCloseDetail,
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Summary Pill
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
                            text = "Present Students (${presentNames.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (presentNames.isEmpty()) {
                            Text(
                                text = "No students recorded as present.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            presentNames.forEach { name ->
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

                    // Absent Students
                    if (absentNames.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Scheduled but Absent (${absentNames.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )

                            absentNames.forEach { name ->
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
            },
            confirmButton = {
                TextButton(onClick = onCloseDetail) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun DateAttendanceGroupCard(
    dateStr: String,
    records: List<AttendanceRecordModel>,
    userMap: Map<String, String>,
    teacherAttendanceMap: Map<String, String> = emptyMap(),
    fallbackTeacherMap: Map<String, String> = emptyMap(),
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                records.forEach { record ->
                    key(record.id.ifBlank { "${record.date}_${record.timeSlot}" }) {
                        BatchAttendanceItemCard(
                            record = record,
                            userMap = userMap,
                            teacherAttendanceMap = teacherAttendanceMap,
                            fallbackTeacherMap = fallbackTeacherMap,
                            onClick = { onRecordClick(record) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BatchAttendanceItemCard(
    record: AttendanceRecordModel,
    userMap: Map<String, String>,
    teacherAttendanceMap: Map<String, String> = emptyMap(),
    fallbackTeacherMap: Map<String, String> = emptyMap(),
    onClick: () -> Unit
) {
    val teacherName = remember(record, userMap, teacherAttendanceMap, fallbackTeacherMap) {
        resolveTeacherName(record, userMap, teacherAttendanceMap, fallbackTeacherMap)
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
        AttendanceHistoryContent(
            uiState = AttendanceHistoryUiState(
                attendanceRecords = com.soumit.technovision.ui.common.PreviewData.attendanceRecords,
                studentsMap = com.soumit.technovision.ui.common.PreviewData.studentsMap,
                userMap = com.soumit.technovision.ui.common.PreviewData.userMap,
                isLoading = false
            ),
            onBack = {},
            onOpenDetail = {},
            onCloseDetail = {}
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AttendanceHistoryScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        AttendanceHistoryContent(
            uiState = AttendanceHistoryUiState(
                attendanceRecords = com.soumit.technovision.ui.common.PreviewData.attendanceRecords,
                studentsMap = com.soumit.technovision.ui.common.PreviewData.studentsMap,
                userMap = com.soumit.technovision.ui.common.PreviewData.userMap,
                isLoading = false
            ),
            onBack = {},
            onOpenDetail = {},
            onCloseDetail = {}
        )
    }
}
