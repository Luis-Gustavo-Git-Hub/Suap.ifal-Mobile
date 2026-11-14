package com.example.worker

import android.content.Context
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {

    private const val PERIODIC_SYNC_WORK_NAME = "periodic_grade_sync"

    fun scheduleDailySync(context: Context) {
        // Sync at most once a day. WorkManager can defer within the flex window
        // to batch network activity with other work and reduce wakeups.
        try {
            cancelLegacyAlarm(context)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS, 6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
        } catch (e: Exception) {
            com.example.util.AppLog.e("WorkScheduler", "Failed to enqueue periodic work", e)
        }

    }

    fun cancelSync(context: Context) {
        try {
            cancelLegacyAlarm(context)
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
        } catch (e: Exception) {
            com.example.util.AppLog.e("WorkScheduler", "Failed to cancel periodic work", e)
        }

    }

    private fun cancelLegacyAlarm(context: Context) {
        val intent = Intent(context, com.example.receiver.SyncAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            3001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.cancel(pendingIntent)
    }
}
