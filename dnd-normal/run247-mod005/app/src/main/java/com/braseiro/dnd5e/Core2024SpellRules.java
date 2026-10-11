package com.braseiro.dnd5e;

import java.text.Normalizer;
import java.util.Locale;

/** RUN199: regras executáveis, deliberadamente limitadas a três magias verificadas
 * no Livro do Jogador 2024, cap. 7 (páginas impressas 248, 266 e 303).
 * Não é catálogo geral de magias e não substitui verificação de geometria. */
public final class Core2024SpellRules {
    private Core2024SpellRules() {}
    public static final class Spell {
        public final String key, name, saveAbility, damageType, area, components;
        public final int level, rangeFeet, areaFeet, diceFaces, baseDice, extraDice;
        public final boolean healing, concentration;
        private Spell(String key,String name,int level,int rangeFeet,String area,int areaFeet,
                      String components,String saveAbility,String damageType,int diceFaces,
                      int baseDice,int extraDice,boolean healing,boolean concentration){
            this.key=key;this.name=name;this.level=level;this.rangeFeet=rangeFeet;
            this.area=area;this.areaFeet=areaFeet;this.components=components;
            this.saveAbility=saveAbility;this.damageType=damageType;this.diceFaces=diceFaces;
            this.baseDice=baseDice;this.extraDice=extraDice;this.healing=healing;
            this.concentration=concentration;
        }
        public int diceCount(int slotLevel){return baseDice+extraDice*(slotLevel-level);}
        public String diceFormula(int slotLevel,int modifier){
            String result=diceCount(slotLevel)+"d"+diceFaces;
            return healing?(result+(modifier<0?"":"+")+modifier):result;
        }
    }
    private static final Spell FIREBALL=new Spell("fireball","Bola de Fogo",3,150,"sphere",20,
        "VSM","dex","fire",6,8,1,false,false);
    private static final Spell BURNING_HANDS=new Spell("burning_hands","Mãos Flamejantes",1,0,"cone",15,
        "VS","dex","fire",6,3,1,false,false);
    private static final Spell CURE_WOUNDS=new Spell("cure_wounds","Curar Ferimentos",1,0,"touch",0,
        "VS","","",8,2,2,true,false);
    /** PHB 2024 Bless has its own persistent, concentration-aware executor. */
    private static final Spell BLESS=new Spell("bless","Bênção",1,30,"targets",0,
        "VSM","","",0,0,0,false,true);
    public static Spell[] supported(){return new Spell[]{FIREBALL,BURNING_HANDS,CURE_WOUNDS};}
    public static Spell[] preparable(){return new Spell[]{FIREBALL,BURNING_HANDS,CURE_WOUNDS,BLESS};}
    public static Spell find(String input){
        String n=norm(input);
        if("fireball".equals(n)||"bola de fogo".equals(n))return FIREBALL;
        if("burning hands".equals(n)||"maos flamejantes".equals(n))return BURNING_HANDS;
        if("cure wounds".equals(n)||"curar ferimentos".equals(n))return CURE_WOUNDS;
        if("bless".equals(n)||"bencao".equals(n))return BLESS;
        return null;
    }
    public static boolean validAbility(String ability){return "str".equals(ability)||"dex".equals(ability)||
        "con".equals(ability)||"int".equals(ability)||"wis".equals(ability)||"cha".equals(ability);}
    public static String classAbility(String className){
        String cls=norm(className);
        if(cls.contains("mago")||cls.contains("wizard")||cls.contains("artificer"))return "int";
        if(cls.contains("clerigo")||cls.contains("cleric")||cls.contains("druida")||
            cls.contains("druid")||cls.contains("guardiao")||cls.contains("ranger"))return "wis";
        if(cls.contains("feiticeiro")||cls.contains("sorcerer")||cls.contains("bardo")||
            cls.contains("bard")||cls.contains("paladino")||cls.contains("paladin")||
            cls.contains("bruxo")||cls.contains("warlock"))return "cha";
        return "";
    }
    public static String validate(String profile,String spell,int slotLevel,int targetCount,
                                  int rawAmount,int spellModifier,boolean componentsConfirmed,
                                  boolean geometryConfirmed,boolean preparedConfirmed){
        if(!"2024".equals(profile))return "requires_2024_rules_profile";
        Spell s=find(spell);
        if(s==null)return "spell_not_compiled";
        if(s==BLESS)return "bless_requires_persistent_runtime";
        if(slotLevel<s.level||slotLevel>9)return "invalid_spell_slot_level";
        if(targetCount<1||targetCount>(s.healing?1:64))return "invalid_target_count";
        if(!componentsConfirmed)return "components_not_confirmed";
        if(!geometryConfirmed)return "range_or_area_not_confirmed";
        if(!preparedConfirmed)return "prepared_spell_not_confirmed";
        int dice=s.diceCount(slotLevel);
        long low=(long)dice+(s.healing?spellModifier:0);
        long high=(long)dice*s.diceFaces+(s.healing?spellModifier:0);
        if(rawAmount<low||rawAmount>high)return "rolled_total_outside_dice_range";
        return "";
    }
    public static String norm(String input){
        if(input==null)return "";
        return Normalizer.normalize(input.toLowerCase(Locale.ROOT),Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").replaceAll("[^a-z0-9]+"," ").trim();
    }
    public static String turnKey(int round,long activeCombatant){
        if(round<1||activeCombatant<=0)throw new IllegalArgumentException("invalid_turn_identity");
        return round+":"+activeCombatant;
    }
}
