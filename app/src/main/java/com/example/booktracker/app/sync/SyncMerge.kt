package com.example.booktracker.app.sync

import com.example.booktracker.app.data.LibrarySnapshot

/**
 * Three-way merge of the library between this phone and the copy in Drive,
 * against the copy both last agreed on (the base). Per record:
 * - unchanged on one side: take the other side, including a delete;
 * - changed on both: a book keeps the later edit, a review the later grade,
 *   everything else this phone's version; an edit beats a delete.
 * With no base (first sync) both libraries are simply combined.
 * Sessions and notes whose book is gone are dropped, as are review states
 * whose highlight or word is gone.
 */
object SyncMerge {

    fun merge(base: LibrarySnapshot?, local: LibrarySnapshot, remote: LibrarySnapshot): LibrarySnapshot {
        val books = mergeById(base?.books, local.books, remote.books, { it.id }) { l, r ->
            if (r.lastUpdated > l.lastUpdated) r else l
        }
        val bookIds = books.map { it.id }.toSet()
        val sessions = mergeById(base?.sessions, local.sessions, remote.sessions, { it.id }) { l, _ -> l }
            .filter { it.bookId in bookIds }
        val notes = mergeById(base?.notes, local.notes, remote.notes, { it.id }) { l, _ -> l }
            .filter { it.bookId in bookIds }
        val words = mergeById(base?.words, local.words, remote.words, { it.id }) { l, _ -> l }
        val reviewable = notes.map { it.id }.toSet() + words.map { it.id }
        val reviews = mergeById(base?.reviews, local.reviews, remote.reviews, { it.itemId }) { l, r ->
            if (r.lastReviewedAt > l.lastReviewedAt) r else l
        }.filter { it.itemId in reviewable }
        return LibrarySnapshot(books, sessions, notes, words, reviews)
    }

    internal fun <T> mergeById(
        base: List<T>?,
        local: List<T>,
        remote: List<T>,
        id: (T) -> String,
        bothChanged: (local: T, remote: T) -> T
    ): List<T> {
        val b = base.orEmpty().associateBy(id)
        val l = local.associateBy(id)
        val r = remote.associateBy(id)
        // Local order first, then anything only Drive has, so lists stay stable.
        val ids = LinkedHashSet<String>().apply { addAll(l.keys); addAll(r.keys); addAll(b.keys) }
        return ids.mapNotNull { key ->
            val lv = l[key]
            val rv = r[key]
            val bv = b[key]
            when {
                lv == rv -> lv
                lv == bv -> rv
                rv == bv -> lv
                lv == null -> rv
                rv == null -> lv
                else -> bothChanged(lv, rv)
            }
        }
    }

    /** What has to be written to and removed from this phone to turn [from] into [to]. */
    data class Changes(
        val snapshot: LibrarySnapshot,
        val deletedBooks: Set<String>,
        val deletedSessions: Set<String>,
        val deletedNotes: Set<String>,
        val deletedWords: Set<String>,
        val deletedReviews: Set<String>
    ) {
        val isEmpty: Boolean
            get() = snapshot.books.isEmpty() && snapshot.sessions.isEmpty() && snapshot.notes.isEmpty() &&
                snapshot.words.isEmpty() && snapshot.reviews.isEmpty() && deletedBooks.isEmpty() &&
                deletedSessions.isEmpty() && deletedNotes.isEmpty() && deletedWords.isEmpty() && deletedReviews.isEmpty()
    }

    fun changes(from: LibrarySnapshot, to: LibrarySnapshot): Changes = Changes(
        snapshot = LibrarySnapshot(
            books = to.books - from.books.toSet(),
            sessions = to.sessions - from.sessions.toSet(),
            notes = to.notes - from.notes.toSet(),
            words = to.words - from.words.toSet(),
            reviews = to.reviews - from.reviews.toSet()
        ),
        deletedBooks = from.books.map { it.id }.toSet() - to.books.map { it.id }.toSet(),
        deletedSessions = from.sessions.map { it.id }.toSet() - to.sessions.map { it.id }.toSet(),
        deletedNotes = from.notes.map { it.id }.toSet() - to.notes.map { it.id }.toSet(),
        deletedWords = from.words.map { it.id }.toSet() - to.words.map { it.id }.toSet(),
        deletedReviews = from.reviews.map { it.itemId }.toSet() - to.reviews.map { it.itemId }.toSet()
    )
}
