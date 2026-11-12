package com.braseiro.dnd5e;

import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Last-mile safety gates: reference, physical reach, intent novelty, scene ticket. */
public final class SceneActionEvidence {
    private SceneActionEvidence(){}

    static String normalize(String s){
        return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+"," ").replaceAll("\\s+"," ").trim();
    }

    static String currentStage(String narration){
        if(narration==null)return "";
        // Dialogue may describe events far away. It is not evidence that a prop
        // is physically available in the current location.
        int dash=narration.indexOf('—');
        int quote=narration.indexOf('“');
        if(quote>=0&&(dash<0||quote<dash))dash=quote;
        return dash<0?narration:narration.substring(0,dash);
    }

    private static final String[] PHYSICAL_PROPS={
        "arbusto","arbustos","planta","plantas","pergaminho","pergaminhos",
        "velino","carta","cartas","mapa","mapas","livro","livros","papel",
        "porta","portas","ponte","ponte","cofre","lacre","manuscrito",
        "armario","armarios","painel","quadro","mesa","frasco","frascos",
        "bau","chave","chaves","selos","selo","prancheta"
    };
    private static final Pattern PHYSICAL_ACTION=Pattern.compile(
        "(?i)(?s).*\\b(?:observo|examino|inspeciono|aproximo|toco|pego|apanho|abro|viro|leio|interajo|reviro|vasculho|investigo|analiso|movo|empurro|puxo|checo|confiro|olho|manuseio|tento examinar|aproximo me)\\b.*");
    private static final Pattern REPORTED_REMOTE=Pattern.compile(
        "(?i)(?s).*\\b(?:ontem|antes|outro lugar|ha dias|costuma|teria|havia|outrora|contou|relatou|lembro|lembrava|longe|distante|ficam no jardim|la no jardim)\\b.*");

    static boolean physicallyGrounded(String narration,String action){
        String a=normalize(action),full=normalize(narration),stage=normalize(currentStage(narration));
        if(!PHYSICAL_ACTION.matcher(a).matches())return true;
        for(String prop:PHYSICAL_PROPS){
            // A mentioned location or topic is not necessarily the object of
            // the action: "observo Aisla quando fala dos livros" is valid.
            String pattern=".*\\b(?:observo|examino|inspeciono|aproximo|toco|pego|apanho|abro|viro|leio|interajo|reviro|vasculho|investigo|analiso|movo|empurro|puxo|checo|confiro|olho|manuseio)\\b(?:\\s+(?:me|de|do|da|dos|das|um|uma|o|a|os|as|ao|aos|no|na|nos|nas|com|sobre|diretamente|atentamente|cuidadosamente)){0,5}\\s+"+prop+"\\b.*";
            if(!a.matches(pattern))continue;
            if(has(stage,prop))continue;
            // Quoted/reported content establishes a topic, not reach.
            if(narration!=null&&(narration.indexOf('—')>=0||narration.indexOf('“')>=0))return false;
            if(REPORTED_REMOTE.matcher(full).matches())return false;
        }
        return true;
    }

    static boolean namedReferencesExist(String narration,String action){
        if(action==null)return false;
        String n=normalize(narration);
        Matcher m=Pattern.compile("(?U)\\b([A-ZÁÀÂÃÉÊÍÓÔÕÚÜÇ][\\p{Ll}]{2,})\\b").matcher(action);
        while(m.find()){
            String name=normalize(m.group(1));
            // Sentence-initial common verbs and inflection, plus the player.
            if(m.start()==0||name.equals("kvothe")||name.equals("eu")
                    ||name.equals("nao")||name.equals("sigo")
                    ||name.equals("pergunto")||name.equals("observo")
                    ||name.equals("examino")||name.equals("tento")
                    ||name.equals("decido")||name.equals("leio")
                    ||name.equals("presto")||name.equals("procuro")
                    ||name.equals("viro")||name.equals("confiro")
                    ||name.equals("comparo")||name.equals("permanejo")
                    ||name.equals("permaneco")||name.equals("escuto")
                    ||name.equals("quero")||name.equals("mantenho")
                    ||name.equals("evito"))continue;
            // If the specific capitalized name is not narrated, it is not an
            // actionable current NPC/location. Existing state alone is insufficient.
            if(!has(n,name))return false;
        }
        return true;
    }

