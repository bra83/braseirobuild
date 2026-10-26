package com.braseiro.talesvtt.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braseiro.talesvtt.audio.TtsEngineType
import com.braseiro.talesvtt.audio.TtsSettings
import com.braseiro.talesvtt.audio.TtsVoiceOption
import com.braseiro.talesvtt.audio.UniversalTtsV3Engine
import com.braseiro.talesvtt.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
  ttsEngine: UniversalTtsV3Engine,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var settings by remember { mutableStateOf(ttsEngine.getSettings()) }
  val availableVoices by remember { mutableStateOf(ttsEngine.getAvailableVoices()) }
  var isSpeakingState by remember { mutableStateOf(false) }

  DisposableEffect(ttsEngine) {
    ttsEngine.onStateChangedListener = { speaking, _ ->
      isSpeakingState = speaking
    }
    onDispose {
      ttsEngine.onStateChangedListener = null
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "CONFIGURAÇÕES DE VOZ (TTS V3)",
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
      // 1. Engine Selection
      Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfacePrimary),
        shape = RoundedCornerShape(12.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "MOTOR DE ÁUDIO / TTS",
            style = AppTheme.typography.label.copy(color = AppTheme.colors.accentCyan, fontWeight = FontWeight.Bold)
          )
          TtsEngineType.values().forEach { engine ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                  val updated = settings.copy(engineType = engine)
                  settings = updated
                  ttsEngine.saveSettings(updated)
                }
                .padding(vertical = 8.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = settings.engineType == engine,
                onClick = {
                  val updated = settings.copy(engineType = engine)
                  settings = updated
                  ttsEngine.saveSettings(updated)
                }
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = engine.label,
                  style = AppTheme.typography.body.copy(
                    fontWeight = if (settings.engineType == engine) FontWeight.Bold else FontWeight.Normal,
                    color = AppTheme.colors.textPrimary
                  )
                )
                if (engine == TtsEngineType.KOKORO_SHERPA) {
                  Text(
                    text = "Requer aplicativo motor neural Sherpa ONNX no dispositivo.",
                    style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary, fontSize = 11.sp)
                  )
                }
              }
            }
          }
        }
      }

      // 2. TTS Synthesis Model Selection (Gemini Speech)
      Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfacePrimary),
        shape = RoundedCornerShape(12.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "MODELO DE SÍNTESE VOCAL (GEMINI SPEECH)",
            style = AppTheme.typography.label.copy(color = AppTheme.colors.accentCyan, fontWeight = FontWeight.Bold)
          )
          Text(
            text = "Modelo utilizado exclusivamente para áudio/voz. Não sobrescreve o modelo de texto da Bárbara.",
            style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary, fontSize = 11.sp)
          )
          val models = listOf(
            "gemini-3.8-flash-lite-tts", "gemini-3.8-flash-tts", "gemini-3.1-flash-tts-preview", "gemini-2.5-pro-preview-tts"
          )
          models.forEach { model ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                  val updated = settings.copy(selectedModel = model)
                  settings = updated
                  ttsEngine.saveSettings(updated)
                }
                .padding(vertical = 6.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = settings.selectedModel == model,
                onClick = {
                  val updated = settings.copy(selectedModel = model)
                  settings = updated
                  ttsEngine.saveSettings(updated)
                }
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = model,
                style = AppTheme.typography.body.copy(
                  fontWeight = if (settings.selectedModel == model) FontWeight.Bold else FontWeight.Normal,
                  color = AppTheme.colors.textPrimary
                )
              )
            }
          }
        }
      }

      // 2.1 Vocal Profile & Style
      Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfacePrimary),
        shape = RoundedCornerShape(12.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "PERFIL E ESTILO VOCAL",
            style = AppTheme.typography.label.copy(color = AppTheme.colors.accentCyan, fontWeight = FontWeight.Bold)
          )
          Text(
            text = "Interpretação e modulação de fala do Mestre.",
            style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary, fontSize = 11.sp)
          )
          OutlinedTextField(
            value = settings.voiceProfile,
            onValueChange = { value ->
              val updated = settings.copy(voiceProfile = value.take(500))
              settings = updated
              ttsEngine.saveSettings(updated)
            },
            label = { Text("Perfil vocal — identidade e características da voz") },
            placeholder = { Text("Ex.: narrador brasileiro, voz grave, dicção clara, sotaque neutro") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
          )
          OutlinedTextField(
            value = settings.voiceStyle,
            onValueChange = { value ->
              val updated = settings.copy(voiceStyle = value.take(500))
              settings = updated
              ttsEngine.saveSettings(updated)
            },
            label = { Text("Estilo vocal — interpretação da fala") },
            placeholder = { Text("Ex.: acolhedor, ritmo natural, suspense crescente sem pausas longas") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
          )
          Text(
            "Gemini 3.8 utiliza speech_metadata.style para a interpretação. Essas instruções não fazem parte do texto narrado.",
            style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
          )
        }
      }

      // 3. Voice Selection
      val voiceOptions = if (settings.engineType == TtsEngineType.GEMINI_SPEECH) {
        com.braseiro.talesvtt.audio.GeminiNarrationTts.SUPPORTED_VOICES.map { voice ->
          TtsVoiceOption(voice, voice, "gemini", "pt-BR", true)
        }
      } else availableVoices
      if (voiceOptions.isNotEmpty()) {
        Card(
          colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfacePrimary),
          shape = RoundedCornerShape(12.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "VOZES DETECTADAS (PORTUGUÊS)",
              style = AppTheme.typography.label.copy(color = AppTheme.colors.accentCyan, fontWeight = FontWeight.Bold)
            )
            voiceOptions.forEach { voice ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .clickable {
                    val updated = settings.copy(selectedVoiceId = voice.id)
                    settings = updated
                    ttsEngine.saveSettings(updated)
                  }
                  .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                RadioButton(
                  selected = settings.selectedVoiceId == voice.id || (settings.selectedVoiceId.isEmpty() && voice == voiceOptions.first()),
                  onClick = {
                    val updated = settings.copy(selectedVoiceId = voice.id)
                    settings = updated
                    ttsEngine.saveSettings(updated)
                  }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = voice.name,
                  style = AppTheme.typography.body.copy(
                    fontWeight = if (settings.selectedVoiceId == voice.id) FontWeight.Bold else FontWeight.Normal,
                    color = AppTheme.colors.textPrimary
                  )
                )
              }
            }
          }
        }
      }

      // 4. Rate & Pitch Sliders
      Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfacePrimary),
        shape = RoundedCornerShape(12.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "MODULAÇÃO VOCAL",
            style = AppTheme.typography.label.copy(color = AppTheme.colors.accentCyan, fontWeight = FontWeight.Bold)
          )

          Text(
            text = "Velocidade da Fala: ${String.format("%.2f", settings.speechRate)}x",
            style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textPrimary)
          )
          Slider(
            value = settings.speechRate,
            onValueChange = {
              val updated = settings.copy(speechRate = it)
              settings = updated
              ttsEngine.saveSettings(updated)
            },
            valueRange = 0.7f..1.5f,
            steps = 7
          )

          Text(
            text = "Tom Vocal (Pitch): ${String.format("%.2f", settings.pitch)}x",
            style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textPrimary)
          )
          Slider(
            value = settings.pitch,
            onValueChange = {
              val updated = settings.copy(pitch = it)
              settings = updated
              ttsEngine.saveSettings(updated)
            },
            valueRange = 0.7f..1.3f,
            steps = 5
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Narrar Cenas Automaticamente",
              style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary)
            )
            Switch(
              checked = settings.autoPlayOnSceneLoad,
              onCheckedChange = {
                val updated = settings.copy(autoPlayOnSceneLoad = it)
                settings = updated
                ttsEngine.saveSettings(updated)
              }
            )
          }
        }
      }

      // 5. Test & Controls
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Button(
          onClick = {
            ttsEngine.speakNarration("A tarde cai fria sobre as árvores úmidas perto das torres do Loop. O ar tem cheiro de ozônio e ferrugem, e as lâmpadas da rua começam a piscar com um zumbido estranho.")
          },
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accentCyan)
        ) {
          Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Testar Voz")
        }

        if (isSpeakingState) {
          Button(
            onClick = { ttsEngine.stop() },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
          ) {
            Icon(imageVector = Icons.Default.Stop, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Interromper")
          }
        }
      }
    }
  }
}