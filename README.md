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
- Reading hero card with +1/+10 progress and Finish.
- Watch shows the active book; +1/+10 with haptics sync back into the phone's database,
  including while the phone app is closed (`WearSyncService`).
- Ambient mode on the watch stays mostly black and shows only the title.

### Not built yet

Camera ISBN scanning UI (analyzer exists), Google Books lookup, session recording, streaks,
the 7×52 analytics grid, swipe actions, and everything in blueprint Phase 2 (those files are stubs).

## Build

```
./gradlew :app:assembleDebug :wear:assembleDebug
```

Toolchain: Gradle 9.3, AGP 8.13, Kotlin 2.2 + Compose plugin, Compose BOM 2025.09,
Room 2.8 (KSP), Wear Compose 1.5. JDK 17+. No API keys or accounts needed.

## Phase B — cloud sync later, still free

The design keeps Firebase integration a bolt-on, not a rewrite:

- The UI only knows `BookRepository`; a Firestore-backed sync engine slots in behind it.
- Book IDs are client-generated UUIDs → they become Firestore document IDs as-is.
- Every write stamps `lastUpdated` (epoch ms) → the LWW merge key already exists.
- The watch **never talks to the cloud**: it keeps relaying through the phone over Bluetooth,
  so no Cloud Functions / Blaze plan are ever needed. Firebase's Spark tier (Firestore + Auth)
  is free and sufficient.

Firestore security rules for Phase B are already drafted in [firestore.rules](firestore.rules).
