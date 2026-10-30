package com.example.booktracker.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.booktracker.app.ui.animations.Motion
import com.example.booktracker.app.ui.animations.rememberReduceMotion
import kotlin.math.roundToInt

/** Height of the slim bar a section header shrinks into, below the status bar. */
val SECTION_BAR_HEIGHT = 64.dp

/**
 * How much of the bottom of the screen the floating bottom bar covers, so a
 * section can let its last item scroll clear of it. Zero where there is no
 * bottom bar (tablets, deeper screens).
 */
val LocalNavBarClearance = compositionLocalOf { 0.dp }

/**
 * A main section with the header all five share: a big title area at the top
 * ([expanded]) that shrinks into a slim bar as the content scrolls, where
 * [collapsed] fades in at the same left edge. [actions] sit at the bar's end
 * the whole time. Scrolling back to the top grows the header again.
 *
 * The header moves with its page in the pager, so swiping between sections
 * never has one bar trying to be two things at once.
 *
 * [seeThrough] leaves the expanded header clear so a [backdrop] (Home's sky)
 * shows behind it; it fills in as it collapses. Slots get `collapse`, 0 when
 * open and 1 when shut, to read in draw or layer blocks.
 *
 * Pass [state] when the content's scroll state is hoisted, so the header is
 * kept and restored alongside it instead of opening over a scrolled page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionPage(
    expanded: @Composable () -> Unit,
    collapsed: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.(collapse: () -> Float) -> Unit = {},
    seeThrough: Boolean = false,
    backdrop: @Composable BoxScope.(collapsedPx: () -> Float) -> Unit = {},
    state: TopAppBarState = rememberTopAppBarState(),
    content: @Composable () -> Unit
) {
    val behavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state)
    Box(modifier.fillMaxSize()) {
        backdrop { -state.heightOffset }
        Column(Modifier.fillMaxSize().nestedScroll(behavior.nestedScrollConnection)) {
            SectionHeader(state, expanded, collapsed, actions, seeThrough, Modifier.zIndex(1f))
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        }
    }
}

/** [SectionPage] with the usual header: a [title] and a live [line] under it, plus anything [below] them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionPage(
    title: String,
    line: String?,
    modifier: Modifier = Modifier,
    below: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.(collapse: () -> Float) -> Unit = {},
    content: @Composable () -> Unit
) = SectionPage(
    expanded = {
        Column {
            SectionTitle(title, line)
            below?.invoke()
        }
    },
    collapsed = { CollapsedTitle(title) },
    modifier = modifier,
    actions = actions,
    content = content
)

/** The big serif title and its live line; the line rolls over when what it says changes. */
@Composable
fun SectionTitle(title: String, line: String?, modifier: Modifier = Modifier) {
    val reduce = rememberReduceMotion()
    Column(modifier) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (line != null) {
            AnimatedContent(
                targetState = line,
                transitionSpec = {
                    if (reduce) fadeIn(tween(Motion.SHORT)) togetherWith fadeOut(tween(Motion.SHORT))
                    else (slideInVertically(tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate)) { it / 2 } + fadeIn(tween(Motion.MEDIUM))) togetherWith
                        (slideOutVertically(tween(Motion.SHORT)) { -it / 2 } + fadeOut(tween(Motion.SHORT)))
                },
                label = "sectionLine"
            ) { text ->
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** The title as it sits in the slim bar. */
@Composable
fun CollapsedTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionHeader(
    state: TopAppBarState,
    expanded: @Composable () -> Unit,
    collapsed: @Composable () -> Unit,
    actions: @Composable RowScope.(collapse: () -> Float) -> Unit,
    seeThrough: Boolean,
    modifier: Modifier = Modifier
) {
    val fill = MaterialTheme.colorScheme.background
    val edge = MaterialTheme.colorScheme.outlineVariant
    val collapse = { state.collapsedFraction }
    // Composed only once it starts to show, so its buttons can't be tapped while invisible.
    val showCollapsed by remember { derivedStateOf { state.collapsedFraction > 0.5f } }
    Column(
        modifier
            .fillMaxWidth()
            .drawBehind {
                val f = collapse()
                drawRect(if (seeThrough) fill.copy(alpha = f) else fill)
                if (f > 0f) {
                    // A hairline and a soft shadow below the bar once content runs under it.
                    drawLine(edge.copy(alpha = 0.5f * f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                    val shadow = 10.dp.toPx()
                    drawRect(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.16f * f), Color.Transparent), startY = size.height, endY = size.height + shadow),
                        topLeft = Offset(0f, size.height),
                        size = Size(size.width, shadow)
                    )
                }
            }
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(SECTION_BAR_HEIGHT).padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f).graphicsLayer {
                    // Arrives over the second half of the collapse, rising into place.
                    val shown = ((collapse() - 0.5f) * 2f).coerceIn(0f, 1f)
                    alpha = shown
                    translationY = (1f - shown) * 12.dp.toPx()
                }
            ) { if (showCollapsed) collapsed() }
            actions(collapse)
        }
        // The big area gives up its height to the scroll; its content slides up and fades.
        Layout(
            content = {
                Box(
                    Modifier
                        .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                        .graphicsLayer { alpha = (1f - collapse() * 1.6f).coerceIn(0f, 1f) }
                ) { expanded() }
            },
            modifier = Modifier.fillMaxWidth().clipToBounds()
        ) { measurables, constraints ->
            val placeable = measurables.first().measure(constraints.copy(minHeight = 0, maxHeight = Int.MAX_VALUE))
            val full = placeable.height
            if (state.heightOffsetLimit != -full.toFloat()) state.heightOffsetLimit = -full.toFloat()
            val height = (full + state.heightOffset).roundToInt().coerceIn(0, full)
            layout(constraints.maxWidth, height) { placeable.place(0, height - full) }
        }
    }
}

/** A wide, friendly search field that opens search on tap, with the barcode scanner at its end. */
@Composable
fun SearchPill(text: String, onSearch: () -> Unit, onScan: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onSearch,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth().height(52.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 18.dp, end = 4.dp)) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onScan) {
                Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan ISBN")
            }
        }
    }
}
