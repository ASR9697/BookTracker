package com.example.booktracker.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.booktracker.app.analytics.LoanEngine
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.booktracker.app.ui.animations.bounceClick
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import kotlin.math.absoluteValue

/**
 * Aesthetic wood styles for the virtual bookcase.
 */
enum class BookshelfWoodTheme(
    val title: String,
    val backboardColor: Color,
    val plankHighlightColor: Color,
    val plankMainColor: Color,
    val plankShadowColor: Color,
    val brassTrimColor: Color
) {
    DARK_MAHOGANY(
        title = "Dark Mahogany",
        backboardColor = Color(0xFF160D08),
        plankHighlightColor = Color(0xFF5A3118),
        plankMainColor = Color(0xFF381D0D),
        plankShadowColor = Color(0xFF140A04),
        brassTrimColor = Color(0xFFD4AF37)
    ),
    CLASSIC_WALNUT(
        title = "Classic Walnut",
        backboardColor = Color(0xFF1E1510),
        plankHighlightColor = Color(0xFF6B4226),
        plankMainColor = Color(0xFF4A2B15),
        plankShadowColor = Color(0xFF1A0E07),
        brassTrimColor = Color(0xFFE5C07B)
    ),
    OBSIDIAN_SLATE(
        title = "Obsidian Slate",
        backboardColor = Color(0xFF121417),
        plankHighlightColor = Color(0xFF2C3238),
        plankMainColor = Color(0xFF1B1E22),
        plankShadowColor = Color(0xFF0D0E10),
        brassTrimColor = Color(0xFF64B5F6)
    )
}

/**
 * Virtual 3D Spine Bookshelf View (Bookmory Flagship Feature).
 * Displays books standing side-by-side with page-proportional spine thickness,
 * cover/genre derived bookbinding textures, embossed metallic titles, dangling bookmark ribbons,
 * and smooth pull-out animations.
 */
@Composable
fun VirtualBookshelfView(
    books: List<Book>,
    onOpenBook: (Book) -> Unit,
    onStartReading: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var selectedWoodTheme by remember { mutableStateOf(BookshelfWoodTheme.DARK_MAHOGANY) }
    var selectedBookId by remember { mutableStateOf<String?>(null) }
    val selectedBook = remember(selectedBookId, books) {
        books.find { it.id == selectedBookId }
    }

    // Distribute books across realistic shelves (~6 to 8 books per shelf tier)
    val shelves = remember(books) {
        if (books.isEmpty()) emptyList()
        else books.chunked(7)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(selectedWoodTheme.backboardColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Wood style selector bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Finish:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
                BookshelfWoodTheme.entries.forEach { theme ->
                    val isSelected = selectedWoodTheme == theme
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) theme.brassTrimColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                        border = if (isSelected) BorderStroke(1.dp, theme.brassTrimColor) else null,
                        modifier = Modifier.bounceClick {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedWoodTheme = theme
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(theme.plankMainColor)
                                    .border(0.8.dp, theme.brassTrimColor, CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                theme.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) theme.brassTrimColor else Color.White.copy(alpha = 0.8f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            if (books.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Your bookshelf is empty.\nAdd or categorize books to stock your shelves.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    itemsIndexed(shelves, key = { index, _ -> "shelf_$index" }) { shelfIndex, shelfBooks ->
                        BookshelfRow(
                            books = shelfBooks,
                            shelfNumber = shelfIndex + 1,
                            woodTheme = selectedWoodTheme,
                            selectedBookId = selectedBookId,
                            onSelectBook = { book ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedBookId = if (selectedBookId == book.id) null else book.id
                            }
                        )
                    }
                }
            }
        }

        // Pull-out Book Inspector Floating Sheet
        AnimatedVisibility(
            visible = selectedBook != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedBook?.let { book ->
                BookPullOutInspector(
                    book = book,
                    woodTheme = selectedWoodTheme,
                    onOpenBook = { onOpenBook(book) },
                    onStartReading = { onStartReading(book) },
                    onDismiss = { selectedBookId = null }
                )
            }
        }
    }
}

/**
 * A single wooden shelf tier supporting a row of books standing side by side.
 */
@Composable
private fun BookshelfRow(
    books: List<Book>,
    shelfNumber: Int,
    woodTheme: BookshelfWoodTheme,
    selectedBookId: String?,
    onSelectBook: (Book) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Book spines resting on the shelf
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Left brass decorative bookend
            BrassBookend(isLeft = true, trimColor = woodTheme.brassTrimColor)

            books.forEach { book ->
                val isSelected = selectedBookId == book.id
                BookSpineItem(
                    book = book,
                    isSelected = isSelected,
                    onClick = { onSelectBook(book) }
                )
            }

            // Right brass decorative bookend
            BrassBookend(isLeft = false, trimColor = woodTheme.brassTrimColor)
        }

        // 3D Wooden Shelf Plank
        WoodenShelfPlank(woodTheme = woodTheme)
    }
}

