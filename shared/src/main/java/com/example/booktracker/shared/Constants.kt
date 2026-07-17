package com.example.booktracker.shared

/**
 * Contract for the phone <-> watch Wearable Data Layer.
 *
 * Phone publishes the currently-reading book at [ACTIVE_BOOK_PATH].
 * Watch publishes page position per book at "[PROGRESS_PATH_PREFIX]/{bookId}".
 * Both sides resolve conflicts by Last-Write-Wins on [KEY_UPDATED_AT].
 */
object Constants {
    const val ACTIVE_BOOK_PATH = "/active_book"
    const val PROGRESS_PATH_PREFIX = "/progress"

    const val KEY_BOOK_ID = "book_id"
    const val KEY_TITLE = "title"
    const val KEY_CURRENT_UNIT = "current_unit"
    const val KEY_TOTAL_UNITS = "total_units"
    const val KEY_UPDATED_AT = "updated_at"
}
