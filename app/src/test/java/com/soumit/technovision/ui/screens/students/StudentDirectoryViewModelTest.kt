package com.soumit.technovision.ui.screens.students

import com.google.firebase.auth.FirebaseUser
import com.soumit.technovision.data.model.FeePaymentModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.repository.AuthRepository
import com.soumit.technovision.data.repository.FeeRepository
import com.soumit.technovision.data.repository.StudentRepository
import com.soumit.technovision.domain.usecase.CalculateBillingDuesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private class FakeStudentRepository(
    private val students: List<StudentModel>
) : StudentRepository {
    override fun getStudentsStream(): Flow<List<StudentModel>> = flowOf(students)
    override suspend fun getStudentsOnce(): Result<List<StudentModel>> = Result.success(students)
    override fun getStudentStream(studentId: String): Flow<StudentModel?> =
        flowOf(students.find { it.id == studentId })
    override suspend fun getStudent(studentId: String): Result<StudentModel> {
        val s = students.find { it.id == studentId }
        return if (s != null) Result.success(s) else Result.failure(NoSuchElementException())
    }
    override suspend fun enrollStudent(student: StudentModel): Result<String> = Result.success(student.id)
    override suspend fun updateStudent(student: StudentModel): Result<Unit> = Result.success(Unit)
    override suspend fun deleteStudent(studentId: String, fullName: String): Result<Unit> = Result.success(Unit)
}

private class FakeFeeRepository(
    private val payments: List<FeePaymentModel>
) : FeeRepository {
    override fun getFeePaymentsStream(): Flow<List<FeePaymentModel>> = flowOf(payments)
    override fun getPaymentsForStudentStream(studentId: String): Flow<List<FeePaymentModel>> =
        flowOf(payments.filter { it.studentId == studentId })
    override suspend fun recordPayment(payment: FeePaymentModel): Result<String> = Result.success(payment.id)
    override suspend fun deletePayment(paymentId: String): Result<Unit> = Result.success(Unit)
    override suspend fun syncAllStudentsTotalPaid(): Result<Unit> = Result.success(Unit)
}

private class FakeAuthRepository(
    override val currentUserId: String? = "test_user_id"
) : AuthRepository {
    override val currentUser: FirebaseUser? = null
    override fun authStateStream(): Flow<FirebaseUser?> = flowOf(null)
    override suspend fun getUserRole(userId: String): Result<String> = Result.success("owner")
    override suspend fun getCurrentUserName(): Result<String> = Result.success("Owner")
    override suspend fun signIn(email: String, password: String): Result<String> = Result.success("owner")
    override suspend fun sendPasswordReset(email: String): Result<Unit> = Result.success(Unit)
    override fun signOut() {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class StudentDirectoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun balancesCalculatedFromActualFeePayments_andRoleLoadedCorrectly() = runTest {
        // Student has totalPaid = 0 in StudentModel, but payments exist in FeeRepository
        val student1 = StudentModel(
            id = "s1",
            fullName = "John Doe",
            admissionDate = "15/01/2024",
            monthlyInstallment = 500L,
            totalPaid = 0L // Stale value in student model
        )
        val payment1 = FeePaymentModel(
            id = "p1",
            studentId = "s1",
            amountPaid = 500L
        )

        val studentRepo = FakeStudentRepository(listOf(student1))
        val feeRepo = FakeFeeRepository(listOf(payment1))
        val authRepo = FakeAuthRepository(currentUserId = "test_user_id")
        val useCase = CalculateBillingDuesUseCase()

        val viewModel = StudentDirectoryViewModel(
            studentRepository = studentRepo,
            feeRepository = feeRepo,
            authRepository = authRepo,
            calculateBillingDuesUseCase = useCase,
            defaultDispatcher = testDispatcher
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        val expectedDues = useCase(
            admissionDate = student1.admissionDate,
            monthlyInstallment = student1.monthlyInstallment,
            totalPaid = 500L // from payment, NOT 0L
        )

        assertEquals(expectedDues.outstandingBalance, state.outstandingBalances["s1"])
        assertEquals(500L, state.students.first().totalPaid)
        org.junit.Assert.assertFalse(state.isLoading)
        assertEquals("owner", state.userRole)
    }

    @Test
    fun initialState_userRoleDefaultsToEmpty_notPrematureOwner() {
        val initialState = StudentDirectoryUiState()
        assertEquals("", initialState.userRole)
        org.junit.Assert.assertTrue(initialState.isLoading)
    }

    @Test
    fun whenUserNotLoggedIn_userRoleIsEmptyAndLoadingCompletes() = runTest {
        val studentRepo = FakeStudentRepository(emptyList())
        val feeRepo = FakeFeeRepository(emptyList())
        val authRepo = FakeAuthRepository(currentUserId = null)
        val viewModel = StudentDirectoryViewModel(
            studentRepository = studentRepo,
            feeRepository = feeRepo,
            authRepository = authRepo,
            calculateBillingDuesUseCase = CalculateBillingDuesUseCase(),
            defaultDispatcher = testDispatcher
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.userRole)
        org.junit.Assert.assertFalse(state.isLoading)
    }
}
