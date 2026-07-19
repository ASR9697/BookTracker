package com.example.booktracker.app.ui.animations

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlinx.coroutines.isActive
import kotlin.random.Random

class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var rotation: Float,
    var rotationSpeed: Float,
    var size: Float,
    var life: Float = 1f
)

@Composable
fun ConfettiExplosion(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFFC107)),
    onFinished: () -> Unit
) {
    val particles = remember { mutableStateListOf<Particle>() }

    LaunchedEffect(Unit) {
        // Initialize particles
        val centerX = 500f // Will be adjusted by canvas size
        val centerY = 1500f // Bottom of screen roughly
        
        repeat(80) {
            particles.add(
                Particle(
                    x = centerX + Random.nextFloat() * 100 - 50,
                    y = centerY,
                    vx = Random.nextFloat() * 40 - 20,
                    vy = -(Random.nextFloat() * 40 + 20),
                    color = colors.random(),
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = Random.nextFloat() * 20 - 10,
                    size = Random.nextFloat() * 15f + 10f
                )
            )
        }

        var lastTime = 0L
        while (isActive && particles.isNotEmpty()) {
            withInfiniteAnimationFrameMillis { time ->
                if (lastTime == 0L) lastTime = time
                val dt = (time - lastTime) / 16f // Normalized to ~60fps
                lastTime = time

                val iterator = particles.iterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    p.vy += 1.5f * dt // Gravity
                    p.rotation += p.rotationSpeed * dt
                    p.life -= 0.01f * dt

                    if (p.life <= 0 || p.y > 3000f) {
                        iterator.remove()
                    }
                }
            }
        }
        onFinished()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            withTransform({
                translate(left = p.x, top = p.y)
                rotate(p.rotation)
            }) {
                drawRect(
                    color = p.color.copy(alpha = p.life.coerceIn(0f, 1f)),
                    size = androidx.compose.ui.geometry.Size(p.size, p.size),
                    topLeft = Offset(-p.size / 2, -p.size / 2)
                )
            }
        }
    }
}
