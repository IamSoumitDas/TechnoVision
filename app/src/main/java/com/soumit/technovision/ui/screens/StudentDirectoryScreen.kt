@file:Suppress("UNCHECKED_CAST")

package com.soumit.technovision.ui.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soumit.technovision.ui.screens.student.StudentModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme

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
    var userRole by remember { mutableStateOf("owner") }
    val outstandingBalances = remember { mutableStateMapOf<String, Long>() }

    LaunchedEffect(Unit) {
        if (isInPreview) {
            userRole = "owner"
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
            val authUser = FirebaseAuth.getInstance().currentUser
            val userId = authUser?.uid

            val db = FirebaseFirestore.getInstance()

            if (userId != null) {
                db.collection("users").document(userId).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            userRole = doc.getString("role") ?: "owner"
                        }
                    }
            }

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
                                weeklySchedule = doc.get("weeklySchedule") as? Map<String, String> ?: emptyMap()
                            )
                        }
                        studentList = list
                        list.forEach { student ->
                            val expected = BillingUtils.calculateBillingMonths(
                                student.admissionDate,
                                student.monthlyInstallment
                            ).second

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
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name, school, class, phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
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
                            userRole = userRole,
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
    val initials = student.fullName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()

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

                if (userRole == "owner" || userRole == "receptionist") {
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