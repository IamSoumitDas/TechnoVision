package com.soumit.technovision.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import java.text.SimpleDateFormat
import java.util.*

data class LogModel(
    val id: String = "",
    val action: String = "",
    val details: String = "",
    val performedBy: String = "",
    val timestamp: Timestamp? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemLogsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    var logList by remember { mutableStateOf<List<LogModel>>(emptyList()) }
    var userMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (isInPreview) {
            // Rich example system logs for preview testing
            logList = listOf(
                LogModel(
                    id = "1",
                    action = "Student Enrolled",
                    details = "Enrolled student Aarav Sharma (Class X)",
                    performedBy = "owner.preview@technovision.com",
                    timestamp = Timestamp.now()
                ),
                LogModel(
                    id = "2",
                    action = "Attendance Saved",
                    details = "Saved attendance for 9:00 AM - 11:00 AM batch",
                    performedBy = "faculty@technovision.com",
                    timestamp = Timestamp(Date(System.currentTimeMillis() - 3600000))
                ),
                LogModel(
                    id = "3",
                    action = "Staff Added",
                    details = "Added receptionist account for Rina",
                    performedBy = "owner.preview@technovision.com",
                    timestamp = Timestamp(Date(System.currentTimeMillis() - 86400000))
                ),
                LogModel(
                    id = "4",
                    action = "Course Batch Configured",
                    details = "Created new batch for DCA (9:00 AM - 11:00 AM)",
                    performedBy = "owner.preview@technovision.com",
                    timestamp = Timestamp(Date(System.currentTimeMillis() - 172800000))
                )
            )
            userMap = mapOf(
                "owner.preview@technovision.com" to "Soumit Das",
                "faculty@technovision.com" to "Amitabha Teacher"
            )
            isLoading = false
        } else {
            val db = FirebaseFirestore.getInstance()

            // Fetch users to map email and UID to fullName
            db.collection("users").get().addOnSuccessListener { userSnapshot ->
                val map = mutableMapOf<String, String>()
                for (doc in userSnapshot.documents) {
                    val fullName = doc.getString("fullName") ?: continue
                    val email = doc.getString("email") ?: ""
                    val uid = doc.id
                    if (email.isNotBlank()) map[email] = fullName
                    map[uid] = fullName
                }
                userMap = map
            }

            // Fetch system logs
            db.collection("system_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val list = snapshot.documents.map { doc ->
                            LogModel(
                                id = doc.id,
                                action = doc.getString("action") ?: "",
                                details = doc.getString("details") ?: "",
                                performedBy = doc.getString("performedBy") ?: "",
                                timestamp = doc.getTimestamp("timestamp")
                            )
                        }
                        logList = list
                        isLoading = false
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Logs") },
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
                .padding(16.dp)
        ) {
            Text(
                text = "Activity History & Time-stamped Changes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (logList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No system logs recorded yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(logList) { log ->
                        LogCard(log = log, userMap = userMap)
                    }
                }
            }
        }
    }
}

@Composable
fun LogCard(log: LogModel, userMap: Map<String, String>) {
    val formattedTime = remember(log.timestamp) {
        if (log.timestamp != null) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(log.timestamp.toDate())
        } else {
            "Just now"
        }
    }

    // Resolve email or UID to Full Name if available in userMap
    val displayName = userMap[log.performedBy] ?: log.performedBy

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.action,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.details,
                style = MaterialTheme.typography.bodyMedium
            )
            if (log.performedBy.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "By: $displayName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun SystemLogsScreenPreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        SystemLogsScreen(onBack = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SystemLogsScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        SystemLogsScreen(onBack = {})
    }
}