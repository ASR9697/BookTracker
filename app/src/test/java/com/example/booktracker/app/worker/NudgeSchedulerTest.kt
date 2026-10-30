package com.example.booktracker.app.worker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class NudgeSchedulerTest {

    @Test
    fun `pick-up reminder goes to tomorrow evening after an evening session`() {
        val now = LocalDateTime.of(2026, 10, 30, 21, 40)
        assertEquals(LocalDateTime.of(2026, 10, 31, 21, 0), NudgeScheduler.pickUpTime(now, 21))
    }

    @Test
    fun `pick-up reminder can fall later today when it's half a day away`() {
        val now = LocalDateTime.of(2026, 10, 30, 7, 15)
        assertEquals(LocalDateTime.of(2026, 10, 30, 21, 0), NudgeScheduler.pickUpTime(now, 21))
    }

    @Test
    fun `a late-night hour skips the same night`() {
        // 1 am is three hours away: too soon after reading, so the night after.
        val now = LocalDateTime.of(2026, 10, 30, 22, 0)
        assertEquals(LocalDateTime.of(2026, 11, 1, 1, 0), NudgeScheduler.pickUpTime(now, 1))
    }
}
