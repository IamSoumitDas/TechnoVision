package com.soumit.technovision.ui.screens.fees

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soumit.technovision.data.model.FeePaymentModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import com.soumit.technovision.util.SystemLogger
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GlobalHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    val mockStudents = remember {
        mapOf(
            "s1" to StudentModel(id = "s1", fullName = "Aarav Sharma", studentClass = "X", school = "Delhi Public School"),
            "s2" to StudentModel(id = "s2", fullName = "Priya Das", studentClass = "IX", school = "St. Xavier's High School"),
            "s3" to StudentModel(id = "s3", fullName = "Rohan Gupta", studentClass = "XII", school = "Kendriya Vidyalaya")
        )
    }

    val mockPayments = remember {
        val now = Timestamp.now()
        listOf(
            FeePaymentModel(
                id = "1",
                studentId = "s1",
                studentName = "Aarav Sharma",
                amountPaid = 1200L,
                paymentMethod = "UPI",
                remarks = "April Tuition Fee",
                receivedBy = "Soumit (Owner)",
                timestamp = now
            ),
            FeePaymentModel(
                id = "2",
                studentId = "s2",
                studentName = "Priya Das",
                amountPaid = 800L,
                paymentMethod = "Cash",
                remarks = "Late fee included",
                receivedBy = "Amitabha Teacher",
                timestamp = now
            ),
            FeePaymentModel(
                id = "3",
                studentId = "s3",
                studentName = "Rohan Gupta",
                amountPaid = 1500L,
                paymentMethod = "UPI",
                remarks = "Quarterly Payment",
                receivedBy = "Soumit (Owner)",
                timestamp = Timestamp(Date(System.currentTimeMillis() - 86400000L * 2))
            )
        )
    }

    var paymentHistory by remember { mutableStateOf<List<FeePaymentModel>>(if (isInPreview) mockPayments else emptyList()) }
    var studentsMap by remember { mutableStateOf<Map<String, StudentModel>>(if (isInPreview) mockStudents else emptyMap()) }
    var isLoading by remember { mutableStateOf(!isInPreview) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMethodFilter by remember { mutableStateOf("All") }
    val paymentMethods = listOf("All", "UPI", "Cash", "Bank Transfer", "Cheque")

    // Role check for payment deletion (owner only)
    var currentUserRole by remember { mutableStateOf(if (isInPreview) "owner" else "") }
    var paymentToDelete by remember { mutableStateOf<FeePaymentModel?>(null) }
    var isDeletingPayment by remember { mutableStateOf(false) }

    val todayDateStr = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
    }

    val currentMonthName = remember {
        SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
    }

    DisposableEffect(Unit) {
        if (isInPreview) {
            onDispose { }
        } else {
            val db = FirebaseFirestore.getInstance()
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            // Fetch current user role to verify owner permissions
            if (currentUserId != null) {
                db.collection("users").document(currentUserId).get()
                    .addOnSuccessListener { doc ->
                        currentUserRole = doc.getString("role")?.lowercase(Locale.ROOT) ?: ""
                    }
            }

            // Load all students for fallback student names and class subtitles
            db.collection("students").get()
                .addOnSuccessListener { snapshot ->
                    val map = snapshot.documents.associate { doc ->
                        doc.id to StudentModel(
                            id = doc.id,
                            fullName = doc.getString("fullName") ?: "",
                            studentClass = doc.getString("studentClass") ?: "",
                            school = doc.getString("school") ?: "",
                            monthlyInstallment = doc.getLong("monthlyInstallment") ?: 500L,
                            phone = doc.getString("phone") ?: ""
                        )
                    }
                    studentsMap = map
                }

            // Real-time listener for all fee payments with cleanup on dispose
            val registration = db.collection("fee_payments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        isLoading = false
                        Toast.makeText(context, "Error loading history: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val history = snapshot.documents.map { doc ->
                            FeePaymentModel(
                                id = doc.id,
                                studentId = doc.getString("studentId") ?: "",
                                studentName = doc.getString("studentName") ?: "",
                                amountPaid = doc.getLong("amountPaid") ?: 0L,
                                paymentMethod = doc.getString("paymentMethod") ?: "",
                                remarks = doc.getString("remarks") ?: "",
                                receivedBy = doc.getString("receivedBy") ?: "",
                                timestamp = doc.getTimestamp("timestamp")
                            )
                        }
                        paymentHistory = history
                        isLoading = false
                    }
                }

            onDispose {
                registration.remove()
            }
        }
    }

    // Filter payments based on search text and payment method chip
    val filteredPayments = remember(paymentHistory, searchQuery, selectedMethodFilter, studentsMap) {
        val query = searchQuery.trim().lowercase(Locale.ROOT)
        paymentHistory.filter { payment ->
            val resolvedStudentName = payment.studentName.ifBlank {
                studentsMap[payment.studentId]?.fullName ?: ""
            }.lowercase(Locale.ROOT)

            val matchesQuery = query.isEmpty() ||
                    resolvedStudentName.contains(query) ||
                    payment.receivedBy.lowercase(Locale.ROOT).contains(query) ||
                    payment.remarks.lowercase(Locale.ROOT).contains(query) ||
                    payment.amountPaid.toString().contains(query)

            val matchesMethod = selectedMethodFilter == "All" ||
                    payment.paymentMethod.equals(selectedMethodFilter, ignoreCase = true)

            matchesQuery && matchesMethod
        }
    }

    // Metrics calculations (Current Month's Total & Today's Total)
    val thisMonthCollected = remember(paymentHistory) {
        val calNow = Calendar.getInstance()
        val currentYear = calNow.get(Calendar.YEAR)
        val currentMonth = calNow.get(Calendar.MONTH)
        val calPayment = Calendar.getInstance()
        paymentHistory.filter { payment ->
            if (payment.timestamp != null) {
                calPayment.time = payment.timestamp.toDate()
                calPayment.get(Calendar.YEAR) == currentYear && calPayment.get(Calendar.MONTH) == currentMonth
            } else false
        }.sumOf { it.amountPaid }
    }

    val todayCollected = remember(paymentHistory, todayDateStr) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        paymentHistory.filter { payment ->
            payment.timestamp != null && dateFormat.format(payment.timestamp.toDate()) == todayDateStr
        }.sumOf { it.amountPaid }
    }

    // Group filtered payments by Date string (e.g. "02 Nov 2026")
    val groupedHistory = remember(filteredPayments) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        filteredPayments.groupBy { payment ->
            if (payment.timestamp != null) {
                dateFormat.format(payment.timestamp.toDate())
            } else {
                "Unknown Date"
            }
        }
    }

    val isOwner = currentUserRole == "owner"

    // Delete Payment Confirmation Dialog (Owner Only)
    if (paymentToDelete != null) {
        val target = paymentToDelete!!
        val paymentFormattedDate = remember(target.timestamp) {
            if (target.timestamp != null) {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(target.timestamp.toDate())
            } else "Recent"
        }
        val targetStudentName = target.studentName.ifBlank {
            studentsMap[target.studentId]?.fullName ?: "Student"
        }

        AlertDialog(
            onDismissRequest = { if (!isDeletingPayment) paymentToDelete = null },
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
                    onClick = {
                        if (isInPreview) {
                            paymentHistory = paymentHistory.filter { it.id != target.id }
                            paymentToDelete = null
                            Toast.makeText(context, "Preview: Payment deleted successfully!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isDeletingPayment = true
                        val db = FirebaseFirestore.getInstance()

                        db.collection("fee_payments").document(target.id).delete()
                            .addOnSuccessListener {
                                isDeletingPayment = false
                                Toast.makeText(context, "Payment record deleted.", Toast.LENGTH_SHORT).show()

                                SystemLogger.log(
                                    action = "Fee Payment Deleted",
                                    details = "Deleted payment of ₹${target.amountPaid} (${target.paymentMethod}) for student: $targetStudentName"
                                )

                                paymentHistory = paymentHistory.filter { it.id != target.id }
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
                        Text("Delete Payment")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { paymentToDelete = null },
                    enabled = !isDeletingPayment
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
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by student, staff, remarks...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
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
                        selected = selectedMethodFilter == method,
                        onClick = { selectedMethodFilter = method },
                        label = { Text(method, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Main Content Area
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else if (filteredPayments.isEmpty()) {
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
                            text = if (searchQuery.isNotBlank() || selectedMethodFilter != "All") {
                                "No payment records match your filters."
                            } else {
                                "No payments recorded yet."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (searchQuery.isNotBlank() || selectedMethodFilter != "All") {
                            TextButton(onClick = {
                                searchQuery = ""
                                selectedMethodFilter = "All"
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
                            // Date Section Header with daily summary
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
                            val student = studentsMap[payment.studentId]
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
                                onDeleteClick = { paymentToDelete = updatedPayment }
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
        GlobalHistoryScreen(onBack = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun GlobalHistoryScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        GlobalHistoryScreen(onBack = {})
    }
}
