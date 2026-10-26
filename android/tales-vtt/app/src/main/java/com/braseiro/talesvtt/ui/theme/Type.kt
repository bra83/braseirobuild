package com.braseiro.talesvtt.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Centralized typography tokens
@Immutable
data class AppTypographyTokens(
  val display: TextStyle,
  val screenTitle: TextStyle,
  val sectionTitle: TextStyle,
  val cardTitle: TextStyle,
  val body: TextStyle,
  val bodySmall: TextStyle,
  val label: TextStyle,
  val caption: TextStyle,
)

val AppTypography = AppTypographyTokens(
  display = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Black,
    fontSize = 26.sp,
    lineHeight = 30.sp,
    letterSpacing = 1.2.sp,
    color = TextPrimary
  ),
  screenTitle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.5.sp,
    color = TextPrimary
  ),
  sectionTitle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = 1.0.sp,
    color = AccentCyan
  ),
  cardTitle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 15.sp,
    lineHeight = 19.sp,
    letterSpacing = 0.6.sp,
    color = TextPrimary
  ),
  body = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.25.sp,
    color = TextPrimary
  ),
  bodySmall = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.2.sp,
    color = TextSecondary
  ),
  label = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.8.sp,
    color = TextSecondary
  ),
  caption = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 10.sp,
    lineHeight = 13.sp,
    letterSpacing = 0.4.sp,
    color = TextMuted
  )
)

val MaterialTypography = Typography(
  displayLarge = AppTypography.display,
  titleLarge = AppTypography.screenTitle,
  titleMedium = AppTypography.sectionTitle,
  titleSmall = AppTypography.cardTitle,
  bodyLarge = AppTypography.body,
  bodyMedium = AppTypography.bodySmall,
  labelLarge = AppTypography.label,
  labelSmall = AppTypography.caption
)

