package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.Session

/**
 * On a re-read, the notes you wrote last time come back as you reach their pages:
 * "you underlined this here in 2025". A note belongs to an earlier read when it
 * was written before the current read began.
 */
object RereadMemories {

    /** A session starting this many pages before the previous one ended is a restart. */
    private const val RESTART_DROP_PAGES = 20

    /**
     * When the current read began: the stored start, else (for re-reads started
     * before it was stored) the last time the bookmark jumped back toward the start.
     * Null on a first read.
     */
    fun currentReadStart(book: Book, sessions: List<Session>): Long? {
        if (book.readCount == 0) return null
        book.readStartedAt?.let { return it }
        val ordered = sessions.filter { it.bookId == book.id && it.startTime > 0 }.sortedBy { it.startTime }
        for (i in ordered.lastIndex downTo 1) {
            val previousEnd = ordered[i - 1].endPage
            val start = ordered[i].startPage
            if (previousEnd - start >= RESTART_DROP_PAGES && start < previousEnd / 2) return ordered[i].startTime
        }
        return null
    }

    /** Notes from earlier reads on pages after [fromPage], up to and including [toPage]. */
    fun reached(notes: List<MarginNote>, readStart: Long, fromPage: Int, toPage: Int): List<MarginNote> =
        if (toPage <= fromPage) emptyList()
        else notes.filter { it.timestamp < readStart && it.pageNumber in (fromPage + 1)..toPage }
            .sortedBy { it.pageNumber }

    /** Earlier notes still ahead of [currentPage], nearest first. */
    fun ahead(notes: List<MarginNote>, readStart: Long, currentPage: Int): List<MarginNote> =
        notes.filter { it.timestamp < readStart && it.pageNumber > currentPage }.sortedBy { it.pageNumber }

    /** Earlier notes already passed on this read, most recent page first. */
    fun passed(notes: List<MarginNote>, readStart: Long, currentPage: Int): List<MarginNote> =
        notes.filter { it.timestamp < readStart && it.pageNumber in 1..currentPage }.sortedByDescending { it.pageNumber }
}
