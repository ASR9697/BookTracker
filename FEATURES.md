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
- [x] **Virtual 3D Spine Bookshelf View**: Realistic wooden and dark bookcase layout where books stand side-by-side with page-proportional spine thickness, cover-derived and genre binding palettes, embossed metallic gold titles, dangling silky bookmark ribbons for active reads, and smooth spring pull-out selection inspection cards (`ui/VirtualBookshelfView.kt`).
- [x] **Annual Reading Heatmap ("Year in Pixels")**: A 52-week horizontal calendar reading intensity matrix with 5 dynamic tiers (Neutral, Soft Tint, Radiant Primary, High-Intensity Accent, and Fiery Flame) reflecting daily pages read, featuring month headers, day labels, and an interactive bottom sheet detailing logged sessions, completed books, and margin notes for any inspected day (`ui/ReadingHeatmap.kt`, `ui/ReadingCalendarView.kt`, `ui/ProfileScreen.kt`).
- [x] **Single Book "Finished Wrap-Up" Victory Card**: High-definition, shareable Canvas victory graphic card generated when completing a book (in both 1:1 Square and 9:16 Story formats) featuring the book cover art with ambient blur backdrop, star ratings and multi-axis score breakdown, personal review snippet or top highlighted quote, and reading pace analytics with 5 distinct luxury aesthetics (`ui/BookVictoryShareHelper.kt`, `ui/BookVictoryCardDialog.kt`).
- [x] **Smart Reading Challenges & Goal Pace Forecaster**: Velocity-based year-end completion forecast (`projectedFinishBooks` and pace status), 30-day rolling habit consistency percentage and rating tiers ("Elite Daily Habit", "Consistent Reader", etc.), and 4-season reading sprint challenges (Spring, Summer, Autumn, Winter 5-book sprints) with countdowns, status messages, and animated progress gauges (`analytics/GoalEngine.kt`, `ui/AnalyticsScreen.kt`, `ui/ProfileScreen.kt`).
- [x] **Library Loan Due Date Alerts & Lending Tracker**: Visual due date countdown badges for library loans and peer lending tracker ("Due in 2 days", "Overdue by 1 day", "Lent to Alice"), with urgent alert banners on the Home dashboard when due within 3 days, dedicated "On Loan" filter chips in library view, loan badges on book cards, authentic bookshelf spine stamps (`🏛️` and `🤝`), and a full lending manager dialog with presets and return status recording (`analytics/LoanEngine.kt`, `ui/LoanComponents.kt`, `ui/BookDetailScreen.kt`, `ui/BookTrackerScreen.kt`, `ui/VirtualBookshelfView.kt`).

### 🚀 Release Readiness & Polish (v2.1)
- [x] **Edit Book Details**: Book Detail → ⋮ → *Edit details* fixes the title, authors, page count, ISBN, cover (URL or camera photo), publisher, series, genres, description and — for finished books — the finish date, so a wrong API result is no longer permanent (`ui/BookEditDialogs.kt`).
- [x] **Status Picker & Log Past Sessions**: Tapping the status pill (or ⋮ → *Change status*) moves a book to any pipeline stage, routing Finished/DNF through their rating and reason dialogs; ⋮ → *Log a reading session* (also the *Log* button on the Sessions tab) back-fills a session with date, page range and minutes without ever rewinding current progress.
- [x] **Reliable Finish Dates**: Books now store `finishedAt` (DB v9 migration backfills it), so yearly goals, calendars, wrap-ups and heatmaps no longer drift when a finished book is later favourited, loaned or edited; CSV imports use Goodreads/StoryGraph read dates.
- [x] **Overall Star Rating**: The finish dialog leads with a tappable half-star rating, with the five detail axes optional and unset by default; ratings show as stars on the detail header and are used by recommendations, share cards and library sorting.
- [x] **Library Sorting & Collections Filter**: Sort by recently updated, title, author (surname), date added, progress or rating, and filter by collection; filter and sort choices survive navigation.
- [x] **Daily Reading Reminder Settings**: Opt-in reminder with a time picker (asks for notification permission at that moment), rescheduled whenever the setting changes; it only fires if the daily goal isn't met yet.
- [x] **Safer Undo, Backup & Import**: Undoing a book delete restores its sessions and notes too; backups (format v2) keep every field plus on-device cover photos and still read v1 files; CSV import handles StoryGraph columns, quoted multi-line reviews and skips books already in the library.
- [x] **Privacy & Play Store Compliance**: In-app privacy policy (Settings → About), opt-in notification permission, app lock that can't be bypassed and re-locks after a minute away, R8 shrinking, 16 KB-aligned native libraries, release signing via `keystore.properties`, and a guard against publishing with a `com.example` ID (see `RELEASE.md`).

