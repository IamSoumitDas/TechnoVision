package com.soumit.technovision.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.soumit.technovision.TechnoVisionApplication
import com.soumit.technovision.data.local.DeltaSyncManager
import com.soumit.technovision.data.local.LocalDatabase
import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel
import com.soumit.technovision.util.getSafeLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

interface AttendanceRepository {
    fun getAttendanceRecordsStream(): Flow<List<AttendanceRecordModel>>
    suspend fun getExistingAttendance(date: String, timeSlot: String): Result<AttendanceRecordModel?>
    suspend fun saveBatchAttendance(record: AttendanceRecordModel): Result<Unit>

    fun getTeacherAttendanceRequestsStream(teacherId: String? = null): Flow<List<TeacherAttendanceRequestModel>>
    suspend fun submitTeacherAttendanceRequest(request: TeacherAttendanceRequestModel): Result<String>
    suspend fun verifyTeacherAttendance(requestId: String, verifiedBy: String): Result<Unit>
    suspend fun rejectTeacherAttendance(requestId: String, rejectedBy: String): Result<Unit>
}

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreAttendanceRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val localDb: LocalDatabase? = null,
    private val syncManager: DeltaSyncManager? = null
) : AttendanceRepository {

    private val db: LocalDatabase?
        get() = localDb ?: TechnoVisionApplication.instance?.container?.localDatabase

    private val sync: DeltaSyncManager?
        get() = syncManager ?: TechnoVisionApplication.instance?.container?.deltaSyncManager

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _attendanceRecordsFlow = MutableStateFlow<List<AttendanceRecordModel>>(emptyList())
    private val _teacherAttendanceFlow = MutableStateFlow<List<TeacherAttendanceRequestModel>>(emptyList())
    private val _isLoaded = MutableStateFlow(false)

    init {
        repositoryScope.launch {
            val database = db
            if (database != null) {
                launch {
                    database.attendanceFlow.collect { records ->
                        _attendanceRecordsFlow.value = records
                        _isLoaded.value = true
                    }
                }
                launch {
                    database.teacherAttendanceFlow.collect { reqs ->
                        _teacherAttendanceFlow.value = reqs
                    }
                }
            } else {
                _isLoaded.value = true
            }
        }
    }

    override fun getAttendanceRecordsStream(): Flow<List<AttendanceRecordModel>> {
        if (db == null) {
            return callbackFlow {
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
                                .map { doc -> parseAttendance(doc) }
                            trySend(records)
                        }
                    }
                awaitClose { subscription.remove() }
            }
        }
        return _isLoaded.filter { it }.flatMapLatest { _attendanceRecordsFlow }
    }

    override suspend fun getExistingAttendance(date: String, timeSlot: String): Result<AttendanceRecordModel?> {
        val cleanDate = date.trim().replace('/', '-')
        val inMemory = _attendanceRecordsFlow.value.find {
            it.date.trim().replace('/', '-') == cleanDate && it.timeSlot.trim().equals(timeSlot.trim(), ignoreCase = true)
        }
        if (inMemory != null) {
            return Result.success(inMemory)
        }

        val local = db?.getExistingAttendance(date, timeSlot)
        if (local != null) {
            return Result.success(local)
        }
        return try {
            val slotIdTag = timeSlot.replace(Regex("[^A-Za-z0-9]"), "_")
            val docId = "${cleanDate}_$slotIdTag"
            val doc = firestore.collection("attendance").document(docId).get().await()
            if (doc.exists() && doc.getBoolean("isDeleted") != true) {
                val record = parseAttendance(doc)
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

            val saved = record.copy(id = docId)
            db?.upsertAttendanceRecord(saved, now.toDate().time)
            _attendanceRecordsFlow.value = (listOf(saved) + _attendanceRecordsFlow.value.filter { it.id != docId })
                .sortedByDescending { it.timestamp?.seconds ?: 0L }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getTeacherAttendanceRequestsStream(teacherId: String?): Flow<List<TeacherAttendanceRequestModel>> {
        if (db == null) {
            return callbackFlow {
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
                            .map { doc -> parseTeacherAttendance(doc) }
                            .sortedByDescending { it.timestamp?.seconds ?: 0L }
                        trySend(requests)
                    }
                }
                awaitClose { subscription.remove() }
            }
        }

        return _isLoaded.filter { it }.flatMapLatest {
            if (!teacherId.isNullOrBlank()) {
                _teacherAttendanceFlow.map { list -> list.filter { it.teacherId == teacherId } }
            } else {
                _teacherAttendanceFlow
            }
        }
    }

    override suspend fun submitTeacherAttendanceRequest(request: TeacherAttendanceRequestModel): Result<String> {
        val now = Timestamp.now()
        val tempId = if (request.id.isNotBlank()) request.id else "offline_${System.currentTimeMillis()}"
        val offlineRequest = request.copy(id = tempId, status = "Pending (Offline)", timestamp = now)

        val isOnline = TechnoVisionApplication.instance?.container?.networkMonitor?.isOnline?.value ?: true
        val database = db

        // If known to be offline, save locally and enqueue immediately without blocking
        if (!isOnline && database != null) {
            database.upsertTeacherAttendance(offlineRequest, now.toDate().time)
            database.enqueueSyncItem(
                entityType = "TEACHER_ATTENDANCE",
                docId = tempId,
                payloadJson = database.teacherAttendanceToJsonPublic(offlineRequest)
            )
            _teacherAttendanceFlow.value = (listOf(offlineRequest) + _teacherAttendanceFlow.value.filter { it.id != tempId })
                .sortedByDescending { it.timestamp?.seconds ?: 0L }
            return Result.success(tempId)
        }

        return try {
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
            val docRef = withTimeout(5000L) {
                firestore.collection("teacher_attendance").add(data).await()
            }
            val saved = request.copy(id = docRef.id, status = "Pending", timestamp = (request.timestamp ?: now))
            db?.upsertTeacherAttendance(saved, now.toDate().time)
            _teacherAttendanceFlow.value = (listOf(saved) + _teacherAttendanceFlow.value.filter { it.id != saved.id })
                .sortedByDescending { it.timestamp?.seconds ?: 0L }

            Result.success(docRef.id)
        } catch (e: Exception) {
            if (database != null) {
                database.upsertTeacherAttendance(offlineRequest, now.toDate().time)
                database.enqueueSyncItem(
                    entityType = "TEACHER_ATTENDANCE",
                    docId = tempId,
                    payloadJson = database.teacherAttendanceToJsonPublic(offlineRequest)
                )
                _teacherAttendanceFlow.value = (listOf(offlineRequest) + _teacherAttendanceFlow.value.filter { it.id != tempId })
                    .sortedByDescending { it.timestamp?.seconds ?: 0L }
                Result.success(tempId)
            } else {
                Result.failure(e)
            }
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
                val updated = existing.copy(status = "Verified", verifiedBy = verifiedBy)
                db?.upsertTeacherAttendance(updated, now.toDate().time)
                _teacherAttendanceFlow.value = _teacherAttendanceFlow.value.map {
                    if (it.id == requestId) updated else it
                }
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
                val updated = existing.copy(status = "Rejected", rejectedBy = rejectedBy)
                db?.upsertTeacherAttendance(updated, now.toDate().time)
                _teacherAttendanceFlow.value = _teacherAttendanceFlow.value.map {
                    if (it.id == requestId) updated else it
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseAttendance(doc: DocumentSnapshot): AttendanceRecordModel {
        val submittedBy = doc.getString("submittedBy")
            ?: doc.getString("submittedByName")
            ?: doc.getString("teacherName")
            ?: doc.getString("facultyName")
            ?: doc.getString("staffName")
            ?: doc.getString("userName")
            ?: doc.getString("recordedBy")
            ?: doc.getString("createdBy")
            ?: doc.getString("takenBy")?.takeIf { !it.contains("@") }
            ?: ""
        val submittedByEmail = doc.getString("submittedByEmail")
            ?: doc.getString("facultyEmail")
            ?: doc.getString("teacherEmail")
            ?: doc.getString("email")
            ?: doc.getString("userEmail")
            ?: doc.getString("takenBy")?.takeIf { it.contains("@") }
            ?: ""
        val submittedByUid = doc.getString("submittedByUid")
            ?: doc.getString("teacherId")
            ?: doc.getString("facultyId")
            ?: doc.getString("userId")
            ?: doc.getString("uid")
            ?: doc.getString("staffId")
            ?: ""

        return AttendanceRecordModel(
            id = doc.id,
            date = doc.getString("date") ?: "",
            timeSlot = doc.getString("timeSlot") ?: "",
            presentStudentIds = (doc.get("presentStudentIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            absentStudentIds = (doc.get("absentStudentIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            additionalStudentIds = (doc.get("additionalStudentIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            totalPresent = doc.getSafeLong("totalPresent", 0L).toInt(),
            totalAbsent = doc.getSafeLong("totalAbsent", 0L).toInt(),
            submittedBy = submittedBy,
            submittedByUid = submittedByUid,
            submittedByEmail = submittedByEmail,
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
