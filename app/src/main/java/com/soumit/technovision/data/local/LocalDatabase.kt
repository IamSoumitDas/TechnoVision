package com.soumit.technovision.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.firebase.Timestamp
import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.data.model.FeePaymentModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONArray
import org.json.JSONObject

class LocalDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "technovision_local.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: LocalDatabase? = null

        fun getInstance(context: Context): LocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _studentsUpdateFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val studentsUpdateFlow: SharedFlow<Unit> = _studentsUpdateFlow.asSharedFlow()

    private val _feesUpdateFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val feesUpdateFlow: SharedFlow<Unit> = _feesUpdateFlow.asSharedFlow()

    private val _attendanceUpdateFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val attendanceUpdateFlow: SharedFlow<Unit> = _attendanceUpdateFlow.asSharedFlow()

    private val _teacherAttendanceUpdateFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val teacherAttendanceUpdateFlow: SharedFlow<Unit> = _teacherAttendanceUpdateFlow.asSharedFlow()

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE students (
                id TEXT PRIMARY KEY,
                full_name TEXT,
                data_json TEXT,
                updated_at INTEGER,
                is_deleted INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_students_name ON students(full_name)")
        db.execSQL("CREATE INDEX idx_students_deleted ON students(is_deleted)")

        db.execSQL(
            """
            CREATE TABLE fee_payments (
                id TEXT PRIMARY KEY,
                student_id TEXT,
                amount_paid INTEGER,
                timestamp_millis INTEGER,
                data_json TEXT,
                updated_at INTEGER,
                is_deleted INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_fees_student ON fee_payments(student_id)")
        db.execSQL("CREATE INDEX idx_fees_time ON fee_payments(timestamp_millis)")
        db.execSQL("CREATE INDEX idx_fees_deleted ON fee_payments(is_deleted)")

        db.execSQL(
            """
            CREATE TABLE attendance (
                id TEXT PRIMARY KEY,
                date TEXT,
                time_slot TEXT,
                timestamp_millis INTEGER,
                data_json TEXT,
                updated_at INTEGER,
                is_deleted INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_att_date_slot ON attendance(date, time_slot)")
        db.execSQL("CREATE INDEX idx_att_time ON attendance(timestamp_millis)")
        db.execSQL("CREATE INDEX idx_att_deleted ON attendance(is_deleted)")

        db.execSQL(
            """
            CREATE TABLE teacher_attendance (
                id TEXT PRIMARY KEY,
                teacher_id TEXT,
                date TEXT,
                time_slot TEXT,
                status TEXT,
                timestamp_millis INTEGER,
                data_json TEXT,
                updated_at INTEGER,
                is_deleted INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_tatt_teacher ON teacher_attendance(teacher_id)")
        db.execSQL("CREATE INDEX idx_tatt_time ON teacher_attendance(timestamp_millis)")
        db.execSQL("CREATE INDEX idx_tatt_deleted ON teacher_attendance(is_deleted)")

        db.execSQL(
            """
            CREATE TABLE sync_metadata (
                collection_name TEXT PRIMARY KEY,
                last_sync_time INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS students")
        db.execSQL("DROP TABLE IF EXISTS fee_payments")
        db.execSQL("DROP TABLE IF EXISTS attendance")
        db.execSQL("DROP TABLE IF EXISTS teacher_attendance")
        db.execSQL("DROP TABLE IF EXISTS sync_metadata")
        onCreate(db)
    }

    // --- Sync Metadata ---

    fun getLastSyncTime(collection: String): Long {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT last_sync_time FROM sync_metadata WHERE collection_name = ?", arrayOf(collection))
        return cursor.use {
            if (it.moveToFirst()) it.getLong(0) else 0L
        }
    }

    fun setLastSyncTime(collection: String, timestamp: Long) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("collection_name", collection)
            put("last_sync_time", timestamp)
        }
        db.insertWithOnConflict("sync_metadata", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun isMigrationDone(key: String): Boolean {
        return getLastSyncTime("migration_$key") > 0L
    }

    fun setMigrationDone(key: String) {
        setLastSyncTime("migration_$key", System.currentTimeMillis())
    }

    // --- Students Operations ---

    fun getAllStudents(): List<StudentModel> {
        val db = readableDatabase
        val list = mutableListOf<StudentModel>()
        val cursor = db.rawQuery(
            "SELECT data_json FROM students WHERE is_deleted = 0 ORDER BY full_name COLLATE NOCASE ASC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                val json = it.getString(0)
                studentFromJson(json)?.let { student -> list.add(student) }
            }
        }
        return list
    }

    fun getStudent(studentId: String): StudentModel? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT data_json FROM students WHERE id = ? AND is_deleted = 0",
            arrayOf(studentId)
        )
        return cursor.use {
            if (it.moveToFirst()) studentFromJson(it.getString(0)) else null
        }
    }

    fun upsertStudent(student: StudentModel, updatedAt: Long = System.currentTimeMillis()) {
        val db = writableDatabase
        val existing = getStudent(student.id)
        val finalStudent = if (student.totalPaid == 0L && existing != null && existing.totalPaid > 0L) {
            student.copy(totalPaid = existing.totalPaid)
        } else {
            student
        }
        val values = ContentValues().apply {
            put("id", finalStudent.id)
            put("full_name", finalStudent.fullName)
            put("data_json", studentToJson(finalStudent))
            put("updated_at", updatedAt)
            put("is_deleted", 0)
        }
        db.insertWithOnConflict("students", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        _studentsUpdateFlow.tryEmit(Unit)
    }

    fun upsertStudentsBatch(students: List<Pair<StudentModel, Long>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for ((student, updatedAt) in students) {
                var currentTotalPaid = 0L
                val cursor = db.rawQuery("SELECT data_json FROM students WHERE id = ? AND is_deleted = 0", arrayOf(student.id))
                cursor.use {
                    if (it.moveToFirst()) {
                        currentTotalPaid = studentFromJson(it.getString(0))?.totalPaid ?: 0L
                    }
                }
                val finalStudent = if (student.totalPaid == 0L && currentTotalPaid > 0L) {
                    student.copy(totalPaid = currentTotalPaid)
                } else {
                    student
                }
                val values = ContentValues().apply {
                    put("id", finalStudent.id)
                    put("full_name", finalStudent.fullName)
                    put("data_json", studentToJson(finalStudent))
                    put("updated_at", updatedAt)
                    put("is_deleted", 0)
                }
                db.insertWithOnConflict("students", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _studentsUpdateFlow.tryEmit(Unit)
    }

    fun markStudentDeleted(studentId: String, updatedAt: Long = System.currentTimeMillis()) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("is_deleted", 1)
            put("updated_at", updatedAt)
        }
        db.update("students", values, "id = ?", arrayOf(studentId))
        _studentsUpdateFlow.tryEmit(Unit)
    }

    // --- Fee Payments Operations ---

    fun getAllFeePayments(): List<FeePaymentModel> {
        val db = readableDatabase
        val list = mutableListOf<FeePaymentModel>()
        val cursor = db.rawQuery(
            "SELECT data_json FROM fee_payments WHERE is_deleted = 0 ORDER BY timestamp_millis DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                feePaymentFromJson(it.getString(0))?.let { payment -> list.add(payment) }
            }
        }
        return list
    }

    fun getFeePaymentsForStudent(studentId: String): List<FeePaymentModel> {
        val db = readableDatabase
        val list = mutableListOf<FeePaymentModel>()
        val cursor = db.rawQuery(
            "SELECT data_json FROM fee_payments WHERE student_id = ? AND is_deleted = 0 ORDER BY timestamp_millis DESC",
            arrayOf(studentId)
        )
        cursor.use {
            while (it.moveToNext()) {
                feePaymentFromJson(it.getString(0))?.let { payment -> list.add(payment) }
            }
        }
        return list
    }

    fun getFeePayment(paymentId: String): FeePaymentModel? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT data_json FROM fee_payments WHERE id = ? AND is_deleted = 0",
            arrayOf(paymentId)
        )
        return cursor.use {
            if (it.moveToFirst()) feePaymentFromJson(it.getString(0)) else null
        }
    }

    fun upsertFeePayment(payment: FeePaymentModel, updatedAt: Long = System.currentTimeMillis()) {
        val db = writableDatabase
        val timeMillis = payment.timestamp?.toDate()?.time ?: System.currentTimeMillis()
        val values = ContentValues().apply {
            put("id", payment.id)
            put("student_id", payment.studentId)
            put("amount_paid", payment.amountPaid)
            put("timestamp_millis", timeMillis)
            put("data_json", feePaymentToJson(payment))
            put("updated_at", updatedAt)
            put("is_deleted", 0)
        }
        db.insertWithOnConflict("fee_payments", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        _feesUpdateFlow.tryEmit(Unit)
    }

    fun upsertFeePaymentsBatch(payments: List<Pair<FeePaymentModel, Long>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for ((payment, updatedAt) in payments) {
                val timeMillis = payment.timestamp?.toDate()?.time ?: System.currentTimeMillis()
                val values = ContentValues().apply {
                    put("id", payment.id)
                    put("student_id", payment.studentId)
                    put("amount_paid", payment.amountPaid)
                    put("timestamp_millis", timeMillis)
                    put("data_json", feePaymentToJson(payment))
                    put("updated_at", updatedAt)
                    put("is_deleted", 0)
                }
                db.insertWithOnConflict("fee_payments", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _feesUpdateFlow.tryEmit(Unit)
    }

    fun markFeePaymentDeleted(paymentId: String, updatedAt: Long = System.currentTimeMillis()) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("is_deleted", 1)
            put("updated_at", updatedAt)
        }
        db.update("fee_payments", values, "id = ?", arrayOf(paymentId))
        _feesUpdateFlow.tryEmit(Unit)
    }

    // --- Attendance Operations ---

    fun getAllAttendanceRecords(): List<AttendanceRecordModel> {
        val db = readableDatabase
        val list = mutableListOf<AttendanceRecordModel>()
        val cursor = db.rawQuery(
            "SELECT data_json FROM attendance WHERE is_deleted = 0 ORDER BY timestamp_millis DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                attendanceFromJson(it.getString(0))?.let { record -> list.add(record) }
            }
        }
        return list
    }

    fun getExistingAttendance(date: String, timeSlot: String): AttendanceRecordModel? {
        val db = readableDatabase
        val altDate = if (date.contains('/')) date.replace('/', '-') else date.replace('-', '/')
        val cursor = db.rawQuery(
            "SELECT data_json FROM attendance WHERE (date = ? OR date = ?) AND time_slot = ? AND is_deleted = 0",
            arrayOf(date, altDate, timeSlot)
        )
        return cursor.use {
            if (it.moveToFirst()) attendanceFromJson(it.getString(0)) else null
        }
    }

    fun upsertAttendanceRecord(record: AttendanceRecordModel, updatedAt: Long = System.currentTimeMillis()) {
        val db = writableDatabase
        val timeMillis = record.timestamp?.toDate()?.time ?: System.currentTimeMillis()
        val values = ContentValues().apply {
            put("id", record.id)
            put("date", record.date)
            put("time_slot", record.timeSlot)
            put("timestamp_millis", timeMillis)
            put("data_json", attendanceToJson(record))
            put("updated_at", updatedAt)
            put("is_deleted", 0)
        }
        db.insertWithOnConflict("attendance", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        _attendanceUpdateFlow.tryEmit(Unit)
    }

    fun upsertAttendanceBatch(records: List<Pair<AttendanceRecordModel, Long>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for ((record, updatedAt) in records) {
                val timeMillis = record.timestamp?.toDate()?.time ?: System.currentTimeMillis()
                val values = ContentValues().apply {
                    put("id", record.id)
                    put("date", record.date)
                    put("time_slot", record.timeSlot)
                    put("timestamp_millis", timeMillis)
                    put("data_json", attendanceToJson(record))
                    put("updated_at", updatedAt)
                    put("is_deleted", 0)
                }
                db.insertWithOnConflict("attendance", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _attendanceUpdateFlow.tryEmit(Unit)
    }

    fun markAttendanceDeleted(id: String, updatedAt: Long = System.currentTimeMillis()) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("is_deleted", 1)
            put("updated_at", updatedAt)
        }
        db.update("attendance", values, "id = ?", arrayOf(id))
        _attendanceUpdateFlow.tryEmit(Unit)
    }

    // --- Teacher Attendance Operations ---

    fun getAllTeacherAttendance(teacherId: String? = null): List<TeacherAttendanceRequestModel> {
        val db = readableDatabase
        val list = mutableListOf<TeacherAttendanceRequestModel>()
        val query = if (!teacherId.isNullOrBlank()) {
            db.rawQuery(
                "SELECT data_json FROM teacher_attendance WHERE teacher_id = ? AND is_deleted = 0 ORDER BY timestamp_millis DESC",
                arrayOf(teacherId)
            )
        } else {
            db.rawQuery(
                "SELECT data_json FROM teacher_attendance WHERE is_deleted = 0 ORDER BY timestamp_millis DESC",
                null
            )
        }
        query.use {
            while (it.moveToNext()) {
                teacherAttendanceFromJson(it.getString(0))?.let { req -> list.add(req) }
            }
        }
        return list
    }

    fun getTeacherAttendance(id: String): TeacherAttendanceRequestModel? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT data_json FROM teacher_attendance WHERE id = ? AND is_deleted = 0",
            arrayOf(id)
        )
        return cursor.use {
            if (it.moveToFirst()) teacherAttendanceFromJson(it.getString(0)) else null
        }
    }

    fun upsertTeacherAttendance(req: TeacherAttendanceRequestModel, updatedAt: Long = System.currentTimeMillis()) {
        val db = writableDatabase
        val timeMillis = req.timestamp?.toDate()?.time ?: System.currentTimeMillis()
        val values = ContentValues().apply {
            put("id", req.id)
            put("teacher_id", req.teacherId)
            put("date", req.date)
            put("time_slot", req.timeSlot)
            put("status", req.status)
            put("timestamp_millis", timeMillis)
            put("data_json", teacherAttendanceToJson(req))
            put("updated_at", updatedAt)
            put("is_deleted", 0)
        }
        db.insertWithOnConflict("teacher_attendance", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        _teacherAttendanceUpdateFlow.tryEmit(Unit)
    }

    fun upsertTeacherAttendanceBatch(records: List<Pair<TeacherAttendanceRequestModel, Long>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for ((req, updatedAt) in records) {
                val timeMillis = req.timestamp?.toDate()?.time ?: System.currentTimeMillis()
                val values = ContentValues().apply {
                    put("id", req.id)
                    put("teacher_id", req.teacherId)
                    put("date", req.date)
                    put("time_slot", req.timeSlot)
                    put("status", req.status)
                    put("timestamp_millis", timeMillis)
                    put("data_json", teacherAttendanceToJson(req))
                    put("updated_at", updatedAt)
                    put("is_deleted", 0)
                }
                db.insertWithOnConflict("teacher_attendance", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _teacherAttendanceUpdateFlow.tryEmit(Unit)
    }

    fun markTeacherAttendanceDeleted(id: String, updatedAt: Long = System.currentTimeMillis()) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("is_deleted", 1)
            put("updated_at", updatedAt)
        }
        db.update("teacher_attendance", values, "id = ?", arrayOf(id))
        _teacherAttendanceUpdateFlow.tryEmit(Unit)
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
}