### 🔎 Search, Data & OCR
- [x] **Universal Full-Text Search**: A search icon in the top app bar opens a dedicated screen that queries a Room FTS4 index across every book field (title, author, description, genre) *and* the full text of all margin notes/quotes, grouped into "Books" and "Margin notes" result sections (`ui/LibrarySearchScreen.kt`, `BookRepository.searchLibrary`). Raw input is turned into a forgiving prefix query so partial words match, and it runs 100% offline against the local database.
- [x] **Local Backup & Export**: From Settings → Backup & restore, the entire library — books, sessions, margin notes and goal settings — serializes to a versioned JSON document inside a portable `.zip` written wherever the system file picker points (`data/BackupEngine.kt`). Restore reads the `.zip` (or a bare `.json`) and upserts by id so it safely merges onto an existing library and preserves `lastUpdated` for sync consistency. No cloud, no accounts.
- [x] **Smart OCR Scanner**: A "Scan text" button on the Book Detail notes header opens a live camera (`ui/OcrCaptureScreen.kt`, `camera/TextRecognitionAnalyzer.kt`) that runs on-device ML Kit Latin text recognition (free, keyless, model bundled in the APK). The recognized passage is frozen on capture, opened in an editable sheet to fix OCR slips, then saved straight into a margin note pre-tagged to the current page.

### ⌚ Watch Precision & Longevity
- [x] **Rotating Bezel / Crown Input**: The active-book screen on the watch consumes rotary scroll events (`onRotaryScrollEvent`) so a rotating bezel or crown adjusts the page count with light haptics per detent; turns are batched into a single Data Layer write ~600ms after the bezel rests, and the pending delta previews in the primary color before it commits (`wear/WearActivity.kt`).
- [x] **Ambient Burn-in Pixel-Shifting**: While the watch is in ambient mode, the on-screen title drifts a few pixels to a fresh pseudo-random offset on every ambient tick (~once a minute) so no pixel stays lit in one place, protecting OLED screens from burn-in.
- [x] **Play Store Auto-Install (verified)**: Confirmed the Wear manifest declares standalone `com.google.android.wearable` metadata and the shared `com.example.booktracker` application ID, which is what Google Play uses to offer automatic companion-watch installation when both AABs ship to the same release track (no code change needed — configuration was already correct).

### 🎨 Personalization, Smart Vision & Sync
- [x] **Aesthetic Theme & App Icon Studio**: Settings → Appearance adds four palettes (Matcha, Nordic Fog, Warm Papyrus, and OLED Midnight with true-black surfaces) beside the existing five, with Espresso renamed "Espresso & Leather". An icon row switches the launcher icon between Classic, Golden Tome, Minimalist Moon, Fountain Pen and Pixel Bookshelf, each an `<activity-alias>` toggled through the package manager (`ui/theme/Color.kt`, `ui/theme/AppIcon.kt`, `ui/SettingsScreen.kt`).
- [x] **Dual-Format Tandem Reading (Physical + Audiobook)**: A "Tandem reading" card on the book page stores the audiobook's running time and the reader's speed (1x–2x, DB v14). One bar shows the page and the matching point in the narration. "Log listening" turns real minutes at the chosen speed into pages and logs them as a session, and "Set position" jumps both editions to the player's elapsed time (`analytics/TandemEngine.kt`, `ui/TandemCard.kt`).
- [x] **On-Device Translation & Dual-Language Reader**: A Translate button in the OCR capture sheet and on every note card detects the passage's language and translates it on the phone with ML Kit (one ~30 MB model per language pair, downloaded once). From OCR the reader can keep the translation or both texts, and tapping any word of the original saves it to Vocabulary with its translation and source sentence, so it joins the daily review (`language/PassageTranslator.kt`, `language/VocabCandidates.kt`, `ui/TranslateSheet.kt`).
- [x] **"Clean-Page" Auto-Cropper & Shadow Remover**: "Clean page" in the note photo row, and a scan button next to the cover field in Edit details, open Google's document scanner from Play services. It detects the page corners, flattens perspective and offers shadow and finger removal before the crisp JPEG is added as a note photo or the book's cover (`camera/PageScanner.kt`).
- [x] **Smart OCR Typo & Hyphenation Auto-Fixer**: All captured page text (live OCR and "extract text from photo") runs through a conservative cleaner. It rejoins words hyphenated across lines while keeping real hyphens like "Anglo-Saxon", unwraps paragraph lines, fixes "wor1d" to "world", "| think" to "I think" and "rnodern" to "modern" (only into known words), tidies spacing and dashes, and sets curly typographic quotes (`camera/OcrTextCleaner.kt`).
- [x] **Google Drive AppData Cross-Device Sync**: Settings → Data → "Sync across devices" connects the reader's Google account with the `drive.appdata` scope only. The library document and the cover and note photos sync through a private app folder in their own Drive, with no server of ours involved. Each sync is a three-way merge against the last agreed copy, so edits and deletions carry across: the newer book edit wins, an edit beats a delete, and a running timer session never syncs. A WorkManager job repeats it every 6 hours on a network connection (`sync/SyncMerge.kt`, `sync/DriveSync.kt`, `sync/DriveClient.kt`, `sync/DriveAuth.kt`, `worker/DriveSyncWorker.kt`). It needs a one-time Google Cloud OAuth client; see `RELEASE.md`.

