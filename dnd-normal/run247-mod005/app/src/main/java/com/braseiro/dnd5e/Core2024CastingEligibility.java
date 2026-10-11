package com.braseiro.dnd5e;

import java.util.ArrayList;
import java.util.List;

/** RUN200: PHB2024 source-class and material gate for the three compiled spells.
 * Multiclass levels must be explicit; character level alone never grants higher
 * circle spells to a low-level class. Subclass/feat grants are NOT assumed.
 */
public final class Core2024CastingEligibility {
    private Core2024CastingEligibility() {}
    public static final class ClassLevel {
        public final String name; public final int level;
        public ClassLevel(String name,int level){this.name=name;this.level=level;}
    }
    public static final class Result {
        public final String error,sourceClass;
        private Result(String error,String sourceClass){this.error=error;this.sourceClass=sourceClass;}
        public boolean allowed(){return error.isEmpty();}
    }
    private static Result fail(String reason){return new Result(reason,"");}
    public static String baseClass(String raw){
        String s=Core2024SpellRules.norm(raw);
        if(s.startsWith("mago")||s.startsWith("wizard"))return "wizard";
        if(s.startsWith("feiticeiro")||s.startsWith("sorcerer"))return "sorcerer";
        if(s.startsWith("bardo")||s.startsWith("bard"))return "bard";
        if(s.startsWith("clerigo")||s.startsWith("cleric"))return "cleric";
        if(s.startsWith("druida")||s.startsWith("druid"))return "druid";
        if(s.startsWith("guardiao")||s.startsWith("ranger")||s.startsWith("patrulheiro"))return "ranger";
        if(s.startsWith("paladino")||s.startsWith("paladin"))return "paladin";
        if(s.startsWith("guerreiro")||s.startsWith("fighter"))return "fighter";
        if(s.startsWith("ladino")||s.startsWith("rogue"))return "rogue";
        if(s.startsWith("monge")||s.startsWith("monk"))return "monk";
        if(s.startsWith("barbaro")||s.startsWith("barbarian"))return "barbarian";
        if(s.startsWith("bruxo")||s.startsWith("warlock"))return "warlock";
        return "";
    }
    static boolean listed(String spell,String cls){
        if("fireball".equals(spell)||"burning_hands".equals(spell))
            return "wizard".equals(cls)||"sorcerer".equals(cls);
        if("cure_wounds".equals(spell))
            return "bard".equals(cls)||"cleric".equals(cls)||"druid".equals(cls)||
                "ranger".equals(cls)||"paladin".equals(cls);
        if("bless".equals(spell))return "cleric".equals(cls)||"paladin".equals(cls);
        return false;
    }
    static String ability(String cls){
        if("wizard".equals(cls))return "int";
        if("cleric".equals(cls)||"druid".equals(cls)||"ranger".equals(cls))return "wis";
        return "cha";
    }
    public static Result check(String spell,int totalLevel,List<ClassLevel> classes,String chosenAbility){
        Core2024SpellRules.Spell compiled=Core2024SpellRules.find(spell);
        if(compiled==null)return fail("spell_not_compiled");
        if(classes==null||classes.isEmpty())return fail("class_levels_required");
        if(totalLevel<1||totalLevel>20)return fail("invalid_character_level");
        int sum=0;String source="";boolean matchingAbility=false;
        java.util.HashSet<String> seen=new java.util.HashSet<String>();
        for(ClassLevel entry:classes){
            if(entry==null||entry.level<1||entry.level>20)return fail("invalid_class_level");
            String cls=baseClass(entry.name);
            if(cls.isEmpty()||!seen.add(cls))return fail("unsupported_or_duplicate_class");
            sum+=entry.level;
            if(listed(compiled.key,cls)&&entry.level>=(compiled.level==3?5:1)){
                if(ability(cls).equals(chosenAbility)){source=cls;matchingAbility=true;}
            }
        }
        if(sum!=totalLevel)return fail("class_levels_do_not_sum_to_character_level");
        if(matchingAbility)return new Result("",source);
        boolean eligible=false;
        for(ClassLevel e:classes)if(listed(compiled.key,baseClass(e.name))&&
            e.level>=(compiled.level==3?5:1))eligible=true;
        return fail(eligible?"casting_ability_mismatch":"spell_not_available_to_class_at_level");
    }
    /** PHB2024 p248: Fireball M = bat guano + sulfur OR appropriate focus/pouch;
     * non-costly, non-consumed material. VS spells need no material inventory.
     */
    public static String material(String spell,String sourceClass,List<String> items){
        Core2024SpellRules.Spell s=Core2024SpellRules.find(spell);
        if(s==null)return "spell_not_compiled";
        if(!s.components.contains("M"))return "";
        if(items==null)return "material_inventory_unavailable";
        boolean guano=false,sulfur=false;
        for(String raw:items){
            String item=Core2024SpellRules.norm(raw);
            if(item.contains("bolsa de componentes")||item.contains("component pouch"))return "";
            if(("wizard".equals(sourceClass)||"sorcerer".equals(sourceClass))&&
                (item.contains("foco arcano")||item.contains("arcane focus")))return "";
            if(item.contains("guano"))guano=true;
            if(item.contains("enxofre")||item.contains("sulfur"))sulfur=true;
        }
        return guano&&sulfur?"":"material_component_missing";
    }
    public static List<ClassLevel> single(String name,int level){
        ArrayList<ClassLevel> result=new ArrayList<ClassLevel>();
        if(name==null||name.trim().isEmpty()||name.matches(".*[/+,].*"))return result;
        result.add(new ClassLevel(name,level));return result;
    }
}
