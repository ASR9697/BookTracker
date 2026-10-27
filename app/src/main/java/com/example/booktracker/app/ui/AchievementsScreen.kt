package com.example.booktracker.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import com.example.booktracker.app.ui.animations.animateCountUp
import com.example.booktracker.app.ui.animations.rememberEntranceState
import com.example.booktracker.app.ui.animations.rememberHaptics
import com.example.booktracker.app.ui.animations.staggeredEntrance
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.analytics.BadgeEngine
import com.example.booktracker.app.analytics.BadgeStatus
import com.example.booktracker.app.ui.animations.bounceClick
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class BadgeFilter(val label: String) { ALL("All"), EARNED("Earned"), LOCKED("Locked") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    statuses: List<BadgeStatus>,
    onBack: () -> Unit,
    monthlyChallenge: BadgeStatus? = null,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(BadgeFilter.ALL) }
    var selected by remember { mutableStateOf<BadgeStatus?>(null) }
    selected?.let { BadgeDetailDialog(status = it, onDismiss = { selected = null }) }

    // Bronze first, so the grid reads as a ladder from easy to hard.
    val ordered = remember(statuses) { statuses.sortedBy { BadgeEngine.TIERS.indexOf(it.badge.tier) } }
    val shown = when (filter) {
        BadgeFilter.ALL -> ordered
        BadgeFilter.EARNED -> ordered.filter { it.unlocked }
        BadgeFilter.LOCKED -> ordered.filter { !it.unlocked }
    }
    val nextUp = remember(statuses) { BadgeEngine.nextUp(statuses) }
    val haptics = rememberHaptics()
    val entrance = rememberEntranceState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Achievements") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { AchievementSummary(statuses) }

            if (monthlyChallenge != null) item(span = { GridItemSpan(maxLineSpan) }) {
                MonthlyChallengeCard(monthlyChallenge)
            }

            if (nextUp != null) item(span = { GridItemSpan(maxLineSpan) }) {
                NextUpCard(nextUp, onClick = { selected = nextUp })
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BadgeFilter.entries.forEach { option ->
                        val count = when (option) {
                            BadgeFilter.ALL -> statuses.size
                            BadgeFilter.EARNED -> statuses.count { it.unlocked }
                            BadgeFilter.LOCKED -> statuses.count { !it.unlocked }
                        }
                        FilterChip(
                            selected = filter == option,
                            onClick = {
                                if (filter != option) haptics.tick()
                                filter = option
                            },
                            label = { Text("${option.label} · $count") }
                        )
                    }
                }
            }

            itemsIndexed(shown, key = { _, status -> status.badge.id }) { index, status ->
                Box(Modifier.animateItem().staggeredEntrance(entrance, index + 3)) {
                    BadgeCard(status, onClick = { selected = status })
                }
            }

            if (shown.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    if (filter == BadgeFilter.EARNED) "Nothing earned yet. Your first session unlocks one."
                    else "Every achievement unlocked. Well read!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                )
            }
        }
    }
}

@Composable
private fun AchievementSummary(statuses: List<BadgeStatus>) {
    val unlocked = statuses.count { it.unlocked }
    val shownUnlocked by animateCountUp(unlocked)
    val fraction = if (statuses.isEmpty()) 0f else shownUnlocked.toFloat() / statuses.size
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
                CircularProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    gapSize = 0.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$shownUnlocked", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "of ${statuses.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                BadgeEngine.TIERS.forEach { tier ->
                    val inTier = statuses.filter { it.badge.tier == tier }
                    if (inTier.isEmpty()) return@forEach
                    val earned = inTier.count { it.unlocked }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(getBadgeTierColor(tier)))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            tier.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "$earned / ${inTier.size}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (earned == inTier.size) getBadgeTierColor(tier)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** This month's rotating challenge; it resets on the 1st, unlike the permanent badges. */
@Composable
private fun MonthlyChallengeCard(status: BadgeStatus) {
    val color = getBadgeTierColor(status.badge.tier)
    val today = LocalDate.now()
    val daysLeft = today.lengthOfMonth() - today.dayOfMonth
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(status.badge.icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${today.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()).uppercase()} CHALLENGE · " +
                        when {
                            status.unlocked -> "COMPLETE"
                            daysLeft == 0 -> "LAST DAY"
                            daysLeft == 1 -> "1 DAY LEFT"
                            else -> "$daysLeft DAYS LEFT"
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Text(status.badge.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    status.badge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                BadgeProgressBar(status)
            }
        }
    }
}

@Composable
private fun NextUpCard(status: BadgeStatus, onClick: () -> Unit) {
    val tierColor = getBadgeTierColor(status.badge.tier)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = tierColor.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, tierColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().bounceClick(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            BadgeMedallion(status, size = 56.dp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "CLOSEST TO UNLOCKING",
                    style = MaterialTheme.typography.labelSmall,
                    color = tierColor,
                    fontWeight = FontWeight.Bold
                )
                Text(status.badge.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    status.badge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                BadgeProgressBar(status)
            }
        }
    }
}

