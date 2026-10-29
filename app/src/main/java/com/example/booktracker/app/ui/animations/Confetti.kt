package com.example.booktracker.app.ui.animations

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
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
    var life: Float = 1f,
    val round: Boolean = false
)

/** How a burst looks: paper confetti that tumbles, or embers that rise and fade. */
enum class BurstStyle { CONFETTI, EMBERS }

val EmberColors = listOf(Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFFF7043), Color(0xFFFF8A65))

/**
 * A one-shot particle burst from [origin] (fractions of the canvas, so (0.5, 1)
 * is bottom centre). Sized from the canvas it lands in, redrawn every frame, and
 * skipped entirely when the reader has turned animations off.
 */
@Composable
fun ConfettiExplosion(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFFC107)),
    origin: Offset = Offset(0.5f, 1f),
    particleCount: Int = 80,
    style: BurstStyle = BurstStyle.CONFETTI,
    onFinished: () -> Unit
) {
    val reduce = rememberReduceMotion()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val particles = remember { ArrayList<Particle>() }
    // Bumped every frame: particles mutate in place, so this is what tells the canvas to redraw.
    var frame by remember { mutableLongStateOf(0L) }

    LaunchedEffect(canvasSize, reduce) {
        if (reduce) { onFinished(); return@LaunchedEffect }
        if (canvasSize == IntSize.Zero) return@LaunchedEffect
        val w = canvasSize.width.toFloat()
        val h = canvasSize.height.toFloat()
        val scale = w / 1080f // tuned on a 1080px-wide phone
        val startX = origin.x * w
        val startY = origin.y * h
        particles.clear()
        repeat(particleCount) {
            particles += when (style) {
                BurstStyle.CONFETTI -> {
                    // A fan pointing up, wider the closer the origin is to the bottom.
                    val angle = Math.toRadians(-90.0 + Random.nextDouble(-55.0, 55.0))
                    val speed = (Random.nextFloat() * 28f + 22f) * scale
                    Particle(
                        x = startX, y = startY,
                        vx = (cos(angle) * speed).toFloat(), vy = (sin(angle) * speed).toFloat(),
                        color = colors.random(),
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = Random.nextFloat() * 20f - 10f,
                        size = (Random.nextFloat() * 15f + 10f) * scale
                    )
                }
                BurstStyle.EMBERS -> Particle(
                    x = startX + (Random.nextFloat() - 0.5f) * 240f * scale,
                    y = startY + (Random.nextFloat() - 0.5f) * 60f * scale,
                    vx = (Random.nextFloat() - 0.5f) * 6f * scale,
                    vy = -(Random.nextFloat() * 10f + 6f) * scale,
                    color = colors.random(),
                    rotation = 0f,
                    rotationSpeed = 0f,
                    size = (Random.nextFloat() * 10f + 6f) * scale,
                    round = true
                )
            }
        }

        var lastTime = 0L
        while (isActive && particles.isNotEmpty()) {
            withInfiniteAnimationFrameMillis { time ->
                if (lastTime == 0L) lastTime = time
                val dt = ((time - lastTime) / 16f).coerceAtMost(3f) // ~60fps steps; no jumps after a stall
                lastTime = time
                val iterator = particles.iterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    when (style) {
                        BurstStyle.CONFETTI -> {
                            p.vy += 1.2f * scale * dt // gravity
                            p.vx *= 0.99f
                            p.life -= 0.008f * dt
                        }
                        BurstStyle.EMBERS -> {
                            p.vy *= 0.98f // embers drift up and slow
                            p.vx += (Random.nextFloat() - 0.5f) * 0.6f * scale
                            p.life -= 0.014f * dt
                        }
                    }
                    p.rotation += p.rotationSpeed * dt
                    if (p.life <= 0f || p.y > h + 100f) iterator.remove()
                }
                frame = time
            }
        }
        onFinished()
    }

    Canvas(modifier = modifier.fillMaxSize().onSizeChanged { canvasSize = it }) {
        frame // read so every frame redraws
        particles.forEach { p ->
            val alpha = p.life.coerceIn(0f, 1f)
            if (p.round) {
                drawCircle(p.color.copy(alpha = alpha * 0.35f), radius = p.size * 1.8f, center = Offset(p.x, p.y))
                drawCircle(p.color.copy(alpha = alpha), radius = p.size / 2f, center = Offset(p.x, p.y))
            } else {
                withTransform({
                    translate(left = p.x, top = p.y)
                    rotate(p.rotation, pivot = Offset.Zero)
                }) {
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        size = Size(p.size, p.size * 0.6f),
                        topLeft = Offset(-p.size / 2, -p.size * 0.3f)
                    )
                }
            }
        }
    }
}
