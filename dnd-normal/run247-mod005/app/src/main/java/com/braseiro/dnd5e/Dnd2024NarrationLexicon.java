package com.braseiro.dnd5e;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A narrow correction layer for GENERATED D&D 2024 narration and speech.
 * Do not use with rule excerpts, character names, character-sheet fields,
 * 2014 campaigns or the frozen Mystara profile.
 *
 * Corrections only target complete stand-alone tokens. This is not a general
 * spellchecker and must not rewrite words such as "orca", "gnômico" or "gnômica".
 */
public final class Dnd2024NarrationLexicon {
    private Dnd2024NarrationLexicon(){}

    private static final Pattern NONCANONICAL_SPECIES =
        Pattern.compile("(?iu)(?<![\\p{L}\\p{N}_])(?:orcos|orco|gnomias|gnomia)(?![\\p{L}\\p{N}_])");

    public static String correct(String narration){
        if(narration==null||narration.isEmpty())return narration==null?"":narration;
        Matcher found=NONCANONICAL_SPECIES.matcher(narration);
        StringBuffer out=new StringBuffer(narration.length());
        while(found.find()){
            String source=found.group();
            String folded=source.toLowerCase(java.util.Locale.ROOT);
            String replacement;
            if(folded.equals("orco"))replacement="orc";
            else if(folded.equals("orcos"))replacement="orcs";
            else if(folded.equals("gnomia"))replacement="gnoma";
            else replacement="gnomas";
            if(source.equals(source.toUpperCase(java.util.Locale.ROOT)))
                replacement=replacement.toUpperCase(java.util.Locale.ROOT);
            else if(Character.isUpperCase(source.codePointAt(0)))
                replacement=Character.toUpperCase(replacement.charAt(0))+replacement.substring(1);
            found.appendReplacement(out,Matcher.quoteReplacement(replacement));
        }
        found.appendTail(out);
        return out.toString();
    }
}
