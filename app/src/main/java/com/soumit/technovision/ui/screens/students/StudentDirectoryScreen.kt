package com.soumit.technovision.ui.screens.students

import android.content.res.Configuration
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.ui.common.AppViewModelProvider
import com.soumit.technovision.ui.theme.TechnoVisionTheme

@Composable
fun StudentDirectoryScreen(
    onBack: () -> Unit,
    onNavigateToUpdateDelete: (String) -> Unit,
    onNavigateToViewer: (String) -> Unit,
    viewModel: StudentDirectoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    StudentDirectoryContent(
        uiState = uiState,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onBack = onBack,
        onNavigateToUpdateDelete = onNavigateToUpdateDelete,
        onNavigateToViewer = onNavigateToViewer
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StudentDirectoryContent(
    uiState: StudentDirectoryUiState,
    onSearchQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToUpdateDelete: (String) -> Unit,
    onNavigateToViewer: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Directory") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search by name, school, class, phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else if (uiState.filteredStudents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (uiState.errorMessage != null) "Error: ${uiState.errorMessage}" else "No students found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.filteredStudents, key = { it.id }) { student ->
                        val outstandingDue = uiState.outstandingBalances[student.id] ?: 0L
                        StudentCard(
                            student = student,
                            outstandingDue = outstandingDue,
                            userRole = uiState.userRole,
                            onCardClicked = { onNavigateToViewer(student.id) },
                            onEditSelected = { onNavigateToUpdateDelete(student.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentCard(
    student: StudentModel,
    outstandingDue: Long,
    userRole: String,
    onCardClicked: () -> Unit,
    onEditSelected: () -> Unit
) {
    val initials = student.fullName.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClicked() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Avatar + Name + Class & School Subtitle
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials.ifEmpty { "ST" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = student.fullName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Class ${student.studentClass} • ${student.school}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Right side: Compact Due Pill & Edit Button
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (outstandingDue > 0) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "₹$outstandingDue Due",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }

                if (userRole.equals("owner", ignoreCase = true) || userRole.equals("receptionist", ignoreCase = true)) {
                    IconButton(onClick = onEditSelected, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Student",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
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
        students = listOf(
            StudentModel(
                id = "1",
                fullName = "Aarav Sharma",
                studentClass = "X",
                school = "St. Xavier's School",
                phone = "9876543210"
            ),
            StudentModel(
                id = "2",
                fullName = "Priya Das",
                studentClass = "XII",
                school = "Alipurduar Girls High School",
                phone = "9123456789"
            )
        ),
        outstandingBalances = mapOf("1" to 500L, "2" to 0L),
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
        students = listOf(
            StudentModel(
                id = "1",
                fullName = "Aarav Sharma",
                studentClass = "X",
                school = "St. Xavier's School",
                phone = "9876543210"
            )
        ),
        outstandingBalances = mapOf("1" to 500L),
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
