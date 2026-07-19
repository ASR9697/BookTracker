package com.example.booktracker.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.booktracker.app.journal.NoiseType
import com.example.booktracker.shared.models.Book
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

enum class TimerPhase { WORK, BREAK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(
    book: Book,
    viewModel: BookTrackerViewModel,
    onBack: () -> Unit,
    onStartSession: (Book, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val phase by viewModel.timerPhase.collectAsState()
    val isRunning by viewModel.timerIsRunning.collectAsState()
    val timeLeftSeconds by viewModel.timeLeftSeconds.collectAsState()
    val workMinutes by viewModel.workMinutes.collectAsState()
    val breakMinutes by viewModel.breakMinutes.collectAsState()
    val noiseType by viewModel.noiseType.collectAsState()
    val volume by viewModel.timerVolume.collectAsState()
    val openSession by viewModel.openSession.collectAsState()
    var showEndSession by remember { mutableStateOf(false) }
    var showStartDialog by remember { mutableStateOf(false) }

    // Ensure the ViewModel knows this is the active timer book
    LaunchedEffect(book.id) {
        viewModel.setTimerBook(book.id)
    }

    LaunchedEffect(openSession) {
        if (openSession == null && !isRunning) {
            showStartDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus Mode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    if (phase == TimerPhase.WORK) "Deep Focus" else "Take a Break",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (phase == TimerPhase.WORK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                )
            }
            
            val totalSeconds = if (phase == TimerPhase.WORK) workMinutes * 60 else breakMinutes * 60
            val progress = timeLeftSeconds.toFloat() / totalSeconds.coerceAtLeast(1)
            val animatedProgress by animateFloatAsState(progress, label = "progress")
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(320.dp).padding(16.dp)) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = if (phase == TimerPhase.WORK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    strokeWidth = 16.dp,
                    strokeCap = StrokeCap.Round
                )
                
                val m = timeLeftSeconds / 60
                val s = timeLeftSeconds % 60
                Text(
                    text = String.format("%02d:%02d", m, s),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRunning) {
                    FilledIconButton(
                        onClick = { viewModel.setTimerRunning(false) },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(Icons.Filled.Pause, contentDescription = "Pause", modifier = Modifier.size(32.dp))
                    }
                } else {
                    FilledIconButton(
                        onClick = { 
                            if (openSession == null) {
                                showStartDialog = true
                            } else {
                                viewModel.setTimerRunning(true) 
                            }
                        },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", modifier = Modifier.size(32.dp))
                    }
                }
                
                OutlinedIconButton(
                    onClick = {
                        viewModel.setTimerRunning(false)
                        viewModel.setTimerPhase(TimerPhase.WORK)
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = "Reset", modifier = Modifier.size(32.dp))
                }
                
                FilledTonalIconButton(
                    onClick = {
                        viewModel.setTimerRunning(false)
                        showEndSession = true
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = "Save for Later", modifier = Modifier.size(32.dp))
                }
            }
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Soundscape", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    
                    val options = NoiseType.entries.toTypedArray()
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        options.forEachIndexed { index, type ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                onClick = { viewModel.setNoiseType(type) },
                                selected = noiseType == type
                            ) {
                                Text(type.name.lowercase().replaceFirstChar { it.uppercase() })
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    Text("Volume", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = volume,
                        onValueChange = { viewModel.setTimerVolume(it) },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showEndSession) {
        EndSessionDialog(
            startPage = openSession?.startUnit ?: book.currentUnit,
            totalUnits = book.totalUnits,
            durationMillis = openSession?.let { System.currentTimeMillis() - it.startTime } ?: 0L,
            onDismiss = { showEndSession = false },
            onConfirm = { tag, endPage ->
                viewModel.endSession(book, endPage, tag)
                showEndSession = false
                onBack() // go back after saving
            }
        )
    }

    if (showStartDialog) {
        StartSessionDialog(
            currentUnit = book.currentUnit,
            totalUnits = book.totalUnits,
            unitName = if (book.totalUnits > 0) "Page" else "Unit",
            onDismiss = { showStartDialog = false },
            onConfirm = { startPage ->
                onStartSession(book, startPage)
                viewModel.setTimerRunning(true)
                showStartDialog = false
            }
        )
    }
}
