package com.example.booktracker.app.hardware

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A handwriting capture sheet: draw a note with a stylus (or finger), then run
 * on-device recognition. Recognized text is handed back via [onRecognized].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylusScratchpad(
    onDismiss: () -> Unit,
    onRecognized: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hardware = remember { HardwareIntegrations() }

    val strokes = remember { mutableStateListOf<List<StylusPoint>>() }
    val current = remember { mutableStateListOf<StylusPoint>() }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val inkColor = MaterialTheme.colorScheme.onSurface

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Handwrite a note", style = MaterialTheme.typography.titleLarge)
            Text(
                "Scribble with your stylus or finger, then tap Recognize.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                current.clear()
                                current.add(StylusPoint(offset.x, offset.y, System.currentTimeMillis()))
                            },
                            onDrag = { change, _ ->
                                current.add(
                                    StylusPoint(change.position.x, change.position.y, System.currentTimeMillis())
                                )
                                change.consume()
                            },
                            onDragEnd = {
                                strokes.add(current.toList())
                                current.clear()
                            }
                        )
                    }
            ) {
                strokes.forEach { drawStroke(it, inkColor) }
                if (current.isNotEmpty()) drawStroke(current.toList(), inkColor)
            }

            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { strokes.clear(); current.clear(); message = null },
                    enabled = !busy
                ) {
                    Text("Clear")
                }
                Button(
                    onClick = {
                        busy = true
                        message = null
                        val captured = strokes.toList()
                        scope.launch {
                            val text = try {
                                hardware.recognizeHandwriting(captured)
                            } catch (e: Exception) {
                                null
                            }
                            busy = false
                            if (text.isNullOrBlank()) {
                                message = "Couldn't read that — try writing again."
                            } else {
                                onRecognized(text)
                            }
                        }
                    },
                    enabled = !busy && strokes.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Recognize")
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawStroke(points: List<StylusPoint>, color: Color) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(color, radius = 3f, center = androidx.compose.ui.geometry.Offset(points[0].x, points[0].y))
        return
    }
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}
