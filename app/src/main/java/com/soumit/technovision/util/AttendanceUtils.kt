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

    private val DATE_SPLIT_REGEX = Regex("[/\\-]")
    private val SLOT_DASH_REGEX = Regex("\\s*-\\s*")
    private val SLOT_AMPM_REGEX = Regex("(?i)(\\d)(am|pm)")
    private val SLOT_LEADING_ZERO_REGEX = Regex("(^|\\s)0(\\d:)")
    private val MULTI_SPACE_REGEX = Regex("\\s+")
    private val LOG_PATTERN_1 = Regex("""(?i)attendance for\s+([0-9\-/]+)\s*\[([^]]+)\]""")
    private val LOG_PATTERN_2 = Regex("""(?i)request for\s*\[([^]]+)\]\s+on\s+([0-9\-/]+)""")
    private val DATE_FORMATTER_DD_MM_YYYY = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    private val DATE_FORMATTER_DD_MMM_YYYY = DateTimeFormatter.ofPattern("dd MMM yyyy")

    fun parseRecordDate(dateStr: String): Triple<Int, Int, Int>? {
        if (dateStr.isBlank()) return null
        val normalized = normalizeDigits(dateStr.trim())
        val parts = normalized.split(DATE_SPLIT_REGEX)
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
            val parts = normalized.split(DATE_SPLIT_REGEX)
            if (parts.size == 3) {
                if (parts[0].length == 4) {
                    val y = parts[0].toInt().let { if (it in 0..99) it + 2000 else it }
                    LocalDate.of(y, parts[1].toInt(), parts[2].toInt())
                } else {
                    val y = parts[2].toInt().let { if (it in 0..99) it + 2000 else it }
                    LocalDate.of(y, parts[1].toInt(), parts[0].toInt())
                }
            } else {
                LocalDate.parse(normalized, DATE_FORMATTER_DD_MM_YYYY)
            }
        } catch (_: Exception) {
            LocalDate.MIN
        }
    }

    fun formatDateHeading(dateStr: String): String {
        return try {
            val normalized = normalizeDigits(dateStr.trim())
            val parts = normalized.split(DATE_SPLIT_REGEX)
            val localDate = if (parts.size == 3) {
                if (parts[0].length == 4) {
                    val y = parts[0].toInt().let { if (it in 0..99) it + 2000 else it }
                    LocalDate.of(y, parts[1].toInt(), parts[2].toInt())
                } else {
                    val y = parts[2].toInt().let { if (it in 0..99) it + 2000 else it }
                    LocalDate.of(y, parts[1].toInt(), parts[0].toInt())
                }
            } else {
                LocalDate.parse(normalized, DATE_FORMATTER_DD_MM_YYYY)
            }
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            val dayString = when (localDate) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> localDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            }
            val formattedDate = localDate.format(DATE_FORMATTER_DD_MMM_YYYY)
            "$dayString, $formattedDate"
        } catch (_: Exception) {
            dateStr
        }
    }

    fun isPlaceholderName(name: String?): Boolean {
        if (name.isNullOrBlank()) return true
        val lower = name.trim().lowercase()
        return lower in listOf(
            "faculty", "teacher", "unknown", "unknown faculty", "unknown teacher",
            "admin", "administrator", "staff", "user", "null", "none", "n/a", "another user"
        )
    }

    fun formatEmailPrefixToName(emailOrPrefix: String): String {
        val prefix = if (emailOrPrefix.contains("@")) emailOrPrefix.substringBefore("@") else emailOrPrefix
        if (isPlaceholderName(prefix)) return ""
        return prefix
            .replace(".", " ")
            .replace("_", " ")
            .replace("-", " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    fun normalizeDate(dateStr: String): String {
        val parsed = parseRecordDate(dateStr)
        return if (parsed != null) {
            String.format(Locale.US, "%02d-%02d-%04d", parsed.third, parsed.second + 1, parsed.first)
        } else {
            dateStr.trim().replace('/', '-')
        }
    }

    fun normalizeTimeSlot(timeSlot: String): String {
        var slot = timeSlot.replace('–', '-').replace('—', '-')
        slot = slot.replace(SLOT_DASH_REGEX, " - ")
        slot = slot.replace(SLOT_AMPM_REGEX, "$1 $2")
        slot = slot.replace(SLOT_LEADING_ZERO_REGEX, "$1$2")
        return slot.replace(MULTI_SPACE_REGEX, " ").trim().lowercase()
    }

    fun normalizeSlotKey(date: String, timeSlot: String): String {
        val cleanDate = normalizeDate(date)
        val cleanSlot = normalizeTimeSlot(timeSlot)
        return "${cleanDate}_${cleanSlot}"
    }

    fun extractDateAndSlotFromLog(details: String): Pair<String, String>? {
        if (details.isBlank()) return null
        val m1 = LOG_PATTERN_1.find(details)
        if (m1 != null) {
            return Pair(m1.groupValues[1].trim(), m1.groupValues[2].trim())
        }
        val m2 = LOG_PATTERN_2.find(details)
        if (m2 != null) {
            return Pair(m2.groupValues[2].trim(), m2.groupValues[1].trim())
        }
        return null
    }

    fun resolveTeacherName(
        record: AttendanceRecordModel,
        userMap: Map<String, String>,
        teacherAttendanceMap: Map<String, String> = emptyMap(),
        fallbackTeacherMap: Map<String, String> = emptyMap()
    ): String {
        // 1. Direct match from teacher attendance collection (by date & time slot or document ID)
        if (teacherAttendanceMap.isNotEmpty()) {
            val normKey = normalizeSlotKey(record.date, record.timeSlot)
            val rawKey = "${record.date}_${record.timeSlot}"
            val rawCleanDate = record.date.trim().replace('/', '-')
            val rawCleanSlot = record.timeSlot.replace(Regex("\\s+"), " ").trim().lowercase()
            val legacyKey = "${rawCleanDate}_${rawCleanSlot}"

            val fromTeacherAttendance = teacherAttendanceMap[normKey]
                ?: teacherAttendanceMap[legacyKey]
                ?: teacherAttendanceMap[rawKey]
                ?: (if (record.id.isNotBlank()) teacherAttendanceMap[record.id] else null)

            if (!fromTeacherAttendance.isNullOrBlank() && !isPlaceholderName(fromTeacherAttendance)) {
                return fromTeacherAttendance
            }
        }

        // 2. Direct match in userMap by UID
        val uid = record.submittedByUid.trim()
        if (uid.isNotBlank() && userMap.containsKey(uid)) {
            val name = userMap[uid]
            if (!name.isNullOrBlank() && !isPlaceholderName(name)) return name
        }

        // 3. Direct match in userMap by Email
        val email = record.submittedByEmail.trim()
        if (email.isNotBlank()) {
            val mapped = userMap[email] ?: userMap[email.lowercase()]
            if (!mapped.isNullOrBlank() && !isPlaceholderName(mapped)) return mapped
        }

        // 4. Check if submittedBy itself is an email or UID in userMap
        val submitted = record.submittedBy.trim()
        if (submitted.isNotBlank()) {
            val mapped = userMap[submitted] ?: userMap[submitted.lowercase()]
            if (!mapped.isNullOrBlank() && !isPlaceholderName(mapped)) return mapped
        }

        // 5. Check if submittedBy on the record is already an authentic teacher name
        if (submitted.isNotBlank() &&
            !isPlaceholderName(submitted) &&
            !submitted.contains("@") &&
            submitted.length < 40
        ) {
            return submitted
        }

        // 6. Check if submittedByEmail was actually a name (e.g. legacy takenBy mapped to submittedByEmail)
        if (email.isNotBlank() &&
            !isPlaceholderName(email) &&
            !email.contains("@") &&
            email.length < 40
        ) {
            return email
        }

        // 7. Format name from submittedBy if it's an email address
        if (submitted.contains("@")) {
            val formatted = formatEmailPrefixToName(submitted)
            if (formatted.isNotBlank() && !isPlaceholderName(formatted)) {
                return formatted
            }
        }

        // 8. Format name from submittedByEmail if it's an email address
        if (email.contains("@")) {
            val formatted = formatEmailPrefixToName(email)
            if (formatted.isNotBlank() && !isPlaceholderName(formatted)) {
                return formatted
            }
        }

        // 9. If submittedBy is not blank and not a generic placeholder
        if (submitted.isNotBlank() && !isPlaceholderName(submitted)) {
            return submitted
        }

        // 10. If submittedByUid or submitted is non-blank, check userMap by prefix/lowercase
        if (uid.isNotBlank()) {
            userMap[uid.lowercase()]?.takeIf { !isPlaceholderName(it) }?.let { return it }
        }

        // 11. Fallback match from pending teacher attendance requests or system audit logs
        if (fallbackTeacherMap.isNotEmpty()) {
            val normKey = normalizeSlotKey(record.date, record.timeSlot)
            val rawKey = "${record.date}_${record.timeSlot}"
            val rawCleanDate = record.date.trim().replace('/', '-')
            val rawCleanSlot = record.timeSlot.replace(Regex("\\s+"), " ").trim().lowercase()
            val legacyKey = "${rawCleanDate}_${rawCleanSlot}"

            val fromFallback = fallbackTeacherMap[normKey]
                ?: fallbackTeacherMap[legacyKey]
                ?: fallbackTeacherMap[rawKey]
                ?: (if (record.id.isNotBlank()) fallbackTeacherMap[record.id] else null)

            if (!fromFallback.isNullOrBlank() && !isPlaceholderName(fromFallback)) {
                return fromFallback
            }

            parseRecordDate(record.date)?.let { parsed ->
                val canonicalDate = "%02d-%02d-%04d".format(parsed.third, parsed.second + 1, parsed.first)
                val canonicalKey = normalizeSlotKey(canonicalDate, record.timeSlot)
                val canonicalMatch = fallbackTeacherMap[canonicalKey]
                if (!canonicalMatch.isNullOrBlank() && !isPlaceholderName(canonicalMatch)) {
                    return canonicalMatch
                }
            }
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
