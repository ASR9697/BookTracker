package com.example.booktracker.app.ui.animations

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue

/** The bottom-bar sections in bar order; they live side by side in one pager. */
val TAB_ORDER = listOf("home", "library", "memorize", "stats", "profile")
val TOP_LEVEL_ROUTES = TAB_ORDER.toSet()

/**
 * How screens hand over when you go deeper: a new screen slides in from the end
 * over a receding one, and back reverses it (which also drives Android's
 * predictive-back peek). Switching between the main sections is the pager's
 * job, not this. With animations off, everything is a short fade.
 */
object NavMotion {
    private fun fadeIn(duration: Int = Motion.MEDIUM) = fadeIn(tween(duration, easing = Motion.EmphasizedDecelerate))
    private fun fadeOut(duration: Int = Motion.SHORT) = fadeOut(tween(duration, easing = Motion.EmphasizedAccelerate))

    fun enter(reduce: Boolean): EnterTransition =
        if (reduce) fadeIn(Motion.SHORT)
        else slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate)) { it / 4 } + fadeIn()

    fun exit(reduce: Boolean): ExitTransition =
        if (reduce) fadeOut()
        else slideOutHorizontally(tween(Motion.MEDIUM, easing = Motion.EmphasizedAccelerate)) { -it / 10 } + fadeOut(Motion.MEDIUM)

    fun popEnter(reduce: Boolean): EnterTransition =
        if (reduce) fadeIn(Motion.SHORT)
        else slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate)) { -it / 10 } + fadeIn()

    fun popExit(reduce: Boolean): ExitTransition =
        if (reduce) fadeOut()
        else slideOutHorizontally(tween(Motion.MEDIUM, easing = Motion.EmphasizedAccelerate)) { it / 4 } +
            scaleOut(tween(Motion.MEDIUM), targetScale = 0.96f) + fadeOut(Motion.MEDIUM)
}

/**
 * Depth for a page in the main pager: the page you're leaving recedes a little
 * and the one you're revealing grows into place as you drag. Purely visual and
 * driven by the drag itself, so it can never lag behind your finger.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerDepth(state: PagerState, page: Int, reduce: Boolean): Modifier =
    if (reduce) this
    else graphicsLayer {
        val distance = ((state.currentPage - page) + state.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
        val scale = 1f - 0.06f * distance
        scaleX = scale
        scaleY = scale
        alpha = 1f - 0.35f * distance
    }
