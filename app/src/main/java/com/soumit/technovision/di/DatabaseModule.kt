package com.soumit.technovision.di

import android.content.Context
import com.soumit.technovision.data.local.LocalDatabase
import com.soumit.technovision.data.local.room.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideLocalDatabase(@ApplicationContext context: Context): LocalDatabase {
        return LocalDatabase.getInstance(context)
    }

    @Provides
    fun provideStudentDao(database: AppDatabase): StudentDao = database.studentDao()

    @Provides
    fun provideFeePaymentDao(database: AppDatabase): FeePaymentDao = database.feePaymentDao()

    @Provides
    fun provideAttendanceDao(database: AppDatabase): AttendanceDao = database.attendanceDao()

    @Provides
    fun provideTeacherAttendanceDao(database: AppDatabase): TeacherAttendanceDao = database.teacherAttendanceDao()

    @Provides
    fun provideSyncMetadataDao(database: AppDatabase): SyncMetadataDao = database.syncMetadataDao()
}
