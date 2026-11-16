package com.soumit.technovision.ui.screens.fees

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soumit.technovision.data.model.FeePaymentModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.ui.common.AppViewModelProvider
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FeeCollectionScreen(
    onBack: () -> Unit,
    onNavigateToGlobalHistory: () -> Unit = {},
    viewModel: FeeCollectionViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    FeeCollectionContent(
        uiState = uiState,
        onBack = onBack,
        onNavigateToGlobalHistory = onNavigateToGlobalHistory,
        onSelectStudent = viewModel::onSelectStudent,
        onChangeStudent = { viewModel.onSelectStudent(StudentModel()) },
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onAmountChange = viewModel::onAmountChanged,
        onMethodChange = viewModel::onMethodChanged,
        onRemarksChange = viewModel::onRemarksChanged,
        onPaymentDateChange = viewModel::onPaymentDateChanged,
        onSetDatePickerVisible = viewModel::setDatePickerVisible,
        onSetMethodExpanded = viewModel::setMethodDropdownExpanded,
        onSetPaymentToDelete = viewModel::setPaymentToDelete,
        onSubmitPayment = { viewModel.submitPayment {} },
        onConfirmDeletePayment = { viewModel.deletePayment {} }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeeCollectionContent(
    uiState: FeeCollectionUiState,
    onBack: () -> Unit,
    onNavigateToGlobalHistory: () -> Unit,
    onSelectStudent: (StudentModel) -> Unit,
    onChangeStudent: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onMethodChange: (String) -> Unit,
    onRemarksChange: (String) -> Unit,
    onPaymentDateChange: (String) -> Unit,
    onSetDatePickerVisible: (Boolean) -> Unit,
    onSetMethodExpanded: (Boolean) -> Unit,
    onSetPaymentToDelete: (FeePaymentModel?) -> Unit,
    onSubmitPayment: () -> Unit,
    onConfirmDeletePayment: () -> Unit
) {
    val paymentMethods = listOf("Cash", "UPI", "Bank Transfer", "Other")

    // Date Picker Dialog
    if (uiState.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { onSetDatePickerVisible(false) },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            val formatted = String.format(
                                Locale.US,
                                "%02d/%02d/%04d",
                                cal.get(Calendar.DAY_OF_MONTH),
                                cal.get(Calendar.MONTH) + 1,
                                cal.get(Calendar.YEAR)
                            )
                            onPaymentDateChange(formatted)
                        }
                        onSetDatePickerVisible(false)
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { onSetDatePickerVisible(false) }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Delete Payment Confirmation Dialog (Owner Only)
    if (uiState.paymentToDelete != null) {
        val target = uiState.paymentToDelete
        val paymentFormattedDate = remember(target.timestamp) {
            if (target.timestamp != null) {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(target.timestamp.toDate())
            } else "Recent"
        }

        AlertDialog(
            onDismissRequest = { if (!uiState.isDeletingPayment) onSetPaymentToDelete(null) },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Received Payment?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to delete this payment record?")
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Student: ${target.studentName.ifBlank { uiState.selectedStudent?.fullName ?: "Student" }}", fontWeight = FontWeight.Bold)
                            Text("Amount: ₹${target.amountPaid} via ${target.paymentMethod}")
                            Text("Date: $paymentFormattedDate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Text(
                        "⚠️ This will revert the student's dues by ₹${target.amountPaid} and log the deletion in Activity History.",
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
                        Text("Delete Record")
                    }
                }
            },
            dismissButton = {
                TextButton(
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
                title = { Text("Collect Payment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToGlobalHistory) {
                        Icon(Icons.Default.History, contentDescription = "Global Payment History")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (uiState.isLoadingStudents) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Receive Monthly Installment",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                if (uiState.selectedStudent == null || uiState.selectedStudent.id.isBlank()) {
                    // Search & Picker Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            label = { Text("Search Student") },
                            placeholder = { Text("Search by name, class, or phone...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
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

                        if (uiState.filteredStudents.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (uiState.searchQuery.isBlank()) "No students registered in the system."
                                        else "No students found matching \"${uiState.searchQuery}\".",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Students (${uiState.filteredStudents.size}) • Tap to select",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.SemiBold
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                uiState.filteredStudents.take(15).forEach { student ->
                                    Card(
                                        onClick = { onSelectStudent(student) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = student.fullName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Class: ${student.studentClass} • ${student.school.ifBlank { "No School" }}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                                if (student.phone.isNotBlank()) {
                                                    Text(
                                                        text = "Phone: ${student.phone}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = MaterialTheme.shapes.small
                                            ) {
                                                Text(
                                                    text = "₹${student.monthlyInstallment}/mo",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (uiState.filteredStudents.size > 15) {
                                    Text(
                                        text = "Showing first 15 of ${uiState.filteredStudents.size} students. Type above to refine search.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val student = uiState.selectedStudent
                    val balanceDue = uiState.balanceDue

                    // Selected Student Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Class: ${student.studentClass} • ${student.school.ifBlank { "No School" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                if (student.phone.isNotBlank()) {
                                    Text(
                                        text = "Phone: ${student.phone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = onChangeStudent,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Change")
                            }
                        }
                    }

                    // Student Financial Dues Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                balanceDue > 0 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                balanceDue < 0 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                                else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Standard Monthly Installment",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "₹${student.monthlyInstallment}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Total Expected: ₹${uiState.expectedTotalFees}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "Total Paid: ₹${uiState.totalPaid}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when {
                                    balanceDue > 0 -> "Outstanding Balance: ₹$balanceDue (Pending)"
                                    balanceDue < 0 -> "Advance Credit: ₹${-balanceDue} (Overpaid)"
                                    else -> "Balance: Fully Paid Up ✓"
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    balanceDue > 0 -> MaterialTheme.colorScheme.error
                                    balanceDue < 0 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }

                    // Amount Received & Quick Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = uiState.amountPaid,
                            onValueChange = onAmountChange,
                            label = { Text("Amount Received (₹)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (balanceDue > 0) {
                                SuggestionChip(
                                    onClick = { onAmountChange(balanceDue.toString()) },
                                    label = { Text("Full Due (₹$balanceDue)", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            SuggestionChip(
                                onClick = { onAmountChange(student.monthlyInstallment.toString()) },
                                label = { Text("1 Month (₹${student.monthlyInstallment})", style = MaterialTheme.typography.labelSmall) }
                            )
                            if (uiState.amountPaid.isNotBlank()) {
                                SuggestionChip(
                                    onClick = { onAmountChange("") },
                                    label = { Text("Clear", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    // Payment Date Picker Field
                    OutlinedTextField(
                        value = uiState.paymentDateStr,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Date") },
                        trailingIcon = {
                            IconButton(onClick = { onSetDatePickerVisible(true) }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Pick Date")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetDatePickerVisible(true) }
                    )

                    // Payment Method Dropdown
                    ExposedDropdownMenuBox(
                        expanded = uiState.methodExpanded,
                        onExpandedChange = onSetMethodExpanded,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = uiState.selectedMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Method") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.methodExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded = uiState.methodExpanded,
                            onDismissRequest = { onSetMethodExpanded(false) }
                        ) {
                            paymentMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = { onMethodChange(method) }
                                )
                            }
                        }
                    }

                    // Remarks
                    OutlinedTextField(
                        value = uiState.remarks,
                        onValueChange = onRemarksChange,
                        label = { Text("Remarks / Receipt Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Save Button / Loader
                    if (uiState.isSaving) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    } else {
                        Button(
                            onClick = onSubmitPayment,
                            enabled = uiState.amountPaid.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Record & Save Payment")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Payment History Ledger
                    Text(
                        text = "Payment History for ${student.fullName} (${uiState.paymentHistory.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    if (uiState.paymentHistory.isEmpty()) {
                        Text(
                            text = "No payment records found for this student.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val canDelete = uiState.currentUserRole == "owner" || uiState.currentUserRole.isBlank()
                            uiState.paymentHistory.forEach { payment ->
                                PaymentHistoryCard(
                                    payment = payment,
                                    showStudentName = false,
                                    canDelete = canDelete,
                                    onDeleteClick = { onSetPaymentToDelete(payment) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentHistoryCard(
    payment: FeePaymentModel,
    showStudentName: Boolean = true,
    studentSubtitle: String? = null,
    canDelete: Boolean = false,
    onDeleteClick: () -> Unit = {}
) {
    val formattedDate = remember(payment.timestamp) {
        if (payment.timestamp != null) {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(payment.timestamp.toDate())
        } else {
            "Just now"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (showStudentName) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(end = 8.dp)) {
                            Text(
                                text = payment.studentName.ifBlank { "Unknown Student" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!studentSubtitle.isNullOrBlank()) {
                                Text(
                                    text = studentSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Payment Received",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "₹${payment.amountPaid}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (canDelete) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete payment",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 0.5.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = when (payment.paymentMethod) {
                            "UPI" -> MaterialTheme.colorScheme.tertiaryContainer
                            "Cash" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = payment.paymentMethod,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = when (payment.paymentMethod) {
                                "UPI" -> MaterialTheme.colorScheme.onTertiaryContainer
                                "Cash" -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }

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
                            text = "Received by: ${payment.receivedBy.ifBlank { "Staff" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (payment.remarks.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Note: ${payment.remarks}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun FeeCollectionScreenPreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        FeeCollectionContent(
            uiState = FeeCollectionUiState(isLoadingStudents = false),
            onBack = {},
            onNavigateToGlobalHistory = {},
            onSelectStudent = {},
            onChangeStudent = {},
            onSearchQueryChange = {},
            onAmountChange = {},
            onMethodChange = {},
            onRemarksChange = {},
            onPaymentDateChange = {},
            onSetDatePickerVisible = {},
            onSetMethodExpanded = {},
            onSetPaymentToDelete = {},
            onSubmitPayment = {},
            onConfirmDeletePayment = {}
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FeeCollectionScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        FeeCollectionContent(
            uiState = FeeCollectionUiState(isLoadingStudents = false),
            onBack = {},
            onNavigateToGlobalHistory = {},
            onSelectStudent = {},
            onChangeStudent = {},
            onSearchQueryChange = {},
            onAmountChange = {},
            onMethodChange = {},
            onRemarksChange = {},
            onPaymentDateChange = {},
            onSetDatePickerVisible = {},
            onSetMethodExpanded = {},
            onSetPaymentToDelete = {},
            onSubmitPayment = {},
            onConfirmDeletePayment = {}
        )
    }
}
