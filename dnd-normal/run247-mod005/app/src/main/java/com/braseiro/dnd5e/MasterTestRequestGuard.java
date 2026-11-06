package com.braseiro.dnd5e;

import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Fail-closed gate for explicitly requested dice checks. An LLM's description
 * of success cannot substitute for a pending test and a real player roll.
 */
public final class MasterTestRequestGuard {
    private MasterTestRequestGuard(){}

    public static boolean wouldResolveWithoutRoll(String playerAction,JSONObject envelope,boolean rollAlreadyRecorded){
        if(rollAlreadyRecorded)return false;
        if(DndActionCheckPolicy.missingRequiredResolution(playerAction,envelope,false))return true;
        if(!isExplicitTestDeclaration(playerAction))return false;
        JSONObject test=envelope==null?null:envelope.optJSONObject("pending_test");
        return test==null||!test.optBoolean("required",true);
    }

    static boolean isExplicitTestDeclaration(String action){
        if(action==null)return false;
        String original=action.trim();
        if(original.isEmpty()||original.length()>600)return false;
        String n=fold(original);
        if(n.startsWith("consulta auxiliar")||n.startsWith("resultado de rolagem")
            ||n.startsWith("o jogador confirma")||n.startsWith("inicio automatico"))
            return false;
        boolean declarative=n.startsWith("eu ")||n.startsWith("quero ")||n.startsWith("vou ")
            ||n.startsWith("tento ")||n.startsWith("faco ")||n.startsWith("realizo ")
            ||n.startsWith("rolarei ")||n.startsWith("rolo ")||n.startsWith("peco ")
            ||n.startsWith("solicito ");
        if(!declarative)return false;
        return n.matches("(?s).*\\b(?:teste|testar|rolar|rolagem|rolo|pericia|checagem)\\b.*");
    }
    private static String fold(String v){
        return Normalizer.normalize(v,Normalizer.Form.NFD).replaceAll("\\p{M}+","")
            .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();
    }
}
