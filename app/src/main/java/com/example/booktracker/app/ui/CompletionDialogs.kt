package com.example.booktracker.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.DnfReasons
import com.example.booktracker.shared.models.RatingAxis
import kotlin.math.roundToInt

private val StarGold = Color(0xFFFFB300)

/**
 * Five stars with half steps: the left half of a star sets x.5, the right half x.0.
 * Tapping the current value again clears the rating.
 */
@Composable
fun StarRatingInput(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    starSize: Dp = 40.dp
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..5) {
            Box(modifier = Modifier.size(starSize)) {
                StarIcon(fill = (rating - (i - 1)).coerceIn(0f, 1f), size = starSize)
                Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                    listOf(i - 0.5f, i.toFloat()).forEach { value ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .semantics { contentDescription = "Rate ${formatRating(value)} stars" }
                                .clickable { onRatingChange(if (rating == value) 0f else value) }
                        )
                    }
                }
            }
        }
    }
}

/** Read-only star row for showing a saved rating. */
@Composable
fun StarRatingDisplay(rating: Float, starSize: Dp = 18.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..5) StarIcon(fill = (rating - (i - 1)).coerceIn(0f, 1f), size = starSize)
    }
}

@Composable
private fun StarIcon(fill: Float, size: Dp) {
    val icon = when {
        fill >= 0.75f -> Icons.Filled.Star
        fill >= 0.25f -> Icons.AutoMirrored.Filled.StarHalf
        else -> Icons.Outlined.StarOutline
    }
    Icon(
        icon,
        contentDescription = null,
        tint = if (fill >= 0.25f) StarGold else MaterialTheme.colorScheme.outline,
        modifier = Modifier.size(size)
    )
}

@Composable
fun FinishDialog(
    book: Book,
    onDismiss: () -> Unit,
    onConfirm: (rating: Map<String, Float>, review: String) -> Unit
) {
    var overall by remember { mutableFloatStateOf(book.rating[RatingAxis.OVERALL] ?: 0f) }
    // Axes start unset; only the ones the reader actually moves are saved.
    val axes = remember {
        mutableStateMapOf<String, Float>().apply {
            putAll(book.rating.filterKeys { it in RatingAxis.ALL })
        }
    }
    var showDetails by remember { mutableStateOf(axes.isNotEmpty()) }
    var review by remember { mutableStateOf(book.review.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Finished “${book.title}”", maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("How was it?", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                StarRatingInput(rating = overall, onRatingChange = { overall = it })
                Text(
                    if (overall > 0f) "${formatRating(overall)} / 5" else "Tap a star to rate",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDetails = !showDetails },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Detailed ratings (optional)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (showDetails) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                AnimatedVisibility(visible = showDetails) {
                    Column {
                        RatingAxis.ALL.forEach { axis ->
                            val v = axes[axis]
                            Text(
                                "$axis: ${v?.let { formatRating(it) } ?: "—"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Slider(
                                value = v ?: 0f,
                                onValueChange = { axes[axis] = (it * 2).roundToInt() / 2f },
                                valueRange = 0f..5f,
                                steps = 9
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    label = { Text("Your review (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val rating = buildMap {
                    if (overall > 0f) put(RatingAxis.OVERALL, overall)
                    axes.forEach { (axis, value) -> if (value > 0f) put(axis, value) }
                }
                onConfirm(rating, review.trim())
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(emptyMap(), review.trim()) }) { Text("Skip rating") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DnfDialog(
    book: Book,
    onDismiss: () -> Unit,
    onConfirm: (abandonedPercentage: Float, reason: String) -> Unit
) {
    val autoPercent = if (book.totalPages > 0) {
        (book.currentPage.toFloat() / book.totalPages * 100f).coerceIn(0f, 100f)
    } else {
        0f
    }
    var percent by remember { mutableFloatStateOf(autoPercent) }
    var reason by remember { mutableStateOf(DnfReasons.ALL.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stop reading this book?") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Stopped at ${percent.roundToInt()}%", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = percent,
                    onValueChange = { percent = it },
                    valueRange = 0f..100f
                )
                Text("Why?", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DnfReasons.ALL.forEach { option ->
                        FilterChip(
                            selected = reason == option,
                            onClick = { reason = option },
                            label = { Text(option) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(percent, reason) }) { Text("Mark as did not finish") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** One decimal, snapped to the nearest half — matches the slider's practical resolution. */
internal fun formatRating(v: Float): String = ((v * 2).roundToInt() / 2.0).toString()
