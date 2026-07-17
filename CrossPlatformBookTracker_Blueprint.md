# 📚 Cross-Platform Book Tracker: Antigravity Workspace Blueprint

## 1. System Architecture & Module Topology
The project is a single multi-module Gradle project to maintain a unified package name and signing configuration. This is required for the Wearable Data Layer API to communicate via Bluetooth.

*   **`:shared` (Kotlin Multiplatform/Android Library):** Holds pure Kotlin data models, serialization utilities, and global constants.
*   **`:app` (Android Application):** Phone/Tablet app using Jetpack Compose, Material 3, CameraX, and Firebase Android SDK.
*   **`:wear` (Wear OS Application):** Standalone watch application using Compose for Wear OS, Wear Ambient APIs, and Firebase SDK.

## 2. Exhaustive Data Model & Firestore Schema (Server Timestamps)
### `/users/{userId}`
*   `uid`: String
*   `displayName`: String
*   `dailyGoalPages`: Int (Default: 20)
*   `currentStreak`: Int (Default: 0)

### `/users/{userId}/books/{bookId}`
*   `id`: String
*   `title`: String, `authors`: List<String>, `coverUrl`: String
*   `totalPages`: Int, `currentPage`: Int
*   `status`: String (Enum: `backlog`, `shortlist`, `up_next`, `reading`, `finished`, `dnf`)
*   `lastUpdated`: Timestamp (Server Timestamp)
*   `rating`: Map (pacing, focus, vibe as Floats 0.0 to 5.0)

### `/users/{userId}/books/{bookId}/sessions/{sessionId}`
*   `id`: String
*   `startTime`: Timestamp, `endTime`: Timestamp
*   `startPage`: Int, `endPage`: Int, `pagesRead`: Int
*   `deviceSource`: String (`phone`, `watch`)

## 3. Feature Specifications

### A. Material You Command Center (`:app`)
*   **Dynamic Theming:** Implement MD3 `dynamicDarkColorScheme` / `dynamicLightColorScheme`.
*   **Kanban TBR Pipeline:** Clean `HorizontalPager` with tabs for Backlog, Shortlist, and Up Next. Use `SwipeToDismissBox`. 
*   **CameraX Barcode Scanning Engine:** Integrate ML Kit's Barcode Scanning API for `Barcode.FORMAT_ISBN` to query Google Books API.
*   **Analytics Dashboard:** 7x52 canvas grid tracking pages read per day. 

### B. Wear OS Frictionless Logger (`:wear`)
*   **Active State Interface:** Massive centered text layout displaying the `currentPage` counter flanked by discrete `+1` and `+10` action buttons triggering native haptics.
*   **Ambient Mode Interface:** Use `AmbientLifecycleObserver`. Upon `onEnterAmbient`, keep at least 85% of the screen black to conserve power. Replace live updating UI values with static placeholder content to prevent misleading information. 

## 4. Sync, Bridge & Conflict Resolution
| Device State | Primary Routing | Sync Mechanism | Conflict Resolution Strategy |
|---|---|---|---|
| **Online** | Direct Cloud | Firebase Firestore SDK sync. | **LWW (Last-Write-Wins):** Based on `lastUpdated`. |
| **Bluetooth** | Watch to Phone | Wearable `DataClient` layer. | Phone intercepts event, updates cache, queues to Firestore. |
| **Offline** | Local Cache | SQLite/Room & Firestore Cache. | Watch transmits local database history via `MessageClient` on reconnect. |

## 5. Execution Milestones
1.  **Environment Setup & Shared Module:** `build.gradle.kts` configuration, Shared Data Models.
2.  **Zero-Touch Security Architecture:** Wearable Data Layer API background authentication pipe.
3.  **Core Database Engine:** Firestore Security Rules and debug mockup injection.
4.  **Jetpack Compose UI:** Dashboard Hero Section, Tabbed Horizontal Pager, Wear OS Ambient states.
5.  **Camera Engine & Polish:** ML Kit bindings, integration verification, MD3 color contrast checks.

## 6. Agent Efficiency & Compilation Constraints
*   **Idempotent Modifications:** Check if a dependency or plugin already exists in the build files before appending new blocks to prevent duplicate errors.
*   **Pre-Compilation Validation:** Before implementing complex Compose UI trees, run `./gradlew assembleDebug` or compile tasks to verify core abstractions compile.
*   **Diff-Driven Code Editing:** When editing existing files, prioritize outputting patch/diff structures rather than printing full-file contents.

## 7. Phase 2: Advanced Features (From PRD)
### A. Advanced Analytics & Contextual Logging
*   **Multi-Axis Qualitative Ratings:** Slider metrics for Pacing, Focus, and Vibe.
*   **Granular DNF Tracking:** Captures abandonment percentage and specific reasons.
*   **Reading Velocity & Context:** Tracks pages per minute; tags sessions with situational context (e.g., morning coffee).
*   **Frequent Flyer Streak Engine:** Rolling 24-hour UTC block for streak goals.

### B. Media, Format & Hardware Adaptability
*   **Serialized & Multi-Format Mode:** Tracking for Manga (Volumes/Chapters), Audiobooks (Hours/Minutes), and Custom Whitepapers.
*   **Adaptation Bridge:** Maps reading progress to anime-canon episodes.
*   **Seamless TTS Handoff:** Native TTS engine for EPUBs/documents.
*   **Hardware Integrations:** Stylus Quick-Capture (Digital Ink), Android Auto UI, Google Cast Big Screen Analytics, ARM-Optimized Desktop PWA.

### C. Lifestyle Planning & Knowledge Management
*   **Smart Scheduling:** Shift-Aware Wind Down Reminders, Location-Based Reading Zones, The Transit Optimizer.
*   **Session Management:** Life Interruption Pause, Guilt-Free Snooze, Companion Care Timers (dual-tracking).
*   **Journaling Engine:** Wrist Dictaphone (speech-to-text to Firestore), Chronological Margin Notes, Markdown Formatting, Dynamic Audio Intent Linking.

## 8. Phase 2 Execution Milestones
6.  **Advanced Data Schema Expansion:** Update Firestore rules and Shared Models for context logging, DNF tracking, and multi-format support.
7.  **Analytics & Context Engine:** Implement Reading Velocity charts, Environment Correlator, and Multi-Axis sliders in `:app`.
8.  **Format Adaptability Layer:** Add Serialized Content Mode and TTS Handoff.
9.  **Hardware Integrations:** Add Android Auto module and Google Cast support.
10. **Journaling & Smart Planning:** Wear OS Wrist Dictaphone, Chronological Margin Notes, and Geofenced Reading Zones.