package com.braseiro.dnd5e;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Pure G13 checks for the existing narration chunk limits; no audio engine is exercised. */
public class Run245G13NarrationChunkerTest {
    private static String longNarration() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 240; i++) text.append("Uma porta se abre. A chuva cai. ");
        return text.toString();
    }

    private static String normalized(String text) {
        return PortugueseNarrationSanitizer.sanitize(text).replaceAll("\\s+", " ").trim();
    }

    private static String reassemble(List<String> chunks) {
        StringBuilder out = new StringBuilder();
        for (String chunk : chunks) {
            if (out.length() > 0) out.append(' ');
            out.append(chunk);
        }
        return out.toString();
    }

    @Test public void charonChunksPreserveTextAndRespectProgressiveBudgets() {
        String source = longNarration();
        List<String> chunks = NarrationChunker.charon(source);
        assertTrue(chunks.size() > 1);
        assertTrue(chunks.get(0).length() <= 1200);
        for (String chunk : chunks) {
            assertTrue(chunk.length() > 0);
            assertTrue(chunk.length() <= 1800);
        }
        assertEquals(normalized(source), reassemble(chunks));
    }

    @Test public void androidChunksPreserveTextAndRespectProgressiveBudgets() {
        String source = longNarration();
        List<String> chunks = NarrationChunker.android(source);
        assertTrue(chunks.size() > 1);
        assertTrue(chunks.get(0).length() <= 480);
        for (String chunk : chunks) {
            assertTrue(chunk.length() > 0);
            assertTrue(chunk.length() <= 720);
        }
        assertEquals(normalized(source), reassemble(chunks));
    }

    @Test public void kokoroChunksPreserveTextAndStayWithinProviderLimit() {
        String source = longNarration();
        List<String> chunks = NarrationChunker.kokoro(source);
        assertTrue(chunks.size() > 1);
        for (String chunk : chunks) {
            assertTrue(chunk.length() > 0);
            assertTrue(chunk.length() <= 650);
        }
        assertEquals(normalized(source), reassemble(chunks));
    }

    @Test public void shortNarrationRemainsOneChunkForEachBackend() {
        String source = "Uma porta se abre. A chuva cai.";
        assertEquals(1, NarrationChunker.charon(source).size());
        assertEquals(1, NarrationChunker.android(source).size());
        assertEquals(1, NarrationChunker.kokoro(source).size());
    }
}
