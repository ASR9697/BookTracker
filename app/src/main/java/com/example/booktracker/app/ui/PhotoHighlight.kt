package com.example.booktracker.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.booktracker.app.data.NoteImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min

private val HighlightColor = Color(0xFFFFE600)
private const val HighlightAlpha = 0.45f
/** A drag shorter than this (in either direction, as a share of the photo) is a stray touch, not a highlight. */
private const val MIN_SIDE = 0.01f

/**
 * Full-screen editor for marking up a note photo: drag across the lines you want
 * to stand out. Saving writes a new image with the highlights burned in and hands
 * back its URI; the original file is left alone until the next clean-up.
 */
@Composable
fun PhotoHighlightEditor(uri: String, onDismiss: () -> Unit, onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bitmap by produceState<Bitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            NoteImageStore.localFile(uri)?.let { BitmapFactory.decodeFile(it.path) }
        }
    }
    val marks = remember { mutableStateListOf<Rect>() }
    var area by remember { mutableStateOf(IntSize.Zero) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragNow by remember { mutableStateOf<Offset?>(null) }
    var saving by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().onSizeChanged { area = it },
                contentAlignment = Alignment.Center
            ) {
                val bmp = bitmap
                if (bmp == null) {
                    CircularProgressIndicator()
                } else {
                    val shown = fitRect(area, bmp.width, bmp.height)
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(shown) {
                                detectDragGestures(
                                    onDragStart = { dragStart = it; dragNow = it },
                                    onDrag = { change, _ -> change.consume(); dragNow = change.position },
                                    onDragEnd = {
                                        val a = dragStart
                                        val b = dragNow
                                        if (a != null && b != null) {
                                            toNormalized(shown, a, b)?.let { marks += it }
                                        }
                                        dragStart = null
                                        dragNow = null
                                    },
                                    onDragCancel = { dragStart = null; dragNow = null }
                                )
                            }
                    ) {
                        drawImage(
                            bmp.asImageBitmap(),
                            dstOffset = IntOffset(shown.left.toInt(), shown.top.toInt()),
                            dstSize = IntSize(shown.width.toInt(), shown.height.toInt())
                        )
                        marks.forEach { drawRect(HighlightColor.copy(alpha = HighlightAlpha), toScreen(shown, it).topLeft, toScreen(shown, it).size) }
                        val a = dragStart
                        val b = dragNow
                        if (a != null && b != null) {
                            val live = Rect(minOf(a.x, b.x), minOf(a.y, b.y), maxOf(a.x, b.x), maxOf(a.y, b.y))
                            drawRect(HighlightColor.copy(alpha = HighlightAlpha), live.topLeft, Size(live.width, live.height))
                        }
                    }
                }
            }
            Text(
                "Drag across the lines you want to highlight",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp).align(Alignment.CenterHorizontally)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
                OutlinedButton(enabled = marks.isNotEmpty() && !saving, onClick = { marks.removeAt(marks.lastIndex) }) { Text("Undo") }
                Box(modifier = Modifier.weight(1f))
                Button(
                    enabled = marks.isNotEmpty() && bitmap != null && !saving,
                    onClick = {
                        val source = bitmap ?: return@Button
                        saving = true
                        scope.launch {
                            val saved = withContext(Dispatchers.IO) { burnIn(context, source, marks.toList()) }
                            saving = false
                            saved?.let(onSaved)
                        }
                    }
                ) { Text(if (saving) "Saving…" else "Save") }
            }
        }
    }
}

/** Where a photo of [bw] x [bh] sits when fitted and centred in [area]. */
internal fun fitRect(area: IntSize, bw: Int, bh: Int): Rect {
    if (area.width == 0 || area.height == 0 || bw == 0 || bh == 0) return Rect.Zero
    val scale = min(area.width.toFloat() / bw, area.height.toFloat() / bh)
    val w = bw * scale
    val h = bh * scale
    val left = (area.width - w) / 2f
    val top = (area.height - h) / 2f
    return Rect(left, top, left + w, top + h)
}

/** A drag in screen space as a rectangle in 0..1 photo space, or null when it's outside the photo or too small. */
internal fun toNormalized(shown: Rect, a: Offset, b: Offset): Rect? {
    if (shown.width <= 0f || shown.height <= 0f) return null
    fun nx(x: Float) = ((x - shown.left) / shown.width).coerceIn(0f, 1f)
    fun ny(y: Float) = ((y - shown.top) / shown.height).coerceIn(0f, 1f)
    val rect = Rect(minOf(nx(a.x), nx(b.x)), minOf(ny(a.y), ny(b.y)), maxOf(nx(a.x), nx(b.x)), maxOf(ny(a.y), ny(b.y)))
    return rect.takeIf { abs(it.width) >= MIN_SIDE && abs(it.height) >= MIN_SIDE }
}

private fun toScreen(shown: Rect, n: Rect): Rect = Rect(
    shown.left + n.left * shown.width, shown.top + n.top * shown.height,
    shown.left + n.right * shown.width, shown.top + n.bottom * shown.height
)

/** Draws [marks] (0..1 photo space) onto a copy of [source] and stores it, returning the new URI. */
private fun burnIn(context: android.content.Context, source: Bitmap, marks: List<Rect>): String? = runCatching {
    val copy = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = AndroidCanvas(copy)
    val paint = Paint().apply {
        color = android.graphics.Color.argb((HighlightAlpha * 255).toInt(), 255, 230, 0)
        style = Paint.Style.FILL
    }
    marks.forEach { m ->
        canvas.drawRect(RectF(m.left * copy.width, m.top * copy.height, m.right * copy.width, m.bottom * copy.height), paint)
    }
    NoteImageStore.saveBitmap(context, copy).also { copy.recycle() }
}.getOrNull()
