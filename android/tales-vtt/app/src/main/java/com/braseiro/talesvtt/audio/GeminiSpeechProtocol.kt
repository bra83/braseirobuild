package com.braseiro.talesvtt.audio

import org.json.JSONArray
import org.json.JSONObject

/** Wire contract for the Gemini 3.8 Interactions API, independent of Android playback. */
object GeminiSpeechProtocol {
  fun request(model: String, voice: String, transcript: String, profile: String, style: String): JSONObject {
    require(model == "gemini-3.8-flash-lite-tts" || model == "gemini-3.8-flash-tts")
    require(transcript.isNotBlank())
    val text = JSONObject().put("type", "text").put("text", transcript)
    val instruction = listOf(profile.trim(), style.trim()).filter { it.isNotEmpty() }.joinToString("; ").take(900)
    if (instruction.isNotBlank()) text.put("annotations", JSONArray().put(
      JSONObject().put("type", "speech_metadata").put("style", instruction)))
    return JSONObject()
      .put("model", model)
      .put("input", JSONArray().put(JSONObject().put("type", "user_input")
        .put("content", JSONArray().put(text))))
      .put("response_format", JSONObject().put("type", "audio").put("mime_type", "audio/wav"))
      .put("generation_config", JSONObject().put("speech_config", JSONArray().put(JSONObject().put("voice", voice))))
  }

  fun response(root: JSONObject): Pair<String, String> {
    val steps = root.optJSONArray("steps") ?: error("Gemini TTS não retornou áudio.")
    for (i in steps.length() - 1 downTo 0) {
      val step = steps.optJSONObject(i) ?: continue
      if (step.optString("type") != "model_output") continue
      val parts = step.optJSONArray("content") ?: continue
      for (j in parts.length() - 1 downTo 0) {
        val audio = parts.optJSONObject(j) ?: continue
        if (audio.optString("type") == "audio" && audio.optString("data").isNotBlank()) {
          return audio.optString("data") to audio.optString("mime_type", "audio/wav")
        }
      }
    }
    error("Gemini TTS retornou resposta sem áudio utilizável.")
  }
}