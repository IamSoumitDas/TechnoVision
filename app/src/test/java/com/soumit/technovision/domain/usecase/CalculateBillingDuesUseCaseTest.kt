package com.soumit.technovision.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalculateBillingDuesUseCaseTest {

    private lateinit var useCase: CalculateBillingDuesUseCase

    @Before
    fun setUp() {
        useCase = CalculateBillingDuesUseCase()
    }

    @Test
    fun invoke_withBlankAdmissionDate_returnsZeroDues() {
        val result = useCase(
            admissionDate = "",
            monthlyInstallment = 500L,
            totalPaid = 0L
        )

        assertEquals(0, result.billableMonths)
        assertEquals(0L, result.expectedAmount)
        assertEquals(0L, result.totalPaid)
        assertEquals(0L, result.outstandingBalance)
    }

    @Test
    fun invoke_whenPaidExceedsExpected_balanceIsZero() {
        val result = useCase(
            admissionDate = "15/01/2024",
            monthlyInstallment = 500L,
            totalPaid = 100_000L
        )

        assertTrue(result.billableMonths > 0)
        assertTrue(result.expectedAmount > 0L)
        assertEquals(100_000L, result.totalPaid)
        assertEquals(0L, result.outstandingBalance)
    }

    @Test
    fun invoke_whenPartiallyPaid_balanceIsExpectedMinusPaid() {
        val result = useCase(
            admissionDate = "15/01/2024",
            monthlyInstallment = 500L,
            totalPaid = 500L
        )

        assertTrue(result.billableMonths > 0)
        val expectedBalance = (result.expectedAmount - 500L).coerceAtLeast(0L)
        assertEquals(expectedBalance, result.outstandingBalance)
    }
}
