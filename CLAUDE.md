# Book Tracker

Cross-platform book tracking app: phone/tablet app (`:app`, Jetpack Compose + Material 3) and a
Wear OS quick logger (`:wear`, Compose for Wear OS), sharing pure-Kotlin models through `:shared`.
Full product spec: [CrossPlatformBookTracker_Blueprint.md](CrossPlatformBookTracker_Blueprint.md).
Session-by-session history: [PROGRESS.md](PROGRESS.md). User-facing state: [README.md](README.md).
Feature Checklist (Completed & Pending): [FEATURES.md](FEATURES.md).

## Cost constraint — read this first

**The user explicitly does not want to pay for anything right now.** No Firebase, no Claude/API
calls from the app, no paid services of any kind. This is why Firebase was removed after being
scaffolded in — see "History" below. Do not reintroduce a paid dependency without asking first,
even if the blueprint calls for it. Google Books lookup and ML Kit barcode scanning are fine
because they're free and keyless.

## Architecture Overview
The app uses a strict **local-first** architecture with a Room Database, synchronized to the Wear OS app via the Bluetooth Data Layer using a Last-Write-Wins conflict resolution strategy. 

For full details on the architecture (including how we are future-proofing for Firebase integration without rewriting the UI), please see: **[ARCHITECTURE.md](ARCHITECTURE.md)**

## Module layout

- `:shared` — pure Kotlin, **zero dependencies on purpose**. Data models (`Book`, `Session`,
  `MarginNote`, `DnfData`, `BookStatus`) and the Data Layer path/key constants. Keep it this way;
  if a model needs a platform type (e.g. `Timestamp`), that's a sign it belongs in `:app` instead,
  not a reason to add a dependency here.
- `:app` — phone/tablet. Compose UI, Room persistence (`data/local/`), repository (`data/`),
  Google Books client (`data/remote/`), Wear sync (`sync/`), ViewModel + screens (`ui/`).
- `:wear` — standalone Wear OS app. `WearActivity` owns both the ambient-mode UI and the
  Data Layer listener for `/active_book` (registers/unregisters `DataClient.OnDataChangedListener`
  in `onResume`/`onPause`).

## What's actually implemented vs. stubbed

**CRITICAL RULE: Before implementing ANY new feature, you MUST check [FEATURES.md](FEATURES.md). If the feature is not listed there under "Pending", add it to the pending list FIRST before starting any work on it. Furthermore, when a feature is completed, you MUST write a 1-2 sentence detailed description of it in the Completed section of `FEATURES.md` outlining exactly what it does and how it works.**

Working end-to-end and verified by building + running:
- Add books manually, or scan an ISBN barcode (CameraX + ML Kit `BarcodeAnalyzer`, filtered to
  `TYPE_ISBN`) with auto-fill from the free Google Books volumes endpoint
  (`data/remote/GoogleBooksClient.kt` — no API key, keyless `q=isbn:` query).
- CSV Import: Bulk import books from Goodreads or StoryGraph CSV exports (`data/CsvImportEngine.kt`,
  invoked from `BookRepository.importCsv`), reached from Settings.
- TBR pipeline: Backlog → Shortlist → Up Next → Reading. Cards promote via a button and
  **swipe-to-dismiss to remove, with an Undo Snackbar** (`repository.restore` re-inserts the
  exact entity, preserving id/status/progress/lastUpdated — see below). Each pipeline tab shows a
  live count pill next to its label.
- Reading hero card: +1/+10 page progress, Finish (opens a Pacing/Focus/Vibe rating dialog), and
  Give up (opens a DNF dialog: abandon-% slider auto-filled from progress + reason chips).
- Completion outcomes (Phase 2 §7A): `repository.finishBook(rating)` / `markDnf(pct, reason)` write
  to the existing `Book.rating` map and `Book.dnfData` (no schema change; rating is JSON in Room).
  Finished + DNF books live in `ui/FinishedScreen.kt` (overflow → "Finished books"), which is the
  only place they're visible — the pipeline tabs are TBR-only (Backlog/Shortlist/Up Next).
