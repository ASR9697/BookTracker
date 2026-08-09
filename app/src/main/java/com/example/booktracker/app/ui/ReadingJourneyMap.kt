package com.example.booktracker.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingJourneyMap(
    books: List<Book>,
    onBack: () -> Unit,
    onOpenBook: (Book) -> Unit
) {
    val finishedBooks = books.filter { it.status == BookStatus.FINISHED.name }
        .sortedByDescending { it.lastUpdated }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading Journey", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // The winding background line
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path()
                val width = size.width
                val heightPerItem = 160.dp.toPx()
                
                path.moveTo(width / 2f, 0f)
                for (i in 0 until finishedBooks.size) {
                    val startY = i * heightPerItem
                    val endY = (i + 1) * heightPerItem
                    
                    // alternate sides for the curve
                    val controlX = if (i % 2 == 0) width * 0.8f else width * 0.2f
                    
                    path.quadraticTo(
                        controlX, startY + heightPerItem / 2f,
                        width / 2f, endY
                    )
                }
                
                drawPath(
                    path = path,
                    color = Color.LightGray.copy(alpha = 0.5f),
                    style = Stroke(width = 8.dp.toPx())
                )
            }
            
            LazyColumn(
                contentPadding = PaddingValues(top = 32.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(finishedBooks, key = { _, book -> book.id }) { index, book ->
                    // Alternate alignment
                    val alignment = if (index % 2 == 0) Alignment.CenterEnd else Alignment.CenterStart
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(144.dp) // Leave room for padding
                            .padding(horizontal = 32.dp),
                        contentAlignment = alignment
                    ) {
                        // Reusing LibraryGridCard logic for visualization, but styled slightly differently
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { onOpenBook(book) },
                            modifier = Modifier.width(120.dp).aspectRatio(0.7f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    book.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp),
                                    maxLines = 3
                                )
                            }
                        }
                        
                        // Milestone markers
                        if (index % 5 == 0 && index > 0) {
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("⭐", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
            
            if (finishedBooks.isEmpty()) {
                Text(
                    "Finish a book to start your journey!",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
