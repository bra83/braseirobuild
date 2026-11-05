package braseiro.ose.app

import java.io.IOException
import org.json.JSONException
import org.junit.Assert.*
import org.junit.Test

class GeminiTextModelSelectorTest {
    private val selected = "gemini-3.6-flash"

    @Test fun successfulSelectionNeverFallsBack() {
        val called = mutableListOf<String>()
        val result = GeminiTextModelSelector.generate(selected, "cena") { model, _ ->
            called += model
            "resposta"
        }
        assertEquals(listOf(selected), called)
        assertEquals(selected, result.selectedModel)
        assertEquals(selected, result.effectiveModel)
        assertNull(result.fallbackReason)
    }

    @Test fun concreteFailureFallsBackButNextTurnRetriesSelection() {
        val called = mutableListOf<String>()
        var failed = true
        val request: (String, String) -> String = { model, prompt ->
            called += model
            assertEquals("cena", prompt)
            if (model == selected && failed) error("GEMINI_HTTP_503")
            "resposta"
        }
        val first = GeminiTextModelSelector.generate(selected, "cena", request)
        assertEquals(listOf(selected, GeminiTextModelSelector.FALLBACK_TEXT_MODEL), called)
        assertEquals(selected, first.selectedModel)
        assertEquals(GeminiTextModelSelector.FALLBACK_TEXT_MODEL, first.effectiveModel)
        assertEquals("GEMINI_HTTP_503", first.fallbackReason)

        failed = false
        called.clear()
        val recovered = GeminiTextModelSelector.generate(selected, "cena", request)
        assertEquals(listOf(selected), called)
        assertEquals(selected, recovered.effectiveModel)
    }

    @Test fun authErrorDoesNotSpendAnotherRequest() {
        val called = mutableListOf<String>()
        try {
            GeminiTextModelSelector.generate(selected, "cena") { model, _ ->
                called += model
                error("GEMINI_HTTP_401")
            }
            fail("401 should reach caller")
        } catch (error: IllegalStateException) {
            assertEquals("GEMINI_HTTP_401", error.message)
        }
        assertEquals(listOf(selected), called)
    }

    @Test fun networkAndInvalidJsonUseOneFallbackOnly() {
        for (failure in listOf(IOException("TIMEOUT"), JSONException("invalid JSON"))) {
            val called = mutableListOf<String>()
            val result = GeminiTextModelSelector.generate(selected, "cena") { model, _ ->
                called += model
                if (model == selected) throw failure
                "resposta"
            }
            assertEquals(listOf(selected, GeminiTextModelSelector.FALLBACK_TEXT_MODEL), called)
            assertEquals(GeminiTextModelSelector.FALLBACK_TEXT_MODEL, result.effectiveModel)
        }
    }

    @Test fun fallbackFailureStopsAfterTwoAttempts() {
        val called = mutableListOf<String>()
        try {
            GeminiTextModelSelector.generate(selected, "cena") { model, _ ->
                called += model
                error("GEMINI_HTTP_503")
            }
            fail("both models failed")
        } catch (_: IllegalStateException) { }
        assertEquals(listOf(selected, GeminiTextModelSelector.FALLBACK_TEXT_MODEL), called)
    }

    @Test fun selectedFallbackModelUsesTheOlderSupportedAlternative() {
        val called = mutableListOf<String>()
        val result = GeminiTextModelSelector.generate(GeminiTextModelSelector.FALLBACK_TEXT_MODEL, "cena") { model, _ ->
            called += model
            if (called.size == 1) error("GEMINI_HTTP_503")
            "resposta"
        }
        assertEquals(listOf(GeminiTextModelSelector.FALLBACK_TEXT_MODEL,
            GeminiInteractionsClient.DEFAULT_TEXT_MODEL), called)
        assertEquals(GeminiInteractionsClient.DEFAULT_TEXT_MODEL, result.effectiveModel)
    }
}
