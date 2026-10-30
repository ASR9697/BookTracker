package com.example.booktracker.shared

/**
 * Contract for the phone <-> watch Wearable Data Layer.
 *
 * Phone publishes the books being read, with today's goal, at [ACTIVE_BOOKS_PATH],
 * and the phone timer at [TIMER_STATE_PATH]. Both are written only when something
 * changes, so an unchanged re-publish never wakes the watch.
 * Watch publishes page position per book at "[PROGRESS_PATH_PREFIX]/{bookId}".
 * Watch publishes a dictated margin note at "[NOTE_PATH_PREFIX]/{noteId}" and a
 * session it timed itself at "[SESSION_PATH_PREFIX]/{sessionId}"; both ids are
 * client-generated on the watch so re-delivery stays idempotent.
 * Watch asks the phone timer to pause or resume at "[TIMER_CONTROL_PATH]/{time}".
 * Page position resolves conflicts by Last-Write-Wins on [KEY_UPDATED_AT].
 */
object Constants {
    const val ACTIVE_BOOKS_PATH = "/active_books"
    const val PROGRESS_PATH_PREFIX = "/progress"
    const val NOTE_PATH_PREFIX = "/note"
    const val SESSION_PATH_PREFIX = "/session"
    const val TIMER_STATE_PATH = "/timer_state"
    const val TIMER_CONTROL_PATH = "/timer_control"

    const val KEY_BOOKS_LIST = "books_list"
    const val KEY_BOOK_ID = "book_id"
    const val KEY_TITLE = "title"
    const val KEY_CURRENT_UNIT = "current_unit"
    const val KEY_TOTAL_UNITS = "total_units"
    const val KEY_UPDATED_AT = "updated_at"
    const val KEY_DAILY_GOAL = "daily_goal"
    const val KEY_PAGES_TODAY = "pages_today"
    /** When the phone's reading day rolls over (epoch ms); [KEY_PAGES_TODAY] counts as 0 after it. */
    const val KEY_DAY_ENDS_AT = "day_ends_at"
    /** Reading days in a row, as the phone's streak counts them. */
    const val KEY_STREAK = "streak"
    /**
     * A book's finish-by plan, present only when it has a deadline: pages a day from
     * today on, pages read in it today, the deadline (epoch day) and how it's going
     * ("ON_TRACK", "AHEAD", "BEHIND", "OVERDUE", "DONE", "NO_PACE").
     */
    const val KEY_PLAN_PER_DAY = "plan_per_day"
    const val KEY_PLAN_READ_TODAY = "plan_read_today"
    const val KEY_PLAN_TARGET_DAY = "plan_target_day"
    const val KEY_PLAN_STATUS = "plan_status"
    /** The reader's palette on the phone (ARGB), so the watch wears the same colours. */
    const val KEY_ACCENT = "accent"
    const val KEY_ACCENT_SECONDARY = "accent_secondary"

    const val KEY_NOTE_ID = "note_id"
    const val KEY_NOTE_TEXT = "note_text"
    const val KEY_PAGE_OR_UNIT = "page_or_unit"

    const val KEY_SESSION_ID = "session_id"
    const val KEY_START_TIME = "start_time"
    const val KEY_END_TIME = "end_time"
    const val KEY_DURATION_SECONDS = "duration_seconds"
    const val KEY_START_UNIT = "start_unit"
    const val KEY_END_UNIT = "end_unit"
    const val KEY_ZONE_ID = "zone_id"
    /** How focused the session felt, 1 to 5; 0 when not rated. */
    const val KEY_FOCUS = "focus"

    const val KEY_TIMER_RUNNING = "timer_running"
    /** Seconds on the clock when published: time left for a countdown, time read for a stopwatch. */
    const val KEY_TIMER_SECONDS = "timer_seconds"
    /** While running, epoch ms the watch counts from: a countdown's end, or a stopwatch's start. */
    const val KEY_TIMER_ANCHOR = "timer_anchor"
    const val KEY_TIMER_PHASE = "timer_phase_name" // "WORK", "BREAK"
    const val KEY_TIMER_MODE = "timer_mode" // "COUNTDOWN", "STOPWATCH"
    const val KEY_TIMER_BOOK_ID = "timer_book_id"
    const val KEY_TIMER_CONTROL_ACTION = "timer_action"

    const val TIMER_ACTION_PAUSE = "PAUSE"
    const val TIMER_ACTION_RESUME = "RESUME"
}
