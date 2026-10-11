package com.braseiro.dnd5e;

import java.util.Map;

/** RUN206: typed proof that a declared SINGLE weapon attack was rolled,
 * charged to its character and resolved on the actual target in one commit.
 * Extra Attack, reactions, spell attacks and feature-modified crit ranges are
 * deliberately NOT inferred by this deliberately narrow executor. */
public final class PartyAttackCausalReceiptGuard {
    private PartyAttackCausalReceiptGuard() {}
    public static final String PREFIX="party-attack-v1:";
    public static final String SOURCE_PREFIX="party_attack_v1:";
    public static boolean validDamageType(String type){
        return "cortante".equals(type)||"perfurante".equals(type)||"contundente".equals(type);
    }
    public static boolean hit(int natural,int bonus,int ac){
        return natural==20 || natural!=1 && natural+bonus>=ac;
    }
    public static String bindingBlock(long action,long character,String status,String intent,
        long actor,long sourceId,boolean player,long target,int natural,int bonus,int ac,
        int damage,String type,String token){
        if(action<=0||character<=0)return "action_not_found";
        if(!"narrated".equals(status))return "action_not_narrated";
        if(!("combate".equals(intent)||"livre".equals(intent)))return "action_intent_not_combat";
        if(!player||actor<=0||sourceId!=character)return "actor_character_mismatch";
        if(target<=0||target==actor)return "target_invalid";
        if(natural<1||natural>20||bonus< -30||bonus>60||ac< -50||ac>100)return "attack_roll_invalid";
        if(damage<0||damage>100000||!validDamageType(type))return "damage_input_invalid";
        if(!hit(natural,bonus,ac)&&damage!=0)return "damage_on_miss";
        if(token==null||!CombatActionReceiptGuard.validToken(token)||token.length()>92)return "invalid_operation_token";
        return "";
    }
    public static String key(long action,long actor,long target,String token){
        if(action<=0||actor<=0||target<=0||target==actor||token==null||!CombatActionReceiptGuard.validToken(token)||token.length()>92)
            throw new IllegalArgumentException("invalid_attack_key");
        return PREFIX+action+":"+actor+":"+target+":"+token;
    }
    public static String source(long character,long actor,long target,int natural,int bonus,int ac,
        int damage,String type){
        if(character<=0||actor<=0||target<=0||target==actor||natural<1||natural>20||bonus< -30||bonus>60||
            ac< -50||ac>100||damage<0||damage>100000||!validDamageType(type)||
            (!hit(natural,bonus,ac)&&damage!=0))throw new IllegalArgumentException("invalid_attack_source");
        return SOURCE_PREFIX+character+":"+actor+":"+target+":action:"+natural+":"+bonus+":"+ac+":"+
            damage+":"+type+":"+(hit(natural,bonus,ac)?1:0)+":"+(natural==20?1:0);
    }
    public static final class Proof {
        public final long actionId,actorId,characterId,targetId;
        public final int natural,bonus,ac,damage;
        public final String token,type;
        public final boolean hit,critical;
        private Proof(long action,long actor,long character,long target,String token,int natural,int bonus,int ac,int damage,String type){
            this.actionId=action;this.actorId=actor;this.characterId=character;this.targetId=target;
            this.token=token;this.natural=natural;this.bonus=bonus;this.ac=ac;this.damage=damage;this.type=type;
            this.hit=hit(natural,bonus,ac);this.critical=natural==20;
        }
    }
    public static Proof proof(long actionId,long characterId,String key,String source){
        if(key==null||source==null||!key.startsWith(PREFIX)||!source.startsWith(SOURCE_PREFIX))return null;
        String[] k=key.split(":",-1),s=source.split(":",-1);
        if(k.length!=5||s.length!=12)return null;
        try{
            long action=Long.parseLong(k[1]),actor=Long.parseLong(k[2]),keyTarget=Long.parseLong(k[3]);
            long character=Long.parseLong(s[1]),fromActor=Long.parseLong(s[2]),target=Long.parseLong(s[3]);
            int natural=Integer.parseInt(s[5]),bonus=Integer.parseInt(s[6]),ac=Integer.parseInt(s[7]),damage=Integer.parseInt(s[8]);
            if(action!=actionId||character!=characterId||actor!=fromActor||keyTarget!=target||!"action".equals(s[4]))return null;
            String valid=bindingBlock(action,character,"narrated","combate",actor,character,true,target,
                natural,bonus,ac,damage,s[9],k[4]);
            if(!valid.isEmpty()||!s[10].equals(hit(natural,bonus,ac)?"1":"0")||
               !s[11].equals(natural==20?"1":"0"))return null;
            return new Proof(action,actor,character,target,k[4],natural,bonus,ac,damage,s[9]);
        }catch(IllegalArgumentException invalid){return null;}
    }
    public static String remapKey(String key,Map<Long,Long> actions,Map<Long,Long> combatants){
        if(key==null||!key.startsWith(PREFIX))return key;
        String[] k=key.split(":",-1);if(k.length!=5)return null;
        try{
            Long action=actions.get(Long.parseLong(k[1])),actor=combatants.get(Long.parseLong(k[2]));
            Long target=combatants.get(Long.parseLong(k[3]));
            return action==null||actor==null||target==null?null:key(action,actor,target,k[4]);
        }catch(IllegalArgumentException invalid){return null;}
    }
    public static String remapSource(String source,Map<Long,Long> characters,Map<Long,Long> combatants){
        if(source==null||!source.startsWith(SOURCE_PREFIX))return source;
        String[] s=source.split(":",-1);if(s.length!=12)return null;
        try{
            Long ch=characters.get(Long.parseLong(s[1])),actor=combatants.get(Long.parseLong(s[2]));
            Long target=combatants.get(Long.parseLong(s[3]));
            if(ch==null||actor==null||target==null||!"action".equals(s[4]))return null;
            String remapped=source(ch,actor,target,Integer.parseInt(s[5]),Integer.parseInt(s[6]),
                Integer.parseInt(s[7]),Integer.parseInt(s[8]),s[9]);
            String[] rec=remapped.split(":",-1);
            return rec[10].equals(s[10])&&rec[11].equals(s[11])?remapped:null;
        }catch(IllegalArgumentException invalid){return null;}
    }
}
