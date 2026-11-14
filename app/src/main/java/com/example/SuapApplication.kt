package com.example

import android.app.Application
import android.os.StrictMode
import android.webkit.WebView
import com.example.BuildConfig
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

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .build()
            )
        }
    }

    /** Runs after the first frame from an IO coroutine so login stays responsive. */
    fun initializeBackgroundServices() {
        NotificationHelper.createNotificationChannel(this)
        val credentialStore = CredentialStore.getInstance(this)
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val backgroundChecksEnabled = sharedPrefs.getBoolean("notifications_enabled", false) ||
            sharedPrefs.getBoolean("notify_grades", false)
        if (backgroundChecksEnabled && credentialStore.hasSuapCredentials()) {
            com.example.worker.WorkScheduler.scheduleDailySync(this)
        } else {
            // Remove schedules created by older versions when checks are now off
            // or the user has disconnected the SUAP account.
            com.example.worker.WorkScheduler.cancelSync(this)
        }
    }
}
