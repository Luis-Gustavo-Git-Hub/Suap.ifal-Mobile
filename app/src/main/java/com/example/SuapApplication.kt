package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.SubjectRepository
import com.example.util.CrashLogger
import com.example.util.CredentialStore
import com.example.util.NotificationHelper

class SuapApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { SubjectRepository(database.subjectDao()) }

    override fun onCreate() {
        super.onCreate()
        CrashLogger.init(this)
        NotificationHelper.createNotificationChannel(this)
        CredentialStore.getInstance(this)
        
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val backgroundChecksEnabled = sharedPrefs.getBoolean("notifications_enabled", false) ||
            sharedPrefs.getBoolean("notify_grades", false)
        if (backgroundChecksEnabled && CredentialStore.getInstance(this).hasSuapCredentials()) {
            com.example.worker.WorkScheduler.scheduleDailySync(this)
        } else {
            // Remove schedules created by older versions when checks are now off
            // or the user has disconnected the SUAP account.
            com.example.worker.WorkScheduler.cancelSync(this)
        }
    }
}
