package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.booktracker.app.ui.animations.bounceClick
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Session
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Bookmory-style Interactive Monthly Wrap-up Dialog.
 * Allows readers to preview and share a beautiful graphic reading card of their monthly reading logs.
 */
@Composable
fun MonthlyReportDialog(
    yearMonth: YearMonth,
    books: List<Book>,
    sessions: List<Session>,
    userName: String = "Reader",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var selectedTheme by remember { mutableStateOf(MonthlyCardTheme.BOOKMORY_AMBER) }
    var selectedFormat by remember { mutableStateOf(QuoteCardFormat.SQUARE) }

    val zone = remember { ZoneId.systemDefault() }
    val monthSessions = remember(yearMonth, sessions) {
        sessions.filter {
            val date = Instant.ofEpochMilli(it.endTime).atZone(zone).toLocalDate()
            YearMonth.from(date) == yearMonth
        }
    }
    val totalPages = remember(monthSessions) { monthSessions.sumOf { it.pagesRead } }
    val totalMinutes = remember(monthSessions) { monthSessions.sumOf { it.durationSeconds } / 60 }
    val totalHours = remember(totalMinutes) { String.format(Locale.getDefault(), "%.1f", totalMinutes / 60f) }
    val activeDays = remember(monthSessions) {
        monthSessions.map { Instant.ofEpochMilli(it.endTime).atZone(zone).toLocalDate() }.distinct().size
    }
    val finishedThisMonth = remember(books, yearMonth) {
        books.filter { b ->
            b.status == BookStatus.FINISHED.name && b.completedAt > 0L &&
                    YearMonth.from(Instant.ofEpochMilli(b.completedAt).atZone(zone).toLocalDate()) == yearMonth
        }
    }

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }

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
                    Column {
                        Text(
                            "Share Monthly Report",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            yearMonth.format(monthFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Format Picker (Square vs Story)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    QuoteCardFormat.entries.forEachIndexed { index, format ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = QuoteCardFormat.entries.size),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedFormat = format
                            },
                            selected = selectedFormat == format
                        ) {
                            Text(format.title)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Theme Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MonthlyCardTheme.entries.forEach { theme ->
                        val isSelected = selectedTheme == theme
                        AssistChip(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTheme = theme
                            },
                            label = { Text(theme.title) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color(theme.gradientColors[0]), CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Live Preview Card
                val aspectRatio = if (selectedFormat == QuoteCardFormat.STORY) 9f / 16f else 1f
                val cardGradient = Brush.linearGradient(selectedTheme.gradientColors.map { Color(it) })
                val cardColor = Color(selectedTheme.cardColor)
                val textColor = Color(selectedTheme.textColor)
                val accentColor = Color(selectedTheme.accentColor)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .clip(AppShape.L)
                        .background(cardGradient)
                        .padding(if (selectedFormat == QuoteCardFormat.STORY) 16.dp else 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(AppShape.M)
                            .background(cardColor)
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "MONTHLY READING JOURNAL",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                yearMonth.format(monthFormatter),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = textColor
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = accentColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "Reader: $userName",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            // 2x2 Stats Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MiniStatBox("🏆 ${finishedThisMonth.size}", "Books", accentColor, textColor, Modifier.weight(1f))
                                MiniStatBox("📄 $totalPages", "Pages", accentColor, textColor, Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MiniStatBox("⏱️ ${totalHours}h", "Time", accentColor, textColor, Modifier.weight(1f))
                                MiniStatBox("📅 $activeDays", "Days Active", accentColor, textColor, Modifier.weight(1f))
                            }

                            Spacer(Modifier.height(8.dp))

                            // Books Finished
                            Text(
                                if (finishedThisMonth.isNotEmpty()) "Finished Books (${finishedThisMonth.size})" else "Reading Highlights",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(Modifier.height(4.dp))

                            val displayBooks = if (finishedThisMonth.isNotEmpty()) finishedThisMonth else {
                                val readBookIds = monthSessions.map { it.bookId }.distinct()
                                books.filter { it.id in readBookIds }
                            }

                            val maxPreview = if (selectedFormat == QuoteCardFormat.STORY) 5 else 3
                            displayBooks.take(maxPreview).forEach { b ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("✦ ", color = accentColor, fontSize = 11.sp)
                                    Text(
                                        b.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = textColor,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            if (displayBooks.size > maxPreview) {
                                Text(
                                    "+ ${displayBooks.size - maxPreview} more books",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                            }

                            Spacer(Modifier.weight(1f))
                            Text(
                                "Nocturnal Reader",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = textColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Action Buttons
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        MonthlyShareHelper.shareMonthlyReport(
                            context = context,
                            yearMonth = yearMonth,
                            books = books,
                            sessions = sessions,
                            userName = userName,
                            theme = selectedTheme,
                            format = selectedFormat
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = AppShape.M
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share Monthly Card", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MiniStatBox(
    value: String,
    label: String,
    accentColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = AppShape.XS,
        color = Color.Black.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accentColor)
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = textColor.copy(alpha = 0.7f))
        }
    }
}
