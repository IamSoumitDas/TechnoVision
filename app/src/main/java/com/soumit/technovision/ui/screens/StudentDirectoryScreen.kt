package com.soumit.technovision.ui.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
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
import com.google.firebase.firestore.Query
import com.soumit.technovision.ui.screens.student.StudentModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDirectoryScreen(
    onBack: () -> Unit,
    onNavigateToUpdateDelete: (String) -> Unit,
    onNavigateToViewer: (String) -> Unit
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    var studentList by remember { mutableStateOf<List<StudentModel>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Map to hold calculated outstanding balances: studentId -> balanceDue
    val outstandingBalances = remember { mutableStateMapOf<String, Long>() }

    // Rule helper to calculate expected dues
    fun calculateExpectedDues(admissionDateStr: String, installment: Long): Long {
        if (admissionDateStr.isBlank()) return installment
        try {
            val parts = admissionDateStr.split("/")
            if (parts.size == 3) {
                val day = parts[0].toInt()
                val admissionMonth = parts[1].toInt()
                val admissionYear = parts[2].toInt()

                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                val currentMonth = calendar.get(Calendar.MONTH) + 1

                var startMonthIndex = admissionYear * 12 + admissionMonth
                if (day > 20) {
                    startMonthIndex += 1
                }

                val currentMonthIndex = currentYear * 12 + currentMonth
                val monthsElapsed = (currentMonthIndex - startMonthIndex) + 1
                val validMonths = if (monthsElapsed > 0) monthsElapsed else 0
                return validMonths * installment
            }
        } catch (_: Exception) {}
        return installment
    }

    LaunchedEffect(Unit) {
        if (isInPreview) {
            val mockStudents = listOf(
                StudentModel(
                    id = "1",
                    fullName = "Aarav Sharma",
                    studentClass = "X",
                    school = "St. Xavier's School",
                    fatherName = "Rajesh Sharma",
                    motherName = "Sunita Sharma",
                    admissionDate = "15/06/2026",
                    monthlyInstallment = 500L,
                    phone = "9876543210",
                    email = "aarav@example.com",
                    batchTimeSlot = "9:00 AM - 11:00 AM"
                ),
                StudentModel(
                    id = "2",
                    fullName = "Priya Das",
                    studentClass = "XII",
                    school = "Alipurduar Girls High School",
                    fatherName = "Anil Das",
                    motherName = "Mita Das",
                    admissionDate = "10/01/2026",
                    monthlyInstallment = 600L,
                    phone = "9123456789",
                    email = "priya@example.com",
                    batchTimeSlot = "5:00 PM - 7:00 PM"
                )
            )
            studentList = mockStudents
            outstandingBalances["1"] = 500L
            outstandingBalances["2"] = 0L
            isLoading = false
        } else {
            val db = FirebaseFirestore.getInstance()
            db.collection("students")
                .orderBy("fullName", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        isLoading = false
                        Toast.makeText(context, "Error loading students: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val list = snapshot.documents.map { doc ->
                            StudentModel(
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
                                email = doc.getString("email") ?: "",
                                batchTimeSlot = doc.getString("batchTimeSlot") ?: "",
                                weeklySchedule = doc.get("weeklySchedule") as? Map<String, String>
                                    ?: emptyMap()
                            )
                        }
                        studentList = list

                        // Calculate balance for each student via fee_payments ledger
                        list.forEach { student ->
                            val expected = calculateExpectedDues(student.admissionDate, student.monthlyInstallment)
                            db.collection("fee_payments")
                                .whereEqualTo("studentId", student.id)
                                .get()
                                .addOnSuccessListener { paymentSnap ->
                                    var paid = 0L
                                    for (payDoc in paymentSnap.documents) {
                                        paid += payDoc.getLong("amountPaid") ?: 0L
                                    }
                                    val due = expected - paid
                                    outstandingBalances[student.id] = if (due > 0) due else 0L
                                }
                        }

                        isLoading = false
                    }
                }
        }
    }

    val filteredStudents = studentList.filter {
        it.fullName.contains(searchQuery, ignoreCase = true) ||
                it.school.contains(searchQuery, ignoreCase = true) ||
                it.studentClass.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Directory") },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name, school, class, phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredStudents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No students found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredStudents) { student ->
                        val outstandingDue = outstandingBalances[student.id] ?: 0L
                        StudentCard(
                            student = student,
                            outstandingDue = outstandingDue,
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
    onCardClicked: () -> Unit,
    onEditSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClicked() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = student.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onEditSelected, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Student",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Class: ${student.studentClass} | School: ${student.school}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Parents: ${student.fatherName} / ${student.motherName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Text(text = "Phone: ${student.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

            // Replaced monthly installment line with outstanding balance display
            Text(
                text = if (outstandingDue > 0) "Outstanding Balance: ₹$outstandingDue (Pending)"
                else "Outstanding Balance: ₹0 (Fully Paid)",
                style = MaterialTheme.typography.bodySmall,
                //fontWeight = FontWeight.Bold,
                color = if (outstandingDue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
            )

            // Show active schedule slots if available
            if (student.weeklySchedule.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                val activeDays = student.weeklySchedule.filter { it.value != "Off" }
                if (activeDays.isNotEmpty()) {
                    val scheduleSummary = activeDayText(activeDays)
                    Text(
                        text = "Schedule: $scheduleSummary",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Text(
                        text = "Schedule: No active days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else if (student.batchTimeSlot.isNotBlank()) {
                Text(
                    text = "Preferred Slot: ${student.batchTimeSlot}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun activeDayText(activeDays: Map<String, String>): String {
    return activeDays.entries.joinToString(", ") { "${it.key.take(3)} (${it.value})" }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun StudentDirectoryScreenPreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        StudentDirectoryScreen(onBack = {}, onNavigateToUpdateDelete = {}, onNavigateToViewer = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun StudentDirectoryScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        StudentDirectoryScreen(onBack = {}, onNavigateToViewer = {}, onNavigateToUpdateDelete = {})
    }
}