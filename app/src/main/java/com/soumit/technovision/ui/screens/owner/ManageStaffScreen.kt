package com.soumit.technovision.ui.screens.owner

import android.content.res.Configuration
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.soumit.technovision.ui.screens.SystemLogger
import com.soumit.technovision.ui.theme.TechnoVisionTheme

data class StaffModel(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "",
    val phone: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageStaffScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    var staffList by remember { mutableStateOf<List<StaffModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Edit Staff Dialog States
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedStaffForEdit by remember { mutableStateOf<StaffModel?>(null) }
    var editFullName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editRole by remember { mutableStateOf("faculty") }
    var editRoleExpanded by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }

    // Add Staff Form States
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("faculty") } // "faculty" or "receptionist"
    var roleExpanded by remember { mutableStateOf(false) }
    val roleOptions = listOf("faculty", "receptionist")

    var passwordVisible by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // Fetch staff list from Firestore
    LaunchedEffect(Unit) {
        if (isInPreview) {
            staffList = listOf(
                StaffModel(id = "1", fullName = "Amitabha Teacher", email = "faculty1@technovision.com", role = "faculty", phone = "9876543210"),
                StaffModel(id = "2", fullName = "Rina Receptionist", email = "reception1@technovision.com", role = "receptionist", phone = "9123456789")
            )
            isLoading = false
        } else {
            FirebaseFirestore.getInstance().collection("users")
                .whereIn("role", listOf("faculty", "receptionist"))
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        isLoading = false
                        Toast.makeText(context, "Error loading staff: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val list = snapshot.documents.map { doc ->
                            StaffModel(
                                id = doc.id,
                                fullName = doc.getString("fullName") ?: "",
                                email = doc.getString("email") ?: "",
                                role = doc.getString("role") ?: "",
                                phone = doc.getString("phone") ?: ""
                            )
                        }
                        staffList = list
                        isLoading = false
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Staff & Faculty") },
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
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Staff")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Center Staff Directory",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (staffList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No staff accounts created yet. Tap '+' to add staff.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(staffList) { staff ->
                        StaffCard(
                            staff = staff,
                            onEditClick = {
                                selectedStaffForEdit = staff
                                editFullName = staff.fullName
                                editPhone = staff.phone
                                editRole = staff.role
                                showEditDialog = true
                            },
                            onDeleteClick = {
                                if (isInPreview) {
                                    Toast.makeText(context, "Preview: Staff deleted!", Toast.LENGTH_SHORT).show()
                                } else {
                                    FirebaseFirestore.getInstance().collection("users").document(staff.id).delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Staff record deleted.", Toast.LENGTH_SHORT).show()

                                            // Log staff deletion
                                            SystemLogger.log(
                                                action = "Staff Deleted",
                                                details = "Deleted staff record: ${staff.fullName} (${staff.email})"
                                            )
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Delete failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Staff Dialog Form
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Staff Member") },
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
                        label = { Text("Email (Login ID)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (min 6 chars)") },
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
                            label = { Text("Staff Role") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
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

                    if (statusMessage != null) {
                        Text(
                            text = statusMessage!!,
                            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                if (isSaving) {
                    CircularProgressIndicator()
                } else {
                    TextButton(
                        onClick = {
                            if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
                                statusMessage = "Please fill in all required fields."
                                isError = true
                                return@TextButton
                            }

                            if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                statusMessage = "Invalid email format."
                                isError = true
                                return@TextButton
                            }

                            if (password.length < 6) {
                                statusMessage = "Password must be at least 6 characters."
                                isError = true
                                return@TextButton
                            }

                            if (isInPreview) {
                                Toast.makeText(context, "Preview: Staff created successfully!", Toast.LENGTH_SHORT).show()
                                showAddDialog = false
                                return@TextButton
                            }

                            isSaving = true
                            statusMessage = null

                            val db = FirebaseFirestore.getInstance()
                            val secondaryApp = try {
                                FirebaseApp.getInstance("SecondaryStaffApp")
                            } catch (e: Exception) {
                                FirebaseApp.initializeApp(
                                    context,
                                    FirebaseOptions.Builder()
                                        .setApiKey(FirebaseApp.getInstance().options.apiKey)
                                        .setApplicationId(FirebaseApp.getInstance().options.applicationId)
                                        .setProjectId(FirebaseApp.getInstance().options.projectId)
                                        .build(),
                                    "SecondaryStaffApp"
                                )
                            }

                            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

                            secondaryAuth.createUserWithEmailAndPassword(email.trim(), password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val userId = secondaryAuth.currentUser?.uid ?: ""
                                        val staffData = hashMapOf(
                                            "fullName" to fullName.trim(),
                                            "email" to email.trim(),
                                            "role" to selectedRole,
                                            "phone" to phone.trim(),
                                            "createdAt" to Timestamp.now()
                                        )

                                        db.collection("users").document(userId).set(staffData)
                                            .addOnSuccessListener {
                                                secondaryAuth.signOut()
                                                isSaving = false
                                                Toast.makeText(context, "Staff account created successfully!", Toast.LENGTH_SHORT).show()

                                                // Log staff creation action
                                                SystemLogger.log(
                                                    action = "Staff Added",
                                                    details = "Registered new $selectedRole: ${fullName.trim()} (${email.trim()})"
                                                )

                                                showAddDialog = false
                                                fullName = ""
                                                email = ""
                                                password = ""
                                                phone = ""
                                            }
                                            .addOnFailureListener { e ->
                                                isSaving = false
                                                statusMessage = "Firestore error: ${e.localizedMessage}"
                                                isError = true
                                            }
                                    } else {
                                        isSaving = false
                                        statusMessage = "Auth error: ${task.exception?.localizedMessage}"
                                        isError = true
                                    }
                                }
                        }
                    ) {
                        Text("Create Account")
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

    // Edit Staff Dialog Form
    if (showEditDialog && selectedStaffForEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Staff Member") },
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
                        value = selectedStaffForEdit!!.email,
                        onValueChange = {},
                        enabled = false,
                        label = { Text("Email (Unmodifiable)") },
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
                            label = { Text("Staff Role") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = editRoleExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
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
                if (isUpdating) {
                    CircularProgressIndicator()
                } else {
                    TextButton(
                        onClick = {
                            if (editFullName.isBlank()) {
                                Toast.makeText(context, "Name cannot be blank.", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }

                            if (isInPreview) {
                                Toast.makeText(context, "Preview: Staff updated!", Toast.LENGTH_SHORT).show()
                                showEditDialog = false
                                return@TextButton
                            }

                            isUpdating = true
                            val db = FirebaseFirestore.getInstance()
                            val updates = mapOf(
                                "fullName" to editFullName.trim(),
                                "phone" to editPhone.trim(),
                                "role" to editRole
                            )

                            db.collection("users").document(selectedStaffForEdit!!.id).update(updates)
                                .addOnSuccessListener {
                                    isUpdating = false
                                    Toast.makeText(context, "Staff updated successfully!", Toast.LENGTH_SHORT).show()

                                    // Log staff update action
                                    SystemLogger.log(
                                        action = "Staff Updated",
                                        details = "Updated staff record for ${selectedStaffForEdit!!.email} (Name: ${editFullName.trim()}, Role: $editRole)"
                                    )

                                    showEditDialog = false
                                }
                                .addOnFailureListener { e ->
                                    isUpdating = false
                                    Toast.makeText(context, "Update failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    ) {
                        Text("Save Changes")
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

@Composable
fun StaffCard(
    staff: StaffModel,
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
                TextButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onDeleteClick,
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
        ManageStaffScreen(onBack = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ManageStaffScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        ManageStaffScreen(onBack = {})
    }
}