package com.braseiro.talesvtt.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.braseiro.talesvtt.R
import com.braseiro.talesvtt.components.AppPanel
import com.braseiro.talesvtt.components.AppShell
import com.braseiro.talesvtt.components.SceneImageSlot
import com.braseiro.talesvtt.components.PrimaryButton
import com.braseiro.talesvtt.components.SecondaryButton
import com.braseiro.talesvtt.components.SectionHeader
import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.game.CompromiseMark
import com.braseiro.talesvtt.game.Condition
import com.braseiro.talesvtt.model.AppDestination
import com.braseiro.talesvtt.model.ContextIconType
import com.braseiro.talesvtt.model.ContextItemData
import com.braseiro.talesvtt.model.WorldStatusData
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun ClimaxScreen(
  onNavigate: (AppDestination) -> Unit,
  gameState: GameState,
  onStartKidScene: () -> Unit,
  onAcceptCompromise: (List<CompromiseMark>) -> Unit,
  onDeclineCompromise: () -> Unit,
  modifier: Modifier = Modifier
) {
  val ext = gameState.extended
  val contextItems = listOf(
    ContextItemData("cl_loc", "Local Atual", gameState.scene.location, ContextIconType.LOCATION, isHighlighted = true),
    ContextItemData("cl_kids", "Kids", if (ext.active) ext.kids.toString() else gameState.group.members.size.toString(), ContextIconType.CHARACTERS),
    ContextItemData("cl_target", "Ameaça", if (ext.active) "${ext.threatLevel.label} • ${ext.target}" else "—", ContextIconType.MYSTERY)
  )

  AppShell(
    currentDestination = AppDestination.SCENE,
    screenTitle = "Clímax",
    chapterLabel = "PROBLEMA ESTENDIDO",
    sceneLabel = gameState.scene.location,
    worldStatus = WorldStatusData(gameState.scene.seasonYear, gameState.scene.weather, gameState.scene.time),
    contextItems = contextItems,
    onNavigate = onNavigate,
    modifier = modifier
  ) {
    SceneImageSlot(
      imageRes = climaxSceneDrawable(gameState.scene.visualAssetKey),
      locationName = gameState.scene.location.uppercase(),
      sceneTone = "Clímax",
      showTts = false,
      onCharactersClick = { onNavigate(AppDestination.GROUP) },
      onLocationClick = { onNavigate(AppDestination.MAP) }
    )
    Spacer(modifier = Modifier.height(Dimens.spacing10))

    if (!ext.active) {
      AppPanel(borderColor = AppTheme.colors.accentCyan.copy(alpha = 0.45f)) {
        Text(text = "NENHUM PROBLEMA ESTENDIDO ATIVO", style = AppTheme.typography.sectionTitle.copy(color = AppTheme.colors.accentCyan))
        Text(
          text = "O Clímax só usa este painel quando o Mestre inicia explicitamente um Problema Estendido. Um confronto comum continua pela CENA e ENCRENCA.",
          style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary)
        )
      }
      Spacer(modifier = Modifier.height(Dimens.spacing24))
      return@AppShell
    }

    AppPanel(borderColor = AppTheme.colors.accentAmber.copy(alpha = 0.55f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.trouble_extended), "Problema Estendido", Modifier.size(52.dp), contentScale = ContentScale.Fit)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(text = "APOSTAS DO MESTRE", style = AppTheme.typography.label.copy(color = AppTheme.colors.accentAmber))
          Text(text = ext.stakes, style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary))
        }
      }
    }

    Spacer(modifier = Modifier.height(Dimens.spacing10))
    AppPanel {
      Text(text = "NÍVEL DE AMEAÇA", style = AppTheme.typography.label.copy(color = AppTheme.colors.accentCyan))
      Text(
        text = "${ext.threatLevel.label.uppercase()}  •  ${ext.threatLevel.multiplier} × ${ext.kids} Kids = ${ext.target} sucessos",
        style = AppTheme.typography.cardTitle
      )
    }

    Spacer(modifier = Modifier.height(Dimens.spacing10))
    AppPanel {
      Text(text = "ORDEM DAS CENAS", style = AppTheme.typography.label.copy(color = AppTheme.colors.accentCyan))
      Spacer(modifier = Modifier.height(6.dp))
      ext.scenes.forEachIndexed { index, scene ->
        val stateLabel = when {
          scene.successes != null -> "${scene.successes} sucesso(s) • concluída"
          index == ext.resolvedScenes -> "cena ativa"
          else -> "aguarda"
        }
        ExtendedKidLine((index + 1).toString(), scene.kid, scene.skill, stateLabel)
      }
    }

    val nextIndex = ext.nextUnresolvedIndex
    val nextScene = nextIndex?.let(ext.scenes::get)
    if (nextScene != null) {
      Spacer(modifier = Modifier.height(Dimens.spacing10))
      val belongsToActiveKid = nextScene.kid.equals(gameState.kid.name, ignoreCase = true) ||
        nextScene.kid.equals(gameState.kid.name.substringBefore(' '), ignoreCase = true)
      if (belongsToActiveKid) {
        PrimaryButton(
          text = if (gameState.activeTest?.extendedSceneIndex == nextIndex) "RETOMAR CENA DE ${nextScene.kid.uppercase()}" else "JOGAR CENA DE ${nextScene.kid.uppercase()}",
          onClick = onStartKidScene,
          modifier = Modifier.fillMaxWidth(),
          testTag = "start_extended_kid_scene"
        )
      } else {
        AppPanel(borderColor = AppTheme.colors.accentPurple.copy(alpha = 0.4f)) {
          Text(
            text = "AGUARDANDO A CENA DE ${nextScene.kid.uppercase()}",
            style = AppTheme.typography.label.copy(color = AppTheme.colors.accentPurple)
          )
          Text(
            text = "Este cliente não inventa a rolagem de outro Kid. O progresso continua quando um resultado verificado dessa cena chega ao Motor.",
            style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(Dimens.spacing10))
    AppPanel(borderColor = AppTheme.colors.accentCyan.copy(alpha = 0.45f)) {
      Text(text = "PROGRESSO DO GRUPO", style = AppTheme.typography.label.copy(color = AppTheme.colors.accentCyan))
      Text(text = "${ext.totalSuccesses} / ${ext.target} sucessos", style = AppTheme.typography.screenTitle)
      Text(
        text = "${ext.resolvedScenes} de ${ext.kids} cenas resolvidas. O resultado coletivo só é determinado depois que todas as cenas terminarem.",
        style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
      )
    }

    Spacer(modifier = Modifier.height(Dimens.spacing10))
    AppPanel(borderColor = AppTheme.colors.accentPurple.copy(alpha = 0.4f)) {
      Text(text = "RESULTADO COLETIVO", style = AppTheme.typography.label.copy(color = AppTheme.colors.accentPurple))
      val resultText = when {
        !ext.allScenesResolved -> "Aguardando todas as cenas."
        ext.fullSuccess && ext.acceptedExtraConditions == 0 -> "Objetivo completo: o alvo foi atingido sem Condições extras."
        ext.fullSuccess && ext.acceptedExtraConditions > 0 -> "Compromisso alcançado com ${ext.acceptedExtraConditions} Condição(ões) extra(s)."
        ext.compromiseAvailable -> "Compromisso disponível: o total ficou entre metade do alvo e o alvo completo."
        ext.completeFailure -> "Falha completa: o total ficou abaixo de metade do Nível de Ameaça."
        ext.declinedCompromiseFailure -> "O grupo recusou o compromisso; o Mestre resolve a falha sem Condições extras voluntárias."
        else -> "Resultado aguardando resolução pelo Mestre."
      }
      Text(resultText, style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary))
    }

    if (ext.compromiseAvailable) {
      Spacer(modifier = Modifier.height(Dimens.spacing10))
      CompromisePanel(
        gameState = gameState,
        onAccept = onAcceptCompromise,
        onDecline = onDeclineCompromise
      )
    }

    Spacer(modifier = Modifier.height(Dimens.spacing24))
  }
}


