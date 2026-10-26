package braseiro.ose.tts

/**
 * Universal text sanitizer for spoken audio across all TTS engines (Gemini, Kokoro, Android TTS).
 *
 * Strips non-spoken annotations (inline tags, vocal profile cues, markdown markup, speaker labels)
 * before audio generation, while strictly preserving canonical visible narration in the UI.
 */
object BraseiroSpokenTextSanitizer {
    private val TAG_REGEX = Regex("<[^>]+>")
    private val BRACKET_PERFIL_REGEX = Regex("\\[(?:perfil vocal|voz|tom|narrador|mestre|pausa)[^\\]]*\\]", setOf(RegexOption.IGNORE_CASE))
    private val SPEAKER_TAG_REGEX = Regex("^(?:Speaker\\s*\\d+|Narrador|Mestre|Bárbara):\\s*", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
    private val ASTERISK_ACTION_REGEX = Regex("\\*[^\\*\\r\\n]+\\*")
    private val MARKDOWN_HEADER_REGEX = Regex("^#{1,6}\\s+", setOf(RegexOption.MULTILINE))
    private val MARKDOWN_FORMAT_REGEX = Regex("[_~`#>]")
    private val BOLD_MARKDOWN_REGEX = Regex("\\*\\*([^*]+)\\*\\*")
    private val ITALIC_MARKDOWN_REGEX = Regex("(?<!\\*)\\*([^*\\r\\n]+)\\*(?!\\*)")
    private val WHITESPACE_REGEX = Regex("[\\t\\r\\n ]+")

    fun sanitize(raw: String): String {
        if (raw.isBlank()) return ""
        var text = raw

        // Remove XML/HTML tags: e.g. <sigh>, <breath>, <whisper>
        text = TAG_REGEX.replace(text, " ")

        // Remove profile tags: e.g. [perfil vocal: calmo], [voz: solene], [pausa: 2s]
        text = BRACKET_PERFIL_REGEX.replace(text, " ")

        // Remove speaker prefixes: e.g. Speaker 1:, Bárbara:, Narrador:
        text = SPEAKER_TAG_REGEX.replace(text, "")

        // Unwrap bold and italic markdown while preserving words
        text = BOLD_MARKDOWN_REGEX.replace(text, "$1")

        // Asterisk actions without markdown text: *risos*, *suspiro*, *passos ecoam*
        text = ASTERISK_ACTION_REGEX.replace(text, " ")

        // Remove headers and formatting chars
        text = MARKDOWN_HEADER_REGEX.replace(text, "")
        text = MARKDOWN_FORMAT_REGEX.replace(text, "")

        // Normalize whitespace
        return WHITESPACE_REGEX.replace(text, " ").trim()
    }
}
