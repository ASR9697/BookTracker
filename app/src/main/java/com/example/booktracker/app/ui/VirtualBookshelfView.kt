package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.ui.animations.Motion
import com.example.booktracker.app.ui.animations.bounceOnPress
import com.example.booktracker.app.ui.animations.rememberEntranceState
import com.example.booktracker.app.ui.animations.rememberHaptics
import com.example.booktracker.app.ui.animations.staggeredEntrance
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import kotlin.math.absoluteValue

/** Shelves by where a book is in your reading life, in the order you'd scan them. */
private enum class ShelfGroup(val label: String, val icon: ImageVector) {
    READING("Reading now", Icons.Rounded.AutoStories),
    UP_NEXT("To read", Icons.Rounded.Schedule),
    FINISHED("Finished", Icons.Rounded.EmojiEvents),
    ON_HOLD("Paused & set aside", Icons.Rounded.PauseCircle);

    companion object {
        fun of(status: String): ShelfGroup = when (status.uppercase()) {
            BookStatus.READING.name -> READING
            BookStatus.FINISHED.name -> FINISHED
            BookStatus.PAUSED.name, BookStatus.ABANDONED.name, BookStatus.DNF.name -> ON_HOLD
            else -> UP_NEXT
        }
    }
}

private enum class ShelfStyle(val label: String) { SPINES("Spines"), COVERS("Covers") }

private val SPINE_GAP = 4.dp
private const val COVERS_PER_ROW = 3

