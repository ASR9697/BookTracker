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

## Confirmed cleanup backlog (below the top-10 cut)

**Efficiency**
- Backup restore runs one implicit transaction per row (no `withTransaction`, single-item DAOs) — [BookRepository.kt:283](app/src/main/java/com/example/booktracker/app/data/BookRepository.kt#L283)
- N+1 `getById` per note hit in `searchLibrary` — batch with `WHERE id IN` — [BookRepository.kt:252](app/src/main/java/com/example/booktracker/app/data/BookRepository.kt#L252)
- `rememberReadAloud()` eagerly binds a TextToSpeech engine on every BookDetail open — create lazily on first Listen tap — [BookDetailScreen.kt:526](app/src/main/java/com/example/booktracker/app/ui/BookDetailScreen.kt#L526)
- Top-bar fade `derivedStateOf` read in composition → full-screen recomposition per scroll pixel; use the `graphicsLayer` pattern the same file already uses for the parallax header — [BookDetailScreen.kt:342](app/src/main/java/com/example/booktracker/app/ui/BookDetailScreen.kt#L342)

**Architecture**
- Theme mode is a magic Int (0/1/2) re-encoded in SettingsRepository comment / MainActivity `when` / SettingsScreen list index — use an enum at the DataStore boundary — [MainActivity.kt:31](app/src/main/java/com/example/booktracker/app/MainActivity.kt#L31)
- Pomodoro work/break/volume knobs live only in VM memory (reset on process death) — persist via SettingsRepository — [BookTrackerViewModel.kt:144](app/src/main/java/com/example/booktracker/app/ui/BookTrackerViewModel.kt#L144)
- `DndManager` receives a `SessionDao` directly — CLAUDE.md says only `BookRepository` touches Room; expose an open-session flow on the repository instead — [ServiceLocator.kt:29](app/src/main/java/com/example/booktracker/app/data/ServiceLocator.kt#L29)

**Duplication**
- Camera preview + permission flow copy-pasted between Scanner and OCR screens — [OcrCaptureScreen.kt:238](app/src/main/java/com/example/booktracker/app/ui/OcrCaptureScreen.kt#L238)
- BookStatus→label mapping duplicated across 4 files (7+ sites), already divergent (LibrarySearchScreen omits PAUSED; one BookTrackerScreen copy omits READING/DNF) — hoist one shared source
- BackupEngine re-implements Mappers.kt JSON conversions (already out of sync — see finding #2) — [BackupEngine.kt:97](app/src/main/java/com/example/booktracker/app/data/BackupEngine.kt#L97)
- DND system-call block duplicated 3× — resolved automatically by finding #8's fix (delete the ViewModel blocks)
- Section-wrapper Box copy-pasted 8× (+4 drifting variants) in BookDetailScreen; NavOptions block 4× in BookTrackerScreen; TTS alias property in FormatAdaptabilityLayer; dead code (unused imports, unconsumed `timerBookId` flow, qualified `rememberTextMeasurer`)

**Conventions (CLAUDE.md: no *what* comments)**
- Narrating comments across AnalyticsScreen (`// Draw the line`), BookTrackerScreen (`// Filter Chips`, `// Status Pill`), BookTrackerViewModel (`// Timer State`…), AddBookScreen, ProfileScreen

**Verified holding:** zero-cost constraint (ML Kit text recognition + GMS fonts are free/keyless), `:shared` stays dependency-free, `lastUpdated` stamped on all mutations, FEATURES.md updated, all three Room migrations (1→2→3→4) registered, `publishedDate` intact end-to-end, wear timer state path consistent phone→watch.
