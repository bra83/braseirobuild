package com.braseiro.talesvtt.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braseiro.talesvtt.game.AdventureFactory
import com.braseiro.talesvtt.life.AdventurePackage
import com.braseiro.talesvtt.life.AdventurePackageValidator
import com.braseiro.talesvtt.life.AdventureRepository
import com.braseiro.talesvtt.components.GuidedModeAction
import com.braseiro.talesvtt.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdventuresScreen(
  repository: AdventureRepository,
  currentProfile: String,
  isAdventureActive: Boolean = false,
  onResumeAdventure: () -> Unit = {},
  onConcludeAdventure: () -> Unit = {},
  onCreateAiAdventure: (suspend () -> Pair<AdventurePackage, Boolean>)? = null,
  onStartAdventure: (AdventurePackage) -> Unit,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var packages by remember { mutableStateOf(repository.listPackages()) }
  var selectedPackage by remember { mutableStateOf<AdventurePackage?>(null) }
  var importErrorMessage by remember { mutableStateOf<String?>(null) }
  var importSuccessMessage by remember { mutableStateOf<String?>(null) }
  var isConcludedBanner by remember { mutableStateOf(false) }
  var isGenerating by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  // Document Picker for SAF JSON imports
  val jsonPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val stream = context.contentResolver.openInputStream(uri)
        val content = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        val result = AdventurePackageValidator.parseAndValidate(content)
        when (result) {
          is AdventurePackageValidator.ValidationResult.Valid -> {
            val pkg = result.adventurePackage
            repository.savePackage(pkg)
            packages = repository.listPackages()
            selectedPackage = pkg
            importSuccessMessage = "Aventura '${pkg.title}' importada com sucesso!"
            importErrorMessage = null
          }
          is AdventurePackageValidator.ValidationResult.Invalid -> {
            importErrorMessage = "Erros de validação:\n" + result.errors.joinToString("\n• ", prefix = "• ")
            importSuccessMessage = null
          }
        }
      } catch (e: Exception) {
        importErrorMessage = "Falha ao abrir arquivo: ${e.message}"
        importSuccessMessage = null
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "AVENTURAS E MISTÉRIOS",
            style = AppTheme.typography.cardTitle.copy(fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Voltar",
              tint = AppTheme.colors.textPrimary
            )
          }
        },
        actions = { GuidedModeAction("Aventuras") },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = AppTheme.colors.surfacePrimary
        )
      )
    },
    containerColor = AppTheme.colors.backgroundPrimary
  ) { paddingValues ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 0. Active Adventure Controls Banner
      if (isAdventureActive && !isConcludedBanner) {
        Card(
          colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceSecondary),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "MISTÉRIO EM ANDAMENTO",
              style = AppTheme.typography.label.copy(color = AppTheme.colors.accentAmber, fontWeight = FontWeight.Bold)
            )
            Text(
              text = "Uma investigação está atualmente ativa. Você pode retomar a cena ou concluir o mistério para registrar o legado na linha do tempo.",
              style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textPrimary)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Button(
                onClick = onResumeAdventure,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accentCyan)
              ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Continuar")
              }
              OutlinedButton(
                onClick = {
                  onConcludeAdventure()
                  isConcludedBanner = true
                  importSuccessMessage = "Mistério encerrado com sucesso. Legado gravado!"
                },
                modifier = Modifier.weight(1f)
              ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Encerrar")
              }
            }
          }
        }
      }

      // 1. Action Buttons
      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = { jsonPickerLauncher.launch(arrayOf("application/json", "text/*")) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accentCyan)
          ) {
            Icon(imageVector = Icons.Default.FileOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Importar", maxLines = 1)
          }

          Button(
            onClick = {
              if (isGenerating) return@Button
              if (onCreateAiAdventure != null) {
                scope.launch {
                  isGenerating = true
                  importErrorMessage = null
                  importSuccessMessage = "Bárbara consultando as frequências e criando mistério inédito via IA..."
                  try {
                    val result = onCreateAiAdventure()
                    packages = repository.listPackages()
                    selectedPackage = result.first
                    importSuccessMessage = if (result.second) {
                      "Novo Mistério criado pela Bárbara (IA) com sucesso!"
                    } else {
                      "Mistério gerado localmente via contingência procedural (offline / sem chave IA)."
                    }
                  } catch (e: Exception) {
                    val proceduralPkg = AdventureFactory.createProceduralAdventurePackage(currentProfile)
                    repository.savePackage(proceduralPkg)
                    packages = repository.listPackages()
                    selectedPackage = proceduralPkg
                    importSuccessMessage = "Mistério procedural de contingência criado localmente (${e.message ?: "fallback"})."
                  } finally {
                    isGenerating = false
                  }
                }
              } else {
                val proceduralPkg = AdventureFactory.createProceduralAdventurePackage(currentProfile)
                repository.savePackage(proceduralPkg)
                packages = repository.listPackages()
                selectedPackage = proceduralPkg
                importSuccessMessage = "Mistério procedural gerado localmente (modo offline)."
              }
            },
            modifier = Modifier.weight(1.3f),
            enabled = !isGenerating,
            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accentAmber)
          ) {
            if (isGenerating) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Criando...", maxLines = 1)
            } else {
              Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Criar c/ Mestre", maxLines = 1)
            }
          }

          OutlinedButton(
            onClick = {
              val proceduralPkg = AdventureFactory.createProceduralAdventurePackage(currentProfile)
              repository.savePackage(proceduralPkg)
              packages = repository.listPackages()
              selectedPackage = proceduralPkg
              importSuccessMessage = "Mistério procedural offline gerado com sucesso!"
              importErrorMessage = null
            },
            modifier = Modifier.weight(1.1f),
            enabled = !isGenerating
          ) {
            Icon(imageVector = Icons.Default.Casino, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Offline", maxLines = 1)
          }
        }
      }

      // Feedback alerts
      if (importErrorMessage != null) {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = importErrorMessage ?: "",
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
            style = AppTheme.typography.bodySmall
          )
        }
      }

      if (importSuccessMessage != null) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E382B)),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = importSuccessMessage ?: "",
            color = Color(0xFF85E3B3),
            modifier = Modifier.padding(12.dp),
            style = AppTheme.typography.bodySmall
          )
        }
      }

      // Imported Adventures List
      Text(
        text = "AVENTURAS DISPONÍVEIS (${packages.size})",
        style = AppTheme.typography.label.copy(color = AppTheme.colors.accentCyan, fontWeight = FontWeight.Bold)
      )

      if (packages.isEmpty()) {
        Card(
          colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfacePrimary),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(
            text = "Nenhuma aventura importada ainda. Toque em 'Importar JSON' para carregar um módulo no formato braseiro.adventure/v1.",
            color = AppTheme.colors.textSecondary,
            modifier = Modifier.padding(16.dp),
            style = AppTheme.typography.body
          )
        }
      } else {
        packages.forEach { pkg ->
          val isSelected = selectedPackage?.id == pkg.id
          Card(
            colors = CardDefaults.cardColors(
              containerColor = if (isSelected) AppTheme.colors.surfaceSecondary else AppTheme.colors.surfacePrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedPackage = pkg }
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = pkg.title,
                  style = AppTheme.typography.cardTitle.copy(fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                )
                AssistChip(
                  onClick = {},
                  label = {
                    Text(
                      text = if (pkg.isFloodTeens()) "FLOOD / TEENS" else "TALES / KIDS",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                )
              }
              Text(
                text = pkg.description.ifEmpty { pkg.story.premise },
                style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary),
                maxLines = 2
              )
            }
          }
        }
      }

      // Public Preview Card (NO SPOILERS: GM_ONLY secrets and background remain completely hidden)
      selectedPackage?.let { pkg ->
        Card(
          colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfacePrimary),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "PRÉ-VISUALIZAÇÃO DA AVENTURA (SEM SPOILERS)",
              style = AppTheme.typography.label.copy(color = AppTheme.colors.accentCyan, fontWeight = FontWeight.Bold)
            )

            Text(
              text = pkg.title,
              style = AppTheme.typography.cardTitle.copy(fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
            )

            Text(
              text = "Premissa Pública:\n${pkg.story.premise}",
              style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary)
            )

            if (pkg.story.playerIntroduction.isNotBlank()) {
              Text(
                text = "Introdução aos Jogadores:\n${pkg.story.playerIntroduction}",
                style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
              )
            }

            Text(
              text = "Local Inicial: ${pkg.start.locationId} · Sistema: ${pkg.system} · Mundo: ${pkg.worldId}",
              style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary, fontSize = 11.sp)
            )

            Text(
              text = "Locais Mapeados: ${pkg.locations.size} · NPCs: ${pkg.npcs.size} · Objetivos: ${pkg.objectives.size}",
              style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentCyan, fontSize = 11.sp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              // Export Package JSON via Android Share
              OutlinedButton(
                onClick = {
                  val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, pkg.rawJson)
                    type = "application/json"
                  }
                  context.startActivity(Intent.createChooser(sendIntent, "Exportar Pacote de Aventura JSON"))
                },
                modifier = Modifier.weight(1f)
              ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Exportar JSON")
              }

              // Start Adventure Button
              Button(
                onClick = { onStartAdventure(pkg) },
                modifier = Modifier.weight(1.5f),
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accentCyan)
              ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Jogar Mistério")
              }
            }
          }
        }
      }
    }
  }
}