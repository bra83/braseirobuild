package braseiro.ose.app

import android.content.Context
import org.json.JSONObject

/** Non-secret Gemini presentation settings. Model identifiers are intentionally user-editable. */
class GeminiRuntimeSettings(context: Context) {
    data class Snapshot(
        val textModel: String,
        val imageModel: String,
        val ttsModel: String,
        val ttsVoice: String
    )

    private val prefs = context.getSharedPreferences("braseiro_ose_gemini_runtime_v1", Context.MODE_PRIVATE)

    fun snapshot(): Snapshot = Snapshot(
        textModel = prefs.getString("textModel", null).orEmpty().ifBlank { GeminiInteractionsClient.DEFAULT_TEXT_MODEL },
        imageModel = prefs.getString("imageModel", null).orEmpty().ifBlank { GeminiInteractionsClient.DEFAULT_IMAGE_MODEL },
        // Migrate only known legacy model IDs; retain custom user selections.
        ttsModel = prefs.getString("ttsModel", null).orEmpty()
            .takeUnless { it.isBlank() || it == "gemini-3.1-flash-tts-preview" ||
                it == "gemini-2.5-flash-preview-tts" }
            ?: GeminiInteractionsClient.DEFAULT_TTS_MODEL,
        ttsVoice = prefs.getString("ttsVoice", null).orEmpty().ifBlank { GeminiInteractionsClient.DEFAULT_TTS_VOICE }
    )

    fun update(textModel: String?, imageModel: String?, ttsModel: String?, ttsVoice: String?) {
        val editor = prefs.edit()
        textModel?.let { editor.putString("textModel", validateModel(it)) }
        imageModel?.let { editor.putString("imageModel", validateModel(it)) }
        ttsModel?.let { editor.putString("ttsModel", validateModel(it)) }
        ttsVoice?.let { editor.putString("ttsVoice", validateVoice(it)) }
        editor.apply()
    }

    fun statusJson(): JSONObject = snapshot().let { s ->
        JSONObject()
            .put("textModel", s.textModel)
            .put("imageModel", s.imageModel)
            .put("ttsModel", s.ttsModel)
            .put("ttsVoice", s.ttsVoice)
    }

    private fun validateModel(raw: String): String = raw.trim().also {
        require(it.isNotBlank() && it.length <= 160 && it.all { c -> c.isLetterOrDigit() || c in "._:/-" }) { "INVALID_GEMINI_MODEL" }
    }

    private fun validateVoice(raw: String): String = raw.trim().also {
        require(it.isNotBlank() && it.length <= 80 && it.all { c -> c.isLetterOrDigit() || c in "._-" }) { "INVALID_TTS_VOICE" }
    }
}
