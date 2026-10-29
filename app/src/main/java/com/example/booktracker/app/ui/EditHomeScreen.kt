package com.example.booktracker.app.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.booktracker.app.data.HomeLayout
import com.example.booktracker.app.data.HomeSection
import kotlin.math.abs

private val RowShape = RoundedCornerShape(16.dp)
private val RowSpacing = 8.dp

/**
 * Reorder (long-press and drag) or hide Home sections. Every change is saved
 * as it happens; a drag is saved once, when the finger lifts.
 */
@Composable
fun EditHomeScreen(
    layout: HomeLayout,
    onLayoutChange: (HomeLayout) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val spacingPx = with(LocalDensity.current) { RowSpacing.toPx() }

    // Gesture handlers outlive recompositions, so they read the latest layout through this.
    val current by rememberUpdatedState(layout)
    // Working order while a drag is in flight; one stable state the handlers can write to.
    var order by remember { mutableStateOf(layout.order) }
    var dragging by remember { mutableStateOf<HomeSection?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<HomeSection, Int>() }

    // Pick up saves, toggles and resets, but never yank a row out from under a finger.
    LaunchedEffect(layout.order) { if (dragging == null) order = layout.order }

    fun save(newOrder: List<HomeSection>) = onLayoutChange(current.copy(order = newOrder))

    fun moveBy(section: HomeSection, delta: Int): Boolean {
        val from = order.indexOf(section)
        val moved = current.copy(order = order).move(from, from + delta)
        if (moved.order == order) return false
        order = moved.order
        save(order)
        return true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .readableWidth()
            .padding(16.dp)
    ) {
        Text(
            "Long-press a section and drag to reorder. Tap the eye to hide it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(16.dp))

        SectionRow(
            title = "Current book",
            description = "Always first on Home",
            leading = { Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
            trailing = {},
            modifier = Modifier.alpha(0.6f)
        )
        Spacer(Modifier.size(RowSpacing))

        Column(verticalArrangement = Arrangement.spacedBy(RowSpacing)) {
            order.forEach { section ->
                key(section) {
                    val isDragging = dragging == section
                    val hidden = section in layout.hidden
                    SectionRow(
                        title = section.title,
                        description = if (hidden) "Hidden" else section.description,
                        elevated = isDragging,
                        leading = {
                            Icon(
                                Icons.Filled.DragIndicator,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailing = {
                            IconButton(onClick = { onLayoutChange(current.copy(order = order).toggle(section)) }) {
                                Icon(
                                    if (hidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (hidden) "Show ${section.title}" else "Hide ${section.title}",
                                    tint = if (hidden) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier
                            .onSizeChanged { heights[section] = it.height }
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                            .alpha(if (hidden && !isDragging) 0.6f else 1f)
                            // Screen readers can't drag, so offer the same moves as actions.
                            .semantics {
                                customActions = listOf(
                                    CustomAccessibilityAction("Move up") { moveBy(section, -1) },
                                    CustomAccessibilityAction("Move down") { moveBy(section, 1) }
                                )
                            }
                            .pointerInput(section) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        dragging = section
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                        val i = order.indexOf(section)
                                        // Swap once the row is past half of its neighbour, then
                                        // re-base the offset on the row's new resting slot.
                                        val neighbour = if (dragOffset > 0) order.getOrNull(i + 1) else order.getOrNull(i - 1)
                                        val span = neighbour?.let { (heights[it] ?: 0) + spacingPx } ?: return@detectDragGesturesAfterLongPress
                                        if (abs(dragOffset) > span / 2) {
                                            val step = if (dragOffset > 0) 1 else -1
                                            order = current.copy(order = order).move(i, i + step).order
                                            dragOffset -= step * span
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    },
                                    onDragEnd = {
                                        dragging = null
                                        dragOffset = 0f
                                        if (order != current.order) save(order)
                                    },
                                    onDragCancel = {
                                        dragging = null
                                        dragOffset = 0f
                                        order = current.order
                                    }
                                )
                            }
                    )
                }
            }
        }

        Spacer(Modifier.size(16.dp))
        TextButton(
            onClick = { onLayoutChange(HomeLayout.DEFAULT) },
            enabled = layout != HomeLayout.DEFAULT,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Reset to default")
        }
    }
}

@Composable
private fun SectionRow(
    title: String,
    description: String,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    elevated: Boolean = false
) {
    Surface(
        shape = RowShape,
        color = if (elevated) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = if (elevated) 8.dp else 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading()
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trailing()
        }
    }
}
