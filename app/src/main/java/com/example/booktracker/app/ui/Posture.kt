package com.example.booktracker.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalContext
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import com.example.booktracker.app.camera.findActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * The hinge of a foldable standing half-open like a laptop ("tabletop"), in
 * window pixels; null on flat phones, tablets and fully open foldables. Screens
 * use it to keep content off the fold: something to look at above, controls
 * to touch below.
 */
@Composable
fun rememberTabletopHinge(): Rect? {
    val activity = LocalContext.current.findActivity()
    val layoutInfo: Flow<WindowLayoutInfo> = remember(activity) {
        activity?.let { WindowInfoTracker.getOrCreate(it).windowLayoutInfo(it) } ?: emptyFlow()
    }
    val info by layoutInfo.collectAsState(initial = null)
    val fold = info?.displayFeatures?.filterIsInstance<FoldingFeature>()?.firstOrNull {
        it.state == FoldingFeature.State.HALF_OPENED && it.orientation == FoldingFeature.Orientation.HORIZONTAL
    }
    return fold?.bounds?.toComposeRect()
}
