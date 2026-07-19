# Architecture Overview

This document outlines the core architectural decisions and data flow for the Book Tracker application.

## 1. Local-First Philosophy
Currently, the application has **no cloud backend**. The phone acts as the central hub and the single source of truth.
* All data is persisted locally using an **Android Room Database**.
* This approach ensures the app is lightning-fast, highly private, and completely functional offline.

## 2. Future-Proofing for Cloud Sync (Firebase)
Although the app is local-first, the architecture was intentionally designed to support a seamless transition to a cloud backend (like Firebase Firestore) in the future without rewriting the UI or business logic.

* **Repository Pattern:** The UI layers interact exclusively with the `BookRepository` interface. The underlying Room implementation can easily be swapped or augmented with a Firestore implementation later.
* **UUIDs for Primary Keys:** Book IDs are generated clientside as UUIDs (`UUID.randomUUID().toString()`). This ensures that if we migrate to Firestore, these IDs can be used directly as document IDs without any complex remapping.
* **Timestamping (Last-Write-Wins):** Every single data mutation (updating a book, logging a session) explicitly stamps a `lastUpdated` field with the current epoch milliseconds. This is critical for resolving sync conflicts.

## 3. Wear OS Sync Strategy
The Wear OS watch app operates as a thin client. It never talks to the internet directly to fetch user data.

* **Transport Layer:** Synchronization happens via the **Bluetooth Wearable Data Layer** provided by Google Play Services.
* **Data Contracts (`:shared`):**
  * The Phone publishes the active, currently reading book to the URI path `/active_book`.
  * The Watch publishes reading progress updates (+1, +10 pages) to the URI path `/progress/{bookId}`.
* **Conflict Resolution:** If the user updates progress on the phone and the watch while disconnected, the system resolves the conflict using a strict **Last-Write-Wins (LWW)** policy, evaluated against the `lastUpdated` timestamp.
* **Cost Efficiency:** Because the watch routes through the phone via Bluetooth, we avoid needing server-side custom auth tokens or paid Cloud Functions.

## 4. Module Separation
* **`:shared`**: Pure Kotlin models (`Book`, `Session`, etc.) and constants. Contains **zero Android dependencies** by design.
* **`:app`**: The primary phone/tablet application. Contains Compose UI, Room persistence, and API clients (Google Books).
* **`:wear`**: The standalone Wear OS application. Contains Wear Compose UI and the Data Layer listener services.
