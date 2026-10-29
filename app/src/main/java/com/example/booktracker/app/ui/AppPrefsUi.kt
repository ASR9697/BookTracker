package com.example.booktracker.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.example.booktracker.app.data.AppPrefs
import java.time.DayOfWeek
import java.time.LocalDate

/** The reader's finer preferences, provided once at the root for screens that read one or two of them. */
val LocalAppPrefs = staticCompositionLocalOf { AppPrefs() }

/** Body text for notes and quotes: a serif face when the reader chose one in Settings. */
@Composable
fun noteTextStyle(): TextStyle {
    val base = MaterialTheme.typography.bodyMedium
    return if (LocalAppPrefs.current.serifNotes) base.copy(fontFamily = FontFamily.Serif, fontSize = base.fontSize * 1.05f) else base
}

/** The first day of the week [this] falls in, when weeks begin on [first]. */
fun LocalDate.startOfWeek(first: DayOfWeek): LocalDate =
    minusDays(((dayOfWeek.value - first.value + 7) % 7).toLong())

/** Empty cells before day 1 in a month grid whose columns begin on [first]. */
fun leadingDays(firstOfMonth: LocalDate, first: DayOfWeek): Int =
    (firstOfMonth.dayOfWeek.value - first.value + 7) % 7

/** The seven days of a week in column order. */
fun weekDays(first: DayOfWeek): List<DayOfWeek> = (0L..6L).map { first.plus(it) }
