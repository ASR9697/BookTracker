package com.example.booktracker.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastUpdated DESC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: String): BookEntity?

    @Upsert
    suspend fun upsert(book: BookEntity)

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
}
