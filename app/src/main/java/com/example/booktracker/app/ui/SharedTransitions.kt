package com.example.booktracker.app.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/**
 * False on content that is composed but not the one on screen, such as the
 * neighbouring pages the main pager keeps ready. Two visible elements with the
 * same key count as one shared element, so one of them would be hidden: that
 * is what blanked Home's Up next covers while Library sat next door.
 */
val LocalHeroEnabled = compositionLocalOf { true }

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.heroSharedElement(
    key: String,
    durationMillis: Int = 400
): Modifier = composed {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedScope = LocalAnimatedVisibilityScope.current

    if (sharedScope != null && animatedScope != null && LocalHeroEnabled.current) {
        with(sharedScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedScope,
                boundsTransform = { _, _ ->
                    tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
                }
            )
        }
    } else {
        Modifier
    }
}
