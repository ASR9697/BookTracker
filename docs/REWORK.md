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
| [Home](#home) | `app/ui/HomeDashboard.kt` | Ready to build |
| [Reading timer](#reading-timer) | `app/ui/FocusTimerScreen.kt` | Ready to build |
| [Save session](#save-session) | `app/ui/SessionSaveScreen.kt` | Ready to build |

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

## Home

**File:** `app/ui/HomeDashboard.kt` (1195 lines), sections in `app/data/HomeLayout.kt`
· **Status:** Ready to build

**Goal:** Home is *today*: the book in your hands, today's goal, the streak, and what
needs you right now. It's in good shape (reorderable sections, swipe-the-cover
actions, celebration), so this is a tune-up, not a rebuild. Problems: navigation is
spread over two sections that overlap Profile (Finished, Wishlist), "Pages per month"
is a Stats breakdown, things that are about today (books planned for today, wished
books out today) never show, and the current-book card gets re-reads and non-page
books wrong.

### Decisions
- [x] "Pages per month": **hidden by default** (still available in Edit home). Only
      changes `HomeLayout.DEFAULT`; saved layouts keep their choice.
- [x] **Merge** "Wishlist & owned" and "Shortcuts" into one Shortcuts section:
      Calendar, Bookshelf, Wishlist, Owned, Gifts, plus Year in review once unlocked.
      Finished leaves Home (it's on Profile).
- [x] Today / year tiles: **tap opens the Goals screen**, not the edit-goal dialog.
- [x] **Add the Heads-up strip**, folding in the loan banner.
- [x] Extras: **A, C, D** (On this day, smart subtitle, "Can't decide?").

### Layout, top to bottom
1. **Greeting (kept):** name, today line. Subtitle stays unless extra C replaces it.
2. **Heads-up strip (new):** only what needs you today, hidden when empty:
   library loans due (today's `UrgentLoanBanner`, moved in), books planned for today
   (`Book.plannedDate`), wished-for books released today (`WishlistEngine.releasedOn`).
   One compact card, one line each, tap opens the book.
3. **Current book carousel (kept)** with the fixes below.
4. **Streak (kept).**
5. **Today & year (kept):** tap opens Goals.
6. **Daily review (kept).**
7. **Up next (kept):** plus extra D if chosen.
8. **Shortcuts (merged):** one section of tiles: Calendar, Bookshelf, Wishlist, Owned,
   Gifts, Year in review (only once unlocked, not a disabled tile). Two rows on a
   phone, or one scrolling row.
9. **Pages per month (optional):** hidden by default.
10. **Edit home screen (kept).**

`HomeSection`: `OWNERSHIP` folds into `SHORTCUTS`, new `HEADS_UP`. `HomeLayout.decode`
already drops unknown names and appends new ones, so saved layouts survive; check
that a saved `-OWNERSHIP` (hidden) doesn't hide the merged section by surprise.

### Fixes
- Current book "Day N · since …" counts from the book's first session ever, so a
  re-read shows the first read's date. Start from `readStartedAt`
  (`RereadMemories.currentReadStart`), and use `today` (day-start hour) instead of
  `LocalDate.now(zone)`.
- Current book shows "p. 120 / 300" for every book, but `progressUnit` can be
  Percentage, Chapter or Minute. Use `FormatAdaptabilityLayer.unitAbbrev`/`countLabel`.
- "+ Pages" opens `CustomProgressDialog` (a +N stepper), while `ProgressPickerSheet`
  is the app's one progress entry point. Switch to it and delete
  `CustomProgressDialog.kt` (Home is its only user).
- Cover drag backdrop `clip(MaterialTheme.shapes.small)` → `AppShape.XS`;
  celebration pill `RoundedCornerShape(50)` → `CircleShape`.
- "Year in review" shortcut shows disabled ("Unlocks after N reading days") while
  Profile hides Wrapped until earned; hide it on Home too.

### Extras
| # | Idea | Effort |
|---|---|---|
| A | "On this day": a book you finished or a highlight you saved on this date in past years (moved here from Profile extra B; it's a *today* thing) | Small |
| B | Tap the goal chip in the collapsed bar to log pages for the current book | Small |
| C | Smart subtitle: replace the generic line with one from your habits, e.g. "You usually read around 9 pm; 20 minutes reaches today's goal" (`bestTimeOfDay`, `SmartPlanningEngine`) | Small |
| D | "Can't decide?" button in Up next → the to-read swipe (`tbr_swipe`) | Small |
| E | Monday recap card: last week's pages and time against the week before | Small |
| F | Monthly badge challenge as one line under the streak | Small |

**Chosen: A, C, D.** B, E, F are parked.

Placement: A ("On this day") goes in the Heads-up strip as its last line, so today's
things stay in one card; C replaces the greeting subtitle; D is a text button beside
Up next's "See all".

### Files
- `app/ui/HomeDashboard.kt`: heads-up strip, merged shortcuts, card fixes, picker
  sheet; consider moving `CurrentBookCard` out to `app/ui/CurrentBookCard.kt`.
- `app/data/HomeLayout.kt` (+ `HomeLayoutTest`): `HEADS_UP`, fold `OWNERSHIP` into
  `SHORTCUTS`, `MONTHLY_PAGES` hidden by default.
- `app/ui/BookTrackerScreen.kt`: move the loan banner into the heads-up strip, pass
  `onOpenGoals`, drop `onOpenFinished` from Home.
- Delete `app/ui/CustomProgressDialog.kt`.
- Heads-up logic as a small pure function (e.g. `HeadsUp.today(books, today)`) in
  `app/analytics/` with a test.
- `npm run update-maps`.

### PRs
1. Fixes: re-read day line, progress units, picker sheet, shapes, Year in review.
2. Layout: heads-up strip, merged shortcuts, tiles → Goals, pages per month hidden.
3. Extras A (On this day, in the heads-up strip), C (smart subtitle), D ("Can't
   decide?" → `tbr_swipe`).

---

## Reading timer

**File:** `app/ui/FocusTimerScreen.kt` (1122 lines), service `app/service/TimerService.kt`
· **Status:** Ready to build

**Goal:** a quiet clock you can trust while you read. This is already the most polished
screen (pick-up card, page goal, "still reading?", zen, foldable and keyboard support),
so this is a tune-up. Problems: logging pages means tapping a small + once per page
with no way to jump, a half-built Break phase sits in the code, and a few project
rules are broken.

### Decisions
- [x] Break phase: **remove it.** (`TimerPhase.BREAK`, `breakMinutes`, its persistence
      and the break text exist, but `setTimerPhase` has no callers and there's no
      setting.) Extra B covers the real need.
- [x] **Tap the page number to open `ProgressPickerSheet`**; +/- stay for single pages.
- [x] Extras: **A, B, C, D, E** (all).

### Layout, top to bottom
1. **Top bar (kept):** minimise, sounds, zen, Done.
2. **Clock and status line (kept).**
3. **Pick-up card (kept)** before starting.
4. **Play button, countdown and page-goal chips (kept).**
5. **Book card:** page number becomes tappable (opens the progress picker); +/-
   buttons grow to 48dp touch targets. Note button kept.

### Fixes
- The breathing glow behind the play button is an infinite loop that ignores
  `rememberReduceMotion()` (AGENTS rule: loops stay still when it's on).
- Page stepper reads "p. 120 / 300" for every book; use
  `FormatAdaptabilityLayer.unitAbbrev` like the rest of the screen already does.
- +/- buttons are 32dp, below the 48dp touch target.
- Countdown and page-goal pickers are plain `ModalBottomSheet`s; small pickers should
  be `ActionSheet` with content.
- Hand-typed shapes: `RoundedCornerShape(50)` ×3 → `CircleShape`; sheet shapes
  `RoundedCornerShape(topStart = 28.dp…)` ×3 go away with the `ActionSheet` move.
- Hard-coded colours `Color(0xFF6EE7A0)` (break ring, goes with the break phase) and
  `Color(0xFF090D14)` (zen background) → theme tokens.

### Extras
| # | Idea | Effort |
|---|---|---|
| A | Volume keys turn the page (+1 / -1) while the timer is open; opt-in `AppPrefs` toggle. The screen already handles key events | Small |
| B | When a countdown ends: "+5 min" beside Save, to keep going without starting over | Small |
| C | Preview a sound while choosing it, even before the clock starts | Small |
| D | Zen mode fades the clock to a dim glow after 10 s; a tap brings it back | Small |
| E | Dictate a note from the note button (speech to text) | Small |

**Chosen: all (A-E).**

Notes:
- B ("+5 min") replaces the break flow: when the countdown ends the screen offers
  Save and "+5 min"; the button adds to `timeLeftSeconds` and keeps the same session.
- C: playing a sound while paused must stop when the sheet closes, unless the clock
  is running (`SoundscapeEngine`).
- D: the dim must respect reduce motion (no fade, just dim).
- E: `NoteSheet` already dictates (`RecognizerIntent`), so this is a long-press on the
  timer's note button that opens the sheet straight into dictation. Small, not Medium.

### Files
- `app/ui/FocusTimerScreen.kt`: page picker, button sizes, unit label, reduce motion,
  sheets → `ActionSheet`, shapes and colours; extras.
- Break removal: `TimerPhase` and its uses in `FocusTimerScreen.kt`,
  `BookTrackerViewModel.kt` (`setTimerPhase`, `breakMinutes`, phase restore),
  `SettingsRepository.kt` (`breakMinutesKey`, `activeTimerPhase`), `TimerService.kt`.
  Check `shared/.../Constants.kt` and the watch (`WatchStore`) for the phase too.
- Volume keys: `AppPrefs` field, toggle in `app/ui/SettingsScreen.kt`.
- `npm run update-maps`.

### PRs
1. Fixes + tappable page number.
2. Break phase removal.
3. Extras B ("+5 min") and D (zen dim): on-screen timer behaviour.
4. Extras A (volume keys, with its Settings toggle), C (sound preview), E (dictation
   long-press).

---

## Save session

**File:** `app/ui/SessionSaveScreen.kt` (879 lines) · **Status:** Ready to build

**Goal:** confirm what you read in a few seconds and get out. It works, but it never
shows *which* book you're saving, edits the same things in two places (a menu and the
rows), labels a time without saying it's the end time, forgets the places you type in,
and can save a book as finished with pages left (bug below).

### Decisions
- [x] **Open the page picker on arrival only when no pages were logged** during the
      session; the progress row still opens it.
- [x] **Save moves to a full-width bottom button**, "Discard session" as a text button
      under it; the top-bar Save and options menu go.
- [x] Extras: **A, B, C** (all).

### Layout, top to bottom
1. **Book header (new):** cover, title, "p. 120 → 164". Replaces the big "Save reading
   session" title and its subtitle.
2. **When:** one stamp "Today · 9:10 – 9:41 PM" (start and end), replacing the
   unlabelled date and time stamps. Tap to edit.
3. **Read time (kept).**
4. **Reading progress + pace banner (kept).**
5. **Status (kept).**
6. **Where you read:** your own past places first (from saved session tags, most used
   first), then the presets. Today typed places are never offered again.
7. **Join previous session (kept).**
8. **Focus (kept).**
9. **A line worth keeping (kept).**
10. **Save button + Discard**, per decision 2.

### Fixes
- **Bug:** choosing the last page sets the status to Finished, but choosing a lower
  page afterwards leaves Finished set, so the book is saved as finished with pages
  left. Only an auto-set Finished should follow the page; one the reader picked stays.
- The options menu repeats "Edit read time" and "Where you read", which are rows on
  the page; only Discard is unique (moves to the bottom, per decision 2).
- Time picker is an `AlertDialog`; use `ActionSheet` with the `TimePicker` inside.
- `RoundedCornerShape(50)` ×2 (Save button, outcome buttons) → `CircleShape`.
- Time left ("~40h to finish at this pace") comes from this one session's pace, which
  swings wildly on short sessions. Use the book's blended pace
  (`AnalyticsEngine.bookEstimatedMinutesLeft`), as Home and the timer do.

### Extras
| # | Idea | Effort |
|---|---|---|
| A | Pre-select the place you usually read at this time of day | Small |
| B | "Full note…" link under the quote field, opening `NoteSheet` (page, photo, scan) | Small |
| C | "Pick up tomorrow at 9 pm?" one-tap reminder, using the existing nudge workers | Medium |

**Chosen: all (A-C).**

Notes:
- A: a suggestion only; the place stays editable and an empty choice is allowed.
  Needs a few past sessions at a similar hour before it guesses.
- B: the quick field stays for one line; "Full note…" hands its text to `NoteSheet`
  so nothing typed is lost.
- C: a one-off reminder at the time you usually read (`bestTimeOfDay`), shown only
  when reminders are allowed (`Notifications.canPost`). Lives with the other nudge
  workers in `app/worker/NudgeWorkers.kt`; check it doesn't double up with the daily
  reading reminder from Goals.

### Files
- `app/ui/SessionSaveScreen.kt`: book header, when stamp, places, status fix, bottom
  Save, dialogs → `ActionSheet`, shapes, ETA.
- Past places: a small pure function over sessions (e.g. in `AnalyticsEngine`, next to
  `environmentStats`) with a test.
- `app/ui/BookTrackerScreen.kt` (focus/save routes): pass whether pages were logged
  in the session, for decision 1.
- `npm run update-maps`.

### PRs
1. Fixes: status bug, ETA, dialogs, shapes. Small; the status bug can ship first.
2. Layout: book header, when stamp, past places, bottom Save, page picker on arrival.
3. Extras A (usual place) and B (full note link).
4. Extra C (pick-up reminder) on its own, since it touches the workers.

Not reviewed yet: `SessionResultScreen.kt`, the recap shown after saving.

---

## Cross-page notes

- **Navigation split (Home ↔ Profile), decided:** Home's shortcuts are about your
  *shelf* (Calendar, Bookshelf, Wishlist, Owned, Gifts). Profile's grid is about
  your *record*: History, Finished, Planner, Journey map. Applied to the Profile
  layout below. AGENTS.md says the wishlist is reached from "Profile, the Home
  ownership tiles…": update that line when this ships.
- **"On this day"** is listed under Home (extra A), not Profile (old extra B).

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
