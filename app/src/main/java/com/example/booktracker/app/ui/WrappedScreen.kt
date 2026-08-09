package com.example.booktracker.app.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Session
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrappedScreen(
    books: List<Book>,
    sessions: List<Session>,
    streak: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isYearly by remember { mutableStateOf(true) }
    
    val graphicsLayer = rememberGraphicsLayer()
    
    val zone = remember { ZoneId.systemDefault() }
    val now = Instant.now()
    val periodStart = if (isYearly) {
        now.atZone(zone).withDayOfYear(1).toInstant().toEpochMilli()
    } else {
        now.atZone(zone).withDayOfMonth(1).toInstant().toEpochMilli()
    }

    val periodSessions = sessions.filter { it.startTime >= periodStart }
    
    val finishedBooks = books.filter { 
        it.status == BookStatus.FINISHED.name && it.lastUpdated >= periodStart
    }
    
    val totalPages = periodSessions.sumOf { it.pagesRead }
    val totalMinutes = periodSessions.sumOf { (it.endTime - it.startTime) } / 60_000L
    val totalHours = totalMinutes / 60
    
    val periodBooks = books.filter { b -> 
        b.status == BookStatus.FINISHED.name || periodSessions.any { it.bookId == b.id }
    }
    val allGenres = periodBooks.flatMap { it.genres }
    val topGenre = allGenres.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "None"
    
    val allEnvs = periodSessions.mapNotNull { it.environmentTag.takeIf { t -> t.isNotBlank() } }
    val topEnv = allEnvs.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "None"
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Wrapped") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        val cacheDir = context.cacheDir
                        val imageFile = File(cacheDir, "wrapped.png")
                        FileOutputStream(imageFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        
                        val uri = FileProvider.getUriForFile(
                            context,
                            "com.example.booktracker.fileprovider",
                            imageFile
                        )
                        
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        
                        context.startActivity(Intent.createChooser(intent, "Share your wrapped"))
                    }
                },
                icon = { Icon(Icons.Filled.Share, contentDescription = null) },
                text = { Text("Share") }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val options = listOf("This Month", "This Year")
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.padding(16.dp)
            ) {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        onClick = { isYearly = index == 1 },
                        selected = (index == 1) == isYearly
                    ) {
                        Text(label)
                    }
                }
            }
            
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (isYearly) "My Reading Year" else "My Reading Month",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    WrappedStat("Books Finished", finishedBooks.size.toString())
                    WrappedStat("Pages Read", totalPages.toString())
                    WrappedStat("Hours Spent", totalHours.toString())
                    WrappedStat("Top Genre", topGenre)
                    WrappedStat("Top Vibe", topEnv)
                    WrappedStat("Longest Streak", "$streak days")
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Generated by Nocturnal Reader",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun WrappedStat(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
