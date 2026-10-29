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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
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
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                                shape = AppShape.M,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        2 -> StepText("A little every day", "How many pages make a good reading day? You can change this any time in Goals.") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf(10, 20, 30, 50).forEach { pages ->
                                    TickFilterChip(
                                        selected = goal == pages,
                                        onClick = { haptics.tick(); goal = pages },
                                        label = { Text("$pages pages") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
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
