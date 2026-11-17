package com.soumit.technovision.ui.screens.fees

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soumit.technovision.data.model.FeePaymentModel
import com.soumit.technovision.ui.common.AppViewModelProvider
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GlobalHistoryScreen(
    onBack: () -> Unit,
    viewModel: GlobalHistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    GlobalHistoryContent(
        uiState = uiState,
        onBack = onBack,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onMethodFilterChange = viewModel::onMethodFilterChanged,
        onSetPaymentToDelete = viewModel::setPaymentToDelete,
        onConfirmDeletePayment = { viewModel.deletePayment {} }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GlobalHistoryContent(
    uiState: GlobalHistoryUiState,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onMethodFilterChange: (String) -> Unit,
    onSetPaymentToDelete: (FeePaymentModel?) -> Unit,
    onConfirmDeletePayment: () -> Unit
) {
    val paymentMethods = listOf("All", "UPI", "Cash", "Bank Transfer", "Other")

    val todayDateStr = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
    }

    val currentMonthName = remember {
        SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
    }

    val thisMonthCollected = remember(uiState.paymentHistory) {
        val calNow = Calendar.getInstance()
        val currentYear = calNow.get(Calendar.YEAR)
        val currentMonth = calNow.get(Calendar.MONTH)
        val calPayment = Calendar.getInstance()
        uiState.paymentHistory.filter { payment ->
            if (payment.timestamp != null) {
                calPayment.time = payment.timestamp.toDate()
                calPayment.get(Calendar.YEAR) == currentYear && calPayment.get(Calendar.MONTH) == currentMonth
            } else false
        }.sumOf { it.amountPaid }
    }

    val todayCollected = remember(uiState.paymentHistory, todayDateStr) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        uiState.paymentHistory.filter { payment ->
            payment.timestamp != null && dateFormat.format(payment.timestamp.toDate()) == todayDateStr
        }.sumOf { it.amountPaid }
    }

    val groupedHistory = remember(uiState.filteredPayments) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        uiState.filteredPayments.groupBy { payment ->
            if (payment.timestamp != null) {
                dateFormat.format(payment.timestamp.toDate())
            } else {
                "Unknown Date"
            }
        }
    }

    val isOwner = uiState.currentUserRole.equals("owner", ignoreCase = true)

    // Delete Payment Confirmation Dialog (Owner Only)
    if (uiState.paymentToDelete != null) {
        val target = uiState.paymentToDelete
        val paymentFormattedDate = remember(target.timestamp) {
            if (target.timestamp != null) {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(target.timestamp.toDate())
            } else "Recent"
        }
        val targetStudentName = target.studentName.ifBlank {
            uiState.studentsMap[target.studentId]?.fullName ?: "Student"
        }

        AlertDialog(
            onDismissRequest = { if (!uiState.isDeletingPayment) onSetPaymentToDelete(null) },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Received Payment?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to permanently delete this payment entry?")
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Student: $targetStudentName", fontWeight = FontWeight.Bold)
                            Text("Amount: ₹${target.amountPaid} via ${target.paymentMethod}")
                            Text("Date: $paymentFormattedDate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            if (target.receivedBy.isNotBlank()) {
                                Text("Collected by: ${target.receivedBy}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                    Text(
                        "⚠️ This will revert the payment record, adjust the student's ledger dues, and log this deletion in Activity History.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmDeletePayment,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !uiState.isDeletingPayment
                ) {
                    if (uiState.isDeletingPayment) {
                        CircularWavyProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onError)
                    } else {
                        Text("Delete Payment")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { onSetPaymentToDelete(null) },
                    enabled = !uiState.isDeletingPayment
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment History") },
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary Cards Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Month's Total
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$currentMonthName's Total",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹$thisMonthCollected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Card 2: Today's Total
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Today's Total",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹$todayCollected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by student, staff, remarks...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // Method Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                paymentMethods.forEach { method ->
                    FilterChip(
                        selected = uiState.selectedMethodFilter == method,
                        onClick = { onMethodFilterChange(method) },
                        label = { Text(method, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Main Content Area
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else if (uiState.filteredPayments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (uiState.searchQuery.isNotBlank() || uiState.selectedMethodFilter != "All") {
                                "No payment records match your filters."
                            } else {
                                "No payments recorded yet."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (uiState.searchQuery.isNotBlank() || uiState.selectedMethodFilter != "All") {
                            TextButton(onClick = {
                                onSearchQueryChange("")
                                onMethodFilterChange("All")
                            }) {
                                Text("Reset Filters")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    groupedHistory.forEach { (dateStr, dailyPayments) ->
                        val dayTotal = dailyPayments.sumOf { it.amountPaid }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (dateStr == todayDateStr) "Today • $dateStr" else dateStr,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        text = "${dailyPayments.size} payment${if (dailyPayments.size == 1) "" else "s"} • ₹$dayTotal",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        items(dailyPayments, key = { it.id.ifBlank { "${it.studentId}_${it.timestamp?.seconds}" } }) { payment ->
                            val student = uiState.studentsMap[payment.studentId]
                            val resolvedName = payment.studentName.ifBlank {
                                student?.fullName ?: "Unknown Student"
                            }
                            val resolvedSubtitle = student?.let {
                                val classPart = if (it.studentClass.isNotBlank()) "Class ${it.studentClass}" else ""
                                val schoolPart = it.school.ifBlank { "" }
                                listOf(classPart, schoolPart).filter { s -> s.isNotBlank() }.joinToString(" • ")
                            }

                            val updatedPayment = payment.copy(studentName = resolvedName)

                            PaymentHistoryCard(
                                payment = updatedPayment,
                                showStudentName = true,
                                studentSubtitle = resolvedSubtitle,
                                canDelete = isOwner,
                                onDeleteClick = { onSetPaymentToDelete(updatedPayment) }
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
fun GlobalHistoryScreenPreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        GlobalHistoryContent(
            uiState = GlobalHistoryUiState(isLoading = false),
            onBack = {},
            onSearchQueryChange = {},
            onMethodFilterChange = {},
            onSetPaymentToDelete = {},
            onConfirmDeletePayment = {}
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun GlobalHistoryScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        GlobalHistoryContent(
            uiState = GlobalHistoryUiState(isLoading = false),
            onBack = {},
            onSearchQueryChange = {},
            onMethodFilterChange = {},
            onSetPaymentToDelete = {},
            onConfirmDeletePayment = {}
        )
    }
}
