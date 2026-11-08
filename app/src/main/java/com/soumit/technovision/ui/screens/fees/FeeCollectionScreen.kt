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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.soumit.technovision.data.model.FeePaymentModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import com.soumit.technovision.util.BillingUtils
import com.soumit.technovision.util.SystemLogger
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeeCollectionScreen(
    onBack: () -> Unit,
    onNavigateToGlobalHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    val mockStudents = remember {
        listOf(
            StudentModel(
                id = "1",
                fullName = "Aarav Sharma",
                studentClass = "X",
                school = "Delhi Public School",
                phone = "9876543210",
                monthlyInstallment = 500L,
                admissionDate = "15/06/2026"
            ),
            StudentModel(
                id = "2",
                fullName = "Priya Das",
                studentClass = "XII",
                school = "Kendriya Vidyalaya",
                phone = "9876543211",
                monthlyInstallment = 600L,
                admissionDate = "10/06/2026"
            ),
            StudentModel(
                id = "3",
                fullName = "Rahul Roy",
                studentClass = "IX",
                school = "Modern High School",
                phone = "9876543212",
                monthlyInstallment = 500L,
                admissionDate = "01/07/2026"
            )
        )
    }

    var studentList by remember { mutableStateOf(if (isInPreview) mockStudents else emptyList()) }
    var selectedStudent by remember { mutableStateOf<StudentModel?>(if (isInPreview) mockStudents[0] else null) }
    var searchQuery by remember { mutableStateOf("") }

    // User Role State (Owner has permission to delete payments)
    var currentUserRole by remember { mutableStateOf(if (isInPreview) "owner" else "") }

    // Delete confirmation state
    var paymentToDelete by remember { mutableStateOf<FeePaymentModel?>(null) }
    var isDeletingPayment by remember { mutableStateOf(false) }

    // Financial states for selected student
    var expectedTotalFees by remember { mutableLongStateOf(0L) }
    var totalPaid by remember { mutableLongStateOf(0L) }

    // Payment Form States
    var amountPaid by remember { mutableStateOf(if (isInPreview) mockStudents[0].monthlyInstallment.toString() else "") }
    var selectedMethod by remember { mutableStateOf("Cash") }
    var methodExpanded by remember { mutableStateOf(false) }
    val paymentMethods = listOf("Cash", "UPI", "Bank Transfer", "Other")
    var remarks by remember { mutableStateOf("") }

    // Payment Date States
    var paymentDateStr by remember {
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    var paymentHistory by remember { mutableStateOf<List<FeePaymentModel>>(emptyList()) }
    var isLoadingStudents by remember { mutableStateOf(!isInPreview) }
    var isSaving by remember { mutableStateOf(false) }

    // Fetch students and current user role on load
    LaunchedEffect(Unit) {
        if (isInPreview) {
            studentList = mockStudents
            selectedStudent = mockStudents[0]
            amountPaid = mockStudents[0].monthlyInstallment.toString()
            isLoadingStudents = false
        } else {
            val db = FirebaseFirestore.getInstance()
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                db.collection("users").document(currentUser.uid).get()
                    .addOnSuccessListener { doc ->
                        currentUserRole = doc.getString("role")?.lowercase() ?: ""
                    }
            }

            db.collection("students")
                .orderBy("fullName")
                .get()
                .addOnSuccessListener { result ->
                    val list = result.documents.map { doc ->
                        StudentModel(
                            id = doc.id,
                            fullName = doc.getString("fullName") ?: "",
                            studentClass = doc.getString("studentClass") ?: "",
                            school = doc.getString("school") ?: "",
                            monthlyInstallment = doc.getLong("monthlyInstallment") ?: 500L,
                            phone = doc.getString("phone") ?: "",
                            admissionDate = doc.getString("admissionDate") ?: ""
                        )
                    }
                    studentList = list
                    isLoadingStudents = false
                }
                .addOnFailureListener { e ->
                    isLoadingStudents = false
                    Toast.makeText(context, "Error loading students: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                            }
                            paymentDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(localCal.time)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Delete Payment Confirmation Dialog (Owner Only)
    if (paymentToDelete != null) {
        val target = paymentToDelete!!
        val paymentFormattedDate = remember(target.timestamp) {
            if (target.timestamp != null) {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(target.timestamp.toDate())
            } else "Recent"
        }

        AlertDialog(
            onDismissRequest = { if (!isDeletingPayment) paymentToDelete = null },
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
                            Text("Student: ${target.studentName.ifBlank { selectedStudent?.fullName ?: "Student" }}", fontWeight = FontWeight.Bold)
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
                    onClick = {
                        if (isInPreview) {
                            totalPaid = (totalPaid - target.amountPaid).coerceAtLeast(0L)
                            paymentHistory = paymentHistory.filter { it.id != target.id }
                            paymentToDelete = null
                            Toast.makeText(context, "Preview: Payment deleted successfully!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isDeletingPayment = true
                        val db = FirebaseFirestore.getInstance()
                        val studentName = target.studentName.ifBlank { selectedStudent?.fullName ?: "Student" }

                        db.collection("fee_payments").document(target.id).delete()
                            .addOnSuccessListener {
                                isDeletingPayment = false
                                Toast.makeText(context, "Payment record deleted.", Toast.LENGTH_SHORT).show()

                                SystemLogger.log(
                                    action = "Fee Payment Deleted",
                                    details = "Deleted payment of ₹${target.amountPaid} (${target.paymentMethod}) for student: $studentName"
                                )

                                paymentHistory = paymentHistory.filter { it.id != target.id }
                                totalPaid = (totalPaid - target.amountPaid).coerceAtLeast(0L)
                                paymentToDelete = null
                            }
                            .addOnFailureListener { e ->
                                isDeletingPayment = false
                                Toast.makeText(context, "Failed to delete: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isDeletingPayment
                ) {
                    if (isDeletingPayment) {
                        CircularWavyProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onError)
                    } else {
                        Text("Delete Record")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { paymentToDelete = null },
                    enabled = !isDeletingPayment
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Fetch payment history and calculate financial dues safely without composite index requirement
    DisposableEffect(selectedStudent?.id) {
        val currentStudent = selectedStudent

        if (currentStudent == null) {
            paymentHistory = emptyList()
            expectedTotalFees = 0L
            totalPaid = 0L
            onDispose { }
        } else if (isInPreview) {
            val (_, expected) = BillingUtils.calculateBillingMonths(
                currentStudent.admissionDate,
                currentStudent.monthlyInstallment
            )
            expectedTotalFees = expected
            totalPaid = 1000L
            paymentHistory = listOf(
                FeePaymentModel(
                    id = "1",
                    studentId = currentStudent.id,
                    studentName = currentStudent.fullName,
                    amountPaid = 500L,
                    paymentMethod = "UPI",
                    remarks = "Installment for July",
                    receivedBy = "Soumit Das",
                    timestamp = Timestamp.now()
                ),
                FeePaymentModel(
                    id = "2",
                    studentId = currentStudent.id,
                    studentName = currentStudent.fullName,
                    amountPaid = 500L,
                    paymentMethod = "Cash",
                    remarks = "Admission Fee",
                    receivedBy = "Soumit Das",
                    timestamp = Timestamp(Date(System.currentTimeMillis() - 86400000))
                )
            )
            onDispose { }
        } else {
            val (_, expected) = BillingUtils.calculateBillingMonths(
                currentStudent.admissionDate,
                currentStudent.monthlyInstallment
            )
            expectedTotalFees = expected

            // Listen to student payments without compound orderBy to avoid Firestore composite index failures
            val registration = FirebaseFirestore.getInstance()
                .collection("fee_payments")
                .whereEqualTo("studentId", currentStudent.id)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Toast.makeText(context, "Error updating ledger: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        var sum = 0L
                        val history = snapshot.documents.map { doc ->
                            val paid = doc.getLong("amountPaid") ?: 0L
                            sum += paid
                            FeePaymentModel(
                                id = doc.id,
                                studentId = doc.getString("studentId") ?: "",
                                studentName = doc.getString("studentName") ?: "",
                                amountPaid = paid,
                                paymentMethod = doc.getString("paymentMethod") ?: "",
                                remarks = doc.getString("remarks") ?: "",
                                receivedBy = doc.getString("receivedBy") ?: "",
                                timestamp = doc.getTimestamp("timestamp")
                            )
                        }.sortedByDescending { it.timestamp?.toDate() ?: Date(0) }

                        paymentHistory = history
                        totalPaid = sum
                    }
                }

            onDispose {
                registration.remove()
            }
        }
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
        if (isLoadingStudents) {
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

                // 1. Student Selector: Searchable Picker or Selected Card
                if (selectedStudent == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search Student") },
                            placeholder = { Text("Search by name, class, or phone...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        val filteredStudents = remember(studentList, searchQuery) {
                            if (searchQuery.isBlank()) {
                                studentList
                            } else {
                                studentList.filter {
                                    it.fullName.contains(searchQuery, ignoreCase = true) ||
                                        it.studentClass.contains(searchQuery, ignoreCase = true) ||
                                        it.phone.contains(searchQuery) ||
                                        it.school.contains(searchQuery, ignoreCase = true)
                                }
                            }
                        }

                        if (filteredStudents.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (searchQuery.isBlank()) "No students registered in the system."
                                        else "No students found matching \"$searchQuery\".",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Students (${filteredStudents.size}) • Tap to select",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.SemiBold
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                filteredStudents.take(15).forEach { student ->
                                    Card(
                                        onClick = {
                                            selectedStudent = student
                                            amountPaid = student.monthlyInstallment.toString()
                                            searchQuery = ""
                                        },
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

                                if (filteredStudents.size > 15) {
                                    Text(
                                        text = "Showing first 15 of ${filteredStudents.size} students. Type above to refine search.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Selected Student Card
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
                                    text = selectedStudent!!.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Class: ${selectedStudent!!.studentClass} • ${selectedStudent!!.school.ifBlank { "No School" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                if (selectedStudent!!.phone.isNotBlank()) {
                                    Text(
                                        text = "Phone: ${selectedStudent!!.phone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    selectedStudent = null
                                    amountPaid = ""
                                    remarks = ""
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Change")
                            }
                        }
                    }

                    val balanceDue = expectedTotalFees - totalPaid

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
                                    text = "₹${selectedStudent!!.monthlyInstallment}",
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
                                Text(text = "Total Expected: ₹$expectedTotalFees", style = MaterialTheme.typography.bodySmall)
                                Text(text = "Total Paid: ₹$totalPaid", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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

                    // 2. Amount Received & Quick Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = amountPaid,
                            onValueChange = { amountPaid = it },
                            label = { Text("Amount Received (₹)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick Fill Amount Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (balanceDue > 0) {
                                SuggestionChip(
                                    onClick = { amountPaid = balanceDue.toString() },
                                    label = { Text("Full Due (₹$balanceDue)", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            SuggestionChip(
                                onClick = { amountPaid = selectedStudent!!.monthlyInstallment.toString() },
                                label = { Text("1 Month (₹${selectedStudent!!.monthlyInstallment})", style = MaterialTheme.typography.labelSmall) }
                            )
                            if (amountPaid.isNotBlank()) {
                                SuggestionChip(
                                    onClick = { amountPaid = "" },
                                    label = { Text("Clear", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    // 3. Payment Date Picker Field
                    OutlinedTextField(
                        value = paymentDateStr,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Date") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Pick Date")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    )

                    // 4. Payment Method Dropdown
                    ExposedDropdownMenuBox(
                        expanded = methodExpanded,
                        onExpandedChange = { methodExpanded = !methodExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Method") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded = methodExpanded,
                            onDismissRequest = { methodExpanded = false }
                        ) {
                            paymentMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = {
                                        selectedMethod = method
                                        methodExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 5. Remarks / Notes
                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Remarks / Receipt Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 6. Save Payment Action Button
                    if (isSaving) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    } else {
                        Button(
                            onClick = {
                                if (selectedStudent == null || amountPaid.isBlank()) {
                                    Toast.makeText(context, "Please enter the amount received.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val parsedAmount = amountPaid.toLongOrNull()
                                if (parsedAmount == null || parsedAmount <= 0) {
                                    Toast.makeText(context, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                if (isInPreview) {
                                    Toast.makeText(context, "Preview: Payment of ₹$parsedAmount recorded!", Toast.LENGTH_SHORT).show()
                                    totalPaid += parsedAmount
                                    amountPaid = ""
                                    remarks = ""
                                    return@Button
                                }

                                isSaving = true
                                val db = FirebaseFirestore.getInstance()
                                val currentUser = FirebaseAuth.getInstance().currentUser

                                // Parse selected payment date into Timestamp
                                val paymentTimestamp = try {
                                    val parsedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(paymentDateStr)
                                    if (parsedDate != null) {
                                        val cal = Calendar.getInstance().apply { time = parsedDate }
                                        val now = Calendar.getInstance()
                                        if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
                                            Timestamp.now()
                                        } else {
                                            cal.set(Calendar.HOUR_OF_DAY, 12)
                                            cal.set(Calendar.MINUTE, 0)
                                            cal.set(Calendar.SECOND, 0)
                                            Timestamp(cal.time)
                                        }
                                    } else {
                                        Timestamp.now()
                                    }
                                } catch (_: Exception) {
                                    Timestamp.now()
                                }

                                val studentId = selectedStudent!!.id
                                val studentName = selectedStudent!!.fullName
                                val currentUserId = currentUser?.uid

                                if (currentUserId.isNullOrBlank()) {
                                    isSaving = false
                                    Toast.makeText(context, "Authentication required. Please sign in again.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                db.collection("users").document(currentUserId).get()
                                    .addOnSuccessListener { userDoc ->
                                        val staffName = if (userDoc.exists()) userDoc.getString("fullName") ?: "Staff" else "Owner"

                                        val paymentData = hashMapOf(
                                            "studentId" to studentId,
                                            "studentName" to studentName,
                                            "amountPaid" to parsedAmount,
                                            "paymentMethod" to selectedMethod,
                                            "remarks" to remarks.trim(),
                                            "receivedBy" to staffName,
                                            "timestamp" to paymentTimestamp
                                        )

                                        db.collection("fee_payments").add(paymentData)
                                            .addOnSuccessListener { newDocRef ->
                                                isSaving = false
                                                Toast.makeText(context, "Payment of ₹$parsedAmount recorded successfully!", Toast.LENGTH_SHORT).show()

                                                SystemLogger.log(
                                                    action = "Fee Collected",
                                                    details = "Received ₹$parsedAmount via $selectedMethod for student: $studentName (Date: $paymentDateStr)"
                                                )

                                                // Update local state immediately so UI refreshes without re-triggering duplicate payment
                                                val newRecord = FeePaymentModel(
                                                    id = newDocRef.id,
                                                    studentId = studentId,
                                                    studentName = studentName,
                                                    amountPaid = parsedAmount,
                                                    paymentMethod = selectedMethod,
                                                    remarks = remarks.trim(),
                                                    receivedBy = staffName,
                                                    timestamp = paymentTimestamp
                                                )
                                                paymentHistory = listOf(newRecord) + paymentHistory
                                                totalPaid += parsedAmount

                                                // Reset payment form fields to prevent accidental multiple submissions
                                                amountPaid = ""
                                                remarks = ""
                                            }
                                            .addOnFailureListener { e ->
                                                isSaving = false
                                                Toast.makeText(context, "Failed to record payment: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        isSaving = false
                                        Toast.makeText(context, "Failed to fetch staff info: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                            },
                            enabled = amountPaid.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Record & Save Payment")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // 7. Payment History Ledger Section for Selected Student
                    Text(
                        text = "Payment History for ${selectedStudent?.fullName} (${paymentHistory.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    if (paymentHistory.isEmpty()) {
                        Text(
                            text = "No payment records found for this student.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val isOwner = currentUserRole == "owner" || currentUserRole.isBlank() // fallback preview
                            paymentHistory.forEach { payment ->
                                PaymentHistoryCard(
                                    payment = payment,
                                    showStudentName = false,
                                    canDelete = isOwner,
                                    onDeleteClick = { paymentToDelete = payment }
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
            // Header Row: Student Info or Record Type & Amount
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

            // Footer Details: Method chip, Received by & Timestamp
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
        FeeCollectionScreen(onBack = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FeeCollectionScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        FeeCollectionScreen(onBack = {})
    }
}
