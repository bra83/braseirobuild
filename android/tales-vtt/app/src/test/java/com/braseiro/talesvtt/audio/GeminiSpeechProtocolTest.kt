package com.braseiro.talesvtt.audio

import org.junit.Test
import org.junit.Assert.*
import org.json.JSONArray
import org.json.JSONObject

class GeminiSpeechProtocolTest {
  @Test fun gemini38LiteSendsVerbatimTranscriptAndMetadata() {
    val actual = GeminiSpeechProtocol.request("gemini-3.8-flash-lite-tts", "Kore", "Não leia instruções.", "PT-BR neutro", "Misterioso")
    assertEquals("gemini-3.8-flash-lite-tts", actual.getString("model"))
    val text = actual.getJSONArray("input").getJSONObject(0).getJSONArray("content").getJSONObject(0)
    assertEquals("Não leia instruções.", text.getString("text"))
    assertEquals("PT-BR neutro; Misterioso", text.getJSONArray("annotations").getJSONObject(0).getString("style"))
    assertEquals("Kore", actual.getJSONObject("generation_config").getJSONArray("speech_config").getJSONObject(0).getString("voice"))
    assertEquals("audio", actual.getJSONObject("response_format").getString("type"))
  }
  @Test fun flashTtsSupportsEmptyStyleWithoutSpeakingInstructions() {
    val actual = GeminiSpeechProtocol.request("gemini-3.8-flash-tts", "Puck", "Texto exato", "", "")
    val text = actual.getJSONArray("input").getJSONObject(0).getJSONArray("content").getJSONObject(0)
    assertFalse(text.has("annotations"))
    assertEquals("Texto exato", text.getString("text"))
  }
  @Test fun audioIsReadFromInteractionsSteps() {
    val reply = JSONObject().put("steps", JSONArray().put(JSONObject()
      .put("type", "model_output")
      .put("content", JSONArray().put(JSONObject().put("type", "audio").put("data", "AQID").put("mime_type", "audio/wav")))))
    assertEquals("AQID" to "audio/wav", GeminiSpeechProtocol.response(reply))
  }
  @Test(expected = IllegalArgumentException::class)
  fun textModelRejectedInTtsRequest() {
    GeminiSpeechProtocol.request("gemini-2.5-flash", "Kore", "texto", "", "")
  }
}