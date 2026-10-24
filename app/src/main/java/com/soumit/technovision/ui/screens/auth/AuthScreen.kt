package com.soumit.technovision.ui.screens.auth

import android.content.res.Configuration
import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.soumit.technovision.ui.theme.TechnoVisionTheme

@Composable
fun AuthScreen(onLoginSuccess: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // UI states for password visibility and inline status message
    var passwordVisible by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val isInPreview = LocalInspectionMode.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TechnoVision Portal",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    statusMessage = null
                },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field with Show/Hide Toggle Icon
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    statusMessage = null
                },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    val description = if (passwordVisible) "Hide password" else "Show password"
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        // 1. Check empty fields
                        if (email.isBlank() || password.isBlank()) {
                            statusMessage = "Please fill in all fields."
                            isError = true
                            return@Button
                        }

                        // 2. Validate email structure
                        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            statusMessage = "Please enter a valid email structure (e.g. name@example.com)."
                            isError = true
                            return@Button
                        }

                        // Preview simulation check
                        if (isInPreview) {
                            statusMessage = "Preview: Logged in successfully"
                            isError = false
                            onLoginSuccess("employee")
                            return@Button
                        }

                        isLoading = true
                        statusMessage = null
                        val auth = FirebaseAuth.getInstance()
                        val db = FirebaseFirestore.getInstance()

                        // 3. Authenticate with Firebase Auth
                        auth.signInWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val userId = auth.currentUser?.uid ?: ""

                                    // 4. Automatically Fetch Role from Firestore Database
                                    db.collection("users").document(userId).get()
                                        .addOnSuccessListener { document ->
                                            isLoading = false
                                            if (document.exists()) {
                                                val dbRole = document.getString("role") ?: ""
                                                isError = false
                                                statusMessage = "Welcome! Logged in successfully."
                                                // Pass the detected role back so navigation handles it
                                                onLoginSuccess(dbRole)
                                            } else {
                                                isLoading = false
                                                isError = true
                                                statusMessage = "User profile not found in database."
                                                auth.signOut()
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            isError = true
                                            statusMessage = "Database Error: ${e.localizedMessage}"
                                            auth.signOut()
                                        }
                                } else {
                                    isLoading = false
                                    isError = true
                                    statusMessage = "Login Error: ${task.exception?.localizedMessage}"
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Log In")
                }
            }

            // Inline Status Message Element
            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = statusMessage!!,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun AuthScreenPreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        AuthScreen(onLoginSuccess = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AuthScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        AuthScreen(onLoginSuccess = {})
    }
}