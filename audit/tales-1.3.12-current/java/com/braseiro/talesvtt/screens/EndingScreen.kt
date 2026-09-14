package com.braseiro.talesvtt.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.braseiro.talesvtt.components.PrimaryButton
import com.braseiro.talesvtt.components.SecondaryButton
import com.braseiro.talesvtt.components.SceneImageSlot
import com.braseiro.talesvtt.components.SectionHeader
import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.game.EndingMode
import com.braseiro.talesvtt.game.TalesSkillCatalog
import com.braseiro.talesvtt.model.AppDestination
import com.braseiro.talesvtt.model.ContextIconType
import com.braseiro.talesvtt.model.ContextItemData
import com.braseiro.talesvtt.model.WorldStatusData
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun EndingScreen(
  onNavigate: (AppDestination) -> Unit,
  gameState: GameState,
  onSetXpAnswer: (Int, Boolean) -> Unit,
  onConfirmChanges: () -> Unit,
  onImproveSkill: (String) -> Unit,
  onSetBirthdayPassed: (Boolean) -> Unit,
  onApplyBirthdayAttribute: (String) -> Unit,
  onStartNextSession: () -> Unit,
  modifier: Modifier = Modifier
) {
  val contextItems = listOf(
    ContextItemData("end_myst", if (gameState.endingMode == EndingMode.SESSION) "Sessão" else "Mistério", "Concluído", ContextIconType.MYSTERY, isHighlighted = true),
    ContextItemData("end_kid", "Kid", gameState.kid.name.substringBefore(' '), ContextIconType.CHARACTERS),
    ContextItemData("end_xp", "XP Atual", gameState.kid.xp.toString(), ContextIconType.MYSTERY)
  )

  AppShell(
    currentDestination = AppDestination.MYSTERY,
    screenTitle = "Encerramento",
    chapterLabel = "MUDANÇA & EXPERIÊNCIA",
    sceneLabel = gameState.scene.title,
    worldStatus = WorldStatusData(gameState.scene.seasonYear, gameState.scene.weather, gameState.scene.time),
    contextItems = contextItems,
    onNavigate = onNavigate,
    modifier = modifier
  ) {
    SceneImageSlot(
      imageRes = endingSceneDrawable(gameState.scene.visualAssetKey),
      locationName = gameState.scene.location.uppercase(),
      sceneTone = "Consequências",
      showTts = false,
      onCharactersClick = { onNavigate(AppDestination.GROUP) },
      onLocationClick = { onNavigate(AppDestination.MAP) }
    )
    Spacer(modifier = Modifier.height(Dimens.spacing10))

    AppPanel(borderColor = AppTheme.colors.accentCyan.copy(alpha = 0.45f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.mystery_complete), "Mistério encerrado", Modifier.size(54.dp), contentScale = ContentScale.Fit)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(text = if (gameState.endingMode == EndingMode.SESSION) "SESSÃO ENCERRADA" else "MISTÉRIO ENCERRADO", style = AppTheme.typography.cardTitle)
          Text(
            text = if (gameState.endingMode == EndingMode.SESSION) "Calcule o XP desta sessão. Condições permanecem até uma recuperação válida ou o fim do Mistério." else "Os Kids voltam para casa. Agora é hora de responder XP e revisar Mudanças.",
            style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(Dimens.spacing12))
    SectionHeader(title = "EXPERIÊNCIA", color = AppTheme.colors.accentAmber)
    Spacer(modifier = Modifier.height(Dimens.spacing6))
    AppPanel {
      val questions = listOf(
        "Você participou da sessão?",
        "Você esteve em apuros por causa do seu Problema ou Relacionamentos?",
        "Você usou ou lutou com seu Orgulho?",
        "Você se colocou em risco pelos outros Kids?",
        "Você aprendeu algo novo?"
      )
      questions.forEachIndexed { index, question ->
        XpQuestion(
          text = question,
          answer = gameState.xpAnswers.getOrNull(index),
          onYes = { onSetXpAnswer(index, true) },
          onNo = { onSetXpAnswer(index, false) }
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      val answered = gameState.xpAnswers.all { it != null }
      val gained = gameState.xpAnswers.count { it == true }
      Text(
        text = when {
          gameState.sessionXpAwarded -> "XP desta sessão aplicado • total ${gameState.kid.xp}"
          answered -> "XP calculado pelas respostas: +$gained"
          else -> "Responda às cinco perguntas para calcular XP."
        },
        style = AppTheme.typography.cardTitle.copy(color = AppTheme.colors.accentCyan)
      )
      gameState.lastError?.let {
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = it, style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentAmber))
      }
    }

    if (gameState.endingMode == EndingMode.MYSTERY) {
      Spacer(modifier = Modifier.height(Dimens.spacing12))
      SectionHeader(title = "MUDANÇA", color = AppTheme.colors.accentPurple)
      Spacer(modifier = Modifier.height(Dimens.spacing6))
      AppPanel {
        Text(
          text = "Entre Mistérios, todas as Condições são curadas. Problema, Orgulho, Item Icônico e Relacionamentos podem ser revisados quando a ficção justificar.",
          style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary)
        )
      }

      if (gameState.sessionXpAwarded) {
        Spacer(modifier = Modifier.height(Dimens.spacing8))
        AppPanel(borderColor = AppTheme.colors.accentPurple.copy(alpha = 0.45f)) {
          Text(text = "ANIVERSÁRIO", style = AppTheme.typography.label.copy(color = AppTheme.colors.accentPurple))
          when {
            !gameState.change.birthdayReviewed -> {
              Text(
                text = "O aniversário de ${gameState.kid.name.substringBefore(' ')} aconteceu desde o último Mistério?",
                style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row {
                SecondaryButton(text = "SIM", onClick = { onSetBirthdayPassed(true) }, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                SecondaryButton(text = "NÃO", onClick = { onSetBirthdayPassed(false) }, modifier = Modifier.weight(1f))
              }
            }
            gameState.change.birthdayPassed && !gameState.change.birthdayApplied -> {
              Text(
                text = "Escolha 1 Atributo para receber +1. Nenhum Atributo pode ultrapassar 5.",
                style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
              )
              gameState.kid.attributes.forEach { (attribute, current) ->
                Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(text = attribute, style = AppTheme.typography.label.copy(color = AppTheme.colors.textPrimary), modifier = Modifier.weight(1f))
                  Text(text = if (current >= 5) "$current • MÁXIMO" else "$current → ${current + 1}", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentCyan))
                  Spacer(modifier = Modifier.width(8.dp))
                  SecondaryButton(
                    text = if (current >= 5) "MÁXIMO" else "+1",
                    onClick = { onApplyBirthdayAttribute(attribute) },
                    enabled = current < 5,
                    modifier = Modifier.width(92.dp)
                  )
                }
              }
            }
            gameState.change.birthdayPassed && gameState.change.birthdayApplied -> {
              Text(
                text = "Aniversário aplicado • ${gameState.kid.age} anos • ${gameState.change.improvedAttribute ?: "Atributo"} +1 • Sorte máxima ${gameState.kid.maxLuck}.",
                style = AppTheme.typography.body.copy(color = AppTheme.colors.accentCyan)
              )
              if (gameState.change.newKidRequired) {
                Text(
                  text = "Aos 16 anos este Kid segue em frente. É necessário criar um novo Kid antes de continuar.",
                  style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentAmber)
                )
              }
            }
            else -> Text(
              text = "Nenhum aniversário ocorreu desde o último Mistério.",
              style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(Dimens.spacing12))
    SectionHeader(title = "APRIMORAR PERÍCIA", color = AppTheme.colors.accentCyan)
    Spacer(modifier = Modifier.height(Dimens.spacing6))
    AppPanel {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.skill_improve), "Aprimorar Perícia", Modifier.size(50.dp), contentScale = ContentScale.Fit)
        Spacer(modifier = Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(text = "5 XP = +1 em uma Perícia", style = AppTheme.typography.cardTitle)
          Text(
            text = "Disponível somente fora da sessão. Máximo da Perícia: 5 • XP armazenado: ${gameState.kid.xp}/10.",
            style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
          )
        }
      }

      if (!gameState.sessionXpAwarded) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Conclua primeiro as cinco perguntas de XP desta sessão.",
          style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
        )
      } else if (gameState.kid.xp < 5) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Você precisa de 5 XP para aumentar uma Perícia.",
          style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
        )
      } else {
        Spacer(modifier = Modifier.height(10.dp))
        TalesSkillCatalog.skillToAttribute.keys.forEach { skill ->
          val current = gameState.kid.skills[skill] ?: 0
          SkillImproveRow(
            skill = skill,
            current = current,
            onImprove = { onImproveSkill(skill) }
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(Dimens.spacing12))
    if (!gameState.sessionXpAwarded) {
      PrimaryButton(
        text = "CONFIRMAR XP",
        onClick = onConfirmChanges,
        enabled = gameState.xpAnswers.all { it != null },
        modifier = Modifier.fillMaxWidth()
      )
    } else {
      val changeReady = gameState.endingMode != EndingMode.MYSTERY ||
        (gameState.change.birthdayReviewed && (!gameState.change.birthdayPassed || gameState.change.birthdayApplied))
      PrimaryButton(
        text = if (gameState.endingMode == EndingMode.MYSTERY) "INICIAR PRÓXIMO MISTÉRIO" else "INICIAR PRÓXIMA SESSÃO",
        onClick = onStartNextSession,
        enabled = changeReady && !gameState.change.newKidRequired,
        modifier = Modifier.fillMaxWidth()
      )
    }
    Spacer(modifier = Modifier.height(Dimens.spacing24))
  }
}

