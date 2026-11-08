package com.soumit.technovision.ui.screens.auth

import android.content.res.Configuration
import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.soumit.technovision.ui.theme.TechnoVisionTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthScreen(onLoginSuccess: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var passwordVisible by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val isInPreview = LocalInspectionMode.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    // Local function for Login handling
    fun handleLogin() {
        keyboardController?.hide()

        if (email.isBlank() || password.isBlank()) {
            statusMessage = "Please fill in all fields."
            isError = true
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            statusMessage = "Please enter a valid email structure (e.g. name@example.com)."
            isError = true
            return
        }

        if (isInPreview) {
            statusMessage = "Preview: Logged in successfully"
            isError = false
            onLoginSuccess("employee")
            return
        }

        isLoading = true
        statusMessage = null
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId.isNullOrBlank()) {
                        isLoading = false
                        isError = true
                        statusMessage = "Authentication error: User ID not found."
                        auth.signOut()
                        return@addOnCompleteListener
                    }
                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { document ->
                            isLoading = false
                            if (document.exists()) {
                                val dbRole = document.getString("role") ?: ""
                                isError = false
                                statusMessage = "Welcome! Logged in successfully."
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
    }

    // Local function for Forgot Password handling
    fun handleForgotPassword() {
        keyboardController?.hide()

        if (email.isBlank()) {
            statusMessage = "Please enter your email address above first."
            isError = true
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            statusMessage = "Please enter a valid email structure (e.g. name@example.com)."
            isError = true
            return
        }

        if (isInPreview) {
            statusMessage = "Preview: Password reset email sent."
            isError = false
            return
        }

        isLoading = true
        statusMessage = null
        val auth = FirebaseAuth.getInstance()

        auth.sendPasswordResetEmail(email.trim())
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    isError = false
                    statusMessage = "Password reset email sent! Check your inbox."
                } else {
                    isError = true
                    statusMessage = "Error: ${task.exception?.localizedMessage}"
                }
            }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = "Logo",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "TechnoVision",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Sign in to access your portal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        statusMessage = null
                    },
                    label = { Text("Email Address") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        statusMessage = null
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { handleLogin() }
                    ),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (passwordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = description)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Forgot Password Button Aligned to the Right
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(onClick = { handleForgotPassword() }) {
                        Text(
                            text = "Forgot Password?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Login Button / Loader
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator(modifier = Modifier.size(36.dp))
                    }
                } else {
                    Button(
                        onClick = { handleLogin() },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Log In",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Inline Status Message
                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        color = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusMessage!!,
                            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
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