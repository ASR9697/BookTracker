package com.example.booktracker.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import com.example.booktracker.app.ui.animations.bounceOnPress
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

/**
 * A section in the bottom bar. [image] replaces the icon when set (Profile shows
 * the reader's avatar), and [badge] puts a small dot on it (review cards due).
 */
data class NavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val badge: Boolean = false,
    val image: (@Composable (Modifier) -> Unit)? = null
)

private val PillShape = RoundedCornerShape(50)

/** How much wider the bubble gets halfway between two tabs. */
private const val BUBBLE_STRETCH = 0.35f

/**
 * The main navigation as a floating pill. [position] reports the pager's live
 * page position (e.g. 1.4 while swiping from Library toward Notes), so the
 * highlight rides along under your finger instead of jumping when you let go.
 * Between tabs it stretches like a bubble and settles round on arrival.
 *
 * Tabs are icons; the one under the bubble lifts and shows its name, so the
 * bar stays light. [onAdd], when set, is a round Add button beside the pill,
 * in the same row, so it never sits on top of content.
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
    modifier: Modifier = Modifier,
    onAdd: (() -> Unit)? = null
) {
    val haptics = rememberHaptics()
    val livePosition by rememberUpdatedState(position)
    val last = (tabs.size - 1).toFloat()
    fun clamped() = livePosition().coerceIn(0f, last)
    val selectedIndex by remember { derivedStateOf { clamped().roundToInt() } }
    val outline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = PillShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
            tonalElevation = 3.dp,
            border = BorderStroke(1.dp, outline),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .padding(5.dp)
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                val bubbleColor = MaterialTheme.colorScheme.primaryContainer
                val bubbleShine = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
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
                        // A soft light from above, so the bubble reads as a raised drop.
                        .background(Brush.verticalGradient(listOf(bubbleShine, Color.Transparent)))
                )
                Row(modifier = Modifier.fillMaxSize()) {
                    tabs.forEachIndexed { index, tab ->
                        val selected = index == selectedIndex
                        val tint by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "navTint"
                        )
                        // How close the bubble is to this tab, 1 on it: lifts the icon and shows the name.
                        fun closeness() = (1f - abs(clamped() - index)).coerceIn(0f, 1f)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(PillShape)
                                .semantics(mergeDescendants = true) {
                                    this.selected = selected
                                    contentDescription = tab.label + if (tab.badge) ", new" else ""
                                }
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
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    val c = closeness()
                                    val s = 1f + 0.06f * c
                                    scaleX = s
                                    scaleY = s
                                    translationY = -8.dp.toPx() * c
                                }
                            ) {
                                val image = tab.image
                                if (image != null) {
                                    image(
                                        Modifier
                                            .size(26.dp)
                                            .border(1.5.dp, tint, CircleShape)
                                            .padding(1.5.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                                }
                                if (tab.badge) {
                                    Box(
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 3.dp, y = (-2).dp)
                                            .size(8.dp)
                                            .border(1.5.dp, MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                                    )
                                }
                            }
                            Text(
                                tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = tint,
                                maxLines = 1,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 5.dp)
                                    .graphicsLayer {
                                        val c = closeness()
                                        alpha = ((c - 0.4f) / 0.6f).coerceIn(0f, 1f)
                                        translationY = (1f - c) * 6.dp.toPx()
                                    }
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = onAdd != null,
            enter = expandHorizontally(Motion.press(), expandFrom = Alignment.Start) + fadeIn(),
            exit = shrinkHorizontally(tween(Motion.SHORT), shrinkTowards = Alignment.Start) + fadeOut(tween(Motion.SHORT))
        ) {
            // Keep the last action while the button shrinks away.
            var lastAdd by remember { mutableStateOf(onAdd) }
            if (onAdd != null) lastAdd = onAdd
            val press = remember { MutableInteractionSource() }
            Surface(
                onClick = { lastAdd?.invoke() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 8.dp,
                interactionSource = press,
                modifier = Modifier.padding(start = 10.dp).size(64.dp).bounceOnPress(press)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add book", modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
