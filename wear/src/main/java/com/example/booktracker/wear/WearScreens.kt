package com.example.booktracker.wear

import android.text.format.DateFormat
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.example.booktracker.shared.Constants
import com.example.booktracker.wear.journal.WristDictaphone
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun WearApp(
    isAmbient: Boolean,
    ambientOffset: IntOffset,
    loaded: Boolean,
    shelf: Shelf?,
    phoneTimer: PhoneTimer?,
    session: WristSession?,
    actions: WearActions
) {
    val books = shelf?.books.orEmpty()
    // Kept above the ambient switch so leaving ambient comes back to the same book.
    val pagerState = rememberPagerState(pageCount = { books.size })
    var positioned by remember { mutableStateOf(false) }
    LaunchedEffect(books.size) {
        if (positioned || books.isEmpty()) return@LaunchedEffect
        // Open on the book a session is timing, or else the one touched last.
        val target = books.indexOfFirst { it.id == session?.bookId }.takeIf { it >= 0 }
            ?: books.indexOf(shelf?.latest).coerceAtLeast(0)
        pagerState.scrollToPage(target)
        positioned = true
    }

    // Also above the switch: dimming to ambient mid-dialog mustn't close it or drop the focus picked.
    var dialog by remember { mutableStateOf<PageDialog?>(null) }

    if (isAmbient) {
        val current = books.getOrNull(pagerState.currentPage)
        AmbientScreen(current, clockFor(current, session, phoneTimer), ambientOffset)
        return
    }

    NocturnalTheme(WatchPalette.of(shelf)) {
        AppScaffold(timeText = { TimeText() }) {
            if (shelf == null || books.isEmpty()) {
                ScreenScaffold {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (loaded) {
                            Text(
                                stringResource(R.string.empty_shelf),
                                style = TitleStyle,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 28.dp)
                            )
                        }
                    }
                }
            } else {
                HorizontalPagerScaffold(pagerState = pagerState) {
                    // The crown logs pages, so it must not turn them.
                    HorizontalPager(state = pagerState, rotaryScrollableBehavior = null) { page ->
                        AnimatedPage(pageIndex = page, pagerState = pagerState) {
                            ScreenScaffold {
                                val book = books[page]
                                BookPage(
                                    book = book,
                                    shelf = shelf,
                                    isCurrent = pagerState.currentPage == page,
                                    clock = clockFor(book, session, phoneTimer),
                                    session = session?.takeIf { it.bookId == book.id },
                                    phoneTimer = phoneTimer?.takeIf { it.bookId == book.id },
                                    canStartSession = session == null && phoneTimer?.bookId == null,
                                    dialog = dialog?.takeIf { it.bookId == book.id },
                                    onDialog = { dialog = it },
                                    actions = actions
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
 * One book: title, the page in large type with +1 / +10 beside it, today's goal or
 * the running clock, and one bright action (read, pause or finish) at the bottom.
 * The crown moves the page too. Two rings frame it: through the book, and today's goal.
 */
@Composable
private fun BookPage(
    book: ActiveBook,
    shelf: Shelf,
    isCurrent: Boolean,
    clock: BookClock?,
    /** A wrist session on this book, if any. */
    session: WristSession?,
    /** The phone's timer, if it's on this book. */
    phoneTimer: PhoneTimer?,
    canStartSession: Boolean,
    /** The dialog open on this page, if any. */
    dialog: PageDialog?,
    onDialog: (PageDialog?) -> Unit,
    actions: WearActions
) {
    val haptics = LocalHapticFeedback.current
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        WristDictaphone.firstResult(result.data)?.let { actions.voiceNote(book, it) }
    }
    val focusRequester = remember { FocusRequester() }
    var rotaryAccum by remember { mutableFloatStateOf(0f) }
    var pendingPages by remember(book.id) { mutableIntStateOf(0) }
    val showStart = dialog is PageDialog.Start
    val showFinish = dialog is PageDialog.Finish

    // Crown turns gather for a moment, then go out as one change.
    LaunchedEffect(pendingPages) {
        if (pendingPages != 0) {
            delay(600)
            val delta = pendingPages
            pendingPages = 0
            actions.addPages(book, delta)
        }
    }
    // The pager composes the neighbours too; only the page on screen takes the crown.
    LaunchedEffect(isCurrent, showFinish, showStart) {
        if (isCurrent && !showFinish && !showStart) focusRequester.requestFocus()
    }

    val shownPage = book.currentUnit + pendingPages
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onRotaryScrollEvent { event ->
                rotaryAccum += event.verticalScrollPixels
                var steps = 0
                while (rotaryAccum >= ROTARY_PIXELS_PER_PAGE) {
                    rotaryAccum -= ROTARY_PIXELS_PER_PAGE
                    steps++
                }
                while (rotaryAccum <= -ROTARY_PIXELS_PER_PAGE) {
                    rotaryAccum += ROTARY_PIXELS_PER_PAGE
                    steps--
                }
                if (steps != 0) {
                    val ceiling = if (book.totalUnits > 0) book.totalUnits else Int.MAX_VALUE
                    val clamped = (shownPage + steps).coerceIn(0, ceiling) - book.currentUnit
                    if (clamped != pendingPages) {
                        pendingPages = clamped
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Rings(
            bookProgress = if (book.totalUnits > 0) (shownPage.toFloat() / book.totalUnits).coerceIn(0f, 1f) else null,
            goalProgress = if (shelf.dailyGoal > 0) {
                ((shelf.pagesToday + pendingPages).toFloat() / shelf.dailyGoal).coerceIn(0f, 1f)
            } else null
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp, bottom = 24.dp)
        ) {
            Text(
                book.title,
                style = TitleStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 136.dp)
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuietButton(R.string.action_add_one, onClick = { actions.addPages(book, 1) }) {
                    Text("+1", style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    "$shownPage",
                    style = PageNumberStyle,
                    color = if (pendingPages != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 64.dp).padding(horizontal = 6.dp)
                )
                QuietButton(R.string.action_add_ten, onClick = { actions.addPages(book, 10) }) {
                    Text("+10", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (book.totalUnits > 0) {
                Text(
                    stringResource(R.string.of_total, book.totalUnits),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            if (clock != null) ClockLine(clock, onExpired = actions::expireSession) else ProgressLine(book, shelf, shownPage, pendingPages)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                QuietIcon(R.drawable.ic_mic, R.string.action_voice_note) {
                    try {
                        voiceLauncher.launch(WristDictaphone.getSpeechToTextIntent())
                    } catch (e: android.content.ActivityNotFoundException) {
                        Log.w("WearScreens", "No speech recognizer", e)
                    }
                }
                when {
                    session != null -> {
                        QuietIcon(
                            if (session.running) R.drawable.ic_pause else R.drawable.ic_play,
                            if (session.running) R.string.action_pause else R.string.action_resume
                        ) { actions.setSessionRunning(!session.running) }
                        BrightIcon(R.drawable.ic_check, R.string.action_finish) { onDialog(PageDialog.Finish(book.id)) }
                    }
                    phoneTimer != null -> {
                        // A finished countdown can't resume; the reader saves it on the phone.
                        val exhausted = phoneTimer.countdown && !phoneTimer.running && phoneTimer.seconds <= 0
                        if (!exhausted) {
                            PrimaryPill(
                                icon = if (phoneTimer.running) R.drawable.ic_pause else R.drawable.ic_play,
                                label = if (phoneTimer.running) R.string.action_pause else R.string.action_resume
                            ) {
                                actions.phoneTimer(
                                    if (phoneTimer.running) Constants.TIMER_ACTION_PAUSE else Constants.TIMER_ACTION_RESUME
                                )
                            }
                        }
                    }
                    canStartSession -> PrimaryPill(R.drawable.ic_play, R.string.read, R.string.action_start_session) {
                        onDialog(PageDialog.Start(book.id))
                    }
                }
            }
        }
    }

    StartDialog(
        visible = showStart,
        onDismiss = { onDialog(null) },
        onStart = { minutes ->
            onDialog(null)
            actions.startSession(book, minutes)
        },
        onAllowOnTime = actions::allowOnTimeBuzz
    )

    if (session != null) {
        val minutes = (session.activeMillis(System.currentTimeMillis()) / 60_000L).toInt()
        val pages = book.currentUnit - session.startPage
        val focus = (dialog as? PageDialog.Finish)?.focus ?: 0
        AlertDialog(
            visible = showFinish,
            onDismissRequest = { onDialog(null) },
            edgeButton = {
                AlertDialogDefaults.EdgeButton(onClick = {
                    onDialog(null)
                    actions.finishSession(book, focus)
                }) { Text(stringResource(R.string.save)) }
            },
            title = { Text(stringResource(R.string.finish_title), style = TitleStyle) },
            text = {
                Text(
                    stringResource(
                        R.string.finish_summary,
                        stringResource(R.string.minutes_short, minutes),
                        pluralStringResource(R.plurals.pages_delta, pages, pages)
                    )
                )
            }
        ) {
            item { FocusPicker(focus) { onDialog(PageDialog.Finish(book.id, it)) } }
            item {
                FilledTonalButton(
                    onClick = {
                        onDialog(null)
                        actions.discardSession(book)
                    },
                    label = { Text(stringResource(R.string.discard)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Through the book on the outside, today's goal just inside it, both open at the top for the time. */
@Composable
private fun Rings(bookProgress: Float?, goalProgress: Float?) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    CircularProgressIndicator(
        progress = { bookProgress ?: 0f },
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp),
        startAngle = RING_START,
        endAngle = RING_END,
        strokeWidth = 5.dp,
        colors = ProgressIndicatorDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primary,
            trackColor = track
        )
    )
    if (goalProgress != null) {
        CircularProgressIndicator(
            progress = { goalProgress },
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            startAngle = RING_START,
            endAngle = RING_END,
            strokeWidth = 3.dp,
            colors = ProgressIndicatorDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.tertiary,
                trackColor = Color.Transparent
            )
        )
    }
}

/**
 * The streak, then the book's finish-by pace when it has a deadline, or else
 * today's pages against the goal (which the inner ring shows either way).
 */
@Composable
private fun ProgressLine(book: ActiveBook, shelf: Shelf, shownPage: Int, pendingPages: Int) {
    val today = (shelf.pagesToday + pendingPages).coerceAtLeast(0)
    val met = shelf.dailyGoal > 0 && today >= shelf.dailyGoal
    val plan = book.plan?.takeIf { !it.done }
    val dateFormat = remember { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMM")) }
    val text = when {
        plan != null -> {
            val due = LocalDate.ofEpochDay(plan.targetDay).format(dateFormat)
            val left = plan.leftToday(book, shownPage)
            when {
                plan.overdue -> stringResource(R.string.plan_overdue, due)
                left > 0 -> stringResource(R.string.plan_to_go, left, due)
                else -> stringResource(R.string.plan_on_track, due)
            }
        }
        met -> stringResource(R.string.goal_met)
        shelf.dailyGoal > 0 -> stringResource(R.string.goal_progress, today, shelf.dailyGoal)
        else -> pluralStringResource(R.plurals.pages_today, today, today)
    }
    val color = when {
        plan?.overdue == true -> MaterialTheme.colorScheme.secondary
        plan == null && met -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    // A deadline line runs longer, so it drops a size to fit the round screen.
    val style = if (plan != null) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (shelf.streak > 0) {
            val streakWords = pluralStringResource(R.plurals.streak_days, shelf.streak, shelf.streak)
            Icon(
                painterResource(R.drawable.ic_flame),
                contentDescription = streakWords,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
            )
            Text("${shelf.streak}", style = style, color = MaterialTheme.colorScheme.primary)
            Text(" · ", style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text, style = style, color = color, maxLines = 1)
    }
}

/** How long to read for: counting up, or one of the countdowns, scrolled with the crown. */
@Composable
private fun StartDialog(visible: Boolean, onDismiss: () -> Unit, onStart: (Int) -> Unit, onAllowOnTime: () -> Unit) {
    val context = LocalContext.current
    val last = remember(visible) { WristSessions.lastMinutes(context) }
    val onTime = remember(visible) { SessionAlarm.canBeOnTime(context) }
    AlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.read_for), style = TitleStyle) }
    ) {
        for (minutes in WristSessions.CHOICES) {
            item {
                val label = if (minutes == 0) stringResource(R.string.no_limit) else stringResource(R.string.minutes_short, minutes)
                if (minutes == last) {
                    Button(onClick = { onStart(minutes) }, modifier = Modifier.fillMaxWidth(), label = { Text(label) })
                } else {
                    FilledTonalButton(onClick = { onStart(minutes) }, modifier = Modifier.fillMaxWidth(), label = { Text(label) })
                }
            }
        }
        if (!onTime) {
            item {
                Text(
                    stringResource(R.string.allow_on_time),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clickable(onClick = onAllowOnTime)
                )
            }
        }
    }
}

/** 1 to 5, lit up to the one picked; tapping the picked one again clears it. */
@Composable
private fun FocusPicker(focus: Int, onChange: (Int) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.how_focused),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (level in 1..5) {
                val spoken = stringResource(R.string.focus_level, level)
                val lit = level <= focus
                FilledTonalIconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onChange(if (level == focus) 0 else level)
                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (lit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (lit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .size(30.dp)
                        .semantics { contentDescription = spoken }
                ) {
                    Text("$level", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/** A running clock that ticks itself here, so the phone needn't send every second. */
@Composable
private fun ClockLine(clock: BookClock, onExpired: () -> Unit) {
    val now by produceState(System.currentTimeMillis(), clock.running) {
        while (clock.running) {
            value = System.currentTimeMillis()
            delay(1000 - value % 1000)
        }
        value = System.currentTimeMillis()
    }
    val seconds = clock.secondsAt(now)
    // On screen the wrist countdown ends on the dot, without waiting for the alarm.
    if (!clock.onPhone && clock.countdown && clock.running && seconds == 0) {
        LaunchedEffect(Unit) { onExpired() }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            formatClock(seconds),
            style = MaterialTheme.typography.titleMedium,
            color = when {
                !clock.running -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.primary
            }
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "${stringResource(clock.label)} · ${stringResource(if (clock.onPhone) R.string.on_phone else R.string.on_watch)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Low-power, all outline: title, page and the clock to the minute (ambient redraws once a minute). */
@Composable
private fun AmbientScreen(book: ActiveBook?, clock: BookClock?, offset: IntOffset) {
    val now = remember(offset) { System.currentTimeMillis() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset { offset }
                .padding(horizontal = 24.dp)
        ) {
            Text(
                book?.title ?: stringResource(R.string.app_name),
                style = TitleStyle,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (book != null) {
                Text("${book.currentUnit}", style = PageNumberStyle, color = Color.LightGray)
            }
            if (clock != null) {
                Text(stringResource(R.string.minutes_short, clock.secondsAt(now) / 60), color = Color.LightGray)
            }
        }
    }
}

/** A grey round button for the secondary actions, so only the main one is in colour. */
@Composable
private fun QuietButton(@StringRes description: Int, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    val haptics = LocalHapticFeedback.current
    val spoken = stringResource(description)
    FilledTonalIconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .size(SMALL_BUTTON)
            .semantics { contentDescription = spoken },
        content = content
    )
}

@Composable
private fun QuietIcon(@DrawableRes icon: Int, @StringRes description: Int, onClick: () -> Unit) {
    QuietButton(description, onClick) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun BrightIcon(@DrawableRes icon: Int, @StringRes description: Int, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    FilledIconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = Modifier.size(SMALL_BUTTON)
    ) {
        Icon(painterResource(icon), contentDescription = stringResource(description), modifier = Modifier.size(18.dp))
    }
}

/** The one action in colour on each page: read, pause or resume. */
@Composable
private fun PrimaryPill(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    @StringRes description: Int = label,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val spoken = stringResource(description)
    Button(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        contentPadding = PaddingValues(start = 10.dp, end = 14.dp),
        modifier = Modifier
            .height(SMALL_BUTTON)
            .semantics { contentDescription = spoken }
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(16.dp).align(Alignment.CenterVertically))
        Spacer(Modifier.width(4.dp))
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.CenterVertically))
    }
}

/** A dialog open on one book's page. */
private sealed interface PageDialog {
    val bookId: String

    data class Start(override val bookId: String) : PageDialog

    data class Finish(override val bookId: String, val focus: Int = 0) : PageDialog
}

/** A clock on one book: a session timed here, or the phone's timer. */
private class BookClock(
    val running: Boolean,
    @StringRes val label: Int,
    val onPhone: Boolean,
    /** Counting down to zero rather than up. */
    val countdown: Boolean,
    private val seconds: (Long) -> Int
) {
    fun secondsAt(now: Long): Int = seconds(now)
}

private fun clockFor(book: ActiveBook?, session: WristSession?, timer: PhoneTimer?): BookClock? = when {
    book == null -> null
    session != null && session.bookId == book.id -> BookClock(
        running = session.running,
        label = when {
            !session.countdown -> R.string.phase_reading
            session.expired(System.currentTimeMillis()) -> R.string.times_up
            else -> R.string.phase_focus
        },
        onPhone = false,
        countdown = session.countdown
    ) { now ->
        // Rounded up, so a countdown shows 00:00 only once it has really run out.
        session.remainingMillis(now)?.let { ((it + 999) / 1000L).toInt() } ?: (session.activeMillis(now) / 1000L).toInt()
    }
    timer != null && timer.bookId == book.id -> BookClock(
        running = timer.running,
        label = when {
            !timer.countdown -> R.string.phase_reading
            else -> R.string.phase_focus
        },
        onPhone = true,
        countdown = timer.countdown
    ) { timer.secondsAt(it) }
    else -> null
}

private fun formatClock(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
}

// One "page" of bezel/crown travel; tuned so a single detent on a rotating
// bezel (~48px of scroll) logs exactly one page.
private const val ROTARY_PIXELS_PER_PAGE = 48f

// Ring angles run clockwise from 3 o'clock; this leaves a gap at 12 for the time.
private const val RING_GAP = 44f
private const val RING_START = 270f + RING_GAP / 2
private const val RING_END = 270f - RING_GAP / 2

private val SMALL_BUTTON = 40.dp
