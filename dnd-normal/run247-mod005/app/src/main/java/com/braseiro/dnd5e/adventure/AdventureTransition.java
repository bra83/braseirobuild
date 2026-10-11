package com.braseiro.dnd5e.adventure;

import org.json.JSONObject;

/** Transição declarativa entre nós. Efeitos são dados, não regras D&D. */
public final class AdventureTransition {
    private final String id, targetNodeId, actionLabel, requiredFlag, requiredClue;
    private final boolean isFailForward;
    private final JSONObject checkOverride;
    private final JSONObject effects;

    public AdventureTransition(String id, String targetNodeId, String actionLabel,
                               String requiredFlag, String requiredClue, boolean isFailForward,
                               JSONObject checkOverride) {
        this(id,targetNodeId,actionLabel,requiredFlag,requiredClue,isFailForward,checkOverride,null);
    }

    public AdventureTransition(String id, String targetNodeId, String actionLabel,
                               String requiredFlag, String requiredClue, boolean isFailForward,
                               JSONObject checkOverride, JSONObject effects) {
        this.id=id!=null?id:"trans_default";
        this.targetNodeId=targetNodeId!=null?targetNodeId:"";
        this.actionLabel=actionLabel!=null?actionLabel:"";
        this.requiredFlag=requiredFlag!=null?requiredFlag:"";
        this.requiredClue=requiredClue!=null?requiredClue:"";
        this.isFailForward=isFailForward;
        this.checkOverride=checkOverride!=null?checkOverride:new JSONObject();
        this.effects=effects!=null?effects:new JSONObject();
    }

    public String getId(){return id;} public String getTargetNodeId(){return targetNodeId;}
    public String getActionLabel(){return actionLabel;} public String getRequiredFlag(){return requiredFlag;}
    public String getRequiredClue(){return requiredClue;} public boolean isFailForward(){return isFailForward;}
    public JSONObject getCheckOverride(){return checkOverride;} public JSONObject getEffects(){return effects;}

    public static AdventureTransition fromJson(JSONObject json){
        if(json==null)return new AdventureTransition("empty","","","","",false,null,null);
        return new AdventureTransition(json.optString("id","t_default"),json.optString("target_node_id",""),
                json.optString("action_label",""),json.optString("required_flag",""),json.optString("required_clue",""),
                json.optBoolean("is_fail_forward",false),json.optJSONObject("check_override"),json.optJSONObject("effects"));
    }

    public JSONObject toJson(){JSONObject o=new JSONObject();try{o.put("id",id).put("target_node_id",targetNodeId).put("action_label",actionLabel)
            .put("required_flag",requiredFlag).put("required_clue",requiredClue).put("is_fail_forward",isFailForward)
            .put("check_override",checkOverride).put("effects",effects);}catch(Exception ignored){}return o;}
}
