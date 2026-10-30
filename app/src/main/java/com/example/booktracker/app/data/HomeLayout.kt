package com.example.booktracker.app.data

/**
 * Home sections the reader can reorder or hide. The greeting and the current-book
 * cards always stay on top, so they are not listed here.
 */
enum class HomeSection(val title: String, val description: String) {
    STREAK("Streak", "Current streak and this week"),
    GOALS("Today & year goals", "Pages today and books this year"),
    REVIEW("Daily review", "A few highlights and words to remember each day"),
    MONTHLY_PAGES("Pages per month", "This year's reading, month by month"),
    UP_NEXT("Up next", "Books waiting to be read"),
    SHORTCUTS("Shortcuts", "Calendar, bookshelf, wishlist, owned and gifts")
}

/** Order of every section plus the ones the reader hid. */
data class HomeLayout(
    val order: List<HomeSection>,
    val hidden: Set<HomeSection> = emptySet()
) {
    val visible: List<HomeSection> get() = order.filterNot { it in hidden }

    /** "STREAK,-GOALS,..." where a leading '-' marks a hidden section. */
    fun encode(): String = order.joinToString(",") { (if (it in hidden) "-" else "") + it.name }

    fun move(from: Int, to: Int): HomeLayout {
        if (from !in order.indices || to !in order.indices || from == to) return this
        return copy(order = order.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun toggle(section: HomeSection): HomeLayout =
        copy(hidden = if (section in hidden) hidden - section else hidden + section)

    companion object {
        /** Pages per month starts hidden: Stats owns breakdowns, Home is today. */
        val DEFAULT = HomeLayout(HomeSection.entries.toList(), hidden = setOf(HomeSection.MONTHLY_PAGES))

        /**
         * Unknown or repeated names are dropped and sections added in later versions
         * are appended in default order, so old settings and backups keep working.
         */
        fun decode(raw: String?): HomeLayout {
            if (raw.isNullOrBlank()) return DEFAULT
            val order = mutableListOf<HomeSection>()
            val hidden = mutableSetOf<HomeSection>()
            raw.split(",").forEach { token ->
                val name = token.trim().removePrefix("-")
                val section = HomeSection.entries.firstOrNull { it.name == name } ?: return@forEach
                if (section in order) return@forEach
                order += section
                if (token.trim().startsWith("-")) hidden += section
            }
            order += HomeSection.entries.filterNot { it in order }
            return HomeLayout(order, hidden)
        }
    }
}
