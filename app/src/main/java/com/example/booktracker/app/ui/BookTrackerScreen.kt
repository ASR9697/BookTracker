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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
    import androidx.compose.material3.CenterAlignedTopAppBar
    import androidx.compose.material3.ElevatedCard
    import androidx.compose.material3.OutlinedCard
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
import android.net.Uri
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
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import coil3.compose.AsyncImage
import com.example.booktracker.app.ui.animations.bounceClick
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.analytics.LoanEngine
import com.example.booktracker.app.ui.animations.ConfettiExplosion
import com.example.booktracker.app.ui.theme.StreakFlame
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Session
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class HomeTabMode { CALENDAR, BOOKSHELF }
enum class LibraryViewMode { GRID, BOOKSHELF }

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun BookTrackerApp(viewModel: BookTrackerViewModel, windowSizeClass: WindowSizeClass? = null) {
    val books by viewModel.books.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val readingBooks = books
        .filter { it.status == BookStatus.READING.name }
        .sortedByDescending { it.lastUpdated }
    var showAddDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var showStartTimerDialog by remember { mutableStateOf(false) }
    var pendingOutcome by remember { mutableStateOf<Pair<String, BookStatus>?>(null) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var initialGoalType by remember { mutableStateOf(GoalType.YEARLY_BOOKS) }
    val monthlyGoalPages by viewModel.monthlyGoalPages.collectAsState()
    val monthlyGoalBooks by viewModel.monthlyGoalBooks.collectAsState()
    val dailyGoal by viewModel.dailyGoal.collectAsState()
    val yearlyGoal by viewModel.yearlyGoal.collectAsState()


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

    if (showGoalDialog) {
        GoalEditDialog(
            initialType = initialGoalType,
            currentYearlyGoal = yearlyGoal,
            currentMonthlyPagesGoal = monthlyGoalPages,
            currentMonthlyBooksGoal = monthlyGoalBooks,
            currentDailyGoal = dailyGoal,
            onSaveYearlyGoal = viewModel::setYearlyGoal,
            onSaveMonthlyPagesGoal = viewModel::setMonthlyGoalPages,
            onSaveMonthlyBooksGoal = viewModel::setMonthlyGoalBooks,
            onSaveDailyGoal = viewModel::setDailyGoal,
            onDismiss = { showGoalDialog = false }
        )
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    
    val isTopLevelRoute = currentRoute in listOf("home", "library", "memorize", "timer", "stats", "profile")
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
                    title = { Text("Nocturnal Reader", style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigate("search") }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search library")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showScanner = true }) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan ISBN")
                        }
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val activeTimerBook by viewModel.activeTimerBook.collectAsState()
                val timerIsRunning by viewModel.timerIsRunning.collectAsState()
                val timeLeftSeconds by viewModel.timeLeftSeconds.collectAsState()
                val timerPhase by viewModel.timerPhase.collectAsState()
                val bottomNavHaptic = LocalHapticFeedback.current

                androidx.compose.animation.AnimatedVisibility(
                    visible = activeTimerBook != null && currentRoute?.startsWith("focus/") != true,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + fadeIn(tween(250)),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200))
                ) {
                    activeTimerBook?.let { book ->
                        TimerMiniPlayer(
                            book = book,
                            isRunning = timerIsRunning,
                            timeLeftSeconds = timeLeftSeconds,
                            phase = timerPhase,
                            onPlayPause = { viewModel.setTimerRunning(!timerIsRunning) },
                            onStop = { navController.navigate("focus/${book.id}") },
                            onDismiss = { 
                                viewModel.setTimerRunning(false)
                                viewModel.setTimerBook(null)
                            },
                            onOpenFullTimer = { navController.navigate("focus/${book.id}") },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                if (isTopLevelRoute && windowSizeClass?.widthSizeClass != WindowWidthSizeClass.Expanded) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
                        tonalElevation = 6.dp
                    ) {
                        val navColors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                        NavigationBarItem(
                            selected = currentRoute == "home" || currentRoute == "library",
                            onClick = { 
                                bottomNavHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                navigateTopLevel("home") 
                            },
                            icon = { Icon(Icons.Filled.AutoStories, contentDescription = "Home") },
                            label = { Text("Home") },
                            colors = navColors
                        )
                        NavigationBarItem(
                            selected = currentRoute == "memorize",
                            onClick = { 
                                bottomNavHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                navigateTopLevel("memorize") 
                            },
                            icon = { Icon(Icons.Filled.Style, contentDescription = "Notes") },
                            label = { Text("Notes") },
                            colors = navColors
                        )
                        NavigationBarItem(
                            selected = currentRoute == "stats",
                            onClick = { 
                                bottomNavHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                navigateTopLevel("stats") 
                            },
                            icon = { Icon(Icons.Filled.Insights, contentDescription = "Stats") },
                            label = { Text("Stats") },
                            colors = navColors
                        )
                        NavigationBarItem(
                            selected = currentRoute == "profile",
                            onClick = { 
                                bottomNavHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                navigateTopLevel("profile") 
                            },
                            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                            label = { Text("Profile") },
                            colors = navColors
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (currentRoute == "library" || currentRoute == "home") {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add book") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isTopLevelRoute && windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f)
                ) {
                    Spacer(Modifier.weight(1f))
                    NavigationRailItem(
                        selected = currentRoute == "home" || currentRoute == "library",
                        onClick = { navigateTopLevel("home") },
                        icon = { Icon(Icons.Filled.AutoStories, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationRailItem(
                        selected = currentRoute == "memorize",
                        onClick = { navigateTopLevel("memorize") },
                        icon = { Icon(Icons.Filled.Style, contentDescription = "Notes") },
                        label = { Text("Notes") }
                    )
                    NavigationRailItem(
                        selected = currentRoute == "stats",
                        onClick = { navigateTopLevel("stats") },
                        icon = { Icon(Icons.Filled.Insights, contentDescription = "Stats") },
                        label = { Text("Stats") }
                    )
                    NavigationRailItem(
                        selected = currentRoute == "profile",
                        onClick = { navigateTopLevel("profile") },
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                        label = { Text("Profile") }
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
androidx.compose.animation.SharedTransitionLayout {
                    androidx.compose.runtime.CompositionLocalProvider(LocalSharedTransitionScope provides this) {
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
                
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                            var homeTabMode by remember { mutableStateOf(HomeTabMode.CALENDAR) }
                                            val openSession by viewModel.openSession.collectAsState()
                                            val sessions by viewModel.completedSessions.collectAsState()
                                            val allNotes by viewModel.allNotes.collectAsState()
                                            val currentUserName by viewModel.userName.collectAsState()

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(bottom = 88.dp)
                                            ) {
                                                // Segmented Switcher (Bookmory Calendar vs Bookshelf)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 20.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    SingleChoiceSegmentedButtonRow(
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        SegmentedButton(
                                                            selected = homeTabMode == HomeTabMode.CALENDAR,
                                                            onClick = { homeTabMode = HomeTabMode.CALENDAR },
                                                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                                            icon = {
                                                                Icon(
                                                                    Icons.Filled.CalendarMonth,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        ) {
                                                            Text("Calendar")
                                                        }
                                                        SegmentedButton(
                                                            selected = homeTabMode == HomeTabMode.BOOKSHELF,
                                                            onClick = { homeTabMode = HomeTabMode.BOOKSHELF },
                                                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                                            icon = {
                                                                Icon(
                                                                    Icons.AutoMirrored.Filled.MenuBook,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        ) {
                                                            Text("Bookshelf")
                                                        }
                                                    }
                                                }

                                                if (homeTabMode == HomeTabMode.CALENDAR) {
                                                    ReadingCalendarView(
                                                        books = books,
                                                        sessions = sessions,
                                                        notes = allNotes,
                                                        monthlyGoalPages = monthlyGoalPages,
                                                        userName = currentUserName,
                                                        onOpenBook = { navController.navigate("book/${it.id}") },
                                                        onStartTimer = { navController.navigate("focus/${it.id}") },
                                                        onEditGoal = {
                                                            initialGoalType = GoalType.MONTHLY_PAGES
                                                            showGoalDialog = true
                                                        }
                                                    )
                                                } else {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .verticalScroll(rememberScrollState())
                                                    ) {
                                                        val urgentLoans = remember(books) { LoanEngine.urgentLoans(books) }
                                                        if (urgentLoans.isNotEmpty()) {
                                                            UrgentLoanBanner(
                                                                urgentLoans = urgentLoans,
                                                                onOpenBook = { navController.navigate("book/${it.id}") },
                                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                                            )
                                                        }

                                                        if (openSession == null && books.isNotEmpty()) {
                                                            ElevatedCard(
                                                                onClick = { 
                                                                    if (readingBooks.size == 1) {
                                                                        navController.navigate("focus/${readingBooks.first().id}")
                                                                    } else {
                                                                        showStartTimerDialog = true 
                                                                    }
                                                                },
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(16.dp)
                                                                    .height(84.dp),
                                                                shape = RoundedCornerShape(16.dp),
                                                                colors = CardDefaults.elevatedCardColors(
                                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                                )
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(44.dp)
                                                                            .clip(CircleShape)
                                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Filled.PlayArrow,
                                                                            contentDescription = "Start Timer",
                                                                            modifier = Modifier.size(26.dp),
                                                                            tint = MaterialTheme.colorScheme.primary
                                                                        )
                                                                    }
                                                                    Spacer(Modifier.width(16.dp))
                                                                    Column {
                                                                        Text(
                                                                            "Start Focus Session",
                                                                            style = MaterialTheme.typography.titleMedium,
                                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                                        )
                                                                        Text(
                                                                            if (readingBooks.size == 1) "Resume ${readingBooks.first().title}"
                                                                            else "Pick a book to start tracking",
                                                                            style = MaterialTheme.typography.bodyMedium,
                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                            maxLines = 1,
                                                                            overflow = TextOverflow.Ellipsis
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        ReadingHero(
                                                            books = readingBooks,
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
                                                            onEndSession = { b -> navController.navigate("session_save/${b.id}") },
                                                            onDeleteSession = { id: String -> 
                                                                viewModel.completedSessions.value.find { it.id == id }?.let { viewModel.deleteSession(it) } 
                                                            },
                                                            onOpenBook = { navController.navigate("book/${it.id}") },
                                                            onGoToLibrary = { navigateTopLevel("library") }
                                                        )
                                                    }
                                                }
                                            }
            
                                        }
                                    }
                                    composable("epubReader/{uri}") { backStackEntry ->
                                        val uriString = backStackEntry.arguments?.getString("uri") ?: ""
                                        com.example.booktracker.app.ui.reader.EpubReaderScreen(
                                            uriString = uriString,
                                            onNavigateUp = { navController.navigateUp() }
                                        )
                                    }
                                    composable("library") {
                
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {                LibraryScreen(
                                                            books = books,
                                                            onPromote = viewModel::promote,
                                                            onDelete = onDeleteWithUndo,
                                                            onOpenBook = { navController.navigate("book/${it.id}") },
                                                            onStartReading = { navController.navigate("focus/${it.id}") },
                                                            onCurationClick = { navController.navigate("tbr_swipe") },
                                                            onReadEpub = { uri -> 
                                                                navController.navigate("epubReader/${Uri.encode(uri.toString())}") 
                                                            },
                                                            onSearch = { navController.navigate("search") },
                                                            onScan = { showScanner = true }
                                                        )
            
                                        }
                                    }
                                    composable("memorize") {
                
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {                val notes by viewModel.allNotes.collectAsState()
                                                        MemorizeScreen(
                                                            notes = notes,
                                                            books = books,
                                                            onToggleFavorite = viewModel::toggleNoteFavorite
                                                        )
            
                                        }
                                    }
                                    composable("timer") {
                
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {                TimerSetupScreen(
                                                            viewModel = viewModel,
                                                            books = books,
                                                            onStartTimer = { book, startPage ->
                                                                navController.navigate("focus/${book.id}")
                                                            },
                                                            onManualSessionSaved = {
                                                                scope.launch {
                                                                    snackbarHostState.showSnackbar(
                                                                        message = "Manual session saved",
                                                                        duration = SnackbarDuration.Short
                                                                    )
                                                                }
                                                            }
                                                        )
            
                                        }
                                    }
                                    composable("stats") {
                
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {                val sessions by viewModel.completedSessions.collectAsState()
                                                        val goal by viewModel.dailyGoal.collectAsState()
                                                        val yearly by viewModel.yearlyGoal.collectAsState()
                                                        val monthlyPages by viewModel.monthlyGoalPages.collectAsState()
                                                        val monthlyBooks by viewModel.monthlyGoalBooks.collectAsState()
                                                        AnalyticsScreen(
                                                            books = books,
                                                            sessions = sessions,
                                                            dailyGoal = goal,
                                                            yearlyGoal = yearly,
                                                            monthlyGoalPages = monthlyPages,
                                                            monthlyGoalBooks = monthlyBooks,
                                                            onEditGoal = { targetGoalType ->
                                                                initialGoalType = targetGoalType
                                                                showGoalDialog = true
                                                            }
                                                        )
            
                                        }
                                    }
                                    composable("profile") {
                
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {                val goal by viewModel.dailyGoal.collectAsState()
                                                        val yearly by viewModel.yearlyGoal.collectAsState()
                                                        val sessions by viewModel.completedSessions.collectAsState()
                                                        val unlockedBadges by viewModel.unlockedBadges.collectAsState()
                                                        val streak by viewModel.streak.collectAsState()
                                                        val userName by viewModel.userName.collectAsState()
                                                        val allNotes by viewModel.allNotes.collectAsState()
                
                                                        ProfileScreen(
                                                            userName = userName,
                                                            books = books,
                                                            sessions = sessions,
                                                            notes = allNotes,
                                                            streak = streak.currentStreak,
                                                            yearlyGoal = yearly,
                                                            unlockedBadges = unlockedBadges,
                                                            onOpenSettings = { navController.navigate("settings") },
                                                            onOpenHistory = { navController.navigate("history") },
                                                            onOpenFinished = { navController.navigate("finished") },
                                                            onOpenPlanner = { navController.navigate("planner") },
                                                            onOpenBook = { navController.navigate("book/${it.id}") },
                                                            onOpenWrapped = { navController.navigate("wrappedAnimated") },
                                                            onUpdateName = viewModel::setUserName,
                                                            onEditGoal = {
                                                                initialGoalType = GoalType.YEARLY_BOOKS
                                                                showGoalDialog = true
                                                            }
                                                        )
            
                                        }
                                    }
                                    composable("wrappedAnimated") {
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                            val completedSessions by viewModel.completedSessions.collectAsState()
                                            val currentStreak by viewModel.streak.collectAsState()
                                            WrappedScreen(
                                                books = books,
                                                sessions = completedSessions,
                                                streak = currentStreak.currentStreak,
                                                onBack = { navController.popBackStack() }
                                            )
                                        }
                                    }
                                    composable("wrapped") {
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                            val completedSessions by viewModel.completedSessions.collectAsState()
                                            val currentStreak by viewModel.streak.collectAsState()
                                            WrappedScreen(
                                                books = books,
                                                sessions = completedSessions,
                                                streak = currentStreak.currentStreak,
                                                onBack = { navController.popBackStack() }
                                            )
                                        }
                                    }
                                    composable("planner") {
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                            PlannerScreen(
                                                books = books,
                                                onPlanBook = { bookId, timestamp -> viewModel.planBook(bookId, timestamp) },
                                                onBack = { navController.popBackStack() }
                                            )
                                        }
                                    }
                                    composable("book/{bookId}") { 
                                        entry ->
androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                                        val bookId = entry.arguments?.getString("bookId").orEmpty()
                                                        val sessions by remember(bookId) { viewModel.sessionsFor(bookId) }
                                                            .collectAsState(initial = emptyList())
                                                        val notes by remember(bookId) { viewModel.notesFor(bookId) }
                                                            .collectAsState(initial = emptyList())
                                                        val allSessions by viewModel.completedSessions.collectAsState()
                                                        BookDetailScreen(
                                                            book = books.find { it.id == bookId },
                                                            sessions = sessions,
                                                            allSessions = allSessions,
                                                            streak = streak,
                                                            notes = notes,
                                                            onAddNote = { page, text, type -> viewModel.addNote(bookId, page, text, type) },
                                                            onDeleteNote = viewModel::deleteNote,
                                                            onToggleNoteFavorite = viewModel::toggleNoteFavorite,
                                                            onSetProgress = { page ->
                                                                books.find { it.id == bookId }?.let { viewModel.setProgress(it, page) }
                                                            },
                                                            onSetCollections = { collections ->
                                                                books.find { it.id == bookId }?.let { viewModel.setCollections(it, collections) }
                                                            },
                                                            allCollections = remember(books) {
                                                                books.flatMap { it.classification.collections }.distinct().sorted()
                                                            },
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
                                                                viewModel.updateSession(session.copy(pagesRead = newUnits))
                                                            },
                                                            onDeleteSession = viewModel::deleteSession,
                                                            onToggleFavorite = {
                                                                books.find { it.id == bookId }?.let { viewModel.toggleFavorite(it) }
                                                            },
                                                            onFinish = { rating, review ->
                                                                books.find { it.id == bookId }?.let { viewModel.finishBook(it, rating, review) }
                                                            },
                                                            onRecordLoan = { loan ->
                                                                books.find { it.id == bookId }?.let { viewModel.recordLoan(it, loan) }
                                                            },
                                                            onMarkLoanReturned = {
                                                                books.find { it.id == bookId }?.let { viewModel.markLoanReturned(it) }
                                                            },
                                                            onBack = { navController.popBackStack() }
                                                        )
            
                                        }
                                    }
                                    composable("history") {
                
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {                val sessions by viewModel.completedSessions.collectAsState()
                                                        HistoryScreen(
                                                            books = books,
                                                            sessions = sessions,
                                                            onDeleteSession = viewModel::deleteSession,
                                                            onUpdateSession = viewModel::updateSession,
                                                            onBack = { navController.popBackStack() }
                                                        )
            
                                        }
                                    }
                                    composable("settings") {
                
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {                val context = LocalContext.current
                                                        val goal by viewModel.dailyGoal.collectAsState()
                                                        val yearly by viewModel.yearlyGoal.collectAsState()
                                                        val dnd by viewModel.dndDuringSession.collectAsState()
                                                        val themeMode by viewModel.themeMode.collectAsState()
                                                        val dynamicColor by viewModel.useDynamicColor.collectAsState()
                                                        val appPalette by viewModel.appPalette.collectAsState()
                                                        val userName by viewModel.userName.collectAsState()
                                                        val dayStartsAtHour by viewModel.dayStartsAtHour.collectAsState()
                                                        val useAppLock by viewModel.useAppLock.collectAsState()
                
                                                        SettingsScreen(
                                                            userName = userName,
                                                            dailyGoal = goal,
                                                            yearlyGoal = yearly,
                                                            dayStartsAtHour = dayStartsAtHour,
                                                            dndDuringSession = dnd,
                                                            themeMode = themeMode,
                                                            palette = appPalette,
                                                            onPaletteChange = viewModel::setAppPalette,
                                                            useDynamicColor = dynamicColor,
                                                            useAppLock = useAppLock,
                                                            onUserNameChange = viewModel::setUserName,
                                                            onDailyGoalChange = viewModel::setDailyGoal,
                                                            onYearlyGoalChange = viewModel::setYearlyGoal,
                                                            onDayStartsAtHourChange = viewModel::setDayStartsAtHour,
                                                            onDndChange = viewModel::setDndDuringSession,
                                                            onThemeModeChange = viewModel::setThemeMode,
                                                            onUseDynamicColorChange = viewModel::setUseDynamicColor,
                                                            onAppLockChange = viewModel::setUseAppLock,
                                                            onImportCsv = { uri, onDone -> viewModel.importCsv(context, uri, onDone) },
                                                            onExportBackup = { uri, onDone -> viewModel.exportBackup(context, uri, onDone) },
                                                            onImportBackup = { uri, onDone -> viewModel.importBackup(context, uri, onDone) },
                                                            onBack = { navController.popBackStack() }
                                                        )
            
                                        }
                                    }
                                    composable("finished") {
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                            val finishedSessions by viewModel.completedSessions.collectAsState()
                                            val finishedNotes by viewModel.allNotes.collectAsState()
                                            FinishedScreen(
                                                books = books,
                                                sessions = finishedSessions,
                                                notes = finishedNotes,
                                                onBack = { navController.popBackStack() },
                                                onOpenBook = { navController.navigate("book/${it.id}") },
                                                onOpenJourneyMap = { navController.navigate("journey_map") }
                                            )
                                        }
                                    }
                                    composable("journey_map") {
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                            com.example.booktracker.app.ui.ReadingJourneyMap(
                                                books = books,
                                                onBack = { navController.popBackStack() },
                                                onOpenBook = { navController.navigate("book/${it.id}") }
                                            )
                                        }
                                    }
                                    composable("search") {
                
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {                LibrarySearchScreen(
                                                            onSearch = viewModel::searchLibrary,
                                                            onOpenBook = { bookId -> navController.navigate("book/$bookId") },
                                                            onBack = { navController.popBackStack() }
                                                        )
            
                                        }
                                    }
                                    composable("focus/{bookId}") { 
                                        entry ->
androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                                        val bookId = entry.arguments?.getString("bookId")
                                                        if (bookId != null) {
                                                            val book = books.find { it.id == bookId }
                                                            if (book != null) {
                                                                FocusTimerScreen(
                                                                    viewModel = viewModel,
                                                                    book = book,
                                                                    onBack = { navController.popBackStack() },
                                                                    onStartSession = viewModel::startSession,
                                                                    onSaveSession = {
                                                                        navController.navigate("session_save/$bookId")
                                                                    },
                                                                    onAddNote = { page, text -> viewModel.addNote(bookId, page, text) }
                                                                )
                                                            }
                                                        }
            
                                        }
                                    }
                                    composable("session_save/{bookId}") { 
                                        entry ->
androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                                        val bookId = entry.arguments?.getString("bookId").orEmpty()
                                                        val book = books.find { it.id == bookId }
                                                        val openSession by viewModel.openSession.collectAsState()
                                                        val activeSeconds by com.example.booktracker.app.data.ServiceLocator
                                                            .activeReadingSeconds.collectAsState()
                                                        if (book != null) {
                                                            // Prefer the timer's measured reading time; fall back to the
                                                            // open session's wall clock for progress logged without a timer.
                                                            val session = openSession?.takeIf { it.bookId == bookId }
                                                            val start = session?.startTime ?: System.currentTimeMillis()
                                                            val duration = if (activeSeconds > 0) activeSeconds
                                                            else ((System.currentTimeMillis() - start) / 1000L).toInt().coerceAtLeast(0)

                                                            SessionSaveScreen(
                                                                book = book,
                                                                initialStartTime = start,
                                                                initialDurationSeconds = duration,
                                                                initialPage = book.currentPage,
                                                                onBack = { navController.popBackStack() },
                                                                onDiscard = {
                                                                    viewModel.discardOpenSession(bookId)
                                                                    navController.popBackStack("home", inclusive = false)
                                                                },
                                                                onSave = { endPage, tag, startTime, endTime, status ->
                                                                    viewModel.saveSession(
                                                                        book = book,
                                                                        endPage = endPage,
                                                                        environmentTag = tag,
                                                                        startTime = startTime,
                                                                        endTime = endTime,
                                                                        newStatus = status
                                                                    ) {
                                                                        navController.navigate("session_result/$bookId") {
                                                                            popUpTo("home") { inclusive = false }
                                                                        }
                                                                    }
                                                                    pendingOutcome = status?.takeIf {
                                                                        it == BookStatus.FINISHED || it == BookStatus.DNF
                                                                    }?.let { bookId to it }
                                                                }
                                                            )
                                                        }
            
                                        }
                                    }
                                    composable("session_result/{bookId}") { 
                                        entry ->
androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                                        val bookId = entry.arguments?.getString("bookId").orEmpty()
                                                        val book = books.find { it.id == bookId }
                                                        val result by viewModel.lastSessionResult.collectAsState()
                                                        val allSessions by viewModel.completedSessions.collectAsState()
                                                        val current = result
                                                        if (book != null && current != null) {
                                                            SessionResultScreen(
                                                                book = book,
                                                                result = current,
                                                                allSessions = allSessions,
                                                                onDone = {
                                                                    viewModel.clearSessionResult()
                                                                    navController.popBackStack("home", inclusive = false)
                                                                }
                                                            )
                                                        }
            
                                        }
                                    }
                                    composable("tbr_swipe") {
                
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {                val tbrBooks by viewModel.tbrCurationBooks.collectAsState()
                                                        TbrSwipeScreen(
                                                            books = tbrBooks,
                                                            onPromote = { b -> viewModel.promote(b) },
                                                            onStartReading = { b -> viewModel.promoteToReading(b) },
                                                            onBack = { navController.popBackStack() }
                                                        )
            
                                        }
                                    }
                                }
                    }
                }
        
        }
    }
    }



    pendingOutcome?.let { (outcomeBookId, outcome) ->
        val outcomeBook = books.find { it.id == outcomeBookId }
        if (outcomeBook == null) {
            pendingOutcome = null
        } else if (outcome == BookStatus.FINISHED) {
            FinishDialog(
                book = outcomeBook,
                onDismiss = {
                    viewModel.finishBook(outcomeBook, emptyMap())
                    pendingOutcome = null
                },
                onConfirm = { rating, review ->
                    viewModel.finishBook(outcomeBook, rating, review)
                    pendingOutcome = null
                }
            )
        } else {
            DnfDialog(
                book = outcomeBook,
                onDismiss = { pendingOutcome = null },
                onConfirm = { percentage, reason ->
                    viewModel.markDnf(outcomeBook, percentage, reason)
                    pendingOutcome = null
                }
            )
        }
    }

    if (showAddDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAddDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            AddBookScreen(
                onDismiss = { showAddDialog = false },
                onAddBook = { metadata, status, format ->
                    viewModel.addScannedBook(metadata, status, format)
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
            },
            onGoToLibrary = {
                showStartTimerDialog = false
                navigateTopLevel("library")
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ReadingHero(
    books: List<Book>,
    openSession: Session?,
    sessions: List<Session>,
    streak: StreakEngine.StreakInfo,
    onProgress: (Book, Int) -> Unit,
    onFinish: (Book, Map<String, Float>, String?) -> Unit,
    onDnf: (Book, Float, String) -> Unit,
    onStartSession: (Book, Int?) -> Unit,
    onEndSession: (Book) -> Unit,
    onDeleteSession: (String) -> Unit,
    onOpenBook: (Book) -> Unit = {},
    onGoToLibrary: () -> Unit = {}
) {
    var bookToFinish by remember { mutableStateOf<Book?>(null) }
    var bookToDnf by remember { mutableStateOf<Book?>(null) }
    var bookToStartSession by remember { mutableStateOf<Book?>(null) }
    var bookToAddCustomProgress by remember { mutableStateOf<Book?>(null) }
    val haptic = LocalHapticFeedback.current
    var showConfetti by remember { androidx.compose.runtime.mutableStateOf(false) }
    
    // Track previous state to only fire on transition
    var previousGoalMet by remember { androidx.compose.runtime.mutableStateOf(streak.goalMetToday) }
    
    LaunchedEffect(streak.goalMetToday) {
        if (streak.goalMetToday && !previousGoalMet) {
            showConfetti = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        previousGoalMet = streak.goalMetToday
    }

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

        if (books.isEmpty()) {
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
                    Text("Nothing in progress", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Promote a book through the pipeline to start reading.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    FilledTonalButton(onClick = onGoToLibrary) {
                        Text("Go to Library")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                books.forEach { book ->
                    val progressVal = if (book.totalPages > 0) book.currentPage.toFloat() / book.totalPages else 0f
                    val progress by androidx.compose.animation.core.animateFloatAsState(targetValue = progressVal, label = "progress")
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.clickable { onOpenBook(book) }) {
                                BookCover(book.coverUrl, Modifier.heroSharedElement("cover-${book.id}").size(width = 96.dp, height = 144.dp))
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
                                    
                                    val bookSessions = sessions.filter { it.bookId == book.id && it.durationSeconds > 0 && it.pagesRead > 0 }
                                    val totalTimeSecs = bookSessions.sumOf { it.durationSeconds }
                                    val totalPagesRead = bookSessions.sumOf { it.pagesRead }
                                    val avgSpeed = if (totalTimeSecs > 0) totalPagesRead.toFloat() / totalTimeSecs else 0f
                                    val remainingPages = (book.totalPages - book.currentPage).coerceAtLeast(0)
                                    val remainingTimeText = if (avgSpeed > 0 && remainingPages > 0) {
                                        val remainingSecs = (remainingPages / avgSpeed).toInt()
                                        val h = remainingSecs / 3600
                                        val m = (remainingSecs % 3600) / 60
                                        if (h > 0) "${h}h ${m}m remaining" else "${m}m remaining"
                                    } else null

                                    if (remainingTimeText != null) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            remainingTimeText,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    
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
                                    com.example.booktracker.app.ui.animations.LiquidWaveProgressBar(
                                        progress = progressVal,
                                        modifier = Modifier.fillMaxWidth(),
                                        height = 10.dp,
                                        waveColor = MaterialTheme.colorScheme.primary,
                                        secondaryWaveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    if (openSession != null && openSession.bookId == book.id) {
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
                                                onClick = { },
                                                modifier = Modifier.bounceClick(endInteractionSource) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onEndSession(book)
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
                                            onClick = { },
                                            modifier = Modifier
                                                .align(Alignment.End)
                                                .bounceClick(resumeInteractionSource) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    bookToStartSession = book
                                                },
                                            interactionSource = resumeInteractionSource,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
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
                            val bookSessions = remember(book, sessions) { sessions.filter { it.bookId == book.id } }
                            val velocity = remember(bookSessions, sessions) { AnalyticsEngine.bookPagesPerHour(bookSessions, sessions) }
                            val speedText = remember(velocity) { AnalyticsEngine.formatReadingSpeed(velocity) }
                            val minutesLeft = remember(book, bookSessions, sessions) {
                                AnalyticsEngine.bookEstimatedMinutesLeft(book, bookSessions, sessions)
                            }
                            val etaText = remember(minutesLeft) { AnalyticsEngine.formatEstimatedTime(minutesLeft) }

                            if (speedText != null || etaText != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (speedText != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Filled.Speed,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    speedText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                    if (etaText != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Filled.Schedule,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = MaterialTheme.colorScheme.secondary
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    etaText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
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
                                            bookToDnf = book 
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                    ) { Text("Give up") }
                                    androidx.compose.material3.TextButton(
                                        onClick = { 
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            bookToFinish = book 
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                    ) { Text("Finish") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    bookToStartSession?.let { startBook ->
        StartSessionDialog(
            currentPage = startBook.currentPage,
            totalPages = startBook.totalPages,
            unitName = if (startBook.totalPages > 0) "Page" else "Unit",
            onDismiss = { bookToStartSession = null },
            onConfirm = { startPage ->
                onStartSession(startBook, startPage)
                bookToStartSession = null
            }
        )
    }

    bookToAddCustomProgress?.let { customProgressBook ->
        CustomProgressDialog(
            unitName = if (customProgressBook.totalPages > 0) "Pages" else "Units",
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

    bookToFinish?.let { finishBook ->
        FinishDialog(
            book = finishBook,
            onDismiss = { bookToFinish = null },
            onConfirm = { rating, review ->
                onFinish(finishBook, rating, review)
                bookToFinish = null
            }
        )
    }
    
    bookToDnf?.let { dnfBook ->
        DnfDialog(
            book = dnfBook,
            onDismiss = { bookToDnf = null },
            onConfirm = { percent, reason ->
                onDnf(dnfBook, percent, reason)
                bookToDnf = null
            }
        )
    }
    
    val activeReadingSeconds by com.example.booktracker.app.data.ServiceLocator.activeReadingSeconds.collectAsState()
    var summaryStartPage by remember { mutableStateOf(0) }
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val hasStreak = streak.currentStreak > 0
        Surface(
            shape = CircleShape,
            color = if (streak.goalMetToday) {
                Color(0xFFFF6D00).copy(alpha = 0.15f)
            } else if (hasStreak) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (streak.goalMetToday) Color(0xFFFF9800).copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = if (streak.goalMetToday) StreakFlame
                    else if (hasStreak) Color(0xFFFFB74D)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${streak.currentStreak}-day streak",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (streak.goalMetToday) Color(0xFFFF9800)
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // 7-day horizontal bubble bar (Mon-Sun)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val today = java.time.LocalDate.now()
            val currentDayOfWeek = today.dayOfWeek.value // 1 (Mon) to 7 (Sun)
            val monday = today.minusDays(currentDayOfWeek - 1L)
            
            for (i in 0..6) {
                val day = monday.plusDays(i.toLong())
                val isActive = streak.activeDays.contains(day)
                val isToday = day == today
                
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .then(
                            if (isActive) {
                                Modifier.background(
                                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                    ),
                                    shape = CircleShape
                                )
                            } else {
                                Modifier.background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = CircleShape
                                )
                            }
                        )
                        .then(
                            if (isToday) {
                                Modifier.border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.dayOfWeek.name.take(1),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isActive || isToday) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = backgroundAlpha),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
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
    onCurationClick: () -> Unit,
    onReadEpub: (Uri) -> Unit = {},
    onSearch: () -> Unit,
    onScan: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf<BookStatus?>(null) }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var showLoansOnly by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(LibraryViewMode.GRID) }
    val haptic = LocalHapticFeedback.current

    val epubPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onReadEpub(uri)
        }
    }
    
    val displayBooks = books.filter { 
        if (showLoansOnly) {
            val hasLoan = LoanEngine.activeLoan(it) != null
            val favoriteMatches = if (showFavoritesOnly) it.isFavorite else true
            return@filter hasLoan && favoriteMatches
        }
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
                null to "Backlog",
                BookStatus.READING to BookStatus.READING.label,
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

            androidx.compose.material3.FilterChip(
                selected = showLoansOnly,
                onClick = {
                    showLoansOnly = !showLoansOnly
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                label = { Text("🏛️ On Loan") },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCurationClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Curate TBR", maxLines = 1)
            }
            OutlinedButton(
                onClick = { epubPickerLauncher.launch("application/epub+zip") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Read EPUB", maxLines = 1)
            }
            androidx.compose.material3.FilterChip(
                selected = viewMode == LibraryViewMode.BOOKSHELF,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewMode = if (viewMode == LibraryViewMode.GRID) LibraryViewMode.BOOKSHELF else LibraryViewMode.GRID
                },
                label = { Text(if (viewMode == LibraryViewMode.BOOKSHELF) "3D Shelf" else "Grid") },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            )
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
                        Spacer(Modifier.height(24.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onScan,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Scan")
                            }
                            FilledTonalButton(
                                onClick = onSearch,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Search")
                            }
                        }
                    }
                }
            }
        } else {
            // Grid of Books
            val displayBooksWithStatus = if (selectedFilter == BookStatus.FINISHED) {
                books.filter { it.status == BookStatus.FINISHED.name }
            } else displayBooks

            if (viewMode == LibraryViewMode.BOOKSHELF) {
                VirtualBookshelfView(
                    books = displayBooksWithStatus,
                    onOpenBook = onOpenBook,
                    onStartReading = onStartReading
                )
            } else {
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
    }
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
    val isReading = book.status == BookStatus.READING.name
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .bounceClick(onClick = onClick)
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = if (isReading) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            BookCover(book.coverUrl, Modifier.heroSharedElement("cover-${book.id}").fillMaxSize())
            
            val activeLoan = remember(book.loanRecord) { LoanEngine.activeLoan(book) }
            if (activeLoan != null) {
                val loanStatus = remember(activeLoan) { LoanEngine.getLoanStatus(activeLoan) }
                LoanBadge(
                    status = loanStatus,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                )
            }

            var expanded by remember { androidx.compose.runtime.mutableStateOf(false) }
            val statusText = try {
                BookStatus.valueOf(book.status).label
            } catch (e: Exception) {
                ""
            }
            if (statusText.isNotEmpty()) {
                val badgeColor = when (book.status) {
                    BookStatus.READING.name -> MaterialTheme.colorScheme.primary
                    BookStatus.FINISHED.name -> MaterialTheme.colorScheme.tertiary
                    BookStatus.UP_NEXT.name -> MaterialTheme.colorScheme.secondary
                    BookStatus.SHORTLIST.name -> MaterialTheme.colorScheme.secondaryContainer
                    BookStatus.DNF.name -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val badgeTextColor = when (book.status) {
                    BookStatus.READING.name -> MaterialTheme.colorScheme.onPrimary
                    BookStatus.FINISHED.name -> MaterialTheme.colorScheme.onTertiary
                    BookStatus.UP_NEXT.name -> MaterialTheme.colorScheme.onSecondary
                    BookStatus.SHORTLIST.name -> MaterialTheme.colorScheme.onSecondaryContainer
                    BookStatus.DNF.name -> MaterialTheme.colorScheme.onError
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.92f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        val prefix = if (book.status == BookStatus.FINISHED.name) "🏆 " else ""
                        val pct = if (book.status == BookStatus.READING.name && book.totalPages > 0) {
                            " · ${((book.currentPage.toFloat() / book.totalPages) * 100).toInt()}%"
                        } else ""
                        Text(
                            "$prefix$statusText$pct",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeTextColor
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
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) }
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
            if (book.currentPage > 0 || book.status == BookStatus.FINISHED.name) {
                val progressFraction = if (book.status == BookStatus.FINISHED.name) 1f else {
                    val total = book.totalPages.takeIf { it > 0 } ?: 1
                    (book.currentPage.toFloat() / total).coerceIn(0f, 1f)
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
        BookStatus.SHORTLIST.name -> "Up Next"
        BookStatus.UP_NEXT.name -> "Start Reading"
        else -> null
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BookCover(
                book.coverUrl, 
                Modifier
                    .heroSharedElement("cover-${book.id}")
                    .size(width = 96.dp, height = 144.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                book.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (book.authors.isNotEmpty()) {
                Text(
                    book.authors.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            if (book.totalPages > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "${book.totalPages} pages",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            if (promoteLabel != null) {
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = { onPromote(book) },
                    modifier = Modifier.fillMaxWidth(),
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
    onConfirm: (Book) -> Unit,
    onGoToLibrary: () -> Unit = {}
) {
    val options = books.filter { it.status == BookStatus.READING.name || it.status == BookStatus.PAUSED.name || it.status == BookStatus.UP_NEXT.name || it.status == BookStatus.SHORTLIST.name }
        .sortedByDescending { it.lastUpdated }
    
    if (options.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("No Books") },
            text = { Text("Add a book to your library first to start a session.") },
            confirmButton = { 
                TextButton(onClick = onGoToLibrary) { Text("Go to Library") } 
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
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
                            .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
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
    onStop: () -> Unit,
    onDismiss: () -> Unit,
    onOpenFullTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
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
                    text = if (phase == TimerPhase.WORK) "DEEP FOCUS" else "BREAK",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary
                )
                val mins = timeLeftSeconds / 60
                val secs = timeLeftSeconds % 60
                Text(
                    text = String.format("%02d:%02d", mins, secs),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onStop) {
                    Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onPlayPause() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) androidx.compose.material.icons.Icons.Filled.Pause else androidx.compose.material.icons.Icons.Filled.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Play",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isRunning) "Pause" else "Resume")
                }
            }
        }
    }
}
