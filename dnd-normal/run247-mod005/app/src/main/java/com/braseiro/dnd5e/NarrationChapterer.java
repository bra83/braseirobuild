package com.braseiro.dnd5e;

import java.util.ArrayList;
import java.util.List;

/** Divide uma cena longa em capítulos estáveis para exibição e TTS. */
public final class NarrationChapterer {
    private NarrationChapterer() {}

    public static List<String> split(String raw, int targetChars) {
        ArrayList<String> out = new ArrayList<String>();
        String clean = PortugueseNarrationSanitizer.sanitize(raw == null ? "" : raw);
        clean = clean.replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
        if (clean.length() == 0) return out;
        int target = Math.max(320, Math.min(900, targetChars));
        int hardMax = Math.max(target + 180, Math.min(1150, target * 3 / 2));
        String[] paragraphs = clean.split("\\n\\s*\\n");
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            String p = paragraph.trim();
            if (p.length() == 0) continue;
            String[] sentences = p.split("(?<=[.!?…])\\s+");
            for (String sentence : sentences) {
                String s = sentence.trim();
                if (s.length() == 0) continue;
                if (s.length() > hardMax) {
                    flush(out, current);
                    splitLongSentence(out, s, target, hardMax);
                    continue;
                }
                if (current.length() > 0 && current.length() + 1 + s.length() > target) flush(out, current);
                if (current.length() > 0) current.append(' ');
                current.append(s);
                if (current.length() >= hardMax) flush(out, current);
            }
            if (current.length() >= target * 3 / 4) flush(out, current);
        }
        flush(out, current);
        if (out.isEmpty()) out.add(clean);
        return out;
    }

    private static void splitLongSentence(List<String> out, String text, int target, int hardMax) {
        String remaining = text.trim();
        while (remaining.length() > hardMax) {
            int cut = bestCut(remaining, target, hardMax);
            out.add(remaining.substring(0, cut).trim());
            remaining = remaining.substring(cut).trim();
        }
        if (remaining.length() > 0) out.add(remaining);
    }

    private static int bestCut(String text, int target, int hardMax) {
        int start = Math.min(text.length() - 1, Math.max(180, target - 120));
        int end = Math.min(text.length() - 1, hardMax);
        for (int i = end; i >= start; i--) {
            char c = text.charAt(i);
            if (c == ';' || c == ':' || c == ',' || Character.isWhitespace(c)) return Math.max(1, i + 1);
        }
        return Math.min(text.length(), hardMax);
    }

    private static void flush(List<String> out, StringBuilder current) {
        String value = current.toString().trim();
        if (value.length() > 0) out.add(value);
        current.setLength(0);
    }
}
