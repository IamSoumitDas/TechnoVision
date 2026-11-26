package com.soumit.technovision.data.local.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE is_deleted = 0 ORDER BY full_name COLLATE NOCASE ASC")
    fun getAllStudentsFlow(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE is_deleted = 0 ORDER BY full_name COLLATE NOCASE ASC")
    suspend fun getAllStudents(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :studentId AND is_deleted = 0 LIMIT 1")
    fun getStudentFlow(studentId: String): Flow<StudentEntity?>

    @Query("SELECT * FROM students WHERE id = :studentId AND is_deleted = 0 LIMIT 1")
    suspend fun getStudent(studentId: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<StudentEntity>)

    @Query("UPDATE students SET is_deleted = 1, updated_at = :updatedAt WHERE id = :studentId")
    suspend fun markDeleted(studentId: String, updatedAt: Long)
}

@Dao
interface FeePaymentDao {
    @Query("SELECT * FROM fee_payments WHERE is_deleted = 0 ORDER BY timestamp_millis DESC")
    fun getAllFeePaymentsFlow(): Flow<List<FeePaymentEntity>>

    @Query("SELECT * FROM fee_payments WHERE is_deleted = 0 ORDER BY timestamp_millis DESC")
    suspend fun getAllFeePayments(): List<FeePaymentEntity>

    @Query("SELECT * FROM fee_payments WHERE student_id = :studentId AND is_deleted = 0 ORDER BY timestamp_millis DESC")
    fun getFeePaymentsForStudentFlow(studentId: String): Flow<List<FeePaymentEntity>>

    @Query("SELECT * FROM fee_payments WHERE student_id = :studentId AND is_deleted = 0 ORDER BY timestamp_millis DESC")
    suspend fun getFeePaymentsForStudent(studentId: String): List<FeePaymentEntity>

    @Query("SELECT * FROM fee_payments WHERE id = :paymentId AND is_deleted = 0 LIMIT 1")
    suspend fun getFeePayment(paymentId: String): FeePaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FeePaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<FeePaymentEntity>)

    @Query("UPDATE fee_payments SET is_deleted = 1, updated_at = :updatedAt WHERE id = :paymentId")
    suspend fun markDeleted(paymentId: String, updatedAt: Long)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE is_deleted = 0 ORDER BY timestamp_millis DESC")
    fun getAllAttendanceFlow(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE is_deleted = 0 ORDER BY timestamp_millis DESC")
    suspend fun getAllAttendance(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE (date = :date OR date = :altDate) AND time_slot = :timeSlot AND is_deleted = 0 LIMIT 1")
    suspend fun getExistingAttendance(date: String, altDate: String, timeSlot: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE (date = :date OR date = :altDate OR date = :normDate) AND is_deleted = 0")
    suspend fun getAttendanceByDateCandidates(date: String, altDate: String, normDate: String): List<AttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AttendanceEntity>)

    @Query("UPDATE attendance SET is_deleted = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun markDeleted(id: String, updatedAt: Long)
}

@Dao
interface TeacherAttendanceDao {
    @Query("SELECT * FROM teacher_attendance WHERE is_deleted = 0 ORDER BY timestamp_millis DESC")
    fun getAllTeacherAttendanceFlow(): Flow<List<TeacherAttendanceEntity>>

    @Query("SELECT * FROM teacher_attendance WHERE is_deleted = 0 ORDER BY timestamp_millis DESC")
    suspend fun getAllTeacherAttendance(): List<TeacherAttendanceEntity>

    @Query("SELECT * FROM teacher_attendance WHERE teacher_id = :teacherId AND is_deleted = 0 ORDER BY timestamp_millis DESC")
    fun getTeacherAttendanceForTeacherFlow(teacherId: String): Flow<List<TeacherAttendanceEntity>>

    @Query("SELECT * FROM teacher_attendance WHERE teacher_id = :teacherId AND is_deleted = 0 ORDER BY timestamp_millis DESC")
    suspend fun getTeacherAttendanceForTeacher(teacherId: String): List<TeacherAttendanceEntity>

    @Query("SELECT * FROM teacher_attendance WHERE id = :id AND is_deleted = 0 LIMIT 1")
    suspend fun getTeacherAttendance(id: String): TeacherAttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TeacherAttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<TeacherAttendanceEntity>)

    @Query("UPDATE teacher_attendance SET is_deleted = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun markDeleted(id: String, updatedAt: Long)

    @Query("DELETE FROM teacher_attendance WHERE id = :id")
    suspend fun deletePermanently(id: String)
}

@Dao
interface SyncMetadataDao {
    @Query("SELECT last_sync_time FROM sync_metadata WHERE collection_name = :collectionName LIMIT 1")
    suspend fun getLastSyncTime(collectionName: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setLastSyncTime(entity: SyncMetadataEntity)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY created_at ASC")
    suspend fun getPendingItems(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY created_at ASC")
    fun getPendingItemsFlow(): Flow<List<SyncQueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(entity: SyncQueueEntity): Long

    @Query("UPDATE sync_queue SET status = :status, attempts = attempts + 1 WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: Long)
}
