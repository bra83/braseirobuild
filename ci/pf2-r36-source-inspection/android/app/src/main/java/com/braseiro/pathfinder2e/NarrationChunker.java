package com.braseiro.pathfinder2e;

import java.util.ArrayList;
import java.util.List;

/**
 * Segmentação de narração.
 *
 * Charon mantém o comportamento existente. O perfil Braseiro Kokoro usa duas
 * camadas equivalentes ao Forbidden Lands atual:
 * 1) capítulos narrativos: primeiro curto (<=320), seguintes ~680;
 * 2) enunciados enviados ao Android TextToSpeech: ~150, procurando corte a
 *    partir de 90 caracteres e nunca dividindo uma palavra.
 */
public final class NarrationChunker {
    private static final int CHARON_SINGLE_BLOCK = 1700;
    private static final int CHARON_FIRST_BLOCK = 1300;
    private static final int CHARON_NEXT_BLOCK = 2200;
    private static final int ANDROID_SINGLE_BLOCK = 720;
    private static final int ANDROID_FIRST_BLOCK = 480;
    private static final int ANDROID_NEXT_BLOCK = 720;

    public static final int KOKORO_FIRST_CHAPTER_MAX = 320;
    public static final int KOKORO_FIRST_CHAPTER_MIN = 120;
    public static final int KOKORO_CHAPTER_MAX = 680;
    public static final int KOKORO_CHAPTER_MIN = 420;
    public static final int KOKORO_ANDROID_MAX = 150;
    public static final int KOKORO_ANDROID_MIN = 90;

    private NarrationChunker() {}

    public static List<String> charon(String text) {
        String clean = normalize(text);
        ArrayList<String> out = new ArrayList<String>();
        if (clean.length() == 0) return out;
        if (clean.length() <= CHARON_SINGLE_BLOCK) {
            out.add(clean);
            return out;
        }
        return progressive(clean, CHARON_FIRST_BLOCK, CHARON_NEXT_BLOCK);
    }

    public static List<String> android(String text) {
        String clean = normalize(text);
        ArrayList<String> out = new ArrayList<String>();
        if (clean.length() == 0) return out;
        if (clean.length() <= ANDROID_SINGLE_BLOCK) {
            out.add(clean);
            return out;
        }
        return progressive(clean, ANDROID_FIRST_BLOCK, ANDROID_NEXT_BLOCK);
    }

    /**
     * Primeira camada Kokoro: capítulos semânticos. O primeiro é
     * deliberadamente menor para reduzir a latência percebida.
     */
    public static List<String> kokoroChapters(String text) {
        String clean = normalize(text);
        ArrayList<String> out = new ArrayList<String>();
        if (clean.length() == 0) return out;
        int cursor = 0;
        while (cursor < clean.length()) {
            while (cursor < clean.length() && Character.isWhitespace(clean.charAt(cursor))) cursor++;
            if (cursor >= clean.length()) break;
            boolean first = out.isEmpty();
            int max = first ? KOKORO_FIRST_CHAPTER_MAX : KOKORO_CHAPTER_MAX;
            int min = first ? KOKORO_FIRST_CHAPTER_MIN : KOKORO_CHAPTER_MIN;
            int end = semanticCut(clean, cursor, max, min);
            if (end <= cursor) end = forwardBoundary(clean, cursor, Math.min(clean.length(), cursor + max));
            String part = clean.substring(cursor, Math.max(cursor + 1, end)).trim();
            if (part.length() > 0) out.add(part);
            cursor = Math.max(cursor + 1, end);
        }
        if (out.isEmpty()) out.add(clean);
        return out;
    }

    /**
     * Segunda camada Kokoro: pequenos enunciados Android TTS. A busca do corte
     * começa por pontuação e depois espaço; se uma palavra excepcionalmente
     * longa ultrapassar o máximo, o corte avança até o próximo limite natural
     * em vez de quebrar a palavra.
     */
    public static List<String> kokoroAndroidSegments(String chapter) {
        String clean = normalize(chapter);
        ArrayList<String> out = new ArrayList<String>();
        if (clean.length() == 0) return out;
        int cursor = 0;
        while (cursor < clean.length()) {
            while (cursor < clean.length() && Character.isWhitespace(clean.charAt(cursor))) cursor++;
            if (cursor >= clean.length()) break;
            int end = androidCut(clean, cursor);
            if (end <= cursor) end = forwardBoundary(clean, cursor, Math.min(clean.length(), cursor + KOKORO_ANDROID_MAX));
            String part = clean.substring(cursor, Math.max(cursor + 1, end)).trim();
            if (part.length() > 0) out.add(part);
            cursor = Math.max(cursor + 1, end);
        }
        if (out.isEmpty()) out.add(clean);
        return out;
    }

