package com.braseiro.talesvtt.screens

import androidx.compose.foundation.Image
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
import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.model.AppDestination
import com.braseiro.talesvtt.model.ContextIconType
import com.braseiro.talesvtt.model.ContextItemData
import com.braseiro.talesvtt.model.WorldStatusData
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun ConnectionsScreen(
  onNavigate: (AppDestination) -> Unit,
  gameState: GameState,
  modifier: Modifier = Modifier
) {
  val contextItems = listOf(
    ContextItemData("con_myst", "Mistério", gameState.scene.title, ContextIconType.MYSTERY, isHighlighted = true),
    ContextItemData("con_nodes", "Conhecido", "${gameState.revealed.clues.size} pistas", ContextIconType.MYSTERY),
    ContextItemData("con_loc", "Local Atual", gameState.scene.location, ContextIconType.LOCATION)
  )

  AppShell(
    currentDestination = AppDestination.MYSTERY,
    screenTitle = "Conexões",
    chapterLabel = "MISTÉRIO",
    sceneLabel = "Somente o que foi revelado",
    worldStatus = WorldStatusData(gameState.scene.seasonYear, gameState.scene.weather, gameState.scene.time),
    contextItems = contextItems,
    onNavigate = onNavigate,
    modifier = modifier
  ) {
    SectionHeader(title = "REDE CONHECIDA", color = AppTheme.colors.accentCyan)
    Spacer(modifier = Modifier.height(Dimens.spacing8))

    if (gameState.revealed.connections.isEmpty()) {
      AppPanel {
        Text(
          text = "Nenhuma conexão foi revelada ainda.",
          style = AppTheme.typography.body.copy(color = AppTheme.colors.textSecondary)
        )
      }
    } else {
      gameState.revealed.connections.forEach { (from, to) ->
        val leftRes = when (from) {
          "Fita cassete com ruído metálico" -> R.drawable.clue_audio_cassette
          "Foto das luzes no lago" -> R.drawable.clue_photo_lights
          "Bilhete rasgado" -> R.drawable.clue_torn_note
          else -> R.drawable.clue_marked_map
        }
        val rightRes = when (to) {
          "Portão de Serviço" -> R.drawable.state_loop_installation
          "Casa de Lasse" -> R.drawable.loc_home
          "Escola Björkskolan" -> R.drawable.loc_school
          "Esconderijo" -> R.drawable.loc_hideout
          else -> R.drawable.state_current_location
        }
        ConnectionNode(leftRes, from, to, rightRes)
        Spacer(modifier = Modifier.height(Dimens.spacing8))
      }
    }
    Spacer(modifier = Modifier.height(Dimens.spacing16))
    AppPanel(borderColor = AppTheme.colors.accentPurple.copy(alpha = 0.4f)) {
      Text(
        text = "Não há nós ocultos, espaços vazios nem linhas apontando para pistas que os Kids ainda não descobriram.",
        style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary)
      )
    }

    Spacer(modifier = Modifier.height(Dimens.spacing24))
  }
}

@Composable
private fun ConnectionNode(leftRes: Int, left: String, right: String, rightRes: Int) {
  AppPanel {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Image(painterResource(leftRes), left, Modifier.size(58.dp), contentScale = ContentScale.Fit)
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(text = left, style = AppTheme.typography.cardTitle)
        Text(text = "conectado a", style = AppTheme.typography.caption.copy(color = AppTheme.colors.textMuted))
        Text(text = right, style = AppTheme.typography.body.copy(color = AppTheme.colors.accentCyan))
      }
      Image(painterResource(rightRes), right, Modifier.size(52.dp), contentScale = ContentScale.Fit)
    }
  }
}
