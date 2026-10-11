package com.braseiro.dnd5e;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** RUN207: deterministic, fail-closed profile for one ordinary equipped weapon.
 * Values are explicit character-sheet data, NOT silently inferred proficiency. */
public final class EquippedWeaponAttackRules {
    private EquippedWeaponAttackRules() {}
    private static final Pattern SIMPLE=Pattern.compile("(10|[1-9])d(4|6|8|10|12)([+-]\\d{1,3})?");
    public static final class Dice {
        public final int count,faces,modifier;
        public final String formula;
        private Dice(int count,int faces,int modifier,String formula){
            this.count=count;this.faces=faces;this.modifier=modifier;this.formula=formula;
        }
        public String rolledFormula(boolean critical){
            return (critical?count*2:count)+"d"+faces+(modifier==0?"":modifier>0?"+"+modifier:String.valueOf(modifier));
        }
        public boolean acceptsTotal(int total,boolean hit,boolean critical){
            if(!hit)return total==0;
            int dice=critical?count*2:count;
            return total>=Math.max(0,dice+modifier)&&total<=Math.max(0,dice*faces+modifier);
        }
    }
    public static Dice parse(String formula){
        if(formula==null)return null;
        String value=formula.trim().toLowerCase(java.util.Locale.ROOT).replace(" ","");
        Matcher m=SIMPLE.matcher(value);if(!m.matches())return null;
        int count=Integer.parseInt(m.group(1)),faces=Integer.parseInt(m.group(2));
        int mod=m.group(3)==null?0:Integer.parseInt(m.group(3));
        if(mod< -100||mod>100)return null;
        return new Dice(count,faces,mod,value);
    }
    public static boolean validProfile(String itemId,int bonus,String formula,String type){
        return itemId!=null&&itemId.matches("[A-Za-z0-9._-]{1,128}")&&
            bonus>= -30&&bonus<=60&&parse(formula)!=null&&
            PartyAttackCausalReceiptGuard.validDamageType(type);
    }
    public static String receiptSource(String itemId,int bonus,String formula,String type){
        if(!validProfile(itemId,bonus,formula,type))throw new IllegalArgumentException("invalid_weapon_profile");
        return "weapon:"+itemId+":"+bonus+":"+parse(formula).formula+":"+type;
    }
    public static boolean validReceipt(String raw){
        if(raw==null||!raw.startsWith("weapon:"))return false;
        String[] p=raw.split(":",-1);
        if(p.length!=5)return false;
        try{return validProfile(p[1],Integer.parseInt(p[2]),p[3],p[4]) &&
            raw.equals(receiptSource(p[1],Integer.parseInt(p[2]),p[3],p[4]));}
        catch(Exception e){return false;}
    }
}
