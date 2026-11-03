package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Extensão determinística Kingmaker para Pathfinder 2e.
 *
 * O núcleo PF2e continua responsável pelos quatro graus de sucesso. Este
 * componente acrescenta apenas os cálculos próprios de Reino, assentamentos,
 * acampamento e exércitos; a IA nunca calcula nem grava esses valores.
 */
public final class KingmakerRulesEngine {
    public static final String PROFILE="Pathfinder 2e Kingmaker";
    public static final String[] PHASES={"UPKEEP","COMMERCE","ACTIVITY","EVENT"};
    public static final String[] ABILITIES={"culture","economy","loyalty","stability"};
    public static final String[] LEADERSHIP_ROLES={"Ruler","Counselor","General","Emissary","Magister","Treasurer","Viceroy","Warden"};
    private static final int[] KINGDOM_CONTROL_DCS={14,15,16,18,20,22,23,24,26,27,28,30,31,32,34,35,36,38,39,40};
    private static final LinkedHashMap<String,String> SKILL_ABILITIES=new LinkedHashMap<String,String>();
    private static final LinkedHashMap<String,String> ROLE_ABILITIES=new LinkedHashMap<String,String>();
    private final Pathfinder2eRulesEngine pf2=new Pathfinder2eRulesEngine();

    static {
        SKILL_ABILITIES.put("agriculture","stability");
        SKILL_ABILITIES.put("arts","culture");
        SKILL_ABILITIES.put("boating","economy");
        SKILL_ABILITIES.put("defense","stability");
        SKILL_ABILITIES.put("engineering","stability");
        SKILL_ABILITIES.put("exploration","economy");
        SKILL_ABILITIES.put("folklore","culture");
        SKILL_ABILITIES.put("industry","economy");
        SKILL_ABILITIES.put("intrigue","loyalty");
        SKILL_ABILITIES.put("magic","culture");
        SKILL_ABILITIES.put("politics","loyalty");
        SKILL_ABILITIES.put("scholarship","culture");
        SKILL_ABILITIES.put("statecraft","loyalty");
        SKILL_ABILITIES.put("trade","economy");
        SKILL_ABILITIES.put("warfare","loyalty");
        SKILL_ABILITIES.put("wilderness","stability");
        ROLE_ABILITIES.put("ruler","loyalty");
        ROLE_ABILITIES.put("counselor","culture");
        ROLE_ABILITIES.put("general","stability");
        ROLE_ABILITIES.put("emissary","loyalty");
        ROLE_ABILITIES.put("magister","culture");
        ROLE_ABILITIES.put("treasurer","economy");
        ROLE_ABILITIES.put("viceroy","economy");
        ROLE_ABILITIES.put("warden","stability");
    }

    public JSONArray skillCatalog(){
        JSONArray out=new JSONArray();
        try{for(Map.Entry<String,String> entry:SKILL_ABILITIES.entrySet())out.put(new JSONObject().put("key",entry.getKey()).put("name",skillLabel(entry.getKey())).put("ability",entry.getValue()).put("ability_name",abilityLabel(entry.getValue())));}catch(Exception ignored){}
        return out;
    }

    public String abilityForSkill(String skill){String key=key(skill);String value=SKILL_ABILITIES.get(key);return value==null?"stability":value;}
    public boolean validSkill(String skill){return SKILL_ABILITIES.containsKey(key(skill));}
    public boolean validAbility(String ability){String k=key(ability);for(String value:ABILITIES)if(value.equals(k))return true;return false;}
    public boolean validPhase(String phase){if(phase==null)return false;for(String value:PHASES)if(value.equalsIgnoreCase(phase.trim()))return true;return false;}
    public boolean validLeadershipRole(String role){if(role==null)return false;for(String value:LEADERSHIP_ROLES)if(value.equalsIgnoreCase(role.trim()))return true;return false;}

    public String nextPhase(String phase){
        String current=phase==null?"":phase.trim().toUpperCase(Locale.US);
        for(int i=0;i<PHASES.length;i++)if(PHASES[i].equals(current))return i==PHASES.length-1?"COMPLETE":PHASES[i+1];
        return "UPKEEP";
    }

    /** CD de Controle própria da progressão de Reino (níveis 1–20). */
    public int levelDc(int level){return KINGDOM_CONTROL_DCS[Math.max(1,Math.min(20,level))-1];}

