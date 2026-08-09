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

object QuoteShareHelper {

    fun shareQuote(context: Context, note: MarginNote, book: Book) {
        val width = 1080
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw gradient background
        val paint = Paint()
        paint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(Color.parseColor("#4B39EF"), Color.parseColor("#FF5963"), Color.parseColor("#EE8B60")),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Draw inner card
        val cardPaint = Paint().apply {
            color = Color.WHITE
            alpha = 240 // slightly transparent
            isAntiAlias = true
        }
        val cardRect = RectF(100f, 100f, width - 100f, height - 100f)
        canvas.drawRoundRect(cardRect, 40f, 40f, cardPaint)

        // Quote Text
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#14181B")
            textSize = 64f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }

        val quote = "“${note.content}”"
        val staticLayout = StaticLayout.Builder.obtain(quote, 0, quote.length, textPaint, (cardRect.width() - 100f).toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.2f)
            .build()

        val textHeight = staticLayout.height
        val textY = cardRect.top + (cardRect.height() - textHeight) / 2f - 40f

        canvas.save()
        canvas.translate(cardRect.left + 50f, textY)
        staticLayout.draw(canvas)
        canvas.restore()

        // Attribution (Title and Author)
        val attrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#57636C")
            textSize = 40f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val authorText = if (book.authors.isNotEmpty()) " by ${book.authors.first()}" else ""
        val attrStr = "— ${book.title}$authorText"

        val attrY = cardRect.bottom - 80f
        canvas.drawText(attrStr, cardRect.centerX(), attrY, attrPaint)
        
        // App branding
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 32f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("BookTracker", width / 2f, height - 40f, brandPaint)

        // Save to cache dir and share
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "quote_share.png")
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
        context.startActivity(Intent.createChooser(shareIntent, "Share Quote"))
    }
}
