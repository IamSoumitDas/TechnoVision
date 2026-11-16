package com.soumit.technovision.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soumit.technovision.TechnoVisionApplication
import com.soumit.technovision.data.local.DeltaSyncManager
import com.soumit.technovision.data.local.LocalDatabase
import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

interface AttendanceRepository {
    fun getAttendanceRecordsStream(): Flow<List<AttendanceRecordModel>>
    suspend fun getExistingAttendance(date: String, timeSlot: String): Result<AttendanceRecordModel?>
    suspend fun saveBatchAttendance(record: AttendanceRecordModel): Result<Unit>

    fun getTeacherAttendanceRequestsStream(teacherId: String? = null): Flow<List<TeacherAttendanceRequestModel>>
    suspend fun submitTeacherAttendanceRequest(request: TeacherAttendanceRequestModel): Result<String>
    suspend fun verifyTeacherAttendance(requestId: String, verifiedBy: String): Result<Unit>
    suspend fun rejectTeacherAttendance(requestId: String, rejectedBy: String): Result<Unit>
}

class FirestoreAttendanceRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val localDb: LocalDatabase? = null,
    private val syncManager: DeltaSyncManager? = null
) : AttendanceRepository {

    private val db: LocalDatabase?
        get() = localDb ?: TechnoVisionApplication.instance?.container?.localDatabase

    private val sync: DeltaSyncManager?
        get() = syncManager ?: TechnoVisionApplication.instance?.container?.deltaSyncManager

    override fun getAttendanceRecordsStream(): Flow<List<AttendanceRecordModel>> = callbackFlow {
        val database = db
        if (database != null) {
            trySend(database.getAllAttendanceRecords())

            val job = launch {
                database.attendanceUpdateFlow.collect {
                    trySend(database.getAllAttendanceRecords())
                }
            }

            launch(Dispatchers.IO) {
                sync?.syncAttendance()
                trySend(database.getAllAttendanceRecords())
            }

            awaitClose { job.cancel() }
        } else {
            val subscription = firestore.collection("attendance")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val records = snapshot.documents
                            .filter { it.getBoolean("isDeleted") != true }
                            .map { doc ->
                                AttendanceRecordModel(
                                    id = doc.id,
                                    date = doc.getString("date") ?: "",
                                    timeSlot = doc.getString("timeSlot") ?: "",
                                    presentStudentIds = (doc.get("presentStudentIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                                    absentStudentIds = (doc.get("absentStudentIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                                    additionalStudentIds = (doc.get("additionalStudentIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                                    totalPresent = (doc.getLong("totalPresent") ?: 0L).toInt(),
                                    totalAbsent = (doc.getLong("totalAbsent") ?: 0L).toInt(),
                                    submittedBy = doc.getString("submittedBy") ?: "",
                                    submittedByUid = doc.getString("submittedByUid") ?: "",
                                    submittedByEmail = doc.getString("submittedByEmail") ?: doc.getString("takenBy") ?: "",
                                    timestamp = doc.getTimestamp("timestamp")
                                )
                            }
                        trySend(records)
                    }
                }
            awaitClose { subscription.remove() }
        }
    }

    override suspend fun getExistingAttendance(date: String, timeSlot: String): Result<AttendanceRecordModel?> {
        val local = db?.getExistingAttendance(date, timeSlot)
        if (local != null) {
            return Result.success(local)
        }
        return try {
            val cleanDate = date.trim().replace('/', '-')
            val slotIdTag = timeSlot.replace(Regex("[^A-Za-z0-9]"), "_")
            val docId = "${cleanDate}_$slotIdTag"
            val doc = firestore.collection("attendance").document(docId).get().await()
            if (doc.exists() && doc.getBoolean("isDeleted") != true) {
                val record = AttendanceRecordModel(
                    id = doc.id,
                    date = doc.getString("date") ?: "",
                    timeSlot = doc.getString("timeSlot") ?: "",
                    presentStudentIds = (doc.get("presentStudentIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    absentStudentIds = (doc.get("absentStudentIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    additionalStudentIds = (doc.get("additionalStudentIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    totalPresent = (doc.getLong("totalPresent") ?: 0L).toInt(),
                    totalAbsent = (doc.getLong("totalAbsent") ?: 0L).toInt(),
                    submittedBy = doc.getString("submittedBy") ?: "",
                    submittedByUid = doc.getString("submittedByUid") ?: "",
                    submittedByEmail = doc.getString("submittedByEmail") ?: doc.getString("takenBy") ?: "",
                    timestamp = doc.getTimestamp("timestamp")
                )
                db?.upsertAttendanceRecord(record)
                Result.success(record)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveBatchAttendance(record: AttendanceRecordModel): Result<Unit> {
        return try {
            val cleanDate = record.date.trim().replace('/', '-')
            val slotIdTag = record.timeSlot.replace(Regex("[^A-Za-z0-9]"), "_")
            val docId = "${cleanDate}_$slotIdTag"
            val docRef = firestore.collection("attendance").document(docId)
            val now = Timestamp.now()

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists() && snapshot.getBoolean("isDeleted") != true) {
                    val recorder = snapshot.getString("submittedBy")
                        ?: snapshot.getString("takenBy")
                        ?: "another user"
                    throw IllegalStateException("Attendance for this batch has already been recorded by $recorder.")
                }

                val data = hashMapOf(
                    "date" to record.date,
                    "timeSlot" to record.timeSlot,
                    "presentStudentIds" to record.presentStudentIds,
                    "absentStudentIds" to record.absentStudentIds,
                    "additionalStudentIds" to record.additionalStudentIds,
                    "totalPresent" to record.totalPresent,
                    "totalAbsent" to record.totalAbsent,
                    "submittedBy" to record.submittedBy,
                    "submittedByUid" to record.submittedByUid,
                    "submittedByEmail" to record.submittedByEmail,
                    "timestamp" to (record.timestamp ?: now),
                    "updatedAt" to now,
                    "isDeleted" to false
                )
                transaction.set(docRef, data)
            }.await()

            db?.upsertAttendanceRecord(record.copy(id = docId), now.toDate().time)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getTeacherAttendanceRequestsStream(teacherId: String?): Flow<List<TeacherAttendanceRequestModel>> = callbackFlow {
        val database = db
        if (database != null) {
            trySend(database.getAllTeacherAttendance(teacherId))

            val job = launch {
                database.teacherAttendanceUpdateFlow.collect {
                    trySend(database.getAllTeacherAttendance(teacherId))
                }
            }

            launch(Dispatchers.IO) {
                sync?.syncTeacherAttendance()
                trySend(database.getAllTeacherAttendance(teacherId))
            }

            awaitClose { job.cancel() }
        } else {
            val query = if (!teacherId.isNullOrBlank()) {
                firestore.collection("teacher_attendance")
                    .whereEqualTo("teacherId", teacherId)
            } else {
                firestore.collection("teacher_attendance")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
            }

            val subscription = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val requests = snapshot.documents
                        .filter { it.getBoolean("isDeleted") != true }
                        .map { doc ->
                            TeacherAttendanceRequestModel(
                                id = doc.id,
                                teacherId = doc.getString("teacherId") ?: "",
                                teacherName = doc.getString("teacherName") ?: "",
                                date = doc.getString("date") ?: "",
                                timeSlot = doc.getString("timeSlot") ?: "",
                                status = doc.getString("status") ?: "Pending",
                                timestamp = doc.getTimestamp("timestamp"),
                                verifiedBy = doc.getString("verifiedBy") ?: "",
                                rejectedBy = doc.getString("rejectedBy") ?: ""
                            )
                        }.sortedByDescending { it.timestamp?.seconds ?: 0L }
                    trySend(requests)
                }
            }
            awaitClose { subscription.remove() }
        }
    }

    override suspend fun submitTeacherAttendanceRequest(request: TeacherAttendanceRequestModel): Result<String> {
        return try {
            val now = Timestamp.now()
            val data = hashMapOf(
                "teacherId" to request.teacherId,
                "teacherName" to request.teacherName,
                "date" to request.date,
                "timeSlot" to request.timeSlot,
                "status" to "Pending",
                "timestamp" to (request.timestamp ?: now),
                "updatedAt" to now,
                "isDeleted" to false
            )
            val docRef = firestore.collection("teacher_attendance").add(data).await()
            db?.upsertTeacherAttendance(request.copy(id = docRef.id, status = "Pending"), now.toDate().time)
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyTeacherAttendance(requestId: String, verifiedBy: String): Result<Unit> {
        return try {
            val now = Timestamp.now()
            val updates = mapOf(
                "status" to "Verified",
                "verifiedBy" to verifiedBy,
                "updatedAt" to now
            )
            firestore.collection("teacher_attendance").document(requestId).update(updates).await()

            // Update in local DB
            val existing = db?.getTeacherAttendance(requestId)
            if (existing != null) {
                db?.upsertTeacherAttendance(existing.copy(status = "Verified", verifiedBy = verifiedBy), now.toDate().time)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectTeacherAttendance(requestId: String, rejectedBy: String): Result<Unit> {
        return try {
            val now = Timestamp.now()
            val updates = mapOf(
                "status" to "Rejected",
                "rejectedBy" to rejectedBy,
                "updatedAt" to now
            )
            firestore.collection("teacher_attendance").document(requestId).update(updates).await()

            val existing = db?.getTeacherAttendance(requestId)
            if (existing != null) {
                db?.upsertTeacherAttendance(existing.copy(status = "Rejected", rejectedBy = rejectedBy), now.toDate().time)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
