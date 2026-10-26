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
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import coil3.BitmapImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.example.booktracker.app.analytics.AnalyticsEngine
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.RatingAxis
import com.example.booktracker.shared.models.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

enum class VictoryCardTheme(
    val title: String,
    val gradientColors: IntArray,
    val cardColor: Int,
    val textColor: Int,
    val accentColor: Int,
    val goldColor: Int,
    val brandColor: Int
) {
    ROYAL_AMBER(
        "Amber Royalty",
        intArrayOf(Color.parseColor("#451A03"), Color.parseColor("#92400E"), Color.parseColor("#D97706")),
        Color.parseColor("#1C1510"),
        Color.parseColor("#FEF3C7"),
        Color.parseColor("#F59E0B"),
        Color.parseColor("#FBBF24"),
        Color.parseColor("#D97706")
    ),
    MIDNIGHT_CELESTIAL(
        "Midnight Celestial",
        intArrayOf(Color.parseColor("#090D16"), Color.parseColor("#1E1B4B"), Color.parseColor("#4338CA")),
        Color.parseColor("#0F1423"),
        Color.parseColor("#F8FAFC"),
        Color.parseColor("#818CF8"),
        Color.parseColor("#38BDF8"),
        Color.parseColor("#6366F1")
    ),
    EMERALD_ACADEMY(
        "Emerald Academy",
        intArrayOf(Color.parseColor("#022C22"), Color.parseColor("#065F46"), Color.parseColor("#0D9488")),
        Color.parseColor("#0B1713"),
        Color.parseColor("#ECFDF5"),
        Color.parseColor("#34D399"),
        Color.parseColor("#10B981"),
        Color.parseColor("#059669")
    ),
    CRIMSON_VICTORY(
        "Crimson Victory",
        intArrayOf(Color.parseColor("#4C0519"), Color.parseColor("#9F1239"), Color.parseColor("#E11D48")),
        Color.parseColor("#1F0E14"),
        Color.parseColor("#FFF1F2"),
        Color.parseColor("#FB7185"),
        Color.parseColor("#F43F5E"),
        Color.parseColor("#BE123C")
    ),
    OBSIDIAN_LUX(
        "Obsidian Lux",
        intArrayOf(Color.parseColor("#0A0A0A"), Color.parseColor("#1F2937"), Color.parseColor("#374151")),
        Color.parseColor("#14161A"),
        Color.parseColor("#F3F4F6"),
        Color.parseColor("#E5E7EB"),
        Color.parseColor("#F59E0B"),
        Color.parseColor("#9CA3AF")
    )
}

