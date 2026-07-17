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
    val rating: String // JSON object of String -> Float
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
