package com.soumit.technovision.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soumit.technovision.data.model.LogModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn

interface AuditLogRepository {
    fun getLogsStream(): Flow<List<LogModel>>
    fun log(action: String, details: String)
}

class FirestoreAuditLogRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuditLogRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val sharedLogsStream: Flow<List<LogModel>> = callbackFlow {
        val subscription = firestore.collection("system_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(150)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val logs = snapshot.documents.map { doc ->
                        LogModel(
                            id = doc.id,
                            action = doc.getString("action") ?: "",
                            details = doc.getString("details") ?: "",
                            performedBy = doc.getString("performedBy") ?: "",
                            timestamp = doc.getTimestamp("timestamp")
                        )
                    }
                    trySend(logs)
                }
            }
        awaitClose { subscription.remove() }
    }.shareIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        replay = 1
    )

    override fun getLogsStream(): Flow<List<LogModel>> = sharedLogsStream

    override fun log(action: String, details: String) {
        try {
            val currentUser = auth.currentUser
            val performedBy = currentUser?.email ?: currentUser?.uid ?: "System"

            val logData = hashMapOf(
                "action" to action,
                "details" to details,
                "performedBy" to performedBy,
                "timestamp" to Timestamp.now()
            )

            firestore.collection("system_logs").add(logData)
                .addOnFailureListener { e ->
                    Log.w("AuditLogRepository", "Failed to record audit log: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            Log.w("AuditLogRepository", "Error executing audit log: ${e.localizedMessage}")
        }
    }
}
