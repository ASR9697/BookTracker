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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.verticalScroll
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
import com.example.booktracker.shared.models.Ownership
import com.example.booktracker.shared.models.RatingAxis
import com.example.booktracker.shared.models.Session
import com.example.booktracker.shared.models.LoanRecord
import com.example.booktracker.app.analytics.LoanEngine
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Badge

enum class BookDetailTab(val label: String) {
    OVERVIEW("Overview"),
    SESSIONS("Sessions"),
    NOTES("Notes"),
    STATS("Stats")
}

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val noteStampFormatter = DateTimeFormatter.ofPattern("M/d/yyyy h:mm a")

@Composable
private fun SectionItem(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp)
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookDetailTabRow(
    selectedTab: BookDetailTab,
    onTabSelected: (BookDetailTab) -> Unit,
    noteCount: Int,
    sessionCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
        ) {
            BookDetailTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                tab.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                            )
                            when (tab) {
                                BookDetailTab.NOTES -> if (noteCount > 0) {
                                    Badge(
                                        containerColor = if (selectedTab == tab)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceContainerHighest
                                    ) {
                                        Text("$noteCount")
                                    }
                                }
                                BookDetailTab.SESSIONS -> if (sessionCount > 0) {
                                    Badge(
                                        containerColor = if (selectedTab == tab)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceContainerHighest
                                    ) {
                                        Text("$sessionCount")
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                )
            }
        }
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
    onUpdateNote: (noteId: String, page: Int, text: String, type: NoteType) -> Unit,
    onDeleteNote: (String) -> Unit,
    onToggleNoteFavorite: (String) -> Unit,
    onSaveDetails: (Book) -> Unit,
    onChangeStatus: (BookStatus) -> Unit,
    onMarkDnf: (Float, String) -> Unit,
    onLogSession: (startPage: Int, endPage: Int, minutes: Int, endTime: Long) -> Unit,
    onSetProgress: (Int) -> Unit,
    onSetCollections: (List<String>) -> Unit,
    allCollections: List<String>,
    onSetFormat: (String) -> Unit,
    onSetOwnership: (Ownership) -> Unit = {},
    onDelete: () -> Unit,
    onStartReading: () -> Unit,
    onReadAgain: () -> Unit,
    onEditSession: (Session, Int) -> Unit,
    onDeleteSession: (Session) -> Unit,
    onToggleFavorite: () -> Unit,
    onFinish: (Map<String, Float>, String?) -> Unit,
    onRecordLoan: (LoanRecord) -> Unit = {},
    onMarkLoanReturned: () -> Unit = {},
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(BookDetailTab.OVERVIEW) }
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
    var noteToShare by remember { mutableStateOf<MarginNote?>(null) }
    var showVictoryCard by remember { mutableStateOf(false) }
    var showLoanDialog by remember { mutableStateOf(false) }
    var showEditDetails by remember { mutableStateOf(false) }
    var showStatusPicker by remember { mutableStateOf(false) }
    var showDnfDialog by remember { mutableStateOf(false) }
    var showLogSession by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<MarginNote?>(null) }
    var noteToDelete by remember { mutableStateOf<MarginNote?>(null) }
    // Offer the finish prompt once per arrival at the last page, not on every recomposition.
    var finishPromptHandled by remember { mutableStateOf(false) }
    val activeLoan = remember(book?.loanRecord) { book?.let { LoanEngine.activeLoan(it) } }
    val loanStatus = remember(activeLoan) { activeLoan?.let { LoanEngine.getLoanStatus(it) } }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    if (showLoanDialog && book != null) {
        LoanRecordDialog(
            existingLoan = activeLoan,
            onDismiss = { showLoanDialog = false },
            onSave = { record ->
                onRecordLoan(record)
                showLoanDialog = false
            }
        )
    }

    val atLastPage = book != null && book.totalPages > 0 && book.currentPage >= book.totalPages &&
        book.status == BookStatus.READING.name
    LaunchedEffect(atLastPage) {
        if (atLastPage && !finishPromptHandled) {
            finishPromptHandled = true
            showFinishDialog = true
        } else if (!atLastPage) {
            finishPromptHandled = false
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

    val completed = sessions.filter { it.endTime > 0 }
    val completionPrediction = remember(book, completed, allSessions) {
        AnalyticsEngine.predictCompletionDate(book, completed, allSessions)
    }

    // The header always carries white text, so its fallback (no cover / not loaded
    // yet) must be dark too — the light theme's surface colours are not.
    val headerColor = accent.immersiveBackground(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f).compositeOver(Color(0xFF1B2430))
    )

    // The cover tints the header only; tabs, buttons and highlights keep the app's
    // one accent so every screen reads as the same app.
    MaterialTheme(colorScheme = MaterialTheme.colorScheme) {
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
                    onStartReading = onStartReading,
                    onReadAgain = onReadAgain,
                    onAddNote = { showAddNote = true },
                    onStatusClick = { showStatusPicker = true }
                )
            }

            item { Spacer(Modifier.height(12.dp)) }

            item {
                BookDetailTabRow(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    noteCount = notes.size,
                    sessionCount = completed.size
                )
            }

            item { Spacer(Modifier.height(8.dp)) }

            when (selectedTab) {
                BookDetailTab.OVERVIEW -> {
                    item {
                        SectionItem {
                            ProgressAndDaysRow(
                                book = book,
                                sessions = completed,
                                allSessions = allSessions,
                                onEditProgress = { showProgressPicker = true }
                            )
                        }
                    }

                    if (completionPrediction != null) {
                        item {
                            SectionItem {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.Flag,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "PREDICTED FINISH",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                completionPrediction.formattedPrediction,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            val remainingPages = (book.totalPages - book.currentPage).coerceAtLeast(0)
                                            Text(
                                                "$remainingPages ${FormatAdaptabilityLayer.getDisplayUnit(book).lowercase()} remaining at your reading pace",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

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

                    if (loanStatus != null && (loanStatus.urgency == LoanEngine.LoanUrgency.OVERDUE || loanStatus.urgency == LoanEngine.LoanUrgency.DUE_SOON)) {
                        item {
                                SectionItem {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (loanStatus.urgency == LoanEngine.LoanUrgency.OVERDUE)
                                            MaterialTheme.colorScheme.errorContainer
                                        else
                                            MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = if (loanStatus.urgency == LoanEngine.LoanUrgency.OVERDUE)
                                            MaterialTheme.colorScheme.onErrorContainer
                                        else
                                            MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                if (loanStatus.urgency == LoanEngine.LoanUrgency.OVERDUE) Icons.Filled.Warning else Icons.Filled.Schedule,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                loanStatus.detailText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                    }

                    item {
                        SectionItem {
                            LoanDetailCard(
                                book = book,
                                activeLoan = activeLoan,
                                onRecordLoan = onRecordLoan,
                                onMarkReturned = onMarkLoanReturned
                            )
                        }
                    }

                    if (book.purchaseLog.isNotEmpty()) {
                        item {
                            SectionItem {
                                LedgerCard(book)
                            }
                        }
                    }

                    item { SectionItem { FormatCard(book, onSetFormat) } }
                    item { SectionItem { OwnershipCard(book, onSetOwnership) } }

                    if (book.status == BookStatus.READING.name) {
                        item { SectionItem { PlannerCard(book, completed) } }
                    }

                    if (book.status == BookStatus.FINISHED.name && book.rating.isNotEmpty()) {
                        item { SectionItem { RatingCard(book, onEdit = { showFinishDialog = true }) } }
                    } else if (book.status == BookStatus.FINISHED.name) {
                        item {
                            SectionItem {
                                DetailCard(title = "Your rating") {
                                    Text(
                                        "You haven't rated this book yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    FilledTonalButton(onClick = { showFinishDialog = true }) { Text("Rate & review") }
                                }
                            }
                        }
                    }
                    if (book.status == BookStatus.FINISHED.name && !book.review.isNullOrBlank()) {
                        item { SectionItem { ReviewCard(book) } }
                    }
                    if (book.status == BookStatus.FINISHED.name) {
                        item {
                            SectionItem {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
                                        .clickable { showVictoryCard = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.EmojiEvents,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "READING VICTORY",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                "Finished Wrap-Up Card",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Icon(
                                            Icons.Filled.Share,
                                            contentDescription = "Share",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
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
                }

                BookDetailTab.SESSIONS -> {
                    item {
                        SectionItem {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Session History",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilledTonalButton(onClick = { showLogSession = true }) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Log")
                                    }
                                }
                                if (completed.isNotEmpty()) {
                                    val totalMinutes = AnalyticsEngine.timedSessions(completed).sumOf { it.endTime - it.startTime } / 60_000L
                                    val totalPages = completed.sumOf { it.pagesRead }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${completed.size} reading sessions · $totalPages pages · ${AnalyticsEngine.formatMinutes(totalMinutes)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    if (completed.isEmpty()) {
                        item {
                            SectionItem {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Filled.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            "No reading sessions yet",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "Start the reading timer to track sessions and pace automatically, or tap Log to add one you've already read.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(completed, key = { it.id }) { session ->
                            SectionItem(modifier = Modifier.animateItem()) {
                                SessionRow(
                                    session = session,
                                    onEdit = { sessionToEdit = session },
                                    onDelete = { sessionToDelete = session }
                                )
                            }
                        }
                    }
                }

                BookDetailTab.NOTES -> {
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
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Notes & Quotes",
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
                                            onClick = { showOcr = true },
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                                                .size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.DocumentScanner,
                                                contentDescription = "Scan passage",
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
                                if (notes.isNotEmpty()) {
                                    Spacer(Modifier.height(10.dp))
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
                    }
                    if (notes.isEmpty()) {
                        item {
                            SectionItem {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Filled.Bookmark,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            "No notes or quotes yet",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "Capture memorable passages, personal thoughts, or scan book pages with OCR.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
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
                            SectionItem(modifier = Modifier.animateItem()) {
                                NoteCard(
                                    note = note,
                                    book = book,
                                    onEdit = { noteToEdit = note },
                                    onDelete = { noteToDelete = note },
                                    onToggleFavorite = { onToggleNoteFavorite(note.id) },
                                    onShare = { noteToShare = note }
                                )
                            }
                        }
                    }
                }

                BookDetailTab.STATS -> {
                    item { SectionItem { BookStatsCard(book, completed, allSessions) } }
                    item { SectionItem { ReadingStreakCard(streak, allSessions) } }
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
                            text = { Text("Edit details") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showOverflow = false
                                showEditDetails = true
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Change status") },
                            leadingIcon = { Icon(Icons.Filled.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showOverflow = false
                                showStatusPicker = true
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Log a reading session") },
                            leadingIcon = { Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showOverflow = false
                                showLogSession = true
                            }
                        )
                        if (book.status == BookStatus.FINISHED.name) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Victory Card") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.EmojiEvents,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showOverflow = false
                                    showVictoryCard = true
                                }
                            )
                        }
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(if (activeLoan != null) "Manage Loan" else "Record Loan / Borrow") },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                showOverflow = false
                                showLoanDialog = true
                            }
                        )
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
                book = book,
                onStartReading = onStartReading,
                onReadAgain = onReadAgain,
                onAddNote = { showAddNote = true },
                container = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }
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

    noteToEdit?.let { note ->
        AddNoteBottomSheet(
            suggestedPage = note.pageNumber,
            totalPages = book.totalPages,
            initialText = note.content,
            initialType = note.type,
            isEditing = true,
            onDismiss = { noteToEdit = null },
            onConfirm = { page, text, type ->
                onUpdateNote(note.id, page, text, type)
                noteToEdit = null
            }
        )
    }

    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete this note?") },
            text = { Text(note.content.take(140) + if (note.content.length > 140) "…" else "") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteNote(note.id)
                    noteToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { noteToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showEditDetails) {
        EditBookDialog(
            book = book,
            onDismiss = { showEditDetails = false },
            onSave = {
                onSaveDetails(it)
                showEditDetails = false
            }
        )
    }

    if (showStatusPicker) {
        StatusPickerDialog(
            current = book.status,
            onDismiss = { showStatusPicker = false },
            onSelect = { status ->
                showStatusPicker = false
                when (status) {
                    BookStatus.FINISHED -> showFinishDialog = true
                    BookStatus.DNF -> showDnfDialog = true
                    else -> if (!book.status.equals(status.name, ignoreCase = true)) onChangeStatus(status)
                }
            }
        )
    }

    if (showDnfDialog) {
        DnfDialog(
            book = book,
            onDismiss = { showDnfDialog = false },
            onConfirm = { percent, reason ->
                onMarkDnf(percent, reason)
                showDnfDialog = false
            }
        )
    }

    if (showLogSession) {
        LogSessionDialog(
            book = book,
            onDismiss = { showLogSession = false },
            onConfirm = { start, end, minutes, endTime ->
                onLogSession(start, end, minutes, endTime)
                showLogSession = false
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

    noteToShare?.let { note ->
        QuoteCardDialog(
            note = note,
            book = book,
            onDismiss = { noteToShare = null }
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
            onConfirm = { rating, review ->
                val wasFinished = book.status == BookStatus.FINISHED.name
                onFinish(rating, review)
                showFinishDialog = false
                // Celebrate a newly finished book; re-rating an old one just saves.
                if (!wasFinished) showVictoryCard = true
            }
        )
    }

    if (showVictoryCard) {
        BookVictoryCardDialog(
            book = book,
            sessions = sessions,
            notes = notes,
            onDismiss = { showVictoryCard = false }
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
    onReadAgain: () -> Unit,
    onAddNote: () -> Unit,
    onStatusClick: () -> Unit
) {
    val onHeader = Color.White

    // The bottom padding reserves room for the half of the pill that hangs below
    // the header; without it the next list item paints over the pill.
    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
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
                    book.overallRating?.let { stars ->
                        Spacer(Modifier.height(8.dp))
                        StarRatingDisplay(stars, starSize = 16.dp)
                    }
                    Spacer(Modifier.height(12.dp))
                    StatusPill(book.status, onClick = onStatusClick)
                }
            }
        }

        // Straddles the header/content boundary, as in the reference.
        ActionPill(
            book = book,
            onStartReading = onStartReading,
            onReadAgain = onReadAgain,
            onAddNote = onAddNote,
            container = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp)
                .offset(y = 28.dp)
        )
    }
}

/**
 * The two-action pill: open the reading timer (or start a re-read of an ended
 * book), or capture a note.
 */
@Composable
private fun ActionPill(
    book: Book,
    onStartReading: () -> Unit,
    onReadAgain: () -> Unit,
    onAddNote: () -> Unit,
    container: Color,
    modifier: Modifier = Modifier
) {
    val ended = book.status == BookStatus.FINISHED.name || book.status == BookStatus.DNF.name
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = if (ended) onReadAgain else onStartReading, modifier = Modifier.size(56.dp)) {
                Icon(
                    if (ended) Icons.Filled.Replay else Icons.Filled.PlayCircleOutline,
                    contentDescription = if (ended) "Read again" else "Start reading timer",
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
private fun StatusPill(status: String, onClick: () -> Unit) {
    val label = statusLabel(status)
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.18f),
        contentColor = Color.White,
        onClick = onClick,
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 6.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Icon(
                androidx.compose.material.icons.Icons.Filled.ArrowDropDown,
                contentDescription = "Change status",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    BookStatus.TO_READ.name, "BACKLOG" -> "Want to read"
    BookStatus.SHORTLIST.name -> "Shortlist"
    BookStatus.UP_NEXT.name -> "Up next"
    BookStatus.READING.name -> "Reading"
    BookStatus.PAUSED.name -> "Paused"
    BookStatus.FINISHED.name -> "Finished"
    BookStatus.DNF.name, BookStatus.ABANDONED.name -> "Did not finish"
    else -> status
}

/** Every pipeline status the reader can move a book to, with a one-line explanation. */
@Composable
private fun StatusPickerDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (BookStatus) -> Unit
) {
    val options = listOf(
        BookStatus.TO_READ to "On your someday pile",
        BookStatus.SHORTLIST to "Candidates for your next read",
        BookStatus.UP_NEXT to "Starting soon",
        BookStatus.READING to "Currently reading",
        BookStatus.PAUSED to "On hold for now",
        BookStatus.FINISHED to "Read to the end",
        BookStatus.DNF to "Stopped partway"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Book status") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (status, hint) ->
                    val selected = current.equals(status.name, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(status) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(selected = selected, onClick = { onSelect(status) })
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(statusLabel(status.name), style = MaterialTheme.typography.bodyLarge)
                            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
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
            Text("Format", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Which edition you're reading. Progress is tracked in ${FormatAdaptabilityLayer.getDisplayUnit(book).lowercase()}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // Two rows of two: four labelled options in one row don't fit a phone.
            FormatAdaptabilityLayer.FORMATS.chunked(2).forEach { rowFormats ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowFormats.forEach { format ->
                        val isSelected = book.format.name.equals(format, ignoreCase = true)
                        val icon = when (format) {
                            "Paperback" -> Icons.AutoMirrored.Filled.MenuBook
                            "Hardcover" -> Icons.AutoMirrored.Filled.LibraryBooks
                            "E-book" -> Icons.Filled.TabletAndroid
                            else -> Icons.Filled.Headphones
                        }
                        androidx.compose.material3.FilterChip(
                            selected = isSelected,
                            onClick = { onSetFormat(format) },
                            label = { Text(format, maxLines = 1) },
                            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnershipCard(book: Book, onSetOwnership: (Ownership) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Ownership", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (book.ownership == Ownership.NONE) "Do you own this book, was it a gift, or is it on your wishlist?"
                else "Tap again to clear. Borrowed books are tracked under loans.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SelectableOwnerships.forEach { option ->
                    val isSelected = book.ownership == option
                    androidx.compose.material3.FilterChip(
                        selected = isSelected,
                        onClick = { onSetOwnership(if (isSelected) Ownership.NONE else option) },
                        label = { Text(option.label, maxLines = 1) },
                        leadingIcon = { Icon(option.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f)
                    )
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
private fun BookStatsCard(book: Book, completed: List<Session>, allSessions: List<Session>) {
    val totalMinutes = AnalyticsEngine.timedSessions(completed).sumOf { it.endTime - it.startTime } / 60_000L
    val velocity = AnalyticsEngine.bookPagesPerHour(completed, allSessions)
    val pagesFromSessions = completed.sumOf { it.pagesRead }
    val timeLabel = if (totalMinutes < 1) "<1m" else AnalyticsEngine.formatMinutes(totalMinutes)
    val speedLabel = AnalyticsEngine.formatReadingSpeed(velocity) ?: "—"
    val minutesLeft = AnalyticsEngine.bookEstimatedMinutesLeft(book, completed, allSessions)
    val etaLabel = if (book.status == BookStatus.FINISHED.name || (book.totalPages > 0 && book.currentPage >= book.totalPages)) {
        "Finished"
    } else {
        minutesLeft?.let { AnalyticsEngine.formatMinutes(it) } ?: "—"
    }

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
                Text("YOUR READING STATS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCell(Icons.Filled.Schedule, timeLabel, "Total Time", Modifier.weight(1f))
                StatCell(Icons.Filled.AutoStories, pagesFromSessions.toString(), "Pages Read", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCell(Icons.Filled.Speed, speedLabel, "Reading Pace", Modifier.weight(1f))
                StatCell(Icons.Filled.Timer, etaLabel, if (etaLabel == "Finished") "Status" else "Time Left", Modifier.weight(1f))
            }

            val prediction = AnalyticsEngine.predictCompletionDate(book, completed, allSessions)
            if (prediction != null) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "ESTIMATED FINISH DATE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                prediction.formattedPrediction,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
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
private fun RatingCard(book: Book, onEdit: () -> Unit) {
    DetailCard(title = "Your rating") {
        book.overallRating?.let { stars ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEdit)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StarRatingDisplay(stars, starSize = 24.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    "${formatRating(stars)} / 5",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
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

internal fun NoteType.label(): String = when (this) {
    NoteType.BOOK_CONTENT -> "Quote"
    NoteType.PERSONAL_THOUGHT -> "Personal thought"
    NoteType.RANDOM -> "Other"
}

internal fun NoteType.shortLabel(): String = when (this) {
    NoteType.BOOK_CONTENT -> "Quote"
    NoteType.PERSONAL_THOUGHT -> "Thought"
    NoteType.RANDOM -> "Other"
}

@Composable
private fun NoteCard(
    note: MarginNote,
    book: Book,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: (MarginNote) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Card(
        onClick = onEdit,
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
    allSessions: List<Session>,
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
    val minutesLeft = remember(book, sessions, allSessions) {
        AnalyticsEngine.bookEstimatedMinutesLeft(book, sessions, allSessions)
    }
    val etaText = remember(minutesLeft) { AnalyticsEngine.formatEstimatedTime(minutesLeft) }

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
            com.example.booktracker.app.ui.animations.LiquidWaveProgressBar(
                progress = fraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                height = 6.dp,
                waveColor = com.example.booktracker.app.ui.theme.ProgressAmber,
                secondaryWaveColor = com.example.booktracker.app.ui.theme.ProgressAmber.copy(alpha = 0.5f),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f),
                cornerRadius = 0.dp
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
                    if (fraction < 1f && etaText != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "⏱ $etaText",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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
    initialType: NoteType = NoteType.BOOK_CONTENT,
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (page: Int, text: String, type: NoteType) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pageText by remember { mutableStateOf(if (suggestedPage > 0) "$suggestedPage" else "") }
    var noteText by remember { mutableStateOf(initialText) }
    var noteType by remember { mutableStateOf(initialType) }
    
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
            Text(
                if (isEditing) "Edit note" else "Add a note",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                NoteType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = NoteType.entries.size),
                        onClick = { noteType = type },
                        selected = noteType == type,
                        icon = {}
                    ) {
                        // Short labels: the full names don't fit three segments on a phone.
                        Text(type.shortLabel(), style = MaterialTheme.typography.labelLarge, maxLines = 1)
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
                Text(if (isEditing) "Save changes" else "Save note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
