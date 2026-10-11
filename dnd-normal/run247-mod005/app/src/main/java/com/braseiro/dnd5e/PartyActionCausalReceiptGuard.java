package com.braseiro.dnd5e;

/**
 * RUN204: causal, fail-closed binding between a narrated declaration and the
 * mechanical combat operation performed by its actual hero. Pure JVM logic.
 * The attestation is minted INSIDE the SQLite transaction that spends the turn
 * resource and applies every target effect; narrative text is never proof.
 */
public final class PartyActionCausalReceiptGuard {
    private PartyActionCausalReceiptGuard() {}
    public static final String PREFIX="party-combat-v2:";
    public static final String SOURCE_PREFIX="party_combat_v2:";

    public static String bindingBlock(long actionId,long characterId,String status,String intent,
        long actorId,long actorCharacterId,boolean player,String resource,String token){
        if(actionId<=0||characterId<=0)return "action_not_found";
        if(!"narrated".equals(status))return "action_not_narrated";
        if(!("combate".equals(intent)||"livre".equals(intent)))return "action_intent_not_combat";
        if(!player||actorId<=0||actorCharacterId!=characterId)return "actor_character_mismatch";
        if(!validResource(resource))return "turn_resource_required";
        if(!CombatActionReceiptGuard.validToken(token)||token.length()>112)return "invalid_operation_token";
        return "";
    }
    public static boolean validResource(String resource){
        return "action".equals(resource)||"bonus_action".equals(resource)||"reaction".equals(resource);
    }
    public static String key(long actionId,long actorId,String token){
        if(actionId<=0||actorId<=0||!CombatActionReceiptGuard.validToken(token)||token.length()>112)
            throw new IllegalArgumentException("invalid_party_receipt_key");
        return PREFIX+actionId+":"+actorId+":"+token;
    }
    public static String source(long characterId,long actorId,String kind,String resource,int count){
        if(characterId<=0||actorId<=0||!("effects".equals(kind)||"saves".equals(kind))||
            !validResource(resource)||count<1||count>64)throw new IllegalArgumentException("invalid_party_receipt_source");
        return SOURCE_PREFIX+characterId+":"+actorId+":"+kind+":"+resource+":"+count;
    }
    /** Backup v21: rebind physical row IDs without changing a single effect token. */
    public static String remapKey(String key,java.util.Map<Long,Long> actions,
        java.util.Map<Long,Long> combatants){
        if(key==null||!key.startsWith(PREFIX))return key;
        String[] parts=key.split(":",-1);if(parts.length!=4)return null;
        try{
            long oldAction=Long.parseLong(parts[1]),oldActor=Long.parseLong(parts[2]);
            Long action=actions==null?null:actions.get(oldAction);
            Long actor=combatants==null?null:combatants.get(oldActor);
            if(action==null||actor==null)return null;
            return key(action.longValue(),actor.longValue(),parts[3]);
        }catch(IllegalArgumentException invalid){return null;}
    }
    public static String remapSource(String source,java.util.Map<Long,Long> characters,
        java.util.Map<Long,Long> combatants){
        if(source==null||!source.startsWith(SOURCE_PREFIX))return source;
        String[] parts=source.split(":",-1);if(parts.length!=6)return null;
        try{
            Long character=characters==null?null:characters.get(Long.parseLong(parts[1]));
            Long actor=combatants==null?null:combatants.get(Long.parseLong(parts[2]));
            if(character==null||actor==null)return null;
            return source(character.longValue(),actor.longValue(),parts[3],parts[4],Integer.parseInt(parts[5]));
        }catch(IllegalArgumentException invalid){return null;}
    }
    public static final class Proof {
        public final long actionId,actorId,characterId;
        public final String token,kind,resource;
        public final int targetCount;
        private Proof(long action,long actor,long character,String token,String kind,String resource,int count){
            this.actionId=action;this.actorId=actor;this.characterId=character;
            this.token=token;this.kind=kind;this.resource=resource;this.targetCount=count;
        }
    }
    /** Returns null for any untrusted/malformed/mismatched receipt, including generic legacy receipts. */
    public static Proof proof(long expectedActionId,long expectedCharacterId,String key,String source){
        if(key==null||source==null||!key.startsWith(PREFIX)||!source.startsWith(SOURCE_PREFIX))return null;
        String[] k=key.split(":",-1),s=source.split(":",-1);
        if(k.length!=4||s.length!=6)return null;
        try{
            long actionId=Long.parseLong(k[1]),actorId=Long.parseLong(k[2]);
            long characterId=Long.parseLong(s[1]),sourceActorId=Long.parseLong(s[2]);
            int count=Integer.parseInt(s[5]);
            if(actionId!=expectedActionId||actorId<=0||sourceActorId!=actorId||
                characterId!=expectedCharacterId||characterId<=0||count<1||count>64||
                !("effects".equals(s[3])||"saves".equals(s[3]))||!validResource(s[4])||
                !CombatActionReceiptGuard.validToken(k[3])||k[3].length()>112)return null;
            return new Proof(actionId,actorId,characterId,k[3],s[3],s[4],count);
        }catch(NumberFormatException invalid){return null;}
    }
}
