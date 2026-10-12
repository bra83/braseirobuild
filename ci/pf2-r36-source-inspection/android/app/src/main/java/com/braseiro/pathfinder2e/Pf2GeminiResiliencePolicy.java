package com.braseiro.pathfinder2e;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Política PF2 de primeira chamada: modelo preferido + fallbacks distintos, sem duplicar modelo. */
public final class Pf2GeminiResiliencePolicy {
    private Pf2GeminiResiliencePolicy(){}
    public static List<String> textCandidates(String preferred,String lastKnownGood){
        LinkedHashSet<String> s=new LinkedHashSet<String>();
        add(s,preferred);add(s,lastKnownGood);add(s,"gemini-3.5-flash-lite");add(s,"gemini-3.5-flash");add(s,"gemini-2.5-flash-lite");
        return new ArrayList<String>(s);
    }
    private static void add(LinkedHashSet<String> s,String v){if(v!=null&&!v.trim().isEmpty())s.add(v.trim());}
}
