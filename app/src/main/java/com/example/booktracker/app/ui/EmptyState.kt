package com.example.booktracker.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.ui.animations.LocalAmbientMotion
import com.example.booktracker.app.ui.animations.rememberAmbientClock
import com.example.booktracker.app.ui.animations.rememberReduceMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A friendly empty screen: an icon that gently floats inside soft rings with a
 * spark orbiting it, a short title, what to do next, and an optional action.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val reduce = rememberReduceMotion()
    // Still while its page is off screen (the pager keeps neighbours composed), so it costs no frames there.
    val clock = rememberAmbientClock(!reduce && LocalAmbientMotion.current)
    val primary = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = center
                    drawCircle(primary.copy(alpha = 0.06f), size.minDimension / 2f, c)
                    drawCircle(primary.copy(alpha = 0.10f), size.minDimension / 2.8f, c)
                    if (!reduce) {
                        val orbit = 2f * PI.toFloat() * (clock.value / 6f % 1f)
                        val r = size.minDimension / 2.3f
                        drawCircle(primary.copy(alpha = 0.7f), 3.dp.toPx(), Offset(c.x + cos(orbit) * r, c.y + sin(orbit) * r))
                        drawCircle(primary.copy(alpha = 0.35f), 2.dp.toPx(), Offset(c.x + cos(orbit + 3.1f) * r * 0.8f, c.y + sin(orbit + 3.1f) * r * 0.8f))
                    }
                }
                Icon(
                    icon,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(48.dp).graphicsLayer { translationY = if (reduce) 0f else (5f * sin(PI.toFloat() * clock.value / 1.8f)).dp.toPx() }
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (action != null) {
                Spacer(Modifier.height(20.dp))
                action()
            }
        }
    }
}
