package com.braseiro.talesvtt.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun PrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  testTag: String = "primary_button"
) {
  Button(
    onClick = onClick,
    modifier = modifier
      .minimumInteractiveComponentSize()
      .testTag(testTag),
    enabled = enabled,
    shape = RoundedCornerShape(Dimens.radiusSmall),
    colors = ButtonDefaults.buttonColors(
      containerColor = AppTheme.colors.accentCyan,
      contentColor = AppTheme.colors.backgroundPrimary,
      disabledContainerColor = AppTheme.colors.surfaceSecondary,
      disabledContentColor = AppTheme.colors.textMuted
    ),
    contentPadding = PaddingValues(horizontal = Dimens.spacing16, vertical = Dimens.spacing12)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = if (enabled) AppTheme.colors.backgroundPrimary else AppTheme.colors.textMuted
        )
        Spacer(modifier = Modifier.width(Dimens.spacing8))
      }
      Text(
        text = text,
        style = AppTheme.typography.cardTitle.copy(
          color = if (enabled) AppTheme.colors.backgroundPrimary else AppTheme.colors.textMuted
        )
      )
    }
  }
}

@Composable
fun SecondaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  testTag: String = "secondary_button"
) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier
      .minimumInteractiveComponentSize()
      .testTag(testTag),
    enabled = enabled,
    shape = RoundedCornerShape(Dimens.radiusSmall),
    border = BorderStroke(Dimens.borderWidth, if (enabled) AppTheme.colors.borderHighlight else AppTheme.colors.borderSubtle),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = AppTheme.colors.surfacePrimary,
      contentColor = AppTheme.colors.textPrimary,
      disabledContainerColor = AppTheme.colors.backgroundSecondary,
      disabledContentColor = AppTheme.colors.textMuted
    ),
    contentPadding = PaddingValues(horizontal = Dimens.spacing16, vertical = Dimens.spacing12)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = if (enabled) AppTheme.colors.accentCyan else AppTheme.colors.textMuted
        )
        Spacer(modifier = Modifier.width(Dimens.spacing8))
      }
      Text(
        text = text,
        style = AppTheme.typography.cardTitle.copy(
          color = if (enabled) AppTheme.colors.textPrimary else AppTheme.colors.textMuted
        )
      )
    }
  }
}

@Composable
fun IconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  tint: Color = AppTheme.colors.textSecondary,
  backgroundColor: Color = Color.Transparent,
  borderColor: Color = Color.Transparent,
  testTag: String = "icon_button"
) {
  Surface(
    modifier = modifier
      .minimumInteractiveComponentSize()
      .defaultMinSize(minWidth = Dimens.minTouchTarget, minHeight = Dimens.minTouchTarget)
      .testTag(testTag),
    shape = RoundedCornerShape(Dimens.radiusSmall),
    color = backgroundColor,
    border = if (borderColor != Color.Transparent) BorderStroke(Dimens.borderWidth, borderColor) else null
  ) {
    Box(
      modifier = Modifier
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = ripple(bounded = true),
          onClick = onClick
        )
        .padding(Dimens.spacing8),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}
