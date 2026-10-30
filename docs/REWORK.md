# Rework backlog

Changes we've agreed on but not built yet, one section per page. Plan a page here
first, then build it in small PRs. When an item ships, delete it (git history keeps
the record); when a page is done, remove its section and its row in the index.

Paths are relative to `app/src/main/java/com/example/booktracker/` unless noted.

## Pages

| Page | File | Status |
|---|---|---|
| [Profile](#profile) | `app/ui/ProfileScreen.kt` | Ready to build |
| [Stats](#stats) | `app/ui/AnalyticsScreen.kt` | Ready to build |

<!-- Status: Idea → Planned, decisions open → Ready to build → In progress -->

## Who owns what

Agreed split so pages stop repeating each other. Check new ideas against it.

| Page | Owns | Doesn't show |
|---|---|---|
| Home | Today: current book, today's ring, streak, glance at the year | History, breakdowns |
| Stats | Numbers over a chosen period: activity, rhythm, pace, what you read | Goal cards, book lists, personal records |
| Goals (`GoalsScreen`) | Every target and its progress, editing targets | Charts |
| Profile | Who you are: taste, favourites, top authors, records, achievements | Today, goal pace, currently reading |
| Finished | Every ended read, month by month | Charts |

---

## Profile

**File:** `app/ui/ProfileScreen.kt` · **Status:** Ready to build

**Goal:** Home is *today*, Stats is *numbers*, Profile is *who you are as a reader*
and your record. Today the page repeats Home (streak, yearly goal, currently
reading) and says little about the reader's taste.

### Decisions
- [x] **Drop "Currently reading"**; Home owns books in progress.
- [x] **Add the reading motto** under the name (one new setting, no DB change).
- [x] Extras: **A, C, D, E** (share card, pinned quote, year by year, reader level).
      F is parked; B moved to Home.

### Layout, top to bottom
1. **Header (trimmed):** avatar in goal ring, name, personality line, optional motto,
   "Tracking since". Remove the "x / y books in 2026 · on track" line (ring + Home
   already show it).
2. **Shortcut grid (new):** 2×2 tiles with live counts: History, Finished &
   abandoned, Planner, Journey map. Replaces the bottom "Your reading" list.
   (Wishlist lives on Home's shortcuts; see Cross-page notes.)
   "Reading goals" leaves the page (reach it via the gear).
3. **Favourites shelf (new):** covers of `isFavorite` books; falls back to
   top-rated finished books ("Your highest rated"). "Pick favourites" chip opens
   a sheet over finished books using the existing favourite toggle. Tap → book page.
4. **Reader card (new):**
   - Taste sentence, e.g. "Mostly fantasy and history, in ebooks, at night."
     Built from `AnalyticsEngine.topGenre`/`genreMix`, top format, `bestTimeOfDay`.
   - Top authors: up to 5 by books finished, with count; tap → Library search for
     that author. (Stats has no author view today.)
5. **Lifetime numbers (redesigned):** streak as a headline row with `LivingFlame`
   (still opens Streak); books · pages · time as one quiet row beneath. Fixes the
   cramped "day streak · best X" label.
6. **Records (new):** longest book finished, fastest read (days, from
   `FinishedShelf.reads`), most re-read (`readCount`), biggest reading day
   (`StreakEngine.pagesByDay`), longest session, languages read (only if > 1).
   Each names a book and opens it; hidden when there's no data.
7. **Kept:** Wrapped card, achievements teaser, heatmap.
8. **Footer:** "Export notes as Markdown" with a snackbar result, styled as an action.
   (Journey map moved into the shortcut grid.)

**Removed:** Currently reading.

### Fixes
- Name editor `AlertDialog` → `ActionSheet` (name + motto fields).
- `AvatarPickerSheet` plain `ModalBottomSheet` → `ActionSheet` with content.
- Selected-cover border `MaterialTheme.shapes.small` → `AppShape.XS`.
- Avatar cover picker lists wishlist books too; filter through `WishlistEngine.onShelf`.

### Extras
| # | Idea | Effort |
|---|---|---|
| A | Shareable profile card image (header, favourites, top authors, lifetime numbers); reuse Wrapped's `shareLayerImage` | Medium |
| B | ~~"On this day"~~ moved to Home extra A | — |
| C | Pinned quote: one favourite highlight (`MarginNote.isFavorite`) under the header, serif note style | Small |
| D | Year by year rows ("2026 · 34 books"), opening Wrapped for past years (needs `WrappedEngine` to take any year) | Medium / Large |
| E | Reader level title from lifetime pages, as a header chip | Small |
| F | Taste over time ("more non-fiction than ever this year") | Medium |

**Chosen: A, C, D, E.** F is parked.

Placement:
- A (share card): a share icon in the header actions beside the gear; the card shows
  avatar, name, motto, reader level, favourites, top authors and lifetime numbers.
- C (pinned quote): under the header. Defaults to the newest favourite highlight;
  long-press it to pick another (stored as a note id in settings, cleared if the note
  is deleted). Hidden when there are no favourite highlights.
- D (year by year): its own card after Records, one row per year with books and a
  mini bar. Tapping a row opens Wrapped for that year, which needs
  `WrappedEngine.summarize` and the `wrappedAnimated` route to take a year.
- E (reader level): a chip in the header beside the personality line. Thresholds
  from lifetime pages, kept in `ReaderProfileEngine` with tests.

### Files
- New `app/analytics/ReaderProfileEngine.kt` (`favourites`, `topAuthors`,
  `tasteSentence`, `records`) + tests in `app/src/test/.../analytics/`.
- New `app/ui/ProfileSections.kt` for the new cards (ProfileScreen is 823 lines).
- `app/ui/ProfileScreen.kt`, `app/ui/BookTrackerScreen.kt` (callbacks: journey map,
  author search, favourites toggle; drop `onOpenSettings`).
- Motto and pinned-quote note id: `app/data/SettingsRepository.kt` →
  `app/ui/BookTrackerViewModel.kt` → `settings` block in `app/data/BackupEngine.kt`.
- Year by year: `app/analytics/WrappedEngine.kt` (any year), `app/ui/WrappedScreen.kt`
  and the `wrappedAnimated` route in `BookTrackerScreen.kt` (year argument).
- Share card: reuse `shareLayerImage` from `app/ui/WrappedScreen.kt`.
- `npm run update-maps` after adding files.

### PRs
1. Clean-up: shortcut grid, remove duplicates, lifetime row, export feedback,
   journey link, fixes.
2. Reader identity: engine + tests, favourites shelf, reader card, records, motto.
3. Extras C (pinned quote) and E (reader level): small, header-only.
4. Extra A (share card), after PR 2 so the card has favourites and authors to show.
5. Extra D (year by year), including Wrapped for past years.

---

## Stats

**File:** `app/ui/AnalyticsScreen.kt` (1162 lines) · **Status:** Ready to build

**Goal:** Stats is *numbers over a period you choose*. Today the top half is four
goal cards (yearly target, monthly target, seasonal sprint, habit consistency),
which Home and the Goals screen already show, so the yearly goal appears on three
pages. The real stats are fixed windows (last 7 days, this month, lifetime) with no
way to look at a week, a month or a past year. Two pie charts, and a "Recently
finished" list of every finished book with no limit.

### Decisions
- [x] Remove the goal cards from Stats, leaving one "Goals" link row. **Yes.**
- [x] Seasonal sprint: **drop it.** (Hard-coded 5 books, northern-hemisphere seasons,
      "Dec 1 – Feb 28" in leap years; the monthly badge challenge fills that role.)
      Delete `GoalEngine.calculateSeasonalSprint`, `currentSeason`, `Season`,
      `SeasonalSprint` and their tests in `GoalEngineTest`; nothing else uses them.
      Keep `calculateHabitConsistency` for the Rhythm card.
- [x] Remember the last period chosen (an `AppPrefs` field). **Yes.**
- [x] Extras: **A only** (compare with last year).

### Layout, top to bottom
1. **Header line:** follows the period ("4,210 pages read in October").
2. **Period switcher (new):** Week · Month · Year · All, with ‹ › to step back
   (last month, 2025…). Everything below follows it.
3. **Summary strip (new):** books finished · pages · time · active days, each with a
   change against the previous period ("+12% vs September").
4. **Activity chart:** bars, not the smooth line (days in a week or month, months in
   a year, years in All). Pages / minutes toggle stays. Tap a bar for its numbers.
   Replaces the 7-day line chart.
5. **Rhythm (merged):** day-of-week × time-of-day grid of when you read, with
   consistency ("active 21 of 31 days") and best time. Replaces the Reading habit
   card and the "Best time" tile.
6. **Pace:** speed (p/h), average session, longest session, average days to finish
   a book (`FinishedShelf.reads`). Replaces the cramped 4-across Reading time row
   (its "This week" is now the summary strip).
7. **Focus & places (merged):** the Focus card and the Environments card in one,
   since both answer "where and when do I read best".
8. **What you read:** of books finished (or read in) the period:
   - genres as horizontal bars (pies with 7 slices are hard to read), "Tidy tags" kept;
   - formats as one stacked bar;
   - length: short (<200 pages) / medium / long (400+);
   - ratings: average and a 1-5 star spread;
   - finished vs abandoned.
9. **Finished in this period:** a row of covers with "See all" → Finished screen.
   Replaces the unlimited list.
10. **Goals link:** one row, e.g. "23 / 40 books this year · on track →", opens Goals.

**Removed:** yearly target card, monthly target card, seasonal sprint, habit card,
the full "Recently finished" list.

### Fixes
- Genres and Formats count every book, including wishlist and to-read: Stats gets
  the full `books` list (`BookTrackerScreen.kt` ~767), not `WishlistEngine.onShelf`.
  Count books on the shelf, and for "what you read" only finished/reading ones.
- "Recently finished" uses `BookCover(url = …)`; use `BookCover(book, modifier)` so
  coverless books get a typeset cover.
- Sprint bar `RoundedCornerShape(4.dp)` → `AppShape`/`CircleShape` (goes away if the
  sprint is dropped).
- Month title built from `today.month.name.lowercase()` and hard-coded season date
  strings: use `getDisplayName(TextStyle.FULL, locale)` / formatted dates.
- Empty-state copy points to "Home"; say what to do instead ("Log a page or time a
  session…").

### Extras
| # | Idea | Effort |
|---|---|---|
| A | Compare with last year: last year's line faint behind this year's on the Year chart | Small |
| B | Speed trend: pages/hour per month, "you're 15% faster than in spring" | Small |
| C | Series: series started / completed, next book you're missing (reuse `WishlistEngine.seriesGaps`) | Medium |
| D | Money: spent on books this period (`WishlistEngine.spentIn`) next to books finished | Small |
| E | Notes: highlights and notes per period, words learned (`vocab_words`) | Small |
| F | Custom date range in the period switcher | Medium |
| G | Share a stats card for the chosen period (same renderer as Profile extra A) | Medium |
| H | Shelf facts: publication decades, languages, oldest book read | Small |

**Chosen: A.** B-H are parked; keep them here in case we come back to them.

### Files
- New `app/analytics/PeriodStats.kt`: `Period` (week/month/year/all + offset),
  `range`, `summary` with previous-period delta, `activityBuckets`, `rhythmGrid`,
  `lengthBuckets`, `ratingSpread`, `finishedVsAbandoned`; tests in
  `app/src/test/.../analytics/`. Reuse `StreakEngine.pagesByDay`,
  `AnalyticsEngine.minutesByDay`/`genreMix`, `FinishedShelf`.
- `app/ui/AnalyticsScreen.kt`: rebuild; move cards into `app/ui/StatsCards.kt` to get
  the file under control. Replace `SmoothLineChart` with a bar chart; `PieChart` can
  go once nothing uses it.
- `app/ui/BookTrackerScreen.kt`: pass `onShelf(books)`, add `onOpenGoals` /
  `onOpenFinished`; drop goal-dialog wiring if the goal cards go.
- Period memory: `AppPrefs` field (`PrefKeys`/`readPrefs`/`updatePrefs`).
- `npm run update-maps`.

### PRs
1. Fixes: onShelf filtering, `BookCover(book)`, copy and locale fixes. Small and safe,
   can ship before the rework.
2. Period engine + tests, period switcher, summary strip, bar chart.
3. Rhythm, pace, focus & places merge, "what you read" section, finished covers row,
   goals link; remove the goal cards and the seasonal sprint code.
4. Extra A: last year's line behind this year's on the Year chart.

---

## Cross-page notes

- **Navigation split (Home ↔ Profile), decided:** Home's shortcuts are about your
  *shelf* (Calendar, Bookshelf, Wishlist, Owned, Gifts). Profile's grid is about
  your *record*: History, Finished, Planner, Journey map. Applied to the Profile
  layout below. AGENTS.md says the wishlist is reached from "Profile, the Home
  ownership tiles…": update that line when this ships.
- **"On this day"** is listed under Home (extra A), not Profile (old extra B).
- **Not reviewed yet:** `SessionResultScreen.kt`, the recap shown after saving a session.

---

<!--
New page template: copy below, add a row to the Pages table.

## <Page name>

**File:** `app/ui/<Screen>.kt` · **Status:** Idea

**Goal:** one or two lines on what this page is for and what's wrong today.

### Decisions
- [ ] ...

### Layout, top to bottom
1. ...

### Fixes
- ...

### Extras
| # | Idea | Effort |
|---|---|---|

### Files
- ...

### PRs
1. ...
-->
