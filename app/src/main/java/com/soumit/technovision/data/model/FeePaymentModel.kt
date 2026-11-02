package com.soumit.technovision.data.model

import com.google.firebase.Timestamp

data class FeePaymentModel(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val amountPaid: Long = 0L,
    val paymentMethod: String = "",
    val remarks: String = "",
    val receivedBy: String = "",
    val timestamp: Timestamp? = null
)
