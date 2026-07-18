# Book Tracker

Cross-platform book tracking app: phone/tablet app (`:app`, Jetpack Compose + Material 3) and a
Wear OS quick logger (`:wear`, Compose for Wear OS), sharing pure-Kotlin models through `:shared`.
Full product spec: [CrossPlatformBookTracker_Blueprint.md](CrossPlatformBookTracker_Blueprint.md).
Session-by-session history: [PROGRESS.md](PROGRESS.md). User-facing state: [README.md](README.md).

## Cost constraint — read this first

**The user explicitly does not want to pay for anything right now.** No Firebase, no Claude/API
calls from the app, no paid services of any kind. This is why Firebase was removed after being
scaffolded in — see "History" below. Do not reintroduce a paid dependency without asking first,
even if the blueprint calls for it. Google Books lookup and ML Kit barcode scanning are fine
because they're free and keyless.

## Architecture: local-first (Phase A), cloud sync deferred (Phase B)

There is currently **no backend**. The phone is the source of truth via a Room database sitting
behind a `BookRepository` interface (`app/src/main/java/.../data/BookRepository.kt`). The watch
is a thin client that syncs over the Bluetooth Wearable Data Layer — free, works offline, and
Google Play services queues data items automatically while devices are apart.

Sync contract (`shared/src/main/java/.../shared/Constants.kt`):
- Phone publishes the currently-reading book at `/active_book`.
- Watch publishes page position at `/progress/{bookId}`.
- Conflicts resolve by **Last-Write-Wins** on an epoch-millis `lastUpdated`/`updated_at` field —
  this is the one field every mutation must stamp, on both sides.

Both `:app` and `:wear` share `applicationId com.example.booktracker` — this is **required** for
the Data Layer to connect the two apps on-device; do not let them diverge.

### Why this design survives Phase B (cloud sync) as a bolt-on, not a rewrite

Three choices were made deliberately so Firebase can slot in later without touching the UI:
1. The UI only ever talks to the `BookRepository` interface — a Firestore-backed implementation
   swaps in behind it.
2. Book IDs are client-generated UUIDs (`UUID.randomUUID().toString()`), so they become Firestore
   document IDs unchanged — nothing to remap during migration.
3. Every write already stamps `lastUpdated` in epoch millis — the same LWW key Firestore sync will
   need, so no schema change is required to add sync.

When Phase B happens: the watch should **still never talk to the cloud directly** — it keeps
relaying through the phone over Bluetooth. This avoids needing a paid Cloud Function to mint
Firebase custom tokens (the original blueprint's "zero-touch" auth design required exactly that,
which is what made the original Firebase approach non-free). Firestore + free-tier Auth on the
Spark plan is enough; do not add a Cloud Function without flagging the cost.

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

Working end-to-end and verified by building + running:
- Add books manually, or scan an ISBN barcode (CameraX + ML Kit `BarcodeAnalyzer`, filtered to
  `TYPE_ISBN`) with auto-fill from the free Google Books volumes endpoint
  (`data/remote/GoogleBooksClient.kt` — no API key, keyless `q=isbn:` query).
- TBR pipeline: Backlog → Shortlist → Up Next → Reading. Cards promote via a button and
  **swipe-to-dismiss to remove, with an Undo Snackbar** (`repository.restore` re-inserts the
  exact entity, preserving id/status/progress/lastUpdated — see below).
- Reading hero card: +1/+10 page progress, Finish.
- Phone chrome is Material 3: `CenterAlignedTopAppBar` (title + Scan-ISBN action icon), a single
  `Add` FAB (Material icons, not text glyphs), `PrimaryTabRow`, and a tinted `LocalFireDepartment`
  streak icon. Dynamic color (Material You) via `dynamicDark/LightColorScheme` on Android 12+.
