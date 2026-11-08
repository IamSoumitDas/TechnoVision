package com.soumit.technovision.ui.screens.attendance

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import com.soumit.technovision.util.SystemLogger

/**
 * Resolves the genuine admin/owner/verifier name using userMap, logsMap, or formatting email.
 */
private fun resolveAdminName(
    rawName: String,
    date: String,
    timeSlot: String,
    userMap: Map<String, String>,
    logsMap: Map<String, String>
): String {
    // 1. Direct match in userMap by UID or Email
    if (rawName.isNotBlank() && userMap.containsKey(rawName)) {
        val mapped = userMap[rawName]
        if (!mapped.isNullOrBlank()) return mapped
    }
    if (rawName.isNotBlank() && userMap.containsKey(rawName.lowercase())) {
        val mapped = userMap[rawName.lowercase()]
        if (!mapped.isNullOrBlank()) return mapped
    }

    // 2. Check if rawName is already a legitimate person's name (and not generic placeholder "Admin")
    if (rawName.isNotBlank() &&
        !rawName.equals("Admin", ignoreCase = true) &&
        !rawName.equals("Faculty", ignoreCase = true) &&
        !rawName.contains("@") &&
        rawName.length < 35
    ) {
        return rawName
    }

    // 3. Check logsMap from system_logs where Attendance Verified/Rejected was audited
    val normKey = "${date.trim()}_${timeSlot.replace(Regex("\\s+"), " ").trim().lowercase()}"
    val rawKey = "${date.trim()}_${timeSlot.trim()}"
    val fromLog = logsMap[normKey] ?: logsMap[rawKey]
    if (!fromLog.isNullOrBlank()) {
        val mapped = userMap[fromLog] ?: userMap[fromLog.lowercase()]
        if (!mapped.isNullOrBlank()) return mapped
        if (!fromLog.contains("@") && !fromLog.equals("Admin", ignoreCase = true) && fromLog.length < 35) {
            return fromLog
        }
        if (fromLog.contains("@")) {
            val prefix = fromLog.substringBefore("@")
            if (!prefix.equals("admin", ignoreCase = true) && !prefix.equals("faculty", ignoreCase = true)) {
                return prefix.replace(".", " ")
                    .replace("_", " ")
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
        }
    }

    // 4. If rawName is an email, format it nicely
    if (rawName.contains("@")) {
        val prefix = rawName.substringBefore("@")
        if (!prefix.equals("admin", ignoreCase = true)) {
            return prefix.replace(".", " ")
                .replace("_", " ")
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    }

    // 5. Fallback to existing non-blank name if present
    if (rawName.isNotBlank()) {
        return rawName
    }

    return "Admin"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VerifyTeacherAttendanceScreen(
    onBack: () -> Unit,
    onNavigateToViewer: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    var currentAdminName by remember { mutableStateOf(if (isInPreview) "Soumit Das" else "Admin") }

    val previewData = remember {
        listOf(
            TeacherAttendanceRequestModel(
                id = "1",
                teacherName = "Amitabha Teacher",
                date = "24-10-2026",
                timeSlot = "9:00 AM - 11:00 AM",
                status = "Pending"
            ),
            TeacherAttendanceRequestModel(
                id = "2",
                teacherName = "Priya Teacher",
                date = "24-10-2026",
                timeSlot = "11:00 AM - 1:00 PM",
                status = "Verified",
                verifiedBy = "Soumit Das"
            ),
            TeacherAttendanceRequestModel(
                id = "3",
                teacherName = "Ramesh Teacher",
                date = "24-10-2026",
                timeSlot = "1:00 PM - 3:00 PM",
                status = "Rejected",
                rejectedBy = "Soumit Das"
            )
        )
    }

    var userMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var logsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var requestsList by remember { mutableStateOf<List<TeacherAttendanceRequestModel>>(if (isInPreview) previewData else emptyList()) }
    var isLoading by remember { mutableStateOf(!isInPreview) }
    var selectedFilter by remember { mutableStateOf("All") }

    val filterOptions = listOf("All", "Pending", "Verified", "Rejected")

    val filteredRequests = remember(requestsList, selectedFilter) {
        when (selectedFilter) {
            "Pending" -> requestsList.filter { it.status.trim().equals("Pending", ignoreCase = true) }
            "Verified" -> requestsList.filter { it.status.trim().equals("Verified", ignoreCase = true) }
            "Rejected" -> requestsList.filter { it.status.trim().equals("Rejected", ignoreCase = true) }
            else -> requestsList
        }
    }

    LaunchedEffect(Unit) {
        if (!isInPreview) {
            val db = FirebaseFirestore.getInstance()
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid

            if (userId != null) {
                db.collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val name = document.getString("fullName")?.takeIf { it.isNotBlank() }
                                ?: document.getString("name")?.takeIf { it.isNotBlank() }
                                ?: currentUser.displayName?.takeIf { it.isNotBlank() }
                                ?: currentUser.email?.substringBefore("@")
                                ?: "Admin"
                            currentAdminName = name
                            val email = document.getString("email") ?: currentUser.email ?: ""
                            userMap = userMap + buildMap {
                                put(userId, name)
                                if (email.isNotBlank()) {
                                    put(email, name)
                                    put(email.lowercase(), name)
                                }
                            }
                        }
                    }
            }
        }
    }

    DisposableEffect(Unit) {
        if (isInPreview) {
            return@DisposableEffect onDispose {}
        }

        val db = FirebaseFirestore.getInstance()

        // 1. Listen to users collection to map email/uid/id -> full name
        val usersReg = db.collection("users").addSnapshotListener { userSnapshot, _ ->
            if (userSnapshot != null) {
                val map = mutableMapOf<String, String>()
                for (doc in userSnapshot.documents) {
                    val fullName = doc.getString("fullName")
                        ?: doc.getString("name")
                        ?: doc.getString("displayName")
                        ?: continue
                    val email = doc.getString("email") ?: ""
                    val uid = doc.id
                    if (email.isNotBlank()) {
                        map[email] = fullName
                        map[email.lowercase()] = fullName
                    }
                    map[uid] = fullName
                }
                userMap = userMap + map
            }
        }

        // 2. Listen to system_logs for verified/rejected action logs
        val logsReg = db.collection("system_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { logsSnapshot, _ ->
                if (logsSnapshot != null) {
                    val map = mutableMapOf<String, String>()
                    val dateRegex = Regex("""\b(\d{2}-\d{2}-\d{4})\b""")
                    val timeSlotRegex = Regex("""(\d{1,2}:\d{2}\s*(?:AM|PM)\s*-\s*\d{1,2}:\d{2}\s*(?:AM|PM)|General / Other)""", RegexOption.IGNORE_CASE)
                    val verifierRegex = Regex("""\(Verified by\s+([^)]+)\)|\(Rejected by\s+([^)]+)\)|by\s+([A-Za-z\s.']+?)(?:\s+on\s+|\s*$|\))""", RegexOption.IGNORE_CASE)

                    for (doc in logsSnapshot.documents) {
                        val action = doc.getString("action") ?: ""
                        if (action.contains("Attendance Verified", ignoreCase = true) || action.contains("Attendance Rejected", ignoreCase = true)) {
                            val details = doc.getString("details") ?: ""
                            val performedBy = doc.getString("performedBy") ?: ""
                            val dMatch = dateRegex.find(details)?.value
                            val sMatch = timeSlotRegex.find(details)?.value
                            if (dMatch != null && sMatch != null) {
                                val key = "${dMatch.trim()}_${sMatch.replace(Regex("\\s+"), " ").trim().lowercase()}"
                                val rawKey = "${dMatch.trim()}_${sMatch.trim()}"

                                val vMatch = verifierRegex.find(details)
                                val extractedName = vMatch?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }?.trim()
                                val resolvedFromLog = if (!extractedName.isNullOrBlank() && !extractedName.equals("Admin", ignoreCase = true)) {
                                    extractedName
                                } else {
                                    performedBy.takeIf { it.isNotBlank() }
                                }

                                if (resolvedFromLog != null) {
                                    if (!map.containsKey(key)) map[key] = resolvedFromLog
                                    if (!map.containsKey(rawKey)) map[rawKey] = resolvedFromLog
                                }
                            }
                        }
                    }
                    logsMap = map
                }
            }

        val registration = FirebaseFirestore.getInstance()
            .collection("teacher_attendance")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    isLoading = false
                    Toast.makeText(context, "Error loading requests: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        TeacherAttendanceRequestModel(
                            id = doc.id,
                            teacherId = doc.getString("teacherId") ?: "",
                            teacherName = doc.getString("teacherName") ?: "",
                            date = doc.getString("date") ?: "",
                            timeSlot = doc.getString("timeSlot") ?: "",
                            status = doc.getString("status") ?: "Pending",
                            timestamp = doc.getTimestamp("timestamp"),
                            verifiedBy = doc.getString("verifiedBy") ?: "",
                            rejectedBy = doc.getString("rejectedBy") ?: ""
                        )
                    }
                    requestsList = list
                    isLoading = false
                }
            }

        onDispose {
            usersReg.remove()
            logsReg.remove()
            registration.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Faculty Attendance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (onNavigateToViewer != null) {
                        IconButton(onClick = onNavigateToViewer) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = "View Verified Records"
                            )
                        }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Faculty Attendance Requests",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { filter ->
                    val count = when (filter) {
                        "Pending" -> requestsList.count { it.status.trim().equals("Pending", ignoreCase = true) }
                        "Verified" -> requestsList.count { it.status.trim().equals("Verified", ignoreCase = true) }
                        "Rejected" -> requestsList.count { it.status.trim().equals("Rejected", ignoreCase = true) }
                        else -> requestsList.size
                    }
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text("$filter ($count)") }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else if (filteredRequests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedFilter == "All") {
                            "No teacher attendance requests found."
                        } else {
                            "No $selectedFilter teacher attendance requests found."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredRequests, key = { it.id }) { request ->
                        AdminVerifyRequestCard(
                            request = request,
                            userMap = userMap,
                            logsMap = logsMap,
                            onVerify = {
                                if (isInPreview) {
                                    Toast.makeText(context, "Preview: Request Verified!", Toast.LENGTH_SHORT).show()
                                    return@AdminVerifyRequestCard
                                }

                                val currentUser = FirebaseAuth.getInstance().currentUser
                                val currentUserUid = currentUser?.uid ?: ""
                                val currentUserEmail = currentUser?.email ?: ""
                                val adminToSave = currentAdminName.takeIf { it.isNotBlank() && it != "Admin" }
                                    ?: userMap[currentUserUid]
                                    ?: userMap[currentUserEmail]
                                    ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
                                    ?: currentUserEmail.takeIf { it.isNotBlank() }?.substringBefore("@")?.replace(".", " ")?.replaceFirstChar { it.uppercase() }
                                    ?: currentAdminName.ifBlank { "Admin" }

                                val updates = mapOf(
                                    "status" to "Verified",
                                    "verifiedBy" to adminToSave
                                )

                                FirebaseFirestore.getInstance().collection("teacher_attendance")
                                    .document(request.id)
                                    .update(updates)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Attendance verified successfully!", Toast.LENGTH_SHORT).show()

                                        SystemLogger.log(
                                            action = "Attendance Verified",
                                            details = "Approved time slot [${request.timeSlot}] for teacher ${request.teacherName} on date ${request.date} (Verified by $adminToSave)"
                                        )
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Failed to verify: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                            },
                            onReject = {
                                if (isInPreview) {
                                    Toast.makeText(context, "Preview: Request Rejected!", Toast.LENGTH_SHORT).show()
                                    return@AdminVerifyRequestCard
                                }

                                val currentUser = FirebaseAuth.getInstance().currentUser
                                val currentUserUid = currentUser?.uid ?: ""
                                val currentUserEmail = currentUser?.email ?: ""
                                val adminToSave = currentAdminName.takeIf { it.isNotBlank() && it != "Admin" }
                                    ?: userMap[currentUserUid]
                                    ?: userMap[currentUserEmail]
                                    ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
                                    ?: currentUserEmail.takeIf { it.isNotBlank() }?.substringBefore("@")?.replace(".", " ")?.replaceFirstChar { it.uppercase() }
                                    ?: currentAdminName.ifBlank { "Admin" }

                                val updates = mapOf(
                                    "status" to "Rejected",
                                    "rejectedBy" to adminToSave
                                )

                                FirebaseFirestore.getInstance().collection("teacher_attendance")
                                    .document(request.id)
                                    .update(updates)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Attendance request rejected. Time slot released.", Toast.LENGTH_SHORT).show()

                                        SystemLogger.log(
                                            action = "Attendance Rejected",
                                            details = "Rejected time slot [${request.timeSlot}] for teacher ${request.teacherName} on date ${request.date} (Rejected by $adminToSave). Slot is now open for re-request."
                                        )
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Failed to reject: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminVerifyRequestCard(
    request: TeacherAttendanceRequestModel,
    userMap: Map<String, String>,
    logsMap: Map<String, String>,
    onVerify: () -> Unit,
    onReject: () -> Unit
) {
    val isVerified = request.status.trim().equals("Verified", ignoreCase = true)
    val isRejected = request.status.trim().equals("Rejected", ignoreCase = true)
    val isPending = request.status.trim().equals("Pending", ignoreCase = true) || (!isVerified && !isRejected)
    var showRejectDialog by remember { mutableStateOf(false) }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reject Attendance Request?") },
            text = {
                Text(
                    "Are you sure you want to reject attendance request for " +
                        "${request.teacherName.ifBlank { "this faculty member" }} for slot " +
                        "${request.timeSlot} on ${request.date}?\n\n" +
                        "This will release the time slot so any faculty member can request it again."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRejectDialog = false
                        onReject()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reject Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isVerified -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                isRejected -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = request.teacherName.ifBlank { "Faculty Member" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Slot: ${request.timeSlot}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Date: ${request.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Surface(
                    color = when {
                        isVerified -> MaterialTheme.colorScheme.primaryContainer
                        isRejected -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isVerified -> Icons.Default.CheckCircle
                                isRejected -> Icons.Default.Cancel
                                else -> Icons.Default.HourglassTop
                            },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = when {
                                isVerified -> MaterialTheme.colorScheme.primary
                                isRejected -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        )
                        Text(
                            text = request.status,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                isVerified -> MaterialTheme.colorScheme.onPrimaryContainer
                                isRejected -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }
                }
            }

            when {
                isPending -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRejectDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                            Text("Reject")
                        }

                        Button(
                            onClick = onVerify,
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                            Text("Verify")
                        }
                    }
                }
                isVerified -> {
                    val verifier = remember(request.verifiedBy, request.date, request.timeSlot, userMap, logsMap) {
                        resolveAdminName(request.verifiedBy, request.date, request.timeSlot, userMap, logsMap)
                    }
                    Text(
                        text = "Verified by $verifier",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                else -> {
                    val rejector = remember(request.rejectedBy, request.date, request.timeSlot, userMap, logsMap) {
                        resolveAdminName(request.rejectedBy, request.date, request.timeSlot, userMap, logsMap)
                    }
                    Text(
                        text = "Rejected by $rejector",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun VerifyTeacherAttendanceScreenPreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        VerifyTeacherAttendanceScreen(onBack = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun VerifyTeacherAttendanceScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        VerifyTeacherAttendanceScreen(onBack = {})
    }
}
