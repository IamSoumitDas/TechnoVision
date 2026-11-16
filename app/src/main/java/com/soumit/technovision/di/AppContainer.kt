package com.soumit.technovision.di

import com.soumit.technovision.data.repository.*

interface AppContainer {
    val authRepository: AuthRepository
    val studentRepository: StudentRepository
    val attendanceRepository: AttendanceRepository
    val feeRepository: FeeRepository
    val staffRepository: StaffRepository
    val auditLogRepository: AuditLogRepository
    val calculateBillingDuesUseCase: com.soumit.technovision.domain.usecase.CalculateBillingDuesUseCase
}

class DefaultAppContainer : AppContainer {
    override val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }
    override val studentRepository: StudentRepository by lazy { FirestoreStudentRepository() }
    override val attendanceRepository: AttendanceRepository by lazy { FirestoreAttendanceRepository() }
    override val feeRepository: FeeRepository by lazy { FirestoreFeeRepository() }
    override val staffRepository: StaffRepository by lazy { FirestoreStaffRepository() }
    override val auditLogRepository: AuditLogRepository by lazy { FirestoreAuditLogRepository() }
    override val calculateBillingDuesUseCase: com.soumit.technovision.domain.usecase.CalculateBillingDuesUseCase by lazy {
        com.soumit.technovision.domain.usecase.CalculateBillingDuesUseCase()
    }
}
