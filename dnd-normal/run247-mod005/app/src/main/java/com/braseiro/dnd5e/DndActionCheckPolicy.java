package com.braseiro.dnd5e;

import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Independent, conservative intent signal for the D&D rules gate.
 * This is NOT a d20 roll and does not assign DCs. The Master still decides
 * whether a permitted action is automatically resolved, impossible, or needs a
 * test; the local guard prevents silently narrating a definite resolution of
 * an unresolved consequential skill challenge.
 */
public final class DndActionCheckPolicy {
    private DndActionCheckPolicy(){}

    public static JSONObject frame(String action,JSONObject scene){
        JSONObject result=new JSONObject();
        try{
            String n=fold(action);
            boolean explicit=explicitTest(n);
            boolean challenge=consequentialChallenge(n,scene);
            String skill=suggestedSkill(n);
            result.put("declares_test",explicit)
                .put("uncertain_skill_challenge",challenge)
                .put("requires_adjudication",explicit||challenge)
                .put("suggested_skill",skill)
                .put("suggested_skill_is_authoritative",false)
                .put("dc_preselected",false)
                .put("narrative_constraint",(explicit||challenge)
                     ?"If consequential and possible, offer pending_test with justified ability/skill/DC and stop BEFORE outcome. If impossible, explain why. Do not narrate success or failure without an actual result."
                     :"Use automatic narration for trivial observation, ordinary conversation and safe travel; do not roll just to fill a quota.");
        }catch(Exception ignored){}
        return result;
    }

    /** An uncertainty signal, not an unconditional instruction to roll. */
    public static boolean consequentialChallenge(String action,JSONObject scene){
        String n=fold(action);
        if(n.isEmpty()||resolvedRoll(n)||isQuestion(n))return false;
        if(any(n,"tento decifrar","tento interpretar a charada","resolver a charada",
            "decifrar a charada","decifro a charada","descobrir a resposta da charada",
            "revelar o significado da inscricao","interpretar runas desconhecidas",
            "decifrar a mensagem","quebrar o codigo","quebrar a cifra"))return true;
        if(any(n,"tento furtar","roubo sem ser visto","furtar sem ser visto",
            "tento me esconder","escondo me","passar despercebido","passar sem ser visto",
            "infiltrar sem ser visto","disfarco meu interesse","enganar o guarda",
            "enganar a aluna","enganar os estudantes","tento enganar","tento despistar",
            "despistar a","despistar o","minto para convencer","blefar para convencer"))return true;
        if(any(n,"tento persuadir","tento convencer","convencer a","convencer o","convencer os",
            "persuadir a","persuadir o","intimido","tento intimidar","ameaço para",
            "tento arrombar","forcar fechadura","forcar a porta",
            "tento escalar","escaleo o muro","atravesso o precipicio","desarmar a armadilha",
            "desativo a armadilha"))return true;
        if(any(n,"investigar a pista escondida","procuro uma pista oculta",
            "procuro pistas escondidas","encontrar passagens secretas","identificar a armadilha",
            "examinar mecanismo oculto","investigo o esconderijo"))return true;
        return false;
    }

    public static boolean explicitTest(String input){
        String n=fold(input);
        if(n.isEmpty()||resolvedRoll(n)||isQuestion(n))return false;
        if(any(n,"nao quero rolar","sem teste","sem rolar","nao peço teste","nao faco teste"))
            return false;
        return any(n,"faco teste","faco um teste","realizo teste","realizo um teste",
              "quero rolar","vou rolar","tento rolar","rolo um d20","rolo d20",
              "teste de investigacao","teste de percepcao","teste de furtividade",
              "teste de enganacao","teste de persuasao","teste de arcanismo",
              "peco um teste","solicito um teste","vou fazer um teste",
              "quero fazer um teste","faco a rolagem","realizo a rolagem");
    }

    public static boolean unresolvedOutcomeClaimed(String narration){
        String n=fold(narration);
        if(n.isEmpty())return false;
        // A deferral statement does not excuse a later narration of success.
        // E.g. "É preciso rolar. Em seguida Kvothe decifra a charada" must
        // still be rejected. Only the outcome language below is authoritative
        // for detecting prematurely resolved consequential challenges.
        return any(n,"kvothe consegue","kvothe conseguiu","voce consegue","voce conseguiu",
             "kvothe descobre","voce descobre","kvothe decifra","voce decifra",
             "kvothe identifica","voce identifica","kvothe encontra","voce encontra",
             "kvothe resolve","voce resolve","consegue passar despercebido",
             "passa despercebido","ninguem percebe seu disfarce",
             "a aluna acredita","a aluna aceita","a guarda acredita","o guarda acredita",
             "seu blefe funciona","seu disfarce funciona",
             "a fechadura cede","a porta se abre com seu esforco",
             "a charada foi resolvida","a resposta da charada e",
             "o segredo revelado e",
             "kvothe falha","voce falha","kvothe fracassa","voce fracassa",
             "kvothe nao consegue","voce nao consegue","sem sucesso na tentativa");
    }

    public static boolean missingRequiredResolution(String action,JSONObject envelope,boolean rollRecorded){
        if(rollRecorded||resolvedRoll(fold(action)))return false;
        JSONObject pending=envelope==null?null:envelope.optJSONObject("pending_test");
        if(pending!=null&&pending.optBoolean("required",true))return false;
        if(explicitTest(action))return true;
        if(!consequentialChallenge(action,null))return false;
        return unresolvedOutcomeClaimed(envelope==null?"":envelope.optString("narration",""));
    }

    public static String suggestedSkill(String action){
        String n=fold(action);
        if(any(n,"charada","cifra","codigo","inscricao","mecanismo","investig","pista"))return "Investigação";
        if(any(n,"escond","furtiv","desperceb","infiltr"))return "Furtividade";
        if(any(n,"disfar","ment","engan","despist","blef"))return "Enganação";
        if(any(n,"persuad","convenc","negoci"))return "Persuasão";
        if(any(n,"intimid","ameaç"))return "Intimidação";
        if(any(n,"escal","arromb","forcar","empurr"))return "Atletismo";
        if(any(n,"armadilha","observo","perceb","escut"))return "Percepção";
        return "";
    }

    private static boolean isQuestion(String n){
        return n.startsWith("qual teste")||n.startsWith("que teste")||n.startsWith("qual pericia")
            ||n.startsWith("que pericia")||n.startsWith("posso fazer um teste")
            ||n.startsWith("preciso rolar")||n.startsWith("devo rolar")
            ||n.startsWith("qual seria")||n.startsWith("como posso");
    }
    private static boolean resolvedRoll(String n){
        return any(n,"mecanica local verificada","resultado de rolagem","rolagem registrada",
            "resultado de teste obrigatorio","mecanica_local_verificada");
    }
    private static boolean any(String n,String... terms){
        String h=" "+fold(n)+" ";
        for(String term:terms){
            String t=" "+fold(term)+" ";
            if(!t.trim().isEmpty()&&h.contains(t))return true;
        }
        return false;
    }
    private static String fold(String raw){
        if(raw==null)return "";
        return Normalizer.normalize(raw,Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+"," ").replaceAll("\\s+"," ").trim();
    }
}
