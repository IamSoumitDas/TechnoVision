package com.soumit.technovision.util

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object SystemLogger {
    fun log(action: String, details: String) {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val performedBy = currentUser?.email ?: currentUser?.uid ?: "System"

        val logData = hashMapOf(
            "action" to action,
            "details" to details,
            "performedBy" to performedBy,
            "timestamp" to Timestamp.now()
        )

        // Write directly to Firestore
        db.collection("system_logs").add(logData)
    }
}
