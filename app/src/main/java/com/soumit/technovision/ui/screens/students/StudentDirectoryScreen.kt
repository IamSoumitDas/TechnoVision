package com.soumit.technovision.ui.screens.students

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.soumit.technovision.R
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.ui.screens.students.components.StudentCard
import com.soumit.technovision.ui.theme.TechnoVisionTheme

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun StudentDirectoryScreen(
    onBack: () -> Unit,
    onNavigateToUpdateDelete: (String) -> Unit,
    onNavigateToViewer: (String) -> Unit,
    viewModel: StudentDirectoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StudentDirectoryContent(
        uiState = uiState,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onClassFilterSelected = viewModel::onClassFilterSelected,
        onDuesOnlyToggled = viewModel::onDuesOnlyToggled,
        onSortOptionSelected = viewModel::onSortOptionSelected,
        onRefresh = viewModel::refresh,
        onBack = onBack,
        onNavigateToUpdateDelete = onNavigateToUpdateDelete,
        onNavigateToViewer = onNavigateToViewer
    )
}

@Composable
private fun ShimmerStudentCard(brushProvider: () -> Brush) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .drawBehind { drawRect(brushProvider()) }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .drawBehind { drawRect(brushProvider()) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .drawBehind { drawRect(brushProvider()) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDirectoryContent(
    uiState: StudentDirectoryUiState,
    onSearchQueryChange: (String) -> Unit,
    onClassFilterSelected: (String) -> Unit = {},
    onDuesOnlyToggled: (Boolean) -> Unit = {},
    onSortOptionSelected: (StudentSortOption) -> Unit = {},
    onRefresh: () -> Unit = {},
    onBack: () -> Unit,
    onNavigateToUpdateDelete: (String) -> Unit,
    onNavigateToViewer: (String) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val classOptions = remember {
        listOf("All", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII")
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_student_directory)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text(stringResource(R.string.student_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.common_search)) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Filter Chips Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dues Only Toggle Chip
                    FilterChip(
                        selected = uiState.duesOnlyFilter,
                        onClick = { onDuesOnlyToggled(!uiState.duesOnlyFilter) },
                        label = { Text("⚠️ Dues Pending") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )

                    // Sort Option Dropdown Chip
                    Box {
                        FilterChip(
                            selected = uiState.sortOption != StudentSortOption.NAME_ASC,
                            onClick = { sortMenuExpanded = true },
                            label = { Text("Sort: ${uiState.sortOption.displayName}") },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            StudentSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.displayName) },
                                    onClick = {
                                        onSortOptionSelected(option)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Class Filter Chips
                    classOptions.forEach { className ->
                        val isSelected = (uiState.selectedClassFilter == className)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onClassFilterSelected(className) },
                            label = { Text(if (className == "All") "All Classes" else "Class $className") }
                        )
                    }
                }

                // Active Filters Summary
                val activeFiltersCount = (if (uiState.selectedClassFilter != "All") 1 else 0) +
                        (if (uiState.duesOnlyFilter) 1 else 0) +
                        (if (uiState.searchQuery.isNotBlank()) 1 else 0)

                if (activeFiltersCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Showing ${uiState.filteredStudents.size} student${if (uiState.filteredStudents.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        TextButton(
                            onClick = {
                                onSearchQueryChange("")
                                onClassFilterSelected("All")
                                onDuesOnlyToggled(false)
                                onSortOptionSelected(StudentSortOption.NAME_ASC)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Reset Filters", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (uiState.isLoading) {
                    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
                    val translateAnim = transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1000f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1200),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "ShimmerTranslate"
                    )
                    val baseVariant = MaterialTheme.colorScheme.surfaceVariant
                    val shimmerColors = remember(baseVariant) {
                        listOf(
                            baseVariant.copy(alpha = 0.6f),
                            baseVariant.copy(alpha = 0.2f),
                            baseVariant.copy(alpha = 0.6f)
                        )
                    }
                    val brushProvider = {
                        Brush.linearGradient(
                            colors = shimmerColors,
                            start = Offset.Zero,
                            end = Offset(x = translateAnim.value, y = translateAnim.value)
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        repeat(5) {
                            ShimmerStudentCard(brushProvider = brushProvider)
                        }
                    }
                } else if (uiState.filteredStudents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (uiState.errorMessage != null) stringResource(R.string.common_error_prefix, uiState.errorMessage) else stringResource(R.string.student_empty_list),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.filteredStudents, key = { it.id }, contentType = { "student_item" }) { student ->
                            val outstandingDue = uiState.outstandingBalances[student.id] ?: 0L
                            StudentCard(
                                student = student,
                                outstandingDue = outstandingDue,
                                userRole = uiState.userRole,
                                onCardClicked = { onNavigateToViewer(student.id) },
                                onEditSelected = { onNavigateToUpdateDelete(student.id) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun StudentDirectoryScreenPreviewLight() {
    val previewState = StudentDirectoryUiState(
        students = com.soumit.technovision.ui.common.PreviewData.students,
        outstandingBalances = com.soumit.technovision.ui.common.PreviewData.outstandingBalances,
        isLoading = false,
        userRole = "owner"
    )
    TechnoVisionTheme(darkTheme = false) {
        StudentDirectoryContent(
            uiState = previewState,
            onSearchQueryChange = {},
            onBack = {},
            onNavigateToUpdateDelete = {},
            onNavigateToViewer = {}
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun StudentDirectoryScreenPreviewDark() {
    val previewState = StudentDirectoryUiState(
        students = com.soumit.technovision.ui.common.PreviewData.students,
        outstandingBalances = com.soumit.technovision.ui.common.PreviewData.outstandingBalances,
        isLoading = false,
        userRole = "owner"
    )
    TechnoVisionTheme(darkTheme = true) {
        StudentDirectoryContent(
            uiState = previewState,
            onSearchQueryChange = {},
            onBack = {},
            onNavigateToViewer = {},
            onNavigateToUpdateDelete = {}
        )
    }
}
