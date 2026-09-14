package braseiro.ose.app

import java.net.URL
import javax.net.ssl.HttpsURLConnection
import org.json.JSONArray
import org.json.JSONObject

/** Interactions API only. API keys are supplied per call and are never retained by this client. */
class GeminiInteractionsClient(private val transport: GeminiTransport = HttpsGeminiTransport()) {
    data class MediaBlob(val mimeType: String, val data: ByteArray, val sampleRate: Int? = null)

    fun testCredential(apiKey: String): Boolean {
        val body = JSONObject()
            .put("model", "gemini-3.1-flash-lite")
            .put("input", "Responda somente OK.")
        val response = transport.post(apiKey, body)
        return textBlocks(response).joinToString(" ").isNotBlank()
    }

    fun generateSceneImage(apiKey: String, prompt: String): MediaBlob {
        require(prompt.isNotBlank()) { "EMPTY_SCENE_PROMPT" }
        val body = JSONObject()
            .put("model", "gemini-3.1-flash-image")
            .put("input", prompt)
            .put("response_format", JSONObject()
                .put("type", "image")
                .put("aspect_ratio", "4:3")
                .put("image_size", "1K")
                .put("mime_type", "image/jpeg"))
        return firstMedia(transport.post(apiKey, body), "image")
    }

    fun generateTtsAudio(apiKey: String, visibleNarration: String): MediaBlob {
        require(visibleNarration.isNotBlank()) { "EMPTY_VISIBLE_NARRATION" }
        val body = JSONObject()
            .put("model", "gemini-3.1-flash-tts-preview")
            .put("input", "Narre em português do Brasil, com voz natural de mestre de RPG e sem acrescentar palavras: $visibleNarration")
            .put("response_format", JSONObject().put("type", "audio").put("sample_rate", 24000))
            .put("generation_config", JSONObject().put("speech_config", JSONArray().put(JSONObject().put("voice", "Kore"))))
        return firstMedia(transport.post(apiKey, body), "audio")
    }

    private fun textBlocks(response: JSONObject): List<String> {
        val out = mutableListOf<String>()
        forEachContent(response) { content ->
            if (content.optString("type") == "text") content.optString("text").takeIf { it.isNotBlank() }?.let(out::add)
        }
        return out
    }

    private fun firstMedia(response: JSONObject, type: String): MediaBlob {
        var found: MediaBlob? = null
        forEachContent(response) { content ->
            if (found == null && content.optString("type") == type) {
                val data = content.optString("data", "")
                if (data.isNotBlank()) {
                    found = MediaBlob(
                        mimeType = content.optString("mime_type", if (type == "image") "image/jpeg" else "audio/L16"),
                        data = java.util.Base64.getDecoder().decode(data),
                        sampleRate = content.optInt("sample_rate", 0).takeIf { it > 0 }
                    )
                }
            }
        }
        return found ?: error("GEMINI_${type.uppercase()}_OUTPUT_MISSING")
    }

    private inline fun forEachContent(response: JSONObject, block: (JSONObject) -> Unit) {
        val steps = response.optJSONArray("steps") ?: JSONArray()
        for (i in 0 until steps.length()) {
            val step = steps.optJSONObject(i) ?: continue
            if (step.optString("type") != "model_output") continue
            val content = step.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) content.optJSONObject(j)?.let(block)
        }
    }
}

interface GeminiTransport {
    fun post(apiKey: String, body: JSONObject): JSONObject
}

class HttpsGeminiTransport : GeminiTransport {
    override fun post(apiKey: String, body: JSONObject): JSONObject {
        require(apiKey.isNotBlank()) { "GEMINI_KEY_MISSING" }
        val connection = URL("https://generativelanguage.googleapis.com/v1beta/interactions")
            .openConnection() as HttpsURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 90_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-goog-api-key", apiKey)
            connection.setRequestProperty("Api-Revision", "2026-05-20")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("GEMINI_HTTP_$code")
            return JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }
}