### 📱 Tablets, Foldables & Large Screens
- [x] **Adaptive App Shell**: The layout follows the window, not the device: phones keep the floating bottom bar, 600–1200dp windows get a navigation rail with Add book on top, and 1200dp+ a labelled side drawer with Daily review and Settings. Split-screen on a tablet falls back to the phone layout (`ui/Adaptive.kt` `WidthTier`/`LocalWidthTier`, `ui/BookTrackerScreen.kt`).
- [x] **Readable Widths & Multi-Column Screens**: Home becomes two columns (current book and sections alternating), Stats an adaptive 1–3 column card grid, the Notes feed a masonry wall, Achievements an adaptive grid, and library covers grow (100/128/148dp tiles). Settings, Goals, session save/recap, review cards, history, search, vocabulary, finished and the privacy policy centre at a readable ~720dp with scrolling margins.
- [x] **List-Detail Panes**: On landscape tablets a tapped book opens beside the library instead of over it (`ui/BookDetailRoute.kt`, shared with the phone route). The vocabulary list keeps the selected word's editor open on the right, and the book page itself splits into a fixed header (cover, details, actions) and scrolling tabs once it is 840dp wide.
- [x] **Tablet Reading Sessions**: The focus timer gets a notes pane on wide screens for jotting a thought or quote at the current page without leaving the clock. On a half-open foldable (tabletop posture) the clock sits above the hinge and the controls below it (`ui/TimerNotesPane.kt`, `ui/Posture.kt`, androidx.window).
- [x] **Keyboard, Mouse, Drag & Drop, Stylus**: Ctrl+F search, Ctrl+N add a book, Ctrl+R daily review, Ctrl+, settings and Ctrl+1–5 for the sections, all listed in Android's shortcut helper (Ctrl+/); Space starts or pauses the timer. Right-click opens a book's quick actions on library covers, Up next and the shelf. Photos can be dragged into a note from another app in split-screen and notes dragged out as text, and the handwriting sheet gets a wider sheet and a 420dp canvas (`ui/TabletInput.kt`, `MainActivity.onKeyShortcut`).
- [x] **Large-Screen Polish**: Wrapped slides keep their 9:16 story shape on tablets (and in the shared image), share-card dialogs cap at 480dp, and the Up next and Year-in-pixels widgets gain 480–640dp size buckets for tablet home screens.

