package com.example.booktracker.app.worker

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.booktracker.app.data.BackupEngine
import com.example.booktracker.app.data.ServiceLocator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Once a week, the same .zip as Settings > Export, written into the folder the
 * reader picked. Only the newest [KEEP] automatic backups are kept there; files
 * the reader saved by hand are left alone.
 */
class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = ServiceLocator.settings(applicationContext)
        val prefs = settings.prefs.first()
        if (!prefs.autoBackup || prefs.autoBackupFolder.isBlank()) return Result.success()
        return runCatching {
            backupNow(applicationContext, Uri.parse(prefs.autoBackupFolder))
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val PREFIX = "nocturnal-reader-auto-"
        private const val KEEP = 4

        /** Writes one backup into [tree] and prunes older automatic ones. Throws when the folder is gone. */
        suspend fun backupNow(context: Context, tree: Uri) {
            val resolver = context.contentResolver
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
            val file = DocumentsContract.createDocument(resolver, parent, "application/zip", "$PREFIX$stamp.zip")
                ?: error("Couldn't create a file in the backup folder")
            val settings = ServiceLocator.settings(context)
            BackupEngine.exportToUri(
                context = context,
                uri = file,
                snapshot = ServiceLocator.repository(context).snapshotForBackup(),
                settings = settings.backupSettings()
            )
            prune(context, tree)
            val now = System.currentTimeMillis()
            settings.updatePrefs { it.copy(lastAutoBackup = now) }
        }

        private fun prune(context: Context, tree: Uri) {
            val resolver = context.contentResolver
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val found = mutableListOf<Pair<String, String>>() // name to document id
            resolver.query(
                children,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null, null, null
            )?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(0) ?: continue
                    if (name.startsWith(PREFIX)) found += name to c.getString(1)
                }
            }
            // Names carry the timestamp, so sorting them sorts by age.
            found.sortedByDescending { it.first }.drop(KEEP).forEach { (_, id) ->
                runCatching { DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, id)) }
            }
        }
    }
}

object AutoBackupScheduler {
    private const val WORK_NAME = "weekly_auto_backup"

    fun apply(context: Context, enabled: Boolean) {
        val work = WorkManager.getInstance(context)
        if (!enabled) {
            work.cancelUniqueWork(WORK_NAME)
            return
        }
        // KEEP: re-enabling or restarting the app doesn't reset the week.
        work.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS).build()
        )
    }
}
