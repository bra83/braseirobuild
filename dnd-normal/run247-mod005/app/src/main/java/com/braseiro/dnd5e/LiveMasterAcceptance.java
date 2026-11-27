package com.braseiro.dnd5e;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Locale;
import java.util.regex.Pattern;

/** Admission check of the actual response, including streamed/offline routes. */
final class LiveMasterAcceptance {
    private LiveMasterAcceptance(){}
    static boolean isOpening(String action){
        String a=FictionTurnIntent.fold(action);
        return a.contains("abertura da aventura principal importada")||
            (a.contains("inicio automatico")&&!a.contains("continuacao da cena existente"));
    }
    static boolean waystone(JSONObject scene,String action){
        String where=FictionTurnIntent.fold(scene==null?"":scene.optString("location",""));
        String a=FictionTurnIntent.fold(action);
        return where.contains("marco do percurso")||where.contains("waystone inn")||
            (isOpening(action)&&a.contains("pousada marco do percurso"));
    }
    static String canonicalInstruction(JSONObject scene,String action){
        if(!waystone(scene,action))return "";
        return "IDENTIDADE CANÔNICA VINCULANTE DA POUSADA: o proprietário e estalajadeiro é Kote; Bast é seu assistente. "
            +"Não substitua o dono por outro nome, inclusive Barl. Não invente presença: se o dono aparece ou responde, use Kote "
            +"em narration, scene_state.present_npcs e npc_updates. Respeite os fatos públicos da ficha e o conhecimento revelado. "
            +"Newarre é aldeia rural, não centro urbano. Não revele segredos.";
    }
    static String problem(String narration,JSONObject envelope,JSONObject prior,String action){
        String t=narration==null?"":narration.trim();
        JSONObject next=envelope==null?null:envelope.optJSONObject("scene_state");
        if(waystone(prior,action)||waystone(next,"")){
            if(Pattern.compile("(?iu)\\bBarl\\b").matcher(t).find())return "estalajadeiro da Pousada Marco do Percurso deve ser Kote, não Barl";
            if(wrongKeeper(next==null?null:next.optJSONArray("present_npcs"))||wrongKeeper(envelope==null?null:envelope.optJSONArray("npc_updates")))
                return "identidade estruturada do estalajadeiro diverge de Kote";
        }
        if(isOpening(action)){
            if(WorldOnboardingNarrativeGuard.countWords(t)<350||WorldOnboardingNarrativeGuard.countParagraphs(t)<4)
                return "abertura insuficiente: desenvolva a introdução e o gancho em pelo menos 350 palavras e quatro parágrafos, sem repetir ou inventar";
            if(!Pattern.compile("(?iu)\\b(vocês|você)\\b").matcher(t).find())
                return "abertura sem comunicação direta com os jogadores";
        }
        return LiveMasterProseGate.problem(t,envelope,prior,action);
    }
    private static boolean wrongKeeper(JSONArray a){
        if(a==null)return false;
        for(int i=0;i<a.length();i++){
            JSONObject n=a.optJSONObject(i);if(n==null)continue;
            String name=FictionTurnIntent.fold(n.optString("name"));
            String role=FictionTurnIntent.fold(n.optString("role")+" "+n.optString("summary"));
            if((role.contains("estalajadeiro")||role.contains("dono da pousada")||role.contains("proprietario da pousada"))
                &&!role.contains("assistente")&&!name.isEmpty()&&!name.equals("kote"))return true;
        }
        return false;
    }
}
