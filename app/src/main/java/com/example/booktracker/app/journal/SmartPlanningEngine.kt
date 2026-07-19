package com.example.booktracker.app.journal

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Turns a measured reading velocity into concrete "what could I read in N minutes"
 * plans. Pure math over pages/hour — no scheduling, no location, no external data —
 * so it's free of the geofencing/shift assumptions the original blueprint stub made.
 */
object SmartPlanningEngine {

    /** Time windows offered by the planner UI, in minutes. */
    val PLAN_WINDOWS_MINUTES = listOf(15, 30, 45, 60)

    /** Units readable in [minutes] at [unitsPerHour]; null when velocity is unknown. */
    fun unitsInWindow(minutes: Int, unitsPerHour: Float?): Int? {
        if (unitsPerHour == null || unitsPerHour <= 0f) return null
        return (unitsPerHour * minutes / 60f).toInt()
    }

    /** Minutes needed to read [units] at [unitsPerHour]; null when velocity is unknown. */
    fun minutesForUnits(units: Int, unitsPerHour: Float?): Int? {
        if (unitsPerHour == null || unitsPerHour <= 0f || units <= 0) return null
        return (units / unitsPerHour * 60f).toInt()
    }
}

/**
 * Minimal inline-Markdown renderer for margin notes — supports **bold**, *italic*
 * (or _italic_), and `inline code`. Deliberately dependency-free: a full Markdown
 * library would be overkill for short reading notes, so this scans the common
 * inline spans and leaves everything else as plain text.
 */
object JournalingEngine {

    private val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
    private val italicStyle = SpanStyle(fontStyle = FontStyle.Italic)
    private val codeStyle = SpanStyle(fontFamily = FontFamily.Monospace)

    private data class Marker(val token: String, val style: SpanStyle)

    // Longest tokens first so "**" wins over "*".
    private val markers = listOf(
        Marker("**", boldStyle),
        Marker("`", codeStyle),
        Marker("*", italicStyle),
        Marker("_", italicStyle)
    )

    fun render(markdown: String): AnnotatedString = buildAnnotatedString {
        var i = 0
        while (i < markdown.length) {
            val marker = markers.firstOrNull { markdown.startsWith(it.token, i) }
            if (marker != null) {
                val contentStart = i + marker.token.length
                val close = markdown.indexOf(marker.token, contentStart)
                if (close != -1 && close > contentStart) {
                    withStyle(marker.style) { append(markdown.substring(contentStart, close)) }
                    i = close + marker.token.length
                    continue
                }
            }
            append(markdown[i])
            i++
        }
    }
}
