package com.example.booktracker.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.booktracker.app.ui.animations.bounceClick
import com.example.booktracker.shared.models.Book
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TbrSwipeScreen(
    books: List<Book>,
    onPromote: (Book) -> Unit,
    onStartReading: (Book) -> Unit,
    onBack: () -> Unit
) {
    val handledIds = remember { mutableStateListOf<String>() }
    val remainingBooks = remember(books, handledIds.size) {
        books.filterNot { it.id in handledIds }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "TBR Discovery Curation",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            if (remainingBooks.isNotEmpty()) "${remainingBooks.size} books waiting" else "Zero backlogs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (remainingBooks.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .animateContentSize()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "All Caught Up!",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "You have reviewed your entire backlog. All shortlisted titles are ready in your To-Read shelf!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(Modifier.height(28.dp))
                        Button(
                            onClick = onBack,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Return to Library", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                val currentBook = remainingBooks[0]
                val nextBook = remainingBooks.getOrNull(1)
                val thirdBook = remainingBooks.getOrNull(2)

                TinderCard(
                    key = currentBook.id,
                    book = currentBook,
                    nextBook = nextBook,
                    thirdBook = thirdBook,
                    onSwipedLeft = { handledIds.add(currentBook.id) },
                    onSwipedRight = {
                        handledIds.add(currentBook.id)
                        onPromote(currentBook)
                    },
                    onSwipedUp = {
                        handledIds.add(currentBook.id)
                        onStartReading(currentBook)
                    }
                )
            }
        }
    }
}

@Composable
fun TinderCard(
    key: String,
    book: Book,
    nextBook: Book? = null,
    thirdBook: Book? = null,
    onSwipedLeft: () -> Unit,
    onSwipedRight: () -> Unit,
    onSwipedUp: () -> Unit
) {
    val density = LocalDensity.current
    val screenWidth = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val scope = rememberCoroutineScope()

    val offsetX = remember(key) { Animatable(0f) }
    val offsetY = remember(key) { Animatable(0f) }
    val haptic = LocalHapticFeedback.current

    var hasPassedThreshold by remember(key) { mutableStateOf(false) }

    val dragDist = sqrt(offsetX.value * offsetX.value + offsetY.value * offsetY.value)
    val dragFraction = (dragDist / (screenWidth * 0.40f)).coerceIn(0f, 1f)

    // Stamp ratio thresholds
    val rightRatio = ((offsetX.value - 30f) / (screenWidth * 0.25f)).coerceIn(0f, 1f)
    val leftRatio = ((-offsetX.value - 30f) / (screenWidth * 0.25f)).coerceIn(0f, 1f)
    val upRatio = ((-offsetY.value - 30f) / (screenHeight * 0.18f)).coerceIn(0f, 1f)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Multi-card 3D physical card stack
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // 3rd Card in stack (lowest layer, peeking through)
            if (thirdBook != null) {
                val thirdScale = 0.88f + (0.06f * dragFraction)
                val thirdAlpha = 0.40f + (0.35f * dragFraction)
                val thirdOffsetY = (28 - (14 * dragFraction)).dp

                SwipeCard(
                    book = thirdBook,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = thirdOffsetY, start = 20.dp, end = 20.dp, bottom = 12.dp)
                        .graphicsLayer {
                            scaleX = thirdScale
                            scaleY = thirdScale
                            alpha = thirdAlpha
                        }
                )
            }

            // 2nd Card in stack (middle layer, scales up smoothly as top card is dragged)
            if (nextBook != null) {
                val nextScale = 0.94f + (0.06f * dragFraction)
                val nextAlpha = 0.72f + (0.28f * dragFraction)
                val nextOffsetY = (14 - (14 * dragFraction)).dp

                SwipeCard(
                    book = nextBook,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = nextOffsetY, start = 10.dp, end = 10.dp, bottom = 6.dp)
                        .graphicsLayer {
                            scaleX = nextScale
                            scaleY = nextScale
                            alpha = nextAlpha
                        }
                )
            }

            // Active dragging top card with full spring & 3D tilt physics
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = offsetX.value
                        translationY = offsetY.value
                        // 3D perspective rotational inertia
                        rotationZ = (offsetX.value / screenWidth) * 22f
                        rotationY = (offsetX.value / screenWidth) * 11f
                        rotationX = (-offsetY.value / screenHeight) * 9f
                        cameraDistance = 16f * density.density

                        val scaleFactor = 1f - (dragDist / (screenWidth * 3.5f)).coerceAtMost(0.05f)
                        scaleX = scaleFactor
                        scaleY = scaleFactor
                        shadowElevation = (8 + (dragDist / 24f).coerceAtMost(16f)).dp.toPx()
                    }
                    .pointerInput(key) {
                        detectDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    val commitThresholdX = screenWidth * 0.32f
                                    val commitThresholdY = screenHeight * 0.20f

                                    if (offsetX.value > commitThresholdX) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        offsetX.animateTo(screenWidth * 1.5f, tween(220))
                                        onSwipedRight()
                                    } else if (offsetX.value < -commitThresholdX) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        offsetX.animateTo(-screenWidth * 1.5f, tween(220))
                                        onSwipedLeft()
                                    } else if (offsetY.value < -commitThresholdY) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        offsetY.animateTo(-screenHeight * 1.3f, tween(220))
                                        onSwipedUp()
                                    } else {
                                        // Bouncy spring elastic snap-back
                                        val springSpec = spring<Float>(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                        launch { offsetX.animateTo(0f, springSpec) }
                                        launch { offsetY.animateTo(0f, springSpec) }
                                    }
                                }
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val newX = offsetX.value + dragAmount.x
                                val newY = offsetY.value + dragAmount.y
                                offsetX.snapTo(newX)
                                offsetY.snapTo(newY)

                                val commitThresholdX = screenWidth * 0.32f
                                val commitThresholdY = screenHeight * 0.20f
                                val passed = abs(newX) > commitThresholdX || newY < -commitThresholdY
                                if (passed && !hasPassedThreshold) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    hasPassedThreshold = true
                                } else if (!passed && hasPassedThreshold) {
                                    hasPassedThreshold = false
                                }
                            }
                        }
                    }
            ) {
                SwipeCard(book = book, modifier = Modifier.fillMaxSize())

                // Dynamic Slanted Stamp Badges
                // 1. Right Drag: SHORTLIST (Green)
                SwipeStampBadge(
                    text = "SHORTLIST",
                    icon = Icons.Default.BookmarkAdded,
                    color = Color(0xFF22C55E),
                    alpha = rightRatio,
                    rotation = -14f,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(28.dp)
                )

                // 2. Left Drag: PASS (Crimson)
                SwipeStampBadge(
                    text = "PASS",
                    icon = Icons.Default.Close,
                    color = Color(0xFFEF4444),
                    alpha = leftRatio,
                    rotation = 14f,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(28.dp)
                )

                // 3. Up Drag: READ NOW (Electric Sky)
                SwipeStampBadge(
                    text = "START READING",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    color = Color(0xFF0EA5E9),
                    alpha = upRatio,
                    rotation = 0f,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Tactile Bottom Dock: Tinder-style 3 Action Controls
        TbrActionControls(
            onPass = {
                scope.launch {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    offsetX.animateTo(-screenWidth * 1.5f, tween(260))
                    onSwipedLeft()
                }
            },
            onReadNow = {
                scope.launch {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    offsetY.animateTo(-screenHeight * 1.3f, tween(260))
                    onSwipedUp()
                }
            },
            onShortlist = {
                scope.launch {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    offsetX.animateTo(screenWidth * 1.5f, tween(260))
                    onSwipedRight()
                }
            }
        )
    }
}