/**
 * Your library as a modern shelf. Books stand on clean, theme-coloured ledges,
 * grouped by status. Each spine takes its colour from the cover and its size
 * from the page count. Tap a book to lift it and see a preview; long-press for
 * quick actions. Switch to Covers to see them face-out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualBookshelfView(
    books: List<Book>,
    onOpenBook: (Book) -> Unit,
    onStartReading: (Book) -> Unit,
    modifier: Modifier = Modifier,
    onChangeStatus: ((Book, BookStatus) -> Unit)? = null,
    /** Told when the preview card opens or closes, so floating buttons can make room. */
    onSelectionChange: (Boolean) -> Unit = {}
) {
    var style by rememberSaveable { mutableStateOf(ShelfStyle.SPINES) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var actionsFor by remember { mutableStateOf<Book?>(null) }
    val selected = remember(selectedId, books) { books.find { it.id == selectedId } }
    val reportSelection by rememberUpdatedState(onSelectionChange)
    LaunchedEffect(selected != null) { reportSelection(selected != null) }
    DisposableEffect(Unit) { onDispose { reportSelection(false) } }
    val haptics = rememberHaptics()
    val entrance = rememberEntranceState()
    val groups = remember(books) {
        books.groupBy { ShelfGroup.of(it.status) }.toSortedMap(compareBy { it.ordinal })
    }

    actionsFor?.let { book ->
        BookQuickActionsSheet(
            book = book,
            onDismiss = { actionsFor = null },
            onOpen = { onOpenBook(book) },
            onStartReading = { onStartReading(book) }.takeIf { book.status != BookStatus.FINISHED.name },
            onChangeStatus = onChangeStatus?.let { change -> { status -> change(book, status) } }
        )
    }

    if (books.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            title = "An empty shelf",
            message = "Books you add will stand here, grouped by what you're reading, what's next and what you've finished.",
            modifier = modifier
        )
        return
    }

    val select: (Book) -> Unit = { book ->
        haptics.tick()
        selectedId = if (selectedId == book.id) null else book.id
    }
    val longPress: (Book) -> Unit = { book ->
        haptics.longPress()
        actionsFor = book
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 200.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    val finished = groups[ShelfGroup.FINISHED]?.size ?: 0
                    Text(
                        "${books.size} ${if (books.size == 1) "book" else "books"} · $finished finished",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    SingleChoiceSegmentedButtonRow {
                        ShelfStyle.entries.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = style == option,
                                onClick = {
                                    if (style != option) haptics.tick()
                                    style = option
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, ShelfStyle.entries.size),
                                icon = {},
                                label = { Text(option.label, maxLines = 1) }
                            )
                        }
                    }
                }
            }

            var groupIndex = 0
            groups.forEach { (group, shelfBooks) ->
                val index = groupIndex++
                item(key = group.name) {
                    Column(modifier = Modifier.staggeredEntrance(entrance, index)) {
                        ShelfHeader(group, shelfBooks.size)
                        when (style) {
                            ShelfStyle.SPINES -> SpineShelf(shelfBooks, selectedId, select, longPress)
                            ShelfStyle.COVERS -> CoverShelf(shelfBooks, selectedId, select, longPress)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selected != null,
            enter = slideInVertically(tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate)) { it } + fadeIn(),
            exit = slideOutVertically(tween(Motion.SHORT, easing = Motion.EmphasizedAccelerate)) { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            // Keep the last book on screen while the card slides away.
            var lastShown by remember { mutableStateOf<Book?>(null) }
            SideEffect { if (selected != null) lastShown = selected }
            (selected ?: lastShown)?.let { book ->
                ShelfInspector(
                    book = book,
                    onOpen = { onOpenBook(book) },
                    onRead = { onStartReading(book) },
                    onDismiss = { selectedId = null }
                )
            }
        }
    }
}

@Composable
private fun ShelfHeader(group: ShelfGroup, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
    ) {
        Icon(group.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(group.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
            Text(
                "$count",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

/** Width by page count: a novella is slim, a doorstop is wide. */
private fun spineWidth(book: Book): Dp {
    if (book.totalPages <= 0) return 34.dp
    val fraction = (book.totalPages.coerceIn(80, 900) - 80) / 820f
    return (24f + fraction * 26f).dp
}

/** Height by page count with a little per-title variation, so a row looks like real books. */
private fun spineHeight(book: Book): Dp {
    val fraction = if (book.totalPages <= 0) 0.4f else (book.totalPages.coerceIn(80, 900) - 80) / 820f
    val jitter = (book.title.hashCode().absoluteValue % 13) - 6
    return (148f + fraction * 34f + jitter).dp
}

@Composable
private fun SpineShelf(
    books: List<Book>,
    selectedId: String?,
    onSelect: (Book) -> Unit,
    onLongPress: (Book) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // Fill each ledge left to right, wrapping when the next spine wouldn't fit.
        val rows = remember(books, maxWidth) {
            val result = mutableListOf<MutableList<Book>>()
            var used = 0.dp
            books.forEach { book ->
                val width = spineWidth(book) + SPINE_GAP
                if (result.isEmpty() || used + width > maxWidth - 8.dp) {
                    result += mutableListOf(book)
                    used = width
                } else {
                    result.last() += book
                    used += width
                }
            }
            result
        }
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            rows.forEach { row ->
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(SPINE_GAP),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        row.forEach { book ->
                            BookSpine(
                                book = book,
                                selected = book.id == selectedId,
                                onClick = { onSelect(book) },
                                onLongClick = { onLongPress(book) }
                            )
                        }
                    }
                    Ledge()
                }
            }
        }
    }
}

@Composable
private fun CoverShelf(
    books: List<Book>,
    selectedId: String?,
    onSelect: (Book) -> Unit,
    onLongPress: (Book) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        books.chunked(COVERS_PER_ROW).forEach { row ->
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    row.forEach { book ->
                        FaceOutCover(
                            book = book,
                            selected = book.id == selectedId,
                            onClick = { onSelect(book) },
                            onLongClick = { onLongPress(book) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(COVERS_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Ledge()
            }
        }
    }
}

/** The surface books stand on: a rounded ledge with a soft shadow falling below it. */
@Composable
private fun Ledge() {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(10.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )
    }
}

/** A colour for books whose cover hasn't loaded or has none, nudged toward the theme so it fits. */
@Composable
private fun fallbackSpineColor(book: Book): Color {
    val hues = listOf(0xFF5C6BC0, 0xFF26A69A, 0xFFEF6C00, 0xFF8D6E63, 0xFFAB47BC, 0xFF42A5F5, 0xFF66BB6A, 0xFFEC407A)
    val base = Color(hues[book.title.hashCode().absoluteValue % hues.size])
    return lerp(base, MaterialTheme.colorScheme.primary, 0.25f)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookSpine(book: Book, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val accent = rememberCoverAccent(book.coverUrl)
    val fallback = fallbackSpineColor(book)
    val color = accent.dominant ?: accent.vibrant ?: fallback
    val ink = if (color.luminance() > 0.5f) Color(0xFF1C1B1F) else Color.White
    val width = spineWidth(book)
    val height = spineHeight(book)
    val lift by animateDpAsState(if (selected) (-16).dp else 0.dp, Motion.press(), label = "lift")
    val press = remember { MutableInteractionSource() }
    val progress = if (book.status == BookStatus.READING.name && book.totalPages > 0)
        (book.currentPage.toFloat() / book.totalPages).coerceIn(0f, 1f) else null
    val shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 3.dp, bottomEnd = 3.dp)

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .graphicsLayer { translationY = lift.toPx() }
            .bounceOnPress(press)
            .shadow(if (selected) 12.dp else 2.dp, shape)
            .clip(shape)
            // Lit from the left, so a flat colour still reads as a rounded spine.
            .background(Brush.horizontalGradient(listOf(lerp(color, Color.White, 0.14f), color, lerp(color, Color.Black, 0.16f))))
            .onSecondaryClick(onLongClick)
            .combinedClickable(
                interactionSource = press,
                indication = null,
                onClick = onClick,
                onLongClickLabel = "Book actions",
                onLongClick = onLongClick
            )
    ) {
        // Head band and a favourite mark near the top.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .fillMaxWidth(0.7f)
                .height(2.dp)
                .background(ink.copy(alpha = 0.35f), CircleShape)
        )
        if (book.isFavorite) {
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = "Favourite",
                tint = ink.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp).size(10.dp)
            )
        }
        Text(
            book.title,
            style = if (width < 30.dp) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(vertical = 34.dp)
                .verticalText()
        )
        // Reading progress as a slim rail at the foot of the spine.
        if (progress != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 5.dp, end = 5.dp, bottom = 8.dp)
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(ink.copy(alpha = 0.25f))
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(ink))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FaceOutCover(
    book: Book,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lift by animateDpAsState(if (selected) (-12).dp else 0.dp, Motion.press(), label = "lift")
    val press = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .graphicsLayer { translationY = lift.toPx() }
            .bounceOnPress(press)
            .shadow(if (selected) 12.dp else 4.dp, MaterialTheme.shapes.small)
            .onSecondaryClick(onLongClick)
            .combinedClickable(
                interactionSource = press,
                indication = null,
                onClick = onClick,
                onLongClickLabel = "Book actions",
                onLongClick = onLongClick
            )
    ) {
        BookCover(book, Modifier.heroSharedElement("cover-${book.id}").fillMaxSize())
        if (book.status == BookStatus.READING.name && book.totalPages > 0) {
            LinearProgressIndicator(
                progress = { (book.currentPage.toFloat() / book.totalPages).coerceIn(0f, 1f) },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Black.copy(alpha = 0.4f),
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
    }
}

/** The card that rises when a book is lifted off the shelf. */
@Composable
private fun ShelfInspector(book: Book, onOpen: () -> Unit, onRead: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        shape = AppShape.XL,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                BookCover(book, Modifier.size(width = 64.dp, height = 96.dp))
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (book.authors.isNotEmpty()) {
                        Text(
                            book.authors.joinToString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    val status = BookStatus.entries.firstOrNull { it.name.equals(book.status, ignoreCase = true) }
                    val detail = buildList {
                        status?.let { add(it.label) }
                        if (book.totalPages > 0) add("${book.totalPages} pages")
                        book.overallRating?.takeIf { it > 0f }?.let { add("★ %.1f".format(it)) }
                    }.joinToString(" · ")
                    Text(detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    if (status == BookStatus.READING && book.totalPages > 0) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (book.currentPage.toFloat() / book.totalPages).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            gapSize = 0.dp,
                            drawStopIndicator = {}
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Put back", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onOpen, modifier = Modifier.weight(1f)) { Text("Open book") }
                if (book.status != BookStatus.FINISHED.name) {
                    Button(onClick = onRead, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (book.status == BookStatus.READING.name) "Read" else "Start")
                    }
                }
            }
        }
    }
}

/** Lays text out rotated a quarter turn, reading bottom to top like a spine. */
private fun Modifier.verticalText(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(
        Constraints(maxWidth = constraints.maxHeight, maxHeight = constraints.maxWidth)
    )
    layout(placeable.height, placeable.width) {
        placeable.placeWithLayer(
            x = (placeable.height - placeable.width) / 2,
            y = (placeable.width - placeable.height) / 2
        ) { rotationZ = -90f }
    }
}
