# Book Tracker

Cross-platform book tracking app: a phone/tablet app (`:app`, Jetpack Compose + Material 3) and a
Wear OS quick logger (`:wear`, Compose for Wear OS), sharing pure-Kotlin models through `:shared`.
Full product spec lives in [CrossPlatformBookTracker_Blueprint.md](CrossPlatformBookTracker_Blueprint.md).

## Architecture: local-first, zero cost (Phase A)

There is **no backend and no Firebase** — nothing to configure, nothing that can bill you.

- **Phone** is the source of truth: a Room database behind a `BookRepository` interface.
- **Watch** syncs over Bluetooth via the Wearable Data Layer (free, works offline, queues
  automatically while devices are apart):
  - Phone publishes the currently-reading book at `/active_book`.
  - Watch publishes page position at `/progress/{bookId}`.
  - Conflicts resolve by Last-Write-Wins on the `updated_at` timestamp.
- Both apps share `applicationId com.example.booktracker` — required for the Data Layer.

### What works today

- Add books (title, authors, pages) and move them through the TBR pipeline:
  Backlog → Shortlist → Up Next → Reading.
- Scan a book's ISBN barcode ("Scan ISBN" button): CameraX + on-device ML Kit, then a free,
  keyless Google Books lookup auto-fills title, authors, page count, and cover URL.
- Reading hero card with +1/+10 progress and Finish.
- Watch shows the active book; +1/+10 with haptics sync back into the phone's database,
  including while the phone app is closed (`WearSyncService`).
- Ambient mode on the watch stays mostly black and shows only the title.
- Reading sessions: Start/End on the hero card for timed sessions; progress made outside a
  session (either device) is auto-recorded so it still counts.
- Daily streak with a 20-page daily goal, shown on the hero card with a tinted flame icon.
- Reading activity screen (overflow menu → Reading activity): a 52-week calendar heatmap of pages
  per day, with stat tiles and tap-to-inspect, colored from the Material theme.
- Book covers shown in the hero and pipeline cards (from Google Books), with a placeholder for
  manually added books.
- Settings (overflow menu → Settings): configurable daily reading goal, persisted with DataStore
  and applied live to streaks and the heatmap.
- Material 3 throughout: top app bar, single Add FAB, swipe-to-remove cards with Undo, dynamic
  color on Android 12+, and a Material 3 watch UI with the clock (`TimeText`) and watch-face color.

### Not built yet

Velocity charts, syncing the configurable goal to the watch, and everything in blueprint
Phase 2 (those files are stubs).

## Build

```
./gradlew :app:assembleDebug :wear:assembleDebug
```

Toolchain: Gradle 9.3, AGP 8.13.2, Kotlin 2.2.20 + Compose plugin, Compose BOM 2025.09,
Room 2.8 (KSP), Wear Compose Material 3 1.6.2, Coil 3.4, DataStore 1.1. JDK 17+.
No API keys or accounts needed.

## Phase B — cloud sync later, still free

The design keeps Firebase integration a bolt-on, not a rewrite:

- The UI only knows `BookRepository`; a Firestore-backed sync engine slots in behind it.
- Book IDs are client-generated UUIDs → they become Firestore document IDs as-is.
- Every write stamps `lastUpdated` (epoch ms) → the LWW merge key already exists.
- The watch **never talks to the cloud**: it keeps relaying through the phone over Bluetooth,
  so no Cloud Functions / Blaze plan are ever needed. Firebase's Spark tier (Firestore + Auth)
  is free and sufficient.

Firestore security rules for Phase B are already drafted in [firestore.rules](firestore.rules).
