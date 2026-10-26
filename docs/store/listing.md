# Google Play store listing (draft)

Paste these into Play Console → Grow → Store presence → Main store listing.
Character limits are Google's.

## App name (max 30)

Nocturnal Reader: Book Tracker

## Short description (max 80)

Track your reading: timer, notes, goals, streaks and stats. No account needed.

## Full description (max 4000)

Nocturnal Reader is a calm, private home for everything you read.

**Track every book**
• Add books in seconds by scanning the barcode or searching by title, author or ISBN
• Organise your shelf: Want to read, Shortlist, Up next, Reading, Paused, Finished and Did not finish
• Collections, favourites, ratings and reviews
• Import your library from Goodreads or StoryGraph

**Read with focus**
• Built-in reading timer with Pomodoro and stopwatch modes
• Log pages as you go, then see your pace and how long is left
• Optional ambient sounds and Do Not Disturb while you read
• Log past reading sessions you did away from the timer

**Keep what matters**
• Save quotes and thoughts for each book, tied to the page
• Scan a passage from the page with your camera (on-device text recognition)
• Share beautiful quote cards
• Review notes with a flashcard-style Memorize view

**See your progress**
• Daily goal, yearly book goal and reading streaks
• Reading calendar and a year-in-pixels heatmap
• Stats on pages, time, pace and your favourite genres
• Monthly and yearly wrap-ups to share

**Made for you, not for advertisers**
• No account, no ads, no tracking
• Your library stays on your phone
• Export a full backup any time, and restore it on a new phone
• App lock with fingerprint, face or screen lock
• Home-screen widget and a Wear OS companion app

## Category and tags

- App category: Books & Reference
- Tags (pick up to 5): Reading tracker, Book list, Reading log, Productivity, Library

## Graphics

- App icon (512×512): `docs/store/play-icon-512.png`
- Feature graphic (1024×500): `docs/store/feature-graphic-1024x500.png`
- Phone screenshots: at least 2 (recommended 4–8), 1080×1920 or larger, taken
  on a real device. Suggested set: Home with a book in progress, Library grid,
  Book detail, Focus timer, Stats, Notes.
- Wear OS screenshots (only if publishing the watch app): at least 1, square 384×384+.

To regenerate the icon and feature graphic with a different name or tagline:

```
python scripts/render_store_art.py docs/store "Nocturnal Reader" "Your reading, beautifully tracked"
```
