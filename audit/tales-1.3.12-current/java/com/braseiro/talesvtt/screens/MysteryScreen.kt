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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.braseiro.talesvtt.R
import com.braseiro.talesvtt.components.AppPanel
import com.braseiro.talesvtt.components.AppShell
import com.braseiro.talesvtt.components.SectionHeader
import com.braseiro.talesvtt.components.SecondaryButton
import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.model.AppDestination
import com.braseiro.talesvtt.model.ContextIconType
import com.braseiro.talesvtt.model.ContextItemData
import com.braseiro.talesvtt.model.WorldStatusData
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun MysteryScreen(
  onNavigate: (AppDestination) -> Unit,
  gameState: GameState,
  onOpenConnections: () -> Unit,
  modifier: Modifier = Modifier
) {
  val contextItems = listOf(
    ContextItemData("myst_loc", "Local Atual", gameState.scene.location, ContextIconType.LOCATION),
    ContextItemData("myst_clues", "Pistas Reveladas", gameState.revealed.clues.size.toString(), ContextIconType.MYSTERY, isHighlighted = true),
    ContextItemData("myst_kids", "Kids Presentes", gameState.group.members.size.toString(), ContextIconType.CHARACTERS)
  )

  AppShell(
    currentDestination = AppDestination.MYSTERY,
    screenTitle = gameState.scene.title,
    chapterLabel = "MISTÉRIO",
    sceneLabel = "O que os Kids sabem",
    worldStatus = WorldStatusData(
      seasonYear = gameState.scene.seasonYear,
      weather = gameState.scene.weather,
      time = gameState.scene.time
    ),
    contextItems = contextItems,
    onNavigate = onNavigate,
    modifier = modifier
  ) {
    SectionHeader(title = "PISTAS REVELADAS", color = AppTheme.colors.accentAmber)
    Spacer(modifier = Modifier.height(Dimens.spacing8))

    if (gameState.revealed.clues.isEmpty()) {
      AppPanel {
        Text(
          text = "Nenhuma pista foi revelada ainda.",
          style = AppTheme.typography.body.copy(color = AppTheme.colors.textSecondary)
        )
      }
    } else {
      gameState.revealed.clues.sorted().forEach { clue ->
        val visual = clueVisual(clue)
        ClueCard(
          iconRes = visual.first,
          title = clue,
          body = visual.second
        )
        Spacer(modifier = Modifier.height(Dimens.spacing8))
      }
    }

    Spacer(modifier = Modifier.height(Dimens.spacing16))
    SectionHeader(title = "CONEXÕES CONHECIDAS", color = AppTheme.colors.accentCyan)
    Spacer(modifier = Modifier.height(Dimens.spacing8))

    AppPanel {
      if (gameState.revealed.connections.isEmpty()) {
        Text(
          text = "Nenhuma conexão revelada ainda.",
          style = AppTheme.typography.body.copy(color = AppTheme.colors.textSecondary)
        )
      } else {
        gameState.revealed.connections.forEach { (from, to) ->
          Text(
            text = "$from  →  $to",
            style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary)
          )
          Spacer(modifier = Modifier.height(6.dp))
        }
      }
    }

    Spacer(modifier = Modifier.height(Dimens.spacing12))
    SecondaryButton(
      text = "VER CONEXÕES REVELADAS",
      onClick = onOpenConnections,
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(Dimens.spacing24))
  }
}

@Composable
private fun ClueCard(iconRes: Int, title: String, body: String) {
  AppPanel(borderColor = AppTheme.colors.accentAmber.copy(alpha = 0.35f)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Image(
        painter = painterResource(iconRes),
        contentDescription = title,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(68.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(text = title, style = AppTheme.typography.cardTitle)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = body,
          style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
        )
      }
    }
  }
}


private fun clueVisual(title: String): Pair<Int, String> = when (title) {
  "Foto das luzes no lago" -> R.drawable.clue_photo_lights to "Uma fotografia já revelada aos Kids."
  "Fita cassete com ruído metálico" -> R.drawable.clue_audio_cassette to "Um registro de áudio já revelado aos Kids."
  "Bilhete rasgado" -> R.drawable.clue_torn_note to "Um documento já revelado aos Kids."
  else -> R.drawable.clue_marked_map to "Pista revelada pelo Mestre; nenhum detalhe oculto é mostrado aqui."
}
