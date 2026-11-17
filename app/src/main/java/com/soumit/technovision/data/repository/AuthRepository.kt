package com.soumit.technovision.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface AuthRepository {
    val currentUser: FirebaseUser?
    val currentUserId: String? get() = currentUser?.uid
    fun authStateStream(): Flow<FirebaseUser?>
    suspend fun getUserRole(userId: String): Result<String>
    suspend fun getCurrentUserName(): Result<String>
    suspend fun signIn(email: String, password: String): Result<String>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    fun signOut()
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    private data class RoleCache(val userId: String, val role: String)
    private data class NameCache(val userId: String, val name: String)

    @Volatile
    private var roleCache: RoleCache? = null

    @Volatile
    private var nameCache: NameCache? = null

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override fun authStateStream(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { currentAuth ->
            trySend(currentAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun getUserRole(userId: String): Result<String> {
        val cached = roleCache
        if (cached != null && cached.userId == userId) {
            return Result.success(cached.role)
        }

        return try {
            val doc = try {
                firestore.collection("users").document(userId).get().await()
            } catch (_: Exception) {
                firestore.collection("users").document(userId).get(Source.CACHE).await()
            }
            if (doc.exists()) {
                val role = doc.getString("role") ?: "auth"
                roleCache = RoleCache(userId, role)
                Result.success(role)
            } else {
                Result.failure(NoSuchElementException("User profile not found in database."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUserName(): Result<String> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("No authenticated user"))
        val cached = nameCache
        if (cached != null && cached.userId == uid) {
            return Result.success(cached.name)
        }

        return try {
            val doc = try {
                firestore.collection("users").document(uid).get().await()
            } catch (_: Exception) {
                firestore.collection("users").document(uid).get(Source.CACHE).await()
            }
            val name = doc.getString("fullName") ?: doc.getString("name") ?: auth.currentUser?.displayName ?: "User"
            nameCache = NameCache(uid, name)
            Result.success(name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("Authentication succeeded but user was null."))

            val roleResult = getUserRole(user.uid)
            if (roleResult.isSuccess) {
                Result.success(roleResult.getOrThrow())
            } else {
                signOut()
                roleResult
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun signOut() {
        roleCache = null
        nameCache = null
        auth.signOut()
    }
}
