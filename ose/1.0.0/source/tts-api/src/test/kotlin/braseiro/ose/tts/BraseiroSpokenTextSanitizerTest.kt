package braseiro.ose.tts

import kotlin.test.Test
import kotlin.test.assertEquals

class BraseiroSpokenTextSanitizerTest {

    @Test
    fun stripsTagsAndVocalProfiles() {
        val raw = "<sigh> Você entra na sala. [perfil vocal: sombrio] Há um baú antigo no centro."
        val cleaned = BraseiroSpokenTextSanitizer.sanitize(raw)
        assertEquals("Você entra na sala. Há um baú antigo no centro.", cleaned)
    }

    @Test
    fun stripsSpeakerPrefixesAndMarkdown() {
        val raw = "Speaker 1: O **guerreiro** avança sobre o _degrau_ de pedra."
        val cleaned = BraseiroSpokenTextSanitizer.sanitize(raw)
        assertEquals("O guerreiro avança sobre o degrau de pedra.", cleaned)
    }

    @Test
    fun handlesWhitespaceAndEmpty() {
        assertEquals("", BraseiroSpokenTextSanitizer.sanitize("   \n\t  "))
        assertEquals("", BraseiroSpokenTextSanitizer.sanitize("<sigh> [voz: calma] "))
    }
}