    /** Compatibilidade com o pipeline existente: devolve a fila final a ser
     * enviada a android.speech.tts.TextToSpeech, preservando as duas camadas. */
    public static List<String> kokoro(String text) {
        ArrayList<String> out = new ArrayList<String>();
        for (String chapter : kokoroChapters(text)) out.addAll(kokoroAndroidSegments(chapter));
        return out;
    }

    private static int semanticCut(String text, int start, int max, int min) {
        int remaining = text.length() - start;
        if (remaining <= max) return text.length();
        int limit = Math.min(text.length(), start + max);
        int minimum = Math.min(limit, start + Math.max(1, min));
        int punctuation = backwardPunctuation(text, minimum, limit);
        if (punctuation > start) return punctuation;
        int whitespace = backwardWhitespace(text, minimum, limit);
        if (whitespace > start) return whitespace;
        return forwardBoundary(text, start, limit);
    }

    private static int androidCut(String text, int start) {
        int remaining = text.length() - start;
        if (remaining <= KOKORO_ANDROID_MAX) return text.length();
        int limit = Math.min(text.length(), start + KOKORO_ANDROID_MAX);
        int minimum = Math.min(limit, start + KOKORO_ANDROID_MIN);
        int punctuation = backwardPunctuation(text, minimum, limit);
        if (punctuation > start) return punctuation;
        int whitespace = backwardWhitespace(text, minimum, limit);
        if (whitespace > start) return whitespace;
        return forwardBoundary(text, start, limit);
    }

    private static int backwardPunctuation(String text, int minimum, int limit) {
        for (int i = Math.min(text.length(), limit) - 1; i >= minimum - 1; i--) {
            if (i < 0 || i >= text.length()) continue;
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '…' || c == ';' || c == ':') return i + 1;
        }
        return -1;
    }

    private static int backwardWhitespace(String text, int minimum, int limit) {
        for (int i = Math.min(text.length(), limit) - 1; i >= minimum - 1; i--) {
            if (i >= 0 && i < text.length() && Character.isWhitespace(text.charAt(i))) return i + 1;
        }
        return -1;
    }

    private static int forwardBoundary(String text, int start, int from) {
        int cursor = Math.max(start + 1, Math.min(text.length(), from));
        while (cursor < text.length()) {
            char previous = text.charAt(cursor - 1);
            if (Character.isWhitespace(previous) || previous == '.' || previous == '!' || previous == '?'
                    || previous == '…' || previous == ';' || previous == ':') return cursor;
            cursor++;
        }
        return text.length();
    }

    private static List<String> progressive(String clean, int firstLimit, int nextLimit) {
        ArrayList<String> out = new ArrayList<String>();
        String[] sentences = clean.split("(?<=[.!?…])\\s+");
        StringBuilder block = new StringBuilder();
        int limit = firstLimit;
        for (String sentence : sentences) {
            String part = sentence.trim();
            if (part.length() == 0) continue;
            if (block.length() > 0 && block.length() + 1 + part.length() > limit) {
                out.add(block.toString());
                block.setLength(0);
                limit = nextLimit;
            }
            if (part.length() > limit) {
                if (block.length() > 0) {
                    out.add(block.toString());
                    block.setLength(0);
                    limit = nextLimit;
                }
                splitLongPart(part, limit, nextLimit, out);
                limit = nextLimit;
            } else {
                if (block.length() > 0) block.append(' ');
                block.append(part);
            }
        }
        if (block.length() > 0) out.add(block.toString());
        if (out.isEmpty()) out.add(clean);
        return out;
    }

    private static void splitLongPart(String part, int firstLimit, int nextLimit, List<String> out) {
        int start = 0;
        int limit = firstLimit;
        while (start < part.length()) {
            int end = Math.min(part.length(), start + limit);
            if (end < part.length()) {
                int boundary = bestBoundary(part, start, end);
                if (boundary > start) end = boundary;
                else end = forwardBoundary(part, start, end);
            }
            String chunk = part.substring(start, end).trim();
            if (chunk.length() > 0) out.add(chunk);
            start = end;
            while (start < part.length() && Character.isWhitespace(part.charAt(start))) start++;
            limit = nextLimit;
        }
    }

    private static int bestBoundary(String text, int start, int end) {
        int minimum = start + Math.max(80, (end - start) / 2);
        for (int i = end; i >= minimum; i--) {
            char value = text.charAt(i - 1);
            if (Character.isWhitespace(value)) return i - 1;
        }
        return -1;
    }

    private static String normalize(String text) {
        return PortugueseNarrationSanitizer.sanitize(text == null ? "" : text)
                .replaceAll("\\s+", " ").trim();
    }
}
