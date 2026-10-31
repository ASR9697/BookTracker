package com.example.booktracker.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Session
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class MonthlyCardTheme(
    val title: String,
    val gradientColors: IntArray,
    val cardColor: Int,
    val textColor: Int,
    val accentColor: Int,
    val brandColor: Int
) {
    BOOKMORY_AMBER(
        "Amber",
        intArrayOf(Color.parseColor("#78350F"), Color.parseColor("#B45309"), Color.parseColor("#F59E0B")),
        Color.parseColor("#1C1613"),
        Color.parseColor("#FEF3C7"),
        Color.parseColor("#F59E0B"),
        Color.parseColor("#D97706")
    ),
    MIDNIGHT_VELVET(
        "Midnight",
        intArrayOf(Color.parseColor("#0B0E17"), Color.parseColor("#1E1B4B"), Color.parseColor("#3730A3")),
        Color.parseColor("#121724"),
        Color.parseColor("#F8FAFC"),
        Color.parseColor("#818CF8"),
        Color.parseColor("#38BDF8")
    ),
    NORDIC_EMERALD(
        "Emerald",
        intArrayOf(Color.parseColor("#064E3B"), Color.parseColor("#065F46"), Color.parseColor("#10B981")),
        Color.parseColor("#0F1C16"),
        Color.parseColor("#ECFDF5"),
        Color.parseColor("#34D399"),
        Color.parseColor("#10B981")
    ),
    SAKURA_ROSE(
        "Sakura",
        intArrayOf(Color.parseColor("#881337"), Color.parseColor("#BE123C"), Color.parseColor("#FB7185")),
        Color.parseColor("#1C1319"),
        Color.parseColor("#FFF1F2"),
        Color.parseColor("#FB7185"),
        Color.parseColor("#F43F5E")
    ),
    WARM_PARCHMENT(
        "Parchment",
        intArrayOf(Color.parseColor("#FAF7F2"), Color.parseColor("#F2ECE1"), Color.parseColor("#E5DCCF")),
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#1F1B18"),
        Color.parseColor("#D97706"),
        Color.parseColor("#8C7B6D")
    )
}

object MonthlyShareHelper {

    // A getter, so a change of language shows without restarting the app.
    private val monthFormatter get() = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    fun shareMonthlyReport(
        context: Context,
        yearMonth: YearMonth,
        books: List<Book>,
        sessions: List<Session>,
        userName: String = "Reader",
        theme: MonthlyCardTheme = MonthlyCardTheme.BOOKMORY_AMBER,
        format: QuoteCardFormat = QuoteCardFormat.SQUARE
    ) {
        val width = format.width
        val height = format.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val zone = ZoneId.systemDefault()
        val monthSessions = sessions.filter {
            val date = Instant.ofEpochMilli(it.endTime).atZone(zone).toLocalDate()
            YearMonth.from(date) == yearMonth
        }

        val totalPages = monthSessions.sumOf { it.pagesRead }
        val totalMinutes = monthSessions.sumOf { it.durationSeconds } / 60
        val totalHours = String.format(Locale.getDefault(), "%.1f", totalMinutes / 60f)
        val activeDays = monthSessions.map {
            Instant.ofEpochMilli(it.endTime).atZone(zone).toLocalDate()
        }.distinct().size

        val finishedThisMonth = books.filter { b ->
            b.status == BookStatus.FINISHED.name && b.completedAt > 0L &&
                    YearMonth.from(Instant.ofEpochMilli(b.completedAt).atZone(zone).toLocalDate()) == yearMonth
        }

        // Draw gradient background
        val bgPaint = Paint()
        val gradientPositions = when (theme.gradientColors.size) {
            2 -> floatArrayOf(0f, 1f)
            3 -> floatArrayOf(0f, 0.5f, 1f)
            else -> floatArrayOf(0f, 1f)
        }
        bgPaint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            theme.gradientColors,
            gradientPositions,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw inner elevated card
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.cardColor
        }

        val (cardRect, cornerRadius) = if (format == QuoteCardFormat.STORY) {
            val hMargin = 80f
            val vTop = 200f
            val vBottom = height - 200f
            RectF(hMargin, vTop, width - hMargin, vBottom) to 56f
        } else {
            val margin = 80f
            RectF(margin, margin, width - margin, height - margin) to 48f
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardPaint)

        // Header Category Label
        val categoryPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accentColor
            textSize = 32f
            letterSpacing = 0.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val headerY = cardRect.top + 90f
        canvas.drawText("MONTHLY READING JOURNAL", cardRect.centerX(), headerY, categoryPaint)

        // Month Title (e.g. October 2026)
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            textSize = if (format == QuoteCardFormat.STORY) 74f else 64f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val titleY = headerY + 80f
        canvas.drawText(yearMonth.format(monthFormatter), cardRect.centerX(), titleY, titlePaint)

