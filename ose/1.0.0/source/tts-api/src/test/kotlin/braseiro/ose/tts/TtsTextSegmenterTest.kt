package braseiro.ose.tts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TtsTextSegmenterTest {
    @Test fun blankInputProducesNoChunks() {
        assertTrue(TtsTextSegmenter.segment("   \n  ").isEmpty())
    }

    @Test fun shortNarrationStaysWhole() {
        val text = "A porta range. O corredor permanece escuro."
        assertEquals(listOf(text), TtsTextSegmenter.segment(text))
    }

    @Test fun longNarrationNeverExceedsConfiguredMaximumOrCutsWords() {
        val text = List(90) { i ->
            "A companhia avança pelo corredor de pedra número $i, ouvindo água ao longe e passos que talvez sejam apenas ecos."
        }.joinToString(" ")
        val chunks = TtsTextSegmenter.segment(text, targetChars = 430, maxChars = 620, minMergeChars = 200)
        assertTrue(chunks.size > 2)
        assertTrue(chunks.all { it.length <= 620 })
        assertEquals(text.replace(Regex("[\\t\\r\\n ]+"), " ").trim(), chunks.joinToString(" "))
    }

    @Test fun oversizedSentenceUsesPunctuationOrWhitespaceBoundaries() {
        val text = (1..180).joinToString(" ") { "palavra$it" }
        val chunks = TtsTextSegmenter.segment(text, targetChars = 300, maxChars = 360, minMergeChars = 120)
        assertTrue(chunks.all { it.length <= 360 })
        assertTrue(chunks.none { it.startsWith(" ") || it.endsWith(" ") })
        assertEquals(text, chunks.joinToString(" "))
    }
}
