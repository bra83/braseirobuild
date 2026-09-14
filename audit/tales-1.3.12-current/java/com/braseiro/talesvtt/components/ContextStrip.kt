package com.braseiro.talesvtt.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.braseiro.talesvtt.model.ContextItemData
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun ContextStrip(
  items: List<ContextItemData>,
  modifier: Modifier = Modifier,
  onItemClick: (ContextItemData) -> Unit = {}
) {
  val scrollState = rememberScrollState()
  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(scrollState)
      .padding(horizontal = Dimens.spacing12, vertical = Dimens.spacing6),
    horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8),
    verticalAlignment = Alignment.CenterVertically
  ) {
    items.forEach { item ->
      ContextItem(
        item = item,
        onClick = { onItemClick(item) }
      )
    }
  }
}
