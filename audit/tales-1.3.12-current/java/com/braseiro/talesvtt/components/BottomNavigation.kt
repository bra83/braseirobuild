package com.braseiro.talesvtt.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.braseiro.talesvtt.R
import com.braseiro.talesvtt.model.AppDestination
import com.braseiro.talesvtt.ui.theme.AppTheme

@Composable
fun AppBottomNavigation(
  currentDestination: AppDestination,
  onNavigate: (AppDestination) -> Unit,
  modifier: Modifier = Modifier
) {
  val border = Color(0xFF28545C)
  val background = Color(0xFF001116)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(background)
      .drawBehind {
        drawLine(
          color = border,
          start = Offset(0f, 0f),
          end = Offset(size.width, 0f),
          strokeWidth = 1f
        )
      }
      .windowInsetsPadding(WindowInsets.navigationBars)
      .testTag("app_bottom_navigation")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(61.dp)
    ) {
      AppDestination.values().forEach { destination ->
        val imageRes = when (destination) {
          AppDestination.SCENE -> R.drawable.nav_scene_approved
          AppDestination.KID -> R.drawable.nav_kid_approved
          AppDestination.GROUP -> R.drawable.nav_group_approved
          AppDestination.MYSTERY -> R.drawable.nav_mystery_approved
          AppDestination.MAP -> R.drawable.nav_map_approved
          AppDestination.BACKPACK -> R.drawable.nav_backpack_approved
          AppDestination.MENU -> R.drawable.nav_menu_approved
        }
        BottomNavItem(
          label = destination.title,
          imageRes = imageRes,
          isSelected = currentDestination == destination,
          onClick = { onNavigate(destination) },
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}
