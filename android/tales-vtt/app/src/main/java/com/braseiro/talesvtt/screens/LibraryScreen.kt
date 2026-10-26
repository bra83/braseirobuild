package com.braseiro.talesvtt.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.braseiro.talesvtt.components.AppPanel
import com.braseiro.talesvtt.components.AppShell
import com.braseiro.talesvtt.components.PrimaryButton
import com.braseiro.talesvtt.components.SecondaryButton
import com.braseiro.talesvtt.components.SectionHeader
import com.braseiro.talesvtt.data.ManualLibraryStatus
import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.model.AppDestination
import com.braseiro.talesvtt.model.ContextIconType
import com.braseiro.talesvtt.model.ContextItemData
import com.braseiro.talesvtt.model.WorldStatusData
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun LibraryScreen(
  onNavigate: (AppDestination) -> Unit,
  gameState: GameState,
  status: ManualLibraryStatus,
  onImportPdfs: (List<android.net.Uri>) -> Unit,
  onSearch: (String) -> Unit,
  onRemove: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var query by remember { mutableStateOf("") }
  val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> onImportPdfs(uris) }

  AppShell(
    currentDestination = AppDestination.MENU,
    screenTitle = "Biblioteca",
    chapterLabel = "PDFs & RAG LOCAL",
    sceneLabel = gameState.scene.title,
    worldStatus = WorldStatusData(gameState.scene.seasonYear, gameState.scene.weather, gameState.scene.time),
    contextItems = listOf(
      ContextItemData("lib_count", "Manuais", "${status.manuals.size}", ContextIconType.MYSTERY, true),
      ContextItemData("lib_mode", "Indexação", if (status.busy) "Em andamento" else "Local", ContextIconType.CHARACTERS)
    ),
    onNavigate = onNavigate,
    onMenuClick = { onNavigate(AppDestination.MENU) },
    modifier = modifier
  ) {
    PrimaryButton("← VOLTAR ÀS CONFIGURAÇÕES", onClick = { onNavigate(AppDestination.MENU) }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(Dimens.spacing12))
    SectionHeader(title = "BIBLIOTECA LOCAL", color = AppTheme.colors.accentAmber)
    Spacer(Modifier.height(Dimens.spacing8))
    AppPanel {
      Text("Os PDFs ficam no aparelho. O índice textual é local e somente pequenos trechos recuperados entram no contexto do Motor Barbara.", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
      Spacer(Modifier.height(8.dp))
      PrimaryButton("ADICIONAR PDFs", onClick = { picker.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth())
      Spacer(Modifier.height(6.dp))
      Text(status.progress, style = AppTheme.typography.bodySmall.copy(color = if (status.lastError == null) AppTheme.colors.accentCyan else AppTheme.colors.accentAmber))
      status.lastError?.let { Text(it, style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentAmber)) }
    }

    Spacer(Modifier.height(Dimens.spacing16))
    SectionHeader(title = "MANUAIS INDEXADOS", color = AppTheme.colors.accentCyan)
    Spacer(Modifier.height(Dimens.spacing8))
    if (status.manuals.isEmpty()) {
      AppPanel { Text("Nenhum PDF indexado ainda.", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)) }
    } else status.manuals.forEach { manual ->
      AppPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) {
            Text(manual.title, style = AppTheme.typography.cardTitle)
            Text("${manual.pages} páginas • ${manual.chunks} trechos", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
          }
          Spacer(Modifier.width(8.dp))
          SecondaryButton("REMOVER", onClick = { onRemove(manual.id) })
        }
      }
      Spacer(Modifier.height(6.dp))
    }

    Spacer(Modifier.height(Dimens.spacing16))
    SectionHeader(title = "BUSCAR NO RAG", color = AppTheme.colors.accentPurple)
    Spacer(Modifier.height(Dimens.spacing8))
    OutlinedTextField(
      value = query, onValueChange = { query = it },
      label = { Text("Regra, termo ou situação") }, singleLine = true, modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(6.dp))
    PrimaryButton("BUSCAR", onClick = { onSearch(query) }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    status.results.forEach { hit ->
      AppPanel(borderColor = AppTheme.colors.accentPurple.copy(alpha = .45f)) {
        Text("${hit.title} • pág. ${hit.page}", style = AppTheme.typography.cardTitle)
        Text(hit.snippet, style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
      }
      Spacer(Modifier.height(6.dp))
    }
    Spacer(Modifier.height(Dimens.spacing24))
  }
}
