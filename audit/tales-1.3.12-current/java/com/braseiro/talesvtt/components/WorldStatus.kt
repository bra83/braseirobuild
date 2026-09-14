package com.braseiro.talesvtt.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import com.braseiro.talesvtt.model.WorldStatusData
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun WorldStatus(
  data: WorldStatusData,
  modifier: Modifier = Modifier,
  compact: Boolean = true
) {
  val shape = RoundedCornerShape(Dimens.radiusSmall)
  Row(
    modifier = modifier
      .clip(shape)
      .background(AppTheme.colors.surfacePrimary.copy(alpha = 0.5f))
      .border(BorderStroke(Dimens.borderWidth, AppTheme.colors.borderSubtle), shape)
      .padding(horizontal = Dimens.spacing8, vertical = Dimens.spacing4),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.End
  ) {
    Icon(
      imageVector = AppIcons.WeatherOvercast,
      contentDescription = "Clima",
      tint = AppTheme.colors.accentAmber,
      modifier = Modifier.size(16.dp)
    )

    Spacer(modifier = Modifier.width(Dimens.spacing6))

    Column(horizontalAlignment = Alignment.End) {
      Text(
        text = data.seasonYear,
        style = AppTheme.typography.caption.copy(color = AppTheme.colors.textPrimary)
      )
      Text(
        text = data.weather,
        style = AppTheme.typography.caption.copy(color = AppTheme.colors.textSecondary)
      )
    }

    Spacer(modifier = Modifier.width(Dimens.spacing6))

    Icon(
      imageVector = AppIcons.Clock,
      contentDescription = "Horário",
      tint = AppTheme.colors.textSecondary,
      modifier = Modifier.size(16.dp)
    )
  }
}
