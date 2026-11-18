package com.soumit.technovision.ui.screens.attendance

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TeacherAttendanceScreen(
    onBack: () -> Unit,
    onNavigateToViewer: (() -> Unit)? = null,
    viewModel: TeacherAttendanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    TeacherAttendanceContent(
        uiState = uiState,
        onBack = onBack,
        onNavigateToViewer = onNavigateToViewer,
        onDateSelected = viewModel::onDateSelected,
        onTimeSlotSelected = viewModel::onTimeSlotSelected,
        onSubmitRequest = { viewModel.submitRequest {} }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TeacherAttendanceContent(
    uiState: TeacherAttendanceUiState,
    onBack: () -> Unit,
    onNavigateToViewer: (() -> Unit)?,
    onDateSelected: (String) -> Unit,
    onTimeSlotSelected: (String) -> Unit,
    onSubmitRequest: () -> Unit
) {
    val timeSlots = listOf(
        "9:00 AM - 11:00 AM",
        "11:00 AM - 1:00 PM",
        "1:00 PM - 3:00 PM",
        "3:00 PM - 5:00 PM",
        "5:00 PM - 7:00 PM",
        "7:00 PM - 9:00 PM"
    )

    var timeSlotExpanded by remember { mutableStateOf(false) }

    val todayDateStr = remember {
        SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
    }

    val calendarInstance = remember { Calendar.getInstance() }
    var displayedYear by remember { mutableIntStateOf(calendarInstance.get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableIntStateOf(calendarInstance.get(Calendar.MONTH)) }

    val requestsForSelectedDate = remember(uiState.requests, uiState.selectedDateStr) {
        uiState.requests.filter { it.date == uiState.selectedDateStr }
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
                actions = {
                    if (onNavigateToViewer != null) {
                        IconButton(onClick = onNavigateToViewer) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = "View Attendance Records"
                            )
                        }
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
                                Text(
                                    text = "Selected Request Date",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = uiState.selectedDateStr,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = if (uiState.selectedDateStr == todayDateStr) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = if (uiState.selectedDateStr == todayDateStr) "Same-Day Active" else "Viewing Past Log",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = if (uiState.selectedDateStr == todayDateStr) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
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
                                value = uiState.selectedTimeSlot,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Batch Time Slot") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeSlotExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            )
                            ExposedDropdownMenu(
                                expanded = timeSlotExpanded,
                                onDismissRequest = { timeSlotExpanded = false }
                            ) {
                                timeSlots.forEach { slot ->
                                    DropdownMenuItem(
                                        text = { Text(slot) },
                                        onClick = {
                                            onTimeSlotSelected(slot)
                                            timeSlotExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (uiState.isSubmitting) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                LoadingIndicator()
                            }
                        } else {
                            Button(
                                onClick = onSubmitRequest,
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
                    selectedDateStr = uiState.selectedDateStr,
                    todayDateStr = todayDateStr,
                    allRequests = uiState.requests,
                    onPrevMonth = {
                        val newMonth = if (displayedMonth == 0) 11 else displayedMonth - 1
                        val newYear = if (displayedMonth == 0) displayedYear - 1 else displayedYear
                        displayedMonth = newMonth
                        displayedYear = newYear
                        val cal = Calendar.getInstance().apply { set(newYear, newMonth, 1) }
                        onDateSelected(SimpleDateFormat("dd-MM-yyyy", Locale.US).format(cal.time))
                    },
                    onNextMonth = {
                        val newMonth = if (displayedMonth == 11) 0 else displayedMonth + 1
                        val newYear = if (displayedMonth == 11) displayedYear + 1 else displayedYear
                        displayedMonth = newMonth
                        displayedYear = newYear
                        val cal = Calendar.getInstance().apply { set(newYear, newMonth, 1) }
                        onDateSelected(SimpleDateFormat("dd-MM-yyyy", Locale.US).format(cal.time))
                    },
                    onDateSelected = onDateSelected
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Requests for ${uiState.selectedDateStr} (${requestsForSelectedDate.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                }
            } else if (requestsForSelectedDate.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
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
            val rowCount = (totalCells + 6) / 7
            val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy", Locale.US) }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0 until rowCount) {
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
                                            val hasPending = dayRequests.any { it.status == "Pending" }
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            hasVerified -> MaterialTheme.colorScheme.primary
                                                            hasPending -> MaterialTheme.colorScheme.tertiary
                                                            else -> MaterialTheme.colorScheme.error
                                                        }
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
    val isRejected = request.status == "Rejected"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isVerified -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                isRejected -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                if (isVerified && request.verifiedBy.isNotBlank()) {
                    Text(
                        text = "Verified by ${request.verifiedBy}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (isRejected) {
                    val rejector = request.rejectedBy.ifBlank { "Admin" }
                    Text(
                        text = "Rejected by $rejector • Slot is open to re-request",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Surface(
                color = when {
                    isVerified -> MaterialTheme.colorScheme.primaryContainer
                    isRejected -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            isVerified -> Icons.Default.CheckCircle
                            isRejected -> Icons.Default.Cancel
                            else -> Icons.Default.HourglassTop
                        },
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = when {
                            isVerified -> MaterialTheme.colorScheme.primary
                            isRejected -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.secondary
                        }
                    )
                    Text(
                        text = request.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isVerified -> MaterialTheme.colorScheme.onPrimaryContainer
                            isRejected -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
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
        TeacherAttendanceContent(
            uiState = TeacherAttendanceUiState(isLoading = false),
            onBack = {},
            onNavigateToViewer = null,
            onDateSelected = {},
            onTimeSlotSelected = {},
            onSubmitRequest = {}
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TeacherAttendanceScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        TeacherAttendanceContent(
            uiState = TeacherAttendanceUiState(isLoading = false),
            onBack = {},
            onNavigateToViewer = null,
            onDateSelected = {},
            onTimeSlotSelected = {},
            onSubmitRequest = {}
        )
    }
}
