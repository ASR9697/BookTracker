package com.example.booktracker.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.booktracker.app.data.AppPalette

// Baseline Fallbacks
val Background = Color(0xFF141218)
val Surface = Color(0xFF141218)
val SurfaceVariant = Color(0xFF49454F)
val SurfaceContainerLowest = Color(0xFF0F0D13)
val SurfaceContainerLow = Color(0xFF1D1B20)
val SurfaceContainer = Color(0xFF211F26)
val SurfaceContainerHigh = Color(0xFF2B2930)
val SurfaceContainerHighest = Color(0xFF36343B)

val OnSurface = Color(0xFFE6E0E9)
val OnSurfaceVariant = Color(0xFFCAC4D0)

val Primary = Color(0xFFF59E0B)
val OnPrimary = Color(0xFF451A03)
val PrimaryContainer = Color(0xFF78350F)
val OnPrimaryContainer = Color(0xFFFEF3C7)

val Secondary = Color(0xFFFB923C)
val OnSecondary = Color(0xFF431407)
val SecondaryContainer = Color(0xFF7C2D12)
val OnSecondaryContainer = Color(0xFFFFEDD5)

val Tertiary = Color(0xFFFDE047)
val OnTertiary = Color(0xFF3F2C00)
val TertiaryContainer = Color(0xFF713F12)
val OnTertiaryContainer = Color(0xFFFEF9C3)

val Error = Color(0xFFF2B8B5)
val OnError = Color(0xFF601410)
val ErrorContainer = Color(0xFF8C1D18)
val OnErrorContainer = Color(0xFFF9DEDC)

val Outline = Color(0xFF938F99)
val OutlineVariant = Color(0xFF49454F)

val PremiumGradientStart = Color(0xFFF59E0B)
val PremiumGradientEnd = Color(0xFFEA580C)

// Material 3 Baseline Light Colors
val LightBackground = Color(0xFFFAF7F2)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEDE5D8)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF7F3EB)
val LightSurfaceContainer = Color(0xFFF2ECE1)
val LightSurfaceContainerHigh = Color(0xFFECE4D7)
val LightSurfaceContainerHighest = Color(0xFFE5DCCF)

val LightOnSurface = Color(0xFF1D1B20)
val LightOnSurfaceVariant = Color(0xFF49454F)

val LightPrimary = Color(0xFFD97706)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFFEF3C7)
val LightOnPrimaryContainer = Color(0xFF451A03)

val LightSecondary = Color(0xFFC2410C)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFFFEDD5)
val LightOnSecondaryContainer = Color(0xFF431407)

val LightTertiary = Color(0xFFB45309)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFEF9C3)
val LightOnTertiaryContainer = Color(0xFF3F2C00)

val LightError = Color(0xFFB3261E)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFF9DEDC)
val LightOnErrorContainer = Color(0xFF410E0B)

val LightOutline = Color(0xFF9E9282)
val LightOutlineVariant = Color(0xFFD5CBBF)

// Streak flame accent
val StreakFlame = Color(0xFFFF6D00)
val StreakFlameGlow = Color(0xFFFFAB00)

// Progress amber accent
val ProgressAmber = Color(0xFFF59E0B)

// -------------------------------------------------------------------------
// Curated Palette Colors
// -------------------------------------------------------------------------

data class PaletteColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val outline: Color,
    val outlineVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color
)

// 1. BOOKMORY AMBER (Literary Gold & Terracotta)
val BookmoryAmberDark = PaletteColors(
    primary = Color(0xFFF59E0B),
    onPrimary = Color(0xFF451A03),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = Color(0xFFFB923C),
    onSecondary = Color(0xFF431407),
    secondaryContainer = Color(0xFF7C2D12),
    onSecondaryContainer = Color(0xFFFFEDD5),
    tertiary = Color(0xFFFDE047),
    onTertiary = Color(0xFF3F2C00),
    tertiaryContainer = Color(0xFF713F12),
    onTertiaryContainer = Color(0xFFFEF9C3),
    background = Color(0xFF131012),
    surface = Color(0xFF1B171A),
    surfaceVariant = Color(0xFF383236),
    surfaceContainerLowest = Color(0xFF0D0A0C),
    surfaceContainerLow = Color(0xFF171316),
    surfaceContainer = Color(0xFF211C20),
    surfaceContainerHigh = Color(0xFF2C262B),
    surfaceContainerHighest = Color(0xFF383137),
    outline = Color(0xFF7A6F75),
    outlineVariant = Color(0xFF463E44),
    onSurface = Color(0xFFF3ECEE),
    onSurfaceVariant = Color(0xFFD6CBCF)
)

