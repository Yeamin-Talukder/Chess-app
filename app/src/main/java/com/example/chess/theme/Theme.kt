package com.example.chess.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = light_primary,
    onPrimary = light_onPrimary,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFDCECCB),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF1E3A12),
    secondary = light_secondary,
    onSecondary = light_textPrimary,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFF4E3C4),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF3E2A16),
    tertiary = light_primary,
    onTertiary = light_onPrimary,
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFE9F4DD),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF274A18),
    background = light_background,
    onBackground = light_textPrimary,
    surface = light_surface,
    onSurface = light_textPrimary,
    surfaceVariant = light_surfaceVariant,
    onSurfaceVariant = light_textSecondary,
    error = light_error,
    onError = light_onPrimary,
    outline = light_border
)

private val DarkColors = darkColorScheme(
    primary = dark_primary,
    onPrimary = dark_onPrimary,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF3D5D27),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFE2F4CD),
    secondary = dark_secondary,
    onSecondary = dark_textPrimary,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF5B4433),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFF4DEC2),
    tertiary = dark_primary,
    onTertiary = dark_onPrimary,
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF304C20),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFE2F4CD),
    background = dark_background,
    onBackground = dark_textPrimary,
    surface = dark_surface,
    onSurface = dark_textPrimary,
    surfaceVariant = dark_surfaceVariant,
    onSurfaceVariant = dark_textSecondary,
    error = dark_error,
    onError = dark_onPrimary,
    outline = dark_border
)

@Composable
fun ChessTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic color is explicitly disabled to preserve the premium custom palette
    val colors = if (useDarkTheme) DarkColors else LightColors
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
