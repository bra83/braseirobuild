package com.braseiro.dnd5e;

import java.util.Map;

/** Ordinary Light-property extra attack for 2014 and 2024 rules. The caller
 * must verify the persisted main Attack action and equipped inventory rows,
 * then spend the bonus action and apply damage in one SQLite transaction. Nick,
 * Dual Wielder, thrown weapons and unarmed strikes need separate executors. */
public final class TwoWeaponFightingRules {
    private TwoWeaponFightingRules() {}
    public static final String PREFIX="two-weapon-v1:";

    public static final class Weapon {
        public final String itemId,hand,baseDice,damageType;
        public final boolean light,melee,oneHanded;
        public final int itemDamageModifier;
        public Weapon(String itemId,String hand,boolean light,boolean melee,boolean oneHanded,
                String baseDice,int itemDamageModifier,String damageType) {
            if(itemId==null||!itemId.matches("[A-Za-z0-9._-]{1,128}")||
                !("left".equals(hand)||"right".equals(hand))||
                baseDice==null||!baseDice.matches("(?:10|[1-9])d(?:4|6|8|10|12)")||
                itemDamageModifier< -100||itemDamageModifier>100||
                !PartyAttackCausalReceiptGuard.validDamageType(damageType))
                throw new IllegalArgumentException("invalid_two_weapon_profile");
            this.itemId=itemId;this.hand=hand;this.light=light;this.melee=melee;
            this.oneHanded=oneHanded;this.baseDice=baseDice;
            this.itemDamageModifier=itemDamageModifier;this.damageType=damageType;
        }
    }

    public static final class Request {
        public final long actorId,targetId;
        public final String mainAttackToken,bonusAttackToken,edition;
        public final Weapon main,off;
        public final boolean mainAttackCommitted,bonusActionAvailable,incapacitated,fightingStyle;
        public final int natural,attackBonus,targetAc,diceTotal,abilityModifier;
        public Request(long actorId,long targetId,String mainAttackToken,String bonusAttackToken,
                String edition,Weapon main,Weapon off,boolean mainAttackCommitted,
                boolean bonusActionAvailable,boolean incapacitated,boolean fightingStyle,
                int natural,int attackBonus,int targetAc,int diceTotal,int abilityModifier) {
            this.actorId=actorId;this.targetId=targetId;this.mainAttackToken=mainAttackToken;
            this.bonusAttackToken=bonusAttackToken;this.edition=edition;this.main=main;this.off=off;
            this.mainAttackCommitted=mainAttackCommitted;this.bonusActionAvailable=bonusActionAvailable;
            this.incapacitated=incapacitated;this.fightingStyle=fightingStyle;
            this.natural=natural;this.attackBonus=attackBonus;this.targetAc=targetAc;
            this.diceTotal=diceTotal;this.abilityModifier=abilityModifier;
        }
    }

    public static final class Resolution {
        public final String blocked,receipt;
        public final boolean hit,critical,spendBonusAction;
        public final int damage;
        private Resolution(String reason,String receipt,boolean hit,boolean critical,int damage) {
            blocked=reason;this.receipt=receipt;this.hit=hit;this.critical=critical;
            this.damage=damage;spendBonusAction=reason.isEmpty();
        }
        public boolean valid(){return blocked.isEmpty();}
    }
    private static Resolution fail(String reason){return new Resolution(reason,"",false,false,0);}

