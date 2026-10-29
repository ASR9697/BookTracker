package com.example.booktracker.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.app.sync.DriveAuth
import com.example.booktracker.app.sync.DriveSync
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Background Drive sync every few hours on a network connection. It only
 * runs with a token Google hands back silently; if the reader has to agree
 * again (access revoked, account removed) it waits for the next manual sync.
 */
class DriveSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val settings = ServiceLocator.settings(context)
        if (!settings.driveSyncEnabled.first()) return Result.success()
        val token = (DriveAuth.authorize(context) as? DriveAuth.Outcome.Token)?.value ?: return Result.success()
        return runCatching { DriveSync.run(context, token) }
            .fold(
                onSuccess = {
                    settings.setDriveLastSync(System.currentTimeMillis())
                    Result.success()
                },
                onFailure = { if (runAttemptCount < 3) Result.retry() else Result.failure() }
            )
    }
}

object DriveSyncScheduler {
    private const val WORK_NAME = "drive_sync"

    fun apply(context: Context, enabled: Boolean) {
        val work = WorkManager.getInstance(context)
        if (!enabled) {
            work.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<DriveSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