- Phone chrome & styling: `CenterAlignedTopAppBar` (title + Scan action icon), an `ExtendedFloatingActionButton`
  ("Add book"), `PrimaryTabRow`. Theming (`ui/theme/Theme.kt`, `Color.kt`, `Type.kt`) is
  **dynamic-color-first** (Material You on Android 12+) with a hand-tuned Indigo/Teal/Rose slate
  palette as the pre-Android-12 fallback — every M3 container/tonal-surface slot is filled in
  explicitly on that fallback scheme so it doesn't silently mix in default M3 purple. The reading
  hero is an `ElevatedCard` whose gradient is derived from the active scheme
  (`primaryContainer` → `tertiaryContainer`, `LocalContentColor` set to `onPrimaryContainer`), not a
  hardcoded color pair, so it tracks dynamic color and light/dark automatically; it also shows a
  streak badge pill and a live progress percentage. Empty pipeline states and Settings/Analytics
  sections use `surfaceContainer`-toned cards instead of bare text on the page background.
- Watch: shows active book, +1/+10 with haptics, syncs back to the phone's Room DB via
  `WearSyncService` even when the phone app is closed. UI is **Wear Compose Material 3**
  (`AppScaffold`/`ScreenScaffold` giving `TimeText`, M3 components) with dynamic color from the
  watch face (`dynamicColorScheme(context)`, null-fallback to `ColorScheme()`). Ambient mode
  bypasses the scaffold entirely and stays mostly black (burn-in + power).
- Session recording & Environment Tagging: explicit Start/End session on the reading hero. Ending
  a session prompts the user to tag their environment/beverage (e.g., "☕ Coffee Shop", "🌧️ Rainy Day").
  Any +1/+10 delta made with **no** session open — on phone or watch — is recorded as a
  self-contained mini-session so it still counts toward streaks.
- Streaks: `analytics/StreakEngine.kt` computes consecutive days meeting a daily page goal
  (constant `DEFAULT_DAILY_GOAL_PAGES = 20` until a settings screen exists) from completed
  sessions; shown on the hero card with today's page count.
- Settings (`ui/SettingsScreen.kt`, reached from the top-app-bar overflow menu): daily reading goal
  via a slider, persisted with `SettingsRepository` over DataStore Preferences. The goal flows into
  streaks and analytics — the ViewModel `combine`s it with sessions, so changing it re-derives the
  streak live. `StreakEngine.DEFAULT_DAILY_GOAL_PAGES` is now only the fallback before DataStore loads.
- Cover images: Coil `AsyncImage` shows book covers (from the Google Books cover URL) in the reading
  hero and pipeline cards, with a `MenuBook` placeholder when the URL is blank (manual adds). Coil's
  OkHttp network fetcher is registered explicitly in `BookTrackerApplication` (a
  `SingletonImageLoader.Factory`), not via service-loader auto-registration.
- Analytics (`ui/AnalyticsScreen.kt`, reached from the top-app-bar overflow menu): the blueprint's
  7×52 calendar heatmap of pages-per-day. Pure aggregation in `analytics/ReadingCalendar.kt`; the
  color ramp is a **sequential single hue derived from the Material scheme** (`surfaceVariant` for
  empty, `primaryContainer`→`primary` for levels 1–4), so it tracks dynamic color + light/dark. Has
  stat tiles (pages / active days / best day), a Less→More legend, and tap-a-day-to-see-pages.
  It also includes an **Environments & Beverages** section that correlates reading volume with the
  tags saved at the end of each session.
- Immersive Reading Timer: Start a session via a bottom sheet to track reading live with **Soundscapes** (e.g., Lofi, Rain, Fireplace) and haptic pulsing across work/break phases.
- Reading Genome (Local Recommendations): A heuristic scorer (in `analytics/ReadingGenome.kt`) that analyzes the user's highly-rated finished books and cross-references authors and genres against books in their backlog, generating a personalized "Recommended for you" carousel on the Home screen.
- Android Home Screen Widget (Glance): `ReadingWidget` built with Jetpack Glance provides a launcher widget to track progress and log +1/+10 pages instantly via `ServiceLocator`.
- Deep Focus Mode: Intercepts and blocks Android system notifications (via Do Not Disturb `INTERRUPTION_FILTER_PRIORITY`) during active reading sessions to keep the user immersed, controlled by a toggle in the Settings menu (requires `ACCESS_NOTIFICATION_POLICY` permission).

Previously-stubbed Phase 2 (§7) classes — now built and wired (verified by building):
- `analytics/AnalyticsEngine.kt` is a full, wired time-analytics engine (`pagesPerHour`,
  `estimatedMinutesLeft`, `minutesByDay`, `bestTimeOfDay`, `environmentStats`) used across the hero
  card, Analytics/Profile/BookDetail/History screens. The old `calculateReadingVelocity` no longer
  exists — don't cite the "unused" note.
