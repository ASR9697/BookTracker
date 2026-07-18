---
name: Nocturnal Reader
colors:
  surface: '#141317'
  surface-dim: '#141317'
  surface-bright: '#3a383d'
  surface-container-lowest: '#0e0e11'
  surface-container-low: '#1c1b1f'
  surface-container: '#201f23'
  surface-container-high: '#2b292d'
  surface-container-highest: '#353438'
  on-surface: '#e5e1e7'
  on-surface-variant: '#cac4d0'
  inverse-surface: '#e5e1e7'
  inverse-on-surface: '#313034'
  outline: '#948f9a'
  outline-variant: '#49454f'
  surface-tint: '#d0bcff'
  primary: '#e9ddff'
  on-primary: '#37265e'
  primary-container: '#d0bcff'
  on-primary-container: '#594983'
  inverse-primary: '#665590'
  secondary: '#4fd8eb'
  on-secondary: '#00363d'
  secondary-container: '#00b1c3'
  on-secondary-container: '#003e45'
  tertiary: '#ffdad6'
  on-tertiary: '#51221d'
  tertiary-container: '#ffb4ab'
  on-tertiary-container: '#7a433d'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e9ddff'
  primary-fixed-dim: '#d0bcff'
  on-primary-fixed: '#210f48'
  on-primary-fixed-variant: '#4d3d76'
  secondary-fixed: '#96f0ff'
  secondary-fixed-dim: '#4fd8eb'
  on-secondary-fixed: '#001f24'
  on-secondary-fixed-variant: '#004f57'
  tertiary-fixed: '#ffdad5'
  tertiary-fixed-dim: '#ffb4ab'
  on-tertiary-fixed: '#360e0a'
  on-tertiary-fixed-variant: '#6c3832'
  background: '#141317'
  on-background: '#e5e1e7'
  surface-variant: '#353438'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 57px
    fontWeight: '400'
    lineHeight: 64px
    letterSpacing: -0.25px
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '400'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '400'
    lineHeight: 36px
  title-lg:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
    letterSpacing: 0.15px
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
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  margin-mobile: 1rem
  margin-tablet: 1.5rem
  gutter: 1rem
  stack-sm: 0.5rem
  stack-md: 1rem
  stack-lg: 1.5rem
---

## Brand & Style
The design system is rooted in the **Corporate / Modern** aesthetic, specifically leveraging the **Material Design 3 (Material You)** framework. The brand personality is scholarly yet contemporary, aiming to provide a focused, "lights-out" environment for bibliophiles. It evokes a sense of quiet immersion, like a private library at midnight. 

The target audience consists of avid readers who value organization and digital mindfulness. The UI response is tactile and responsive, utilizing dynamic color relationships where primary actions feel like illuminated beacons against a deep, structural background.

## Colors
The palette follows the M3 tonal system for dark mode.
- **Primary (Orchid):** Used for key calls-to-action, active states, and Floating Action Buttons (FAB).
- **Secondary (Teal):** Used for progress indicators, "currently reading" badges, and secondary navigation elements.
- **Tertiary (Coral):** Reserved for system alerts, "want to read" highlights, or delete actions.
- **Neutral (Charcoal/Slate):** The foundation of the UI. Backgrounds use a deep charcoal, while containers (cards, sheets) use a slightly lighter slate to establish depth without relying on heavy shadows.

## Typography
This design system utilizes **Inter** for all roles to ensure maximum legibility at small scales and a clean, systematic feel. 
- **Headlines:** Use regular weights with generous leading for a sophisticated editorial feel.
- **Titles:** Medium weights are used for book titles and section headers to provide immediate hierarchy.
- **Body:** Optimized for reading long synopses or notes, using a slightly increased letter spacing for dark mode legibility.
- **Labels:** Used for metadata (ISBN, Page counts, Dates) in uppercase or semi-bold variants.

## Layout & Spacing
The layout follows a **Fluid Grid** model based on an 8dp square grid.
- **Mobile:** 4-column grid with 16px (1rem) side margins and 16px gutters.
- **Tablet:** 8-column grid with 24px (1.5rem) side margins and 24px gutters.
- **Vertical Rhythm:** Content is stacked in 8px increments. Large sections (e.g., between "Reading Now" and "Library") use 32px or 48px spacing to signify distinct topical shifts.

## Elevation & Depth
Elevation is expressed through **Tonal Layers** rather than dramatic shadows, consistent with M3 principles. 
- **Level 0 (Background):** Pure deep charcoal (#1C1B1F).
- **Level 1 (Cards/Lists):** Surface-container slate (#2B2930).
- **Level 2 (Dialogs/Menus):** A lighter slate tint to indicate priority.
- **Interactive Depth:** When a card is pressed, it uses a subtle primary-colored inner glow or a 1dp stroke instead of an drop shadow.
- **FAB:** The Floating Action Button occupies the highest elevation, using a distinct primary color fill to "float" above all content.

## Shapes
The design system features a very high roundedness profile to feel organic and friendly.
- **Standard Cards:** 28px radius.
- **Small Components (Chips/Buttons):** Fully pill-shaped (100px).
- **Search Bars:** 28px radius.
- **Bottom Sheets:** Top corners at 32px radius.
Book covers within cards maintain a smaller 8px radius to preserve the "object" feel of a physical book.

## Components
- **Buttons:** Primary buttons are pill-shaped, filled with the Primary color. Outlined buttons use a 1px stroke of the Primary color on a transparent background.
- **FAB:** A large 56x56dp square with heavily rounded corners (28px), containing a single icon for adding a new book.
- **Cards:** M3-style cards with no borders, using the Surface-Container color. They should feature a "contained" layout where the book cover is on the left and metadata on the right.
- **Chips:** Used for genre tags (e.g., "Sci-Fi", "History"). These are low-profile, pill-shaped elements with a secondary color stroke.
- **Progress Bars:** Linear indicators for reading progress. The track is a low-opacity secondary color, and the filler is a high-vibrancy teal.
- **Input Fields:** Filled style with a 1px bottom indicator line and a 28px top corner radius, fitting the high-roundedness theme.
- **Navigation Bar:** A bottom navigation bar with pill-shaped active state indicators behind icons.