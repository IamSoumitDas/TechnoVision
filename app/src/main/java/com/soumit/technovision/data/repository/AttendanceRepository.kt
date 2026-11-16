package com.soumit.technovision.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface AttendanceRepository {
    fun getAttendanceRecordsStream(): Flow<List<AttendanceRecordModel>>
    suspend fun getExistingAttendance(date: String, timeSlot: String): Result<AttendanceRecordModel?>
    suspend fun saveBatchAttendance(record: AttendanceRecordModel): Result<Unit>

    fun getTeacherAttendanceRequestsStream(): Flow<List<TeacherAttendanceRequestModel>>
    suspend fun submitTeacherAttendanceRequest(request: TeacherAttendanceRequestModel): Result<String>
    suspend fun verifyTeacherAttendance(requestId: String, verifiedBy: String): Result<Unit>
    suspend fun rejectTeacherAttendance(requestId: String, rejectedBy: String): Result<Unit>
}

class FirestoreAttendanceRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AttendanceRepository {

    override fun getAttendanceRecordsStream(): Flow<List<AttendanceRecordModel>> = callbackFlow {
        val subscription = firestore.collection("attendance")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val records = snapshot.documents.map { doc ->
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

    override suspend fun getExistingAttendance(date: String, timeSlot: String): Result<AttendanceRecordModel?> {
        return try {
            val slotIdTag = timeSlot.replace(Regex("[^A-Za-z0-9]"), "_")
            val docId = "${date}_$slotIdTag"
            val doc = firestore.collection("attendance").document(docId).get().await()
            if (doc.exists()) {
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
            val slotIdTag = record.timeSlot.replace(Regex("[^A-Za-z0-9]"), "_")
            val docId = "${record.date}_$slotIdTag"
            val docRef = firestore.collection("attendance").document(docId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
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
                    "timestamp" to (record.timestamp ?: Timestamp.now())
                )
                transaction.set(docRef, data)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getTeacherAttendanceRequestsStream(): Flow<List<TeacherAttendanceRequestModel>> = callbackFlow {
        val subscription = firestore.collection("teacher_attendance")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val requests = snapshot.documents.map { doc ->
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
                    }
                    trySend(requests)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun submitTeacherAttendanceRequest(request: TeacherAttendanceRequestModel): Result<String> {
        return try {
            val data = hashMapOf(
                "teacherId" to request.teacherId,
                "teacherName" to request.teacherName,
                "date" to request.date,
                "timeSlot" to request.timeSlot,
                "status" to "Pending",
                "timestamp" to (request.timestamp ?: Timestamp.now())
            )
            val docRef = firestore.collection("teacher_attendance").add(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyTeacherAttendance(requestId: String, verifiedBy: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to "Verified",
                "verifiedBy" to verifiedBy
            )
            firestore.collection("teacher_attendance").document(requestId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectTeacherAttendance(requestId: String, rejectedBy: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to "Rejected",
                "rejectedBy" to rejectedBy
            )
            firestore.collection("teacher_attendance").document(requestId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
