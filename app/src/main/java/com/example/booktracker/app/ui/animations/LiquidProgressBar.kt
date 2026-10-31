package com.example.booktracker.app.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** How long the wave keeps moving after it appears or its level changes. */
private const val WAVE_SETTLE_MS = 4000L

/**
 * High-performance, ambient liquid wave progress bar.
 * Renders dual travelling sine waves with a fluid crest highlight and micro-bubbles,
 * smoothly responding to progress updates with spring physics.
 */
@Composable
fun LiquidWaveProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    waveColor: Color = MaterialTheme.colorScheme.primary,
    secondaryWaveColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    cornerRadius: Dp = 50.dp,
    amplitude: Float = 2.5f,
    speedMillis: Int = 2200
) {
    // Spring physics animation for progress level
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "liquidProgressSpring"
    )

    // Waves and sheen run on the shared ambient clock, still off screen and for reduced
    // motion. They slosh for a few seconds when the bar appears or its level changes,
    // then rest: a bar that moved forever kept the whole window redrawing.
    val reduce = rememberReduceMotion()
    var sloshing by remember { mutableStateOf(true) }
    LaunchedEffect(progress) {
        sloshing = true
        kotlinx.coroutines.delay(WAVE_SETTLE_MS)
        sloshing = false
    }
    val clock = rememberAmbientClock(!reduce && sloshing && LocalAmbientMotion.current)
    val twoPi = 2f * PI.toFloat()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val totalHeight = size.height
            if (width <= 0f || totalHeight <= 0f) return@Canvas

            // 1. Draw rounded background track
            drawRoundRect(
                color = trackColor,
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
            )

            if (animatedProgress <= 0.005f) return@Canvas

            val millis = clock.value * 1000f
            val wavePhase1 = twoPi * (millis % speedMillis) / speedMillis
            val wavePhase2 = twoPi - twoPi * (millis % (speedMillis * 1.45f)) / (speedMillis * 1.45f)
            val shimmerOffset = -0.5f + 2f * (millis % 3000f) / 3000f

            val fillWidth = width * animatedProgress
            val waveAmp = if (animatedProgress >= 0.98f) 0.5f else amplitude.coerceAtMost(totalHeight * 0.4f)

            // Clip drawing to progress fill width
            clipRect(left = 0f, top = 0f, right = fillWidth, bottom = totalHeight) {
                // Secondary background wave path
                val bgPath = Path().apply {
                    moveTo(0f, totalHeight)
                    val wavelength2 = width * 0.65f
                    var x = 0f
                    val step = 4f
                    val baseWaterY = totalHeight * 0.3f
                    while (x <= fillWidth + 10f) {
                        val y = baseWaterY + (sin((x / wavelength2) * 2f * PI.toFloat() + wavePhase2) * waveAmp * 0.85f)
                        lineTo(x, y)
                        x += step
                    }
                    lineTo(fillWidth, totalHeight)
                    close()
                }
                drawPath(path = bgPath, color = secondaryWaveColor)

                // Foreground primary wave path
                val fgPath = Path().apply {
                    moveTo(0f, totalHeight)
                    val wavelength1 = width * 0.5f
                    var x = 0f
                    val step = 4f
                    val baseWaterY = totalHeight * 0.25f
                    while (x <= fillWidth + 10f) {
                        val y = baseWaterY + (sin((x / wavelength1) * 2f * PI.toFloat() + wavePhase1) * waveAmp)
                        lineTo(x, y)
                        x += step
                    }
                    lineTo(fillWidth, totalHeight)
                    close()
                }

                // Vertical gradient for natural liquid depth
                val liquidBrush = Brush.verticalGradient(
                    colors = listOf(
                        waveColor.copy(alpha = 0.95f),
                        waveColor
                    ),
                    startY = 0f,
                    endY = totalHeight
                )
                drawPath(path = fgPath, brush = liquidBrush)

                // Top fluid crest highlight line
                val crestPath = Path().apply {
                    val wavelength1 = width * 0.5f
                    var x = 0f
                    val step = 4f
                    val baseWaterY = totalHeight * 0.25f
                    moveTo(0f, baseWaterY + (sin(wavePhase1) * waveAmp))
                    while (x <= fillWidth) {
                        val y = baseWaterY + (sin((x / wavelength1) * 2f * PI.toFloat() + wavePhase1) * waveAmp)
                        lineTo(x, y)
                        x += step
                    }
                }
                drawPath(
                    path = crestPath,
                    color = Color.White.copy(alpha = 0.45f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Animated light beam reflection sweeping across fluid; gone once the wave rests.
                if (!sloshing || reduce) return@clipRect
                val shimmerX = width * shimmerOffset
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        startX = shimmerX - 50f,
                        endX = shimmerX + 50f
                    ),
                    size = Size(fillWidth, totalHeight)
                )
            }
        }
    }
}
