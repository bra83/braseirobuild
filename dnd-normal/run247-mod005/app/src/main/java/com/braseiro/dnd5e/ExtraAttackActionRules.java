package com.braseiro.dnd5e;

import java.util.Locale;
import java.util.Map;

/** RUN210: conservative PHB 2014/2024 Attack-action repeat count, NOT additional actions. */
public final class ExtraAttackActionRules {
    private ExtraAttackActionRules() {}
    public static int attacks(String cls,int classLevel) {
        if(cls==null||classLevel<1||classLevel>20)return 0;
        String c=cls.trim().toLowerCase(Locale.ROOT);
        // Native Portuguese and English class identifiers. Subclass/invocation features
        // are deliberately excluded until their choices are verified on the sheet.
        if(c.equals("fighter")||c.equals("guerreiro"))
            return classLevel>=20?4:classLevel>=11?3:classLevel>=5?2:1;
        if(c.equals("barbarian")||c.equals("bárbaro")||c.equals("barbaro")||
            c.equals("monk")||c.equals("monge")||c.equals("paladin")||c.equals("paladino")||
            c.equals("ranger")||c.equals("patrulheiro"))return classLevel>=5?2:1;
        return 1;
    }
    public static boolean validCount(int available,int requested){return available>=2&&available<=4&&requested>=2&&requested<=available;}
    public static String block(String status,String intent,boolean linked,long actor,long character,
                               int maxAttacks,int requested,String token){
        if(!"narrated".equals(status))return "action_not_narrated";
        if(!("combate".equals(intent)||"livre".equals(intent)))return "intent_not_combat";
        if(!linked||actor<=0||character<=0)return "actor_character_mismatch";
        if(!validCount(maxAttacks,requested))return "extra_attack_not_granted";
        if(token==null||!token.matches("ui-extra-[A-Za-z0-9-]{10,80}"))return "invalid_token";
        return "";
    }
    public static final String PREFIX="party-extra-v1:";
    public static final String SOURCE_PREFIX="party_extra_v1:";
    public static final String STEP_PREFIX="extra-step-v1:";
    public static String key(long action,long actor,String token){
        if(action<=0||actor<=0||token==null||!token.matches("ui-extra-[A-Za-z0-9-]{10,80}"))throw new IllegalArgumentException("extra_key_invalid");
        return PREFIX+action+":"+actor+":"+token;
    }
    public static final class Swing {
        public final long target;
        public final int natural,bonus,ac,damage;
        public final String type;
        public Swing(long target,int natural,int bonus,int ac,int damage,String type){
            if(target<=0||natural<1||natural>20||bonus< -30||bonus>60||ac< -50||ac>100||
               damage<0||damage>100000||!PartyAttackCausalReceiptGuard.validDamageType(type)||
               (!PartyAttackCausalReceiptGuard.hit(natural,bonus,ac)&&damage!=0))throw new IllegalArgumentException("extra_swing_invalid");
            this.target=target;this.natural=natural;this.bonus=bonus;this.ac=ac;this.damage=damage;this.type=type;
        }
        public boolean hit(){return PartyAttackCausalReceiptGuard.hit(natural,bonus,ac);}
        public String encode(){return target+","+natural+","+bonus+","+ac+","+damage+","+type;}
    }
    public static String source(long character,long actor,Swing[] swings){
        if(character<=0||actor<=0||swings==null||swings.length<2||swings.length>4)throw new IllegalArgumentException("extra_source_invalid");
        StringBuilder b=new StringBuilder(SOURCE_PREFIX).append(character).append(':').append(actor);
        for(Swing s:swings){if(s==null)throw new IllegalArgumentException("extra_step_null");b.append(':').append(s.encode());}
        return b.toString();
    }
    public static final class Proof {
        public final long action,actor,character;
        public final String token;
        public final Swing[] swings;
        private Proof(long a,long b,long c,String t,Swing[] s){action=a;actor=b;character=c;token=t;swings=s;}
    }
    public static Proof proof(long action,long character,String key,String source){
        if(key==null||source==null||!key.startsWith(PREFIX)||!source.startsWith(SOURCE_PREFIX))return null;
        try {
            String[] k=key.split(":",-1),s=source.split(":",-1);
            if(k.length!=4||s.length<5||s.length>7)return null;
            long a=Long.parseLong(k[1]),actor=Long.parseLong(k[2]),c=Long.parseLong(s[1]),fromActor=Long.parseLong(s[2]);
            if(a!=action||c!=character||actor!=fromActor)return null;
            key(a,actor,k[3]);
            Swing[] swings=new Swing[s.length-3];
            for(int i=3;i<s.length;i++){
                String[] v=s[i].split(",",-1);if(v.length!=6)return null;
                swings[i-3]=new Swing(Long.parseLong(v[0]),Integer.parseInt(v[1]),Integer.parseInt(v[2]),
                    Integer.parseInt(v[3]),Integer.parseInt(v[4]),v[5]);
            }
            if(!source.equals(source(c,actor,swings)))return null;
            return new Proof(a,actor,c,k[3],swings);
        }catch(IllegalArgumentException bad){return null;}
    }
    public static String remapKey(String key,Map<Long,Long> actions,Map<Long,Long> combatants){
        if(key==null||!key.startsWith(PREFIX))return key;
        try{String[] v=key.split(":",-1);if(v.length!=4)return null;
            Long a=actions.get(Long.parseLong(v[1])),b=combatants.get(Long.parseLong(v[2]));
            return a==null||b==null?null:key(a,b,v[3]);
        }catch(IllegalArgumentException bad){return null;}
    }
    public static String remapSource(String value,Map<Long,Long> chars,Map<Long,Long> combatants){
        if(value==null||!value.startsWith(SOURCE_PREFIX))return value;
        try{String[] v=value.split(":",-1);if(v.length<5||v.length>7)return null;
            Long ch=chars.get(Long.parseLong(v[1])),actor=combatants.get(Long.parseLong(v[2]));
            if(ch==null||actor==null)return null;
            Swing[] ss=new Swing[v.length-3];
            for(int i=3;i<v.length;i++){
                String[] f=v[i].split(",",-1);if(f.length!=6)return null;
                Long target=combatants.get(Long.parseLong(f[0]));if(target==null)return null;
                ss[i-3]=new Swing(target,Integer.parseInt(f[1]),Integer.parseInt(f[2]),
                    Integer.parseInt(f[3]),Integer.parseInt(f[4]),f[5]);
            }
            return source(ch,actor,ss);
        }catch(IllegalArgumentException bad){return null;}
    }
    public static String stepProof(Swing s){return STEP_PREFIX+s.encode();}
    public static boolean stepMatches(String proof,Swing swing){return stepProof(swing).equals(proof);}
    public static String remapStep(String source,Map<Long,Long> combatants){
        if(source==null||!source.startsWith(STEP_PREFIX))return source;
        try{String[] v=source.substring(STEP_PREFIX.length()).split(",",-1);
            if(v.length!=6)return null;
            Long target=combatants.get(Long.parseLong(v[0]));if(target==null)return null;
            return stepProof(new Swing(target,Integer.parseInt(v[1]),Integer.parseInt(v[2]),
                Integer.parseInt(v[3]),Integer.parseInt(v[4]),v[5]));
        }catch(IllegalArgumentException bad){return null;}
    }
}
