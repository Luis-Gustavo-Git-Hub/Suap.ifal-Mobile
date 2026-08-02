package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.SubjectRepository
import com.example.util.CrashLogger
import com.example.util.NotificationHelper

class SuapApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { SubjectRepository(database.subjectDao()) }

    override fun onCreate() {
        super.onCreate()
        CrashLogger.init(this)
        NotificationHelper.createNotificationChannel(this)
        
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (sharedPrefs.getBoolean("notifications_enabled", false)) {
            com.example.worker.WorkScheduler.scheduleDailySync(this)
        }
    }
}
