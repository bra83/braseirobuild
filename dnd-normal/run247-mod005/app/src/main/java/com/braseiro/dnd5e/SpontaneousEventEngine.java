package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;
import java.util.Random;

/** RUN187: deterministic event proposals; never directly mutates game state. */
public final class SpontaneousEventEngine {
    private SpontaneousEventEngine() {}
    private static final String[] CATEGORIES={"social","ambiente","humor","mistério","cotidiano","oportunidade","consequência"};
    private static final String[] DEFAULT={
        "Uma pessoa tem um pequeno contratempo social sem relação necessária com a missão.",
        "Uma mudança menor no ambiente interrompe a rotina sem criar dano nem condição.",
        "Um mal-entendido entre personagens locais pode render humor contextual.",
        "Surge uma peculiaridade ambígua; não revele segredos canônicos.",
        "Uma atividade rotineira de habitantes acontece sem envolver obrigatoriamente o grupo.",
        "Surge uma oportunidade plausível que pode ser ignorada sem penalidade.",
        "Um compromisso ou consequência JÁ presente na memória reaparece; não invente dívida nova."
    };
    public static JSONObject candidate(JSONObject state, String location, String world, String sceneFingerprint,
                                       boolean protectedScene, boolean nonAdvancing) {
        JSONObject out=new JSONObject();
        if(protectedScene || nonAdvancing) return out;
        int turns=state.optInt("turns",0), cooldown=state.optInt("event_cooldown",0);
        double pressure=state.optDouble("surprise_pressure",0.20);
        if(cooldown>0 || pressure<0.36) return out;
        long seed=(((long)sceneFingerprint.hashCode())<<32) ^ location.hashCode() ^ (turns*104729L);
        Random r=new Random(seed);
        if(r.nextDouble()>Math.min(0.72,pressure)) return out;
        JSONArray recent=DynamicMasterState.recent(state,"recent_events");
        int index=r.nextInt(CATEGORIES.length);
        for(int j=0;j<CATEGORIES.length;j++){
            int candidate=(index+j)%CATEGORIES.length;
            if(!CATEGORIES[candidate].equals(recent.optString(0,""))){index=candidate;break;}
        }
        String category=CATEGORIES[index], idea=DEFAULT[index];
        String w=world.toLowerCase(Locale.ROOT);
        if(w.contains("strixhaven")||w.contains("campus")){
            if("humor".equals(category))idea="Em um momento apropriado, um incidente mágico escolar pequeno e embaraçoso poderia interromper a rotina, sem mudar provas ou marcos oficiais.";
            if("cotidiano".equals(category))idea="Mostre uma atividade de estudantes ou funcionários fora da missão, respeitando calendário e relações.";
        }else if(w.contains("mystara")||w.contains("ose")){
            if("humor".equals(category))idea="Permita humor raro de superstição, tropeço social ou acompanhante, sem alterar mecânicas BECMI.";
        }else if(w.contains("cyberpunk")){
            if("humor".equals(category))idea="Um anúncio inconveniente ou bug de equipamento CENOGRÁFICO pode gerar contraste; não aplicar defeito mecânico.";
        }
        try{out.put("category",category).put("idea",idea).put("kind","suggestion_only")
            .put("requires_canonical_guard",true).put("requires_state_guard",true).put("scene_fingerprint",sceneFingerprint);}
        catch(Exception ignored){}
        return out;
    }
}
