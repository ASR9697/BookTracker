package com.example.booktracker.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.booktracker.shared.models.Book
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
                title = { Text("TBR Curation") },
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
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (remainingBooks.isEmpty()) {
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
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(24.dp))
                            Text("All Caught Up!", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "You've reviewed your entire backlog. Time to start reading!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(32.dp))
                            Button(onClick = onBack) {
                                Text("Return to Library")
                            }
                        }
                    }
                }
            } else {
                // Draw bottom card for visual stack depth if there's more than 1
                val nextBook = if (remainingBooks.size > 1) remainingBooks[1] else null
                val currentBook = remainingBooks[0]

                TinderCard(
                    book = currentBook,
                    nextBook = nextBook,
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
    book: Book,
    nextBook: Book? = null,
    onSwipedLeft: () -> Unit,
    onSwipedRight: () -> Unit,
    onSwipedUp: () -> Unit
) {
    val screenWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val rotation = (offsetX.value / 60).coerceIn(-40f, 40f)

    val dragDist = kotlin.math.sqrt(offsetX.value * offsetX.value + offsetY.value * offsetY.value)
    val dragFraction = (dragDist / (screenWidth * 0.45f)).coerceIn(0f, 1f)
    val nextCardAlpha = 0.5f + (0.5f * dragFraction)
    val nextCardScale = 0.94f + (0.06f * dragFraction)

    Box(modifier = Modifier.fillMaxSize()) {
        // Next card in stack scaling up smoothly as user drags
        if (nextBook != null) {
            SwipeCard(
                book = nextBook,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
                    .graphicsLayer {
                        alpha = nextCardAlpha
                        scaleX = nextCardScale
                        scaleY = nextCardScale
                    }
            )
        }

        // Active dragging card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .graphicsLayer {
                    rotationZ = rotation
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value > screenWidth / 3) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    offsetX.animateTo(screenWidth * 1.5f, tween(250))
                                    onSwipedRight()
                                } else if (offsetX.value < -screenWidth / 3) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    offsetX.animateTo(-screenWidth * 1.5f, tween(250))
                                    onSwipedLeft()
                                } else if (offsetY.value < -screenWidth / 3) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    offsetY.animateTo(-screenWidth * 1.5f, tween(250))
                                    onSwipedUp()
                                } else {
                                    // Bouncy spring reset
                                    val bounceSpec = androidx.compose.animation.core.spring<Float>(
                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                    )
                                    launch { offsetX.animateTo(0f, bounceSpec) }
                                    launch { offsetY.animateTo(0f, bounceSpec) }
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                        }
                    }
                }
        ) {
            SwipeCard(book = book, modifier = Modifier.fillMaxSize())

            // Swipe overlays (Like/Pass/Read) with smooth opacity and Material You colors
            val shortlistColor = MaterialTheme.colorScheme.primary
            val passColor = MaterialTheme.colorScheme.error
            val readColor = MaterialTheme.colorScheme.tertiary

            if (offsetX.value > 20f) {
                val stampAlpha = ((offsetX.value - 20f) / 100f).coerceIn(0f, 1f)
                Text(
                    "SHORTLIST",
                    color = shortlistColor.copy(alpha = stampAlpha),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(32.dp)
                        .border(4.dp, shortlistColor.copy(alpha = stampAlpha), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .graphicsLayer { rotationZ = -15f }
                )
            } else if (offsetX.value < -20f) {
                val stampAlpha = ((-offsetX.value - 20f) / 100f).coerceIn(0f, 1f)
                Text(
                    "PASS",
                    color = passColor.copy(alpha = stampAlpha),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(32.dp)
                        .border(4.dp, passColor.copy(alpha = stampAlpha), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .graphicsLayer { rotationZ = 15f }
                )
            } else if (offsetY.value < -20f) {
                val stampAlpha = ((-offsetY.value - 20f) / 100f).coerceIn(0f, 1f)
                Text(
                    "READ",
                    color = readColor.copy(alpha = stampAlpha),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp)
                        .border(4.dp, readColor.copy(alpha = stampAlpha), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun SwipeCard(book: Book, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = book.coverUrl?.replace("http:", "https:"),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 500f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = book.authors.joinToString(", "),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                if (book.genres.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        book.genres.take(3).forEach { genre ->
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = genre,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
