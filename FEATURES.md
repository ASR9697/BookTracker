# Book Tracker Features Checklist

This file acts as the central source of truth for all implemented and planned features for the Book Tracker app. 
**Before implementing any new feature, it MUST be added to the pending list in this file.**

## Completed Features 🟢

### 📱 Core Architecture & Sync
- [x] **Privacy-First & Local-First**: All data lives securely on your device (using a Room database) with zero external servers or cloud subscriptions required.
- [x] **Seamless Bluetooth Syncing**: The phone and Wear OS app automatically synchronize data in the background using Google Play Services' Data Layer.
- [x] **Offline Capable**: The app is 100% functional without an internet connection (aside from fetching initial cover images or doing a Google Books lookup).

### 📚 Adding Books & Library Management
- [x] **Smart ISBN Barcode Scanner**: Utilizes your device's camera and on-device ML Kit to scan book barcodes and instantly identify the ISBN.
- [x] **API Auto-Fetch**: Searches the free Google Books API to automatically pull down book titles, authors, page counts, genres, descriptions, and cover art.
- [x] **Bulk CSV Import**: Easily import your existing libraries from Goodreads or StoryGraph directly via CSV files.
- [x] **TBR (To Be Read) Pipeline**: Organize your library into a kanban-style pipeline with distinct tabs: Backlog, Shortlist, Up Next, and currently Reading.
- [x] **Dynamic Library Grid**: An adaptive grid view of your library with fast filter chips to sort by genres or book status.

### 📖 Reading Experience & Progress Tracking
- [x] **Immersive Reading Timer**: A built-in Pomodoro focus timer with auto-session logging and purely local, zero-cost generated soundscapes (white/brown noise) using `AudioTrack` to help you focus.
- [x] **Reading Hero Dashboard**: A dynamic, beautiful card that acts as your reading command center for your current active book.
- [x] **Session Tracking**: Explicitly start and end reading sessions, which records exactly how much time you spent and your reading velocity (pages per hour).
- [x] **Environment Tagging**: Tag your reading sessions with the vibe/environment (e.g., "☕ Coffee Shop", "🌧️ Rainy Day") to see how your surroundings affect your reading habits.
- [x] **Reading Genome (Local Recommendations)**: An intelligent, purely local recommendation engine that matches your backlog books against your highest-rated reads to suggest what to read next based on genres and authors.
- [x] **Glance Home Screen Widget**: An Android home screen widget to see your active reading progress and log +1 or +10 pages with a single tap, without opening the app.
- [x] **Deep Focus DND**: Optionally silence system notifications while you are actively tracking a reading session.
- [x] **Quick Progress Logging**: +1 and +10 quick add buttons for rapid tracking.
- [x] **Margin Notes**: Capture quotes, thoughts, or questions tied to specific pages using a clean "Add Note" bottom sheet on the Book Details screen.
- [x] **Completion Outcomes**: When you finish a book, you can rate it on multiple axes (Pacing, Focus, Vibe). If you give up on a book, you can log it as a DNF (Did Not Finish) and track your abandonment percentage and reason.
- [x] **Multi-Format Tracking**: Books can be tracked in the units that fit them — Pages, Chapters, Volumes, or Hours (audiobooks) — via a segmented format picker on the Book Detail screen; progress stats, logging buttons, and margin-note prefixes all adapt their display units to the chosen format (`format/FormatAdaptabilityLayer.kt`).
- [x] **Read-Aloud (Local TTS)**: A read-aloud controller drives Android's built-in on-device TextToSpeech engine (free, offline, keyless) to speak book text/notes aloud — replacing the blueprint's cloud "TTS handoff" with a zero-cost equivalent.
- [x] **Smart Reading Planner**: A velocity-based planner card on Book Detail estimates how many pages/chapters/minutes of the book you can get through in a 15/30/45/60-minute window, computed from your historical reading speed (`journal/SmartPlanningEngine.kt`). Margin-note cards also render lightweight inline Markdown (**bold**, *italic*, `code`) via a dependency-free renderer.
- [x] **Stylus Scratchpad (Handwritten Notes)**: A "Handwrite" bottom sheet on Book Detail lets you scribble a note with finger or stylus; on-device ML Kit Digital Ink recognition converts the strokes to text for a margin note (small per-language model downloads on first use — keyless and free).

### 📊 Analytics, Stats & Goals
- [x] **Smooth Line Charts**: A premium, custom Canvas-based Bezier curve line chart with a transparent gradient fill visualizing your recent reading activity.
- [x] **Calendar Heatmap**: A GitHub-style 7x52 weekly heatmap tracking your reading volume throughout the year.
- [x] **Streak Tracking**: The app computes consecutive days you've hit your daily page goal and displays your current streak.
- [x] **Configurable Goals**: Set customized daily and yearly reading goals through the Settings screen.
- [x] **Contextual Insights**: Correlation data showing how much you read under specific environment tags.
- [x] **"ETA to Finish" Predictions**: `AnalyticsEngine` derives your reading velocity (pages per hour) from completed session history and surfaces an estimated time remaining for the active book on the reading hero card and Book Detail stats.
- [x] **Gamification & Secret Badges**: A hidden achievement system that unlocks glassmorphic badges (e.g., 'Night Owl', 'Marathoner') based on your reading behavior, dynamically surfaced on the Profile screen with celebratory snackbars.
- [x] **Book Tracker Wrapped**: A shareable end-of-year or end-of-month summary graphic showing books finished, pages read, reading hours, top genre, and longest streak. The graphic is generated locally using Compose `GraphicsLayer` and shared securely via Android's `FileProvider`.

