package com.soumit.technovision.data.model

import com.google.firebase.Timestamp

data class LogModel(
    val id: String = "",
    val action: String = "",
    val details: String = "",
    val performedBy: String = "",
    val timestamp: Timestamp? = null
)
