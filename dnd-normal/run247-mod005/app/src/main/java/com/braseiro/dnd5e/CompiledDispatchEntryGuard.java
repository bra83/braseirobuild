package com.braseiro.dnd5e;

/** Fail-closed identity validation shared by compiled class, resource and spell actions. */
public final class CompiledDispatchEntryGuard {
    private CompiledDispatchEntryGuard() {}
    public static String reject(String channel,String actionId){
        if(!"PLAYER_REACTION".equals(channel))return "blocked_invalid_action_channel";
        if(actionId==null||actionId.length()<12||actionId.length()>96)
            return "blocked_invalid_action_id";
        for(int i=0;i<actionId.length();i++){
            char c=actionId.charAt(i);
            if(!((c>='0'&&c<='9')||(c>='a'&&c<='z')||(c>='A'&&c<='Z')||c=='-'||c=='_'))
                return "blocked_invalid_action_id";
        }
        return "";
    }
}
