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
import com.braseiro.talesvtt.components.IconicItemVisual
import com.braseiro.talesvtt.components.AppShell
import com.braseiro.talesvtt.components.AssetImageSlot
import com.braseiro.talesvtt.components.SectionHeader
import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.model.AppDestination
import com.braseiro.talesvtt.model.ContextIconType
import com.braseiro.talesvtt.model.ContextItemData
import com.braseiro.talesvtt.model.WorldStatusData
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun BackpackScreen(
  onNavigate: (AppDestination) -> Unit,
  gameState: GameState,
  onUseIconicItem: () -> Unit,
  modifier: Modifier = Modifier
) {
  val contextItems = listOf(
    ContextItemData("bp_kid", "Kid", gameState.kid.name, ContextIconType.CHARACTERS),
    ContextItemData("bp_loc", "Local Atual", gameState.scene.location, ContextIconType.LOCATION),
    ContextItemData("bp_test", "Teste Ativo", if (gameState.activeTest != null) "Sim" else "Não", ContextIconType.MYSTERY)
  )

  AppShell(
    currentDestination = AppDestination.BACKPACK,
    screenTitle = "Mochila",
    chapterLabel = "ITENS DO KID",
    sceneLabel = gameState.kid.name,
    worldStatus = WorldStatusData(
      seasonYear = gameState.scene.seasonYear,
      weather = gameState.scene.weather,
      time = gameState.scene.time
    ),
    contextItems = contextItems,
    onNavigate = onNavigate,
    modifier = modifier
  ) {
    SectionHeader(title = "ITEM ICÔNICO", color = AppTheme.colors.accentAmber)
    Spacer(modifier = Modifier.height(Dimens.spacing8))

    AppPanel(borderColor = AppTheme.colors.accentPurple.copy(alpha = 0.55f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
          painter = painterResource(IconicItemVisual.drawable(gameState.kid.iconicItemName)),
          contentDescription = gameState.kid.iconicItemName,
          modifier = Modifier.size(104.dp),
          contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(text = gameState.kid.iconicItemName.uppercase(), style = AppTheme.typography.cardTitle)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "+2 dados quando claramente útil.",
            style = AppTheme.typography.body.copy(color = AppTheme.colors.accentPurple)
          )
          Spacer(modifier = Modifier.height(5.dp))
          Text(
            text = "O Mestre decide se o Item é aplicável à ação declarada.",
            style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
          )
        }
      }
      if (gameState.activeTest != null && gameState.testResolution == null && gameState.activeTest.approvedItemBonus == 0) {
        Spacer(modifier = Modifier.height(10.dp))
        com.braseiro.talesvtt.components.PrimaryButton(
          text = "PEDIR APROVAÇÃO NO TESTE",
          onClick = onUseIconicItem,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }

    Spacer(modifier = Modifier.height(Dimens.spacing16))
    SectionHeader(title = "ITENS POSSUÍDOS", color = AppTheme.colors.accentCyan)
    Spacer(modifier = Modifier.height(Dimens.spacing8))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      AssetImageSlot(
        title = "Lanterna",
        imageRes = R.drawable.item_flashlight,
        size = 82.dp,
        accentColor = AppTheme.colors.accentCyan
      )
      AssetImageSlot(
        title = "Fita cassete",
        imageRes = R.drawable.item_audio_cassette,
        size = 82.dp,
        accentColor = AppTheme.colors.accentAmber
      )
      AssetImageSlot(
        title = "Caderno",
        imageRes = R.drawable.item_notebook,
        size = 82.dp,
        accentColor = AppTheme.colors.accentOlive
      )
      AssetImageSlot(
        title = "Binóculos",
        imageRes = R.drawable.item_binoculars,
        size = 82.dp,
        accentColor = AppTheme.colors.accentPurple
      )
    }

    Spacer(modifier = Modifier.height(Dimens.spacing16))

    AppPanel {
      Text(text = "USO EM TESTES", style = AppTheme.typography.label.copy(color = AppTheme.colors.accentCyan))
      Spacer(modifier = Modifier.height(5.dp))
      Text(
        text = "A Mochila não adiciona dados sozinha. No teste ativo, o Item é apenas proposto; o Motor/Mestre valida a utilidade antes de qualquer bônus entrar no pool.",
        style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary)
      )
    }

    Spacer(modifier = Modifier.height(Dimens.spacing16))

    AppPanel(borderColor = AppTheme.colors.accentAmber.copy(alpha = 0.35f)) {
      Text(text = "ENTRE MISTÉRIOS", style = AppTheme.typography.label.copy(color = AppTheme.colors.accentAmber))
      Spacer(modifier = Modifier.height(5.dp))
      Text(
        text = "Itens comuns do Mistério podem deixar de estar disponíveis. A troca do Item Icônico acontece somente no fluxo permitido pelas regras.",
        style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
      )
    }

    Spacer(modifier = Modifier.height(Dimens.spacing24))
  }
}
