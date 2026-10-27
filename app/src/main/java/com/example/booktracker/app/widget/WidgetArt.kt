package com.example.booktracker.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import java.time.LocalDate
import kotlin.math.min

/**
 * Bitmaps for what Glance can't draw by itself: progress rings, the year in
 * pixels, the week's reading dots and rounded covers. Rounded corners are baked
 * in because widget corner clipping only exists on Android 12 and later.
 */
object WidgetArt {

    /** A ring with [fraction] of it swept in [color] over a [track], starting at twelve o'clock. */
    fun ring(sizePx: Int, fraction: Float, track: Int, color: Int, strokeFraction: Float = 0.1f): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val stroke = sizePx * strokeFraction
        val inset = stroke / 2f + 1f
        val oval = RectF(inset, inset, sizePx - inset, sizePx - inset)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }
        paint.color = track
        canvas.drawArc(oval, 0f, 360f, false, paint)
        val sweep = 360f * fraction.coerceIn(0f, 1f)
        if (sweep > 0f) {
            paint.color = color
            canvas.drawArc(oval, -90f, sweep, false, paint)
        }
        return bitmap
    }

    /**
     * Week columns (oldest first, Monday on top) ending with the week of [today].
     * [levels] gives 0..4 for each reading day; days after today stay empty.
     */
    fun pixels(
        today: LocalDate,
        weeks: Int,
        levels: (LocalDate) -> Int,
        colors: IntArray,
        cellPx: Int,
        gapPx: Int
    ): Bitmap {
        val width = weeks * cellPx + (weeks - 1) * gapPx
        val height = 7 * cellPx + 6 * gapPx
        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = cellPx * 0.25f
        val monday = today.minusDays(today.dayOfWeek.value - 1L).minusWeeks(weeks - 1L)
        for (week in 0 until weeks) {
            for (day in 0 until 7) {
                val date = monday.plusDays(week * 7L + day)
                if (date.isAfter(today)) continue
                paint.color = colors[levels(date).coerceIn(0, colors.lastIndex)]
                val left = (week * (cellPx + gapPx)).toFloat()
                val top = (day * (cellPx + gapPx)).toFloat()
                canvas.drawRoundRect(left, top, left + cellPx, top + cellPx, radius, radius, paint)
            }
        }
        return bitmap
    }

    /** Monday-to-Sunday dots with day letters, filled on reading days; today gets a ring. */
    fun weekDots(
        today: LocalDate,
        readDays: Set<LocalDate>,
        dotPx: Int,
        filled: Int,
        empty: Int,
        label: Int,
        onFilled: Int
    ): Bitmap {
        val gap = dotPx / 3
        val width = 7 * dotPx + 6 * gap
        val bitmap = Bitmap.createBitmap(width, dotPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = dotPx * 0.42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val monday = today.minusDays(today.dayOfWeek.value - 1L)
        val letters = listOf("M", "T", "W", "T", "F", "S", "S")
        for (i in 0 until 7) {
            val date = monday.plusDays(i.toLong())
            val cx = i * (dotPx + gap) + dotPx / 2f
            val cy = dotPx / 2f
            val read = date in readDays
            paint.style = Paint.Style.FILL
            paint.color = if (read) filled else empty
            canvas.drawCircle(cx, cy, dotPx / 2f - 2f, paint)
            if (date == today && !read) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dotPx * 0.08f
                paint.color = filled
                canvas.drawCircle(cx, cy, dotPx / 2f - paint.strokeWidth, paint)
            }
            text.color = if (read) onFilled else label
            val bounds = Rect()
            text.getTextBounds(letters[i], 0, 1, bounds)
            canvas.drawText(letters[i], cx, cy + bounds.height() / 2f, text)
        }
        return bitmap
    }

    /** The cover cropped to fill [widthPx] x [heightPx] with rounded corners, or null if it can't load. */
    suspend fun cover(context: Context, url: String, widthPx: Int, heightPx: Int, cornerPx: Float): Bitmap? {
        if (url.isBlank()) return null
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(widthPx * 2, heightPx * 2)
            .allowHardware(false)
            .build()
        val source = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap() ?: return null
        val output = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val clip = Path().apply { addRoundRect(RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()), cornerPx, cornerPx, Path.Direction.CW) }
        canvas.clipPath(clip)
        // Centre-crop: scale until both sides cover, then trim the overflow evenly.
        val scale = maxOf(widthPx.toFloat() / source.width, heightPx.toFloat() / source.height)
        val drawnW = source.width * scale
        val drawnH = source.height * scale
        val left = (widthPx - drawnW) / 2f
        val top = (heightPx - drawnH) / 2f
        canvas.drawBitmap(source, null, RectF(left, top, left + drawnW, top + drawnH), Paint(Paint.FILTER_BITMAP_FLAG))
        return output
    }

    /** A soft placeholder when a book has no cover: its initial on a tinted card. */
    fun coverPlaceholder(title: String, widthPx: Int, heightPx: Int, cornerPx: Float, background: Int, foreground: Int): Bitmap {
        val output = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = background }
        canvas.drawRoundRect(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), cornerPx, cornerPx, paint)
        paint.apply {
            color = foreground
            textAlign = Paint.Align.CENTER
            textSize = min(widthPx, heightPx) * 0.45f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val letter = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        val bounds = Rect()
        paint.getTextBounds(letter, 0, 1, bounds)
        canvas.drawText(letter, widthPx / 2f, heightPx / 2f + bounds.height() / 2f, paint)
        return output
    }
}