/**
 * Individual 3D book spine standing vertically with page-proportional thickness,
 * realistic curvature shading, embossed typography, and ribbon bookmarks.
 */
@Composable
private fun BookSpineItem(
    book: Book,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val coverAccent = rememberCoverAccent(book.coverUrl)
    val baseColor = remember(book, coverAccent) {
        coverAccent.dominant
            ?: coverAccent.vibrant
            ?: getGenreSpineColor(book.genres.firstOrNull(), book.title)
    }

    // Dynamic spine thickness proportional to totalPages (32.dp to 74.dp)
    val spineWidth: Dp = remember(book.totalPages) {
        if (book.totalPages <= 0) 44.dp
        else {
            val clamped = book.totalPages.coerceIn(50, 1100)
            val fraction = (clamped - 50) / 1050f
            (32f + fraction * 42f).dp
        }
    }

    // Organic height variance (195.dp to 222.dp)
    val spineHeight: Dp = remember(book.title) {
        val hash = book.title.hashCode().absoluteValue
        (196 + (hash % 26)).dp
    }

    // Spring physics for the pull-out motion
    val pullOutOffset by animateDpAsState(
        targetValue = if (isSelected) (-34).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pullOutOffset"
    )

    val spineScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        label = "spineScale"
    )

    val rotationY by animateFloatAsState(
        targetValue = if (isSelected) -11f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "spineRotationY"
    )
    val rotationZ by animateFloatAsState(
        targetValue = if (isSelected) -1.8f else 0f,
        label = "spineRotationZ"
    )

    val isReading = book.status.equals(BookStatus.READING.name, ignoreCase = true)

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .offset(y = pullOutOffset)
            .graphicsLayer {
                scaleX = spineScale
                scaleY = spineScale
                this.rotationY = rotationY
                this.rotationZ = rotationZ
                cameraDistance = 14f * density
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Ambient golden halo spotlight behind the selected pull-out spine
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(width = spineWidth + 14.dp, height = spineHeight + 18.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD54F).copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spine Body
            Box(
                modifier = Modifier
                    .width(spineWidth)
                    .height(spineHeight)
                    .shadow(
                        elevation = if (isSelected) 22.dp else 4.dp,
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 1.dp, bottomEnd = 1.dp),
                        spotColor = if (isSelected) Color(0xFFD4AF37) else Color.Black
                    )
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 1.dp, bottomEnd = 1.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                baseColor.copy(alpha = 0.95f),
                                baseColor.lighter(0.18f),
                                baseColor,
                                baseColor.darker(0.25f)
                            )
                        )
                    )
                    .border(
                        width = if (isSelected) 2.dp else 0.5.dp,
                        color = if (isSelected) Color(0xFFFFD54F) else Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 1.dp, bottomEnd = 1.dp)
                    )
            ) {
                // Left book joint / hinge groove
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // Right page edge bevel shadow
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(3.5.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )

                // Embossed decorative headcap and tail bands
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                // Top double gold ribs
                SpineGoldRibs()

                // Vertical title & author
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VerticalSpineText(
                        title = book.title,
                        author = book.authors.firstOrNull() ?: "",
                        spineWidth = spineWidth
                    )
                }

                // Bottom double gold ribs & status indicator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val activeLoan = remember(book.loanRecord) { LoanEngine.activeLoan(book) }
                    if (activeLoan != null) {
                        Text(
                            if (activeLoan.isLentOut) "🤝" else "🏛️",
                            fontSize = 9.sp,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    } else if (book.isFavorite) {
                        Text(
                            "★",
                            color = Color(0xFFFFD54F),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    SpineGoldRibs()
                }
            }
        }

        // Dangling Bookmark Ribbon (for actively reading books)
        if (isReading) {
            BookmarkRibbon(
                progressPercent = if (book.totalPages > 0) ((book.currentPage.toFloat() / book.totalPages) * 100).toInt() else null
            )
        } else {
            Spacer(Modifier.height(14.dp))
        }
    }
    }
}

/**
 * Elegant embossed double metallic bands on book spine headcaps.
 */
