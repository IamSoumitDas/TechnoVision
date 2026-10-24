package com.soumit.technovision.ui.screens

import android.app.DatePickerDialog
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDeleteStudentScreen(studentId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    // Form Fields State
    var fullName by remember { mutableStateOf("") }

    var selectedClass by remember { mutableStateOf("") }
    var classExpanded by remember { mutableStateOf(false) }
    val classOptions = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII")

    var selectedSchool by remember { mutableStateOf("") }
    var schoolExpanded by remember { mutableStateOf(false) }
    val schoolOptions = listOf(
        "Mc William High School (HS)",
        "Balika Siksha Mandir (HS)",
        "Alipurduar Girls High School",
        "Gobinda High School (HS)",
        "Newtown Girls High School",
        "Railway Higher Secondary School",
        "Kendriya Vidyalaya",
        "St. Xavier's School",
        "Stepping Stone Model School",
        "Little Flowers English School",
        "Techno India Group Public School",
        "Sukanta High School"
    )

    var selectedGender by remember { mutableStateOf("") }
    var genderExpanded by remember { mutableStateOf(false) }
    val genderOptions = listOf("Male", "Female", "Other")

    var dob by remember { mutableStateOf("") }
    var admissionDate by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var motherName by remember { mutableStateOf("") }
    var monthlyInstallment by remember { mutableStateOf("500") }
    var phone by remember { mutableStateOf("") }
    var altPhone by remember { mutableStateOf("") }

    val daysOfWeek = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val timeSlotOptions = listOf("Off", "9:00 AM - 11:00 AM", "11:00 AM - 1:00 PM", "1:00 PM - 3:00 PM", "3:00 PM - 5:00 PM", "5:00 PM - 7:00 PM", "7:00 PM - 9:00 PM")

    val daySchedules = remember {
        mutableStateMapOf<String, String>().apply {
            daysOfWeek.forEach { put(it, "Off") }
        }
    }
    var expandedDayMenu by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // Fetch existing student data from Firestore on load
    LaunchedEffect(studentId) {
        if (isInPreview) {
            fullName = "Aarav Sharma"
            selectedClass = "X"
            selectedSchool = "St. Xavier's School"
            fatherName = "Rajesh Sharma"
            motherName = "Sunita Sharma"
            monthlyInstallment = "500"
            phone = "9876543210"
            isLoading = false
        } else {
            FirebaseFirestore.getInstance().collection("students").document(studentId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        fullName = doc.getString("fullName") ?: ""
                        selectedClass = doc.getString("studentClass") ?: ""
                        selectedSchool = doc.getString("school") ?: ""
                        dob = doc.getString("dob") ?: ""
                        selectedGender = doc.getString("gender") ?: ""
                        admissionDate = doc.getString("admissionDate") ?: ""
                        fatherName = doc.getString("fatherName") ?: ""
                        motherName = doc.getString("motherName") ?: ""
                        monthlyInstallment = (doc.getLong("monthlyInstallment") ?: 500L).toString()
                        phone = doc.getString("phone") ?: ""
                        altPhone = doc.getString("altPhone") ?: ""

                        @Suppress("UNCHECKED_CAST")
                        val scheduleMap = doc.get("weeklySchedule") as? Map<String, String>
                        if (scheduleMap != null) {
                            scheduleMap.forEach { (day, slot) ->
                                daySchedules[day] = slot
                            }
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    Toast.makeText(context, "Failed to load profile: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            val formattedDate = String.format("%02d/%02d/%d", day, month + 1, year)
            onDateSelected(formattedDate)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update / Delete Student") },
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
        if (isLoading) {
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Edit Student Profile Record",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Student Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = classExpanded,
                        onExpandedChange = { classExpanded = !classExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedClass,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Class") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = classExpanded, onDismissRequest = { classExpanded = false }) {
                            classOptions.forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = { selectedClass = option; classExpanded = false })
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = genderExpanded,
                        onExpandedChange = { genderExpanded = !genderExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedGender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                            genderOptions.forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = { selectedGender = option; genderExpanded = false })
                            }
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = schoolExpanded,
                    onExpandedChange = { schoolExpanded = !schoolExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedSchool,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("School Name") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = schoolExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = schoolExpanded, onDismissRequest = { schoolExpanded = false }) {
                        schoolOptions.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { selectedSchool = option; schoolExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = dob,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date of Birth (DD/MM/YYYY)") },
                    trailingIcon = { IconButton(onClick = { if (!isInPreview) showDatePicker { dob = it } }) { Icon(Icons.Default.DateRange, contentDescription = null) } },
                    modifier = Modifier.fillMaxWidth().clickable { if (!isInPreview) showDatePicker { dob = it } },
                    enabled = false
                )

                OutlinedTextField(
                    value = admissionDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Admission Date (DD/MM/YYYY)") },
                    trailingIcon = { IconButton(onClick = { if (!isInPreview) showDatePicker { admissionDate = it } }) { Icon(Icons.Default.DateRange, contentDescription = null) } },
                    modifier = Modifier.fillMaxWidth().clickable { if (!isInPreview) showDatePicker { admissionDate = it } },
                    enabled = false
                )

                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    label = { Text("Father's Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = motherName,
                    onValueChange = { motherName = it },
                    label = { Text("Mother's Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Day-by-Day Batch Timings",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Start)
                )

                daysOfWeek.forEach { day ->
                    val currentSlot = daySchedules[day] ?: "Off"
                    val isMenuExpanded = (expandedDayMenu == day)

                    ExposedDropdownMenuBox(
                        expanded = isMenuExpanded,
                        onExpandedChange = { expandedDayMenu = if (isMenuExpanded) null else day },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentSlot,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(day) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMenuExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = isMenuExpanded, onDismissRequest = { expandedDayMenu = null }) {
                            timeSlotOptions.forEach { slot ->
                                DropdownMenuItem(text = { Text(slot) }, onClick = { daySchedules[day] = slot; expandedDayMenu = null })
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                OutlinedTextField(
                    value = monthlyInstallment,
                    onValueChange = { monthlyInstallment = it },
                    label = { Text("Monthly Installment (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = altPhone,
                    onValueChange = { altPhone = it },
                    label = { Text("Alternative Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (fullName.isBlank()) {
                                statusMessage = "Full name cannot be blank."
                                isError = true
                                return@Button
                            }

                            if (isInPreview) {
                                Toast.makeText(context, "Preview: Information Updated!", Toast.LENGTH_SHORT).show()
                                onBack()
                                return@Button
                            }

                            val db = FirebaseFirestore.getInstance()
                            val updatedData = mapOf(
                                "fullName" to fullName.trim(),
                                "studentClass" to selectedClass,
                                "school" to selectedSchool,
                                "dob" to dob,
                                "gender" to selectedGender,
                                "admissionDate" to admissionDate,
                                "fatherName" to fatherName.trim(),
                                "motherName" to motherName.trim(),
                                "monthlyInstallment" to (monthlyInstallment.toIntOrNull() ?: 500),
                                "weeklySchedule" to daySchedules.toMap(),
                                "phone" to phone.trim(),
                                "altPhone" to altPhone.trim()
                            )

                            db.collection("students").document(studentId).update(updatedData)
                                .addOnSuccessListener {
                                    db.collection("users").document(studentId).update("fullName", fullName.trim())
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Student updated successfully!", Toast.LENGTH_SHORT).show()

                                            // Log student profile update
                                            SystemLogger.log(
                                                action = "Student Profile Updated",
                                                details = "Updated profile record for student: ${fullName.trim()} (ID: $studentId)"
                                            )

                                            onBack()
                                        }
                                }
                                .addOnFailureListener { e ->
                                    statusMessage = "Update failed: ${e.localizedMessage}"
                                    isError = true
                                }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Update Information")
                    }

                    Button(
                        onClick = {
                            if (isInPreview) {
                                Toast.makeText(context, "Preview: Student Deleted!", Toast.LENGTH_SHORT).show()
                                onBack()
                                return@Button
                            }

                            val db = FirebaseFirestore.getInstance()
                            db.collection("students").document(studentId).delete()
                                .addOnSuccessListener {
                                    db.collection("users").document(studentId).delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Student deleted successfully.", Toast.LENGTH_SHORT).show()

                                            // Log student deletion
                                            SystemLogger.log(
                                                action = "Student Deleted",
                                                details = "Deleted student record (ID: $studentId)"
                                            )

                                            onBack()
                                        }
                                }
                                .addOnFailureListener { e ->
                                    statusMessage = "Delete failed: ${e.localizedMessage}"
                                    isError = true
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Delete Student")
                    }
                }

                if (statusMessage != null) {
                    Text(
                        text = statusMessage!!,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun UpdateDeleteScreenPreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        UpdateDeleteStudentScreen(studentId = "dummy_id", onBack = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun UpdateDeleteScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        UpdateDeleteStudentScreen(studentId = "dummy_id", onBack = {})
    }
}