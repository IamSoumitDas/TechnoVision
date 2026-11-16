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

    @Test
    fun billingUtils_handlesIsoAndTwoDigitYearsCorrectly() {
        // ISO format YYYY-MM-DD
        val (monthsIso, amountIso) = BillingUtils.calculateBillingMonths("2024-01-15", 500L)
        val (monthsStandard, amountStandard) = BillingUtils.calculateBillingMonths("15/01/2024", 500L)
        assertEquals(monthsStandard, monthsIso)
        assertEquals(amountStandard, amountIso)

        // 2-digit year (e.g. 24 -> 2024)
        val (months2Digit, amount2Digit) = BillingUtils.calculateBillingMonths("15/01/24", 500L)
        assertEquals(monthsStandard, months2Digit)
        assertEquals(amountStandard, amount2Digit)
    }

    @Test
    fun parseSlotDurationHours_handlesOvernightAndStandardDurations() {
        val stdDuration = com.soumit.technovision.util.AttendanceUtils.parseSlotDurationHours("9:00 AM - 11:00 AM")
        assertEquals(2.0, stdDuration, 0.01)

        val afternoonDuration = com.soumit.technovision.util.AttendanceUtils.parseSlotDurationHours("11:00 AM - 1:00 PM")
        assertEquals(2.0, afternoonDuration, 0.01)

        val crossMidnightDuration = com.soumit.technovision.util.AttendanceUtils.parseSlotDurationHours("11:00 PM - 1:00 AM")
        assertEquals(2.0, crossMidnightDuration, 0.01)
    }

    @Test
    fun resolveTeacherName_authoritativeAttribution() {
        val userMap = mapOf(
            "faculty1_uid" to "Rahul Sharma",
            "faculty2_uid" to "Amitabha Roy",
            "faculty1@technovision.com" to "Rahul Sharma",
            "faculty2@technovision.com" to "Amitabha Roy"
        )

        // Case 1: Record taken by faculty 1 (UID present)
        val record1 = com.soumit.technovision.data.model.AttendanceRecordModel(
            date = "15-11-2026",
            timeSlot = "9:00 AM - 11:00 AM",
            submittedByUid = "faculty1_uid",
            submittedBy = "Faculty",
            submittedByEmail = "faculty1@technovision.com"
        )
        val resolved1 = com.soumit.technovision.ui.screens.attendance.resolveTeacherName(record1, userMap)
        assertEquals("Rahul Sharma", resolved1)

        // Case 2: Record taken by faculty 2 (name direct)
        val record2 = com.soumit.technovision.data.model.AttendanceRecordModel(
            date = "15-11-2026",
            timeSlot = "9:00 AM - 11:00 AM",
            submittedBy = "Amitabha Roy"
        )
        val resolved2 = com.soumit.technovision.ui.screens.attendance.resolveTeacherName(record2, emptyMap())
        assertEquals("Amitabha Roy", resolved2)

        // Case 3: Email formatting when userMap not populated
        val record3 = com.soumit.technovision.data.model.AttendanceRecordModel(
            date = "15-11-2026",
            timeSlot = "9:00 AM - 11:00 AM",
            submittedByEmail = "priya.das@technovision.com"
        )
        val resolved3 = com.soumit.technovision.ui.screens.attendance.resolveTeacherName(record3, emptyMap())
        assertEquals("Priya Das", resolved3)
    }

    @Test
    fun resolveAdminName_directResolution() {
        val userMap = mapOf(
            "admin_uid" to "Soumit Das",
            "admin@technovision.com" to "Soumit Das"
        )
        val resolved = com.soumit.technovision.ui.screens.attendance.resolveAdminName("admin_uid", userMap)
        assertEquals("Soumit Das", resolved)

        val resolvedFromEmail = com.soumit.technovision.ui.screens.attendance.resolveAdminName("owner.soumit@technovision.com", emptyMap())
        assertEquals("Owner Soumit", resolvedFromEmail)
    }

    @Test
    fun attendanceUtils_directFunctionsMatch() {
        val stdDuration = com.soumit.technovision.util.AttendanceUtils.parseSlotDurationHours("9:00 AM - 11:00 AM")
        assertEquals(2.0, stdDuration, 0.01)

        val resolved = com.soumit.technovision.util.AttendanceUtils.resolveAdminName("admin_uid", mapOf("admin_uid" to "Soumit Das"))
        assertEquals("Soumit Das", resolved)
    }

    @Test
    fun billingUtils_handlesNonAsciiDigits() {
        // Arabic-Indic digits: ١٥/٠١/٢٠٢٤ (15/01/2024)
        val arabicDate = "١٥/٠١/٢٠٢٤"
        val (monthsArabic, amountArabic) = BillingUtils.calculateBillingMonths(arabicDate, 500L)
        val (monthsAscii, amountAscii) = BillingUtils.calculateBillingMonths("15/01/2024", 500L)
        assertEquals(monthsAscii, monthsArabic)
        assertEquals(amountAscii, amountArabic)
        assertTrue(monthsArabic > 0)

        // Devanagari digits: १५/०१/२०२४ (15/01/2024)
        val devanagariDate = "१५/०१/२०२४"
        val (monthsDev, amountDev) = BillingUtils.calculateBillingMonths(devanagariDate, 500L)
        assertEquals(monthsAscii, monthsDev)
        assertEquals(amountAscii, amountDev)
    }

    @Test
    fun attendanceUtils_parseRecordDate_handlesNonAsciiDigits() {
        // Arabic-Indic digits: ١٥-١١-٢٠٢٦ (15-11-2026)
        val triple = com.soumit.technovision.util.AttendanceUtils.parseRecordDate("١٥-١١-٢٠٢٦")
        assertNotNull(triple)
        assertEquals(2026, triple!!.first)
        assertEquals(10, triple.second) // 0-based month for November
        assertEquals(15, triple.third)
    }
}