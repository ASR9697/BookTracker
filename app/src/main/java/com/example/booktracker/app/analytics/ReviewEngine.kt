package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.NoteType
import com.example.booktracker.shared.models.ReviewKind
import com.example.booktracker.shared.models.ReviewState
import com.example.booktracker.shared.models.VocabWord

/**
 * Spaced review for saved highlights and vocabulary words, kept deliberately
 * simple: every good recall moves an item to a longer gap, forgetting it brings
 * it back tomorrow. Only a few new items join each day, so a big library of
 * quotes never turns into a wall of cards.
 */
object ReviewEngine {

    /** Days until the next review after each successive good recall. */
    val INTERVALS = listOf(1, 3, 7, 16, 35, 90, 180)

    /** New items introduced per reading day, fewer once some reviewing is done. */
    const val NEW_PER_DAY = 3

    /** Upper bound on one day's review, so a missed week doesn't pile up into a chore. */
    const val MAX_PER_DAY = 15

    enum class Grade(val label: String) {
        AGAIN("Forgot"),
        HARD("Hard"),
        GOOD("Good"),
        EASY("Easy")
    }

    /** Something that can be reviewed; [preferred] items (favourites) are introduced first. */
    data class Item(
        val id: String,
        val kind: ReviewKind,
        val addedAt: Long,
        val preferred: Boolean = false
    )

    /** Highlights worth remembering: passages from the book and anything starred. */
    fun noteItems(notes: List<MarginNote>): List<Item> =
        notes.filter { (it.type == NoteType.BOOK_CONTENT || it.isFavorite) && it.content.isNotBlank() }
            .map { Item(it.id, ReviewKind.NOTE, it.timestamp, preferred = it.isFavorite) }

    fun wordItems(words: List<VocabWord>): List<Item> =
        words.filter { it.word.isNotBlank() }.map { Item(it.id, ReviewKind.WORD, it.addedAt) }

    /**
     * Today's review: items already due (longest overdue first), then a few new
     * ones. [reviewedToday] is how many reviews were already done today; it uses
     * up the new-item allowance so reopening the review never adds a fresh batch.
     */
    fun dailyQueue(
        items: List<Item>,
        states: Map<String, ReviewState>,
        today: Long,
        reviewedToday: Int = reviewedOn(states.values, today),
        newPerDay: Int = NEW_PER_DAY,
        maxPerDay: Int = MAX_PER_DAY
    ): List<Item> {
        val due = items
            .mapNotNull { item -> states[item.id]?.takeIf { it.dueDay <= today }?.let { item to it.dueDay } }
            .sortedBy { it.second }
            .map { it.first }
        val newAllowance = (newPerDay - reviewedToday).coerceAtLeast(0)
        val fresh = items
            .filter { it.id !in states }
            .sortedWith(compareByDescending<Item> { it.preferred }.thenBy { it.addedAt })
            .take(newAllowance)
        return (due + fresh).take((maxPerDay - reviewedToday).coerceAtLeast(0))
    }

    /** How many items were reviewed on [day] (epoch day), judged by their last review. */
    fun reviewedOn(states: Collection<ReviewState>, day: Long, dayOf: (Long) -> Long = ::defaultDay): Int =
        states.count { it.lastReviewedAt > 0 && dayOf(it.lastReviewedAt) == day }

    /**
     * The next state after grading. Forgetting resets the climb and comes back
     * tomorrow; Hard repeats the current gap; Good moves up one gap; Easy two.
     */
    fun schedule(
        previous: ReviewState?,
        itemId: String,
        kind: ReviewKind,
        grade: Grade,
        today: Long,
        now: Long
    ): ReviewState {
        val reps = previous?.reps ?: 0
        val lapses = previous?.lapses ?: 0
        val newReps = when (grade) {
            Grade.AGAIN -> 0
            Grade.HARD -> reps.coerceAtLeast(1)
            Grade.GOOD -> reps + 1
            Grade.EASY -> reps + 2
        }
        val interval = if (grade == Grade.AGAIN) 1 else INTERVALS[(newReps - 1).coerceIn(0, INTERVALS.lastIndex)]
        return ReviewState(
            itemId = itemId,
            kind = kind,
            dueDay = today + interval,
            intervalDays = interval,
            reps = newReps,
            // Only forgetting something already learned counts as a lapse.
            lapses = if (grade == Grade.AGAIN && reps > 0) lapses + 1 else lapses,
            lastReviewedAt = now
        )
    }

    private fun defaultDay(millis: Long): Long =
        StreakEngine.readingDate(millis, java.time.ZoneId.systemDefault()).toEpochDay()
}
