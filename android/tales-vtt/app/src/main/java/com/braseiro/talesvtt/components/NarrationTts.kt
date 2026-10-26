package com.braseiro.talesvtt.components

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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

internal object NarrationTextChunker {
  fun split(text: String, limit: Int = 3000): List<String> {
    val clean = text.trim()
    if (clean.isBlank()) return emptyList()
    if (clean.length <= limit) return listOf(clean)

    val sentences = clean.split(Regex("(?<=[.!?…])\\s+"))
    val out = mutableListOf<String>()
    val current = StringBuilder()

    fun flush() {
      if (current.isNotBlank()) {
        out += current.toString().trim()
        current.clear()
      }
    }

    sentences.forEach { sentence ->
      val part = sentence.trim()
      if (part.isBlank()) return@forEach
      if (part.length > limit) {
        flush()
        var remaining = part
        while (remaining.length > limit) {
          var cut = remaining.lastIndexOf(' ', limit)
          if (cut < limit / 2) cut = limit
          out += remaining.substring(0, cut).trim()
          remaining = remaining.substring(cut).trim()
        }
        if (remaining.isNotBlank()) current.append(remaining)
      } else if (current.isEmpty()) {
        current.append(part)
      } else if (current.length + 1 + part.length <= limit) {
        current.append(' ').append(part)
      } else {
        flush()
        current.append(part)
      }
    }
    flush()
    return out
  }
}

class NarrationTtsController(context: Context) {
  private val appContext = context.applicationContext
  private val v3Engine = com.braseiro.talesvtt.audio.UniversalTtsV3Engine(appContext)

  fun speakVisibleNarrative(paragraphs: List<String>) {
    val visible = paragraphs.joinToString("\n\n").trim()
    if (visible.isBlank()) return
    v3Engine.speakNarration(visible)
  }

  fun stop() {
    v3Engine.stop()
  }

  fun shutdown() {
    v3Engine.shutdown()
  }
}

@Composable
fun rememberNarrationTtsController(): NarrationTtsController {
  val context = LocalContext.current
  val controller = remember(context) { NarrationTtsController(context) }
  DisposableEffect(controller) { onDispose { controller.shutdown() } }
  return controller
}
