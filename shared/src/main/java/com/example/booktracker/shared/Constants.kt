package com.example.booktracker.shared

/**
 * Contract for the phone <-> watch Wearable Data Layer.
 *
 * Phone publishes the currently-reading book at [ACTIVE_BOOK_PATH].
 * Watch publishes page position per book at "[PROGRESS_PATH_PREFIX]/{bookId}".
 * Watch publishes a dictated margin note at "[NOTE_PATH_PREFIX]/{noteId}"; the
 * note id is client-generated on the watch so re-delivery stays idempotent.
 * Both sides resolve conflicts by Last-Write-Wins on [KEY_UPDATED_AT].
 */
object Constants {
    const val ACTIVE_BOOK_PATH = "/active_book"
    const val PROGRESS_PATH_PREFIX = "/progress"
    const val NOTE_PATH_PREFIX = "/note"

    const val KEY_BOOK_ID = "book_id"
    const val KEY_TITLE = "title"
    const val KEY_CURRENT_UNIT = "current_unit"
    const val KEY_TOTAL_UNITS = "total_units"
    const val KEY_UPDATED_AT = "updated_at"
    const val KEY_DAILY_GOAL = "daily_goal"
    const val KEY_PAGES_TODAY = "pages_today"

    const val KEY_NOTE_ID = "note_id"
    const val KEY_NOTE_TEXT = "note_text"
    const val KEY_PAGE_OR_UNIT = "page_or_unit"

    const val ACTIVE_BOOKS_PATH = "/active_books"
    const val TIMER_STATE_PATH = "/timer_state"
    const val TIMER_CONTROL_PATH = "/timer_control"

    const val KEY_BOOKS_LIST = "books_list"
    
    const val KEY_TIMER_RUNNING = "timer_running"
    const val KEY_TIMER_TIME_LEFT = "timer_time_left"
    const val KEY_TIMER_PHASE = "timer_phase"
    const val KEY_TIMER_BOOK_ID = "timer_book_id"
    const val KEY_TIMER_CONTROL_ACTION = "timer_action" // "PAUSE", "RESUME"
}
