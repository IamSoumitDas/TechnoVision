package com.soumit.technovision.ui.screens.attendance

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import com.soumit.technovision.util.AttendanceUtils

internal fun resolveAdminName(
    rawName: String,
    userMap: Map<String, String>
): String = AttendanceUtils.resolveAdminName(rawName, userMap)

@Composable
fun VerifyTeacherAttendanceScreen(
    onBack: () -> Unit,
    onNavigateToViewer: (() -> Unit)? = null,
    viewModel: VerifyTeacherAttendanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    VerifyTeacherAttendanceContent(
        uiState = uiState,
        onBack = onBack,
        onNavigateToViewer = onNavigateToViewer,
        onVerify = viewModel::verifyRequest,
        onReject = viewModel::rejectRequest
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VerifyTeacherAttendanceContent(
    uiState: VerifyTeacherAttendanceUiState,
    onBack: () -> Unit,
    onNavigateToViewer: (() -> Unit)?,
    onVerify: (TeacherAttendanceRequestModel) -> Unit,
    onReject: (TeacherAttendanceRequestModel) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "Pending", "Verified", "Rejected")

    val filteredRequests = remember(uiState.requests, selectedFilter) {
        when (selectedFilter) {
            "Pending" -> uiState.requests.filter { it.status.trim().equals("Pending", ignoreCase = true) }
            "Verified" -> uiState.requests.filter { it.status.trim().equals("Verified", ignoreCase = true) }
            "Rejected" -> uiState.requests.filter { it.status.trim().equals("Rejected", ignoreCase = true) }
            else -> uiState.requests
        }
    }

    val pendingCount = uiState.pendingRequests.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Teacher Attendance") },
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
                                contentDescription = "View Verified Records"
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!uiState.isOnline) {
                com.soumit.technovision.ui.common.OfflineWarningBanner(
                    message = "Approving or rejecting faculty attendance requests requires an active internet connection."
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Pending Approvals",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Verify daily completed batch hours",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Surface(
                        color = if (pendingCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "$pendingCount Pending",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (pendingCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else if (filteredRequests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedFilter == "All") "No teacher attendance requests logged." else "No $selectedFilter requests found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredRequests, key = { it.id }, contentType = { "verification_request" }) { request ->
                        AdminVerifyRequestCard(
                            request = request,
                            userMap = uiState.userMap,
                            isOnline = uiState.isOnline,
                            onVerify = { onVerify(request) },
                            onReject = { onReject(request) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminVerifyRequestCard(
    request: TeacherAttendanceRequestModel,
    userMap: Map<String, String>,
    isOnline: Boolean = true,
    onVerify: () -> Unit,
    onReject: () -> Unit
) {
    val isVerified = request.status.trim().equals("Verified", ignoreCase = true)
    val isRejected = request.status.trim().equals("Rejected", ignoreCase = true)
    val isPending = request.status.trim().equals("Pending", ignoreCase = true) || (!isVerified && !isRejected)
    var showRejectDialog by remember { mutableStateOf(false) }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reject Attendance Request?") },
            text = {
                Text(
                    "Are you sure you want to reject attendance request for " +
                            "${request.teacherName.ifBlank { "this faculty member" }} for slot " +
                            "${request.timeSlot} on ${request.date}?\n\n" +
                            "This will release the time slot so any faculty member can request it again."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRejectDialog = false
                        onReject()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reject Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isVerified -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                isRejected -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = request.teacherName.ifBlank { "Faculty Member" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Slot: ${request.timeSlot}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Date: ${request.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
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

            when {
                isPending -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRejectDialog = true },
                            enabled = isOnline,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, if (isOnline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                            Text("Reject")
                        }

                        Button(
                            onClick = onVerify,
                            enabled = isOnline,
                            modifier = Modifier.weight(1.5f)
                        ) {
                            if (!isOnline) {
                                Icon(Icons.Default.WifiOff, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                Text("Offline")
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                Text("Verify")
                            }
                        }
                    }
                }
                isVerified -> {
                    val verifier = remember(request.verifiedBy, userMap) {
                        resolveAdminName(request.verifiedBy, userMap)
                    }
                    Text(
                        text = "Verified by $verifier",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                else -> {
                    val rejector = remember(request.rejectedBy, userMap) {
                        resolveAdminName(request.rejectedBy, userMap)
                    }
                    Text(
                        text = "Rejected by $rejector",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun VerifyTeacherAttendanceScreenPreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        VerifyTeacherAttendanceContent(
            uiState = VerifyTeacherAttendanceUiState(
                requests = com.soumit.technovision.ui.common.PreviewData.teacherAttendanceRequests,
                userMap = com.soumit.technovision.ui.common.PreviewData.userMap,
                currentAdminName = com.soumit.technovision.ui.common.PreviewData.ownerStaff.fullName,
                isLoading = false
            ),
            onBack = {},
            onNavigateToViewer = null,
            onVerify = {},
            onReject = {}
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun VerifyTeacherAttendanceScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        VerifyTeacherAttendanceContent(
            uiState = VerifyTeacherAttendanceUiState(
                requests = com.soumit.technovision.ui.common.PreviewData.teacherAttendanceRequests,
                userMap = com.soumit.technovision.ui.common.PreviewData.userMap,
                currentAdminName = com.soumit.technovision.ui.common.PreviewData.ownerStaff.fullName,
                isLoading = false
            ),
            onBack = {},
            onNavigateToViewer = null,
            onVerify = {},
            onReject = {}
        )
    }
}
