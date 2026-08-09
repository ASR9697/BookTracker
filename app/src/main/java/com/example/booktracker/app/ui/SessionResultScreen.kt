package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.app.format.FormatAdaptabilityLayer
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val resultDateFormat = DateTimeFormatter.ofPattern("M/d/yyyy")

/**
 * Post-save recap. Answers the three questions a reader actually has when they
 * close a book: how long was that, how much is left, and how long will the rest
 * take at the pace I'm actually reading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionResultScreen(
    book: Book,
    result: BookTrackerViewModel.SessionResult,
    allSessions: List<Session>,
    onDone: () -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val unit = FormatAdaptabilityLayer.getDisplayUnit(book).lowercase()

    val pagesLeft = (book.totalPages - book.currentPage).coerceAtLeast(0)
    val minutesLeft = remember(book, allSessions) {
        AnalyticsEngine.estimatedMinutesLeft(book, allSessions)
    }
    val progress = if (book.totalPages > 0) {
        book.currentPage.toFloat() / book.totalPages
    } else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Done")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Read session result",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                Instant.ofEpochMilli(result.savedAt).atZone(zone).toLocalDate().format(resultDateFormat),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(40.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ProgressRingCover(
                    coverUrl = book.coverUrl,
                    progress = progress,
                    ringColor = com.example.booktracker.app.ui.theme.ProgressAmber
                )
            }

            Spacer(Modifier.height(40.dp))

            StatSentence {
                append("You read for ")
                highlight(AnalyticsEngine.formatMinutes(result.durationSeconds / 60L).let {
                    if (result.durationSeconds < 60) "${result.durationSeconds}s" else it
                })
                append(".")
            }

            Spacer(Modifier.height(12.dp))

            if (pagesLeft == 0 && book.totalPages > 0) {
                StatSentence {
                    highlight("You finished this book.")
                }
            } else {
                if (minutesLeft != null) {
                    StatSentence {
                        append("The remaining time until completion is ")
                        highlight(AnalyticsEngine.formatMinutes(minutesLeft))
                        append(".")
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (book.totalPages > 0) {
                    StatSentence {
                        highlight("$pagesLeft $unit")
                        append(" left until you finish.")
                    }
                }
            }

            if (result.endPage > result.startPage) {
                Spacer(Modifier.height(12.dp))
                StatSentence {
                    append("That session covered ")
                    highlight("${result.endPage - result.startPage} $unit")
                    append(".")
                }
            }
        }
    }
}

/**
 * Builder scope for the mixed-weight recap sentences — plain body text with the
 * number itself pulled out in the accent colour.
 */
class StatSentenceScope internal constructor() {
    internal val parts = mutableListOf<Pair<String, Boolean>>()
    fun append(text: String) { parts += text to false }
    fun highlight(text: String) { parts += text to true }
}

@Composable
private fun StatSentence(content: StatSentenceScope.() -> Unit) {
    val scope = remember(content) { StatSentenceScope().apply(content) }
    val accent = MaterialTheme.colorScheme.primary
    val base = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(
            buildAnnotatedString {
                scope.parts.forEach { (text, isHighlight) ->
                    if (isHighlight) {
                        withStyle(
                            SpanStyle(
                                color = accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = MaterialTheme.typography.headlineSmall.fontSize
                            )
                        ) { append(text) }
                    } else {
                        withStyle(SpanStyle(color = base)) { append(text) }
                    }
                }
            },
            style = MaterialTheme.typography.titleMedium
        )
    }
}
