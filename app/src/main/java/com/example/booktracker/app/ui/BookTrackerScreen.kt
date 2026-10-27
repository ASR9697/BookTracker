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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import com.example.booktracker.app.ui.animations.staggeredEntrance
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewColumn
import com.example.booktracker.app.ui.animations.bounceOnPress
import com.example.booktracker.app.ui.animations.shimmer
import com.example.booktracker.app.ui.animations.Motion
import com.example.booktracker.app.ui.animations.NavMotion
import com.example.booktracker.app.ui.animations.TAB_ORDER
import com.example.booktracker.app.ui.animations.TOP_LEVEL_ROUTES
import com.example.booktracker.app.ui.animations.pagerDepth
import com.example.booktracker.app.ui.animations.rememberReduceMotion
import coil3.request.crossfade
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.StreakEngine
import com.example.booktracker.app.analytics.LoanEngine
import com.example.booktracker.app.ui.animations.ConfettiExplosion
import com.example.booktracker.app.ui.theme.StreakFlame
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookFormat
import com.example.booktracker.shared.models.Ownership
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Session
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class LibraryViewMode { GRID, BOOKSHELF }

/** The one destination that hosts the five main sections in a pager. */
private const val MAIN_ROUTE = "main"

/** The five main sections, in bottom-bar order. */
private val MainTabs = listOf(
    NavTab("home", "Home", Icons.Filled.AutoStories),
    NavTab("library", "Library", Icons.AutoMirrored.Filled.MenuBook),
    NavTab("memorize", "Notes", Icons.Filled.Style),
    NavTab("stats", "Stats", Icons.Filled.Insights),
    NavTab("profile", "Profile", Icons.Filled.Person)
)

enum class LibrarySort(val label: String) {
    RECENT("Recently updated"),
    TITLE("Title"),
    AUTHOR("Author"),
    ADDED("Date added"),
    PROGRESS("Progress"),
    RATING("Rating")
}

/**
 * Root of the app UI. Provides the reader's feel preferences to everything below:
 * [LocalAppFeel] for the app's own motion and haptics, and a silent
 * [LocalHapticFeedback] when haptics are off, so older screens that buzz through
 * Compose directly go quiet too.
 */
@Composable
fun BookTrackerApp(viewModel: BookTrackerViewModel, windowSizeClass: WindowSizeClass? = null) {
    val haptics by viewModel.hapticsEnabled.collectAsState()
    val reduceMotion by viewModel.reduceMotion.collectAsState()
    val systemHaptics = LocalHapticFeedback.current
    CompositionLocalProvider(
        com.example.booktracker.app.ui.animations.LocalAppFeel provides
            com.example.booktracker.app.ui.animations.AppFeel(reduceMotion = reduceMotion, haptics = haptics),
        LocalHapticFeedback provides if (haptics) systemHaptics else SilentHaptics
    ) {
        BookTrackerAppContent(viewModel, windowSizeClass)
    }
}

