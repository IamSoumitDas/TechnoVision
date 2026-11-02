package com.soumit.technovision.ui.screens.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.soumit.technovision.data.model.PortalAction
import com.soumit.technovision.data.model.PortalRoleSections
import com.soumit.technovision.data.model.PortalSectionModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalDashboardScreen(
    portalTitle: String,
    roleSubtitle: String,
    sections: List<PortalSectionModel>,
    onLogout: () -> Unit
) {
    val isInPreview = LocalInspectionMode.current
    var userName by remember { mutableStateOf("User") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (isInPreview) {
            userName = "Soumit Das"
        } else {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid
            if (userId != null) {
                FirebaseFirestore.getInstance().collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            userName = document.getString("fullName") ?: "User"
                        }
                    }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (userName.isNotBlank() && userName != "User") {
                                    val parts = userName.trim().split("\\s+".toRegex())
                                    if (parts.size > 1) "${parts.first().take(1)}${parts.last().take(1)}".uppercase()
                                    else parts.first().take(1).uppercase()
                                } else "U",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Techno Vision Portal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            sections.forEach { section ->
                PortalDashboardSection(section = section)
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        if (!isInPreview) {
                            FirebaseAuth.getInstance().signOut()
                        }
                        onLogout()
                    }
                ) {
                    Text("Log Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PortalDashboardSection(section: PortalSectionModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        section.actions.chunked(2).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (action in rowActions) {
                    Box(modifier = Modifier.weight(1f)) {
                        PortalActionCard(action = action)
                    }
                }
                if (rowActions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun PortalActionCard(action: PortalAction) {
    Card(
        onClick = action.onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = action.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Preview(name = "Interactive Role Switcher Preview", showBackground = true)
@Composable
fun PortalDashboardInteractivePreview() {
    TechnoVisionTheme {
        var selectedRole by remember { mutableStateOf("Owner") }
        val sections = PortalRoleSections.getSectionsForRole(selectedRole)
        val portalTitle = when (selectedRole) {
            "Faculty" -> "Faculty Portal"
            "Receptionist" -> "Receptionist Portal"
            else -> "Owner Portal"
        }
        val roleSubtitle = when (selectedRole) {
            "Faculty" -> "TechnoVision Teaching Staff"
            "Receptionist" -> "TechnoVision Front Desk"
            else -> "TechnoVision Administration"
        }

        Box(modifier = Modifier.fillMaxSize()) {
            PortalDashboardScreen(
                portalTitle = portalTitle,
                roleSubtitle = roleSubtitle,
                sections = sections,
                onLogout = {}
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Owner", "Faculty", "Receptionist").forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            label = { Text(role) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Dark Mode Preview", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PortalDashboardDarkPreview() {
    TechnoVisionTheme(darkTheme = true) {
        PortalDashboardScreen(
            portalTitle = "Owner Portal",
            roleSubtitle = "TechnoVision Administration",
            sections = emptyList(),
            onLogout = {}
        )
    }
}
