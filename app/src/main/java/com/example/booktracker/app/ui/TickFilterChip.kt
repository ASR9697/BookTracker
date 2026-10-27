package com.example.booktracker.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SelectableChipElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.example.booktracker.app.ui.animations.rememberHaptics

/** [FilterChip] with the app's light haptic tick, so every filter you flip answers your finger. */
@Composable
fun TickFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = FilterChipDefaults.shape,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
    border: BorderStroke? = FilterChipDefaults.filterChipBorder(enabled, selected),
    interactionSource: MutableInteractionSource? = null
) {
    val haptics = rememberHaptics()
    FilterChip(
        selected = selected,
        onClick = {
            haptics.tick()
            onClick()
        },
        label = label,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        interactionSource = interactionSource
    )
}
