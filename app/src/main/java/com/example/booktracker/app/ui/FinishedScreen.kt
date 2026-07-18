package com.example.booktracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.RatingAxis
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishedScreen(
    books: List<Book>,
    onBack: () -> Unit
) {
    val finished = books
        .filter { it.status == BookStatus.FINISHED.name }
        .sortedByDescending { it.lastUpdated }
    val dnf = books
        .filter { it.status == BookStatus.DNF.name }
        .sortedByDescending { it.lastUpdated }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finished books") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (finished.isEmpty() && dnf.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Finish or give up on a book and it will show here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (finished.isNotEmpty()) {
                    item { SectionHeader("Finished (${finished.size})") }
                    items(finished, key = { it.id }) { CompletedBookCard(it) }
                }
                if (dnf.isNotEmpty()) {
                    item { SectionHeader("Did not finish (${dnf.size})") }
                    items(dnf, key = { it.id }) { CompletedBookCard(it) }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun CompletedBookCard(book: Book) {
    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCover(book.coverUrl, Modifier.size(width = 48.dp, height = 72.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium,
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
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                .mapNotNull { axis -> book.rating[axis]?.let { "$axis ${formatRating(it)}" } }
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

private fun formatRating(v: Float): String = ((v * 2).roundToInt() / 2.0).toString()
