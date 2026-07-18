# Book Tracker App: UI Architecture & Implementation Roadmap

## Overview
This document outlines the complete UI structure for the "Nocturnal Reader" mobile application, designed with a dark Material Design 3 (Material You) aesthetic.

## 1. Visual Identity (Design System)
- **Name:** Nocturnal Reader
- **Theme:** Dark Mode
- **Color Palette:**
    - **Primary:** Violet (#D0BCFF)
    - **Surface:** Deep Charcoal (#141317)
    - **Accents:** Teal/Cyan (Success/Progress), Coral (Alerts)
- **Typography:** Inter (San-serif)
- **Shape:** Rounded Full (32px+ for cards/containers)

## 2. Existing Screen Inventory
These screens have already been designed and are ready for implementation:

### Onboarding & Goal Setting
- **Reading Goal Step 1:** Annual Target (Slider + Presets)
- **Reading Goal Step 2:** Genre Selection (Tag Cloud)
- **Reading Goal Step 3:** Reading Reminders (Time Picker)

### Core Experience
- **Home Dashboard:** Reading status, "Continue Reading" hero, recent activity.
- **Reading Statistics:** In-depth metrics, reading streaks, pages read.
- **Explore Books:** Discovery feed, search, and category browsing.
- **Reading Timer:** Active session tracking with progress ring and quick-add time.

### Library & Details
- **Book Details (Project Hail Mary):** Metadata, synopsis, and progress tracking.

### User & Management
- **User Profile:** Achievements, library insights, and "Year in Books" chart.
- **Account Settings:** Notifications, security, and data privacy controls.

## 3. Recommended Roadmap (Missing Screens)
To complete the MVP, the following views should be planned:

### Phase 1: Library Management
- **My Library (List View):** A grid or list of all tracked books (Backlog, Shortlist, Finished).
- **Add New Book (Search/Scan):** Interface for adding books via ISBN scan or manual search.

### Phase 2: Active Reading
- **Reading Session Summary:** A "Session Complete" celebration screen shown after stopping the timer, highlighting pages read.
- **Edit Progress:** A quick modal to manually update the current page/percentage.

### Phase 3: Social & Community
- **Friends Feed:** Updates on what friends are reading.
- **Book Reviews/Notes:** A dedicated space for writing and reading thoughts on a title.

## 4. Interaction Patterns
- **Navigation:** Bottom Navigation Bar (Home, Library, Stats, Profile).
- **Primary Actions:** Floating Action Button (FAB) for "Add Book" on the Home/Library screens.
- **Feedback:** Haptic feedback on timer controls and subtle micro-interactions for goal achievements.