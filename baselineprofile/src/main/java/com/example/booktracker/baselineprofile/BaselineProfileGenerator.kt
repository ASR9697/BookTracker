package com.example.booktracker.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the startup profile: a cold start, then the walk a reader takes most
 * days (Home, Library and a book's page, Notes, Stats, Profile), scrolling each.
 * The benchmark build seeds a demo library, so every screen has real content.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    private val targetAppId: String =
        InstrumentationRegistry.getArguments().getString("targetAppId")
            ?: error("targetAppId instrumentation argument is missing")

    @Test
    fun generate() = rule.collect(
        packageName = targetAppId,
        maxIterations = 8,
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
        // The demo library's current read sits on Home once data has loaded.
        device.wait(Until.hasObject(By.textContains("Pride and Prejudice")), TIMEOUT)
        scrollThrough()

        goTo("Library")
        device.wait(Until.hasObject(By.textContains("Moby-Dick")), TIMEOUT)
        scrollThrough()
        // A book's page: beside the library on a tablet, its own screen on a phone.
        device.findObject(By.textContains("Moby-Dick"))?.click()
        device.wait(Until.hasObject(By.text("Overview")), TIMEOUT)
        device.waitForIdle()
        scrollThrough()
        device.pressBack()
        device.waitForIdle()

        goTo("Notes")
        scrollThrough()
        goTo("Stats")
        scrollThrough()
        goTo("Profile")
        scrollThrough()
        goTo("Home")
    }

    /** Taps a main section in the bottom bar, rail or drawer, whichever this window shows. */
    private fun MacrobenchmarkScope.goTo(section: String) {
        val target = device.findObject(By.text(section)) ?: device.findObject(By.desc(section)) ?: return
        target.click()
        device.waitForIdle()
    }

    /** Down and back up through the content, so lists build and recycle their items. */
    private fun MacrobenchmarkScope.scrollThrough() {
        val x = device.displayWidth * 3 / 5
        val top = device.displayHeight / 4
        val bottom = device.displayHeight * 3 / 4
        repeat(2) {
            device.swipe(x, bottom, x, top, SWIPE_STEPS)
            device.waitForIdle()
        }
        repeat(2) {
            device.swipe(x, top, x, bottom, SWIPE_STEPS)
            device.waitForIdle()
        }
    }

    private companion object {
        const val TIMEOUT = 10_000L
        const val SWIPE_STEPS = 20
    }
}
