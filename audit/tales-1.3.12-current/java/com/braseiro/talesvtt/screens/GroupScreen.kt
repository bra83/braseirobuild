package com.braseiro.talesvtt.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.braseiro.talesvtt.R
import com.braseiro.talesvtt.components.AppPanel
import com.braseiro.talesvtt.components.AppShell
import com.braseiro.talesvtt.components.PortraitSlot
import com.braseiro.talesvtt.components.SectionHeader
import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.model.AppDestination
import com.braseiro.talesvtt.model.ContextIconType
import com.braseiro.talesvtt.model.ContextItemData
import com.braseiro.talesvtt.model.WorldStatusData
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun GroupScreen(
  onNavigate: (AppDestination) -> Unit,
  gameState: GameState,
  modifier: Modifier = Modifier
) {
  val contextItems = listOf(
    ContextItemData("grp_members", "Kids Presentes", "${gameState.group.members.size} Kids", ContextIconType.CHARACTERS, isHighlighted = true),
    ContextItemData("grp_loc", "Local Atual", gameState.scene.location, ContextIconType.LOCATION),
    ContextItemData("grp_hideout", "Esconderijo", gameState.group.hideoutName, ContextIconType.MYSTERY)
  )

  AppShell(
    currentDestination = AppDestination.GROUP,
    screenTitle = "Grupo",
    chapterLabel = "KIDS & ESCONDERIJO",
    sceneLabel = "Visão geral",
    worldStatus = WorldStatusData(
      seasonYear = gameState.scene.seasonYear,
      weather = gameState.scene.weather,
      time = gameState.scene.time
    ),
    contextItems = contextItems,
    onNavigate = onNavigate,
    modifier = modifier
  ) {
    SectionHeader(title = "NOSSOS KIDS", color = AppTheme.colors.accentCyan)
    Spacer(modifier = Modifier.height(Dimens.spacing8))

    gameState.group.members.chunked(2).forEach { rowMembers ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8)
      ) {
        rowMembers.forEach { member ->
          val isCurrentKid = member.name.equals(gameState.kid.name, ignoreCase = true)
          val currentPortraitAsset = if (isCurrentKid) gameState.kid.portraitAssetKey else member.portraitAssetKey
          val currentPortraitImage = if (isCurrentKid) gameState.kid.portraitImageBase64 else null
          val stateLabel = when {
            member.broken -> "Quebrado"
            member.conditions.isEmpty() -> "OK"
            else -> member.conditions.first().label
          }
          val stateColor = if (member.broken || member.conditions.isNotEmpty()) {
            AppTheme.colors.accentAmber
          } else {
            AppTheme.colors.accentOlive
          }
          GroupKidCard(
            modifier = Modifier.weight(1f),
            name = member.name.substringBefore(' '),
            archetype = member.archetype,
            imageRes = portraitDrawable(currentPortraitAsset),
            imageBase64 = currentPortraitImage,
            state = stateLabel,
            stateColor = stateColor
          )
        }
        if (rowMembers.size == 1) Spacer(modifier = Modifier.weight(1f))
      }
      Spacer(modifier = Modifier.height(Dimens.spacing8))
    }

    Spacer(modifier = Modifier.height(Dimens.spacing8))

    SectionHeader(title = "RELAÇÕES DO GRUPO", color = AppTheme.colors.accentPurple)
    Spacer(modifier = Modifier.height(Dimens.spacing8))
    AppPanel {
      gameState.group.relationships.forEach { relationship ->
        RelationshipLine(relationship.left, relationship.relation, relationship.right)
      }
    }

    Spacer(modifier = Modifier.height(Dimens.spacing16))

    SectionHeader(title = "ESCONDERIJO", color = AppTheme.colors.accentCyan)
    Spacer(modifier = Modifier.height(Dimens.spacing8))

    AppPanel(borderColor = AppTheme.colors.accentCyan.copy(alpha = 0.35f)) {
      Image(
        painter = painterResource(R.drawable.scene_hideout),
        contentDescription = gameState.group.hideoutName,
        modifier = Modifier
          .fillMaxWidth()
          .height(190.dp),
        contentScale = ContentScale.Crop
      )
      Spacer(modifier = Modifier.height(10.dp))
      Text(text = gameState.group.hideoutName, style = AppTheme.typography.cardTitle)
      Text(
        text = gameState.group.hideoutDescription,
        style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary)
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Quando 2 ou mais Kids estão presentes, próximos e a cena permanece segura, a recuperação pode ocorrer conforme as regras do Esconderijo.",
        style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
      )
    }

    Spacer(modifier = Modifier.height(Dimens.spacing16))

    SectionHeader(title = "ANOTAÇÕES DO GRUPO", color = AppTheme.colors.accentAmber)
    Spacer(modifier = Modifier.height(Dimens.spacing8))
    AppPanel {
      if (gameState.group.notes.isEmpty()) {
        Text(
          text = "Nenhuma anotação do grupo ainda.",
          style = AppTheme.typography.body.copy(color = AppTheme.colors.textSecondary)
        )
      } else {
        Text(
          text = gameState.group.notes.joinToString("\n") { "• $it" },
          style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary)
        )
      }
    }

    Spacer(modifier = Modifier.height(Dimens.spacing24))
  }
}

@Composable
private fun GroupKidCard(
  name: String,
  archetype: String,
  imageRes: Int,
  imageBase64: String? = null,
  state: String?,
  stateColor: Color,
  modifier: Modifier = Modifier
) {
  AppPanel(modifier = modifier, contentPadding = 7.dp) {
    PortraitSlot(
      name = name,
      archetype = archetype,
      imageRes = imageRes,
      imageBase64 = imageBase64,
      size = 128.dp,
      borderColor = stateColor,
      modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    if (state != null) {
      Spacer(modifier = Modifier.height(7.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
          painter = painterResource(
            when (state) {
              "Assustado" -> R.drawable.tales_condition_scared
              "Chateado" -> R.drawable.tales_condition_upset
              "Exausto" -> R.drawable.tales_condition_exhausted
              "Machucado" -> R.drawable.tales_condition_injured
              "Quebrado" -> R.drawable.tales_condition_broken
              else -> R.drawable.nav_kid
            }
          ),
          contentDescription = state,
          modifier = Modifier.size(24.dp),
          contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = state,
          style = AppTheme.typography.label.copy(color = stateColor, fontWeight = FontWeight.Bold)
        )
      }
    }
  }
}

@Composable
private fun RelationshipLine(left: String, verb: String, right: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(34.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = left, style = AppTheme.typography.body.copy(color = AppTheme.colors.accentCyan))
    Spacer(modifier = Modifier.width(5.dp))
    Text(text = verb, style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary))
    if (right.isNotBlank()) {
      Spacer(modifier = Modifier.width(5.dp))
      Text(text = right, style = AppTheme.typography.body.copy(color = AppTheme.colors.accentPurple))
    }
  }
}


private fun portraitDrawable(assetKey: String): Int = when (assetKey.removeSuffix(".png")) {
  "kid_portrait_01", "tales_type_computer_geek" -> R.drawable.kid_portrait_01
  "kid_portrait_02", "tales_type_popular" -> R.drawable.kid_portrait_02
  "kid_portrait_03", "tales_type_jock" -> R.drawable.kid_portrait_03
  "kid_portrait_04", "tales_type_bookworm" -> R.drawable.kid_portrait_04
  else -> R.drawable.kid_portrait_01
}
