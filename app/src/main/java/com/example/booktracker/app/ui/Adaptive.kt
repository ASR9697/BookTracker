package com.example.booktracker.app.ui

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How much room the window gives us, in the Material breakpoints: phones are
 * [COMPACT]; portrait tablets and unfolded foldables [MEDIUM]; landscape
 * tablets [EXPANDED]; big tablets and desktop windows [LARGE]. Read it from
 * [LocalWidthTier] rather than the screen size, so split-screen and freeform
 * windows get the layout that fits the window they're actually in.
 */
enum class WidthTier {
    COMPACT, MEDIUM, EXPANDED, LARGE;

    val atLeastMedium: Boolean get() = this >= MEDIUM
    val atLeastExpanded: Boolean get() = this >= EXPANDED

    companion object {
        fun forWidth(widthDp: Int): WidthTier = when {
            widthDp >= 1200 -> LARGE
            widthDp >= 840 -> EXPANDED
            widthDp >= 600 -> MEDIUM
            else -> COMPACT
        }
    }
}

val LocalWidthTier = staticCompositionLocalOf { WidthTier.COMPACT }

/** The reader has folded the landscape side navigation down to icons. Dynamic: only its readers redraw on a toggle. */
val LocalSideNavCollapsed = compositionLocalOf { false }

/** Longest comfortable line for reading and forms; wider than this, text gets hard to follow. */
val ReadableWidth: Dp = 720.dp

/**
 * Centres the content and caps its width at [max], so text-heavy screens read
 * like a page on a tablet instead of a banner. A no-op on phones.
 */
fun Modifier.readableWidth(max: Dp = ReadableWidth): Modifier =
    this.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = max)

/**
 * Side padding that centres a [max]-wide column in a full-width list. Use it
 * as a lazy list's content padding, so the margins still scroll the list.
 */
fun BoxWithConstraintsScope.centredPadding(max: Dp = ReadableWidth, min: Dp = 0.dp): Dp =
    ((maxWidth - max) / 2).coerceAtLeast(min)

/**
 * Side padding that centres a [max]-wide column in a list spanning the window,
 * minus [chrome] (the rail or drawer beside top-level screens). Add it to the
 * list's content padding: the margins stay part of the list, so they scroll it.
 */
@Composable
fun windowCentredPadding(max: Dp = ReadableWidth, chrome: Dp = 0.dp): Dp {
    val width = LocalConfiguration.current.screenWidthDp.dp - chrome
    return ((width - max) / 2).coerceAtLeast(0.dp)
}

/** Width of the side navigation next to the five main sections at the current tier. */
@Composable
fun navChromeWidth(): Dp = when (LocalWidthTier.current) {
    // Phones and portrait tablets navigate from the bottom bar.
    WidthTier.COMPACT, WidthTier.MEDIUM -> 0.dp
    WidthTier.EXPANDED -> NAV_RAIL_WIDTH
    WidthTier.LARGE -> if (LocalSideNavCollapsed.current) NAV_RAIL_WIDTH else NAV_DRAWER_WIDTH
}

/** The side navigation folded to icons, and opened out with labels. */
val NAV_RAIL_WIDTH: Dp = 80.dp
val NAV_DRAWER_WIDTH: Dp = 248.dp

/** Narrowest a cover tile may get: three across on a phone, larger covers (not just more of them) on tablets. */
@Composable
fun coverTileMinWidth(): Dp = when (LocalWidthTier.current) {
    WidthTier.COMPACT -> 100.dp
    WidthTier.MEDIUM -> 128.dp
    else -> 148.dp
}