val BookmoryAmberLight = PaletteColors(
    primary = Color(0xFFD97706),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF451A03),
    secondary = Color(0xFFC2410C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFEDD5),
    onSecondaryContainer = Color(0xFF431407),
    tertiary = Color(0xFFB45309),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF9C3),
    onTertiaryContainer = Color(0xFF3F2C00),
    background = Color(0xFFFAF7F2),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDE5D8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F3EB),
    surfaceContainer = Color(0xFFF2ECE1),
    surfaceContainerHigh = Color(0xFFECE4D7),
    surfaceContainerHighest = Color(0xFFE5DCCF),
    outline = Color(0xFF9E9282),
    outlineVariant = Color(0xFFD5CBBF),
    onSurface = Color(0xFF1F1B18),
    onSurfaceVariant = Color(0xFF5A524A)
)

// 2. MIDNIGHT VELVET (Electric Indigo & Sky Twilight)
val MidnightVelvetDark = PaletteColors(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0369A1),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFFA855F7),
    onTertiary = Color(0xFF2E1065),
    tertiaryContainer = Color(0xFF6B21A8),
    onTertiaryContainer = Color(0xFFF3E8FF),
    background = Color(0xFF0B0E17),
    surface = Color(0xFF121724),
    surfaceVariant = Color(0xFF273147),
    surfaceContainerLowest = Color(0xFF070910),
    surfaceContainerLow = Color(0xFF0E121E),
    surfaceContainer = Color(0xFF171E2E),
    surfaceContainerHigh = Color(0xFF202A3F),
    surfaceContainerHighest = Color(0xFF2B3751),
    outline = Color(0xFF5D6E90),
    outlineVariant = Color(0xFF303B52),
    onSurface = Color(0xFFE8EDF8),
    onSurfaceVariant = Color(0xFFBAC5DA)
)

val MidnightVelvetLight = PaletteColors(
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0284C7),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF082F49),
    tertiary = Color(0xFF7E22CE),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3E8FF),
    onTertiaryContainer = Color(0xFF2E1065),
    background = Color(0xFFF6F8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFDFE5F2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4FA),
    surfaceContainer = Color(0xFFE8EEF7),
    surfaceContainerHigh = Color(0xFFDFE7F2),
    surfaceContainerHighest = Color(0xFFD6E0EE),
    outline = Color(0xFF7D8FA9),
    outlineVariant = Color(0xFFC4D0E3),
    onSurface = Color(0xFF141923),
    onSurfaceVariant = Color(0xFF475266)
)

// 3. NORDIC EMERALD (Botanical Sage & Forest Pine)
val NordicEmeraldDark = PaletteColors(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF064E3B),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF0F766E),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFA3E635),
    onTertiary = Color(0xFF1A2E05),
    tertiaryContainer = Color(0xFF365314),
    onTertiaryContainer = Color(0xFFECFCCB),
    background = Color(0xFF09120E),
    surface = Color(0xFF0F1C16),
    surfaceVariant = Color(0xFF233B31),
    surfaceContainerLowest = Color(0xFF050B08),
    surfaceContainerLow = Color(0xFF0C1612),
    surfaceContainer = Color(0xFF14241E),
    surfaceContainerHigh = Color(0xFF1C3129),
    surfaceContainerHighest = Color(0xFF264036),
    outline = Color(0xFF5A7C6E),
    outlineVariant = Color(0xFF2C443A),
    onSurface = Color(0xFFE8F5EF),
    onSurfaceVariant = Color(0xFFB8D3C6)
)

val NordicEmeraldLight = PaletteColors(
    primary = Color(0xFF059669),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0D9488),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF042F2E),
    tertiary = Color(0xFF4D7C0F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFECFCCB),
    onTertiaryContainer = Color(0xFF1A2E05),
    background = Color(0xFFF4F9F6),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFD9E7E0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEF5F1),
    surfaceContainer = Color(0xFFE5EFE9),
    surfaceContainerHigh = Color(0xFFDCE8E1),
    surfaceContainerHighest = Color(0xFFD3E1DA),
    outline = Color(0xFF729184),
    outlineVariant = Color(0xFFBFD4CA),
    onSurface = Color(0xFF121F19),
    onSurfaceVariant = Color(0xFF42594F)
)

