package com.soumit.technovision.util

import java.util.Calendar

object BillingUtils {
    /**
     * Normalizes non-ASCII numerals (e.g. Eastern Arabic, Devanagari, Bengali) to standard ASCII 0-9 digits.
     */
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

    /**
     * Calculates billable months due and expected total fees.
     * Rule:
     * - Admitted <= 20th counts the current month as start; > 20th shifts start to next month.
     * - Subtracts 1 month to match your exact billing requirement.
     */
    fun calculateBillingMonths(
        admissionDateStr: String,
        installment: Long,
        currentYear: Int = -1,
        currentMonth: Int = -1
    ): Pair<Int, Long> {
        if (admissionDateStr.isBlank()) return Pair(0, 0L)

        try {
            val normalized = normalizeDigits(admissionDateStr.trim())
            val parts = normalized.split(Regex("[/\\-]"))
            if (parts.size == 3) {
                val day: Int
                val admissionMonth: Int
                var admissionYear: Int

                if (parts[0].length == 4) {
                    // Format: YYYY-MM-DD or YYYY/MM/DD
                    admissionYear = parts[0].toInt()
                    admissionMonth = parts[1].toInt()
                    day = parts[2].toInt()
                } else {
                    // Format: DD-MM-YYYY or DD/MM/YYYY
                    day = parts[0].toInt()
                    admissionMonth = parts[1].toInt()
                    admissionYear = parts[2].toInt()
                }

                // Expand 2-digit years (e.g., 26 -> 2026)
                if (admissionYear in 0..99) {
                    admissionYear += 2000
                }

                if (admissionMonth !in 1..12 || day !in 1..31 || admissionYear < 1900) {
                    return Pair(0, 0L)
                }

                val curYear: Int
                val curMonth: Int
                if (currentYear > 0 && currentMonth > 0) {
                    curYear = currentYear
                    curMonth = currentMonth
                } else {
                    val calendar = Calendar.getInstance()
                    curYear = calendar.get(Calendar.YEAR)
                    curMonth = calendar.get(Calendar.MONTH) + 1 // 1-based index
                }

                // Determine start month index based on <= 20th rule
                var startMonthIndex = admissionYear * 12 + admissionMonth
                if (day > 20) {
                    startMonthIndex += 1 // Shift start to next month if admitted after 20th
                }

                val currentMonthIndex = curYear * 12 + curMonth
                val rawMonthsElapsed = (currentMonthIndex - startMonthIndex) + 1

                // Apply the -1 adjustment rule
                val adjustedMonths = rawMonthsElapsed - 1

                val validMonths = if (adjustedMonths > 0) adjustedMonths else 0
                return Pair(validMonths, validMonths * installment)
            }
        } catch (_: Exception) {
        }
        return Pair(0, 0L)
    }
}
