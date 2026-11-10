package com.example.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
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
class BaselineProfileGenerator {

    private companion object {
        const val APP_READY_TIMEOUT_MS = 10_000L
    }

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateStartupAndDashboardProfile() {
        prepareBenchmarkData()
        baselineProfileRule.collect(packageName = BenchmarkTestData.TARGET_PACKAGE) {
            pressHome()
            startActivityAndWait()
            BenchmarkTestData.waitForDashboard(device)
            assertDashboardIsReady()
            device.waitForIdle()
        }
    }

    @Test
    fun generateBoletimProfile() {
        prepareBenchmarkData()
        baselineProfileRule.collect(packageName = BenchmarkTestData.TARGET_PACKAGE) {
            pressHome()
            startActivityAndWait()
            BenchmarkTestData.waitForDashboard(device)
            BenchmarkTestData.clickTextContaining(device, "Boletim Escolar")
            assertTrue(
                "Boletim did not show the seeded subject",
                device.wait(Until.hasObject(By.textContains("Programação de Sistemas")), APP_READY_TIMEOUT_MS)
            )
            val subjectList = device.findObject(By.scrollable(true))
            assertTrue("Boletim should expose a scrollable subject list", subjectList != null)
            subjectList?.fling(Direction.DOWN)
            subjectList?.fling(Direction.UP)
            device.waitForIdle()
        }
    }

    @Test
    fun generateStatisticsProfile() {
        prepareBenchmarkData()
        baselineProfileRule.collect(packageName = BenchmarkTestData.TARGET_PACKAGE) {
            pressHome()
            startActivityAndWait()
            BenchmarkTestData.waitForDashboard(device)
            BenchmarkTestData.clickText(device, "Estatística")
            assertTrue(
                "Statistics screen did not open",
                device.wait(Until.hasObject(By.text("Seu resumo acadêmico")), APP_READY_TIMEOUT_MS)
            )
            val statisticsList = device.findObject(By.scrollable(true))
            assertTrue("Statistics should expose a scrollable content area", statisticsList != null)
            statisticsList?.fling(Direction.DOWN)
            statisticsList?.fling(Direction.UP)
            device.waitForIdle()
        }
    }

    private fun prepareBenchmarkData() {
        BenchmarkTestData.prepare(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()))
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.assertDashboardIsReady() {
        assertTrue(
            "SUAP app did not reach the Dashboard",
            device.wait(Until.hasObject(By.text("Início")), APP_READY_TIMEOUT_MS)
        )
    }
}
