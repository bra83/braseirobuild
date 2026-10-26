package com.braseiro.talesvtt.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.braseiro.talesvtt.R
import com.braseiro.talesvtt.BuildConfig
import com.braseiro.talesvtt.components.AppPanel
import com.braseiro.talesvtt.components.AppShell
import com.braseiro.talesvtt.components.PrimaryButton
import com.braseiro.talesvtt.components.SecondaryButton
import com.braseiro.talesvtt.components.SectionHeader
import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.game.MotorRuntimeStatus
import com.braseiro.talesvtt.game.MotorTransportConfig
import com.braseiro.talesvtt.game.RulesProfile
import com.braseiro.talesvtt.model.AppDestination
import com.braseiro.talesvtt.model.ContextIconType
import com.braseiro.talesvtt.model.ContextItemData
import com.braseiro.talesvtt.model.WorldStatusData
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens
import com.braseiro.talesvtt.updater.AppUpdateManager
import com.braseiro.talesvtt.updater.UpdateCheckResult
import com.braseiro.talesvtt.updater.UpdateStatus
import kotlinx.coroutines.launch

@Composable
fun MenuScreen(
  onNavigate: (AppDestination) -> Unit,
  gameState: GameState,
  adventureStarted: Boolean,
  hasManualSave: Boolean,
  motorStatus: MotorRuntimeStatus,
  motorConfig: MotorTransportConfig,
  dataStatus: String,
  automaticBackupConfigured: Boolean,
  onSaveNow: () -> Unit,
  onLoadSave: () -> Unit,
  onSaveMotorConfig: (String?, String, String, String, String) -> Unit,
  onClearMotorLocalApiKey: () -> Unit,
  onExportBackup: (Uri) -> Unit,
  onImportBackup: (Uri) -> Unit,
  onSelectBackupDirectory: (Uri) -> Unit,
  onExportKid: (Uri) -> Unit,
  onImportKid: (Uri) -> Unit,
  onOpenLibrary: () -> Unit,
  onOpenAdventures: () -> Unit = {},
  onOpenVoiceSettings: () -> Unit = {},
  onStartRandomAdventure: () -> Unit,
  onCreateRandomKid: () -> Unit,
  onSelectRulesProfile: (RulesProfile) -> Unit,
  onContinueAdventure: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var apiKeyInput by remember { mutableStateOf("") }
  var modelInput by remember(motorConfig.model) { mutableStateOf(motorConfig.model) }
  var ttsModelInput by remember(motorConfig.ttsModel) { mutableStateOf(motorConfig.ttsModel) }
  var ttsVoiceInput by remember(motorConfig.ttsVoice) { mutableStateOf(motorConfig.ttsVoice) }
  var ttsMode by remember(motorConfig.ttsMode) { mutableStateOf(motorConfig.ttsMode) }
  val flood = gameState.rulesProfile == RulesProfile.FLOOD_TEENS
  val profileLabel = if (flood) "Things from the Flood / Teens" else "Tales from the Loop / Kids"
  val characterLabel = if (flood) "TEEN" else "KID"

  val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(onExportBackup) }
  val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(onImportBackup) }
  val selectBackupDir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { it?.let(onSelectBackupDirectory) }
  val exportKid = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(onExportKid) }
  val importKid = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(onImportKid) }

  AppShell(
    currentDestination = AppDestination.MENU,
    screenTitle = "Menu",
    chapterLabel = "CAMPANHA & CONFIGURAÇÕES",
    sceneLabel = gameState.scene.title,
    worldStatus = WorldStatusData(gameState.scene.seasonYear, gameState.scene.weather, gameState.scene.time),
    contextItems = listOf(
      ContextItemData("menu_profile", "Perfil", if(flood) "Flood / Teens" else "Tales / Kids", ContextIconType.CHARACTERS, true),
      ContextItemData("menu_save", "Estado", if (hasManualSave) "Save disponível" else "Autosave", ContextIconType.MYSTERY)
    ),
    onNavigate = onNavigate,
    modifier = modifier
  ) {
    SectionHeader(title = "SISTEMA", color = AppTheme.colors.accentPurple)
    Spacer(Modifier.height(Dimens.spacing8))
    AppPanel(borderColor = AppTheme.colors.accentPurple.copy(alpha = .45f)) {
      Text(profileLabel.uppercase(), style = AppTheme.typography.cardTitle)
      Text("Tales/Kids e Flood/Teens usam contratos mecânicos separados. A troca fica bloqueada durante uma campanha ativa.", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
      Spacer(Modifier.height(8.dp))
      Row {
        if (!flood) PrimaryButton("TALES • KIDS", onClick = { onSelectRulesProfile(RulesProfile.TALES_KIDS) }, modifier = Modifier.weight(1f))
        else SecondaryButton("TALES • KIDS", onClick = { onSelectRulesProfile(RulesProfile.TALES_KIDS) }, enabled = !adventureStarted, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(7.dp))
        if (flood) PrimaryButton("FLOOD • TEENS", onClick = { onSelectRulesProfile(RulesProfile.FLOOD_TEENS) }, modifier = Modifier.weight(1f))
        else SecondaryButton("FLOOD • TEENS", onClick = { onSelectRulesProfile(RulesProfile.FLOOD_TEENS) }, enabled = !adventureStarted, modifier = Modifier.weight(1f))
      }
    }

    Spacer(Modifier.height(Dimens.spacing16))
    SectionHeader(title = "COMEÇAR", color = AppTheme.colors.accentCyan)
    Spacer(Modifier.height(Dimens.spacing8))
    AppPanel(borderColor = AppTheme.colors.accentCyan.copy(alpha = .45f)) {
      Text("VOCÊ DECIDE QUANDO A AVENTURA COMEÇA", style = AppTheme.typography.cardTitle)
      Text(
        "Configure o Motor/voz, crie ou importe seu ${if(flood) "Teen" else "Kid"} e só então inicie. Nenhuma aventura é iniciada automaticamente.",
        style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
      )
      Spacer(Modifier.height(8.dp))
        PrimaryButton("AVENTURAS E MISTÉRIOS (JSON / BÁRBARA)", onClick = onOpenAdventures, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
      PrimaryButton("NOVA AVENTURA ALEATÓRIA", onClick = onStartRandomAdventure, modifier = Modifier.fillMaxWidth())
      Spacer(Modifier.height(6.dp))
      SecondaryButton("CRIAR $characterLabel ALEATÓRIO PELAS REGRAS", onClick = onCreateRandomKid, modifier = Modifier.fillMaxWidth())
      if (adventureStarted) {
        Spacer(Modifier.height(6.dp))
        SecondaryButton("CONTINUAR AVENTURA ATUAL", onClick = onContinueAdventure, modifier = Modifier.fillMaxWidth())
      }
      Spacer(Modifier.height(6.dp))
      Text(dataStatus, style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentAmber))
    }

    Spacer(Modifier.height(Dimens.spacing16))
    SectionHeader(title = "CAMPANHA ATUAL", color = AppTheme.colors.accentAmber)
    Spacer(Modifier.height(Dimens.spacing8))
    AppPanel {
      if (adventureStarted) {
        Text(gameState.scene.title, style = AppTheme.typography.screenTitle)
        Text("$profileLabel • ${gameState.group.members.size} ${if(flood) "Teens" else "Kids"} • ${gameState.scene.location}", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
      } else {
        Text("Nenhuma aventura iniciada", style = AppTheme.typography.screenTitle)
        Text("Seu ${if(flood) "Teen" else "Kid"} atual: ${gameState.kid.name} • ${gameState.kid.age} anos • ${gameState.kid.archetype}", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
      }
    }

    Spacer(Modifier.height(Dimens.spacing16))
    SectionHeader(title = "SAVE & BACKUP", color = AppTheme.colors.accentCyan)
    Spacer(Modifier.height(Dimens.spacing8))
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp)) {
      PrimaryButton("SALVAR AGORA", onClick = onSaveNow, modifier = Modifier.fillMaxWidth())
      SecondaryButton(if (hasManualSave) "CARREGAR SAVE LOCAL" else "NENHUM SAVE MANUAL", onClick = { if (hasManualSave) onLoadSave() }, modifier = Modifier.fillMaxWidth())
      PrimaryButton("EXPORTAR BACKUP COMPLETO", onClick = { exportBackup.launch("Braseiro_Tales_backup.json") }, modifier = Modifier.fillMaxWidth())
      SecondaryButton("IMPORTAR BACKUP", onClick = { importBackup.launch(arrayOf("application/json", "text/json", "text/plain")) }, modifier = Modifier.fillMaxWidth())
      SecondaryButton(if (automaticBackupConfigured) "TROCAR PASTA DE BACKUP AUTOMÁTICO" else "ESCOLHER PASTA DE BACKUP AUTOMÁTICO", onClick = { selectBackupDir.launch(null) }, modifier = Modifier.fillMaxWidth())
      Text("O backup portátil inclui estado jogável e estado privado do Mestre, com validação e round-trip antes de ser aceito.", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
    }

    Spacer(Modifier.height(Dimens.spacing16))
    SectionHeader(title = "FICHA DO $characterLabel • JSON", color = AppTheme.colors.accentOlive)
    Spacer(Modifier.height(Dimens.spacing8))
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp)) {
      PrimaryButton("EXPORTAR FICHA JSON", onClick = { exportKid.launch("Ficha_${gameState.kid.name.replace(Regex("[^A-Za-z0-9_-]"), "_")}.json") }, modifier = Modifier.fillMaxWidth())
      SecondaryButton("IMPORTAR FICHA JSON", onClick = { importKid.launch(arrayOf("application/json", "text/json", "text/plain")) }, modifier = Modifier.fillMaxWidth())
      Text(if(flood) "A importação valida Flood/Teens: 14–19 anos, Atributos total 14, Perícias, Vergonha, Cicatrizes e ausência de Sorte/Orgulho." else "A importação valida Tales/Kids, idade, Atributos, Perícias, Sorte e campos obrigatórios antes de alterar a campanha.", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
    }

    Spacer(Modifier.height(Dimens.spacing16))
    SectionHeader(title = "BIBLIOTECA & RAG", color = AppTheme.colors.accentPurple)
    Spacer(Modifier.height(Dimens.spacing8))
    AppPanel(borderColor = AppTheme.colors.accentPurple.copy(alpha = .45f)) {
      Text("PDFs LOCAIS", style = AppTheme.typography.cardTitle)
      Text("Adicione manuais, indexe texto/OCR e pesquise regras. O Motor recebe somente os trechos recuperados, não o PDF inteiro.", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
      Spacer(Modifier.height(8.dp))
      PrimaryButton("ABRIR BIBLIOTECA", onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth())
    }

    Spacer(Modifier.height(Dimens.spacing16))
    SectionHeader(title = "MOTOR BARBARA • GEMINI", color = AppTheme.colors.accentCyan)
    Spacer(Modifier.height(Dimens.spacing8))
    AppPanel(borderColor = AppTheme.colors.accentCyan.copy(alpha = .4f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.motor_barbara), "Motor Barbara", Modifier.size(58.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
          Text("MESTRE AUTOMÁTICO", style = AppTheme.typography.cardTitle)
          Text(if (motorStatus.transportConnected) "CONECTADO • EVENTOS VALIDADOS" else "FIREWALL PRONTO", style = AppTheme.typography.bodySmall.copy(color = if (motorStatus.transportConnected) AppTheme.colors.accentOlive else AppTheme.colors.accentAmber))
          Text(motorStatus.lastMessage, style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
        }
      }
      Spacer(Modifier.height(10.dp))
      OutlinedTextField(modelInput, { modelInput = it }, label = { Text("Modelo Gemini • texto") }, singleLine = true, modifier = Modifier.fillMaxWidth())
      Spacer(Modifier.height(7.dp))
      OutlinedTextField(ttsModelInput, { ttsModelInput = it }, label = { Text("Modelo Gemini • voz") }, singleLine = true, modifier = Modifier.fillMaxWidth())
      Spacer(Modifier.height(7.dp))
      OutlinedTextField(ttsVoiceInput, { ttsVoiceInput = it }, label = { Text("Voz Gemini") }, singleLine = true, modifier = Modifier.fillMaxWidth())
      Spacer(Modifier.height(7.dp))
      Row {
        SecondaryButton(if (ttsMode == "gemini") "● GEMINI TTS" else "GEMINI TTS", onClick = { ttsMode = "gemini" }, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(7.dp))
        SecondaryButton(if (ttsMode == "android") "● TTS ANDROID" else "TTS ANDROID", onClick = { ttsMode = "android" }, modifier = Modifier.weight(1f))
      }
      Spacer(Modifier.height(7.dp))
      OutlinedTextField(
        apiKeyInput, { apiKeyInput = it },
        label = { Text(if (motorConfig.apiKeyConfigured) "Substituir chave Gemini (opcional)" else "Chave Gemini") },
        placeholder = { Text(if (motorConfig.apiKeyConfigured) "Já configurada" else "Cole sua chave") },
        visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth()
      )
      Spacer(Modifier.height(8.dp))
      PrimaryButton("SALVAR IA & VOZ", onClick = {
        onSaveMotorConfig(apiKeyInput.ifBlank { null }, modelInput, ttsModelInput, ttsVoiceInput, ttsMode)
        apiKeyInput = ""
      }, modifier = Modifier.fillMaxWidth())
      if (motorConfig.usingLocalOverride) {
        Spacer(Modifier.height(6.dp)); SecondaryButton("REMOVER CHAVE LOCAL", onClick = onClearMotorLocalApiKey, modifier = Modifier.fillMaxWidth())
      }
    }

    Spacer(Modifier.height(Dimens.spacing16))
    SectionHeader(title = "ILUSTRAÇÃO DA CENA", color = AppTheme.colors.accentAmber)
    Spacer(Modifier.height(Dimens.spacing8))
    AppPanel {
      Text("Na CENA, toque em ILUSTRAÇÃO para compartilhar o prompt com o Gemini. Depois compartilhe a imagem gerada de volta para o BRASEIRO; ela só é vinculada se ainda for a mesma cena.", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
    }

    Spacer(Modifier.height(Dimens.spacing16))
    SectionHeader(title = "VOZ & LEITURA", color = AppTheme.colors.accentPurple)
    Spacer(Modifier.height(Dimens.spacing8))
    AppPanel(borderColor = AppTheme.colors.accentPurple.copy(alpha = .45f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.action_tts), "TTS", Modifier.size(52.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
          Text(if (ttsMode == "gemini") "GEMINI TTS" else "TTS DO ANDROID", style = AppTheme.typography.cardTitle)
          Text("OUVIR lê somente a narrativa visível do Mestre.", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
        }
      }
        Spacer(Modifier.height(10.dp))
        SecondaryButton("CONFIGURAÇÕES DE VOZ (TTS V3)", onClick = onOpenVoiceSettings, modifier = Modifier.fillMaxWidth())
    }

    Spacer(Modifier.height(Dimens.spacing16))
    SectionHeader(title = "SISTEMA & ATUALIZAÇÕES (M13)", color = AppTheme.colors.accentCyan)
    Spacer(Modifier.height(Dimens.spacing8))
    val updateScope = rememberCoroutineScope()
    val updateManager = remember { AppUpdateManager(currentVersionCode = BuildConfig.VERSION_CODE, currentVersionName = BuildConfig.VERSION_NAME) }
    var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var updateInstallError by remember { mutableStateOf<String?>(null) }

    AppPanel {
      Text(
        text = "Versão Instalada: ${BuildConfig.VERSION_NAME} • Build ${BuildConfig.VERSION_CODE}\nCanal oficial Braseiro",
        style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
      )
      Spacer(Modifier.height(8.dp))
      SecondaryButton(
        text = if (isCheckingUpdate) "VERIFICANDO SERVIDORES..." else "VERIFICAR ATUALIZAÇÕES",
        enabled = !isCheckingUpdate,
        onClick = {
          isCheckingUpdate = true
          updateScope.launch {
            val res = updateManager.checkForUpdates()
            updateResult = res
            isCheckingUpdate = false
          }
        },
        modifier = Modifier.fillMaxWidth()
      )

      updateInstallError?.let { Text(it, style = AppTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)) }
      updateResult?.let { res ->
        Spacer(Modifier.height(8.dp))
        when (res.status) {
          UpdateStatus.UP_TO_DATE -> {
            Text(
              text = "✓ Nenhuma versão mais nova disponível (instalada: ${BuildConfig.VERSION_NAME}).",
              style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentCyan)
            )
          }
          UpdateStatus.UPDATE_AVAILABLE -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "★ Nova versão disponível: ${res.remoteVersionName} (build ${res.remoteVersionCode})",
                style = AppTheme.typography.cardTitle.copy(color = AppTheme.colors.accentAmber)
              )
              if (!res.changelog.isNullOrBlank()) {
                Text(
                  text = "Novidades: ${res.changelog}",
                  style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
                )
              }
              if (!res.downloadUrl.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                PrimaryButton(
                  text = if (isDownloadingUpdate) "BAIXANDO E VERIFICANDO APK..." else "BAIXAR ATUALIZAÇÃO OFICIAL",
                  enabled = !isDownloadingUpdate,
                  onClick = {
                    isDownloadingUpdate = true
                    updateInstallError = null
                    updateScope.launch {
                      val verified = updateManager.downloadVerifiedApk(context, res)
                      isDownloadingUpdate = false
                      verified.onSuccess { apk ->
                        runCatching { context.startActivity(updateManager.createInstallIntent(context, apk)) }
                          .onFailure { updateInstallError = "Instalação não pôde ser aberta: ${it.message}" }
                      }.onFailure { updateInstallError = "Atualização rejeitada: ${it.message}" }
                    }
                  },
                  modifier = Modifier.fillMaxWidth()
                )
              }
            }
          }
          UpdateStatus.NETWORK_ERROR -> {
            Text(
              text = "Não foi possível verificar atualizações. Sem conexão de rede ou servidor inacessível.",
              style = AppTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
            )
          }
          UpdateStatus.INVALID_MANIFEST -> {
            Text(
              text = "Resposta do servidor de atualização inválida.",
              style = AppTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
            )
          }
        }
      }
    }

    Spacer(Modifier.height(Dimens.spacing24))
  }
}