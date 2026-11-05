package braseiro.ose.app

import java.io.IOException
import org.json.JSONException

/** Keeps the player's text preference separate from the model used for one response. */
internal object GeminiTextModelSelector {
    const val FALLBACK_TEXT_MODEL = "gemini-3.5-flash-lite"

    data class Result(
        val text: String,
        val selectedModel: String,
        val effectiveModel: String,
        val fallbackReason: String? = null
    )

    fun generate(selectedModel: String, prompt: String, request: (String, String) -> String): Result {
        require(selectedModel.isNotBlank()) { "GEMINI_TEXT_MODEL_MISSING" }
        try {
            return Result(request(selectedModel, prompt), selectedModel, selectedModel)
        } catch (first: Exception) {
            if (!canFallback(first)) throw first
            val fallback = if (selectedModel == FALLBACK_TEXT_MODEL)
                GeminiInteractionsClient.DEFAULT_TEXT_MODEL else FALLBACK_TEXT_MODEL
            if (fallback == selectedModel) throw first
            val text = request(fallback, prompt)
            return Result(text, selectedModel, fallback, safeReason(first))
        }
    }

    private fun canFallback(error: Exception): Boolean {
        val code = error.message.orEmpty().uppercase()
        if (code.contains("GEMINI_HTTP_401") || code.contains("GEMINI_HTTP_403") ||
            code.contains("GEMINI_KEY_MISSING") || code.contains("INVALID_GEMINI_MODEL")) return false
        return error is IOException || error is JSONException ||
            listOf("GEMINI_HTTP_400", "GEMINI_HTTP_404", "GEMINI_HTTP_408", "GEMINI_HTTP_409",
                "GEMINI_HTTP_429", "GEMINI_HTTP_500", "GEMINI_HTTP_502", "GEMINI_HTTP_503",
                "GEMINI_HTTP_504", "GEMINI_TEXT_OUTPUT_MISSING", "TIMEOUT").any(code::contains)
    }

    private fun safeReason(error: Exception): String = error.message.orEmpty()
        .replace(Regex("[^A-Za-z0-9_ -]"), " ").trim().take(100)
        .ifBlank { error.javaClass.simpleName }
}
