package com.braseiro.pathfinder2e;

import org.json.JSONObject;

/** Varia o tamanho das batidas por função, temperatura e cadência da campanha. */
public final class NarrativePacingDirector {
    private NarrativePacingDirector(){}

    public static JSONObject plan(String userText,String sceneMode,JSONObject scene,int beatNo,boolean economy,JSONObject worldBeat,boolean opening,boolean nonAdvancing){
        JSONObject out=new JSONObject();
        try{
            if(nonAdvancing)return out.put("kind","clarification").put("min_words",8).put("max_words",120).put("paragraphs","1-3").put("instruction","Responda diretamente; não crie ambientação nem acontecimento novo.");
            if(opening)return out.put("kind","opening").put("min_words",320).put("max_words",650).put("paragraphs","3-5").put("instruction","Apresente espaço, atividade, pessoas e implicação; não preencha por cota.");
            String function=BraseiroNarrativeDirector.sceneFunction(userText,scene);int temperature=BraseiroNarrativeDirector.sceneTemperature(scene,userText);String wb=worldBeat==null?"":worldBeat.optString("intensity","");
            int min,max;String paragraphs="1-3";
            if("combate".equals(sceneMode)||"combate".equals(function)){min=35;max=130;paragraphs="1-2";}
            else if("conversa".equals(sceneMode)||"diálogo".equals(function)||"negociação".equals(function)||"interrogatório".equals(function)){min=35;max=145;paragraphs="1-3";}
            else if("viagem".equals(sceneMode)||"viagem".equals(function)){min=55;max=190;paragraphs="1-3";}
            else if("chegada".equals(function)){min=120;max=280;paragraphs="2-4";}
            else if("investigação".equals(function)){min=80;max=210;paragraphs="1-4";}
            else {int phase=Math.floorMod(beatNo,4);int[][] bands={{55,105},{85,155},{120,225},{65,135}};min=bands[phase][0];max=bands[phase][1];}
            if("acontecimento".equals(wb)){min+=35;max+=80;paragraphs="2-4";}else if("sinal".equals(wb)){min+=15;max+=45;}
            if(temperature>=4){min=Math.max(35,min-20);max=Math.min(max,175);}else if(temperature<=1&&"chegada".equals(function)){max+=40;}
            if(economy){min=Math.max(25,(int)Math.round(min*0.80d));max=Math.max(min+25,(int)Math.round(max*0.82d));}
            out.put("kind",function).put("min_words",min).put("max_words",max).put("paragraphs",paragraphs).put("temperature",temperature).put("beat_phase",Math.floorMod(beatNo,4));
            out.put("instruction","Comprimento segue a importância da batida, não uma faixa fixa. Varie visivelmente em relação às cenas recentes; pare no primeiro ponto de decisão real. A faixa é orientação, nunca meta de preenchimento.");
        }catch(Exception ignored){}
        return out;
    }

    public static String instruction(JSONObject plan){if(plan==null||plan.length()==0)return "";return "RITMO LOCAL DESTA BATIDA: função="+plan.optString("kind","cena")+", normalmente "+plan.optInt("min_words",40)+"–"+plan.optInt("max_words",180)+" palavras, "+plan.optString("paragraphs","1-3")+" parágrafo(s). "+plan.optString("instruction","");}
}
