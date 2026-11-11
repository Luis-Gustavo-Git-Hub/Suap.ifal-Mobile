@file:Suppress("InvalidFragmentVersionForActivityResult")
@file:SuppressLint("InvalidFragmentVersionForActivityResult")
package com.example

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.receiver.ClassReminderReceiver
import com.example.receiver.LunchReminderReceiver
import com.example.ui.AppNavigation
import com.example.ui.SuapViewModel
import com.example.ui.SuapViewModelFactory
import com.example.ui.theme.MyApplicationTheme

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@OptIn(androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)
@SuppressLint("InvalidFragmentVersionForActivityResult")
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.BENCHMARK_SEED_ENABLED && intent?.action == BenchmarkDataSeeder.ACTION) {
            runBlocking {
                withContext(Dispatchers.IO) {
                    BenchmarkDataSeeder.seed(applicationContext)
                }
            }
            finish()
            return
        }
        
        val app = application as SuapApplication
        val sharedPrefs = getSharedPreferences("suap_credentials", android.content.Context.MODE_PRIVATE)
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

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                app.initializeBackgroundServices()
                safeScheduleAlarms(applicationContext)
            } catch (e: Exception) {
                com.example.util.AppLog.e("MainActivity", "Could not initialize background services", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun safeScheduleAlarms(context: android.content.Context) {
        try {
            LunchReminderReceiver.scheduleAlarm(context)
            ClassReminderReceiver.scheduleNextClassAlarm(context)
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
        }
    }
}
