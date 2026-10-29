package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.ui.animations.Motion
import com.example.booktracker.app.ui.animations.rememberHaptics
import kotlin.math.abs

/** Drag distance for one step; fast drags take bigger steps. */
private val STEP = 14.dp

/**
 * A number you drag instead of type: drag up to raise it, down to lower it,
 * with a haptic tick per step. Fast flicks jump by 5. +/- buttons and quick
 * chips cover small and big jumps, and tapping the number opens the keyboard.
 */
@Composable
fun PageDial(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    unit: String,
    modifier: Modifier = Modifier,
    quickSteps: List<Int> = listOf(5, 10, 25, 50)
) {
    val haptics = rememberHaptics()
    val stepPx = with(LocalDensity.current) { STEP.toPx() }
    val current by rememberUpdatedState(value)
    var typing by remember { mutableStateOf(false) }

    fun set(next: Int) {
        val clamped = next.coerceIn(range)
        if (clamped != current) {
            haptics.tick()
            onValueChange(clamped)
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { set(value - 1) }) { Icon(Icons.Rounded.Remove, contentDescription = "Less") }
            Spacer(Modifier.width(16.dp))
            if (typing) {
                OutlinedTextField(
                    value = if (value == 0) "" else value.toString(),
                    onValueChange = { text -> onValueChange((text.filter(Char::isDigit).take(5).toIntOrNull() ?: 0).coerceIn(range)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.width(120.dp)
                )
            } else {
                var accumulated by remember { mutableStateOf(0f) }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 128.dp, height = 96.dp)
                        .clip(AppShape.L)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable { typing = true }
                        .pointerInput(range) {
                            detectVerticalDragGestures(onDragEnd = { accumulated = 0f }) { change, dy ->
                                change.consume()
                                accumulated -= dy // up is more
                                val steps = (accumulated / stepPx).toInt()
                                if (steps != 0) {
                                    accumulated -= steps * stepPx
                                    val fast = abs(dy) > stepPx * 1.5f
                                    set(current + steps * if (fast) 5 else 1)
                                }
                            }
                        }
                ) {
                    AnimatedContent(
                        targetState = value,
                        transitionSpec = {
                            val up = targetState > initialState
                            (slideInVertically(tween(Motion.SHORT)) { if (up) it / 2 else -it / 2 } + fadeIn(tween(Motion.SHORT)))
                                .togetherWith(slideOutVertically(tween(Motion.SHORT)) { if (up) -it / 2 else it / 2 } + fadeOut(tween(Motion.SHORT)))
                                .using(SizeTransform(clip = false))
                        },
                        label = "dial"
                    ) { shown ->
                        Text(shown.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            FilledTonalIconButton(onClick = { set(value + 1) }) { Icon(Icons.Rounded.Add, contentDescription = "More") }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (typing) unit else "$unit · drag up or down, or tap to type",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quickSteps.forEach { step ->
                AssistChip(onClick = { set(value + step) }, label = { Text("+$step") })
            }
        }
    }
}
