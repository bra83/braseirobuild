package com.braseiro.talesvtt.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.braseiro.talesvtt.R
import com.braseiro.talesvtt.components.AppIcons
import com.braseiro.talesvtt.components.AppShell
import com.braseiro.talesvtt.components.ChoiceCard
import com.braseiro.talesvtt.components.FreeActionPrompt
import com.braseiro.talesvtt.components.NarrativePanel
import com.braseiro.talesvtt.components.SceneImageSlot
import com.braseiro.talesvtt.components.SectionHeader
import com.braseiro.talesvtt.components.NarrationTtsController
import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.game.SceneChoiceDefaults
import com.braseiro.talesvtt.model.AppDestination
import com.braseiro.talesvtt.model.ContextIconType
import com.braseiro.talesvtt.model.ContextItemData
import com.braseiro.talesvtt.model.WorldStatusData
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun LocationScreen(
  onNavigate: (AppDestination) -> Unit,
  gameState: GameState,
  onDeclareAction: (String) -> Unit,
  narrationTts: NarrationTtsController,
  modifier: Modifier = Modifier
) {
  val scene = gameState.scene
  val tts = narrationTts
  val contextItems = listOf(
    ContextItemData("loc_here", "Local Atual", scene.location, ContextIconType.LOCATION, isHighlighted = true),
    ContextItemData("loc_kids", "Kids Presentes", "${gameState.group.members.size}", ContextIconType.CHARACTERS),
    ContextItemData("loc_tone", "Luz do Dia", scene.daylight, ContextIconType.DAYLIGHT)
  )

  AppShell(
    currentDestination = AppDestination.SCENE,
    screenTitle = scene.location,
    chapterLabel = "LOCAL",
    sceneLabel = scene.title,
    worldStatus = WorldStatusData(scene.seasonYear, scene.weather, scene.time),
    contextItems = contextItems,
    onNavigate = onNavigate,
    modifier = modifier
  ) {
    SceneImageSlot(
      imageRes = locationSceneDrawable(scene.visualAssetKey),
      locationName = scene.location.uppercase(),
      sceneTone = scene.daylight,
      onTtsClick = { tts.speakVisibleNarrative(scene.narrative) },
      onCharactersClick = { onNavigate(AppDestination.GROUP) },
      onLocationClick = { onNavigate(AppDestination.MAP) }
    )
    Spacer(modifier = Modifier.height(Dimens.spacing10))
    NarrativePanel(
      title = "Narrativa do Mestre",
      paragraphs = scene.narrative
    )
    Spacer(modifier = Modifier.height(Dimens.spacing12))
    SectionHeader(title = "O QUE VOCÊ FAZ?", color = AppTheme.colors.accentCyan)
    Spacer(modifier = Modifier.height(Dimens.spacing6))
    Column {
      scene.choices.take(3).ifEmpty { SceneChoiceDefaults.standard() }.forEachIndexed { index, choice ->
        ChoiceCard(
          number = index + 1,
          title = choice.title,
          description = choice.description,
          icon = when (index) {
            0 -> AppIcons.ActionLook
            1 -> AppIcons.AudioWaves
            else -> AppIcons.Group
          },
          accentColor = when (index) {
            0 -> AppTheme.colors.accentCyan
            1 -> AppTheme.colors.accentAmber
            else -> AppTheme.colors.accentOlive
          },
          onClick = { onDeclareAction("${choice.title}: ${choice.description}") }
        )
        Spacer(modifier = Modifier.height(Dimens.spacing8))
      }
    }
    FreeActionPrompt(onSubmit = onDeclareAction)
    Spacer(modifier = Modifier.height(Dimens.spacing24))
  }
}

private fun locationSceneDrawable(assetKey: String): Int = when (assetKey.removeSuffix(".png")) {
  "scene_maintenance_corridor" -> R.drawable.scene_maintenance_corridor
  "scene_town_square" -> R.drawable.scene_town_square
  "scene_hideout" -> R.drawable.scene_hideout
  "scene_family_kitchen" -> R.drawable.scene_family_kitchen
  "scene_reactor_hall" -> R.drawable.scene_reactor_hall
  else -> R.drawable.scene_loop_gate
}
