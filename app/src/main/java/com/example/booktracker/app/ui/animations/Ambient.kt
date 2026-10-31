package com.example.booktracker.app.ui.animations

import android.app.ActivityManager
import android.content.Context
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Streak days after which the flame stops growing. */
private const val FLAME_FULL_GROWTH_DAYS = 30

/**
 * False on content that is composed but not on screen, such as the neighbouring
 * pages the main pager keeps ready, so their ambient loops hold still. Unlike
 * LocalHeroEnabled it stays true in the library's side pane, which is visible.
 */
val LocalAmbientMotion = compositionLocalOf { true }

/**
 * False once the reader has left the screen alone for a while, and always on low-end
 * phones; see [AmbientWakeHost]. Ambient loops slow to a stop while it's false.
 */
val LocalAmbientAwake = compositionLocalOf { true }

/** How long ambient motion carries on after the last touch. */
private const val AMBIENT_AWAKE_MS = 12_000L

/** Seconds ambient time takes to slow to a stop, or to get going again. */
private const val AMBIENT_EASE_SECONDS = 1.5f

/**
 * Seconds of ambient time, ticking while [running] and holding still otherwise,
 * so a page that is off screen or a reader who asked for less motion costs no
 * frames. Read it only while drawing, so the loop redraws without recomposing.
 *
 * It ticks every frame on purpose: throttled to ~30 a second, a 144 Hz tablet
 * dropped its refresh rate for the app and scrolling over the sky stuttered.
 * Instead it rests completely while the reader isn't touching anything
 * ([LocalAmbientAwake]), unless [whileIdle] says the loop has its own limit.
 */
@Composable
fun rememberAmbientClock(running: Boolean, whileIdle: Boolean = false): State<Float> {
    val seconds = remember { mutableFloatStateOf(0f) }
    val awake by rememberUpdatedState(whileIdle || LocalAmbientAwake.current)
    if (running) {
        LaunchedEffect(Unit) {
            var speed = 0f
            var last = 0L
            while (isActive) {
                if (!awake && speed == 0f) {
                    // Nobody's around: no frames at all until the next touch.
                    snapshotFlow { awake }.first { it }
                    last = 0L
                }
                withInfiniteAnimationFrameMillis { now ->
                    val dt = if (last == 0L) 0f else (now - last) / 1000f
                    last = now
                    // Eased both ways, so it never freezes mid-twinkle or jumps back to life.
                    val step = dt / AMBIENT_EASE_SECONDS
                    speed = if (awake) (speed + step).coerceAtMost(1f) else (speed - step).coerceAtLeast(0f)
                    // Time carries on from where it stopped, so nothing jumps when the page comes back.
                    seconds.floatValue += dt * speed
                }
            }
        }
    }
    return seconds
}

/**
 * Keeps ambient motion (the Home sky, the streak flame, sloshing bars) going only
 * while the reader is around: from a touch anywhere in [content], or the app
 * coming back to the front, until [AMBIENT_AWAKE_MS] after the last one. Left
 * alone on Home, the sky redrew the whole window 144 times a second and kept
 * over a CPU core busy. On low-end phones it stays still from the start.
 */
@Composable
fun AmbientWakeHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lowEnd = remember(context) { isLowEndDevice(context) }
    var awake by remember { mutableStateOf(!lowEnd) }
    val lastWake = remember { longArrayOf(SystemClock.uptimeMillis()) }
    val wake = {
        lastWake[0] = SystemClock.uptimeMillis()
        if (!awake && !lowEnd) awake = true
    }
    LaunchedEffect(lowEnd) {
        if (lowEnd) return@LaunchedEffect
        while (true) {
            if (awake) {
                val left = lastWake[0] + AMBIENT_AWAKE_MS - SystemClock.uptimeMillis()
                if (left > 0) delay(left) else awake = false
            } else {
                snapshotFlow { awake }.first { it }
            }
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) wake() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Box(
        // Watches touches on their way down without taking them.
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial)
                    wake()
                }
            }
        }
    ) {
        CompositionLocalProvider(LocalAmbientAwake provides awake, content = content)
    }
}

/** Android Go phones, and others with 3 GB of memory or less (they report a little under). */
private fun isLowEndDevice(context: Context): Boolean {
    val manager = context.getSystemService(ActivityManager::class.java) ?: return false
    if (manager.isLowRamDevice) return true
    val memory = ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
    return memory.totalMem <= 3L * 1024 * 1024 * 1024
}

/** A smooth swing between [low] and [high], one round trip every [period] seconds. */
private fun swing(seconds: Float, period: Float, low: Float, high: Float): Float =
    low + (high - low) * (1f - cos(2f * PI.toFloat() * seconds / period)) / 2f

/**
 * The streak flame, alive: it flickers and sways while the streak is going,
 * grows a little with every day up to a month, and pops when the streak ticks
 * up while you watch. A dead streak is a plain grey flame. It holds still
 * while [active] is false (by default, on a page that isn't on screen).
 */
@Composable
fun LivingFlame(
    streak: Int,
    modifier: Modifier = Modifier,
    baseSize: Dp = 28.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    idleTint: Color = MaterialTheme.colorScheme.outline,
    active: Boolean = LocalAmbientMotion.current
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

    val animate = alive && !reduce
    val clock = rememberAmbientClock(animate && active)

    Box(modifier = modifier.size(baseSize * 1.6f), contentAlignment = Alignment.Center) {
        if (alive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val glow = if (animate) swing(clock.value, 1.88f, 0.22f, 0.45f) else 0.3f
                drawCircle(
                    Brush.radialGradient(listOf(tint.copy(alpha = glow), Color.Transparent)),
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
                    val t = clock.value
                    val s = (if (animate) swing(t, 1.24f, 0.93f, 1.07f) else 1f) * pop.value
                    scaleX = s
                    scaleY = s
                    rotationZ = if (animate) swing(t, 2.6f, -4f, 4f) else 0f
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
    val animated = !reduce && active
    val seconds = rememberAmbientClock(animated)
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

    Canvas(modifier = modifier) {
        val t = seconds.value
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
                    // A crescent cut as a path, so the whole sky needn't be drawn into its own offscreen layer.
                    drawPath(crescent(body, r), moon.copy(alpha = rise))
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

/** The moon's disc of radius [r] at [centre], with a bite out of its upper right. */
private fun crescent(centre: Offset, r: Float): Path {
    val disc = Path().apply { addOval(Rect(centre, r)) }
    val bite = Path().apply { addOval(Rect(centre + Offset(r * 0.55f, -r * 0.25f), r * 0.85f)) }
    return Path().apply { op(disc, bite, PathOperation.Difference) }
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