@Composable
private fun BadgeCard(status: BadgeStatus, onClick: () -> Unit) {
    val badge = status.badge
    val tierColor = getBadgeTierColor(badge.tier)
    Card(
        modifier = Modifier.fillMaxWidth().bounceClick(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (status.unlocked) tierColor.copy(alpha = 0.10f)
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = if (status.unlocked) BorderStroke(1.5.dp, tierColor.copy(alpha = 0.45f)) else null,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TierTag(badge.tier, status.unlocked)
            Spacer(Modifier.height(10.dp))
            BadgeMedallion(status, size = 52.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                badge.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            // Two fixed lines keep every card in a grid row the same height.
            Text(
                badge.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            if (status.unlocked) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(24.dp)) {
                    Icon(Icons.Filled.CheckCircle, null, tint = tierColor, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        status.earnedAt?.let { "Earned ${formatEarned(it, withYear = false)}" } ?: "Earned",
                        style = MaterialTheme.typography.labelSmall,
                        color = tierColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Column(modifier = Modifier.height(24.dp), verticalArrangement = Arrangement.Center) {
                    BadgeProgressBar(status, compact = true)
                }
            }
        }
    }
}

@Composable
private fun TierTag(tier: String, unlocked: Boolean) {
    val tierColor = getBadgeTierColor(tier)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (unlocked) tierColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(
            tier,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (unlocked) tierColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/** The badge's own icon; locked badges show it dimmed with a small lock, so the goal is still recognisable. */
@Composable
internal fun BadgeMedallion(status: BadgeStatus, size: Dp) {
    val tierColor = getBadgeTierColor(status.badge.tier)
    Box(modifier = Modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(if (status.unlocked) tierColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(
                    1.5.dp,
                    if (status.unlocked) tierColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                status.badge.icon,
                contentDescription = null,
                tint = if (status.unlocked) tierColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(size / 2)
            )
        }
        if (!status.unlocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size / 3)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(size / 5)
                )
            }
        }
    }
}

@Composable
internal fun BadgeProgressBar(status: BadgeStatus, compact: Boolean = false) {
    val tierColor = getBadgeTierColor(status.badge.tier)
    Column(horizontalAlignment = if (compact) Alignment.CenterHorizontally else Alignment.Start) {
        LinearProgressIndicator(
            progress = { status.fraction },
            modifier = Modifier.fillMaxWidth().height(if (compact) 4.dp else 6.dp).clip(CircleShape),
            color = tierColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
        Spacer(Modifier.height(4.dp))
        Text(
            status.progressLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** "12 Sep" or "12 Sep 2026". */
private fun formatEarned(millis: Long, withYear: Boolean): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern(if (withYear) "d MMM yyyy" else "d MMM"))

internal fun getBadgeTierColor(tier: String): Color {
    return when (tier.uppercase()) {
        BadgeEngine.MONTHLY_TIER -> Color(0xFF4DD0B5)
        "PLATINUM" -> Color(0xFF00E5FF)
        "GOLD" -> Color(0xFFFFB300)
        "SILVER" -> Color(0xFFB0BEC5)
        else -> Color(0xFFCD7F32) // BRONZE
    }
}

@Composable
internal fun BadgeDetailDialog(
    status: BadgeStatus,
    onDismiss: () -> Unit
) {
    val badge = status.badge
    val tierColor = getBadgeTierColor(badge.tier)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = tierColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, tierColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        "✦ ${badge.tier} ACHIEVEMENT ✦",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tierColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))
                BadgeMedallion(status, size = 80.dp)
                Spacer(Modifier.height(16.dp))

                Text(
                    badge.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    badge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))

                if (status.unlocked) {
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                status.earnedAt?.let { "Earned ${formatEarned(it, withYear = true)}" } ?: "Unlocked & Earned!",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                } else {
                    Box(Modifier.fillMaxWidth(0.8f)) { BadgeProgressBar(status, compact = true) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
