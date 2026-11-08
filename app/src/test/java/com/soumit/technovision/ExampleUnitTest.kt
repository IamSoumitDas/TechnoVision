package com.soumit.technovision

import com.soumit.technovision.util.BillingUtils
import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun billingUtils_handlesSlashAndHyphenDates() {
        val (monthsSlash, amountSlash) = BillingUtils.calculateBillingMonths("15/01/2024", 500L)
        val (monthsHyphen, amountHyphen) = BillingUtils.calculateBillingMonths("15-01-2024", 500L)
        assertEquals(monthsSlash, monthsHyphen)
        assertEquals(amountSlash, amountHyphen)
        assertTrue(monthsSlash > 0)
    }

    @Test
    fun billingUtils_handlesEmptyAndMalformedDatesGracefully() {
        val (monthsEmpty, amountEmpty) = BillingUtils.calculateBillingMonths("", 500L)
        assertEquals(0, monthsEmpty)
        assertEquals(0L, amountEmpty)

        val (monthsBad, amountBad) = BillingUtils.calculateBillingMonths("invalid-date", 500L)
        assertEquals(0, monthsBad)
        assertEquals(0L, amountBad)
    }
}