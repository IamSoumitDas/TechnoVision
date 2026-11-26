package com.soumit.technovision.domain.usecase

import com.soumit.technovision.util.BillingUtils

/**
 * Domain model representing billing dues calculation.
 */
data class BillingDues(
    val billableMonths: Int,
    val expectedAmount: Long,
    val totalPaid: Long,
    val outstandingBalance: Long
)

/**
 * Google-recommended Domain Layer UseCase encapsulating the billing dues calculation.
 */
class CalculateBillingDuesUseCase {
    private val memoCache = object : java.util.LinkedHashMap<String, BillingDues>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BillingDues>?): Boolean {
            return size > 256
        }
    }

    operator fun invoke(
        admissionDate: String,
        monthlyInstallment: Long,
        totalPaid: Long = 0L,
        currentYear: Int = -1,
        currentMonth: Int = -1
    ): BillingDues {
        val (resolvedYear, resolvedMonth) = if (currentYear > 0 && currentMonth > 0) {
            Pair(currentYear, currentMonth)
        } else {
            val calendar = java.util.Calendar.getInstance()
            Pair(calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH) + 1)
        }
        val cleanAdmissionDate = admissionDate.trim().replace('/', '-')
        val cacheKey = "$cleanAdmissionDate|$monthlyInstallment|$totalPaid|$resolvedYear|$resolvedMonth"
        synchronized(memoCache) {
            memoCache[cacheKey]?.let { return it }
        }

        val (months, expected) = BillingUtils.calculateBillingMonths(
            admissionDateStr = cleanAdmissionDate,
            installment = monthlyInstallment,
            currentYear = resolvedYear,
            currentMonth = resolvedMonth
        )
        val balance = (expected - totalPaid).coerceAtLeast(0L)
        val result = BillingDues(
            billableMonths = months,
            expectedAmount = expected,
            totalPaid = totalPaid,
            outstandingBalance = balance
        )
        synchronized(memoCache) {
            memoCache[cacheKey] = result
        }
        return result
    }
}
