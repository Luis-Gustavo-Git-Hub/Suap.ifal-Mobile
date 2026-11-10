package com.example.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SuapMacrobenchmark {
    private companion object {
        const val TARGET_PACKAGE = "com.aistudio.suap.xkcdz"
        const val APP_READY_TIMEOUT_MS = 10_000L
        const val DASHBOARD_SCROLL_DURATION_MS = 10_000L
    }

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
        setupBlock = { pressHome() }
    ) {
        startActivityAndWait()
        assertDashboardIsReady()
    }

    @Test
    fun dashboardScrollFrameTiming() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
        setupBlock = { pressHome() }
    ) {
        startActivityAndWait()
        assertDashboardIsReady()

        val scrollable = device.findObject(By.scrollable(true))
        assertTrue("Dashboard should expose a scrollable content area", scrollable != null)

        val deadline = SystemClock.uptimeMillis() + DASHBOARD_SCROLL_DURATION_MS
        while (SystemClock.uptimeMillis() < deadline) {
            scrollable?.fling(Direction.DOWN)
            scrollable?.fling(Direction.UP)
        }
    }

    private fun MacrobenchmarkScope.assertDashboardIsReady() {
        assertTrue(
            "SUAP app did not reach its first screen",
            device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE)), APP_READY_TIMEOUT_MS)
        )
    }
}
