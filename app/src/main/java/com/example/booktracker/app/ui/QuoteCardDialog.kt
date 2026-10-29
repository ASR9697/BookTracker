package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.MarginNote

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

/**
 * Bookmory-style Aesthetic Quote Card Dialog.
 * Allows users to choose an aesthetic theme, card format (Square vs Story),
 * preview the card, and export/share a high-resolution image to Instagram/social media or copy the quote.
 */
@Composable
fun QuoteCardDialog(
    note: MarginNote,
    book: Book,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(QuoteCardTheme.WARM_PARCHMENT) }
    var selectedFormat by remember { mutableStateOf(QuoteCardFormat.SQUARE) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                // A share card, not a banner: on a tablet it stays card-sized.
                .widthIn(max = 480.dp)
                .wrapContentHeight(),
            shape = AppShape.XL,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Quote card",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Format Selector (Square 1:1 vs Story 9:16)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShape.S)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    QuoteCardFormat.entries.forEach { fmt ->
                        val isSelected = selectedFormat == fmt
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(AppShape.S)
                                .clickable { selectedFormat = fmt },
                            shape = AppShape.S,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ) {
                            Text(
                                text = fmt.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Card Preview (dynamically sized)
                val brush = Brush.linearGradient(
                    colors = selectedTheme.gradientColors.map { Color(it) }
                )
                val cardColor = Color(selectedTheme.cardColor)
                val textColor = Color(selectedTheme.textColor)
                val attrColor = Color(selectedTheme.attrColor)
                val previewAspectRatio = if (selectedFormat == QuoteCardFormat.SQUARE) 1f else 0.75f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(previewAspectRatio)
                        .clip(AppShape.L)
                        .background(brush)
                        .padding(if (selectedFormat == QuoteCardFormat.STORY) 14.dp else 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(if (selectedFormat == QuoteCardFormat.STORY) 0.88f else 1f),
                        shape = AppShape.L,
                        color = cardColor,
                        border = if (selectedTheme == QuoteCardTheme.POLAROID) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)) else null
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top quote glyph
                            Icon(
                                Icons.Rounded.FormatQuote,
                                contentDescription = null,
                                tint = attrColor.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(34.dp)
                                    .align(Alignment.Start)
                            )

                            // Quote Content
                            Text(
                                text = "“${note.content}”",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = if (note.content.length > 180) 13.sp else 16.sp,
                                    lineHeight = if (note.content.length > 180) 19.sp else 23.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontStyle = FontStyle.Italic
                                ),
                                color = textColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )

                            // Bottom Attribution
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (note.pageNumber > 0) {
                                    Text(
                                        text = "Page ${note.pageNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = attrColor
                                    )
                                    Spacer(Modifier.height(2.dp))
                                }
                                val author = if (book.authors.isNotEmpty()) " by ${book.authors.first()}" else ""
                                Text(
                                    text = "— ${book.title}$author",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif
                                    ),
                                    color = attrColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Theme selector (Horizontal scroll)
                Text(
                    "Card style (${QuoteCardTheme.entries.size} looks)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuoteCardTheme.entries.forEach { theme ->
                        val isSelected = selectedTheme == theme
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTheme = theme },
                            label = { Text(theme.title) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(theme.gradientColors[0]))
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val author = if (book.authors.isNotEmpty()) " by ${book.authors.first()}" else ""
                            val clip = ClipData.newPlainText(
                                "Quote",
                                "“${note.content}”\n— ${book.title}$author"
                            )
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Quote copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = AppShape.M
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy")
                    }

                    Button(
                        onClick = {
                            QuoteShareHelper.shareQuote(context, note, book, selectedTheme, selectedFormat)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = AppShape.M
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share card")
                    }
                }
            }
        }
    }
}
