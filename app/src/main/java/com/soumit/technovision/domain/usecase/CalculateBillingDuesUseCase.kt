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
    operator fun invoke(
        admissionDate: String,
        monthlyInstallment: Long,
        totalPaid: Long = 0L,
        currentYear: Int = -1,
        currentMonth: Int = -1
    ): BillingDues {
        val (months, expected) = BillingUtils.calculateBillingMonths(
            admissionDateStr = admissionDate,
            installment = monthlyInstallment,
            currentYear = currentYear,
            currentMonth = currentMonth
        )
        val balance = (expected - totalPaid).coerceAtLeast(0L)
        return BillingDues(
            billableMonths = months,
            expectedAmount = expected,
            totalPaid = totalPaid,
            outstandingBalance = balance
        )
    }
}
