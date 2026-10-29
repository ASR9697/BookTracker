package com.example.booktracker.app.ui

import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Settings card for Google Drive sync: connect (which runs the first sync),
 * sync now, and stop. Google's consent screen is launched from here because
 * it needs an activity result.
 */
@Composable
fun DriveSyncCard(
    enabled: Boolean,
    lastSync: Long,
    state: DriveSyncState,
    onSync: () -> Unit,
    onConsentShown: () -> Unit,
    onConsentResult: (android.content.Intent?) -> Unit,
    onDisconnect: () -> Unit,
    onDismissStatus: () -> Unit = {}
) {
    DisposableEffect(Unit) { onDispose(onDismissStatus) }
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        onConsentResult(result.data)
    }
    LaunchedEffect(state) {
        if (state is DriveSyncState.NeedsConsent) {
            onConsentShown()
            consent.launch(IntentSenderRequest.Builder(state.sender).build())
        }
    }
    val syncing = state == DriveSyncState.Syncing || state is DriveSyncState.NeedsConsent

    SettingsCard(
        title = "Sync across devices",
        icon = { Icon(Icons.Filled.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
    ) {
        InfoNote(
            "Same library on every phone and tablet with your Google account.",
            "Your library, sessions, notes and photos are kept in a private app folder in your own " +
                "Google Drive that only this app can see. No other server is involved, and it syncs " +
                "on its own every few hours."
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(enabled = !syncing, onClick = onSync, modifier = Modifier.weight(1f)) {
                if (syncing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        syncing -> "Syncing…"
                        enabled -> "Sync now"
                        else -> "Connect Google Drive"
                    }
                )
            }
            if (enabled) {
                OutlinedButton(enabled = !syncing, onClick = onDisconnect) { Text("Stop") }
            }
        }
        Spacer(Modifier.height(8.dp))
        val status = when (state) {
            is DriveSyncState.Done -> state.message
            is DriveSyncState.Failed -> state.message
            else -> when {
                !enabled -> null
                lastSync > 0 -> "Last synced " + DateUtils.getRelativeTimeSpanString(lastSync, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS) +
                    ". Syncs on its own every few hours."
                else -> "Not synced yet."
            }
        }
        status?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = if (state is DriveSyncState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
