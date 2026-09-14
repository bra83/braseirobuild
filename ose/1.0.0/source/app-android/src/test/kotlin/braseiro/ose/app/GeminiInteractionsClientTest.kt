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

    private fun response(type: String, mime: String, bytes: ByteArray, sampleRate: Int? = null) = JSONObject()
        .put("steps", JSONArray().put(JSONObject()
            .put("type", "model_output")
            .put("content", JSONArray().put(JSONObject()
                .put("type", type)
                .put("mime_type", mime)
                .put("data", Base64.getEncoder().encodeToString(bytes))
                .apply { if (sampleRate != null) put("sample_rate", sampleRate) })))

    @Test fun sceneImageUsesCurrentInteractionsImageContract() {
        val transport = FakeTransport(response("image", "image/jpeg", byteArrayOf(1, 2, 3)))
        val client = GeminiInteractionsClient(transport)
        val blob = client.generateSceneImage("secret-do-not-echo", "visible scene only")
        assertEquals("image/jpeg", blob.mimeType)
        assertArrayEquals(byteArrayOf(1, 2, 3), blob.data)
        val body = transport.bodySeen!!
        assertEquals("gemini-3.1-flash-image", body.getString("model"))
        assertEquals("visible scene only", body.getString("input"))
        assertEquals("image", body.getJSONObject("response_format").getString("type"))
        assertFalse(body.toString().contains("secret-do-not-echo"))
        assertEquals("secret-do-not-echo", transport.keySeen)
    }

    @Test fun ttsUsesOnlySuppliedVisibleNarrationAndAudioFormat() {
        val transport = FakeTransport(response("audio", "audio/L16", byteArrayOf(4, 5), 24000))
        val client = GeminiInteractionsClient(transport)
        val blob = client.generateTtsAudio("tts-secret", "Narração visível")
        assertArrayEquals(byteArrayOf(4, 5), blob.data)
        assertEquals(24000, blob.sampleRate)
        val body = transport.bodySeen!!
        assertEquals("gemini-3.1-flash-tts-preview", body.getString("model"))
        assertTrue(body.getString("input").endsWith("Narração visível"))
        assertEquals("audio", body.getJSONObject("response_format").getString("type"))
        assertFalse(body.toString().contains("tts-secret"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankScenePromptIsRejectedBeforeTransport() {
        GeminiInteractionsClient(FakeTransport(JSONObject())).generateSceneImage("key", "  ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankNarrationIsRejectedBeforeTransport() {
        GeminiInteractionsClient(FakeTransport(JSONObject())).generateTtsAudio("key", "")
    }
}
