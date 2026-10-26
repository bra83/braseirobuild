package com.braseiro.talesvtt.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.braseiro.talesvtt.R
import com.braseiro.talesvtt.components.AppPanel
import com.braseiro.talesvtt.components.AppShell
import com.braseiro.talesvtt.components.FreeActionPrompt
import com.braseiro.talesvtt.components.NarrativePanel
import com.braseiro.talesvtt.components.SceneImageSlot
import com.braseiro.talesvtt.components.SectionHeader
import com.braseiro.talesvtt.components.rememberNarrationTtsController
import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.model.AppDestination
import com.braseiro.talesvtt.model.ContextIconType
import com.braseiro.talesvtt.model.ContextItemData
import com.braseiro.talesvtt.model.WorldStatusData
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun EverydayScreen(
  onNavigate: (AppDestination) -> Unit,
  gameState: GameState,
  onDeclareAction: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val scene = gameState.scene
  val anchorLabel = gameState.kid.anchorName.substringBefore('—').trim().ifBlank { gameState.kid.anchorName }
  val tts = rememberNarrationTtsController()
  val contextItems = listOf(
    ContextItemData("ev_loc", "Local Atual", scene.location, ContextIconType.LOCATION, isHighlighted = true),
    ContextItemData("ev_anchor", "Âncora", anchorLabel, ContextIconType.CHARACTERS),
    ContextItemData("ev_cond", "Condições", if (gameState.kid.broken) "Quebrado" else "${gameState.kid.conditions.size} ativa(s)", ContextIconType.MYSTERY)
  )

  AppShell(
    currentDestination = AppDestination.SCENE,
    screenTitle = "Cotidiano",
    chapterLabel = "CENA PESSOAL",
    sceneLabel = scene.location,
    worldStatus = WorldStatusData(scene.seasonYear, scene.weather, scene.time),
    contextItems = contextItems,
    onNavigate = onNavigate,
    modifier = modifier
  ) {
    SceneImageSlot(
      imageRes = everydaySceneDrawable(scene.visualAssetKey),
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
    SectionHeader(title = "ÂNCORA", color = AppTheme.colors.accentOlive)
    Spacer(modifier = Modifier.height(Dimens.spacing6))
    AppPanel(borderColor = AppTheme.colors.accentOlive.copy(alpha = 0.4f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.char_anchor), "Âncora", Modifier.size(52.dp), contentScale = ContentScale.Fit)
        Spacer(modifier = Modifier.width(9.dp))
        Text(
          text = "${gameState.kid.anchorName} pode ajudar na recuperação apenas se esta for uma cena segura, próxima e de cuidado. A cura não é aplicada automaticamente pela tela.",
          style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary),
          modifier = Modifier.weight(1f)
        )
      }
    }
    Spacer(modifier = Modifier.height(Dimens.spacing10))
    FreeActionPrompt(onSubmit = onDeclareAction)
    Spacer(modifier = Modifier.height(Dimens.spacing24))
  }
}

private fun everydaySceneDrawable(assetKey: String): Int = when (assetKey.removeSuffix(".png")) {
  "scene_loop_gate" -> R.drawable.scene_loop_gate
  "scene_maintenance_corridor" -> R.drawable.scene_maintenance_corridor
  "scene_town_square" -> R.drawable.scene_town_square
  "scene_hideout" -> R.drawable.scene_hideout
  "scene_reactor_hall" -> R.drawable.scene_reactor_hall
  else -> R.drawable.scene_family_kitchen
}
