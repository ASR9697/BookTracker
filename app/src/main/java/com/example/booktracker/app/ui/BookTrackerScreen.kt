package com.example.booktracker.app.ui

import androidx.activity.compose.BackHandler
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.ui.theme.StreakFlame
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Session
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class PipelineTab(
    val status: BookStatus,
    val label: String,
    val emptyTitle: String,
    val emptyHint: String
)

private val PIPELINE_TABS = listOf(
    PipelineTab(
        BookStatus.BACKLOG, "Backlog",
        "Your backlog is empty",
        "Every book you might read someday goes here — scan an ISBN or tap Add book."
    ),
    PipelineTab(
        BookStatus.SHORTLIST, "Shortlist",
        "Nothing shortlisted",
        "Promote the backlog books you're serious about reading soon."
    ),
    PipelineTab(
        BookStatus.UP_NEXT, "Up Next",
        "Nothing queued",
        "Pick your next read from the shortlist so it's ready when you finish."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookTrackerApp(viewModel: BookTrackerViewModel) {
    val books by viewModel.books.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val readingBook = books
        .filter { it.status == BookStatus.READING.name }
        .maxByOrNull { it.lastUpdated }
    var showAddDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }

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

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val onDeleteWithUndo: (Book) -> Unit = { book ->
        viewModel.delete(book)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Book removed",
                actionLabel = "Undo"
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restore(book)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nocturnal Reader") },
                actions = {
                    IconButton(onClick = { showScanner = true }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan ISBN")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ) {
                NavigationBarItem(
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home") },
                    icon = { Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = currentRoute == "library",
                    onClick = { navController.navigate("library") },
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Library") },
                    label = { Text("Library") }
                )
                NavigationBarItem(
                    selected = currentRoute == "stats",
                    onClick = { navController.navigate("stats") },
                    icon = { Icon(Icons.Filled.Insights, contentDescription = "Stats") },
                    label = { Text("Stats") }
                )
                NavigationBarItem(
                    selected = currentRoute == "profile",
                    onClick = { navController.navigate("profile") },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (currentRoute == "library" || currentRoute == "home") {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add book") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                Column(modifier = Modifier.fillMaxSize()) {
                    val openSession by viewModel.openSession.collectAsState()
                    val sessions by viewModel.completedSessions.collectAsState()
                    ReadingHero(
                        book = readingBook,
                        openSession = openSession,
                        sessions = sessions,
                        streak = streak,
                        onProgress = viewModel::addProgress,
                        onFinish = viewModel::finishBook,
                        onDnf = viewModel::markDnf,
                        onStartSession = viewModel::startSession,
                        onEndSession = viewModel::endSession,
                        onOpenBook = { navController.navigate("book/${it.id}") }
                    )
                }
            }
            composable("library") {
                Column(modifier = Modifier.fillMaxSize()) {
                    LibraryScreen(
                        books = books,
                        onPromote = viewModel::promote,
                        onDelete = onDeleteWithUndo,
                        onOpenBook = { navController.navigate("book/${it.id}") }
                    )
                }
            }
            composable("stats") {
                val sessions by viewModel.completedSessions.collectAsState()
                val goal by viewModel.dailyGoal.collectAsState()
                val yearly by viewModel.yearlyGoal.collectAsState()
                AnalyticsScreen(
                    books = books,
                    sessions = sessions,
                    dailyGoal = goal,
                    yearlyGoal = yearly
                )
            }
            composable("profile") {
                val sessions by viewModel.completedSessions.collectAsState()
                val yearly by viewModel.yearlyGoal.collectAsState()
                ProfileScreen(
                    books = books,
                    sessions = sessions,
                    streak = streak.currentStreak,
                    yearlyGoal = yearly,
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenHistory = { navController.navigate("history") },
                    onOpenFinished = { navController.navigate("finished") },
                    onOpenBook = { navController.navigate("book/${it.id}") }
                )
            }
            composable("book/{bookId}") { entry ->
                val bookId = entry.arguments?.getString("bookId").orEmpty()
                val sessions by remember(bookId) { viewModel.sessionsFor(bookId) }
                    .collectAsState(initial = emptyList())
                val notes by remember(bookId) { viewModel.notesFor(bookId) }
                    .collectAsState(initial = emptyList())
                BookDetailScreen(
                    book = books.find { it.id == bookId },
                    sessions = sessions,
                    notes = notes,
                    onAddNote = { page, text -> viewModel.addNote(bookId, page, text) },
                    onDeleteNote = viewModel::deleteNote,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("history") {
                val sessions by viewModel.completedSessions.collectAsState()
                HistoryScreen(
                    books = books,
                    sessions = sessions,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                val context = LocalContext.current
                val goal by viewModel.dailyGoal.collectAsState()
                val yearly by viewModel.yearlyGoal.collectAsState()
                SettingsScreen(
                    dailyGoal = goal,
                    yearlyGoal = yearly,
                    onDailyGoalChange = viewModel::setDailyGoal,
                    onYearlyGoalChange = viewModel::setYearlyGoal,
                    onImportCsv = { uri, onDone -> viewModel.importCsv(context, uri, onDone) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("finished") {
                FinishedScreen(
                    books = books,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    if (showAddDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAddDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            AddBookScreen(
                onDismiss = { showAddDialog = false },
                onAddBook = { metadata ->
                    viewModel.addScannedBook(metadata)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun ReadingHero(
    book: Book?,
    openSession: Session?,
    sessions: List<Session>,
    streak: StreakEngine.StreakInfo,
    onProgress: (Book, Int) -> Unit,
    onFinish: (Book, Map<String, Float>) -> Unit,
    onDnf: (Book, Float, String) -> Unit,
    onStartSession: (Book) -> Unit,
    onEndSession: (Book, String) -> Unit,
    onOpenBook: (Book) -> Unit = {}
) {
    var showFinishDialog by remember { mutableStateOf(false) }
    var showDnfDialog by remember { mutableStateOf(false) }
    var bookToEndSession by remember { mutableStateOf<Book?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Currently Reading",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            StreakBadge(streak)
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
                .animateContentSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (book == null) {
                    Text("Nothing in progress", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Promote a book through the pipeline to start reading.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(modifier = Modifier.clickable { onOpenBook(book) }) {
                        BookCover(book.coverUrl, Modifier.size(width = 96.dp, height = 144.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(144.dp)
                        ) {
                            Text(
                                book.title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (book.authors.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    book.authors.joinToString(", "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            val targetProgress = if (book.totalUnits > 0) book.currentUnit.toFloat() / book.totalUnits else 0f
                            val progress by androidx.compose.animation.core.animateFloatAsState(targetValue = targetProgress, label = "progress")
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Progress",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${(progress * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            Spacer(Modifier.height(12.dp))
                            if (openSession != null && openSession.bookId == book.id) {
                                // Live elapsed clock for the running session; ticks
                                // once a second while the hero is on screen.
                                var nowMillis by remember(openSession.id) {
                                    mutableLongStateOf(System.currentTimeMillis())
                                }
                                LaunchedEffect(openSession.id) {
                                    while (true) {
                                        nowMillis = System.currentTimeMillis()
                                        delay(1_000)
                                    }
                                }
                                Row(
                                    modifier = Modifier.align(Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        formatElapsed(nowMillis - openSession.startTime),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Button(
                                        onClick = { bookToEndSession = book },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    ) {
                                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("End")
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { onStartSession(book) },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Resume")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))
                    // Pace line: measured speed + time left, once enough timed sessions exist.
                    val velocity = remember(sessions) { AnalyticsEngine.pagesPerHour(sessions) }
                    val minutesLeft = remember(book, sessions) {
                        AnalyticsEngine.estimatedMinutesLeft(book, sessions)
                    }
                    if (velocity != null) {
                        Text(
                            buildString {
                                append("≈ ${velocity.roundToInt()} pages/hour")
                                if (minutesLeft != null && minutesLeft > 0) {
                                    append(" · ${AnalyticsEngine.formatMinutes(minutesLeft)} left")
                                }
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HeroPillButton("+1") { onProgress(book, 1) }
                            HeroPillButton("+10") { onProgress(book, 10) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            androidx.compose.material3.TextButton(
                                onClick = { showDnfDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            ) { Text("Give up") }
                            androidx.compose.material3.TextButton(
                                onClick = { showFinishDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) { Text("Finish") }
                        }
                    }
                }
            }
        }
    }

    if (book != null && showFinishDialog) {
        FinishDialog(
            book = book,
            onDismiss = { showFinishDialog = false },
            onConfirm = { rating ->
                onFinish(book, rating)
                showFinishDialog = false
            }
        )
    }
    if (book != null && showDnfDialog) {
        DnfDialog(
            book = book,
            onDismiss = { showDnfDialog = false },
            onConfirm = { percent, reason ->
                onDnf(book, percent, reason)
                showDnfDialog = false
            }
        )
    }

    if (bookToEndSession != null) {
        val b = bookToEndSession!!
        // Freeze the recap numbers at the moment the dialog opened.
        val openedAt = remember(b.id) { System.currentTimeMillis() }
        val recapSession = openSession?.takeIf { it.bookId == b.id }
        EndSessionDialog(
            pagesRead = recapSession?.let { (b.currentUnit - it.startUnit).coerceAtLeast(0) } ?: 0,
            durationMillis = recapSession?.let { (openedAt - it.startTime).coerceAtLeast(0) } ?: 0L,
            onDismiss = { bookToEndSession = null },
            onConfirm = { tag ->
                onEndSession(b, tag)
                bookToEndSession = null
            }
        )
    }
}

private fun formatElapsed(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0) / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

@Composable
fun StreakBadge(streak: StreakEngine.StreakInfo) {
    Surface(
        shape = CircleShape,
        color = LocalContentColor.current.copy(alpha = 0.10f),
        contentColor = LocalContentColor.current
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = if (streak.goalMetToday) StreakFlame
                else LocalContentColor.current.copy(alpha = 0.55f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                "${streak.currentStreak}-day streak · " +
                    "${streak.pagesToday}/${streak.dailyGoal}",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun HeroPillButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = LocalContentColor.current.copy(alpha = 0.12f),
            contentColor = LocalContentColor.current
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) { Text(label, style = MaterialTheme.typography.labelLarge) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    books: List<Book>,
    onPromote: (Book) -> Unit,
    onDelete: (Book) -> Unit,
    onOpenBook: (Book) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf<BookStatus?>(null) }
    
    val displayBooks = books.filter { 
        if (selectedFilter == null) {
            it.status != BookStatus.READING.name && it.status != BookStatus.FINISHED.name && it.status != BookStatus.DNF.name
        } else {
            it.status == selectedFilter!!.name
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                null to "All To-Read",
                BookStatus.BACKLOG to "Backlog",
                BookStatus.SHORTLIST to "Shortlist",
                BookStatus.UP_NEXT to "Up Next",
                BookStatus.FINISHED to "Finished"
            )
            
            filters.forEach { (status, label) ->
                val isSelected = selectedFilter == status
                androidx.compose.material3.FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = status },
                    label = { Text(label) },
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = CircleShape,
                    border = null
                )
            }
        }
        
        if (displayBooks.isEmpty()) {
            // Empty State
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No books found", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Try adding some books or changing your filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Grid of Books
            val displayBooksWithStatus = if (selectedFilter == BookStatus.FINISHED) {
                books.filter { it.status == BookStatus.FINISHED.name }
            } else displayBooks

            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(displayBooksWithStatus, key = { it.id }) { book ->
                    LibraryGridCard(
                        book = book,
                        modifier = Modifier.animateItem(),
                        onClick = { onOpenBook(book) },
                        onPromote = { onPromote(book) },
                        onDelete = { onDelete(book) }
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryGridCard(
    book: Book,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onPromote: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                BookCover(book.coverUrl, Modifier.fillMaxSize())
                // Status Pill
                val statusText = when(book.status) {
                    BookStatus.BACKLOG.name -> "Backlog"
                    BookStatus.SHORTLIST.name -> "Shortlist"
                    BookStatus.UP_NEXT.name -> "Up Next"
                    BookStatus.FINISHED.name -> "Finished"
                    else -> ""
                }
                if (statusText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (book.authors.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            book.authors.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (book.status != BookStatus.FINISHED.name) {
                        IconButton(onClick = onPromote, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Promote", modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun CountPill(count: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EmptyPipelineState(tab: PipelineTab) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(tab.emptyTitle, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            tab.emptyHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableBookCard(
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
                    .clip(RoundedCornerShape(20.dp))
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
fun BookCard(
    book: Book,
    onPromote: (Book) -> Unit
) {
    val promoteLabel = when (book.status) {
        BookStatus.BACKLOG.name -> "Shortlist"
        BookStatus.SHORTLIST.name -> "Up Next"
        BookStatus.UP_NEXT.name -> "Start Reading"
        else -> null
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCover(book.coverUrl, Modifier.size(width = 48.dp, height = 72.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.authors.isNotEmpty()) {
                    Text(
                        book.authors.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (book.totalUnits > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${book.totalUnits} pages",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (promoteLabel != null) {
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = { onPromote(book) },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(promoteLabel, style = MaterialTheme.typography.labelMedium)
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
fun BookCover(url: String, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.small
    if (url.isBlank()) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
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
