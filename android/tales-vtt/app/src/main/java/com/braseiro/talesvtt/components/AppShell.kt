package com.braseiro.talesvtt.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.braseiro.talesvtt.model.AppDestination
import com.braseiro.talesvtt.model.ContextItemData
import com.braseiro.talesvtt.model.WorldStatusData
import com.braseiro.talesvtt.ui.theme.AppTheme

@Composable
fun AppShell(
  currentDestination: AppDestination,
  screenTitle: String,
  onNavigate: (AppDestination) -> Unit,
  modifier: Modifier = Modifier,
  chapterLabel: String? = "CAPÍTULO 1",
  sceneLabel: String? = "Cena 7",
  worldStatus: WorldStatusData = WorldStatusData(),
  contextItems: List<ContextItemData>? = null,
  onContextItemClick: (ContextItemData) -> Unit = {},
  onMenuClick: () -> Unit = { onNavigate(AppDestination.MENU) },
  scrollable: Boolean = true,
  content: @Composable ColumnScope.() -> Unit
) {
  val guidedContext = LocalGuideContext.current
  val guide = GuidedMode.resolve(currentDestination, screenTitle, guidedContext)
  val (guideEnabled, setGuideEnabled) = rememberGuidedModePreference()
  var showGuidedHelp by remember { mutableStateOf(false) }
  val hasNewGuideTip = rememberUnseenGuideTip(guide.id, guideEnabled, showGuidedHelp)

  if (showGuidedHelp) {
    GuidedModeSheet(
      page = guide,
      profile = guidedContext.game.rulesProfile,
      enabled = guideEnabled,
      onEnabledChange = setGuideEnabled,
      onDismiss = { showGuidedHelp = false },
      onNavigate = onNavigate
    )
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppTheme.colors.backgroundPrimary)
      .statusBarsPadding(),
    containerColor = AppTheme.colors.backgroundPrimary,
    bottomBar = {
      AppBottomNavigation(
        currentDestination = currentDestination,
        onNavigate = onNavigate
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(AppTheme.colors.backgroundPrimary)
    ) {
      // 1. App Header
      AppHeader(
        screenTitle = screenTitle,
        chapterLabel = chapterLabel,
        sceneLabel = sceneLabel,
        worldStatus = worldStatus,
        onMenuClick = onMenuClick,
        onHelpClick = { showGuidedHelp = true },
        guideTipAvailable = hasNewGuideTip
      )

      // 2. Optional Context Strip
      if (!contextItems.isNullOrEmpty()) {
        ContextStrip(
          items = contextItems,
          onItemClick = onContextItemClick
        )
      }

      // 3. Screen Main Content Area
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        if (scrollable) {
          ScrollableScreenContent {
            content()
          }
        } else {
          ScreenContent {
            content()
          }
        }
      }
    }
  }
}