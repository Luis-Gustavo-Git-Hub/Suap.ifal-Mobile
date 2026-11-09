package com.example.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateStartupAndDashboardProfile() = baselineProfileRule.collect(
        packageName = "com.aistudio.suap.xkcdz"
    ) {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()

        // Wait for main screen content (login or dashboard)
        device.wait(Until.hasObject(By.pkg("com.aistudio.suap.xkcdz")), 5_000)
    }

    @Test
    fun generateBoletimProfile() = baselineProfileRule.collect(
        packageName = "com.aistudio.suap.xkcdz"
    ) {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()

        // Locate and click Boletim button/card if present
        val boletimEntry = device.findObject(By.textContains("Boletim"))
        boletimEntry?.click()
        device.waitForIdle()

        // Fling/scroll the subject list
        val subjectList = device.findObject(By.scrollable(true))
        subjectList?.fling(Direction.DOWN)
        device.waitForIdle()
        subjectList?.fling(Direction.UP)
        device.waitForIdle()
    }
}

