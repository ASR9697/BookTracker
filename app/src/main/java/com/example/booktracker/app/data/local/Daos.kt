package com.example.booktracker.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books")
    suspend fun getAll(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: String): BookEntity?

    @Query("SELECT * FROM books WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<BookEntity>

    @Query(
        "SELECT books.* FROM books JOIN books_fts ON books.rowid = books_fts.rowid " +
            "WHERE books_fts MATCH :query"
    )
    suspend fun search(query: String): List<BookEntity>

    @Upsert
    suspend fun upsert(book: BookEntity)

    @Upsert
    suspend fun upsert(books: List<BookEntity>)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: String)
}

// endTime = 0 marks a session still in progress (open); it survives process death.
@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity)

    @Upsert
    suspend fun upsert(session: SessionEntity)

    @Upsert
    suspend fun upsert(sessions: List<SessionEntity>)

    @Query("SELECT * FROM sessions WHERE bookId = :bookId ORDER BY startTime DESC")
    fun observeForBook(bookId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE endTime != 0 ORDER BY startTime DESC")
    fun observeCompleted(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE endTime = 0 LIMIT 1")
    fun observeOpenSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE endTime = 0 LIMIT 1")
    suspend fun getOpenSession(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE endTime = 0 AND bookId = :bookId LIMIT 1")
    suspend fun getOpenSessionForBook(bookId: String): SessionEntity?

    @Query("SELECT * FROM sessions")
    suspend fun getAll(): List<SessionEntity>

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM sessions WHERE bookId = :bookId")
    suspend fun deleteByBookId(bookId: String)
}

@Dao
interface MarginNoteDao {
    @Query("SELECT * FROM margin_notes WHERE bookId = :bookId ORDER BY timestamp DESC")
    fun observeForBook(bookId: String): Flow<List<MarginNoteEntity>>

    @Query("SELECT * FROM margin_notes ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<MarginNoteEntity>>

    @Query("SELECT COUNT(*) FROM margin_notes")
    fun observeTotalNotesCount(): Flow<Int>


    @Query("SELECT * FROM margin_notes")
    suspend fun getAll(): List<MarginNoteEntity>

    @Query(
        "SELECT margin_notes.* FROM margin_notes " +
            "JOIN notes_fts ON margin_notes.rowid = notes_fts.rowid " +
            "WHERE notes_fts MATCH :query ORDER BY margin_notes.timestamp DESC"
    )
    suspend fun search(query: String): List<MarginNoteEntity>

    @Upsert
    suspend fun upsert(note: MarginNoteEntity)

    @Upsert
    suspend fun upsert(notes: List<MarginNoteEntity>)

    @Query("DELETE FROM margin_notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE margin_notes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM margin_notes WHERE bookId = :bookId")
    suspend fun deleteByBookId(bookId: String)
}