- `format/FormatAdaptabilityLayer.kt` — multi-format tracking (§7B). Maps `Book.format`
  (PAGES/CHAPTERS/VOLUMES/HOURS) to display units, wired through BookDetail (header, stats, note
  prefixes) with a segmented format picker (`repository.updateFormat`). Also owns the read-aloud:
  `startTTSHandoff` + `ReadAloudController`/`rememberReadAloud` drive Android's **built-in**
  `TextToSpeech` (local, free — replaces the blueprint's cloud "TTS handoff").
- `journal/SmartPlanningEngine.kt` — `SmartPlanningEngine` is a velocity-based reading planner
  ("~N units in a 15/30/45/60-min window"), surfaced as the BookDetail "Reading planner" card.
  `JournalingEngine.render` is a dependency-free inline-Markdown renderer (**bold**, *italic*,
  `code`) used by the margin-note cards (§7C — notes have full Room storage + UI).
- `hardware/HardwareIntegrations.kt` — `recognizeHandwriting` (ML Kit Digital Ink, on-device,
  keyless) backing the `hardware/StylusScratchpad.kt` "Handwrite" sheet in BookDetail. (An
  external-display "cast dashboard" was built here via the Presentation API, then removed at the
  user's request. Real Chromecast needs a registered Cast app id + a hosted receiver page —
  off-constraint for a zero-account app — so don't reintroduce it as "Cast" without that.)
- `wear/journal/WristDictaphone.kt` — wrist voice margin notes. The watch's 🎤 button captures
  speech via the platform recognizer (no RECORD_AUDIO, no paid service) and publishes it to the
  phone over a new Data Layer path `/note/{noteId}` (see Constants); `WearSyncService` writes it
  through `repository.addRemoteNote` (upsert-by-id → replay-idempotent) and deletes the transport
  item afterward.

Phase 3 and Code Review Fixes (Verified Complete):
- **Backup & Restore Resilience**: Restore fully respects Last-Write-Wins based on `lastUpdated`/`timestamp`, preventing stale backups from overwriting recent progress. Backup schemas include all fields, like `isFavorite`.
- **System Integrity**: Android 14+ specific fixes (e.g. `specialUse` foreground service types) and proper cleanup of resources like `SoundscapeEngine` to prevent native memory leaks.
- **WearOS Timer Enhancements**: Local ticking mechanisms built into `WearActivity` for the Focus Timer, so it no longer relies on constant DataLayer pushes. Also synced Timer Pause/Resume actions back to the phone. Goal complication reads from the new `ACTIVE_BOOKS_PATH` data map properly.
- **UI Safety & Accidental Data Loss Prevention**: Confirmation dialogs for deleting sessions, duplicate-key crash prevention for ISBN-less books in `AddBookScreen`, and removal of `BookTrackerViewModel`'s DND overrides in favor of a centralized Application-scoped `DndManager`.

Still not built / deliberately deprioritized (don't build without a clear ask): Chromecast /
external-display projection (removed), anime-canon bridge, Android Auto, desktop PWA, geofencing,
wear rotating-bezel input, ambient burn-in pixel-shifting, and syncing the configurable goal to the
watch as a dedicated path.

**Before citing PROGRESS.md's milestone tracker as evidence a feature exists, verify against
actual code.** The original agent run marked all 10 blueprint milestones "COMPLETED" while the
project didn't even compile — see History. Trust `git log` and the code, not old status claims.

### Build / Toolchain
- Multi-module Gradle build (`app`, `wear`, `shared`).
- `minSdk` 26 (app), 30 (wear); `targetSdk` 36.
- Uses KSP for Room. Compose compiler plugin natively.
- **Deployment / Publishing**: The `app` and `wear` modules intentionally share the same Application ID (`com.example.booktracker`). When publishing to the Google Play Store, upload *both* the phone App Bundle (AAB) and the Wear OS AAB to the same release track. Google Play will natively handle prompting users to install the companion Wear OS app on their watch. Do *not* attempt to use the deprecated `wearApp()` gradle configuration.

```
./gradlew :app:assembleDebug :wear:assembleDebug
```

Gradle 9.3, AGP 8.13.2, Kotlin 2.2.20 (Compose compiler via `org.jetbrains.kotlin.plugin.compose`,
*not* the old `composeOptions.kotlinCompilerExtensionVersion`), Compose BOM 2025.09.00,
Room 2.8.4 via KSP 2.2.20-2.0.4, **Wear Compose Material 3 1.6.2** (old `compose-material` removed),
material-icons-extended (BOM-managed), Coil 3.4.0 (coil-compose + coil-network-okhttp),
DataStore Preferences 1.1.7, CameraX 1.4.2, ML Kit barcode-scanning 17.3.0, ML Kit
digital-ink-recognition 18.1.0 (on-device handwriting for the stylus scratchpad; downloads a small
per-language model at first use — packages `libdigitalink.so`, hence the benign strip warning).
History note: Kotlin was briefly pinned to 2.0.0 by the user, then raised back to 2.2.20 because
Coil 3.4.0 transitively pulls Kotlin stdlib 2.3.x / okio with metadata a 2.0.0 compiler can't read
(the exact "future library bump" this file predicted). If you re-pin Kotlin down, Coil will break
the build again — keep Kotlin, KSP, and the Compose plugin on the same 2.2.x line.
compileSdk/targetSdk 36, minSdk 26 (`:app`) / 30 (`:wear`). JDK 17. No API keys, no accounts,
no `google-services.json` — none of that exists in this project anymore.

**Always verify with a real build after nontrivial changes** (`gradlew ... assembleDebug`), not
just a read-through. This project's history is full of code that looked plausible and didn't
compile (see History) — do not repeat that pattern. This is a Windows box; use the PowerShell
tool with `.\gradlew.bat`, not the Bash tool with `./gradlew`.

## Known gotchas hit during development

- `Barcode.FORMAT_ISBN` **does not exist** in ML Kit. ISBNs are physically EAN-13 barcodes — scan
  `FORMAT_EAN_13` and filter results by `barcode.valueType == Barcode.TYPE_ISBN`.
- `androidx.wear.ambient:ambient:1.1.0` as a standalone artifact does not resolve on any repo;
  `AmbientLifecycleObserver` lives in `androidx.wear:wear:1.3.0`.
- Compose's `KeyboardOptions` for text fields is `androidx.compose.foundation.text.KeyboardOptions`,
  not `androidx.compose.ui.text.input.KeyboardOptions` (that package only has `KeyboardType`).
- Wear Material 3 lives in `androidx.wear.compose.compose-material3` — a **different** artifact and
  package (`androidx.wear.compose.material3.*`) from the phone's `androidx.compose.material3.*`.
  Don't mix them. `AppScaffold`/`ScreenScaffold` provide `TimeText`; `dynamicColorScheme(context)`
  returns a nullable `ColorScheme` (fall back to `ColorScheme()`), and its version track (1.6.x)
  is ahead of the other wear-compose artifacts (foundation) — keep foundation on 1.6.2 to match.
- material-icons-extended adds ~2 MB to the debug APK but R8 strips unused vectors in release;
  `Icons.Filled.Add`/`Close`/`Check` are in the core set, but `LocalFireDepartment`/`QrCodeScanner`
  need the extended artifact.
- A Kotlin-multiplatform-flavored module (`:shared`) that exposes a type from a dependency
  declared with `implementation(...)` will fail to compile for consumers — that dependency needs
  `api(...)` instead if the type appears in a public signature. (Was the case for Firebase's
  `Timestamp`; less relevant now that `:shared` has no dependencies, but the principle applies if
  `:shared` ever gains one.)
- PowerShell mis-parses a `git commit -m @'...'@` here-string whenever the message body contains a
  double-quote character (surfaces as a confusing `pathspec ... did not match any file(s)` error).
  The reliable fix: write the message to a file and `git commit -F <file>` — do this for any
  multi-line commit message rather than fighting quoting.
- Windows/PowerShell here-strings for multi-line commit messages: `@'...'@` with the closing `'@`
  at column 0, no leading whitespace.

## Conventions

- Every `Book`/`Session` mutation must stamp `lastUpdated`/timestamps in epoch millis — it's the
  sync conflict key, not just an audit field.
- Repository is the only thing allowed to touch Room directly; UI and sync services go through
  `BookRepository` (resolved via `ServiceLocator`), never `AppDatabase` directly.
- No comments explaining *what* code does — only *why*, for non-obvious constraints (see the
  existing sparse comments in `WearSyncService.kt`, `BookRepository.kt` for the target style).
- Git identity in this repo is set locally (not global) to the user's name/email — already
  configured, no need to touch `git config` again.
- **UI/UX Guidelines:** Strict rules for Material You theming, fluid animations, and Haptic feedback usage can be found in **[UI_UX_GUIDELINES.md](UI_UX_GUIDELINES.md)**.

## History & Release Notes
For a full history of all feature phases and architectural pivots (including the initial removal of Firebase to achieve a zero-cost app), please consult the changelog: **[CHANGELOG.md](CHANGELOG.md)**.