### ⌚ Wear OS Companion App
- [x] **Wrist-Based Logging**: A standalone Wear OS app that allows you to quickly log pages (+1 or +10) without pulling out your phone.
- [x] **Dynamic Material You Theming**: The watch app inherits its colors directly from your active watch face.
- [x] **Ambient Mode**: A battery-friendly ambient mode that prevents screen burn-in while keeping the app accessible.
- [x] **Reading Goal Complication**: A dynamic "Ranged Value" watch face complication that visually tracks your daily reading goal progress right on your favorite watch face.
- [x] **Wrist Dictaphone (Voice Margin Notes)**: The watch's 🎤 button captures speech through the platform speech recognizer (no RECORD_AUDIO permission, no paid service) and publishes the transcript to the phone over the Data Layer (`/note/{noteId}`); `WearSyncService` upserts it into the active book's margin notes idempotently, even when the phone app is closed.
- [x] **Watch Goal Sync**: The phone publishes the configured daily goal and today's page count to the watch over the Data Layer (`WearBridge`), so both the watch UI and the goal complication reflect your real Settings goal instead of a hardcoded fallback.

### 🎨 Premium UI/UX Polish
- [x] **Dynamic Theming (Material You)**: The app's color palette adapts entirely to your system wallpaper colors on Android 12+. You can also explicitly force Light or Dark mode, or fall back to system defaults via the Settings screen.
- [x] **Settings & Customization**: A dedicated settings screen where you can adjust visual preferences (Dynamic Color, Theme Mode), Do Not Disturb settings, daily/yearly reading goals, and handle data backups.
- [x] **Edge-to-Edge Parallax**: The Book Detail screen features a stunning edge-to-edge cover image that moves independently of the scroll (parallax effect), complete with a frosted glassmorphism app bar overlay.
- [x] **Timer Mini-Player**: When a focus session is active, an omnipresent mini-player appears at the bottom of the screen (above the bottom navigation bar), allowing you to play/pause the timer and seamlessly return to the deep focus screen from anywhere in the app.
- [x] **Micro-Interactions & Physics**: Fluid `slideInVertically`/`fadeIn` crossfade transitions for navigation. Added a custom `bounceClick` spring physics modifier so elements physically squish under your touch.
- [x] **Haptic Feedback**: Deliberate tactile haptics trigger when starting/ending sessions, achieving goals, or logging pages, ensuring the app feels highly responsive and physical.
- [x] **Dopamine Confetti**: A high-performance Canvas particle system triggers a vibrant burst of confetti across the screen when you log pages and hit your daily reading goal.
- [x] **Modern Empty States & Shimmer Skeletons**: Replaced plain text empty states with illustrative, glassmorphic cards. Used sweeping, animated shimmer gradients (instead of circular spinners) for loading screens like the Google Books API search.
- [x] **TBR "Tinder-Style" Swiping**: An interactive card-swiping interface to curate your `BACKLOG`. Accessible from the Library screen, you can swipe cards right to promote to Shortlist/Up Next, up to start reading immediately, or left to skip them for now.

### 🔎 Search, Data & OCR
- [x] **Universal Full-Text Search**: A search icon in the top app bar opens a dedicated screen that queries a Room FTS4 index across every book field (title, author, description, genre) *and* the full text of all margin notes/quotes, grouped into "Books" and "Margin notes" result sections (`ui/LibrarySearchScreen.kt`, `BookRepository.searchLibrary`). Raw input is turned into a forgiving prefix query so partial words match, and it runs 100% offline against the local database.
- [x] **Local Backup & Export**: From Settings → Backup & restore, the entire library — books, sessions, margin notes and goal settings — serializes to a versioned JSON document inside a portable `.zip` written wherever the system file picker points (`data/BackupEngine.kt`). Restore reads the `.zip` (or a bare `.json`) and upserts by id so it safely merges onto an existing library and preserves `lastUpdated` for sync consistency. No cloud, no accounts.
- [x] **Smart OCR Scanner**: A "Scan text" button on the Book Detail notes header opens a live camera (`ui/OcrCaptureScreen.kt`, `camera/TextRecognitionAnalyzer.kt`) that runs on-device ML Kit Latin text recognition (free, keyless, model bundled in the APK). The recognized passage is frozen on capture, opened in an editable sheet to fix OCR slips, then saved straight into a margin note pre-tagged to the current page.

### ⌚ Watch Precision & Longevity
- [x] **Rotating Bezel / Crown Input**: The active-book screen on the watch consumes rotary scroll events (`onRotaryScrollEvent`) so a rotating bezel or crown adjusts the page count with light haptics per detent; turns are batched into a single Data Layer write ~600ms after the bezel rests, and the pending delta previews in the primary color before it commits (`wear/WearActivity.kt`).
- [x] **Ambient Burn-in Pixel-Shifting**: While the watch is in ambient mode, the on-screen title drifts a few pixels to a fresh pseudo-random offset on every ambient tick (~once a minute) so no pixel stays lit in one place, protecting OLED screens from burn-in.
- [x] **Play Store Auto-Install (verified)**: Confirmed the Wear manifest declares standalone `com.google.android.wearable` metadata and the shared `com.example.booktracker` application ID, which is what Google Play uses to offer automatic companion-watch installation when both AABs ship to the same release track (no code change needed — configuration was already correct).

---

## Pending / Future Features 🟡

*(Add any newly discussed features here before starting work on them. **CRITICAL:** When moving a feature to the Completed section, you MUST write a detailed 1-2 sentence description explaining exactly what it does, following the format of the completed features above.)*

> **Sequencing note:** Phase 1 (watch precision/longevity) and Phase 2 (search, backup, OCR)
> are done and are in the Completed section above. The items below are Phases 3–4. See
> [ROADMAP.md](ROADMAP.md) for the detailed implementation plan, ordering, and per-feature approach.

### Unscheduled / Backlog
- [ ] No more pending features in the backlog at this time.
