# UI & UX Guidelines

To maintain the "Premium Polish" of the Book Tracker app, all future UI additions must adhere to the following design principles.

## 1. Dynamic Theming (Material You)
The app is built to be a deeply native Android citizen.
* **No Hardcoded Colors:** Never hardcode HEX colors (e.g., `#FF0000`). Always extract colors from the `MaterialTheme.colorScheme` (e.g., `primaryContainer`, `onSurfaceVariant`).
* **Fallback Scheme:** The app defines a highly-curated fallback scheme (Indigo, Teal, Rose) in `Theme.kt`. If the user is on a pre-Android 12 device, this fallback is used.
* **Tonal Elevation:** Rely on tonal elevation for cards and surfaces rather than drop shadows.

## 2. Haptics & Tactile Feedback
The app should feel physically responsive to the user.
* Utilize `LocalHapticFeedback` for all primary or meaningful actions.
* **Long Press:** Use `HapticFeedbackType.LongPress` when the user initiates a deliberate action, such as starting or ending a reading session, or marking a book as DNF.
* **Light Interactions:** Use `HapticFeedbackType.TextHandleMove` (or similar light haptics) for scrolling adjustments, slider dragging, or quick-adding pages.

## 3. Micro-Animations & Fluidity
State changes should never be abrupt.
* When revealing or hiding UI elements (like expanding a card), always use `Modifier.animateContentSize()`.
* Animate lists using `Modifier.animateItem()` so elements smoothly rearrange when items are added, removed, or filtered.
* Use `animateFloatAsState` to animate progress bars and dynamic opacity changes.

## 4. Modern Layouts & Immersion
Avoid visually disruptive legacy Android patterns.
* **Edge-to-Edge:** Take full advantage of the device screen. Use parallax scrolling for large media assets (like Book Covers) to create depth.
* **Glassmorphism:** Use translucent overlays and frosted glass effects (e.g., changing alpha on scroll for Top App Bars) instead of solid, heavy blocks of color.
* **Bottom Sheets over Dialogs:** Prefer `ModalBottomSheet` for complex data entry (like adding a Margin Note) rather than popping a centered alert dialog, as it keeps the user grounded in their current context.