// 4. SAKURA ROSE (Sunset Coral & Rose Gold)
val SakuraRoseDark = PaletteColors(
    primary = Color(0xFFFB7185),
    onPrimary = Color(0xFF4C0519),
    primaryContainer = Color(0xFF881337),
    onPrimaryContainer = Color(0xFFFFE4E6),
    secondary = Color(0xFFF472B6),
    onSecondary = Color(0xFF500724),
    secondaryContainer = Color(0xFF831843),
    onSecondaryContainer = Color(0xFFFCE7F3),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF140D11),
    surface = Color(0xFF1C1319),
    surfaceVariant = Color(0xFF382732),
    surfaceContainerLowest = Color(0xFF0E080C),
    surfaceContainerLow = Color(0xFF170F14),
    surfaceContainer = Color(0xFF22171F),
    surfaceContainerHigh = Color(0xFF2D1F2A),
    surfaceContainerHighest = Color(0xFF3A2937),
    outline = Color(0xFF7D596E),
    outlineVariant = Color(0xFF472F3E),
    onSurface = Color(0xFFF8ECF2),
    onSurfaceVariant = Color(0xFFDAC0CE)
)

val SakuraRoseLight = PaletteColors(
    primary = Color(0xFFE11D48),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE4E6),
    onPrimaryContainer = Color(0xFF4C0519),
    secondary = Color(0xFFDB2777),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFCE7F3),
    onSecondaryContainer = Color(0xFF500724),
    tertiary = Color(0xFFD97706),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF451A03),
    background = Color(0xFFFAF5F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEDFE5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7EFF2),
    surfaceContainer = Color(0xFFF2E6EB),
    surfaceContainerHigh = Color(0xFFEBDDE3),
    surfaceContainerHighest = Color(0xFFE3D3DB),
    outline = Color(0xFF9C7A8B),
    outlineVariant = Color(0xFFD7C2CC),
    onSurface = Color(0xFF20131A),
    onSurfaceVariant = Color(0xFF5F4653)
)

// 5. ESPRESSO CARAMEL (Roasted Coffee & Spiced Cinnamon)
val EspressoCaramelDark = PaletteColors(
    primary = Color(0xFFF59E0B),
    onPrimary = Color(0xFF451A03),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = Color(0xFFEA580C),
    onSecondary = Color(0xFF431407),
    secondaryContainer = Color(0xFF7C2D12),
    onSecondaryContainer = Color(0xFFFFEDD5),
    tertiary = Color(0xFFEAB308),
    onTertiary = Color(0xFF422006),
    tertiaryContainer = Color(0xFF713F12),
    onTertiaryContainer = Color(0xFFFEF9C3),
    background = Color(0xFF130E0C),
    surface = Color(0xFF1C1512),
    surfaceVariant = Color(0xFF382B24),
    surfaceContainerLowest = Color(0xFF0D0A08),
    surfaceContainerLow = Color(0xFF17110E),
    surfaceContainer = Color(0xFF221A16),
    surfaceContainerHigh = Color(0xFF2D231E),
    surfaceContainerHighest = Color(0xFF3A2D27),
    outline = Color(0xFF7C6354),
    outlineVariant = Color(0xFF46362D),
    onSurface = Color(0xFFF7EFE9),
    onSurfaceVariant = Color(0xFFD8C6B9)
)

val EspressoCaramelLight = PaletteColors(
    primary = Color(0xFFB45309),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF451A03),
    secondary = Color(0xFF9A3412),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFEDD5),
    onSecondaryContainer = Color(0xFF431407),
    tertiary = Color(0xFF854D0E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF9C3),
    onTertiaryContainer = Color(0xFF422006),
    background = Color(0xFFFAF7F2),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDE4D8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F2E9),
    surfaceContainer = Color(0xFFF1EAE0),
    surfaceContainerHigh = Color(0xFFEBE2D6),
    surfaceContainerHighest = Color(0xFFE4DACD),
    outline = Color(0xFF988170),
    outlineVariant = Color(0xFFD2C3B3),
    onSurface = Color(0xFF1F1813),
    onSurfaceVariant = Color(0xFF5A4D43)
)

