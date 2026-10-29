package com.example.booktracker.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

/**
 * Photos attached to notes live in filesDir/note_images as downscaled JPEGs,
 * referenced from notes as "file://" URIs, like local covers.
 */
object NoteImageStore {

    const val DIR = "note_images"

    /** Long edge after import: sharp enough to read a page and for OCR, small enough for backups. */
    const val MAX_EDGE_PX = 2048
    private const val JPEG_QUALITY = 85
    private const val CAPTURE_PREFIX = "capture_"

    fun dir(context: Context): File = File(context.filesDir, DIR).apply { mkdirs() }

    /** Empty file the camera writes into; [import] replaces it with the downscaled copy. */
    fun newCaptureFile(context: Context): File = File(dir(context), "$CAPTURE_PREFIX${UUID.randomUUID()}.jpg")

    /**
     * Copies [source] (camera file or picked content URI) into the store, rotated
     * upright and downscaled. Returns the stored "file://" URI, or null if the
     * image could not be read. Deletes [source] when it is one of our capture files.
     */
    suspend fun import(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        val target = File(dir(context), "note_${UUID.randomUUID()}.jpg")
        if (!writeUpright(context, source, target, MAX_EDGE_PX)) return@withContext null
        localFile(source.toString())?.takeIf { it.name.startsWith(CAPTURE_PREFIX) }?.delete()
        "file://${target.absolutePath}"
    }

    /** Decodes [source], rotates it upright, downscales to [maxEdge] and writes it to [target] as a JPEG. */
    internal fun writeUpright(context: Context, source: Uri, target: File, maxEdge: Int): Boolean {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // A bounds-only decode always returns null by design; only a missing stream means unreadable.
        // (Treating that null as failure is what made every photo import, scan and avatar fail.)
        val boundsStream = resolver.openInputStream(source) ?: return false
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge) }
        val decoded = resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return false
        val rotation = resolver.openInputStream(source)?.use { exifRotation(it) } ?: 0
        val upright = scaleAndRotate(decoded, maxEdge, rotation)

        FileOutputStream(target).use { upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        if (upright !== decoded) upright.recycle()
        decoded.recycle()
        return true
    }

    /** Writes an already-processed bitmap (for example a highlighted copy) into the store as a JPEG. */
    fun saveBitmap(context: Context, bitmap: Bitmap): String {
        val target = File(dir(context), "note_${UUID.randomUUID()}.jpg")
        FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        return "file://${target.absolutePath}"
    }

    /** The stored file behind a "file://" URI inside the store, or null for anything else. */
    fun localFile(uri: String): File? {
        if (!uri.startsWith("file://")) return null
        val file = File(Uri.parse(uri).path ?: return null)
        return file.takeIf { it.parentFile?.name == DIR }
    }

    /**
     * Deletes stored photos no note references any more. Runs at startup rather
     * than on delete, so undoing a deleted note or book keeps its photos.
     */
    suspend fun sweepOrphans(context: Context, referenced: Collection<String>) = withContext(Dispatchers.IO) {
        val keep = referenced.mapNotNull { localFile(it)?.name }.toSet()
        dir(context).listFiles()?.forEach { if (it.name !in keep) it.delete() }
    }

    /** Largest power-of-two step down that keeps the long edge at or above [maxEdge]. */
    fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }

    private fun exifRotation(stream: java.io.InputStream): Int =
        when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    private fun scaleAndRotate(bitmap: Bitmap, maxEdge: Int, rotation: Int): Bitmap {
        val scale = (maxEdge.toFloat() / max(bitmap.width, bitmap.height)).coerceAtMost(1f)
        if (scale == 1f && rotation == 0) return bitmap
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
