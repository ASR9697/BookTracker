import os
import re

file_path = r"c:\Users\abhij\Documents\IT Data\Book Tracker\app\src\main\java\com\example\booktracker\app\ui\BookDetailScreen.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Update Imports
imports_to_add = """
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.blur
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
"""
content = content.replace("import kotlin.math.roundToInt\n", "import kotlin.math.roundToInt\n" + imports_to_add)

# 2. Main screen parallax and glassmorphism updates
# Find the Box that contains the main content
box_pattern = r'Box\(modifier = Modifier\.fillMaxSize\(\)\.background\(MaterialTheme\.colorScheme\.background\)\) \{'
content = re.sub(box_pattern, 'Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest)) {', content)

# Update BookCover height and LazyColumn Spacer height
content = content.replace('.height(450.dp)', '.height(500.dp)')
content = content.replace('Spacer(modifier = Modifier.height(300.dp))', 'Spacer(modifier = Modifier.height(380.dp))')
content = content.replace('RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)', 'RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)')
content = content.replace('.padding(16.dp)', '.padding(horizontal = 24.dp, vertical = 32.dp)')

# Update DetailHeader
detail_header_pattern = r'@Composable\s+private fun DetailHeader\(.*?\)\s*\{.*?(?=@Composable\s+private fun StatusPill)'
new_detail_header = """@Composable
private fun DetailHeader(
    book: Book, 
    onStartReading: () -> Unit,
    onPauseReading: () -> Unit,
    onReadAgain: () -> Unit
) {
    Column {
        Text(
            book.title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            lineHeight = androidx.compose.ui.unit.TextUnit(48f, androidx.compose.ui.unit.TextUnitType.Sp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            book.authors.joinToString(", "),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val facts = buildList {
                if (book.totalUnits > 0) add("${book.totalUnits} pages")
                if (book.publishedDate.isNotBlank()) add(book.publishedDate.take(4))
            }
            if (facts.isNotEmpty()) {
                Text(
                    facts.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(book.status)
        }
        
        Spacer(Modifier.height(24.dp))
        
        val buttonText = when (book.status) {
            BookStatus.READING.name -> "Pause Reading"
            BookStatus.PAUSED.name -> "Resume Reading"
            BookStatus.FINISHED.name, BookStatus.DNF.name -> "Read Again"
            else -> "Start Reading"
        }
        
        val icon = when (book.status) {
            BookStatus.READING.name -> Icons.Filled.Pause
            else -> Icons.Filled.MenuBook
        }
        
        val onClick = when (book.status) {
            BookStatus.READING.name -> onPauseReading
            BookStatus.FINISHED.name, BookStatus.DNF.name -> onReadAgain
            else -> onStartReading
        }
        
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(buttonText, style = MaterialTheme.typography.titleMedium)
        }
    }
}
"""
content = re.sub(detail_header_pattern, new_detail_header, content, flags=re.DOTALL)

# Update StatusPill
status_pill_pattern = r'@Composable\s+private fun StatusPill\(.*?\}'
new_status_pill = """@Composable
private fun StatusPill(status: String) {
    val label = when (status) {
        BookStatus.BACKLOG.name -> "Backlog"
        BookStatus.SHORTLIST.name -> "Shortlist"
        BookStatus.UP_NEXT.name -> "Up Next"
        BookStatus.READING.name -> "Reading"
        BookStatus.PAUSED.name -> "Paused"
        BookStatus.FINISHED.name -> "Finished"
        BookStatus.DNF.name -> "Did not finish"
        else -> status
    }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.height(28.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}"""
content = re.sub(status_pill_pattern, new_status_pill, content, flags=re.DOTALL)


# Update BookStatsCard -> "Your reading"
book_stats_card_pattern = r'@Composable\s+private fun BookStatsCard\(.*?\}'
new_book_stats_card = """@Composable
private fun BookStatsCard(book: Book, completed: List<Session>) {
    val totalMinutes = AnalyticsEngine.timedSessions(completed).sumOf { it.endTime - it.startTime } / 60_000L
    val velocity = AnalyticsEngine.pagesPerHour(completed)
    val pagesFromSessions = completed.sumOf { it.unitsRead }
    val timeLabel = if (totalMinutes < 1) "<1m" else AnalyticsEngine.formatMinutes(totalMinutes)
    val speedLabel = velocity?.let { "${it.roundToInt()}" } ?: "—"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 20.dp)) {
                Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text("YOUR READING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCell(Icons.Filled.Schedule, timeLabel, "Time", Modifier.weight(1f))
                StatCell(Icons.Filled.AutoStories, pagesFromSessions.toString(), "Pages", Modifier.weight(1f))
                StatCell(Icons.Filled.Speed, speedLabel, "Speed", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCell(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}"""
content = re.sub(book_stats_card_pattern, new_book_stats_card, content, flags=re.DOTALL)
content = re.sub(r'@Composable\s+private fun StatCell\(.*?\}', '', content, flags=re.DOTALL) # Remove old StatCell

# Format Card
format_card_pattern = r'@Composable\s+private fun FormatCard\(.*?\}'
new_format_card = """@Composable
private fun FormatCard(book: Book, onSetFormat: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Tracking format", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Track this book in ${FormatAdaptabilityLayer.getDisplayUnit(book.format).lowercase()}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .height(48.dp)
            ) {
                FormatAdaptabilityLayer.FORMATS.forEachIndexed { index, format ->
                    val isSelected = book.format.equals(format, ignoreCase = true)
                    val bg = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(bg)
                            .androidx.compose.foundation.clickable { onSetFormat(format) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                FormatAdaptabilityLayer.getDisplayUnit(format),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (index < FormatAdaptabilityLayer.FORMATS.size - 1) {
                        Box(modifier = Modifier.width(1.dp).fillMaxSize().background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                    }
                }
            }
        }
    }
}"""
content = re.sub(format_card_pattern, new_format_card, content, flags=re.DOTALL)


# DetailCard update
detail_card_pattern = r'@Composable\s+private fun DetailCard\(.*?\}'
new_detail_card = """@Composable
private fun DetailCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}"""
content = re.sub(detail_card_pattern, new_detail_card, content, flags=re.DOTALL)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
