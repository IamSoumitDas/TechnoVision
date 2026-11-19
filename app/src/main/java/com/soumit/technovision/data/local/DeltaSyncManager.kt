package com.soumit.technovision.data.local

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.data.model.FeePaymentModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.Date

class DeltaSyncManager(
    private val localDb: LocalDatabase,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG = "DeltaSyncManager"
        private const val CLOCK_SKEW_BUFFER_MS = 5000L
        private const val SYNC_THROTTLE_MS = 30_000L // 30-second cooldown per collection
    }

    private val lastSyncAttempt = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val activeListeners = mutableListOf<ListenerRegistration>()
    private val listenerLock = Any()
    private var liveSyncScope: CoroutineScope? = null

    suspend fun syncStudents(force: Boolean = false): Result<Int> {
        val now = System.currentTimeMillis()
        val lastAttempt = lastSyncAttempt["students"] ?: 0L
        if (!force && now - lastAttempt < SYNC_THROTTLE_MS) {
            return Result.success(0)
        }
        lastSyncAttempt["students"] = now

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
            if (snapshot.isEmpty) {
                localDb.setLastSyncTime("students", maxServerTime)
                return Result.success(0)
            }

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

    suspend fun syncFees(force: Boolean = false): Result<Int> {
        val now = System.currentTimeMillis()
        val lastAttempt = lastSyncAttempt["fee_payments"] ?: 0L
        if (!force && now - lastAttempt < SYNC_THROTTLE_MS) {
            return Result.success(0)
        }
        lastSyncAttempt["fee_payments"] = now

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
            if (snapshot.isEmpty) {
                localDb.setLastSyncTime("fee_payments", maxServerTime)
                return Result.success(0)
            }

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

    suspend fun syncAttendance(force: Boolean = false): Result<Int> {
        val now = System.currentTimeMillis()
        val lastAttempt = lastSyncAttempt["attendance"] ?: 0L
        if (!force && now - lastAttempt < SYNC_THROTTLE_MS) {
            return Result.success(0)
        }
        lastSyncAttempt["attendance"] = now

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
            if (snapshot.isEmpty) {
                localDb.setLastSyncTime("attendance", maxServerTime)
                return Result.success(0)
            }

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

    suspend fun syncTeacherAttendance(force: Boolean = false): Result<Int> {
        val now = System.currentTimeMillis()
        val lastAttempt = lastSyncAttempt["teacher_attendance"] ?: 0L
        if (!force && now - lastAttempt < SYNC_THROTTLE_MS) {
            return Result.success(0)
        }
        lastSyncAttempt["teacher_attendance"] = now

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
            if (snapshot.isEmpty) {
                localDb.setLastSyncTime("teacher_attendance", maxServerTime)
                return Result.success(0)
            }

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

    suspend fun syncAll(force: Boolean = false): Result<Unit> {
        syncStudents(force)
        syncFees(force)
        syncAttendance(force)
        syncTeacherAttendance(force)
        return Result.success(Unit)
    }

    fun startLiveDeltaSync(scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())) {
        synchronized(listenerLock) {
            if (activeListeners.isNotEmpty()) {
                Log.d(TAG, "Live delta sync is already running.")
                return
            }
            liveSyncScope = scope
        }

        // 1. Initial catch-up delta sync
        scope.launch {
            try {
                syncAll(force = false)
            } catch (e: Exception) {
                Log.w(TAG, "Initial delta catch-up failed: ${e.localizedMessage}")
            }
        }

        // 2. Attach live delta stream listeners
        val startTs = Timestamp(Date((System.currentTimeMillis() - CLOCK_SKEW_BUFFER_MS).coerceAtLeast(0L)))

        // Students live listener
        val studentsReg = firestore.collection("students")
            .whereGreaterThan("updatedAt", startTs)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Live students stream error: ${error.localizedMessage}")
                    return@addSnapshotListener
                }
                if (snapshot == null || snapshot.isEmpty) return@addSnapshotListener
                scope.launch {
                    try {
                        var maxServerTime = 0L
                        val batchUpserts = mutableListOf<Pair<StudentModel, Long>>()
                        for (doc in snapshot.documents) {
                            val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
                            if (updatedAt > maxServerTime) maxServerTime = updatedAt
                            if (doc.getBoolean("isDeleted") == true) {
                                localDb.markStudentDeleted(doc.id, updatedAt)
                            } else {
                                batchUpserts.add(Pair(parseStudent(doc), updatedAt))
                            }
                        }
                        if (batchUpserts.isNotEmpty()) {
                            localDb.upsertStudentsBatch(batchUpserts)
                        }
                        if (maxServerTime > 0L) {
                            localDb.setLastSyncTime("students", maxServerTime)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed processing live student updates: ${e.localizedMessage}")
                    }
                }
            }

        // Fee payments live listener
        val feesReg = firestore.collection("fee_payments")
            .whereGreaterThan("updatedAt", startTs)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Live fee stream error: ${error.localizedMessage}")
                    return@addSnapshotListener
                }
                if (snapshot == null || snapshot.isEmpty) return@addSnapshotListener
                scope.launch {
                    try {
                        var maxServerTime = 0L
                        val batchUpserts = mutableListOf<Pair<FeePaymentModel, Long>>()
                        for (doc in snapshot.documents) {
                            val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
                            if (updatedAt > maxServerTime) maxServerTime = updatedAt
                            if (doc.getBoolean("isDeleted") == true) {
                                localDb.markFeePaymentDeleted(doc.id, updatedAt)
                            } else {
                                batchUpserts.add(Pair(parseFeePayment(doc), updatedAt))
                            }
                        }
                        if (batchUpserts.isNotEmpty()) {
                            localDb.upsertFeePaymentsBatch(batchUpserts)
                        }
                        if (maxServerTime > 0L) {
                            localDb.setLastSyncTime("fee_payments", maxServerTime)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed processing live fee updates: ${e.localizedMessage}")
                    }
                }
            }

        // Attendance live listener
        val attendanceReg = firestore.collection("attendance")
            .whereGreaterThan("updatedAt", startTs)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Live attendance stream error: ${error.localizedMessage}")
                    return@addSnapshotListener
                }
                if (snapshot == null || snapshot.isEmpty) return@addSnapshotListener
                scope.launch {
                    try {
                        var maxServerTime = 0L
                        val batchUpserts = mutableListOf<Pair<AttendanceRecordModel, Long>>()
                        for (doc in snapshot.documents) {
                            val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
                            if (updatedAt > maxServerTime) maxServerTime = updatedAt
                            if (doc.getBoolean("isDeleted") == true) {
                                localDb.markAttendanceDeleted(doc.id, updatedAt)
                            } else {
                                batchUpserts.add(Pair(parseAttendance(doc), updatedAt))
                            }
                        }
                        if (batchUpserts.isNotEmpty()) {
                            localDb.upsertAttendanceBatch(batchUpserts)
                        }
                        if (maxServerTime > 0L) {
                            localDb.setLastSyncTime("attendance", maxServerTime)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed processing live attendance updates: ${e.localizedMessage}")
                    }
                }
            }

        // Teacher attendance live listener
        val teacherAttendanceReg = firestore.collection("teacher_attendance")
            .whereGreaterThan("updatedAt", startTs)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Live teacher attendance stream error: ${error.localizedMessage}")
                    return@addSnapshotListener
                }
                if (snapshot == null || snapshot.isEmpty) return@addSnapshotListener
                scope.launch {
                    try {
                        var maxServerTime = 0L
                        val batchUpserts = mutableListOf<Pair<TeacherAttendanceRequestModel, Long>>()
                        for (doc in snapshot.documents) {
                            val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
                            if (updatedAt > maxServerTime) maxServerTime = updatedAt
                            if (doc.getBoolean("isDeleted") == true) {
                                localDb.markTeacherAttendanceDeleted(doc.id, updatedAt)
                            } else {
                                batchUpserts.add(Pair(parseTeacherAttendance(doc), updatedAt))
                            }
                        }
                        if (batchUpserts.isNotEmpty()) {
                            localDb.upsertTeacherAttendanceBatch(batchUpserts)
                        }
                        if (maxServerTime > 0L) {
                            localDb.setLastSyncTime("teacher_attendance", maxServerTime)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed processing live teacher attendance updates: ${e.localizedMessage}")
                    }
                }
            }

        synchronized(listenerLock) {
            activeListeners.add(studentsReg)
            activeListeners.add(feesReg)
            activeListeners.add(attendanceReg)
            activeListeners.add(teacherAttendanceReg)
        }
        Log.d(TAG, "Started live delta sync listeners.")
        scope.launch { flushOfflineSyncQueue() }
    }

    private val flushMutex = Mutex()

    suspend fun flushOfflineSyncQueue(): Result<Int> = withContext(Dispatchers.IO) {
        if (!flushMutex.tryLock()) {
            Log.d(TAG, "flushOfflineSyncQueue already running, skipping concurrent trigger.")
            return@withContext Result.success(0)
        }
        try {
            val pending = localDb.getPendingSyncItems()
            if (pending.isEmpty()) return@withContext Result.success(0)

            var flushedCount = 0
            for (item in pending) {
                if (item.entityType == "TEACHER_ATTENDANCE") {
                    val req = localDb.teacherAttendanceFromJsonPublic(item.payloadJson)
                    if (req != null) {
                        try {
                            localDb.syncQueueDao.updateStatus(item.id, "PROCESSING")
                            val now = Timestamp.now()
                            val data = hashMapOf(
                                "teacherId" to req.teacherId,
                                "teacherName" to req.teacherName,
                                "date" to req.date,
                                "timeSlot" to req.timeSlot,
                                "status" to req.status.replace(" (Offline)", ""),
                                "timestamp" to (req.timestamp ?: now),
                                "updatedAt" to now,
                                "isDeleted" to false
                            )
                            val docRef = if (req.id.startsWith("offline_")) {
                                firestore.collection("teacher_attendance").document()
                            } else {
                                firestore.collection("teacher_attendance").document(req.id)
                            }

                            withTimeout(7000L) {
                                docRef.set(data).await()
                            }

                            val updatedReq = req.copy(
                                id = docRef.id,
                                status = req.status.replace(" (Offline)", "")
                            )
                            if (req.id != docRef.id) {
                                localDb.deleteTeacherAttendancePermanently(req.id)
                            }
                            localDb.upsertTeacherAttendance(updatedReq, now.toDate().time)
                            localDb.removeSyncItem(item.id)
                            flushedCount++
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed syncing item ${item.id}: ${e.localizedMessage}")
                            localDb.syncQueueDao.updateStatus(item.id, "PENDING")
                        }
                    } else {
                        localDb.removeSyncItem(item.id)
                    }
                }
            }
            Result.success(flushedCount)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            flushMutex.unlock()
        }
    }

    fun stopLiveDeltaSync() {
        synchronized(listenerLock) {
            for (listener in activeListeners) {
                try {
                    listener.remove()
                } catch (e: Exception) {
                    Log.w(TAG, "Error removing listener: ${e.localizedMessage}")
                }
            }
            activeListeners.clear()
            liveSyncScope = null
        }
        Log.d(TAG, "Stopped live delta sync listeners.")
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