@Composable
private fun SpineGoldRibs() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFB8860B),
                            Color(0xFFFFDF73),
                            Color(0xFFD4AF37),
                            Color(0xFF8B6508)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF8B6508),
                            Color(0xFFFFDF73),
                            Color(0xFF8B6508)
                        )
                    )
                )
        )
    }
}

/**
 * Rotated vertical spine title with embossed metallic shadow.
 */
@Composable
private fun VerticalSpineText(
    title: String,
    author: String,
    spineWidth: Dp
) {
    val showAuthor = spineWidth >= 46.dp
    val textToDisplay = if (showAuthor && author.isNotBlank()) "$title • $author" else title

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = textToDisplay,
            color = Color(0xFFF9F6EE),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = if (spineWidth < 38.dp) 10.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.85f),
                    offset = Offset(1.5f, 1.5f),
                    blurRadius = 2f
                )
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .rotate(-90f)
                .requiredWidth(170.dp)
        )
    }
}

/**
 * Silky dangling bookmark ribbon hanging from the bottom of an active book.
 */
@Composable
private fun BookmarkRibbon(progressPercent: Int?) {
    val ribbonColor = Color(0xFFB91C1C) // Luxurious crimson silk
    val ribbonShape = remember {
        GenericShape { size, _ ->
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(size.width / 2f, size.height - 6.dp.value)
            lineTo(0f, size.height)
            close()
        }
    }

    Box(
        modifier = Modifier
            .width(14.dp)
            .height(20.dp)
            .offset(y = (-2).dp)
            .clip(ribbonShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        ribbonColor,
                        Color(0xFFDC2626),
                        Color(0xFF991B1B)
                    )
                )
            )
            .shadow(4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        if (progressPercent != null) {
            Text(
                text = "$progressPercent%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54F)
                ),
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

/**
 * 3D Wooden Shelf Plank with depth beveling, procedural wood grain texture, brass accent inlay, and drop shadow.
 */
@Composable
private fun WoodenShelfPlank(woodTheme: BookshelfWoodTheme) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Shelf face and procedural wood-grain canvas
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {
            val width = size.width
            val height = size.height

            // 1. Top chamfer bevel highlight
            val bevelHeight = 3.dp.toPx()
            drawRect(
                color = woodTheme.plankHighlightColor,
                topLeft = Offset(0f, 0f),
                size = Size(width, bevelHeight)
            )

            // 2. Brass accent inlay runner strip embedded in the front lip
            val brassHeight = 1.dp.toPx()
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        woodTheme.brassTrimColor.copy(alpha = 0.5f),
                        woodTheme.brassTrimColor,
                        woodTheme.brassTrimColor.copy(alpha = 0.8f),
                        woodTheme.brassTrimColor.copy(alpha = 0.4f)
                    )
                ),
                topLeft = Offset(0f, bevelHeight),
                size = Size(width, brassHeight)
            )

            // 3. Main fascia vertical gradient
            val fasciaTop = bevelHeight + brassHeight
            val fasciaHeight = height - fasciaTop - 2.5.dp.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        woodTheme.plankMainColor,
                        woodTheme.plankShadowColor
                    ),
                    startY = fasciaTop,
                    endY = fasciaTop + fasciaHeight
                ),
                topLeft = Offset(0f, fasciaTop),
                size = Size(width, fasciaHeight)
            )

            // 4. Procedural organic wood grain striations
            val grainColor = woodTheme.plankHighlightColor.copy(alpha = 0.14f)
            val shadowGrainColor = woodTheme.plankShadowColor.darker(0.3f).copy(alpha = 0.28f)
            val grainYOffsets = listOf(0.22f, 0.42f, 0.65f, 0.85f)
            grainYOffsets.forEachIndexed { idx, frac ->
                val lineY = fasciaTop + (fasciaHeight * frac)
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, lineY)
                    var x = 0f
                    val waveLen = 80f + (idx * 25f)
                    while (x <= width) {
                        val dy = (kotlin.math.sin((x / waveLen).toDouble()) * 1.2).toFloat()
                        lineTo(x, lineY + dy)
                        x += 16f
                    }
                }
                drawPath(
                    path = path,
                    color = if (idx % 2 == 0) grainColor else shadowGrainColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }

            // 5. Bottom angled chamfer undercut
            val undercutY = fasciaTop + fasciaHeight
            val undercutHeight = height - undercutY
            drawRect(
                color = woodTheme.plankShadowColor.darker(0.4f),
                topLeft = Offset(0f, undercutY),
                size = Size(width, undercutHeight)
            )
        }

        // Deep underside cast shadow onto the wall/books below
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * Antique brass decorative bookend to frame the sides of a shelf row.
 */
