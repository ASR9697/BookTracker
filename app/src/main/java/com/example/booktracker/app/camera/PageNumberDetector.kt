package com.example.booktracker.app.camera

import kotlin.math.abs

/**
 * Finds the page number in what the camera reads off a page. Page numbers sit
 * alone near the top or bottom edge, or at one end of a short running header
 * ("124   THE NAME OF THE WIND"), and they are close to where the reader left off.
 * Every candidate is scored on those three hints; the reader still confirms.
 */
object PageNumberDetector {

    /** One recognised line; positions are fractions of the frame (0 top/left, 1 bottom/right). */
    data class Line(val text: String, val centerY: Float, val centerX: Float = 0.5f)

    data class Candidate(val page: Int, val score: Double)

    /** How far past the bookmark a session plausibly ends. */
    private const val PLAUSIBLE_JUMP = 80

    private val standalone = Regex("""^(?:p(?:age|g)?\.?\s*)?[\-–—·•(\[]?\s*(\d{1,4})\s*[\-–—·•)\]]?$""", RegexOption.IGNORE_CASE)
    private val number = Regex("""^\d{1,4}$""")

    fun candidates(lines: List<Line>, currentPage: Int, totalPages: Int): List<Candidate> {
        val found = mutableMapOf<Int, Double>()
        for (line in lines) {
            val text = line.text.trim()
            val (page, base) = parse(text) ?: continue
            if (page <= 0) continue
            if (totalPages > 0 && page > totalPages) continue
            if (totalPages <= 0 && page > 3000) continue

            val edge = (abs(line.centerY - 0.5f) * 2f).toDouble() // 0 in the middle, 1 at an edge
            val side = (abs(line.centerX - 0.5f) * 2f).toDouble() // corners are common too
            val delta = page - currentPage
            val nearness = when {
                delta in 0..PLAUSIBLE_JUMP -> 1.5 * (1.0 - delta.toDouble() / PLAUSIBLE_JUMP) + 0.5
                delta in -10..-1 -> 0.4 // flipped back a little
                else -> 0.0
            }
            val score = base + edge * 1.2 + side * 0.3 + nearness
            found[page] = maxOf(found[page] ?: 0.0, score)
        }
        return found.map { Candidate(it.key, it.value) }.sortedByDescending { it.score }
    }

    /** The page and how page-number-like the line looks, or null when it holds none. */
    private fun parse(text: String): Pair<Int, Double>? {
        standalone.matchEntire(text)?.let { return it.groupValues[1].toInt() to 1.0 }
        // A running header or footer: a number at either end of a few words.
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.size in 2..8) {
            val first = words.first()
            val last = words.last()
            if (number.matches(first) && words.drop(1).none { number.matches(it) }) return first.toInt() to 0.6
            if (number.matches(last) && words.dropLast(1).none { number.matches(it) }) return last.toInt() to 0.6
        }
        return null
    }
}
