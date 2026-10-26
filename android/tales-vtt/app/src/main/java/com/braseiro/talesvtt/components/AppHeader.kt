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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braseiro.talesvtt.R
import com.braseiro.talesvtt.model.WorldStatusData
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun AppHeader(
  screenTitle: String,
  modifier: Modifier = Modifier,
  chapterLabel: String? = "CAPÍTULO 1",
  sceneLabel: String? = "Cena 7",
  worldStatus: WorldStatusData = WorldStatusData(),
  onMenuClick: () -> Unit = {},
  onHelpClick: () -> Unit = {},
  guideTipAvailable: Boolean = false
) {
  val borderSubtle = AppTheme.colors.borderSubtle
  val backgroundSecondary = AppTheme.colors.backgroundSecondary

  Box(
    modifier = modifier
      .fillMaxWidth()
      .drawBehind {
        // Bottom subtle border matching Elegant Dark
        drawLine(
          color = borderSubtle,
          start = Offset(0f, size.height),
          end = Offset(size.width, size.height),
          strokeWidth = 1f
        )
      }
      .background(backgroundSecondary)
      .padding(horizontal = Dimens.spacing16, vertical = Dimens.spacing10)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // ESQUERDA: 40x40 icon box + Braseiro Tales & Title
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacing12)
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(Dimens.radiusSmall))
            .background(AppTheme.colors.surfacePrimary)
            .border(BorderStroke(Dimens.borderWidth, borderSubtle), RoundedCornerShape(Dimens.radiusSmall))
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = ripple(bounded = true),
              onClick = onMenuClick
            )
            .testTag("header_menu_button"),
          contentAlignment = Alignment.Center
        ) {
          Image(
            painter = painterResource(id = R.drawable.action_hamburger),
            contentDescription = "Menu Principal",
            modifier = Modifier.size(22.dp)
          )
        }

        Column {
          Text(
            text = "BRASEIRO TALES",
            style = AppTheme.typography.caption.copy(
              color = AppTheme.colors.accentCyan,
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp,
              letterSpacing = 1.6.sp,
              lineHeight = 11.sp
            )
          )
          Text(
            text = screenTitle.uppercase(),
            style = AppTheme.typography.cardTitle.copy(
              color = AppTheme.colors.textPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              letterSpacing = 0.5.sp,
              lineHeight = 16.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // DIREITA: World Status sem container; informação + ícone de clima
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8)
      ) {
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = (chapterLabel ?: worldStatus.seasonYear).uppercase(),
            style = AppTheme.typography.caption.copy(
              color = AppTheme.colors.textSecondary,
              fontWeight = FontWeight.Medium,
              fontSize = 10.sp,
              letterSpacing = 0.8.sp
            )
          )
          Text(
            text = "${worldStatus.time} — ${worldStatus.weather}",
            style = AppTheme.typography.caption.copy(
              color = AppTheme.colors.accentAmber,
              fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
              fontWeight = FontWeight.SemiBold,
              fontSize = 11.sp
            )
          )
        }

        // Reuse the existing header help icon: no floating overlay over narration or dice.
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(34.dp)
            .clip(CircleShape)
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = ripple(bounded = false),
              onClick = onHelpClick
            )
            .testTag("header_game_order_help")
        ) {
          Icon(
            imageVector = AppIcons.Help,
            contentDescription = "Abrir guia contextual da partida",
            tint = AppTheme.colors.accentCyan,
            modifier = Modifier.size(22.dp).testTag("header_guided_mode_icon")
          )
          if (guideTipAvailable) Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .size(6.dp)
              .background(AppTheme.colors.accentAmber, CircleShape)
              .testTag("header_guided_tip_available")
          )
        }

        val weatherLower = worldStatus.weather.lowercase()
        val weatherIcon = when {
          "neve" in weatherLower -> AppIcons.WeatherSnow
          "nebl" in weatherLower || "névo" in weatherLower -> AppIcons.WeatherFog
          "vento" in weatherLower -> AppIcons.WeatherWind
          "chuva" in weatherLower || "garoa" in weatherLower -> AppIcons.WeatherRain
          "limpo" in weatherLower || "sol" in weatherLower -> AppIcons.WeatherClear
          else -> AppIcons.WeatherOvercast
        }
        Icon(
          imageVector = weatherIcon,
          contentDescription = worldStatus.weather,
          tint = AppTheme.colors.accentAmber,
          modifier = Modifier
            .size(25.dp)
            .testTag("header_world_status")
        )
      }
    }
  }
}