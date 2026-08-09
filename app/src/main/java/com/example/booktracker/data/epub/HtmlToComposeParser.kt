package com.example.booktracker.data.epub

import android.graphics.Typeface
import android.text.Html
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import androidx.core.text.HtmlCompat

object HtmlToComposeParser {
    fun parse(html: String): AnnotatedString {
        // Strip out HEAD/STYLE tags which HtmlCompat might render raw
        val bodyContent = html.substringAfter("<body", html)
            .substringAfter(">", html)
            .substringBefore("</body>", html)
        
        val spanned: Spanned = HtmlCompat.fromHtml(bodyContent, HtmlCompat.FROM_HTML_MODE_COMPACT)
        
        return buildAnnotatedString {
            append(spanned.toString())
            
            val spans = spanned.getSpans(0, spanned.length, Any::class.java)
            for (span in spans) {
                val start = spanned.getSpanStart(span)
                val end = spanned.getSpanEnd(span)
                
                if (start < 0 || end < 0 || start >= end) continue
                
                when (span) {
                    is StyleSpan -> {
                        when (span.style) {
                            Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                            Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                            Typeface.BOLD_ITALIC -> addStyle(
                                SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                                start,
                                end
                            )
                        }
                    }
                    is UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                    is StrikethroughSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
                    is RelativeSizeSpan -> addStyle(SpanStyle(fontSize = span.sizeChange.em), start, end)
                    is ForegroundColorSpan -> addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
                    // We can handle URL spans or headers here if needed
                }
            }
        }
    }
}
