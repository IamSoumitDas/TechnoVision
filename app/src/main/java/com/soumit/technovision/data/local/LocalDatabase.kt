package com.soumit.technovision.data.local

import android.content.Context
import com.google.firebase.Timestamp
import com.soumit.technovision.TechnoVisionApplication
import com.soumit.technovision.data.local.room.*
import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.data.model.FeePaymentModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel
import com.soumit.technovision.util.AttendanceUtils
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

    // --- In-memory deserialization caches keyed by (id -> (updatedAt, Model)) ---
    private val studentCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, StudentModel>>()
    private val feePaymentCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, FeePaymentModel>>()
    private val attendanceCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, AttendanceRecordModel>>()
    private val teacherAttendanceCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, TeacherAttendanceRequestModel>>()

    private fun parseWeeklySchedule(json: String): Map<String, String> {
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optString(k, "")
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun getOrParseStudent(entity: StudentEntity): StudentModel {
        val cached = studentCache[entity.id]
        if (cached != null && cached.first == entity.updatedAt) {
            return cached.second
        }
        val schedule = if (entity.weeklyScheduleJson.isNotBlank() && entity.weeklyScheduleJson != "{}") {
            parseWeeklySchedule(entity.weeklyScheduleJson)
        } else if (entity.dataJson.isNotBlank()) {
            studentFromJson(entity.dataJson)?.weeklySchedule ?: emptyMap()
        } else {
            emptyMap()
        }
        val student = StudentModel(
            id = entity.id,
            fullName = entity.fullName,
            studentClass = entity.studentClass,
            school = entity.school,
            dob = entity.dob,
            gender = entity.gender,
            fatherName = entity.fatherName,
            motherName = entity.motherName,
            monthlyInstallment = entity.monthlyInstallment,
            phone = entity.phone,
            altPhone = entity.altPhone,
            email = entity.email,
            batchTimeSlot = entity.batchTimeSlot,
            weeklySchedule = schedule,
            admissionDate = entity.admissionDate,
            totalPaid = entity.totalPaid
        )
        studentCache[entity.id] = Pair(entity.updatedAt, student)
        return student
    }

    private fun getOrParseFeePayment(entity: FeePaymentEntity): FeePaymentModel {
        val cached = feePaymentCache[entity.id]
        if (cached != null && cached.first == entity.updatedAt) {
            return cached.second
        }
        val ts = if (entity.timestampMillis > 0L) Timestamp(java.util.Date(entity.timestampMillis)) else null
        val payment = FeePaymentModel(
            id = entity.id,
            studentId = entity.studentId,
            studentName = entity.studentName,
            amountPaid = entity.amountPaid,
            paymentMethod = entity.paymentMethod,
            remarks = entity.remarks,
            receivedBy = entity.receivedBy,
            timestamp = ts
        )
        feePaymentCache[entity.id] = Pair(entity.updatedAt, payment)
        return payment
    }

    private fun getOrParseAttendance(entity: AttendanceEntity): AttendanceRecordModel {
        val cached = attendanceCache[entity.id]
        if (cached != null && cached.first == entity.updatedAt) {
            return cached.second
        }
        val ts = if (entity.timestampMillis > 0L) Timestamp(java.util.Date(entity.timestampMillis)) else null
        val presentList = if (entity.presentIdsCsv.isNotBlank()) entity.presentIdsCsv.split(",") else emptyList()
        val absentList = if (entity.absentIdsCsv.isNotBlank()) entity.absentIdsCsv.split(",") else emptyList()
        val extraList = if (entity.additionalIdsCsv.isNotBlank()) entity.additionalIdsCsv.split(",") else emptyList()
        val record = AttendanceRecordModel(
            id = entity.id,
            date = entity.date,
            timeSlot = entity.timeSlot,
            presentStudentIds = presentList,
            absentStudentIds = absentList,
            additionalStudentIds = extraList,
            totalPresent = entity.totalPresent,
            totalAbsent = entity.totalAbsent,
            submittedBy = entity.submittedBy,
            submittedByUid = entity.submittedByUid,
            submittedByEmail = entity.submittedByEmail,
            timestamp = ts
        )
        attendanceCache[entity.id] = Pair(entity.updatedAt, record)
        return record
    }

    private fun getOrParseTeacherAttendance(entity: TeacherAttendanceEntity): TeacherAttendanceRequestModel {
        val cached = teacherAttendanceCache[entity.id]
        if (cached != null && cached.first == entity.updatedAt) {
            return cached.second
        }
        val ts = if (entity.timestampMillis > 0L) Timestamp(java.util.Date(entity.timestampMillis)) else null
        val req = TeacherAttendanceRequestModel(
            id = entity.id,
            teacherId = entity.teacherId,
            teacherName = entity.teacherName,
            date = entity.date,
            timeSlot = entity.timeSlot,
            status = entity.status,
            timestamp = ts,
            verifiedBy = entity.verifiedBy,
            rejectedBy = entity.rejectedBy
        )
        teacherAttendanceCache[entity.id] = Pair(entity.updatedAt, req)
        return req
    }

    // --- Reactive Flow streams backed directly by Room ---

    val studentsFlow: Flow<List<StudentModel>> =
        appDb.studentDao().getAllStudentsFlow().map { list ->
            list.mapNotNull { getOrParseStudent(it) }
        }

    val feePaymentsFlow: Flow<List<FeePaymentModel>> =
        appDb.feePaymentDao().getAllFeePaymentsFlow().map { list ->
            list.mapNotNull { getOrParseFeePayment(it) }
        }

    val attendanceFlow: Flow<List<AttendanceRecordModel>> =
        appDb.attendanceDao().getAllAttendanceFlow().map { list ->
            list.mapNotNull { getOrParseAttendance(it) }
        }

    val teacherAttendanceFlow: Flow<List<TeacherAttendanceRequestModel>> =
        appDb.teacherAttendanceDao().getAllTeacherAttendanceFlow().map { list ->
            list.mapNotNull { getOrParseTeacherAttendance(it) }
        }

    fun getStudentFlow(studentId: String): Flow<StudentModel?> =
        appDb.studentDao().getStudentFlow(studentId).map { entity ->
            entity?.let { getOrParseStudent(it) }
        }

    fun getFeePaymentsForStudentFlow(studentId: String): Flow<List<FeePaymentModel>> =
        appDb.feePaymentDao().getFeePaymentsForStudentFlow(studentId).map { list ->
            list.mapNotNull { getOrParseFeePayment(it) }
        }

    fun getTeacherAttendanceForTeacherFlow(teacherId: String): Flow<List<TeacherAttendanceRequestModel>> =
        appDb.teacherAttendanceDao().getTeacherAttendanceForTeacherFlow(teacherId).map { list ->
            list.mapNotNull { getOrParseTeacherAttendance(it) }
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
        appDb.studentDao().getAllStudents().mapNotNull { getOrParseStudent(it) }
    }

    suspend fun getStudent(studentId: String): StudentModel? = withContext(Dispatchers.IO) {
        appDb.studentDao().getStudent(studentId)?.let { getOrParseStudent(it) }
    }

    suspend fun upsertStudent(
        student: StudentModel,
        updatedAt: Long = System.currentTimeMillis(),
        forceTotalPaid: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val existing = getStudent(student.id)
        val finalTotalPaid = if (forceTotalPaid) {
            student.totalPaid
        } else {
            maxOf(student.totalPaid, existing?.totalPaid ?: 0L)
        }
        val finalStudent = student.copy(totalPaid = finalTotalPaid)
        studentCache[finalStudent.id] = Pair(updatedAt, finalStudent)
        val entity = studentToEntity(finalStudent, updatedAt)
        appDb.studentDao().upsert(entity)
    }

    suspend fun upsertStudentsBatch(
        students: List<Pair<StudentModel, Long>>,
        forceTotalPaid: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val existingStudents = appDb.studentDao().getAllStudents().associateBy { it.id }
        val entities = students.map { (student, updatedAt) ->
            val existing = existingStudents[student.id]
            val currentTotalPaid = existing?.totalPaid ?: 0L
            val finalTotalPaid = if (forceTotalPaid) student.totalPaid else maxOf(student.totalPaid, currentTotalPaid)
            val finalStudent = student.copy(totalPaid = finalTotalPaid)
            studentCache[finalStudent.id] = Pair(updatedAt, finalStudent)
            studentToEntity(finalStudent, updatedAt)
        }
        entities.chunked(100).forEach { appDb.studentDao().upsertAll(it) }
    }

    suspend fun markStudentDeleted(studentId: String, updatedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        studentCache.remove(studentId)
        appDb.studentDao().markDeleted(studentId, updatedAt)
    }

    // --- Fee Payments Operations ---

    suspend fun getAllFeePayments(): List<FeePaymentModel> = withContext(Dispatchers.IO) {
        appDb.feePaymentDao().getAllFeePayments().mapNotNull { getOrParseFeePayment(it) }
    }

    suspend fun getFeePaymentsForStudent(studentId: String): List<FeePaymentModel> = withContext(Dispatchers.IO) {
        appDb.feePaymentDao().getFeePaymentsForStudent(studentId).mapNotNull { getOrParseFeePayment(it) }
    }

    suspend fun getFeePayment(paymentId: String): FeePaymentModel? = withContext(Dispatchers.IO) {
        appDb.feePaymentDao().getFeePayment(paymentId)?.let { getOrParseFeePayment(it) }
    }

    suspend fun upsertFeePayment(payment: FeePaymentModel, updatedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        feePaymentCache[payment.id] = Pair(updatedAt, payment)
        val entity = feePaymentToEntity(payment, updatedAt)
        appDb.feePaymentDao().upsert(entity)
    }

    suspend fun upsertFeePaymentsBatch(payments: List<Pair<FeePaymentModel, Long>>) = withContext(Dispatchers.IO) {
        val entities = payments.map { (payment, updatedAt) ->
            feePaymentCache[payment.id] = Pair(updatedAt, payment)
            feePaymentToEntity(payment, updatedAt)
        }
        entities.chunked(100).forEach { appDb.feePaymentDao().upsertAll(it) }
    }

    suspend fun markFeePaymentDeleted(paymentId: String, updatedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        feePaymentCache.remove(paymentId)
        appDb.feePaymentDao().markDeleted(paymentId, updatedAt)
    }

    // --- Attendance Operations ---

    suspend fun getAllAttendanceRecords(): List<AttendanceRecordModel> = withContext(Dispatchers.IO) {
        appDb.attendanceDao().getAllAttendance().mapNotNull { getOrParseAttendance(it) }
    }

    suspend fun getExistingAttendance(date: String, timeSlot: String): AttendanceRecordModel? = withContext(Dispatchers.IO) {
        val altDate = if (date.contains('/')) date.replace('/', '-') else date.replace('-', '/')
        val normDate = AttendanceUtils.normalizeDate(date)
        val targetSlot = AttendanceUtils.normalizeTimeSlot(timeSlot)

        val exact = appDb.attendanceDao().getExistingAttendance(date, altDate, timeSlot)?.let {
            getOrParseAttendance(it)
        }
        if (exact != null) return@withContext exact

        val candidates = appDb.attendanceDao().getAttendanceByDateCandidates(date, altDate, normDate)
        candidates.firstNotNullOfOrNull { entity ->
            val record = getOrParseAttendance(entity)
            if (record != null && AttendanceUtils.normalizeTimeSlot(record.timeSlot) == targetSlot) {
                record
            } else null
        }
    }

    suspend fun upsertAttendanceRecord(record: AttendanceRecordModel, updatedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        attendanceCache[record.id] = Pair(updatedAt, record)
        val entity = attendanceToEntity(record, updatedAt)
        appDb.attendanceDao().upsert(entity)
    }

    suspend fun upsertAttendanceBatch(records: List<Pair<AttendanceRecordModel, Long>>) = withContext(Dispatchers.IO) {
        val entities = records.map { (record, updatedAt) ->
            attendanceCache[record.id] = Pair(updatedAt, record)
            attendanceToEntity(record, updatedAt)
        }
        entities.chunked(100).forEach { appDb.attendanceDao().upsertAll(it) }
    }

    suspend fun markAttendanceDeleted(id: String, updatedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        attendanceCache.remove(id)
        appDb.attendanceDao().markDeleted(id, updatedAt)
    }

    // --- Teacher Attendance Operations ---

    suspend fun getAllTeacherAttendance(teacherId: String? = null): List<TeacherAttendanceRequestModel> = withContext(Dispatchers.IO) {
        if (!teacherId.isNullOrBlank()) {
            appDb.teacherAttendanceDao().getTeacherAttendanceForTeacher(teacherId).mapNotNull {
                getOrParseTeacherAttendance(it)
            }
        } else {
            appDb.teacherAttendanceDao().getAllTeacherAttendance().mapNotNull {
                getOrParseTeacherAttendance(it)
            }
        }
    }

    suspend fun getTeacherAttendance(id: String): TeacherAttendanceRequestModel? = withContext(Dispatchers.IO) {
        appDb.teacherAttendanceDao().getTeacherAttendance(id)?.let {
            getOrParseTeacherAttendance(it)
        }
    }

    suspend fun upsertTeacherAttendance(req: TeacherAttendanceRequestModel, updatedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        teacherAttendanceCache[req.id] = Pair(updatedAt, req)
        val entity = teacherAttendanceToEntity(req, updatedAt)
        appDb.teacherAttendanceDao().upsert(entity)
    }

    suspend fun upsertTeacherAttendanceBatch(records: List<Pair<TeacherAttendanceRequestModel, Long>>) = withContext(Dispatchers.IO) {
        val entities = records.map { (req, updatedAt) ->
            teacherAttendanceCache[req.id] = Pair(updatedAt, req)
            teacherAttendanceToEntity(req, updatedAt)
        }
        entities.chunked(100).forEach { appDb.teacherAttendanceDao().upsertAll(it) }
    }

    suspend fun markTeacherAttendanceDeleted(id: String, updatedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        teacherAttendanceCache.remove(id)
        appDb.teacherAttendanceDao().markDeleted(id, updatedAt)
    }

    suspend fun deleteTeacherAttendancePermanently(id: String) = withContext(Dispatchers.IO) {
        teacherAttendanceCache.remove(id)
        appDb.teacherAttendanceDao().deletePermanently(id)
    }

    // --- Entity Converters ---

    private fun studentToEntity(student: StudentModel, updatedAt: Long): StudentEntity {
        val schedJson = if (student.weeklySchedule.isNotEmpty()) {
            JSONObject(student.weeklySchedule as Map<*, *>).toString()
        } else "{}"
        return StudentEntity(
            id = student.id,
            fullName = student.fullName,
            studentClass = student.studentClass,
            school = student.school,
            dob = student.dob,
            gender = student.gender,
            fatherName = student.fatherName,
            motherName = student.motherName,
            monthlyInstallment = student.monthlyInstallment,
            phone = student.phone,
            altPhone = student.altPhone,
            email = student.email,
            batchTimeSlot = student.batchTimeSlot,
            weeklyScheduleJson = schedJson,
            admissionDate = student.admissionDate,
            totalPaid = student.totalPaid,
            dataJson = "",
            updatedAt = updatedAt,
            isDeleted = 0
        )
    }

    private fun feePaymentToEntity(payment: FeePaymentModel, updatedAt: Long): FeePaymentEntity {
        val timeMillis = payment.timestamp?.toDate()?.time ?: System.currentTimeMillis()
        return FeePaymentEntity(
            id = payment.id,
            studentId = payment.studentId,
            studentName = payment.studentName,
            amountPaid = payment.amountPaid,
            paymentMethod = payment.paymentMethod,
            remarks = payment.remarks,
            receivedBy = payment.receivedBy,
            timestampMillis = timeMillis,
            dataJson = "",
            updatedAt = updatedAt,
            isDeleted = 0
        )
    }

    private fun attendanceToEntity(record: AttendanceRecordModel, updatedAt: Long): AttendanceEntity {
        val timeMillis = record.timestamp?.toDate()?.time ?: System.currentTimeMillis()
        return AttendanceEntity(
            id = record.id,
            date = record.date,
            timeSlot = record.timeSlot,
            timestampMillis = timeMillis,
            presentIdsCsv = record.presentStudentIds.joinToString(","),
            absentIdsCsv = record.absentStudentIds.joinToString(","),
            additionalIdsCsv = record.additionalStudentIds.joinToString(","),
            totalPresent = record.totalPresent,
            totalAbsent = record.totalAbsent,
            submittedBy = record.submittedBy,
            submittedByUid = record.submittedByUid,
            submittedByEmail = record.submittedByEmail,
            dataJson = "",
            updatedAt = updatedAt,
            isDeleted = 0
        )
    }

    private fun teacherAttendanceToEntity(req: TeacherAttendanceRequestModel, updatedAt: Long): TeacherAttendanceEntity {
        val timeMillis = req.timestamp?.toDate()?.time ?: System.currentTimeMillis()
        return TeacherAttendanceEntity(
            id = req.id,
            teacherId = req.teacherId,
            teacherName = req.teacherName,
            date = req.date,
            timeSlot = req.timeSlot,
            status = req.status,
            timestampMillis = timeMillis,
            verifiedBy = req.verifiedBy,
            rejectedBy = req.rejectedBy,
            dataJson = "",
            updatedAt = updatedAt,
            isDeleted = 0
        )
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
        val id = appDb.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = entityType,
                docId = docId,
                payloadJson = payloadJson
            )
        )
        TechnoVisionApplication.instance?.let { app ->
            com.soumit.technovision.data.worker.DeltaSyncWorker.enqueueOneTimeSync(app)
        }
        id
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
