package com.braseiro.talesvtt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val DarkColorScheme =
  darkColorScheme(
    primary = AccentCyan,
    onPrimary = BackgroundPrimary,
    primaryContainer = SurfaceSecondary,
    onPrimaryContainer = AccentCyan,
    secondary = AccentAmber,
    onSecondary = BackgroundPrimary,
    secondaryContainer = SurfaceSecondary,
    onSecondaryContainer = AccentAmber,
    tertiary = AccentOlive,
    onTertiary = BackgroundPrimary,
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = SurfacePrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSecondary,
    onSurfaceVariant = TextSecondary,
    outline = BorderMedium,
    outlineVariant = BorderSubtle,
    error = Negative,
    onError = TextPrimary
  )

val LocalAppTypography = staticCompositionLocalOf { AppTypography }

object AppTheme {
  val typography: AppTypographyTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalAppTypography.current

  val colors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current
}

data class AppColors(
  val backgroundPrimary: androidx.compose.ui.graphics.Color = BackgroundPrimary,
  val backgroundSecondary: androidx.compose.ui.graphics.Color = BackgroundSecondary,
  val surfacePrimary: androidx.compose.ui.graphics.Color = SurfacePrimary,
  val surfaceSecondary: androidx.compose.ui.graphics.Color = SurfaceSecondary,
  val textPrimary: androidx.compose.ui.graphics.Color = TextPrimary,
  val textSecondary: androidx.compose.ui.graphics.Color = TextSecondary,
  val textMuted: androidx.compose.ui.graphics.Color = TextMuted,
  val accentCyan: androidx.compose.ui.graphics.Color = AccentCyan,
  val accentAmber: androidx.compose.ui.graphics.Color = AccentAmber,
  val negative: androidx.compose.ui.graphics.Color = Negative,
  val accentOlive: androidx.compose.ui.graphics.Color = AccentOlive,
  val accentPurple: androidx.compose.ui.graphics.Color = AccentPurple,
  val borderSubtle: androidx.compose.ui.graphics.Color = BorderSubtle,
  val borderMedium: androidx.compose.ui.graphics.Color = BorderMedium,
  val borderHighlight: androidx.compose.ui.graphics.Color = BorderHighlight,
)

val LocalAppColors = staticCompositionLocalOf { AppColors() }

@Composable
fun TalesVttTheme(
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(
    LocalAppTypography provides AppTypography,
    LocalAppColors provides AppColors()
  ) {
    MaterialTheme(
      colorScheme = DarkColorScheme,
      typography = MaterialTypography,
      content = content
    )
  }
}

// Alias for compatibility with template tests
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  TalesVttTheme(content = content)
}

