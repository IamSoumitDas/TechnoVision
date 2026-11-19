package com.soumit.technovision.di

import android.content.Context
import com.soumit.technovision.data.local.DeltaSyncManager
import com.soumit.technovision.data.local.LocalDatabase
import com.soumit.technovision.data.repository.*
import com.soumit.technovision.util.NetworkMonitor

interface AppContainer {
    val localDatabase: LocalDatabase
    val deltaSyncManager: DeltaSyncManager
    val networkMonitor: NetworkMonitor
    val authRepository: AuthRepository
    val studentRepository: StudentRepository
    val attendanceRepository: AttendanceRepository
    val feeRepository: FeeRepository
    val staffRepository: StaffRepository
    val auditLogRepository: AuditLogRepository
    val calculateBillingDuesUseCase: com.soumit.technovision.domain.usecase.CalculateBillingDuesUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val localDatabase: LocalDatabase by lazy { LocalDatabase.getInstance(context) }
    override val deltaSyncManager: DeltaSyncManager by lazy { DeltaSyncManager(localDatabase) }
    override val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(context) }
    override val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }
    override val studentRepository: StudentRepository by lazy { FirestoreStudentRepository(localDb = localDatabase, syncManager = deltaSyncManager) }
    override val attendanceRepository: AttendanceRepository by lazy { FirestoreAttendanceRepository(localDb = localDatabase, syncManager = deltaSyncManager) }
    override val feeRepository: FeeRepository by lazy { FirestoreFeeRepository(localDb = localDatabase, syncManager = deltaSyncManager) }
    override val staffRepository: StaffRepository by lazy { FirestoreStaffRepository() }
    override val auditLogRepository: AuditLogRepository by lazy { FirestoreAuditLogRepository() }
    override val calculateBillingDuesUseCase: com.soumit.technovision.domain.usecase.CalculateBillingDuesUseCase by lazy {
        com.soumit.technovision.domain.usecase.CalculateBillingDuesUseCase()
    }
}
