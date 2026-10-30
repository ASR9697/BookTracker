package com.example.booktracker.wear

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * The reader's palette from the phone, as plain ARGB so the app and the tile colour
 * themselves the same way. Until the phone has said, it's the app's own amber.
 */
data class WatchPalette(
    val primary: Int,
    val primaryDim: Int,
    val primaryContainer: Int,
    val onPrimary: Int,
    val onPrimaryContainer: Int,
    val secondary: Int,
    val secondaryContainer: Int,
    val onSecondary: Int,
    val onSecondaryContainer: Int
) {
    companion object {
        private const val AMBER = 0xFFF59E0BL
        private const val TERRACOTTA = 0xFFFB923CL

        fun of(shelf: Shelf?): WatchPalette = from(
            shelf?.accent?.takeIf { it != 0L } ?: AMBER,
            shelf?.accentSecondary?.takeIf { it != 0L } ?: TERRACOTTA
        )

        fun from(accent: Long, second: Long): WatchPalette {
            val a = accent.toInt()
            val b = second.toInt()
            return WatchPalette(
                primary = a,
                primaryDim = mix(a, BLACK, 0.25f),
                primaryContainer = mix(a, BLACK, 0.62f),
                onPrimary = mix(a, BLACK, 0.8f),
                onPrimaryContainer = mix(a, WHITE, 0.75f),
                secondary = b,
                secondaryContainer = mix(b, BLACK, 0.62f),
                onSecondary = mix(b, BLACK, 0.8f),
                onSecondaryContainer = mix(b, WHITE, 0.75f)
            )
        }

        private const val BLACK = 0xFF000000.toInt()
        private const val WHITE = 0xFFFFFFFF.toInt()

        private fun mix(from: Int, to: Int, t: Float): Int {
            fun channel(shift: Int): Int {
                val x = (from shr shift) and 0xFF
                val y = (to shr shift) and 0xFF
                return (x + (y - x) * t).toInt().coerceIn(0, 255) shl shift
            }
            return (0xFF shl 24) or channel(16) or channel(8) or channel(0)
        }
    }
}

// The phone's dark surfaces, on a true-black background for OLED.
private val SurfaceLow = Color(0xFF1D1B20)
private val SurfaceMid = Color(0xFF211F26)
private val SurfaceHigh = Color(0xFF2B2930)
private val OnSurface = Color(0xFFE6E0E9)
private val OnSurfaceVariant = Color(0xFFCAC4D0)

/** The phone's display face, for titles and the page number. */
val Fraunces = FontFamily(Font(R.font.fraunces_semibold, FontWeight.SemiBold))

val TitleStyle = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 18.sp)
val PageNumberStyle = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 42.sp)

@Composable
fun NocturnalTheme(palette: WatchPalette, content: @Composable () -> Unit) {
    val colors = remember(palette) {
        ColorScheme(
            primary = Color(palette.primary),
            primaryDim = Color(palette.primaryDim),
            primaryContainer = Color(palette.primaryContainer),
            onPrimary = Color(palette.onPrimary),
            onPrimaryContainer = Color(palette.onPrimaryContainer),
            secondary = Color(palette.secondary),
            secondaryContainer = Color(palette.secondaryContainer),
            onSecondary = Color(palette.onSecondary),
            onSecondaryContainer = Color(palette.onSecondaryContainer),
            tertiary = Color(palette.secondary),
            onTertiary = Color(palette.onSecondary),
            surfaceContainerLow = SurfaceLow,
            surfaceContainer = SurfaceMid,
            surfaceContainerHigh = SurfaceHigh,
            onSurface = OnSurface,
            onSurfaceVariant = OnSurfaceVariant,
            outline = Color(0xFF938F99),
            outlineVariant = Color(0xFF49454F),
            background = Color.Black,
            onBackground = OnSurface
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
