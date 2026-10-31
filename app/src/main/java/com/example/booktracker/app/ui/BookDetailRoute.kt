package com.example.booktracker.app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.shared.models.Book
import kotlinx.coroutines.launch

/**
 * A book's page wired to the ViewModel. It is its own route on phones and the
 * right-hand pane beside the library on tablets, so both share this one
 * wiring. [onBack] pops the route or closes the pane; [onStartReading] opens
 * the timer for the book id it is given. [showBack] is false for the book the
 * pane shows before one is picked, where there is nothing to go back to.
 */
@Composable
fun BookDetailRoute(
    bookId: String,
    viewModel: BookTrackerViewModel,
    books: List<Book>,
    streak: StreakEngine.StreakInfo,
    snackbarHostState: SnackbarHostState,
    onDeleteWithUndo: (Book) -> Unit,
    onStartReading: (String) -> Unit,
    onBack: () -> Unit,
    showBack: Boolean = true
) {
    val scope = rememberCoroutineScope()
    val sessions by remember(bookId) { viewModel.sessionsFor(bookId) }.collectAsState(initial = emptyList())
    val notes by remember(bookId) { viewModel.notesFor(bookId) }.collectAsState(initial = emptyList())
    val allSessions by viewModel.completedSessions.collectAsState()
    val book = books.find { it.id == bookId }
    fun snack(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    BookDetailScreen(
        book = book,
        sessions = sessions,
        allSessions = allSessions,
        streak = streak,
        notes = notes,
        onAddNote = { page, text, type, images -> viewModel.addNote(bookId, page, text, type, images) },
        onUpdateNote = viewModel::updateNote,
        onDeleteNote = viewModel::deleteNote,
        onToggleNoteFavorite = viewModel::toggleNoteFavorite,
        onSaveDetails = viewModel::updateBookDetails,
        onChangeStatus = { status -> book?.let { viewModel.changeStatus(it, status) } },
        onMarkDnf = { percent, reason -> book?.let { viewModel.markDnf(it, percent, reason) } },
        onLogSession = { startPage, endPage, minutes, endTime ->
            viewModel.logManualSession(bookId, startPage, endPage, minutes, endTime)
            snack("Reading session logged")
        },
        onSetProgress = { page -> book?.let { viewModel.setProgress(it, page) } },
        onSetCollections = { collections -> book?.let { viewModel.setCollections(it, collections) } },
        allCollections = remember(books) { books.flatMap { it.classification.collections }.distinct().sorted() },
        onSetFormat = { format -> book?.let { viewModel.setFormat(it, format) } },
        onSetOwnership = { ownership -> book?.let { viewModel.setOwnership(it, ownership) } },
        onSetPurchases = { purchases -> book?.let { viewModel.setPurchases(it, purchases) } },
        onSetWish = { wish -> book?.let { viewModel.setWish(it, wish) } },
        onGotWished = { purchase ->
            book?.let {
                viewModel.gotWished(it, purchase)
                snack("“${it.title}” is on your shelf")
            }
        },
        onSetFinishBy = { finishBy -> book?.let { viewModel.setFinishBy(it, finishBy) } },
        onDelete = {
            book?.let { onDeleteWithUndo(it) }
            onBack()
        },
        onStartReading = { onStartReading(bookId) },
        onReadAgain = { book?.let { viewModel.readAgain(it) } },
        onEditSession = { session, newUnits -> viewModel.updateSession(session.copy(pagesRead = newUnits)) },
        onDeleteSession = viewModel::deleteSession,
        onToggleFavorite = { book?.let { viewModel.toggleFavorite(it) } },
        onFinish = { rating, review -> book?.let { viewModel.finishBook(it, rating, review) } },
        onRecordLoan = { loan -> book?.let { viewModel.recordLoan(it, loan) } },
        onMarkLoanReturned = { book?.let { viewModel.markLoanReturned(it) } },
        onSaveWord = { word ->
            viewModel.saveWord(word)
            snack("\"${word.word}\" added to your vocabulary")
        },
        onSetAudiobook = { minutes, speed -> book?.let { viewModel.setAudiobook(it, minutes, speed) } },
        onLogListening = { minutes ->
            book?.let { viewModel.logListening(it, minutes) }
            snack("Listening logged")
        },
        onSetAudioPosition = { position -> book?.let { viewModel.setAudioPosition(it, position) } },
        onBack = onBack,
        showBack = showBack
    )
}
