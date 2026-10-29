package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.runtime.key
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.booktracker.app.format.FormatAdaptabilityLayer
import com.example.booktracker.shared.models.Book
import java.util.Locale
import kotlin.math.abs

private val ITEM_HEIGHT = 48.dp
private val WHEEL_HEIGHT = 240.dp

/**
 * "How much did you read?" — the single progress entry point used by the save
 * screen and the book detail header.
 *
 * Defaults to a snapping wheel because picking a nearby page is the common case
 * and scrolling beats typing for it; jumping a long way is the exception, so the
 * keyboard stays one tap away rather than being the default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressPickerSheet(
    book: Book,
    initialPage: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    /** Set when logging a session: adds quick steps from it and a live "+N pages" line. */
    sessionStartPage: Int? = null,
    sessionSeconds: Int = 0,
    /** Opens straight onto the page scanner. */
    startWithScan: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    val unit = FormatAdaptabilityLayer.unitAbbrev(book)
    val maxPage = if (book.totalPages > 0) book.totalPages else 9_999

    var keyboardMode by remember { mutableStateOf(false) }
    var selected by remember { mutableIntStateOf(initialPage.coerceIn(0, maxPage)) }
    var scanning by remember { mutableStateOf(startWithScan) }
    // Bumped when a scan sets the page, so the wheel re-centres on it.
    var wheelKey by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "How much did you read?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                onClick = { keyboardMode = !keyboardMode }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        if (keyboardMode) "Wheel" else "Keyboard",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(0.dp))
                    Icon(
                        if (keyboardMode) Icons.Rounded.Numbers else Icons.Rounded.Keyboard,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .height(18.dp)
                    )
                }
            }
            // Read the page number off the page itself.
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                onClick = { scanning = true }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.height(18.dp))
                    Text("Scan page", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
                }
            }
            }

            if (sessionStartPage != null) {
                Spacer(Modifier.height(12.dp))
                // Steps count from where the session began, not from the wheel.
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    listOf(0, 5, 10, 20).forEach { step ->
                        val page = (sessionStartPage + step).coerceIn(0, maxPage)
                        TickFilterChip(
                            selected = selected == page,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selected = page
                                wheelKey++
                            },
                            label = { Text(if (step == 0) "Same page" else "+$step") }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (keyboardMode) {
                key(wheelKey) {
                    KeyboardEntry(
                        book = book,
                        maxPage = maxPage,
                        value = selected,
                        onValueChange = { selected = it }
                    )
                }
            } else {
                key(wheelKey) {
                    PageWheel(
                        maxPage = maxPage,
                        totalPages = book.totalPages,
                        unit = unit,
                        initial = selected,
                        onSelected = { selected = it }
                    )
                }
            }

            if (sessionStartPage != null) {
                val delta = selected - sessionStartPage
                val pace = com.example.booktracker.app.analytics.AnalyticsEngine.formatSessionPpm(
                    com.example.booktracker.app.analytics.AnalyticsEngine.sessionSpeedPpm(delta, sessionSeconds)
                )
                val word = FormatAdaptabilityLayer.getDisplayUnit(book).lowercase().let { if (abs(delta) == 1) it.trimEnd('s') else it }
                Spacer(Modifier.height(12.dp))
                Text(
                    when {
                        delta > 0 && pace != null -> "+$delta $word this session · $pace"
                        delta > 0 -> "+$delta $word this session"
                        delta == 0 -> "Same ${word.trimEnd('s')} as you started on"
                        else -> "${-delta} $word back from where you started"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (delta > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onConfirm(selected)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = AppShape.M
            ) {
                Icon(Icons.Rounded.Check, contentDescription = "Confirm", modifier = Modifier.height(26.dp))
            }
        }
    }

    if (scanning) {
        PageScanDialog(
            book = book,
            onDismiss = { scanning = false },
            onPage = { page ->
                scanning = false
                selected = page.coerceIn(0, maxPage)
                keyboardMode = false
                wheelKey++
            }
        )
    }
}

@Composable
private fun PageWheel(
    maxPage: Int,
    totalPages: Int,
    unit: String,
    initial: Int,
    onSelected: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // The row nearest the viewport centre is the selection; deriving it from
    // layout rather than tracking scroll deltas keeps it correct after a fling.
    val centered by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) return@derivedStateOf initial
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            info.visibleItemsInfo.minByOrNull {
                abs((it.offset + it.size / 2f) - center)
            }?.index ?: initial
        }
    }

    // The list starts at index 0, so it reports a spurious "centered = 0" before
    // the initial scroll lands. Gate reporting until that scroll has happened,
    // otherwise confirming immediately would save page 0.
    var settled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        listState.scrollToItem(initial)
        settled = true
    }

    LaunchedEffect(centered, settled) {
        if (!settled) return@LaunchedEffect
        onSelected(centered)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(WHEEL_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ITEM_HEIGHT + 8.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    AppShape.S
                )
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                vertical = (WHEEL_HEIGHT - ITEM_HEIGHT) / 2
            )
        ) {
            items(count = maxPage + 1) { page ->
                val distance = abs(page - centered)
                val isSelected = distance == 0
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ITEM_HEIGHT),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "$unit $page",
                            fontSize = if (isSelected) 26.sp else 18.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = when (distance) {
                                    0 -> 1f
                                    1 -> 0.55f
                                    2 -> 0.3f
                                    else -> 0.15f
                                }
                            )
                        )
                        if (totalPages > 0) {
                            Spacer(Modifier.height(0.dp))
                            Text(
                                " (${percentLabel(page, totalPages)})",
                                fontSize = if (isSelected) 15.sp else 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = when (distance) {
                                        0 -> 1f
                                        1 -> 0.5f
                                        2 -> 0.28f
                                        else -> 0.12f
                                    }
                                ),
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardEntry(
    book: Book,
    maxPage: Int,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    var text by remember { mutableStateOf(value.toString()) }
    val parsed = text.toIntOrNull()
    val valid = parsed != null && parsed in 0..maxPage

    LaunchedEffect(parsed, valid) {
        if (valid && parsed != null) onValueChange(parsed)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(WHEEL_HEIGHT),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.filter(Char::isDigit).take(6) },
            label = { Text(FormatAdaptabilityLayer.getDisplayUnit(book).trimEnd('s')) },
            supportingText = {
                Text(
                    if (book.totalPages > 0) {
                        "of ${book.totalPages}" +
                            (parsed?.takeIf { valid }?.let { " · ${percentLabel(it, book.totalPages)}" } ?: "")
                    } else "No total recorded for this book"
                )
            },
            isError = text.isNotEmpty() && !valid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun percentLabel(page: Int, totalPages: Int): String =
    String.format(Locale.US, "%.1f%%", page * 100f / totalPages)
