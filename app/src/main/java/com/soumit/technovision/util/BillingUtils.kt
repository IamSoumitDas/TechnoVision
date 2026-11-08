package com.soumit.technovision.util

import java.util.Calendar

object BillingUtils {
    /**
     * Calculates billable months due and expected total fees.
     * Rule:
     * - Admitted <= 20th counts the current month as start; > 20th shifts start to next month.
     * - Subtracts 1 month to match your exact billing requirement.
     */
    fun calculateBillingMonths(admissionDateStr: String, installment: Long): Pair<Int, Long> {
        if (admissionDateStr.isBlank()) return Pair(0, 0L)

        try {
            val parts = admissionDateStr.trim().split(Regex("[/\\-]"))
            if (parts.size == 3) {
                val day = parts[0].toInt()
                val admissionMonth = parts[1].toInt()
                val admissionYear = parts[2].toInt()

                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                val currentMonth = calendar.get(Calendar.MONTH) + 1 // 1-based index

                // Determine start month index based on <= 20th rule
                var startMonthIndex = admissionYear * 12 + admissionMonth
                if (day > 20) {
                    startMonthIndex += 1 // Shift start to next month if admitted after 20th
                }

                val currentMonthIndex = currentYear * 12 + currentMonth
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
