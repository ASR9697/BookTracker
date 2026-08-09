package com.example.booktracker.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A book cover cropped to a circle with a completion arc sweeping around it.
 * The arc starts at 12 o'clock and reads clockwise, so a glance gives the same
 * "how far through am I" answer as a progress bar without the horizontal space.
 */
@Composable
fun ProgressRingCover(
    coverUrl: String,
    progress: Float,
    size: Dp = 240.dp,
    ringWidth: Dp = 18.dp,
    trackColor: Color = Color.Unspecified,
    ringColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 900),
        label = "completion"
    )

    val resolvedRing = if (ringColor == Color.Unspecified) {
        androidx.compose.material3.MaterialTheme.colorScheme.primary
    } else ringColor
    val resolvedTrack = if (trackColor == Color.Unspecified) {
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest
    } else trackColor

    val density = LocalDensity.current
    val strokePx = with(density) { ringWidth.toPx() }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = resolvedTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (animated > 0f) {
                drawArc(
                    color = resolvedRing,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        BookCover(
            url = coverUrl,
            modifier = Modifier
                .padding(ringWidth + 10.dp)
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}