private object SilentHaptics : androidx.compose.ui.hapticfeedback.HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
private fun BookTrackerAppContent(viewModel: BookTrackerViewModel, windowSizeClass: WindowSizeClass? = null) {
    val books by viewModel.books.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val readingBooks = books
        .filter { it.status.equals(BookStatus.READING.name, ignoreCase = true) }
        .sortedByDescending { it.lastUpdated }
    var showAddDialog by remember { mutableStateOf(false) }
    // Hoisted so the Add book button can shrink once Home scrolls under it.
    val homeScroll = rememberScrollState()
    val homeAtTop by remember { derivedStateOf { homeScroll.value == 0 } }
    var pendingOutcome by remember { mutableStateOf<Pair<String, BookStatus>?>(null) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var initialGoalType by remember { mutableStateOf(GoalType.YEARLY_BOOKS) }
    val monthlyGoalPages by viewModel.monthlyGoalPages.collectAsState()
    val monthlyGoalBooks by viewModel.monthlyGoalBooks.collectAsState()
    val dailyGoal by viewModel.dailyGoal.collectAsState()
    val yearlyGoal by viewModel.yearlyGoal.collectAsState()


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
    val mainPager = androidx.compose.foundation.pager.rememberPagerState(pageCount = { TAB_ORDER.size })
    val navRoute = navBackStackEntry?.destination?.route ?: MAIN_ROUTE
    val onMain = navRoute == MAIN_ROUTE
    // On the main screen the visible section acts as the route, so everything keyed
    // on "home", "library"... (top bar, Add button, gear) keeps working.
    val currentRoute = if (onMain) TAB_ORDER[mainPager.currentPage] else navRoute

    val isTopLevelRoute = currentRoute in TOP_LEVEL_ROUTES
    val openScanner: () -> Unit = { navController.navigate("scanner") { launchSingleTop = true } }
    val tabScope = rememberCoroutineScope()
    val navigateTopLevel: (String) -> Unit = { route ->
        val page = TAB_ORDER.indexOf(route)
        if (page >= 0) {
            if (navController.currentDestination?.route != MAIN_ROUTE) navController.popBackStack(MAIN_ROUTE, inclusive = false)
            tabScope.launch { mainPager.animateScrollToPage(page, animationSpec = tween(Motion.LONG, easing = Motion.Emphasized)) }
        } else {
            navController.navigate(route)
        }
    }
    // Back from any other section returns to Home first, as it did before the pager.
    BackHandler(enabled = onMain && mainPager.currentPage != 0) {
        tabScope.launch { mainPager.animateScrollToPage(0, animationSpec = tween(Motion.LONG, easing = Motion.Emphasized)) }
    }
    // The shelf's preview card sits where the Add button would; the button steps aside for it.
    var libraryOverlayOpen by remember { mutableStateOf(false) }

    // A finished countdown opens the save screen wherever the reader is. The event
    // is replayed until consumed, so it also fires on returning to the app.
    LaunchedEffect(navController) {
        com.example.booktracker.app.data.ServiceLocator.timerExpiredEvents.collect {
            val bookId = com.example.booktracker.app.data.ServiceLocator.timerBookId.value
            com.example.booktracker.app.data.ServiceLocator.consumeTimerExpired()
            if (bookId != null && navController.currentDestination?.route != "session_save/{bookId}") {
                navController.navigate("session_save/$bookId") { launchSingleTop = true }
            }
        }
    }

    // A widget tap opens a screen: sections switch the pager, anything else goes on top of Main.
    val pendingRoute by viewModel.pendingRoute.collectAsState()
    LaunchedEffect(pendingRoute) {
        val route = pendingRoute ?: return@LaunchedEffect
        viewModel.consumeRoute()
        androidx.compose.runtime.withFrameNanos { } // let the nav graph attach first
        if (route in TAB_ORDER) {
            navigateTopLevel(route)
        } else {
            if (navController.currentDestination?.route != MAIN_ROUTE) navController.popBackStack(MAIN_ROUTE, inclusive = false)
            runCatching { navController.navigate(route) { launchSingleTop = true } }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showEndTimerDialog by remember { mutableStateOf(false) }
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

    val reduceMotion = rememberReduceMotion()
    var unlockedToShow by remember { mutableStateOf<List<com.example.booktracker.app.analytics.Badge>>(emptyList()) }
    LaunchedEffect(Unit) {
        viewModel.newUnlockEvents.collect { ids ->
            val unlocked = com.example.booktracker.app.analytics.BadgeEngine.BADGES.filter { it.id in ids }
            if (unlocked.isNotEmpty()) unlockedToShow = unlockedToShow + unlocked
        }
    }
    if (unlockedToShow.isNotEmpty()) {
        BadgeUnlockDialog(
            badges = unlockedToShow,
            onViewAll = {
                unlockedToShow = emptyList()
                navController.navigate("achievements") { launchSingleTop = true }
            },
            onDismiss = { unlockedToShow = emptyList() }
        )
    }

    Scaffold(
        topBar = {
            if (isTopLevelRoute) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            androidx.compose.ui.res.stringResource(com.example.booktracker.app.R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigate("search") }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search library")
                        }
                    },
                    actions = {
                        IconButton(onClick = openScanner) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan ISBN")
                        }
                        // Settings sit behind the gear on Profile.
                        androidx.compose.animation.AnimatedVisibility(visible = currentRoute == "profile") {
                            IconButton(onClick = { navController.navigate("settings") { launchSingleTop = true } }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Settings")
                            }
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
                val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
                val timerMode by viewModel.timerMode.collectAsState()
                val timerPhase by viewModel.timerPhase.collectAsState()
                val bottomNavHaptic = LocalHapticFeedback.current

                androidx.compose.animation.AnimatedVisibility(
                    visible = activeTimerBook != null &&
                        currentRoute.startsWith("focus/") != true &&
                        currentRoute.startsWith("session_") != true,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + fadeIn(tween(250)),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200))
                ) {
                    activeTimerBook?.let { book ->
                        TimerMiniPlayer(
                            book = book,
                            isRunning = timerIsRunning,
                            displaySeconds = if (timerMode == TimerMode.STOPWATCH) elapsedSeconds else timeLeftSeconds,
                            isCountdownDone = timerMode == TimerMode.COUNTDOWN && timeLeftSeconds <= 0,
                            label = when {
                                !timerIsRunning && (
                                    if (timerMode == TimerMode.STOPWATCH) elapsedSeconds == 0
                                    else timeLeftSeconds >= viewModel.countdownTotalSeconds()
                                ) -> "READY"
                                !timerIsRunning -> "PAUSED"
                                timerMode == TimerMode.STOPWATCH -> "READING"
                                timerPhase == TimerPhase.BREAK -> "BREAK"
                                else -> "DEEP FOCUS"
                            },
                            onPlayPause = { viewModel.setTimerRunning(!timerIsRunning) },
                            onSave = { navController.navigate("session_save/${book.id}") { launchSingleTop = true } },
                            onEnd = { showEndTimerDialog = true },
                            onOpenFullTimer = { navController.navigate("focus/${book.id}") { launchSingleTop = true } },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                if (isTopLevelRoute && windowSizeClass?.widthSizeClass != WindowWidthSizeClass.Expanded) {
                    FloatingNavBar(
                        tabs = MainTabs,
                        position = {
                            if (onMain) mainPager.currentPage + mainPager.currentPageOffsetFraction
                            else TAB_ORDER.indexOf(currentRoute).toFloat()
                        },
                        onSelect = navigateTopLevel
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            androidx.compose.animation.AnimatedVisibility(
                visible = currentRoute == "home" || (currentRoute == "library" && !libraryOverlayOpen),
                enter = androidx.compose.animation.scaleIn(Motion.press()) + fadeIn(),
                exit = androidx.compose.animation.scaleOut(tween(Motion.SHORT)) + fadeOut(tween(Motion.SHORT))
            ) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    expanded = currentRoute != "home" || homeAtTop,
                    icon = { Icon(Icons.Filled.Add, contentDescription = "Add book") },
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
                        selected = currentRoute == "home",
                        onClick = { navigateTopLevel("home") },
                        icon = { Icon(Icons.Filled.AutoStories, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationRailItem(
                        selected = currentRoute == "library",
                        onClick = { navigateTopLevel("library") },
                        icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Library") },
                        label = { Text("Library") }
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
                                        startDestination = MAIN_ROUTE,
                                        // Going deeper slides in from the side and back slides out,
                                        // which also drives Android's predictive-back peek.
                                        enterTransition = { NavMotion.enter(reduceMotion) },
                                        exitTransition = { NavMotion.exit(reduceMotion) },
                                        popEnterTransition = { NavMotion.popEnter(reduceMotion) },
                                        popExitTransition = { NavMotion.popExit(reduceMotion) }
                                    ) {
                                    composable(MAIN_ROUTE) {
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                            // The five sections side by side: swiping drags the next one into view,
                                            // and the bottom bar's bubble follows the same position.
                                            androidx.compose.foundation.pager.HorizontalPager(
                                                state = mainPager,
                                                beyondViewportPageCount = 1,
                                                key = { TAB_ORDER[it] },
                                                modifier = Modifier.fillMaxSize()
                                            ) { page ->
                                                Box(Modifier.fillMaxSize().pagerDepth(mainPager, page, reduceMotion)) {
                                                // Only the settled page may own cover animations; see LocalHeroEnabled.
                                                androidx.compose.runtime.CompositionLocalProvider(LocalHeroEnabled provides (page == mainPager.settledPage)) {
                                                when (TAB_ORDER[page]) {
                                                    "home" -> {
                                            val openSession by viewModel.openSession.collectAsState()
                                            val homeLayout by viewModel.homeLayout.collectAsState()
                                            val sessions by viewModel.completedSessions.collectAsState()
                                            val currentUserName by viewModel.userName.collectAsState()
                                            val homeReview by viewModel.reviewQueue.collectAsState()
                                            val homeReviewStates by viewModel.reviewStates.collectAsState()
                                            HomeDashboard(
                                                reviewDue = homeReview?.size ?: 0,
                                                reviewStarted = homeReviewStates.isNotEmpty(),
                                                onOpenReview = { navController.navigate("review") { launchSingleTop = true } },
                                                scrollState = homeScroll,
                                                layout = homeLayout,
                                                onEditHome = { navController.navigate("editHome") },
                                                onOpenOwnership = { navController.navigate("owned/${it.name}") },
                                                onAddNote = { book, page, text, type, images -> viewModel.addNote(book.id, page, text, type, images) },
                                                onChangeStatus = { book, status -> viewModel.changeStatus(book, status) },
                                                onOpenStreak = { navController.navigate("streak") { launchSingleTop = true } },
                                                books = books,
                                                readingBooks = readingBooks,
                                                sessions = sessions,
                                                openSession = openSession,
                                                streak = streak,
                                                userName = currentUserName,
                                                yearlyGoal = yearlyGoal,
                                                onOpenBook = { navController.navigate("book/${it.id}") },
                                                onOpenTimer = { navController.navigate("focus/${it.id}") },
                                                onLogPages = { b, delta ->
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
                                                onAddBook = { showAddDialog = true },
                                                onOpenLibrary = { navigateTopLevel("library") },
                                                onOpenCalendar = { navController.navigate("calendar") },
                                                onOpenBookshelf = { navController.navigate("bookshelf") },
                                                onOpenStats = { navigateTopLevel("stats") },
                                                onOpenFinished = { navController.navigate("finished") },
                                                onOpenWrapped = { navController.navigate("wrapped") },
                                                onEditGoal = { type ->
                                                    initialGoalType = type
                                                    showGoalDialog = true
                                                },
                                                banner = {
                                                    val urgentLoans = remember(books) { LoanEngine.urgentLoans(books) }
                                                    if (urgentLoans.isNotEmpty()) {
                                                        UrgentLoanBanner(
                                                            urgentLoans = urgentLoans,
                                                            onOpenBook = { navController.navigate("book/${it.id}") },
                                                            modifier = Modifier.padding(horizontal = 16.dp)
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                                    "library" -> {
                                                        LibraryScreen(
                                                            books = books,
                                                            onDelete = onDeleteWithUndo,
                                                            onOpenBook = { navController.navigate("book/${it.id}") },
                                                            onStartReading = { navController.navigate("focus/${it.id}") },
                                                            onCurationClick = { navController.navigate("tbr_swipe") },
                                                            onReadEpub = { uri -> 
                                                                navController.navigate("epubReader/${Uri.encode(uri.toString())}") 
                                                            },
                                                            onSearch = { navController.navigate("search") },
                                                            onScan = openScanner,
                                                            onChangeStatus = { book, status -> viewModel.changeStatus(book, status) },
                                                            onSaveNote = { book, page, text, type, images -> viewModel.addNote(book.id, page, text, type, images) },
                                                            onOverlayChange = { libraryOverlayOpen = it }
                                                        )
            
                                        }
                                                    "memorize" -> {
                                                        val notes by viewModel.allNotes.collectAsState()
                                                        val notesReview by viewModel.reviewQueue.collectAsState()
                                                        val wordList by viewModel.words.collectAsState()
                                                        MemorizeScreen(
                                                            reviewDue = notesReview?.size ?: 0,
                                                            wordCount = wordList.size,
                                                            onOpenReview = { navController.navigate("review") { launchSingleTop = true } },
                                                            onOpenWords = { navController.navigate("vocabulary") { launchSingleTop = true } },
                                                            notes = notes,
                                                            books = books,
                                                            onToggleFavorite = viewModel::toggleNoteFavorite,
                                                            onOpenBook = { navController.navigate("book/${it.id}") },
                                                            onUpdateNote = viewModel::updateNote,
                                                            onDeleteNote = viewModel::deleteNote
                                                        )
            
                                        }
                                                    "stats" -> {
                                                        val sessions by viewModel.completedSessions.collectAsState()
                                                        val goal by viewModel.dailyGoal.collectAsState()
                                                        val yearly by viewModel.yearlyGoal.collectAsState()
                                                        val monthlyPages by viewModel.monthlyGoalPages.collectAsState()
                                                        val monthlyBooks by viewModel.monthlyGoalBooks.collectAsState()
                                                        val dayStartsAtHour by viewModel.dayStartsAtHour.collectAsState()
                                                        AnalyticsScreen(
                                                            books = books,
                                                            sessions = sessions,
                                                            dailyGoal = goal,
                                                            yearlyGoal = yearly,
                                                            monthlyGoalPages = monthlyPages,
                                                            monthlyGoalBooks = monthlyBooks,
                                                            dayStartsAtHour = dayStartsAtHour,
                                                            onEditGoal = { targetGoalType ->
                                                                initialGoalType = targetGoalType
                                                                showGoalDialog = true
                                                            }
                                                        )
            
                                        }
                                                    "profile" -> {
                                                        val goal by viewModel.dailyGoal.collectAsState()
                                                        val sessions by viewModel.completedSessions.collectAsState()
                                                        val badgeStatuses by viewModel.badgeStatuses.collectAsState()
                                                        val streak by viewModel.streak.collectAsState()
                                                        val userName by viewModel.userName.collectAsState()
                                                        val allNotes by viewModel.allNotes.collectAsState()
                                                        val monthlyChallenge by viewModel.monthlyChallenge.collectAsState()
                                                        val yearlyGoal by viewModel.yearlyGoal.collectAsState()
                                                        val avatar by viewModel.avatar.collectAsState()

                                                        ProfileScreen(
                                                            monthlyChallenge = monthlyChallenge,
                                                            yearlyGoal = yearlyGoal,
                                                            avatar = avatar,
                                                            onSetAvatarPhoto = { uri ->
                                                                viewModel.setAvatarPhoto(uri) { ok ->
                                                                    if (!ok) scope.launch { snackbarHostState.showSnackbar("Couldn't read that photo") }
                                                                }
                                                            },
                                                            onSetAvatarCover = { viewModel.setAvatarCover(it.id) },
                                                            onClearAvatar = viewModel::clearAvatar,
                                                            userName = userName,
                                                            books = books,
                                                            sessions = sessions,
                                                            notes = allNotes,
                                                            streak = streak.currentStreak,
                                                            longestStreak = streak.longestStreak,
                                                            dayStartsAtHour = streak.dayStartsAtHour,
                                                            badgeStatuses = badgeStatuses,
                                                            onOpenAchievements = { navController.navigate("achievements") },
                                                            onOpenStreak = { navController.navigate("streak") { launchSingleTop = true } },
                                                            onOpenSettings = { navController.navigate("goals") },
                                                            onOpenHistory = { navController.navigate("history") },
                                                            onOpenFinished = { navController.navigate("finished") },
                                                            onOpenPlanner = { navController.navigate("planner") },
                                                            onOpenBook = { navController.navigate("book/${it.id}") },
                                                            onOpenWrapped = { navController.navigate("wrappedAnimated") },
                                                            onUpdateName = viewModel::setUserName
                                                        )
            
                                        }
                                                }
                                                }
                                                }
                                            }
                                        }
                                    }
                                    composable("editHome") {
                                        val homeLayout by viewModel.homeLayout.collectAsState()
                                        SubScreen(title = "Edit home screen", onBack = { navController.popBackStack() }) {
                                            EditHomeScreen(layout = homeLayout, onLayoutChange = viewModel::setHomeLayout)
                                        }
                                    }
                                    composable("owned/{ownership}") { entry ->
                                        val ownership = Ownership.fromStored(entry.arguments?.getString("ownership"), hasPurchase = false)
                                        val title = if (ownership == Ownership.GIFT) "Gifts" else ownership.label
                                        SubScreen(title = title, onBack = { navController.popBackStack() }) {
                                            val shelf = remember(books, ownership) {
                                                books.filter { it.ownership == ownership }.sortedByDescending { it.lastUpdated }
                                            }
                                            if (shelf.isEmpty()) {
                                                EmptyState(
                                                    icon = ownership.icon,
                                                    title = "Nothing here yet",
                                                    message = "Open any book and set its ownership to add it."
                                                )
                                            } else {
                                                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                                    columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 100.dp),
                                                    contentPadding = PaddingValues(16.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    items(shelf, key = { it.id }) { book ->
                                                        LibraryGridCard(
                                                            book = book,
                                                            onClick = { navController.navigate("book/${book.id}") },
                                                            onStartReading = { navController.navigate("focus/${book.id}") },
                                                            onDelete = { onDeleteWithUndo(book) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    composable("calendar") {
                                        val sessions by viewModel.completedSessions.collectAsState()
                                        val allNotes by viewModel.allNotes.collectAsState()
                                        val currentUserName by viewModel.userName.collectAsState()
                                        val dayStartsAtHour by viewModel.dayStartsAtHour.collectAsState()
                                        SubScreen(title = "Calendar", onBack = { navController.popBackStack() }) {
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
                                                },
                                                dayStartsAtHour = dayStartsAtHour
                                            )
                                        }
                                    }
                                    composable("bookshelf") {
                                        SubScreen(title = "Bookshelf", onBack = { navController.popBackStack() }) {
                                            VirtualBookshelfView(
                                                books = books,
                                                onOpenBook = { navController.navigate("book/${it.id}") },
                                                onStartReading = { navController.navigate("focus/${it.id}") },
                                                modifier = Modifier.fillMaxSize(),
                                                onChangeStatus = { book, status -> viewModel.changeStatus(book, status) }
                                            )
                                        }
                                    }
                                    composable("epubReader/{uri}") { backStackEntry ->
                                        val uriString = backStackEntry.arguments?.getString("uri") ?: ""
                                        com.example.booktracker.app.ui.reader.EpubReaderScreen(
                                            uriString = uriString,
                                            onNavigateUp = { navController.navigateUp() }
                                        )
                                    }
                                    composable("scanner") {
                                        ScannerScreen(
                                            onClose = { navController.popBackStack() },
                                            onBookConfirmed = { scanned, status, format ->
                                                viewModel.addScannedBook(scanned, status, format)
                                                navController.popBackStack()
                                                scope.launch {
                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                    snackbarHostState.showSnackbar("Added “${scanned.title}”")
                                                }
                                            },
                                            onAddManually = {
                                                navController.popBackStack()
                                                showAddDialog = true
                                            }
                                        )
                                    }
                                    composable("privacy") {
                                        PrivacyPolicyScreen(onBack = { navController.popBackStack() })
                                    }
                                    composable("achievements") {
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                            val badgeStatuses by viewModel.badgeStatuses.collectAsState()
                                            val monthlyChallenge by viewModel.monthlyChallenge.collectAsState()
                                            AchievementsScreen(
                                                statuses = badgeStatuses,
                                                monthlyChallenge = monthlyChallenge,
                                                onBack = { navController.popBackStack() }
                                            )
                                        }
                                    }
                                    composable("wrappedAnimated") {
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                            val completedSessions by viewModel.completedSessions.collectAsState()
                                            val currentStreak by viewModel.streak.collectAsState()
                                            val wrappedNotes by viewModel.allNotes.collectAsState()
                                            val wrappedName by viewModel.userName.collectAsState()
                                            WrappedScreen(
                                                books = books,
                                                sessions = completedSessions,
                                                notes = wrappedNotes,
                                                userName = wrappedName,
                                                dayStartsAtHour = currentStreak.dayStartsAtHour,
                                                onBack = { navController.popBackStack() }
                                            )
                                        }
                                    }
                                    composable("wrapped") {
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                            val completedSessions by viewModel.completedSessions.collectAsState()
                                            val currentStreak by viewModel.streak.collectAsState()
                                            val wrappedNotes by viewModel.allNotes.collectAsState()
                                            val wrappedName by viewModel.userName.collectAsState()
                                            WrappedScreen(
                                                books = books,
                                                sessions = completedSessions,
                                                notes = wrappedNotes,
                                                userName = wrappedName,
                                                dayStartsAtHour = currentStreak.dayStartsAtHour,
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
                                                            onAddNote = { page, text, type, images -> viewModel.addNote(bookId, page, text, type, images) },
                                                            onUpdateNote = viewModel::updateNote,
                                                            onDeleteNote = viewModel::deleteNote,
                                                            onToggleNoteFavorite = viewModel::toggleNoteFavorite,
                                                            onSaveDetails = viewModel::updateBookDetails,
                                                            onChangeStatus = { status ->
                                                                books.find { it.id == bookId }?.let { viewModel.changeStatus(it, status) }
                                                            },
                                                            onMarkDnf = { percent, reason ->
                                                                books.find { it.id == bookId }?.let { viewModel.markDnf(it, percent, reason) }
                                                            },
                                                            onLogSession = { startPage, endPage, minutes, endTime ->
                                                                viewModel.logManualSession(bookId, startPage, endPage, minutes, endTime)
                                                                scope.launch {
                                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                                    snackbarHostState.showSnackbar("Reading session logged")
                                                                }
                                                            },
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
                                                            onSetOwnership = { ownership ->
                                                                books.find { it.id == bookId }?.let { viewModel.setOwnership(it, ownership) }
                                                            },
                                                            onSetPurchases = { purchases ->
                                                                books.find { it.id == bookId }?.let { viewModel.setPurchases(it, purchases) }
                                                            },
                                                            onSetFinishBy = { finishBy ->
                                                                books.find { it.id == bookId }?.let { viewModel.setFinishBy(it, finishBy) }
                                                            },
                                                            onDelete = {
                                                                books.find { it.id == bookId }?.let { onDeleteWithUndo(it) }
                                                                navController.popBackStack()
                                                            },
                                                            onStartReading = {
                                                                navController.navigate("focus/$bookId")
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
                                                        val reminder by viewModel.reminder.collectAsState()

                                                        val hapticsOn by viewModel.hapticsEnabled.collectAsState()
                                                        val calmMotion by viewModel.reduceMotion.collectAsState()
                                                        val screenOn by viewModel.keepScreenOn.collectAsState()
                                                        SettingsScreen(
                                                            userName = userName,
                                                            dndDuringSession = dnd,
                                                            hapticsEnabled = hapticsOn,
                                                            onHapticsChange = viewModel::setHapticsEnabled,
                                                            reduceMotion = calmMotion,
                                                            onReduceMotionChange = viewModel::setReduceMotion,
                                                            keepScreenOn = screenOn,
                                                            onKeepScreenOnChange = viewModel::setKeepScreenOn,
                                                            themeMode = themeMode,
                                                            palette = appPalette,
                                                            onPaletteChange = viewModel::setAppPalette,
                                                            useDynamicColor = dynamicColor,
                                                            useAppLock = useAppLock,
                                                            onUserNameChange = viewModel::setUserName,
                                                            onDndChange = viewModel::setDndDuringSession,
                                                            onThemeModeChange = viewModel::setThemeMode,
                                                            onUseDynamicColorChange = viewModel::setUseDynamicColor,
                                                            onAppLockChange = viewModel::setUseAppLock,
                                                            onImportCsv = { uri, onDone -> viewModel.importCsv(context, uri, onDone) },
                                                            onImportBookmory = { uri, onDone -> viewModel.importBookmory(context, uri, onDone) },
                                                            onExportBackup = { uri, onDone -> viewModel.exportBackup(context, uri, onDone) },
                                                            onImportBackup = { uri, onDone -> viewModel.importBackup(context, uri, onDone) },
                                                            onOpenPrivacyPolicy = { navController.navigate("privacy") },
                                                            onEditHome = { navController.navigate("editHome") },
                                                            onBack = { navController.popBackStack() }
                                                        )
            
                                        }
                                    }
                                    composable("streak") {
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                            val streakSessions by viewModel.completedSessions.collectAsState()
                                            val streakInfo by viewModel.streak.collectAsState()
                                            HistoryScreen(
                                                books = books,
                                                sessions = streakSessions,
                                                onDeleteSession = viewModel::deleteSession,
                                                onUpdateSession = viewModel::updateSession,
                                                onBack = { navController.popBackStack() },
                                                title = "Your streak",
                                                header = { StreakHeader(streakInfo, modifier = Modifier.padding(bottom = 8.dp)) }
                                            )
                                        }
                                    }
                                    composable("goals") {
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                            val dailyGoal by viewModel.dailyGoal.collectAsState()
                                            val yearlyGoal by viewModel.yearlyGoal.collectAsState()
                                            val monthlyPages by viewModel.monthlyGoalPages.collectAsState()
                                            val monthlyBooks by viewModel.monthlyGoalBooks.collectAsState()
                                            val dayStart by viewModel.dayStartsAtHour.collectAsState()
                                            val reminder by viewModel.reminder.collectAsState()
                                            val goalStreak by viewModel.streak.collectAsState()
                                            val goalSessions by viewModel.completedSessions.collectAsState()
                                            val progress = remember(books, goalSessions, goalStreak) {
                                                val zone = java.time.ZoneId.systemDefault()
                                                val today = goalStreak.today
                                                val finished = books.filter {
                                                    it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) && it.completedAt > 0
                                                }.map { java.time.Instant.ofEpochMilli(it.completedAt).atZone(zone).toLocalDate() }
                                                GoalProgress(
                                                    pagesToday = goalStreak.pagesToday,
                                                    booksThisYear = finished.count { it.year == today.year },
                                                    pagesThisMonth = StreakEngine.pagesByDay(goalSessions, zone, goalStreak.dayStartsAtHour)
                                                        .filterKeys { it.year == today.year && it.month == today.month }.values.sum(),
                                                    booksThisMonth = finished.count { it.year == today.year && it.month == today.month }
                                                )
                                            }
                                            GoalsScreen(
                                                dailyGoal = dailyGoal,
                                                yearlyGoal = yearlyGoal,
                                                monthlyGoalPages = monthlyPages,
                                                monthlyGoalBooks = monthlyBooks,
                                                dayStartsAtHour = dayStart,
                                                reminder = reminder,
                                                progress = progress,
                                                onDailyGoalChange = viewModel::setDailyGoal,
                                                onYearlyGoalChange = viewModel::setYearlyGoal,
                                                onMonthlyGoalPagesChange = viewModel::setMonthlyGoalPages,
                                                onMonthlyGoalBooksChange = viewModel::setMonthlyGoalBooks,
                                                onDayStartsAtHourChange = viewModel::setDayStartsAtHour,
                                                onReminderChange = viewModel::setReminder,
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
                                                                    onSaveSession = {
                                                                        navController.navigate("session_save/$bookId") { launchSingleTop = true }
                                                                    },
                                                                    onAddNote = { page, text, type, images -> viewModel.addNote(bookId, page, text, type, images) }
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
                                                                sessionStartPage = session?.startPage ?: book.currentPage,
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
                                                                            popUpTo(MAIN_ROUTE) { inclusive = false }
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
                                                        val resultNotes by remember(bookId) { viewModel.notesFor(bookId) }
                                                            .collectAsState(initial = emptyList())
                                                        val current = result
                                                        if (book != null && current != null) {
                                                            SessionResultScreen(
                                                                book = book,
                                                                result = current,
                                                                allSessions = allSessions,
                                                                notes = resultNotes,
                                                                onDone = {
                                                                    viewModel.clearSessionResult()
                                                                    navController.popBackStack("home", inclusive = false)
                                                                }
                                                            )
                                                        }
            
                                        }
                                    }
                                    composable("review") {
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                            val queue by viewModel.reviewQueue.collectAsState()
                                            val reviewNotes by viewModel.allNotes.collectAsState()
                                            val reviewWords by viewModel.words.collectAsState()
                                            val states by viewModel.reviewStates.collectAsState()
                                            ReviewScreen(
                                                queue = queue,
                                                notes = reviewNotes,
                                                words = reviewWords,
                                                books = books,
                                                states = states,
                                                today = streak.today.toEpochDay(),
                                                onGrade = viewModel::gradeReview,
                                                onOpenBook = { navController.navigate("book/${it.id}") },
                                                onBack = { navController.popBackStack() }
                                            )
                                        }
                                    }
                                    composable("vocabulary") {
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                            val vocab by viewModel.words.collectAsState()
                                            val queue by viewModel.reviewQueue.collectAsState()
                                            VocabularyScreen(
                                                words = vocab,
                                                books = books,
                                                dueCount = queue.orEmpty().count { it.kind == com.example.booktracker.shared.models.ReviewKind.WORD },
                                                onSave = viewModel::saveWord,
                                                onDelete = viewModel::deleteWord,
                                                onReview = { navController.navigate("review") { launchSingleTop = true } },
                                                onBack = { navController.popBackStack() }
                                            )
                                        }
                                    }
                                    composable("shelfScan") {
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                            ShelfScanScreen(
                                                library = books,
                                                onAdd = { found, status, owned ->
                                                    viewModel.addShelfBooks(found, status, owned)
                                                    navController.popBackStack()
                                                    scope.launch {
                                                        snackbarHostState.currentSnackbarData?.dismiss()
                                                        snackbarHostState.showSnackbar(if (found.size == 1) "Added 1 book" else "Added ${found.size} books")
                                                    }
                                                },
                                                onBack = { navController.popBackStack() }
                                            )
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
                    openScanner()
                },
                onScanShelf = {
                    showAddDialog = false
                    navController.navigate("shelfScan") { launchSingleTop = true }
                }
            )
        }
    }


    if (showEndTimerDialog) {
        val timerBook by viewModel.activeTimerBook.collectAsState()
        val endingBook = timerBook
        if (endingBook == null) {
            showEndTimerDialog = false
        } else {
            AlertDialog(
                onDismissRequest = { showEndTimerDialog = false },
                title = { Text("End this reading session?") },
                text = { Text("Save it to keep the time and pages in your stats, or discard it.") },
                confirmButton = {
                    TextButton(onClick = {
                        showEndTimerDialog = false
                        viewModel.setTimerRunning(false)
                        navController.navigate("session_save/${endingBook.id}") { launchSingleTop = true }
                    }) { Text("Save") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { showEndTimerDialog = false }) { Text("Keep reading") }
                        TextButton(onClick = {
                            showEndTimerDialog = false
                            viewModel.discardOpenSession(endingBook.id)
                        }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
                    }
                }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

/** This week (Mon–Sun) as bubbles, filled on days with any reading. */
@Composable
fun WeekBubbles(streak: StreakEngine.StreakInfo, modifier: Modifier = Modifier) {
    val today = streak.today
    val monday = today.minusDays(today.dayOfWeek.value - 1L)
    val locale = java.util.Locale.getDefault()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0..6) {
            val day = monday.plusDays(i.toLong())
            val isActive = streak.activeDays.contains(day)
            val isToday = day == today
            val isFuture = day.isAfter(today)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .then(
                            if (isActive) {
                                Modifier.background(
                                    brush = Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                    ),
                                    shape = CircleShape
                                )
                            } else {
                                Modifier.background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (isFuture) 0.5f else 1f),
                                    shape = CircleShape
                                )
                            }
                        )
                        .then(
                            if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive) {
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = day.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun LibraryScreen(
    books: List<Book>,
    onDelete: (Book) -> Unit,
    onOpenBook: (Book) -> Unit,
    onStartReading: (Book) -> Unit,
    onCurationClick: () -> Unit,
    onReadEpub: (Uri) -> Unit = {},
    onSearch: () -> Unit,
    onScan: () -> Unit,
    onChangeStatus: (Book, BookStatus) -> Unit = { _, _ -> },
    onSaveNote: (Book, Int, String, com.example.booktracker.shared.models.NoteType, List<String>) -> Unit = { _, _, _, _, _ -> },
    onOverlayChange: (Boolean) -> Unit = {}
) {
    var noteFor by remember { mutableStateOf<Book?>(null) }
    val gridEntrance = com.example.booktracker.app.ui.animations.rememberEntranceState()
    noteFor?.let { book ->
        NoteSheet(
            suggestedPage = book.currentPage,
            totalPages = book.totalPages,
            pageLabel = com.example.booktracker.app.format.FormatAdaptabilityLayer.getDisplayUnit(book).trimEnd('s'),
            draftKey = book.id,
            onDismiss = { noteFor = null },
            onConfirm = { page, text, type, images ->
                onSaveNote(book, page, text, type, images)
                noteFor = null
            },
            onConfirmAndAddAnother = { page, text, type, images -> onSaveNote(book, page, text, type, images) }
        )
    }
    var selectedFilter by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<BookStatus?>(null) }
    var showFavoritesOnly by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showLoansOnly by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var selectedCollection by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var sortOrder by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(LibrarySort.RECENT) }
    var viewMode by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(LibraryViewMode.GRID) }
    var selectedFormat by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<BookFormat?>(null) }
    var showFormatMenu by remember { mutableStateOf(false) }
    var selectedOwnership by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Ownership?>(null) }
    var showOwnershipMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showCollectionMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val epubPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onReadEpub(uri)
        }
    }

    val collections = remember(books) {
        books.flatMap { it.classification.collections }.filter { it.isNotBlank() }.distinct().sorted()
    }
    if (selectedCollection != null && selectedCollection !in collections) selectedCollection = null
    val formats = remember(books) { BookFormat.entries.filter { f -> books.any { it.format == f } } }
    if (selectedFormat != null && selectedFormat !in formats) selectedFormat = null
    val ownershipCounts = remember(books) {
        SelectableOwnerships.associateWith { o -> books.count { it.ownership == o } }.filterValues { it > 0 }
    }
    if (selectedOwnership != null && selectedOwnership !in ownershipCounts) selectedOwnership = null

    val displayBooks = remember(books, selectedFilter, showFavoritesOnly, showLoansOnly, selectedCollection, selectedFormat, selectedOwnership, sortOrder) {
        books.filter { book ->
            val statusMatches = when (selectedFilter) {
                null -> true
                BookStatus.TO_READ -> book.status.equals(BookStatus.TO_READ.name, ignoreCase = true) || book.status.equals("BACKLOG", ignoreCase = true)
                BookStatus.DNF -> book.status.equals(BookStatus.DNF.name, ignoreCase = true) || book.status.equals(BookStatus.ABANDONED.name, ignoreCase = true)
                else -> book.status.equals(selectedFilter?.name, ignoreCase = true)
            }
            val favoriteMatches = !showFavoritesOnly || book.isFavorite
            val loanMatches = !showLoansOnly || LoanEngine.activeLoan(book) != null
            val collectionMatches = selectedCollection == null || selectedCollection in book.classification.collections
            val formatMatches = selectedFormat == null || book.format == selectedFormat
            val ownershipMatches = selectedOwnership == null || book.ownership == selectedOwnership
            statusMatches && favoriteMatches && loanMatches && collectionMatches && formatMatches && ownershipMatches
        }.let { list ->
            when (sortOrder) {
                LibrarySort.RECENT -> list.sortedByDescending { it.lastUpdated }
                LibrarySort.TITLE -> list.sortedBy { it.title.lowercase() }
                // Surname order; books without an author sort last.
                LibrarySort.AUTHOR -> list.sortedWith(
                    compareBy<Book> { it.authors.firstOrNull()?.trim()?.substringAfterLast(" ")?.lowercase() ?: "￿" }
                        .thenBy { it.title.lowercase() }
                )
                LibrarySort.ADDED -> list.sortedByDescending { it.dateAdded }
                LibrarySort.PROGRESS -> list.sortedByDescending {
                    if (it.totalPages > 0) it.currentPage.toFloat() / it.totalPages else 0f
                }
                LibrarySort.RATING -> list.sortedByDescending { it.overallRating ?: -1f }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                null to "All (${books.size})",
                BookStatus.READING to "${BookStatus.READING.label} (${books.count { it.status.equals(BookStatus.READING.name, ignoreCase = true) }})",
                BookStatus.TO_READ to "${BookStatus.TO_READ.label} (${books.count { it.status.equals(BookStatus.TO_READ.name, ignoreCase = true) || it.status.equals("BACKLOG", ignoreCase = true) }})",
                BookStatus.SHORTLIST to "${BookStatus.SHORTLIST.label} (${books.count { it.status.equals(BookStatus.SHORTLIST.name, ignoreCase = true) }})",
                BookStatus.UP_NEXT to "${BookStatus.UP_NEXT.label} (${books.count { it.status.equals(BookStatus.UP_NEXT.name, ignoreCase = true) }})",
                BookStatus.PAUSED to "${BookStatus.PAUSED.label} (${books.count { it.status.equals(BookStatus.PAUSED.name, ignoreCase = true) }})",
                BookStatus.FINISHED to "${BookStatus.FINISHED.label} (${books.count { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) }})",
                BookStatus.DNF to "${BookStatus.DNF.label} (${books.count { it.status.equals(BookStatus.DNF.name, ignoreCase = true) || it.status.equals(BookStatus.ABANDONED.name, ignoreCase = true) }})"
            )

            filters.forEach { (status, label) ->
                val isSelected = selectedFilter == status
                TickFilterChip(
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

            TickFilterChip(
                selected = showFavoritesOnly,
                onClick = { showFavoritesOnly = !showFavoritesOnly },
                label = { Text("Favorites") },
                leadingIcon = { Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                shape = CircleShape
            )

            TickFilterChip(
                selected = showLoansOnly,
                onClick = {
                    showLoansOnly = !showLoansOnly
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                label = { Text("On loan") },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = CircleShape
            )

            // Appears once the reader has marked any book owned, gifted or wishlisted.
            if (ownershipCounts.isNotEmpty()) {
                Box {
                    TickFilterChip(
                        selected = selectedOwnership != null,
                        onClick = { showOwnershipMenu = true },
                        label = { Text(selectedOwnership?.label ?: "Ownership") },
                        leadingIcon = selectedOwnership?.let { o ->
                            { Icon(o.icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        shape = CircleShape
                    )
                    DropdownMenu(expanded = showOwnershipMenu, onDismissRequest = { showOwnershipMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Any ownership") },
                            onClick = {
                                selectedOwnership = null
                                showOwnershipMenu = false
                            }
                        )
                        ownershipCounts.forEach { (ownership, count) ->
                            DropdownMenuItem(
                                text = { Text("${ownership.label} ($count)") },
                                leadingIcon = { Icon(ownership.icon, contentDescription = null) },
                                onClick = {
                                    selectedOwnership = ownership
                                    showOwnershipMenu = false
                                },
                                trailingIcon = {
                                    if (ownership == selectedOwnership) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
            }

            // Only worth offering once the library mixes formats.
            if (formats.size > 1) {
                Box {
                    TickFilterChip(
                        selected = selectedFormat != null,
                        onClick = { showFormatMenu = true },
                        label = { Text(selectedFormat?.name ?: "Format") },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        shape = CircleShape
                    )
                    DropdownMenu(expanded = showFormatMenu, onDismissRequest = { showFormatMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("All formats") },
                            onClick = {
                                selectedFormat = null
                                showFormatMenu = false
                            }
                        )
                        formats.forEach { format ->
                            DropdownMenuItem(
                                text = { Text("${format.name} (${books.count { it.format == format }})") },
                                onClick = {
                                    selectedFormat = format
                                    showFormatMenu = false
                                },
                                trailingIcon = {
                                    if (format == selectedFormat) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                androidx.compose.material3.AssistChip(
                    onClick = { showSortMenu = true },
                    label = { Text(sortOrder.label, maxLines = 1) },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    shape = CircleShape
                )
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    LibrarySort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                sortOrder = option
                                showSortMenu = false
                            },
                            trailingIcon = {
                                if (option == sortOrder) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }
            if (collections.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    TickFilterChip(
                        selected = selectedCollection != null,
                        onClick = { showCollectionMenu = true },
                        label = {
                            Text(
                                selectedCollection ?: "Collections",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        shape = CircleShape
                    )
                    DropdownMenu(expanded = showCollectionMenu, onDismissRequest = { showCollectionMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("All collections") },
                            onClick = {
                                selectedCollection = null
                                showCollectionMenu = false
                            }
                        )
                        collections.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedCollection = name
                                    showCollectionMenu = false
                                },
                                trailingIcon = {
                                    if (name == selectedCollection) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewMode = if (viewMode == LibraryViewMode.GRID) LibraryViewMode.BOOKSHELF else LibraryViewMode.GRID
            }) {
                Icon(
                    if (viewMode == LibraryViewMode.GRID) Icons.Filled.ViewColumn else Icons.Filled.GridView,
                    contentDescription = if (viewMode == LibraryViewMode.GRID) "Show as bookshelf" else "Show as grid"
                )
            }
            // Less common actions get words, not unexplained icons.
            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More library actions")
                }
                DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Sort your to-read pile") },
                        leadingIcon = { Icon(Icons.Filled.Style, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            onCurationClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Open an EPUB file") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            epubPickerLauncher.launch("application/epub+zip")
                        }
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (displayBooks.isEmpty()) {
                if (books.isEmpty()) {
                    // Library is completely empty
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
                    // Filter resulted in no books matching
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
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    "No Matching Books", 
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "No books match the selected filter criteria. Clear filters to view your entire collection.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        selectedFilter = null
                                        showFavoritesOnly = false
                                        showLoansOnly = false
                                    }
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Reset Filters")
                                }
                            }
                        }
                    }
                }
            } else {
                // Grid or Shelf of Books
                if (viewMode == LibraryViewMode.BOOKSHELF) {
                    VirtualBookshelfView(
                        books = displayBooks,
                        onOpenBook = onOpenBook,
                        onStartReading = onStartReading,
                        onChangeStatus = onChangeStatus,
                        onSelectionChange = onOverlayChange,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        // Three across on a phone, more on wider screens.
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(displayBooks, key = { _, book -> book.id }) { index, book ->
                            LibraryGridCard(
                                book = book,
                                modifier = Modifier
                                    .animateItem()
                                    .staggeredEntrance(gridEntrance, index),
                                onClick = { onOpenBook(book) },
                                onStartReading = { onStartReading(book) },
                                onDelete = { onDelete(book) },
                                onChangeStatus = { status -> onChangeStatus(book, status) },
                                onAddNote = { noteFor = book }
                            )
                        }
                    }
                }
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
        // A shimmer holds the cover's place while it loads, then the image fades in over it.
        var state by remember(url) { mutableStateOf<coil3.compose.AsyncImagePainter.State>(coil3.compose.AsyncImagePainter.State.Empty) }
        val context = coil3.compose.LocalPlatformContext.current
        val request = remember(url, context) {
            coil3.request.ImageRequest.Builder(context).data(url).crossfade(true).build()
        }
        Box(
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is coil3.compose.AsyncImagePainter.State.Loading, coil3.compose.AsyncImagePainter.State.Empty ->
                    Box(Modifier.matchParentSize().shimmer())
                is coil3.compose.AsyncImagePainter.State.Error ->
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Unit
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                onState = { state = it },
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        }
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

/** A plain back-arrow screen for views reached from a Home tile. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubScreen(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) { content() }
    }
}

@Composable
fun TimerMiniPlayer(
    book: Book,
    isRunning: Boolean,
    displaySeconds: Int,
    isCountdownDone: Boolean,
    label: String,
    onPlayPause: () -> Unit,
    onSave: () -> Unit,
    onEnd: () -> Unit,
    onOpenFullTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        onClick = onOpenFullTimer
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCover(book.coverUrl, Modifier.size(width = 36.dp, height = 52.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isCountdownDone) "TIME'S UP" else label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatElapsed(displaySeconds * 1000L),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    book.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onEnd) {
                Icon(Icons.Filled.Close, contentDescription = "End session", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isCountdownDone) {
                FilledTonalButton(onClick = onSave) { Text("Save") }
            } else {
                androidx.compose.material3.FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isRunning) "Pause timer" else "Resume timer"
                    )
                }
            }
        }
    }
}
