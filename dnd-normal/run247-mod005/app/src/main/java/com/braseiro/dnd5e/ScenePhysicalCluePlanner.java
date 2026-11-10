package com.braseiro.dnd5e;

import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Choices for a clue that is physically being handled in the CURRENT beat.
 * Mere mention of a garden, shrub or magic object in dialogue is not physical
 * presence. This planner never declares a discovery or rolls for the player.
 */
public final class ScenePhysicalCluePlanner {
    private ScenePhysicalCluePlanner(){}

    public static JSONObject choice(String slot,String narration) {
        String n=fold(narration);
        // Current beat explicitly shows the vellum unfolded with written verses
        // and wax. Do not offer these interactions just because a character
        // quoted the words "folha" or "versos".
        if(!(n.contains("folha de velino")&&n.contains("versos")
               &&n.contains("cera")&&(n.contains("desenrolad")||n.contains("exibe"))))
            return null;
        String type=slot, label="", action="";
        if(GroundedSceneActionSlots.OBSERVAR.equals(slot)){
            label="LER OS VERSOS DO VELINO";
            action="Leio atentamente os versos na folha de velino, procurando compreender o texto sem presumir que decifrei o enigma.";
        }else if(GroundedSceneActionSlots.AGIR.equals(slot)){
            label="CONFERIR A OUTRA FACE DO VELINO";
            action="Viro cuidadosamente a folha de velino para verificar o outro lado, sem presumir que exista ali uma inscrição.";
        }else if(GroundedSceneActionSlots.DESPISTAR.equals(slot)){
            type="INVESTIGAR";
            label="COMPARAR OS VERSOS E A CERA";
            action="Comparo a disposição dos versos da folha com as marcas da cera, procurando uma relação sem assumir que haja uma pista.";
        }else return null;
        JSONObject o=new JSONObject();
        try{
            o.put("type",type).put("action_type",type)
              .put("label",type+" • "+label).put("action",action).put("description",action);
        }catch(Exception ignored){}
        return o;
    }
    private static String fold(String s){
        return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+"," ").trim();
    }
}
