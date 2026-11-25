package com.soumit.technovision.ui.screens.students

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun BirthdayCalendarScreen(
    onBack: () -> Unit,
    onStudentClick: (String) -> Unit,
    viewModel: BirthdayCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BirthdayCalendarContent(
        uiState = uiState,
        onBack = onBack,
        onStudentClick = onStudentClick,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onJumpToToday = viewModel::onJumpToToday,
        onDateSelected = viewModel::onDateSelected,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onClassFilterSelected = viewModel::onClassFilterSelected,
        onTabSelected = viewModel::onTabSelected,
        onCallStudent = { phone -> dialStudent(context, phone) },
        onWhatsAppWish = { phone, name -> sendWhatsAppWish(context, phone, name) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BirthdayCalendarContent(
    uiState: BirthdayCalendarUiState,
    onBack: () -> Unit,
    onStudentClick: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToToday: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClassFilterSelected: (String) -> Unit,
    onTabSelected: (Int) -> Unit,
    onCallStudent: (String) -> Unit,
    onWhatsAppWish: (String, String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Birthday Calendar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onJumpToToday) {
                        Icon(Icons.Default.Today, contentDescription = "Jump to Today")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Search Bar & Class Filter Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    placeholder = { Text("Search by name, class, school, phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true
                )

                if (uiState.availableClasses.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.availableClasses.forEach { className ->
                            val isSelected = uiState.selectedClassFilter == className
                            FilterChip(
                                selected = isSelected,
                                onClick = { onClassFilterSelected(className) },
                                label = { Text(if (className == "All") "All Classes" else "Class $className") },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            // 2. Today's Celebrations Highlight Banner
            TodaysCelebrationsBanner(
                todaysBirthdays = uiState.todaysBirthdays,
                nextCelebration = uiState.nextUpcomingCelebration,
                onStudentClick = onStudentClick,
                onCallStudent = onCallStudent,
                onWhatsAppWish = onWhatsAppWish
            )

            // 3. Primary Tab Row (Month View vs Upcoming Timeline)
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Month View")
                        }
                    }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Upcoming Timeline")
                        }
                    }
                )
            }

            // 4. Tab Content
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else if (uiState.errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Error: ${uiState.errorMessage}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                when (uiState.selectedTab) {
                    0 -> MonthCalendarTabView(
                        uiState = uiState,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onJumpToToday = onJumpToToday,
                        onDateSelected = onDateSelected,
                        onStudentClick = onStudentClick,
                        onCallStudent = onCallStudent,
                        onWhatsAppWish = onWhatsAppWish
                    )
                    1 -> UpcomingTimelineTabView(
                        items = uiState.timelineUpcomingItems,
                        onStudentClick = onStudentClick,
                        onCallStudent = onCallStudent,
                        onWhatsAppWish = onWhatsAppWish
                    )
                }
            }
        }
    }
}

