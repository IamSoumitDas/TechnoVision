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
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.Timestamp
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import com.soumit.technovision.util.AttendanceUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FacultyAttendanceViewerScreen(
    onBack: () -> Unit,
    onNavigateToVerify: (() -> Unit)? = null,
    viewModel: FacultyAttendanceViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    FacultyAttendanceViewerContent(
        uiState = uiState,
        onBack = onBack,
        onNavigateToVerify = onNavigateToVerify,
        onMonthChanged = viewModel::onMonthChanged
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FacultyAttendanceViewerContent(
    uiState: FacultyAttendanceViewerUiState,
    onBack: () -> Unit,
    onNavigateToVerify: (() -> Unit)? = null,
    onMonthChanged: (Int, Int) -> Unit
) {
    val todayCal = remember { Calendar.getInstance() }
    val todayYear = remember { todayCal.get(Calendar.YEAR) }
    val todayMonth = remember { todayCal.get(Calendar.MONTH) }

    var selectedDayInMonth by remember { mutableStateOf<Int?>(null) }
    var selectedTeacherFilter by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf("All") }

    val monthRecords = remember(uiState.requests, uiState.selectedYear, uiState.selectedMonth) {
        uiState.requests.filter { record ->
            val parsed = AttendanceUtils.parseRecordDate(record.date)
            if (parsed != null) {
                parsed.first == uiState.selectedYear && parsed.second == uiState.selectedMonth
            } else if (record.timestamp != null) {
                val c = Calendar.getInstance().apply { time = record.timestamp.toDate() }
                c.get(Calendar.YEAR) == uiState.selectedYear && c.get(Calendar.MONTH) == uiState.selectedMonth
            } else {
                false
            }
        }
    }

    val totalMonthClasses = remember(monthRecords) {
        monthRecords.count { it.status.equals("Verified", ignoreCase = true) }
    }

    val filteredLogRecords = remember(
        monthRecords,
        selectedDayInMonth,
        selectedTeacherFilter,
        selectedStatusFilter,
        uiState.selectedYear,
        uiState.selectedMonth
    ) {
        monthRecords.filter { record ->
            val matchesDay = selectedDayInMonth?.let { day ->
                val parsed = AttendanceUtils.parseRecordDate(record.date)
                if (parsed != null) {
                    parsed.first == uiState.selectedYear &&
                            parsed.second == uiState.selectedMonth &&
                            parsed.third == day
                } else if (record.timestamp != null) {
                    val c = Calendar.getInstance().apply { time = record.timestamp.toDate() }
                    c.get(Calendar.YEAR) == uiState.selectedYear &&
                            c.get(Calendar.MONTH) == uiState.selectedMonth &&
                            c.get(Calendar.DAY_OF_MONTH) == day
                } else false
            } ?: true

            val resolvedTeacher = uiState.userMap[record.teacherId]
                ?: uiState.userMap[record.teacherName]
                ?: record.teacherName.ifBlank { "Faculty Member" }

            val matchesTeacher = selectedTeacherFilter == null ||
                    resolvedTeacher.equals(selectedTeacherFilter, ignoreCase = true) ||
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

    val selectedMonthDisplayName = remember(uiState.selectedYear, uiState.selectedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, uiState.selectedYear)
            set(Calendar.MONTH, uiState.selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    val isCurrentRealMonth = (uiState.selectedYear == todayYear && uiState.selectedMonth == todayMonth)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Faculty Attendance Records",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
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
            item {
                MonthNavigatorCard(
                    monthTitle = selectedMonthDisplayName,
                    isCurrentMonth = isCurrentRealMonth,
                    onPreviousMonth = {
                        selectedDayInMonth = null
                        if (uiState.selectedMonth == 0) {
                            onMonthChanged(uiState.selectedYear - 1, 11)
                        } else {
                            onMonthChanged(uiState.selectedYear, uiState.selectedMonth - 1)
                        }
                    },
                    onNextMonth = {
                        selectedDayInMonth = null
                        if (uiState.selectedMonth == 11) {
                            onMonthChanged(uiState.selectedYear + 1, 0)
                        } else {
                            onMonthChanged(uiState.selectedYear, uiState.selectedMonth + 1)
                        }
                    },
                    onResetToCurrentMonth = {
                        selectedDayInMonth = null
                        onMonthChanged(todayYear, todayMonth)
                    }
                )
            }

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
                                    text = "Classes taken by each faculty member in $selectedMonthDisplayName",
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

                        if (uiState.teacherSummaries.isEmpty()) {
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
                                for (summary in uiState.teacherSummaries) {
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
                                                    color = MaterialTheme.colorScheme.primary
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val statuses = listOf("All", "Verified", "Pending", "Rejected")
                        statuses.forEach { status ->
                            FilterChip(
                                selected = selectedStatusFilter == status,
                                onClick = { selectedStatusFilter = status },
                                label = { Text(status) }
                            )
                        }

                        if (selectedDayInMonth != null) {
                            SuggestionChip(
                                onClick = { selectedDayInMonth = null },
                                label = {
                                    Text("Day: $selectedDayInMonth $selectedMonthDisplayName ✕")
                                }
                            )
                        }

                        if (selectedTeacherFilter != null) {
                            SuggestionChip(
                                onClick = { selectedTeacherFilter = null },
                                label = {
                                    Text("Faculty: $selectedTeacherFilter ✕")
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator(modifier = Modifier.size(36.dp))
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
                items(filteredLogRecords, key = { it.id }, contentType = { "attendance_record" }) { record ->
                    val resolvedName = uiState.userMap[record.teacherId]
                        ?: uiState.userMap[record.teacherName]
                        ?: record.teacherName.ifBlank { "Faculty Member" }
                    AttendanceLogCard(
                        record = record,
                        resolvedTeacherName = resolvedName,
                        onDaySelected = { selectedDayInMonth = it },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

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

@Composable
fun AttendanceLogCard(
    record: TeacherAttendanceRequestModel,
    resolvedTeacherName: String = record.teacherName.ifBlank { "Faculty Member" },
    onDaySelected: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isVerified = record.status.equals("Verified", ignoreCase = true)
    val isRejected = record.status.equals("Rejected", ignoreCase = true)

    val durationHours = remember(record.timeSlot) { AttendanceUtils.parseSlotDurationHours(record.timeSlot) }
    val durationText = if (durationHours == durationHours.toInt().toDouble()) "${durationHours.toInt()}h class" else "${String.format(Locale.US, "%.1f", durationHours)}h class"

    val statusColor = when {
        isVerified -> Color(0xFF2E7D32)
        isRejected -> MaterialTheme.colorScheme.error
        else -> Color(0xFFF57C00)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = resolvedTeacherName,
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

                val dayNumber = remember(record.date, record.timestamp) {
                    AttendanceUtils.parseRecordDate(record.date)?.third
                        ?: record.timestamp?.let {
                            Calendar.getInstance().apply { time = it.toDate() }.get(Calendar.DAY_OF_MONTH)
                        }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = if (dayNumber != null && onDaySelected != null) {
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onDaySelected(dayNumber) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    } else Modifier
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = if (dayNumber != null && onDaySelected != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = record.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (dayNumber != null && onDaySelected != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        fontWeight = if (dayNumber != null && onDaySelected != null) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun FacultyAttendanceViewerScreenPreview() {
    TechnoVisionTheme {
        FacultyAttendanceViewerContent(
            uiState = FacultyAttendanceViewerUiState(
                requests = com.soumit.technovision.ui.common.PreviewData.teacherAttendanceRequests,
                userMap = com.soumit.technovision.ui.common.PreviewData.userMap,
                isLoading = false
            ),
            onBack = {},
            onMonthChanged = { _, _ -> }
        )
    }
}