@Composable
private fun XpQuestion(
  text: String,
  answer: Boolean?,
  onYes: () -> Unit,
  onNo: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = text, style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textPrimary), modifier = Modifier.weight(1f))
    Text(
      text = "SIM",
      modifier = Modifier.clickable(onClick = onYes).padding(horizontal = 8.dp, vertical = 6.dp),
      style = AppTheme.typography.label.copy(color = if (answer == true) AppTheme.colors.accentCyan else AppTheme.colors.textMuted)
    )
    Text(
      text = "NÃO",
      modifier = Modifier.clickable(onClick = onNo).padding(horizontal = 8.dp, vertical = 6.dp),
      style = AppTheme.typography.label.copy(color = if (answer == false) AppTheme.colors.accentAmber else AppTheme.colors.textMuted)
    )
  }
}


@Composable
private fun SkillImproveRow(
  skill: String,
  current: Int,
  onImprove: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = skill.uppercase(), style = AppTheme.typography.label.copy(color = AppTheme.colors.textPrimary))
      Text(
        text = if (current >= 5) "$current • MÁXIMO" else "$current → ${current + 1}",
        style = AppTheme.typography.bodySmall.copy(color = if (current >= 5) AppTheme.colors.textMuted else AppTheme.colors.accentCyan)
      )
    }
    SecondaryButton(
      text = if (current >= 5) "MÁXIMO" else "AUMENTAR",
      onClick = onImprove,
      enabled = current < 5,
      modifier = Modifier.width(112.dp),
      testTag = "improve_${skill.lowercase()}"
    )
  }
}

private fun endingSceneDrawable(assetKey: String): Int = when (assetKey.removeSuffix(".png")) {
  "scene_loop_gate" -> R.drawable.scene_loop_gate
  "scene_maintenance_corridor" -> R.drawable.scene_maintenance_corridor
  "scene_town_square" -> R.drawable.scene_town_square
  "scene_hideout" -> R.drawable.scene_hideout
  "scene_family_kitchen" -> R.drawable.scene_family_kitchen
  "scene_reactor_hall" -> R.drawable.scene_reactor_hall
  else -> R.drawable.scene_town_square
}
