package com.example.booktracker.app.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import java.time.LocalTime
import kotlin.math.sin
import kotlin.random.Random

/** Streak days after which the flame stops growing. */
private const val FLAME_FULL_GROWTH_DAYS = 30

/**
 * The streak flame, alive: it flickers and sways while the streak is going,
 * grows a little with every day up to a month, and pops when the streak ticks
 * up while you watch. A dead streak is a plain grey flame.
 */
@Composable
fun LivingFlame(
    streak: Int,
    modifier: Modifier = Modifier,
    baseSize: Dp = 28.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    idleTint: Color = MaterialTheme.colorScheme.outline
) {
    val reduce = rememberReduceMotion()
    val alive = streak > 0
    val growth = 1f + (streak.coerceAtMost(FLAME_FULL_GROWTH_DAYS) / FLAME_FULL_GROWTH_DAYS.toFloat()) * 0.4f

    val pop = remember { Animatable(1f) }
    var lastStreak by remember { mutableIntStateOf(streak) }
    LaunchedEffect(streak) {
        // Only a real one-day step pops; data loading in (0 -> 20) just appears.
        if (streak == lastStreak + 1 && !reduce) {
            pop.snapTo(1.4f)
            pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        }
        lastStreak = streak
    }

    val transition = rememberInfiniteTransition(label = "flame")
    val flicker by transition.animateFloat(
        0.93f, 1.07f, infiniteRepeatable(tween(620, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flicker"
    )
    val sway by transition.animateFloat(
        -4f, 4f, infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sway"
    )
    val glow by transition.animateFloat(
        0.22f, 0.45f, infiniteRepeatable(tween(940, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow"
    )
    val animate = alive && !reduce

    Box(modifier = modifier.size(baseSize * 1.6f), contentAlignment = Alignment.Center) {
        if (alive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    Brush.radialGradient(listOf(tint.copy(alpha = if (animate) glow else 0.3f), Color.Transparent)),
                    radius = size.minDimension / 2f
                )
            }
        }
        Icon(
            Icons.Filled.LocalFireDepartment,
            contentDescription = null,
            tint = if (alive) tint else idleTint,
            modifier = Modifier
                .size(baseSize * growth)
                .graphicsLayer {
                    val s = (if (animate) flicker else 1f) * pop.value
                    scaleX = s
                    scaleY = s
                    rotationZ = if (animate) sway else 0f
                    transformOrigin = TransformOrigin(0.5f, 1f) // flicker from the base, like a real flame
                }
        )
    }
}

private enum class DayPart { DAWN, DAY, DUSK, NIGHT }

private fun dayPart(hour: Int) = when (hour) {
    in 5..7 -> DayPart.DAWN
    in 8..16 -> DayPart.DAY
    in 17..19 -> DayPart.DUSK
    else -> DayPart.NIGHT
}

private class Star(val x: Float, val y: Float, val radius: Float, val phase: Float, val speed: Float)

/**
 * A soft sky behind the Home greeting that follows the clock: a warm wash at
 * dawn and dusk, a faint sun by day, and at night (Nocturnal Reader's hour) a
 * moon with twinkling stars. Fades out downward so it never fights the content.
 */
@Composable
fun TimeOfDaySky(modifier: Modifier = Modifier) {
    val reduce = rememberReduceMotion()
    val part = remember { dayPart(LocalTime.now().hour) }
    val stars = remember {
        val random = Random(7) // same sky every visit
        List(36) { Star(random.nextFloat(), random.nextFloat() * 0.85f, random.nextFloat() * 1.6f + 0.6f, random.nextFloat() * 6.28f, random.nextFloat() * 1.5f + 0.5f) }
    }
    var seconds by remember { mutableFloatStateOf(0f) }
    if (!reduce && (part == DayPart.NIGHT || part == DayPart.DUSK)) {
        LaunchedEffect(Unit) {
            val start = withInfiniteAnimationFrameMillis { it }
            while (isActive) withInfiniteAnimationFrameMillis { seconds = (it - start) / 1000f }
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val wash = when (part) {
        DayPart.DAWN -> Color(0xFFFFB74D).copy(alpha = 0.16f)
        DayPart.DAY -> primary.copy(alpha = 0.06f)
        DayPart.DUSK -> Color(0xFFFF7043).copy(alpha = 0.14f)
        DayPart.NIGHT -> Color(0xFF3949AB).copy(alpha = 0.22f)
    }
    val moon = Color(0xFFFFF3C4)

    Canvas(modifier = modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        drawRect(Brush.verticalGradient(listOf(wash, Color.Transparent)))
        val body = Offset(size.width - 48.dp.toPx(), 28.dp.toPx())
        val r = 14.dp.toPx()
        when (part) {
            DayPart.NIGHT, DayPart.DUSK -> {
                stars.forEach { star ->
                    val twinkle = if (reduce) 0.7f else 0.45f + 0.55f * ((sin(seconds * star.speed + star.phase) + 1f) / 2f)
                    drawCircle(
                        Color.White.copy(alpha = twinkle * if (part == DayPart.DUSK) 0.35f else 0.8f),
                        radius = star.radius.dp.toPx() / 2f,
                        center = Offset(star.x * size.width, star.y * size.height)
                    )
                }
                if (part == DayPart.NIGHT) {
                    drawCircle(Brush.radialGradient(listOf(moon.copy(alpha = 0.25f), Color.Transparent), body, r * 3f), r * 3f, body)
                    drawCircle(moon, r, body)
                    // Bite out of the disc for a crescent.
                    drawCircle(Color.Black, r * 0.85f, body + Offset(r * 0.55f, -r * 0.25f), blendMode = BlendMode.Clear)
                }
            }
            DayPart.DAWN, DayPart.DAY -> {
                val sun = if (part == DayPart.DAWN) Color(0xFFFFCC80) else primary
                drawCircle(Brush.radialGradient(listOf(sun.copy(alpha = 0.35f), Color.Transparent), body, r * 3.5f), r * 3.5f, body)
            }
        }
    }
}
