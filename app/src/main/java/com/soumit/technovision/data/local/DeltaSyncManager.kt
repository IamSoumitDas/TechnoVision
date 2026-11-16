package com.soumit.technovision.data.local

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.data.model.FeePaymentModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel
import kotlinx.coroutines.tasks.await
import java.util.Date

class DeltaSyncManager(
    private val localDb: LocalDatabase,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG = "DeltaSyncManager"
        private const val CLOCK_SKEW_BUFFER_MS = 5000L
    }

    suspend fun syncStudents(): Result<Int> {
        return try {
            val lastSync = localDb.getLastSyncTime("students")
            val syncStartTime = System.currentTimeMillis()
            var maxServerTime = syncStartTime

            val query = if (lastSync > 0L) {
                val safeLastSync = (lastSync - CLOCK_SKEW_BUFFER_MS).coerceAtLeast(0L)
                firestore.collection("students")
                    .whereGreaterThan("updatedAt", Timestamp(Date(safeLastSync)))
            } else {
                firestore.collection("students")
            }

            val snapshot = query.get().await()
            var count = 0
            val batchUpserts = mutableListOf<Pair<StudentModel, Long>>()

            for (doc in snapshot.documents) {
                val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: syncStartTime
                if (updatedAt > maxServerTime) {
                    maxServerTime = updatedAt
                }
                val isDeleted = doc.getBoolean("isDeleted") ?: false

                if (isDeleted) {
                    localDb.markStudentDeleted(doc.id, updatedAt)
                } else {
                    val student = parseStudent(doc)
                    batchUpserts.add(Pair(student, updatedAt))
                }
                count++
            }

            if (batchUpserts.isNotEmpty()) {
                localDb.upsertStudentsBatch(batchUpserts)
            }

            localDb.setLastSyncTime("students", maxServerTime)
            Log.d(TAG, "Synced $count student updates (Delta query from lastSync: $lastSync)")
            Result.success(count)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync students: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun syncFees(): Result<Int> {
        return try {
            val lastSync = localDb.getLastSyncTime("fee_payments")
            val syncStartTime = System.currentTimeMillis()
            var maxServerTime = syncStartTime

            val query = if (lastSync > 0L) {
                val safeLastSync = (lastSync - CLOCK_SKEW_BUFFER_MS).coerceAtLeast(0L)
                firestore.collection("fee_payments")
                    .whereGreaterThan("updatedAt", Timestamp(Date(safeLastSync)))
            } else {
                firestore.collection("fee_payments")
            }

            val snapshot = query.get().await()
            var count = 0
            val batchUpserts = mutableListOf<Pair<FeePaymentModel, Long>>()

            for (doc in snapshot.documents) {
                val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: syncStartTime
                if (updatedAt > maxServerTime) {
                    maxServerTime = updatedAt
                }
                val isDeleted = doc.getBoolean("isDeleted") ?: false

                if (isDeleted) {
                    localDb.markFeePaymentDeleted(doc.id, updatedAt)
                } else {
                    val payment = parseFeePayment(doc)
                    batchUpserts.add(Pair(payment, updatedAt))
                }
                count++
            }

            if (batchUpserts.isNotEmpty()) {
                localDb.upsertFeePaymentsBatch(batchUpserts)
            }

            localDb.setLastSyncTime("fee_payments", maxServerTime)
            Log.d(TAG, "Synced $count fee updates (Delta query from lastSync: $lastSync)")
            Result.success(count)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync fees: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun syncAttendance(): Result<Int> {
        return try {
            val lastSync = localDb.getLastSyncTime("attendance")
            val syncStartTime = System.currentTimeMillis()
            var maxServerTime = syncStartTime

            val query = if (lastSync > 0L) {
                val safeLastSync = (lastSync - CLOCK_SKEW_BUFFER_MS).coerceAtLeast(0L)
                firestore.collection("attendance")
                    .whereGreaterThan("updatedAt", Timestamp(Date(safeLastSync)))
            } else {
                firestore.collection("attendance")
            }

            val snapshot = query.get().await()
            var count = 0
            val batchUpserts = mutableListOf<Pair<AttendanceRecordModel, Long>>()

            for (doc in snapshot.documents) {
                val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: syncStartTime
                if (updatedAt > maxServerTime) {
                    maxServerTime = updatedAt
                }
                val isDeleted = doc.getBoolean("isDeleted") ?: false

                if (isDeleted) {
                    localDb.markAttendanceDeleted(doc.id, updatedAt)
                } else {
                    val record = parseAttendance(doc)
                    batchUpserts.add(Pair(record, updatedAt))
                }
                count++
            }

            if (batchUpserts.isNotEmpty()) {
                localDb.upsertAttendanceBatch(batchUpserts)
            }

            localDb.setLastSyncTime("attendance", maxServerTime)
            Log.d(TAG, "Synced $count attendance updates (Delta query from lastSync: $lastSync)")
            Result.success(count)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync attendance: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun syncTeacherAttendance(): Result<Int> {
        return try {
            val lastSync = localDb.getLastSyncTime("teacher_attendance")
            val syncStartTime = System.currentTimeMillis()
            var maxServerTime = syncStartTime

            val query = if (lastSync > 0L) {
                val safeLastSync = (lastSync - CLOCK_SKEW_BUFFER_MS).coerceAtLeast(0L)
                firestore.collection("teacher_attendance")
                    .whereGreaterThan("updatedAt", Timestamp(Date(safeLastSync)))
            } else {
                firestore.collection("teacher_attendance")
            }

            val snapshot = query.get().await()
            var count = 0
            val batchUpserts = mutableListOf<Pair<TeacherAttendanceRequestModel, Long>>()

            for (doc in snapshot.documents) {
                val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: syncStartTime
                if (updatedAt > maxServerTime) {
                    maxServerTime = updatedAt
                }
                val isDeleted = doc.getBoolean("isDeleted") ?: false

                if (isDeleted) {
                    localDb.markTeacherAttendanceDeleted(doc.id, updatedAt)
                } else {
                    val req = parseTeacherAttendance(doc)
                    batchUpserts.add(Pair(req, updatedAt))
                }
                count++
            }

            if (batchUpserts.isNotEmpty()) {
                localDb.upsertTeacherAttendanceBatch(batchUpserts)
            }

            localDb.setLastSyncTime("teacher_attendance", maxServerTime)
            Log.d(TAG, "Synced $count teacher attendance updates (Delta query from lastSync: $lastSync)")
            Result.success(count)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync teacher attendance: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun syncAll(): Result<Unit> {
        syncStudents()
        syncFees()
        syncAttendance()
        syncTeacherAttendance()
        return Result.success(Unit)
    }

    // --- Document Parsers ---

    private fun parseStudent(doc: DocumentSnapshot): StudentModel {
        val rawSched = doc.get("weeklySchedule") as? Map<*, *> ?: emptyMap<String, String>()
        val schedMap = mutableMapOf<String, String>()
        for ((k, v) in rawSched) {
            if (k is String && v is String) {
                schedMap[k] = v
            }
        }
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
            weeklySchedule = schedMap,
            totalPaid = doc.getLong("totalPaid") ?: 0L
        )
    }

    private fun parseFeePayment(doc: DocumentSnapshot): FeePaymentModel {
        return FeePaymentModel(
            id = doc.id,
            studentId = doc.getString("studentId") ?: "",
            studentName = doc.getString("studentName") ?: "",
            amountPaid = doc.getLong("amountPaid") ?: 0L,
            paymentMethod = doc.getString("paymentMethod") ?: "",
            remarks = doc.getString("remarks") ?: "",
            receivedBy = doc.getString("receivedBy") ?: "",
            timestamp = doc.getTimestamp("timestamp")
        )
    }

    private fun parseAttendance(doc: DocumentSnapshot): AttendanceRecordModel {
        return AttendanceRecordModel(
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

    private fun parseTeacherAttendance(doc: DocumentSnapshot): TeacherAttendanceRequestModel {
        return TeacherAttendanceRequestModel(
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
}
