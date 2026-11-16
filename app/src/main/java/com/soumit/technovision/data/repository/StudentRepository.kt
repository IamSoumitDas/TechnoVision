package com.soumit.technovision.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soumit.technovision.data.model.StudentModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface StudentRepository {
    fun getStudentsStream(): Flow<List<StudentModel>>
    fun getStudentStream(studentId: String): Flow<StudentModel?>
    suspend fun getStudent(studentId: String): Result<StudentModel>
    suspend fun enrollStudent(student: StudentModel): Result<String>
    suspend fun updateStudent(student: StudentModel): Result<Unit>
    suspend fun deleteStudent(studentId: String, fullName: String): Result<Unit>
}

class FirestoreStudentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : StudentRepository {

    override fun getStudentsStream(): Flow<List<StudentModel>> = callbackFlow {
        val subscription = firestore.collection("students")
            .orderBy("fullName", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val students = snapshot.documents.map { doc ->
                        StudentModel(
                            id = doc.id,
                            fullName = doc.getString("fullName") ?: "",
                            studentClass = doc.getString("studentClass") ?: "",
                            school = doc.getString("school") ?: "",
                            dob = doc.getString("dob") ?: "",
                            gender = doc.getString("gender") ?: "",
                            admissionDate = doc.getString("admissionDate") ?: "",
                            fatherName = doc.getString("fatherName") ?: "",
                            motherName = doc.getString("motherName") ?: "",
                            monthlyInstallment = doc.getLong("monthlyInstallment") ?: 500L,
                            phone = doc.getString("phone") ?: "",
                            altPhone = doc.getString("altPhone") ?: "",
                            email = doc.getString("email") ?: "",
                            batchTimeSlot = doc.getString("batchTimeSlot") ?: "",
                            weeklySchedule = parseWeeklySchedule(doc.get("weeklySchedule"))
                        )
                    }
                    trySend(students)
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getStudentStream(studentId: String): Flow<StudentModel?> = callbackFlow {
        if (studentId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val subscription = firestore.collection("students").document(studentId)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    val student = StudentModel(
                        id = doc.id,
                        fullName = doc.getString("fullName") ?: "",
                        studentClass = doc.getString("studentClass") ?: "",
                        school = doc.getString("school") ?: "",
                        dob = doc.getString("dob") ?: "",
                        gender = doc.getString("gender") ?: "",
                        admissionDate = doc.getString("admissionDate") ?: "",
                        fatherName = doc.getString("fatherName") ?: "",
                        motherName = doc.getString("motherName") ?: "",
                        monthlyInstallment = doc.getLong("monthlyInstallment") ?: 500L,
                        phone = doc.getString("phone") ?: "",
                        altPhone = doc.getString("altPhone") ?: "",
                        email = doc.getString("email") ?: "",
                        batchTimeSlot = doc.getString("batchTimeSlot") ?: "",
                        weeklySchedule = parseWeeklySchedule(doc.get("weeklySchedule"))
                    )
                    trySend(student)
                } else {
                    trySend(null)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun getStudent(studentId: String): Result<StudentModel> {
        return try {
            val doc = firestore.collection("students").document(studentId).get().await()
            if (doc.exists()) {
                val student = StudentModel(
                    id = doc.id,
                    fullName = doc.getString("fullName") ?: "",
                    studentClass = doc.getString("studentClass") ?: "",
                    school = doc.getString("school") ?: "",
                    dob = doc.getString("dob") ?: "",
                    gender = doc.getString("gender") ?: "",
                    admissionDate = doc.getString("admissionDate") ?: "",
                    fatherName = doc.getString("fatherName") ?: "",
                    motherName = doc.getString("motherName") ?: "",
                    monthlyInstallment = doc.getLong("monthlyInstallment") ?: 500L,
                    phone = doc.getString("phone") ?: "",
                    altPhone = doc.getString("altPhone") ?: "",
                    email = doc.getString("email") ?: "",
                    batchTimeSlot = doc.getString("batchTimeSlot") ?: "",
                    weeklySchedule = parseWeeklySchedule(doc.get("weeklySchedule"))
                )
                Result.success(student)
            } else {
                Result.failure(NoSuchElementException("Student record not found."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun enrollStudent(student: StudentModel): Result<String> {
        return try {
            val newUserId = firestore.collection("users").document().id
            val data = hashMapOf(
                "fullName" to student.fullName.trim(),
                "studentClass" to student.studentClass.trim(),
                "school" to student.school.trim(),
                "dob" to student.dob.trim(),
                "gender" to student.gender.trim(),
                "admissionDate" to student.admissionDate.trim(),
                "fatherName" to student.fatherName.trim(),
                "motherName" to student.motherName.trim(),
                "monthlyInstallment" to student.monthlyInstallment,
                "phone" to student.phone.trim(),
                "altPhone" to student.altPhone.trim(),
                "email" to student.email.trim(),
                "batchTimeSlot" to student.batchTimeSlot.trim(),
                "weeklySchedule" to student.weeklySchedule,
                "role" to "student",
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            val batch = firestore.batch()
            batch.set(firestore.collection("users").document(newUserId), data)
            batch.set(firestore.collection("students").document(newUserId), data)
            batch.commit().await()
            Result.success(newUserId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStudent(student: StudentModel): Result<Unit> {
        return try {
            val data = hashMapOf(
                "fullName" to student.fullName.trim(),
                "studentClass" to student.studentClass.trim(),
                "school" to student.school.trim(),
                "dob" to student.dob.trim(),
                "gender" to student.gender.trim(),
                "admissionDate" to student.admissionDate.trim(),
                "fatherName" to student.fatherName.trim(),
                "motherName" to student.motherName.trim(),
                "monthlyInstallment" to student.monthlyInstallment,
                "phone" to student.phone.trim(),
                "altPhone" to student.altPhone.trim(),
                "email" to student.email.trim(),
                "batchTimeSlot" to student.batchTimeSlot.trim(),
                "weeklySchedule" to student.weeklySchedule,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            val batch = firestore.batch()
            batch.set(firestore.collection("students").document(student.id), data, com.google.firebase.firestore.SetOptions.merge())
            batch.set(firestore.collection("users").document(student.id), mapOf("fullName" to student.fullName.trim(), "phone" to student.phone.trim()), com.google.firebase.firestore.SetOptions.merge())
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteStudent(studentId: String, fullName: String): Result<Unit> {
        return try {
            val batch = firestore.batch()
            batch.delete(firestore.collection("students").document(studentId))
            batch.delete(firestore.collection("users").document(studentId))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseWeeklySchedule(raw: Any?): Map<String, String> {
        val map = raw as? Map<*, *> ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        for ((key, value) in map) {
            if (key is String && value is String) {
                result[key] = value
            }
        }
        return result
    }
}
