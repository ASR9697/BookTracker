package com.example.booktracker.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchStoreTest {

    private val now = 1_000_000L
    private val book = ActiveBook(id = "a", title = "Dune", currentUnit = 100, totalUnits = 400, updatedAt = 500L)
    private val shelf = Shelf(books = listOf(book), dailyGoal = 30, pagesToday = 10, dayEndsAt = now + 60_000)

    @Test
    fun `an unconfirmed tap shows over the phone's page and counts toward today`() {
        val (shown, retired) = WatchStore.overlay(shelf, mapOf("a" to LocalPage(105, at = 900L)), now)
        assertEquals(105, shown.books.single().currentUnit)
        assertEquals(900L, shown.books.single().updatedAt)
        assertEquals(15, shown.pagesToday)
        assertTrue(retired.isEmpty())
    }

    @Test
    fun `the phone's echo of the tap retires it`() {
        // The phone stamps the book with the watch's own time when it applies the tap.
        val echoed = shelf.copy(books = listOf(book.copy(currentUnit = 105, updatedAt = 900L)), pagesToday = 15)
        val (shown, retired) = WatchStore.overlay(echoed, mapOf("a" to LocalPage(105, at = 900L)), now)
        assertEquals(105, shown.books.single().currentUnit)
        assertEquals(15, shown.pagesToday)
        assertEquals(setOf("a"), retired)
    }

    @Test
    fun `a newer change on the phone beats an older tap`() {
        val moved = shelf.copy(books = listOf(book.copy(currentUnit = 120, updatedAt = 950L)))
        val (shown, retired) = WatchStore.overlay(moved, mapOf("a" to LocalPage(105, at = 900L)), now)
        assertEquals(120, shown.books.single().currentUnit)
        assertEquals(setOf("a"), retired)
    }

    @Test
    fun `a tap for a book no longer being read is retired`() {
        val (_, retired) = WatchStore.overlay(shelf, mapOf("gone" to LocalPage(3, at = 900L)), now)
        assertEquals(setOf("gone"), retired)
    }

    @Test
    fun `today's pages start again once the phone's reading day is over`() {
        val (shown, _) = WatchStore.overlay(shelf.copy(dayEndsAt = now - 1), emptyMap(), now)
        assertEquals(0, shown.pagesToday)
    }

    @Test
    fun `taking pages back never makes today negative`() {
        val (shown, _) = WatchStore.overlay(shelf, mapOf("a" to LocalPage(80, at = 900L)), now)
        assertEquals(0, shown.pagesToday)
    }

    @Test
    fun `the phone clock counts from its anchor`() {
        val countdown = PhoneTimer("a", running = true, countdown = true, onBreak = false, seconds = 600, anchor = now + 300_000)
        assertEquals(300, countdown.secondsAt(now))
        assertEquals(0, countdown.secondsAt(now + 400_000))

        val stopwatch = PhoneTimer("a", running = true, countdown = false, onBreak = false, seconds = 0, anchor = now - 90_000)
        assertEquals(90, stopwatch.secondsAt(now))

        val paused = stopwatch.copy(running = false, seconds = 42)
        assertEquals(42, paused.secondsAt(now + 999_000))
    }

    @Test
    fun `pages to go on a plan count taps the phone hasn't seen`() {
        val plan = FinishPlan(perDay = 20, readToday = 5, targetDay = 20_400, status = "ON_TRACK")
        val planned = book.copy(plan = plan)
        assertEquals(15, plan.leftToday(planned))
        // Four pages tapped here: the phone still says 100, the watch shows 104.
        assertEquals(11, plan.leftToday(planned, shownPage = 104))
        // Never more than the book has left.
        assertEquals(3, plan.leftToday(planned.copy(currentUnit = 397, syncedUnit = 397)))
    }

    @Test
    fun `a new day clears a plan's pages read today`() {
        val plan = FinishPlan(perDay = 20, readToday = 18, targetDay = 20_400, status = "ON_TRACK")
        val planned = shelf.copy(books = listOf(book.copy(plan = plan)), dayEndsAt = now - 1)
        val (shown, _) = WatchStore.overlay(planned, emptyMap(), now)
        assertEquals(0, shown.books.single().plan?.readToday)
    }

    @Test
    fun `a countdown stops counting at its length`() {
        val countdown = WristSession("s", "a", "Dune", 100, startedAt = 0, banked = 0, runningSince = now, targetMillis = 600_000)
        assertEquals(300_000L, countdown.remainingMillis(now + 300_000))
        assertEquals(600_000L, countdown.activeMillis(now + 900_000))
        assertEquals(0L, countdown.remainingMillis(now + 900_000))
        assertTrue(countdown.expired(now + 600_000))
        assertEquals(null, countdown.copy(targetMillis = null).remainingMillis(now))
    }

    @Test
    fun `a wrist session only counts time while it runs`() {
        val running = WristSession("s", "a", "Dune", startPage = 100, startedAt = 0, banked = 60_000, runningSince = now)
        assertEquals(90_000, running.activeMillis(now + 30_000))
        assertEquals(60_000, running.copy(runningSince = null).activeMillis(now + 30_000))
    }
}
