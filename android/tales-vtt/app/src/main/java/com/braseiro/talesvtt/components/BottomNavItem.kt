package com.braseiro.talesvtt.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braseiro.talesvtt.ui.theme.AppTheme

@Composable
fun BottomNavItem(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  imageRes: Int? = null
) {
  val cyan = AppTheme.colors.accentCyan
  val backgroundBrush = if (isSelected) {
    Brush.verticalGradient(
      listOf(Color(0xFF04525F).copy(alpha = 0.72f), Color(0xFF002C36).copy(alpha = 0.54f))
    )
  } else {
    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(backgroundBrush)
      .drawBehind {
        if (isSelected) {
          val lineWidth = size.width * 0.58f
          val x0 = (size.width - lineWidth) / 2f
          drawLine(
            color = cyan,
            start = Offset(x0, 1.5.dp.toPx()),
            end = Offset(x0 + lineWidth, 1.5.dp.toPx()),
            strokeWidth = 3.dp.toPx()
          )
        }
      }
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true),
        onClick = onClick
      )
      .testTag("bottom_nav_${label.lowercase()}") ,
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      when {
        imageRes != null -> Image(
          painter = painterResource(id = imageRes),
          contentDescription = label,
          colorFilter = ColorFilter.tint(cyan),
          modifier = Modifier.size(28.dp)
        )
        icon != null -> Icon(
          imageVector = icon,
          contentDescription = label,
          tint = cyan,
          modifier = Modifier.size(28.dp)
        )
      }
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = label,
        style = AppTheme.typography.caption.copy(
          color = cyan,
          fontSize = 9.sp,
          fontWeight = FontWeight.Medium,
          letterSpacing = 0.25.sp,
          lineHeight = 9.sp
        )
      )
    }
  }
}
