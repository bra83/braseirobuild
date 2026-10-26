package com.braseiro.talesvtt.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.braseiro.talesvtt.game.RulesProfile
import com.braseiro.talesvtt.model.AppDestination
import com.braseiro.talesvtt.ui.theme.AppTheme

/** A closed-by-default help sheet, never a blocking tutorial or an active GM. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidedModeSheet(
  page: GuidedPage,
  profile: RulesProfile,
  enabled: Boolean,
  onEnabledChange: (Boolean) -> Unit,
  onDismiss: () -> Unit,
  onNavigate: (AppDestination) -> Unit
) {
  var tab by remember { mutableStateOf(0) }
  ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("guide_sheet")) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 620.dp)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text("GUIA DO JOGADOR", style = AppTheme.typography.sectionTitle.copy(color = AppTheme.colors.accentCyan))
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Modo guiado ativo", style = AppTheme.typography.body)
        Switch(checked = enabled, onCheckedChange = onEnabledChange, modifier = Modifier.testTag("guide_enabled_switch"))
      }
      Text("O ícone ? fica no cabeçalho; não cobre a cena, não narra spoilers e não executa escolhas.", style = AppTheme.typography.bodySmall)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("AGORA") }, modifier = Modifier.testTag("guide_now_tab"))
        FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("PASSOS") }, modifier = Modifier.testTag("guide_steps_tab"))
        FilterChip(selected = tab == 2, onClick = { tab = 2 }, label = { Text("REGRAS") }, modifier = Modifier.testTag("guide_rules_tab"))
      }
      when (tab) {
        0 -> {
          Text(page.title, style = AppTheme.typography.sectionTitle.copy(color = AppTheme.colors.textPrimary))
          Text("REGRA / ESTADO ATUAL", style = AppTheme.typography.label.copy(color = AppTheme.colors.accentCyan))
          Text(page.rule, style = AppTheme.typography.body)
          Spacer(Modifier.height(4.dp))
          Text("O QUE FAZER AGORA", style = AppTheme.typography.label.copy(color = AppTheme.colors.accentAmber))
          Text(page.next, style = AppTheme.typography.body, modifier = Modifier.testTag("guide_next_action"))
          Text("POR QUÊ", style = AppTheme.typography.label.copy(color = AppTheme.colors.accentCyan))
          Text(page.why, style = AppTheme.typography.bodySmall)
          if (page.destination != null && page.destinationLabel != null) {
            OutlinedButton(onClick = { onDismiss(); onNavigate(page.destination) }, modifier = Modifier.fillMaxWidth().testTag("guide_go_to_screen")) {
              Text(page.destinationLabel)
            }
          }
        }
        1 -> {
          Text("PASSO A PASSO • ${page.title}", style = AppTheme.typography.sectionTitle)
          page.steps.forEachIndexed { index, step ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Text("${index + 1}. ${step.instruction}", style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold))
              Text(step.reason, style = AppTheme.typography.bodySmall)
            }
            HorizontalDivider()
          }
          Text("Siga o jogo pela tela original: este guia não realiza nenhuma ação por você.", style = AppTheme.typography.bodySmall)
        }
        else -> {
          Text("CONSULTA DE REGRAS • ${if (profile == RulesProfile.FLOOD_TEENS) "FLOOD" else "TALES"}", style = AppTheme.typography.sectionTitle)
          GuidedMode.ruleCards(profile).forEach { (heading, detail) ->
            Text(heading, style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold))
            Text(detail, style = AppTheme.typography.bodySmall)
            HorizontalDivider()
          }
          Text("Para verificar detalhes de um suplemento ou referências do livro, use MENU → BIBLIOTECA. Este resumo não substitui os livros.", style = AppTheme.typography.bodySmall)
        }
      }
      TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("FECHAR E CONTINUAR JOGANDO") }
      Spacer(Modifier.height(8.dp))
    }
  }
}

/** Single persisted preference shared by all screens and all navigation destinations. */
@Composable
fun rememberGuidedModePreference(): Pair<Boolean, (Boolean) -> Unit> {
  val context = LocalContext.current
  val prefs = remember(context) { context.getSharedPreferences("braseiro_guided_mode", Context.MODE_PRIVATE) }
  var enabled by remember(prefs) { mutableStateOf(prefs.getBoolean("enabled", true)) }
  return enabled to { value ->
    enabled = value
    prefs.edit().putBoolean("enabled", value).apply()
  }
}

@Composable
fun rememberUnseenGuideTip(id: String, enabled: Boolean, open: Boolean): Boolean {
  val context = LocalContext.current
  val prefs = remember(context) { context.getSharedPreferences("braseiro_guided_mode", Context.MODE_PRIVATE) }
  var seenId by remember(prefs) { mutableStateOf(prefs.getString("seen_stage", "").orEmpty()) }
  LaunchedEffect(open, id) {
    if (open && seenId != id) {
      seenId = id
      prefs.edit().putString("seen_stage", id).apply()
    }
  }
  return enabled && seenId != id
}

/** Same compact ? entry point in internal configuration screens without AppShell. */
@Composable
fun GuidedModeAction(title: String, destination: AppDestination = AppDestination.MENU) {
  val context = LocalGuideContext.current
  val page = GuidedMode.resolve(destination, title, context)
  val (enabled, onEnabledChange) = rememberGuidedModePreference()
  var open by remember { mutableStateOf(false) }
  val unseen = rememberUnseenGuideTip(page.id, enabled, open)
  if (open) GuidedModeSheet(
    page = page, profile = context.game.rulesProfile, enabled = enabled,
    onEnabledChange = onEnabledChange, onDismiss = { open = false },
    onNavigate = { open = false }
  )
  IconButton(onClick = { open = true }, modifier = Modifier.testTag("guided_mode_action")) {
    Box(contentAlignment = Alignment.Center) {
      Icon(imageVector = AppIcons.Help, contentDescription = "Guia do jogador nesta tela", tint = AppTheme.colors.accentCyan)
      if (unseen) Box(
        modifier = Modifier.align(Alignment.TopEnd).size(5.dp).background(AppTheme.colors.accentAmber, CircleShape)
      )
    }
  }
}