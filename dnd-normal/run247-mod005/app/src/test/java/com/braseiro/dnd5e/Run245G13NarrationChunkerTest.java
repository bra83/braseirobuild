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
        assertTrue("Kokoro first block must follow Forbidden Lands size", chunks.get(0).length() <= 560);
        for (String chunk : chunks) {
            assertTrue(chunk.length() > 0);
            assertTrue(chunk.length() <= 760);
        }
        assertEquals(normalized(source), reassemble(chunks));
    }

    @Test public void kokoroPreservesQuotesPunctuationAndOriginalWords() {
        String quoted = "Ela sussurrou: “A porta está aberta.” O vento respondeu; "
                + "um outro murmúrio surgiu… ";
        StringBuilder source = new StringBuilder();
        for (int i = 0; i < 110; i++) source.append(quoted);
        List<String> chunks = NarrationChunker.kokoro(source.toString());
        assertTrue(chunks.size() >= 3);
        assertTrue(chunks.get(0).length() <= 560);
        for (int i = 1; i < chunks.size(); i++) assertTrue(chunks.get(i).length() <= 760);
        assertEquals(normalized(source.toString()), reassemble(chunks));
        for (String chunk : chunks) {
            assertTrue("Closing quote must stay with sentence", !chunk.startsWith("”"));
            assertTrue("No leading punctuation from a mid-word split", !chunk.startsWith(","));
        }
    }

    @Test public void kokoroNeverSplitsAnUnusuallyLongWord() {
        StringBuilder source = new StringBuilder("Uma sombra atravessa a soleira. ");
        for (int i = 0; i < 120; i++) source.append('x');
        source.append(" ");
        for (int i = 0; i < 850; i++) source.append('y');
        source.append(" e tudo ficou em silêncio.");
        List<String> chunks = NarrationChunker.kokoro(source.toString());
        assertEquals(normalized(source.toString()), reassemble(chunks));
        StringBuilder longWord = new StringBuilder();
        for (int i = 0; i < 850; i++) longWord.append('y');
        boolean present = false;
        for (String chunk : chunks) if (chunk.contains(longWord.toString())) present = true;
        assertTrue("Long word must survive in one piece", present);
    }

    @Test public void shortNarrationRemainsOneChunkForEachBackend() {
        String source = "Uma porta se abre. A chuva cai.";
        assertEquals(1, NarrationChunker.charon(source).size());
        assertEquals(1, NarrationChunker.android(source).size());
        assertEquals(1, NarrationChunker.kokoro(source).size());
    }
}
