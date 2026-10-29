package com.example.booktracker.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.lerp
import com.example.booktracker.app.ui.animations.Motion
import com.example.booktracker.app.ui.animations.rememberReduceMotion

/** Scroll distance (dp) over which Home's big greeting hands over to the bar's title. */
const val HOME_HEADER_FADE_DP = 96

/**
 * The bar above the five main sections.
 *
 * Over the sky ([overSky], Home on phones) it is see-through with the title hidden;
 * as the greeting scrolls away [skyFade] runs 0 -> 1, the bar fills in and the title
 * (the same words as the greeting) takes its place. Elsewhere it is a normal bar
 * whose title slides when the section changes and which hides on scroll through
 * [scrollBehavior].
 *
 * [skyFade] is read in draw and layer blocks only, so scrolling never recomposes the bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    overSky: Boolean,
    skyFade: () -> Float,
    scrollBehavior: TopAppBarScrollBehavior?,
    showSettings: Boolean,
    onSearch: () -> Unit,
    onScan: () -> Unit,
    onSettings: () -> Unit
) {
    val reduce = rememberReduceMotion()
    val surface = MaterialTheme.colorScheme.surface
    val skyMode by animateFloatAsState(if (overSky) 1f else 0f, tween(Motion.MEDIUM), label = "barOverSky")

    // A soft disc behind each icon while it floats over the sky, so it stays findable.
    val iconBackdrop = Modifier.drawBehind {
        val alpha = 0.55f * skyMode * (1f - skyFade())
        if (alpha > 0f) drawCircle(surface.copy(alpha = alpha))
    }

    CenterAlignedTopAppBar(
        title = {
            Box(modifier = Modifier.graphicsLayer { alpha = lerp(1f, skyFade(), skyMode) }) {
                AnimatedContent(
                    targetState = title,
                    transitionSpec = {
                        if (reduce) {
                            fadeIn(tween(Motion.SHORT)) togetherWith fadeOut(tween(Motion.SHORT))
                        } else {
                            (slideInVertically(tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate)) { it / 3 } +
                                fadeIn(tween(Motion.MEDIUM))) togetherWith
                                (slideOutVertically(tween(Motion.SHORT)) { -it / 3 } + fadeOut(tween(Motion.SHORT)))
                        }
                    },
                    label = "barTitle"
                ) { text ->
                    Text(text, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onSearch, modifier = iconBackdrop) {
                Icon(Icons.Filled.Search, contentDescription = "Search library")
            }
        },
        actions = {
            IconButton(onClick = onScan, modifier = iconBackdrop) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan ISBN")
            }
            // Settings sit behind the gear on Profile.
            AnimatedVisibility(visible = showSettings) {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
        },
        // The fill is drawn below so it can follow the scroll without recomposing.
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        ),
        scrollBehavior = scrollBehavior,
        modifier = Modifier.drawBehind {
            drawRect(surface.copy(alpha = lerp(0.9f, 0.95f * skyFade(), skyMode)))
        }
    )
}
