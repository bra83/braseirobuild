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

    /**
     * A pending check is not enough when narration already claims its outcome.
     * Remote repair is permitted to propose a check and justified DC, never
     * to roll a die or commit world state on the player's behalf.
     */
    public static boolean requiresRemoteCorrection(String action,JSONObject envelope){
        return isExplicitTestDeclaration(action)&&!isPreparedPendingCheck(envelope);
    }

    /** A short narration is correct when the output merely asks the player to roll.
     *  It must not be rejected by the prose-length or repetition gates. */
    public static boolean isPreparedPendingCheck(JSONObject envelope){
        if(envelope==null||envelope.optInt("protocol_version",0)!=MasterProtocolV1.VERSION)return false;
        JSONObject test=envelope.optJSONObject("pending_test");
        if(test==null||!test.optBoolean("required",true))return false;
        if(!test.has("dc")||test.isNull("dc")||test.optInt("dc",0)<5||test.optInt("dc",0)>30)return false;
        String kind=test.optString("type","ability");
        if(("ability".equalsIgnoreCase(kind)||kind.trim().isEmpty())
                &&test.optString("ability","").trim().isEmpty())return false;
        return !DndActionCheckPolicy.unresolvedOutcomeClaimed(envelope.optString("narration",""));
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
