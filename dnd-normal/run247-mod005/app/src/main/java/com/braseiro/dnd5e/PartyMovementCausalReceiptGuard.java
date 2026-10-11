package com.braseiro.dnd5e;

import java.util.Map;

/**
 * RUN205: movement/standing is an executed rule, not a narrated movement.
 * The receipt is minted only after the movement budget/condition engine
 * successfully commits in the SAME SQLite transaction as the party action.
 */
public final class PartyMovementCausalReceiptGuard {
    private PartyMovementCausalReceiptGuard() {}
    public static final String PREFIX="party-movement-v1:";
    public static final String SOURCE_PREFIX="party_movement_v1:";

    public static String bindingBlock(long actionId,long characterId,String status,String intent,
            long actorId,long actorCharacterId,boolean player,int feet,boolean standing,String token) {
        if(actionId<=0||characterId<=0)return "action_not_found";
        if(!"narrated".equals(status))return "action_not_narrated";
        if(!("movimento".equals(intent)||"livre".equals(intent)))return "action_intent_not_movement";
        if(!player||actorId<=0||actorCharacterId!=characterId)return "actor_character_mismatch";
        if(!standing&&(feet<=0||feet>600))return "invalid_distance";
        if(standing&&feet!=0)return "stand_cannot_charge_distance_twice";
        if(!CombatActionReceiptGuard.validToken(token)||token.length()>112)return "invalid_operation_token";
        return "";
    }
    public static String key(long actionId,long actorId,String token){
        if(actionId<=0||actorId<=0||!CombatActionReceiptGuard.validToken(token)||token.length()>112)
            throw new IllegalArgumentException("invalid_movement_receipt_key");
        return PREFIX+actionId+":"+actorId+":"+token;
    }
    public static String source(long characterId,long actorId,boolean standing,int feet){
        if(characterId<=0||actorId<=0||(standing?feet!=0:(feet<=0||feet>600)))
            throw new IllegalArgumentException("invalid_movement_receipt_source");
        return SOURCE_PREFIX+characterId+":"+actorId+":"+(standing?"stand":"move")+":"+feet;
    }
    public static final class Proof {
        public final long actionId,actorId,characterId;
        public final String token,kind;
        public final int feet;
        private Proof(long action,long actor,long character,String token,String kind,int feet){
            this.actionId=action;this.actorId=actor;this.characterId=character;
            this.token=token;this.kind=kind;this.feet=feet;
        }
    }
    public static Proof proof(long expectedActionId,long expectedCharacterId,String key,String source){
        if(key==null||source==null||!key.startsWith(PREFIX)||!source.startsWith(SOURCE_PREFIX))return null;
        String[] k=key.split(":",-1),s=source.split(":",-1);
        if(k.length!=4||s.length!=5)return null;
        try{
            long action=Long.parseLong(k[1]),actor=Long.parseLong(k[2]);
            long character=Long.parseLong(s[1]),sourceActor=Long.parseLong(s[2]);
            int feet=Integer.parseInt(s[4]);
            if(action!=expectedActionId||actor<=0||character!=expectedCharacterId||character<=0||
                sourceActor!=actor||!("move".equals(s[3])||"stand".equals(s[3]))||
                ("stand".equals(s[3])?feet!=0:(feet<=0||feet>600))||
                !CombatActionReceiptGuard.validToken(k[3])||k[3].length()>112)return null;
            return new Proof(action,actor,character,k[3],s[3],feet);
        }catch(NumberFormatException invalid){return null;}
    }
    public static String remapKey(String key,Map<Long,Long> actions,Map<Long,Long> combatants){
        if(key==null||!key.startsWith(PREFIX))return key;
        String[] parts=key.split(":",-1);if(parts.length!=4)return null;
        try{
            Long action=actions==null?null:actions.get(Long.parseLong(parts[1]));
            Long actor=combatants==null?null:combatants.get(Long.parseLong(parts[2]));
            return action==null||actor==null?null:key(action,actor,parts[3]);
        }catch(IllegalArgumentException invalid){return null;}
    }
    public static String remapSource(String source,Map<Long,Long> characters,Map<Long,Long> combatants){
        if(source==null||!source.startsWith(SOURCE_PREFIX))return source;
        String[] s=source.split(":",-1);if(s.length!=5)return null;
        try{
            Long ch=characters==null?null:characters.get(Long.parseLong(s[1]));
            Long actor=combatants==null?null:combatants.get(Long.parseLong(s[2]));
            if(ch==null||actor==null)return null;
            String kind=s[3];int feet=Integer.parseInt(s[4]);
            if(!("move".equals(kind)||"stand".equals(kind)))return null;
            return source(ch,actor,"stand".equals(kind),feet);
        }catch(IllegalArgumentException invalid){return null;}
    }
}
