package com.soumit.technovision

import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.data.model.FeePaymentModel
import com.soumit.technovision.data.model.LogModel
import com.soumit.technovision.data.model.StaffModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel
import com.soumit.technovision.data.repository.*
import com.soumit.technovision.ui.screens.attendance.AttendanceHistoryViewModel
import com.soumit.technovision.ui.screens.attendance.FacultyAttendanceViewerViewModel
import com.soumit.technovision.ui.screens.attendance.TakeAttendanceViewModel
import com.soumit.technovision.ui.screens.attendance.VerifyTeacherAttendanceViewModel
import com.soumit.technovision.ui.screens.batches.BatchesViewModel
import com.soumit.technovision.ui.screens.fees.GlobalHistoryViewModel
import com.soumit.technovision.ui.screens.logs.SystemLogsViewModel
import com.soumit.technovision.ui.screens.staff.ManageStaffViewModel
import com.soumit.technovision.ui.screens.students.BirthdayCalendarViewModel
import com.soumit.technovision.ui.screens.students.UpdateDeleteStudentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ComprehensiveStressAndFuzzingTest {

    // --- Mocks & Fakes ---

    private class FakeStudentRepo(
        val students: MutableList<StudentModel> = mutableListOf()
    ) : StudentRepository {
        override fun getStudentsStream(): Flow<List<StudentModel>> = flow {
            emit(students.toList())
        }
        override suspend fun getStudentsOnce(): Result<List<StudentModel>> = Result.success(students.toList())
        override fun getStudentStream(studentId: String): Flow<StudentModel?> = flow {
            emit(students.find { it.id == studentId })
        }
        override suspend fun getStudent(studentId: String): Result<StudentModel> =
            students.find { it.id == studentId }?.let { Result.success(it) }
                ?: Result.failure(NoSuchElementException("Student $studentId not found"))
        override suspend fun enrollStudent(student: StudentModel): Result<String> {
            val id = "s_${students.size + 1}"
            students.add(student.copy(id = id))
            return Result.success(id)
        }
        override suspend fun updateStudent(student: StudentModel): Result<Unit> {
            val idx = students.indexOfFirst { it.id == student.id }
            if (idx != -1) students[idx] = student
            return Result.success(Unit)
        }
        override suspend fun deleteStudent(studentId: String, fullName: String): Result<Unit> {
            students.removeAll { it.id == studentId }
            return Result.success(Unit)
        }
    }

    private class FakeStaffRepo(
        val staffList: MutableList<StaffModel> = mutableListOf(),
        val userMap: MutableMap<String, String> = mutableMapOf()
    ) : StaffRepository {
        override fun getStaffStream(): Flow<List<StaffModel>> = flow {
            emit(staffList.toList())
        }
        override fun getUsersMapStream(): Flow<Map<String, String>> = flow {
            emit(userMap.toMap())
        }
        override suspend fun getUsersMap(): Result<Map<String, String>> = Result.success(userMap.toMap())
        override suspend fun createStaff(fullName: String, email: String, password: String, phone: String, role: String): Result<String> {
            val id = "staff_${staffList.size + 1}"
            staffList.add(StaffModel(id = id, fullName = fullName, email = email, phone = phone, role = role))
            userMap[id] = fullName
            return Result.success(id)
        }
        override suspend fun updateStaff(id: String, fullName: String, phone: String, role: String): Result<Unit> {
            val idx = staffList.indexOfFirst { it.id == id }
            if (idx != -1) {
                staffList[idx] = staffList[idx].copy(fullName = fullName, phone = phone, role = role)
                userMap[id] = fullName
            }
            return Result.success(Unit)
        }
        override suspend fun deleteStaff(id: String): Result<Unit> {
            staffList.removeAll { it.id == id }
            userMap.remove(id)
            return Result.success(Unit)
        }
    }

    private class FakeAttendanceRepo : AttendanceRepository {
        val records = mutableListOf<AttendanceRecordModel>()
        val teacherRequests = mutableListOf<TeacherAttendanceRequestModel>()

        override fun getAttendanceRecordsStream(): Flow<List<AttendanceRecordModel>> = flow {
            emit(records.toList())
        }
        override suspend fun getExistingAttendance(date: String, timeSlot: String): Result<AttendanceRecordModel?> {
            val rec = records.find { it.date == date && it.timeSlot.equals(timeSlot, ignoreCase = true) }
            return Result.success(rec)
        }
        override suspend fun saveBatchAttendance(record: AttendanceRecordModel): Result<Unit> {
            records.add(record)
            return Result.success(Unit)
        }
        override fun getTeacherAttendanceRequestsStream(teacherId: String?): Flow<List<TeacherAttendanceRequestModel>> = flow {
            emit(teacherRequests.toList())
        }
        override suspend fun submitTeacherAttendanceRequest(request: TeacherAttendanceRequestModel): Result<String> {
            val id = if (request.id.isNotBlank()) request.id else "req_${teacherRequests.size + 1}"
            teacherRequests.add(request.copy(id = id))
            return Result.success(id)
        }
        override suspend fun verifyTeacherAttendance(requestId: String, verifiedBy: String): Result<Unit> {
            val idx = teacherRequests.indexOfFirst { it.id == requestId }
            if (idx != -1) {
                teacherRequests[idx] = teacherRequests[idx].copy(status = "Verified", verifiedBy = verifiedBy)
            }
            return Result.success(Unit)
        }
        override suspend fun rejectTeacherAttendance(requestId: String, rejectedBy: String): Result<Unit> {
            val idx = teacherRequests.indexOfFirst { it.id == requestId }
            if (idx != -1) {
                teacherRequests[idx] = teacherRequests[idx].copy(status = "Rejected", rejectedBy = rejectedBy)
            }
            return Result.success(Unit)
        }
    }

    private class FakeFeeRepo : FeeRepository {
        val payments = mutableListOf<FeePaymentModel>()

        override fun getFeePaymentsStream(): Flow<List<FeePaymentModel>> = flow {
            emit(payments.toList())
        }
        override fun getPaymentsForStudentStream(studentId: String): Flow<List<FeePaymentModel>> = flow {
            emit(payments.filter { it.studentId == studentId })
        }
        override suspend fun recordPayment(payment: FeePaymentModel): Result<String> {
            payments.add(payment)
            return Result.success(payment.id)
        }
        override suspend fun deletePayment(paymentId: String): Result<Unit> {
            payments.removeAll { it.id == paymentId }
            return Result.success(Unit)
        }
        override suspend fun syncAllStudentsTotalPaid(): Result<Unit> = Result.success(Unit)
    }

    private class FakeAuditLogRepo : AuditLogRepository {
        val logs = mutableListOf<LogModel>()
        override fun getLogsStream(): Flow<List<LogModel>> = flow {
            emit(logs.toList())
        }
        override fun log(action: String, details: String) {
            logs.add(LogModel(id = "l_${logs.size + 1}", action = action, details = details, performedBy = "Tester"))
        }
    }

    private class FakeAuthRepo(
        val role: String = "owner"
    ) : AuthRepository {
        override val currentUserId: String = "user_1"
        override val currentUser: com.google.firebase.auth.FirebaseUser? = null
        override fun authStateStream() = flowOf(null)
        override suspend fun getUserRole(userId: String) = Result.success(role)
        override suspend fun getCurrentUserName() = Result.success("Test User")
        override suspend fun signIn(email: String, password: String) = Result.success(role)
        override suspend fun sendPasswordReset(email: String) = Result.success(Unit)
        override fun signOut() {}
    }

    // ==========================================
    // 1. ManageStaffViewModel Stress & Validation
    // ==========================================

    @Test
    fun manageStaff_validation_enforcesLengthAndEmailFormat() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val staffRepo = FakeStaffRepo()
            val auditRepo = FakeAuditLogRepo()
            val vm = ManageStaffViewModel(staffRepo, auditRepo, networkMonitor = null)
            testScheduler.advanceUntilIdle()

            // 1. Empty fields
            var created = false
            vm.createStaff("", "", "", "", "") { created = true }
            assertFalse(created)
            assertEquals("Please fill in all required fields.", vm.uiState.value.errorMessage)

            // 2. Bad email
            vm.createStaff("John", "notanemail", "123456", "9876543210", "Faculty") { created = true }
            assertFalse(created)
            assertEquals("Invalid email format.", vm.uiState.value.errorMessage)

            // 3. Short password (< 6 chars)
            vm.createStaff("John", "john@example.com", "123", "9876543210", "Faculty") { created = true }
            assertFalse(created)
            assertEquals("Password must be at least 6 characters.", vm.uiState.value.errorMessage)

            // 4. Valid staff creation
            vm.createStaff("John Doe", "john@example.com", "secure123", "9876543210", "Faculty") { created = true }
            testScheduler.advanceUntilIdle()
            assertTrue(created)
            assertEquals(1, staffRepo.staffList.size)
            assertEquals("John Doe", staffRepo.staffList.first().fullName)

            // 5. Update staff validation
            var updated = false
            vm.updateStaff(staffRepo.staffList.first().id, "   ", "9876543210", "Faculty") { updated = true }
            assertFalse(updated)
            assertEquals("Full name cannot be blank.", vm.uiState.value.errorMessage)

            vm.updateStaff(staffRepo.staffList.first().id, "Johnathan Doe", "9876543210", "Owner") { updated = true }
            testScheduler.advanceUntilIdle()
            assertTrue(updated)
            assertEquals("Johnathan Doe", staffRepo.staffList.first().fullName)
            assertEquals("Owner", staffRepo.staffList.first().role)

            // 6. Delete staff
            var deleted = false
            vm.deleteStaff(staffRepo.staffList.first()) { deleted = true }
            testScheduler.advanceUntilIdle()
            assertTrue(deleted)
            assertEquals(0, staffRepo.staffList.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    // ==========================================
    // 2. FacultyAttendanceViewer Aggregations
    // ==========================================

    @Test
    fun facultyAttendanceViewer_summariesAndHours_computedAccurately() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val attRepo = FakeAttendanceRepo()
            val staffRepo = FakeStaffRepo()

            val now = LocalDate.now()
            val curDateStr = String.format("%02d-%02d-%d", now.dayOfMonth, now.monthValue, now.year)

            // Request 1: Verified in current month (2.0 hours)
            attRepo.teacherRequests.add(
                TeacherAttendanceRequestModel(
                    id = "r1",
                    teacherId = "t1",
                    teacherName = "Prof. Xavier",
                    date = curDateStr,
                    timeSlot = "9:00 AM - 11:00 AM", // 2 hours
                    status = "Verified"
                )
            )

            // Request 2: Rejected in current month (should NOT count in verified summaries)
            attRepo.teacherRequests.add(
                TeacherAttendanceRequestModel(
                    id = "r2",
                    teacherId = "t1",
                    teacherName = "Prof. Xavier",
                    date = curDateStr,
                    timeSlot = "11:00 AM - 1:00 PM",
                    status = "Rejected"
                )
            )

            // Request 3: Verified in previous year (counts in all-time, NOT this month)
            attRepo.teacherRequests.add(
                TeacherAttendanceRequestModel(
                    id = "r3",
                    teacherId = "t1",
                    teacherName = "Prof. Xavier",
                    date = "15-05-2022",
                    timeSlot = "1:00 PM - 3:00 PM", // 2 hours
                    status = "Verified"
                )
            )

            val vm = FacultyAttendanceViewerViewModel(attRepo, staffRepo)
            testScheduler.advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertEquals(2, state.verifiedRequests.size)

            val summaries = state.teacherSummaries
            assertEquals(1, summaries.size)

            val xavierSummary = summaries.first()
            assertEquals("Prof. Xavier", xavierSummary.teacherName)
            assertEquals(1, xavierSummary.monthClassesCount)
            assertEquals(2, xavierSummary.allTimeClassesCount)
            assertEquals(2.0, xavierSummary.monthHours, 0.001)
            assertEquals(4.0, xavierSummary.allTimeHours, 0.001)
        } finally {
            Dispatchers.resetMain()
        }
    }

    // ==========================================
    // 3. BatchesViewModel Schedule Categorization
    // ==========================================

    @Test
    fun batchesViewModel_filtersAndListsStudents_seamlessly() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val studentRepo = FakeStudentRepo()
            studentRepo.students.add(
                StudentModel(
                    id = "s1",
                    fullName = "Aarav Gupta",
                    studentClass = "Class 10",
                    weeklySchedule = mapOf("Monday" to "9:00 AM - 11:00 AM", "Wednesday" to "9:00 AM - 11:00 AM")
                )
            )
            studentRepo.students.add(
                StudentModel(
                    id = "s2",
                    fullName = "Priya Sen",
                    studentClass = "Class 12",
                    weeklySchedule = mapOf("Tuesday" to "5:00 PM - 7:00 PM", "Thursday" to "5:00 PM - 7:00 PM")
                )
            )

            val vm = BatchesViewModel(studentRepo)
            testScheduler.advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertEquals(2, state.studentList.size)

            vm.onViewSelected("Weekly View")
            assertEquals("Weekly View", vm.uiState.value.selectedView)
        } finally {
            Dispatchers.resetMain()
        }
    }

    // ==========================================
    // 4. BirthdayCalendarViewModel Search & Filters
    // ==========================================

    @Test
    fun birthdayCalendarViewModel_filteringAndTabs_workCorrectly() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val studentRepo = FakeStudentRepo()
            studentRepo.students.add(
                StudentModel(
                    id = "s1",
                    fullName = "Aarav Gupta",
                    dob = "15/08/2008",
                    studentClass = "Class 10"
                )
            )
            studentRepo.students.add(
                StudentModel(
                    id = "s2",
                    fullName = "Neha Verma",
                    dob = "22/10/2009",
                    studentClass = "Class 9"
                )
            )

            val vm = BirthdayCalendarViewModel(studentRepo)
            testScheduler.advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertEquals(2, state.allItems.size)

            // Filter by name
            vm.onSearchQueryChanged("Aarav")
            assertEquals(1, vm.uiState.value.filteredItems.size)
            assertEquals("Aarav Gupta", vm.uiState.value.filteredItems.first().student.fullName)

            // Reset search, filter by class
            vm.onSearchQueryChanged("")
            vm.onClassFilterSelected("Class 9")
            assertEquals(1, vm.uiState.value.filteredItems.size)
            assertEquals("Neha Verma", vm.uiState.value.filteredItems.first().student.fullName)

            // Switch tab
            vm.onTabSelected(1)
            assertEquals(1, vm.uiState.value.selectedTab)
        } finally {
            Dispatchers.resetMain()
        }
    }

    // ==========================================
    // 5. SystemLogsViewModel Filtering & Sorting
    // ==========================================

    @Test
    fun systemLogsViewModel_filtersByCategoryAndTimeSlot() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val auditRepo = FakeAuditLogRepo()
            val staffRepo = FakeStaffRepo()

            auditRepo.logs.add(LogModel(id = "l1", action = "Fee Collected", details = "Collected ₹500"))
            auditRepo.logs.add(LogModel(id = "l2", action = "Student Enrolled", details = "New admission"))
            auditRepo.logs.add(LogModel(id = "l3", action = "Attendance Verified", details = "Slot [9:00 AM - 11:00 AM]"))

            val vm = SystemLogsViewModel(auditRepo, staffRepo)
            testScheduler.advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertEquals(3, state.logs.size)

            vm.setSelectedCategory("Fees")
            assertEquals("Fees", vm.uiState.value.selectedCategory)

            vm.setSelectedTimeSlot("9:00 AM - 11:00 AM")
            assertEquals("9:00 AM - 11:00 AM", vm.uiState.value.selectedTimeSlot)

            vm.setShowAllHistory(true)
            assertTrue(vm.uiState.value.showAllHistory)
            assertNull(vm.uiState.value.selectedCustomDate)
        } finally {
            Dispatchers.resetMain()
        }
    }

    // ==========================================
    // 6. VerifyTeacherAttendanceViewModel State Machine
    // ==========================================

    @Test
    fun verifyTeacherAttendanceViewModel_approveAndRejectFlow() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val attRepo = FakeAttendanceRepo()
            val staffRepo = FakeStaffRepo()
            val authRepo = FakeAuthRepo("owner")
            val auditRepo = FakeAuditLogRepo()

            val req1 = TeacherAttendanceRequestModel(
                id = "req_1",
                teacherId = "t1",
                teacherName = "Teacher One",
                date = "19-11-2026",
                timeSlot = "9:00 AM - 11:00 AM",
                status = "Pending"
            )
            val req2 = TeacherAttendanceRequestModel(
                id = "req_2",
                teacherId = "t2",
                teacherName = "Teacher Two",
                date = "19-11-2026",
                timeSlot = "11:00 AM - 1:00 PM",
                status = "Pending"
            )
            attRepo.teacherRequests.addAll(listOf(req1, req2))

            val vm = VerifyTeacherAttendanceViewModel(
                attendanceRepository = attRepo,
                staffRepository = staffRepo,
                authRepository = authRepo,
                auditLogRepository = auditRepo,
                networkMonitor = null
            )
            testScheduler.advanceUntilIdle()

            assertEquals(2, vm.uiState.value.pendingRequests.size)

            // Verify req1
            vm.verifyRequest(req1)
            testScheduler.advanceUntilIdle()

            val updatedReq1 = attRepo.teacherRequests.find { it.id == "req_1" }
            assertEquals("Verified", updatedReq1?.status)
            assertEquals(1, vm.uiState.value.pendingRequests.size)
            assertEquals(1, vm.uiState.value.resolvedRequests.size)

            // Reject req2
            vm.rejectRequest(req2)
            testScheduler.advanceUntilIdle()

            val updatedReq2 = attRepo.teacherRequests.find { it.id == "req_2" }
            assertEquals("Rejected", updatedReq2?.status)
            assertEquals(0, vm.uiState.value.pendingRequests.size)
            assertEquals(2, vm.uiState.value.resolvedRequests.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    // ==========================================
    // 7. GlobalHistoryViewModel Filtering & Delete
    // ==========================================

    @Test
    fun globalHistoryViewModel_searchFilteringAndDeletion() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val feeRepo = FakeFeeRepo()
            val studentRepo = FakeStudentRepo()
            val authRepo = FakeAuthRepo("owner")
            val auditRepo = FakeAuditLogRepo()

            val s1 = StudentModel(id = "s1", fullName = "Rahul Das", studentClass = "Class 10", school = "DPS")
            studentRepo.students.add(s1)

            val p1 = FeePaymentModel(
                id = "p1",
                studentId = "s1",
                studentName = "Rahul Das",
                amountPaid = 1200L,
                paymentMethod = "Cash",
                remarks = "Term 1"
            )
            val p2 = FeePaymentModel(
                id = "p2",
                studentId = "s1",
                studentName = "Rahul Das",
                amountPaid = 800L,
                paymentMethod = "UPI",
                remarks = "Term 2"
            )
            feeRepo.payments.addAll(listOf(p1, p2))

            val vm = GlobalHistoryViewModel(
                feeRepository = feeRepo,
                studentRepository = studentRepo,
                authRepository = authRepo,
                auditLogRepository = auditRepo,
                defaultDispatcher = testDispatcher,
                networkMonitor = null
            )
            testScheduler.advanceUntilIdle()

            assertFalse(vm.uiState.value.isLoading)
            assertEquals(2, vm.uiState.value.paymentHistory.size)
            assertEquals("owner", vm.uiState.value.currentUserRole)

            // Filter by method
            vm.onMethodFilterChanged("UPI")
            assertEquals(1, vm.uiState.value.filteredPayments.size)
            assertEquals(800L, vm.uiState.value.filteredPayments.first().amountPaid)

            // Filter by search query
            vm.onMethodFilterChanged("All")
            vm.onSearchQueryChanged("Term 1")
            assertEquals(1, vm.uiState.value.filteredPayments.size)
            assertEquals(1200L, vm.uiState.value.filteredPayments.first().amountPaid)

            // Delete payment
            vm.setPaymentToDelete(p1)
            var deleted = false
            vm.deletePayment { deleted = true }
            testScheduler.advanceUntilIdle()

            assertTrue(deleted)
            assertEquals(1, feeRepo.payments.size)
            assertEquals("p2", feeRepo.payments.first().id)
        } finally {
            Dispatchers.resetMain()
        }
    }

    // ==========================================
    // 8. AttendanceHistoryViewModel Detail Inspection
    // ==========================================

    @Test
    fun attendanceHistoryViewModel_openAndCloseDetail() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val attRepo = FakeAttendanceRepo()
            val studentRepo = FakeStudentRepo()
            val staffRepo = FakeStaffRepo()

            val rec = AttendanceRecordModel(
                id = "att_1",
                date = "19-11-2026",
                timeSlot = "9:00 AM - 11:00 AM",
                totalPresent = 15,
                totalAbsent = 2
            )
            attRepo.records.add(rec)

            val vm = AttendanceHistoryViewModel(attRepo, studentRepo, staffRepo)
            testScheduler.advanceUntilIdle()

            assertEquals(1, vm.uiState.value.attendanceRecords.size)
            assertFalse(vm.uiState.value.isDetailOpen)

            vm.openDetail(rec)
            assertTrue(vm.uiState.value.isDetailOpen)
            assertEquals(rec.id, vm.uiState.value.selectedRecord?.id)

            vm.closeDetail()
            assertFalse(vm.uiState.value.isDetailOpen)
            assertNull(vm.uiState.value.selectedRecord)
        } finally {
            Dispatchers.resetMain()
        }
    }

    // ==========================================
    // 9. TakeAttendanceViewModel Full Lifecycle
    // ==========================================

    @Test
    fun takeAttendanceViewModel_fullCycle_togglesAndLocking() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val studentRepo = FakeStudentRepo()
            val attRepo = FakeAttendanceRepo()
            val authRepo = FakeAuthRepo("faculty")
            val staffRepo = FakeStaffRepo()
            val auditRepo = FakeAuditLogRepo()

            val curDay = com.soumit.technovision.util.AttendanceUtils.getTodayDayName()
            val testSlot = "9:00 AM - 11:00 AM"

            val s1 = StudentModel(
                id = "s1",
                fullName = "Rohan Das",
                weeklySchedule = mapOf(curDay to testSlot)
            )
            val s2 = StudentModel(
                id = "s2",
                fullName = "Ananya Roy",
                weeklySchedule = mapOf(curDay to testSlot)
            )
            val sExtra = StudentModel(
                id = "sExtra",
                fullName = "Guest Student",
                weeklySchedule = emptyMap()
            )
            studentRepo.students.addAll(listOf(s1, s2, sExtra))

            val vm = TakeAttendanceViewModel(
                attendanceRepository = attRepo,
                studentRepository = studentRepo,
                authRepository = authRepo,
                staffRepository = staffRepo,
                auditLogRepository = auditRepo,
                networkMonitor = null
            )
            testScheduler.advanceUntilIdle()

            vm.onSelectTimeSlot(testSlot)
            testScheduler.advanceUntilIdle()

            // 1. Initial scheduled students loaded
            assertEquals(2, vm.uiState.value.scheduledStudents.size)
            assertFalse(vm.uiState.value.isAttendanceLocked)

            // 2. Toggle s1 present
            vm.toggleStudentAttendance("s1")
            assertTrue(vm.uiState.value.attendanceStates["s1"] == true)

            // 3. Set all present
            vm.setAllAttendance(true)
            assertTrue(vm.uiState.value.attendanceStates["s1"] == true)
            assertTrue(vm.uiState.value.attendanceStates["s2"] == true)

            // 4. Add additional student
            vm.addAdditionalStudent(sExtra)
            assertEquals(1, vm.uiState.value.additionalStudents.size)
            assertTrue(vm.uiState.value.attendanceStates["sExtra"] == true)

            // 5. Save attendance online
            var saved = false
            vm.saveAttendance { saved = true }
            testScheduler.advanceUntilIdle()

            assertTrue(saved)
            assertEquals(1, attRepo.records.size)
            val savedRec = attRepo.records.first()
            assertEquals(3, savedRec.totalPresent)
            assertEquals(0, savedRec.totalAbsent)
            assertEquals(listOf("sExtra"), savedRec.additionalStudentIds)
        } finally {
            Dispatchers.resetMain()
        }
    }

    // ==========================================
    // 10. UpdateDeleteStudentViewModel Validation & Deletion
    // ==========================================

    @Test
    fun updateDeleteStudentViewModel_validationAndDeletion() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val studentRepo = FakeStudentRepo()
            val auditRepo = FakeAuditLogRepo()

            val existing = StudentModel(
                id = "s_target",
                fullName = "Original Name",
                studentClass = "Class 10",
                monthlyInstallment = 500L,
                weeklySchedule = mapOf("Monday" to "9:00 AM - 11:00 AM")
            )
            studentRepo.students.add(existing)

            val handle = androidx.lifecycle.SavedStateHandle(mapOf("studentId" to "s_target"))
            val vm = UpdateDeleteStudentViewModel(
                savedStateHandle = handle,
                studentRepository = studentRepo,
                auditLogRepository = auditRepo,
                networkMonitor = null
            )
            testScheduler.advanceUntilIdle()

            assertEquals("Original Name", vm.uiState.value.fullName)
            assertFalse(vm.uiState.value.isLoading)

            // 1. Validation failure: blank name
            vm.onFullNameChanged("   ")
            var updated = false
            vm.updateStudent { updated = true }
            assertFalse(updated)
            assertEquals("Full name cannot be blank.", vm.uiState.value.errorMessage)

            // 2. Validation failure: non-numeric installment
            vm.onFullNameChanged("Valid Name")
            vm.onMonthlyInstallmentChanged("-200")
            vm.updateStudent { updated = true }
            assertFalse(updated)
            assertEquals("Monthly installment must be a valid positive amount.", vm.uiState.value.errorMessage)

            // 3. Successful update
            vm.onMonthlyInstallmentChanged("750")
            vm.updateStudent { updated = true }
            testScheduler.advanceUntilIdle()

            assertTrue(updated)
            val updatedModel = studentRepo.students.find { it.id == "s_target" }
            assertEquals("Valid Name", updatedModel?.fullName)
            assertEquals(750L, updatedModel?.monthlyInstallment)

            // 4. Successful delete
            var deleted = false
            vm.deleteStudent { deleted = true }
            testScheduler.advanceUntilIdle()

            assertTrue(deleted)
            assertEquals(0, studentRepo.students.size)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
