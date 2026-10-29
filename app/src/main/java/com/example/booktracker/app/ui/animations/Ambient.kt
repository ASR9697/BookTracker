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
import androidx.compose.material.icons.rounded.LocalFireDepartment
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import java.time.LocalTime
import kotlin.math.PI
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
            Icons.Rounded.LocalFireDepartment,
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

/** A soft drifting cloud: height as a share of the sky, width, speed in dp per second, and where it starts. */
private class Cloud(val y: Float, val widthDp: Float, val speedDp: Float, val start: Float)

private val CLOUDS = listOf(
    Cloud(0.42f, 120f, 5f, 0.1f),
    Cloud(0.62f, 90f, 3.5f, 0.55f),
    Cloud(0.32f, 70f, 7f, 0.85f)
)

/** Seconds between shooting stars, and how long one takes to cross. */
private const val SHOOTING_STAR_EVERY = 30f
private const val SHOOTING_STAR_SPAN = 1.1f

/**
 * A soft sky behind the Home greeting that follows the clock: a warm wash at
 * dawn and dusk, drifting clouds by day, and at night (Nocturnal Reader's hour) a
 * moon with twinkling stars and the odd shooting star. The sun or moon sits on a
 * slow arc for the current hour and eases in when Home opens. Fades out downward
 * so it never fights the content.
 *
 * [bodyTop] is where the sun or moon sits from the top, so it clears the app bar.
 * Set [active] false while the sky is off screen to stop its frame loop.
 */
@Composable
fun TimeOfDaySky(modifier: Modifier = Modifier, bodyTop: Dp = 28.dp, active: Boolean = true) {
    val reduce = rememberReduceMotion()
    val now = remember { LocalTime.now() }
    val part = remember { dayPart(now.hour) }
    val stars = remember {
        val random = Random(7) // same sky every visit
        List(36) { Star(random.nextFloat(), random.nextFloat() * 0.85f, random.nextFloat() * 1.6f + 0.6f, random.nextFloat() * 6.28f, random.nextFloat() * 1.5f + 0.5f) }
    }
    // Read only while drawing, so the loop redraws the sky without recomposing anything.
    val seconds = remember { mutableFloatStateOf(0f) }
    val animated = !reduce && active
    if (animated) {
        LaunchedEffect(Unit) {
            val start = withInfiniteAnimationFrameMillis { it }
            while (isActive) withInfiniteAnimationFrameMillis { seconds.floatValue = (it - start) / 1000f }
        }
    }
    val intro = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(1200, easing = Motion.EmphasizedDecelerate)) }

    // How far the sun (06-18h) or moon (18-06h) has travelled across its half of the day.
    val arc = remember(now) {
        val sinceSix = (now.hour + now.minute / 60f - 6f + 24f) % 24f
        (if (sinceSix < 12f) sinceSix else sinceSix - 12f) / 12f
    }

    val primary = MaterialTheme.colorScheme.primary
    val wash = when (part) {
        DayPart.DAWN -> Color(0xFFFFB74D).copy(alpha = 0.16f)
        DayPart.DAY -> primary.copy(alpha = 0.06f)
        DayPart.DUSK -> Color(0xFFFF7043).copy(alpha = 0.14f)
        DayPart.NIGHT -> Color(0xFF3949AB).copy(alpha = 0.22f)
    }
    val moon = Color(0xFFFFF3C4)
    val cloud = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.07f)

    Canvas(modifier = modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val t = seconds.floatValue
        val rise = intro.value
        drawRect(Brush.verticalGradient(listOf(wash, Color.Transparent)))
        val r = 14.dp.toPx()
        val body = Offset(
            size.width * (0.58f + 0.32f * arc),
            bodyTop.toPx() + r + 16.dp.toPx() * (1f - sin(PI.toFloat() * arc)) + (1f - rise) * 14.dp.toPx()
        )
        when (part) {
            DayPart.NIGHT, DayPart.DUSK -> {
                stars.forEach { star ->
                    val twinkle = if (reduce) 0.7f else 0.45f + 0.55f * ((sin(t * star.speed + star.phase) + 1f) / 2f)
                    drawCircle(
                        Color.White.copy(alpha = twinkle * rise * if (part == DayPart.DUSK) 0.35f else 0.8f),
                        radius = star.radius.dp.toPx() / 2f,
                        center = Offset(star.x * size.width, star.y * size.height)
                    )
                }
                if (part == DayPart.NIGHT) {
                    drawCircle(Brush.radialGradient(listOf(moon.copy(alpha = 0.25f * rise), Color.Transparent), body, r * 3f), r * 3f, body)
                    drawCircle(moon.copy(alpha = rise), r, body)
                    // Bite out of the disc for a crescent.
                    drawCircle(Color.Black, r * 0.85f, body + Offset(r * 0.55f, -r * 0.25f), blendMode = BlendMode.Clear)
                    if (animated) drawShootingStar(t)
                }
            }
            DayPart.DAWN, DayPart.DAY -> {
                CLOUDS.forEach { c ->
                    val w = c.widthDp.dp.toPx()
                    val span = size.width + 2f * w
                    val x = (c.start * span + t * c.speedDp.dp.toPx()) % span - w
                    drawCloud(cloud, Offset(x, size.height * c.y), w)
                }
                val sun = if (part == DayPart.DAWN) Color(0xFFFFCC80) else primary
                drawCircle(Brush.radialGradient(listOf(sun.copy(alpha = 0.35f * rise), Color.Transparent), body, r * 3.5f), r * 3.5f, body)
            }
        }
    }
}

/** Three soft puffs; [origin] is the cloud's left edge and [width] its span. */
private fun DrawScope.drawCloud(color: Color, origin: Offset, width: Float) {
    listOf(0.25f to 0.2f, 0.5f to 0.28f, 0.76f to 0.18f).forEach { (at, size) ->
        val radius = width * size
        val centre = Offset(origin.x + width * at, origin.y - if (size > 0.25f) radius * 0.3f else 0f)
        drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), centre, radius), radius, centre)
    }
}

/** One streak across the upper sky every [SHOOTING_STAR_EVERY] seconds, the first about ten seconds in. */
private fun DrawScope.drawShootingStar(seconds: Float) {
    val into = (seconds + SHOOTING_STAR_EVERY - 10f) % SHOOTING_STAR_EVERY
    if (into > SHOOTING_STAR_SPAN) return
    val f = into / SHOOTING_STAR_SPAN
    val fade = sin(PI.toFloat() * f)
    val direction = Offset(1f, 0.45f).let { it / it.getDistance() }
    val head = Offset(size.width * 0.15f, size.height * 0.12f) + direction * (f * size.width * 0.5f)
    val tail = head - direction * (70.dp.toPx() * fade)
    drawLine(
        Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f * fade)), tail, head),
        tail, head, strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round
    )
}
