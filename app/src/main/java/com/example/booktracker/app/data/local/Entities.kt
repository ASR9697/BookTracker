package com.example.booktracker.app.data.local

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val isbn: String?,
    val coverUrl: String,
    val totalPages: Int,
    val currentPage: Int,
    val language: String,
    val creators: String, // JSON array of Creator objects
    val publication: String?, // JSON object
    val format: String,
    val progressUnit: String,
    val description: String,
    val seriesInfo: String?, // JSON object
    val classification: String, // JSON object
    val status: String,
    val rating: String,
    val dnfData: String?,
    val purchaseLog: String, // JSON array
    val loanRecord: String, // JSON array
    val dateAdded: Long,
    val lastUpdated: Long,
    val isFavorite: Boolean,
    // The SQL default must be declared here too, or Room's post-migration schema
    // validation sees a default the expected TableInfo doesn't have and throws.
    @androidx.room.ColumnInfo(defaultValue = "0")
    val readCount: Int = 0,
    val plannedDate: Long? = null,
    val review: String? = null
)

@Entity(
    tableName = "sessions",
    indices = [Index("bookId")],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ]
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Int,
    val startPage: Int,
    val endPage: Int,
    val pagesRead: Int,
    val environmentTag: String
)

@Entity(
    tableName = "margin_notes",
    indices = [Index("bookId")],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ]
)
data class MarginNoteEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val pageNumber: Int,
    val content: String,
    val type: String,
    val isFavorite: Boolean,
    val timestamp: Long
)

// External-content FTS mirrors
@Fts4(contentEntity = BookEntity::class)
@Entity(tableName = "books_fts")
data class BookFtsEntity(
    val title: String,
    val creators: String,
    val description: String,
    val classification: String
)

@Fts4(contentEntity = MarginNoteEntity::class)
@Entity(tableName = "notes_fts")
data class NoteFtsEntity(
    val content: String
)
