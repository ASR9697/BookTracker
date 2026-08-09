# 🎯 SYSTEM INSTRUCTION: BOOK TRACKER APP REVIEW & IMPLEMENTATION

**Role:** You are an expert mobile/web application developer and architect.
**Task:** Review the current existing codebase of this reading tracker application against the detailed functional specifications, workflows, and logical rules provided below. Identify any missing features, UI inconsistencies, or logical gaps, and write the necessary code to implement them exactly as specified.

---

## 🏗️ MODULE 1: DATA MODELS & SCHEMA

Ensure the backend/local store database strictly contains the following relational models and fields. If fields are missing, write migration scripts to add them.

### 1. `Book` Model
*   `id` (UUID)
*   `title` (String, Required)
*   `isbn` (String, Optional)
*   `total_pages` (Integer, Required)
*   `current_page` (Integer, Default: 0)
*   `language` (String)
*   `creators`: Array of objects `[{ role: "Author" | "Translator" | "Illustrator" | "Narrator", name: String }]`
*   `publication`: `{ publisher: String, date: Date }`
*   `format`: Enum (`Paperback`, `Hardcover`, `E-book`, `Audiobook`)
*   `progress_unit`: Enum (`Page`, `Percentage`)
*   `description` (Text)
*   `series_info`: `{ is_part_of_series: Boolean, series_name: String, position: Integer }`
*   `classification`: `collections` (Array of Strings), `tags` (Array of Strings)
*   `status`: Enum (`To Read`, `Reading`, `Paused`, `Abandoned`, `Finished`)
*   `purchase_log`: Array of `[{ date: Date, vendor: String, price: Decimal, currency: String, memo: String }]`
*   `loan_record`: Array of `[{ loan_date: Date, due_date: Date, lender: String, memo: String }]`

### 2. `ReadingSession` Model
*   `id` (UUID)
*   `book_id` (Foreign Key -> Book)
*   `start_time` (Timestamp)
*   `end_time` (Timestamp)
*   `duration_seconds` (Integer)
*   `start_page` (Integer)
*   `end_page` (Integer)
*   `pages_read` (Integer -> `end_page - start_page`)

### 3. `Note` Model
*   `id` (UUID)
*   `book_id` (Foreign Key -> Book)
*   `page_number` (Integer)
*   `content` (Text)
*   `type`: Enum (`Book Content`, `Personal Thought`, `Random`)
*   `is_favorite` (Boolean)
*   `created_at` (Timestamp)

---

## 🛠️ MODULE 2: BOOK INGESTION WORKFLOWS

Implement three distinct pathways for adding a book to the library:

1.  **API Search Pathway:** Connect search bar to OpenLibrary / Google Books API. Auto-populate: Title, Cover Image, ISBN, Total Pages, Authors, Publisher, Description, and Categories.
2.  **Barcode Scanner:** Integrate the device camera to scan ISBN barcodes. Auto-query the API, preview populated fields, and save.
3.  **Manual Entry Form:** A detailed UI supporting all fields in the `Book` model. Include dynamic lists for adding multiple creators and expandable accordions for *Purchase Logs* and *Loan Records*.

---

## ⏱️ MODULE 3: LIVE TIMER & SPEED CALCULATIONS

### Timer Logic
*   Provide two modes: **Stopwatch Mode** (count-up) and **Countdown Timer** (count-down).
*   **Background State Fix:** Do not rely on frontend intervals. Record a UNIX timestamp on `start`. Calculate elapsed time using `current_timestamp - start_timestamp` when the app returns to the foreground.

### Post-Session Completion Flow
1. Prompt user: *"How much did you read?"*
2. User inputs `end_page`.
3. System calculates `pages_read` and updates `Book.current_page`.
4. Validate: If `end_page` >= `total_pages`, cap progress at `total_pages` and auto-prompt to change status to `Finished`.

### Estimated Time Remaining Algorithm
Execute this formula dynamically per book:
*   `Average Speed (pages/sec) = Sum(Total Pages Read) / Sum(Total Session Duration in sec)`
*   `Remaining Pages = total_pages - current_page`
*   `Time Remaining (seconds) = Remaining Pages / Average Speed`
*   **UI Requirement:** Display as formatted text (e.g., `9h 15m remaining`) on the active book card.

---

## 📝 MODULE 4: NOTES & OCR

1.  **Manual Capture:** Standard text field associated with a specific page number.
2.  **Camera OCR Capture:** Integrate text recognition (e.g., ML Kit). Allow the user to snap a photo of a book page and extract the text directly into the note content field. Provide a manual typing fallback if OCR fails.
3.  **Memorize Tab:** Build a dedicated UI to view all notes. Include a randomized flashcard-style review carousel and a favorite toggle.

---

## 📊 MODULE 5: STREAK ENGINE & UNCONVENTIONAL SCHEDULES

### Core Streak UI
*   7-day horizontal bubble bar (Mon–Sun).
*   Fill bubble if $\ge 1$ `ReadingSession` exists for that specific day.

### CRITICAL LOGIC: Custom Day Boundary (`day_starts_at`)
*   **Requirement:** Users must be able to customize when a "day" rolls over to accommodate late-night workflows.
*   **Test Case Implementation:** Set the default or test user schedule configuration so that a day starts at **03:00 AM** (not 00:00). 
*   **Logic Rule:** If a user logs a reading session at `02:30 AM` on a Wednesday, the system *must* credit this session to **Tuesday's** daily statistics and streak. The calculation engine must evaluate the `start_time` timestamp against this localized `day_starts_at` boundary.

---

## 🔌 MODULE 6: ARCHITECTURE & UX FAILSAFES

### 1. Offline-First Sync
*   All CRUD operations must write to the local database first (Optimistic UI) to prevent data loss during commutes.
*   Queue network requests and sync when internet connection is restored.

### 2. Cascading Deletes
*   If a `Book` is deleted, the system must securely delete all tied `ReadingSession` and `Note` records.

### 3. UX Polish
*   **Loaders:** Use skeleton cards for API fetches, avoiding blocking loading spinners.
*   **Haptics:** Trigger native haptic feedback on timer start/stop, successful barcode scans, and streak completions.
*   **Rate Limiting:** Implement debouncing (500ms) on API searches. Handle HTTP 429 gracefully with a toast notification.

---
**END OF SPECIFICATION. AGENT, PROCEED WITH CODEBASE AUDIT AND REFACTORING.**