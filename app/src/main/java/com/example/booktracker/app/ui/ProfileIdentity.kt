package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.ReaderProfileEngine
import com.example.booktracker.app.ui.theme.AppShape
import com.example.booktracker.shared.models.Book

/**
 * The books you'd hand someone: the ones you hearted, or (until you heart any) your
 * highest rated. "Pick favourites" opens a sheet over your finished books.
 */
@Composable
fun FavouritesShelf(
    favourites: ReaderProfileEngine.Favourites,
    onOpenBook: (Book) -> Unit,
    onPick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (favourites.marked || favourites.books.isEmpty()) "Favourites" else "Your highest rated",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            AssistChip(
                onClick = onPick,
                label = { Text(if (favourites.marked) "Edit" else "Pick favourites") },
                leadingIcon = { Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) },
                shape = CircleShape
            )
        }
        Spacer(Modifier.height(8.dp))
        if (favourites.books.isEmpty()) {
            Text(
                "Heart the books you'd hand a friend and they'll sit here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(favourites.books, key = { it.id }) { book ->
                    BookCover(
                        book,
                        Modifier
                            .heroSharedElement("cover-${book.id}")
                            .size(width = 84.dp, height = 126.dp)
                            .clip(AppShape.XS)
                            .clickable { onOpenBook(book) }
                    )
                }
            }
        }
    }
}

/** Finished books as a grid of covers; a tap hearts or un-hearts one. */
@Composable
fun FavouritePickerSheet(finished: List<Book>, onToggle: (Book) -> Unit, onDismiss: () -> Unit) {
    ActionSheet(
        onDismiss = onDismiss,
        icon = Icons.Rounded.Favorite,
        title = "Your favourites",
        message = "Tap the books you'd hand a friend.",
        dismissLabel = "Done"
    ) {
        if (finished.isEmpty()) {
            Text(
                "Finish a book and it can join your favourites.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 76.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)
            ) {
                items(finished, key = { it.id }) { book ->
                    Box(
                        modifier = Modifier
                            .clip(AppShape.XS)
                            .then(
                                if (book.isFavorite) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, AppShape.XS)
                                else Modifier
                            )
                            .clickable(onClickLabel = if (book.isFavorite) "Remove from favourites" else "Add to favourites") { onToggle(book) }
                    ) {
                        BookCover(book, Modifier.fillMaxWidth().height(112.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            Icon(
                                if (book.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                                tint = if (book.isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Your taste in a sentence, then the authors you keep coming back to. Tapping an
 * author searches your library for them.
 */
@Composable
fun ReaderCard(
    taste: String?,
    authors: List<ReaderProfileEngine.Author>,
    onAuthor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = AppShape.L,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "You as a reader",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (taste != null) {
                Spacer(Modifier.height(6.dp))
                Text(taste, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (authors.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Authors you come back to",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(authors, key = { it.name }) { author ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(72.dp)
                                .clip(AppShape.S)
                                .clickable(onClickLabel = "Find ${author.name} in your library") { onAuthor(author.name) }
                        ) {
                            BookCover(author.cover, Modifier.size(56.dp).clip(CircleShape))
                            Spacer(Modifier.height(6.dp))
                            Text(
                                author.name,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "${author.books} ${if (author.books == 1) "book" else "books"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Personal bests, one per row; a row with a book behind it opens the book. */
@Composable
fun RecordsCard(records: List<ReaderProfileEngine.Record>, onOpenBook: (Book) -> Unit, modifier: Modifier = Modifier) {
    Card(
        shape = AppShape.L,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                "Records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            records.forEachIndexed { i, record ->
                if (i > 0) HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(record.book?.let { book -> Modifier.clickable { onOpenBook(book) } } ?: Modifier)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(record.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(record.value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        record.book?.let {
                            Text(
                                it.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    record.book?.let { BookCover(it, Modifier.size(width = 32.dp, height = 48.dp)) }
                }
            }
        }
    }
}
