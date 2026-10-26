# AI Coding Agent Dispatcher

**CRITICAL RULES:**
1. **MAP FIRST:** `.agents/maps/code_map.md` lists every Kotlin file with its purpose, size, and declarations as `name:line`. Grep it for a feature word (e.g. `Rating`, `Loan`, `timer`) or read the section for the domain, then `Read` the target file with `offset`/`limit` at that line. Do not grep the source tree or read big files whole until the map has failed you.
2. **KEEP IT FRESH:** After adding, renaming, moving, or deleting a file, class, or function, run `npm run update-maps`. `npm run check-maps` exits 1 if the map is stale; the pre-commit hook in `.githooks/` runs it (enable once per clone with `npm run install-hooks`).
3. **Historical architecture notes:** `docs/ARCHITECTURE_HISTORY.md`. DO NOT modify it.

## Change X -> touch these files
Paths are relative to `app/src/main/java/com/example/booktracker/` unless they start with a module name.

- **New/changed stored field on Book, Session, or MarginNote:** `shared/.../models/Models.kt` (domain) -> `app/data/local/Entities.kt` -> `app/data/local/Mappers.kt` (toModel/toEntity) -> `app/data/local/AppDatabase.kt` (bump `version`, add `MIGRATION_n_n+1`, register it) -> `app/data/BackupEngine.kt` (`toJson`/`toBook`/`toSession`/`toNote`; bump `FORMAT_VERSION` if old backups need handling). Add DAO queries in `app/data/local/Daos.kt` only if you query the field.
- **New screen / route:** add a `composable("route")` in the NavHost in `app/ui/BookTrackerScreen.kt` (routes are listed in the map), state/actions in `app/ui/BookTrackerViewModel.kt`, data via `app/data/BookRepository.kt` (interface + `RoomBookRepository`).
- **New user setting:** `app/data/SettingsRepository.kt` (key + setter) -> `app/ui/BookTrackerViewModel.kt` (setter) -> `app/ui/SettingsScreen.kt`. If it should survive backup/restore, also update the `settings` block in `app/data/BackupEngine.kt`.
- **Stats / goals / streaks / badges / loans:** pure engines in `app/analytics/` with unit tests in `app/src/test/.../analytics/`; shown in `app/ui/AnalyticsScreen.kt`, `HomeDashboard.kt`, `ProfileScreen.kt`.
- **Reading timer:** `app/service/TimerService.kt` (foreground service + persistence), `app/ui/FocusTimerScreen.kt`, then `SessionSaveScreen.kt` -> `SessionResultScreen.kt`.
- **Phone <-> watch sync:** paths and keys in `shared/.../Constants.kt`; phone side `app/sync/WearBridge.kt` (send) and `WearSyncService.kt` (receive); watch side `wear/.../WearActivity.kt` and `GoalComplicationService.kt`.
- **Wiring / singletons:** `app/data/ServiceLocator.kt` (manual DI, no Hilt).
