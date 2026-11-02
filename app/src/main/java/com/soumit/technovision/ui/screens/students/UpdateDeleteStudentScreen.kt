package com.soumit.technovision.ui.screens.students

import android.app.DatePickerDialog
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.google.firebase.firestore.FirebaseFirestore
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import com.soumit.technovision.util.SystemLogger
import java.util.Calendar
import java.util.Locale

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
    var isScheduleExpanded by remember { mutableStateOf(false) }

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
                        scheduleMap?.forEach { (day, slot) ->
                            daySchedules[day] = slot
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
            val formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year)
            onDateSelected(formattedDate)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    // Summary text for collapsed schedule card
    val activeDaysCount = daySchedules.count { it.value != "Off" }
    val scheduleSummaryText = if (activeDaysCount > 0) {
        daySchedules.filter { it.value != "Off" }.entries.joinToString(", ") { "${it.key.take(3)}: ${it.value.take(7)}..." }
    } else {
        "No active batch slots assigned"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update / Delete Student") },
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Personal & Family Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Personal & Family Information",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Student Full Name *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        ExposedDropdownMenuBox(
                            expanded = genderExpanded,
                            onExpandedChange = { genderExpanded = !genderExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedGender,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Gender") },
                                leadingIcon = { Icon(Icons.Default.Wc, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            )
                            ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                                genderOptions.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { selectedGender = option; genderExpanded = false })
                                }
                            }
                        }

                        OutlinedTextField(
                            value = fatherName,
                            onValueChange = { fatherName = it },
                            label = { Text("Father's Name") },
                            leadingIcon = { Icon(Icons.Default.Man, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = motherName,
                            onValueChange = { motherName = it },
                            label = { Text("Mother's Name") },
                            leadingIcon = { Icon(Icons.Default.Woman, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Section 2: Academic & School Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Academic & School Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

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
                                leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = schoolExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            )
                            ExposedDropdownMenu(expanded = schoolExpanded, onDismissRequest = { schoolExpanded = false }) {
                                schoolOptions.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { selectedSchool = option; schoolExpanded = false })
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = classExpanded,
                            onExpandedChange = { classExpanded = !classExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedClass,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Class") },
                                leadingIcon = { Icon(Icons.Default.Class, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            )
                            ExposedDropdownMenu(expanded = classExpanded, onDismissRequest = { classExpanded = false }) {
                                classOptions.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { selectedClass = option; classExpanded = false })
                                }
                            }
                        }

                        OutlinedTextField(
                            value = dob,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Date of Birth") },
                            leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { if (!isInPreview) showDatePicker { dob = it } }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Select DOB")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (!isInPreview) showDatePicker { dob = it } },
                            enabled = false
                        )

                        OutlinedTextField(
                            value = admissionDate,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Admission Date") },
                            leadingIcon = { Icon(Icons.Default.EventAvailable, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { if (!isInPreview) showDatePicker { admissionDate = it } }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Select Admission Date")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (!isInPreview) showDatePicker { admissionDate = it } },
                            enabled = false
                        )
                    }
                }

                // Section 3: Collapsible Weekly Schedule Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header Row with Accordion Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isScheduleExpanded = !isScheduleExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Weekly Batch Timings",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isScheduleExpanded) "Tap to collapse" else scheduleSummaryText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1
                                )
                            }

                            IconButton(onClick = { isScheduleExpanded = !isScheduleExpanded }) {
                                Icon(
                                    imageVector = if (isScheduleExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle Schedule",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Expanded Day-by-Day Dropdowns
                        if (isScheduleExpanded) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Spacer(modifier = Modifier.height(4.dp))

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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isMenuExpanded,
                                        onDismissRequest = { expandedDayMenu = null }
                                    ) {
                                        timeSlotOptions.forEach { slot ->
                                            DropdownMenuItem(
                                                text = { Text(slot) },
                                                onClick = {
                                                    daySchedules[day] = slot
                                                    expandedDayMenu = null
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Contact & Billing Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Contact & Billing",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = monthlyInstallment,
                            onValueChange = { monthlyInstallment = it },
                            label = { Text("Monthly Installment (₹)") },
                            leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = altPhone,
                            onValueChange = { altPhone = it },
                            label = { Text("Alternative Phone Number") },
                            leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Primary Update Button
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
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Update Information", style = MaterialTheme.typography.titleSmall)
                }

                // Destructive Delete Button
                OutlinedButton(
                    onClick = {
                        if (isInPreview) {
                            Toast.makeText(context, "Preview: Student Deleted!", Toast.LENGTH_SHORT).show()
                            onBack()
                            return@OutlinedButton
                        }

                        val db = FirebaseFirestore.getInstance()
                        db.collection("students").document(studentId).delete()
                            .addOnSuccessListener {
                                db.collection("users").document(studentId).delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Student deleted successfully.", Toast.LENGTH_SHORT).show()

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
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Student Record", style = MaterialTheme.typography.titleSmall)
                }

                if (statusMessage != null) {
                    Text(
                        text = statusMessage!!,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
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
