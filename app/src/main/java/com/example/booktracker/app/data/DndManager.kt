package com.example.booktracker.app.data

import android.app.NotificationManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DndManager(
    private val context: Context,
    private val repository: BookRepository,
    private val settings: SettingsRepository
) {
    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var previousFilter: Int? = null

    init {
        CoroutineScope(Dispatchers.Default).launch {
            combine(
                repository.observeOpenSession(),
                settings.dndDuringSession
            ) { openSession, dndEnabled ->
                openSession != null && dndEnabled
            }.collect { shouldBeInDnd ->
                if (shouldBeInDnd) {
                    enableDnd()
                } else {
                    disableDnd()
                }
            }
        }
    }

    private fun enableDnd() {
        if (!nm.isNotificationPolicyAccessGranted) return
        if (nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
            previousFilter = nm.currentInterruptionFilter
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }
    }

    private fun disableDnd() {
        if (!nm.isNotificationPolicyAccessGranted) return
        if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
            nm.setInterruptionFilter(previousFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL)
            previousFilter = null
        }
    }
}
