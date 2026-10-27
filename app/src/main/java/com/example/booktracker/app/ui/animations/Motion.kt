package com.example.booktracker.app.ui.animations

import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * One set of timings for the whole app, so motion feels like a single system:
 * quick for feedback, medium for moving between places, long for celebrations.
 * Easings follow Material 3's emphasized curves.
 */
object Motion {
    const val SHORT = 150
    const val MEDIUM = 300
    const val LONG = 500
    const val COUNT_UP = 900

    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Press and release: bouncy enough to feel physical, settles fast. */
    fun <T> press(): SpringSpec<T> = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)

    /** Values settling into place (rings, bars, drags snapping back). */
    fun <T> settle(): SpringSpec<T> = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
}

/**
 * True when the reader turned animations off in Android's settings. Finite
 * animations already shorten themselves then, but loops (flames, stars,
 * particles) and count-ups don't, so those check this and stay still.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    val system = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    return system || LocalAppFeel.current.reduceMotion
}

/** The reader's in-app choices for how the app feels, provided once at the root. */
data class AppFeel(val reduceMotion: Boolean = false, val haptics: Boolean = true)

val LocalAppFeel = staticCompositionLocalOf { AppFeel() }

/**
 * The app's haptic vocabulary. Each kind of moment has one feel, everywhere:
 * [tick] for small steps, [confirm] for a saved action, [reject] for a refusal,
 * [celebrate] for milestones. Goes through the View so the system's own haptic
 * setting is respected.
 */
class Haptics internal constructor(private val view: View, private val enabled: Boolean) {
    fun tick() { if (enabled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }

    fun longPress() { if (enabled) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }

    fun confirm() {
        if (!enabled) return
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.VIRTUAL_KEY
        )
    }

    fun reject() {
        if (!enabled) return
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT
            else HapticFeedbackConstants.LONG_PRESS
        )
    }

    /** A strong hit then a lighter echo, for goals and unlocks. */
    fun celebrate() {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        view.postDelayed({ confirm() }, 140)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val enabled = LocalAppFeel.current.haptics
    return remember(view, enabled) { Haptics(view, enabled) }
}

/**
 * [target] as a number that rolls up from 0 the first time it is shown, then
 * glides between later values. Shows the final value at once when motion is off.
 */
@Composable
fun animateCountUp(target: Int, durationMillis: Int = Motion.COUNT_UP): State<Int> {
    val reduce = rememberReduceMotion()
    val value = remember { Animatable(if (reduce) target.toFloat() else 0f) }
    LaunchedEffect(target, reduce) {
        if (reduce) value.snapTo(target.toFloat())
        else value.animateTo(target.toFloat(), tween(durationMillis, easing = Motion.EmphasizedDecelerate))
    }
    return remember { derivedStateOf { value.value.roundToInt() } }
}

/** Marks when a list first appeared, so only its opening batch staggers in. */
class EntranceState internal constructor(internal val createdAt: Long)

/**
 * Saved across navigation, so coming back to a screen (back press, tab switch)
 * shows it settled instead of replaying the entrance.
 */
@Composable
fun rememberEntranceState(): EntranceState {
    val createdAt = rememberSaveable { SystemClock.uptimeMillis() }
    return remember(createdAt) { EntranceState(createdAt) }
}

private const val ENTRANCE_WINDOW_MS = 700L
private const val ENTRANCE_MAX_ITEMS = 15
private const val ENTRANCE_STAGGER_MS = 40L

/**
 * Fades and lifts an item in, a beat after the one before it. Only items that
 * appear while the list is first opening animate; anything scrolled to later
 * just appears, so scrolling never feels slowed down.
 */
fun Modifier.staggeredEntrance(state: EntranceState, index: Int): Modifier = composed {
    val reduce = rememberReduceMotion()
    val play = remember {
        !reduce && index < ENTRANCE_MAX_ITEMS && SystemClock.uptimeMillis() - state.createdAt < ENTRANCE_WINDOW_MS
    }
    val progress = remember { Animatable(if (play) 0f else 1f) }
    val lift = with(LocalDensity.current) { 24.dp.toPx() }
    LaunchedEffect(Unit) {
        if (play) {
            delay(index * ENTRANCE_STAGGER_MS)
            progress.animateTo(1f, tween(Motion.LONG, easing = Motion.EmphasizedDecelerate))
        }
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * lift
    }
}

/**
 * Drag sideways to act: past [threshold] a tick says "let go to do it", and
 * letting go runs [onSwipeRight] or [onSwipeLeft]. Short of it, the element
 * springs back. [onOffset] reports the live drag so callers can reveal hints.
 */
fun Modifier.swipeToAct(
    threshold: Dp = 72.dp,
    onOffset: (Float) -> Unit = {},
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null
): Modifier = composed {
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
    // Latest callbacks without restarting the gesture when the caller recomposes mid-drag.
    val left by rememberUpdatedState(onSwipeLeft)
    val right by rememberUpdatedState(onSwipeRight)
    val report by rememberUpdatedState(onOffset)
    // Plain holders, read and written only inside the gesture: the live drag and whether it is past the threshold.
    val drag = remember { floatArrayOf(0f) }
    val armed = remember { booleanArrayOf(false) }

    fun settleBack() {
        armed[0] = false
        drag[0] = 0f
        scope.launch { offset.animateTo(0f, Motion.settle()) { report(this.value) } }
    }

    this
        .graphicsLayer {
            translationX = offset.value
            rotationZ = offset.value / thresholdPx * 3f
        }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    val value = drag[0]
                    val r = right
                    val l = left
                    when {
                        value >= thresholdPx && r != null -> { haptics.confirm(); r() }
                        value <= -thresholdPx && l != null -> { haptics.confirm(); l() }
                    }
                    settleBack()
                },
                onDragCancel = { settleBack() }
            ) { change, dragAmount ->
                val next = (drag[0] + dragAmount).let {
                    when {
                        it > 0 && right == null -> 0f
                        it < 0 && left == null -> 0f
                        // Resistance past the threshold, so it feels like a detent.
                        abs(it) > thresholdPx -> drag[0] + dragAmount * 0.35f
                        else -> it
                    }
                }
                change.consume()
                drag[0] = next
                scope.launch { offset.snapTo(next) }
                report(next)
                val nowArmed = abs(next) >= thresholdPx
                if (nowArmed != armed[0]) {
                    armed[0] = nowArmed
                    if (nowArmed) haptics.tick()
                }
            }
        }
}
