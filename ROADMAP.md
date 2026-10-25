# Book Tracker — Implementation Roadmap (Phases 3–4)

This file is the working plan for the remaining pending features. Phases 1 and 2 are **done and
verified by build** (see the Completed section of [FEATURES.md](FEATURES.md)). Everything below
holds to the project's hard constraints: **zero paid services, no accounts, no new cloud
dependencies** — every feature here is pure-local computation or a free/keyless on-device library.

## How to use this file
1. Pick the next unchecked item **in order** (foundations first within each phase).
2. Re-read [CLAUDE.md](CLAUDE.md) conventions before coding: repository is the only thing that
   touches Room; every `Book`/`Session` mutation stamps `lastUpdated`/timestamps in epoch millis;
   no "what" comments; follow [UI_UX_GUIDELINES.md](UI_UX_GUIDELINES.md) (Material You colors only,
   haptics on meaningful actions, `animateContentSize`/`animateItem`, bottom sheets over dialogs).
2. Add the feature to FEATURES.md **Pending** before starting (it's already listed here).
3. Build after each nontrivial change: `.\gradlew.bat :app:assembleDebug` (and `:wear:assembleDebug`
   if the watch is touched). This is a Windows box — use the **PowerShell** tool with `.\gradlew.bat`.
4. On completion, move the item to FEATURES.md **Completed** with a 1–2 sentence description, and
   check it off here.

---

## Phase 3 — Engagement & Insights

All four features read existing data (`completedSessions`, `books`, `rating`, `genres`,
`environmentTag`) — no schema changes except where noted. Land them in this order.

### [x] 3.1 Gamification & Secret Badges
**Goal:** A hidden achievement system that unlocks glassmorphic badges from reading behavior.

- **New:** `analytics/BadgeEngine.kt` — a pure function
  `fun evaluate(books: List<Book>, sessions: List<Session>): Set<BadgeId>`. Keep badge definitions
  as a sealed list with `id`, `title`, `description`, `icon`, and a `predicate`. Starter set:
  - *Night Owl* — a session whose `startTime` falls between 00:00–04:00 local.
  - *Marathoner* — a single session ≥ 180 minutes (`endTime - startTime`).
  - *Finisher* — first book moved to `FINISHED`.
  - *Streak Keeper* — `StreakEngine` current streak ≥ 7.
  - *Polyglot Shelf* — books spanning ≥ 5 distinct genres.
  - *Marginalia* — ≥ 25 margin notes total.
- **UI:** a badge grid on `ProfileScreen` (locked badges show a silhouette + "???" per the "secret"
  framing). New unlocks fire a snackbar + `HapticFeedbackType.LongPress`.
- **Unlock detection:** persist the set of already-seen badge ids in `SettingsRepository`
  (DataStore, a `stringSetPreferencesKey`). Diff freshly-evaluated vs. seen in the ViewModel to
  drive the "new unlock" event; then store the union.
- **No schema change.** Everything derives from data already observed by the ViewModel.

### 3.2 "Book Tracker Wrapped"
**Goal:** [DONE] A shareable end-of-year/month summary graphic.

- **New:** `ui/WrappedScreen.kt` — a Compose layout that renders headline stats (books finished,
  pages/units read, total hours, best genre, longest streak, top environment tag) reusing
  `AnalyticsEngine` + `ReadingCalendar` aggregations. Add a period toggle (This Month / This Year).
- **Share:** capture the composable to a bitmap (`GraphicsLayer.toImageBitmap()` via
  `rememberGraphicsLayer()` / `Modifier.drawWithContent { … record }` in current Compose, or an
  offscreen `ComposeView` → `drawToBitmap()`), write the PNG to `cacheDir`, and share with
  `Intent.ACTION_SEND` + a `FileProvider` (`content://` uri, `image/png`).
- **Manifest:** add a `<provider android:name="androidx.core.content.FileProvider">` with a
  `file_paths.xml` exposing `cacheDir` (`res/xml/`). No new dependency (`androidx.core` already present).
- Reached from Profile (a "Your Wrapped" card) — keep the design bold and single-purpose, it's the
  one screen meant to be screenshotted.

### 3.3 Immersive Reading Timer (Pomodoro + ambient noise)
**Goal:** [DONE] A focus mode layered on the existing session start/end with optional white/brown noise.

- **New:** `journal/FocusTimer.kt` (state holder) and UI on the reading hero or a dedicated focus
  sheet: configurable work/break lengths (default 25/5), a large countdown, and a session that
  auto-starts/ends the reading session (`repository.startSession`/`endSession`) so pages still count.
- **Ambient sound (zero-cost):** generate noise on-device with `AudioTrack` in a small coroutine —
  white noise = uniform random samples; brown noise = integrated/low-passed white. **Do not** bundle
  or stream copyrighted lo-fi. Provide an off switch and a volume slider; stop and release the
  `AudioTrack` in `onPause`/when the timer stops (lifecycle-safe).
- Haptic pulse at phase transitions (work→break). No schema change.

### 3.4 "Reading Genome" (local recommendations)
**Goal:** [DONE] Surface backlog books to read next based on what the user rated highly.

- **New:** `analytics/ReadingGenome.kt` — a heuristic scorer, no ML dependency:
  - Build a taste profile from `FINISHED` books with a high average `rating` (≥ 4.0): weight their
    `genres` and `authors`.
  - Score each `BACKLOG`/`SHORTLIST` book by overlap with that profile; return the top N with a
    human reason string ("Because you loved *Dune*" / "More epic fantasy").
- **UI:** a "Recommended for you" carousel on Home or Library. Empty state when there aren't enough
  rated books yet ("Finish and rate a few books to unlock recommendations").
- Pure function over existing `books` — no schema change, no persistence.

**Build checkpoint:** after 3.1–3.4, run `.\gradlew.bat :app:assembleDebug` and fix before Phase 4.

---

## Phase 4 — System Integration

### 4.1 Android Home Screen Widgets (Glance)
**Goal:** [DONE] A home-screen widget showing current book progress with 1-tap +1/+10 logging.

- **New dependency (free):** `androidx.glance:glance-appwidget` (Jetpack Glance). Add to
  `app/build.gradle.kts`.
- **New:** `widget/ReadingWidget.kt` (`GlanceAppWidget`) + `ReadingWidgetReceiver`
  (`GlanceAppWidgetReceiver`), registered in the manifest with an `appwidget-provider` xml.
- **Data:** the widget reads the active book + today's pages. Because Glance runs outside the
  Activity, resolve the repository through `ServiceLocator` exactly like `WearSyncService` does, and
  drive quick-log via an `ActionCallback` calling `repository.addProgress(...)` — then
  `ReadingWidget.update(...)`. Keep all Room access in the repository.
- Match Material You: Glance supports `GlanceTheme` with dynamic color on Android 12+.
- Verify by installing the debug APK and adding the widget to a launcher.

### 4.2 "Deep Focus" Integration — **scoped down; confirm before building**
**Goal:** [DONE] **Reality check:** there is **no public Digital Wellbeing API**, and truly blocking other apps
requires an `AccessibilityService`, which is heavy and risks Play Store policy rejection for a
reading app. Do **not** build the accessibility-blocking version.

**Recommended v1 (no special risk):**
- Toggle **Do Not Disturb** on while a reading session is active via
  `NotificationManager.setInterruptionFilter(...)`. This needs the user to grant
  *notification policy access* (`ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`) — request it lazily
  with a clear explanation, and no-op gracefully if not granted.
- Optionally offer **screen pinning** (`startLockTask()`) as a "lock me into reading" button.
- Restore the prior interruption filter when the session ends.
- Surface as a Settings toggle ("Silence notifications during reading sessions") wired into
  `startSession`/`endSession`.

Because this changes device-level behavior (silencing notifications), **get a thumbs-up on the
reduced scope before implementing.**

---

## Phase 5 — Visual Excellence & Reading Immersion (Bookmory Flagship Parity)

These features build on top of our existing 100% offline, local-first Room + DataStore architecture with zero cloud dependencies.

### 5.1 Virtual 3D "Spine Bookshelf" View
- **Goal:** In addition to List and Grid library views, provide an interactive wooden/dark Bookshelf view.
- **Details:** Books stand vertically side-by-side with dynamic spine thickness proportional to `totalPages`. Spines are styled with cover-derived palettes or genre themes, vertical embossed metallic titles, and dangling bookmark ribbons for actively read books. Includes smooth pull-out selection animations.

### 5.2 Annual Reading Heatmap ("Year in Pixels")
- **Goal:** A GitHub-style 52-week horizontal calendar reading intensity matrix in `ReadingCalendarView` and `ProfileScreen`.
- **Details:** 5 intensity levels (Neutral, Soft Tint, Radiant Primary, High-Intensity Accent, Fiery Flame) reflecting pages read per day. Tap-to-inspect displays a day's logged reading sessions, completed books, and margin notes.

### 5.3 Single Book "Finished Wrap-Up" Victory Card
- **Goal:** A dedicated, shareable victory graphic card generated when a reader finishes a book.
- **Details:** High-res Canvas export (1:1 square & 9:16 story) featuring the book cover art with ambient blur backdrop, star rating breakdown, personal review snippet, total reading time, average velocity, and top highlighted quote.

### 5.4 Smart Reading Challenges & Goal Pace Forecaster
- **Goal:** Comprehensive goal projections and challenge tracking in `GoalEngine.kt`.
- **Details:** Pace ahead/behind gauge (e.g. *"You are 2 books ahead of your yearly target! Projected to finish 28 books"*), daily habit consistency percentage, and seasonal reading sprint challenges (Spring/Summer/Autumn 5-book sprints).

### 5.5 Library Loan Due Date Alerts & Lending Tracker
- **Goal:** Track library return deadlines and books lent to friends using the existing `LoanRecord` schema.
- **Details:** Visual due date countdown badges for library books (*"Due back to library in 3 days"*) and a lent-out register with contact names and loan dates.

---

## Explicitly dropped (do not build without a new ask)
Removed at the user's request / off-constraint — see [CLAUDE.md](CLAUDE.md) history:
Google Cast, Android Auto, Desktop PWA, Anime-canon bridge, and any Firebase/paid dependency.

## Suggested commit boundaries
One commit per feature (or per phase if small), e.g.:
`feat: reading-genome local recommendation carousel`. Use `git commit -F <file>` for multi-line
messages (PowerShell here-string quoting gotcha — see CLAUDE.md).
