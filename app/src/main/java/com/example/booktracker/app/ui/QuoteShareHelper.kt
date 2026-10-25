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
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.MarginNote
import java.io.File
import java.io.FileOutputStream

enum class QuoteCardFormat(
    val title: String,
    val width: Int,
    val height: Int,
    val isVertical: Boolean
) {
    SQUARE("Square (1:1)", 1080, 1080, false),
    STORY("Story (9:16)", 1080, 1920, true)
}

enum class QuoteCardTheme(
    val title: String,
    val gradientColors: IntArray,
    val cardColor: Int,
    val textColor: Int,
    val attrColor: Int,
    val brandColor: Int
) {
    WARM_PARCHMENT(
        "Parchment",
        intArrayOf(Color.parseColor("#F5EBE1"), Color.parseColor("#EBD5BD")),
        Color.parseColor("#FFFDF9"),
        Color.parseColor("#2C241E"),
        Color.parseColor("#8C7B6D"),
        Color.parseColor("#8C7B6D")
    ),
    MIDNIGHT(
        "Midnight",
        intArrayOf(Color.parseColor("#0F172A"), Color.parseColor("#1E293B")),
        Color.parseColor("#1E293B"),
        Color.parseColor("#F8FAFC"),
        Color.parseColor("#94A3B8"),
        Color.parseColor("#64748B")
    ),
    SUNSET(
        "Sunset",
        intArrayOf(Color.parseColor("#4B39EF"), Color.parseColor("#FF5963"), Color.parseColor("#EE8B60")),
        Color.WHITE,
        Color.parseColor("#14181B"),
        Color.parseColor("#57636C"),
        Color.WHITE
    ),
    SAGE(
        "Sage",
        intArrayOf(Color.parseColor("#7D9D9C"), Color.parseColor("#576F72")),
        Color.parseColor("#F0EBE3"),
        Color.parseColor("#1E2D24"),
        Color.parseColor("#526D58"),
        Color.WHITE
    ),
    POLAROID(
        "Polaroid",
        intArrayOf(Color.parseColor("#F8FAFC"), Color.parseColor("#E2E8F0")),
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#0F172A"),
        Color.parseColor("#475569"),
        Color.parseColor("#64748B")
    ),
    ROSE_GOLD(
        "Sakura",
        intArrayOf(Color.parseColor("#FFF1F2"), Color.parseColor("#FFE4E6"), Color.parseColor("#FECDD3")),
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#881337"),
        Color.parseColor("#BE123C"),
        Color.parseColor("#9F1239")
    ),
    COFFEE(
        "Espresso",
        intArrayOf(Color.parseColor("#271810"), Color.parseColor("#3E2723")),
        Color.parseColor("#3E2723"),
        Color.parseColor("#EFEBE9"),
        Color.parseColor("#D7CCC8"),
        Color.parseColor("#BCAAA4")
    )
}

object QuoteShareHelper {

    fun shareQuote(
        context: Context,
        note: MarginNote,
        book: Book,
        theme: QuoteCardTheme = QuoteCardTheme.WARM_PARCHMENT,
        format: QuoteCardFormat = QuoteCardFormat.SQUARE
    ) {
        val width = format.width
        val height = format.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw gradient background
        val paint = Paint()
        val gradientPositions = when (theme.gradientColors.size) {
            2 -> floatArrayOf(0f, 1f)
            3 -> floatArrayOf(0f, 0.5f, 1f)
            else -> floatArrayOf(0f, 1f)
        }
        paint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            theme.gradientColors,
            gradientPositions,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Draw inner card with format-aware margins
        val cardPaint = Paint().apply {
            color = theme.cardColor
            alpha = if (theme == QuoteCardTheme.SUNSET) 240 else 255
            isAntiAlias = true
        }

        val (cardRect, cornerRadius) = if (format == QuoteCardFormat.STORY) {
            val hMargin = 90f
            val vTop = 260f
            val vBottom = height - 260f
            RectF(hMargin, vTop, width - hMargin, vBottom) to 56f
        } else {
            val margin = 100f
            RectF(margin, margin, width - margin, height - margin) to 48f
        }

        // Polaroid card border if applicable
        if (theme == QuoteCardTheme.POLAROID) {
            val borderPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }
            canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardPaint)
            canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)
        } else {
            canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardPaint)
        }

        // Decorative quotation mark glyph
        val glyphPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.attrColor
            alpha = 65
            textSize = if (format == QuoteCardFormat.STORY) 200f else 180f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("“", cardRect.left + 50f, cardRect.top + (if (format == QuoteCardFormat.STORY) 190f else 160f), glyphPaint)

        // Quote Text with dynamic sizing
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            textSize = when {
                note.content.length > 280 -> 42f
                note.content.length > 180 -> 48f
                else -> if (format == QuoteCardFormat.STORY) 56f else 54f
            }
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }

        val quote = note.content
        val staticLayout = StaticLayout.Builder.obtain(quote, 0, quote.length, textPaint, (cardRect.width() - 140f).toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.35f)
            .build()

        val textHeight = staticLayout.height
        val textY = cardRect.top + (cardRect.height() - textHeight) / 2f - (if (format == QuoteCardFormat.STORY) 40f else 30f)

        canvas.save()
        canvas.translate(cardRect.left + 70f, textY)
        staticLayout.draw(canvas)
        canvas.restore()

        // Page number (if any)
        if (note.pageNumber > 0) {
            val pagePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.attrColor
                textSize = 34f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Page ${note.pageNumber}", cardRect.centerX(), cardRect.bottom - (if (format == QuoteCardFormat.STORY) 160f else 130f), pagePaint)
        }

        // Attribution (Title and Author)
        val attrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.attrColor
            textSize = 38f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val authorText = if (book.authors.isNotEmpty()) " by ${book.authors.first()}" else ""
        val attrStr = "— ${book.title}$authorText"

        val attrY = cardRect.bottom - (if (format == QuoteCardFormat.STORY) 90f else 75f)
        canvas.drawText(attrStr, cardRect.centerX(), attrY, attrPaint)

        // App branding watermark
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.brandColor
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val brandY = if (format == QuoteCardFormat.STORY) height - 120f else height - 40f
        canvas.drawText("BookTracker", width / 2f, brandY, brandPaint)

        // Save to cache dir and share
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "quote_share_${System.currentTimeMillis()}.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Quote Card"))
    }
}
