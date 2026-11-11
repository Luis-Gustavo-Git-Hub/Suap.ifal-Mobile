package com.example.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SuapMacrobenchmark {
    private companion object {
        const val APP_READY_TIMEOUT_MS = 30_000L
        const val SCREEN_SCROLL_DURATION_MS = 10_000L
        const val FRAME_ITERATIONS = 5
    }

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() {
        prepareBenchmarkData()
        benchmarkRule.measureRepeated(
            packageName = BenchmarkTestData.TARGET_PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            iterations = 10,
            startupMode = StartupMode.COLD,
            setupBlock = { pressHome() }
        ) {
            startActivityAndWait()
            BenchmarkTestData.waitForDashboard(device)
            assertDashboardIsReady()
        }
    }

    @Test
    fun dashboardScrollFrameTiming() = benchmarkScreenScroll(By.text("Início"))

    @Test
    fun aulasScrollFrameTiming() = benchmarkScreenScroll(
        By.textContains("Conteúdo de teste de desempenho"),
        route = "aulas"
    )

    @Test
    fun friendsScrollFrameTiming() = benchmarkScreenScroll(By.text("Rede Acadêmica"), route = "friends")

    @Test
    fun horariosScrollFrameTiming() = benchmarkScreenScroll(By.textContains("Disciplina de teste"), route = "horarios")

    @Test
    fun avisosScrollFrameTiming() = benchmarkScreenScroll(By.textContains("Aviso de teste de desempenho"), route = "avisos")

    @Test
    fun statisticsScrollFrameTiming() = benchmarkScreenScroll(By.text("Média geral"), route = "statistics")

    @Test
    fun boletimScrollFrameTiming() = benchmarkScreenScroll(By.text("1 disciplinas"), route = "boletim")

    private fun benchmarkScreenScroll(
        expectedScreenContent: androidx.test.uiautomator.BySelector,
        route: String? = null
    ) {
        prepareBenchmarkData()
        benchmarkRule.measureRepeated(
            packageName = BenchmarkTestData.TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            iterations = FRAME_ITERATIONS,
            startupMode = StartupMode.COLD,
            setupBlock = { pressHome() }
        ) {
            if (route == null) {
                startActivityAndWait()
                BenchmarkTestData.waitForDashboard(device)
                assertDashboardIsReady()
            } else {
                startActivityAndWait(BenchmarkTestData.routeIntent(route))
            }
            assertTrue(
                "Expected screen content was not found after navigation",
                device.wait(Until.hasObject(expectedScreenContent), APP_READY_TIMEOUT_MS)
            )

            val scrollable = device.findObject(By.scrollable(true))

            val deadline = SystemClock.uptimeMillis() + SCREEN_SCROLL_DURATION_MS
            while (SystemClock.uptimeMillis() < deadline) {
                if (scrollable != null) {
                    scrollable.fling(Direction.DOWN)
                    scrollable.fling(Direction.UP)
                } else {
                    val x = device.displayWidth / 2
                    val top = (device.displayHeight * 0.32f).toInt()
                    val bottom = (device.displayHeight * 0.76f).toInt()
                    device.swipe(x, bottom, x, top, 12)
                    device.swipe(x, top, x, bottom, 12)
                }
            }
        }
    }

    private fun prepareBenchmarkData() {
        BenchmarkTestData.prepare(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()))
    }

    private fun MacrobenchmarkScope.assertDashboardIsReady() {
        assertTrue(
            "SUAP app did not reach the Dashboard",
            device.wait(Until.hasObject(By.text("Início")), APP_READY_TIMEOUT_MS)
        )
    }
}
