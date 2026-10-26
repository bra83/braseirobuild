package com.braseiro.talesvtt.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braseiro.talesvtt.R
import com.braseiro.talesvtt.model.ContextIconType
import com.braseiro.talesvtt.model.ContextItemData
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun ContextItem(
  item: ContextItemData,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {}
) {
  val shape = RoundedCornerShape(Dimens.radiusSmall)
  val icon: ImageVector = when (item.iconType) {
    ContextIconType.ILLUSTRATION -> AppIcons.Illustration
    ContextIconType.CHARACTERS -> AppIcons.Group
    ContextIconType.LOCATION -> AppIcons.Location
    ContextIconType.SCENE_TONE -> AppIcons.AudioWaves
    ContextIconType.MYSTERY -> AppIcons.Mystery
    ContextIconType.DAYLIGHT -> AppIcons.WeatherOvercast
    ContextIconType.AUDIO_TTS -> AppIcons.AudioTts
  }
  val imageRes: Int? = when (item.iconType) {
    ContextIconType.CHARACTERS -> R.drawable.nav_group
    ContextIconType.LOCATION -> R.drawable.state_current_location
    ContextIconType.MYSTERY -> R.drawable.nav_mystery
    ContextIconType.DAYLIGHT -> R.drawable.state_daylight
    ContextIconType.AUDIO_TTS -> R.drawable.action_tts
    else -> null
  }

  val hasValue = !item.value.isNullOrBlank()
  val borderColor = if (item.isHighlighted) AppTheme.colors.accentCyan.copy(alpha = 0.25f) else AppTheme.colors.borderSubtle
  val textColor = if (item.isHighlighted) AppTheme.colors.accentCyan else AppTheme.colors.textSecondary

  Box(
    modifier = modifier
      .clip(shape)
      .background(AppTheme.colors.surfacePrimary)
      .border(BorderStroke(Dimens.borderWidth, borderColor), shape)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true),
        onClick = onClick
      )
      .testTag("context_item_${item.id}")
      .padding(horizontal = Dimens.spacing10, vertical = Dimens.spacing6)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start
    ) {
      if (item.isHighlighted) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(AppTheme.colors.accentCyan)
        )
        Spacer(modifier = Modifier.width(Dimens.spacing6))
      }

      if (imageRes != null) {
        Image(
          painter = painterResource(id = imageRes),
          contentDescription = item.label,
          modifier = Modifier.size(16.dp)
        )
      } else {
        Icon(
          imageVector = icon,
          contentDescription = item.label,
          tint = textColor,
          modifier = Modifier.size(13.dp)
        )
      }

      Spacer(modifier = Modifier.width(Dimens.spacing6))

      Column {
        Text(
          text = item.label.uppercase(),
          style = AppTheme.typography.caption.copy(
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp,
            letterSpacing = 0.8.sp
          )
        )
        if (hasValue) {
          Text(
            text = item.value ?: "",
            style = AppTheme.typography.caption.copy(
              color = if (item.isHighlighted) AppTheme.colors.accentCyan else AppTheme.colors.textPrimary,
              fontWeight = FontWeight.Medium,
              fontSize = 9.sp
            )
          )
        }
      }
    }
  }
}
