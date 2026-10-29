package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.crossfade
import com.example.booktracker.app.ui.animations.shimmer
import com.example.booktracker.app.ui.theme.AppShape
import com.example.booktracker.app.ui.theme.CoverCloth
import com.example.booktracker.app.ui.theme.CoverInk
import com.example.booktracker.app.ui.theme.SerifDisplay
import com.example.booktracker.shared.models.Book

/** A library book's cover, typeset from its title and author when it has no art. */
@Composable
fun BookCover(book: Book, modifier: Modifier = Modifier) =
    BookCover(book.coverUrl, modifier, title = book.title, author = book.authors.firstOrNull().orEmpty())

/**
 * A book cover: the image when there is one (a shimmer while it loads, then a
 * fade), otherwise a typeset cover in a cloth colour picked from the title, so
 * a book without art still looks like a book. A soft spine shading on the left
 * edge, a faint sheen and a small shadow make either read as an object.
 */
@Composable
fun BookCover(url: String, modifier: Modifier = Modifier, title: String = "", author: String = "") {
    val shape = AppShape.XS
    var state by remember(url) { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    Box(
        modifier = modifier
            .shadow(3.dp, shape, clip = false)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .drawWithContent {
                drawContent()
                // Spine: a shadowed fold a few percent in from the left, then a soft highlight.
                drawRect(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.30f),
                        0.05f to Color.Black.copy(alpha = 0.10f),
                        0.075f to Color.White.copy(alpha = 0.10f),
                        0.12f to Color.Transparent
                    )
                )
                // Sheen from the top left, as if lit from above.
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                        start = Offset.Zero,
                        end = Offset(size.width * 0.7f, size.height * 0.45f)
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val failed = state is AsyncImagePainter.State.Error
        if (url.isBlank() || failed) {
            TypesetCover(title, author)
        } else {
            if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Empty) {
                Box(Modifier.matchParentSize().shimmer())
            }
            val context = coil3.compose.LocalPlatformContext.current
            val request = remember(url, context) {
                coil3.request.ImageRequest.Builder(context).data(url).crossfade(true).build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                onState = { state = it },
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/** Title in the display serif and author in small caps on book cloth; just the initial when small. */
@Composable
private fun TypesetCover(title: String, author: String) {
    val cloth = remember(title) { CoverCloth[Math.floorMod(title.trim().lowercase().hashCode(), CoverCloth.size)] }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(cloth, lerp(cloth, Color.Black, 0.25f))))
    ) {
        val width = maxWidth.value
        val boxWidth = maxWidth
        val boxHeight = maxHeight
        when {
            title.isBlank() -> Icon(
                Icons.AutoMirrored.Rounded.MenuBook,
                contentDescription = null,
                tint = CoverInk.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.Center).size(maxWidth * 0.4f)
            )
            width < 56f -> Text(
                title.trim().first().uppercase(),
                fontFamily = SerifDisplay,
                fontWeight = FontWeight.SemiBold,
                fontSize = (width * 0.48f).sp,
                color = CoverInk,
                modifier = Modifier.align(Alignment.Center)
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = boxWidth * 0.16f, end = boxWidth * 0.1f, top = boxHeight * 0.1f, bottom = boxHeight * 0.08f)
            ) {
                Box(Modifier.fillMaxWidth(0.35f).height(1.dp).background(CoverInk.copy(alpha = 0.55f)))
                Spacer(Modifier.height(boxHeight * 0.05f))
                val titleSize = (width * 0.13f).coerceIn(9f, 24f)
                Text(
                    title.trim(),
                    fontFamily = SerifDisplay,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = titleSize.sp,
                    lineHeight = (titleSize * 1.15f).sp,
                    color = CoverInk,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                if (author.isNotBlank()) {
                    Text(
                        author.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = (width * 0.065f).coerceIn(7f, 11f).sp,
                        letterSpacing = 1.sp,
                        color = CoverInk.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
