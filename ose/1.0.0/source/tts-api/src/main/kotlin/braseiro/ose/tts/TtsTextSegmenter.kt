package braseiro.ose.tts

/**
 * Deterministic narration chunker shared by every Android TTS engine.
 * It never cuts a word and strongly prefers sentence/punctuation boundaries.
 */
object TtsTextSegmenter {
    fun segment(
        raw: String,
        targetChars: Int = 450,
        maxChars: Int = 650,
        minMergeChars: Int = 220
    ): List<String> {
        require(targetChars in 120..maxChars)
        require(maxChars >= 180)
        require(minMergeChars in 0..targetChars)

        val normalized = raw
            .replace(Regex("[\\t\\r\\n ]+"), " ")
            .trim()
        if (normalized.isBlank()) return emptyList()
        if (normalized.length <= maxChars) return listOf(normalized)

        val atoms = mutableListOf<String>()
        val sentences = normalized.split(Regex("(?<=[.!?…])\\s+"))
        for (sentence in sentences) {
            val clean = sentence.trim()
            if (clean.isBlank()) continue
            if (clean.length <= maxChars) atoms += clean
            else atoms += splitLong(clean, maxChars)
        }

        val out = mutableListOf<String>()
        var current = ""
        for (atom in atoms) {
            if (current.isBlank()) {
                current = atom
                continue
            }
            val joined = "$current $atom"
            if (joined.length <= targetChars || (current.length < minMergeChars && joined.length <= maxChars)) {
                current = joined
            } else {
                out += current
                current = atom
            }
        }
        if (current.isNotBlank()) out += current
        return out.flatMap { if (it.length <= maxChars) listOf(it) else splitLong(it, maxChars) }
    }

    private fun splitLong(text: String, maxChars: Int): List<String> {
        val out = mutableListOf<String>()
        var rest = text.trim()
        while (rest.length > maxChars) {
            val window = rest.take(maxChars + 1)
            val preferred = listOf(
                window.lastIndexOf(';'),
                window.lastIndexOf(','),
                window.lastIndexOf(':'),
                window.lastIndexOf(' ')
            ).filter { it >= (maxChars * 0.55).toInt() }.maxOrNull()
            val cut = preferred ?: window.lastIndexOf(' ').takeIf { it > 0 } ?: maxChars
            val piece = rest.substring(0, cut).trim()
            if (piece.isNotBlank()) out += piece
            rest = rest.substring(cut).trimStart(' ', ';', ',', ':')
        }
        if (rest.isNotBlank()) out += rest
        return out
    }
}
