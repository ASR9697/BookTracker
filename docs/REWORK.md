# Rework backlog

Changes we've agreed on but not built yet, one section per page. Plan a page here
first, then build it in small PRs. When an item ships, delete it (git history keeps
the record); when a page is done, remove its section and its row in the index.

Paths are relative to `app/src/main/java/com/example/booktracker/` unless noted.

## Pages

| Page | File | Status |
|---|---|---|
| (none planned yet) | | |

<!-- Status: Idea → Planned, decisions open → Ready to build → In progress -->

## Who owns what

Agreed split so pages stop repeating each other. Check new ideas against it.

| Page | Owns | Doesn't show |
|---|---|---|
| Home | Today: current book, today's ring, streak, glance at the year | History, breakdowns |
| Stats | Numbers over a chosen period: activity, rhythm, pace, what you read | Goal cards, book lists, personal records |
| Goals (`GoalsScreen`) | Every target and its progress, editing targets | Charts |
| Profile | Who you are: taste, favourites, top authors, records, achievements | Today, goal pace, currently reading |
| Finished | Every ended read, month by month | Charts |

---

## Cross-page notes

- **Not reviewed yet:** `SessionResultScreen.kt`, the recap shown after saving a session.

---

<!--
New page template: copy below, add a row to the Pages table.

## <Page name>

**File:** `app/ui/<Screen>.kt` · **Status:** Idea

**Goal:** one or two lines on what this page is for and what's wrong today.

### Decisions
- [ ] ...

### Layout, top to bottom
1. ...

### Fixes
- ...

### Extras
| # | Idea | Effort |
|---|---|---|

### Files
- ...

### PRs
1. ...
-->
