# Changelog

All notable changes to the Book Tracker project will be documented in this file.

## [v1.1.0-beta] - Hyper-Interactive & Premium UI Overhaul
### Added
- **Hyper-Interactive Physics:** Introduced `Modifier.bounceClick()` to add a tactile, elastic spring squish to library cards and buttons.
- **Dopamine Confetti System:** Engineered a high-performance Canvas particle system that fires a confetti explosion when you meet your daily reading goals.
- **Shimmer Skeletons:** Implemented sweeping gradient shimmer skeletons for loading states (e.g. Google Books API search).
- **Splash Screen:** Integrated the Android `SplashScreen` API for a buttery smooth app launch experience.
### Changed
- **Navigation Transitions:** Migrated all `NavHost` routes to use fluid `slideInVertically` and `fadeIn` crossfade transitions.
- **Premium Edge-to-Edge Cards:** Re-engineered the Library Grid to use "Netflix-style" edge-to-edge cover cards with bottom gradient text overlays.
- **Settings Glow-up:** Softened card corner radii and introduced rich vector icons for all setting categories.
- **Glassmorphic Quick Logs:** Upgraded the `HeroPillButton` (+1/+10) to use dynamic frosted glass opacity that shifts on press.
- **Empty States:** Completely replaced plain text empty states with illustrative, glassmorphic cards in the TBR and Library screens.
- **Book Preview Dialog:** Searching for a book and selecting it now opens a Book Preview Dialog to choose exactly which pipeline status (Backlog, Shortlist, Up Next, Reading) it should land in, instead of defaulting to Backlog.
- **Direct Book Actions:** Added a prominent "Start Reading" button and an App Bar "Delete" action directly inside the `BookDetailScreen` to streamline library management.
- **Scanner Wiring:** Fixed the "Scan ISBN Barcode" stub button to properly invoke the on-device CameraX scanner.
- **Navigation Fix:** Fixed a massive memory leak and UX bug where tapping Bottom Navigation Bar icons repeatedly spawned an infinite backstack. Navigation now correctly saves state and pops up to the start destination.
- **Nested Scaffolds Fix:** Solved the duplicate TopAppBar/BottomAppBar issue by dynamically hiding the global Scaffold bars when navigating into sub-screens (e.g. `BookDetailScreen`, `SettingsScreen`), allowing edge-to-edge layouts to shine.

## [v1.0.0-beta] - Phase 3 (Premium Polish)
### Added
- **Haptic Engine:** Added tactile feedback (`LocalHapticFeedback`) to all primary reading actions (Start, End, +1/+10, DNF).
- **Data Visualization:** Built a custom Canvas-based `SmoothLineChart` using Bezier curves and gradient fills for a premium look on the Analytics screen.
- **Wear OS Complication:** Developed a `GoalComplicationService` that surfaces the user's daily reading progress as a Ranged Value Complication on compatible watch faces.
- **Margin Notes UI:** Implemented a new `AddNoteBottomSheet` modal on the Book Detail screen for quick, frictionless journaling.
### Changed
- **UI Architecture:** Overhauled `BookDetailScreen` to feature an edge-to-edge parallax cover image with a dynamic, glassmorphic top app bar overlay.

## [v0.9.0-alpha] - Phase 2 (Library Expansion)
### Added
- **API Auto-Fetch:** Integrated the free Google Books API to automatically pull down book titles, authors, page counts, genres, descriptions, and cover art.
- **Smart Scanner:** Integrated CameraX and on-device ML Kit to scan book barcodes and extract ISBNs instantly.
- **Bulk Import:** Added CSV import capabilities supporting standard Goodreads and StoryGraph exports.
- **Analytics & Heatmap:** Built a 7x52 GitHub-style calendar heatmap to visualize yearly reading volume.
- **Streak Engine:** Implemented logic to compute and display consecutive days the user hits their daily page goal.
### Changed
- **Navigation:** Migrated to a modern 4-tab `NavigationCompose` structure (Home, Library, Stats, Profile).

## [v0.1.0-alpha] - Phase 1 (Local First Foundation)
### Added
- **Local Database:** Established the core Room Database architecture behind a `BookRepository` interface.
- **Wear OS Companion:** Created a thin Wear OS client with Bluetooth synchronization using the Google Play Services Wearable Data Layer.
- **TBR Pipeline:** Organized books into a kanban-style pipeline (Backlog, Shortlist, Up Next, Reading).
- **Session Tracking:** Added explicit start/end triggers for reading sessions to compute reading velocity.
- **Conflict Resolution:** Implemented a Last-Write-Wins (LWW) resolution strategy using epoch timestamps to sync phone and watch states while offline.
