package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Reading one book in print and audio at once. Pages stay the single measure
 * of progress (so streaks, goals and widgets need nothing new); the audio
 * position is the same fraction of the audiobook. Listening converts back to
 * pages through the playback speed: 20 minutes at 1.5x covers 30 minutes of
 * narration.
 */
object TandemEngine {

    val SPEEDS = listOf(1f, 1.25f, 1.5f, 1.75f, 2f)

    fun isTandem(book: Book): Boolean = book.audioMinutes > 0 && book.totalPages > 0

    /** Minutes into the narration (at 1x) that [page] corresponds to. */
    fun audioPositionFor(book: Book, page: Int = book.currentPage): Int {
        if (!isTandem(book)) return 0
        val fraction = page.coerceIn(0, book.totalPages).toDouble() / book.totalPages
        return (fraction * book.audioMinutes).roundToInt()
    }

    /** The page the narration has reached at [position] minutes (at 1x). */
    fun pageForAudio(book: Book, position: Int): Int {
        if (!isTandem(book)) return book.currentPage
        val fraction = position.coerceIn(0, book.audioMinutes).toDouble() / book.audioMinutes
        return (fraction * book.totalPages).roundToInt()
    }

    /** Narration minutes covered by [listened] real minutes at the reader's speed. */
    fun narrationMinutes(book: Book, listened: Int): Int = (listened * book.audioSpeed).roundToInt()

    /** Where listening for [listened] real minutes from the current page leaves the reader. */
    fun pageAfterListening(book: Book, listened: Int): Int =
        pageForAudio(book, audioPositionFor(book) + narrationMinutes(book, listened))

    /** Real minutes of listening left at the reader's speed. */
    fun listeningMinutesLeft(book: Book): Int {
        if (!isTandem(book)) return 0
        val left = (book.audioMinutes - audioPositionFor(book)).coerceAtLeast(0)
        return ceil(left / book.audioSpeed.coerceAtLeast(0.5f)).toInt()
    }

    /** "3h 05m" or "42m". */
    fun formatClock(minutes: Int): String {
        val m = minutes.coerceAtLeast(0)
        return if (m >= 60) "${m / 60}h ${"%02d".format(m % 60)}m" else "${m}m"
    }

    /** "1.25x", "2x". */
    fun formatSpeed(speed: Float): String =
        (if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0')) + "x"

    /** Parses "9:40", "9h 40m", "9h", "580" (minutes) into minutes; null when unreadable. */
    fun parseLength(text: String): Int? {
        val t = text.trim().lowercase()
        Regex("^(\\d+):(\\d{1,2})$").find(t)?.let { m ->
            return m.groupValues[1].toInt() * 60 + m.groupValues[2].toInt()
        }
        Regex("^(?:(\\d+)\\s*h)?\\s*(?:(\\d+)\\s*m(?:in)?)?$").find(t)?.let { m ->
            val h = m.groupValues[1].toIntOrNull()
            val min = m.groupValues[2].toIntOrNull()
            if (h != null || min != null) return (h ?: 0) * 60 + (min ?: 0)
        }
        return t.toIntOrNull()
    }
}
