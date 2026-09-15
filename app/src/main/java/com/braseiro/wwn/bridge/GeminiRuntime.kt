package com.braseiro.wwn.bridge

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.math.max

class GeminiRuntime(private val context: Context) {
    data class SpeechPayload(val pcm: ByteArray, val model: String, val voice: String, val mimeType: String)

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun status(): JSONObject = JSONObject()
        .put("status", "OK")
        .put("configured", readSecret().isNotBlank())
        .put("model", configuredTextModel())
        .put("textModel", configuredTextModel())
        .put("imageModel", configuredImageModel())
        .put("ttsModel", configuredTtsModel())
        .put("ttsVoice", configuredTtsVoice())
        .put("ttsProvider", configuredTtsProvider())
        .put("visionEnabled", preferences.getBoolean(PREF_KEY_VISION, true))
        .put("transport", "ANDROID_NATIVE")
        .put("secretExposedToJavascript", false)

    fun setApiKey(apiKey: String, requestedModel: String?): JSONObject {
        val key = apiKey.trim()
        require(key.length in 20..512) { "Gemini API key length is invalid" }
        require(!key.contains(Regex("\\s"))) { "Gemini API key contains whitespace" }
        requestedModel?.takeIf { it.isNotBlank() }?.let(::setModel)
        preferences.edit().putString(PREF_KEY_SECRET, encrypt(key)).apply()
        return status()
    }

    fun setModel(requestedModel: String): JSONObject {
        val model = sanitizeModel(requestedModel, DEFAULT_TEXT_MODEL)
        preferences.edit().putString(PREF_KEY_MODEL, model).apply()
        return status()
    }

    fun setImageModel(requestedModel: String): JSONObject {
        val model = sanitizeModel(requestedModel, DEFAULT_IMAGE_MODEL)
        preferences.edit().putString(PREF_KEY_IMAGE_MODEL, model).apply()
        return status()
    }

    fun setTtsConfig(requestedModel: String, requestedVoice: String, requestedProvider: String? = null): JSONObject {
        val model = sanitizeModel(requestedModel, DEFAULT_TTS_MODEL)
        val voice = sanitizeVoice(requestedVoice)
        val provider = sanitizeTtsProvider(requestedProvider ?: configuredTtsProvider())
        preferences.edit()
            .putString(PREF_KEY_TTS_MODEL, model)
            .putString(PREF_KEY_TTS_VOICE, voice)
            .putString(PREF_KEY_TTS_PROVIDER, provider)
            .apply()
        return status()
    }

    fun setVisionEnabled(enabled: Boolean): JSONObject {
        preferences.edit().putBoolean(PREF_KEY_VISION, enabled).apply()
        return status()
    }

    fun clearApiKey(): JSONObject {
        preferences.edit().remove(PREF_KEY_SECRET).apply()
        return status()
    }

