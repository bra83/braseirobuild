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
    public static List<String> kokoro(String text) {
        String clean = normalize(text);
        ArrayList<String> out = new ArrayList<String>();
        if (clean.length() == 0) return out;
        if (clean.length() <= 650) { out.add(clean); return out; }
        int start = 0;
        while (start < clean.length()) {
            int remaining = clean.length() - start;
            if (remaining <= 650) { out.add(clean.substring(start).trim()); break; }
            int target = Math.min(clean.length(), start + 450);
            int min = Math.min(clean.length(), start + 250);
            int max = Math.min(clean.length(), start + 650);
            int cut = bestNaturalBreak(clean, min, target, max);
            if (cut <= start) cut = max;
            String chunk = clean.substring(start, cut).trim();
            if (chunk.length() > 0) out.add(chunk);
            start = cut; while (start < clean.length() && Character.isWhitespace(clean.charAt(start))) start++;
        }
        return out;
    }

    private static int bestNaturalBreak(String text, int min, int target, int max) {
        char[][] priorities = {{'.','!','?','…'},{';'},{','},{' '}};
        for (char[] group : priorities) {
            int best = -1, bestDistance = Integer.MAX_VALUE;
            for (int i = min; i < max && i < text.length(); i++) {
                char ch = text.charAt(i); boolean match = false; for (char p : group) if (ch == p) { match = true; break; }
                if (!match) continue; int cut = i + 1; int d = Math.abs(cut - target); if (d < bestDistance) { best = cut; bestDistance = d; }
            }
            if (best > 0) return best;
        }
        return max;
    }

}