### ⏱️ Reading Sessions
- [x] **Live Timer**: The status line shows this session's measured pace once a page has turned (or the book's usual pace before that), "+N pages this session" under the clock, and how long a pause has lasted. Count-up mode has no ring unless a page goal is set. After 30 minutes untouched the timer asks "Still reading?" and pauses itself five minutes later, taking those five minutes back (Settings → Reading). Before the clock starts, a card shows when the book was last read and the last note in it (`ui/FocusTimerScreen.kt`).
- [x] **Page Goals**: A session can aim for a page (+10/+20/+30/+50, to the end, or a typed page), set or changed at any time; the ring fills towards it, and reaching it buzzes in the app or posts an alert outside it. It survives a restart with the rest of the timer state (`ServiceLocator.sessionPageTarget`, `TimerPersistence`).
- [x] **Session Save Review**: The page picker opens on arrival (or the page scanner, with "Scan the page when you finish" on), with Same page / +5 / +10 / +20 steps and a live "+N pages · pace" line. The reader can rate focus 1–5 (stored as `Session.focus`, DB v15), keep a line from the book as a note, join the session onto one of the same book that ended within 30 minutes, and is asked before saving a session shorter than the chosen minimum with no pages (`ui/SessionSaveScreen.kt`, `BookRepository.endSessionDetailed`).
- [x] **Session Recap**: A shareable card with the ring (this session's stretch highlighted), time / pages / pace, a comparison with the book's usual pace and the focus rating; then pages and time left, and what the session counted towards: today's goal, the streak, a finish-by plan and the nearest badge. It has confetti when the daily goal is reached, plus Add a note, Share and Done (`ui/SessionResultScreen.kt`).
- [x] **Focus Insights**: Stats shows average focus per time of day and per place, and names the best of each once there's a clear winner (`analytics/FocusInsights.kt`).
- [x] **Timer Notification**: A self-ticking clock, pages and "+N this session", the page goal (or book) as a progress bar and the book cover, with Pause/Resume, +1 page and Finish; tapping it opens the timer (`service/TimerService.kt`).
- [x] **Wishlist**: A page of its own (Home tile, Profile row) for the buying side of a book, separate from its reading status: priority (Must have / Want / Maybe), expected price and store, who recommended it, a note, and a release day. It shows the total expected cost and a monthly book budget measured against logged purchases, filters (Must have, In a series, Coming soon, Fits the budget) and four sorts. "Next in your series" finds the next number of a series you've started. "Got it" (button or swipe right) logs the purchase prefilled from the wishlist, or marks a gift. The list can be shared as text for gift-givers, "Find it online" searches the web, and a 9 AM notification fires the day a wished-for book comes out. Search and ISBN scan can add straight to the wishlist, and so can a long-press on a library cover. Details are stored as `Book.wish` (DB v16, in backups) (`ui/WishlistScreen.kt`, `analytics/WishlistEngine.kt`, `worker/NudgeWorkers.kt`).
- [x] **Reading Session Widget**: The session in progress on the home screen: a self-ticking clock, pages this session and the page goal, with Pause/Resume, +1 and Finish. With no session running, it offers to start one on the latest book (`widget/SessionWidget.kt`).


### ⚙️ Settings & Finished Shelf
- [x] **Settings Hub**: Settings opens on a profile card and one row per area (Appearance, Reading & timer, Goals & reminders, Notifications, Privacy & security, Data & sync, Motion & feel, About), each showing its current value and sliding into a page of its own (`ui/SettingsScreen.kt`). The finer preferences live in one `AppPrefs` value (`data/SettingsRepository.kt`), provided to every screen as `LocalAppPrefs` (`ui/AppPrefsUi.kt`).
- [x] **New Preferences**: Nocturnal mode (dark between chosen hours), text size, serif notes, Home sky on/off, week starts Monday or Sunday (streak week, calendars, heatmap, widgets), default format for new books, show/hide time-left estimates, streak-at-risk / weekly recap / daily review notifications with quiet hours (`worker/NudgeWorkers.kt`), hide titles in widgets and the timer notification, App lock delay, screenshot blocking, weekly automatic backup to a chosen folder keeping the newest four (`worker/AutoBackupWorker.kt`), Goodreads-style CSV export (`util/CsvExporter.kt`), storage use with an image-cache clear, and Delete all data behind a typed confirmation (refused while Drive sync is on).
- [x] **Finished Shelf & Reading Journey**: Finished books open on a count-up summary (books, pages, hours, average rating) with Finished / Did not finish tabs, year chips and month headers; each row shows finish date, days taken, reading time and stars (`ui/FinishedScreen.kt`, `analytics/FinishedShelf.kt`). The Reading Journey draws a winding path row by row so it always meets the covers, traces itself in as you scroll, and marks every fifth book and each year (`ui/ReadingJourneyMap.kt`).

---

## Pending / Future Features 🟡

*(Add any newly discussed features here before starting work on them. **CRITICAL:** When moving a feature to the Completed section, you MUST write a detailed 1-2 sentence description explaining exactly what it does, following the format of the completed features above.)*

> **Sequencing note:** All previous roadmap phases (1 through 5) are complete and listed above.

### 🔥 Habit Protection & Monetization (Feature D)
- [ ] **Streak Freeze & Mindful Rest Days**: Protect reading momentum by allowing readers to preserve their active streak during emergencies, travel, or illness with a calm frost badge (`🧊`) on the calendar. Provide 1–2 free freezes per period, with additional freeze packs available for purchase to support the app and safeguard long-term streaks.

### 📸 Smart Library Onboarding & Vision
- [ ] **Shelfie Batch Importer (Bookshelf Spine OCR)**: Snap a single photo of a physical bookshelf or book stack; on-device text recognition extracts titles along the book spines (`ShelfSpineParser.kt`) and searches Google Books/Open Library, letting users review matches and batch-add 10–20 books to their Backlog in seconds.

### 🎯 Pacing & Bedtime Planning
- [ ] **Bedtime & Chapter Pace Coach**: Velocity-based reading planner that answers "Can I finish this chapter tonight?" by calculating whether a reader will hit a target chapter or page milestone before their chosen bedtime (e.g., 11:15 PM) based on their real historical reading speed.

### 🗣️ Vocabulary & Active Learning
- [ ] **Daily 3-Minute Vocabulary Sprint**: Gamified flashcard drill in the Notes/Review tab utilizing the built-in `ReviewEngine` (Leitner spaced repetition) to test readers on unfamiliar words and literary terms captured during reading sessions, with review streak tracking and on-device pronunciation.




