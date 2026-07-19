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
    var currentIndex by remember(books) { mutableIntStateOf(0) }
    val remainingBooks = if (currentIndex < books.size) books.subList(currentIndex, books.size) else emptyList()

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
                if (remainingBooks.size > 1) {
                    val nextBook = remainingBooks[1]
                    SwipeCard(
                        book = nextBook,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
                            .graphicsLayer { alpha = 0.5f }
                    )
                }

                // Draw top card
                val currentBook = remainingBooks[0]
                TinderCard(
                    book = currentBook,
                    onSwipedLeft = { currentIndex++ },
                    onSwipedRight = {
                        onPromote(currentBook)
                        currentIndex++
                    },
                    onSwipedUp = {
                        onStartReading(currentBook)
                        currentIndex++
                    }
                )
            }
        }
    }
}

@Composable
fun TinderCard(
    book: Book,
    onSwipedLeft: () -> Unit,
    onSwipedRight: () -> Unit,
    onSwipedUp: () -> Unit
) {
    val screenWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    val rotation = (offsetX.value / 60).coerceIn(-40f, 40f)

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
                                // Swipe right
                                offsetX.animateTo(screenWidth * 1.5f, tween(300))
                                onSwipedRight()
                            } else if (offsetX.value < -screenWidth / 3) {
                                // Swipe left
                                offsetX.animateTo(-screenWidth * 1.5f, tween(300))
                                onSwipedLeft()
                            } else if (offsetY.value < -screenWidth / 3) {
                                // Swipe up
                                offsetY.animateTo(-screenWidth * 1.5f, tween(300))
                                onSwipedUp()
                            } else {
                                // Reset
                                launch { offsetX.animateTo(0f, tween(300)) }
                                launch { offsetY.animateTo(0f, tween(300)) }
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

        // Swipe overlays (Like/Pass/Read)
        if (offsetX.value > 50f) {
            Text(
                "SHORTLIST",
                color = Color.Green,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(32.dp)
                    .border(4.dp, Color.Green, RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .graphicsLayer { rotationZ = -15f }
            )
        } else if (offsetX.value < -50f) {
            Text(
                "PASS",
                color = Color.Red,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
                    .border(4.dp, Color.Red, RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .graphicsLayer { rotationZ = 15f }
            )
        } else if (offsetY.value < -50f) {
            Text(
                "READ",
                color = Color.Blue,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
                    .border(4.dp, Color.Blue, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )
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
