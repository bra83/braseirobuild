package com.braseiro.dnd5e;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public final class GeminiTextModelPriorityTest {
    @Test public void preferred36FirstAndLiteSecond() {
        List<String> models=GeminiTextModelPriority.ordered("gemini-3.6-flash");
        assertEquals(2,models.size());
        assertEquals("gemini-3.6-flash",models.get(0));
        assertEquals("gemini-3.5-flash-lite",models.get(1));
    }

    @Test public void rememberedWorkingLiteMustNeverPreemptPreference() {
        // Priority is independent of last_text_model_ok; the service must not
        // use old successful models when selecting the current request.
        assertEquals("gemini-3.6-flash",GeminiTextModelPriority.ordered("models/gemini-3.6-flash").get(0));
        assertTrue(GeminiTextModelPriority.isFallback("gemini-3.6-flash","gemini-3.5-flash-lite"));
        assertFalse(GeminiTextModelPriority.isFallback("gemini-3.6-flash","gemini-3.6-flash"));
    }

    @Test public void litePreferenceDoesNotMakeDuplicateCall() {
        List<String> models=GeminiTextModelPriority.ordered("gemini-3.5-flash-lite");
        assertEquals(1,models.size());
        assertEquals("gemini-3.5-flash-lite",models.get(0));
    }

    @Test public void emptyPreferenceUsesLiteAsSafeDefault() {
        List<String> models=GeminiTextModelPriority.ordered("");
        assertEquals(1,models.size());
        assertEquals("gemini-3.5-flash-lite",models.get(0));
    }
}