    static boolean valid(JSONObject o,String narration){
        if(o==null||narration==null||narration.trim().isEmpty())return false;
        String action=o.optString("action","");
        if(action.trim().isEmpty())return false;
        if(StoryMomentumAction.isRequest(action))return true;
        return namedReferencesExist(narration,action) &&
               physicallyGrounded(narration,action) &&
               SceneOptionVisibleEvidence.supported(o,narration);
    }

    static boolean sameDecision(String a,String b){
        if(SceneOptionDiversifier.similar(a,b))return true;
        String fa=family(a),fb=family(b);
        if(!fa.equals(fb))return false;
        String aa=normalize(a),bb=normalize(b);
        if(fa.equals("inspect")||fa.equals("conceal")){
            // Inspecting same document by replacing "examino" with "analiso"
            // is never an independent option, even if phrased differently.
            String[] anchors={"charada","pergaminho","velino","documento","arbusto",
                "pista","mapa","livro","carta","lacre","selo","quadro","porta",
                "estudante","assistente","aluna","guarda","npc","silencio"};
            for(String anchor:anchors)if(has(aa,anchor)&&has(bb,anchor))return true;
        }
        return false;
    }

    static String family(String a){
        String n=normalize(a);
        if(n.matches(".*\\b(?:pergunto|questiono|converso|falo|respondo|indago|interrogo)\\b.*"))return "talk";
        if(n.matches(".*\\b(?:disfarco|escondo|oculto|finjo|despisto|discretamente)\\b.*"))return "conceal";
        if(n.matches(".*\\b(?:viajo|dirijo|vou|sigo|caminho|desloco|procuro o caminho)\\b.*"))return "travel";
        if(n.matches(".*\\b(?:viro|abro|fecho|pego|seguro|movo|toco|empurro|puxo)\\b.*"))return "handle";
        if(n.matches(".*\\b(?:observo|examino|analiso|inspeciono|confiro|comparo|investigo|vasculho|olho|leio)\\b.*"))return "inspect";
        if(n.matches(".*\\b(?:ataco|golpeio|conjuro)\\b.*"))return "combat";
        if(n.matches(".*\\b(?:aguardo|espero|permane[cç]o)\\b.*"))return "wait";
        return "other";
    }

    static boolean has(String context,String term){
        return (" "+normalize(context)+" ").contains(" "+normalize(term)+" ");
    }

    static String hash(String s){
        try{
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(
                normalize(s).getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder();
            for(int i=0;i<16;i++)out.append(String.format(Locale.ROOT,"%02x",bytes[i]&255));
            return out.toString();
        }catch(Exception e){return Integer.toHexString(normalize(s).hashCode());}
    }

    static JSONObject bind(JSONObject option,String narration){
        if(option==null)return new JSONObject();
        try{
            JSONObject safe=new JSONObject(option.toString());
            // Suggested actions express player intent only. Never allow model
            // metadata to authorize a check, set its DC, decide a roll, or
            // predeclare its mechanical outcome.
            for(String key:new String[]{"test","check","skill_check","saving_throw",
                    "roll","dice","dc","difficulty_class","target_number",
                    "result","success","failure","effects","mechanical_effects",
                    "state_patch","world_patch","resource_delta","time_delta"}){
                safe.remove(key);
            }
            safe.put("scene_fingerprint",hash(narration));
            safe.put("action_id",hash(narration+"|"+safe.optString("action")+"|"+safe.optString("action_type")));
            return safe;
        }catch(Exception e){return option;}
    }

    static boolean ticketMatches(JSONObject option,String narration){
        if(option==null)return false;
        String expected=hash(narration);
        String got=option.optString("scene_fingerprint","");
        if(!expected.equals(got))return false;
        return hash(narration+"|"+option.optString("action")+"|"+option.optString("action_type"))
            .equals(option.optString("action_id","")) &&
            valid(option,narration);
    }
}
