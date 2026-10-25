package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.LoanRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Pure logic engine for library return dates, due date countdowns,
 * lending tracking, and urgency evaluation.
 */
object LoanEngine {

    enum class LoanUrgency {
        NORMAL,      // Future return date (> 3 days) or open-ended loan
        DUE_SOON,    // Due within 3 days (0..3 days)
        OVERDUE,     // Past due date (< 0 days)
        RETURNED     // Completed / Returned loan
    }

    data class LoanStatusInfo(
        val record: LoanRecord,
        val isLentOut: Boolean,
        val urgency: LoanUrgency,
        val daysRemaining: Long?,
        val badgeText: String,
        val detailText: String
    )

    /**
     * Returns the currently active (unreturned) loan for a book, or null if none.
     */
    fun activeLoan(book: Book): LoanRecord? {
        return book.loanRecord.lastOrNull { it.isActive }
    }

    /**
     * Evaluates loan status, urgency, countdown, and human-readable badges.
     */
    fun getLoanStatus(
        record: LoanRecord,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): LoanStatusInfo {
        if (record.isReturned) {
            val returnLocalDate = record.returnDate?.let {
                Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
            }
            val returnStr = returnLocalDate?.toString() ?: "Returned"
            return LoanStatusInfo(
                record = record,
                isLentOut = record.isLentOut,
                urgency = LoanUrgency.RETURNED,
                daysRemaining = null,
                badgeText = "Returned",
                detailText = "Returned on $returnStr"
            )
        }

        if (record.dueDate <= 0L) {
            val badge = if (record.isLentOut) "Lent (${record.lender})" else "Borrowed (${record.lender})"
            val detail = if (record.isLentOut) "Lent to ${record.lender} · No due date" else "Borrowed from ${record.lender} · No due date"
            return LoanStatusInfo(
                record = record,
                isLentOut = record.isLentOut,
                urgency = LoanUrgency.NORMAL,
                daysRemaining = null,
                badgeText = badge,
                detailText = detail
            )
        }

        val dueLocalDate = Instant.ofEpochMilli(record.dueDate).atZone(zone).toLocalDate()
        val daysDiff = ChronoUnit.DAYS.between(today, dueLocalDate)

        val urgency = when {
            daysDiff < 0 -> LoanUrgency.OVERDUE
            daysDiff <= 3 -> LoanUrgency.DUE_SOON
            else -> LoanUrgency.NORMAL
        }

        val badgeText = when {
            daysDiff < 0 -> "Overdue by ${-daysDiff}d"
            daysDiff == 0L -> "Due today!"
            daysDiff == 1L -> "Due tomorrow"
            daysDiff <= 3L -> "Due in ${daysDiff}d"
            record.isLentOut -> "Lent (${record.lender})"
            else -> "Due ${dueLocalDate.monthValue}/${dueLocalDate.dayOfMonth}"
        }

        val detailText = when {
            record.isLentOut -> {
                when {
                    daysDiff < 0 -> "Overdue from ${record.lender} by ${-daysDiff} day${if (-daysDiff > 1) "s" else ""}"
                    daysDiff == 0L -> "Expected back from ${record.lender} today"
                    daysDiff == 1L -> "Expected back from ${record.lender} tomorrow"
                    else -> "Lent to ${record.lender} · Due in $daysDiff days"
                }
            }
            else -> {
                when {
                    daysDiff < 0 -> "Overdue to ${record.lender} by ${-daysDiff} day${if (-daysDiff > 1) "s" else ""}"
                    daysDiff == 0L -> "Due back to ${record.lender} today"
                    daysDiff == 1L -> "Due back to ${record.lender} tomorrow"
                    else -> "Due back to ${record.lender} in $daysDiff days (${dueLocalDate.month.name.take(3)} ${dueLocalDate.dayOfMonth})"
                }
            }
        }

        return LoanStatusInfo(
            record = record,
            isLentOut = record.isLentOut,
            urgency = urgency,
            daysRemaining = daysDiff,
            badgeText = badgeText,
            detailText = detailText
        )
    }

    /**
     * Returns all books with their active loan record.
     */
    fun allActiveLoans(books: List<Book>): List<Pair<Book, LoanRecord>> {
        return books.mapNotNull { book ->
            activeLoan(book)?.let { book to it }
        }
    }

    /**
     * Filters active loans that are urgent (Overdue or Due within 3 days),
     * sorted with overdue items first.
     */
    fun urgentLoans(
        books: List<Book>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<Pair<Book, LoanStatusInfo>> {
        return allActiveLoans(books).mapNotNull { (book, loan) ->
            val status = getLoanStatus(loan, today, zone)
            if (status.urgency == LoanUrgency.OVERDUE || status.urgency == LoanUrgency.DUE_SOON) {
                book to status
            } else null
        }.sortedBy { it.second.daysRemaining ?: Long.MAX_VALUE }
    }
}
