package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/**
 * The source composer and diegetic intention are independent. A question in
 * PLAYER_ACTION remains inside the story unless it explicitly asks for rules.
 * No state is mutated here; only the existing mechanical host may commit.
 */
public final class FictionTurnIntent {
    private FictionTurnIntent() {}

    public enum Kind {
        FICTION_OBSERVATION, CHARACTER_SPEECH, CHARACTER_ACTION,
        RULES_HELP, MIXED_CONDITIONAL, AMBIGUOUS_REFERENCE
    }
    public enum Condition { NONE, TRUE, FALSE, UNKNOWN }

    public static Kind classify(String raw) {
        String s=fold(raw);
        if(s.isEmpty())return Kind.CHARACTER_ACTION;
        // A dependent question and conditional declaration cannot be split
        // into concurrent actions or silently discarded.
        if(s.contains(" se ") && (s.contains("?") || s.startsWith("se ")))
            return Kind.MIXED_CONDITIONAL;
        if(starts(s,"pergunto ","eu pergunto ","falo ","eu falo ","digo ",
                "eu digo ","respondo ","eu respondo ","questiono ","converso ",
                "eu converso ","sussurro ","abordo "))return Kind.CHARACTER_SPEECH;
        if(starts(s,"qual regra ","quais regras ","como funciona a regra ",
                "segundo as regras ","mestre qual regra ","mestre, qual regra ",
                "qual e a regra ","qual pericia posso usar segundo as regras ",
                "onde esta escrito ","qual livro explica ","qual cd a regra ",
                "em dnd ","no livro de regras ","o que diz a regra ",
                "que regra ","qual o modificador ","como se calcula ","como calcular "))
            return Kind.RULES_HELP;
        if(starts(s,"quero fazer um teste ","faco um teste ","vou fazer um teste ",
                "tento um teste ","examino ","eu examino ","procuro ","eu procuro ",
                "investigo ","eu investigo ","revisto ","vasculho ","tento ",
                "entro ","caminho ","ataco ","uso ","conjuro ","sigo ","me aproximo ",
                "abro ","desarmo ","pego ","vou ","quero ","eu quero "))
            return Kind.CHARACTER_ACTION;
        if(starts(s,"como esta ","como e ","como essa ","como esse ",
                "o que vejo ","o que eu vejo ","o que esta vestindo ",
                "que roupa ","esta armado","esta armada","ha armas visiveis",
                "posso ver ","consigo ver ","o que ha ","que horas ",
                "qual e a hora ","onde fica ","onde esta ","quem esta ",
                "quem e essa ","quem e esse ","qual a aparencia ","parece ",
                "qual o clima ","esta chovendo ","esta escuro ","o que consigo perceber ",
                "consigo ler ","tem alguma arma visivel ","o que esta acontecendo ",
                "tem algum teste ","ha algum teste ","posso tentar ",
                "qual o caminho ","quantos metros ","qual a distancia "))
            return Kind.FICTION_OBSERVATION;
        if(s.endsWith("?"))return Kind.FICTION_OBSERVATION;
        return Kind.CHARACTER_ACTION;
    }

    /** Main composer: direct address of a present NPC is dialogue, not observation. */
    public static Kind classify(String raw,JSONObject scene,String channel){
        if("GM_HELP".equals(channel))return Kind.RULES_HELP;
        Kind base=classify(raw);
        if(base==Kind.RULES_HELP||base==Kind.CHARACTER_SPEECH)return base;
        String q=fold(raw);
        if(q.isEmpty())return base;
        if(starts(q,"perguntar ","pergunta ao ","pergunta a ","pergunto ao ","pergunto a ",
                "conversar com ","falar com ","cumprimentar ","digo ao ","diga ao "))return Kind.CHARACTER_SPEECH;
        JSONArray cast=present(scene);
        if(cast.length()==0)return base;
        String original=raw==null?"":raw.trim();
        for(int i=0;i<cast.length();i++){
            JSONObject n=cast.optJSONObject(i);
            String name=fold(n==null?cast.optString(i,""):n.optString("name",""));
            if(name.isEmpty())continue;
            if(q.startsWith(name+" ")&&(original.regionMatches(true,0,name+",",0,name.length()+1)
                    || original.contains("?")))return Kind.CHARACTER_SPEECH;
        }
        // The player speaking in second person to somebody visible is distinct
        // from asking what the player can see or asking for a game rule.
        if(starts(q,"voce e ","voce tem ","voce sabe ","voce viu ","voce conhece ","voce pode ",
                "voces sao ","voces tem ","voces sabem ","voces viram ","o senhor ","a senhora ",
                "me diga ","me conte ","quanto custa ","quanto cobra ","qual seu nome","qual e seu nome",
                "qual o seu nome","e voce o dono","e o senhor o dono","e o dono","e a dona","tem quartos","tem quarto","tem comida","pode me dizer","pode nos dizer","bom dia","boa tarde","boa noite"))
            return Kind.CHARACTER_SPEECH;
        if(q.matches("^(ola |ei )?(estalajadeiro|taberneiro|hospedeiro|taverneiro) .+"))
            return Kind.CHARACTER_SPEECH;
        return base;
    }

