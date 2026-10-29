package com.example.booktracker.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.booktracker.app.R
import com.example.booktracker.app.journal.JournalingEngine

/**
 * Shows the bundled privacy policy (res/raw/privacy_policy.md) so it's readable
 * offline. The same file is what gets hosted for the Play Console URL.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lines = remember {
        val contact = context.getString(R.string.support_email)
            .ifBlank { "the developer contact address on the app's Google Play listing" }
        context.resources.openRawResource(R.raw.privacy_policy)
            .bufferedReader()
            .use { it.readLines() }
            .map {
                it.replace("{{APP_NAME}}", context.getString(R.string.app_name))
                    .replace("{{CONTACT_EMAIL}}", contact)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp + windowCentredPadding(), vertical = 16.dp)
        ) {
            items(lines) { line ->
                when {
                    line.startsWith("# ") -> Column {
                        Text(line.removePrefix("# "), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                    }
                    line.startsWith("## ") -> Column {
                        Spacer(Modifier.height(16.dp))
                        Text(line.removePrefix("## "), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                    }
                    line.startsWith("- ") -> Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("•", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(8.dp))
                        Text(JournalingEngine.render(line.removePrefix("- ")), style = MaterialTheme.typography.bodyMedium)
                    }
                    line.isBlank() -> Spacer(Modifier.height(8.dp))
                    else -> Text(
                        JournalingEngine.render(line),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
