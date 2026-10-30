package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.booktracker.app.ui.animations.bounceOnPress
import com.example.booktracker.app.ui.animations.rememberHaptics

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.LoanEngine
import com.example.booktracker.app.analytics.WishlistEngine
import com.example.booktracker.app.ui.animations.rememberReduceMotion
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import java.util.Locale

private val CoverShape = AppShape.XS

/**
 * Compact library tile sized for three across on a phone: cover with status,
 * rating and loan marks, title and author underneath. Long-press for actions,
 * so the covers stay free of buttons.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryGridCard(
    book: Book,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onStartReading: () -> Unit,
    onDelete: () -> Unit,
    onChangeStatus: ((BookStatus) -> Unit)? = null,
    onAddNote: (() -> Unit)? = null,
    onToggleWishlist: (() -> Unit)? = null
) {
    val haptics = rememberHaptics()
    var menuOpen by remember { mutableStateOf(false) }
    val status = remember(book.status) { libraryStatus(book.status) }
    val rating = book.overallRating?.takeIf { it > 0f }
    val canStart = status != BookStatus.FINISHED
    val press = remember { MutableInteractionSource() }

    if (menuOpen) {
        BookQuickActionsSheet(
            book = book,
            onDismiss = { menuOpen = false },
            onOpen = onClick,
            onStartReading = onStartReading.takeIf { canStart },
            onAddNote = onAddNote,
            onChangeStatus = onChangeStatus,
            onDelete = onDelete,
            onToggleWishlist = onToggleWishlist
        )
    }

    Column(
        modifier = modifier
            .bounceOnPress(press)
            .clip(CoverShape)
            .onSecondaryClick { menuOpen = true }
            .combinedClickable(
                interactionSource = press,
                indication = null,
                onClick = onClick,
                onLongClickLabel = "Book actions",
                onLongClick = {
                    haptics.longPress()
                    menuOpen = true
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        ) {
            BookCover(book, Modifier.heroSharedElement("cover-${book.id}").fillMaxSize().clip(CoverShape))

            LoanEngine.activeLoan(book)?.let { loan ->
                LoanBadge(
                    status = remember(loan) { LoanEngine.getLoanStatus(loan) },
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                )
            }

            if (rating != null) {
                CoverPill(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFFFC94D), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(
                        String.format(Locale.getDefault(), "%.1f", rating),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            statusIcon(status)?.let { icon ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 4.dp, bottom = 8.dp)
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = status?.label,
                        tint = if (status == BookStatus.READING) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (status == BookStatus.READING && book.totalPages > 0) {
                LinearProgressIndicator(
                    progress = { (book.currentPage.toFloat() / book.totalPages).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Black.copy(alpha = 0.4f)
                )
            }

        }

        Spacer(Modifier.height(6.dp))
        Text(
            book.title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (book.authors.isNotEmpty()) {
            Text(
                book.authors.joinToString(", "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CoverPill(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

/** Legacy "BACKLOG" and "ABANDONED" rows fold into the statuses the filters use. */
private fun libraryStatus(raw: String): BookStatus? = when {
    raw.equals("BACKLOG", ignoreCase = true) -> BookStatus.TO_READ
    raw.equals(BookStatus.ABANDONED.name, ignoreCase = true) -> BookStatus.DNF
    else -> BookStatus.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
}

/** To Read is the default state, so it carries no mark; everything else gets one. */
private fun statusIcon(status: BookStatus?): ImageVector? = when (status) {
    BookStatus.READING -> Icons.AutoMirrored.Rounded.MenuBook
    BookStatus.FINISHED -> Icons.Rounded.EmojiEvents
    BookStatus.UP_NEXT, BookStatus.SHORTLIST -> Icons.Rounded.Schedule
    BookStatus.PAUSED -> Icons.Rounded.Pause
    BookStatus.DNF, BookStatus.ABANDONED -> Icons.Rounded.Flag
    BookStatus.TO_READ, null -> null
}

/**
 * The wishlist as one tile at the front of the library: the newest wished-for
 * covers fanned like a small stack, the count under them. Wished books aren't
 * on the shelf yet, so this is where they live in the grid; tapping opens the
 * wishlist page. It gives a little pop when a book joins it.
 */
@Composable
fun WishlistTile(wished: List<Book>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val reduce = rememberReduceMotion()
    val press = remember { MutableInteractionSource() }
    val fan = remember(wished) { WishlistEngine.sorted(wished, WishlistEngine.Sort.ADDED).take(3).reversed() }
    val count = wished.size

    val pop = remember { Animatable(1f) }
    var lastCount by remember { mutableIntStateOf(count) }
    LaunchedEffect(count) {
        if (count > lastCount && !reduce) {
            pop.snapTo(1.08f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        }
        lastCount = count
    }

    Column(
        modifier = modifier
            .bounceOnPress(press)
            .clip(CoverShape)
            .combinedClickable(interactionSource = press, indication = null, onClickLabel = "Open wishlist", onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }
                .clip(CoverShape)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f), MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            val mid = (fan.size - 1) / 2f
            fan.forEachIndexed { i, book ->
                val step = i - mid
                BookCover(
                    book,
                    Modifier
                        .fillMaxWidth(0.56f)
                        .aspectRatio(2f / 3f)
                        .graphicsLayer {
                            rotationZ = step * 9f
                            translationX = step * 14.dp.toPx()
                            translationY = kotlin.math.abs(step) * 6.dp.toPx()
                        }
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(26.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Wishlist", style = MaterialTheme.typography.labelLarge, maxLines = 1)
        Text(
            if (count == 1) "1 book" else "$count books",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
