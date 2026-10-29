package com.example.booktracker.app.sync

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

/**
 * Asks Google for permission to the app's own Drive folder (drive.appdata),
 * nothing else in the reader's Drive. The first time shows Google's account
 * and consent screen; after that the token comes back silently, refreshed by
 * Play services, so background sync can run without the reader.
 *
 * Needs an Android OAuth client for this package + signing certificate in the
 * Google Cloud project (see RELEASE.md); without one, authorize fails.
 */
object DriveAuth {

    const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"

    sealed interface Outcome {
        data class Token(val value: String) : Outcome
        /** Launch [sender] with StartIntentSenderForResult, then pass the result to [fromConsent]. */
        data class NeedsConsent(val sender: IntentSender) : Outcome
        data class Failed(val message: String) : Outcome
    }

    private fun request() = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(SCOPE)))
        .build()

    suspend fun authorize(context: Context): Outcome = runCatching {
        val result = Identity.getAuthorizationClient(context).authorize(request()).await()
        val pending = result.pendingIntent
        val token = result.accessToken
        when {
            result.hasResolution() && pending != null -> Outcome.NeedsConsent(pending.intentSender)
            token != null -> Outcome.Token(token)
            else -> Outcome.Failed("Google didn't grant access to Drive.")
        }
    }.getOrElse { Outcome.Failed(it.message ?: "Couldn't reach Google sign-in.") }

    /** The token from the consent screen's result, or null when the reader backed out. */
    fun fromConsent(context: Context, data: Intent?): String? = runCatching {
        Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data).accessToken
    }.getOrNull()
}
