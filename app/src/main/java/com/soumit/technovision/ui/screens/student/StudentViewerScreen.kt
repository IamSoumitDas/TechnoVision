package com.soumit.technovision.ui.screens.student

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.soumit.technovision.ui.screens.BillingUtils
import com.soumit.technovision.ui.theme.TechnoVisionTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentViewerScreen(studentId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    var student by remember { mutableStateOf<StudentModel?>(null) }
    var weeklySchedule by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var totalPaid by remember { mutableStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }

    // Financial Calculation States
    var calculatedMonthsDue by remember { mutableStateOf(0) }
    var expectedTotalFees by remember { mutableStateOf(0L) }

    LaunchedEffect(studentId) {
        if (isInPreview || studentId == "preview_id") {
            val mockStudent = StudentModel(
                id = "preview_id",
                fullName = "Aarav Sharma",
                studentClass = "X",
                school = "St. Xavier's School",
                dob = "12/05/2010",
                gender = "Male",
                admissionDate = "15/06/2026",
                fatherName = "Rajesh Sharma",
                motherName = "Sunita Sharma",
                monthlyInstallment = 500L,
                phone = "9876543210",
                altPhone = "9123456789",
                email = "aarav@example.com"
            )
            student = mockStudent
            weeklySchedule = mapOf("Sunday" to "9:00 AM - 11:00 AM", "Tuesday" to "5:00 PM - 7:00 PM")
            totalPaid = 1500L

            val (months, expected) = BillingUtils.calculateBillingMonths(mockStudent.admissionDate, mockStudent.monthlyInstallment)
            calculatedMonthsDue = months
            expectedTotalFees = expected
            isLoading = false
        } else {
            val db = FirebaseFirestore.getInstance()

            db.collection("students").document(studentId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val s = StudentModel(
                            id = doc.id,
                            fullName = doc.getString("fullName") ?: "",
                            studentClass = doc.getString("studentClass") ?: "",
                            school = doc.getString("school") ?: "",
                            dob = doc.getString("dob") ?: "",
                            gender = doc.getString("gender") ?: "",
                            admissionDate = doc.getString("admissionDate") ?: "",
                            fatherName = doc.getString("fatherName") ?: "",
                            motherName = doc.getString("motherName") ?: "",
                            monthlyInstallment = doc.getLong("monthlyInstallment") ?: 500L,
                            phone = doc.getString("phone") ?: "",
                            altPhone = doc.getString("altPhone") ?: "",
                            //email = doc.getString("email") ?: "",
                            batchTimeSlot = doc.getString("batchTimeSlot") ?: ""
                        )
                        student = s
                        @Suppress("UNCHECKED_CAST")
                        weeklySchedule = doc.get("weeklySchedule") as? Map<String, String> ?: emptyMap()

                        val (months, expected) = BillingUtils.calculateBillingMonths(s.admissionDate, s.monthlyInstallment)
                        calculatedMonthsDue = months
                        expectedTotalFees = expected

                        db.collection("fee_payments")
                            .whereEqualTo("studentId", studentId)
                            .get()
                            .addOnSuccessListener { paymentSnapshot ->
                                var sum = 0L
                                for (paymentDoc in paymentSnapshot.documents) {
                                    sum += paymentDoc.getLong("amountPaid") ?: 0L
                                }
                                totalPaid = sum
                                isLoading = false
                            }
                            .addOnFailureListener {
                                isLoading = false
                            }
                    } else {
                        isLoading = false
                    }
                }
                .addOnFailureListener {
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Information & Ledger") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (!isLoading && student != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (student!!.altPhone.isNotBlank()) {
                        SmallFloatingActionButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${student!!.altPhone}")
                                }
                                context.startActivity(intent)
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Call Alt Phone")
                        }
                    }

                    if (student!!.phone.isNotBlank()) {
                        FloatingActionButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${student!!.phone}")
                                }
                                context.startActivity(intent)
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Call Primary Phone")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (student == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Student record not found.")
            }
        } else {
            val balanceDue = expectedTotalFees - totalPaid

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = student!!.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        //Text(text = "Email ID: ${student!!.email}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Phone: ${student!!.phone}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (student!!.altPhone.isNotBlank()) {
                            Text(text = "Alt Phone: ${student!!.altPhone}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (balanceDue > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Financial & Fee Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = "Admission Date: ${student!!.admissionDate}")
                        Text(text = "Billable Months Accrued: $calculatedMonthsDue months")
                        Text(text = "Total Expected Dues: ₹$expectedTotalFees")
                        Text(text = "Total Amount Paid: ₹$totalPaid")

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = if (balanceDue > 0) "Outstanding Balance: ₹$balanceDue (Pending)"
                            else "Outstanding Balance: ₹0 (Fully Paid Up)",
                            fontWeight = FontWeight.Bold,
                            color = if (balanceDue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Personal & Academic Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = "Class: ${student!!.studentClass}")
                        Text(text = "School: ${student!!.school}")
                        Text(text = "Gender: ${student!!.gender}")
                        Text(text = "Date of Birth: ${student!!.dob}")
                        Text(text = "Father's Name: ${student!!.fatherName}")
                        Text(text = "Mother's Name: ${student!!.motherName}")
                        Text(text = "Monthly Installment Rate: ₹${student!!.monthlyInstallment}")
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Weekly Batch Schedule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        weeklySchedule.forEach { (day, slot) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = day, fontWeight = FontWeight.Medium)
                                Text(text = slot, color = if (slot == "Off") MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun StudentViewerScreenPreview() {
    TechnoVisionTheme {
        StudentViewerScreen(studentId = "preview_id", onBack = {})
    }
}