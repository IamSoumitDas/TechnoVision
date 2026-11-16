package com.soumit.technovision.util

import com.soumit.technovision.data.model.AttendanceRecordModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

object AttendanceUtils {

    val BATCH_TIME_SLOTS = listOf(
        "9:00 AM - 11:00 AM",
        "11:00 AM - 1:00 PM",
        "1:00 PM - 3:00 PM",
        "3:00 PM - 5:00 PM",
        "5:00 PM - 7:00 PM",
        "7:00 PM - 9:00 PM",
        "General / Other"
    )

    fun getAutomaticTimeSlot(): String {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val totalMinutes = hour * 60 + minute

        return when {
            totalMinutes < 11 * 60 -> "9:00 AM - 11:00 AM"
            totalMinutes < 13 * 60 -> "11:00 AM - 1:00 PM"
            totalMinutes < 15 * 60 -> "1:00 PM - 3:00 PM"
            totalMinutes < 17 * 60 -> "3:00 PM - 5:00 PM"
            totalMinutes < 19 * 60 -> "5:00 PM - 7:00 PM"
            totalMinutes < 21 * 60 -> "7:00 PM - 9:00 PM"
            else -> "7:00 PM - 9:00 PM"
        }
    }

    fun getTodayDayName(): String {
        val calendar = Calendar.getInstance()
        val dayOfWeekIndex = calendar.get(Calendar.DAY_OF_WEEK)
        val daysArray = arrayOf("", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        return if (dayOfWeekIndex in daysArray.indices) daysArray[dayOfWeekIndex] else "Sunday"
    }

    fun parseSlotDurationHours(timeSlot: String): Double {
        return try {
            val parts = timeSlot.split("-").map { it.trim() }
            if (parts.size == 2) {
                val format = SimpleDateFormat("h:mm a", Locale.US)
                val start = format.parse(parts[0])
                val end = format.parse(parts[1])
                if (start != null && end != null) {
                    var diff = end.time - start.time
                    if (diff < 0) {
                        diff += 24 * 60 * 60 * 1000L
                    }
                    val hours = diff.toDouble() / (1000 * 60 * 60)
                    if (hours > 0) hours else 2.0
                } else 2.0
            } else 2.0
        } catch (_: Exception) {
            2.0
        }
    }

    fun normalizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(ch)
            } else if (Character.isDigit(ch)) {
                val num = Character.getNumericValue(ch)
                if (num in 0..9) sb.append(num) else sb.append(ch)
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun parseRecordDate(dateStr: String): Triple<Int, Int, Int>? {
        if (dateStr.isBlank()) return null
        val normalized = normalizeDigits(dateStr.trim())
        val parts = normalized.split(Regex("[/\\-]"))
        if (parts.size == 3) {
            val d: Int?
            val m: Int?
            var y: Int?

            if (parts[0].length == 4) {
                // YYYY-MM-DD
                y = parts[0].toIntOrNull()
                m = parts[1].toIntOrNull()
                d = parts[2].toIntOrNull()
            } else {
                // DD-MM-YYYY
                d = parts[0].toIntOrNull()
                m = parts[1].toIntOrNull()
                y = parts[2].toIntOrNull()
            }

            if (d != null && m != null && y != null) {
                if (y in 0..99) y += 2000
                if (m in 1..12 && d in 1..31 && y >= 1900) {
                    return Triple(y, m - 1, d)
                }
            }
        }
        return null
    }

    fun parseDateForSorting(dateStr: String): LocalDate {
        return try {
            val normalized = normalizeDigits(dateStr.trim())
            val parts = normalized.split(Regex("[/\\-]"))
            if (parts.size == 3) {
                if (parts[0].length == 4) {
                    val y = parts[0].toInt().let { if (it in 0..99) it + 2000 else it }
                    LocalDate.of(y, parts[1].toInt(), parts[2].toInt())
                } else {
                    val y = parts[2].toInt().let { if (it in 0..99) it + 2000 else it }
                    LocalDate.of(y, parts[1].toInt(), parts[0].toInt())
                }
            } else {
                LocalDate.parse(normalized, DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            }
        } catch (_: Exception) {
            LocalDate.MIN
        }
    }

    fun formatDateHeading(dateStr: String): String {
        return try {
            val normalized = normalizeDigits(dateStr.trim())
            val parts = normalized.split(Regex("[/\\-]"))
            val localDate = if (parts.size == 3) {
                if (parts[0].length == 4) {
                    val y = parts[0].toInt().let { if (it in 0..99) it + 2000 else it }
                    LocalDate.of(y, parts[1].toInt(), parts[2].toInt())
                } else {
                    val y = parts[2].toInt().let { if (it in 0..99) it + 2000 else it }
                    LocalDate.of(y, parts[1].toInt(), parts[0].toInt())
                }
            } else {
                LocalDate.parse(normalized, DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            }
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            val dayString = when (localDate) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> localDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            }
            val formattedDate = localDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
            "$dayString, $formattedDate"
        } catch (_: Exception) {
            dateStr
        }
    }

    fun resolveTeacherName(
        record: AttendanceRecordModel,
        userMap: Map<String, String>
    ): String {
        if (record.submittedByUid.isNotBlank() && userMap.containsKey(record.submittedByUid)) {
            val name = userMap[record.submittedByUid]
            if (!name.isNullOrBlank()) return name
        }

        if (record.submittedByEmail.isNotBlank()) {
            val mapped = userMap[record.submittedByEmail] ?: userMap[record.submittedByEmail.lowercase()]
            if (!mapped.isNullOrBlank()) return mapped
        }

        if (record.submittedBy.isNotBlank()) {
            val mapped = userMap[record.submittedBy] ?: userMap[record.submittedBy.lowercase()]
            if (!mapped.isNullOrBlank()) return mapped
        }

        if (record.submittedBy.isNotBlank() &&
            !record.submittedBy.equals("Faculty", ignoreCase = true) &&
            !record.submittedBy.contains("@") &&
            record.submittedBy.length < 35
        ) {
            return record.submittedBy
        }

        if (record.submittedBy.contains("@")) {
            val prefix = record.submittedBy.substringBefore("@")
            if (!prefix.equals("faculty", ignoreCase = true)) {
                return prefix
                    .replace(".", " ")
                    .replace("_", " ")
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
        }

        if (record.submittedByEmail.contains("@")) {
            val prefix = record.submittedByEmail.substringBefore("@")
            if (!prefix.equals("faculty", ignoreCase = true)) {
                return prefix
                    .replace(".", " ")
                    .replace("_", " ")
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
        }

        if (record.submittedBy.isNotBlank()) {
            return record.submittedBy
        }

        return "Unknown Faculty"
    }

    fun resolveAdminName(
        rawName: String,
        userMap: Map<String, String>
    ): String {
        if (rawName.isNotBlank() && userMap.containsKey(rawName)) {
            val mapped = userMap[rawName]
            if (!mapped.isNullOrBlank()) return mapped
        }
        if (rawName.isNotBlank() && userMap.containsKey(rawName.lowercase())) {
            val mapped = userMap[rawName.lowercase()]
            if (!mapped.isNullOrBlank()) return mapped
        }

        if (rawName.isNotBlank() &&
            !rawName.equals("Admin", ignoreCase = true) &&
            !rawName.equals("Faculty", ignoreCase = true) &&
            !rawName.contains("@") &&
            rawName.length < 35
        ) {
            return rawName
        }

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

        return "Admin"
    }
}
