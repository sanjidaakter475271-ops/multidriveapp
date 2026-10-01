package com.multidrive.app.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// MultiDrive brand palette — deep blue/indigo with teal accents
private val md_theme_light_primary = Color(0xFF1A56C4)
private val md_theme_light_onPrimary = Color(0xFFFFFFFF)
private val md_theme_light_primaryContainer = Color(0xFFD8E2FF)
private val md_theme_light_secondary = Color(0xFF0097A7)
private val md_theme_light_secondaryContainer = Color(0xFFB2EBF2)
private val md_theme_light_background = Color(0xFFF8F9FF)
private val md_theme_light_surface = Color(0xFFFFFFFF)
private val md_theme_light_error = Color(0xFFBA1A1A)

private val md_theme_dark_primary = Color(0xFF90BFFF)
private val md_theme_dark_onPrimary = Color(0xFF002E6E)
private val md_theme_dark_primaryContainer = Color(0xFF00429A)
private val md_theme_dark_secondary = Color(0xFF4DD0E1)
private val md_theme_dark_secondaryContainer = Color(0xFF006978)
private val md_theme_dark_background = Color(0xFF1B1B1F)
private val md_theme_dark_surface = Color(0xFF1B1B1F)
private val md_theme_dark_error = Color(0xFFFFB4AB)

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    secondary = md_theme_light_secondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    background = md_theme_light_background,
    surface = md_theme_light_surface,
    error = md_theme_light_error,
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    secondary = md_theme_dark_secondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    background = md_theme_dark_background,
    surface = md_theme_dark_surface,
    error = md_theme_dark_error,
)

@Composable
fun MultiDriveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Use dynamic Material You on Android 12+ only as fallback
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalView.current.context
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Edge-to-edge: transparent status bar
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
