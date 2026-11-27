package com.braseiro.dnd5e;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;
import java.util.regex.Pattern;

/** Final visible prose admission. No geography or dialogue invented by this guard. */
final class LiveMasterProseGate {
    private LiveMasterProseGate(){}
    private static final Pattern THIRD_PERSON=Pattern.compile("(?iu)\\b(?:a companhia|o grupo de aventureiros|os heróis|os aventureiros)\\s+(?:avança(?:m)?|chega(?:m)?|entra(?:m)?|observa(?:m)?|caminha(?:m)?|decide(?:m)?|segue(?:m)?|está|estão|encontra(?:m)?|percebe(?:m)?)\\b");
    static String problem(String narration,JSONObject envelope,JSONObject prior,String action){
        String text=narration==null?"":narration.trim();
        String input=action==null?"":action.toLowerCase(Locale.ROOT);
        StringBuilder prose=new StringBuilder();
        boolean speech=false;
        for(String line:text.split("\\r?\\n")){
            String trimmed=line.trim();
            if(trimmed.startsWith("—")||trimmed.matches("^[^:]{1,70}:\\s*[—–].*")||trimmed.startsWith("“")||trimmed.startsWith("\"")){speech=true;continue;}
            if(trimmed.contains("—")||trimmed.contains("“"))speech=true;
            prose.append(trimmed).append(' ');
        }
        if(THIRD_PERSON.matcher(prose).find()||prose.toString().toLowerCase(Locale.ROOT).contains("diante de a companhia"))
            return "voz em terceira pessoa para os heróis; dirija-se a vocês ou ao personagem pelo nome";
        JSONObject next=envelope==null?null:envelope.optJSONObject("scene_state");
        String loc=(prior==null?"":prior.optString("location",""))+" "+(next==null?"":next.optString("location",""));
        String lower=prose.toString().toLowerCase(Locale.ROOT);
        boolean inn=loc.toLowerCase(Locale.ROOT).contains("marco do percurso")||input.contains("local inicial obrigatório pousada marco do percurso");
        if(inn&&(lower.contains("bairros de comércio")||lower.contains("bairros centrais")||lower.contains("muralhas de cantaria")||lower.contains("praças centrais")||lower.contains("mercados abertos")))
            return "a estalagem rural foi convertida em centro urbano";
        JSONArray cast=next==null?null:next.optJSONArray("present_npcs");
        if(cast==null&&prior!=null)cast=prior.optJSONArray("present_npcs");
        if(cast==null&&prior!=null)try{cast=new JSONArray(prior.optString("present_npcs_json","[]"));}catch(Exception ignored){}
        boolean opening=LiveMasterAcceptance.isOpening(action);
        boolean social=FictionTurnIntent.classify(action,prior,"PLAYER_ACTION")==FictionTurnIntent.Kind.CHARACTER_SPEECH;
        if(!speech&&cast!=null&&cast.length()>0&&(opening||social))return "interação com NPC presente sem nenhuma fala direta; encene o interlocutor sem escrever a resposta do herói";
        return "";
    }
}