@Composable
private fun BrassBookend(isLeft: Boolean, trimColor: Color) {
    Box(
        modifier = Modifier
            .padding(bottom = 2.dp)
            .size(width = 10.dp, height = 70.dp)
            .background(
                Brush.horizontalGradient(
                    if (isLeft) listOf(trimColor.copy(alpha = 0.5f), trimColor, trimColor.copy(alpha = 0.8f))
                    else listOf(trimColor.copy(alpha = 0.8f), trimColor, trimColor.copy(alpha = 0.4f))
                ),
                shape = RoundedCornerShape(
                    topStart = if (isLeft) 4.dp else 0.dp,
                    topEnd = if (!isLeft) 4.dp else 0.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            )
            .border(
                0.8.dp,
                Color.Black.copy(alpha = 0.5f),
                RoundedCornerShape(
                    topStart = if (isLeft) 4.dp else 0.dp,
                    topEnd = if (!isLeft) 4.dp else 0.dp
                )
            )
    )
}

/**
 * Sleek bottom inspector card that slides up when a book is selected on the shelf.
 */
@Composable
private fun BookPullOutInspector(
    book: Book,
    woodTheme: BookshelfWoodTheme,
    onOpenBook: () -> Unit,
    onStartReading: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 12.dp,
        border = BorderStroke(1.dp, woodTheme.brassTrimColor.copy(alpha = 0.4f)),
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Book Cover Thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 54.dp, height = 80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .shadow(6.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (book.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = book.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        book.authors.firstOrNull()?.ifBlank { "Unknown Author" } ?: "Unknown Author",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(6.dp))

                    val progress = if (book.totalPages > 0) book.currentPage.toFloat() / book.totalPages else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${book.currentPage} of ${book.totalPages} pages (${(progress * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val activeLoan = remember(book.loanRecord) { LoanEngine.activeLoan(book) }
                    if (activeLoan != null) {
                        Spacer(Modifier.height(6.dp))
                        val loanStatus = remember(activeLoan) { LoanEngine.getLoanStatus(activeLoan) }
                        LoanBadge(loanStatus)
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Inspector")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenBook,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Details")
                }

                Button(
                    onClick = onStartReading,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (book.currentPage > 0) "Resume" else "Start Read")
                }
            }
        }
    }
}

/**
 * Genre-based fallback bookbinding palette.
 */
private fun getGenreSpineColor(genre: String?, title: String): Color {
    val clean = (genre ?: "").lowercase()
    return when {
        clean.contains("fantasy") || clean.contains("magic") -> Color(0xFF4A1D4E) // Royal Plum
        clean.contains("sci-fi") || clean.contains("science") -> Color(0xFF0F4C5C) // Cosmic Teal
        clean.contains("history") || clean.contains("classic") -> Color(0xFF5B1E31) // Burgundy Leather
        clean.contains("mystery") || clean.contains("thriller") -> Color(0xFF1E293B) // Midnight Navy
        clean.contains("nature") || clean.contains("fiction") -> Color(0xFF1B4332) // Forest Green
        clean.contains("biography") || clean.contains("memoir") -> Color(0xFF78350F) // Warm Ochre
        clean.contains("philosophy") || clean.contains("essay") -> Color(0xFF38220F) // Espresso Leather
        else -> {
            val palettes = listOf(
                Color(0xFF5B1E31), // Burgundy
                Color(0xFF1B4332), // Forest Green
                Color(0xFF162A45), // Imperial Navy
                Color(0xFF78350F), // Ochre
                Color(0xFF4A1D4E), // Plum
                Color(0xFF38220F), // Espresso
                Color(0xFF0F4C5C)  // Teal
            )
            palettes[title.hashCode().absoluteValue % palettes.size]
        }
    }
}

private fun Color.lighter(factor: Float = 0.15f): Color {
    return Color(
        red = (red + (1f - red) * factor).coerceIn(0f, 1f),
        green = (green + (1f - green) * factor).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
        alpha = alpha
    )
}

private fun Color.darker(factor: Float = 0.2f): Color {
    return Color(
        red = (red * (1f - factor)).coerceIn(0f, 1f),
        green = (green * (1f - factor)).coerceIn(0f, 1f),
        blue = (blue * (1f - factor)).coerceIn(0f, 1f),
        alpha = alpha
    )
}
