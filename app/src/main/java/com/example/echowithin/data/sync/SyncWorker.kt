package com.example.echowithin.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.echowithin.data.network.SessionManager
import com.example.echowithin.data.repository.NotesRepository
import java.util.concurrent.TimeUnit

/**
 * Background sync worker. Runs periodically with a network constraint and
 * exponential backoff, so pending local changes are pushed and fresh notes
 * pulled even when the app is backgrounded. The Room/SQLite local store is
 * the source of truth; this worker only reconciles with the server.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (SessionManager.token.isNullOrBlank() || SessionManager.token == "null") {
            return Result.success()
        }
        return try {
            NotesRepository().syncNotes().fold(
                onSuccess = { Result.success() },
                onFailure = { e ->
                    // Transient network failure → retry with backoff. Any other
                    // failure (e.g. auth) is terminal for this run.
                    if (e is java.io.IOException || e.message == "Server unreachable") Result.retry()
                    else Result.success()
                }
            )
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK = "echowithin_background_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
