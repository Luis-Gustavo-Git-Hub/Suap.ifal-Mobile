package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.worker.WorkScheduler

class SyncAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notifications_enabled", false) &&
            !prefs.getBoolean("notify_grades", false)
        ) {
            WorkScheduler.cancelSync(context)
            return
        }

        // Migrate devices with the previous exact alarm to the single
        // battery-friendly periodic WorkManager schedule.
        WorkScheduler.scheduleDailySync(context)
    }
}
