package com.example.booktracker.app.analytics

import com.example.booktracker.app.analytics.ReviewEngine.Grade
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.NoteType
import com.example.booktracker.shared.models.ReviewKind
import com.example.booktracker.shared.models.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewEngineTest {

    private val today = 20_000L
    private val now = 1_000L

    private fun item(id: String, addedAt: Long = 0, preferred: Boolean = false) =
        ReviewEngine.Item(id, ReviewKind.NOTE, addedAt, preferred)

    @Test
    fun `good recalls climb the gaps and easy skips one`() {
        val first = ReviewEngine.schedule(null, "n", ReviewKind.NOTE, Grade.GOOD, today, now)
        assertEquals(1, first.intervalDays)
        assertEquals(today + 1, first.dueDay)
        val second = ReviewEngine.schedule(first, "n", ReviewKind.NOTE, Grade.GOOD, today + 1, now)
        assertEquals(3, second.intervalDays)
        val easy = ReviewEngine.schedule(second, "n", ReviewKind.NOTE, Grade.EASY, today + 4, now)
        assertEquals(16, easy.intervalDays)
    }

    @Test
    fun `forgetting a learned item resets it and counts a lapse`() {
        val learned = ReviewState("n", ReviewKind.NOTE, dueDay = today, intervalDays = 7, reps = 3)
        val again = ReviewEngine.schedule(learned, "n", ReviewKind.NOTE, Grade.AGAIN, today, now)
        assertEquals(0, again.reps)
        assertEquals(1, again.lapses)
        assertEquals(today + 1, again.dueDay)
        // A brand-new item forgotten on first sight was never learned, so no lapse.
        assertEquals(0, ReviewEngine.schedule(null, "m", ReviewKind.NOTE, Grade.AGAIN, today, now).lapses)
    }

    @Test
    fun `hard repeats the current gap`() {
        val learned = ReviewState("n", ReviewKind.NOTE, dueDay = today, intervalDays = 7, reps = 3)
        assertEquals(7, ReviewEngine.schedule(learned, "n", ReviewKind.NOTE, Grade.HARD, today, now).intervalDays)
    }

    @Test
    fun `queue puts overdue first then a few new items, favourites before older ones`() {
        val items = listOf(item("due-late"), item("due-early"), item("future"), item("new-old", 1), item("new-fav", 9, preferred = true), item("new-young", 5))
        val states = mapOf(
            "due-late" to ReviewState("due-late", ReviewKind.NOTE, dueDay = today),
            "due-early" to ReviewState("due-early", ReviewKind.NOTE, dueDay = today - 3),
            "future" to ReviewState("future", ReviewKind.NOTE, dueDay = today + 2)
        )
        val queue = ReviewEngine.dailyQueue(items, states, today, reviewedToday = 0, newPerDay = 2)
        assertEquals(listOf("due-early", "due-late", "new-fav", "new-old"), queue.map { it.id })
    }

    @Test
    fun `reviews already done today use up the new allowance and the daily cap`() {
        val items = (1..10).map { item("n$it", it.toLong()) }
        assertEquals(1, ReviewEngine.dailyQueue(items, emptyMap(), today, reviewedToday = 2, newPerDay = 3).size)
        assertEquals(0, ReviewEngine.dailyQueue(items, emptyMap(), today, reviewedToday = 3, newPerDay = 3).size)
        val allDue = items.associate { it.id to ReviewState(it.id, ReviewKind.NOTE, dueDay = today) }
        assertEquals(4, ReviewEngine.dailyQueue(items, allDue, today, reviewedToday = 6, maxPerDay = 10).size)
    }

    @Test
    fun `only passages and starred notes are reviewed`() {
        val notes = listOf(
            MarginNote(id = "quote", content = "To be", type = NoteType.BOOK_CONTENT),
            MarginNote(id = "thought", content = "I liked it", type = NoteType.PERSONAL_THOUGHT),
            MarginNote(id = "starred", content = "Remember", type = NoteType.RANDOM, isFavorite = true),
            MarginNote(id = "blank", content = " ", type = NoteType.BOOK_CONTENT)
        )
        assertEquals(listOf("quote", "starred"), ReviewEngine.noteItems(notes).map { it.id })
    }

    @Test
    fun `reviewed today counts by the day of the last review`() {
        val states = listOf(
            ReviewState("a", ReviewKind.NOTE, dueDay = 0, lastReviewedAt = 10),
            ReviewState("b", ReviewKind.NOTE, dueDay = 0, lastReviewedAt = 20),
            ReviewState("c", ReviewKind.NOTE, dueDay = 0, lastReviewedAt = 0)
        )
        assertEquals(1, ReviewEngine.reviewedOn(states, today) { if (it == 10L) today else today - 1 })
    }
}
