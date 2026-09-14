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
import com.braseiro.talesvtt.components.SectionHeader
import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.game.KidItemKind
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
  onUseItem: (String) -> Unit,
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

    if (gameState.kid.items.isEmpty()) {
      AppPanel {
        Text(
          text = "Nenhum objeto comum ou Item do Mistério está registrado nesta aventura.",
          style = AppTheme.typography.body.copy(color = AppTheme.colors.textSecondary)
        )
      }
    } else {
      gameState.kid.items.forEach { item ->
        AppPanel(borderColor = if (item.kind == KidItemKind.MYSTERY) AppTheme.colors.accentAmber.copy(alpha = 0.45f) else AppTheme.colors.accentCyan.copy(alpha = 0.30f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
              painter = painterResource(inventoryItemDrawable(item.name)),
              contentDescription = item.name,
              modifier = Modifier.size(58.dp),
              contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(item.name.uppercase(), style = AppTheme.typography.cardTitle)
              if (item.description.isNotBlank()) Text(item.description, style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
              Text(
                text = if (item.kind == KidItemKind.MYSTERY) "+${item.bonus} dados quando este Item específico for claramente útil e aprovado pelo Mestre." else "Objeto comum • sem bônus automático.",
                style = AppTheme.typography.bodySmall.copy(color = if (item.kind == KidItemKind.MYSTERY) AppTheme.colors.accentAmber else AppTheme.colors.textMuted)
              )
            }
          }
          if (item.kind == KidItemKind.MYSTERY && gameState.activeTest != null && gameState.testResolution == null && gameState.activeTest.approvedItemBonus == 0) {
            Spacer(modifier = Modifier.height(8.dp))
            com.braseiro.talesvtt.components.SecondaryButton(
              text = "PROPOR ESTE ITEM NO TESTE",
              onClick = { onUseItem(item.name) },
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
        Spacer(modifier = Modifier.height(6.dp))
      }
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


private fun inventoryItemDrawable(name: String): Int {
  val n = name.lowercase()
  return when {
    "lanterna" in n -> R.drawable.item_flashlight
    "cassete" in n || "fita" in n || "gravador" in n -> R.drawable.item_audio_cassette
    "binócul" in n -> R.drawable.item_binoculars
    "caderno" in n || "mapa" in n || "anota" in n -> R.drawable.item_notebook
    else -> R.drawable.item_notebook
  }
}
