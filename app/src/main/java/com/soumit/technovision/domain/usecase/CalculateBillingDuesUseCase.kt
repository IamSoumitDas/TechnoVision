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
    private val memoCache = java.util.concurrent.ConcurrentHashMap<String, BillingDues>()

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
        val cacheKey = "$admissionDate|$monthlyInstallment|$totalPaid|$resolvedYear|$resolvedMonth"
        memoCache[cacheKey]?.let { return it }

        val (months, expected) = BillingUtils.calculateBillingMonths(
            admissionDateStr = admissionDate,
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
        memoCache[cacheKey] = result
        return result
    }
}