// 6. MATCHA (Whisked Green Tea & Botanical Moss)
val MatchaDark = PaletteColors(
    primary = Color(0xFF9CC07A),
    onPrimary = Color(0xFF1C2B0F),
    primaryContainer = Color(0xFF3B5228),
    onPrimaryContainer = Color(0xFFE2F0D2),
    secondary = Color(0xFFC9B27C),
    onSecondary = Color(0xFF2E2510),
    secondaryContainer = Color(0xFF574A2A),
    onSecondaryContainer = Color(0xFFF3E9CF),
    tertiary = Color(0xFF8CC4B0),
    onTertiary = Color(0xFF0F2E24),
    tertiaryContainer = Color(0xFF2B5345),
    onTertiaryContainer = Color(0xFFD5F0E5),
    background = Color(0xFF0F120D),
    surface = Color(0xFF161A13),
    surfaceVariant = Color(0xFF2F3729),
    surfaceContainerLowest = Color(0xFF0A0C08),
    surfaceContainerLow = Color(0xFF131711),
    surfaceContainer = Color(0xFF1B2018),
    surfaceContainerHigh = Color(0xFF242A20),
    surfaceContainerHighest = Color(0xFF2E3529),
    outline = Color(0xFF6E7A63),
    outlineVariant = Color(0xFF3A4333),
    onSurface = Color(0xFFEEF2E8),
    onSurfaceVariant = Color(0xFFC6CFBC)
)

val MatchaLight = PaletteColors(
    primary = Color(0xFF55743A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDEBCB),
    onPrimaryContainer = Color(0xFF1C2B0F),
    secondary = Color(0xFF7D6A3C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1E6C9),
    onSecondaryContainer = Color(0xFF2E2510),
    tertiary = Color(0xFF3D7764),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3EEE3),
    onTertiaryContainer = Color(0xFF0F2E24),
    background = Color(0xFFF7F8F1),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2E7D6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F4EA),
    surfaceContainer = Color(0xFFEAEFE1),
    surfaceContainerHigh = Color(0xFFE3E9D8),
    surfaceContainerHighest = Color(0xFFDBE2CF),
    outline = Color(0xFF7F8C72),
    outlineVariant = Color(0xFFC8D1BB),
    onSurface = Color(0xFF181D14),
    onSurfaceVariant = Color(0xFF4B5642)
)

// 7. NORDIC FOG (Cool Slate & Misty Fjord)
val NordicFogDark = PaletteColors(
    primary = Color(0xFF9FB4C7),
    onPrimary = Color(0xFF15222E),
    primaryContainer = Color(0xFF34495C),
    onPrimaryContainer = Color(0xFFDCE7F1),
    secondary = Color(0xFFB8C4CE),
    onSecondary = Color(0xFF1F272E),
    secondaryContainer = Color(0xFF414C56),
    onSecondaryContainer = Color(0xFFE3E9EE),
    tertiary = Color(0xFFC9B8D6),
    onTertiary = Color(0xFF2A1F33),
    tertiaryContainer = Color(0xFF4E4259),
    onTertiaryContainer = Color(0xFFEFE5F6),
    background = Color(0xFF101315),
    surface = Color(0xFF171B1E),
    surfaceVariant = Color(0xFF2F363C),
    surfaceContainerLowest = Color(0xFF0B0D0F),
    surfaceContainerLow = Color(0xFF14181B),
    surfaceContainer = Color(0xFF1C2125),
    surfaceContainerHigh = Color(0xFF252B30),
    surfaceContainerHighest = Color(0xFF2F363C),
    outline = Color(0xFF6C7882),
    outlineVariant = Color(0xFF39424A),
    onSurface = Color(0xFFE9EDF0),
    onSurfaceVariant = Color(0xFFC1CAD1)
)

val NordicFogLight = PaletteColors(
    primary = Color(0xFF4A6378),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE6EF),
    onPrimaryContainer = Color(0xFF15222E),
    secondary = Color(0xFF5B6772),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1E7EC),
    onSecondaryContainer = Color(0xFF1F272E),
    tertiary = Color(0xFF6B5A7A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEEE4F5),
    onTertiaryContainer = Color(0xFF2A1F33),
    background = Color(0xFFF4F6F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFDDE3E8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEF1F4),
    surfaceContainer = Color(0xFFE7EBEF),
    surfaceContainerHigh = Color(0xFFE0E5EA),
    surfaceContainerHighest = Color(0xFFD8DEE4),
    outline = Color(0xFF7D8892),
    outlineVariant = Color(0xFFC3CBD2),
    onSurface = Color(0xFF161B1F),
    onSurfaceVariant = Color(0xFF465058)
)