- Watch: shows active book, +1/+10 with haptics, syncs back to the phone's Room DB via
  `WearSyncService` even when the phone app is closed. UI is **Wear Compose Material 3**
  (`AppScaffold`/`ScreenScaffold` giving `TimeText`, M3 components) with dynamic color from the
  watch face (`dynamicColorScheme(context)`, null-fallback to `ColorScheme()`). Ambient mode
  bypasses the scaffold entirely and stays mostly black (burn-in + power).
- Session recording: explicit Start/End session on the reading hero (open session = `endTime = 0`
  row in Room, survives process death; auto-closed when the book leaves READING or another book's
  session starts). Any +1/+10 delta made with **no** session open — on phone or watch — is
  recorded as a self-contained mini-session so it still counts toward streaks; deltas made while
  a session is open are NOT double-recorded (finalize captures them via endUnit - startUnit).
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

Stubbed / not built (do not assume these work — they are placeholder classes from the original
scaffold, kept for the Phase 2 roadmap but not wired to anything):
- `analytics/AnalyticsEngine.kt`, `format/FormatAdaptabilityLayer.kt`,
  `hardware/HardwareIntegrations.kt`, `journal/SmartPlanningEngine.kt`,
  `wear/journal/WristDictaphone.kt` — all Phase 2 blueprint features (§7 of the blueprint).
- Reading-velocity charts, a full-screen add-book dialog (still an AlertDialog), wear rotating-bezel
  input, ambient burn-in pixel-shifting, and syncing the configurable goal to the watch.

**Before citing PROGRESS.md's milestone tracker as evidence a feature exists, verify against
actual code.** The original agent run marked all 10 blueprint milestones "COMPLETED" while the
project didn't even compile — see History. Trust `git log` and the code, not old status claims.

## Build & toolchain

```
./gradlew :app:assembleDebug :wear:assembleDebug
```

Gradle 9.3, AGP 8.13.2, Kotlin 2.2.20 (Compose compiler via `org.jetbrains.kotlin.plugin.compose`,
*not* the old `composeOptions.kotlinCompilerExtensionVersion`), Compose BOM 2025.09.00,
Room 2.8.4 via KSP 2.2.20-2.0.4, **Wear Compose Material 3 1.6.2** (old `compose-material` removed),
material-icons-extended (BOM-managed), Coil 3.4.0 (coil-compose + coil-network-okhttp),
DataStore Preferences 1.1.7, CameraX 1.4.2, ML Kit barcode-scanning 17.3.0.
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
- PowerShell (this environment's Bash tool runs Git Bash, but the PowerShell tool is primary) will
  mis-parse a `git commit -m` heredoc if the message body contains double-quote characters —
  avoid `"` inside commit messages, or the pathspec error surfaces as a confusing "did not match
  any file(s)" failure.
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

## History (condensed — see PROGRESS.md for full detail)

1. **Original scaffold** (pre-session, by a different agent): built the full 10-milestone Firebase
   architecture per the blueprint, marked everything "COMPLETED" in PROGRESS.md. In reality it had
   never successfully compiled — verified by this session's initial review. Confirmed defects:
   missing `google-services.json`/launcher resources (build blockers), `Barcode.FORMAT_ISBN`
   compile error, `Timestamp` visibility bug in `:shared`, phantom `ambient:1.1.0` dependency, and
   an auth design (`signInWithCustomToken` on the watch) that cannot work without a server-side
   Cloud Function that was never built.
2. **Repair pass**: modernized the toolchain, added missing resources/config, fixed the confirmed
   compile errors, got both modules building green. Firebase was still present at this point.
3. **Cost-driven pivot to local-first**: user asked for zero ongoing cost. Firebase removed
   entirely (this also incidentally solved the broken auth design). Room + repository +
   Bluetooth Data Layer sync built from scratch. This is the current architecture.
4. **ISBN scanner**: CameraX + ML Kit scanning wired to a free Google Books lookup, added on top
   of the local-first architecture.

Git history preserves each stage as its own commit — `git log --oneline` — including a baseline
commit of the original broken scaffold, if you need to see what changed and why.
