package braseiro.ose.app

import java.net.URL
import javax.net.ssl.HttpsURLConnection
import org.json.JSONArray
import org.json.JSONObject

/** Interactions API only. API keys are supplied per call and are never retained by this client. */
class GeminiInteractionsClient(private val transport: GeminiTransport = HttpsGeminiTransport()) {
    companion object {
        const val DEFAULT_TEXT_MODEL = "gemini-3.1-flash-lite"
        const val DEFAULT_IMAGE_MODEL = "gemini-3.1-flash-image"
        const val DEFAULT_TTS_MODEL = "gemini-3.1-flash-tts-preview"
        const val DEFAULT_TTS_VOICE = "Kore"
    }

    data class MediaBlob(val mimeType: String, val data: ByteArray, val sampleRate: Int? = null)

    fun testCredential(apiKey: String, model: String = DEFAULT_TEXT_MODEL): Boolean {
        val body = JSONObject()
            .put("model", validateModel(model))
            .put("input", "Responda somente OK.")
        val response = transport.post(apiKey, body)
        return textBlocks(response).joinToString(" ").isNotBlank()
    }

    fun testTtsCredential(apiKey: String, model: String, voice: String): Boolean =
        runCatching { generateTtsAudio(apiKey, model, voice, "Teste.").data.isNotEmpty() }.getOrDefault(false)

    fun generateText(apiKey: String, model: String, prompt: String): String {
        require(prompt.isNotBlank()) { "EMPTY_GEMINI_PROMPT" }
        val body = JSONObject()
            .put("model", validateModel(model))
            .put("input", prompt)
        val response = transport.post(apiKey, body)
        return textBlocks(response).joinToString("\n\n").trim().ifBlank { error("GEMINI_TEXT_OUTPUT_MISSING") }
    }

    fun generateSceneImage(apiKey: String, model: String, prompt: String, aspectRatio: String = "4:3"): MediaBlob {
        require(prompt.isNotBlank()) { "EMPTY_SCENE_PROMPT" }
        val body = JSONObject()
            .put("model", validateModel(model))
            .put("input", prompt)
            .put("response_format", JSONObject()
                .put("type", "image")
                .put("aspect_ratio", aspectRatio.takeIf { it in setOf("1:1", "4:3", "16:9") } ?: "4:3")
                .put("image_size", "1K")
                .put("mime_type", "image/jpeg"))
        return firstMedia(transport.post(apiKey, body), "image")
    }

    fun generateTtsAudio(apiKey: String, model: String, voice: String, visibleNarration: String): MediaBlob {
        require(visibleNarration.isNotBlank()) { "EMPTY_VISIBLE_NARRATION" }
        val safeVoice = voice.trim().also {
            require(it.isNotBlank() && it.length <= 80 && it.all { c -> c.isLetterOrDigit() || c in "._-" }) { "INVALID_TTS_VOICE" }
        }
        val body = JSONObject()
            .put("model", validateModel(model))
            .put("input", "Narre em português do Brasil, com voz natural de mestre de RPG, ritmo expressivo e sem acrescentar nem remover informação: $visibleNarration")
            .put("response_format", JSONObject().put("type", "audio").put("sample_rate", 24000))
            .put("generation_config", JSONObject().put("speech_config", JSONArray().put(JSONObject().put("voice", safeVoice))))
        return firstMedia(transport.post(apiKey, body), "audio")
    }

    private fun validateModel(raw: String): String = raw.trim().also {
        require(it.isNotBlank() && it.length <= 160 && it.all { c -> c.isLetterOrDigit() || c in "._:/-" }) { "INVALID_GEMINI_MODEL" }
    }

    private fun textBlocks(response: JSONObject): List<String> {
        val out = mutableListOf<String>()
        forEachContent(response) { content ->
            if (content.optString("type") == "text") content.optString("text").takeIf { it.isNotBlank() }?.let(out::add)
        }
        return out
    }

    private fun firstMedia(response: JSONObject, type: String): MediaBlob {
        // Gemini Interactions API image responses can arrive as top-level output_image.
        // The previous implementation only inspected steps[].model_output.content[],
        // incorrectly rejecting successfully generated images.
        if (type == "image") {
            response.optJSONObject("output_image")?.let { image ->
                val base64 = image.optString("data", "")
                if (base64.isNotBlank()) return MediaBlob(
                    mimeType = image.optString("mime_type", "image/jpeg"),
                    data = java.util.Base64.getDecoder().decode(base64)
                )
            }
        }
        if (type == "audio") {
            response.optJSONObject("output_audio")?.let { audio ->
                val data = audio.optString("data", "")
                if (data.isNotBlank()) {
                    return MediaBlob(
                        mimeType = audio.optString("mime_type", "audio/L16"),
                        data = java.util.Base64.getDecoder().decode(data),
                        sampleRate = audio.optInt("sample_rate", 24000).takeIf { it > 0 } ?: 24000
                    )
                }
            }
        }
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
            // Bounded read prevents a giant/malformed base64 image response
            // from exhausting memory and making WebView appear permanently stuck.
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                val buffer = CharArray(8192)
                val out = StringBuilder()
                while (true) {
                    val count = reader.read(buffer)
                    if (count < 0) break
                    require(out.length + count <= 28 * 1024 * 1024) { "GEMINI_RESPONSE_TOO_LARGE" }
                    out.append(buffer, 0, count)
                }
                out.toString()
            }.orEmpty()
            if (code !in 200..299) error("GEMINI_HTTP_$code")
            return JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }
}
