package com.example.baselineprofile

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

/** Creates isolated, non-personal data so protected dashboard routes are measurable in CI. */
internal object BenchmarkTestData {
    const val TARGET_PACKAGE = "com.aistudio.suap.xkcdz"
    private const val MAIN_ACTIVITY = "$TARGET_PACKAGE/com.example.MainActivity"
    private const val SEED_ACTION = "$TARGET_PACKAGE.action.SEED_BENCHMARK"
    private const val READY_TIMEOUT_MS = 15_000L

    fun prepare(device: UiDevice) {
        device.executeShellCommand("am force-stop $TARGET_PACKAGE")
        val seedResult = device.executeShellCommand("am start -W -n $MAIN_ACTIVITY -a $SEED_ACTION")
        check(seedResult.contains("Status: ok")) {
            "SUAP benchmark data seeding did not complete: $seedResult"
        }
        device.executeShellCommand("am force-stop $TARGET_PACKAGE")
        launchApp(device)
        device.pressHome()
        device.executeShellCommand("am force-stop $TARGET_PACKAGE")
    }

    fun waitForDashboard(device: UiDevice) {
        // A first-access dialog can exist on older app data; close it before measuring navigation.
        device.wait(Until.findObject(By.text("Pular")), 500L)?.click()
        val homeTab = By.text("Início")
        for (attempt in 0 until 4) {
            if (device.findObject(homeTab) != null) break
            device.pressBack()
        }
        check(device.wait(Until.hasObject(homeTab), READY_TIMEOUT_MS)) { "Main navigation did not become ready" }
        val dashboardTitle = By.textContains("Olá,")
        if (device.findObject(dashboardTitle) == null) device.findObject(homeTab)?.click()
        check(device.wait(Until.hasObject(dashboardTitle), READY_TIMEOUT_MS)) { "Dashboard did not become ready" }
    }

    fun clickText(device: UiDevice, text: String) = click(device, By.text(text), text)

    fun clickTextContaining(device: UiDevice, text: String) = click(device, By.textContains(text), text)

    fun scrollScreen(device: UiDevice) {
        val scrollable = device.findObject(By.scrollable(true))
        if (scrollable != null) {
            scrollable.fling(Direction.DOWN)
            scrollable.fling(Direction.UP)
            return
        }

        val x = device.displayWidth / 2
        val top = (device.displayHeight * 0.32f).toInt()
        val bottom = (device.displayHeight * 0.76f).toInt()
        device.swipe(x, bottom, x, top, 12)
        device.swipe(x, top, x, bottom, 12)
    }

    private fun click(device: UiDevice, selector: androidx.test.uiautomator.BySelector, label: String) {
        for (attempt in 0 until 8) {
            device.findObject(selector)?.let { target ->
                target.click()
                return
            }
            val scrollable = device.findObject(By.scrollable(true)) ?: break
            if (!scrollable.scroll(Direction.DOWN, 0.7f)) break
        }
        error("Could not find '$label' in the current screen")
    }

    private fun launchApp(device: UiDevice) {
        val launchResult = device.executeShellCommand("am start -W -n $MAIN_ACTIVITY")
        check(launchResult.contains("Status: ok")) {
            "SUAP app did not start for benchmark setup: $launchResult"
        }
        waitForDashboard(device)
    }

}
