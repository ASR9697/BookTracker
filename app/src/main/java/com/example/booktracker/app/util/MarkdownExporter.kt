package com.example.booktracker.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.MarginNote
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MarkdownExporter {

    fun exportToMarkdown(
        context: Context,
        books: List<Book>,
        notes: List<MarginNote>
    ) {
        val sb = StringBuilder()
        sb.append("# My Reading Notes\n\n")
        sb.append("Generated on ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\n\n")
        
        val notesByBook = notes.groupBy { it.bookId }

        for (book in books) {
            sb.append("## ${book.title}\n")
            if (book.authors.isNotEmpty()) {
                sb.append("**Author(s):** ${book.authors.joinToString()}\n")
            }
            val statusLabel = BookStatus.entries
                .firstOrNull { it.name.equals(book.status, ignoreCase = true) }?.label ?: book.status
            sb.append("**Status:** $statusLabel\n")
            if (book.rating.isNotEmpty()) {
                sb.append("**Rating:** ${String.format(Locale.getDefault(), "%.1f", book.overallRating ?: 0f)}/5.0\n")
            }
            sb.append("\n")

            val bookNotes = notesByBook[book.id] ?: emptyList()
            if (bookNotes.isNotEmpty()) {
                sb.append("### Margin Notes & Highlights\n\n")
                for (note in bookNotes) {
                    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.timestamp))
                    // Indent continuation lines so multi-line notes stay inside their bullet.
                    sb.append("- **Page ${note.pageNumber}** ($dateStr): ${note.content.trim().replace("\n", "\n  ")}\n")
                }
                sb.append("\n")
            }
            sb.append("---\n\n")
        }

        val fileName = "NocturnalReader_Notes.md"
        val file = File(context.cacheDir, fileName)
        file.writeText(sb.toString())

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/markdown"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export reading notes"))
    }
}
