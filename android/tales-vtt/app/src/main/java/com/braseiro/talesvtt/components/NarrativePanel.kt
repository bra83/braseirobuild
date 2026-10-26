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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun NarrativePanel(
  title: String,
  paragraphs: List<String>,
  modifier: Modifier = Modifier,
  enableInternalScroll: Boolean = false,
  maxScrollHeight: Int = 300
) {
  var fontScale by remember { mutableFloatStateOf(1.0f) }
  var isBookmarked by remember { mutableStateOf(false) }
  val narrativeScroll = rememberScrollState()

  val shape = RoundedCornerShape(Dimens.radiusMedium)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(AppTheme.colors.surfacePrimary)
      .border(BorderStroke(Dimens.borderWidth, AppTheme.colors.borderSubtle), shape)
      .testTag("narrative_panel")
      .padding(Dimens.spacing12)
  ) {
    Column {
      // Header row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "✦",
            style = AppTheme.typography.sectionTitle.copy(
              color = AppTheme.colors.accentAmber,
              fontSize = 14.sp
            )
          )
          Spacer(modifier = Modifier.width(Dimens.spacing6))
          Text(
            text = title.uppercase(),
            style = AppTheme.typography.sectionTitle.copy(
              color = AppTheme.colors.accentAmber,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.0.sp
            )
          )
        }

        // Font size pills & Bookmark
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(Dimens.spacing6)
        ) {
          // A- / A+ font control pill
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(Dimens.radiusSmall))
              .background(AppTheme.colors.surfaceSecondary)
              .border(BorderStroke(Dimens.borderWidth, AppTheme.colors.borderSubtle), RoundedCornerShape(Dimens.radiusSmall))
              .padding(horizontal = Dimens.spacing6, vertical = Dimens.spacing2),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "A-",
              style = AppTheme.typography.caption.copy(
                color = if (fontScale > 0.85f) AppTheme.colors.textPrimary else AppTheme.colors.textMuted,
                fontWeight = FontWeight.Bold
              ),
              modifier = Modifier
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = ripple(bounded = false),
                  onClick = {
                    if (fontScale > 0.85f) fontScale -= 0.15f
                  }
                )
                .padding(horizontal = Dimens.spacing4)
            )
            Text(
              text = "|",
              style = AppTheme.typography.caption.copy(color = AppTheme.colors.borderMedium)
            )
            Text(
              text = "A+",
              style = AppTheme.typography.caption.copy(
                color = if (fontScale < 1.45f) AppTheme.colors.textPrimary else AppTheme.colors.textMuted,
                fontWeight = FontWeight.Bold
              ),
              modifier = Modifier
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = ripple(bounded = false),
                  onClick = {
                    if (fontScale < 1.45f) fontScale += 0.15f
                  }
                )
                .padding(horizontal = Dimens.spacing4)
            )
          }

          // Bookmark icon
          Icon(
            imageVector = AppIcons.Bookmark,
            contentDescription = "Salvar marcador",
            tint = if (isBookmarked) AppTheme.colors.accentAmber else AppTheme.colors.textSecondary,
            modifier = Modifier
              .size(18.dp)
              .clip(CircleShape)
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = { isBookmarked = !isBookmarked }
              )
          )
        }
      }

      Spacer(modifier = Modifier.height(Dimens.spacing10))

      // Narrative has its own real scroll state. Dragging inside this area scrolls the Master's
      // text first instead of moving the whole screen.
      Row(modifier = Modifier.fillMaxWidth()) {
        val textModifier = Modifier
          .weight(1f)
          .padding(end = Dimens.spacing8)
          .then(if (enableInternalScroll) Modifier.heightIn(max = maxScrollHeight.dp).verticalScroll(narrativeScroll) else Modifier)

        Column(modifier = textModifier) {
          paragraphs.forEachIndexed { index, paragraph ->
            Text(
              text = paragraph,
              style = AppTheme.typography.body.copy(
                fontSize = (16.5f * fontScale).sp,
                lineHeight = (25.5f * fontScale).sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = AppTheme.colors.textPrimary
              )
            )
            if (index < paragraphs.size - 1) Spacer(modifier = Modifier.height(Dimens.spacing10))
          }
        }

        if (enableInternalScroll) {
          val trackHeight = maxScrollHeight.coerceAtLeast(120)
          val thumbHeight = if (narrativeScroll.maxValue <= 0) trackHeight else (trackHeight / 3).coerceAtLeast(42)
          val travel = (trackHeight - thumbHeight).coerceAtLeast(0)
          val progress = if (narrativeScroll.maxValue <= 0) 0f else narrativeScroll.value.toFloat() / narrativeScroll.maxValue.toFloat()
          Box(
            modifier = Modifier
              .width(4.dp)
              .height(trackHeight.dp)
              .clip(RoundedCornerShape(Dimens.radiusPill))
              .background(AppTheme.colors.borderSubtle)
          ) {
            Box(
              modifier = Modifier
                .width(4.dp)
                .height(thumbHeight.dp)
                .offset(y = (travel * progress).dp)
                .clip(RoundedCornerShape(Dimens.radiusPill))
                .background(AppTheme.colors.accentCyan.copy(alpha = 0.9f))
            )
          }
        }
      }
    }
  }
}
