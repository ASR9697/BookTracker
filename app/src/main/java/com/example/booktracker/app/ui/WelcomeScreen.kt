package com.example.booktracker.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.ui.animations.Motion
import com.example.booktracker.app.ui.animations.animateCountUp
import com.example.booktracker.app.ui.animations.rememberHaptics
import com.example.booktracker.app.ui.theme.AppShape

private const val STEPS = 4

/**
 * First run, shown over an empty library: what the app is for, your name, a
 * daily goal, and your first book. Every step can be skipped; whatever is
 * chosen is saved as you go, so leaving partway still keeps it.
 */
@Composable
fun WelcomeScreen(
    initialName: String,
    initialGoal: Int,
    onSetName: (String) -> Unit,
    onSetGoal: (Int) -> Unit,
    onScanBook: () -> Unit,
    onSearchBook: () -> Unit,
    onFinish: () -> Unit
) {
    val haptics = rememberHaptics()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(initialName.takeUnless { it == "Reader" }.orEmpty()) }
    var goal by rememberSaveable { mutableIntStateOf(initialGoal) }
    val next = {
        haptics.tick()
        if (step == 1 && name.isNotBlank()) onSetName(name.trim())
        if (step == 2) onSetGoal(goal)
        step = (step + 1).coerceAtMost(STEPS - 1)
    }
    BackHandler(enabled = step > 0) { step-- }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), MaterialTheme.colorScheme.background.copy(alpha = 0f))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .readableWidth()
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            // A fixed height, so the dots stay put when Skip leaves on the last step.
            Row(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                StepDots(step)
                Spacer(Modifier.weight(1f))
                if (step < STEPS - 1) TextButton(onClick = onFinish) { Text("Skip") }
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { if (forward) -it / 3 else it / 3 } + fadeOut())
                },
                label = "welcomeStep",
                modifier = Modifier.weight(1f)
            ) { shown ->
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    when (shown) {
                        0 -> Intro()
                        1 -> StepText("What should we call you?", "It's how Home greets you. Nobody else sees it.") {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it.take(40) },
                                placeholder = { Text("Your name") },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleLarge,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { next() }),
                                shape = AppShape.M,
                                modifier = Modifier.fillMaxWidth()
                            )
                            val typed = name.trim()
                            Text(
                                if (typed.isNotEmpty()) "Home will greet you with “Hi, $typed”." else "Leave it empty and we'll keep it simple.",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        2 -> StepText("A little every day", "How many pages make a good reading day? You can change this any time in Goals.") {
                            GoalPicker(goal = goal, onGoal = { haptics.tick(); goal = it })
                        }
                        else -> StepText("Your first book", "Scan the barcode on the back, or search by title. It goes straight onto your shelf.") {
                            Button(
                                onClick = { haptics.confirm(); onFinish(); onScanBook() },
                                shape = AppShape.M,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text("Scan a book", style = MaterialTheme.typography.titleSmall)
                            }
                            OutlinedButton(
                                onClick = { haptics.confirm(); onFinish(); onSearchBook() },
                                shape = AppShape.M,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text("Search for a book", style = MaterialTheme.typography.titleSmall)
                            }
                            TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("I'll add one later") }
                        }
                    }
                }
            }

            if (step < STEPS - 1) {
                Button(
                    onClick = next,
                    shape = AppShape.M,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(if (step == 0) "Get started" else "Continue", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun Intro() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(88.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape)
    ) {
        Icon(Icons.Rounded.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
    }
    Spacer(Modifier.height(28.dp))
    Text("A quiet place for your reading life", style = MaterialTheme.typography.displaySmall)
    Spacer(Modifier.height(12.dp))
    Text(
        "Keep track of every book, time your reading, and hold on to the lines that stayed with you.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(28.dp))
    Promise(Icons.AutoMirrored.Rounded.MenuBook, "Your whole library, with progress and time left")
    Promise(Icons.Rounded.Timer, "A reading timer that stays out of the way")
    Promise(Icons.Rounded.EditNote, "Notes and quotes that come back for review")
}

private const val GOAL_MIN = 5
private const val GOAL_MAX = 200
private const val GOAL_STEP = 5

/** Quick picks under the stepper; the stepper covers everything in between. */
private val GoalPresets = listOf(10, 20, 30, 50)

/** A rough reading pace (40 pages an hour) and book length, only to make the number feel real. */
private const val MINUTES_PER_PAGE = 1.5f
private const val PAGES_PER_BOOK = 300

/**
 * The daily page goal as one big number with a stepper, a few presets, and a
 * line translating it into time and books so the choice means something.
 */
@Composable
private fun GoalPicker(goal: Int, onGoal: (Int) -> Unit) {
    val shown by animateCountUp(goal, durationMillis = 250)
    val minutes = (goal * MINUTES_PER_PAGE / 5).toInt() * 5
    val booksPerYear = (goal * 365f / PAGES_PER_BOOK).toInt().coerceAtLeast(1)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            FilledTonalIconButton(
                onClick = { onGoal((goal - GOAL_STEP).coerceAtLeast(GOAL_MIN)) },
                enabled = goal > GOAL_MIN,
                modifier = Modifier.size(52.dp)
            ) { Icon(Icons.Rounded.Remove, contentDescription = "Fewer pages") }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(128.dp)) {
                Text("$shown", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                Text("pages a day", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalIconButton(
                onClick = { onGoal((goal + GOAL_STEP).coerceAtMost(GOAL_MAX)) },
                enabled = goal < GOAL_MAX,
                modifier = Modifier.size(52.dp)
            ) { Icon(Icons.Rounded.Add, contentDescription = "More pages") }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GoalPresets.forEach { pages ->
                TickFilterChip(
                    selected = goal == pages,
                    onClick = { onGoal(pages) },
                    label = { Text("$pages", modifier = Modifier.padding(horizontal = 8.dp)) }
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Surface(shape = AppShape.M, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
            Text(
                "About $minutes minutes of reading a day, or around $booksPerYear ${if (booksPerYear == 1) "book" else "books"} a year.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
    }
}

@Composable
private fun Promise(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Surface(shape = AppShape.S, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun StepText(title: String, message: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.displaySmall)
    Spacer(Modifier.height(12.dp))
    Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(28.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

/** Four dots; the current one stretches into a pill. */
@Composable
private fun StepDots(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(STEPS) { index ->
            val width by animateDpAsState(if (index == step) 22.dp else 8.dp, Motion.settle(), label = "dot")
            val color by animateColorAsState(
                if (index <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "dotColor"
            )
            Box(Modifier.size(width = width, height = 8.dp).background(color, CircleShape))
        }
    }
}
