package com.example.booktracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.TagTidy
import com.example.booktracker.app.ui.theme.AppShape
import com.example.booktracker.shared.models.Book

/**
 * Every tag in the library with how many books carry it. Tap one to rename it,
 * merge it into another by giving it that tag's name, or take it off every book.
 * Tags double as genres, so this is what tidies the genre chart on Stats.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagTidyScreen(
    books: List<Book>,
    onApply: (from: String, to: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val counts = remember(books) { TagTidy.counts(books) }
    var editing by remember { mutableStateOf<String?>(null) }

    if (counts.isEmpty()) {
        Column(modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
            Text("No tags yet", style = MaterialTheme.typography.titleLarge)
            Text(
                "Books added by search or the barcode scanner bring their genres along as tags. You can also add your own when editing a book.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp + windowCentredPadding(), vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "${counts.size} tags. Tap one to rename it, merge it into another, or remove it. " +
                    "Fewer, clearer tags make a better genre chart.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(counts, key = { it.first }) { (tag, count) ->
            Surface(
                onClick = { editing = tag },
                shape = AppShape.M,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Sell, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(tag, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).padding(start = 14.dp))
                    Text(
                        "$count ${if (count == 1) "book" else "books"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Edit $tag",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }

    editing?.let { tag ->
        var name by remember(tag) { mutableStateOf(tag) }
        val onBooks = counts.firstOrNull { it.first == tag }?.second ?: 0
        val newName = name.trim()
        val merges = newName.isNotEmpty() && newName != tag && counts.any { it.first.equals(newName, ignoreCase = true) }
        ActionSheet(
            onDismiss = { editing = null },
            title = "Edit tag",
            icon = Icons.Rounded.Sell,
            message = "$tag is on $onBooks ${if (onBooks == 1) "book" else "books"}.",
            confirmLabel = if (merges) "Merge" else "Save",
            confirmEnabled = newName.isNotEmpty() && newName != tag,
            onConfirm = { onApply(tag, newName); editing = null }
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = { Text("Tag name") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                shape = AppShape.M,
                modifier = Modifier.fillMaxWidth()
            )
            val others = counts.filter { it.first != tag }.take(6)
            if (others.isNotEmpty()) {
                Text(
                    "Or merge into",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    others.forEach { (other, _) ->
                        TickFilterChip(selected = name == other, onClick = { name = other }, label = { Text(other) })
                    }
                }
            }
            TextButton(
                onClick = { onApply(tag, null); editing = null },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Remove from $onBooks ${if (onBooks == 1) "book" else "books"}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
