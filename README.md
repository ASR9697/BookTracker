# Book Tracker

Cross-platform book tracking app: a phone/tablet app (`:app`, Jetpack Compose + Material 3) and a
standalone Wear OS logger (`:wear`, Compose for Wear OS), sharing data models through `:shared` and
syncing via Firebase Firestore. Full product spec lives in
[CrossPlatformBookTracker_Blueprint.md](CrossPlatformBookTracker_Blueprint.md).

## Current state (honest)

Both modules **compile and assemble**, but this is an early skeleton:

- Phone UI is a placeholder pager (Backlog / Shortlist / Up Next tabs) with no data wiring yet.
- Wear UI is a working +1/+10 page counter with haptics and ambient mode, but the count is
  local-only — no session recording or sync yet.
- The barcode ISBN analyzer exists but no camera screen uses it yet.
- The Firebase config files are **placeholders** (see setup below); Firebase calls fail until replaced.
- Phase 2 files (`AnalyticsEngine`, `FormatAdaptabilityLayer`, `HardwareIntegrations`,
  `SmartPlanningEngine`, `WristDictaphone`) are stubs, not features.

## One-time setup

1. **Create a Firebase project** at <https://console.firebase.google.com>.
2. Add an **Android app** with package name `com.example.booktracker` (used by both phone and watch).
3. Download `google-services.json` and replace **both** placeholder copies:
   - `app/google-services.json`
   - `wear/google-services.json`
4. In the console, enable **Firestore** and **Authentication**.
5. Deploy the security rules: `firebase deploy --only firestore:rules` (rules are in
   [firestore.rules](firestore.rules)).

## Build

```
./gradlew :app:assembleDebug :wear:assembleDebug
```

Toolchain: Gradle 9.3, AGP 8.13, Kotlin 2.2 (Compose compiler via the Kotlin Compose plugin),
Compose BOM 2025.09, Firebase BoM 34.x. JDK 17+ required.

## Known architectural gap: watch authentication

`WearableAuthListenerService` signs in with `signInWithCustomToken()`, but custom tokens can only be
minted **server-side** with the Firebase Admin SDK — no client API produces one, and nothing on the
phone currently sends a token at all. Before the watch can sync, we must either:

- **(Recommended)** Add a callable Cloud Function that mints a custom token for the signed-in phone
  user; the phone fetches it and pushes it over the Data Layer (requires the Blaze plan), or
- Sign in on the watch directly (Google Sign-In on Wear), accepting a less "zero-touch" flow.

This decision is open — the Data Layer pipe itself is already in place and reusable either way.

## Roadmap (from the blueprint)

1. Phone sign-in flow, then a repository layer over Firestore.
2. Real Kanban pipeline UI + swipe actions, dashboard hero with live data.
3. Camera screen wired to the ISBN analyzer + Google Books lookup.
4. Wear session recording + Data Layer sync with offline queue (LWW conflict resolution).
5. Streak engine and the 7×52 analytics grid.
6. Phase 2 features (see blueprint §7).
