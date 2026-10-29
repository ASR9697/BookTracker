package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FocusInsightsTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val day = LocalDate.of(2026, 10, 27).atStartOfDay(zone).toInstant().toEpochMilli()
    private var next = 0

    private fun rated(hour: Int, focus: Int, place: String = "") = Session(
        id = "s${next++}",
        bookId = "b",
        startTime = day + hour * 3_600_000L,
        endTime = day + hour * 3_600_000L + 1_800_000L,
        durationSeconds = 1800,
        pagesRead = 10,
        environmentTag = place,
        zoneId = "UTC",
        focus = focus
    )

    @Test
    fun `says nothing until enough sessions are rated`() {
        assertNull(FocusInsights.summarize(listOf(rated(20, 5), rated(9, 2)), zone))
    }

    @Test
    fun `unrated sessions are left out`() {
        val sessions = listOf(rated(20, 5), rated(20, 4), rated(9, 0), rated(9, 0))
        assertNull(FocusInsights.summarize(sessions, zone))
    }

    @Test
    fun `best time and place need a rival with enough sessions`() {
        val sessions = listOf(
            rated(20, 5, "Desk"), rated(21, 4, "Desk"),
            rated(8, 2, "Bed"), rated(9, 3, "Bed")
        )
        val summary = FocusInsights.summarize(sessions, zone)!!
        assertEquals("Evening", summary.bestTime?.label)
        assertEquals("Desk", summary.bestPlace?.label)
        assertEquals("You focus best in the evening, at Desk", FocusInsights.headline(summary))
        assertEquals(listOf("Morning", "Evening"), summary.byTime.map { it.label })
    }

    @Test
    fun `one time of day alone is not called best`() {
        val summary = FocusInsights.summarize(listOf(rated(20, 5), rated(20, 4), rated(21, 3)), zone)!!
        assertNull(summary.bestTime)
        assertNull(FocusInsights.headline(summary))
        assertEquals(4f, summary.average, 0.001f)
    }

    @Test
    fun `a tie has no winner`() {
        val sessions = listOf(rated(20, 4), rated(20, 4), rated(9, 4), rated(9, 4))
        assertNull(FocusInsights.summarize(sessions, zone)!!.bestTime)
    }
}
