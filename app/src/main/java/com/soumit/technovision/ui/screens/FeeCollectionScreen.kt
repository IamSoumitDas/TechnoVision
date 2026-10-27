package com.soumit.technovision.ui.screens

import android.app.DatePickerDialog
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soumit.technovision.ui.screens.student.StudentModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import java.text.SimpleDateFormat
import java.util.*

data class FeePaymentModel(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val amountPaid: Long = 0L,
    val paymentMethod: String = "",
    val remarks: String = "",
    val receivedBy: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeCollectionScreen(
    onBack: () -> Unit,
    onNavigateToGlobalHistory: () -> Unit
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    var studentList by remember { mutableStateOf<List<StudentModel>>(emptyList()) }
    var selectedStudent by remember { mutableStateOf<StudentModel?>(null) }
    var studentDropdownExpanded by remember { mutableStateOf(false) }

    // Financial states for selected student
    var expectedTotalFees by remember { mutableStateOf(0L) }
    var totalPaid by remember { mutableStateOf(0L) }

    // Payment Form States
    var amountPaid by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("Cash") }
    var methodExpanded by remember { mutableStateOf(false) }
    val paymentMethods = listOf("Cash", "UPI", "Bank Transfer", "Other")
    var remarks by remember { mutableStateOf("") }

    var paymentHistory by remember { mutableStateOf<List<FeePaymentModel>>(emptyList()) }
    var isLoadingStudents by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Fetch students on load
    LaunchedEffect(Unit) {
        if (isInPreview) {
            val mockStudents = listOf(
                StudentModel(
                    id = "1",
                    fullName = "Aarav Sharma",
                    studentClass = "X",
                    monthlyInstallment = 500L,
                    admissionDate = "15/06/2026"
                ),
                StudentModel(
                    id = "2",
                    fullName = "Priya Das",
                    studentClass = "XII",
                    monthlyInstallment = 600L,
                    admissionDate = "10/06/2026"
                )
            )
            studentList = mockStudents
            isLoadingStudents = false
        } else {
            FirebaseFirestore.getInstance().collection("students")
                .orderBy("fullName", Query.Direction.ASCENDING)
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

    // Fetch payment history and calculate financial dues when selected student changes
    LaunchedEffect(selectedStudent) {
        if (!isInPreview && selectedStudent != null) {
            val s = selectedStudent!!
            val (_, expected) = BillingUtils.calculateBillingMonths(s.admissionDate, s.monthlyInstallment)
            expectedTotalFees = expected

            val db = FirebaseFirestore.getInstance()

            // Fetch payment history
            db.collection("fee_payments")
                .whereEqualTo("studentId", s.id)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
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
                        }
                        paymentHistory = history
                        totalPaid = sum
                    }
                }
        } else if (isInPreview && selectedStudent != null) {
            val (_, expected) = BillingUtils.calculateBillingMonths(selectedStudent!!.admissionDate, selectedStudent!!.monthlyInstallment)
            expectedTotalFees = expected
            totalPaid = 1000L
            paymentHistory = emptyList()
        } else {
            paymentHistory = emptyList()
            expectedTotalFees = 0L
            totalPaid = 0L
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fee Collection & Ledger") },
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
        if (isLoadingStudents) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Receive Monthly Installment",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // 1. Student Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = studentDropdownExpanded,
                    onExpandedChange = { studentDropdownExpanded = !studentDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedStudent?.fullName ?: "NONE",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Student") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = studentDropdownExpanded,
                        onDismissRequest = { studentDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("NONE") },
                            onClick = {
                                selectedStudent = null
                                amountPaid = ""
                                studentDropdownExpanded = false
                            }
                        )
                        studentList.forEach { student ->
                            DropdownMenuItem(
                                text = { Text("${student.fullName} (Class ${student.studentClass})") },
                                onClick = {
                                    selectedStudent = student
                                    amountPaid = student.monthlyInstallment.toString()
                                    studentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedStudent == null) {
                    /*Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Select a student above to record a payment, or click below to view all recent payment transactions.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = onNavigateToGlobalHistory,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                Text("View All Students Payment History")
                            }
                        }
                    }*/
                } else {
                    val balanceDue = expectedTotalFees - totalPaid

                    // Student Quick Financial Info Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (balanceDue > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Standard Monthly Due: ₹${selectedStudent!!.monthlyInstallment}", fontWeight = FontWeight.Bold)
                            Text(text = "Total Expected Dues: ₹$expectedTotalFees | Paid: ₹$totalPaid")
                            Text(
                                text = if (balanceDue > 0) "Outstanding Balance: ₹$balanceDue (Pending)" else "Balance: Fully Paid Up",
                                fontWeight = FontWeight.Bold,
                                color = if (balanceDue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // 2. Amount Paid
                    OutlinedTextField(
                        value = amountPaid,
                        onValueChange = { amountPaid = it },
                        label = { Text("Amount Received (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 3. Payment Method Dropdown
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
                            modifier = Modifier.fillMaxWidth().menuAnchor()
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

                    // 4. Remarks / Notes
                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Remarks / Receipt Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Save Payment Button
                    if (isSaving) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
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
                                    amountPaid = selectedStudent!!.monthlyInstallment.toString()
                                    remarks = ""
                                    return@Button
                                }

                                isSaving = true
                                val db = FirebaseFirestore.getInstance()
                                val currentUser = FirebaseAuth.getInstance().currentUser

                                db.collection("users").document(currentUser?.uid ?: "").get()
                                    .addOnSuccessListener { userDoc ->
                                        val staffName = if (userDoc.exists()) userDoc.getString("fullName") ?: "Staff" else "Owner"

                                        val paymentData = hashMapOf(
                                            "studentId" to selectedStudent!!.id,
                                            "studentName" to selectedStudent!!.fullName,
                                            "amountPaid" to parsedAmount,
                                            "paymentMethod" to selectedMethod,
                                            "remarks" to remarks.trim(),
                                            "receivedBy" to staffName,
                                            "timestamp" to com.google.firebase.Timestamp.now()
                                        )

                                        db.collection("fee_payments").add(paymentData)
                                            .addOnSuccessListener {
                                                isSaving = false
                                                Toast.makeText(context, "Payment recorded successfully!", Toast.LENGTH_SHORT).show()

                                                // Log the fee collection action
                                                SystemLogger.log(
                                                    action = "Fee Collected",
                                                    details = "Received ₹$parsedAmount via $selectedMethod for student: ${selectedStudent!!.fullName}"
                                                )

                                                remarks = ""
                                            }
                                            .addOnFailureListener { e ->
                                                isSaving = false
                                                Toast.makeText(context, "Failed to record payment: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Record & Print Payment")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Payment History Ledger Section for Selected Student
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
                            paymentHistory.forEach { payment ->
                                PaymentHistoryCard(payment = payment)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentHistoryCard(payment: FeePaymentModel) {
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
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (payment.studentName.isNotBlank()) "${payment.studentName} - ₹${payment.amountPaid}" else "₹${payment.amountPaid}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = payment.paymentMethod,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "Received by: ${payment.receivedBy} on $formattedDate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            if (payment.remarks.isNotBlank()) {
                Text(text = "Notes: ${payment.remarks}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun FeeCollectionScreenPreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        FeeCollectionScreen(onBack = {}, onNavigateToGlobalHistory = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FeeCollectionScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        FeeCollectionScreen(onBack = {}, onNavigateToGlobalHistory = {})
    }
}