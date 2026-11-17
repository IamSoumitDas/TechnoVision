package com.soumit.technovision.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soumit.technovision.TechnoVisionApplication
import com.soumit.technovision.data.local.DeltaSyncManager
import com.soumit.technovision.data.local.LocalDatabase
import com.soumit.technovision.data.model.StudentModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

interface StudentRepository {
    fun getStudentsStream(): Flow<List<StudentModel>>
    suspend fun getStudentsOnce(): Result<List<StudentModel>>
    fun getStudentStream(studentId: String): Flow<StudentModel?>
    suspend fun getStudent(studentId: String): Result<StudentModel>
    suspend fun enrollStudent(student: StudentModel): Result<String>
    suspend fun updateStudent(student: StudentModel): Result<Unit>
    suspend fun deleteStudent(studentId: String, fullName: String): Result<Unit>
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FirestoreStudentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val localDb: LocalDatabase? = null,
    private val syncManager: DeltaSyncManager? = null
) : StudentRepository {

    private val db: LocalDatabase?
        get() = localDb ?: TechnoVisionApplication.instance?.container?.localDatabase

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _studentsFlow = MutableStateFlow<List<StudentModel>>(emptyList())
    private val _isLoaded = MutableStateFlow(false)

    init {
        repositoryScope.launch {
            val database = db
            if (database != null) {
                _studentsFlow.value = database.getAllStudents()
                _isLoaded.value = true
                database.studentsUpdateFlow.collect {
                    _studentsFlow.value = database.getAllStudents()
                }
            } else {
                _isLoaded.value = true
            }
        }
    }

    override fun getStudentsStream(): Flow<List<StudentModel>> {
        if (db == null) {
            return callbackFlow {
                val subscription = firestore.collection("students")
                    .orderBy("fullName", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val students = snapshot.documents
                                .filter { it.getBoolean("isDeleted") != true }
                                .map { doc -> parseStudent(doc) }
                            trySend(students)
                        }
                    }
                awaitClose { subscription.remove() }
            }
        }
        return _isLoaded.filter { it }.flatMapLatest { _studentsFlow }
    }

    override suspend fun getStudentsOnce(): Result<List<StudentModel>> {
        val inMemory = _studentsFlow.value
        if (inMemory.isNotEmpty()) {
            return Result.success(inMemory)
        }
        val database = db
        if (database != null) {
            val localList = database.getAllStudents()
            if (localList.isNotEmpty()) {
                return Result.success(localList)
            }
        }
        return try {
            val snapshot = firestore.collection("students")
                .orderBy("fullName", Query.Direction.ASCENDING)
                .get()
                .await()
            val list = snapshot.documents
                .filter { it.getBoolean("isDeleted") != true }
                .map { doc -> parseStudent(doc) }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getStudentStream(studentId: String): Flow<StudentModel?> {
        if (studentId.isBlank()) return flowOf(null)

        if (db == null) {
            return callbackFlow {
                val subscription = firestore.collection("students").document(studentId)
                    .addSnapshotListener { doc, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        if (doc != null && doc.exists() && doc.getBoolean("isDeleted") != true) {
                            trySend(parseStudent(doc))
                        } else {
                            trySend(null)
                        }
                    }
                awaitClose { subscription.remove() }
            }
        }
        return _isLoaded.filter { it }.flatMapLatest {
            _studentsFlow.map { list -> list.find { it.id == studentId } }
        }
    }

    override suspend fun getStudent(studentId: String): Result<StudentModel> {
        val inMemory = _studentsFlow.value.find { it.id == studentId }
        if (inMemory != null) {
            return Result.success(inMemory)
        }
        val local = db?.getStudent(studentId)
        if (local != null) {
            return Result.success(local)
        }
        return try {
            val doc = firestore.collection("students").document(studentId).get().await()
            if (doc.exists() && doc.getBoolean("isDeleted") != true) {
                val student = parseStudent(doc)
                db?.upsertStudent(student)
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
            val now = Timestamp.now()
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
                "totalPaid" to 0L,
                "isDeleted" to false,
                "updatedAt" to now,
                "createdAt" to now
            )
            val batch = firestore.batch()
            batch.set(firestore.collection("users").document(newUserId), data)
            batch.set(firestore.collection("students").document(newUserId), data)
            batch.commit().await()

            val enrolled = student.copy(id = newUserId, totalPaid = 0L)
            db?.upsertStudent(enrolled, now.toDate().time)
            _studentsFlow.value = (listOf(enrolled) + _studentsFlow.value.filter { it.id != newUserId })
                .sortedBy { it.fullName.lowercase() }

            Result.success(newUserId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStudent(student: StudentModel): Result<Unit> {
        return try {
            val now = Timestamp.now()
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
                "isDeleted" to false,
                "updatedAt" to now
            )
            val batch = firestore.batch()
            batch.set(firestore.collection("students").document(student.id), data, com.google.firebase.firestore.SetOptions.merge())
            batch.set(firestore.collection("users").document(student.id), mapOf("fullName" to student.fullName.trim(), "phone" to student.phone.trim()), com.google.firebase.firestore.SetOptions.merge())
            batch.commit().await()

            val currentTotalPaid = db?.getStudent(student.id)?.totalPaid ?: student.totalPaid
            val resolvedStudent = if (student.totalPaid == 0L && currentTotalPaid > 0L) {
                student.copy(totalPaid = currentTotalPaid)
            } else {
                student
            }
            db?.upsertStudent(resolvedStudent, now.toDate().time)
            _studentsFlow.value = _studentsFlow.value.map {
                if (it.id == resolvedStudent.id) resolvedStudent else it
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteStudent(studentId: String, fullName: String): Result<Unit> {
        return try {
            val now = Timestamp.now()
            val batch = firestore.batch()
            batch.update(firestore.collection("students").document(studentId), mapOf("isDeleted" to true, "updatedAt" to now))
            batch.delete(firestore.collection("users").document(studentId))
            batch.commit().await()

            db?.markStudentDeleted(studentId, now.toDate().time)
            _studentsFlow.value = _studentsFlow.value.filter { it.id != studentId }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseStudent(doc: DocumentSnapshot): StudentModel {
        return StudentModel(
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
            weeklySchedule = parseWeeklySchedule(doc.get("weeklySchedule")),
            totalPaid = doc.getLong("totalPaid") ?: 0L
        )
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
