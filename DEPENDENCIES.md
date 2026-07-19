# Book Tracker Dependencies

This file serves as a reference for all the major libraries, plugins, and dependencies used across the Book Tracker project. 

## Project Level (Plugins & Build Tools)
- **Gradle:** 9.3.0
- **Android Gradle Plugin (AGP):** 9.3.0
- **Kotlin:** 2.2.20
- **KSP (Kotlin Symbol Processing):** 2.3.2
- **Compose Compiler Plugin:** 2.2.20

## Phone App (`:app`)

### UI & Compose
- **Compose BOM (Bill of Materials):** 2025.09.00
- **Compose UI Core:** `androidx.compose.ui:ui`
- **Material 3:** `androidx.compose.material3:material3`
- **Material Icons Extended:** `androidx.compose.material:material-icons-extended`
- **Compose Foundation:** `androidx.compose.foundation:foundation`
- **Activity Compose:** `androidx.activity:activity-compose:1.10.1`
- **ViewModel Compose:** `androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4`
- **Navigation Compose:** `androidx.navigation:navigation-compose:2.8.8`
- **Google Fonts:** `androidx.compose.ui:ui-text-google-fonts`
- **Core Splashscreen:** `androidx.core:core-splashscreen:1.0.1`

### Storage & Persistence
- **Room Database:** `androidx.room:room-runtime:2.8.4` (Compiler via KSP: `androidx.room:room-compiler:2.8.4`)
- **DataStore Preferences:** `androidx.datastore:datastore-preferences:1.1.7` (For settings persistence)

### Media & Networking
- **Coil Compose:** `io.coil-kt.coil3:coil-compose:3.4.0` (Image loading)
- **Coil OkHttp Network:** `io.coil-kt.coil3:coil-network-okhttp:3.4.0`

### Hardware & ML (ISBN Scanning)
- **CameraX Core/Camera2:** `androidx.camera:camera-camera2:1.4.2`
- **CameraX Lifecycle:** `androidx.camera:camera-lifecycle:1.4.2`
- **CameraX View:** `androidx.camera:camera-view:1.4.2`
- **Google ML Kit (Barcode Scanning):** `com.google.mlkit:barcode-scanning:17.3.0`

### Wear OS Integration
- **Play Services Wearable:** `com.google.android.gms:play-services-wearable:19.0.0` (Data Layer Sync)
- **Coroutines for Play Services:** `org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2`

### Widgets
- **Glance AppWidget:** `androidx.glance:glance-appwidget:1.1.1`

---

## Wear OS App (`:wear`)

### UI & Compose for Wear OS
- **Compose BOM:** 2025.09.00
- **Wear Compose Material 3:** `androidx.wear.compose:compose-material3:1.6.2`
- **Wear Compose Foundation:** `androidx.wear.compose:compose-foundation:1.6.2`
- **Wear Ambient/Core:** `androidx.wear:wear:1.3.0`
- **Activity Compose:** `androidx.activity:activity-compose:1.10.1`
- **Wear OS Complications (Data Source):** `androidx.wear.watchface:watchface-complications-data-source-ktx:1.3.0`

### Wear OS Integration
- **Play Services Wearable:** `com.google.android.gms:play-services-wearable:19.0.0` (Data Layer Sync)
- **Coroutines for Play Services:** `org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2`

---

## Shared Module (`:shared`)

*(Intentionally left dependency-free to isolate pure Kotlin domain models and Data Layer constants from Android/framework dependencies.)*
