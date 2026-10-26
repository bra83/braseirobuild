package com.braseiro.talesvtt.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun InfoChip(
  label: String,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  value: String? = null,
  tintColor: Color = AppTheme.colors.accentCyan,
  backgroundColor: Color = AppTheme.colors.surfacePrimary.copy(alpha = 0.85f),
  borderColor: Color = AppTheme.colors.borderMedium,
  onClick: (() -> Unit)? = null
) {
  val shape = RoundedCornerShape(Dimens.radiusSmall)
  Box(
    modifier = modifier
      .clip(shape)
      .background(backgroundColor)
      .border(BorderStroke(Dimens.borderWidth, borderColor), shape)
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
      .padding(horizontal = Dimens.spacing8, vertical = Dimens.spacing4)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(12.dp),
          tint = tintColor
        )
        Spacer(modifier = Modifier.width(Dimens.spacing4))
      }
      Text(
        text = label.uppercase(),
        style = AppTheme.typography.caption.copy(color = tintColor)
      )
      if (value != null) {
        Spacer(modifier = Modifier.width(Dimens.spacing4))
        Text(
          text = value,
          style = AppTheme.typography.caption.copy(color = AppTheme.colors.textPrimary)
        )
      }
    }
  }
}

@Composable
fun StatusChip(
  status: String,
  modifier: Modifier = Modifier,
  isActive: Boolean = false,
  activeColor: Color = AppTheme.colors.accentCyan,
  inactiveColor: Color = AppTheme.colors.textMuted
) {
  val shape = RoundedCornerShape(Dimens.radiusSmall)
  val currentTint = if (isActive) activeColor else inactiveColor
  val currentBg = if (isActive) activeColor.copy(alpha = 0.15f) else Color.Transparent
  val currentBorder = if (isActive) activeColor.copy(alpha = 0.5f) else AppTheme.colors.borderSubtle

  Box(
    modifier = modifier
      .clip(shape)
      .background(currentBg)
      .border(BorderStroke(Dimens.borderWidth, currentBorder), shape)
      .padding(horizontal = Dimens.spacing8, vertical = Dimens.spacing4)
  ) {
    Text(
      text = status.uppercase(),
      style = AppTheme.typography.caption.copy(
        color = currentTint
      )
    )
  }
}