    public int abilityModifier(int score){return Math.floorDiv(Math.max(1,score)-10,2);}

    public int controlDc(int kingdomLevel,int size,int unrest,int rulerVacant,int explicitModifier){
        return Math.max(10,levelDc(kingdomLevel)+sizeControlModifier(size)+(rulerVacant>0?2:0)+explicitModifier);
    }

    public int skillModifier(int kingdomLevel,int abilityScore,String rank,int itemBonus,int statusBonus,int circumstanceBonus,int unrest){
        int proficiency=pf2.proficiency(Math.max(1,Math.min(20,kingdomLevel)),rank);
        int unrestPenalty=unrestPenalty(unrest);
        return abilityModifier(abilityScore)+proficiency+itemBonus+statusBonus+circumstanceBonus-unrestPenalty;
    }

    public int unrestPenalty(int unrest){int value=Math.max(0,unrest);return value>=15?4:value>=10?3:value>=5?2:value>=1?1:0;}

    public int ruinPenalty(int ruin,int threshold){int t=Math.max(1,threshold);return Math.max(0,ruin)/t;}

    public JSONObject resolveKingdomCheck(String skill,int natural,int modifier,int dc){
        JSONObject result=pf2.resolveCheck(natural,modifier,Math.max(0,dc));
        try{result.put("subsystem","kingdom").put("skill",key(skill)).put("skill_name",skillLabel(skill)).put("ability",abilityForSkill(skill)).put("ability_name",abilityLabel(abilityForSkill(skill)));}catch(Exception ignored){}
        return result;
    }

    public JSONObject resourceDice(int kingdomLevel,int size){
        int dice=Math.max(1,Math.min(20,kingdomLevel))+4,die=resourceDieSize(size);
        JSONObject out=new JSONObject();try{out.put("count",dice).put("die",die).put("formula",dice+"d"+die).put("commodity_storage",commodityStorage(size));}catch(Exception ignored){}return out;
    }

    public JSONObject applyKingdomXp(int level,int xp,int delta){
        int nextLevel=Math.max(1,Math.min(20,level)),nextXp=Math.max(0,xp+delta),gained=0;
        if(nextLevel<20&&nextXp>=1000){nextXp-=1000;nextLevel++;gained=1;}
        JSONObject out=new JSONObject();try{out.put("level",nextLevel).put("xp",nextXp).put("levels_gained",gained);}catch(Exception ignored){}return out;
    }

    public String sizeCategory(int size){int value=Math.max(1,size);return value>=100?"dominion":value>=50?"country":value>=25?"state":value>=10?"province":"territory";}

    public int settlementInfluence(String type){String k=key(type);if(k.contains("metropolis")||k.contains("metropole"))return 3;if(k.contains("city")||k.contains("cidade"))return 2;if(k.contains("town")||k.contains("vila"))return 1;return 0;}

    public JSONObject settlementLimits(String type){
        String k=key(type);int minKingdom,minLevel,maxLevel,minBlocks,maxBlocks,populationFloor,populationCeiling,consumption,itemBonus;
        if(k.contains("metropolis")||k.contains("metropole")){minKingdom=15;minLevel=10;maxLevel=20;minBlocks=10;maxBlocks=0;populationFloor=25001;populationCeiling=Integer.MAX_VALUE;consumption=6;itemBonus=3;}
        else if(k.contains("city")||k.contains("cidade")){minKingdom=9;minLevel=5;maxLevel=9;minBlocks=5;maxBlocks=9;populationFloor=2001;populationCeiling=25000;consumption=4;itemBonus=2;}
        else if(k.contains("town")||k.contains("vila")){minKingdom=3;minLevel=2;maxLevel=4;minBlocks=2;maxBlocks=4;populationFloor=401;populationCeiling=2000;consumption=2;itemBonus=1;}
        else{minKingdom=1;minLevel=1;maxLevel=1;minBlocks=1;maxBlocks=1;populationFloor=0;populationCeiling=400;consumption=1;itemBonus=1;}
        JSONObject out=new JSONObject();try{out.put("minimum_kingdom_level",minKingdom).put("min_level",minLevel).put("max_level",maxLevel).put("min_blocks",minBlocks).put("max_blocks",maxBlocks).put("blocks",maxBlocks==0?minBlocks:maxBlocks).put("population_floor",populationFloor).put("population_ceiling",populationCeiling).put("consumption",consumption).put("max_item_bonus",itemBonus).put("influence",settlementInfluence(type));}catch(Exception ignored){}return out;
    }

