package com.example.booktracker.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.booktracker.app.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val OutfitFont = FontFamily(
    Font(googleFont = GoogleFont("Outfit"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = provider, weight = FontWeight.Bold)
)

// Modern typography scale relying on Outfit for a premium feel
val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = OutfitFont,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = OutfitFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = OutfitFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = OutfitFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelMedium = TextStyle(
        fontFamily = OutfitFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
