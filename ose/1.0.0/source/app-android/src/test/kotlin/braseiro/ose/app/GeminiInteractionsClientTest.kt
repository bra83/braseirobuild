package braseiro.ose.app

import java.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GeminiInteractionsClientTest {
    private class FakeTransport(private val response: JSONObject) : GeminiTransport {
        var keySeen: String? = null
        var bodySeen: JSONObject? = null
        override fun post(apiKey: String, body: JSONObject): JSONObject {
            keySeen = apiKey
            bodySeen = body
            return response
        }
    }

    private fun response(type: String, mime: String, bytes: ByteArray, sampleRate: Int? = null): JSONObject {
        val media = JSONObject()
            .put("type", type)
            .put("mime_type", mime)
            .put("data", Base64.getEncoder().encodeToString(bytes))
        if (sampleRate != null) media.put("sample_rate", sampleRate)
        val output = JSONObject()
            .put("type", "model_output")
            .put("content", JSONArray().put(media))
        return JSONObject().put("steps", JSONArray().put(output))
    }

    @Test fun sceneImageUsesCurrentInteractionsImageContract() {
        val transport = FakeTransport(response("image", "image/jpeg", byteArrayOf(1, 2, 3)))
        val client = GeminiInteractionsClient(transport)
        val blob = client.generateSceneImage("secret-do-not-echo", "custom-image-model", "visible scene only")
        assertEquals("image/jpeg", blob.mimeType)
        assertArrayEquals(byteArrayOf(1, 2, 3), blob.data)
        val body = transport.bodySeen!!
        assertEquals("custom-image-model", body.getString("model"))
        assertEquals("visible scene only", body.getString("input"))
        assertEquals("image", body.getJSONObject("response_format").getString("type"))
        assertFalse(body.toString().contains("secret-do-not-echo"))
        assertEquals("secret-do-not-echo", transport.keySeen)
    }

    @Test fun ttsUsesOnlySuppliedVisibleNarrationAndAudioFormat() {
        val transport = FakeTransport(response("audio", "audio/L16", byteArrayOf(4, 5), 24000))
        val client = GeminiInteractionsClient(transport)
        val blob = client.generateTtsAudio("tts-secret", "custom-tts-model", "Kore", "Narração visível")
        assertArrayEquals(byteArrayOf(4, 5), blob.data)
        assertEquals(24000, blob.sampleRate)
        val body = transport.bodySeen!!
        assertEquals("custom-tts-model", body.getString("model"))
        val input = body.getJSONArray("input").getJSONObject(0)
            .getJSONArray("content").getJSONObject(0)
        assertEquals("Narração visível", input.getString("text"))
        assertEquals("speech_metadata", input.getJSONArray("annotations")
            .getJSONObject(0).getString("type"))
        assertFalse(input.getString("text").contains("Narre em"))
        assertEquals("audio", body.getJSONObject("response_format").getString("type"))
        assertFalse(body.toString().contains("tts-secret"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankScenePromptIsRejectedBeforeTransport() {
        GeminiInteractionsClient(FakeTransport(JSONObject())).generateSceneImage("key", "model", "  ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankNarrationIsRejectedBeforeTransport() {
        GeminiInteractionsClient(FakeTransport(JSONObject())).generateTtsAudio("key", "model", "Kore", "")
    }
    @Test fun ttsParsesCurrentOutputAudioField() {
        val bytes = byteArrayOf(9, 8, 7, 6)
        val response = JSONObject().put("output_audio", JSONObject()
            .put("data", Base64.getEncoder().encodeToString(bytes))
            .put("mime_type", "audio/L16")
            .put("sample_rate", 24000))
        val client = GeminiInteractionsClient(FakeTransport(response))
        val blob = client.generateTtsAudio("tts-secret", "gemini-3.1-flash-tts-preview", "Kore", "Teste em português")
        assertArrayEquals(bytes, blob.data)
        assertEquals("audio/L16", blob.mimeType)
        assertEquals(24000, blob.sampleRate)
    }

}