// 8. WARM PAPYRUS (Parchment & Sepia Ink)
val WarmPapyrusDark = PaletteColors(
    primary = Color(0xFFE0A866),
    onPrimary = Color(0xFF3A2208),
    primaryContainer = Color(0xFF6B4520),
    onPrimaryContainer = Color(0xFFF8E3C6),
    secondary = Color(0xFFC9A57D),
    onSecondary = Color(0xFF33230F),
    secondaryContainer = Color(0xFF5C4630),
    onSecondaryContainer = Color(0xFFF2E2CC),
    tertiary = Color(0xFFB9A26E),
    onTertiary = Color(0xFF2E2508),
    tertiaryContainer = Color(0xFF55492A),
    onTertiaryContainer = Color(0xFFF0E6C6),
    background = Color(0xFF17130E),
    surface = Color(0xFF1F1A14),
    surfaceVariant = Color(0xFF3B3328),
    surfaceContainerLowest = Color(0xFF110E0A),
    surfaceContainerLow = Color(0xFF1B1712),
    surfaceContainer = Color(0xFF241F18),
    surfaceContainerHigh = Color(0xFF2E2820),
    surfaceContainerHighest = Color(0xFF39322A),
    outline = Color(0xFF85776A),
    outlineVariant = Color(0xFF4A4036),
    onSurface = Color(0xFFF2E9DC),
    onSurfaceVariant = Color(0xFFD4C6B3)
)

val WarmPapyrusLight = PaletteColors(
    primary = Color(0xFF8E5A26),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF3DDBF),
    onPrimaryContainer = Color(0xFF3A2208),
    secondary = Color(0xFF6E5537),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEBDBC2),
    onSecondaryContainer = Color(0xFF33230F),
    tertiary = Color(0xFF6B5D2E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDE3C2),
    onTertiaryContainer = Color(0xFF2E2508),
    background = Color(0xFFF4EBDA),
    surface = Color(0xFFFAF4E8),
    surfaceVariant = Color(0xFFE6D9C2),
    surfaceContainerLowest = Color(0xFFFDF9F1),
    surfaceContainerLow = Color(0xFFF6EEDF),
    surfaceContainer = Color(0xFFF0E5D2),
    surfaceContainerHigh = Color(0xFFEADDC7),
    surfaceContainerHighest = Color(0xFFE3D4BC),
    outline = Color(0xFF9A8670),
    outlineVariant = Color(0xFFD4C3AA),
    onSurface = Color(0xFF2B2117),
    onSurfaceVariant = Color(0xFF5E4F3E)
)

// 9. OLED MIDNIGHT (True Black & Moonlit Silver): pure black surfaces switch pixels off on OLED screens.
val OledMidnightDark = PaletteColors(
    primary = Color(0xFFE2E8F0),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFFF1F5F9),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E2633),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFFFDE68A),
    onTertiary = Color(0xFF3A2E05),
    tertiaryContainer = Color(0xFF3F3514),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF1C1F24),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF07080A),
    surfaceContainer = Color(0xFF0D0F12),
    surfaceContainerHigh = Color(0xFF15181C),
    surfaceContainerHighest = Color(0xFF1E2227),
    outline = Color(0xFF5B6470),
    outlineVariant = Color(0xFF262B32),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFFB4BDC9)
)

val OledMidnightLight = PaletteColors(
    primary = Color(0xFF334155),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF64748B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5E9EF),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFF92700C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF3A2E05),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2E8F0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5F9),
    surfaceContainer = Color(0xFFEBEFF4),
    surfaceContainerHigh = Color(0xFFE4E9EF),
    surfaceContainerHighest = Color(0xFFDCE2E9),
    outline = Color(0xFF7C8796),
    outlineVariant = Color(0xFFCBD5E1),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569)
)

fun getPaletteColors(palette: AppPalette, darkTheme: Boolean): PaletteColors {
    return when (palette) {
        AppPalette.BOOKMORY_AMBER -> if (darkTheme) BookmoryAmberDark else BookmoryAmberLight
        AppPalette.MIDNIGHT_VELVET -> if (darkTheme) MidnightVelvetDark else MidnightVelvetLight
        AppPalette.NORDIC_EMERALD -> if (darkTheme) NordicEmeraldDark else NordicEmeraldLight
        AppPalette.SAKURA_ROSE -> if (darkTheme) SakuraRoseDark else SakuraRoseLight
        AppPalette.ESPRESSO_CARAMEL -> if (darkTheme) EspressoCaramelDark else EspressoCaramelLight
        AppPalette.MATCHA -> if (darkTheme) MatchaDark else MatchaLight
        AppPalette.NORDIC_FOG -> if (darkTheme) NordicFogDark else NordicFogLight
        AppPalette.WARM_PAPYRUS -> if (darkTheme) WarmPapyrusDark else WarmPapyrusLight
        AppPalette.OLED_MIDNIGHT -> if (darkTheme) OledMidnightDark else OledMidnightLight
    }
}


