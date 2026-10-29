package com.example.booktracker.app.ui.animations

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Adds a physical "squish" effect when the element is pressed, making it feel tactile and elastic.
 */
fun Modifier.bounceClick(
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit
) = composed {
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by actualInteractionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = Motion.press(),
        label = "bounceScale"
    )

    val haptics = rememberHaptics()
    LaunchedEffect(isPressed) {
        // A crisp, subtle tick for the physical squish
        if (isPressed) haptics.tick()
    }

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = actualInteractionSource,
            indication = null, // Remove standard ripple, the physics is the feedback
            role = androidx.compose.ui.semantics.Role.Button,
            onClick = onClick
        )
}

/**
 * The same squish as [bounceClick] for components that already handle clicks
 * (Button, Card): pass the component's own interactionSource so there is a single
 * click target, which keeps TalkBack activation and the visual press in sync.
 */
fun Modifier.bounceOnPress(interactionSource: MutableInteractionSource) = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = Motion.press(),
        label = "pressScale"
    )
    val haptics = rememberHaptics()
    LaunchedEffect(isPressed) { if (isPressed) haptics.tick() }
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * A sweeping gradient animation to indicate a loading state (skeleton).
 */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -500f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    // Drawn from the theme's ink so it reads on both light and dark surfaces.
    val ink = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    val shimmerColors = listOf(ink.copy(alpha = 0.10f), ink.copy(alpha = 0.03f), ink.copy(alpha = 0.10f))

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnim - 500f, y = translateAnim - 500f),
        end = Offset(x = translateAnim, y = translateAnim)
    )
    Modifier.background(brush)
}

/**
 * A gentle, ambient breathing pulse animation for live or active indicators.
 */
fun Modifier.pulseAnimation(
    minScale: Float = 0.96f,
    maxScale: Float = 1.04f,
    durationMillis: Int = 1400
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

