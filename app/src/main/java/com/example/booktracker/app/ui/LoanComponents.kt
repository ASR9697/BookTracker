package com.example.booktracker.app.ui

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.booktracker.app.analytics.LoanEngine
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.LoanRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Reusable visual badge displaying loan status and due date countdown.
 */
@Composable
fun LoanBadge(
    status: LoanEngine.LoanStatusInfo,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor, icon) = when (status.urgency) {
        LoanEngine.LoanUrgency.OVERDUE -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Filled.Warning
        )
        LoanEngine.LoanUrgency.DUE_SOON -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Filled.Schedule
        )
        LoanEngine.LoanUrgency.NORMAL -> {
            if (status.isLentOut) {
                Triple(
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                    Icons.Filled.Person
                )
            } else {
                Triple(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    Icons.AutoMirrored.Filled.MenuBook
                )
            }
        }
        LoanEngine.LoanUrgency.RETURNED -> Triple(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Filled.CheckCircle
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
            )
            Text(
                status.badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * Alert banner highlighting urgent library loan deadlines on Home / Dashboard.
 */
@Composable
fun UrgentLoanBanner(
    urgentLoans: List<Pair<Book, LoanEngine.LoanStatusInfo>>,
    onOpenBook: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    if (urgentLoans.isEmpty()) return

    val firstUrgent = urgentLoans.first()
    val book = firstUrgent.first
    val status = firstUrgent.second
    val isOverdue = status.urgency == LoanEngine.LoanUrgency.OVERDUE

    val containerColor = if (isOverdue)
        MaterialTheme.colorScheme.errorContainer
    else
        MaterialTheme.colorScheme.tertiaryContainer

    val contentColor = if (isOverdue)
        MaterialTheme.colorScheme.onErrorContainer
    else
        MaterialTheme.colorScheme.onTertiaryContainer

    val icon = if (isOverdue) Icons.Filled.ErrorOutline else Icons.Filled.Schedule

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenBook(book) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isOverdue) "Library Book Overdue!" else "Library Return Approaching",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "\"${book.title}\" · ${status.detailText}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (urgentLoans.size > 1) {
                Surface(
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.2f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        "+${urgentLoans.size - 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Loan / Lending Card for BookDetailScreen overview tab.
 */
@Composable
fun LoanDetailCard(
    book: Book,
    activeLoan: LoanRecord?,
    onRecordLoan: (LoanRecord) -> Unit,
    onMarkReturned: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }

    if (showDialog) {
        LoanRecordDialog(
            existingLoan = activeLoan,
            onDismiss = { showDialog = false },
            onSave = { record ->
                onRecordLoan(record)
                showDialog = false
            }
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (activeLoan?.isLentOut == true) Icons.Filled.Person else Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (activeLoan?.isLentOut == true) "Lending Record" else "Library & Lending",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (activeLoan != null) {
                    val status = remember(activeLoan, today) { LoanEngine.getLoanStatus(activeLoan, today, zone) }
                    LoanBadge(status)
                }
            }

            Spacer(Modifier.height(14.dp))

            if (activeLoan != null) {
                val status = remember(activeLoan, today) { LoanEngine.getLoanStatus(activeLoan, today, zone) }
                val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

                val loanDateStr = remember(activeLoan.loanDate) {
                    if (activeLoan.loanDate > 0)
                        Instant.ofEpochMilli(activeLoan.loanDate).atZone(zone).toLocalDate().format(dateFormatter)
                    else "Recorded"
                }

                val dueDateStr = remember(activeLoan.dueDate) {
                    if (activeLoan.dueDate > 0)
                        Instant.ofEpochMilli(activeLoan.dueDate).atZone(zone).toLocalDate().format(dateFormatter)
                    else "None"
                }

                Text(
                    text = status.detailText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Loan Date: $loanDateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Due: $dueDateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (activeLoan.memo.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Note: ${activeLoan.memo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onMarkReturned,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mark Returned")
                    }
                    OutlinedButton(
                        onClick = { showDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit")
                    }
                }
            } else {
                Text(
                    "Track library return deadlines or keep track of physical copies lent out to friends and family.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { showDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Borrow from Library or Lend to Friend")
                }
            }

            // Historical loan logs if any
            val pastLoans = remember(book.loanRecord) { book.loanRecord.filter { it.isReturned } }
            if (pastLoans.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                var showHistory by remember { mutableStateOf(false) }
                TextButton(
                    onClick = { showHistory = !showHistory },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (showHistory) "Hide Loan History" else "View Past Loan History (${pastLoans.size})",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                AnimatedVisibility(visible = showHistory) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pastLoans.reversed().forEach { past ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            if (past.isLentOut) "Lent to ${past.lender}" else "Borrowed from ${past.lender}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            past.memo.ifBlank { "Returned successfully" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Dialog to create or edit a LoanRecord.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LoanRecordDialog(
    existingLoan: LoanRecord? = null,
    onDismiss: () -> Unit,
    onSave: (LoanRecord) -> Unit
) {
    var isLentOut by remember { mutableStateOf(existingLoan?.isLentOut ?: false) }
    var lenderName by remember { mutableStateOf(existingLoan?.lender ?: "") }
    var memo by remember { mutableStateOf(existingLoan?.memo ?: "") }
    var daysSelected by remember {
        mutableStateOf(
            if (existingLoan == null) 14
            else if (existingLoan.dueDate <= 0L) 0
            else {
                val diff = ((existingLoan.dueDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L)).toInt()
                diff.coerceAtLeast(1)
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (existingLoan != null) "Edit loan" else "Borrowed or lent",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isLentOut,
                        onClick = { isLentOut = false },
                        label = { Text("I borrowed it", maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isLentOut,
                        onClick = { isLentOut = true },
                        label = { Text("I lent it", maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = lenderName,
                    onValueChange = { lenderName = it },
                    label = { Text(if (isLentOut) "Who has it?" else "From (library or person)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Due date duration presets
                Column {
                    Text(
                        "Due back in",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(7 to "1 week", 14 to "2 weeks", 21 to "3 weeks", 30 to "30 days", 0 to "No due date").forEach { (days, label) ->
                            FilterChip(
                                selected = daysSelected == days,
                                onClick = { daysSelected = days },
                                label = { Text(label) },
                                shape = CircleShape
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            val dueDate = if (daysSelected > 0)
                                now + (daysSelected.toLong() * 24L * 60L * 60L * 1000L)
                            else 0L

                            val record = LoanRecord(
                                loanDate = existingLoan?.loanDate ?: now,
                                dueDate = dueDate,
                                lender = lenderName.trim().ifBlank { if (isLentOut) "Friend" else "Library" },
                                memo = memo.trim(),
                                returnDate = null,
                                isLentOut = isLentOut
                            )
                            onSave(record)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
