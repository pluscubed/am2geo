package com.pluscubed.am2geo.ui.theme

import androidx.compose.ui.graphics.Color

// Warm, intentional palette. We bypass Material 3 dynamic color: the
// screen's identity is the typography and the hero status mark, and
// inheriting the user's wallpaper-derived tints would scramble that.

// Neutrals — paper / ink
internal val Cream = Color(0xFFF3EDE0)
internal val CreamDim = Color(0xFFD7CFBE)
internal val Ink = Color(0xFF14110D)
internal val InkSoft = Color(0xFF1F1B16)
internal val InkMute = Color(0xFF6E665A)

// Status accents — chosen so the hero mark reads at a glance and feels
// considered rather than "system green / system red".
internal val MossGo = Color(0xFF5A8C58)        // configured — temperate, confident
internal val RustHold = Color(0xFFC15A3C)      // not configured — warm, urgent without alarm
internal val AmberMaybe = Color(0xFFD9A441)    // unknown — wait-and-see ochre

// Legacy aliases (Theme.kt used to import these). Kept around so nothing
// breaks; nothing new should reference them.
val Purple80 = MossGo
val PurpleGrey80 = CreamDim
val Pink80 = RustHold
val Purple40 = MossGo
val PurpleGrey40 = InkMute
val Pink40 = RustHold
