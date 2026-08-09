package com.example.booktracker.app.ui

import androidx.compose.material3.TextButton
import coil3.request.allowHardware
import coil3.imageLoader
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.format.FormatAdaptabilityLayer
import com.example.booktracker.app.format.rememberReadAloud
import com.example.booktracker.app.hardware.StylusScratchpad
import com.example.booktracker.app.journal.JournalingEngine
import com.example.booktracker.app.journal.SmartPlanningEngine
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.NoteType
import com.example.booktracker.shared.models.RatingAxis
import com.example.booktracker.shared.models.Session
import com.example.booktracker.app.analytics.ReadingCalendar
import com.example.booktracker.app.analytics.StreakEngine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val noteStampFormatter = DateTimeFormatter.ofPattern("M/d/yyyy h:mm a")

@Composable
private fun SectionItem(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp)
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    book: Book?,
    sessions: List<Session>,
    allSessions: List<Session>,
    streak: StreakEngine.StreakInfo,
    notes: List<MarginNote>,
    onAddNote: (page: Int, text: String, type: NoteType) -> Unit,
    onDeleteNote: (String) -> Unit,
    onToggleNoteFavorite: (String) -> Unit,
    onSetProgress: (Int) -> Unit,
    onSetCollections: (List<String>) -> Unit,
    allCollections: List<String>,
    onSetFormat: (String) -> Unit,
    onDelete: () -> Unit,
    onStartReading: () -> Unit,
    onPauseReading: () -> Unit,
    onReadAgain: () -> Unit,
    onEditSession: (Session, Int) -> Unit,
    onDeleteSession: (Session) -> Unit,
    onToggleFavorite: () -> Unit,
    onFinish: (Map<String, Float>, String?) -> Unit,
    onBack: () -> Unit
) {
    var showAddNote by remember { mutableStateOf(false) }
    var sessionToEdit by remember { mutableStateOf<Session?>(null) }
    var sessionToDelete by remember { mutableStateOf<Session?>(null) }
    var showScratchpad by remember { mutableStateOf(false) }
    var showOcr by remember { mutableStateOf(false) }
    var ocrDraft by remember { mutableStateOf<String?>(null) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showProgressPicker by remember { mutableStateOf(false) }
    var showCollectionPicker by remember { mutableStateOf(false) }
    var noteSort by remember { mutableStateOf(NoteSort.CREATED) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(book) {
        if (book != null && book.currentPage == book.totalPages && book.totalPages > 0 && book.status == BookStatus.READING.name) {
            showFinishDialog = true
        }
    }

    if (book == null) {
        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "This book is no longer in your library.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    // Full-screen OCR camera takes over while active, then hands the recognized
    // text back into the note sheet (via ocrDraft) for editing before saving.
    if (showOcr) {
        androidx.activity.compose.BackHandler { showOcr = false }
        OcrCaptureScreen(
            onClose = { showOcr = false },
            onTextConfirmed = { text ->
                showOcr = false
                ocrDraft = text
                showAddNote = true
            }
        )
        return
    }

    val accent = rememberCoverAccent(book.coverUrl)

    val currentColorScheme = MaterialTheme.colorScheme
    val dynamicColorScheme = remember(currentColorScheme, accent) {
        if (accent.hasColor) {
            currentColorScheme.copy(
                primary = accent.vibrant!!,
                primaryContainer = accent.vibrant.copy(alpha = 0.3f),
                secondary = accent.dominant!!,
                secondaryContainer = accent.dominant.copy(alpha = 0.3f),
            )
        } else {
            currentColorScheme
        }
    }

    val completed = sessions.filter { it.endTime > 0 }

    val headerColor = accent.immersiveBackground(MaterialTheme.colorScheme.surfaceContainerHighest)

    MaterialTheme(colorScheme = dynamicColorScheme) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Compact colour-washed header: title on top, small cover at the left,
            // imprint details beside it, and the action pill straddling its edge.
            item {
                DetailHeader(
                    book = book,
                    headerColor = headerColor,
                    onStartReading = {
                        if (book.status != BookStatus.READING.name) onStartReading()
                    },
                    onPauseReading = onPauseReading,
                    onReadAgain = onReadAgain,
                    onAddNote = { showAddNote = true }
                )
            }

            item { Spacer(Modifier.height(20.dp)) }

            item { SectionItem { MetadataChips(book) } }

            item { SectionItem { AboutCard(book) } }

            item {
                SectionItem {
                    CollectionsCard(
                        collections = book.classification.collections,
                        onEdit = { showCollectionPicker = true }
                    )
                }
            }

            if (book.genres.isNotEmpty()) {
                item {
                    SectionItem {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Sell,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            book.genres.forEach { genre ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ) {
                                    Text(
                                        "#$genre",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (book.purchaseLog.isNotEmpty() || book.loanRecord.isNotEmpty()) {
                item {
                    SectionItem {
                        LedgerCard(book)
                    }
                }
            }

            item {
                SectionItem {
                    ProgressAndDaysRow(
                        book = book,
                        sessions = completed,
                        onEditProgress = { showProgressPicker = true }
                    )
                }
            }

            item { SectionItem { BookStatsCard(book, completed) } }
            item { SectionItem { FormatCard(book, onSetFormat) } }
            item { SectionItem { ReadingStreakCard(streak, allSessions) } }
            if (book.status == BookStatus.READING.name) {
                item { SectionItem { PlannerCard(book, completed) } }
            }

            if (book.status == BookStatus.FINISHED.name && book.rating.isNotEmpty()) {
                item { SectionItem { RatingCard(book) } }
            }
            if (book.status == BookStatus.FINISHED.name && !book.review.isNullOrBlank()) {
                item { SectionItem { ReviewCard(book) } }
            }
            if (book.status == BookStatus.DNF.name && book.dnfData != null) {
                item {
                    SectionItem {
                        DetailCard(title = "Did not finish") {
                            Text(
                                "Abandoned at ${book.dnfData!!.abandonedPercentage.roundToInt()}%" +
                                    book.dnfData!!.reason
                                        .takeIf { it.isNotBlank() }
                                        ?.let { " · $it" }.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            item {
                SectionItem {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                "Notes",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { showScratchpad = true },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CenterFocusStrong,
                                        contentDescription = "Handwrite a note",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { showAddNote = true },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Add note",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (notes.size == 1) "1 note" else "${notes.size} notes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            NoteSortToggle(current = noteSort, onChange = { noteSort = it })
                        }
                    }
                }
            }
            if (notes.isEmpty()) {
                item {
                    SectionItem {
                        Text(
                            "Capture thoughts, quotes, or questions tied to a page.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                val ordered = when (noteSort) {
                    NoteSort.CREATED -> notes.sortedByDescending { it.timestamp }
                    NoteSort.PAGE -> notes.sortedBy { it.pageNumber }
                    NoteSort.FAVORITES -> notes.sortedWith(
                        compareByDescending<MarginNote> { it.isFavorite }.thenByDescending { it.timestamp }
                    )
                }
                items(ordered, key = { it.id }) { note ->
                    SectionItem {
                        NoteCard(
                            note = note,
                            book = book,
                            onDelete = { onDeleteNote(note.id) },
                            onToggleFavorite = { onToggleNoteFavorite(note.id) },
                            onShare = { /* We will trigger share intent from viewmodel later */ }
                        )
                    }
                }
            }

            item {
                SectionItem {
                    Text("Session history", style = MaterialTheme.typography.titleLarge)
                }
            }
            if (completed.isEmpty()) {
                item {
                    SectionItem {
                        Text(
                            "No recorded sessions yet — start one from the Home tab.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                items(completed.take(30), key = { it.id }) { session ->
                    SectionItem {
                        SessionRow(
                            session = session,
                            onEdit = { sessionToEdit = session },
                            onDelete = { sessionToDelete = session }
                        )
                    }
                }
            }
        }
        
        // Transparent over the header, then fading in once it scrolls away.
        val headerScrolled by remember {
            derivedStateOf { listState.firstVisibleItemIndex > 0 }
        }
        val barColor by androidx.compose.animation.animateColorAsState(
            if (headerScrolled) MaterialTheme.colorScheme.background else headerColor,
            label = "topBar"
        )
        var showOverflow by remember { mutableStateOf(false) }
        val barContent = if (headerScrolled) MaterialTheme.colorScheme.onSurface else Color.White

        TopAppBar(
            modifier = Modifier.background(barColor),
            title = {
                if (headerScrolled) {
                    Text(
                        book.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        color = barContent
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = barContent
                    )
                }
            },
            actions = {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (book.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (book.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = barContent
                    )
                }
                Box {
                    IconButton(onClick = { showOverflow = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = barContent)
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = showOverflow,
                        onDismissRequest = { showOverflow = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Delete from library") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showOverflow = false
                                onDelete()
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
        )

        // Persistent timer | note pill, mirroring the one on the header so the
        // two primary actions stay reachable however far down the page you are.
        androidx.compose.animation.AnimatedVisibility(
            visible = headerScrolled,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.8f),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            ActionPill(
                onTimer = { if (book.status != BookStatus.READING.name) onStartReading() else onStartReading() },
                onAddNote = { showAddNote = true },
                container = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }
    }

    if (showAddNote) {
        AddNoteBottomSheet(
            suggestedPage = book.currentPage,
            totalPages = book.totalPages,
            initialText = ocrDraft ?: "",
            onDismiss = {
                showAddNote = false
                ocrDraft = null
            },
            onConfirm = { page, text, type ->
                onAddNote(page, text, type)
                showAddNote = false
                ocrDraft = null
            }
        )
    }

    if (showScratchpad) {
        StylusScratchpad(
            onDismiss = { showScratchpad = false },
            onRecognized = { text ->
                onAddNote(book.currentPage, text, NoteType.BOOK_CONTENT)
                showScratchpad = false
            }
        )
    }

    if (showProgressPicker) {
        ProgressPickerSheet(
            book = book,
            initialPage = book.currentPage,
            onDismiss = { showProgressPicker = false },
            onConfirm = {
                onSetProgress(it)
                showProgressPicker = false
            }
        )
    }

    if (showCollectionPicker) {
        CollectionPickerDialog(
            selected = book.classification.collections,
            known = allCollections,
            onDismiss = { showCollectionPicker = false },
            onConfirm = {
                onSetCollections(it)
                showCollectionPicker = false
            }
        )
    }

    sessionToEdit?.let { session ->
        EditSessionDialog(
            session = session,
            book = book,
            onDismiss = { sessionToEdit = null },
            onConfirm = { newUnits ->
                onEditSession(session, newUnits)
                sessionToEdit = null
            }
        )
    }

    if (showFinishDialog) {
        FinishDialog(
            book = book,
            onDismiss = { showFinishDialog = false },
            onConfirm = { rating ->
                onFinish(rating)
                showFinishDialog = false
            }
        )
    }

    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Session") },
            text = { Text("Are you sure you want to delete this reading session? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSession(session)
                    sessionToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    }
}


/**
 * The colour-washed header block. Layout follows the reference: title across the
 * top, a small cover at the left with the imprint details stacked beside it, and
 * the primary action pill straddling the header's bottom edge so it reads as the
 * entry point to both reading and note-taking.
 */
@Composable
private fun DetailHeader(
    book: Book,
    headerColor: Color,
    onStartReading: () -> Unit,
    onPauseReading: () -> Unit,
    onReadAgain: () -> Unit,
    onAddNote: () -> Unit
) {
    val onHeader = Color.White

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    headerColor,
                    RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                )
                .padding(start = 24.dp, end = 24.dp, top = 96.dp, bottom = 40.dp)
        ) {
            Text(
                book.title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = onHeader
            )

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.Top) {
                BookCover(book.coverUrl, Modifier.heroSharedElement("cover-${book.id}").size(width = 96.dp, height = 146.dp))
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (book.authors.isNotEmpty()) {
                        Text(
                            book.authors.joinToString(", "),
                            style = MaterialTheme.typography.titleMedium,
                            color = onHeader
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    book.publication?.publisher?.takeIf { it.isNotBlank() }?.let { publisher ->
                        Text(
                            publisher,
                            style = MaterialTheme.typography.bodyLarge,
                            color = onHeader.copy(alpha = 0.75f)
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        book.readingLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = onHeader.copy(alpha = 0.75f)
                    )
                    Spacer(Modifier.height(12.dp))
                    StatusPill(book.status)
                }
            }
        }

        // Straddles the header/content boundary, as in the reference.
        ActionPill(
            onTimer = when (book.status) {
                BookStatus.READING.name -> onPauseReading
                BookStatus.FINISHED.name, BookStatus.DNF.name -> onReadAgain
                else -> onStartReading
            },
            onAddNote = onAddNote,
            timerIcon = when (book.status) {
                BookStatus.READING.name -> Icons.Filled.Pause
                BookStatus.FINISHED.name, BookStatus.DNF.name -> Icons.Filled.Replay
                else -> Icons.Filled.PlayCircleOutline
            },
            container = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp)
                .offset(y = 28.dp)
        )
    }
}

/** The two-action pill: start/pause the timer, or capture a note. */
@Composable
private fun ActionPill(
    onTimer: () -> Unit,
    onAddNote: () -> Unit,
    container: Color,
    modifier: Modifier = Modifier,
    timerIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.PlayCircleOutline
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onTimer, modifier = Modifier.size(56.dp)) {
                Icon(
                    timerIcon,
                    contentDescription = "Reading timer",
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            IconButton(onClick = onAddNote, modifier = Modifier.size(56.dp)) {
                Icon(
                    Icons.Filled.PostAdd,
                    contentDescription = "Add a note",
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val label = when (status) {
        BookStatus.TO_READ.name -> "Backlog"
        BookStatus.SHORTLIST.name -> "Shortlist"
        BookStatus.UP_NEXT.name -> "Up Next"
        BookStatus.READING.name -> "Reading"
        BookStatus.PAUSED.name -> "Paused"
        BookStatus.FINISHED.name -> "Finished"
        BookStatus.DNF.name -> "Did not finish"
        else -> status
    }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.height(28.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AboutCard(book: Book) {
    var expanded by remember { mutableStateOf(false) }
    val readAloud = rememberReadAloud()
    val speaking by readAloud.isSpeaking
    DetailCard(title = "About") {
        if (book.description.isBlank()) {
            Text(
                "No summary available for this edition.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                book.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 6,
                overflow = TextOverflow.Ellipsis
            )
            if (book.description.length > 300) {
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (expanded) "Show less" else "Read more")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(
            onClick = { readAloud.toggle(FormatAdaptabilityLayer.readAloudText(book)) }
        ) {
            Icon(
                if (speaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(if (speaking) "Stop" else "Listen")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatCard(book: Book, onSetFormat: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Tracking format", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Track this book in ${FormatAdaptabilityLayer.getDisplayUnit(book).lowercase()}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .height(48.dp)
            ) {
                FormatAdaptabilityLayer.FORMATS.forEachIndexed { index, format ->
                    val isSelected = book.format.name.equals(format, ignoreCase = true)
                    val bg = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(bg, if (index == 0) RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp) else if (index == FormatAdaptabilityLayer.FORMATS.size - 1) RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp) else RoundedCornerShape(0.dp))
                            .clickable { onSetFormat(format) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Icon(androidx.compose.material.icons.Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            val icon = when(format) {
                                "PAGES" -> androidx.compose.material.icons.Icons.Filled.CheckCircle
                                "CHAPTERS" -> Icons.Filled.FormatListNumbered
                                "VOLUMES" -> Icons.AutoMirrored.Filled.LibraryBooks
                                else -> Icons.Filled.Timer
                            }
                            if (!isSelected) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp).graphicsLayer { alpha = 0.6f }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                format,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (index < FormatAdaptabilityLayer.FORMATS.size - 1) {
                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlannerCard(book: Book, completed: List<Session>) {
    val velocity = AnalyticsEngine.pagesPerHour(completed)
    val unit = FormatAdaptabilityLayer.getDisplayUnit(book).lowercase()
    DetailCard(title = "Reading planner") {
        if (velocity == null) {
            Text(
                "Log a few timed sessions and this will estimate how much you can read in a spare moment.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "At your pace of ${velocity.roundToInt()} $unit/hour you could read about:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmartPlanningEngine.PLAN_WINDOWS_MINUTES.forEach { minutes ->
                    val units = SmartPlanningEngine.unitsInWindow(minutes, velocity) ?: 0
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("${minutes}m · ~$units") },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun BookStatsCard(book: Book, completed: List<Session>) {
    val totalMinutes = AnalyticsEngine.timedSessions(completed).sumOf { it.endTime - it.startTime } / 60_000L
    val velocity = AnalyticsEngine.pagesPerHour(completed)
    val pagesFromSessions = completed.sumOf { it.pagesRead }
    val timeLabel = if (totalMinutes < 1) "<1m" else AnalyticsEngine.formatMinutes(totalMinutes)
    val speedLabel = velocity?.let { "${it.roundToInt()}" } ?: "—"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 20.dp)) {
                Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text("YOUR READING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCell(Icons.Filled.Schedule, timeLabel, "Time", Modifier.weight(1f))
                StatCell(Icons.Filled.AutoStories, pagesFromSessions.toString(), "Pages", Modifier.weight(1f))
                StatCell(Icons.Filled.Speed, speedLabel, "Speed", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCell(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Global reading streak, plus the last 7 days of volume as a bar sparkline.
 * Bar heights are scaled against the busiest day in the window so a quiet week
 * still reads as a shape rather than seven flat stubs.
 */
@Composable
private fun ReadingStreakCard(
    streak: StreakEngine.StreakInfo,
    allSessions: List<Session>
) {
    val today = remember { LocalDate.now() }
    val week = remember(allSessions, today) {
        val pagesByDay = ReadingCalendar.pagesByDay(allSessions)
        val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val peak = days.maxOf { pagesByDay[it] ?: 0 }.coerceAtLeast(1)
        days.map { day ->
            val pages = pagesByDay[day] ?: 0
            Triple(
                day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                pages,
                if (pages > 0) (pages.toFloat() / peak).coerceIn(0.15f, 1f) else 0f
            )
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("READING STREAK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CURRENT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(dayCount(streak.currentStreak), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LONGEST", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(dayCount(streak.longestStreak), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                week.forEach { (label, pages, fraction) ->
                    val read = pages > 0
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .fillMaxHeight(if (read) fraction else 0.08f)
                                .background(
                                    if (read) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    RoundedCornerShape(50)
                                )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (read) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

private fun dayCount(days: Int): String = if (days == 1) "1 Day" else "$days Days"

@Composable
private fun RatingCard(book: Book) {
    DetailCard(title = "Your rating") {
        RatingAxis.ALL.forEach { axis ->
            val value = book.rating[axis] ?: return@forEach
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(axis, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "%.1f / 5".format(value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(book: Book) {
    DetailCard(title = "Your review") {
        Text(
            text = book.review ?: "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

internal enum class NoteSort(val label: String) {
    CREATED("Created"),
    PAGE("Page"),
    FAVORITES("Favorites")
}

@Composable
private fun NoteSortToggle(current: NoteSort, onChange: (NoteSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            onClick = { expanded = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(current.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            NoteSort.entries.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onChange(option)
                        expanded = false
                    },
                    trailingIcon = {
                        if (option == current) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                )
            }
        }
    }
}

private fun NoteType.label(): String = when (this) {
    NoteType.BOOK_CONTENT -> "Book Content"
    NoteType.PERSONAL_THOUGHT -> "Personal Thought"
    NoteType.RANDOM -> "Random"
}

@Composable
private fun NoteCard(
    note: MarginNote,
    book: Book,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: (MarginNote) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Column {
            // Type banner, matching the reference's colour-coded note headers.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Bookmark,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    note.type.label(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        Instant.ofEpochMilli(note.timestamp)
                            .atZone(ZoneId.systemDefault())
                            .format(noteStampFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        "${FormatAdaptabilityLayer.unitAbbrev(book)} ${note.pageNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    JournalingEngine.render(note.content),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onToggleFavorite()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (note.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (note.isFavorite) "Remove from favorites" else "Add to favorites",
                            modifier = Modifier.size(18.dp),
                            tint = if (note.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Note", note.content))
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "Copy note",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            onShare(note)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Share quote",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete note",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/** Format chips: the at-a-glance facts the reference puts under the header. */
@Composable
private fun MetadataChips(book: Book) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetaChip(Icons.AutoMirrored.Filled.LibraryBooks, "Book type", book.format.name)
        MetaChip(Icons.Filled.Bookmark, "Progress unit", book.progressUnit.name)
        if (book.totalPages > 0) {
            MetaChip(
                Icons.Filled.AutoStories,
                "Total ${FormatAdaptabilityLayer.getDisplayUnit(book).lowercase()}",
                "${FormatAdaptabilityLayer.unitAbbrev(book)} ${book.totalPages}"
            )
        }
    }
}

@Composable
private fun MetaChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

/**
 * The bookmark card and the "how long have I been at this" counter, side by side.
 * Day 1 is the day of the first recorded session, so a book started today reads
 * "Day 1" rather than "Day 0".
 */
@Composable
private fun ProgressAndDaysRow(
    book: Book,
    sessions: List<Session>,
    onEditProgress: () -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val startedAt = remember(sessions, book.dateAdded) {
        sessions.filter { it.startTime > 0 }.minOfOrNull { it.startTime }
            ?: book.dateAdded.takeIf { it > 0 }
    }
    val dayNumber = remember(startedAt) {
        startedAt?.let {
            val first = Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
            java.time.temporal.ChronoUnit.DAYS.between(first, LocalDate.now(zone)).toInt() + 1
        }
    }
    val fraction = if (book.totalPages > 0) book.currentPage.toFloat() / book.totalPages else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .clickable(onClick = onEditProgress)
        ) {
            // Bookmark ribbon whose fill mirrors how far through the book you are.
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(
                        com.example.booktracker.app.ui.theme.ProgressAmber,
                        RoundedCornerShape(topStart = 20.dp)
                    )
            )
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${FormatAdaptabilityLayer.unitAbbrev(book)} ${book.currentPage}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (book.totalPages > 0) {
                        Text(
                            " / ${book.totalPages}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit progress",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (book.totalPages > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${(fraction * 100).roundToInt()}% complete",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text(
                startedAt?.let {
                    "From ${Instant.ofEpochMilli(it).atZone(zone).toLocalDate().format(dateFormatter)}"
                } ?: "Not started yet",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                dayNumber?.let { "Day $it" } ?: "—",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = com.example.booktracker.app.ui.theme.ProgressAmber
            )
        }
    }
}

@Composable
private fun CollectionsCard(collections: List<String>, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .clickable(onClick = onEdit)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Folder,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        if (collections.isEmpty()) {
            Text(
                "Select collections",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                collections.forEach { name ->
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            name,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Collections are free-form and shared across the library, so this offers every
 * name already in use as a toggle and lets a new one be typed once.
 */
@Composable
private fun CollectionPickerDialog(
    selected: List<String>,
    known: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val working = remember { androidx.compose.runtime.mutableStateListOf(*selected.toTypedArray()) }
    var newName by remember { mutableStateOf("") }
    val options = remember(known, selected) { (known + selected).distinct().sorted() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Collections") },
        text = {
            Column {
                if (options.isEmpty()) {
                    Text(
                        "No collections yet — add one below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    options.forEach { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (working.contains(name)) working.remove(name) else working.add(name)
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = working.contains(name),
                                onCheckedChange = {
                                    if (working.contains(name)) working.remove(name) else working.add(name)
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New collection") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val trimmed = newName.trim()
                            if (trimmed.isNotEmpty() && !working.contains(trimmed)) {
                                working.add(trimmed)
                                newName = ""
                            }
                        },
                        enabled = newName.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add collection")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(working.toList()) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SessionRow(
    session: Session,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val date = Instant.ofEpochMilli(session.endTime).atZone(ZoneId.systemDefault()).toLocalDate()
    val month = date.format(DateTimeFormatter.ofPattern("MMM")).uppercase()
    val dayOfMonth = date.dayOfMonth.toString()
    val durationMinutes = (session.endTime - session.startTime) / 60_000L

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.2f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f), RoundedCornerShape(12.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(month, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(dayOfMonth, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${session.pagesRead} Pages Read", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (durationMinutes >= 1) {
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(AnalyticsEngine.formatMinutes(durationMinutes), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (durationMinutes >= 1 && session.pagesRead > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.1f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f))
                        Spacer(Modifier.width(6.dp))
                        Text("${(session.pagesRead * 60L / durationMinutes)} p/h", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f))
                    }
                    Spacer(Modifier.width(12.dp))
                }
                if (session.environmentTag.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f))
                        Spacer(Modifier.width(6.dp))
                        Text(session.environmentTag, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f))
                    }
                }
                if (session.deviceSource == "watch") {
                    Spacer(Modifier.width(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Watch, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f))
                    }
                }
            }
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EditSessionDialog(
    session: Session,
    book: Book,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var units by remember { mutableStateOf(session.pagesRead.toString()) }
    val newUnits = units.toIntOrNull()
    val isError = newUnits != null && (newUnits < 0 || (book.totalPages > 0 && newUnits > book.totalPages))
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Session") },
        text = {
            Column {
                OutlinedTextField(
                    value = units,
                    onValueChange = { units = it.filter { char -> char.isDigit() } },
                    label = { Text("Pages read") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError
                )
                if (isError) {
                    Text(
                        if (newUnits < 0) "Pages cannot be negative"
                        else "Cannot exceed total pages (${book.totalPages})",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    newUnits?.let { onConfirm(it) } 
                },
                enabled = newUnits != null && !isError
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun LedgerCard(book: Book) {
    DetailCard(title = "Ledger") {
        if (book.purchaseLog.isNotEmpty()) {
            Text(
                "Purchases",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            book.purchaseLog.forEach { log ->
                val date = Instant.ofEpochMilli(log.date).atZone(ZoneId.systemDefault()).toLocalDate()
                val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            log.vendor.takeIf { it.isNotBlank() } ?: "Unknown Vendor",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (log.memo.isNotBlank()) {
                            Text(
                                log.memo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${log.currency} ${"%.2f".format(log.price)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            date.format(formatter),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        if (book.purchaseLog.isNotEmpty() && book.loanRecord.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }
        
        if (book.loanRecord.isNotEmpty()) {
            Text(
                "Loan Records",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.height(8.dp))
            book.loanRecord.forEach { log ->
                val loanDate = Instant.ofEpochMilli(log.loanDate).atZone(ZoneId.systemDefault()).toLocalDate()
                val dueDate = Instant.ofEpochMilli(log.dueDate).atZone(ZoneId.systemDefault()).toLocalDate()
                val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "To: ${log.lender.takeIf { it.isNotBlank() } ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (log.memo.isNotBlank()) {
                            Text(
                                log.memo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Due: ${dueDate.format(formatter)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Lent: ${loanDate.format(formatter)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddNoteBottomSheet(
    suggestedPage: Int,
    totalPages: Int,
    initialText: String = "",
    onDismiss: () -> Unit,
    onConfirm: (page: Int, text: String, type: NoteType) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pageText by remember { mutableStateOf(if (suggestedPage > 0) "$suggestedPage" else "") }
    var noteText by remember { mutableStateOf(initialText) }
    var noteType by remember { mutableStateOf(NoteType.BOOK_CONTENT) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Add margin note", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                NoteType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = NoteType.entries.size),
                        onClick = { noteType = type },
                        selected = noteType == type
                    ) {
                        Text(type.label(), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }

            val pageInt = pageText.toIntOrNull()
            val isPageError = pageInt != null && (pageInt < 0 || (totalPages > 0 && pageInt > totalPages))
            
            OutlinedTextField(
                value = pageText,
                onValueChange = { pageText = it.filter(Char::isDigit) },
                label = { Text("Page") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = isPageError,
                shape = RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                )
            )
            if (isPageError) {
                Text(
                    if (pageInt < 0) "Page cannot be negative"
                    else "Cannot exceed total pages ($totalPages)",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Your thoughts...") },
                minLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                )
            )
            
            Button(
                enabled = noteText.isNotBlank() && !isPageError,
                onClick = { onConfirm(pageInt ?: 0, noteText.trim(), noteType) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Save Note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
