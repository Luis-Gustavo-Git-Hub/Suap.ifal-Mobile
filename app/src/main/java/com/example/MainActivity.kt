@file:Suppress("InvalidFragmentVersionForActivityResult")
@file:SuppressLint("InvalidFragmentVersionForActivityResult")
package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.receiver.ClassReminderReceiver
import com.example.receiver.LunchReminderReceiver
import com.example.ui.AppNavigation
import com.example.ui.SuapViewModel
import com.example.ui.SuapViewModelFactory
import com.example.ui.theme.MyApplicationTheme

import com.example.util.CrashLogger
import com.example.util.NotificationHelper

@OptIn(androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)
@SuppressLint("InvalidFragmentVersionForActivityResult")
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        safeScheduleAlarms()

        val app = application as SuapApplication
        val sharedPrefs = getSharedPreferences("suap_credentials", android.content.Context.MODE_PRIVATE)
        
        com.example.util.CredentialStore.getInstance(this).migrateOldCredentialsIfNeeded()
        com.example.util.NotificationHelper.createNotificationChannel(this)
        val viewModelFactory = SuapViewModelFactory(this.applicationContext, app.repository, sharedPrefs)
        val viewModel = ViewModelProvider(this, viewModelFactory)[SuapViewModel::class.java]

        enableEdgeToEdge()

        setContent {
            val windowSizeClass = androidx.compose.material3.windowsizeclass.calculateWindowSizeClass(this)
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            val fontOverride by viewModel.fontOverride.collectAsStateWithLifecycle()

            androidx.compose.runtime.CompositionLocalProvider(
                com.example.ui.components.LocalWindowSizeClass provides windowSizeClass
            ) {
                MyApplicationTheme(appTheme = appTheme, fontOverride = fontOverride) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(viewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun safeScheduleAlarms() {
        try {
            LunchReminderReceiver.scheduleAlarm(this)
            ClassReminderReceiver.scheduleNextClassAlarm(this)
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
        }
    }
}
