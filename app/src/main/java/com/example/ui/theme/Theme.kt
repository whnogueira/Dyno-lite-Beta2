package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DynoPowerCyan,
    onPrimary = DynoBg,
    primaryContainer = DynoCardSurface,
    onPrimaryContainer = DynoTextPrimary,
    secondary = DynoTorqueAmber,
    onSecondary = DynoBg,
    secondaryContainer = DynoCardBg,
    onSecondaryContainer = DynoTextPrimary,
    tertiary = DynoRed,
    onTertiary = DynoTextPrimary,
    background = DynoBg,
    onBackground = DynoTextPrimary,
    surface = DynoCardBg,
    onSurface = DynoTextPrimary,
    surfaceVariant = DynoCardSurface,
    onSurfaceVariant = DynoTextSecondary,
    outline = DynoCardBorder,
    error = DynoErrorRed,
    onError = DynoTextPrimary
)

@Composable
fun DynoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = DynoBg.toArgb()
                it.navigationBarColor = DynoBg.toArgb()
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(it, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
