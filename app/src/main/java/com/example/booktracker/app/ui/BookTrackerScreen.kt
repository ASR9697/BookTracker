package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import androidx.activity.compose.BackHandler
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.animation.core.animate
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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Home
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import coil3.compose.AsyncImage
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewColumn
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
import kotlin.math.abs
import kotlin.math.roundToInt

enum class LibraryViewMode { GRID, BOOKSHELF }

/** The one destination that hosts the five main sections in a pager. */
private const val MAIN_ROUTE = "main"

/** Scroll (px per frame) that counts as meaning it, for hiding or showing the bottom bar. */
private const val NAV_HIDE_SLOP = 2f
// `page` is optional: the timer passes its live page, every other entry uses the book's.
private const val SESSION_SAVE_ROUTE = "session_save/{bookId}?page={page}"
/** How soon after one session another must start for the save screen to offer joining them. */
private const val JOIN_WINDOW_MILLIS = 30 * 60_000L

/** Not a screen: asks the app to open the Add book dialog (the Ctrl+N shortcut). */
const val ADD_BOOK_ACTION = "action/addBook"

/** The five main sections, in bottom-bar order. */
private val MainTabs = listOf(
    NavTab("home", "Home", Icons.Rounded.Home),
    NavTab("library", "Library", Icons.AutoMirrored.Rounded.LibraryBooks),
    NavTab("memorize", "Notes", Icons.Rounded.EditNote),
    NavTab("stats", "Stats", Icons.Rounded.Insights),
    NavTab("profile", "Profile", Icons.Rounded.Person)
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
fun BookTrackerApp(viewModel: BookTrackerViewModel) {
    val haptics by viewModel.hapticsEnabled.collectAsState()
    val reduceMotion by viewModel.reduceMotion.collectAsState()
    val systemHaptics = LocalHapticFeedback.current
    // The window's width, not the screen's: split-screen on a tablet gets the phone layout.
    val widthTier = WidthTier.forWidth(androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp)
    val prefs by viewModel.prefs.collectAsState()
    CompositionLocalProvider(
        LocalAppPrefs provides prefs,
        com.example.booktracker.app.ui.animations.LocalAppFeel provides
            com.example.booktracker.app.ui.animations.AppFeel(reduceMotion = reduceMotion, haptics = haptics),
        LocalHapticFeedback provides if (haptics) systemHaptics else SilentHaptics,
        LocalWidthTier provides widthTier
    ) {
        BookTrackerAppContent(viewModel)
    }
}

private object SilentHaptics : androidx.compose.ui.hapticfeedback.HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
private fun BookTrackerAppContent(viewModel: BookTrackerViewModel) {
    val books by viewModel.books.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val readingBooks = books
        .filter { it.status.equals(BookStatus.READING.name, ignoreCase = true) }
        .sortedByDescending { it.lastUpdated }
    var showAddDialog by remember { mutableStateOf(false) }
    var addQuery by remember { mutableStateOf("") }
    // Hoisted so Home keeps its place while you visit other sections.
    val homeScroll = rememberScrollState()
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
    // The section a swipe or tap has landed on; currentRoute flips halfway through the drag.
    val settledRoute = if (onMain) TAB_ORDER[mainPager.settledPage] else navRoute

    val isTopLevelRoute = currentRoute in TOP_LEVEL_ROUTES
    // Phones: floating bottom bar. Tablets: a rail. Big windows: a labelled side drawer.
    val widthTier = LocalWidthTier.current
    val showBottomNav = isTopLevelRoute && widthTier == WidthTier.COMPACT
    val showRail = isTopLevelRoute && (widthTier == WidthTier.MEDIUM || widthTier == WidthTier.EXPANDED)
    val showDrawer = isTopLevelRoute && widthTier == WidthTier.LARGE
    val openScanner: () -> Unit = { navController.navigate("scanner") { launchSingleTop = true } }
    val tabScope = rememberCoroutineScope()
    // A far jump (Profile to Home) lands next to the target and slides only the last
    // step, so the sections in between are never built just to be scrolled past.
    val showSection: (Int) -> Unit = { page ->
        tabScope.launch {
            val from = mainPager.currentPage
            if (abs(page - from) > 1) mainPager.scrollToPage(if (page > from) page - 1 else page + 1)
            mainPager.animateScrollToPage(page, animationSpec = tween(Motion.LONG, easing = Motion.Emphasized))
        }
    }
    val navigateTopLevel: (String) -> Unit = { route ->
        val page = TAB_ORDER.indexOf(route)
        if (page >= 0) {
            if (navController.currentDestination?.route != MAIN_ROUTE) navController.popBackStack(MAIN_ROUTE, inclusive = false)
            showSection(page)
        } else {
            navController.navigate(route)
        }
    }
    // Back from any other section returns to Home first, as it did before the pager.
    BackHandler(enabled = onMain && mainPager.currentPage != 0) {
        showSection(0)
    }
    // The shelf's preview card sits where the Add button would; the button steps aside for it.
    var libraryOverlayOpen by remember { mutableStateOf(false) }
    // Tablets show a tapped book beside the library instead of on top of it.
    val libraryListDetail = widthTier.atLeastExpanded
    var libraryPaneBookId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val openBookFromLibrary: (Book) -> Unit = { book ->
        if (libraryListDetail) libraryPaneBookId = book.id else navController.navigate("book/${book.id}")
    }

    // A finished countdown opens the save screen wherever the reader is. The event
    // is replayed until consumed, so it also fires on returning to the app.
    LaunchedEffect(navController) {
        com.example.booktracker.app.data.ServiceLocator.timerExpiredEvents.collect {
            val bookId = com.example.booktracker.app.data.ServiceLocator.timerBookId.value
            com.example.booktracker.app.data.ServiceLocator.consumeTimerExpired()
            if (bookId != null && navController.currentDestination?.route != SESSION_SAVE_ROUTE) {
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
        if (route == ADD_BOOK_ACTION) {
            showAddDialog = true
        } else if (route in TAB_ORDER) {
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

    // The bottom bar floats over the sections on phones: it slips away while you scroll
    // down through a section and comes back as soon as you scroll up or change section.
    var navHidden by remember { mutableStateOf(false) }
    LaunchedEffect(settledRoute) { navHidden = false }
    val navHider = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (consumed.y < -NAV_HIDE_SLOP) navHidden = true
                else if (consumed.y > NAV_HIDE_SLOP) navHidden = false
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }
    val navSlide by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (navHidden && showBottomNav) 1f else 0f,
        animationSpec = if (reduceMotion) tween(Motion.SHORT) else tween(Motion.MEDIUM, easing = Motion.Emphasized),
        label = "navSlide"
    )
    val reviewDueForTab by viewModel.reviewQueue.collectAsState()
    val avatarForTab by viewModel.avatar.collectAsState()
    val nameForTab by viewModel.userName.collectAsState()
    val navTabs = MainTabs.map { tab ->
        when (tab.route) {
            "memorize" -> tab.copy(badge = (reviewDueForTab?.size ?: 0) > 0)
            "profile" -> tab.copy(image = { modifier ->
                ProfileAvatar(avatarForTab, nameForTab, books, modifier, initialStyle = MaterialTheme.typography.labelLarge)
            })
            else -> tab
        }
    }

    Scaffold(
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().graphicsLayer { translationY = navSlide * size.height }) {
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

                if (showBottomNav) {
                    FloatingNavBar(
                        tabs = navTabs,
                        position = {
                            if (onMain) mainPager.currentPage + mainPager.currentPageOffsetFraction
                            else TAB_ORDER.indexOf(currentRoute).toFloat()
                        },
                        onSelect = navigateTopLevel,
                        // Add book rides beside the bar where adding makes sense.
                        onAdd = { showAddDialog = true }.takeIf {
                            currentRoute == "home" || (currentRoute == "library" && !libraryOverlayOpen)
                        }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
        // Sections draw their own headers under the status bar, and on phones run on under
        // the floating bottom bar, leaving room for it at the end of their scroll.
        val navClearance = if (showBottomNav) innerPadding.calculateBottomPadding() else 0.dp
        Row(
            modifier = Modifier.fillMaxSize().padding(
                start = innerPadding.calculateStartPadding(layoutDirection),
                top = if (onMain) 0.dp else innerPadding.calculateTopPadding(),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = if (showBottomNav) 0.dp else innerPadding.calculateBottomPadding()
            )
        ) {
            if (showRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f),
                    header = {
                        androidx.compose.material3.FloatingActionButton(
                            onClick = { showAddDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add book")
                        }
                    }
                ) {
                    Spacer(Modifier.weight(1f))
                    MainTabs.forEach { tab ->
                        NavigationRailItem(
                            selected = currentRoute == tab.route,
                            onClick = { navigateTopLevel(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
            if (showDrawer) {
                androidx.compose.material3.PermanentDrawerSheet(
                    modifier = Modifier.width(248.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
                        ExtendedFloatingActionButton(
                            onClick = { showAddDialog = true },
                            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                            text = { Text("Add book") },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 20.dp)
                        )
                        MainTabs.forEach { tab ->
                            androidx.compose.material3.NavigationDrawerItem(
                                selected = currentRoute == tab.route,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                        androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        androidx.compose.material3.NavigationDrawerItem(
                            selected = false,
                            onClick = { navController.navigate("review") { launchSingleTop = true } },
                            icon = { Icon(Icons.Rounded.Style, contentDescription = null) },
                            label = { Text("Daily review") }
                        )
                        androidx.compose.material3.NavigationDrawerItem(
                            selected = false,
                            onClick = { navController.navigate("settings") { launchSingleTop = true } },
                            icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                            label = { Text("Settings") }
                        )
                    }
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
                                                // Home reaches up under the bar; the rest start below it.
                                                Box(
                                                    Modifier
                                                        .fillMaxSize()
                                                        .pagerDepth(mainPager, page, reduceMotion, MaterialTheme.colorScheme.background)
                                                        .nestedScroll(navHider)
                                                ) {
                                                CompositionLocalProvider(LocalNavBarClearance provides navClearance) {
                                                // Only the settled page may own cover animations; see LocalHeroEnabled.
                                                CompositionLocalProvider(LocalHeroEnabled provides (page == mainPager.settledPage)) {
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
                                                onSearch = { navController.navigate("search") },
                                                layout = homeLayout,
                                                onEditHome = { navController.navigate("editHome") },
                                                onOpenOwnership = {
                                                    navController.navigate(if (it == Ownership.WISHLIST) "wishlist" else "owned/${it.name}")
                                                },
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
                                                        val library: @Composable () -> Unit = {
                                                            LibraryScreen(
                                                                books = books,
                                                                onDelete = onDeleteWithUndo,
                                                                onOpenBook = openBookFromLibrary,
                                                                onStartReading = { navController.navigate("focus/${it.id}") },
                                                                onCurationClick = { navController.navigate("tbr_swipe") },
                                                                onReadEpub = { uri ->
                                                                    navController.navigate("epubReader/${Uri.encode(uri.toString())}")
                                                                },
                                                                onSearch = { navController.navigate("search") },
                                                                onScan = openScanner,
                                                                onChangeStatus = { book, status -> viewModel.changeStatus(book, status) },
                                                                onSaveNote = { book, page, text, type, images -> viewModel.addNote(book.id, page, text, type, images) },
                                                                onOverlayChange = { libraryOverlayOpen = it },
                                                                onOpenWishlist = { navController.navigate("wishlist") { launchSingleTop = true } },
                                                                onToggleWishlist = { book ->
                                                                    val adding = book.ownership != Ownership.WISHLIST
                                                                    viewModel.setOwnership(book, if (adding) Ownership.WISHLIST else Ownership.NONE)
                                                                    scope.launch {
                                                                        snackbarHostState.currentSnackbarData?.dismiss()
                                                                        snackbarHostState.showSnackbar(if (adding) "Added to your wishlist" else "Removed from your wishlist")
                                                                    }
                                                                }
                                                            )
                                                        }
                                                        if (!libraryListDetail) {
                                                            library()
                                                        } else {
                                                            // A book that was deleted (or never loaded) leaves the pane empty.
                                                            val paneBookId = libraryPaneBookId?.takeIf { id -> books.any { it.id == id } }
                                                            BackHandler(enabled = paneBookId != null && page == mainPager.currentPage) {
                                                                libraryPaneBookId = null
                                                            }
                                                            Row(modifier = Modifier.fillMaxSize()) {
                                                                Box(modifier = Modifier.weight(0.42f).fillMaxHeight()) { library() }
                                                                androidx.compose.material3.VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                                                Box(modifier = Modifier.weight(0.58f).fillMaxHeight()) {
                                                                    if (paneBookId == null) {
                                                                        EmptyState(
                                                                            icon = Icons.AutoMirrored.Rounded.MenuBook,
                                                                            title = "Pick a book",
                                                                            message = "Its page opens here, right beside your library."
                                                                        )
                                                                    } else {
                                                                        // The grid's covers are still on screen, so the pane's cover must not claim the same hero key.
                                                                        androidx.compose.runtime.key(paneBookId) {
                                                                            CompositionLocalProvider(LocalHeroEnabled provides false) {
                                                                                BookDetailRoute(
                                                                                    bookId = paneBookId,
                                                                                    viewModel = viewModel,
                                                                                    books = books,
                                                                                    streak = streak,
                                                                                    snackbarHostState = snackbarHostState,
                                                                                    onDeleteWithUndo = onDeleteWithUndo,
                                                                                    onStartReading = { id -> navController.navigate("focus/$id") },
                                                                                    onBack = { libraryPaneBookId = null }
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
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
                                                            onOpenWishlist = { navController.navigate("wishlist") },
                                                            onOpenBook = { navController.navigate("book/${it.id}") },
                                                            onOpenWrapped = { navController.navigate("wrappedAnimated") },
                                                            onOpenAppSettings = { navController.navigate("settings") { launchSingleTop = true } },
                                                            onUpdateName = viewModel::setUserName
                                                        )
            
                                        }
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
                                    composable("wishlist") {
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                            val wishOwner by viewModel.userName.collectAsState()
                                            WishlistScreen(
                                                books = books,
                                                ownerName = wishOwner,
                                                onBack = { navController.popBackStack() },
                                                onOpenBook = { navController.navigate("book/${it.id}") },
                                                onSetWish = viewModel::setWish,
                                                onGotIt = { book, purchase ->
                                                    viewModel.gotWished(book, purchase)
                                                    scope.launch {
                                                        snackbarHostState.currentSnackbarData?.dismiss()
                                                        snackbarHostState.showSnackbar("“${book.title}” is on your shelf")
                                                    }
                                                },
                                                onRemove = { book ->
                                                    viewModel.setOwnership(book, Ownership.NONE)
                                                    scope.launch {
                                                        snackbarHostState.currentSnackbarData?.dismiss()
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "Removed from your wishlist",
                                                            actionLabel = "Undo",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) viewModel.setOwnership(book, Ownership.WISHLIST)
                                                    }
                                                },
                                                onSetBudget = { amount -> viewModel.updatePrefs { it.copy(monthlyBookBudget = amount) } },
                                                onSearch = { query ->
                                                    addQuery = query
                                                    showAddDialog = true
                                                }
                                            )
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
                                                    columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = coverTileMinWidth()),
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
                                            onAddToWishlist = { scanned, format ->
                                                viewModel.addToWishlist(scanned, format)
                                                navController.popBackStack()
                                                scope.launch {
                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                    snackbarHostState.showSnackbar("“${scanned.title}” added to your wishlist")
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
                                    composable("book/{bookId}") { entry ->
                                        androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                            BookDetailRoute(
                                                bookId = entry.arguments?.getString("bookId").orEmpty(),
                                                viewModel = viewModel,
                                                books = books,
                                                streak = streak,
                                                snackbarHostState = snackbarHostState,
                                                onDeleteWithUndo = onDeleteWithUndo,
                                                onStartReading = { id -> navController.navigate("focus/$id") },
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
                                                        val settingsPrefs by viewModel.prefs.collectAsState()
                                                        val driveConnected by viewModel.driveSyncEnabled.collectAsState()
                                                        val finishedBooks = remember(books) { books.filter(com.example.booktracker.app.analytics.FinishedShelf::isFinished) }
                                                        SettingsScreen(
                                                            prefs = settingsPrefs,
                                                            onPrefsChange = viewModel::updatePrefs,
                                                            dailyGoal = goal,
                                                            yearlyGoal = yearly,
                                                            reminder = reminder,
                                                            finishedCount = finishedBooks.size,
                                                            readingSinceYear = finishedBooks.minOfOrNull { it.completedAt }?.let {
                                                                java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).year
                                                            },
                                                            driveConnected = driveConnected,
                                                            onOpenGoals = { navController.navigate("goals") { launchSingleTop = true } },
                                                            onExportCsv = { uri, onDone -> viewModel.exportCsv(context, uri, onDone) },
                                                            onBackupNow = { onDone -> viewModel.backupNow(context, onDone) },
                                                            onDeleteAll = { onDone -> viewModel.deleteAllData(context, onDone) },
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
                                                            driveSync = {
                                                                val driveEnabled by viewModel.driveSyncEnabled.collectAsState()
                                                                val driveLastSync by viewModel.driveLastSync.collectAsState()
                                                                val driveState by viewModel.driveSync.collectAsState()
                                                                DriveSyncCard(
                                                                    enabled = driveEnabled,
                                                                    lastSync = driveLastSync,
                                                                    state = driveState,
                                                                    onSync = viewModel::syncDrive,
                                                                    onConsentShown = viewModel::driveConsentShown,
                                                                    onConsentResult = viewModel::onDriveConsent,
                                                                    onDisconnect = viewModel::disconnectDrive,
                                                                    onDismissStatus = viewModel::clearDriveStatus
                                                                )
                                                            },
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
                                            val journeySessions by viewModel.completedSessions.collectAsState()
                                            com.example.booktracker.app.ui.ReadingJourneyMap(
                                                books = books,
                                                sessions = journeySessions,
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
                                                                    onSaveSession = { endPage ->
                                                                        navController.navigate("session_save/$bookId?page=$endPage") { launchSingleTop = true }
                                                                    },
                                                                    onAddNote = { page, text, type, images -> viewModel.addNote(bookId, page, text, type, images) }
                                                                )
                                                            }
                                                        }
            
                                        }
                                    }
                                    composable(
                                        SESSION_SAVE_ROUTE,
                                        arguments = listOf(
                                            androidx.navigation.navArgument("page") {
                                                type = androidx.navigation.NavType.IntType
                                                defaultValue = -1
                                            }
                                        )
                                    ) {
                                        entry ->
androidx.compose.runtime.CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                                        val bookId = entry.arguments?.getString("bookId").orEmpty()
                                                        val handedPage = entry.arguments?.getInt("page") ?: -1
                                                        val book = books.find { it.id == bookId }
                                                        val openSession by viewModel.openSession.collectAsState()
                                                        val activeSeconds by com.example.booktracker.app.data.ServiceLocator
                                                            .activeReadingSeconds.collectAsState()
                                                        val saveSessions by viewModel.completedSessions.collectAsState()
                                                        val savePrefs = LocalAppPrefs.current
                                                        // Reviewing a session stops its clock, whichever way the reader got here
                                                        // (timer, notification, widget), so the time can't drift while they edit.
                                                        LaunchedEffect(bookId) {
                                                            if (com.example.booktracker.app.data.ServiceLocator.timerBookId.value == bookId) viewModel.setTimerRunning(false)
                                                        }
                                                        if (book != null) {
                                                            // Prefer the timer's measured reading time; fall back to the
                                                            // open session's wall clock for progress logged without a timer.
                                                            val session = openSession?.takeIf { it.bookId == bookId }
                                                            val start = session?.startTime ?: System.currentTimeMillis()
                                                            val duration = if (activeSeconds > 0) activeSeconds
                                                            else ((System.currentTimeMillis() - start) / 1000L).toInt().coerceAtLeast(0)
                                                            // A session of this book that ended shortly before this one began,
                                                            // likely the same sitting interrupted.
                                                            val previous = remember(saveSessions, bookId, start) {
                                                                saveSessions
                                                                    .filter { it.bookId == bookId && it.endTime in (start - JOIN_WINDOW_MILLIS)..start }
                                                                    .maxByOrNull { it.endTime }
                                                            }

                                                            SessionSaveScreen(
                                                                book = book,
                                                                initialStartTime = start,
                                                                initialDurationSeconds = duration,
                                                                initialPage = if (handedPage >= 0) handedPage else book.currentPage,
                                                                sessionStartPage = session?.startPage ?: book.currentPage,
                                                                onBack = { navController.popBackStack() },
                                                                onDiscard = {
                                                                    viewModel.discardOpenSession(bookId)
                                                                    navigateTopLevel("home")
                                                                },
                                                                previousSession = previous,
                                                                minSessionSeconds = savePrefs.minSessionSeconds,
                                                                scanFirst = savePrefs.scanOnFinish,
                                                                onSave = { draft ->
                                                                    viewModel.saveSession(
                                                                        book = book,
                                                                        endPage = draft.endPage,
                                                                        environmentTag = draft.tag,
                                                                        startTime = draft.startTime,
                                                                        endTime = draft.endTime,
                                                                        newStatus = draft.status,
                                                                        focus = draft.focus,
                                                                        quote = draft.quote,
                                                                        mergeWithId = draft.mergeWithId
                                                                    ) {
                                                                        navController.navigate("session_result/$bookId") {
                                                                            popUpTo(MAIN_ROUTE) { inclusive = false }
                                                                        }
                                                                    }
                                                                    pendingOutcome = draft.status?.takeIf {
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
                                                        val resultBadges by viewModel.badgeStatuses.collectAsState()
                                                        val current = result
                                                        if (book != null && current != null) {
                                                            SessionResultScreen(
                                                                book = book,
                                                                result = current,
                                                                allSessions = allSessions,
                                                                notes = resultNotes,
                                                                streak = streak,
                                                                badges = resultBadges,
                                                                onAddNote = { page, text, type, images -> viewModel.addNote(bookId, page, text, type, images) },
                                                                // The result is left in place: clearing it blanked this
                                                                // screen mid-exit, and the next save replaces it anyway.
                                                                onDone = { navigateTopLevel("home") }
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

    val showWelcome by viewModel.showWelcome.collectAsState()
    if (showWelcome) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {},
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                decorFitsSystemWindows = false
            )
        ) {
            val welcomeName by viewModel.userName.collectAsState()
            val welcomeGoal by viewModel.dailyGoal.collectAsState()
            WelcomeScreen(
                initialName = welcomeName,
                initialGoal = welcomeGoal,
                onSetName = viewModel::setUserName,
                onSetGoal = viewModel::setDailyGoal,
                onScanBook = { openScanner() },
                onSearchBook = { showAddDialog = true },
                onFinish = viewModel::finishWelcome
            )
        }
    }

    if (showAddDialog) {
        // A search opened from the wishlist comes with its query; it's used once.
        androidx.compose.runtime.DisposableEffect(Unit) { onDispose { addQuery = "" } }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAddDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            AddBookScreen(
                initialQuery = addQuery,
                onDismiss = { showAddDialog = false },
                onAddBook = { metadata, status, format ->
                    viewModel.addScannedBook(metadata, status, format)
                    showAddDialog = false
                },
                onAddToWishlist = { metadata, format ->
                    viewModel.addToWishlist(metadata, format)
                    showAddDialog = false
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar("“${metadata.title}” added to your wishlist")
                    }
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
    val monday = today.startOfWeek(LocalAppPrefs.current.weekStart)
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
                            Icons.Rounded.LocalFireDepartment,
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
    onOverlayChange: (Boolean) -> Unit = {},
    onToggleWishlist: (Book) -> Unit = {},
    onOpenWishlist: () -> Unit = {}
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
    // Wished-for books aren't on the shelf yet: they wait in the Wishlist tile, and only
    // join the grid when the Ownership filter asks for them.
    val shelf = remember(books) { com.example.booktracker.app.analytics.WishlistEngine.onShelf(books) }
    val wished = remember(books) { com.example.booktracker.app.analytics.WishlistEngine.wishlist(books) }

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

    val pool = if (selectedOwnership == Ownership.WISHLIST) books else shelf
    val displayBooks = remember(pool, selectedFilter, showFavoritesOnly, showLoansOnly, selectedCollection, selectedFormat, selectedOwnership, sortOrder) {
        pool.filter { book ->
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

    val readingCount = remember(shelf) { shelf.count { it.status.equals(BookStatus.READING.name, ignoreCase = true) } }
    val noFilters = selectedFilter == null && !showFavoritesOnly && !showLoansOnly &&
        selectedCollection == null && selectedFormat == null && selectedOwnership == null
    val showWishTile = wished.isNotEmpty() && noFilters && viewMode == LibraryViewMode.GRID
    SectionPage(
        title = "Library",
        line = when {
            shelf.isEmpty() -> if (wished.isEmpty()) "Start your shelf" else "${wished.size} on your wishlist"
            readingCount > 0 -> "${shelf.size} ${if (shelf.size == 1) "book" else "books"} · $readingCount reading"
            else -> "${shelf.size} ${if (shelf.size == 1) "book" else "books"}"
        },
        below = {
            SearchPill(
                text = if (books.isEmpty()) "Search for a book" else "Search ${books.size} ${if (books.size == 1) "book" else "books"}",
                onSearch = onSearch,
                onScan = onScan,
                modifier = Modifier.padding(top = 14.dp)
            )
        },
        actions = { collapse ->
            // The search field leaves with the big header; its icon takes over in the slim bar.
            val shut by remember { derivedStateOf { collapse() > 0.6f } }
            androidx.compose.animation.AnimatedVisibility(
                visible = shut,
                enter = fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.8f),
                exit = fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.8f)
            ) {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search library")
                }
            }
        }
    ) {
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
                null to "All (${pool.size})",
                BookStatus.READING to "${BookStatus.READING.label} (${pool.count { it.status.equals(BookStatus.READING.name, ignoreCase = true) }})",
                BookStatus.TO_READ to "${BookStatus.TO_READ.label} (${pool.count { it.status.equals(BookStatus.TO_READ.name, ignoreCase = true) || it.status.equals("BACKLOG", ignoreCase = true) }})",
                BookStatus.SHORTLIST to "${BookStatus.SHORTLIST.label} (${pool.count { it.status.equals(BookStatus.SHORTLIST.name, ignoreCase = true) }})",
                BookStatus.UP_NEXT to "${BookStatus.UP_NEXT.label} (${pool.count { it.status.equals(BookStatus.UP_NEXT.name, ignoreCase = true) }})",
                BookStatus.PAUSED to "${BookStatus.PAUSED.label} (${pool.count { it.status.equals(BookStatus.PAUSED.name, ignoreCase = true) }})",
                BookStatus.FINISHED to "${BookStatus.FINISHED.label} (${pool.count { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) }})",
                BookStatus.DNF to "${BookStatus.DNF.label} (${pool.count { it.status.equals(BookStatus.DNF.name, ignoreCase = true) || it.status.equals(BookStatus.ABANDONED.name, ignoreCase = true) }})"
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
                leadingIcon = { Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) },
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
                        trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
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
                                    if (ownership == selectedOwnership) Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
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
                        trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
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
                                    if (format == selectedFormat) Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
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
                        Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
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
                                if (option == sortOrder) Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
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
                            Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
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
                                    if (name == selectedCollection) Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
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
                    if (viewMode == LibraryViewMode.GRID) Icons.Rounded.ViewColumn else Icons.Rounded.GridView,
                    contentDescription = if (viewMode == LibraryViewMode.GRID) "Show as bookshelf" else "Show as grid"
                )
            }
            // Less common actions get words, not unexplained icons.
            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More library actions")
                }
                DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Sort your to-read pile") },
                        leadingIcon = { Icon(Icons.Rounded.Style, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            onCurationClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Open an EPUB file") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            epubPickerLauncher.launch("application/epub+zip")
                        }
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (displayBooks.isEmpty() && !showWishTile) {
                if (shelf.isEmpty()) {
                    // Library is completely empty
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Surface(
                            shape = AppShape.XL,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.animateContentSize()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.MenuBook,
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
                                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Scan")
                                    }
                                    FilledTonalButton(
                                        onClick = onSearch,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
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
                            shape = AppShape.XL,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.animateContentSize()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.MenuBook,
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
                                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
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
                        // Three across on a phone; bigger covers on tablets.
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = coverTileMinWidth()),
                        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp + LocalNavBarClearance.current),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (showWishTile) {
                            item(key = "wishlist") {
                                WishlistTile(
                                    wished = wished,
                                    onClick = onOpenWishlist,
                                    modifier = Modifier.animateItem().staggeredEntrance(gridEntrance, 0)
                                )
                            }
                        }
                        val firstBook = if (showWishTile) 1 else 0
                        itemsIndexed(displayBooks, key = { _, book -> book.id }) { index, book ->
                            LibraryGridCard(
                                book = book,
                                modifier = Modifier
                                    .animateItem()
                                    .staggeredEntrance(gridEntrance, firstBook + index),
                                onClick = { onOpenBook(book) },
                                onStartReading = { onStartReading(book) },
                                onDelete = { onDelete(book) },
                                onChangeStatus = { status -> onChangeStatus(book, status) },
                                onAddNote = { noteFor = book },
                                onToggleWishlist = { onToggleWishlist(book) }
                            )
                        }
                    }
                }
            }
        }
    }
    }
    }
}

// BookCover lives in BookCover.kt.

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
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
        shape = AppShape.L,
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
            BookCover(book, Modifier.size(width = 36.dp, height = 52.dp))
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
                Icon(Icons.Rounded.Close, contentDescription = "End session", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isCountdownDone) {
                FilledTonalButton(onClick = onSave) { Text("Save") }
            } else {
                androidx.compose.material3.FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isRunning) "Pause timer" else "Resume timer"
                    )
                }
            }
        }
    }
}
