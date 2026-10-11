package com.braseiro.dnd5e;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** PHB 2024, class tables and p. 237 (preparation by class).
 * Pure, deterministic rules. Class levels (not total multiclass caster slots) determine
 * preparation capacity and maximum spell level. No adventures or Mystara data.
 */
public final class Core2024PreparedSpellRules {
    private Core2024PreparedSpellRules() {}
    // PHB 2024: Bard p60, Cleric p82, Druid p92, Sorcerer p104,
    // Ranger p118, Wizard p148, Paladin p168 (printed pages).
    private static final int[] FULL = {0,4,5,6,7,9,10,11,12,14,15,16,16,17,17,18,18,19,20,21,22};
    private static final int[] SORCERER = {0,2,4,6,7,9,10,11,12,14,15,16,16,17,17,18,18,19,20,21,22};
    private static final int[] WIZARD = {0,4,5,6,7,9,10,11,12,14,15,16,16,17,18,19,21,22,23,24,25};
    private static final int[] HALF = {0,2,3,4,5,6,6,7,7,9,9,10,10,11,11,12,12,14,14,15,15};
    public static boolean supportedClass(String cls){return "bard".equals(cls)||"cleric".equals(cls)||
        "druid".equals(cls)||"sorcerer".equals(cls)||"wizard".equals(cls)||
        "ranger".equals(cls)||"paladin".equals(cls);}
    public static int capacity(String cls,int level){
        if(!supportedClass(cls)||level<1||level>20)return -1;
        if("sorcerer".equals(cls))return SORCERER[level];
        if("wizard".equals(cls))return WIZARD[level];
        if("paladin".equals(cls)||"ranger".equals(cls))return HALF[level];
        return FULL[level];
    }
    public static int maxCircle(String cls,int level){
        if(capacity(cls,level)<0)return -1;
        if("paladin".equals(cls)||"ranger".equals(cls))return Math.min(5,(level+3)/4);
        return Math.min(9,(level+1)/2);
    }
    /** -1 means any number of replacements, 0 none, 1 one replacement. */
    public static int replacements(String cls,String event){
        if(!supportedClass(cls))return 0;
        if("setup".equals(event))return -1;
        if("short_rest".equals(event))return "wizard".equals(cls)?1:0;
        if("long_rest".equals(event)){
            if("cleric".equals(cls)||"druid".equals(cls)||"wizard".equals(cls))return -1;
            if("ranger".equals(cls)||"paladin".equals(cls))return 1;
            return 0;
        }
        if("level_up".equals(event))return "bard".equals(cls)||"sorcerer".equals(cls)?1:0;
        return 0;
    }
    public static boolean canOpen(String cls,String event){
        return "setup".equals(event)||"level_up".equals(event)||
            ("short_rest".equals(event)&&"wizard".equals(cls))||
            ("long_rest".equals(event)&&replacements(cls,event)!=0);
    }
    public static String validateSelection(String cls,int level,List<String> baseline,
                                           List<String> next,String event){
        int cap=capacity(cls,level);
        if(cap<0)return "unsupported_class_or_level";
        if(next==null||baseline==null)return "missing_spell_list";
        if(next.size()>cap)return "prepared_capacity_exceeded";
        Set<String> nextSet=new HashSet<String>(next),oldSet=new HashSet<String>(baseline);
        if(nextSet.size()!=next.size()||oldSet.size()!=baseline.size())return "duplicate_spell";
        if(!canOpen(cls,event)||"short_rest".equals(event)&&level<5)
            return "preparation_window_closed";
        for(String key:next){
            Core2024SpellRules.Spell spell=Core2024SpellRules.find(key);
            if(spell==null||!key.equals(spell.key))return "spell_not_compiled";
            if(!Core2024CastingEligibility.listed(key,cls))return "spell_not_on_class_list";
            if(spell.level>maxCircle(cls,level))return "spell_circle_above_class_level";
        }
        int removed=0;for(String key:oldSet)if(!nextSet.contains(key))removed++;
        int allowed=replacements(cls,event);
        if(allowed>=0&&removed>allowed)return "too_many_replacements_for_event";
        if("long_rest".equals(event)&&("ranger".equals(cls)||"paladin".equals(cls))){
            int added=0;for(String key:nextSet)if(!oldSet.contains(key))added++;
            if(added>removed)return "long_rest_requires_one_for_one_replacement";
        }
        return "";
    }
}