    public static boolean freezesWorld(Kind kind, String raw, JSONObject scene) {
        if(kind==Kind.RULES_HELP||kind==Kind.FICTION_OBSERVATION
                ||kind==Kind.AMBIGUOUS_REFERENCE)return true;
        return kind==Kind.MIXED_CONDITIONAL && condition(raw,scene)!=Condition.TRUE;
    }

    /** Evaluate only stated rain predicates against the *committed* scene.
     * Unknown conditions never execute the dependent action.
     */
    public static Condition condition(String raw,JSONObject scene) {
        String s=fold(raw);
        if(!s.contains(" se ")&&!s.startsWith("se "))return Condition.NONE;
        if(!(s.contains("se chover")||s.contains("se estiver chovendo")
                ||s.contains("se chove")||s.contains("se houver chuva")))
            return Condition.UNKNOWN;
        if(scene==null)return Condition.UNKNOWN;
        String weather=fold(scene.optString("weather",""));
        if(weather.isEmpty()||weather.equals("desconhecido"))return Condition.UNKNOWN;
        return contains(weather,"chuva","chov","chuvisc","tempestade","garoa","aguaceiro")?
                Condition.TRUE:Condition.FALSE;
    }

    /** Resolve an on-stage demonstrative without inventing an interlocutor.
     * The map only supplies present identities. The character's knowledge of
     * their name is a separate property, not inferred from DB registration.
     */
    public static JSONObject resolveReferent(String question,JSONObject scene) {
        JSONObject result=new JSONObject();
        try {
            JSONArray present=present(scene);
            String q=fold(question);
            boolean demonstrative=contains(q,"essa pessoa","esse homem","essa mulher",
                    "esse orc","esse elfo","essa elfa","essa crianca","essa criatura",
                    "ela ","ele ","dele ","dela ","seu casaco","sua roupa");
            JSONArray matches=new JSONArray();
            for(int i=0;i<present.length();i++){
                JSONObject npc=present.optJSONObject(i);
                if(npc==null||npc.optBoolean("departed",false)||npc.optBoolean("left_scene",false))continue;
                String name=npc.optString("name","").trim();
                // Anonymous people can still be observed; DB registration is not discovery.
                String normalizedName=fold(name);
                if(!demonstrative && (normalizedName.isEmpty() || !q.contains(normalizedName)))continue;
                // Resolve *which visible person* without passing internal NPC IDs,
                // classified roles or an unknown canonical name to the model.
                // Presentation/knowledge is controlled by NpcPerceptionLedger.
                String lighting=fold(scene.optString("lighting",scene.optString("light","")));
                boolean dark=(contains(lighting,"escuridao","breu","darkness","no light")
                        && !scene.optBoolean("perception_visibility_ok",false));
                JSONObject visible=new JSONObject().put("scene_index",i)
                    .put("target_visible",!dark)
                    .put("visible_description",dark?"":npc.optString("visual_description",""));
                matches.put(visible);
            }
            result.put("demonstrative",demonstrative).put("candidates",matches)
                  .put("ambiguous",demonstrative&&matches.length()>1)
                  .put("unresolved",demonstrative&&matches.length()==0);
            if(matches.length()==1)result.put("target",matches.optJSONObject(0));
        } catch(Exception failure){try{result.put("unresolved",true).put("error",failure.getClass().getSimpleName());}catch(Exception ignored){}}
        return result;
    }

    public static JSONObject contract(String raw,JSONObject scene,String inputChannel){
        JSONObject result=new JSONObject();
        try{
            Kind kind=classify(raw,scene,inputChannel);
            JSONObject referent=resolveReferent(raw,scene);
            if(kind==Kind.FICTION_OBSERVATION&&referent.optBoolean("ambiguous",false))
                kind=Kind.AMBIGUOUS_REFERENCE;
            Condition condition=kind==Kind.MIXED_CONDITIONAL?condition(raw,scene):Condition.NONE;
            boolean frozen=freezesWorld(kind,raw,scene);
            result.put("kind",kind.name()).put("input_channel",inputChannel==null?"":inputChannel)
                .put("non_advancing",frozen).put("condition",condition.name())
                .put("referent",referent)
                .put("authority","Only accepted host state and visibility determine answers; model cannot set time, HP, identity, NPC secrets or geography.")
                .put("request",raw==null?"":raw);
        }catch(Exception e){try{result.put("kind","UNRESOLVED").put("non_advancing",true).put("error",e.getClass().getSimpleName());}catch(Exception ignored){}}
        return result;
    }

    public static JSONArray present(JSONObject scene){
        if(scene==null)return new JSONArray();
        Object raw=scene.opt("present_npcs");
        if(raw instanceof JSONArray)return (JSONArray)raw;
        try{return new JSONArray(scene.optString("present_npcs_json","[]"));}catch(Exception ignored){return new JSONArray();}
    }
    static boolean contains(String text,String... terms){for(String x:terms)if(text.contains(x))return true;return false;}
    static boolean starts(String text,String... terms){for(String x:terms)if(text.startsWith(x))return true;return false;}
    static String fold(String v) {
        if(v==null)return "";
        return Normalizer.normalize(v.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","").replaceAll("[^a-z0-9? ]+"," ")
            .replaceAll("\\s+"," ").trim();
    }
}
