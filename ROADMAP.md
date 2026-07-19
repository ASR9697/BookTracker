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

## Explicitly dropped (do not build without a new ask)
Removed at the user's request / off-constraint — see [CLAUDE.md](CLAUDE.md) history:
Google Cast, Android Auto, Desktop PWA, Anime-canon bridge, and any Firebase/paid dependency.

## Suggested commit boundaries
One commit per feature (or per phase if small), e.g.:
`feat: reading-genome local recommendation carousel`. Use `git commit -F <file>` for multi-line
messages (PowerShell here-string quoting gotcha — see CLAUDE.md).