/**
 * Slanted translucent stamp badge with icon, glowing glass backdrop, and scale pop animation.
 */
@Composable
fun SwipeStampBadge(
    text: String,
    icon: ImageVector,
    color: Color,
    alpha: Float,
    rotation: Float,
    modifier: Modifier = Modifier
) {
    if (alpha <= 0.02f) return
    val scale = 0.85f + (0.25f * alpha.coerceIn(0f, 1f))
    Surface(
        color = color.copy(alpha = 0.16f * alpha),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(3.5.dp, color.copy(alpha = alpha)),
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            )
        }
    }
}

/**
 * Circular 3-button action dock for programmatic swiping.
 */
@Composable
fun TbrActionControls(
    onPass: () -> Unit,
    onReadNow: () -> Unit,
    onShortlist: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pass / Skip Button (Red circle)
        FilledIconButton(
            onClick = onPass,
            modifier = Modifier
                .size(54.dp)
                .bounceClick(onClick = onPass),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Pass / Later",
                modifier = Modifier.size(26.dp)
            )
        }

        // Read Now Hero Button (Vibrant Blue/Cyan elevated circle)
        FilledIconButton(
            onClick = onReadNow,
            modifier = Modifier
                .size(68.dp)
                .bounceClick(onClick = onReadNow),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color(0xFF0284C7),
                contentColor = Color.White
            )
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = "Start Reading",
                modifier = Modifier.size(32.dp)
            )
        }

        // Shortlist Button (Green/Primary circle)
        FilledIconButton(
            onClick = onShortlist,
            modifier = Modifier
                .size(54.dp)
                .bounceClick(onClick = onShortlist),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color(0xFFDCFCE7),
                contentColor = Color(0xFF15803D)
            )
        ) {
            Icon(
                Icons.Default.BookmarkAdded,
                contentDescription = "Shortlist for TBR",
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun SwipeCard(book: Book, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val hasCover = !book.coverUrl.isNullOrBlank()

            if (hasCover) {
                AsyncImage(
                    model = book.coverUrl.replace("http:", "https:"),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Procedural luxury book cover fallback
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E293B),
                                    Color(0xFF0F172A),
                                    Color(0xFF020617)
                                )
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 120.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (book.authors.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "by ${book.authors.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Top Badges Row (Format & Pages Pill)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = if (book.totalPages > 0) "${book.totalPages} pages" else "Uncounted",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = book.format.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Dark gradient overlay for high contrast readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.90f)
                            ),
                            startY = 240f
                        )
                    )
            )

            // Book Details Bottom Information Container
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.authors.joinToString(", ").ifBlank { "Unknown Author" },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Rating stars if present
                val avgRating = if (book.rating.isNotEmpty()) book.rating.values.average().toFloat() else 0f
                if (avgRating > 0f) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = String.format(java.util.Locale.getDefault(), "%.1f", avgRating),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFBBF24)
                        )
                    }
                }

                // Genre Tags
                if (book.genres.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        book.genres.take(3).forEach { genre ->
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = genre,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Brief description teaser
                if (book.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = book.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = FontStyle.Italic,
                            lineHeight = 16.sp
                        ),
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
