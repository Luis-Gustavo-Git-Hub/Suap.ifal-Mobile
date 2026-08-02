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

@SuppressLint("InvalidFragmentVersionForActivityResult")
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        CrashLogger.autoSaveFullLogToDownloads(this)
        safeScheduleAlarms()

        val app = application as SuapApplication
        val sharedPrefs = getSharedPreferences("suap_credentials", android.content.Context.MODE_PRIVATE)
        val viewModelFactory = SuapViewModelFactory(app.repository, sharedPrefs)
        val viewModel = ViewModelProvider(this, viewModelFactory)[SuapViewModel::class.java]

        enableEdgeToEdge()

        setContent {
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()

            MyApplicationTheme(appTheme = appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        CrashLogger.autoSaveFullLogToDownloads(this)
    }

    private fun safeScheduleAlarms() {
        try {
            LunchReminderReceiver.scheduleAlarm(this)
            ClassReminderReceiver.scheduleNextClassAlarm(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
