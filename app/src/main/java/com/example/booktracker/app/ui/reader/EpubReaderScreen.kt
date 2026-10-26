package com.example.booktracker.app.ui.reader

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.booktracker.data.epub.EpubBook
import com.example.booktracker.data.epub.EpubParser
import com.example.booktracker.data.epub.HtmlToComposeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Splits at blank lines, keeping each paragraph's styling. */
private fun AnnotatedString.paragraphs(): List<AnnotatedString> {
    val result = mutableListOf<AnnotatedString>()
    var start = 0
    while (start < length) {
        val breakAt = text.indexOf("\n\n", start).let { if (it == -1) length else it }
        val piece = subSequence(start, breakAt)
        if (piece.isNotBlank()) result += piece
        start = breakAt + 2
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpubReaderScreen(
    uriString: String,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    var book by remember { mutableStateOf<EpubBook?>(null) }
    var chapters by remember { mutableStateOf<List<AnnotatedString>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(uriString) {
        try {
            withContext(Dispatchers.IO) {
                // Navigation already decoded the argument; decoding again would
                // corrupt content URIs that legitimately contain %-escapes.
                val uri = Uri.parse(uriString)
                val parser = EpubParser()
                val parsedBook = parser.parse(context, uri)
                book = parsedBook

                // Paragraph-sized items keep the list smooth; a whole chapter in
                // one Text is an enormous layout for a single lazy item.
                chapters = parser.readSpineHtml(context, uri, parsedBook)
                    .map { HtmlToComposeParser.parse(it) }
                    .filter { it.isNotBlank() }
                    .flatMap { it.paragraphs() }
            }
        } catch (e: Exception) {
            error = "This file couldn't be opened as an EPUB."
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(book?.title ?: "Loading...", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when {
                loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null || chapters.isEmpty() -> {
                    Text(
                        text = error ?: "This book has no readable text.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp)
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(chapters) { chapterText ->
                            Text(
                                text = chapterText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
