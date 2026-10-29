package com.example.booktracker.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.ui.animations.Motion
import com.example.booktracker.app.ui.animations.rememberHaptics
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

data class NavTab(val route: String, val label: String, val icon: ImageVector)

private val PillShape = RoundedCornerShape(50)

/** How much wider the bubble gets halfway between two tabs. */
private const val BUBBLE_STRETCH = 0.35f

/**
 * The main navigation as a floating pill. [position] reports the pager's live
 * page position (e.g. 1.4 while swiping from Library toward Notes), so the
 * highlight rides along under your finger instead of jumping when you let go.
 * Between tabs it stretches like a bubble and settles round on arrival.
 *
 * The position is only read while laying out and drawing, never while
 * composing, so a swipe moves the bubble every frame without rebuilding the
 * bar; colours change once, when the nearest tab changes.
 */
@Composable
fun FloatingNavBar(
    tabs: List<NavTab>,
    position: () -> Float,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    val livePosition by rememberUpdatedState(position)
    val last = (tabs.size - 1).toFloat()
    fun clamped() = livePosition().coerceIn(0f, last)
    val selectedIndex by remember { derivedStateOf { clamped().roundToInt() } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = PillShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 10.dp,
            tonalElevation = 3.dp
        ) {
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                val bubbleColor = MaterialTheme.colorScheme.primaryContainer
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .layout { measurable, constraints ->
                            // Width and x from the live position, measured fresh each frame.
                            val itemWidth = constraints.maxWidth / tabs.size.toFloat()
                            val p = clamped()
                            val between = (p - floor(p)).let { 4f * it * (1f - it) }
                            val width = (itemWidth * (1f + BUBBLE_STRETCH * between)).roundToInt()
                            val placeable = measurable.measure(Constraints.fixed(width, constraints.maxHeight))
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                val x = itemWidth * p - (width - itemWidth) / 2f
                                placeable.place(x.roundToInt(), 0)
                            }
                        }
                        .graphicsLayer {
                            // A touch of squash while it travels, like a drop of liquid.
                            val p = clamped()
                            val between = (p - floor(p)).let { 4f * it * (1f - it) }
                            scaleY = 1f - 0.1f * between
                        }
                        .clip(PillShape)
                        .background(bubbleColor)
                )
                Row(modifier = Modifier.fillMaxSize()) {
                    tabs.forEachIndexed { index, tab ->
                        val selected = index == selectedIndex
                        val tint by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "navTint"
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(PillShape)
                                .semantics { this.selected = selected }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Tab
                                ) {
                                    if (!selected) {
                                        haptics.tick()
                                        onSelect(tab.route)
                                    }
                                }
                        ) {
                            Icon(
                                tab.icon,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        // Grows as the bubble arrives, shrinks as it leaves.
                                        val closeness = (1f - abs(clamped() - index)).coerceIn(0f, 1f)
                                        val s = 1f + 0.12f * closeness
                                        scaleX = s
                                        scaleY = s
                                    }
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                tab.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = tint,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