object BookVictoryShareHelper {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())

    private suspend fun fetchCoverBitmap(context: Context, coverUrl: String): Bitmap? {
        if (coverUrl.isBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                (result as? SuccessResult)?.let { success ->
                    (success.image as? BitmapImage)?.bitmap
                }
            }.getOrNull()
        }
    }

    suspend fun shareVictoryCard(
        context: Context,
        book: Book,
        sessions: List<Session>,
        topNote: MarginNote? = null,
        theme: VictoryCardTheme = VictoryCardTheme.ROYAL_AMBER,
        format: QuoteCardFormat = QuoteCardFormat.SQUARE
    ) = withContext(Dispatchers.IO) {
        val width = format.width
        val height = format.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Fetch cover bitmap for thumbnail and ambient backdrop
        val coverBitmap = fetchCoverBitmap(context, book.coverUrl)

        // 2. Draw gradient background
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

        // 3. Ambient blurred cover backdrop (diffuse cover overlay)
        coverBitmap?.let { cover ->
            val ambientPaint = Paint().apply {
                alpha = 40
                isFilterBitmap = true
            }
            val srcRect = android.graphics.Rect(0, 0, cover.width, cover.height)
            val dstRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawBitmap(cover, srcRect, dstRect, ambientPaint)

            // Soft dark scrim over the ambient cover
            val scrimPaint = Paint().apply {
                color = Color.parseColor("#55000000")
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
        }

        // 4. Elevated Main Card
        val (cardRect, cornerRadius) = if (format == QuoteCardFormat.STORY) {
            val hMargin = 72f
            val vTop = 150f
            val vBottom = height - 150f
            RectF(hMargin, vTop, width - hMargin, vBottom) to 54f
        } else {
            val margin = 72f
            RectF(margin, margin, width - margin, height - margin) to 46f
        }

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.cardColor
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardPaint)

        // Card border stroke
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 35
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        // Book sessions stats calculation
        val bookSessions = sessions.filter { it.bookId == book.id }
        val timed = AnalyticsEngine.timedSessions(bookSessions)
        val totalMinutes = timed.sumOf { it.endTime - it.startTime } / 60_000L
        val formattedTime = if (totalMinutes < 1) "<1h" else AnalyticsEngine.formatMinutes(totalMinutes)
        val velocity = AnalyticsEngine.pagesPerHour(bookSessions)
        val formattedSpeed = velocity?.let { "${it.roundToInt()} p/h" } ?: "—"
        val totalPagesRead = if (book.totalPages > 0) book.totalPages else bookSessions.sumOf { it.pagesRead }
        val sessionCount = bookSessions.size.coerceAtLeast(1)

        val finishTimestamp = if (book.completedAt > 0L) book.completedAt else System.currentTimeMillis()
        val finishedDateStr = "Finished on ${Instant.ofEpochMilli(finishTimestamp).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)}"

        // 5. Draw Victory Badge & Header
        val headerY = cardRect.top + 70f
        val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.goldColor
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.15f
        }
        canvas.drawText("✦ READING VICTORY ✦", cardRect.centerX(), headerY, badgePaint)

        val datePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            alpha = 180
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(finishedDateStr, cardRect.centerX(), headerY + 40f, datePaint)

        // 6. Layout according to Format (Square vs Story)
        if (format == QuoteCardFormat.SQUARE) {
            drawSquareLayout(
                canvas = canvas,
                cardRect = cardRect,
                book = book,
                coverBitmap = coverBitmap,
                theme = theme,
                formattedTime = formattedTime,
                formattedSpeed = formattedSpeed,
                totalPagesRead = totalPagesRead,
                sessionCount = sessionCount,
                topNote = topNote
            )
        } else {
            drawStoryLayout(
                canvas = canvas,
                cardRect = cardRect,
                book = book,
                coverBitmap = coverBitmap,
                theme = theme,
                formattedTime = formattedTime,
                formattedSpeed = formattedSpeed,
                totalPagesRead = totalPagesRead,
                sessionCount = sessionCount,
                topNote = topNote
            )
        }

        // 7. Watermark
        val watermarkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.brandColor
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val watermarkY = if (format == QuoteCardFormat.STORY) height - 75f else height - 32f
        canvas.drawText("Nocturnal Reader", width / 2f, watermarkY, watermarkPaint)

        // 8. Save PNG to cache and fire share intent
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "victory_card_${book.id}_${System.currentTimeMillis()}.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "I just finished “${book.title}”!")
            putExtra(Intent.EXTRA_TEXT, "I just finished reading “${book.title}” with Nocturnal Reader!")
        }

        withContext(Dispatchers.Main) {
            context.startActivity(Intent.createChooser(shareIntent, "Share Victory Card"))
        }
    }

    private fun drawSquareLayout(
        canvas: Canvas,
        cardRect: RectF,
        book: Book,
        coverBitmap: Bitmap?,
        theme: VictoryCardTheme,
        formattedTime: String,
        formattedSpeed: String,
        totalPagesRead: Int,
        sessionCount: Int,
        topNote: MarginNote?
    ) {
        val contentTop = cardRect.top + 160f
        val coverWidth = 220f
        val coverHeight = 330f
        val coverLeft = cardRect.left + 50f
        val coverRect = RectF(coverLeft, contentTop, coverLeft + coverWidth, contentTop + coverHeight)

        drawCoverWithFrame(canvas, coverRect, coverBitmap, book, theme)

        // Details next to cover
        val infoLeft = coverRect.right + 40f
        val infoWidth = (cardRect.right - 50f - infoLeft).toInt().coerceAtLeast(200)

        // Title
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            textSize = if (book.title.length > 28) 36f else 42f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val titleLayout = StaticLayout.Builder.obtain(book.title, 0, book.title.length, titlePaint, infoWidth)
            .setMaxLines(3)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(infoLeft, contentTop + 10f)
        titleLayout.draw(canvas)
        canvas.restore()

        var currentInfoY = contentTop + 10f + titleLayout.height + 18f

        // Author
        if (book.authors.isNotEmpty()) {
            val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.accentColor
                textSize = 28f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val authorStr = "by ${book.authors.joinToString(", ")}"
            val authorLayout = StaticLayout.Builder.obtain(authorStr, 0, authorStr.length, authorPaint, infoWidth)
                .setMaxLines(1)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .build()
            canvas.save()
            canvas.translate(infoLeft, currentInfoY)
            authorLayout.draw(canvas)
            canvas.restore()
            currentInfoY += authorLayout.height + 20f
        }

        // Stars & Rating
        val avgRating = book.overallRating ?: 0f
        if (avgRating > 0f) {
            val starsPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.goldColor
                textSize = 34f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
            val starsCount = (avgRating).roundToInt().coerceIn(1, 5)
            val starsStr = "★".repeat(starsCount) + "☆".repeat(5 - starsCount)
            val ratingText = "$starsStr  ${String.format(Locale.getDefault(), "%.1f", avgRating)}"
            canvas.drawText(ratingText, infoLeft, currentInfoY + 28f, starsPaint)
            currentInfoY += 50f
        }

        // Format Badge
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 25
        }
        val badgeRect = RectF(infoLeft, currentInfoY, infoLeft + 220f, currentInfoY + 44f)
        canvas.drawRoundRect(badgeRect, 22f, 22f, pillPaint)
        val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            alpha = 220
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val formatStr = "${book.format.name} • $totalPagesRead p"
        canvas.drawText(formatStr, badgeRect.centerX(), badgeRect.centerY() + 8f, badgeTextPaint)

        // Stats Row across the bottom half
        val statsTop = coverRect.bottom + 40f
        val statWidth = (cardRect.width() - 80f) / 3f

        drawStatPill(canvas, RectF(cardRect.left + 40f, statsTop, cardRect.left + 40f + statWidth - 10f, statsTop + 100f), "READING TIME", formattedTime, theme)
        drawStatPill(canvas, RectF(cardRect.left + 40f + statWidth, statsTop, cardRect.left + 40f + (statWidth * 2) - 10f, statsTop + 100f), "AVG PACE", formattedSpeed, theme)
        drawStatPill(canvas, RectF(cardRect.left + 40f + (statWidth * 2), statsTop, cardRect.right - 40f, statsTop + 100f), "SESSIONS", "$sessionCount sessions", theme)

        // Review or Favorite Quote Box
        val quoteTop = statsTop + 124f
        val quoteBottom = cardRect.bottom - 40f
        val quoteRect = RectF(cardRect.left + 40f, quoteTop, cardRect.right - 40f, quoteBottom)

        drawReviewOrQuote(canvas, quoteRect, book, topNote, theme)
    }

    private fun drawStoryLayout(
        canvas: Canvas,
        cardRect: RectF,
        book: Book,
        coverBitmap: Bitmap?,
        theme: VictoryCardTheme,
        formattedTime: String,
        formattedSpeed: String,
        totalPagesRead: Int,
        sessionCount: Int,
        topNote: MarginNote?
    ) {
        val contentTop = cardRect.top + 170f

        // Prominent Centered Cover in Story mode
        val coverWidth = 320f
        val coverHeight = 480f
        val coverLeft = cardRect.centerX() - (coverWidth / 2f)
        val coverRect = RectF(coverLeft, contentTop, coverLeft + coverWidth, contentTop + coverHeight)

        drawCoverWithFrame(canvas, coverRect, coverBitmap, book, theme)

        var currentY = coverRect.bottom + 50f
        val maxTextWidth = (cardRect.width() - 80f).toInt()

        // Title (Centered)
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            textSize = if (book.title.length > 25) 44f else 52f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val titleLayout = StaticLayout.Builder.obtain(book.title, 0, book.title.length, titlePaint, maxTextWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(2)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(cardRect.centerX(), currentY)
        titleLayout.draw(canvas)
        canvas.restore()

        currentY += titleLayout.height + 24f

        // Author
        if (book.authors.isNotEmpty()) {
            val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.accentColor
                textSize = 32f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            val authorStr = "by ${book.authors.joinToString(", ")}"
            canvas.drawText(authorStr, cardRect.centerX(), currentY, authorPaint)
            currentY += 46f
        }

        // Stars & Rating
        val avgRating = book.overallRating ?: 0f
        if (avgRating > 0f) {
            val starsPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.goldColor
                textSize = 42f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val starsCount = avgRating.roundToInt().coerceIn(1, 5)
            val starsStr = "★".repeat(starsCount) + "☆".repeat(5 - starsCount)
            canvas.drawText("$starsStr   ${String.format(Locale.getDefault(), "%.1f / 5.0", avgRating)}", cardRect.centerX(), currentY + 10f, starsPaint)
            currentY += 60f
        }

        // Breakdown Axis Chips if available
        if (book.rating.isNotEmpty()) {
            val ratingPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = 20
            }
            val axisTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.textColor
                textSize = 22f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            val axes = RatingAxis.ALL.filter { book.rating.containsKey(it) }.take(3)
            if (axes.isNotEmpty()) {
                val chipWidth = 240f
                val chipHeight = 44f
                val totalChipsWidth = (axes.size * chipWidth) + ((axes.size - 1) * 16f)
                var startChipX = cardRect.centerX() - (totalChipsWidth / 2f)

                axes.forEach { axis ->
                    val r = book.rating[axis] ?: 0f
                    val chipRect = RectF(startChipX, currentY, startChipX + chipWidth, currentY + chipHeight)
                    canvas.drawRoundRect(chipRect, 22f, 22f, ratingPillPaint)
                    canvas.drawText("$axis: ${String.format(Locale.getDefault(), "%.1f", r)}★", chipRect.centerX(), chipRect.centerY() + 8f, axisTextPaint)
                    startChipX += chipWidth + 16f
                }
                currentY += 76f
            }
        }

        // Stats Row (3 Pillars)
        val statWidth = (cardRect.width() - 80f) / 3f
        val statTop = currentY + 20f
        drawStatPill(canvas, RectF(cardRect.left + 40f, statTop, cardRect.left + 40f + statWidth - 12f, statTop + 120f), "READING TIME", formattedTime, theme)
        drawStatPill(canvas, RectF(cardRect.left + 40f + statWidth, statTop, cardRect.left + 40f + (statWidth * 2) - 12f, statTop + 120f), "READING PACE", formattedSpeed, theme)
        drawStatPill(canvas, RectF(cardRect.left + 40f + (statWidth * 2), statTop, cardRect.right - 40f, statTop + 120f), "VOLUME", "$totalPagesRead pages", theme)

        // Review or Favorite Quote Box
        val quoteTop = statTop + 150f
        val quoteBottom = cardRect.bottom - 60f
        val quoteRect = RectF(cardRect.left + 40f, quoteTop, cardRect.right - 40f, quoteBottom)

        drawReviewOrQuote(canvas, quoteRect, book, topNote, theme)
    }

    private fun drawCoverWithFrame(
        canvas: Canvas,
        rect: RectF,
        coverBitmap: Bitmap?,
        book: Book,
        theme: VictoryCardTheme
    ) {
        // Drop shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = 110
        }
        val shadowRect = RectF(rect.left + 8f, rect.top + 12f, rect.right + 8f, rect.bottom + 12f)
        canvas.drawRoundRect(shadowRect, 18f, 18f, shadowPaint)

        if (coverBitmap != null) {
            val coverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isFilterBitmap = true
            }
            // Clip to rounded rect
            canvas.save()
            val path = android.graphics.Path().apply {
                addRoundRect(rect, 16f, 16f, android.graphics.Path.Direction.CW)
            }
            canvas.clipPath(path)
            val src = android.graphics.Rect(0, 0, coverBitmap.width, coverBitmap.height)
            canvas.drawBitmap(coverBitmap, src, rect, coverPaint)
            canvas.restore()
        } else {
            // Elegant fallback book cover
            val bookBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.brandColor
            }
            canvas.drawRoundRect(rect, 16f, 16f, bookBgPaint)

            val spinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                alpha = 60
            }
            canvas.drawRect(rect.left, rect.top, rect.left + 24f, rect.bottom, spinePaint)

            val coverTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 26f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val titleLayout = StaticLayout.Builder.obtain(book.title, 0, book.title.length, coverTextPaint, (rect.width() - 36f).toInt())
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(4)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .build()
            canvas.save()
            canvas.translate(rect.centerX() + 6f, rect.centerY() - (titleLayout.height / 2f))
            titleLayout.draw(canvas)
            canvas.restore()
        }

        // Cover border outline
        val frameBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 50
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(rect, 16f, 16f, frameBorder)
    }

    private fun drawStatPill(
        canvas: Canvas,
        rect: RectF,
        label: String,
        value: String,
        theme: VictoryCardTheme
    ) {
        val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 20
        }
        canvas.drawRoundRect(rect, 20f, 20f, pillBg)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 30
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(rect, 20f, 20f, border)

        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            alpha = 150
            textSize = 19f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
        }

        canvas.drawText(value, rect.centerX(), rect.centerY() - 2f, valuePaint)
        canvas.drawText(label, rect.centerX(), rect.centerY() + 32f, labelPaint)
    }

    private fun drawReviewOrQuote(
        canvas: Canvas,
        rect: RectF,
        book: Book,
        topNote: MarginNote?,
        theme: VictoryCardTheme
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 15
        }
        canvas.drawRoundRect(rect, 24f, 24f, bgPaint)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 25
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(rect, 24f, 24f, border)

        val isReview = !book.review.isNullOrBlank()
        val quoteText = when {
            isReview -> "“${book.review}”"
            topNote != null && topNote.content.isNotBlank() -> "“${topNote.content}”"
            else -> "“A reader lives a thousand lives before he dies. The man who never reads lives only one.”"
        }

        val tagStr = when {
            isReview -> "READER'S REVIEW"
            topNote != null -> "HIGHLIGHTED QUOTE • PAGE ${topNote.pageNumber}"
            else -> "READING MEMORY"
        }

        val tagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accentColor
            textSize = 21f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.1f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(tagStr, rect.centerX(), rect.top + 36f, tagPaint)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            textSize = if (quoteText.length > 180) 26f else 30f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }

        val textWidth = (rect.width() - 60f).toInt()
        val textLayout = StaticLayout.Builder.obtain(quoteText, 0, quoteText.length, textPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(4)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()

        val textY = rect.centerY() - (textLayout.height / 2f) + 12f
        canvas.save()
        canvas.translate(rect.centerX(), textY)
        textLayout.draw(canvas)
        canvas.restore()
    }
}
