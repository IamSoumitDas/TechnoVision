package com.soumit.technovision.data.local

import android.content.Context
import com.google.firebase.Timestamp
import com.soumit.technovision.data.local.room.*
import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.data.model.FeePaymentModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LocalDatabase private constructor(private val appDb: AppDatabase) {

    companion object {
        @Volatile
        private var INSTANCE: LocalDatabase? = null

        fun getInstance(context: Context): LocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalDatabase(AppDatabase.getInstance(context)).also { INSTANCE = it }
            }
        }
    }

    val roomDatabase: AppDatabase get() = appDb

    // --- Reactive Flow streams backed directly by Room ---

    val studentsFlow: Flow<List<StudentModel>> =
        appDb.studentDao().getAllStudentsFlow().map { list ->
            list.mapNotNull { studentFromJson(it.dataJson) }
        }

    val feePaymentsFlow: Flow<List<FeePaymentModel>> =
        appDb.feePaymentDao().getAllFeePaymentsFlow().map { list ->
            list.mapNotNull { feePaymentFromJson(it.dataJson) }
        }

    val attendanceFlow: Flow<List<AttendanceRecordModel>> =
        appDb.attendanceDao().getAllAttendanceFlow().map { list ->
            list.mapNotNull { attendanceFromJson(it.dataJson) }
        }

    val teacherAttendanceFlow: Flow<List<TeacherAttendanceRequestModel>> =
        appDb.teacherAttendanceDao().getAllTeacherAttendanceFlow().map { list ->
            list.mapNotNull { teacherAttendanceFromJson(it.dataJson) }
        }

    fun getStudentFlow(studentId: String): Flow<StudentModel?> =
        appDb.studentDao().getStudentFlow(studentId).map { entity ->
            entity?.let { studentFromJson(it.dataJson) }
        }

    fun getFeePaymentsForStudentFlow(studentId: String): Flow<List<FeePaymentModel>> =
        appDb.feePaymentDao().getFeePaymentsForStudentFlow(studentId).map { list ->
            list.mapNotNull { feePaymentFromJson(it.dataJson) }
        }

    fun getTeacherAttendanceForTeacherFlow(teacherId: String): Flow<List<TeacherAttendanceRequestModel>> =
        appDb.teacherAttendanceDao().getTeacherAttendanceForTeacherFlow(teacherId).map { list ->
            list.mapNotNull { teacherAttendanceFromJson(it.dataJson) }
        }

    // --- Sync Metadata ---

    suspend fun getLastSyncTime(collection: String): Long = withContext(Dispatchers.IO) {
        appDb.syncMetadataDao().getLastSyncTime(collection) ?: 0L
    }

    suspend fun setLastSyncTime(collection: String, timestamp: Long) = withContext(Dispatchers.IO) {
        appDb.syncMetadataDao().setLastSyncTime(SyncMetadataEntity(collection, timestamp))
    }

    suspend fun isMigrationDone(key: String): Boolean {
        return getLastSyncTime("migration_$key") > 0L
    }

    suspend fun setMigrationDone(key: String) {
        setLastSyncTime("migration_$key", System.currentTimeMillis())
    }

    // --- Students Operations ---

    suspend fun getAllStudents(): List<StudentModel> = withContext(Dispatchers.IO) {
        appDb.studentDao().getAllStudents().mapNotNull { studentFromJson(it.dataJson) }
    }

    suspend fun getStudent(studentId: String): StudentModel? = withContext(Dispatchers.IO) {
        appDb.studentDao().getStudent(studentId)?.let { studentFromJson(it.dataJson) }
    }

    suspend fun upsertStudent(student: StudentModel, updatedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val existing = getStudent(student.id)
        val finalStudent = if (student.totalPaid == 0L && existing != null && existing.totalPaid > 0L) {
            student.copy(totalPaid = existing.totalPaid)
        } else {
            student
        }
        val entity = StudentEntity(
            id = finalStudent.id,
            fullName = finalStudent.fullName,
            dataJson = studentToJson(finalStudent),
            updatedAt = updatedAt,
            isDeleted = 0
        )
        appDb.studentDao().upsert(entity)
    }

    suspend fun upsertStudentsBatch(students: List<Pair<StudentModel, Long>>) = withContext(Dispatchers.IO) {
        val existingStudents = appDb.studentDao().getAllStudents().associateBy { it.id }
        val entities = students.map { (student, updatedAt) ->
            var currentTotalPaid = 0L
            val existing = existingStudents[student.id]
            if (existing != null) {
                studentFromJson(existing.dataJson)?.totalPaid?.let { currentTotalPaid = it }
            }
            val finalStudent = if (student.totalPaid == 0L && currentTotalPaid > 0L) {
                student.copy(totalPaid = currentTotalPaid)
            } else {
                student
            }
            StudentEntity(
                id = finalStudent.id,
                fullName = finalStudent.fullName,
                dataJson = studentToJson(finalStudent),
                updatedAt = updatedAt,
                isDeleted = 0
            )
        }
        appDb.studentDao().upsertAll(entities)
    }

    suspend fun markStudentDeleted(studentId: String, updatedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        appDb.studentDao().markDeleted(studentId, updatedAt)
    }

    // --- Fee Payments Operations ---

    suspend fun getAllFeePayments(): List<FeePaymentModel> = withContext(Dispatchers.IO) {
        appDb.feePaymentDao().getAllFeePayments().mapNotNull { feePaymentFromJson(it.dataJson) }
    }

    suspend fun getFeePaymentsForStudent(studentId: String): List<FeePaymentModel> = withContext(Dispatchers.IO) {
        appDb.feePaymentDao().getFeePaymentsForStudent(studentId).mapNotNull { feePaymentFromJson(it.dataJson) }
    }

    suspend fun getFeePayment(paymentId: String): FeePaymentModel? = withContext(Dispatchers.IO) {
        appDb.feePaymentDao().getFeePayment(paymentId)?.let { feePaymentFromJson(it.dataJson) }
    }

    suspend fun upsertFeePayment(payment: FeePaymentModel, updatedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val timeMillis = payment.timestamp?.toDate()?.time ?: System.currentTimeMillis()
        val entity = FeePaymentEntity(
            id = payment.id,
            studentId = payment.studentId,
            amountPaid = payment.amountPaid,
            timestampMillis = timeMillis,
            dataJson = feePaymentToJson(payment),
            updatedAt = updatedAt,
            isDeleted = 0
        )
        appDb.feePaymentDao().upsert(entity)
    }

    suspend fun upsertFeePaymentsBatch(payments: List<Pair<FeePaymentModel, Long>>) = withContext(Dispatchers.IO) {
        val entities = payments.map { (payment, updatedAt) ->
            val timeMillis = payment.timestamp?.toDate()?.time ?: System.currentTimeMillis()
            FeePaymentEntity(
                id = payment.id,
                studentId = payment.studentId,
                amountPaid = payment.amountPaid,
                timestampMillis = timeMillis,
                dataJson = feePaymentToJson(payment),
                updatedAt = updatedAt,
                isDeleted = 0
            )
        }
        appDb.feePaymentDao().upsertAll(entities)
    }

    suspend fun markFeePaymentDeleted(paymentId: String, updatedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        appDb.feePaymentDao().markDeleted(paymentId, updatedAt)
    }

    // --- Attendance Operations ---

    suspend fun getAllAttendanceRecords(): List<AttendanceRecordModel> = withContext(Dispatchers.IO) {
        appDb.attendanceDao().getAllAttendance().mapNotNull { attendanceFromJson(it.dataJson) }
    }

    suspend fun getExistingAttendance(date: String, timeSlot: String): AttendanceRecordModel? = withContext(Dispatchers.IO) {
        val altDate = if (date.contains('/')) date.replace('/', '-') else date.replace('-', '/')
        appDb.attendanceDao().getExistingAttendance(date, altDate, timeSlot)?.let {
            attendanceFromJson(it.dataJson)
        }
    }

    suspend fun upsertAttendanceRecord(record: AttendanceRecordModel, updatedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val timeMillis = record.timestamp?.toDate()?.time ?: System.currentTimeMillis()
        val entity = AttendanceEntity(
            id = record.id,
            date = record.date,
            timeSlot = record.timeSlot,
            timestampMillis = timeMillis,
            dataJson = attendanceToJson(record),
            updatedAt = updatedAt,
            isDeleted = 0
        )
        appDb.attendanceDao().upsert(entity)
    }

    suspend fun upsertAttendanceBatch(records: List<Pair<AttendanceRecordModel, Long>>) = withContext(Dispatchers.IO) {
        val entities = records.map { (record, updatedAt) ->
            val timeMillis = record.timestamp?.toDate()?.time ?: System.currentTimeMillis()
            AttendanceEntity(
                id = record.id,
                date = record.date,
                timeSlot = record.timeSlot,
                timestampMillis = timeMillis,
                dataJson = attendanceToJson(record),
                updatedAt = updatedAt,
                isDeleted = 0
            )
        }
        appDb.attendanceDao().upsertAll(entities)
    }

    suspend fun markAttendanceDeleted(id: String, updatedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        appDb.attendanceDao().markDeleted(id, updatedAt)
    }

    // --- Teacher Attendance Operations ---

    suspend fun getAllTeacherAttendance(teacherId: String? = null): List<TeacherAttendanceRequestModel> = withContext(Dispatchers.IO) {
        if (!teacherId.isNullOrBlank()) {
            appDb.teacherAttendanceDao().getTeacherAttendanceForTeacher(teacherId).mapNotNull {
                teacherAttendanceFromJson(it.dataJson)
            }
        } else {
            appDb.teacherAttendanceDao().getAllTeacherAttendance().mapNotNull {
                teacherAttendanceFromJson(it.dataJson)
            }
        }
    }

    suspend fun getTeacherAttendance(id: String): TeacherAttendanceRequestModel? = withContext(Dispatchers.IO) {
        appDb.teacherAttendanceDao().getTeacherAttendance(id)?.let {
            teacherAttendanceFromJson(it.dataJson)
        }
    }

    suspend fun upsertTeacherAttendance(req: TeacherAttendanceRequestModel, updatedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val timeMillis = req.timestamp?.toDate()?.time ?: System.currentTimeMillis()
        val entity = TeacherAttendanceEntity(
            id = req.id,
            teacherId = req.teacherId,
            date = req.date,
            timeSlot = req.timeSlot,
            status = req.status,
            timestampMillis = timeMillis,
            dataJson = teacherAttendanceToJson(req),
            updatedAt = updatedAt,
            isDeleted = 0
        )
        appDb.teacherAttendanceDao().upsert(entity)
    }

    suspend fun upsertTeacherAttendanceBatch(records: List<Pair<TeacherAttendanceRequestModel, Long>>) = withContext(Dispatchers.IO) {
        val entities = records.map { (req, updatedAt) ->
            val timeMillis = req.timestamp?.toDate()?.time ?: System.currentTimeMillis()
            TeacherAttendanceEntity(
                id = req.id,
                teacherId = req.teacherId,
                date = req.date,
                timeSlot = req.timeSlot,
                status = req.status,
                timestampMillis = timeMillis,
                dataJson = teacherAttendanceToJson(req),
                updatedAt = updatedAt,
                isDeleted = 0
            )
        }
        appDb.teacherAttendanceDao().upsertAll(entities)
    }

    suspend fun markTeacherAttendanceDeleted(id: String, updatedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        appDb.teacherAttendanceDao().markDeleted(id, updatedAt)
    }

    // --- JSON Serialization Helpers ---

    private fun studentToJson(s: StudentModel): String {
        val obj = JSONObject()
        obj.put("id", s.id)
        obj.put("fullName", s.fullName)
        obj.put("studentClass", s.studentClass)
        obj.put("school", s.school)
        obj.put("dob", s.dob)
        obj.put("gender", s.gender)
        obj.put("fatherName", s.fatherName)
        obj.put("motherName", s.motherName)
        obj.put("monthlyInstallment", s.monthlyInstallment)
        obj.put("phone", s.phone)
        obj.put("altPhone", s.altPhone)
        obj.put("email", s.email)
        obj.put("batchTimeSlot", s.batchTimeSlot)
        obj.put("admissionDate", s.admissionDate)
        obj.put("totalPaid", s.totalPaid)

        val schedObj = JSONObject()
        for ((k, v) in s.weeklySchedule) {
            schedObj.put(k, v)
        }
        obj.put("weeklySchedule", schedObj)
        return obj.toString()
    }

    private fun studentFromJson(str: String): StudentModel? {
        return try {
            val obj = JSONObject(str)
            val schedMap = mutableMapOf<String, String>()
            val schedObj = obj.optJSONObject("weeklySchedule")
            if (schedObj != null) {
                val keys = schedObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    schedMap[k] = schedObj.optString(k, "")
                }
            }
            StudentModel(
                id = obj.optString("id", ""),
                fullName = obj.optString("fullName", ""),
                studentClass = obj.optString("studentClass", ""),
                school = obj.optString("school", ""),
                dob = obj.optString("dob", ""),
                gender = obj.optString("gender", ""),
                fatherName = obj.optString("fatherName", ""),
                motherName = obj.optString("motherName", ""),
                monthlyInstallment = obj.optLong("monthlyInstallment", 500L),
                phone = obj.optString("phone", ""),
                altPhone = obj.optString("altPhone", ""),
                email = obj.optString("email", ""),
                batchTimeSlot = obj.optString("batchTimeSlot", ""),
                weeklySchedule = schedMap,
                admissionDate = obj.optString("admissionDate", ""),
                totalPaid = obj.optLong("totalPaid", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun feePaymentToJson(p: FeePaymentModel): String {
        val obj = JSONObject()
        obj.put("id", p.id)
        obj.put("studentId", p.studentId)
        obj.put("studentName", p.studentName)
        obj.put("amountPaid", p.amountPaid)
        obj.put("paymentMethod", p.paymentMethod)
        obj.put("remarks", p.remarks)
        obj.put("receivedBy", p.receivedBy)
        if (p.timestamp != null) {
            obj.put("ts_sec", p.timestamp.seconds)
            obj.put("ts_nan", p.timestamp.nanoseconds)
        }
        return obj.toString()
    }

    private fun feePaymentFromJson(str: String): FeePaymentModel? {
        return try {
            val obj = JSONObject(str)
            val ts = if (obj.has("ts_sec")) {
                Timestamp(obj.getLong("ts_sec"), obj.optInt("ts_nan", 0))
            } else null
            FeePaymentModel(
                id = obj.optString("id", ""),
                studentId = obj.optString("studentId", ""),
                studentName = obj.optString("studentName", ""),
                amountPaid = obj.optLong("amountPaid", 0L),
                paymentMethod = obj.optString("paymentMethod", ""),
                remarks = obj.optString("remarks", ""),
                receivedBy = obj.optString("receivedBy", ""),
                timestamp = ts
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun attendanceToJson(a: AttendanceRecordModel): String {
        val obj = JSONObject()
        obj.put("id", a.id)
        obj.put("date", a.date)
        obj.put("timeSlot", a.timeSlot)
        obj.put("totalPresent", a.totalPresent)
        obj.put("totalAbsent", a.totalAbsent)
        obj.put("submittedBy", a.submittedBy)
        obj.put("submittedByUid", a.submittedByUid)
        obj.put("submittedByEmail", a.submittedByEmail)

        obj.put("presentIds", JSONArray(a.presentStudentIds))
        obj.put("absentIds", JSONArray(a.absentStudentIds))
        obj.put("additionalIds", JSONArray(a.additionalStudentIds))

        if (a.timestamp != null) {
            obj.put("ts_sec", a.timestamp.seconds)
            obj.put("ts_nan", a.timestamp.nanoseconds)
        }
        return obj.toString()
    }

    private fun attendanceFromJson(str: String): AttendanceRecordModel? {
        return try {
            val obj = JSONObject(str)
            val ts = if (obj.has("ts_sec")) {
                Timestamp(obj.getLong("ts_sec"), obj.optInt("ts_nan", 0))
            } else null

            fun toList(arr: JSONArray?): List<String> {
                if (arr == null) return emptyList()
                val res = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    res.add(arr.optString(i, ""))
                }
                return res
            }

            AttendanceRecordModel(
                id = obj.optString("id", ""),
                date = obj.optString("date", ""),
                timeSlot = obj.optString("timeSlot", ""),
                presentStudentIds = toList(obj.optJSONArray("presentIds")),
                absentStudentIds = toList(obj.optJSONArray("absentIds")),
                additionalStudentIds = toList(obj.optJSONArray("additionalIds")),
                totalPresent = obj.optInt("totalPresent", 0),
                totalAbsent = obj.optInt("totalAbsent", 0),
                submittedBy = obj.optString("submittedBy", ""),
                submittedByUid = obj.optString("submittedByUid", ""),
                submittedByEmail = obj.optString("submittedByEmail", ""),
                timestamp = ts
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun teacherAttendanceToJson(t: TeacherAttendanceRequestModel): String {
        val obj = JSONObject()
        obj.put("id", t.id)
        obj.put("teacherId", t.teacherId)
        obj.put("teacherName", t.teacherName)
        obj.put("date", t.date)
        obj.put("timeSlot", t.timeSlot)
        obj.put("status", t.status)
        obj.put("verifiedBy", t.verifiedBy)
        obj.put("rejectedBy", t.rejectedBy)
        if (t.timestamp != null) {
            obj.put("ts_sec", t.timestamp.seconds)
            obj.put("ts_nan", t.timestamp.nanoseconds)
        }
        return obj.toString()
    }

    private fun teacherAttendanceFromJson(str: String): TeacherAttendanceRequestModel? {
        return try {
            val obj = JSONObject(str)
            val ts = if (obj.has("ts_sec")) {
                Timestamp(obj.getLong("ts_sec"), obj.optInt("ts_nan", 0))
            } else null
            TeacherAttendanceRequestModel(
                id = obj.optString("id", ""),
                teacherId = obj.optString("teacherId", ""),
                teacherName = obj.optString("teacherName", ""),
                date = obj.optString("date", ""),
                timeSlot = obj.optString("timeSlot", ""),
                status = obj.optString("status", "Pending"),
                timestamp = ts,
                verifiedBy = obj.optString("verifiedBy", ""),
                rejectedBy = obj.optString("rejectedBy", "")
            )
        } catch (_: Exception) {
            null
        }
    }

    // --- Sync Queue Operations ---

    val syncQueueDao: SyncQueueDao
        get() = appDb.syncQueueDao()

    suspend fun enqueueSyncItem(entityType: String, docId: String, payloadJson: String): Long = withContext(Dispatchers.IO) {
        appDb.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = entityType,
                docId = docId,
                payloadJson = payloadJson
            )
        )
    }

    suspend fun getPendingSyncItems(): List<SyncQueueEntity> = withContext(Dispatchers.IO) {
        appDb.syncQueueDao().getPendingItems()
    }

    suspend fun removeSyncItem(id: Long) = withContext(Dispatchers.IO) {
        appDb.syncQueueDao().delete(id)
    }

    fun teacherAttendanceToJsonPublic(t: TeacherAttendanceRequestModel): String = teacherAttendanceToJson(t)
    fun teacherAttendanceFromJsonPublic(str: String): TeacherAttendanceRequestModel? = teacherAttendanceFromJson(str)
}
