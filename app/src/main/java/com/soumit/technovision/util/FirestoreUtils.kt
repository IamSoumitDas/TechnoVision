package com.soumit.technovision.util

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Safely parses any numerical or string value to Long without throwing ClassCastException.
 */
fun parseSafeLong(value: Any?, default: Long = 0L): Long {
    val obj = value ?: return default
    return when (obj) {
        is Number -> obj.toLong()
        is String -> obj.trim().toLongOrNull() ?: default
        else -> default
    }
}

/**
 * Safely extracts a Long value from a Firestore DocumentSnapshot without throwing
 * ClassCastException if the field is stored as a Double, Float, Int, or String.
 */
fun DocumentSnapshot.getSafeLong(field: String, default: Long = 0L): Long =
    parseSafeLong(get(field), default)
