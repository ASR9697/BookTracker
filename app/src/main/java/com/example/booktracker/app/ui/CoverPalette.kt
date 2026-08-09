package com.example.booktracker.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
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
        return base.copy(alpha = 0.55f).compositeOver(Color(0xFF1B2430))
    }
}

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
