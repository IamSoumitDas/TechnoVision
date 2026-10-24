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
fun EnrollStudentScreen(
    onBack: () -> Unit,
    onStudentEnrolled: (String) -> Unit
) {
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

    // Day-by-day batch schedule states
    val daysOfWeek = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val timeSlotOptions = listOf("Off", "9:00 AM - 11:00 AM", "11:00 AM - 1:00 PM", "1:00 PM - 3:00 PM", "3:00 PM - 5:00 PM", "5:00 PM - 7:00 PM", "7:00 PM - 9:00 PM")

    val daySchedules = remember {
        mutableStateMapOf<String, String>().apply {
            daysOfWeek.forEach { put(it, "Off") }
        }
    }
    var expandedDayMenu by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var attemptedSubmit by remember { mutableStateOf(false) }

    // Reusable DatePicker function
    fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            val formattedDate = String.format("%02d/%02d/%d", day, month + 1, year)
            onDateSelected(formattedDate)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun isFieldInvalid(value: String): Boolean {
        return attemptedSubmit && value.isBlank()
    }

    val activeBatchDaysCount = daySchedules.values.count { it != "Off" }
    val isScheduleInvalid = attemptedSubmit && activeBatchDaysCount < 2

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enroll New Student") },
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
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Complete Student Profile",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Student Full Name
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    statusMessage = null
                },
                label = { Text("Student Full Name *") },
                singleLine = true,
                isError = isFieldInvalid(fullName),
                modifier = Modifier.fillMaxWidth()
            )

            // Class & Gender Dropdown Row
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
                        label = { Text("Class *") },
                        isError = isFieldInvalid(selectedClass),
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
                        label = { Text("Gender *") },
                        isError = isFieldInvalid(selectedGender),
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

            // School Name Dropdown
            ExposedDropdownMenuBox(
                expanded = schoolExpanded,
                onExpandedChange = { schoolExpanded = !schoolExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedSchool,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("School Name *") },
                    isError = isFieldInvalid(selectedSchool),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = schoolExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = schoolExpanded, onDismissRequest = { schoolExpanded = false }) {
                    schoolOptions.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { selectedSchool = option; schoolExpanded = false })
                    }
                }
            }

            // DOB
            OutlinedTextField(
                value = dob,
                onValueChange = {},
                readOnly = true,
                label = { Text("Date of Birth (DD/MM/YYYY) *") },
                isError = isFieldInvalid(dob),
                trailingIcon = {
                    IconButton(onClick = {
                        if (!isInPreview) showDatePicker {
                            dob = it
                        }
                    }) { Icon(Icons.Default.DateRange, contentDescription = null) }
                },
                modifier = Modifier.fillMaxWidth().clickable {
                    if (!isInPreview) showDatePicker {
                        dob = it
                    }
                },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = if (isFieldInvalid(dob)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            // Admission Date
            OutlinedTextField(
                value = admissionDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Admission Date (DD/MM/YYYY) *") },
                isError = isFieldInvalid(admissionDate),
                trailingIcon = {
                    IconButton(onClick = {
                        if (!isInPreview) showDatePicker { admissionDate = it }
                    }) { Icon(Icons.Default.DateRange, contentDescription = null) }
                },
                modifier = Modifier.fillMaxWidth().clickable {
                    if (!isInPreview) showDatePicker { admissionDate = it }
                },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = if (isFieldInvalid(admissionDate)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            // Parents' Names
            OutlinedTextField(
                value = fatherName,
                onValueChange = { fatherName = it },
                label = { Text("Father's Name *") },
                singleLine = true,
                isError = isFieldInvalid(fatherName),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = motherName,
                onValueChange = { motherName = it },
                label = { Text("Mother's Name *") },
                singleLine = true,
                isError = isFieldInvalid(motherName),
                modifier = Modifier.fillMaxWidth()
            )

            // --- BATCH SCHEDULE SECTION ---
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "Day-by-Day Batch Timings (Min 2 days required) *",
                style = MaterialTheme.typography.titleSmall,
                color = if (isScheduleInvalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
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
                        isError = isScheduleInvalid,
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
                label = { Text("Monthly Installment (₹) *") },
                singleLine = true,
                isError = isFieldInvalid(monthlyInstallment),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number *") },
                singleLine = true,
                isError = isFieldInvalid(phone),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = altPhone,
                onValueChange = { altPhone = it },
                label = { Text("Alternative Phone Number (Optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        attemptedSubmit = true

                        if (fullName.isBlank() || selectedClass.isBlank() || selectedSchool.isBlank() ||
                            dob.isBlank() || admissionDate.isBlank() || fatherName.isBlank() ||
                            motherName.isBlank() || monthlyInstallment.isBlank() || phone.isBlank()
                        ) {
                            statusMessage = "Please fill in all required (*) fields highlighted in red."
                            isError = true
                            return@Button
                        }

                        if (activeBatchDaysCount < 2) {
                            statusMessage = "Student must have at least 2 batch days scheduled."
                            isError = true
                            return@Button
                        }

                        if (isInPreview) {
                            statusMessage = "Preview: Student successfully enrolled!"
                            isError = false
                            onStudentEnrolled("preview_id")
                            return@Button
                        }

                        isLoading = true
                        statusMessage = null

                        val cleanPhone = phone.replace(Regex("\\s+"), "")
                        val db = FirebaseFirestore.getInstance()

                        val newUserId = db.collection("users").document().id

                        val studentData = hashMapOf(
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
                            "role" to "student",
                            "phone" to cleanPhone,
                            "altPhone" to altPhone.trim(),
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )

                        db.collection("users").document(newUserId).set(studentData)
                            .addOnSuccessListener {
                                db.collection("students").document(newUserId).set(studentData)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        isError = false

                                        // Log the action using the correct variable names
                                        SystemLogger.log(
                                            action = "Student Enrolled",
                                            details = "New student registered: ${fullName.trim()} (ID: $newUserId)"
                                        )

                                        onStudentEnrolled(newUserId)
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        isError = true
                                        statusMessage = "Error saving student profile: ${e.localizedMessage}"
                                    }
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                isError = true
                                statusMessage = "Error creating user record: ${e.localizedMessage}"
                            }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enroll Student")
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

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun EnrollStudentScreenPreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        EnrollStudentScreen(onBack = {}, onStudentEnrolled = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EnrollStudentScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        EnrollStudentScreen(onBack = {}, onStudentEnrolled = {})
    }
}