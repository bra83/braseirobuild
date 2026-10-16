package com.braseiro.starwarsedge;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/** Pure-Java text preparation for TTS. The on-screen narration is never mutated. */
final class TtsChunker {
    static final int NEURAL_TARGET = 450;
    static final int NEURAL_MIN = 250;
    static final int NEURAL_MAX = 650;

    private TtsChunker() { }

    static String normalizeForAudio(String raw) {
        if (raw == null) return "";
        return Normalizer.normalize(raw, Normalizer.Form.NFC)
                .replace('\u00A0', ' ')
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll(" *\\n+ *", "\n")
                .trim();
    }

    static List<String> splitNeural(String raw) {
        String text = normalizeForAudio(raw);
        List<String> out = new ArrayList<>();
        while (!text.isEmpty()) {
            if (text.length() <= NEURAL_MAX) {
                out.add(text);
                break;
            }
            int cut = bestNaturalCut(text, NEURAL_MIN, NEURAL_MAX, NEURAL_TARGET);
            if (cut <= 0) cut = safeSpaceCut(text, NEURAL_MIN, NEURAL_MAX, NEURAL_TARGET);
            if (cut <= 0) cut = Math.min(NEURAL_MAX, text.length());
            String chunk = text.substring(0, cut).trim();
            if (!chunk.isEmpty()) out.add(chunk);
            text = text.substring(cut).trim();
        }
        return out;
    }

    static List<String> splitAndroid(String raw, int maxChars) {
        String text = normalizeForAudio(raw);
        List<String> out = new ArrayList<>();
        int max = Math.max(500, maxChars);
        while (!text.isEmpty()) {
            if (text.length() <= max) {
                out.add(text);
                break;
            }
            int min = Math.max(200, max / 2);
            int target = Math.max(min, Math.min(max - 1, (int) (max * 0.82)));
            int cut = bestNaturalCut(text, min, max, target);
            if (cut <= 0) cut = safeSpaceCut(text, min, max, target);
            if (cut <= 0) cut = Math.min(max, text.length());
            String chunk = text.substring(0, cut).trim();
            if (!chunk.isEmpty()) out.add(chunk);
            text = text.substring(cut).trim();
        }
        return out;
    }

    private static int bestNaturalCut(String text, int min, int max, int target) {
        char[][] groups = new char[][]{{'.', '!', '?'}, {';'}, {','}};
        int upper = Math.min(max, text.length() - 1);
        for (char[] group : groups) {
            int best = -1;
            int bestDistance = Integer.MAX_VALUE;
            for (int i = min; i < upper; i++) {
                char c = text.charAt(i);
                if (!contains(group, c)) continue;
                int cut = i + 1;
                if (cut < min || cut > max) continue;
                if (cut < text.length()) {
                    char next = text.charAt(cut);
                    if (!Character.isWhitespace(next) && next != '\"' && next != '\'' && next != ')' && next != ']') continue;
                }
                int distance = Math.abs(cut - target);
                if (distance < bestDistance || (distance == bestDistance && cut > best)) {
                    best = cut;
                    bestDistance = distance;
                }
            }
            if (best > 0) return best;
        }
        return -1;
    }

    private static int safeSpaceCut(String text, int min, int max, int target) {
        int upper = Math.min(max, text.length() - 1);
        int best = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = min; i <= upper; i++) {
            if (!Character.isWhitespace(text.charAt(i))) continue;
            int distance = Math.abs(i - target);
            if (distance < bestDistance || (distance == bestDistance && i > best)) {
                best = i;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static boolean contains(char[] values, char needle) {
        for (char value : values) if (value == needle) return true;
        return false;
    }
}
