package com.example.booktracker.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The app's five corner sizes. Use these rather than a hand-typed radius, so
 * a card on one screen matches a card on the next. Plain values, so they work
 * outside composition too (top-level shapes, draw code); the Material theme
 * hands the same five to its components.
 */
object AppShape {
    /** Tags, small pills on covers, progress tracks. */
    val XS = RoundedCornerShape(8.dp)
    /** Chips, text fields, small tiles, covers. */
    val S = RoundedCornerShape(12.dp)
    /** List rows, stat tiles, inner cards. */
    val M = RoundedCornerShape(16.dp)
    /** Cards. */
    val L = RoundedCornerShape(20.dp)
    /** Hero cards, dialogs, sheets. */
    val XL = RoundedCornerShape(28.dp)
}

val AppShapes = Shapes(
    extraSmall = AppShape.XS,
    small = AppShape.S,
    medium = AppShape.M,
    large = AppShape.L,
    extraLarge = AppShape.XL
)
