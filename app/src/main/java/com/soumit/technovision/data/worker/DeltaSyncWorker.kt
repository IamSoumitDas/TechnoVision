package com.soumit.technovision.data.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.soumit.technovision.TechnoVisionApplication
import com.soumit.technovision.data.local.DeltaSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class DeltaSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as? TechnoVisionApplication
        val syncManager = app?.container?.deltaSyncManager
            ?: return@withContext Result.failure()

        Log.d(TAG, "Starting periodic background delta sync via WorkManager...")

        try {
            val studentsResult = syncManager.syncStudents(force = true)
            val feesResult = syncManager.syncFees(force = true)
            val attendanceResult = syncManager.syncAttendance(force = true)
            val teacherAttendanceResult = syncManager.syncTeacherAttendance(force = true)

            val anyFailed = studentsResult.isFailure ||
                    feesResult.isFailure ||
                    attendanceResult.isFailure ||
                    teacherAttendanceResult.isFailure

            if (anyFailed) {
                Log.w(TAG, "One or more delta sync operations failed during WorkManager run.")
                Result.retry()
            } else {
                Log.d(TAG, "Background delta sync completed successfully.")
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing background delta sync: ${e.localizedMessage}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "DeltaSyncWorker"
        const val WORK_NAME = "technovision_delta_sync_work"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<DeltaSyncWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            Log.d(TAG, "Scheduled unique periodic WorkManager task (15 min interval).")
        }

        fun enqueueOneTimeSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<DeltaSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(oneTimeRequest)
            Log.d(TAG, "Enqueued immediate one-time WorkManager delta sync.")
        }
    }
}