@Composable
private fun CompromisePanel(
  gameState: GameState,
  onAccept: (List<CompromiseMark>) -> Unit,
  onDecline: () -> Unit
) {
  val ext = gameState.extended
  val needed = (ext.target - ext.totalSuccesses).coerceAtLeast(0)
  var selected by remember(ext.totalSuccesses, ext.target, ext.compromiseMarks.size) {
    mutableStateOf<List<CompromiseMark>>(emptyList())
  }

  AppPanel(borderColor = AppTheme.colors.accentAmber.copy(alpha = 0.6f)) {
    Text(
      text = "COMPROMISSO • FALTAM $needed SUCESSO(S)",
      style = AppTheme.typography.sectionTitle.copy(color = AppTheme.colors.accentAmber)
    )
    Text(
      text = "Cada Condição extra escolhida vale 1 sucesso. A escolha é voluntária e pode levar um Kid a ficar Quebrado.",
      style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
    )
    Spacer(modifier = Modifier.height(8.dp))

    gameState.group.members.forEach { member ->
      val marksForKid = selected.filter { it.kid.equals(member.name, ignoreCase = true) }
      val virtualConditions = member.conditions + marksForKid.mapNotNull { it.condition }
      val virtualBroken = member.broken || marksForKid.any { it.condition == null }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 6.dp)
      ) {
        Text(
          text = member.name.uppercase(),
          style = AppTheme.typography.label.copy(color = AppTheme.colors.textPrimary)
        )
        Text(
          text = when {
            virtualBroken -> "QUEBRADO selecionado"
            virtualConditions.isEmpty() -> "Sem Condições leves"
            else -> virtualConditions.joinToString(" • ") { it.label }
          },
          style = AppTheme.typography.bodySmall.copy(color = if (virtualBroken) AppTheme.colors.accentAmber else AppTheme.colors.textSecondary)
        )

        if (!virtualBroken && selected.size < needed) {
          Condition.entries.filterNot { it in virtualConditions }.forEach { condition ->
            Text(
              text = "+ ${condition.label}",
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  if (selected.size < needed) selected = selected + CompromiseMark(member.name, condition)
                }
                .padding(vertical = 7.dp),
              style = AppTheme.typography.body.copy(color = AppTheme.colors.accentCyan)
            )
          }
          if (virtualConditions.size == 4) {
            Text(
              text = "+ FICAR QUEBRADO",
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  if (selected.size < needed) selected = selected + CompromiseMark(member.name, null)
                }
                .padding(vertical = 7.dp),
              style = AppTheme.typography.body.copy(color = AppTheme.colors.accentAmber)
            )
          }
        }
      }
    }

    if (selected.isNotEmpty()) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "SELECIONADAS ${selected.size}/$needed • toque abaixo para limpar ou confirmar.",
        modifier = Modifier
          .fillMaxWidth()
          .clickable { selected = emptyList() }
          .padding(vertical = 8.dp),
        style = AppTheme.typography.label.copy(color = AppTheme.colors.accentPurple)
      )
    }

    PrimaryButton(
      text = "ACEITAR COMPROMISSO",
      onClick = { onAccept(selected) },
      enabled = needed > 0 && selected.size == needed,
      modifier = Modifier.fillMaxWidth(),
      testTag = "accept_extended_compromise"
    )
    Spacer(modifier = Modifier.height(6.dp))
    SecondaryButton(
      text = "NÃO ACEITAR CONDIÇÕES EXTRAS",
      onClick = onDecline,
      modifier = Modifier.fillMaxWidth(),
      testTag = "decline_extended_compromise"
    )
  }
}

@Composable
private fun ExtendedKidLine(order: String, name: String, skill: String, state: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(34.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(order, style = AppTheme.typography.label.copy(color = AppTheme.colors.accentAmber))
    Spacer(modifier = Modifier.width(8.dp))
    Text(name, style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary), modifier = Modifier.width(72.dp))
    Text(skill, style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentCyan), modifier = Modifier.weight(1f))
    Text(state, style = AppTheme.typography.caption.copy(color = AppTheme.colors.textSecondary))
  }
}

private fun climaxSceneDrawable(assetKey: String): Int = when (assetKey.removeSuffix(".png")) {
  "scene_loop_gate" -> R.drawable.scene_loop_gate
  "scene_maintenance_corridor" -> R.drawable.scene_maintenance_corridor
  "scene_town_square" -> R.drawable.scene_town_square
  "scene_hideout" -> R.drawable.scene_hideout
  "scene_family_kitchen" -> R.drawable.scene_family_kitchen
  else -> R.drawable.scene_reactor_hall
}
