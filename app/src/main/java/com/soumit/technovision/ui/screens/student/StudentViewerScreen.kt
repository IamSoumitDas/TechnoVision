package com.soumit.technovision.ui.screens.student

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.soumit.technovision.ui.screens.BillingUtils
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentViewerScreen(studentId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    var student by remember { mutableStateOf<StudentModel?>(null) }
    var weeklySchedule by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var totalPaid by remember { mutableLongStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }
    var calculatedMonthsDue by remember { mutableIntStateOf(0) }
    var expectedTotalFees by remember { mutableLongStateOf(0L) }

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
            weeklySchedule = mapOf("Sunday" to "9:00 AM - 11:00 AM", "Monday" to "Off", "Tuesday" to "5:00 PM - 7:00 PM", "Wednesday" to "Off")
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                                    data = "tel:${student!!.altPhone}".toUri()
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
                                    data = "tel:${student!!.phone}".toUri()
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
            // Filter out empty or "Off" days so only active schedule entries show
            val activeSchedule = weeklySchedule.filter { it.value.isNotBlank() && it.value != "Off" }
            val initials = student!!.fullName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Hero Identity Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = initials.ifEmpty { "ST" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = student!!.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                                Text(text = student!!.phone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (student!!.altPhone.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                                    Text(text = student!!.altPhone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Section 2: Financial & Fee Status Card (Includes Monthly Rate)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (balanceDue > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(text = "Financial & Fee Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Monthly Rate:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "₹${student!!.monthlyInstallment}", fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Admission Date:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = student!!.admissionDate, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Billable Months:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "$calculatedMonthsDue months", fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Total Expected Dues:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "₹$expectedTotalFees", fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Total Amount Paid:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "₹$totalPaid", fontWeight = FontWeight.Medium)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Outstanding Balance:", fontWeight = FontWeight.Bold)
                            Text(
                                text = if (balanceDue > 0) "₹$balanceDue (Pending)" else "₹0 (Paid Up)",
                                fontWeight = FontWeight.Bold,
                                color = if (balanceDue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Section 3: Personal & Family Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "Personal & Family Information", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wc, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                            Text(text = "Gender: ${student!!.gender}")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cake, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                            Text(text = "Date of Birth: ${student!!.dob}")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Man, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                            Text(text = "Father's Name: ${student!!.fatherName}")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Woman, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                            Text(text = "Mother's Name: ${student!!.motherName}")
                        }
                    }
                }

                // Section 4: Academic & School Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "Academic & School Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                            Text(text = "School: ${student!!.school}")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Class, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                            Text(text = "Class: ${student!!.studentClass}")
                        }
                    }
                }

                // Section 5: Weekly Batch Schedule Card (Active days only)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "Weekly Batch Schedule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        if (activeSchedule.isEmpty()) {
                            Text(text = "No active batch schedules assigned.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        } else {
                            activeSchedule.forEach { (day, slot) ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = day, fontWeight = FontWeight.Medium)
                                    Text(text = slot, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                                }
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