@Composable
fun TodaysCelebrationsBanner(
    todaysBirthdays: List<StudentBirthdayItem>,
    nextCelebration: StudentBirthdayItem?,
    onStudentClick: (String) -> Unit,
    onCallStudent: (String) -> Unit,
    onWhatsAppWish: (String, String) -> Unit
) {
    if (todaysBirthdays.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Celebration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = if (todaysBirthdays.size == 1) "Today's Birthday" else "${todaysBirthdays.size} Birthdays Today",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Send your wishes to celebrating students",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                todaysBirthdays.forEach { item ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStudentClick(item.student.id) },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.student.fullName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Class ${item.student.studentClass} • ${item.turningAge?.let { "Turning $it" } ?: "Birthday"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val phone = item.student.phone.ifBlank { item.student.altPhone }
                                FilledTonalIconButton(
                                    onClick = { onWhatsAppWish(phone, item.student.fullName) },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = "WhatsApp",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                FilledTonalIconButton(
                                    onClick = { onCallStudent(phone) },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else if (nextCelebration != null) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cake,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "No birthdays today • Next is ${nextCelebration.student.fullName} (${nextCelebration.daysUntilNextBirthday} days away)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MonthCalendarTabView(
    uiState: BirthdayCalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToToday: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onStudentClick: (String) -> Unit,
    onCallStudent: (String) -> Unit,
    onWhatsAppWish: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Grid Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Month Navigation Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPreviousMonth) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val monthTitle = uiState.currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
                            Text(
                                text = monthTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            TextButton(
                                onClick = onJumpToToday,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Today", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        IconButton(onClick = onNextMonth) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Days of week header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { dayLabel ->
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                modifier = Modifier.width(38.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Calendar Day Grid
                    val currentYm = uiState.currentYearMonth
                    val daysInMonth = currentYm.lengthOfMonth()
                    val firstDayOfWeek = currentYm.atDay(1).dayOfWeek
                    val emptyDaysAtStart = firstDayOfWeek.value % 7
                    val totalCells = emptyDaysAtStart + daysInMonth
                    val rows = (totalCells + 6) / 7

                    val today = LocalDate.now()

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (r in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (c in 0 until 7) {
                                    val cellIndex = r * 7 + c
                                    val dayNum = cellIndex - emptyDaysAtStart + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val date = currentYm.atDay(dayNum)
                                        val isToday = date == today
                                        val isSelected = date == uiState.selectedDate
                                        val birthdaysOnDay = uiState.birthdaysByDayInCurrentMonth[dayNum] ?: emptyList()
                                        val hasBirthday = birthdaysOnDay.isNotEmpty()

                                        CalendarDayCell(
                                            dayNumber = dayNum,
                                            isToday = isToday,
                                            isSelected = isSelected,
                                            hasBirthday = hasBirthday,
                                            birthdayCount = birthdaysOnDay.size,
                                            onClick = { onDateSelected(date) }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(38.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day / Month Birthdays Header
        item {
            val selectedDate = uiState.selectedDate
            val isCurrentMonthSelected = selectedDate != null &&
                    selectedDate.year == uiState.currentYearMonth.year &&
                    selectedDate.monthValue == uiState.currentYearMonth.monthValue

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedDate != null && isCurrentMonthSelected) {
                    val formattedSelected = selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.US))
                    Text(
                        text = "Birthdays on $formattedSelected (${uiState.birthdaysOnSelectedDate.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    val monthName = uiState.currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.US)
                    Text(
                        text = "Birthdays in $monthName (${uiState.birthdaysInCurrentMonth.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Birthday items list
        val displayItems = if (uiState.selectedDate != null &&
            uiState.selectedDate.year == uiState.currentYearMonth.year &&
            uiState.selectedDate.monthValue == uiState.currentYearMonth.monthValue
        ) {
            uiState.birthdaysOnSelectedDate
        } else {
            uiState.birthdaysInCurrentMonth
        }

        if (displayItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = if (uiState.selectedDate != null) "No birthdays on this day" else "No birthdays this month",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.selectedDate != null) {
                            TextButton(onClick = { onDateSelected(uiState.selectedDate) }) {
                                Text("View all in this month")
                            }
                        }
                    }
                }
            }
        } else {
            items(displayItems, key = { it.student.id }, contentType = { "birthday_card" }) { item ->
                BirthdayStudentCard(
                    item = item,
                    onClick = { onStudentClick(item.student.id) },
                    onCall = { onCallStudent(item.student.phone.ifBlank { item.student.altPhone }) },
                    onWhatsApp = { onWhatsAppWish(item.student.phone.ifBlank { item.student.altPhone }, item.student.fullName) }
                )
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    dayNumber: Int,
    isToday: Boolean,
    isSelected: Boolean,
    hasBirthday: Boolean,
    birthdayCount: Int,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        hasBirthday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.surface
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        hasBirthday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                } else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday || hasBirthday) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )

            if (hasBirthday) {
                Box(
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(if (birthdayCount > 1) 6.dp else 4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
fun UpcomingTimelineTabView(
    items: List<StudentBirthdayItem>,
    onStudentClick: (String) -> Unit,
    onCallStudent: (String) -> Unit,
    onWhatsAppWish: (String, String) -> Unit
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No birthdays found.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
        return
    }

    val todayItems = items.filter { it.isToday }
    val tomorrowItems = items.filter { !it.isToday && it.daysUntilNextBirthday == 1L }
    val thisWeekItems = items.filter { !it.isToday && it.daysUntilNextBirthday in 2L..7L }
    val thisMonthItems = items.filter { !it.isToday && it.daysUntilNextBirthday in 8L..30L }
    val laterItems = items.filter { !it.isToday && it.daysUntilNextBirthday > 30L }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (todayItems.isNotEmpty()) {
            item {
                TimelineSectionHeader(title = "Today", count = todayItems.size)
            }
            items(todayItems, key = { it.student.id }, contentType = { "birthday_card" }) { item ->
                BirthdayStudentCard(
                    item = item,
                    onClick = { onStudentClick(item.student.id) },
                    onCall = { onCallStudent(item.student.phone.ifBlank { item.student.altPhone }) },
                    onWhatsApp = { onWhatsAppWish(item.student.phone.ifBlank { item.student.altPhone }, item.student.fullName) }
                )
            }
        }

        if (tomorrowItems.isNotEmpty()) {
            item {
                TimelineSectionHeader(title = "Tomorrow", count = tomorrowItems.size)
            }
            items(tomorrowItems, key = { it.student.id }, contentType = { "birthday_card" }) { item ->
                BirthdayStudentCard(
                    item = item,
                    onClick = { onStudentClick(item.student.id) },
                    onCall = { onCallStudent(item.student.phone.ifBlank { item.student.altPhone }) },
                    onWhatsApp = { onWhatsAppWish(item.student.phone.ifBlank { item.student.altPhone }, item.student.fullName) }
                )
            }
        }

        if (thisWeekItems.isNotEmpty()) {
            item {
                TimelineSectionHeader(title = "This Week (Next 7 Days)", count = thisWeekItems.size)
            }
            items(thisWeekItems, key = { it.student.id }, contentType = { "birthday_card" }) { item ->
                BirthdayStudentCard(
                    item = item,
                    onClick = { onStudentClick(item.student.id) },
                    onCall = { onCallStudent(item.student.phone.ifBlank { item.student.altPhone }) },
                    onWhatsApp = { onWhatsAppWish(item.student.phone.ifBlank { item.student.altPhone }, item.student.fullName) }
                )
            }
        }

        if (thisMonthItems.isNotEmpty()) {
            item {
                TimelineSectionHeader(title = "Upcoming This Month", count = thisMonthItems.size)
            }
            items(thisMonthItems, key = { it.student.id }, contentType = { "birthday_card" }) { item ->
                BirthdayStudentCard(
                    item = item,
                    onClick = { onStudentClick(item.student.id) },
                    onCall = { onCallStudent(item.student.phone.ifBlank { item.student.altPhone }) },
                    onWhatsApp = { onWhatsAppWish(item.student.phone.ifBlank { item.student.altPhone }, item.student.fullName) }
                )
            }
        }

        if (laterItems.isNotEmpty()) {
            item {
                TimelineSectionHeader(title = "Later This Year", count = laterItems.size)
            }
            items(laterItems, key = { it.student.id }, contentType = { "birthday_card" }) { item ->
                BirthdayStudentCard(
                    item = item,
                    onClick = { onStudentClick(item.student.id) },
                    onCall = { onCallStudent(item.student.phone.ifBlank { item.student.altPhone }) },
                    onWhatsApp = { onWhatsAppWish(item.student.phone.ifBlank { item.student.altPhone }, item.student.fullName) }
                )
            }
        }
    }
}

@Composable
fun TimelineSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$count students",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BirthdayStudentCard(
    item: StudentBirthdayItem,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (item.isToday) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Student Initials Avatar - exactly matching StudentDirectoryScreen style
            val initials = item.student.fullName.trim().split("\\s+".toRegex()).let { parts ->
                if (parts.size > 1) "${parts.first().take(1)}${parts.last().take(1)}".uppercase()
                else parts.first().take(2).uppercase()
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Student Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.student.fullName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Countdown badge
                    val countdownBadge = when {
                        item.isToday -> "Today"
                        item.daysUntilNextBirthday == 1L -> "Tomorrow"
                        item.daysUntilNextBirthday < 7L -> "In ${item.daysUntilNextBirthday}d"
                        else -> null
                    }
                    if (countdownBadge != null) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = if (item.isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = countdownBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Class ${item.student.studentClass} • ${item.student.batchTimeSlot.ifBlank { item.student.school }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Birthday Date & Turning Age Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cake,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    val dateFormatted = item.nextBirthdayDate.format(DateTimeFormatter.ofPattern("d MMM", Locale.US))
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (item.turningAge != null) {
                        Text(
                            text = "• Turning ${item.turningAge}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Actions: Themed WhatsApp & Call buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalIconButton(
                    onClick = onWhatsApp,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Send WhatsApp Wish",
                        modifier = Modifier.size(18.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onCall,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call Student",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun dialStudent(context: Context, phone: String) {
    if (phone.isBlank()) {
        Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = "tel:$phone".toUri()
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open dialer: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

private fun sendWhatsAppWish(context: Context, phone: String, studentName: String) {
    if (phone.isBlank()) {
        Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val cleanPhone = phone.replace("[^0-9]".toRegex(), "")
        val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
        val message = "Dear $studentName, Happy Birthday! Wishing you a wonderful year ahead filled with happiness, health, and academic excellence! Warm wishes from Techno Vision."
        val encodedMessage = URLEncoder.encode(message, "UTF-8")
        val uri = "https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMessage".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open WhatsApp: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

@Preview(name = "Birthday Calendar Preview", showBackground = true)
@Composable
fun BirthdayCalendarPreview() {
    TechnoVisionTheme {
        val today = LocalDate.now()
        val items = com.soumit.technovision.ui.common.PreviewData.students.mapNotNull { student ->
            BirthdayUtils.calculateBirthdayItem(student, today)
        }
        val classes = listOf("All") + com.soumit.technovision.ui.common.PreviewData.students.map { it.studentClass }.distinct().sorted()

        BirthdayCalendarContent(
            uiState = BirthdayCalendarUiState(
                allItems = items,
                availableClasses = classes,
                isLoading = false
            ),
            onBack = {},
            onStudentClick = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onJumpToToday = {},
            onDateSelected = {},
            onSearchQueryChange = {},
            onClassFilterSelected = {},
            onTabSelected = {},
            onCallStudent = {},
            onWhatsAppWish = { _, _ -> }
        )
    }
}
