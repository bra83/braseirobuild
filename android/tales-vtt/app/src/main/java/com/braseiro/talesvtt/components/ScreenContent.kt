package com.braseiro.talesvtt.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun ScreenContent(
  modifier: Modifier = Modifier,
  horizontalPadding: Dp = Dimens.spacing12,
  verticalPadding: Dp = Dimens.spacing8,
  content: @Composable ColumnScope.() -> Unit
) {
  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
      content()
    }
  }
}

@Composable
fun ScrollableScreenContent(
  modifier: Modifier = Modifier,
  horizontalPadding: Dp = Dimens.spacing12,
  verticalPadding: Dp = Dimens.spacing8,
  content: @Composable ColumnScope.() -> Unit
) {
  val scrollState = rememberScrollState()
  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(scrollState)
        .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
      content()
    }
  }
}
