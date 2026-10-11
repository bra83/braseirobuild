package com.braseiro.dnd5e;

import org.json.JSONObject;

/** Fail-closed gate for a real, replay-safe guided class/species/feat action. */
public final class GuidedCharacterActionCommitGuard {
    public static final String PREFIX="guided-class-effect:";
    private GuidedCharacterActionCommitGuard() {}
    public static String reject(String channel,String actionId,long characterId,String kind,String action,String detail){
        if(!"PLAYER_REACTION".equals(channel))return "blocked_invalid_action_channel";
        if(actionId==null||actionId.length()<12||actionId.length()>96)return "blocked_invalid_action_id";
        for(int i=0;i<actionId.length();i++){char c=actionId.charAt(i);
            if(!((c>='a'&&c<='z')||(c>='A'&&c<='Z')||(c>='0'&&c<='9')||c=='_'||c=='-'))return "blocked_invalid_action_id";
        }
        if(characterId<1)return "blocked_missing_character";
        if(!("class".equals(kind)||"subclass".equals(kind)||"species".equals(kind)||"feat".equals(kind)||"invocation".equals(kind)))return "blocked_unknown_action_kind";
        if(action==null||action.trim().isEmpty()||action.length()>180)return "blocked_missing_action";
        if(detail!=null&&detail.length()>180)return "blocked_invalid_detail";
        if(("invocation".equals(kind)||"feat".equals(kind))&&(detail==null||detail.trim().isEmpty()))return "blocked_missing_feature_identity";
        return "";
    }
    public static boolean resolved(String kind,boolean explicitResolved, String status,boolean stateChanged){
        return statusAllowsCommit(explicitResolved,status,stateChanged);
    }

    /**
     * Stronger proof used before an action receipt may be minted. A resolver's
     * `*_action_resolved` flag and incidental sheet change are only a decision;
     * the persisted result must also carry its typed event and effect payload.
     */
    public static String commitRejection(String kind,boolean explicitResolved,String status,
            boolean persistedStateChanged,JSONObject result){
        if(!statusAllowsCommit(explicitResolved,status,persistedStateChanged))
            return "blocked_guided_action_not_committed";
        if(kind==null||kind.trim().isEmpty()||result==null)
            return "blocked_guided_action_missing_result";
        String event=result.optString(kind+"_action_event","").trim();
        JSONObject effect=result.optJSONObject(kind+"_action_effect");
        if(event.isEmpty()||effect==null||effect.length()==0)
            return "blocked_guided_action_missing_effect_proof";
        return "";
    }

    public static boolean committedEffect(String kind,boolean explicitResolved,String status,
            boolean persistedStateChanged,JSONObject result){
        return commitRejection(kind,explicitResolved,status,persistedStateChanged,result).isEmpty();
    }

    private static boolean statusAllowsCommit(boolean explicitResolved,String status,boolean stateChanged){
        if(!explicitResolved||!stateChanged)return false;
        if(status==null)return true;
        String s=status.toLowerCase(java.util.Locale.ROOT);
        return !(s.contains("blocked")||s.contains("pending")||s.contains("requires_")||s.contains("rejected")||s.contains("error")||s.contains("cancelled"));
    }
}
