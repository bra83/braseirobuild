package com.braseiro.pathfinder2e;

import java.util.ArrayList;
import java.util.List;

/**
 * Segmentação de voz orientada à continuidade e à cota. O Charon recebe blocos
 * semânticos suficientemente longos para manter naturalidade, mas não a cena inteira.
 * O próximo bloco é solicitado durante a reprodução do atual, com espaçamento, para
 * evitar rajadas de requests e também não deixar a fala parar no primeiro ponto.
 */
public final class NarrationChunker {
    private static final int CHARON_SINGLE_BLOCK = 1700;
    private static final int CHARON_FIRST_BLOCK = 1300;
    private static final int CHARON_NEXT_BLOCK = 2200;
    private static final int ANDROID_SINGLE_BLOCK = 720;
    private static final int ANDROID_FIRST_BLOCK = 480;
    private static final int ANDROID_NEXT_BLOCK = 720;
    public static final int KOKORO_MIN_BLOCK = 250;
    public static final int KOKORO_TARGET_BLOCK = 450;
    public static final int KOKORO_MAX_BLOCK = 650;

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

    /** Perfil neural local: preserva várias frases no mesmo bloco e escolhe
     * fronteiras naturais perto de 450 caracteres. Nunca altera o texto visual. */
    public static List<String> kokoro(String text) {
        String clean = normalize(text);
        ArrayList<String> out = new ArrayList<String>();
        if (clean.length() == 0) return out;
        int start = 0;
        while (start < clean.length()) {
            while (start < clean.length() && Character.isWhitespace(clean.charAt(start))) start++;
            if (start >= clean.length()) break;
            int remaining = clean.length() - start;
            if (remaining <= KOKORO_MAX_BLOCK) { out.add(clean.substring(start).trim()); break; }
            int low = Math.min(clean.length(), start + KOKORO_MIN_BLOCK);
            int target = Math.min(clean.length(), start + KOKORO_TARGET_BLOCK);
            int high = Math.min(clean.length(), start + KOKORO_MAX_BLOCK);
            int end = nearestBoundary(clean, low, target, high, new char[]{'.','!','?'}, true);
            if (end < 0) end = nearestBoundary(clean, low, target, high, new char[]{';',','}, false);
            if (end < 0) end = nearestWhitespace(clean, low, target, high);
            if (end < 0) end = high;
            String part = clean.substring(start, end).trim();
            if (part.length() > 0) out.add(part);
            start = end;
        }
        if (out.isEmpty()) out.add(clean);
        return out;
    }

    private static int nearestBoundary(String text,int low,int target,int high,char[] chars,boolean requireSpaceAfter){
        int best=-1,bestDistance=Integer.MAX_VALUE;
        for(int i=low;i<high;i++){
            char c=text.charAt(i);boolean match=false;for(char x:chars)if(c==x){match=true;break;}if(!match)continue;
            if(requireSpaceAfter&&i+1<text.length()&&!Character.isWhitespace(text.charAt(i+1)))continue;
            int boundary=i+1,d=Math.abs(boundary-target);if(d<bestDistance){best=boundary;bestDistance=d;}
        }
        return best;
    }

    private static int nearestWhitespace(String text,int low,int target,int high){
        int best=-1,bestDistance=Integer.MAX_VALUE;
        for(int i=low;i<high;i++)if(Character.isWhitespace(text.charAt(i))){int d=Math.abs(i-target);if(d<bestDistance){best=i;bestDistance=d;}}
        return best;
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
}
