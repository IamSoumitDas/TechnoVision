package com.soumit.technovision.ui.screens.staff

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soumit.technovision.data.model.StaffModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme

@Composable
fun ManageStaffScreen(
    onBack: () -> Unit,
    viewModel: ManageStaffViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    ManageStaffContent(
        uiState = uiState,
        onBack = onBack,
        onCreateStaff = viewModel::createStaff,
        onUpdateStaff = viewModel::updateStaff,
        onDeleteStaff = viewModel::deleteStaff
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ManageStaffContent(
    uiState: ManageStaffUiState,
    onBack: () -> Unit,
    onCreateStaff: (fullName: String, email: String, password: String, phone: String, role: String, onSuccess: () -> Unit) -> Unit,
    onUpdateStaff: (id: String, fullName: String, phone: String, role: String, onSuccess: () -> Unit) -> Unit,
    onDeleteStaff: (staff: StaffModel, onSuccess: () -> Unit) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var staffToDelete by remember { mutableStateOf<StaffModel?>(null) }

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedStaffForEdit by remember { mutableStateOf<StaffModel?>(null) }
    var editFullName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editRole by remember { mutableStateOf("faculty") }
    var editRoleExpanded by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("faculty") }
    var roleExpanded by remember { mutableStateOf(false) }
    val roleOptions = listOf("faculty", "receptionist")

    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Faculty Management") },
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
            if (uiState.isOnline) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    shape = FloatingActionButtonDefaults.shape
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Staff")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (!uiState.isOnline) {
                com.soumit.technovision.ui.common.OfflineWarningBanner(
                    message = "Connect to the internet to manage staff accounts.",
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Text(
                text = "Staff Directory",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else if (uiState.staffList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No staff accounts yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.staffList, key = { it.id }, contentType = { "staff_item" }) { staff ->
                        StaffCard(
                            staff = staff,
                            isOnline = uiState.isOnline,
                            onEditClick = {
                                selectedStaffForEdit = staff
                                editFullName = staff.fullName
                                editPhone = staff.phone
                                editRole = staff.role
                                showEditDialog = true
                            },
                            onDeleteClick = {
                                staffToDelete = staff
                            }
                        )
                    }
                }
            }
        }
    }

    staffToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { staffToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Staff Member?") },
            text = { Text("Delete account for \"${target.fullName}\" (${target.email})?") },
            confirmButton = {
                Button(
                    onClick = {
                        val currentStaff = target
                        staffToDelete = null
                        onDeleteStaff(currentStaff) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { staffToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Staff") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle password")
                            }
                        },
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

                    ExposedDropdownMenuBox(
                        expanded = roleExpanded,
                        onExpandedChange = { roleExpanded = !roleExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedRole,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Role") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded = roleExpanded,
                            onDismissRequest = { roleExpanded = false }
                        ) {
                            roleOptions.forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role.replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        selectedRole = role
                                        roleExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (uiState.isSaving) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    TextButton(
                        onClick = {
                            onCreateStaff(fullName, email, password, phone, selectedRole) {
                                showAddDialog = false
                                fullName = ""
                                email = ""
                                password = ""
                                phone = ""
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditDialog) {
        selectedStaffForEdit?.let { staffEditTarget ->
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Staff") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editFullName,
                        onValueChange = { editFullName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = staffEditTarget.email,
                        onValueChange = {},
                        enabled = false,
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = editRoleExpanded,
                        onExpandedChange = { editRoleExpanded = !editRoleExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = editRole,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Role") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = editRoleExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded = editRoleExpanded,
                            onDismissRequest = { editRoleExpanded = false }
                        ) {
                            roleOptions.forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role.replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        editRole = role
                                        editRoleExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (uiState.isUpdating) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    TextButton(
                        onClick = {
                            onUpdateStaff(staffEditTarget.id, editFullName, editPhone, editRole) {
                                showEditDialog = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
}

@Composable
fun StaffCard(
    staff: StaffModel,
    isOnline: Boolean = true,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = staff.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = staff.role.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Email: ${staff.email}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            if (staff.phone.isNotBlank()) {
                Text(text = "Phone: ${staff.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEditClick,
                    enabled = isOnline
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isOnline) "Edit" else "Edit (Offline)")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onDeleteClick,
                    enabled = isOnline,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun ManageStaffScreenPreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        ManageStaffContent(
            uiState = ManageStaffUiState(
                staffList = com.soumit.technovision.ui.common.PreviewData.allStaff,
                isLoading = false
            ),
            onBack = {},
            onCreateStaff = { _, _, _, _, _, _ -> },
            onUpdateStaff = { _, _, _, _, _ -> },
            onDeleteStaff = { _, _ -> }
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ManageStaffScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        ManageStaffContent(
            uiState = ManageStaffUiState(
                staffList = com.soumit.technovision.ui.common.PreviewData.allStaff,
                isLoading = false
            ),
            onBack = {},
            onCreateStaff = { _, _, _, _, _, _ -> },
            onUpdateStaff = { _, _, _, _, _ -> },
            onDeleteStaff = { _, _ -> }
        )
    }
}
