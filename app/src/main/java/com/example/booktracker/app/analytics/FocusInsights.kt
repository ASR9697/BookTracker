package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.ZoneId

/**
 * When and where the reader focuses best, from the focus they rate on the save
 * screen. Only rated sessions count, and a group needs a few of them before it
 * can be called "best", so one lucky evening doesn't become a pattern.
 */
object FocusInsights {

    /** Rated sessions needed before anything is said at all. */
    const val MIN_RATED = 3

    /** Sessions a time of day or a place needs before it can be compared. */
    const val MIN_GROUP = 2

    data class Group(val label: String, val average: Float, val sessions: Int)

    data class Summary(
        val rated: Int,
        val average: Float,
        /** Morning, afternoon, evening, night: only the ones with rated sessions, in that order. */
        val byTime: List<Group>,
        /** Places tagged on the save screen, best first. */
        val byPlace: List<Group>,
        /** Null unless at least two times of day have [MIN_GROUP] sessions. */
        val bestTime: Group?,
        val bestPlace: Group?
    )

    private val DAY_PARTS = listOf("Morning", "Afternoon", "Evening", "Night")

    fun summarize(sessions: List<Session>, zone: ZoneId = ZoneId.systemDefault()): Summary? {
        val rated = sessions.filter { it.focus in 1..5 && it.endTime > 0 }
        if (rated.size < MIN_RATED) return null

        val byTime = rated
            .groupBy { dayPart(hourOf(it, zone)) }
            .map { (part, list) -> group(part, list) }
            .sortedBy { DAY_PARTS.indexOf(it.label) }
        val byPlace = rated
            .filter { it.environmentTag.isNotBlank() }
            .groupBy { it.environmentTag }
            .map { (tag, list) -> group(tag, list) }
            .sortedWith(compareByDescending<Group> { it.average }.thenByDescending { it.sessions })

        return Summary(
            rated = rated.size,
            average = rated.map { it.focus }.average().toFloat(),
            byTime = byTime,
            byPlace = byPlace,
            bestTime = best(byTime),
            bestPlace = best(byPlace)
        )
    }

    /** "You focus best in the evening, at Desk", or null when there's no clear best yet. */
    fun headline(summary: Summary): String? {
        val time = summary.bestTime?.label?.lowercase()
        val place = summary.bestPlace?.label
        return when {
            time != null && place != null -> "You focus best in the $time, at $place"
            time != null -> "You focus best in the $time"
            place != null -> "You focus best at $place"
            else -> null
        }
    }

    private fun group(label: String, list: List<Session>) =
        Group(label, list.map { it.focus }.average().toFloat(), list.size)

    /** The top group, but only when there's something to beat and the win isn't a tie. */
    private fun best(groups: List<Group>): Group? {
        val eligible = groups.filter { it.sessions >= MIN_GROUP }
        if (eligible.size < 2) return null
        val sorted = eligible.sortedByDescending { it.average }
        return sorted[0].takeIf { it.average > sorted[1].average }
    }

    private fun hourOf(session: Session, fallback: ZoneId): Int {
        val zone = session.zoneId.takeIf { it.isNotBlank() }?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: fallback
        return Instant.ofEpochMilli(session.startTime).atZone(zone).hour
    }

    private fun dayPart(hour: Int): String = when (hour) {
        in 5..11 -> "Morning"
        in 12..16 -> "Afternoon"
        in 17..21 -> "Evening"
        else -> "Night"
    }
}
