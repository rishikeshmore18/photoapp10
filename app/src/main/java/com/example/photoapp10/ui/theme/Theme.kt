package com.example.photoapp10.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Seeded scheme (manual, since dynamic color is not available on Nougat)
private fun lightSchemeFromSeed(): ColorScheme = lightColorScheme(
    primary = Seed,
    onPrimary = Color.White,
    primaryContainer = Seed.copy(alpha = 0.90f),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF546E7A),
    onSecondary = Color.White,
    background = Color(0xFFFDFDFD),
    onBackground = Color(0xFF121212),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF121212)
)

private fun darkSchemeFromSeed(): ColorScheme = darkColorScheme(
    primary = Seed.copy(alpha = 0.85f),
    onPrimary = Color(0xFF101010),
    primaryContainer = Seed.copy(alpha = 0.60f),
    onPrimaryContainer = Color(0xFF101010),
    secondary = Color(0xFF90A4AE),
    onSecondary = Color(0xFF101010),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFEDEDED)
)

@Composable
fun PhotoAppTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }

    val colors = if (dark) darkSchemeFromSeed() else lightSchemeFromSeed()

    // Set system bars contrast-friendly
    val view = LocalView.current
    SideEffect {
        // If background is light, set dark icons; else light icons
        val lightIcons = colors.background.luminance() > 0.5f
        WindowCompat.getInsetsController(view.context.findActivity().window, view)
            .isAppearanceLightStatusBars = lightIcons
        view.context.findActivity().window.statusBarColor = colors.background.toArgb()
        view.context.findActivity().window.navigationBarColor = colors.background.toArgb()
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}

// Helper to get Activity from context
private fun android.content.Context.findActivity(): android.app.Activity {
    var c = this
    while (c is android.content.ContextWrapper) {
        if (c is android.app.Activity) return c
        c = c.baseContext
    }
    error("Activity not found")
}