package com.soumit.technovision

import com.google.firebase.auth.FirebaseUser
import com.soumit.technovision.data.repository.AuthRepository
import com.soumit.technovision.navigation.Screen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

private class FakeAuthRepository(
    override val currentUser: FirebaseUser? = null
) : AuthRepository {
    override fun authStateStream(): Flow<FirebaseUser?> = flowOf(currentUser)
    override suspend fun getUserRole(userId: String): Result<String> = Result.success("owner")
    override suspend fun getCurrentUserName(): Result<String> = Result.success("Admin")
    override suspend fun signIn(email: String, password: String): Result<String> = Result.success("owner")
    override suspend fun sendPasswordReset(email: String): Result<Unit> = Result.success(Unit)
    override fun signOut() {}
}

class MainViewModelTest {

    @Test
    fun whenUserNotLoggedIn_startDestinationIsAuthScreen() {
        val fakeAuthRepository = FakeAuthRepository(currentUser = null)
        val viewModel = MainViewModel(authRepository = fakeAuthRepository)

        val state = viewModel.uiState.value
        assertEquals(Screen.Auth, state.startDestination)
        assertFalse(state.isLoadingRole)
    }
}
