package com.braseiro.talesvtt.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun AppPanel(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(Dimens.radiusMedium),
  backgroundColor: Color = AppTheme.colors.surfacePrimary,
  borderColor: Color = AppTheme.colors.borderSubtle,
  borderWidth: Dp = Dimens.borderWidth,
  contentPadding: Dp = Dimens.spacing12,
  content: @Composable ColumnScope.() -> Unit
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(backgroundColor)
      .border(BorderStroke(borderWidth, borderColor), shape)
      .padding(contentPadding)
  ) {
    Column {
      content()
    }
  }
}
