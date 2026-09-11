package com.braseiro.talesvtt.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import com.braseiro.talesvtt.BuildConfig
import com.braseiro.talesvtt.data.SecureSecretStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

class GeminiNarrationTts(private val context: Context) {
  private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(90, TimeUnit.SECONDS)
    .build()
  private val prefs = context.getSharedPreferences("braseiro_motor_cfg_v1", Context.MODE_PRIVATE)
  private val secrets = SecureSecretStore(context)
  private var player: MediaPlayer? = null

  fun enabled(): Boolean = prefs.getString("tts_mode", "gemini") == "gemini"

  suspend fun speak(text: String): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      val apiKey = resolveApiKey()
      require(apiKey.isNotBlank()) { "Chave Gemini não configurada para voz." }
      val configured = prefs.getString("tts_model", DEFAULT_TTS_MODEL).orEmpty().trim()
      val voice = sanitizeVoice(prefs.getString("tts_voice", DEFAULT_VOICE).orEmpty())
      val models = linkedSetOf<String>().apply {
        if (configured.contains("tts", ignoreCase = true)) add(configured)
        add(DEFAULT_TTS_MODEL)
        add(LEGACY_TTS_MODEL)
      }

      var lastFailure: Throwable? = null
      for (model in models) {
        try {
          val generated = generateAudio(apiKey, model, voice, text)
          playGenerated(generated)
          return@runCatching Unit
        } catch (t: Throwable) {
          lastFailure = t
        }
      }
      throw IllegalStateException(lastFailure?.message ?: "Gemini TTS indisponível.")
    }
  }

  private fun generateAudio(apiKey: String, model: String, voice: String, text: String): GeneratedAudio {
    val body = JSONObject()
      .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put(
        "text",
        "Narre em português do Brasil, com voz natural de Mestre de RPG, sem adicionar nenhuma palavra ao texto:\n$text"
      )))))
      .put("generationConfig", JSONObject()
        .put("responseModalities", JSONArray().put("AUDIO"))
        .put("speechConfig", JSONObject()
          .put("languageCode", "pt-BR")
          .put("voiceConfig", JSONObject()
            .put("prebuiltVoiceConfig", JSONObject().put("voiceName", voice))))
      )
    val request = Request.Builder()
      .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
      .header("Content-Type", "application/json; charset=utf-8")
      .header("x-goog-api-key", apiKey)
      .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
      .build()

    client.newCall(request).execute().use { response ->
      val raw = response.body?.string().orEmpty()
      if (!response.isSuccessful) error(userSafeHttpError(response.code))
      val root = JSONObject(raw)
      val candidates = root.optJSONArray("candidates")
      require(candidates != null && candidates.length() > 0) { "Gemini TTS respondeu sem áudio." }
      val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
        ?: error("Gemini TTS respondeu sem áudio.")
      for (i in 0 until parts.length()) {
        val inline = parts.optJSONObject(i)?.optJSONObject("inlineData") ?: continue
        val data = inline.optString("data")
        if (data.isBlank()) continue
        return GeneratedAudio(Base64.decode(data, Base64.DEFAULT), inline.optString("mimeType"))
      }
      error("Gemini TTS não retornou áudio utilizável.")
    }
  }

  private suspend fun playGenerated(generated: GeneratedAudio) {
    val mime = generated.mimeType.lowercase()
    val isPcm = mime.contains("l16") || mime.contains("pcm") || mime.isBlank()
    val extension = when {
      isPcm || mime.contains("wav") -> ".wav"
      mime.contains("mpeg") || mime.contains("mp3") -> ".mp3"
      mime.contains("aac") -> ".aac"
      mime.contains("ogg") -> ".ogg"
      else -> ".audio"
    }
    val bytes = if (isPcm) pcmToWav(generated.bytes, rateFromMime(mime), 1, 16) else generated.bytes
    val file = File.createTempFile("tales_tts_", extension, context.cacheDir)
    file.writeBytes(bytes)
    withContext(Dispatchers.Main) {
      player?.release()
      player = MediaPlayer().apply {
        setDataSource(file.absolutePath)
        setOnCompletionListener { finished ->
          finished.release()
          if (player === finished) player = null
          file.delete()
        }
        setOnErrorListener { failed, _, _ ->
          failed.release()
          if (player === failed) player = null
          file.delete()
          true
        }
        prepare()
        start()
      }
    }
  }

  fun stop() {
    runCatching { player?.stop() }
    player?.release()
    player = null
  }

  private fun resolveApiKey(): String {
    val local = secrets.get("gemini_api_key").trim()
    if (local.isNotEmpty()) return local
    return runCatching { BuildConfig::class.java.getField("GEMINI_API_KEY").get(null)?.toString().orEmpty().trim() }.getOrDefault("")
  }

  private fun sanitizeVoice(raw: String): String {
    val candidate = raw.trim()
    return if (candidate in SUPPORTED_VOICES) candidate else DEFAULT_VOICE
  }

  private fun userSafeHttpError(code: Int): String = when (code) {
    400 -> "Configuração de voz recusada pelo Gemini (HTTP 400)."
    401, 403 -> "A chave Gemini não autorizou a geração de voz (HTTP $code)."
    404 -> "Modelo de voz Gemini indisponível (HTTP 404)."
    429 -> "Limite temporário da voz Gemini atingido (HTTP 429)."
    500, 502, 503, 504 -> "Serviço de voz Gemini temporariamente indisponível (HTTP $code)."
    else -> "Falha na voz Gemini (HTTP $code)."
  }

  private fun rateFromMime(mime: String): Int = Regex("rate=(\\d+)", RegexOption.IGNORE_CASE)
    .find(mime)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 24000

  private fun pcmToWav(pcm: ByteArray, rate: Int, channels: Int, bits: Int): ByteArray {
    val out = ByteBuffer.allocate(44 + pcm.size).order(ByteOrder.LITTLE_ENDIAN)
    out.put("RIFF".toByteArray(Charsets.US_ASCII)); out.putInt(36 + pcm.size); out.put("WAVE".toByteArray(Charsets.US_ASCII))
    out.put("fmt ".toByteArray(Charsets.US_ASCII)); out.putInt(16); out.putShort(1); out.putShort(channels.toShort()); out.putInt(rate)
    out.putInt(rate * channels * bits / 8); out.putShort((channels * bits / 8).toShort()); out.putShort(bits.toShort())
    out.put("data".toByteArray(Charsets.US_ASCII)); out.putInt(pcm.size); out.put(pcm)
    return out.array()
  }

  private data class GeneratedAudio(val bytes: ByteArray, val mimeType: String)

  companion object {
    const val DEFAULT_TTS_MODEL = "gemini-3.1-flash-tts-preview"
    const val DEFAULT_VOICE = "Kore"
    private const val LEGACY_TTS_MODEL = "gemini-2.5-flash-preview-tts"
    private val SUPPORTED_VOICES = setOf(
      "Zephyr", "Puck", "Charon", "Kore", "Fenrir", "Leda", "Orus", "Aoede", "Callirrhoe", "Autonoe",
      "Enceladus", "Iapetus", "Umbriel", "Algieba", "Despina", "Erinome", "Algenib", "Rasalgethi",
      "Laomedeia", "Achernar", "Alnilam", "Schedar", "Gacrux", "Pulcherrima", "Achird", "Zubenelgenubi",
      "Vindemiatrix", "Sadachbia", "Sadaltager", "Sulafat"
    )
  }
}
