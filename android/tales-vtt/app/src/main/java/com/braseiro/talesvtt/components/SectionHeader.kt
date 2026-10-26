package com.braseiro.talesvtt.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun SectionHeader(
  title: String,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  color: Color = AppTheme.colors.accentCyan,
  actionContent: (@Composable () -> Unit)? = null
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = Dimens.spacing4),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = color,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(Dimens.spacing6))
      }
      Text(
        text = title.uppercase(),
        style = AppTheme.typography.sectionTitle.copy(
          color = color,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.0.sp
        )
      )
    }

    if (actionContent != null) {
      actionContent()
    }
  }
}
