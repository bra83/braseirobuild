package com.braseiro.dnd5e.adventure;

import org.json.JSONObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of the provider-neutral roll intent contract [[ROLL:...]] from the
 * MIT IPedrax/AetherTable project. We parse the AI request but NEVER trust
 * the AI to supply a die result or mutate the character state.
 */
public final class AetherTableRollIntentPort {
    private static final Pattern TAG = Pattern.compile("\\[\\[ROLL:([^\\]\\r\\n]{1,50})\\]\\]");
    private static final Pattern D20 = Pattern.compile("1d20([+-][0-9]{1,2})?",Pattern.CASE_INSENSITIVE);
    private AetherTableRollIntentPort() {}
    /** AetherTable tag converted into a safe d20 check specification. */
    public static JSONObject parseDndRequest(String narration,int dc){
        if(narration==null||narration.length()>3000 || dc<0 || dc>99)
            return error("Narração ou CD inválida.");
        Matcher finder=TAG.matcher(narration);
        if(!finder.find())return error("A resposta não contém pedido de rolagem AetherTable.");
        String raw=finder.group(1).trim();
        if(finder.find())return error("Múltiplas rolagens exigem propostas separadas.");
        Matcher match=D20.matcher(raw);
        if(!match.matches())return error("O adaptador D&D exige [[ROLL:1d20+N]]; outra família usa seu motor próprio.");
        int modifier=match.group(1)==null?0:Integer.parseInt(match.group(1));
        return new JSONObject().put("status","recognized").put("dice_expression",raw)
            .put("requires_player_input",true).put("auto_rolls",false)
            .put("spec",new JSONObject().put("dc",dc).put("modifier",modifier));
    }
    private static JSONObject error(String m){return new JSONObject().put("status","blocked").put("error",m);}
}
