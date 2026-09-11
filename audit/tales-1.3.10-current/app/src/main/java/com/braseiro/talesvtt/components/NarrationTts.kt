package com.braseiro.talesvtt.components

import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.braseiro.talesvtt.audio.GeminiNarrationTts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

class NarrationTtsController(context: Context) : TextToSpeech.OnInitListener {
  private val appContext = context.applicationContext
  private val engine = TextToSpeech(appContext, this)
  private val gemini = GeminiNarrationTts(appContext)
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private var ready = false
  private var pendingAndroidText: String? = null

  override fun onInit(status: Int) {
    if (status != TextToSpeech.SUCCESS) {
      ready = false
      return
    }
    val result = engine.setLanguage(Locale("pt", "BR"))
    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
      val fallback = engine.setLanguage(Locale.getDefault())
      ready = fallback != TextToSpeech.LANG_MISSING_DATA && fallback != TextToSpeech.LANG_NOT_SUPPORTED
    } else {
      ready = true
    }
    if (ready) {
      pendingAndroidText?.let { queued ->
        pendingAndroidText = null
        speakAndroidNow(queued)
      }
    }
  }

  fun speakVisibleNarrative(paragraphs: List<String>) {
    val visible = paragraphs.joinToString("\n\n").trim()
    if (visible.isBlank()) return
    stop()
    if (gemini.enabled()) {
      scope.launch {
        val result = gemini.speak(visible)
        if (result.isFailure) {
          Toast.makeText(appContext, "Voz Gemini indisponível; usando TTS do Android.", Toast.LENGTH_SHORT).show()
          speakAndroidOrQueue(visible)
        }
      }
    } else {
      speakAndroidOrQueue(visible)
    }
  }

  private fun speakAndroidOrQueue(text: String) {
    if (!ready) {
      pendingAndroidText = text
      return
    }
    speakAndroidNow(text)
  }

  private fun speakAndroidNow(text: String) {
    val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tftl-visible-narrative")
    if (result == TextToSpeech.ERROR) {
      Toast.makeText(appContext, "O TTS do Android não conseguiu iniciar a leitura.", Toast.LENGTH_LONG).show()
    }
  }

  fun stop() {
    pendingAndroidText = null
    gemini.stop()
    if (ready) engine.stop()
  }

  fun shutdown() {
    stop()
    engine.shutdown()
    scope.cancel()
    ready = false
  }
}

@Composable
fun rememberNarrationTtsController(): NarrationTtsController {
  val context = LocalContext.current
  val controller = remember(context) { NarrationTtsController(context) }
  DisposableEffect(controller) { onDispose { controller.shutdown() } }
  return controller
}
