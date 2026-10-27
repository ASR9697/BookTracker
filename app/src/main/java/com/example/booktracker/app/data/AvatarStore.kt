package com.example.booktracker.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * The profile picture, kept in filesDir/avatar apart from note photos so the
 * note-photo sweep never touches it. Only the current picture is kept.
 *
 * The stored setting is one of: "" (the name's initial), [COVER_PREFIX] + book id,
 * or [PHOTO_PREFIX] + a "file://" URI from [importPhoto].
 */
object AvatarStore {

    const val COVER_PREFIX = "cover:"
    const val PHOTO_PREFIX = "photo:"
    private const val DIR = "avatar"
    private const val MAX_EDGE_PX = 512

    /** Copies a picked image in as the new avatar and deletes the old one; null if unreadable. */
    suspend fun importPhoto(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val target = File(dir, "avatar_${UUID.randomUUID()}.jpg")
        if (!NoteImageStore.writeUpright(context, source, target, MAX_EDGE_PX)) return@withContext null
        dir.listFiles()?.forEach { if (it != target) it.delete() }
        PHOTO_PREFIX + "file://${target.absolutePath}"
    }

    /** Drops the stored photo when the reader switches to a cover or the initial. */
    suspend fun clearPhoto(context: Context) = withContext(Dispatchers.IO) {
        File(context.filesDir, DIR).listFiles()?.forEach { it.delete() }
    }
}
