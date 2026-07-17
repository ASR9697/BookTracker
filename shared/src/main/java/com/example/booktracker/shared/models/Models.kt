package com.example.booktracker.shared.models

enum class BookStatus {
    BACKLOG, SHORTLIST, UP_NEXT, READING, FINISHED, DNF
}

data class DnfData(
    val abandonedPercentage: Float = 0f,
    val reason: String = "" // e.g., prose, pacing, characters
)

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
    val rating: Map<String, Float> = emptyMap() // Pacing, Focus, Vibe
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