    fun generateText(requestJson: String): JSONObject {
        val key = readSecret()
        if (key.isBlank()) return JSONObject().put("status", "CREDENTIAL_UNAVAILABLE").put("model", configuredTextModel())
        require(requestJson.length <= MAX_REQUEST_JSON_CHARS) { "Gemini request too large" }
        val request = JSONObject(requestJson)
        val model = sanitizeModel(request.optString("model", configuredTextModel()), DEFAULT_TEXT_MODEL)
        val systemInstruction = request.optString("systemInstruction", "").trim()
        val userText = request.optString("userText", "").trim()
        require(userText.isNotBlank()) { "Gemini userText is required" }
        require(userText.length <= MAX_PROMPT_CHARS) { "Gemini prompt too large" }
        require(systemInstruction.length <= MAX_SYSTEM_CHARS) { "Gemini system instruction too large" }

        val parts = JSONArray()
        val visionEnabled = preferences.getBoolean(PREF_KEY_VISION, true)
        val imageUrl = request.optString("imageUrl", "").trim()
        var imageAttached = false
        if (visionEnabled && imageUrl.isNotBlank()) {
            resolveSceneImagePart(imageUrl)?.let {
                parts.put(it)
                imageAttached = true
            }
        }
        parts.put(JSONObject().put("text", userText))

        val body = JSONObject()
        if (systemInstruction.isNotBlank()) {
            body.put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
        }
        body.put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts)))
        val result = postGenerateContent(model, key, body, MAX_TEXT_RESPONSE_BYTES)
        result.put("imageAttached", imageAttached)
        return result
    }

    fun generateImage(requestJson: String): JSONObject {
        val key = readSecret()
        if (key.isBlank()) return JSONObject().put("status", "CREDENTIAL_UNAVAILABLE").put("model", configuredImageModel())
        require(requestJson.length <= MAX_REQUEST_JSON_CHARS) { "Gemini image request too large" }
        val request = JSONObject(requestJson)
        val model = sanitizeModel(request.optString("model", configuredImageModel()), DEFAULT_IMAGE_MODEL)
        val prompt = request.optString("prompt", "").trim()
        require(prompt.isNotBlank()) { "Gemini image prompt is required" }
        require(prompt.length <= MAX_IMAGE_PROMPT_CHARS) { "Gemini image prompt too large" }
        val aspectRatio = request.optString("aspectRatio", "16:9").trim().ifBlank { "16:9" }
        require(aspectRatio in IMAGE_ASPECT_RATIOS) { "Gemini image aspect ratio is invalid" }
        val imageSize = request.optString("imageSize", "1K").trim().ifBlank { "1K" }
        require(imageSize in IMAGE_SIZES) { "Gemini image size is invalid" }
        val previousInteractionId = request.optString("previousInteractionId", "").trim()
        if (previousInteractionId.isNotBlank()) require(INTERACTION_ID_PATTERN.matches(previousInteractionId)) { "Gemini interaction id is invalid" }

        val input = JSONArray().put(JSONObject().put("type", "text").put("text", prompt))
        val references = request.optJSONArray("referenceImageUrls") ?: JSONArray()
        var referenceCount = 0
        for (i in 0 until minOf(references.length(), MAX_REFERENCE_IMAGES)) {
            val raw = references.optString(i, "").trim()
            if (raw.isBlank()) continue
            resolveInteractionImageContent(raw)?.let { input.put(it); referenceCount += 1 }
        }
        val body = JSONObject()
            .put("model", model)
            .put("input", input)
            .put("store", true)
            .put("response_format", JSONArray().put(JSONObject()
                .put("type", "image")
                .put("aspect_ratio", aspectRatio)
                .put("image_size", imageSize)
                .put("mime_type", "image/jpeg")
                .put("delivery", "inline")))
        if (previousInteractionId.isNotBlank()) body.put("previous_interaction_id", previousInteractionId)

        val connection = URL("https://generativelanguage.googleapis.com/v1beta/interactions").openConnection() as HttpURLConnection
        return try {
            configureConnection(connection, key)
            connection.readTimeout = IMAGE_READ_TIMEOUT_MS
            connection.outputStream.use { output -> output.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val httpStatus = connection.responseCode
            val stream = if (httpStatus in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.use { readBounded(it, MAX_IMAGE_RESPONSE_BYTES) }.orEmpty()
            if (httpStatus !in 200..299) {
                JSONObject().put("status", "HTTP_ERROR").put("httpStatus", httpStatus).put("model", model).put("reason", safeHttpReason(httpStatus, responseText))
            } else {
                val response = JSONObject(responseText)
                var data = ""
                var mime = "image/jpeg"
                val steps = response.optJSONArray("steps") ?: JSONArray()
                loop@ for (i in 0 until steps.length()) {
                    val content = steps.optJSONObject(i)?.optJSONArray("content") ?: continue
                    for (j in 0 until content.length()) {
                        val part = content.optJSONObject(j) ?: continue
                        if (part.optString("type") == "image") {
                            data = part.optString("data", "")
                            mime = part.optString("mime_type", "image/jpeg")
                            if (data.isNotBlank()) break@loop
                        }
                    }
                }
                if (data.isBlank()) JSONObject().put("status", "EMPTY_IMAGE_RESPONSE").put("model", model).put("interactionId", response.optString("id", ""))
                else {
                    val bytes = Base64.decode(data, Base64.DEFAULT)
                    val stored = SceneImageRuntime(context).saveGeneratedImage(bytes, mime)
                    stored.put("status", "OK")
                        .put("model", model)
                        .put("interactionId", response.optString("id", ""))
                        .put("referenceImageCount", referenceCount)
                }
            }
        } catch (t: Throwable) {
            JSONObject().put("status", "NETWORK_ERROR").put("model", model).put("reason", t.message ?: t.javaClass.simpleName)
        } finally {
            connection.disconnect()
        }
    }

    fun generateSpeech(text: String, requestedModel: String? = null, requestedVoice: String? = null, style: String? = null): SpeechPayload {
        val key = readSecret()
        require(key.isNotBlank()) { "Gemini API key is not configured" }
        val model = sanitizeModel(requestedModel.orEmpty().ifBlank { configuredTtsModel() }, DEFAULT_TTS_MODEL)
        val voice = sanitizeVoice(requestedVoice.orEmpty().ifBlank { configuredTtsVoice() })
        val cleanText = text.trim()
        require(cleanText.isNotBlank()) { "TTS text is required" }
        require(cleanText.length <= MAX_TTS_TEXT_CHARS) { "TTS text is too long" }
        val spokenInput = buildString {
            val direction = style?.trim().orEmpty()
            if (direction.isNotBlank()) append(direction.take(600)).append("\n\n")
            append(cleanText)
        }
        val generationConfig = JSONObject()
            .put("responseModalities", JSONArray().put("AUDIO"))
            .put("speechConfig", JSONObject()
                .put("languageCode", "pt-BR")
                .put("voiceConfig", JSONObject()
                    .put("prebuiltVoiceConfig", JSONObject().put("voiceName", voice))))
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", spokenInput)))))
            .put("generationConfig", generationConfig)

        val connection = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent").openConnection() as HttpURLConnection
        try {
            configureConnection(connection, key)
            connection.outputStream.use { output -> output.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val httpStatus = connection.responseCode
            val stream = if (httpStatus in 200..299) connection.inputStream else connection.errorStream
            val responseBytes = stream?.use { readBoundedBytes(it, MAX_AUDIO_RESPONSE_BYTES) } ?: ByteArray(0)
            val responseText = String(responseBytes, Charsets.UTF_8)
            if (httpStatus !in 200..299) throw IllegalStateException(safeHttpReason(httpStatus, responseText))
            val response = JSONObject(responseText)
            val part = response.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)
                ?: throw IllegalStateException("EMPTY_TTS_RESPONSE")
            val inline = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data")
                ?: throw IllegalStateException("TTS_AUDIO_MISSING")
            val data = inline.optString("data", "")
            require(data.isNotBlank()) { "TTS_AUDIO_MISSING" }
            val pcm = Base64.decode(data, Base64.DEFAULT)
            require(pcm.isNotEmpty()) { "TTS_AUDIO_EMPTY" }
            val mime = inline.optString("mimeType", inline.optString("mime_type", "audio/pcm;rate=24000"))
            return SpeechPayload(pcm = pcm, model = model, voice = voice, mimeType = mime)
        } finally {
            connection.disconnect()
        }
    }

    fun configuredImageModel(): String = runCatching { sanitizeModel(preferences.getString(PREF_KEY_IMAGE_MODEL, DEFAULT_IMAGE_MODEL) ?: DEFAULT_IMAGE_MODEL, DEFAULT_IMAGE_MODEL) }.getOrDefault(DEFAULT_IMAGE_MODEL)
    fun configuredTtsProvider(): String = sanitizeTtsProvider(preferences.getString(PREF_KEY_TTS_PROVIDER, DEFAULT_TTS_PROVIDER) ?: DEFAULT_TTS_PROVIDER)
    fun configuredTtsModel(): String = runCatching { sanitizeModel(preferences.getString(PREF_KEY_TTS_MODEL, DEFAULT_TTS_MODEL) ?: DEFAULT_TTS_MODEL, DEFAULT_TTS_MODEL) }.getOrDefault(DEFAULT_TTS_MODEL)
    fun configuredTtsVoice(): String = runCatching { sanitizeVoice(preferences.getString(PREF_KEY_TTS_VOICE, DEFAULT_TTS_VOICE) ?: DEFAULT_TTS_VOICE) }.getOrDefault(DEFAULT_TTS_VOICE)

    private fun postGenerateContent(model: String, key: String, body: JSONObject, maxBytes: Int): JSONObject {
        val connection = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent").openConnection() as HttpURLConnection
        return try {
            configureConnection(connection, key)
            connection.outputStream.use { output -> output.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val httpStatus = connection.responseCode
            val stream = if (httpStatus in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.use { readBounded(it, maxBytes) }.orEmpty()
            if (httpStatus !in 200..299) {
                JSONObject().put("status", "HTTP_ERROR").put("httpStatus", httpStatus).put("model", model).put("reason", safeHttpReason(httpStatus, responseText))
            } else {
                val response = JSONObject(responseText)
                val text = extractText(response)
                if (text.isBlank()) JSONObject().put("status", "EMPTY_RESPONSE").put("model", model)
                else JSONObject().put("status", "OK").put("model", model).put("text", text.take(MAX_RETURNED_TEXT_CHARS))
                    .put("finishReason", response.optJSONArray("candidates")?.optJSONObject(0)?.optString("finishReason", "") ?: "")
            }
        } catch (t: Throwable) {
            JSONObject().put("status", "NETWORK_ERROR").put("model", model).put("reason", t.message ?: t.javaClass.simpleName)
        } finally {
            connection.disconnect()
        }
    }

    private fun configureConnection(connection: HttpURLConnection, key: String) {
        connection.requestMethod = "POST"
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("x-goog-api-key", key)
        connection.setRequestProperty("User-Agent", "Braseiro-WWN/1.0")
    }

    private fun resolveSceneImagePart(rawUrl: String): JSONObject? {
        return runCatching {
            val uri = Uri.parse(rawUrl)
            require(uri.scheme == "https" && uri.host == SceneImageRuntime.LOCAL_HOST) { "Scene image URL is not local" }
            val name = uri.path?.removePrefix("/private-scene/").orEmpty()
            require(SCENE_IMAGE_NAME.matches(name)) { "Scene image name is invalid" }
            val dir = File(context.filesDir, "scene-images-v1").canonicalFile
            val file = File(dir, name).canonicalFile
            require(file.isFile && file.parentFile == dir) { "Scene image is unavailable" }
            val prepared = prepareImageForGemini(file)
            JSONObject().put("inlineData", JSONObject().put("mimeType", prepared.first).put("data", Base64.encodeToString(prepared.second, Base64.NO_WRAP)))
        }.getOrNull()
    }

    private fun resolveInteractionImageContent(rawUrl: String): JSONObject? {
        return runCatching {
            val uri = Uri.parse(rawUrl)
            require(uri.scheme == "https" && uri.host == SceneImageRuntime.LOCAL_HOST) { "Reference image URL is not local" }
            val name = uri.path?.removePrefix("/private-scene/").orEmpty()
            require(SCENE_IMAGE_NAME.matches(name)) { "Reference image name is invalid" }
            val dir = File(context.filesDir, "scene-images-v1").canonicalFile
            val file = File(dir, name).canonicalFile
            require(file.isFile && file.parentFile == dir) { "Reference image is unavailable" }
            val prepared = prepareImageForGemini(file)
            JSONObject().put("type", "image").put("data", Base64.encodeToString(prepared.second, Base64.NO_WRAP)).put("mime_type", prepared.first)
        }.getOrNull()
    }

    private fun prepareImageForGemini(file: File): Pair<String, ByteArray> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Scene image cannot be decoded" }
        var sample = 1
        while (max(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_VISION_DIMENSION * 2) sample *= 2
        val decoded = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IllegalStateException("Scene image cannot be decoded")
        val maxSide = max(decoded.width, decoded.height)
        val scaled = if (maxSide > MAX_VISION_DIMENSION) {
            val ratio = MAX_VISION_DIMENSION.toDouble() / maxSide.toDouble()
            Bitmap.createScaledBitmap(decoded, (decoded.width * ratio).toInt().coerceAtLeast(1), (decoded.height * ratio).toInt().coerceAtLeast(1), true)
        } else decoded
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 84, out)
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()
        val bytes = out.toByteArray()
        require(bytes.size <= MAX_VISION_BYTES) { "Prepared scene image is too large" }
        return "image/jpeg" to bytes
    }

    private fun extractText(response: JSONObject): String {
        val parts = response.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts") ?: return ""
        val out = StringBuilder()
        for (index in 0 until parts.length()) {
            val text = parts.optJSONObject(index)?.optString("text", "").orEmpty()
            if (text.isNotBlank()) { if (out.isNotEmpty()) out.append('\n'); out.append(text) }
        }
        return out.toString().trim()
    }

    private fun safeHttpReason(status: Int, responseBody: String): String {
        val error = runCatching { JSONObject(responseBody).optJSONObject("error") }.getOrNull()
        val apiStatus = error?.optString("status", "").orEmpty()
        val message = error?.optString("message", "").orEmpty()
        return when {
            message.isNotBlank() -> message.take(240)
            apiStatus.isNotBlank() -> apiStatus.take(80)
            status == 401 || status == 403 -> "AUTHENTICATION_FAILED"
            status == 429 -> "RATE_LIMITED"
            status >= 500 -> "UPSTREAM_UNAVAILABLE"
            else -> "GEMINI_HTTP_$status"
        }
    }

    private fun configuredTextModel(): String = runCatching { sanitizeModel(preferences.getString(PREF_KEY_MODEL, DEFAULT_TEXT_MODEL) ?: DEFAULT_TEXT_MODEL, DEFAULT_TEXT_MODEL) }.getOrDefault(DEFAULT_TEXT_MODEL)
    private fun sanitizeModel(value: String, fallback: String): String { val clean = value.trim().ifBlank { fallback }; require(MODEL_PATTERN.matches(clean)) { "Gemini model is invalid" }; return clean }
    private fun sanitizeVoice(value: String): String { val clean = value.trim().ifBlank { DEFAULT_TTS_VOICE }; require(clean in VOICES) { "Gemini TTS voice is invalid" }; return clean }
    private fun sanitizeTtsProvider(value: String): String { val clean = value.trim().uppercase().ifBlank { DEFAULT_TTS_PROVIDER }; require(clean in setOf("GEMINI", "ANDROID")) { "TTS provider is invalid" }; return clean }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv; val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8)); val packed = ByteArray(1 + iv.size + encrypted.size)
        packed[0] = iv.size.toByte(); System.arraycopy(iv, 0, packed, 1, iv.size); System.arraycopy(encrypted, 0, packed, 1 + iv.size, encrypted.size)
        return Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    private fun readSecret(): String {
        val encoded = preferences.getString(PREF_KEY_SECRET, "").orEmpty(); if (encoded.isBlank()) return ""
        return try {
            val packed = Base64.decode(encoded, Base64.NO_WRAP); require(packed.size > 14); val ivSize = packed[0].toInt() and 0xff; require(ivSize in 12..16 && packed.size > 1 + ivSize)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, packed.copyOfRange(1, 1 + ivSize)))
            String(cipher.doFinal(packed.copyOfRange(1 + ivSize, packed.size)), Charsets.UTF_8)
        } catch (_: Throwable) { "" }
    }

    private fun readBounded(stream: InputStream, maxBytes: Int): String = String(readBoundedBytes(stream, maxBytes), Charsets.UTF_8)
    private fun readBoundedBytes(stream: InputStream, maxBytes: Int): ByteArray {
        val buffer = ByteArray(8192); val output = ByteArrayOutputStream(); var total = 0
        while (true) { val read = stream.read(buffer); if (read < 0) break; total += read; require(total <= maxBytes) { "Gemini response too large" }; output.write(buffer, 0, read) }
        return output.toByteArray()
    }

    companion object {
        const val DEFAULT_TEXT_MODEL = "gemini-3.8-flash"
        const val DEFAULT_IMAGE_MODEL = "gemini-3.1-flash-image"
        const val DEFAULT_TTS_MODEL = "gemini-3.1-flash-tts-preview"
        const val DEFAULT_TTS_VOICE = "Charon"
        const val DEFAULT_TTS_PROVIDER = "GEMINI"
        private const val PREFS_NAME = "braseiro_wwn_secure_runtime_v1"
        private const val PREF_KEY_SECRET = "gemini_api_key_encrypted"
        private const val PREF_KEY_MODEL = "gemini_text_model"
        private const val PREF_KEY_IMAGE_MODEL = "gemini_image_model"
        private const val PREF_KEY_TTS_MODEL = "gemini_tts_model"
        private const val PREF_KEY_TTS_VOICE = "gemini_tts_voice"
        private const val PREF_KEY_TTS_PROVIDER = "gemini_tts_provider"
        private const val PREF_KEY_VISION = "gemini_scene_vision_enabled"
        private const val KEY_ALIAS = "braseiro_wwn_gemini_secret_v1"
        private val MODEL_PATTERN = Regex("^[A-Za-z0-9._-]{3,96}$")
        private val SCENE_IMAGE_NAME = Regex("^[a-f0-9]{32}\\.(png|jpg|webp)$")
        val VOICES: Set<String> = setOf("Zephyr","Puck","Charon","Kore","Fenrir","Leda","Orus","Aoede","Callirrhoe","Autonoe","Enceladus","Iapetus","Umbriel","Algieba","Despina","Erinome","Algenib","Rasalgethi","Laomedeia","Achernar","Alnilam","Schedar","Gacrux","Pulcherrima","Achird","Zubenelgenubi","Vindemiatrix","Sadachbia","Sadaltager","Sulafat")

        private val IMAGE_ASPECT_RATIOS: Set<String> = setOf("1:1","1:4","1:8","2:3","3:2","3:4","4:1","4:3","4:5","5:4","8:1","9:16","16:9","21:9")
        private val IMAGE_SIZES: Set<String> = setOf("512","1K","2K","4K")
        private val INTERACTION_ID_PATTERN = Regex("^v[0-9]+_[A-Za-z0-9_-]{20,512}$")
        private const val MAX_REQUEST_JSON_CHARS = 260_000
        private const val MAX_PROMPT_CHARS = 180_000
        private const val MAX_SYSTEM_CHARS = 20_000
        private const val MAX_TEXT_RESPONSE_BYTES = 2_000_000
        private const val MAX_AUDIO_RESPONSE_BYTES = 24_000_000
        private const val MAX_RETURNED_TEXT_CHARS = 60_000
        private const val MAX_TTS_TEXT_CHARS = 1_200
        private const val MAX_IMAGE_PROMPT_CHARS = 20_000
        private const val MAX_REFERENCE_IMAGES = 5
        private const val MAX_IMAGE_RESPONSE_BYTES = 24_000_000
        private const val MAX_VISION_DIMENSION = 1536
        private const val MAX_VISION_BYTES = 3_000_000
        private const val CONNECT_TIMEOUT_MS = 20_000
        private const val READ_TIMEOUT_MS = 120_000
        private const val IMAGE_READ_TIMEOUT_MS = 180_000
    }
}
