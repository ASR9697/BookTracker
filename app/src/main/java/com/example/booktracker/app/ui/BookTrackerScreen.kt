package com.example.booktracker.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Session
import kotlinx.coroutines.launch

private val PIPELINE_TABS = listOf(
    BookStatus.BACKLOG to "Backlog",
    BookStatus.SHORTLIST to "Shortlist",
    BookStatus.UP_NEXT to "Up Next"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookTrackerApp(viewModel: BookTrackerViewModel) {
    val books by viewModel.books.collectAsState()
    val readingBook = books
        .filter { it.status == BookStatus.READING.name }
        .maxByOrNull { it.lastUpdated }
    var showAddDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var showAnalytics by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    if (showScanner) {
        BackHandler { showScanner = false }
        ScannerScreen(
            onClose = { showScanner = false },
            onBookConfirmed = { scanned ->
                viewModel.addScannedBook(scanned)
                showScanner = false
            }
        )
        return
    }

    if (showAnalytics) {
        BackHandler { showAnalytics = false }
        val sessions by viewModel.completedSessions.collectAsState()
        val goal by viewModel.dailyGoal.collectAsState()
        AnalyticsScreen(
            sessions = sessions,
            dailyGoal = goal,
            onBack = { showAnalytics = false }
        )
        return
    }

    if (showSettings) {
        BackHandler { showSettings = false }
        val goal by viewModel.dailyGoal.collectAsState()
        SettingsScreen(
            dailyGoal = goal,
            onDailyGoalChange = viewModel::setDailyGoal,
            onBack = { showSettings = false }
        )
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val onDeleteWithUndo: (Book) -> Unit = { book ->
        viewModel.delete(book)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Removed “${book.title}”",
                actionLabel = "Undo",
                withDismissAction = true
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restore(book)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Book Tracker") },
                actions = {
                    IconButton(onClick = { showScanner = true }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan ISBN")
                    }
                    var menuOpen by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Reading activity") },
                            leadingIcon = { Icon(Icons.Filled.Insights, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                showAnalytics = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                showSettings = true
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add book")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val openSession by viewModel.openSession.collectAsState()
            val streak by viewModel.streak.collectAsState()
            ReadingHero(
                book = readingBook,
                openSession = openSession,
                streak = streak,
                onProgress = viewModel::addProgress,
                onFinish = viewModel::markFinished,
                onStartSession = viewModel::startSession,
                onEndSession = viewModel::endSession
            )
            PipelineTabs(
                books = books,
                onPromote = viewModel::promote,
                onDelete = onDeleteWithUndo
            )
        }
    }

    if (showAddDialog) {
        AddBookDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, authors, pages ->
                viewModel.addBook(title, authors, pages)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ReadingHero(
    book: Book?,
    openSession: Session?,
    streak: StreakEngine.StreakInfo,
    onProgress: (Book, Int) -> Unit,
    onFinish: (Book) -> Unit,
    onStartSession: (Book) -> Unit,
    onEndSession: (Book) -> Unit
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            StreakRow(streak)
            Spacer(Modifier.height(8.dp))
            if (book == null) {
                Text("Nothing in progress", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Promote a book through the pipeline to start reading.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BookCover(book.coverUrl, Modifier.size(width = 64.dp, height = 96.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(book.title, style = MaterialTheme.typography.titleLarge)
                        if (book.authors.isNotEmpty()) {
                            Text(
                                book.authors.joinToString(", "),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val progress =
                    if (book.totalUnits > 0) book.currentUnit.toFloat() / book.totalUnits else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Page ${book.currentUnit} of ${book.totalUnits}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { onProgress(book, 1) }) { Text("+1") }
                    OutlinedButton(onClick = { onProgress(book, 10) }) { Text("+10") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { onFinish(book) }) { Text("Finish") }
                }
                if (openSession != null && openSession.bookId == book.id) {
                    val pagesThisSession =
                        (book.currentUnit - openSession.startUnit).coerceAtLeast(0)
                    TextButton(onClick = { onEndSession(book) }) {
                        Text("End session ($pagesThisSession pages this session)")
                    }
                } else {
                    TextButton(onClick = { onStartSession(book) }) {
                        Text("Start session")
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakRow(streak: StreakEngine.StreakInfo) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.LocalFireDepartment,
            contentDescription = null,
            tint = if (streak.goalMetToday) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.height(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "${streak.currentStreak}-day streak · " +
                "${streak.pagesToday}/${streak.dailyGoal} pages today",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PipelineTabs(
    books: List<Book>,
    onPromote: (Book) -> Unit,
    onDelete: (Book) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { PIPELINE_TABS.size })
    val scope = rememberCoroutineScope()

    PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
        PIPELINE_TABS.forEachIndexed { index, (_, label) ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                text = { Text(label) }
            )
        }
    }
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        val (status, label) = PIPELINE_TABS[page]
        val pageBooks = books.filter { it.status == status.name }
        if (pageBooks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No books in $label yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp, vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pageBooks, key = { it.id }) { book ->
                    SwipeableBookCard(book = book, onPromote = onPromote, onDelete = onDelete)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableBookCard(
    book: Book,
    onPromote: (Book) -> Unit,
    onDelete: (Book) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            if (target != SwipeToDismissBoxValue.Settled) {
                onDelete(book)
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val active = dismissState.targetValue != SwipeToDismissBoxValue.Settled
            val alignment =
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Alignment.CenterEnd
                } else {
                    Alignment.CenterStart
                }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (active) MaterialTheme.colorScheme.errorContainer
                        else Color.Transparent
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                if (active) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
        content = { BookCard(book = book, onPromote = onPromote) }
    )
}

@Composable
private fun BookCard(
    book: Book,
    onPromote: (Book) -> Unit
) {
    val promoteLabel = when (book.status) {
        BookStatus.BACKLOG.name -> "Shortlist"
        BookStatus.SHORTLIST.name -> "Up Next"
        BookStatus.UP_NEXT.name -> "Start Reading"
        else -> null
    }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp)) {
            BookCover(book.coverUrl, Modifier.size(width = 40.dp, height = 56.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(book.title, style = MaterialTheme.typography.titleMedium)
                if (book.authors.isNotEmpty()) {
                    Text(
                        book.authors.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (book.totalUnits > 0) {
                    Text("${book.totalUnits} pages", style = MaterialTheme.typography.bodySmall)
                }
                if (promoteLabel != null) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { onPromote(book) }) { Text(promoteLabel) }
                }
            }
        }
    }
}

/**
 * Book cover from a URL, with a neutral book-glyph placeholder for missing or
 * still-loading covers (manually added books have no cover URL).
 */
@Composable
private fun BookCover(url: String, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.small
    if (url.isBlank()) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier.clip(shape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun AddBookDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, authors: String, pages: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var authors by remember { mutableStateOf("") }
    var pagesText by remember { mutableStateOf("") }
    val pages = pagesText.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add book") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = authors,
                    onValueChange = { authors = it },
                    label = { Text("Authors (comma-separated)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = pagesText,
                    onValueChange = { pagesText = it.filter(Char::isDigit) },
                    label = { Text("Total pages") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && pages > 0,
                onClick = { onConfirm(title, authors, pages) }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
