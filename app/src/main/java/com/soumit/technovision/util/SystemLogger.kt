package com.soumit.technovision.util

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import com.soumit.technovision.TechnoVisionApplication

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

            val logData = hashMapOf(
                "action" to action,
                "details" to details,
                "performedBy" to performedBy,
                "timestamp" to Timestamp.now()
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
