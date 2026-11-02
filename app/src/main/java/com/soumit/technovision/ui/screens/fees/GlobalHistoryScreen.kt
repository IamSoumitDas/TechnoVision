package com.soumit.technovision.ui.screens.fees

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soumit.technovision.data.model.FeePaymentModel
import com.soumit.technovision.ui.theme.TechnoVisionTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    var paymentHistory by remember { mutableStateOf<List<FeePaymentModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Grouping by Date string (e.g., "24 Oct 2026")
    val groupedHistory = remember(paymentHistory) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        paymentHistory.groupBy { payment ->
            if (payment.timestamp != null) {
                dateFormat.format(payment.timestamp.toDate())
            } else {
                "Unknown Date"
            }
        }
    }

    LaunchedEffect(Unit) {
        if (isInPreview) {
            paymentHistory = listOf(
                FeePaymentModel(
                    id = "1",
                    studentName = "Aarav Sharma",
                    amountPaid = 500L,
                    paymentMethod = "UPI",
                    receivedBy = "Amitabha Teacher",
                    timestamp = com.google.firebase.Timestamp.now()
                ),
                FeePaymentModel(
                    id = "2",
                    studentName = "Priya Das",
                    amountPaid = 600L,
                    paymentMethod = "Cash",
                    receivedBy = "Owner",
                    timestamp = com.google.firebase.Timestamp.now()
                )
            )
            isLoading = false
        } else {
            FirebaseFirestore.getInstance().collection("fee_payments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        isLoading = false
                        Toast.makeText(context, "Error loading history: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val history = snapshot.documents.map { doc ->
                            FeePaymentModel(
                                id = doc.id,
                                studentId = doc.getString("studentId") ?: "",
                                studentName = doc.getString("studentName") ?: "",
                                amountPaid = doc.getLong("amountPaid") ?: 0L,
                                paymentMethod = doc.getString("paymentMethod") ?: "",
                                remarks = doc.getString("remarks") ?: "",
                                receivedBy = doc.getString("receivedBy") ?: "",
                                timestamp = doc.getTimestamp("timestamp")
                            )
                        }
                        paymentHistory = history
                        isLoading = false
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Global Payment History") },
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
                text = "All Received Payments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (paymentHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No payments have been recorded yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    groupedHistory.forEach { (dateStr, dailyPayments) ->
                        item {
                            // Date Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        items(dailyPayments) { payment ->
                            PaymentHistoryCard(payment = payment)
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun GlobalHistoryScreenPreviewLight() {
    TechnoVisionTheme(darkTheme = false) {
        GlobalHistoryScreen(onBack = {})
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun GlobalHistoryScreenPreviewDark() {
    TechnoVisionTheme(darkTheme = true) {
        GlobalHistoryScreen(onBack = {})
    }
}
