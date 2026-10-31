package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.ui.animations.rememberHaptics
import com.example.booktracker.app.ui.theme.AppShape

/**
 * The app's stand-in for a stock dialog: a bottom sheet with an icon, a title,
 * a line of explanation, optional [content], and up to two actions, the main
 * one full width. Reachable with a thumb and in keeping with the rest of the
 * app. [destructive] paints the main action in the error colour and gives it
 * a reject buzz. [alternateLabel] adds a third choice between the two, for
 * throwing the thing away (discard rather than save), in the error colour.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(
    onDismiss: () -> Unit,
    title: String,
    icon: ImageVector? = null,
    message: String? = null,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    dismissLabel: String? = "Cancel",
    onDismissAction: () -> Unit = onDismiss,
    alternateLabel: String? = null,
    onAlternate: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val haptics = rememberHaptics()
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                // Sheets with a text field (read time, budget) stay above the keyboard.
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
        ) {
            if (icon != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp).background(tint.copy(alpha = 0.14f), CircleShape)
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            if (message != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            if (content != null) {
                Spacer(Modifier.height(20.dp))
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
            }
            Spacer(Modifier.height(24.dp))
            if (confirmLabel != null && onConfirm != null) {
                Button(
                    onClick = {
                        if (destructive) haptics.reject() else haptics.confirm()
                        onConfirm()
                    },
                    enabled = confirmEnabled,
                    shape = AppShape.M,
                    colors = if (destructive) ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) else ButtonDefaults.buttonColors(),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text(confirmLabel, style = MaterialTheme.typography.titleSmall) }
            }
            if (alternateLabel != null && onAlternate != null) {
                TextButton(
                    onClick = {
                        haptics.reject()
                        onAlternate()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text(alternateLabel, color = MaterialTheme.colorScheme.error) }
            }
            if (dismissLabel != null) {
                TextButton(onClick = onDismissAction, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text(dismissLabel, color = if (destructive) MaterialTheme.colorScheme.onSurface else Color.Unspecified)
                }
            }
        }
    }
}
