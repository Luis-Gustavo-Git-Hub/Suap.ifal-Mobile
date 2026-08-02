package com.example.worker

import android.content.Context
import androidx.work.*
import java.util.*
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val WORK_NAME = "SuapSyncWork"

    fun scheduleDailySync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        val initialDelay = target.timeInMillis - now.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
