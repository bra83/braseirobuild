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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun FreeActionPrompt(
  modifier: Modifier = Modifier,
  onSubmit: (String) -> Unit = {}
) {
  var actionText by remember { mutableStateOf("") }
  val purpleColor = AppTheme.colors.accentPurple
  val shape = RoundedCornerShape(Dimens.radiusMedium)

  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = "AÇÃO LIVRE",
      style = AppTheme.typography.sectionTitle.copy(
        color = purpleColor,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.0.sp
      )
    )

    Spacer(modifier = Modifier.height(Dimens.spacing6))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(AppTheme.colors.surfaceSecondary)
        .border(BorderStroke(Dimens.borderWidth, AppTheme.colors.borderSubtle), shape)
        .padding(horizontal = Dimens.spacing12, vertical = Dimens.spacing8),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Icon(
        imageVector = AppIcons.Edit,
        contentDescription = null,
        tint = purpleColor,
        modifier = Modifier.size(18.dp)
      )

      Spacer(modifier = Modifier.width(Dimens.spacing8))

      Box(modifier = Modifier.weight(1f)) {
        if (actionText.isEmpty()) {
          Column {
            Text(
              text = "O que você faz?",
              style = AppTheme.typography.cardTitle.copy(
                color = AppTheme.colors.textSecondary,
                fontSize = 12.sp
              )
            )
            Text(
              text = "Descreva qualquer ação que quiser tentar.",
              style = AppTheme.typography.caption.copy(
                color = AppTheme.colors.textMuted,
                fontSize = 10.sp
              )
            )
          }
        }
        BasicTextField(
          value = actionText,
          onValueChange = { actionText = it },
          textStyle = AppTheme.typography.body.copy(
            color = AppTheme.colors.textPrimary,
            fontSize = 12.sp
          ),
          cursorBrush = SolidColor(purpleColor),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("free_action_input")
        )
      }

      Spacer(modifier = Modifier.width(Dimens.spacing8))

      // Send button
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(Dimens.radiusSmall))
          .background(purpleColor.copy(alpha = 0.35f))
          .border(BorderStroke(1.dp, purpleColor.copy(alpha = 0.6f)), RoundedCornerShape(Dimens.radiusSmall))
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true),
            onClick = {
              if (actionText.isNotBlank()) {
                onSubmit(actionText)
                actionText = ""
              }
            }
          )
          .testTag("free_action_submit"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = AppIcons.Send,
          contentDescription = "Enviar ação",
          tint = AppTheme.colors.textPrimary,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}