    public int armyMapPenalty(int attackIndex,boolean agile){return pf2.mapPenalty(attackIndex,agile);}

    public JSONObject resolveArmyCheck(String action,int natural,int modifier,int dc,int attackIndex,boolean agile){
        String kind=key(action);int map=(kind.contains("attack")||kind.contains("strike")||kind.contains("ataque"))?armyMapPenalty(attackIndex,agile):0;
        JSONObject result=pf2.resolveCheck(natural,modifier+map,dc);
        try{result.put("subsystem","warfare").put("action",kind).put("base_modifier",modifier).put("multiple_attack_penalty",map).put("hits",armyHits(result.optString("degree"))).put("rout_check",result.optString("degree").equals("CRITICAL_FAILURE"));}catch(Exception ignored){}
        return result;
    }

    public int armyHits(String degree){if("CRITICAL_SUCCESS".equalsIgnoreCase(degree))return 2;if("SUCCESS".equalsIgnoreCase(degree))return 1;return 0;}

    public JSONObject applyArmyDamage(int hp,int maxHp,int hits,int weakness,int resistance,int routThreshold){
        int raw=Math.max(0,hits)+Math.max(0,weakness),damage=Math.max(0,raw-Math.max(0,resistance));int next=Math.max(0,Math.min(Math.max(1,maxHp),hp)-damage);
        JSONObject out=new JSONObject();try{out.put("damage",damage).put("hp",next).put("defeated",next<=0).put("destroyed",false).put("routed",false);}catch(Exception ignored){}return out;
    }

    public JSONObject companionInfluence(int current,int delta){
        int next=Math.max(0,current+delta);JSONObject out=new JSONObject();try{out.put("influence",next).put("changed",next-current).put("attitude_policy","source_specific_thresholds");}catch(Exception ignored){}return out;
    }

    public int resourceDieSize(int size){int value=Math.max(1,size);return value>=100?12:value>=50?10:value>=25?8:value>=10?6:4;}
    public int sizeControlModifier(int size){int value=Math.max(1,size);return value>=100?4:value>=50?3:value>=25?2:value>=10?1:0;}
    public int commodityStorage(int size){int value=Math.max(1,size);return value>=100?20:value>=50?16:value>=25?12:value>=10?8:4;}
    public int investedLeadershipBonus(int level){return level>=16?3:level>=8?2:1;}
    public String roleAbility(String role){String value=ROLE_ABILITIES.get(key(role));return value==null?"":value;}

    public String authority(){return "Kingmaker PF2e: turno de Reino em Upkeep, Commerce, Activity e Event; perícias do Reino usam quatro graus PF2e; posse de hexes, assentamentos, estruturas, recursos, Unrest/Ruin, liderança, exércitos, influência de companheiros e acampamento são persistidos e resolvidos localmente; o Atlas oficial é somente leitura; a IA apenas narra ou propõe.";}

    public static String abilityLabel(String ability){String k=key(ability);if("culture".equals(k))return "Cultura";if("economy".equals(k))return "Economia";if("loyalty".equals(k))return "Lealdade";return "Estabilidade";}
    public static String skillLabel(String skill){String k=key(skill);if("agriculture".equals(k))return "Agricultura";if("arts".equals(k))return "Artes";if("boating".equals(k))return "Navegação";if("defense".equals(k))return "Defesa";if("engineering".equals(k))return "Engenharia";if("exploration".equals(k))return "Exploração";if("folklore".equals(k))return "Folclore";if("industry".equals(k))return "Indústria";if("intrigue".equals(k))return "Intriga";if("magic".equals(k))return "Magia";if("politics".equals(k))return "Política";if("scholarship".equals(k))return "Erudição";if("statecraft".equals(k))return "Administração";if("trade".equals(k))return "Comércio";if("warfare".equals(k))return "Guerra";if("wilderness".equals(k))return "Ermos";return skill==null?"":skill;}
    private static String key(String value){if(value==null)return "";return Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-z0-9]+","_").replaceAll("^_+|_+$","");}
}
