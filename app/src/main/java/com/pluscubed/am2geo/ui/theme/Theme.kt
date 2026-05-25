package com.pluscubed.am2geo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Status-accent palette. Material 3 only has `error` for a semantic colour
 * role — there is no built-in success or warning. We provide our own
 * (light/dark variants of moss / rust / amber) and expose them via
 * [LocalStatusPalette] so screens can read them without threading them
 * through every composable.
 */
data class StatusPalette(
    val configured: Color,
    val onConfigured: Color,
    val notConfigured: Color,
    val onNotConfigured: Color,
    val unknown: Color,
    val onUnknown: Color,
)

private val DarkStatusPalette = StatusPalette(
    configured = Color(0xFF7CC07A),
    onConfigured = Color(0xFF0A1A09),
    notConfigured = Color(0xFFE08366),
    onNotConfigured = Color(0xFF1F0B05),
    unknown = Color(0xFFE7BC5F),
    onUnknown = Color(0xFF1B1306),
)

private val LightStatusPalette = StatusPalette(
    configured = Color(0xFF3F6E3D),
    onConfigured = Color(0xFFFFFFFF),
    notConfigured = Color(0xFFB04A2D),
    onNotConfigured = Color(0xFFFFFFFF),
    unknown = Color(0xFF8D6B1F),
    onUnknown = Color(0xFFFFFFFF),
)

val LocalStatusPalette = staticCompositionLocalOf { DarkStatusPalette }

// Generous shape system — leans into the Expressive corner radii so cards
// and buttons feel softer and more contemporary than vanilla M3 defaults.
private val Am2geoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun Am2geoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic colour follows the user's wallpaper on Android 12+ — the
    // Material 3 default. We respect it.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    val statusPalette = if (darkTheme) DarkStatusPalette else LightStatusPalette

    CompositionLocalProvider(LocalStatusPalette provides statusPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Am2geoTypography,
            shapes = Am2geoShapes,
            content = content,
        )
    }
}
