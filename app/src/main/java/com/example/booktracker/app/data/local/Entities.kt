package com.example.booktracker.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val authors: String, // JSON array of strings
    val coverUrl: String,
    val format: String,
    val totalUnits: Int,
    val currentUnit: Int,
    val status: String,
    val dnfPercentage: Float?,
    val dnfReason: String?,
    val lastUpdated: Long,
    val rating: String, // JSON object of String -> Float
    val description: String,
    val genres: String, // JSON array of strings
    val publishedDate: String
)

@Entity(tableName = "sessions", indices = [Index("bookId")])
data class SessionEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val startTime: Long,
    val endTime: Long,
    val startUnit: Int,
    val endUnit: Int,
    val unitsRead: Int,
    val deviceSource: String,
    val environmentTag: String,
    val isInterrupted: Boolean
)

@Entity(tableName = "margin_notes", indices = [Index("bookId")])
data class MarginNoteEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val timestamp: Long,
    val pageOrUnit: Int,
    val markdownContent: String,
    val isVoiceDictated: Boolean
)
