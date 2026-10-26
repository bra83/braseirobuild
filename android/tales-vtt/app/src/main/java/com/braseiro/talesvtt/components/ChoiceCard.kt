package com.braseiro.talesvtt.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun ChoiceCard(
  number: Int,
  title: String,
  description: String,
  icon: ImageVector,
  accentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val shape = RoundedCornerShape(Dimens.radiusSmall)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(AppTheme.colors.surfaceSecondary)
      .border(BorderStroke(Dimens.borderWidth, AppTheme.colors.borderSubtle), shape)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true, color = AppTheme.colors.accentCyan.copy(alpha = 0.2f)),
        onClick = onClick
      )
      .testTag("choice_card_$number")
      .padding(horizontal = Dimens.spacing12, vertical = Dimens.spacing12)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      // Left: Number circle
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(accentColor.copy(alpha = 0.2f))
          .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = number.toString(),
          style = AppTheme.typography.cardTitle.copy(
            color = accentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
        )
      }

      Spacer(modifier = Modifier.width(Dimens.spacing12))

      // Icon
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier.size(24.dp)
      )

      Spacer(modifier = Modifier.width(Dimens.spacing12))

      // Middle: Title & Description
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title.uppercase(),
          style = AppTheme.typography.cardTitle.copy(
            color = accentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 0.8.sp
          )
        )
        Text(
          text = description,
          style = AppTheme.typography.bodySmall.copy(
            color = AppTheme.colors.textSecondary,
            fontSize = 11.5.sp,
            lineHeight = 15.sp
          )
        )
      }

      Spacer(modifier = Modifier.width(Dimens.spacing8))

      // Right: Chevron
      Icon(
        imageVector = AppIcons.ChevronRight,
        contentDescription = "Selecionar ação",
        tint = accentColor,
        modifier = Modifier.size(16.dp)
      )
    }
  }
}
