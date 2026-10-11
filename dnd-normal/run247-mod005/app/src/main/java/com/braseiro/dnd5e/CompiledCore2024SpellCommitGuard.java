package com.braseiro.dnd5e;

/** Strict typed gate for casting via the existing atomic 2024 spell executor. */
public final class CompiledCore2024SpellCommitGuard {
    private CompiledCore2024SpellCommitGuard() {}

    public static String reject(String channel,String family,String id,long characterId,long casterId,
                                String spell,int slotLevel,boolean hasPayload){
        if(!"PLAYER_REACTION".equals(channel))return "blocked_invalid_action_channel";
        if(!"core2024_spell".equals(family))return "blocked_invalid_spell_family";
        if(id==null||id.length()<12||id.length()>78)return "blocked_invalid_spell_action_id";
        for(int i=0;i<id.length();i++){
            char c=id.charAt(i);
            if(!((c>='a'&&c<='z')||(c>='A'&&c<='Z')||(c>='0'&&c<='9')||c=='_'||c=='-'))
                return "blocked_invalid_spell_action_id";
        }
        if(characterId<=0||casterId<=0)return "blocked_invalid_spell_caster";
        if(spell==null||spell.trim().isEmpty()||spell.length()>120)return "blocked_missing_compiled_spell";
        if(slotLevel<1||slotLevel>9)return "blocked_invalid_spell_slot_level";
        if(!hasPayload)return "blocked_missing_spell_request";
        return "";
    }

    public static String token(String actionId){return "compiled-2024-"+actionId;}

    public static boolean persisted(boolean committed,boolean characterExists,
                                    boolean operationReceiptExists,boolean combatOutcomePresent){
        return committed&&characterExists&&operationReceiptExists&&combatOutcomePresent;
    }
}
