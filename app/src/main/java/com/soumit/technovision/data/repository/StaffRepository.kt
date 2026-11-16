package com.soumit.technovision.data.repository

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.soumit.technovision.TechnoVisionApplication
import com.soumit.technovision.data.model.StaffModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface StaffRepository {
    fun getStaffStream(): Flow<List<StaffModel>>
    fun getUsersMapStream(): Flow<Map<String, String>>
    suspend fun getUsersMap(): Result<Map<String, String>>
    suspend fun createStaff(fullName: String, email: String, password: String, phone: String, role: String): Result<String>
    suspend fun updateStaff(id: String, fullName: String, phone: String, role: String): Result<Unit>
    suspend fun deleteStaff(id: String): Result<Unit>
}

class FirestoreStaffRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : StaffRepository {

    companion object {
        private val STAFF_ROLES = listOf("owner", "faculty", "receptionist", "Faculty", "Receptionist", "Owner", "admin", "Admin")
    }

    override fun getStaffStream(): Flow<List<StaffModel>> = callbackFlow {
        val subscription = firestore.collection("users")
            .whereIn("role", listOf("faculty", "receptionist", "Faculty", "Receptionist"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val staff = snapshot.documents.map { doc ->
                        StaffModel(
                            id = doc.id,
                            fullName = doc.getString("fullName") ?: "",
                            email = doc.getString("email") ?: "",
                            role = doc.getString("role") ?: "",
                            phone = doc.getString("phone") ?: ""
                        )
                    }
                    trySend(staff)
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getUsersMapStream(): Flow<Map<String, String>> = callbackFlow {
        val subscription = firestore.collection("users")
            .whereIn("role", STAFF_ROLES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val map = mutableMapOf<String, String>()
                    for (doc in snapshot.documents) {
                        val fullName = doc.getString("fullName")
                            ?: doc.getString("name")
                            ?: doc.getString("displayName")
                            ?: continue
                        val email = doc.getString("email") ?: ""
                        val uid = doc.id
                        if (email.isNotBlank()) {
                            map[email] = fullName
                            map[email.lowercase()] = fullName
                        }
                        map[uid] = fullName
                    }
                    trySend(map)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun getUsersMap(): Result<Map<String, String>> {
        return try {
            val snapshot = firestore.collection("users")
                .whereIn("role", STAFF_ROLES)
                .get()
                .await()
            val map = mutableMapOf<String, String>()
            for (doc in snapshot.documents) {
                val fullName = doc.getString("fullName")
                    ?: doc.getString("name")
                    ?: doc.getString("displayName")
                    ?: continue
                val email = doc.getString("email") ?: ""
                val uid = doc.id
                if (email.isNotBlank()) {
                    map[email] = fullName
                    map[email.lowercase()] = fullName
                }
                map[uid] = fullName
            }
            Result.success(map)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createStaff(
        fullName: String,
        email: String,
        password: String,
        phone: String,
        role: String
    ): Result<String> {
        return try {
            val primaryApp = FirebaseApp.getInstance()
            val secondaryApp = try {
                FirebaseApp.getInstance("SecondaryStaffApp")
            } catch (_: Exception) {
                val context = TechnoVisionApplication.instance?.applicationContext ?: primaryApp.applicationContext
                val builder = FirebaseOptions.Builder()
                    .setApiKey(primaryApp.options.apiKey)
                    .setApplicationId(primaryApp.options.applicationId)
                    .setProjectId(primaryApp.options.projectId)
                primaryApp.options.databaseUrl?.let { builder.setDatabaseUrl(it) }
                primaryApp.options.gcmSenderId?.let { builder.setGcmSenderId(it) }
                primaryApp.options.storageBucket?.let { builder.setStorageBucket(it) }
                FirebaseApp.initializeApp(context, builder.build(), "SecondaryStaffApp")
            }

            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
            val authResult = secondaryAuth.createUserWithEmailAndPassword(email.trim(), password).await()
            val uid = authResult.user?.uid ?: throw IllegalStateException("Auth user ID could not be retrieved")

            val staffData = hashMapOf(
                "fullName" to fullName.trim(),
                "email" to email.trim(),
                "role" to role.trim(),
                "phone" to phone.trim(),
                "createdAt" to Timestamp.now()
            )

            try {
                firestore.collection("users").document(uid).set(staffData).await()
                secondaryAuth.signOut()
                Result.success(uid)
            } catch (e: Exception) {
                try {
                    secondaryAuth.currentUser?.delete()?.await()
                } catch (_: Exception) {}
                secondaryAuth.signOut()
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStaff(id: String, fullName: String, phone: String, role: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "fullName" to fullName.trim(),
                "phone" to phone.trim(),
                "role" to role.trim()
            )
            firestore.collection("users").document(id).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteStaff(id: String): Result<Unit> {
        return try {
            firestore.collection("users").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
