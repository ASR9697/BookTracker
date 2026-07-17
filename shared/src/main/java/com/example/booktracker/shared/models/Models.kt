package com.example.booktracker.shared.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class User(
    val uid: String = "",
    val displayName: String = "",
    val dailyGoalPages: Int = 20,
    val currentStreak: Int = 0
)

enum class BookStatus {
    BACKLOG, SHORTLIST, UP_NEXT, READING, FINISHED, DNF
}

data class DnfData(
    val abandonedPercentage: Float = 0f,
    val reason: String = "" // e.g., prose, pacing, characters
)

data class Book(
    val id: String = "",
    val title: String = "",
    val authors: List<String> = emptyList(),
    val coverUrl: String = "",
    val format: String = "PAGES", // PAGES, VOLUMES, HOURS
    val totalUnits: Int = 0, // replaces totalPages for multi-format
    val currentUnit: Int = 0, // replaces currentPage
    val status: String = BookStatus.BACKLOG.name,
    val dnfData: DnfData? = null,
    @ServerTimestamp val lastUpdated: Timestamp? = null,
    val rating: Map<String, Float> = emptyMap() // Pacing, Focus, Vibe
)

data class Session(
    val id: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val startUnit: Int = 0,
    val endUnit: Int = 0,
    val unitsRead: Int = 0,
    val deviceSource: String = "", // "phone" or "watch"
    val environmentTag: String = "", // e.g., "morning coffee", "late-night whiskey"
    val isInterrupted: Boolean = false // Life Interruption Pause
)

data class MarginNote(
    val id: String = "",
    val timestamp: Timestamp? = null,
    val pageOrUnit: Int = 0,
    val markdownContent: String = "",
    val isVoiceDictated: Boolean = false
)
