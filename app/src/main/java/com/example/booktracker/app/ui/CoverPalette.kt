package com.example.booktracker.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware

/** Accent colours pulled from a book cover, or nulls until the image resolves. */
data class CoverAccent(
    val vibrant: Color? = null,
    val dominant: Color? = null
) {
    val hasColor: Boolean get() = vibrant != null && dominant != null

    /**
     * A background dark and desaturated enough to carry white text, derived from
     * the cover rather than the theme. Falls back to [fallback] before the image
     * loads or when the cover yields no usable swatch.
     */
    fun immersiveBackground(fallback: Color): Color {
        val base = dominant ?: vibrant ?: return fallback
        val blended = base.copy(alpha = 0.55f).compositeOver(NightBase)
        // Pale covers (white, cream) would still leave white text unreadable;
        // pull them further toward the night base to keep roughly 4.5:1 contrast.
        return if (blended.luminance() > 0.17f) lerp(blended, NightBase, 0.5f) else blended
    }

    private companion object {
        val NightBase = Color(0xFF1B2430)
    }
}

/**
 * A soft wash of the cover's colour fading into the surface: down from the top
 * of a screen, or in from the left of a card. It fades in once the palette
 * resolves, so each book's pages feel like its own.
 */
fun Modifier.coverGlow(accent: CoverAccent, strength: Float = 0.28f, fromLeft: Boolean = false): Modifier = composed {
    val target = (accent.vibrant ?: accent.dominant)?.copy(alpha = strength) ?: Color.Transparent
    val color by animateColorAsState(target, tween(com.example.booktracker.app.ui.animations.Motion.LONG), label = "coverGlow")
    drawBehind {
        if (color.alpha <= 0f) return@drawBehind
        drawRect(
            if (fromLeft) Brush.horizontalGradient(listOf(color, Color.Transparent), endX = size.width * 0.7f)
            else Brush.verticalGradient(listOf(color, Color.Transparent), endY = size.height * 0.5f)
        )
    }
}

private const val PALETTE_SIZE_PX = 160

/**
 * Decodes the cover once and extracts its palette. Coil caches the bitmap, so
 * repeat visits to the same book resolve without another decode.
 */
@Composable
fun rememberCoverAccent(coverUrl: String): CoverAccent {
    val context = LocalContext.current
    var accent by remember(coverUrl) { mutableStateOf(CoverAccent()) }

    LaunchedEffect(coverUrl) {
        if (coverUrl.isBlank()) {
            accent = CoverAccent()
            return@LaunchedEffect
        }
        runCatching {
            val request = ImageRequest.Builder(context)
                .data(coverUrl)
                .allowHardware(false) // Palette needs to read pixels back.
                // Palette samples about 112 px square anyway; decoding the full
                // cover in software for it stalled screens that open with one.
                .size(PALETTE_SIZE_PX)
                .build()
            val result = context.imageLoader.execute(request)
            (result as? SuccessResult)?.let { success ->
                (success.image as? BitmapImage)?.bitmap?.let { bitmap ->
                    Palette.from(bitmap).generate { palette ->
                        val dominant = palette?.dominantSwatch?.rgb
                        val vibrant = palette?.vibrantSwatch?.rgb ?: dominant
                        accent = CoverAccent(
                            vibrant = vibrant?.let { Color(it) },
                            dominant = dominant?.let { Color(it) }
                        )
                    }
                }
            }
        }
    }

    return accent
}
