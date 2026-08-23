package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =========================================================================================
// DYNO LITE - ESQUEMA DE CORES MATERIAL 3
// =========================================================================================

private val DynoColorScheme = darkColorScheme(
  primary = DynoBluePrimary,
  onPrimary = DynoTextPrimary,
  primaryContainer = DynoBluePrimary.copy(alpha = 0.25f),
  onPrimaryContainer = DynoBlueLight,

  secondary = DynoSurfaceElevated,
  onSecondary = DynoTextPrimary,
  secondaryContainer = DynoSurfaceElevated,
  onSecondaryContainer = DynoTextPrimary,

  tertiary = DynoPowerCyan,
  onTertiary = DynoBackground,
  tertiaryContainer = DynoPowerCyan.copy(alpha = 0.2f),
  onTertiaryContainer = DynoPowerCyan,

  background = DynoBackground,
  onBackground = DynoTextPrimary,

  surface = DynoSurface,
  onSurface = DynoTextPrimary,
  surfaceVariant = DynoSurface,
  onSurfaceVariant = DynoTextSecondary,
  surfaceContainer = DynoSurfaceContainer,
  surfaceContainerHigh = DynoSurfaceElevated,

  error = DynoErrorRed,
  onError = DynoTextPrimary,
  errorContainer = DynoErrorRed.copy(alpha = 0.2f),
  onErrorContainer = DynoErrorRed,

  outline = DynoBorder,
  outlineVariant = DynoBorderLight,
)

@Composable
fun DynoLiteTheme(
  darkTheme: Boolean = true, // Dyno Lite uses a tailored automotive dark canvas
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        window.statusBarColor = DynoBackground.toArgb()
        window.navigationBarColor = DynoBackground.toArgb()
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false
      }
    }
  }

  MaterialTheme(
    colorScheme = DynoColorScheme,
    typography = Typography,
    content = content
  )
}

// Compatibilidade
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  DynoLiteTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
