package com.example.booktracker.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSetupScreen(
    viewModel: BookTrackerViewModel,
    books: List<Book>,
    onStartTimer: (Book, Int) -> Unit,
    onManualSessionSaved: () -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedBook by remember { mutableStateOf<Book?>(books.firstOrNull { it.status == BookStatus.READING.name }) }
    var expanded by remember { mutableStateOf(false) }

    // Pre-session setup
    var startPageInput by remember(selectedBook) { mutableStateOf(selectedBook?.currentPage?.toString() ?: "0") }
    
    // Modes: "10 mins", "30 mins", "Custom", "Indefinitely"
    val modes = listOf("10 mins", "30 mins", "Custom", "Indefinitely")
    var selectedMode by remember { mutableStateOf("30 mins") }
    var customMinutesInput by remember { mutableStateOf("45") }

    // Manual session entry
    var isManualEntryMode by remember { mutableStateOf(false) }
    var manualEndPageInput by remember { mutableStateOf("") }
    var manualDurationInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 88.dp) // Bottom nav padding
    ) {
        Text("Timer & Sessions", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        
        // Book Selection
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedBook?.title ?: "Select a book",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                    .fillMaxWidth(),
                label = { Text("Book") }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                books.filter { it.status != BookStatus.FINISHED.name && it.status != BookStatus.DNF.name }.forEach { book ->
                    DropdownMenuItem(
                        text = { Text(book.title) },
                        onClick = {
                            selectedBook = book
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        val isStartPageError = selectedBook?.totalPages?.let { total -> total > 0 && (startPageInput.toIntOrNull() ?: 0) > total } == true

        OutlinedTextField(
            value = startPageInput,
            onValueChange = { startPageInput = it.filter { char -> char.isDigit() } },
            label = { Text("Starting Page") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = isStartPageError,
            supportingText = if (isStartPageError) { { Text("Exceeds total pages (${selectedBook?.totalPages})") } } else null
        )

        Spacer(Modifier.height(24.dp))
        
        // Mode switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            SegmentedButtonRow(
                isManualEntryMode = isManualEntryMode,
                onModeChange = { isManualEntryMode = it }
            )
        }
        
        Spacer(Modifier.height(24.dp))

        if (isManualEntryMode) {
            // Manual Entry UI
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Log Past Session", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    val isManualEndPageError = selectedBook?.totalPages?.let { total -> total > 0 && (manualEndPageInput.toIntOrNull() ?: 0) > total } == true
                    
                    OutlinedTextField(
                        value = manualEndPageInput,
                        onValueChange = { manualEndPageInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Ending Page") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        isError = isManualEndPageError,
                        supportingText = if (isManualEndPageError) { { Text("Exceeds total pages (${selectedBook?.totalPages})") } } else null
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualDurationInput,
                        onValueChange = { manualDurationInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Duration (minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val book = selectedBook
                            val start = startPageInput.toIntOrNull()
                            val end = manualEndPageInput.toIntOrNull()
                            val duration = manualDurationInput.toIntOrNull()
                            if (book != null && start != null && end != null && duration != null) {
                                viewModel.logManualSession(book.id, start, end, duration)
                                onManualSessionSaved()
                                // Reset fields
                                manualEndPageInput = ""
                                manualDurationInput = ""
                                startPageInput = end.toString()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedBook != null && startPageInput.isNotEmpty() && !isStartPageError && manualEndPageInput.isNotEmpty() && !isManualEndPageError && manualDurationInput.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Session")
                    }
                }
            }
        } else {
            // Timer Setup UI
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Timer Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(16.dp))
                    
                    // Modes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        modes.chunked(2).forEach { rowModes ->
                            Column(modifier = Modifier.weight(1f)) {
                                rowModes.forEach { mode ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedMode = mode }
                                            .padding(vertical = 8.dp)
                                    ) {
                                        RadioButton(
                                            selected = selectedMode == mode,
                                            onClick = { selectedMode = mode }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(mode)
                                    }
                                }
                            }
                        }
                    }
                    
                    if (selectedMode == "Custom") {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customMinutesInput,
                            onValueChange = { customMinutesInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Minutes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            val book = selectedBook ?: return@Button
                            val startPage = startPageInput.toIntOrNull() ?: book.currentPage
                            
                            when (selectedMode) {
                                "10 mins" -> {
                                    viewModel.setTimerMode(TimerMode.COUNTDOWN)
                                    viewModel.setCustomWorkMinutes(10)
                                }
                                "30 mins" -> {
                                    viewModel.setTimerMode(TimerMode.COUNTDOWN)
                                    viewModel.setCustomWorkMinutes(30)
                                }
                                "Custom" -> {
                                    viewModel.setTimerMode(TimerMode.COUNTDOWN)
                                    val mins = customMinutesInput.toIntOrNull() ?: 25
                                    viewModel.setCustomWorkMinutes(mins)
                                }
                                "Indefinitely" -> {
                                    viewModel.setTimerMode(TimerMode.STOPWATCH)
                                }
                            }
                            viewModel.setTimerBook(book.id)
                            viewModel.startSession(book, startPage)
                            viewModel.setTimerRunning(true)
                            onStartTimer(book, startPage)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedBook != null && startPageInput.isNotEmpty() && !isStartPageError
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start Timer")
                    }
                }
            }
        }
    }
}

@Composable
fun SegmentedButtonRow(
    isManualEntryMode: Boolean,
    onModeChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        val selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
        val unselectedContainerColor = MaterialTheme.colorScheme.surface
        val selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        val unselectedContentColor = MaterialTheme.colorScheme.onSurface

        Surface(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
            color = if (!isManualEntryMode) selectedContainerColor else unselectedContainerColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            onClick = { onModeChange(false) }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("Live Timer", color = if (!isManualEntryMode) selectedContentColor else unselectedContentColor)
            }
        }
        
        Surface(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
            color = if (isManualEntryMode) selectedContainerColor else unselectedContainerColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            onClick = { onModeChange(true) }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("Manual Log", color = if (isManualEntryMode) selectedContentColor else unselectedContentColor)
            }
        }
    }
}
