package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.booktracker.app.analytics.ReviewEngine
import com.example.booktracker.app.analytics.ReviewEngine.Grade
import com.example.booktracker.app.ui.animations.BurstStyle
import com.example.booktracker.app.ui.animations.ConfettiExplosion
import com.example.booktracker.app.ui.animations.rememberHaptics
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.ReviewKind
import com.example.booktracker.shared.models.ReviewState
import com.example.booktracker.shared.models.VocabWord

/**
 * The daily review: today's highlights and words, one card at a time. Each card
 * is graded by how well it stuck, which decides when it comes back. A card you
 * forgot also returns at the end of today's run.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    queue: List<ReviewEngine.Item>?,
    notes: List<MarginNote>,
    words: List<VocabWord>,
    books: List<Book>,
    states: List<ReviewState>,
    today: Long,
    onGrade: (ReviewEngine.Item, Grade) -> Unit,
    onOpenBook: (Book) -> Unit,
    onBack: () -> Unit
) {
    val haptics = rememberHaptics()
    // The run is fixed when the screen opens; grading changes the live queue, not this list.
    var run by remember { mutableStateOf<List<ReviewEngine.Item>?>(null) }
    LaunchedEffect(queue) { if (run == null && queue != null) run = queue }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var reviewed by rememberSaveable { mutableIntStateOf(0) }
    val notesById = remember(notes) { notes.associateBy { it.id } }
    val wordsById = remember(words) { words.associateBy { it.id } }
    val booksById = remember(books) { books.associateBy { it.id } }
    val statesById = remember(states) { states.associateBy { it.itemId } }
    // Items deleted mid-run are skipped rather than shown blank.
    val items = remember(run, notesById, wordsById) {
        run.orEmpty().filter { if (it.kind == ReviewKind.NOTE) it.id in notesById else it.id in wordsById }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily review") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (items.isNotEmpty() && index < items.size) {
                        Text(
                            "${index + 1} of ${items.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                run == null -> Unit
                run!!.isEmpty() -> EmptyState(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Nothing to review today",
                    message = "Save passages in your books or add words you meet, and a few come back here each day so they stick."
                )
                // Cards picked but their notes and words not loaded yet: wait rather than flash "done".
                items.isEmpty() && reviewed == 0 -> Unit
                index >= items.size -> ReviewDone(reviewed, onBack)
                else -> {
                    val progress = index.toFloat() / items.size
                    Column(modifier = Modifier.fillMaxSize()) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(4.dp).clip(CircleShape),
                            gapSize = 0.dp,
                            drawStopIndicator = {}
                        )
                        AnimatedContent(
                            targetState = index,
                            transitionSpec = {
                                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
                            },
                            label = "review card",
                            modifier = Modifier.weight(1f)
                        ) { shown ->
                            val item = items.getOrNull(shown) ?: return@AnimatedContent
                            ReviewCard(
                                item = item,
                                note = notesById[item.id],
                                word = wordsById[item.id],
                                booksById = booksById,
                                previous = statesById[item.id],
                                today = today,
                                onOpenBook = onOpenBook,
                                onGrade = { grade ->
                                    if (grade == Grade.AGAIN) haptics.reject() else haptics.tick()
                                    onGrade(item, grade)
                                    reviewed++
                                    // A forgotten card comes round once more before the run ends.
                                    if (grade == Grade.AGAIN && items.count { it.id == item.id } < 2) {
                                        run = run.orEmpty() + item
                                    }
                                    index++
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    item: ReviewEngine.Item,
    note: MarginNote?,
    word: VocabWord?,
    booksById: Map<String, Book>,
    previous: ReviewState?,
    today: Long,
    onOpenBook: (Book) -> Unit,
    onGrade: (Grade) -> Unit
) {
    // Words hide their meaning until asked; a highlight is simply read again.
    var revealed by remember(item.id) { mutableStateOf(item.kind == ReviewKind.NOTE) }
    val book = (note?.bookId ?: word?.bookId)?.let { booksById[it] }

    // A card, not a banner: on a tablet it keeps a readable width in the middle.
    Column(modifier = Modifier.fillMaxHeight().readableWidth(640.dp).padding(16.dp)) {
        Surface(
            shape = AppShape.XL,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                if (note != null) {
                    Icon(Icons.Rounded.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        note.content,
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif, lineHeight = 34.sp),
                        fontStyle = FontStyle.Italic
                    )
                } else if (word != null) {
                    Icon(Icons.Rounded.Spellcheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(word.word, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    if (word.context.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "“${word.context}”",
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AnimatedVisibility(visible = revealed) {
                        Column {
                            Spacer(Modifier.height(20.dp))
                            Text(
                                word.definition.ifBlank { "No meaning saved. Add one from your word list." },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                if (book != null) {
                    Spacer(Modifier.height(24.dp))
                    Surface(
                        onClick = { onOpenBook(book) },
                        shape = AppShape.M,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                            BookCover(book, Modifier.size(width = 32.dp, height = 48.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(book.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                val page = note?.pageNumber ?: word?.pageNumber ?: 0
                                Text(
                                    listOfNotNull(book.authors.firstOrNull(), page.takeIf { it > 0 }?.let { "p. $it" }).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (!revealed) {
            Button(
                onClick = { revealed = true },
                shape = AppShape.M,
                modifier = Modifier.fillMaxWidth().height(56.dp).navigationBarsPadding()
            ) { Text("Show meaning", fontWeight = FontWeight.SemiBold) }
        } else {
            Text(
                if (item.kind == ReviewKind.NOTE) "How well did you remember this?" else "Did you know it?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                Grade.entries.forEach { grade ->
                    val next = ReviewEngine.schedule(previous, item.id, item.kind, grade, today, 0L).intervalDays
                    val strong = grade == Grade.GOOD
                    val colors = if (grade == Grade.AGAIN) ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) else if (strong) ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) else ButtonDefaults.filledTonalButtonColors()
                    FilledTonalButton(
                        onClick = { onGrade(grade) },
                        colors = colors,
                        shape = AppShape.M,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp),
                        modifier = Modifier.weight(1f).height(64.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(grade.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Text(intervalLabel(next), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

private fun intervalLabel(days: Int): String = when {
    days <= 1 -> "tomorrow"
    days < 30 -> "${days}d"
    days < 365 -> "${days / 30}mo"
    else -> "${days / 365}y"
}

@Composable
private fun ReviewDone(reviewed: Int, onBack: () -> Unit) {
    var burst by remember { mutableStateOf(reviewed > 0) }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(32.dp)
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text("Done for today", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "You went over $reviewed ${if (reviewed == 1) "card" else "cards"}. The ones you know well wait longer before coming back.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onBack, shape = AppShape.M) { Text("Back") }
        }
        if (burst) {
            ConfettiExplosion(style = BurstStyle.CONFETTI, onFinished = { burst = false })
        }
    }
}