    public static Resolution resolve(Request r) {
        if(r==null||r.main==null||r.off==null||r.actorId<=0||r.targetId<=0||
                r.actorId==r.targetId||!CombatActionReceiptGuard.validToken(r.mainAttackToken)||
                !CombatActionReceiptGuard.validToken(r.bonusAttackToken)||
                r.mainAttackToken.equals(r.bonusAttackToken)||
                !("2014".equals(r.edition)||"2024".equals(r.edition)))
            return fail("invalid_two_weapon_request");
        if(!r.mainAttackCommitted)return fail("main_attack_not_committed");
        if(r.incapacitated)return fail("incapacitated");
        if(!r.bonusActionAvailable)return fail("bonus_action_spent");
        if(!r.main.light||!r.off.light||!r.main.melee||!r.off.melee||
                !r.main.oneHanded||!r.off.oneHanded||
                r.main.hand.equals(r.off.hand)||r.main.itemId.equals(r.off.itemId))
            return fail("two_distinct_light_melee_weapons_required");
        if(r.natural<1||r.natural>20||r.attackBonus< -30||r.attackBonus>60||
                r.targetAc< -50||r.targetAc>100||r.abilityModifier< -20||r.abilityModifier>30)
            return fail("invalid_attack_roll");
        boolean hit=PartyAttackCausalReceiptGuard.hit(r.natural,r.attackBonus,r.targetAc);
        boolean critical=hit&&r.natural==20;
        EquippedWeaponAttackRules.Dice die=EquippedWeaponAttackRules.parse(r.off.baseDice);
        if(die==null)return fail("invalid_damage_dice");
        int count=die.count*(critical?2:1);
        if(hit?(r.diceTotal<count||r.diceTotal>count*die.faces):r.diceTotal!=0)
            return fail("invalid_damage_roll");
        int abilityDamage=r.fightingStyle?r.abilityModifier:Math.min(0,r.abilityModifier);
        int damage=hit?Math.max(0,r.diceTotal+r.off.itemDamageModifier+abilityDamage):0;
        String receipt=PREFIX+r.actorId+":"+r.targetId+":"+r.mainAttackToken+":"+
            r.bonusAttackToken+":"+r.edition+":"+r.main.itemId+":"+r.main.hand+":"+
            r.off.itemId+":"+r.off.hand+":"+r.off.baseDice+":"+
            r.off.itemDamageModifier+":"+r.off.damageType+":"+
            r.natural+":"+r.attackBonus+":"+r.targetAc+":"+r.diceTotal+":"+
            r.abilityModifier+":"+(r.fightingStyle?1:0)+":"+damage;
        return new Resolution("",receipt,hit,critical,damage);
    }

    /** Exact same token and result is a replay, any divergent payload is a
     * collision. The caller must look up the durable receipt before mutation. */
    public static String replayDecision(String persisted,String requested) {
        if(persisted==null||persisted.isEmpty())return "new";
        return persisted.equals(requested)?"replayed":"operation_token_collision";
    }

    /** Structural validator; the caller must separately verify the main attack,
     * weapon ownership and current combat state against live SQLite rows. */
    public static boolean validReceipt(String raw) {
        if(raw==null||!raw.startsWith(PREFIX))return false;
        try{
            String[] p=raw.split(":",-1);
            if(p.length!=20)return false;
            Weapon main=new Weapon(p[6],p[7],true,true,true,"1d4",0,"cortante");
            Weapon off=new Weapon(p[8],p[9],true,true,true,p[10],Integer.parseInt(p[11]),p[12]);
            Request r=new Request(Long.parseLong(p[1]),Long.parseLong(p[2]),p[3],p[4],p[5],
                main,off,true,true,false,"1".equals(p[18]),Integer.parseInt(p[13]),
                Integer.parseInt(p[14]),Integer.parseInt(p[15]),Integer.parseInt(p[16]),
                Integer.parseInt(p[17]));
            Resolution resolved=resolve(r);
            return resolved.valid()&&Integer.parseInt(p[19])==resolved.damage&&raw.equals(resolved.receipt);
        }catch(RuntimeException invalid){return false;}
    }
    /** Combatant IDs change on backup import; orphaned receipts are dropped. */
    public static String remapSource(String raw,Map<Long,Long> combatants) {
        if(raw==null||!raw.startsWith(PREFIX))return raw;
        if(!validReceipt(raw)||combatants==null)return null;
        try{
            String[] p=raw.split(":",-1);
            Long actor=combatants.get(Long.parseLong(p[1]));
            Long target=combatants.get(Long.parseLong(p[2]));
            if(actor==null||target==null||actor<=0||target<=0||actor.equals(target))return null;
            p[1]=String.valueOf(actor);p[2]=String.valueOf(target);
            StringBuilder result=new StringBuilder();
            for(int i=0;i<p.length;i++){if(i>0)result.append(':');result.append(p[i]);}
            return validReceipt(result.toString())?result.toString():null;
        }catch(RuntimeException invalid){return null;}
    }
}
