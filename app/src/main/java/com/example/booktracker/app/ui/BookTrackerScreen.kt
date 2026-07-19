package com.example.booktracker.app.ui

import androidx.activity.compose.BackHandler
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
    import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.material3.SnackbarDuration
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
import com.example.booktracker.app.ui.animations.bounceClick
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.ui.animations.ConfettiExplosion
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
        BookStatus.BACKLOG, BookStatus.BACKLOG.label,
        "Your backlog is empty",
        "Every book you might read someday goes here — scan an ISBN or tap Add book."
    ),
    PipelineTab(
        BookStatus.SHORTLIST, BookStatus.SHORTLIST.label,
        "Nothing shortlisted",
        "Promote the backlog books you're serious about reading soon."
    ),
    PipelineTab(
        BookStatus.UP_NEXT, BookStatus.UP_NEXT.label,
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
    var showStartTimerDialog by remember { mutableStateOf(false) }


    if (showScanner) {
        BackHandler { showScanner = false }
        ScannerScreen(
            onClose = { showScanner = false },
            onBookConfirmed = { scanned ->
                viewModel.addScannedBook(scanned)
                showScanner = false
            },
            onAddManually = {
                showScanner = false
                showAddDialog = true
            }
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    
    val isTopLevelRoute = currentRoute in listOf("home", "library", "stats", "profile")
    val navigateTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val onDeleteWithUndo: (Book) -> Unit = { book ->
        viewModel.delete(book)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Book removed",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restore(book)
            }
        }
    }

    Scaffold(
        topBar = {
            if (isTopLevelRoute) {
                CenterAlignedTopAppBar(
                    title = { Text("Nocturnal Reader") },
                    actions = {
                        IconButton(onClick = { navController.navigate("search") }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search library")
                        }
                        IconButton(onClick = { showScanner = true }) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan ISBN")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isTopLevelRoute) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ) {
                    NavigationBarItem(
                        selected = currentRoute == "home",
                        onClick = { navigateTopLevel("home") },
                        icon = { Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "library",
                        onClick = { navigateTopLevel("library") },
                        icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Library") },
                        label = { Text("Library") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "stats",
                        onClick = { navigateTopLevel("stats") },
                        icon = { Icon(Icons.Filled.Insights, contentDescription = "Stats") },
                        label = { Text("Stats") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "profile",
                        onClick = { navigateTopLevel("profile") },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Profile") },
                        label = { Text("Profile") }
                    )
                }
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
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = "home",
                enterTransition = { 
                    androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) + 
                    androidx.compose.animation.slideInVertically(initialOffsetY = { 50 }, animationSpec = androidx.compose.animation.core.tween(300)) 
                },
                exitTransition = { 
                    androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) 
                }
            ) {
            composable("home") {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 88.dp)
                ) {
                    val openSession by viewModel.openSession.collectAsState()
                    val sessions by viewModel.completedSessions.collectAsState()

                    if (openSession == null && books.isNotEmpty()) {
                        Card(
                            onClick = { 
                                if (readingBook != null && books.filter { it.status == BookStatus.READING.name }.size == 1) {
                                    navController.navigate("focus/${readingBook.id}")
                                } else {
                                    showStartTimerDialog = true 
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(80.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Start Timer", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text("Start Focus Session", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    val readingCount = books.count { it.status == BookStatus.READING.name }
                                    Text(
                                        if (readingCount == 1 && readingBook != null) "Resume ${readingBook.title}"
                                        else "Pick a book to start tracking",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    ReadingHero(
                        book = readingBook,
                        openSession = openSession,
                        sessions = sessions,
                        streak = streak,
                        onProgress = { b, delta ->
                            viewModel.addProgress(b, delta)
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = "Added $delta pages",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.addProgress(b, -delta)
                                }
                            }
                        },
                        onFinish = viewModel::finishBook,
                        onDnf = viewModel::markDnf,
                        onStartSession = viewModel::startSession,
                        onEndSession = viewModel::endSession,
                        onOpenBook = { navController.navigate("book/${it.id}") }
                    )
                }
            }
            composable("library") {
                LibraryScreen(
                    books = books,
                    onPromote = viewModel::promote,
                    onDelete = onDeleteWithUndo,
                    onOpenBook = { navController.navigate("book/${it.id}") },
                    onStartReading = { navController.navigate("focus/${it.id}") },
                    onCurationClick = { navController.navigate("tbr_swipe") }
                )
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
                val goal by viewModel.dailyGoal.collectAsState()
                val yearly by viewModel.yearlyGoal.collectAsState()
                val sessions by viewModel.completedSessions.collectAsState()
                val unlockedBadges by viewModel.unlockedBadges.collectAsState()
                val streak by viewModel.streak.collectAsState()
                val userName by viewModel.userName.collectAsState()
                
                ProfileScreen(
                    userName = userName,
                    books = books,
                    sessions = sessions,
                    streak = streak.currentStreak,
                    yearlyGoal = yearly,
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenHistory = { navController.navigate("history") },
                    onOpenFinished = { navController.navigate("finished") },
                    onOpenBook = { navController.navigate("book/${it.id}") },
                    onUpdateName = viewModel::setUserName
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
                    onSetFormat = { format ->
                        books.find { it.id == bookId }?.let { viewModel.setFormat(it, format) }
                    },
                    onDelete = {
                        books.find { it.id == bookId }?.let { onDeleteWithUndo(it) }
                        navController.popBackStack()
                    },
                    onStartReading = {
                        navController.navigate("focus/$bookId")
                    },
                    onPauseReading = {
                        books.find { it.id == bookId }?.let { viewModel.pauseBook(it.id) }
                    },
                    onReadAgain = {
                        books.find { it.id == bookId }?.let { viewModel.readAgain(it) }
                    },
                    onEditSession = { session, newUnits ->
                        viewModel.updateSession(session.copy(unitsRead = newUnits))
                    },
                    onDeleteSession = viewModel::deleteSession,
                    onToggleFavorite = {
                        books.find { it.id == bookId }?.let { viewModel.toggleFavorite(it) }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("history") {
                val sessions by viewModel.completedSessions.collectAsState()
                HistoryScreen(
                    books = books,
                    sessions = sessions,
                    onDeleteSession = viewModel::deleteSession,
                    onUpdateSession = viewModel::updateSession,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                val context = LocalContext.current
                val goal by viewModel.dailyGoal.collectAsState()
                val yearly by viewModel.yearlyGoal.collectAsState()
                val dnd by viewModel.dndDuringSession.collectAsState()
                val themeMode by viewModel.themeMode.collectAsState()
                val dynamicColor by viewModel.useDynamicColor.collectAsState()
                val userName by viewModel.userName.collectAsState()
                
                SettingsScreen(
                    userName = userName,
                    dailyGoal = goal,
                    yearlyGoal = yearly,
                    dndDuringSession = dnd,
                    themeMode = themeMode,
                    useDynamicColor = dynamicColor,
                    onUserNameChange = viewModel::setUserName,
                    onDailyGoalChange = viewModel::setDailyGoal,
                    onYearlyGoalChange = viewModel::setYearlyGoal,
                    onDndChange = viewModel::setDndDuringSession,
                    onThemeModeChange = viewModel::setThemeMode,
                    onUseDynamicColorChange = viewModel::setUseDynamicColor,
                    onImportCsv = { uri, onDone -> viewModel.importCsv(context, uri, onDone) },
                    onExportBackup = { uri, onDone -> viewModel.exportBackup(context, uri, onDone) },
                    onImportBackup = { uri, onDone -> viewModel.importBackup(context, uri, onDone) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("finished") {
                FinishedScreen(
                    books = books,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("search") {
                LibrarySearchScreen(
                    onSearch = viewModel::searchLibrary,
                    onOpenBook = { bookId -> navController.navigate("book/$bookId") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("focus/{bookId}") { entry ->
                val bookId = entry.arguments?.getString("bookId")
                if (bookId != null) {
                    val book = books.find { it.id == bookId }
                    if (book != null) {
                        FocusTimerScreen(
                            viewModel = viewModel,
                            book = book,
                            onBack = { navController.popBackStack() },
                            onStartSession = viewModel::startSession
                        )
                    }
                }
            }
            composable("tbr_swipe") {
                val tbrBooks by viewModel.tbrCurationBooks.collectAsState()
                TbrSwipeScreen(
                    books = tbrBooks,
                    onPromote = { b -> viewModel.promote(b) },
                    onStartReading = { b -> viewModel.promoteToReading(b) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
        
        val activeTimerBook by viewModel.activeTimerBook.collectAsState()
        val timerIsRunning by viewModel.timerIsRunning.collectAsState()
        val timeLeftSeconds by viewModel.timeLeftSeconds.collectAsState()
        val timerPhase by viewModel.timerPhase.collectAsState()
        
        if (activeTimerBook != null && currentRoute?.startsWith("focus/") != true) {
            TimerMiniPlayer(
                book = activeTimerBook!!,
                isRunning = timerIsRunning,
                timeLeftSeconds = timeLeftSeconds,
                phase = timerPhase,
                onPlayPause = { viewModel.setTimerRunning(!timerIsRunning) },
                onOpenFullTimer = { navController.navigate("focus/${activeTimerBook!!.id}") },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
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
                onAddBook = { metadata, status ->
                    viewModel.addScannedBook(metadata, status)
                    showAddDialog = false
                },
                onScanBarcode = {
                    showAddDialog = false
                    showScanner = true
                }
            )
        }
    }


    if (showStartTimerDialog) {
        SelectTimerBookDialog(
            books = books,
            onDismiss = { showStartTimerDialog = false },
            onConfirm = { book ->
                showStartTimerDialog = false
                navController.navigate("focus/${book.id}")
            }
        )
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
    onStartSession: (Book, Int?) -> Unit,
    onEndSession: (Book, Int, String) -> Unit,
    onOpenBook: (Book) -> Unit = {}
) {
    var showFinishDialog by remember { mutableStateOf(false) }
    var showDnfDialog by remember { mutableStateOf(false) }
    var bookToEndSession by remember { mutableStateOf<Book?>(null) }
    var bookToStartSession by remember { mutableStateOf<Book?>(null) }
    var bookToAddCustomProgress by remember { mutableStateOf<Book?>(null) }
    val haptic = LocalHapticFeedback.current
    var showConfetti by remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showConfetti) {
        ConfettiExplosion(onFinished = { showConfetti = false })
    }

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
                                    val endInteractionSource = remember { MutableInteractionSource() }
                                    Button(
                                        onClick = { }, // Handled by bounceClick
                                        modifier = Modifier.bounceClick(endInteractionSource) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            bookToEndSession = book 
                                        },
                                        interactionSource = endInteractionSource,
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
                                val resumeInteractionSource = remember { MutableInteractionSource() }
                                Button(
                                    onClick = { }, // Handled by bounceClick
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .bounceClick(resumeInteractionSource) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            bookToStartSession = book
                                        },
                                    interactionSource = resumeInteractionSource
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
                            HeroPillButton("+1") {
                                if (streak.pagesToday < streak.dailyGoal && streak.pagesToday + 1 >= streak.dailyGoal) {
                                    showConfetti = true
                                }
                                onProgress(book, 1)
                            }
                            HeroPillButton("+10") {
                                if (streak.pagesToday < streak.dailyGoal && streak.pagesToday + 10 >= streak.dailyGoal) {
                                    showConfetti = true
                                }
                                onProgress(book, 10)
                            }
                            HeroPillButton("+ Custom") {
                                bookToAddCustomProgress = book
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            androidx.compose.material3.TextButton(
                                onClick = { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showDnfDialog = true 
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            ) { Text("Give up") }
                            androidx.compose.material3.TextButton(
                                onClick = { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showFinishDialog = true 
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) { Text("Finish") }
                        }
                    }
                }
            }
        }
    }

    bookToStartSession?.let { startBook ->
        StartSessionDialog(
            currentUnit = startBook.currentUnit,
            totalUnits = startBook.totalUnits,
            unitName = if (startBook.totalUnits > 0) "Page" else "Unit",
            onDismiss = { bookToStartSession = null },
            onConfirm = { startPage ->
                onStartSession(startBook, startPage)
                bookToStartSession = null
            }
        )
    }

    bookToAddCustomProgress?.let { customProgressBook ->
        CustomProgressDialog(
            unitName = if (customProgressBook.totalUnits > 0) "Pages" else "Units",
            onDismiss = { bookToAddCustomProgress = null },
            onConfirm = { delta ->
                onProgress(customProgressBook, delta)
                if (streak.pagesToday < streak.dailyGoal && streak.pagesToday + delta >= streak.dailyGoal) {
                    showConfetti = true
                }
                bookToAddCustomProgress = null
            }
        )
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
        EndSessionDialog(
            startPage = openSession?.startUnit ?: b.currentUnit,
            totalUnits = b.totalUnits,
            durationMillis = openSession?.let { System.currentTimeMillis() - it.startTime } ?: 0L,
            onDismiss = { bookToEndSession = null },
            onConfirm = { tag, endPage ->
                onEndSession(b, endPage, tag)
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
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Animate alpha based on press state
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.35f else 0.2f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "pillAlpha"
    )

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = backgroundAlpha),
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(androidx.compose.ui.graphics.Color.Transparent)
            .bounceClick(interactionSource = interactionSource) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    books: List<Book>,
    onPromote: (Book) -> Unit,
    onDelete: (Book) -> Unit,
    onOpenBook: (Book) -> Unit,
    onStartReading: (Book) -> Unit,
    onCurationClick: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf<BookStatus?>(null) }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    
    val displayBooks = books.filter { 
        val statusMatches = if (selectedFilter == null) {
            it.status != BookStatus.READING.name && it.status != BookStatus.FINISHED.name && it.status != BookStatus.DNF.name
        } else {
            it.status == selectedFilter!!.name
        }
        val favoriteMatches = if (showFavoritesOnly) it.isFavorite else true
        statusMatches && favoriteMatches
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                null to "All To-Read",
                BookStatus.BACKLOG to BookStatus.BACKLOG.label,
                BookStatus.SHORTLIST to BookStatus.SHORTLIST.label,
                BookStatus.UP_NEXT to BookStatus.UP_NEXT.label,
                BookStatus.PAUSED to BookStatus.PAUSED.label,
                BookStatus.FINISHED to BookStatus.FINISHED.label
            )
            
            androidx.compose.material3.FilterChip(
                selected = showFavoritesOnly,
                onClick = { showFavoritesOnly = !showFavoritesOnly },
                label = { Text("⭐ Favorites") },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                shape = CircleShape
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
            // Polished Empty State
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.animateContentSize()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "Your Library is Empty", 
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Scan a barcode or search for a book to start tracking your reading journey.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Grid of Books
            val displayBooksWithStatus = if (selectedFilter == BookStatus.FINISHED) {
                books.filter { it.status == BookStatus.FINISHED.name }
            } else displayBooks

            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(displayBooksWithStatus, key = { it.id }) { book ->
                    LibraryGridCard(
                        book = book,
                        modifier = Modifier.animateItem(),
                        onClick = { onOpenBook(book) },
                        onPromote = { onPromote(book) },
                        onStartReading = { onStartReading(book) },
                        onDelete = { onDelete(book) }
                    )
                }
            }
        }
    }
        
        // Curation FAB
        androidx.compose.material3.ExtendedFloatingActionButton(
            onClick = onCurationClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 96.dp), // Lift above bottom navigation bar
            icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
            text = { Text("Curate TBR") }
        )
    }
}

@Composable
fun LibraryGridCard(
    book: Book,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onPromote: () -> Unit,
    onStartReading: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .bounceClick(onClick = onClick)
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            BookCover(book.coverUrl, Modifier.fillMaxSize())
            
            var expanded by remember { androidx.compose.runtime.mutableStateOf(false) }
            val statusText = try {
                BookStatus.valueOf(book.status).label
            } catch (e: Exception) {
                ""
            }
            if (statusText.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Box {
                        IconButton(
                            onClick = { expanded = true },
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Options",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            if (book.status != BookStatus.READING.name && book.status != BookStatus.FINISHED.name) {
                                DropdownMenuItem(
                                    text = { Text("Start Reading") },
                                    onClick = {
                                        expanded = false
                                        onStartReading()
                                    },
                                    leadingIcon = { Icon(Icons.Filled.MenuBook, contentDescription = null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    expanded = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            // Bottom Gradient Overlay for text
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            // Content Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
            ) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = androidx.compose.ui.graphics.Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.authors.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        book.authors.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Progress Bar at the very bottom edge
            if (book.currentUnit > 0 || book.status == BookStatus.FINISHED.name) {
                val progressFraction = if (book.status == BookStatus.FINISHED.name) 1f else {
                    val total = book.totalUnits.takeIf { it > 0 } ?: 1
                    (book.currentUnit.toFloat() / total).coerceIn(0f, 1f)
                }
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f),
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectTimerBookDialog(
    books: List<Book>,
    onDismiss: () -> Unit,
    onConfirm: (Book) -> Unit
) {
    val options = books.filter { it.status == BookStatus.READING.name || it.status == BookStatus.PAUSED.name || it.status == BookStatus.UP_NEXT.name || it.status == BookStatus.SHORTLIST.name || it.status == BookStatus.BACKLOG.name }
        .sortedByDescending { it.lastUpdated }
    
    if (options.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("No Books") },
            text = { Text("Add a book to your library first to start a session.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
        )
        return
    }

    var selectedBook by remember { mutableStateOf<Book?>(options.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Which book are you reading?", style = MaterialTheme.typography.bodyMedium)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = selectedBook?.title ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.take(15).forEach { book ->
                            DropdownMenuItem(
                                text = { Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = {
                                    selectedBook = book
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedBook != null,
                onClick = { selectedBook?.let { onConfirm(it) } }
            ) { Text("Continue") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TimerMiniPlayer(
    book: Book,
    isRunning: Boolean,
    timeLeftSeconds: Int,
    phase: TimerPhase,
    onPlayPause: () -> Unit,
    onOpenFullTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(36.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        onClick = onOpenFullTimer
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (phase == TimerPhase.WORK) "Deep Focus" else "Break",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                val mins = timeLeftSeconds / 60
                val secs = timeLeftSeconds % 60
                Text(
                    text = String.format("%02d:%02d", mins, secs),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            FilledTonalIconButton(
                onClick = { onPlayPause() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Play"
                )
            }
        }
    }
}
