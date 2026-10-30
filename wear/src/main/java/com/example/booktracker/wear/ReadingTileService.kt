package com.example.booktracker.wear

import android.content.ComponentName
import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.buttonGroup
import androidx.wear.protolayout.material3.circularProgressIndicator
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textButton
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture

/**
 * The tile beside the watch face: the book touched last (or the one a wrist
 * session is timing), how far through it you are, today's goal, and +1 / +10
 * that log pages without opening the app.
 */
class ReadingTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        SuspendToFutureAdapter.launchFuture {
            var shelf = WatchStore.loadShelf(this@ReadingTileService)
            parseAdd(requestParams.currentState.lastClickableId)?.let { (delta, bookId) ->
                val book = shelf?.books?.find { it.id == bookId } ?: return@let
                WatchStore.addPages(this@ReadingTileService, book, delta)
                WatchStore.refreshComplication(this@ReadingTileService)
                shelf = WatchStore.loadShelf(this@ReadingTileService)
            }
            val session = WristSessions.current(this@ReadingTileService)
            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout(requestParams, shelf, session)))
                .build()
        }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        SuspendToFutureAdapter.launchFuture {
            ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
        }

    private fun layout(
        request: RequestBuilders.TileRequest,
        shelf: Shelf?,
        session: WristSession?
    ): LayoutElementBuilders.LayoutElement = materialScope(
        this,
        request.deviceConfiguration,
        // The reader's palette from the phone, not the watch's own, to match the app.
        allowDynamicTheme = false,
        defaultColorScheme = tileColors(WatchPalette.of(shelf))
    ) {
        val openApp = ModifiersBuilders.Clickable.Builder()
            .setId(OPEN_ID)
            .setOnClick(ActionBuilders.launchAction(ComponentName(this@ReadingTileService, WearActivity::class.java)))
            .build()
        val book = session?.let { s -> shelf?.books?.find { it.id == s.bookId } } ?: shelf?.latest

        if (shelf == null || book == null) {
            primaryLayout(
                titleSlot = { text(getString(R.string.app_name).layoutString) },
                mainSlot = { text(getString(R.string.empty_shelf).layoutString, maxLines = 3) },
                bottomSlot = { textEdgeButton(onClick = openApp) { text(getString(R.string.open).layoutString) } }
            )
        } else {
            // Pages still to go on a deadline book, else today against the goal; the streak leads.
            val leftOnPlan = book.plan?.takeIf { !it.done && !it.overdue }?.leftToday(book)
            val today = when {
                leftOnPlan != null && leftOnPlan > 0 -> getString(R.string.tile_plan_to_go, leftOnPlan)
                shelf.goalMet -> getString(R.string.goal_met)
                shelf.dailyGoal > 0 -> getString(R.string.goal_progress, shelf.pagesToday, shelf.dailyGoal)
                else -> resources.getQuantityString(R.plurals.pages_today, shelf.pagesToday, shelf.pagesToday)
            }
            val goalLine = if (shelf.streak > 0) "🔥${shelf.streak} · $today" else today
            primaryLayout(
                titleSlot = { text(book.title.layoutString) },
                mainSlot = {
                    buttonGroup {
                        buttonGroupItem { pageButton(book, 1) }
                        buttonGroupItem { pageRing(book) }
                        buttonGroupItem { pageButton(book, 10) }
                    }
                },
                labelForBottomSlot = { text(goalLine.layoutString) },
                bottomSlot = {
                    textEdgeButton(onClick = openApp) {
                        val label = if (session != null) {
                            val minutes = (session.activeMillis(System.currentTimeMillis()) / 60_000L).toInt()
                            getString(R.string.minutes_short, minutes)
                        } else {
                            getString(R.string.open)
                        }
                        text(label.layoutString)
                    }
                }
            )
        }
    }

    private fun MaterialScope.pageButton(book: ActiveBook, delta: Int): LayoutElementBuilders.LayoutElement =
        textButton(
            onClick = ModifiersBuilders.Clickable.Builder()
                .setId("$ADD_PREFIX$delta:${book.id}")
                .setOnClick(ActionBuilders.LoadAction.Builder().build())
                .build(),
            labelContent = { text("+$delta".layoutString) }
        )

    /** How far through the book, with the page in the middle. */
    private fun MaterialScope.pageRing(book: ActiveBook): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .addContent(circularProgressIndicator(staticProgress = book.progress))
            .addContent(text(book.currentUnit.toString().layoutString, typography = Typography.NUMERAL_SMALL))
            .build()

    private fun tileColors(p: WatchPalette) = ColorScheme(
        primary = p.primary.argb,
        primaryDim = p.primaryDim.argb,
        primaryContainer = p.primaryContainer.argb,
        onPrimary = p.onPrimary.argb,
        onPrimaryContainer = p.onPrimaryContainer.argb,
        secondary = p.secondary.argb,
        secondaryContainer = p.secondaryContainer.argb,
        onSecondary = p.onSecondary.argb,
        onSecondaryContainer = p.onSecondaryContainer.argb,
        tertiary = p.secondary.argb,
        onTertiary = p.onSecondary.argb
    )

    /** "add:{delta}:{bookId}" from a +1 / +10 tap, or null for anything else. */
    private fun parseAdd(id: String): Pair<Int, String>? {
        if (!id.startsWith(ADD_PREFIX)) return null
        val parts = id.removePrefix(ADD_PREFIX).split(':', limit = 2)
        val delta = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val bookId = parts.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: return null
        return delta to bookId
    }

    private companion object {
        const val RESOURCES_VERSION = "1"
        const val ADD_PREFIX = "add:"
        const val OPEN_ID = "open"
    }
}
