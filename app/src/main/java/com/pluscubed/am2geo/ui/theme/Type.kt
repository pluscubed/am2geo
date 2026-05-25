package com.pluscubed.am2geo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System serif — used as the display face: status word ("READY" / "NOT YET"
// / "?") and large headings. Resolves to the platform's default serif
// (Noto Serif on most Android builds).
private val DisplaySerif = FontFamily.Serif

// System monospace — used for everything technical: domain names, version
// strings, the test field placeholder. Resolves to the platform's default
// monospace (Droid Sans Mono / Roboto Mono depending on vendor).
private val TechnicalMono = FontFamily.Monospace

val Am2geoTypography = Typography(
    // Display — hero status word
    displayLarge = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 96.sp,
        lineHeight = 92.sp,
        letterSpacing = (-2).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Normal,
        fontSize = 72.sp,
        lineHeight = 72.sp,
        letterSpacing = (-1.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
    ),

    titleMedium = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 26.sp,
    ),

    // Body — mono, technical
    bodyLarge = TextStyle(
        fontFamily = TechnicalMono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = TechnicalMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = TechnicalMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),

    // Label — small mono caps, used for wordmark / tags / button labels
    labelLarge = TextStyle(
        fontFamily = TechnicalMono,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = TechnicalMono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = TechnicalMono,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        lineHeight = 13.sp,
        letterSpacing = 2.5.sp,
    ),
)

// Keep this alias so any external references to `Typography` still compile.
val Typography = Am2geoTypography
