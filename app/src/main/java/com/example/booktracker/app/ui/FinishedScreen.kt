package com.example.booktracker.app.ui

import androidx.compose.foundation.lazy.itemsIndexed
import com.example.booktracker.app.ui.animations.rememberEntranceState
import com.example.booktracker.app.ui.animations.staggeredEntrance
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.RatingAxis
import com.example.booktracker.shared.models.Session
import com.example.booktracker.app.ui.animations.bounceClick
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishedScreen(
    books: List<Book>,
    sessions: List<Session> = emptyList(),
    notes: List<MarginNote> = emptyList(),
    onBack: () -> Unit,
    onOpenBook: (Book) -> Unit = {},
    onOpenJourneyMap: () -> Unit = {}
) {
    var bookForVictoryCard by remember { mutableStateOf<Book?>(null) }
    val finished = books
        .filter { it.status.equals(BookStatus.FINISHED.name, ignoreCase = true) }
        .sortedByDescending { it.completedAt }
    val dnf = books
        .filter { it.status.equals(BookStatus.DNF.name, ignoreCase = true) || it.status.equals(BookStatus.ABANDONED.name, ignoreCase = true) }
        .sortedByDescending { it.completedAt }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finished & Abandoned") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenJourneyMap) {
                        Icon(Icons.Filled.Timeline, contentDescription = "Reading Journey Map")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (finished.isEmpty() && dnf.isEmpty()) {
            EmptyState(
                icon = androidx.compose.material.icons.Icons.Filled.EmojiEvents,
                title = "Your trophy shelf is waiting",
                message = "Finish or set aside a book and it will show here.",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            val listEntrance = rememberEntranceState()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (finished.isNotEmpty()) {
                    item { SectionHeader("Finished (${finished.size})") }
                    itemsIndexed(finished, key = { _, book -> book.id }) { index, book ->
                        CompletedBookCard(
                            book = book,
                            onClick = { onOpenBook(book) },
                            onShareVictory = { bookForVictoryCard = book },
                            modifier = Modifier.animateItem().staggeredEntrance(listEntrance, index + 1)
                        )
                    }
                }
                if (dnf.isNotEmpty()) {
                    item { SectionHeader("Did not finish (${dnf.size})") }
                    items(dnf, key = { it.id }) { CompletedBookCard(it, onClick = { onOpenBook(it) }, modifier = Modifier.animateItem()) }
                }
            }
        }
    }

    bookForVictoryCard?.let { victoryBook ->
        BookVictoryCardDialog(
            book = victoryBook,
            sessions = sessions,
            notes = notes,
            onDismiss = { bookForVictoryCard = null }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun CompletedBookCard(
    book: Book,
    onClick: () -> Unit,
    onShareVictory: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                BookCover(book.coverUrl, Modifier.size(width = 52.dp, height = 76.dp))
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (book.authors.isNotEmpty()) {
                        Text(
                            book.authors.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val detail = completionDetail(book)
                    if (detail != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            detail,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (onShareVictory != null) {
                    IconButton(
                        onClick = onShareVictory,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            contentDescription = "Share Victory Card",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (!book.review.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "“${book.review}”",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

private fun completionDetail(book: Book): String? = when (book.status) {
    BookStatus.FINISHED.name -> {
        if (book.rating.isEmpty()) {
            "Not rated"
        } else {
            RatingAxis.ALL
                .mapNotNull { axis -> book.rating[axis]?.let { "$axis ${formatFinishedRating(it)}★" } }
                .joinToString(" · ")
        }
    }
    BookStatus.DNF.name -> {
        val dnf = book.dnfData
        if (dnf != null) {
            "Abandoned at ${dnf.abandonedPercentage.roundToInt()}%" +
                if (dnf.reason.isNotBlank()) " · ${dnf.reason}" else ""
        } else {
            "Did not finish"
        }
    }
    else -> null
}

private fun formatFinishedRating(v: Float): String = ((v * 2).roundToInt() / 2.0).toString()
