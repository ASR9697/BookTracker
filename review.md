# Code Review — working-tree changes (Phase 2 expansion) — FINAL

**Scope:** uncommitted diff vs HEAD — 44 files, ~4,100 insertions / 460 deletions (6,015 diff lines).
**Build:** ✅ `:app:assembleDebug` + `:wear:assembleDebug` both pass.
**Method:** 8 finder angles over the full diff (line-by-line, removed-behavior, cross-file tracer, reuse, simplification, efficiency, altitude, CLAUDE.md conventions), then independent verification of every candidate against the code. 38 candidates raised → 33 confirmed, 2 refuted, 3 superseded/fixed mid-review.

> ⚠️ **Note:** some files were edited *while the review ran* (GoalComplicationService fallback 1→20, fetch-all→filtered query; DataLayerListenerService.kt added). Verdicts below reflect the **current** state of the working tree.

## Progress: 100% — review complete

---

## Final findings (ranked, max 10)

| # | Severity | Finding | Where |
|---|---|---|---|
| 1 | 🔴 Data loss | Backup restore bypasses Last-Write-Wins — blind upsert overwrites newer local progress/status/ratings with stale backup rows (UI promises a safe merge; `applyRemoteProgress` has the guard, restore doesn't) | [BookRepository.kt:283](app/src/main/java/com/example/booktracker/app/data/BookRepository.kt#L283) |
| 2 | 🔴 Data loss | Backup restore wipes all favorites — `isFavorite` is the one field missing from the Book JSON round-trip; `@Upsert` full-row-replace clears it on every restore | [BackupEngine.kt:117](app/src/main/java/com/example/booktracker/app/data/BackupEngine.kt#L117) |
| 3 | 🔴 Crash | Focus timer crashes on Android 14+ — `TimerService` calls `startForeground` but its manifest entry has no `android:foregroundServiceType` (mandatory at targetSdk 34+, this app targets 36) → `MissingForegroundServiceTypeException` the moment the timer starts | [TimerService.kt:34](app/src/main/java/com/example/booktracker/app/service/TimerService.kt#L34), [AndroidManifest.xml:36](app/src/main/AndroidManifest.xml#L36) |
| 4 | 🔴 Crash | Add-book search results crash on duplicate LazyColumn keys — `key = { it.isbn + it.title }`; Google Books returns ISBN-less editions (`isbn = ""`) with identical titles → `IllegalArgumentException` | [AddBookScreen.kt:213](app/src/main/java/com/example/booktracker/app/ui/AddBookScreen.kt#L213) |
| 5 | 🔴 Broken feature | Badge unlocks permanently stalled — `_newUnlockEvents` is a rendezvous Channel with no collector anywhere, so the first `send()` suspends forever and `markBadgesSeen` never runs; repeats every launch | [BookTrackerViewModel.kt:194](app/src/main/java/com/example/booktracker/app/ui/BookTrackerViewModel.kt#L194) |
| 6 | 🔴 Broken feature | Goal complication reads a dead path — phone now publishes only `/active_books` (list), nothing publishes `/active_book` anymore, and the query URI is malformed anyway (`wear://*/` + `/active_book` = double slash) → complication shows "0 / 20" forever | [GoalComplicationService.kt:34](wear/src/main/java/com/example/booktracker/wear/GoalComplicationService.kt#L34) |
| 7 | 🟠 Broken feature | Watch timer Pause/Resume buttons do nothing — watch publishes `/timer_control/{ts}`, but no phone-side listener handles `TIMER_CONTROL_PATH` (`WearSyncService` only handles notes + progress) | [WearActivity.kt:262](wear/src/main/java/com/example/booktracker/wear/WearActivity.kt#L262) |
| 8 | 🟠 System-state bug | Two competing DND implementations — an Application-scoped `DndManager` does Deep Focus *correctly* (observes open sessions + settings, saves/restores `previousFilter`), but the ViewModel's copy-pasted DND blocks fight it: startSession sets PRIORITY first so `DndManager` records no `previousFilter`, and endSession forces `FILTER_ALL`, clobbering any DND the user had set before the session. Fix: **delete the ViewModel blocks**; `DndManager` already covers every entry point (including the cold-start and toggle-off-mid-session cases) | [BookTrackerViewModel.kt:296](app/src/main/java/com/example/booktracker/app/ui/BookTrackerViewModel.kt#L296), [DndManager.kt:36](app/src/main/java/com/example/booktracker/app/data/DndManager.kt#L36) |
| 9 | 🟡 Battery | Focus-timer coroutine ticks at 1 Hz for the app's whole life — `while (true) { delay(1000) }` launched unconditionally in `init`, even if the timer is never used | [BookTrackerViewModel.kt:200](app/src/main/java/com/example/booktracker/app/ui/BookTrackerViewModel.kt#L200) |
| 10 | 🟡 Wrong UI | Search results label chapter/hour/volume books as "p. N" — hardcoded prefix instead of `FormatAdaptabilityLayer.unitAbbrev(book.format)` used by BookDetail; the `book` is already in scope | [LibrarySearchScreen.kt:238](app/src/main/java/com/example/booktracker/app/ui/LibrarySearchScreen.kt#L238) |

### Formal result (JSON)

```json
[
  {"file": "app/src/main/java/com/example/booktracker/app/data/BookRepository.kt", "line": 283, "summary": "restoreFromBackup blindly upserts rows with no lastUpdated comparison, bypassing the repo's Last-Write-Wins convention", "failure_scenario": "Restore a week-old backup onto a current library → this week's progress/status/ratings replaced by stale rows, which then re-sync to the watch"},
  {"file": "app/src/main/java/com/example/booktracker/app/data/BackupEngine.kt", "line": 117, "summary": "Book JSON round-trip omits isFavorite (the only dropped field), and restore full-row-upserts over existing rows", "failure_scenario": "Export backup → restore on same library → every favorite flag silently cleared"},
  {"file": "app/src/main/java/com/example/booktracker/app/service/TimerService.kt", "line": 34, "summary": "startForeground called but manifest <service> declares no android:foregroundServiceType (targetSdk 36)", "failure_scenario": "Start the focus timer on Android 14+ → MissingForegroundServiceTypeException crash"},
  {"file": "app/src/main/java/com/example/booktracker/app/ui/AddBookScreen.kt", "line": 213, "summary": "LazyColumn key { it.isbn + it.title } is not unique across Google Books results", "failure_scenario": "Search a popular title → two ISBN-less editions with the same title → IllegalArgumentException duplicate-key crash"},
  {"file": "app/src/main/java/com/example/booktracker/app/ui/BookTrackerViewModel.kt", "line": 194, "summary": "_newUnlockEvents is a rendezvous Channel with no collector, so send() suspends forever and markBadgesSeen never runs", "failure_scenario": "Unlock any badge → watcher coroutine stalls permanently, seen-badges never persisted, repeats every launch"},
  {"file": "wear/src/main/java/com/example/booktracker/wear/GoalComplicationService.kt", "line": 34, "summary": "Complication queries /active_book, which nothing publishes anymore (phone publishes /active_books), via a malformed double-slash URI", "failure_scenario": "Any watch face complication → data fetch always empty → renders 0/20 regardless of actual reading progress"},
  {"file": "wear/src/main/java/com/example/booktracker/wear/WearActivity.kt", "line": 262, "summary": "Watch publishes timer control actions to /timer_control but no phone-side listener consumes TIMER_CONTROL_PATH", "failure_scenario": "Tap Pause/Resume on the watch timer → data item written, nothing on the phone reacts, timer keeps running"},
  {"file": "app/src/main/java/com/example/booktracker/app/ui/BookTrackerViewModel.kt", "line": 296, "summary": "ViewModel's duplicated DND blocks conflict with the Application-scoped DndManager, defeating its previousFilter save/restore", "failure_scenario": "User has personal DND on → starts+ends a reading session → ViewModel forces INTERRUPTION_FILTER_ALL, user's own DND silently disabled"},
  {"file": "app/src/main/java/com/example/booktracker/app/ui/BookTrackerViewModel.kt", "line": 200, "summary": "Unconditional while(true){delay(1000)} ticker launched in init for the ViewModel's whole lifetime", "failure_scenario": "User never opens the focus timer → app still wakes a coroutine every second for the entire process lifetime"},
  {"file": "app/src/main/java/com/example/booktracker/app/ui/LibrarySearchScreen.kt", "line": 238, "summary": "Note search hits hardcode the 'p.' unit prefix instead of FormatAdaptabilityLayer.unitAbbrev(book.format)", "failure_scenario": "Note on a CHAPTERS/HOURS book → search shows 'p. 12' while BookDetail shows 'ch. 12' for the same note"}
]
```

---

## Fixed mid-review (verified fixed in current working tree)

- **Stale complication plumbing** — `DataLayerListenerService.kt` was added during the review; its manifest `pathPrefix="/active_book"` prefix-matches `/active_books`, so background data changes now trigger complication refreshes. (The refresh works; the *data read* is still broken — finding #6.)
- **Complication fallback goal** — was `1f` ("0 / 1" ring), now `20f`.
- **Complication fetch-all** — `dataItems` fetch-all was replaced with a URI-filtered `getDataItems` (but see finding #6: the URI is malformed and the path is dead).

## Refuted (no action needed)

- **FTS search crash on `:` input** — `toFtsQuery` does leave `:` unstripped, but FTS4 parses an unrecognized column-filter prefix as a token boundary, not a syntax error. No crash.
- **"5×" nav-options duplication** — it's 4×, all in the NavigationBarItems; the claimed 5th site has no nav code. (The 4× duplication still merits a `navigateTopLevel(route)` helper.)

---

## Cleanup backlog — now closed

All top-10 findings were fixed and committed (`ed89797`). A follow-up pass then re-checked every item in this backlog against current code; most had *already* been fixed in the same working tree (list-upsert DAOs, lazy TTS init, `graphicsLayer`-based top-bar fade, `ThemeMode` enum, persisted pomodoro settings, shared `CameraPreview`, mostly-consolidated `.label` usage). The remaining handful were fixed directly in this pass:

**Already fixed (found during re-check, no action needed)**
- Backup restore now does one list-upsert per entity type (no more per-row implicit transactions) — [BookRepository.kt:293](app/src/main/java/com/example/booktracker/app/data/BookRepository.kt#L293)
- N+1 `getById` in `searchLibrary` replaced with a batched `getByIds` — [BookRepository.kt:258](app/src/main/java/com/example/booktracker/app/data/BookRepository.kt#L258)
- `ReadAloudController` now lazily creates its `TextToSpeech` engine on first `toggle()`, not eagerly — [FormatAdaptabilityLayer.kt:106](app/src/main/java/com/example/booktracker/app/format/FormatAdaptabilityLayer.kt#L106)
- Top-bar fade now reads `topBarAlpha()` inside `drawBehind`/`graphicsLayer` (draw-phase), not composition-phase — [BookDetailScreen.kt:371](app/src/main/java/com/example/booktracker/app/ui/BookDetailScreen.kt#L371)
- Theme mode is now a `ThemeMode` enum, not a magic Int — [SettingsRepository.kt:18](app/src/main/java/com/example/booktracker/app/data/SettingsRepository.kt#L18)
- Pomodoro work/break minutes now persist via `SettingsRepository`/DataStore — [SettingsRepository.kt:84](app/src/main/java/com/example/booktracker/app/data/SettingsRepository.kt#L84)
- Camera preview is a single shared `CameraPreview` composable used by both Scanner and OCR screens — [CameraPreview.kt](app/src/main/java/com/example/booktracker/app/ui/CameraPreview.kt)
- `BookStatus.label` (an enum property) is now used at nearly every call site instead of ad-hoc strings

**Fixed in this pass**
- Removed unused `Intent`/`Settings`/`NotificationManager` imports from `BookTrackerViewModel.kt` (dead since DND moved to `DndManager`)
- `AnalyticsScreen.kt`: `rememberTextMeasurer()` now uses its import instead of a redundant fully-qualified call
- Removed the 6 CLAUDE.md "what"-comment violations (`// Draw the line`, `// Filter Chips`, `// Status Pill`, `// Timer State`, `// Scan Barcode Button`, `// Your Wrapped`)
- `AddBookScreen.kt`'s two remaining hardcoded status-label lists now map through `BookStatus.label` instead of duplicating the string literals
- `DndManager` no longer takes a `SessionDao` directly (was violating "only `BookRepository` touches Room") — it now takes a `BookRepository` and calls `repository.observeOpenSession()`, which already existed; `ServiceLocator` updated to match
- `BookDetailScreen.kt`: the 3 remaining exact-duplicate `Box(background+padding)` wrappers (margin-notes header, session-history header, session row) now use the existing `SectionItem` helper instead of repeating the modifier chain
- `BookTrackerScreen.kt`: extracted a `navigateTopLevel(route)` helper and pointed all 4 `NavigationBarItem`s at it instead of repeating the `popUpTo`/`launchSingleTop`/`restoreState` block

**Left alone (real but out of scope for a safe pass)**
- `BackupEngine.kt` still re-implements `Mappers.kt`'s JSON conversions independently — real duplication, but consolidating touches serialization logic in a currently-correct area (isFavorite bug already fixed); left alone to avoid risking new data bugs without a device to test restore against.
- The two empty-state `Text` modifiers in `BookDetailScreen.kt` (`horizontal = 16.dp` only, no vertical) were deliberately **not** forced into `SectionItem` (which adds `vertical = 8.dp`) since that would be a visual change I can't verify without running the app.

**Verified holding:** zero-cost constraint (ML Kit text recognition + GMS fonts are free/keyless), `:shared` stays dependency-free, `lastUpdated` stamped on all mutations, FEATURES.md updated, all three Room migrations (1→2→3→4) registered, `publishedDate` intact end-to-end, wear timer state path consistent phone→watch.

**Build:** ✅ `:app:assembleDebug` + `:wear:assembleDebug` both pass after all cleanup fixes.
