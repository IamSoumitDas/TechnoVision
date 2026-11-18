package com.soumit.technovision.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.soumit.technovision.data.local.DeltaSyncManager
import com.soumit.technovision.data.local.LocalDatabase
import com.soumit.technovision.data.repository.*
import com.soumit.technovision.domain.usecase.CalculateBillingDuesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideDeltaSyncManager(
        localDb: LocalDatabase,
        firestore: FirebaseFirestore
    ): DeltaSyncManager = DeltaSyncManager(localDb, firestore)

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository = FirebaseAuthRepository(auth, firestore)

    @Provides
    @Singleton
    fun provideStudentRepository(
        firestore: FirebaseFirestore,
        localDb: LocalDatabase,
        syncManager: DeltaSyncManager
    ): StudentRepository = FirestoreStudentRepository(firestore, localDb, syncManager)

    @Provides
    @Singleton
    fun provideFeeRepository(
        firestore: FirebaseFirestore,
        localDb: LocalDatabase,
        syncManager: DeltaSyncManager
    ): FeeRepository = FirestoreFeeRepository(firestore, localDb, syncManager)

    @Provides
    @Singleton
    fun provideAttendanceRepository(
        firestore: FirebaseFirestore,
        localDb: LocalDatabase,
        syncManager: DeltaSyncManager
    ): AttendanceRepository = FirestoreAttendanceRepository(firestore, localDb, syncManager)

    @Provides
    @Singleton
    fun provideStaffRepository(
        firestore: FirebaseFirestore
    ): StaffRepository = FirestoreStaffRepository(firestore)

    @Provides
    @Singleton
    fun provideAuditLogRepository(
        firestore: FirebaseFirestore
    ): AuditLogRepository = FirestoreAuditLogRepository(firestore)

    @Provides
    @Singleton
    fun provideCalculateBillingDuesUseCase(): CalculateBillingDuesUseCase = CalculateBillingDuesUseCase()
}
