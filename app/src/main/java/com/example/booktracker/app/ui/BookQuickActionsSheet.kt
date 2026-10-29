package com.example.booktracker.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.ui.animations.Motion
import com.example.booktracker.app.ui.animations.rememberHaptics
import com.example.booktracker.app.ui.animations.rememberReduceMotion
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus

/** Statuses offered as one-tap chips; the rest stay on the book's own page. */
private val QUICK_STATUSES = listOf(
    BookStatus.TO_READ, BookStatus.UP_NEXT, BookStatus.READING, BookStatus.PAUSED, BookStatus.FINISHED, BookStatus.DNF
)

/**
 * What a long-press on a cover opens: the cover lifts into view with the book's
 * title, then the things you'd want to do without opening the page. Actions
 * passed as null are left out.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookQuickActionsSheet(
    book: Book,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onStartReading: (() -> Unit)? = null,
    onAddNote: (() -> Unit)? = null,
    onChangeStatus: ((BookStatus) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    /** Puts the book on the wishlist, or takes it off when it's there. */
    onToggleWishlist: (() -> Unit)? = null
) {
    val haptics = rememberHaptics()
    val reduce = rememberReduceMotion()
    val lift = remember { Animatable(if (reduce) 1f else 0.8f) }
    LaunchedEffect(Unit) { lift.animateTo(1f, Motion.press()) }
    val isReading = book.status == BookStatus.READING.name

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                BookCover(
                    book,
                    Modifier
                        .size(width = 72.dp, height = 108.dp)
                        .graphicsLayer {
                            scaleX = lift.value
                            scaleY = lift.value
                            rotationZ = (1f - lift.value) * -20f
                        }
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(book.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (book.authors.isNotEmpty()) {
                        Text(
                            book.authors.joinToString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            SettingsRow(icon = Icons.AutoMirrored.Rounded.OpenInNew, label = "Open book", onClick = { onDismiss(); onOpen() })
            onStartReading?.let { start ->
                SettingsRow(
                    icon = if (isReading) Icons.Rounded.PlayArrow else Icons.AutoMirrored.Rounded.MenuBook,
                    label = if (isReading) "Read now" else "Start reading",
                    onClick = { onDismiss(); start() }
                )
            }
            onAddNote?.let { add ->
                SettingsRow(icon = Icons.AutoMirrored.Rounded.NoteAdd, label = "Add a note", onClick = { onDismiss(); add() })
            }
            onToggleWishlist?.let { toggle ->
                val wished = book.ownership == com.example.booktracker.shared.models.Ownership.WISHLIST
                SettingsRow(
                    icon = if (wished) Icons.Rounded.BookmarkRemove else Icons.Rounded.BookmarkAdd,
                    label = if (wished) "Remove from wishlist" else "Add to wishlist",
                    onClick = { haptics.confirm(); onDismiss(); toggle() }
                )
            }
            onChangeStatus?.let { change ->
                Text(
                    "Move to",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 4.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    QUICK_STATUSES.forEach { status ->
                        FilterChip(
                            selected = book.status == status.name,
                            onClick = {
                                if (book.status != status.name) {
                                    haptics.confirm()
                                    change(status)
                                }
                                onDismiss()
                            },
                            label = { Text(status.label) }
                        )
                    }
                }
            }
            onDelete?.let { delete ->
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { haptics.reject(); onDismiss(); delete() }
                        .padding(16.dp)
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(16.dp))
                    Text("Delete", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
