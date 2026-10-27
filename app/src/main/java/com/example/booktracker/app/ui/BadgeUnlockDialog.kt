package com.example.booktracker.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.booktracker.app.analytics.Badge
import com.example.booktracker.app.ui.animations.ConfettiExplosion
import com.example.booktracker.app.ui.animations.Motion
import com.example.booktracker.app.ui.animations.rememberHaptics
import com.example.booktracker.app.ui.animations.rememberReduceMotion
import kotlinx.coroutines.launch

/**
 * The moment a badge unlocks: the medal flips in from its back, a shine sweeps
 * across it and confetti in its tier colour bursts behind. Several unlocks at
 * once are shown one after another.
 */
@Composable
fun BadgeUnlockDialog(
    badges: List<Badge>,
    onViewAll: () -> Unit,
    onDismiss: () -> Unit
) {
    if (badges.isEmpty()) return
    var index by remember(badges) { mutableIntStateOf(0) }
    val badge = badges[index.coerceIn(badges.indices)]
    val tierColor = getBadgeTierColor(badge.tier)
    val reduce = rememberReduceMotion()
    val haptics = rememberHaptics()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center
        ) {
            key(index) {
                var showConfetti by remember { mutableStateOf(true) }
                if (showConfetti) {
                    ConfettiExplosion(
                        colors = listOf(tierColor, tierColor.copy(alpha = 0.7f), Color.White, Color(0xFFFFD54F)),
                        origin = Offset(0.5f, 0.42f),
                        particleCount = 90,
                        onFinished = { showConfetti = false }
                    )
                }

                val flip = remember { Animatable(if (reduce) 0f else 180f) }
                val scale = remember { Animatable(if (reduce) 1f else 0.6f) }
                LaunchedEffect(Unit) {
                    haptics.celebrate()
                    launch { flip.animateTo(0f, tween(750, easing = Motion.Emphasized)) }
                    launch { scale.animateTo(1f, Motion.press()) }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        "ACHIEVEMENT UNLOCKED",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = tierColor
                    )
                    Spacer(Modifier.height(28.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(168.dp)
                            .graphicsLayer {
                                rotationY = flip.value
                                scaleX = scale.value
                                scaleY = scale.value
                                cameraDistance = 14f * density
                            }
                    ) {
                        if (flip.value > 90f) {
                            // The back of the medal, seen during the first half of the flip.
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    .background(tierColor.copy(alpha = 0.25f))
                                    .border(3.dp, tierColor, CircleShape)
                            ) {
                                Icon(Icons.Filled.QuestionMark, contentDescription = null, tint = tierColor, modifier = Modifier.size(64.dp))
                            }
                        } else {
                            ShiningMedal(badge, tierColor, shine = !reduce)
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = tierColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, tierColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            badge.tier,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = tierColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        badge.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        badge.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(36.dp))
                    Button(
                        onClick = { if (index < badges.lastIndex) index++ else onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = tierColor, contentColor = Color.Black)
                    ) {
                        Text(if (index < badges.lastIndex) "Next (${index + 2} of ${badges.size})" else "Nice!", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onViewAll) {
                        Text("See all achievements", color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }
        }
    }
}

/** The medal face, with a band of light sweeping across it every couple of seconds. */
@Composable
private fun ShiningMedal(badge: Badge, tierColor: Color, shine: Boolean) {
    val transition = rememberInfiniteTransition(label = "shine")
    val sweep by transition.animateFloat(
        -1f, 2f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "sweep"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(tierColor.copy(alpha = 0.45f), tierColor.copy(alpha = 0.15f))))
            .border(3.dp, tierColor, CircleShape)
            .drawWithContent {
                drawContent()
                if (shine) {
                    val x = sweep * size.width
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                            start = Offset(x - size.width * 0.3f, 0f),
                            end = Offset(x + size.width * 0.3f, size.height)
                        )
                    )
                }
            }
    ) {
        Icon(badge.icon, contentDescription = null, tint = tierColor, modifier = Modifier.size(80.dp))
    }
}
