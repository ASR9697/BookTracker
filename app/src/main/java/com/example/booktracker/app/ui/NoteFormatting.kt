package com.example.booktracker.app.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Edits to the note text for the formatting toolbar, dictation and scanning.
 * Markers match what [com.example.booktracker.app.journal.JournalingEngine]
 * renders: **bold**, _italic_ and plain "• " bullets.
 */
object NoteFormatting {

    const val BOLD = "**"
    const val ITALIC = "_"
    const val BULLET = "• "

    /** Wraps the selection in [token], or removes it when the selection is already wrapped. */
    fun toggleWrap(value: TextFieldValue, token: String): TextFieldValue {
        val text = value.text
        val start = value.selection.min
        val end = value.selection.max
        val n = token.length
        if (start == end) {
            // Nothing selected: drop an empty pair at the cursor and sit inside it.
            return TextFieldValue(text.substring(0, start) + token + token + text.substring(end), TextRange(start + n))
        }
        val wrapped = start >= n && end + n <= text.length &&
            text.substring(start - n, start) == token && text.substring(end, end + n) == token
        return if (wrapped) {
            TextFieldValue(
                text.substring(0, start - n) + text.substring(start, end) + text.substring(end + n),
                TextRange(start - n, end - n)
            )
        } else {
            TextFieldValue(
                text.substring(0, start) + token + text.substring(start, end) + token + text.substring(end),
                TextRange(start + n, end + n)
            )
        }
    }

    /** Adds "• " to every line the selection touches, or strips it if they all have one. */
    fun toggleBullets(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val lineStart = text.lastIndexOf('\n', value.selection.min - 1) + 1
        val lineEnd = text.indexOf('\n', value.selection.max).let { if (it == -1) text.length else it }
        val lines = text.substring(lineStart, lineEnd).split('\n')
        val allBulleted = lines.all { it.startsWith(BULLET) }
        val changed = lines.joinToString("\n") { if (allBulleted) it.removePrefix(BULLET) else if (it.startsWith(BULLET)) it else BULLET + it }
        val result = text.substring(0, lineStart) + changed + text.substring(lineEnd)
        return TextFieldValue(result, TextRange(lineStart + changed.length))
    }

    /** Puts [inserted] at the cursor (replacing any selection), with a space where words would run together. */
    fun insertAtCursor(value: TextFieldValue, inserted: String): TextFieldValue {
        val text = value.text
        val start = value.selection.min
        val before = text.substring(0, start)
        val needsSpace = before.isNotEmpty() && !before.last().isWhitespace()
        val piece = (if (needsSpace) " " else "") + inserted.trim()
        val result = before + piece + text.substring(value.selection.max)
        return TextFieldValue(result, TextRange(before.length + piece.length))
    }

    /** Adds [block] as a new paragraph at the end, which is where scanned text belongs. */
    fun appendParagraph(value: TextFieldValue, block: String): TextFieldValue {
        val base = value.text.trimEnd()
        val result = if (base.isEmpty()) block.trim() else base + "\n\n" + block.trim()
        return TextFieldValue(result, TextRange(result.length))
    }

    /** OCR returns one block per paragraph with a line break at every physical line; join them. */
    fun unwrapLines(block: String): String =
        block.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")
}
