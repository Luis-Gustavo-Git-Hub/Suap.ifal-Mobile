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
        const val DASHBOARD_SCROLL_DURATION_MS = 10_000L
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
    fun dashboardScrollFrameTiming() = benchmarkScreenScroll(By.text("Início")) { }

    @Test
    fun aulasScrollFrameTiming() = benchmarkScreenScroll(By.textContains("Conteúdo de teste de desempenho")) {
        BenchmarkTestData.clickTextContaining(device, "Materiais e Aulas")
        BenchmarkTestData.clickText(device, "Aulas")
    }

    @Test
    fun friendsScrollFrameTiming() = benchmarkScreenScroll(By.text("Rede Acadêmica")) {
        BenchmarkTestData.clickText(device, "Amigos")
    }

    @Test
    fun horariosScrollFrameTiming() = benchmarkScreenScroll(By.textContains("Disciplina de teste")) {
        BenchmarkTestData.clickText(device, "Horários")
    }

    @Test
    fun avisosScrollFrameTiming() = benchmarkScreenScroll(By.textContains("Aviso de teste de desempenho")) {
        BenchmarkTestData.clickText(device, "Avisos")
    }

    @Test
    fun statisticsScrollFrameTiming() = benchmarkScreenScroll(By.text("Seu resumo acadêmico")) {
        BenchmarkTestData.clickText(device, "Estatística")
    }

    @Test
    fun boletimScrollFrameTiming() = benchmarkScreenScroll(By.textContains("Programação de Sistemas")) {
        BenchmarkTestData.clickTextContaining(device, "Boletim Escolar")
    }

    private fun benchmarkScreenScroll(
        expectedScreenContent: androidx.test.uiautomator.BySelector,
        navigateToScreen: MacrobenchmarkScope.() -> Unit
    ) {
        prepareBenchmarkData()
        benchmarkRule.measureRepeated(
            packageName = BenchmarkTestData.TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            iterations = FRAME_ITERATIONS,
            startupMode = StartupMode.COLD,
            setupBlock = { pressHome() }
        ) {
            startActivityAndWait()
            BenchmarkTestData.waitForDashboard(device)
            assertDashboardIsReady()
            navigateToScreen()
            assertTrue(
                "Expected screen content was not found after navigation",
                device.wait(Until.hasObject(expectedScreenContent), APP_READY_TIMEOUT_MS)
            )

            val scrollable = device.findObject(By.scrollable(true))
            assertTrue("The screen should expose a scrollable content area", scrollable != null)

            val deadline = SystemClock.uptimeMillis() + DASHBOARD_SCROLL_DURATION_MS
            while (SystemClock.uptimeMillis() < deadline) {
                scrollable?.fling(Direction.DOWN)
                scrollable?.fling(Direction.UP)
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
