package com.example.booktracker.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.booktracker.app.data.BookRepository
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.app.sync.WearBridge
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookTrackerViewModel(
    private val repository: BookRepository,
    private val wearBridge: WearBridge
) : ViewModel() {

    val books: StateFlow<List<Book>> = repository.observeBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Keep the watch in step with whichever book is currently being read.
        viewModelScope.launch {
            repository.observeBooks()
                .map { list ->
                    list.filter { it.status == BookStatus.READING.name }
                        .maxByOrNull { it.lastUpdated }
                }
                .filterNotNull()
                .distinctUntilChanged { old, new ->
                    old.id == new.id &&
                        old.title == new.title &&
                        old.currentUnit == new.currentUnit &&
                        old.totalUnits == new.totalUnits
                }
                .collect { wearBridge.publishActiveBook(it) }
        }
    }

    fun addBook(title: String, authorsInput: String, totalUnits: Int) {
        val authors = authorsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        viewModelScope.launch { repository.addBook(title.trim(), authors, totalUnits) }
    }

    fun promote(book: Book) {
        val next = when (book.status) {
            BookStatus.BACKLOG.name -> BookStatus.SHORTLIST
            BookStatus.SHORTLIST.name -> BookStatus.UP_NEXT
            BookStatus.UP_NEXT.name -> BookStatus.READING
            else -> return
        }
        viewModelScope.launch { repository.updateStatus(book.id, next) }
    }

    fun addProgress(book: Book, delta: Int) {
        viewModelScope.launch { repository.addProgress(book.id, delta) }
    }

    fun markFinished(book: Book) {
        viewModelScope.launch { repository.updateStatus(book.id, BookStatus.FINISHED) }
    }

    fun delete(book: Book) {
        viewModelScope.launch { repository.deleteBook(book.id) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    BookTrackerViewModel(
                        ServiceLocator.repository(appContext),
                        WearBridge(appContext)
                    )
                }
            }
        }
    }
}
