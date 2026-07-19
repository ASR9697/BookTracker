package com.example.booktracker.shared.models

enum class BookStatus(val label: String) {
    BACKLOG("Backlog"),
    SHORTLIST("Shortlist"),
    UP_NEXT("Up Next"),
    READING("Reading"),
    PAUSED("Paused"),
    FINISHED("Finished"),
    DNF("DNF")
}

data class DnfData(
    val abandonedPercentage: Float = 0f,
    val reason: String = "" // e.g., prose, pacing, characters
)

/** The three qualitative axes a finished book can be rated on (0.0–5.0). */
object RatingAxis {
    const val PACING = "Pacing"
    const val FOCUS = "Focus"
    const val VIBE = "Vibe"
    val ALL = listOf(PACING, FOCUS, VIBE)
}

/** Preset reasons offered when marking a book did-not-finish. */
object DnfReasons {
    val ALL = listOf("Prose", "Pacing", "Characters", "Plot", "Lost interest", "Other")
}

// All timestamps are epoch milliseconds. They double as the Last-Write-Wins
// key for sync (Data Layer now, Firestore in Phase B), so every mutation
// must stamp lastUpdated.
data class Book(
    val id: String = "",
    val title: String = "",
    val authors: List<String> = emptyList(),
    val coverUrl: String = "",
    val format: String = "PAGES", // PAGES, VOLUMES, CHAPTERS, HOURS
    val totalUnits: Int = 0,
    val currentUnit: Int = 0,
    val status: String = BookStatus.BACKLOG.name,
    val dnfData: DnfData? = null,
    val lastUpdated: Long = 0L,
    val rating: Map<String, Float> = emptyMap(), // Pacing, Focus, Vibe
    val description: String = "",
    val genres: List<String> = emptyList(),
    val publishedDate: String = "",
    val isFavorite: Boolean = false
)

data class Session(
    val id: String = "",
    val bookId: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val startUnit: Int = 0,
    val endUnit: Int = 0,
    val unitsRead: Int = 0,
    val deviceSource: String = "", // "phone" or "watch"
    val environmentTag: String = "", // e.g., "morning coffee", "late-night whiskey"
    val isInterrupted: Boolean = false // Life Interruption Pause
)

data class MarginNote(
    val id: String = "",
    val bookId: String = "",
    val timestamp: Long = 0L,
    val pageOrUnit: Int = 0,
    val markdownContent: String = "",
    val isVoiceDictated: Boolean = false
)
