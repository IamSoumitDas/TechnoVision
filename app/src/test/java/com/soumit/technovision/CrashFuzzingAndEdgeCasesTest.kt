package com.soumit.technovision

import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.domain.usecase.CalculateBillingDuesUseCase
import com.soumit.technovision.ui.screens.students.BirthdayUtils
import com.soumit.technovision.util.AttendanceUtils
import com.soumit.technovision.util.BillingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class CrashFuzzingAndEdgeCasesTest {

    // ==========================================
    // 1. BirthdayUtils Edge Cases & Fuzzing
    // ==========================================

    @Test
    fun birthdayUtils_leapYearFeb29_evaluatedInNonLeapYear_doesNotCrash() {
        val leapYearStudent = StudentModel(
            id = "leap1",
            fullName = "Leapling",
            dob = "29/02/2000" // Born on leap day
        )
        // 2025 is NOT a leap year
        val nonLeapYearDate = LocalDate.of(2025, 6, 15)
        val item = BirthdayUtils.calculateBirthdayItem(leapYearStudent, nonLeapYearDate)
        assertNotNull(item)
        assertEquals(28, item!!.nextBirthdayDate.dayOfMonth)
        assertEquals(2, item.nextBirthdayDate.monthValue)
        assertEquals(2026, item.nextBirthdayDate.year)
    }

    @Test
    fun birthdayUtils_fuzzingMalformedDates_doesNotCrash() {
        val malformedDates = listOf(
            "",
            "   ",
            "null",
            "31/02/2024", // Feb 31
            "00/00/0000",
            "32/13/2020",
            "99/99/9999",
            "-1/-1/2020",
            "2024-13-40",
            "invalid/date/format",
            "15//2024",
            "/15/2024",
            "15/01/",
            "15-01",
            "15.01",
            "2024.01.15",
            "15 01 2024",
            "undefined",
            "NaN",
            "!@#\$%^&*()",
            "\n\t\r",
            "31-06-2023" // June only has 30 days
        )

        for (date in malformedDates) {
            val student = StudentModel(id = "test", fullName = "Test", dob = date)
            val parsed = BirthdayUtils.parseDob(date)
            // It should safely return null or a valid ParsedDate, never throw
            val item = BirthdayUtils.calculateBirthdayItem(student, LocalDate.now())
            // Should safely return null or valid item, never throw
        }
    }

    @Test
    fun birthdayUtils_birthdayToday_calculationIsAccurate() {
        val today = LocalDate.of(2026, 11, 18)
        val student = StudentModel(
            id = "today_student",
            fullName = "Today Birthday",
            dob = "18/11/2010"
        )
        val item = BirthdayUtils.calculateBirthdayItem(student, today)
        assertNotNull(item)
        assertTrue(item!!.isToday)
        assertEquals(0L, item.daysUntilNextBirthday)
        assertEquals(16, item.turningAge)
    }

    @Test
    fun birthdayUtils_birthdayTomorrow_daysUntilNextBirthdayIsOne() {
        val today = LocalDate.of(2026, 11, 18)
        val student = StudentModel(
            id = "tomorrow_student",
            fullName = "Tomorrow Birthday",
            dob = "19/11/2010"
        )
        val item = BirthdayUtils.calculateBirthdayItem(student, today)
        assertNotNull(item)
        assertFalse(item!!.isToday)
        assertEquals(1L, item.daysUntilNextBirthday)
        assertEquals(16, item.turningAge)
    }

    @Test
    fun birthdayUtils_birthdayYesterday_nextBirthdayIsNextYear() {
        val today = LocalDate.of(2026, 11, 18)
        val student = StudentModel(
            id = "yesterday_student",
            fullName = "Yesterday Birthday",
            dob = "17/11/2010"
        )
        val item = BirthdayUtils.calculateBirthdayItem(student, today)
        assertNotNull(item)
        assertFalse(item!!.isToday)
        assertTrue(item!!.daysUntilNextBirthday > 300)
        assertEquals(17, item.turningAge)
        assertEquals(2027, item.nextBirthdayDate.year)
    }

    // ==========================================
    // 2. BillingUtils & CalculateBillingDues Edge Cases
    // ==========================================

    @Test
    fun billingUtils_fuzzingAdmissionDates_neverThrows() {
        val testDates = listOf(
            "",
            "   ",
            "invalid",
            "31/02/2024",
            "2024-02-31",
            "99/99/9999",
            "00/00/0000",
            "0/0/0",
            "2024/00/01",
            "2024/01/00",
            "1800-01-01", // year < 1900
            "1900-01-01",
            "2099-12-31",
            "15/01/24",
            "15/01/1924",
            "1/1/24",
            "2024-1-1",
            "2024-12-31T23:59:59",
            "null",
            "15.01.2024",
            "15_01_2024"
        )

        for (d in testDates) {
            val (months, amount) = BillingUtils.calculateBillingMonths(d, 500L, 2026, 11)
            assertTrue("Months should be non-negative for date $d", months >= 0)
            assertTrue("Amount should be non-negative for date $d", amount >= 0L)
        }
    }

    @Test
    fun billingUtils_extremeInstallments_handlesLongGracefully() {
        // Zero installment
        val (m0, a0) = BillingUtils.calculateBillingMonths("15/01/2024", 0L, 2026, 11)
        assertEquals(0L, a0)

        // Negative installment
        val (mNeg, aNeg) = BillingUtils.calculateBillingMonths("15/01/2024", -500L, 2026, 11)
        assertTrue(aNeg <= 0L)

        // Large installment
        val (mLarge, aLarge) = BillingUtils.calculateBillingMonths("15/01/2024", 1_000_000L, 2026, 11)
        assertEquals(mLarge * 1_000_000L, aLarge)
    }

    @Test
    fun billingUtils_day20VersusDay21_cutoffRuleAccurate() {
        // Admitted on 20th: counts current month
        val (m20, _) = BillingUtils.calculateBillingMonths("20/01/2024", 500L, 2024, 5)
        // Admitted on 21st: shifts start to next month (so 1 fewer billable month)
        val (m21, _) = BillingUtils.calculateBillingMonths("21/01/2024", 500L, 2024, 5)
        assertEquals(m20 - 1, m21)
    }

    @Test
    fun calculateBillingDuesUseCase_handlesOverflowAndNegativePaid() {
        val useCase = CalculateBillingDuesUseCase()

        // Negative totalPaid
        val resNeg = useCase("15/01/2024", 500L, -500L)
        assertTrue(resNeg.outstandingBalance >= resNeg.expectedAmount)

        // Huge paid
        val resHuge = useCase("15/01/2024", 500L, Long.MAX_VALUE)
        assertEquals(0L, resHuge.outstandingBalance)
    }

    // ==========================================
    // 3. AttendanceUtils Edge Cases & Fuzzing
    // ==========================================

    @Test
    fun attendanceUtils_fuzzingTimeSlots_neverThrows() {
        val slots = listOf(
            "",
            "   ",
            "9:00 AM - 11:00 AM",
            "11:00 PM - 1:00 AM", // Cross midnight
            "12:00 AM - 2:00 AM",
            "12:00 PM - 2:00 PM",
            "invalid",
            "9:00 - 11:00",
            "9:00 AM",
            "9:00 AM - 11:00 AM - 1:00 PM",
            "25:00 AM - 26:00 AM",
            "General / Other",
            "Special Batch"
        )

        for (s in slots) {
            val duration = AttendanceUtils.parseSlotDurationHours(s)
            assertTrue("Duration should be positive for slot '$s'", duration > 0.0)
        }
    }

    @Test
    fun attendanceUtils_fuzzingDateSorting_neverThrows() {
        val dates = listOf(
            "",
            "   ",
            "invalid",
            "31-02-2024",
            "15/01/2024",
            "2024-01-15",
            "15-01-2024",
            "15/01/24",
            "00-00-0000",
            "99-99-9999"
        )

        for (d in dates) {
            val date = AttendanceUtils.parseDateForSorting(d)
            assertNotNull(date)
            val heading = AttendanceUtils.formatDateHeading(d)
            assertNotNull(heading)
        }
    }

    @Test
    fun attendanceUtils_resolveNames_handlesNullAndEmptyMaps() {
        val record = AttendanceRecordModel(
            date = "18-11-2026",
            timeSlot = "9:00 AM - 11:00 AM",
            submittedByUid = "",
            submittedBy = "",
            submittedByEmail = ""
        )
        val name1 = AttendanceUtils.resolveTeacherName(record, emptyMap())
        assertEquals("Unknown Faculty", name1)

        val adminName = AttendanceUtils.resolveAdminName("", emptyMap())
        assertEquals("Admin", adminName)
    }

    // ==========================================
    // 4. Name Initials Formatting Edge Cases
    // ==========================================

    @Test
    fun nameInitials_emptyOrWhitespace_doesNotCrash() {
        val names = listOf(
            "",
            " ",
            "   ",
            "A",
            "AB",
            "John",
            "John Doe",
            "John Middle Doe",
            "   John   Doe   ",
            "123 456",
            "😊 🎉",
            "Dr. John Doe, Jr."
        )

        for (name in names) {
            // Test BirthdayCalendarScreen style
            val clean = name.trim()
            val initials1 = if (clean.isNotBlank()) {
                val parts = clean.split("\\s+".toRegex())
                if (parts.size > 1) "${parts.first().take(1)}${parts.last().take(1)}".uppercase()
                else parts.first().take(2).uppercase()
            } else {
                "ST"
            }
            assertNotNull(initials1)

            // Test StudentCard style
            val initials2 = name.split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
            assertNotNull(initials2)
        }
    }

    // ==========================================
    // 5. AuthViewModel Tests
    // ==========================================

    private class TestAuthRepo : com.soumit.technovision.data.repository.AuthRepository {
        override val currentUserId: String? = null
        override val currentUser: com.google.firebase.auth.FirebaseUser? = null
        override fun authStateStream() = kotlinx.coroutines.flow.flowOf(null)
        override suspend fun getUserRole(userId: String) = Result.success("owner")
        override suspend fun getCurrentUserName() = Result.success("Owner")
        override suspend fun signIn(email: String, password: String) = Result.success("owner")
        override suspend fun sendPasswordReset(email: String) = Result.success(Unit)
        override fun signOut() {}
    }

    @Test
    fun authViewModel_emptyCredentials_showsErrorWithoutCallingRepo() {
        val vm = com.soumit.technovision.ui.screens.auth.AuthViewModel(TestAuthRepo())
        var successRole: String? = null

        vm.login { successRole = it }

        val state = vm.uiState.value
        assertNull(successRole)
        assertTrue(state.isError)
        assertEquals("Please fill in all fields.", state.statusMessage)
    }

    @Test
    fun authViewModel_invalidEmail_showsValidationMessage() {
        val vm = com.soumit.technovision.ui.screens.auth.AuthViewModel(TestAuthRepo())
        vm.onEmailChanged("not-an-email")
        vm.onPasswordChanged("password123")
        vm.login {}
        val state = vm.uiState.value
        assertTrue(state.isError)
        assertEquals("Please enter a valid email structure (e.g. name@example.com).", state.statusMessage)
    }

    // ==========================================
    // 6. EnrollStudentViewModel Validation Tests
    // ==========================================

    private class TestStudentRepo(
        private val students: MutableList<StudentModel> = mutableListOf()
    ) : com.soumit.technovision.data.repository.StudentRepository {
        override fun getStudentsStream() = kotlinx.coroutines.flow.flowOf(students.toList())
        override suspend fun getStudentsOnce() = Result.success(students.toList())
        override fun getStudentStream(studentId: String) = kotlinx.coroutines.flow.flowOf(students.find { it.id == studentId })
        override suspend fun getStudent(studentId: String) =
            students.find { it.id == studentId }?.let { Result.success(it) } ?: Result.failure(NoSuchElementException())
        override suspend fun enrollStudent(student: StudentModel): Result<String> {
            val id = "gen_${students.size + 1}"
            students.add(student.copy(id = id))
            return Result.success(id)
        }
        override suspend fun updateStudent(student: StudentModel): Result<Unit> = Result.success(Unit)
        override suspend fun deleteStudent(studentId: String, fullName: String): Result<Unit> = Result.success(Unit)
    }

    private class TestAuditLogRepo : com.soumit.technovision.data.repository.AuditLogRepository {
        override fun getLogsStream() = kotlinx.coroutines.flow.flowOf(emptyList<com.soumit.technovision.data.model.LogModel>())
        override fun log(action: String, details: String) {}
    }

    private class TestFeeRepo : com.soumit.technovision.data.repository.FeeRepository {
        override fun getFeePaymentsStream() = kotlinx.coroutines.flow.flowOf(emptyList<com.soumit.technovision.data.model.FeePaymentModel>())
        override fun getPaymentsForStudentStream(studentId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.soumit.technovision.data.model.FeePaymentModel>())
        override suspend fun recordPayment(payment: com.soumit.technovision.data.model.FeePaymentModel) = Result.success(payment.id)
        override suspend fun deletePayment(paymentId: String) = Result.success(Unit)
        override suspend fun syncAllStudentsTotalPaid() = Result.success(Unit)
    }

    @Test
    fun enrollStudentViewModel_validationStopsOnBlankRequiredFields() {
        val vm = com.soumit.technovision.ui.screens.students.EnrollStudentViewModel(
            studentRepository = TestStudentRepo(),
            auditLogRepository = TestAuditLogRepo()
        )

        var enrolledId: String? = null
        vm.enrollStudent { enrolledId = it }

        assertNull(enrolledId)
        val state = vm.uiState.value
        assertEquals("Please fill in all required (*) fields highlighted in red.", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun enrollStudentViewModel_validationRequiresAtLeast2BatchDays() {
        val vm = com.soumit.technovision.ui.screens.students.EnrollStudentViewModel(
            studentRepository = TestStudentRepo(),
            auditLogRepository = TestAuditLogRepo()
        )

        vm.onFullNameChanged("Rohan Sharma")
        vm.onClassChanged("10")
        vm.onSchoolChanged("DPS")
        vm.onGenderChanged("Male")
        vm.onDobChanged("15/05/2010")
        vm.onAdmissionDateChanged("15/01/2024")
        vm.onFatherNameChanged("Father")
        vm.onMotherNameChanged("Mother")
        vm.onPhoneChanged("9876543210")
        // Only 1 day scheduled
        vm.onDayScheduleChanged("Monday", "9:00 AM - 11:00 AM")

        var enrolledId: String? = null
        vm.enrollStudent { enrolledId = it }

        assertNull(enrolledId)
        val state = vm.uiState.value
        assertEquals("Student must have at least 2 batch days scheduled.", state.errorMessage)
    }

    @Test
    fun enrollStudentViewModel_validationRejectsInvalidMonthlyInstallment() {
        val vm = com.soumit.technovision.ui.screens.students.EnrollStudentViewModel(
            studentRepository = TestStudentRepo(),
            auditLogRepository = TestAuditLogRepo()
        )

        vm.onFullNameChanged("Rohan Sharma")
        vm.onClassChanged("10")
        vm.onSchoolChanged("DPS")
        vm.onGenderChanged("Male")
        vm.onDobChanged("15/05/2010")
        vm.onAdmissionDateChanged("15/01/2024")
        vm.onFatherNameChanged("Father")
        vm.onMotherNameChanged("Mother")
        vm.onPhoneChanged("9876543210")
        vm.onDayScheduleChanged("Monday", "9:00 AM - 11:00 AM")
        vm.onDayScheduleChanged("Wednesday", "9:00 AM - 11:00 AM")

        // Test non-numeric installment
        vm.onMonthlyInstallmentChanged("invalid-number")
        vm.enrollStudent {}
        assertEquals("Monthly installment must be a valid positive amount.", vm.uiState.value.errorMessage)

        // Test zero installment
        vm.onMonthlyInstallmentChanged("0")
        vm.enrollStudent {}
        assertEquals("Monthly installment must be a valid positive amount.", vm.uiState.value.errorMessage)

        // Test negative installment
        vm.onMonthlyInstallmentChanged("-500")
        vm.enrollStudent {}
        assertEquals("Monthly installment must be a valid positive amount.", vm.uiState.value.errorMessage)
    }

    // ==========================================
    // 7. StudentViewerViewModel Edge Cases
    // ==========================================

    @Test
    fun studentViewerViewModel_blankStudentId_handlesGracefully() {
        val handle = androidx.lifecycle.SavedStateHandle()
        val vm = com.soumit.technovision.ui.screens.students.StudentViewerViewModel(
            savedStateHandle = handle,
            studentRepository = TestStudentRepo(),
            feeRepository = TestFeeRepo(),
            defaultDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
        )

        val state = vm.uiState.value
        assertEquals("Invalid student ID", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun studentViewerViewModel_studentNotFound_setsErrorMessage() = kotlinx.coroutines.test.runTest {
        val testDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        try {
            val handle = androidx.lifecycle.SavedStateHandle(mapOf("studentId" to "non_existent"))
            val vm = com.soumit.technovision.ui.screens.students.StudentViewerViewModel(
                savedStateHandle = handle,
                studentRepository = TestStudentRepo(),
                feeRepository = TestFeeRepo(),
                defaultDispatcher = testDispatcher
            )

            testScheduler.advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals("Student not found", state.errorMessage)
            assertFalse(state.isLoading)
            assertNull(state.student)
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    // ==========================================
    // 8. FeeCollectionViewModel Edge Cases
    // ==========================================

    private class TestStaffRepo : com.soumit.technovision.data.repository.StaffRepository {
        override fun getStaffStream() = kotlinx.coroutines.flow.flowOf(emptyList<com.soumit.technovision.data.model.StaffModel>())
        override fun getUsersMapStream() = kotlinx.coroutines.flow.flowOf(mapOf("test_uid" to "Soumit"))
        override suspend fun getUsersMap() = Result.success(mapOf("test_uid" to "Soumit"))
        override suspend fun createStaff(fullName: String, email: String, password: String, phone: String, role: String) = Result.success("id")
        override suspend fun updateStaff(id: String, fullName: String, phone: String, role: String) = Result.success(Unit)
        override suspend fun deleteStaff(id: String) = Result.success(Unit)
    }

    @Test
    fun feeCollectionViewModel_invalidAmounts_validationRejection() = kotlinx.coroutines.test.runTest {
        val testDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        try {
            val s1 = StudentModel(id = "s1", fullName = "Student One", admissionDate = "15/01/2024", monthlyInstallment = 500L)
            val vm = com.soumit.technovision.ui.screens.fees.FeeCollectionViewModel(
                studentRepository = TestStudentRepo(mutableListOf(s1)),
                feeRepository = TestFeeRepo(),
                authRepository = TestAuthRepo(),
                auditLogRepository = TestAuditLogRepo(),
                staffRepository = TestStaffRepo()
            )
            testScheduler.advanceUntilIdle()

            // 1. Submit with no student selected
            vm.submitPayment {}
            assertNull(vm.uiState.value.userMessage) // No-op, safely returns

            // 2. Select student
            vm.onSelectStudent(s1)
            testScheduler.advanceUntilIdle()

            // 3. Amount is invalid string
            vm.onAmountChanged("not-a-number")
            vm.submitPayment {}
            assertEquals("Please enter a valid payment amount.", vm.uiState.value.userMessage)

            // 4. Amount is negative
            vm.onAmountChanged("-100")
            vm.submitPayment {}
            assertEquals("Please enter a valid payment amount.", vm.uiState.value.userMessage)

            // 5. Amount is 0
            vm.onAmountChanged("0")
            vm.submitPayment {}
            assertEquals("Please enter a valid payment amount.", vm.uiState.value.userMessage)
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    // ==========================================
    // 9. Offline Mode & Lockdown Verification Tests
    // ==========================================

    private class TestAttendanceRepo : com.soumit.technovision.data.repository.AttendanceRepository {
        val submittedRequests = mutableListOf<com.soumit.technovision.data.model.TeacherAttendanceRequestModel>()

        override fun getAttendanceRecordsStream() = kotlinx.coroutines.flow.flowOf(emptyList<com.soumit.technovision.data.model.AttendanceRecordModel>())
        override suspend fun getExistingAttendance(date: String, timeSlot: String) = Result.success(null)
        override suspend fun saveBatchAttendance(record: com.soumit.technovision.data.model.AttendanceRecordModel) = Result.success(Unit)
        override fun getTeacherAttendanceRequestsStream(teacherId: String?) = kotlinx.coroutines.flow.flowOf(submittedRequests)
        override suspend fun submitTeacherAttendanceRequest(request: com.soumit.technovision.data.model.TeacherAttendanceRequestModel): Result<String> {
            val id = if (request.id.isNotBlank()) request.id else "offline_${System.currentTimeMillis()}"
            submittedRequests.add(request.copy(id = id))
            return Result.success(id)
        }
        override suspend fun verifyTeacherAttendance(requestId: String, verifiedBy: String) = Result.success(Unit)
        override suspend fun rejectTeacherAttendance(requestId: String, rejectedBy: String) = Result.success(Unit)
    }

    @Test
    fun teacherAttendance_offlineSubmissionAllowed_andEnqueued() = kotlinx.coroutines.test.runTest {
        val testDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        try {
            val repo = TestAttendanceRepo()
            val vm = com.soumit.technovision.ui.screens.attendance.TeacherAttendanceViewModel(
                attendanceRepository = repo,
                authRepository = TestAuthRepo(),
                staffRepository = TestStaffRepo(),
                auditLogRepository = TestAuditLogRepo(),
                networkMonitor = null // offline/default state
            )
            testScheduler.advanceUntilIdle()

            var submitted = false
            vm.submitRequest { submitted = true }
            testScheduler.advanceUntilIdle()

            assertTrue(submitted)
            val state = vm.uiState.value
            assertNotNull(state.userMessage)
            assertTrue(state.userMessage!!.contains("offline", ignoreCase = true) || state.userMessage!!.contains("success", ignoreCase = true))
            assertEquals(1, repo.submittedRequests.size)
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    @Test
    fun syncQueueEntity_fieldsAndStatuses_workCorrectly() {
        val entity = com.soumit.technovision.data.local.room.SyncQueueEntity(
            id = 1L,
            entityType = "TEACHER_ATTENDANCE",
            docId = "offline_12345",
            payloadJson = "{\"teacherId\":\"t1\",\"status\":\"Pending (Offline)\"}",
            status = "PENDING"
        )
        assertEquals("TEACHER_ATTENDANCE", entity.entityType)
        assertEquals("offline_12345", entity.docId)
        assertEquals("PENDING", entity.status)
        assertTrue(entity.docId.startsWith("offline_"))
    }
}



