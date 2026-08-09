---
name: Literary Motion
colors:
  surface: '#FFFBFE'
  surface-dim: '#dbd9e2'
  surface-bright: '#fbf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f4f2fc'
  surface-container: '#efedf6'
  surface-container-high: '#e9e7f0'
  surface-container-highest: '#e3e1ea'
  on-surface: '#1a1b22'
  on-surface-variant: '#454652'
  inverse-surface: '#2f3037'
  inverse-on-surface: '#f2eff9'
  outline: '#757684'
  outline-variant: '#c5c5d4'
  surface-tint: '#4355b9'
  primary: '#24389c'
  on-primary: '#ffffff'
  primary-container: '#3f51b5'
  on-primary-container: '#cacfff'
  inverse-primary: '#bac3ff'
  secondary: '#006a60'
  on-secondary: '#ffffff'
  secondary-container: '#85f6e5'
  on-secondary-container: '#007166'
  tertiary: '#890035'
  on-tertiary: '#ffffff'
  tertiary-container: '#b50048'
  on-tertiary-container: '#ffc3cb'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dee0ff'
  primary-fixed-dim: '#bac3ff'
  on-primary-fixed: '#00105c'
  on-primary-fixed-variant: '#293ca0'
  secondary-fixed: '#85f6e5'
  secondary-fixed-dim: '#67d9c9'
  on-secondary-fixed: '#00201c'
  on-secondary-fixed-variant: '#005048'
  tertiary-fixed: '#ffd9de'
  tertiary-fixed-dim: '#ffb2be'
  on-tertiary-fixed: '#400014'
  on-tertiary-fixed-variant: '#900038'
  background: '#fbf8ff'
  on-background: '#1a1b22'
  surface-variant: '#e3e1ea'
  primaryContainer: '#E8EAF6'
  onPrimaryContainer: '#1A237E'
  secondaryContainer: '#E0F2F1'
  onSecondaryContainer: '#004D40'
  surfaceVariant: '#E7E0EC'
  onSurfaceVariant: '#49454F'
  glassOverlay: rgba(255, 255, 255, 0.7)
typography:
  display-lg:
    fontFamily: Manrope
    fontSize: 57px
    fontWeight: '700'
    lineHeight: 64px
    letterSpacing: -0.25px
  headline-lg:
    fontFamily: Manrope
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Manrope
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  title-lg:
    fontFamily: Manrope
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.5px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.25px
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  baseline: 4px
  margin-mobile: 16px
  margin-tablet: 24px
  gutter: 16px
  touch-target: 48px
---

## Brand & Style

The design system is centered on a **Premium Android Native** aesthetic, blending the structured logic of Material 3 with the sophisticated depth of modern glassmorphism. It targets readers who value focus and tactility, evoking a sense of calm, digital craftsmanship. 

The visual style is **Corporate / Modern** with a **Tactile** edge. It leverages Material You's dynamic theming to ensure the app feels like an organic extension of the user's device. Key characteristics include:
- **Immersive Surfaces:** Edge-to-edge layouts that treat book covers as primary visual anchors.
- **Dynamic Adaptability:** A UI that breathes through color shifts and fluid motion.
- **Physicality:** Every interaction is grounded in haptic feedback and spatial transitions, moving away from "flat" digital patterns toward a more responsive, physical experience.

## Colors

The color strategy relies on **Material 3 Dynamic Theming**. While the system prioritizes user-generated palettes via Android 12+ APIs, the fallback palette uses a curated mix of Indigo (Primary) for trust, Teal (Secondary) for progress, and Rose (Tertiary) for emotive accents like "Finished" states or "Favorites."

- **Tonal Logic:** Avoid hardcoded neutrals. Use `surfaceVariant` for secondary containers and `onSurfaceVariant` for medium-emphasis text.
- **Transparency:** Glassmorphism is achieved using `glassOverlay` with a background blur (15px - 25px). This is reserved for `TopAppBar` and floating contextual overlays to maintain legibility while preserving the sense of the content underneath.

## Typography

Typography is clean and systematic. **Manrope** provides a refined, modern geometric feel for headlines that remains approachable. **Inter** is utilized for body copy to ensure maximum legibility during long reading sessions. 

For technical data—such as "Page 342 of 500" or reading timestamps—**JetBrains Mono** is used as a label font to provide a subtle "utility" feel that contrasts against the more literary headings. 

On mobile devices, scale `headline-lg` down to the mobile variant to prevent awkward line breaks on narrow viewports.

## Layout & Spacing

This design system uses a **Fluid Grid** model based on an 8dp (8px) rhythm, with a 4px baseline for micro-adjustments. 

- **Edge-to-Edge:** Layouts must extend behind the system status and navigation bars. Use window insets to provide appropriate padding for content.
- **Reflow:** On mobile, content is a single column with 16px margins. On tablets, move to a multi-pane layout (e.g., Book List on the left, Book Details on the right) using a 12-column grid.
- **Motion-Driven Spacing:** Spacing between items in a list should animate via `Modifier.animateItem()` during reordering. All size changes, such as expanding a card to show notes, must use `animateContentSize` with the `emphasized` easing curve for a premium feel.

## Elevation & Depth

Depth is communicated through **Tonal Layers** rather than shadows. 
- **Level 0 (Surface):** The base background.
- **Level 1-5 (Surface Tints):** As an element gains "elevation," its color shifts slightly closer to the primary theme color. A Floating Action Button (FAB) sits at a higher tonal level than a standard card.
- **Glassmorphism:** Use for persistent elements like `TopAppBar` when scrolled. The surface should have 70% opacity and a backdrop blur to create a "layered" effect that keeps the book covers visible as they pass underneath.
- **Parallax:** Large book covers on detail screens should use a slow parallax scroll effect (0.5x speed) to create a sense of physical depth within the UI.

## Shapes

The shape language follows Material 3 standards with a **Rounded** (0.5rem) baseline. 
- **Small Components:** Checkboxes and Chips use `rounded-sm`.
- **Medium Components:** Standard cards and Modal Bottom Sheets use `rounded-lg` (1rem). 
- **Large Components:** Floating Action Buttons and "Now Reading" containers use `rounded-xl` (1.5rem).
- **Special Case:** Book covers should retain a very slight rounding (4px) to mimic the physical corners of a hardcover book without looking overly "digital."

## Components

### Modal Bottom Sheets
The primary container for data entry. Use for "Add Note," "Edit Progress," and "Filter Books." These should always snap to half-height first, then allow full expansion.

### Cards
Cards are the primary unit of the library view. They must never use drop shadows. Instead, use a `surfaceVariant` background. Incorporate `Modifier.animateContentSize()` so that when a card is tapped, it expands to reveal secondary actions (Share, Delete, Move to Shelf) smoothly.

### Buttons & Haptics
- **Primary Buttons:** High tonal elevation. Trigger `HapticFeedbackType.TextHandleMove` on click for a "light" confirmation.
- **Destructive/Deliberate Actions:** Marking a book as "Dropped" or "DNF" requires a long-press. Trigger `HapticFeedbackType.LongPress` at the moment the action registers.

### Input Fields
Filled text fields with a bottom-line indicator (standard Material 3). Use the `secondary` color for the active cursor and underline to differentiate from the primary brand color.

### Progress Bars
Use a thick, rounded track for the "Reading Progress" bar. Animate the progress change using `animateFloatAsState` so the bar "fills" smoothly when the user updates their page count.