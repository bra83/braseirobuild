package com.braseiro.dnd5e;

import java.util.Locale;

/** Explicit Gemini 3.8 voice direction; never mutates the persisted story text. */
public final class GeminiTtsPerformance {
    public static final String PREF_CHARACTER_DIFFERENCE="tts_gemini_character_difference_v1";
    private static final String[] PALETTE={"Kore","Puck","Fenrir","Aoede","Zephyr","Orus","Leda","Charon"};
    private GeminiTtsPerformance(){}
    public static int clamp(int value){return Math.max(0,Math.min(100,value));}
    public static boolean isCharacter(String speaker){
        return speaker!=null&&!speaker.trim().isEmpty()&&!"Narrador".equalsIgnoreCase(speaker.trim());
    }
    /**
     * 0-34: one voice with different acting at 25+;
     * 35-59: two possible NPC voices; 60-84: four; 85+: full palette.
     * Selection is stable by speaker; no voice roulette between chunks.
     */
    public static String voice(String narratorVoice,String suggestedNpcVoice,String speaker,int difference){
        String base=narratorVoice==null||narratorVoice.trim().isEmpty()?"Charon":narratorVoice.trim();
        int pct=clamp(difference);
        if(!isCharacter(speaker)||pct<35)return base;
        int limit=pct<60?2:pct<85?4:PALETTE.length;
        int slot=Math.floorMod(speaker.toLowerCase(Locale.ROOT).hashCode(),limit);
        String candidate=PALETTE[slot];
        if(candidate.equalsIgnoreCase(base))candidate=PALETTE[(slot+1)%limit];
        if(pct>=85&&suggestedNpcVoice!=null&&!suggestedNpcVoice.isEmpty()
                &&!suggestedNpcVoice.equalsIgnoreCase(base))return suggestedNpcVoice;
        return candidate;
    }
    /** Sent as speech_metadata.style, never printed or read as transcript. */
    public static String direction(String speaker,int difference){
        int pct=clamp(difference);
        if(!isCharacter(speaker)||pct==0)return "";
        String scope="Speaking the quoted dialogue of "+speaker.trim().replaceAll("[\\r\\n<>]"," ")+
                ". Maintain this character's recognizable performance across the story. ";
        if(pct<35)return scope+"Subtle difference from the narrator: small changes in rhythm, warmth and inflection, natural Brazilian Portuguese.";
        if(pct<60)return scope+"Moderately distinct character voice, clearly audible but natural, never a caricature. Brazilian Portuguese.";
        if(pct<85)return scope+"Pronounced character differentiation in pitch, rhythm and timbre, expressive dialogue, natural Brazilian Portuguese.";
        return scope+"Highly distinct theatrical characterization with marked intonation, contrast and dynamic prosody, but intelligible and consistent Brazilian Portuguese. No caricature.";
    }
    /** Only supported point-in-time Gemini 3.8 tags; do not invent reactions. */
    public static String taggedTranscript(String speech){
        if(speech==null)return "";
        String value=speech.trim();
        // Pauses stem only from an actual hesitation already written by the narrator.
        return value.replaceAll("(?s)\\.\\.\\.(?!\\s*<short pause>)(\\s+)", "... <short pause>$1");
    }
}
