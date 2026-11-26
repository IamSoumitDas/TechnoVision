package com.soumit.technovision.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "students",
    indices = [
        Index(value = ["full_name"], name = "idx_students_name"),
        Index(value = ["student_class"], name = "idx_students_class"),
        Index(value = ["phone"], name = "idx_students_phone"),
        Index(value = ["is_deleted"], name = "idx_students_deleted"),
        Index(value = ["is_deleted", "full_name"], name = "idx_students_deleted_name")
    ]
)
data class StudentEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "full_name")
    val fullName: String,
    @ColumnInfo(name = "student_class", defaultValue = "''")
    val studentClass: String = "",
    @ColumnInfo(name = "school", defaultValue = "''")
    val school: String = "",
    @ColumnInfo(name = "dob", defaultValue = "''")
    val dob: String = "",
    @ColumnInfo(name = "gender", defaultValue = "''")
    val gender: String = "",
    @ColumnInfo(name = "father_name", defaultValue = "''")
    val fatherName: String = "",
    @ColumnInfo(name = "mother_name", defaultValue = "''")
    val motherName: String = "",
    @ColumnInfo(name = "monthly_installment", defaultValue = "500")
    val monthlyInstallment: Long = 500L,
    @ColumnInfo(name = "phone", defaultValue = "''")
    val phone: String = "",
    @ColumnInfo(name = "alt_phone", defaultValue = "''")
    val altPhone: String = "",
    @ColumnInfo(name = "email", defaultValue = "''")
    val email: String = "",
    @ColumnInfo(name = "batch_time_slot", defaultValue = "''")
    val batchTimeSlot: String = "",
    @ColumnInfo(name = "weekly_schedule_json", defaultValue = "'{}'")
    val weeklyScheduleJson: String = "{}",
    @ColumnInfo(name = "admission_date", defaultValue = "''")
    val admissionDate: String = "",
    @ColumnInfo(name = "total_paid", defaultValue = "0")
    val totalPaid: Long = 0L,
    @ColumnInfo(name = "data_json", defaultValue = "''")
    val dataJson: String = "",
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
        Index(value = ["is_deleted"], name = "idx_fees_deleted"),
        Index(value = ["is_deleted", "timestamp_millis"], name = "idx_fees_deleted_time"),
        Index(value = ["student_id", "is_deleted", "timestamp_millis"], name = "idx_fees_student_deleted_time")
    ]
)
data class FeePaymentEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "student_id")
    val studentId: String,
    @ColumnInfo(name = "student_name", defaultValue = "''")
    val studentName: String = "",
    @ColumnInfo(name = "amount_paid")
    val amountPaid: Long,
    @ColumnInfo(name = "payment_method", defaultValue = "''")
    val paymentMethod: String = "",
    @ColumnInfo(name = "remarks", defaultValue = "''")
    val remarks: String = "",
    @ColumnInfo(name = "received_by", defaultValue = "''")
    val receivedBy: String = "",
    @ColumnInfo(name = "timestamp_millis")
    val timestampMillis: Long,
    @ColumnInfo(name = "data_json", defaultValue = "''")
    val dataJson: String = "",
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
        Index(value = ["is_deleted"], name = "idx_att_deleted"),
        Index(value = ["is_deleted", "timestamp_millis"], name = "idx_att_deleted_time"),
        Index(value = ["date", "time_slot", "is_deleted"], name = "idx_att_date_slot_deleted")
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
    @ColumnInfo(name = "present_ids_csv", defaultValue = "''")
    val presentIdsCsv: String = "",
    @ColumnInfo(name = "absent_ids_csv", defaultValue = "''")
    val absentIdsCsv: String = "",
    @ColumnInfo(name = "additional_ids_csv", defaultValue = "''")
    val additionalIdsCsv: String = "",
    @ColumnInfo(name = "total_present", defaultValue = "0")
    val totalPresent: Int = 0,
    @ColumnInfo(name = "total_absent", defaultValue = "0")
    val totalAbsent: Int = 0,
    @ColumnInfo(name = "submitted_by", defaultValue = "''")
    val submittedBy: String = "",
    @ColumnInfo(name = "submitted_by_uid", defaultValue = "''")
    val submittedByUid: String = "",
    @ColumnInfo(name = "submitted_by_email", defaultValue = "''")
    val submittedByEmail: String = "",
    @ColumnInfo(name = "data_json", defaultValue = "''")
    val dataJson: String = "",
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
        Index(value = ["is_deleted"], name = "idx_tatt_deleted"),
        Index(value = ["teacher_id", "is_deleted", "timestamp_millis"], name = "idx_tatt_teacher_deleted_time"),
        Index(value = ["is_deleted", "timestamp_millis"], name = "idx_tatt_deleted_time")
    ]
)
data class TeacherAttendanceEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "teacher_id")
    val teacherId: String,
    @ColumnInfo(name = "teacher_name", defaultValue = "''")
    val teacherName: String = "",
    val date: String,
    @ColumnInfo(name = "time_slot")
    val timeSlot: String,
    val status: String,
    @ColumnInfo(name = "timestamp_millis")
    val timestampMillis: Long,
    @ColumnInfo(name = "verified_by", defaultValue = "''")
    val verifiedBy: String = "",
    @ColumnInfo(name = "rejected_by", defaultValue = "''")
    val rejectedBy: String = "",
    @ColumnInfo(name = "data_json", defaultValue = "''")
    val dataJson: String = "",
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
    val id: Long = 0L,
    @ColumnInfo(name = "entity_type")
    val entityType: String,
    @ColumnInfo(name = "doc_id")
    val docId: String,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    @ColumnInfo(name = "status")
    val status: String = "PENDING",
    @ColumnInfo(name = "attempts", defaultValue = "0")
    val attempts: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
