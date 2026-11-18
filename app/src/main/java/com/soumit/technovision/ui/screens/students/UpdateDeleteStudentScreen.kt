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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import java.util.Calendar
import java.util.Locale

@Composable
fun UpdateDeleteStudentScreen(
    @Suppress("UNUSED_PARAMETER") studentId: String,
    onBack: () -> Unit,
    viewModel: UpdateDeleteStudentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    UpdateDeleteStudentContent(
        uiState = uiState,
        onBack = onBack,
        onFullNameChanged = viewModel::onFullNameChanged,
        onClassChanged = viewModel::onClassChanged,
        onSchoolChanged = viewModel::onSchoolChanged,
        onGenderChanged = viewModel::onGenderChanged,
        onDobChanged = viewModel::onDobChanged,
        onAdmissionDateChanged = viewModel::onAdmissionDateChanged,
        onFatherNameChanged = viewModel::onFatherNameChanged,
        onMotherNameChanged = viewModel::onMotherNameChanged,
        onMonthlyInstallmentChanged = viewModel::onMonthlyInstallmentChanged,
        onPhoneChanged = viewModel::onPhoneChanged,
        onAltPhoneChanged = viewModel::onAltPhoneChanged,
        onDayScheduleChanged = viewModel::onDayScheduleChanged,
        onUpdateClicked = { viewModel.updateStudent(onBack) },
        onDeleteConfirmed = { viewModel.deleteStudent(onBack) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateDeleteStudentContent(
    uiState: UpdateDeleteStudentUiState,
    onBack: () -> Unit,
    onFullNameChanged: (String) -> Unit,
    onClassChanged: (String) -> Unit,
    onSchoolChanged: (String) -> Unit,
    onGenderChanged: (String) -> Unit,
    onDobChanged: (String) -> Unit,
    onAdmissionDateChanged: (String) -> Unit,
    onFatherNameChanged: (String) -> Unit,
    onMotherNameChanged: (String) -> Unit,
    onMonthlyInstallmentChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onAltPhoneChanged: (String) -> Unit,
    onDayScheduleChanged: (String, String) -> Unit,
    onUpdateClicked: () -> Unit,
    onDeleteConfirmed: () -> Unit
) {
    val context = LocalContext.current

    var classExpanded by remember { mutableStateOf(false) }
    val classOptions = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII")

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

    var genderExpanded by remember { mutableStateOf(false) }
    val genderOptions = listOf("Male", "Female", "Other")

    val daysOfWeek = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val timeSlotOptions = listOf("Off", "9:00 AM - 11:00 AM", "11:00 AM - 1:00 PM", "1:00 PM - 3:00 PM", "3:00 PM - 5:00 PM", "5:00 PM - 7:00 PM", "7:00 PM - 9:00 PM")

    var expandedDayMenu by remember { mutableStateOf<String?>(null) }
    var isScheduleExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val formattedDate = String.format(Locale.US, "%02d/%02d/%04d", day, month + 1, year)
                onDateSelected(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val scheduleSummaryText = if (uiState.activeDaysCount > 0) {
        uiState.daySchedules.filter { it.value != "Off" }.entries.joinToString(", ") { "${it.key.take(3)}: ${it.value.take(7)}..." }
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
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
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
                            value = uiState.fullName,
                            onValueChange = onFullNameChanged,
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
                                value = uiState.selectedGender,
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
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            onGenderChanged(option)
                                            genderExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = uiState.fatherName,
                            onValueChange = onFatherNameChanged,
                            label = { Text("Father's Name") },
                            leadingIcon = { Icon(Icons.Default.Man, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.motherName,
                            onValueChange = onMotherNameChanged,
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
                                value = uiState.selectedSchool,
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
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            onSchoolChanged(option)
                                            schoolExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = classExpanded,
                            onExpandedChange = { classExpanded = !classExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = uiState.selectedClass,
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
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            onClassChanged(option)
                                            classExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = uiState.dob,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Date of Birth") },
                            leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker(onDobChanged) }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Select DOB")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker(onDobChanged) }
                        )

                        OutlinedTextField(
                            value = uiState.admissionDate,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Admission Date") },
                            leadingIcon = { Icon(Icons.Default.EventAvailable, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker(onAdmissionDateChanged) }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Select Admission Date")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker(onAdmissionDateChanged) }
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

                        if (isScheduleExpanded) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Spacer(modifier = Modifier.height(4.dp))

                            daysOfWeek.forEach { day ->
                                val currentSlot = uiState.daySchedules[day] ?: "Off"
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
                                                    onDayScheduleChanged(day, slot)
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
                            value = uiState.monthlyInstallment,
                            onValueChange = onMonthlyInstallmentChanged,
                            label = { Text("Monthly Installment (₹)") },
                            leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.phone,
                            onValueChange = onPhoneChanged,
                            label = { Text("Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.altPhone,
                            onValueChange = onAltPhoneChanged,
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
                if (uiState.isUpdating) {
                    Box(modifier = Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                } else {
                    Button(
                        onClick = onUpdateClicked,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Information", style = MaterialTheme.typography.titleSmall)
                    }
                }

                // Destructive Delete Button
                if (uiState.isDeleting) {
                    Box(modifier = Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                } else {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Student Record", style = MaterialTheme.typography.titleSmall)
                    }
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        title = { Text("Delete Student?") },
                        text = { Text("Are you sure you want to delete the student record for \"${uiState.fullName}\"? This action cannot be undone.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDeleteDialog = false
                                    onDeleteConfirmed()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
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
        UpdateDeleteStudentContent(
            uiState = UpdateDeleteStudentUiState(
                fullName = "Aarav Sharma",
                selectedClass = "X",
                selectedSchool = "St. Xavier's School",
                fatherName = "Rajesh Sharma",
                motherName = "Sunita Sharma",
                monthlyInstallment = "500",
                phone = "9876543210",
                isLoading = false
            ),
            onBack = {},
            onFullNameChanged = {},
            onClassChanged = {},
            onSchoolChanged = {},
            onGenderChanged = {},
            onDobChanged = {},
            onAdmissionDateChanged = {},
            onFatherNameChanged = {},
            onMotherNameChanged = {},
            onMonthlyInstallmentChanged = {},
            onPhoneChanged = {},
            onAltPhoneChanged = {},
            onDayScheduleChanged = { _, _ -> },
            onUpdateClicked = {},
            onDeleteConfirmed = {}
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun UpdateDeleteScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        UpdateDeleteStudentContent(
            uiState = UpdateDeleteStudentUiState(
                fullName = "Aarav Sharma",
                selectedClass = "X",
                selectedSchool = "St. Xavier's School",
                fatherName = "Rajesh Sharma",
                motherName = "Sunita Sharma",
                monthlyInstallment = "500",
                phone = "9876543210",
                isLoading = false
            ),
            onBack = {},
            onFullNameChanged = {},
            onClassChanged = {},
            onSchoolChanged = {},
            onGenderChanged = {},
            onDobChanged = {},
            onAdmissionDateChanged = {},
            onFatherNameChanged = {},
            onMotherNameChanged = {},
            onMonthlyInstallmentChanged = {},
            onPhoneChanged = {},
            onAltPhoneChanged = {},
            onDayScheduleChanged = { _, _ -> },
            onUpdateClicked = {},
            onDeleteConfirmed = {}
        )
    }
}
