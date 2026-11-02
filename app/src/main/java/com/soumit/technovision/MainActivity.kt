package com.soumit.technovision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.soumit.technovision.navigation.AppNavHost
import com.soumit.technovision.navigation.Routes
import com.soumit.technovision.ui.theme.TechnoVisionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TechnoVisionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val auth = FirebaseAuth.getInstance()
                    val currentUser = auth.currentUser

                    var startDestination by remember { mutableStateOf<String?>(null) }
                    var isLoadingRole by remember { mutableStateOf(currentUser != null) }

                    LaunchedEffect(currentUser) {
                        if (currentUser != null) {
                            val userId = currentUser.uid
                            FirebaseFirestore.getInstance().collection("users").document(userId).get()
                                .addOnSuccessListener { document ->
                                    val role = document.getString("role") ?: "auth"
                                    startDestination = when (role.lowercase()) {
                                        "owner" -> Routes.OWNER_HOME
                                        "faculty" -> Routes.FACULTY_HOME
                                        "receptionist" -> Routes.RECEPTIONIST_HOME
                                        else -> Routes.AUTH
                                    }
                                    isLoadingRole = false
                                }
                                .addOnFailureListener {
                                    startDestination = Routes.AUTH
                                    isLoadingRole = false
                                }
                        } else {
                            startDestination = Routes.AUTH
                            isLoadingRole = false
                        }
                    }

                    if (isLoadingRole || startDestination == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        AppNavHost(
                            navController = navController,
                            startDestination = startDestination!!
                        )
                    }
                }
            }
        }
    }
}
