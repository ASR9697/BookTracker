package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.analytics.ReaderProfileEngine
import com.example.booktracker.app.ui.theme.AppShape
import com.example.booktracker.shared.models.Book
import kotlinx.coroutines.launch

/** What the reader card shows; gathered by Profile, which already has every piece. */
data class ReaderCardContent(
    val name: String,
    val avatar: String,
    val motto: String,
    val level: String,
    val taste: String?,
    val favourites: List<Book>,
    val authors: List<String>,
    val books: Int,
    val pages: Int,
    val minutes: Long
)

/** A preview of the reader card and a Share button that sends it as an image. */
@Composable
fun ShareReaderCardSheet(content: ReaderCardContent, allBooks: List<Book>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    ActionSheet(
        onDismiss = onDismiss,
        icon = Icons.Rounded.IosShare,
        title = "Your reader card",
        confirmLabel = "Share",
        onConfirm = {
            scope.launch { shareLayerImage(context, layer, "reader-card.png", "Share your reader card") }
        }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    }
            ) {
                ReaderCardFace(content, allBooks)
            }
        }
    }
}

/** The card itself: who you are, the books you'd hand someone, and your numbers. */
@Composable
private fun ReaderCardFace(content: ReaderCardContent, allBooks: List<Book>) {
    val ink = MaterialTheme.colorScheme.onPrimaryContainer
    Column(
        modifier = Modifier
            .clip(AppShape.XL)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(content.avatar, content.name, allBooks, Modifier.size(56.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    content.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(content.level, style = MaterialTheme.typography.labelLarge, color = ink.copy(alpha = 0.8f))
            }
        }
        if (content.motto.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                "“${content.motto.trim()}”",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = ink
            )
        }
        content.taste?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = ink)
        }
        if (content.favourites.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                content.favourites.take(4).forEach { book ->
                    BookCover(book, Modifier.size(width = 60.dp, height = 90.dp))
                }
            }
        }
        if (content.authors.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Reads " + content.authors.take(3).joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = ink.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "${"%,d".format(content.books)} ${if (content.books == 1) "book" else "books"} · " +
                "${"%,d".format(content.pages)} pages · ${AnalyticsEngine.formatMinutes(content.minutes)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = ink
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Nocturnal Reader",
            style = MaterialTheme.typography.labelSmall,
            color = ink.copy(alpha = 0.6f)
        )
    }
}

/** Gathers the card from what Profile already computed. */
fun readerCardContent(
    name: String,
    avatar: String,
    motto: String,
    level: ReaderProfileEngine.Level,
    taste: String?,
    favourites: ReaderProfileEngine.Favourites,
    authors: List<ReaderProfileEngine.Author>,
    books: Int,
    pages: Int,
    minutes: Long
) = ReaderCardContent(name, avatar, motto, level.title, taste, favourites.books, authors.map { it.name }, books, pages, minutes)
