package com.braseiro.dnd5e;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Preferred model always wins; Lite is the sole automatic text fallback. */
public final class GeminiTextModelPriority {
    public static final String FALLBACK="gemini-3.5-flash-lite";
    private GeminiTextModelPriority(){}

    public static List<String> ordered(String preferred){
        LinkedHashSet<String> result=new LinkedHashSet<String>();
        String selected=preferred==null?"":preferred.trim();
        if(selected.startsWith("models/"))selected=selected.substring(7);
        if(!selected.isEmpty())result.add(selected);
        result.add(FALLBACK);
        return new ArrayList<String>(result);
    }

    public static boolean isFallback(String preferred,String used){
        String selected=preferred==null?"":preferred.trim().replaceFirst("^models/","");
        String actual=used==null?"":used.trim().replaceFirst("^models/","");
        return !selected.isEmpty()&&!actual.isEmpty()&&!selected.equalsIgnoreCase(actual);
    }
}
