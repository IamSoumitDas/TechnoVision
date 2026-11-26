package com.soumit.technovision.util

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.soumit.technovision.TechnoVisionApplication
import java.util.Date

object SystemLogger {
    fun log(action: String, details: String) {
        try {
            TechnoVisionApplication.instance?.container?.auditLogRepository?.log(action, details)
            if (TechnoVisionApplication.instance != null) {
                return
            }
        } catch (_: Exception) {
            // Fallback to direct Firestore below
        }
        try {
            val db = FirebaseFirestore.getInstance()
            val currentUser = FirebaseAuth.getInstance().currentUser
            val performedBy = currentUser?.email ?: currentUser?.uid ?: "System"
            val now = Timestamp.now()
            val expireAt = Timestamp(Date(System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000)) // 90 days TTL

            val logData = hashMapOf(
                "action" to action,
                "details" to details,
                "performedBy" to performedBy,
                "timestamp" to now,
                "expireAt" to expireAt
            )

            db.collection("system_logs").add(logData)
                .addOnFailureListener { e ->
                    Log.w("SystemLogger", "Failed to record audit log: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            Log.w("SystemLogger", "Error executing audit log: ${e.localizedMessage}")
        }
    }
}
