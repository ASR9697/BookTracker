package com.example.booktracker.app.data

import com.example.booktracker.app.data.local.BookDao
import com.example.booktracker.app.data.local.toEntity
import com.example.booktracker.app.data.local.toModel
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single data access boundary for the UI and sync layers. Phase B adds cloud
 * sync behind this same interface, so nothing above it may touch storage
 * directly. Every local mutation stamps lastUpdated (the LWW conflict key).
 */
interface BookRepository {
    fun observeBooks(): Flow<List<Book>>
    suspend fun addBook(title: String, authors: List<String>, totalUnits: Int): Book
    suspend fun updateStatus(id: String, status: BookStatus)
    suspend fun addProgress(id: String, delta: Int)
    suspend fun applyRemoteProgress(id: String, currentUnit: Int, updatedAt: Long)
    suspend fun deleteBook(id: String)
}

class RoomBookRepository(private val bookDao: BookDao) : BookRepository {

    override fun observeBooks(): Flow<List<Book>> =
        bookDao.observeAll().map { entities -> entities.map { it.toModel() } }

    override suspend fun addBook(title: String, authors: List<String>, totalUnits: Int): Book {
        val book = Book(
            id = UUID.randomUUID().toString(),
            title = title,
            authors = authors,
            totalUnits = totalUnits,
            status = BookStatus.BACKLOG.name,
            lastUpdated = System.currentTimeMillis()
        )
        bookDao.upsert(book.toEntity())
        return book
    }

    override suspend fun updateStatus(id: String, status: BookStatus) {
        val entity = bookDao.getById(id) ?: return
        bookDao.upsert(
            entity.copy(status = status.name, lastUpdated = System.currentTimeMillis())
        )
    }

    override suspend fun addProgress(id: String, delta: Int) {
        val entity = bookDao.getById(id) ?: return
        val ceiling = if (entity.totalUnits > 0) entity.totalUnits else Int.MAX_VALUE
        val newUnit = (entity.currentUnit + delta).coerceIn(0, ceiling)
        bookDao.upsert(
            entity.copy(currentUnit = newUnit, lastUpdated = System.currentTimeMillis())
        )
    }

    override suspend fun applyRemoteProgress(id: String, currentUnit: Int, updatedAt: Long) {
        val entity = bookDao.getById(id) ?: return
        // Last-Write-Wins: ignore stale updates from the watch.
        if (updatedAt <= entity.lastUpdated) return
        val ceiling = if (entity.totalUnits > 0) entity.totalUnits else Int.MAX_VALUE
        bookDao.upsert(
            entity.copy(currentUnit = currentUnit.coerceIn(0, ceiling), lastUpdated = updatedAt)
        )
    }

    override suspend fun deleteBook(id: String) {
        bookDao.deleteById(id)
    }
}
