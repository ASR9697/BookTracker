package com.example.booktracker.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.booktracker.app.MainActivity

/**
 * Opens the app on a given screen, for taps on a widget. The activity is
 * exported (it's the launcher), so only routes the widgets themselves build are
 * honoured; anything else arriving in the extra is ignored.
 */
object DeepLink {

    const val EXTRA_ROUTE = "com.example.booktracker.OPEN_ROUTE"

    private val ALLOWED = Regex("^(home|library|memorize|stats|profile|review|streak|calendar|vocabulary|(book|focus|session_save)/[A-Za-z0-9_.:-]+)$")

    fun intent(context: Context, route: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            // A distinct data URI per route keeps the system from merging the widgets' PendingIntents.
            data = Uri.parse("nocturnal://open/" + Uri.encode(route))
            putExtra(EXTRA_ROUTE, route)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

    fun routeFrom(intent: Intent?): String? =
        intent?.getStringExtra(EXTRA_ROUTE)?.takeIf { ALLOWED.matches(it) }
}
