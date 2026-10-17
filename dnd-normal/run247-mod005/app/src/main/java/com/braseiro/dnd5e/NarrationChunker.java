package com.braseiro.dnd5e;

import java.util.ArrayList;
import java.util.List;

/**
 * Segmentação progressiva para começar a falar cedo sem transformar uma cena
 * em dezenas de frases isoladas.
 */
public final class NarrationChunker {
    private static final int CHARON_SINGLE_BLOCK = 1800;
    private static final int CHARON_FIRST_BLOCK = 1200;
    private static final int CHARON_NEXT_BLOCK = 1800;
    private static final int ANDROID_SINGLE_BLOCK = 720;
    private static final int ANDROID_FIRST_BLOCK = 480;
    private static final int ANDROID_NEXT_BLOCK = 720;

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
                else {
                    int forward=end;
                    while(forward<part.length()&&!Character.isWhitespace(part.charAt(forward)))forward++;
                    end=forward<part.length()?forward:part.length();
                }
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
    // Paridade com o planejador nativo aprovado no Forbidden Lands R98:
    // primeiro bloco até 560 caracteres, demais até 760 e cortes naturais.
    // Uma palavra excepcionalmente longa nunca pode ser dividida.
    private static final int KOKORO_FIRST_MAX = 560;
    private static final int KOKORO_NEXT_MAX = 760;
    private static final int KOKORO_MIN_CUT = 350;
    private static final int ANDROID_UTTERANCE_SAFE_LIMIT = 3900;

    public static List<String> kokoro(String text) {
        String clean = normalize(text);
        ArrayList<String> chunks = new ArrayList<String>();
        int cursor = 0;
        while (cursor < clean.length()) {
            while (cursor < clean.length() && Character.isWhitespace(clean.charAt(cursor))) cursor++;
            if (cursor >= clean.length()) break;
            int maximum = chunks.isEmpty() ? KOKORO_FIRST_MAX : KOKORO_NEXT_MAX;
            int end = Math.min(clean.length(), cursor + maximum);
            int cut = end;
            if (end < clean.length()) {
                int minimum = Math.min(end, cursor + Math.min(KOKORO_MIN_CUT, maximum - 20));
                cut = kokoroNaturalBoundary(clean, minimum, end);
                if (cut < 0) {
                    cut = end;
                    while (cut < clean.length() && !Character.isWhitespace(clean.charAt(cut))) cut++;
                }
            }
            if (cut <= cursor || cut - cursor > ANDROID_UTTERANCE_SAFE_LIMIT)
                throw new IllegalArgumentException("Palavra de narração excede o limite do Android TTS.");
            String chunk = clean.substring(cursor, cut).trim();
            if (!chunk.isEmpty()) chunks.add(chunk);
            cursor = cut;
        }
        return chunks;
    }

    private static int kokoroNaturalBoundary(String text, int minimum, int end) {
        // Pontuação final (com aspas de fechamento), depois vírgulas e espaços.
        for (int i = end; i >= minimum; i--) {
            char c = text.charAt(i - 1);
            if (c != '.' && c != '!' && c != '?' && c != '…') continue;
            int close = i;
            while (close < text.length() && isClosingQuote(text.charAt(close))) close++;
            if (close <= end && (close == text.length() || Character.isWhitespace(text.charAt(close))))
                return close;
        }
        for (int i = end; i >= minimum; i--) {
            char c = text.charAt(i - 1);
            if ((c == ';' || c == ':' || c == ',')
                    && (i == text.length() || Character.isWhitespace(text.charAt(i))))
                return i;
        }
        for (int i = end; i >= minimum; i--)
            if (i < text.length() && Character.isWhitespace(text.charAt(i))) return i;
        return -1;
    }

    private static boolean isClosingQuote(char c) {
        return c == '"' || c == '\'' || c == '”' || c == '’'
                || c == '»' || c == ')' || c == '}' || c == ']';
    }

}
