package com.example.booktracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.HeadsUp
import com.example.booktracker.app.ui.theme.AppShape
import com.example.booktracker.shared.models.Book

/**
 * Home's "today" card: one line for each thing that needs the reader today
 * ([HeadsUp]), each opening its book. Draws nothing on an ordinary day.
 */
@Composable
fun HeadsUpCard(items: List<HeadsUp.Item>, onOpenBook: (Book) -> Unit, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    Surface(
        shape = AppShape.L,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            items.forEachIndexed { i, item ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                HeadsUpRow(item, onClick = { onOpenBook(item.book) })
            }
        }
    }
}

@Composable
private fun HeadsUpRow(item: HeadsUp.Item, onClick: () -> Unit) {
    val urgent = item.kind == HeadsUp.Kind.LOAN_OVERDUE
    val tint = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val icon = when (item.kind) {
        HeadsUp.Kind.LOAN_OVERDUE -> Icons.Rounded.ErrorOutline
        HeadsUp.Kind.LOAN_DUE -> Icons.Rounded.Schedule
        HeadsUp.Kind.PLANNED -> Icons.Rounded.Event
        HeadsUp.Kind.RELEASED -> Icons.Rounded.NewReleases
        HeadsUp.Kind.ON_THIS_DAY -> Icons.Rounded.History
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f))
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (urgent) tint else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                item.detail,
                style = MaterialTheme.typography.bodyMedium,
                // Two lines, so a long title still leaves room for when the loan is due.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