        // User badge pill
        val userBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accentColor
            alpha = 40
        }
        val userTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val userPillWidth = 320f
        val userPillHeight = 56f
        val userPillRect = RectF(
            cardRect.centerX() - userPillWidth / 2f,
            titleY + 30f,
            cardRect.centerX() + userPillWidth / 2f,
            titleY + 30f + userPillHeight
        )
        canvas.drawRoundRect(userPillRect, userPillHeight / 2f, userPillHeight / 2f, userBadgePaint)
        canvas.drawText("Reader: $userName", cardRect.centerX(), userPillRect.centerY() + 10f, userTextPaint)

        // 4 Key Stats Grid / Row
        val statsTop = userPillRect.bottom + 50f
        val statBoxWidth = (cardRect.width() - 80f) / 2f
        val statBoxHeight = if (format == QuoteCardFormat.STORY) 180f else 150f

        val statCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#80000000")
            alpha = if (theme == MonthlyCardTheme.WARM_PARCHMENT) 20 else 80
        }
        val statValPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accentColor
            textSize = 58f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val statLblPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            alpha = 180
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        val stats = listOf(
            "🏆 ${finishedThisMonth.size}" to "Books finished",
            "📄 $totalPages" to "Pages read",
            "⏱️ ${totalHours}h" to "Reading time",
            "📅 $activeDays" to "Days active"
        )

        for (i in stats.indices) {
            val col = i % 2
            val row = i / 2
            val boxLeft = cardRect.left + 30f + col * (statBoxWidth + 20f)
            val boxTop = statsTop + row * (statBoxHeight + 20f)
            val boxRect = RectF(boxLeft, boxTop, boxLeft + statBoxWidth, boxTop + statBoxHeight)

            canvas.drawRoundRect(boxRect, 24f, 24f, statCardPaint)
            val (valText, lblText) = stats[i]
            canvas.drawText(valText, boxRect.centerX(), boxRect.top + statBoxHeight * 0.48f, statValPaint)
            canvas.drawText(lblText, boxRect.centerX(), boxRect.top + statBoxHeight * 0.78f, statLblPaint)
        }

        // Books Finished / Read section
        val booksSectionTop = statsTop + 2 * (statBoxHeight + 20f) + 30f
        val sectionHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            textSize = 34f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        val sectionTitle = if (finishedThisMonth.isNotEmpty()) {
            "Books finished (${finishedThisMonth.size})"
        } else {
            "Reading highlights"
        }
        canvas.drawText(sectionTitle, cardRect.left + 40f, booksSectionTop, sectionHeaderPaint)

        val booksToDisplay = if (finishedThisMonth.isNotEmpty()) finishedThisMonth else {
            val readBookIds = monthSessions.map { it.bookId }.distinct()
            books.filter { it.id in readBookIds }
        }

        var currentBookY = booksSectionTop + 45f
        val bookTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            textSize = 30f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val bookAuthorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accentColor
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }

        val maxBooks = if (format == QuoteCardFormat.STORY) 7 else 4
        val displayList = booksToDisplay.take(maxBooks)

        if (displayList.isEmpty()) {
            val emptyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.textColor
                alpha = 140
                textSize = 28f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("No finished books this month yet — keep reading!", cardRect.centerX(), currentBookY + 40f, emptyPaint)
        } else {
            for (book in displayList) {
                val bullet = "✦ "
                val titleTruncated = if (book.title.length > 36) book.title.take(33) + "..." else book.title
                canvas.drawText("$bullet$titleTruncated", cardRect.left + 40f, currentBookY, bookTitlePaint)
                val authorStr = if (book.authors.isNotEmpty()) "by ${book.authors.first()}" else ""
                val ratingStr = if (book.rating != null && book.rating.isNotEmpty()) {
                    val stars = " ★ " + String.format(Locale.getDefault(), "%.1f", book.overallRating ?: 0f)
                    stars
                } else ""
                canvas.drawText("$authorStr$ratingStr", cardRect.left + 65f, currentBookY + 32f, bookAuthorPaint)
                currentBookY += 72f
            }
            if (booksToDisplay.size > maxBooks) {
                val extraCount = booksToDisplay.size - maxBooks
                val morePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = theme.accentColor
                    textSize = 26f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.LEFT
                }
                canvas.drawText("+ $extraCount more books", cardRect.left + 65f, currentBookY, morePaint)
            }
        }

        // Bottom branding watermark
        val watermarkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.brandColor
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val watermarkY = if (format == QuoteCardFormat.STORY) height - 100f else height - 35f
        canvas.drawText("Nocturnal Reader", width / 2f, watermarkY, watermarkPaint)

        // Save to cache dir and launch share intent
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "monthly_wrapup_${yearMonth.year}_${yearMonth.monthValue}_${System.currentTimeMillis()}.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "${yearMonth.format(monthFormatter)} Reading Report")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share monthly card"))
    }
}
