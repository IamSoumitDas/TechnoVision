package com.soumit.technovision.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.soumit.technovision.data.model.AttendanceRecordModel
import com.soumit.technovision.data.model.FeePaymentModel
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.model.TeacherAttendanceRequestModel

@Entity(
    tableName = "students",
    indices = [
        Index(value = ["full_name"], name = "idx_students_name"),
        Index(value = ["is_deleted"], name = "idx_students_deleted")
    ]
)
data class StudentEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "full_name")
    val fullName: String,
    @ColumnInfo(name = "data_json")
    val dataJson: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Int = 0
)

@Entity(
    tableName = "fee_payments",
    indices = [
        Index(value = ["student_id"], name = "idx_fees_student"),
        Index(value = ["timestamp_millis"], name = "idx_fees_time"),
        Index(value = ["is_deleted"], name = "idx_fees_deleted")
    ]
)
data class FeePaymentEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "student_id")
    val studentId: String,
    @ColumnInfo(name = "amount_paid")
    val amountPaid: Long,
    @ColumnInfo(name = "timestamp_millis")
    val timestampMillis: Long,
    @ColumnInfo(name = "data_json")
    val dataJson: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Int = 0
)

@Entity(
    tableName = "attendance",
    indices = [
        Index(value = ["date", "time_slot"], name = "idx_att_date_slot"),
        Index(value = ["timestamp_millis"], name = "idx_att_time"),
        Index(value = ["is_deleted"], name = "idx_att_deleted")
    ]
)
data class AttendanceEntity(
    @PrimaryKey
    val id: String,
    val date: String,
    @ColumnInfo(name = "time_slot")
    val timeSlot: String,
    @ColumnInfo(name = "timestamp_millis")
    val timestampMillis: Long,
    @ColumnInfo(name = "data_json")
    val dataJson: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Int = 0
)

@Entity(
    tableName = "teacher_attendance",
    indices = [
        Index(value = ["teacher_id"], name = "idx_tatt_teacher"),
        Index(value = ["timestamp_millis"], name = "idx_tatt_time"),
        Index(value = ["is_deleted"], name = "idx_tatt_deleted")
    ]
)
data class TeacherAttendanceEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "teacher_id")
    val teacherId: String,
    val date: String,
    @ColumnInfo(name = "time_slot")
    val timeSlot: String,
    val status: String,
    @ColumnInfo(name = "timestamp_millis")
    val timestampMillis: Long,
    @ColumnInfo(name = "data_json")
    val dataJson: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Int = 0
)

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "collection_name")
    val collectionName: String,
    @ColumnInfo(name = "last_sync_time", defaultValue = "0")
    val lastSyncTime: Long = 0L
)

@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["status"], name = "idx_sq_status"),
        Index(value = ["created_at"], name = "idx_sq_created")
    ]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "entity_type")
    val entityType: String,
    @ColumnInfo(name = "doc_id")
    val docId: String,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING",
    val attempts: Int = 0
)
