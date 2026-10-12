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
    public static final int BASE_EVENT_DC=16;
    public static final int ANARCHY_UNREST_THRESHOLD=20;

    private static final int[] KINGDOM_CONTROL_DCS={14,15,16,18,20,22,23,24,26,27,28,30,31,32,34,35,36,38,39,40};
    private static final LinkedHashMap<String,String> SKILL_ABILITIES=new LinkedHashMap<String,String>();
    private static final LinkedHashMap<String,String> ROLE_ABILITIES=new LinkedHashMap<String,String>();

    public static final String[] RESTORATIVE_ACTIVITIES={
        "quell_unrest", "rest_and_relax", "proclamation", "supernatural_solution", "improve_lifestyle"
    };

    public static final String[] ACTIVITIES={
        "new_leadership", "proclamation", "rest_and_relax", "supernatural_solution",
        "take_charge", "send_diplomatic_envoy", "pledge_of_fealty", "request_foreign_aid", "quell_unrest",
        "claim_hex", "abandon_hex", "clear_hex", "reconnoiter_hex",
        "build_roads", "irrigate", "establish_settlement", "work_sites", "fortify_hex",
        "build_structure", "demolish", "repair_structure", "upgrade_settlement",
        "collect_taxes", "improve_lifestyle", "tap_treasury", "trade_commodities",
        "manage_trade_agreements", "establish_trade_agreement", "capital_investment"
    };

    private final Pathfinder2eRulesEngine pf2=new Pathfinder2eRulesEngine();
    private final KingmakerWarfareEngine warfare=new KingmakerWarfareEngine();

    public KingmakerWarfareEngine warfare(){return warfare;}

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

    public static int unrestPenalty(int unrest){int value=Math.max(0,unrest);return value>=15?4:value>=10?3:value>=5?2:value>=1?1:0;}

    public static boolean isAnarchy(int unrest){return Math.max(0,unrest)>=ANARCHY_UNREST_THRESHOLD;}

    public static boolean isRestorativeActivity(String activityKey){
        String k=key(activityKey);
        for(String item:RESTORATIVE_ACTIVITIES)if(item.equals(k))return true;
        return false;
    }

    public static boolean canPerformActivity(String activityKey,int unrest){
        if(isAnarchy(unrest))return isRestorativeActivity(activityKey);
        return true;
    }

    public static String ruinForAbility(String ability){
        String a=key(ability);
        if("culture".equals(a))return "corruption";
        if("economy".equals(a))return "crime";
        if("loyalty".equals(a))return "strife";
        return "decay";
    }

    public static String abilityForRuin(String ruin){
        String r=key(ruin);
        if("corruption".equals(r))return "culture";
        if("crime".equals(r))return "economy";
        if("strife".equals(r))return "loyalty";
        return "stability";
    }

    public static String ruinLabel(String ruin){
        String r=key(ruin);
        if("corruption".equals(r))return "Corrupção";
        if("crime".equals(r))return "Crime";
        if("strife".equals(r))return "Conflito";
        if("decay".equals(r))return "Decadência";
        return ruin==null?"":ruin;
    }

    public static int ruinPenalty(int ruin,int threshold){int t=Math.max(1,threshold);return Math.max(0,ruin)/t;}

    public static int nextEventDc(int currentDc,boolean eventOccurred){
        if(eventOccurred)return BASE_EVENT_DC;
        return Math.max(1,currentDc-5);
    }

    public static boolean checkEventOccurs(int flatRoll,int currentDc){
        return flatRoll>=Math.max(1,currentDc);
    }

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
        int raw=Math.max(0,hits)+Math.max(0,weakness),damage=Math.max(0,raw-Math.max(0,resistance));
        int max=Math.max(1,maxHp);
        int current=Math.max(0,Math.min(max,hp));
        int next=Math.max(0,current-damage);
        JSONObject out=new JSONObject();
        try{out.put("damage",damage).put("hp",next).put("max_hp",max).put("defeated",next<=0).put("destroyed",false).put("rout_threshold",Math.max(1,routThreshold)).put("routed",next>0&&next<=Math.max(1,routThreshold));}catch(Exception ignored){}
        return out;
    }

    public JSONObject applyArmyDamage(int hp,int maxHp,int hits,int weakness,int resistance,int routThreshold,boolean criticalFinish){
        JSONObject out=applyArmyDamage(hp,maxHp,hits,weakness,resistance,routThreshold);
        if(criticalFinish&&out.optBoolean("defeated")){
            try{out.put("destroyed",true);}catch(Exception ignored){}
        }
        return out;
    }

    public JSONObject resolveWarAttack(int natural,int attackModifier,int targetAc,int attackIndex,boolean agile,int weakness,int resistance){
        int map=armyMapPenalty(attackIndex,agile);
        JSONObject check=pf2.resolveCheck(natural,attackModifier+map,targetAc);
        int hits=armyHits(check.optString("degree","FAILURE"));
        JSONObject out=new JSONObject();
        try{
            out.put("check",check).put("hits",hits).put("attack_index",attackIndex).put("map_penalty",map);
        }catch(Exception ignored){}
        return out;
    }

    public JSONObject resolveWarRoutCheck(int natural,int moraleModifier,int moraleDc){
        JSONObject check=pf2.resolveCheck(natural,moraleModifier,moraleDc);
        String deg=check.optString("degree","FAILURE");
        boolean routed="CRITICAL_FAILURE".equals(deg);
        JSONObject out=new JSONObject();
        try{
            out.put("check",check).put("routed",routed).put("shaken","FAILURE".equals(deg));
        }catch(Exception ignored){}
        return out;
    }

    public JSONObject resolveWarRally(int natural,int moraleModifier,int moraleDc){
        JSONObject check=pf2.resolveCheck(natural,moraleModifier,moraleDc);
        String deg=check.optString("degree","FAILURE");
        boolean success="CRITICAL_SUCCESS".equals(deg)||"SUCCESS".equals(deg);
        JSONObject out=new JSONObject();
        try{
            out.put("check",check).put("rallied",success);
        }catch(Exception ignored){}
        return out;
    }

    public JSONObject evaluateArmyConditions(JSONArray conditions){
        JSONObject out=new JSONObject();
        try{
            boolean routed=false, shaken=false, pinned=false, defeated=false, destroyed=false;
            int moralePen=0, attackPen=0;
            if(conditions!=null){
                for(int i=0;i<conditions.length();i++){
                    String c=String.valueOf(conditions.opt(i)).toLowerCase(Locale.US);
                    if(c.contains("rout"))routed=true;
                    if(c.contains("shaken")){shaken=true;moralePen=Math.min(moralePen,-1);attackPen=Math.min(attackPen,-1);}
                    if(c.contains("pinned"))pinned=true;
                    if(c.contains("fright")){moralePen=Math.min(moralePen,-1);attackPen=Math.min(attackPen,-1);}
                    if(c.contains("defeat"))defeated=true;
                    if(c.contains("destroy"))destroyed=true;
                }
            }
            out.put("routed",routed).put("shaken",shaken).put("pinned",pinned)
               .put("defeated",defeated).put("destroyed",destroyed)
               .put("can_act",!defeated&&!destroyed&&!routed)
               .put("morale_penalty",moralePen).put("attack_penalty",attackPen);
        }catch(Exception ignored){}
        return out;
    }

    public String companionAttitude(int influence){
        int val=influence;
        if(val<=5)return "hostile";
        if(val<=11)return "unfriendly";
        if(val<=19)return "indifferent";
        if(val<=29)return "friendly";
        return "helpful";
    }

    public JSONObject companionMilestones(int influence,String questStatus){
        JSONObject out=new JSONObject();
        try{
            String attitude=companionAttitude(influence);
            boolean helpful="helpful".equals(attitude);
            boolean questComplete="complete".equalsIgnoreCase(questStatus)||"completed".equalsIgnoreCase(questStatus);
            int advisorBonus=helpful?1:0;
            out.put("influence",influence).put("attitude",attitude).put("helpful",helpful)
               .put("quest_complete",questComplete).put("advisor_circumstance_bonus",advisorBonus);
        }catch(Exception ignored){}
        return out;
    }

// =========================================================================
    // Subsistema de Companheiros (Kingmaker PF2e)
    // =========================================================================

    public static final String[] COMPANION_KEYS = {
        "amiri", "ekundayo", "jubilost", "linzi", "nok_nok", "tristian",
        "valerie", "harrim", "jaethal", "kalikke_kanerah", "octavia", "regongar"
    };

    public static final String[] ATTITUDES = {
        "hostile", "unfriendly", "indifferent", "friendly", "helpful"
    };

    public static String attitudeLabel(String attitude) {
        String k = key(attitude);
        if ("hostile".equals(k) || "hostil".equals(k)) return "Hostil";
        if ("unfriendly".equals(k) || "inamistoso".equals(k)) return "Inamistoso";
        if ("indifferent".equals(k) || "indiferente".equals(k)) return "Indiferente";
        if ("friendly".equals(k) || "amistoso".equals(k)) return "Amistoso";
        if ("helpful".equals(k) || "prestativo".equals(k)) return "Prestativo";
        return "Indiferente";
    }

    public static String canonicalCompanionKey(String raw) {
        String k = key(raw);
        if (k.equals("jubiloso")) return "jubilost";
        if (k.equals("noknok")) return "nok_nok";
        if (k.equals("otavia")) return "octavia";
        if (k.equals("kalikke") || k.equals("kanerah") || k.equals("kalikke_e_kanerah")) return "kalikke_kanerah";
        return k;
    }

    public JSONObject companionProfile(String companionKey) {
        String k = canonicalCompanionKey(companionKey);
        JSONObject c = new JSONObject();
        try {
            if ("amiri".equals(k)) {
                c.put("companion_key", "amiri").put("name", "Amiri").put("class_name", "Bárbara").put("ancestry", "Humana")
                 .put("initial_attitude", "indifferent").put("initial_attitude_label", "Indiferente")
                 .put("discovery", "Percepção CD 15, Sociedade CD 17, Saber de Guerra CD 13")
                 .put("influence_skills", "Atletismo CD 13, Intimidação CD 15, Sobrevivência CD 17, Performance CD 19, Diplomacia CD 19, Dissimulação CD 21")
                 .put("camping_activity", "Aprimorar Armas").put("camping_activity_key", "amiri_aprimorar_armas")
                 .put("favorite_meal", "Guisado de monstro").put("favorite_meal_key", "guisado_de_monstro")
                 .put("leadership_role", "general").put("leadership_role_label", "General")
                 .put("personal_quest", "O Acampamento dos Seis Ursos")
                 .put("personal_quest_summary", "Provar sua força e libertar sua tribo do jugo de gigantes nos Ermos.")
                 .put("resistances", "Desdenha postura frágil ou covardia; distrai o grupo se não interagir.")
                 .put("weaknesses", "Respeita demonstrações físicas de bravura e determinação indomável.");
                JSONArray t = new JSONArray();
                t.put(new JSONObject().put("points", 2).put("attitude", "friendly").put("attitude_label", "Amistoso")
                     .put("reward_id", "amiri_friendly").put("reward_title", "Confiança Bárbara").put("reward_type", "combat_ability")
                     .put("description", "Amiri aceita sugestões táticas e luta com ferocidade em conjunto com o grupo.")
                     .put("mechanical_bonus", new JSONObject().put("type", "tactical_support").put("bonus", 1)));
                t.put(new JSONObject().put("points", 4).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "amiri_helpful").put("reward_title", "Irmandade de Aço").put("reward_type", "boon")
                     .put("description", "+1 de bônus de circunstância em testes de Atletismo e Intimidação quando adjacente a aliados.")
                     .put("mechanical_bonus", new JSONObject().put("type", "stat_bonus").put("bonus", 1)));
                t.put(new JSONObject().put("points", 6).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "amiri_camping").put("reward_title", "Atividade: Aprimorar Armas").put("reward_type", "camping_activity")
                     .put("description", "Desbloqueia a atividade de acampamento Aprimorar Armas (+1 de dano de circunstância no próximo combate).")
                     .put("mechanical_bonus", new JSONObject().put("type", "camping_activity").put("activity", "amiri_aprimorar_armas")));
                t.put(new JSONObject().put("points", 8).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "amiri_reward").put("reward_title", "Presente de Amiri: Adaga +1").put("reward_type", "gift_item")
                     .put("description", "Amiri presenteia os heróis com uma Adaga +1 e concorda formalmente em assumir a função de General.")
                     .put("mechanical_bonus", new JSONObject().put("type", "gift_and_role").put("item", "Adaga +1").put("role", "general")));
                c.put("thresholds", t);
            } else if ("ekundayo".equals(k)) {
                c.put("companion_key", "ekundayo").put("name", "Ekundayo").put("class_name", "Patrulheiro").put("ancestry", "Humano")
                 .put("initial_attitude", "unfriendly").put("initial_attitude_label", "Inamistoso")
                 .put("discovery", "Saber de Gigantes CD 13, Percepção CD 15, Sobrevivência CD 17")
                 .put("influence_skills", "Saber de Gigantes CD 13, Sobrevivência CD 15, Natureza CD 17, Intimidação CD 19, Diplomacia CD 21, Dissimulação CD 23")
                 .put("camping_activity", "Sobrevivência nos Ermos").put("camping_activity_key", "ekundayo_sobrevivencia_ermos")
                 .put("favorite_meal", "Assado do caçador").put("favorite_meal_key", "assado_do_cacador")
                 .put("leadership_role", "warden").put("leadership_role_label", "Guardião")
                 .put("personal_quest", "Vingança por Kargadd")
                 .put("personal_quest_summary", "Rastrear e abater os gigantes que massacraram sua família.")
                 .put("resistances", "Penalidade severa de -2 caso alguém o chame pelo apelido sem consentimento prévio.")
                 .put("weaknesses", "Compreensão de sua dor e respeito solene à memória de sua família.");
                JSONArray t = new JSONArray();
                t.put(new JSONObject().put("points", 2).put("attitude", "indifferent").put("attitude_label", "Indiferente")
                     .put("reward_id", "ekundayo_indifferent").put("reward_title", "Trégua Silenciosa").put("reward_type", "attitude")
                     .put("description", "Ekundayo tolera dialogar e remove a atitude inicial hostil/inamistosa.")
                     .put("mechanical_bonus", new JSONObject().put("type", "attitude_shift")));
                t.put(new JSONObject().put("points", 4).put("attitude", "friendly").put("attitude_label", "Amistoso")
                     .put("reward_id", "ekundayo_friendly").put("reward_title", "Companheiro de Caça").put("reward_type", "boon")
                     .put("description", "Ekundayo viaja com o grupo e concede +1 de bônus de status em Sobrevivência para caça e rastreamento.")
                     .put("mechanical_bonus", new JSONObject().put("type", "stat_bonus").put("bonus", 1)));
                t.put(new JSONObject().put("points", 6).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "ekundayo_boon").put("reward_title", "Mira do Vingador (30 XP)").put("reward_type", "combat_ability")
                     .put("description", "Concede 30 XP de marco e +1 de bônus de status em ataques à distância na primeira rodada.")
                     .put("mechanical_bonus", new JSONObject().put("type", "combat_buff").put("bonus", 1).put("xp", 30)));
                t.put(new JSONObject().put("points", 8).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "ekundayo_reward").put("reward_title", "Sobrevivência nos Ermos (30 XP)").put("reward_type", "camping_activity")
                     .put("description", "Libera a atividade Sobrevivência nos Ermos, concede 30 XP e aceita o cargo de Guardião do Reino.")
                     .put("mechanical_bonus", new JSONObject().put("type", "camping_and_role").put("activity", "ekundayo_sobrevivencia_ermos").put("role", "warden").put("xp", 30)));
                c.put("thresholds", t);
            } else if ("jubilost".equals(k)) {
                c.put("companion_key", "jubilost").put("name", "Jubiloso Nartropo").put("class_name", "Alquimista").put("ancestry", "Gnomo")
                 .put("initial_attitude", "indifferent").put("initial_attitude_label", "Indiferente")
                 .put("discovery", "Saber Acadêmico CD 13, Manufatura CD 15, Percepção CD 17")
                 .put("influence_skills", "Saber Acadêmico CD 13, Manufatura CD 15, Saber Geral CD 17, Intimidação CD 19, Diplomacia CD 21, Dissimulação CD 23")
                 .put("camping_activity", "Gerenciar Acampamento").put("camping_activity_key", "jubilost_gerenciar_acampamento")
                 .put("favorite_meal", "Sopa de cebola").put("favorite_meal_key", "sopa_de_cebola")
                 .put("leadership_role", "treasurer").put("leadership_role_label", "Tesoureiro")
                 .put("personal_quest", "A Busca pela Cura do Branqueamento")
                 .put("personal_quest_summary", "Pesquisar os mistérios do Primeiro Mundo para evitar o Branqueamento gnômico.")
                 .put("resistances", "Intolerante a debates superficiais, superstições sem embasamento e grosserias.")
                 .put("weaknesses", "Fascinado por tópicos acadêmicos rigorosos, história e teoria de compostos alquímicos.");
                JSONArray t = new JSONArray();
                t.put(new JSONObject().put("points", 2).put("attitude", "friendly").put("attitude_label", "Amistoso")
                     .put("reward_id", "jubilost_friendly").put("reward_title", "Debate Acadêmico").put("reward_type", "attitude")
                     .put("description", "Jubiloso junta-se às expedições e auxilia com bombas e extratos alquímicos.")
                     .put("mechanical_bonus", new JSONObject().put("type", "combat_support")));
                t.put(new JSONObject().put("points", 4).put("attitude", "friendly").put("attitude_label", "Amistoso")
                     .put("reward_id", "jubilost_infused").put("reward_title", "Partilha de Fórmulas").put("reward_type", "formula_access")
                     .put("description", "Disponibiliza fórmulas alquímicas e itens infundidos diários para o grupo.")
                     .put("mechanical_bonus", new JSONObject().put("type", "formula_share")));
                t.put(new JSONObject().put("points", 6).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "jubilost_camping").put("reward_title", "Gerenciar Acampamento (30 XP)").put("reward_type", "camping_activity")
                     .put("description", "Libera a atividade Gerenciar Acampamento (+1 circ em atividades de acampamento) e concede 30 XP.")
                     .put("mechanical_bonus", new JSONObject().put("type", "camping_activity").put("activity", "jubilost_gerenciar_acampamento").put("xp", 30)));
                t.put(new JSONObject().put("points", 8).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "jubilost_treasurer").put("reward_title", "Ministro do Tesouro (30 XP)").put("reward_type", "kingdom_role")
                     .put("description", "Assume a função de Tesoureiro (+1 em testes de Economia do Reino) e concede 30 XP.")
                     .put("mechanical_bonus", new JSONObject().put("type", "role_and_xp").put("role", "treasurer").put("xp", 30)));
                c.put("thresholds", t);
            } else if ("linzi".equals(k)) {
                c.put("companion_key", "linzi").put("name", "Linzi").put("class_name", "Barda").put("ancestry", "Halfling")
                 .put("initial_attitude", "friendly").put("initial_attitude_label", "Amistoso")
                 .put("discovery", "Ocultismo CD 17, Percepção CD 15, Sociedade CD 13")
                 .put("influence_skills", "Performance CD 13, Sociedade CD 15, Ocultismo CD 17, Dissimulação CD 19, Diplomacia CD 19, Intimidação CD 21")
                 .put("camping_activity", "Melhorar Confiança").put("camping_activity_key", "linzi_melhorar_confianca")
                 .put("favorite_meal", "Panqueca doce").put("favorite_meal_key", "panqueca_doce")
                 .put("leadership_role", "counselor").put("leadership_role_label", "Conselheira")
                 .put("personal_quest", "Crônica de uma Nova Nação")
                 .put("personal_quest_summary", "Registrar em livro a epopeia da fundação do reino e desmascarar a tirania de Pitax.")
                 .put("resistances", "Penalidade de -2 se alguém insultar as artes ou elogiar censores e déspotas.")
                 .put("weaknesses", "Encantada por ambições nobres, poemas, canções e aspirações heroicas.");
                JSONArray t = new JSONArray();
                t.put(new JSONObject().put("points", 2).put("attitude", "friendly").put("attitude_label", "Amistoso")
                     .put("reward_id", "linzi_friendly").put("reward_title", "Crônica da Aliança").put("reward_type", "boon")
                     .put("description", "Linzi dedica baladas aos pioneiros, concedendo +1 de circunstância em testes de Diplomacia no Reino.")
                     .put("mechanical_bonus", new JSONObject().put("type", "stat_bonus").put("bonus", 1)));
                t.put(new JSONObject().put("points", 4).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "linzi_confidence").put("reward_title", "Melhorar Confiança").put("reward_type", "camping_activity")
                     .put("description", "Libera a atividade Melhorar Confiança (+1 status em salvamentos de Vontade durante o acampamento).")
                     .put("mechanical_bonus", new JSONObject().put("type", "camping_activity").put("activity", "linzi_melhorar_confianca")));
                t.put(new JSONObject().put("points", 6).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "linzi_spells").put("reward_title", "Poesia Encantatória").put("reward_type", "spell_access")
                     .put("description", "Compartilha as magias raras Palavra de Revisão e Disparo de Tinta, e aceita o cargo de Conselheira.")
                     .put("mechanical_bonus", new JSONObject().put("type", "spells_and_role").put("role", "counselor")));
                t.put(new JSONObject().put("points", 8).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "linzi_talismans").put("reward_title", "Talismãs de Linzi").put("reward_type", "gift_item")
                     .put("description", "Linzi entrega ao grupo os talismãs Gato de Jade e Pantera de Ônix em honra heroica.")
                     .put("mechanical_bonus", new JSONObject().put("type", "item_gift").put("items", "Gato de Jade, Pantera de Ônix")));
                c.put("thresholds", t);
            } else if ("nok_nok".equals(k)) {
                c.put("companion_key", "nok_nok").put("name", "Nok-Nok").put("class_name", "Ladino").put("ancestry", "Goblin")
                 .put("initial_attitude", "unfriendly").put("initial_attitude_label", "Inamistoso")
                 .put("discovery", "Sobrevivência CD 13, Ladroagem CD 15, Religião CD 17")
                 .put("influence_skills", "Saber de Submundo CD 18, Ladroagem CD 15, Intimidação CD 17, Diplomacia CD 19, Dissimulação CD 17")
                 .put("camping_activity", "Colocar Armadilhas").put("camping_activity_key", "nok_nok_colocar_armadilhas")
                 .put("favorite_meal", "Perna de aranha assada").put("favorite_meal_key", "perna_de_aranha_assada")
                 .put("leadership_role", "warden").put("leadership_role_label", "Guardião")
                 .put("personal_quest", "O Quinto Herói Goblin")
                 .put("personal_quest_summary", "Construir santuários excêntricos e provar que é um herói valoroso e não um monstro.")
                 .put("resistances", "Fica raivoso e desconfiado quando zombam de sua estatura ou o tratam como escória.")
                 .put("weaknesses", "Vaidade estimulada por títulos pomposos, elogios a sua furtividade e agrados comestíveis.");
                JSONArray t = new JSONArray();
                t.put(new JSONObject().put("points", 2).put("attitude", "indifferent").put("attitude_label", "Indiferente")
                     .put("reward_id", "nok_nok_hero_dream").put("reward_title", "Sonho de Herói").put("reward_type", "attitude")
                     .put("description", "Nok-Nok se fascina com a ideia de honra e concede +1 de circunstância em testes para influenciá-lo.")
                     .put("mechanical_bonus", new JSONObject().put("type", "influence_bonus").put("bonus", 1)));
                t.put(new JSONObject().put("points", 4).put("attitude", "friendly").put("attitude_label", "Amistoso")
                     .put("reward_id", "nok_nok_traps").put("reward_title", "Colocar Armadilhas").put("reward_type", "camping_activity")
                     .put("description", "Libera a atividade de acampamento Colocar Armadilhas no perímetro defensivo.")
                     .put("mechanical_bonus", new JSONObject().put("type", "camping_activity").put("activity", "nok_nok_colocar_armadilhas")));
                t.put(new JSONObject().put("points", 6).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "nok_nok_code").put("reward_title", "Código Goblin").put("reward_type", "skill_bonus")
                     .put("description", "Nok-Nok ensina astúcias de emboscada (+1 de circunstância em Furtividade e Ladroagem).")
                     .put("mechanical_bonus", new JSONObject().put("type", "stat_bonus").put("bonus", 1)));
                t.put(new JSONObject().put("points", 8).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "nok_nok_warden").put("reward_title", "Herói de Quatro Braços").put("reward_type", "gift_item")
                     .put("description", "Presenteia os líderes com adagas afiadas e assume como Guardião Noturno do Reino.")
                     .put("mechanical_bonus", new JSONObject().put("type", "gift_and_role").put("role", "warden")));
                c.put("thresholds", t);
            } else if ("tristian".equals(k)) {
                c.put("companion_key", "tristian").put("name", "Tristian").put("class_name", "Clérigo").put("ancestry", "Aasimar")
                 .put("initial_attitude", "friendly").put("initial_attitude_label", "Amistoso")
                 .put("discovery", "Saber de Sarenrae CD 13, Religião CD 15, Percepção CD 17")
                 .put("influence_skills", "Saber de Sarenrae CD 13, Medicina CD 15, Religião CD 17, Diplomacia CD 19, Dissimulação CD 21, Intimidação CD 23")
                 .put("camping_activity", "Benção da Flor do Alvorecer").put("camping_activity_key", "tristian_bencao_alvorecer")
                 .put("favorite_meal", "Torta de frutakame").put("favorite_meal_key", "torta_de_frutakame")
                 .put("leadership_role", "counselor").put("leadership_role_label", "Conselheiro")
                 .put("personal_quest", "Segredos da Luz e Redenção")
                 .put("personal_quest_summary", "Descobrir a verdade sobre sua herança celestial e buscar expiação sob a graça de Sarenrae.")
                 .put("resistances", "Repulsa absoluta a atos de tortura, chacina desnecessária e profanação sacrílega.")
                 .put("weaknesses", "Coração aberto à compaixão sincera, socorro aos aflitos e preces de esperança.");
                JSONArray t = new JSONArray();
                t.put(new JSONObject().put("points", 2).put("attitude", "friendly").put("attitude_label", "Amistoso")
                     .put("reward_id", "tristian_support").put("reward_title", "Prece Protetora").put("reward_type", "combat_ability")
                     .put("description", "Tristian utiliza seus recursos divinos e preces ativas para curar aliados em perigo.")
                     .put("mechanical_bonus", new JSONObject().put("type", "combat_healing")));
                t.put(new JSONObject().put("points", 4).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "tristian_spells").put("reward_title", "Chamas da Alvorada").put("reward_type", "spell_access")
                     .put("description", "Concede acesso às magias raras Lâmina Ardente e Luz da Flor do Alvorecer.")
                     .put("mechanical_bonus", new JSONObject().put("type", "spells").put("spells", "Lâmina Ardente, Luz da Flor do Alvorecer")));
                t.put(new JSONObject().put("points", 6).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "tristian_camping").put("reward_title", "Benção da Flor do Alvorecer").put("reward_type", "camping_activity")
                     .put("description", "Libera a atividade Benção da Flor do Alvorecer (+1d8 PVs de cura extra e alívio de fadiga no descanso).")
                     .put("mechanical_bonus", new JSONObject().put("type", "camping_activity").put("activity", "tristian_bencao_alvorecer")));
                t.put(new JSONObject().put("points", 8).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "tristian_counselor").put("reward_title", "Guia Espiritual de Sarenrae").put("reward_type", "kingdom_role")
                     .put("description", "Assume a função de Conselheiro do Reino, concedendo bônus em testes de Estabilidade e Cultura.")
                     .put("mechanical_bonus", new JSONObject().put("type", "role").put("role", "counselor")));
                c.put("thresholds", t);
            } else if ("valerie".equals(k)) {
                c.put("companion_key", "valerie").put("name", "Valerie").put("class_name", "Guerreira").put("ancestry", "Humana")
                 .put("initial_attitude", "indifferent").put("initial_attitude_label", "Indiferente")
                 .put("discovery", "Percepção CD 15, Sociedade CD 17, Saber de Guerra CD 13")
                 .put("influence_skills", "Diplomacia CD 13, Saber de Guerra CD 15, Sociedade CD 17, Dissimulação CD 19, Intimidação CD 21")
                 .put("camping_activity", "Preservar Armadura").put("camping_activity_key", "valerie_preservar_armadura")
                 .put("favorite_meal", "Ostras de Rosalva").put("favorite_meal_key", "ostras_de_rosalva")
                 .put("leadership_role", "general").put("leadership_role_label", "General")
                 .put("personal_quest", "O Julgamento da Graça")
                 .put("personal_quest_summary", "Confrontar Sir Fredero Sinnet e libertar-se da perseguição dos devotos obstinados de Shelyn.")
                 .put("resistances", "Rejeita bajulações sobre sua beleza exterior e devotos fanáticos de Shelyn.")
                 .put("weaknesses", "Respeito à honra militar legítima, disciplina marcial e compromisso com o dever civil.");
                JSONArray t = new JSONArray();
                t.put(new JSONObject().put("points", 2).put("attitude", "friendly").put("attitude_label", "Amistoso")
                     .put("reward_id", "valerie_friendly").put("reward_title", "Muralha Disciplinada").put("reward_type", "boon")
                     .put("description", "Valerie assume a vanguarda e concede +1 de circunstância em testes contra medo para aliados adjacentes.")
                     .put("mechanical_bonus", new JSONObject().put("type", "stat_bonus").put("bonus", 1)));
                t.put(new JSONObject().put("points", 4).put("attitude", "friendly").put("attitude_label", "Amistoso")
                     .put("reward_id", "valerie_armor").put("reward_title", "Preservar Armadura").put("reward_type", "camping_activity")
                     .put("description", "Libera a atividade de acampamento Preservar Armadura (+1 de bônus de item na CA de escudos/armaduras).")
                     .put("mechanical_bonus", new JSONObject().put("type", "camping_activity").put("activity", "valerie_preservar_armadura")));
                t.put(new JSONObject().put("points", 6).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "valerie_tactics").put("reward_title", "Defesa Inabalável").put("reward_type", "combat_ability")
                     .put("description", "Concede +1 em testes de Defesa do Reino e treinamento em manobras de bloqueio.")
                     .put("mechanical_bonus", new JSONObject().put("type", "defense_bonus").put("bonus", 1)));
                t.put(new JSONObject().put("points", 8).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "valerie_reward").put("reward_title", "Ordem de Ferro").put("reward_type", "gift_item")
                     .put("description", "Presenteia o grupo com um Mutagênio Juggernaut Moderado e assume a função de General do Reino.")
                     .put("mechanical_bonus", new JSONObject().put("type", "gift_and_role").put("item", "Mutagênio Juggernaut Moderado").put("role", "general")));
                c.put("thresholds", t);
            } else if ("harrim".equals(k)) {
                c.put("companion_key", "harrim").put("name", "Harrim").put("class_name", "Clérigo").put("ancestry", "Anão")
                 .put("initial_attitude", "indifferent").put("initial_attitude_label", "Indiferente")
                 .put("discovery", "Saber dos Anões CD 13, Religião CD 15, Percepção CD 17")
                 .put("influence_skills", "Saber de Groetus CD 13, Saber dos Anões CD 15, Religião CD 17, Diplomacia CD 19, Dissimulação CD 21, Performance CD 21")
                 .put("camping_activity", "Fazer Parte da Noite").put("camping_activity_key", "harrim_fazer_parte_da_noite")
                 .put("favorite_meal", "Buchada de ovelha").put("favorite_meal_key", "buchada_de_ovelha")
                 .put("leadership_role", "magister").put("leadership_role_label", "Magíster")
                 .put("personal_quest", "O Chamado do Fim dos Tempos")
                 .put("personal_quest_summary", "Explorar as ruínas anãs esquecidas e reconciliar a herança de Torag com o fatalismo de Groetus.")
                 .put("resistances", "Irrita-se com otimismo ingênuo, festividades efusivas e deboche religioso.")
                 .put("weaknesses", "Respeito à ancestralidade anã e reflexões filosóficas sobre a transitoriedade do mundo.");
                JSONArray t = new JSONArray();
                t.put(new JSONObject().put("points", 2).put("attitude", "indifferent").put("attitude_label", "Indiferente")
                     .put("reward_id", "harrim_tolerance").put("reward_title", "Aceitação da Ruína").put("reward_type", "attitude")
                     .put("description", "Harrim tolera viajar e participar dos conselhos sem hostilidade ou desdém.")
                     .put("mechanical_bonus", new JSONObject().put("type", "attitude_shift")));
                t.put(new JSONObject().put("points", 4).put("attitude", "friendly").put("attitude_label", "Amistoso")
                     .put("reward_id", "harrim_potion").put("reward_title", "Consolo Efêmero").put("reward_type", "gift_item")
                     .put("description", "Presenteia com Poção de Cura Menor e presta auxílio com preces nos momentos críticos.")
                     .put("mechanical_bonus", new JSONObject().put("type", "item_gift").put("item", "Poção de Cura Menor")));
                t.put(new JSONObject().put("points", 6).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "harrim_lassitude").put("reward_title", "Lassidão Contagiante").put("reward_type", "spell_access")
                     .put("description", "Concede a magia rara Lassidão Contagiante e libera Fazer Parte da Noite no acampamento.")
                     .put("mechanical_bonus", new JSONObject().put("type", "spell_and_activity").put("spell", "Lassidão Contagiante").put("activity", "harrim_fazer_parte_da_noite")));
                t.put(new JSONObject().put("points", 8).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "harrim_magister").put("reward_title", "Magíster do Crepúsculo").put("reward_type", "kingdom_role")
                     .put("description", "Assume a função de Magíster, oferecendo serenidade contra crises e penalidades de Ruína no Reino.")
                     .put("mechanical_bonus", new JSONObject().put("type", "role").put("role", "magister")));
                c.put("thresholds", t);
            } else if ("jaethal".equals(k)) {
                c.put("companion_key", "jaethal").put("name", "Jaethal").put("class_name", "Campeã").put("ancestry", "Elfa (Morta-viva)")
                 .put("initial_attitude", "indifferent").put("initial_attitude_label", "Indiferente")
                 .put("discovery", "Percepção CD 17, Sociedade CD 15, Religião CD 13")
                 .put("influence_skills", "Diplomacia CD 15, Religião CD 17, Intimidação CD 17, Dissimulação CD 19")
                 .put("camping_activity", "Guardiões Mortos-Vivos").put("camping_activity_key", "jaethal_guardioes_mortos_vivos")
                 .put("favorite_meal", "Arroz colorido").put("favorite_meal_key", "arroz_colorido")
                 .put("leadership_role", "emissary").put("leadership_role_label", "Emissária")
                 .put("personal_quest", "O Sangue de Urgathoa")
                 .put("personal_quest_summary", "Proteger seus ritos secretos e confrontar os fantasmas de seus algozes em Kyonin.")
                 .put("resistances", "Defensiva extrema sobre sua não-vida e seu exílio sangrento de Kyonin.")
                 .put("weaknesses", "Solidão secular oculta; responde bem a interlocutores pragmáticos e diplomáticos.");
                JSONArray t = new JSONArray();
                t.put(new JSONObject().put("points", 2).put("attitude", "indifferent").put("attitude_label", "Indiferente")
                     .put("reward_id", "jaethal_pragmatism").put("reward_title", "Pragmatismo Sombrio").put("reward_type", "attitude")
                     .put("description", "Jaethal reconhece a aliança como benéfica e compartilha conselhos estratégicos sem dissimulação.")
                     .put("mechanical_bonus", new JSONObject().put("type", "attitude_shift")));
                t.put(new JSONObject().put("points", 4).put("attitude", "friendly").put("attitude_label", "Amistoso")
                     .put("reward_id", "jaethal_friendly").put("reward_title", "Pacto da Foice").put("reward_type", "boon")
                     .put("description", "Concede +1 em testes de Religião para discernir ameaças de mortos-vivos e rituais profanos.")
                     .put("mechanical_bonus", new JSONObject().put("type", "stat_bonus").put("bonus", 1)));
                t.put(new JSONObject().put("points", 6).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "jaethal_undead").put("reward_title", "Guardiões Mortos-Vivos").put("reward_type", "camping_activity")
                     .put("description", "Libera a atividade Guardiões Mortos-Vivos no acampamento e concorda em atuar como Emissária.")
                     .put("mechanical_bonus", new JSONObject().put("type", "camping_and_role").put("activity", "jaethal_guardioes_mortos_vivos").put("role", "emissary")));
                t.put(new JSONObject().put("points", 8).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "jaethal_rune").put("reward_title", "Runa Sorvedora").put("reward_type", "formula_access")
                     .put("description", "Presenteia com Pergaminho de Restauração (2º) e a fórmula da Runa de Arma Sorvedora (nível 7).")
                     .put("mechanical_bonus", new JSONObject().put("type", "items").put("item", "Pergaminho de Restauração").put("rune", "Runa Sorvedora")));
                c.put("thresholds", t);
            } else if ("kalikke_kanerah".equals(k)) {
                c.put("companion_key", "kalikke_kanerah").put("name", "Kalikke & Kanerah").put("class_name", "Cineticista").put("ancestry", "Tiefling")
                 .put("initial_attitude", "indifferent").put("initial_attitude_label", "Indiferente")
                 .put("discovery", "Natureza CD 13, Percepção CD 15, Ocultismo CD 17")
                 .put("influence_skills", "Manufatura CD 13, Ladroagem CD 13, Natureza CD 15, Sociedade CD 17, Diplomacia CD 19, Intimidação CD 21, Dissimulação CD 23")
                 .put("camping_activity", "Perigos Aquáticos & Fogueira Elemental").put("camping_activity_key", "kalikke_perigos_aquaticos")
                 .put("favorite_meal", "Sorvete de chocolate").put("favorite_meal_key", "sorvete_de_chocolate")
                 .put("leadership_role", "counselor").put("leadership_role_label", "Conselheira / Emissária")
                 .put("personal_quest", "O Ritual da Separação das Irmãs Tiefling")
                 .put("personal_quest_summary", "Descobrir o mistério da alma duplicada e realizar o ritual para que ambas sobrevivam separadas.")
                 .put("resistances", "Preconceito contra tieflings ou tentativa de forçar a supressão permanente de uma das irmãs.")
                 .put("weaknesses", "Empatia demonstrada com sua condição única e respeito equilibrado tanto pela água quanto pelo fogo.");
                JSONArray t = new JSONArray();
                t.put(new JSONObject().put("points", 2).put("attitude", "indifferent").put("attitude_label", "Indiferente")
                     .put("reward_id", "kalikke_harmony").put("reward_title", "Harmonia Elemental").put("reward_type", "attitude")
                     .put("description", "As irmãs alternam de corpo com cooperação harmônica e apoiam as decisões do grupo.")
                     .put("mechanical_bonus", new JSONObject().put("type", "attitude_shift")));
                t.put(new JSONObject().put("points", 4).put("attitude", "friendly").put("attitude_label", "Amistoso")
                     .put("reward_id", "kalikke_friendly").put("reward_title", "Aliança das Gêmeas").put("reward_type", "kingdom_role")
                     .put("description", "Ajudam no acampamento e aceitam funções (Kalikke: Conselheira; Kanerah: Emissária do Reino).")
                     .put("mechanical_bonus", new JSONObject().put("type", "roles").put("roles", "counselor, emissary")));
                t.put(new JSONObject().put("points", 6).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "kalikke_activities").put("reward_title", "Domínio Elemental").put("reward_type", "camping_activity")
                     .put("description", "Liberam Perigos Aquáticos e Melhorar Fogueira no acampamento, e magias de Rajada Aquosa e Abrasadora.")
                     .put("mechanical_bonus", new JSONObject().put("type", "elemental_activities")));
                t.put(new JSONObject().put("points", 8).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "kalikke_secret").put("reward_title", "Segredo das Almas Gêmeas").put("reward_type", "quest_unlock")
                     .put("description", "Revelam a verdade da maldição que compartilham e inauguram o grande ritual de separação corpórea.")
                     .put("mechanical_bonus", new JSONObject().put("type", "quest_unlock")));
                c.put("thresholds", t);
            } else if ("octavia".equals(k)) {
                c.put("companion_key", "octavia").put("name", "Otávia").put("class_name", "Feiticeira/Maga").put("ancestry", "Meio-elfa")
                 .put("initial_attitude", "indifferent").put("initial_attitude_label", "Indiferente")
                 .put("discovery", "Arcanismo CD 13, Percepção CD 15, Sociedade CD 17")
                 .put("influence_skills", "Performance CD 13, Arcanismo CD 15, Sociedade CD 17, Diplomacia CD 19, Dissimulação CD 21, Intimidação CD 23")
                 .put("camping_activity", "Colocar Alarmes").put("camping_activity_key", "octavia_colocar_alarmes")
                 .put("favorite_meal", "Arroz doce com nozes").put("favorite_meal_key", "arroz_doce_com_nozes")
                 .put("leadership_role", "magister").put("leadership_role_label", "Magístra")
                 .put("personal_quest", "Liberdade da Liga Técnica")
                 .put("personal_quest_summary", "Destruir os perseguidores da Liga Técnica e resgatar sua mãe mantida em cativeiro.")
                 .put("resistances", "Hostilidade caso Regongar seja desrespeitado (+2 nas CDs de influência enquanto Regongar for hostil).")
                 .put("weaknesses", "Amor genuíno pela liberdade, poesia, humor leve e promessas firmes de refúgio seguro.");
                JSONArray t = new JSONArray();
                t.put(new JSONObject().put("points", 2).put("attitude", "friendly").put("attitude_label", "Amistoso")
                     .put("reward_id", "octavia_friendly").put("reward_title", "Acolhimento Nobre").put("reward_type", "attitude")
                     .put("description", "Otávia sente-se protegida e concorda em participar dos acampamentos auxiliando com truques arcanos.")
                     .put("mechanical_bonus", new JSONObject().put("type", "attitude_shift")));
                t.put(new JSONObject().put("points", 4).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "octavia_alarms").put("reward_title", "Colocar Alarmes").put("reward_type", "camping_activity")
                     .put("description", "Libera a atividade Colocar Alarmes e compartilha a magia Visão da Beleza no 3º nível.")
                     .put("mechanical_bonus", new JSONObject().put("type", "camping_and_spell").put("activity", "octavia_colocar_alarmes").put("spell", "Visão da Beleza")));
                t.put(new JSONObject().put("points", 6).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "octavia_magister").put("reward_title", "Magístra da Nova Ordem").put("reward_type", "kingdom_role")
                     .put("description", "Assume a função de Magístra do Reino, com bônus de investidura em testes de Magia e Cultura.")
                     .put("mechanical_bonus", new JSONObject().put("type", "role").put("role", "magister")));
                t.put(new JSONObject().put("points", 8).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "octavia_reward").put("reward_title", "Asas da Liberdade").put("reward_type", "gift_item")
                     .put("description", "Presenteia os líderes com itens arcanos e consagra sua fidelidade inabalável ao Reino.")
                     .put("mechanical_bonus", new JSONObject().put("type", "gift_item").put("item", "Varinha de Truques Mágicos")));
                c.put("thresholds", t);
            } else if ("regongar".equals(k)) {
                c.put("companion_key", "regongar").put("name", "Regongar").put("class_name", "Magus").put("ancestry", "Meio-orc")
                 .put("initial_attitude", "unfriendly").put("initial_attitude_label", "Inamistoso")
                 .put("discovery", "Arcanismo CD 13, Percepção CD 15, Intimidação CD 17")
                 .put("influence_skills", "Intimidação CD 13, Saber de Guerra CD 15, Arcanismo CD 17, Dissimulação CD 19, Diplomacia CD 21")
                 .put("camping_activity", "Postura Intimidante").put("camping_activity_key", "regongar_postura_intimidante")
                 .put("favorite_meal", "Linguiça suculenta").put("favorite_meal_key", "linguica_suculenta")
                 .put("leadership_role", "general").put("leadership_role_label", "General")
                 .put("personal_quest", "Fúria contra a Escravidão")
                 .put("personal_quest_summary", "Exterminar os capatazes da Liga Técnica e quebrar para sempre os grilhões da escravidão.")
                 .put("resistances", "Ordens autoritárias ou demonstrações de fraqueza despertam fúria e deboche.")
                 .put("weaknesses", "Respeito mútuo com guerreiros destemidos e lealdade protetora incondicional a Otávia.");
                JSONArray t = new JSONArray();
                t.put(new JSONObject().put("points", 2).put("attitude", "indifferent").put("attitude_label", "Indiferente")
                     .put("reward_id", "regongar_indifferent").put("reward_title", "Respeito Relutante").put("reward_type", "attitude")
                     .put("description", "Regongar aceita os heróis como companheiros de armas e diminui sua postura hostil.")
                     .put("mechanical_bonus", new JSONObject().put("type", "attitude_shift")));
                t.put(new JSONObject().put("points", 4).put("attitude", "friendly").put("attitude_label", "Amistoso")
                     .put("reward_id", "regongar_posture").put("reward_title", "Postura Intimidante").put("reward_type", "camping_activity")
                     .put("description", "Libera a atividade Postura Intimidante (afasta automaticamente encontros aleatórios de nível baixo/trivial).")
                     .put("mechanical_bonus", new JSONObject().put("type", "camping_activity").put("activity", "regongar_postura_intimidante")));
                t.put(new JSONObject().put("points", 6).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "regongar_rune").put("reward_title", "Runa de Armadura Absorvente").put("reward_type", "formula_access")
                     .put("description", "Compartilha a fórmula rara da Runa de Armadura Absorvente ao alcançar o nível 11.")
                     .put("mechanical_bonus", new JSONObject().put("type", "formula").put("rune", "Runa de Armadura Absorvente")));
                t.put(new JSONObject().put("points", 8).put("attitude", "helpful").put("attitude_label", "Prestativo")
                     .put("reward_id", "regongar_general").put("reward_title", "Senhor da Guerra").put("reward_type", "kingdom_role")
                     .put("description", "Aceita formalmente o cargo de General do Reino, conferindo grande poder ofensivo aos exércitos.")
                     .put("mechanical_bonus", new JSONObject().put("type", "role").put("role", "general")));
                c.put("thresholds", t);
            }
        } catch (Exception ignored) {}
        return c;
    }

    public JSONArray companionCatalog() {
        JSONArray arr = new JSONArray();
        for (String k : COMPANION_KEYS) {
            JSONObject p = companionProfile(k);
            if (p.length() > 0) arr.put(p);
        }
        return arr;
    }

    public JSONObject companionInfluence(int current, int delta) {
        int next = Math.max(0, current + delta);
        String att = next >= 8 ? "helpful" : next >= 4 ? "friendly" : next >= 2 ? "indifferent" : "unfriendly";
        JSONObject out = new JSONObject();
        try {
            out.put("influence", next)
               .put("previous_influence", current)
               .put("changed", next - current)
               .put("attitude", att)
               .put("attitude_label", attitudeLabel(att))
               .put("attitude_policy", "discrete_thresholds")
               .put("newly_unlocked_rewards", new JSONArray());
        } catch (Exception ignored) {}
        return out;
    }

    public JSONObject companionInfluence(String companionKey, int current, int delta, JSONArray alreadyUnlockedRewards) {
        String k = canonicalCompanionKey(companionKey);
        JSONObject profile = companionProfile(k);
        int next = Math.max(0, current + delta);
        String att = profile.optString("initial_attitude", "indifferent");
        JSONArray thresholds = profile.optJSONArray("thresholds");
        if (thresholds == null) thresholds = new JSONArray();

        java.util.HashSet<String> unlockedSet = new java.util.HashSet<String>();
        if (alreadyUnlockedRewards != null) {
            for (int i = 0; i < alreadyUnlockedRewards.length(); i++) {
                unlockedSet.add(alreadyUnlockedRewards.optString(i));
            }
        }

        JSONArray newlyUnlocked = new JSONArray();
        JSONArray allUnlocked = new JSONArray();
        for (String u : unlockedSet) allUnlocked.put(u);

        for (int i = 0; i < thresholds.length(); i++) {
            JSONObject t = thresholds.optJSONObject(i);
            if (t == null) continue;
            int req = t.optInt("points", 0);
            if (next >= req) {
                att = t.optString("attitude", att);
                String rid = t.optString("reward_id", "");
                if (rid.length() > 0 && !unlockedSet.contains(rid)) {
                    unlockedSet.add(rid);
                    allUnlocked.put(rid);
                    newlyUnlocked.put(t);
                }
            }
        }

        JSONObject out = new JSONObject();
        try {
            out.put("companion_key", k)
               .put("name", profile.optString("name", companionKey))
               .put("influence", next)
               .put("previous_influence", current)
               .put("changed", next - current)
               .put("attitude", att)
               .put("attitude_label", attitudeLabel(att))
               .put("attitude_policy", "discrete_thresholds")
               .put("favorite_meal", profile.optString("favorite_meal"))
               .put("camping_activity", profile.optString("camping_activity"))
               .put("personal_quest", profile.optString("personal_quest"))
               .put("newly_unlocked_rewards", newlyUnlocked)
               .put("all_unlocked_rewards", allUnlocked);
        } catch (Exception ignored) {}
        return out;
    }

    // =========================================================================
    // Subsistema de Acampamento — Procedimento Oficial em 5 Passos (Kingmaker)
    // =========================================================================

    public static final Object[][] CAMP_ZONE_RECORDS = {
        {0, "Brevoy", 14, 12, "Planícies"},
        {1, "Interior de Correnterra", 15, 12, "Planícies"},
        {2, "Zonaverde", 16, 14, "Floresta"},
        {3, "Aquadente", 18, 12, "Rio/Planícies"},
        {4, "Kamelândia", 19, 12, "Colinas"},
        {5, "Fronteiras Nodosas", 20, 14, "Floresta"},
        {6, "Colinas Sellen", 20, 12, "Colinas"},
        {7, "Dunrelva", 18, 12, "Pântano"},
        {8, "Elevados de Nomen", 24, 12, "Planícies"},
        {9, "Rochedos de Levenies", 28, 16, "Montanhas"},
        {10, "Linguancho", 32, 14, "Pântano"},
        {11, "Drelev", 28, 12, "Pântano/Colinas"},
        {12, "Lordes Tigres", 28, 12, "Floresta"},
        {13, "Lumeeira", 26, 12, "Colinas"},
        {14, "Terras Baixas de Valebano", 30, 12, "Planícies"},
        {15, "Pitax", 29, 12, "Rio/Planícies"},
        {16, "Terras Altas de Valebano", 35, 12, "Colinas"},
        {17, "Numéria", 36, 12, "Planícies Estéreis"},
        {18, "Mil Vozes", 43, 14, "Floresta Primeval"},
        {19, "Montanhas Branthlend", 41, 16, "Montanhas"}
    };

    public JSONArray campZonesCatalog() {
        JSONArray arr = new JSONArray();
        for (Object[] r : CAMP_ZONE_RECORDS) {
            JSONObject z = new JSONObject();
            try {
                z.put("level", r[0]).put("name", r[1]).put("zone_dc", r[2]).put("encounter_dc", r[3]).put("terrain", r[4]);
            } catch (Exception ignored) {}
            arr.put(z);
        }
        return arr;
    }

    public JSONObject campZone(String zoneNameOrIndex) {
        String query = key(zoneNameOrIndex);
        for (Object[] r : CAMP_ZONE_RECORDS) {
            String nameKey = key(String.valueOf(r[1]));
            String indexStr = String.valueOf(r[0]);
            if (nameKey.equals(query) || indexStr.equals(query) || query.contains(nameKey) || nameKey.contains(query)) {
                JSONObject z = new JSONObject();
                try {
                    z.put("level", r[0]).put("name", r[1]).put("zone_dc", r[2]).put("encounter_dc", r[3]).put("terrain", r[4]);
                } catch (Exception ignored) {}
                return z;
            }
        }
        JSONObject def = new JSONObject();
        try { def.put("level", 2).put("name", "Zonaverde").put("zone_dc", 16).put("encounter_dc", 14).put("terrain", "Floresta"); } catch (Exception ignored) {}
        return def;
    }

    // Passo 1: Seleção de Acampamento
    public JSONObject campsiteSelection(String zoneName, String terrain) {
        JSONObject z = campZone(zoneName);
        JSONObject out = new JSONObject();
        try {
            out.put("step", 1)
               .put("step_name", "Seleção do Local de Acampamento")
               .put("zone_name", z.optString("name"))
               .put("zone_level", z.optInt("level"))
               .put("zone_dc", z.optInt("zone_dc"))
               .put("encounter_dc", z.optInt("encounter_dc"))
               .put("terrain", (terrain != null && terrain.length() > 0) ? terrain : z.optString("terrain"))
               .put("instructions", "Faça um teste de Sobrevivência contra a CD da Zona (" + z.optInt("zone_dc") + ") para preparar o acampamento.");
        } catch (Exception ignored) {}
        return out;
    }

    // Passo 2: Preparar Acampamento
    public JSONObject prepareCamp(int naturalRoll, int modifier, int zoneDc, boolean reused) {
        int dc = Math.max(10, zoneDc);
        int total = naturalRoll + modifier;
        JSONObject check = pf2.resolveCheck(naturalRoll, modifier, dc);
        String degree = check.optString("degree", "SUCCESS");

        int encounterModifier = 0;
        int activityPenalty = 0;
        boolean activitiesAllowed = true;
        boolean immediateEncounter = false;
        boolean firstHourSafe = false;
        String flavor;

        if ("CRITICAL_SUCCESS".equals(degree)) {
            encounterModifier = 2; // aumenta a CD do flat check (+2 CD = mais seguro)
            firstHourSafe = true;
            flavor = "Local perfeito para acampar. A CD de Encontro aumenta em +2 pelas próximas 24 horas, e a primeira hora de atividades não sofre teste simples de encontro.";
        } else if ("SUCCESS".equals(degree)) {
            flavor = "Local aceitável para acampar. As atividades de acampamento podem ser realizadas normalmente.";
        } else if ("FAILURE".equals(degree)) {
            activityPenalty = -2;
            flavor = "Acampamento utilizável, mas imperfeito. Atividades que exigem testes sofrem -2 de penalidade de circunstância.";
        } else { // CRITICAL_FAILURE
            activitiesAllowed = false;
            immediateEncounter = true;
            flavor = "Acampamento desastroso. O local não é adequado para atividades de acampamento (apenas descanso); um teste simples imediato de encontro é disparado!";
        }

        JSONObject out = new JSONObject();
        try {
            out.put("step", 2)
               .put("step_name", "Preparar Acampamento")
               .put("natural_roll", naturalRoll)
               .put("modifier", modifier)
               .put("total", total)
               .put("target_dc", dc)
               .put("degree", degree)
               .put("reused", reused)
               .put("activities_allowed", activitiesAllowed)
               .put("activity_penalty", activityPenalty)
               .put("encounter_dc_bonus", encounterModifier)
               .put("first_hour_safe", firstHourSafe)
               .put("immediate_encounter_check", immediateEncounter)
               .put("description", flavor);
        } catch (Exception ignored) {}
        return out;
    }

    // Passo 3: Atividades de Acampamento (2 horas cada)
    public JSONArray campingActivitiesCatalog() {
        JSONArray arr = new JSONArray();
        String[][] list = {
            {"camuflar_acampamento", "Camuflar Acampamento", "Furtividade", "Oculta tendas e fogueiras para dificultar detecção.", "stealth"},
            {"cacar_coletar", "Caçar e Coletar", "Sobrevivência", "Busca provisões, ingredientes básicos e especiarias selvagens.", "survival"},
            {"manter_equipamento", "Manter Equipamento", "Manufatura", "Afia lâminas, conserta armaduras e escudos danificados.", "crafting"},
            {"organizar_vigilia", "Organizar Vigília", "Percepção", "Planeja turnos eficientes e posicionamento das sentinelas.", "perception"},
            {"contar_historia", "Contar História ao Redor da Fogueira", "Performance", "Inspira a comitiva com lendas valorosas e canções.", "performance"},
            {"relaxar", "Relaxar", "Nenhuma", "Descansa, cochila ou medita para conceder +1 circ na próxima atividade.", "none"},
            {"auxiliar", "Fornecer Auxílio", "Qualquer", "Ajuda um companheiro em sua atividade de acampamento (CD 20).", "aid"},
            {"aprender_companheiro", "Aprender com Companheiro", "Percepção", "Aprende uma atividade especial com um aliado amistoso (CD 20).", "perception"},
            {"amiri_aprimorar_armas", "Aprimorar Armas (Amiri)", "Manufatura", "Afia armas rústicas para +1 de circunstância em dano físico.", "companion"},
            {"ekundayo_sobrevivencia_ermos", "Sobrevivência nos Ermos (Ekundayo)", "Sobrevivência", "Otimiza rastreamento e recursos com +2 em Sobrevivência.", "companion"},
            {"jubilost_gerenciar_acampamento", "Gerenciar Acampamento (Jubiloso)", "Sobrevivência", "Coordena o acampamento (+1 circ em atividades e menor tempo de preparo).", "companion"},
            {"linzi_melhorar_confianca", "Melhorar Confiança (Linzi)", "Performance", "+1 de bônus de status em salvamentos de Vontade durante o acampamento.", "companion"},
            {"nok_nok_colocar_armadilhas", "Colocar Armadilhas (Nok-Nok)", "Ladroagem", "Arma armadilhas primitivas ao redor da área de vigília.", "companion"},
            {"tristian_bencao_alvorecer", "Benção da Flor do Alvorecer (Tristian)", "Religião", "Cura divina extra (+1d8 PVs) e remoção da condição Fatigado.", "companion"},
            {"valerie_preservar_armadura", "Preservar Armadura (Valerie)", "Manufatura", "+1 de bônus de item na CA de escudos e armaduras polidas.", "companion"},
            {"harrim_fazer_parte_da_noite", "Fazer Parte da Noite (Harrim)", "Religião", "Invoca a serenidade de Groetus contra o medo da noite.", "companion"},
            {"jaethal_guardioes_mortos_vivos", "Guardiões Mortos-Vivos (Jaethal)", "Religião", "Ergue sentinelas esqueléticas temporárias para a vigília.", "companion"},
            {"kalikke_perigos_aquaticos", "Perigos Aquáticos (Kalikke)", "Natureza", "Cria terreno difícil aquático contra invasores em rios/pântanos.", "companion"},
            {"kanerah_melhorar_fogueira", "Melhorar Fogueira (Kanerah)", "Natureza", "Fogo sagrado elemental que repele predadores e frio extremo.", "companion"},
            {"octavia_colocar_alarmes", "Colocar Alarmes (Otávia)", "Arcanismo", "Posiciona pequenos alarmes mágicos contra ataques surpresa.", "companion"},
            {"regongar_postura_intimidante", "Postura Intimidante (Regongar)", "Intimidação", "Postura selvagem que cancela encontros aleatórios de nível baixo ou trivial.", "companion"}
        };
        for (String[] act : list) {
            JSONObject a = new JSONObject();
            try {
                a.put("key", act[0]).put("name", act[1]).put("skill", act[2]).put("description", act[3]).put("category", act[4]);
            } catch (Exception ignored) {}
            arr.put(a);
        }
        return arr;
    }

    public JSONObject resolveCampingActivity(String activityKey, String actorName, int naturalRoll, int modifier, int zoneDc, int zoneLevel, JSONObject campState) {
        String act = key(activityKey);
        int targetDc = zoneDc;
        if (act.contains("auxiliar") || act.contains("aprender")) targetDc = 20;
        JSONObject check = pf2.resolveCheck(naturalRoll, modifier, targetDc);
        String deg = check.optString("degree", "SUCCESS");

        int encounterDcDelta = 0;
        int basicIngredients = 0;
        int specialIngredients = 0;
        int bonusCircumstance = 0;
        int bonusStatus = 0;
        String mechanicalBenefit = "";

        if (act.contains("camuflar")) {
            if ("CRITICAL_SUCCESS".equals(deg)) { encounterDcDelta = 2; mechanicalBenefit = "CD de Encontro +2 e o primeiro encontro potencial é evitado."; }
            else if ("SUCCESS".equals(deg)) { encounterDcDelta = 1; mechanicalBenefit = "CD de Encontro +1 pelo resto do acampamento."; }
            else if ("CRITICAL_FAILURE".equals(deg)) { encounterDcDelta = -1; mechanicalBenefit = "Camuflagem falha e atrai atenção (CD de Encontro -1)."; }
            else { mechanicalBenefit = "Nenhum benefício de camuflagem."; }
        } else if (act.contains("cacar") || act.contains("coletar")) {
            int baseLevel = Math.max(0, zoneLevel);
            if ("CRITICAL_SUCCESS".equals(deg)) {
                basicIngredients = zoneDc * 2;
                specialIngredients = baseLevel >= 14 ? 3 : baseLevel >= 7 ? 2 : 1;
                mechanicalBenefit = "Abundância: " + basicIngredients + " ingredientes básicos e " + specialIngredients + " ingredientes especiais coletados.";
            } else if ("SUCCESS".equals(deg)) {
                basicIngredients = zoneDc;
                specialIngredients = 1;
                mechanicalBenefit = "Sucesso: " + basicIngredients + " ingredientes básicos e " + specialIngredients + " ingrediente especial coletados.";
            } else if ("FAILURE".equals(deg)) {
                basicIngredients = zoneDc;
                mechanicalBenefit = "Fracasso parcial: apenas " + basicIngredients + " ingredientes básicos encontrados.";
            } else {
                basicIngredients = 2;
                mechanicalBenefit = "Falha crítica: apenas 2 ingredientes básicos e risco de encontro imediato!";
            }
        } else if (act.contains("vigilia")) {
            if ("CRITICAL_SUCCESS".equals(deg)) { bonusStatus = 2; mechanicalBenefit = "Vigília excelente: +2 de bônus de status em Percepção e descanso mais compacto."; }
            else if ("SUCCESS".equals(deg)) { bonusStatus = 1; mechanicalBenefit = "+1 de bônus de status em Percepção durante as vigílias."; }
            else { mechanicalBenefit = "Vigília padrão sem bônus extras."; }
        } else if (act.contains("historia")) {
            if ("CRITICAL_SUCCESS".equals(deg)) { bonusStatus = 2; mechanicalBenefit = "+2 de bônus de status em combates no acampamento e re-rolagem de falha para quem relaxou."; }
            else if ("SUCCESS".equals(deg)) { bonusStatus = 1; mechanicalBenefit = "+1 de bônus de status em combate para o grupo (+2 para quem relaxou)."; }
            else if ("CRITICAL_FAILURE".equals(deg)) { bonusStatus = -1; mechanicalBenefit = "História perturbadora: -1 de penalidade de estado até o descanso diário."; }
            else { mechanicalBenefit = "A plateia permanece indiferente."; }
        } else if (act.contains("relaxar")) {
            bonusCircumstance = 1;
            mechanicalBenefit = "+1 de bônus de circunstância no próximo teste de atividade e alívio de distrações.";
        } else if (act.contains("aprimorar_armas")) {
            mechanicalBenefit = "+1 de dano de circunstância em ataques físicos durante o próximo combate.";
        } else if (act.contains("sobrevivencia_ermos")) {
            mechanicalBenefit = "+2 de circunstância em testes de Sobrevivência e imunidade a terrenos difíceis naturais.";
        } else if (act.contains("gerenciar_acampamento")) {
            mechanicalBenefit = "+1 de circunstância em todas as atividades do acampamento e preparo 1 hora mais rápido.";
        } else if (act.contains("melhorar_confianca")) {
            mechanicalBenefit = "+1 de status em salvamentos de Vontade e testes mentais pelo resto do acampamento.";
        } else if (act.contains("colocar_armadilhas")) {
            mechanicalBenefit = "Armadilhas perimetrais causam 2d6 de dano perfurante a qualquer invasor no primeiro assalto.";
        } else if (act.contains("bencao_alvorecer")) {
            mechanicalBenefit = "Cura divina revigorante (+1d8 PVs durante o descanso) e condição Fatigado removida.";
        } else if (act.contains("preservar_armadura")) {
            mechanicalBenefit = "+1 de bônus de item na CA e +2 na Dureza do escudo preparado.";
        } else if (act.contains("fazer_parte_da_noite")) {
            mechanicalBenefit = "Visão na penumbra para vigias e imunidade a pesadelos e medo sombrio.";
        } else if (act.contains("guardioes_mortos_vivos")) {
            mechanicalBenefit = "Sentinelas mortas-vivas assumem as primeiras vigílias, garantindo alerta infalível sem fadiga.";
        } else if (act.contains("perigos_aquaticos")) {
            mechanicalBenefit = "Água infundida atrasa invasores (terreno difícil aquático e lentidão 1).";
        } else if (act.contains("melhorar_fogueira")) {
            mechanicalBenefit = "Fogo elemental aquece o raio do acampamento contra frio severo e afasta feras naturais.";
        } else if (act.contains("colocar_alarmes")) {
            mechanicalBenefit = "Alarmes arcanos impedem qualquer surpresa e acordam a comitiva imediatamente.";
        } else if (act.contains("postura_intimidante")) {
            mechanicalBenefit = "Postura aterrorizante desestimula ameaças: cancela encontros de nível baixo ou trivial.";
        } else {
            mechanicalBenefit = "Atividade de acampamento resolvida com sucesso.";
        }

        JSONObject out = new JSONObject();
        try {
            out.put("step", 3)
               .put("step_name", "Atividades de Acampamento")
               .put("activity_key", act)
               .put("actor_name", actorName)
               .put("natural_roll", naturalRoll)
               .put("modifier", modifier)
               .put("target_dc", targetDc)
               .put("degree", deg)
               .put("duration_hours", 2)
               .put("encounter_dc_delta", encounterDcDelta)
               .put("basic_ingredients_gained", basicIngredients)
               .put("special_ingredients_gained", specialIngredients)
               .put("circumstance_bonus", bonusCircumstance)
               .put("status_bonus", bonusStatus)
               .put("benefit_summary", mechanicalBenefit);
        } catch (Exception ignored) {}
        return out;
    }

    // Passo 4: Refeições Especiais e Culinária
    public JSONArray recipesCatalog() {
        JSONArray arr = new JSONArray();
        Object[][] recs = {
            {"refeicao_basica", "Refeição Básica", 0, 14, 1, 0, "Alimenta e protege contra fome e fadiga simples.", "Nenhum", "Saciedade diária."},
            {"guisado_de_monstro", "Guisado de Monstro", 11, 28, 8, 2, "+2 de estado em Atletismo e Fortitude por 24h.", "Amiri", "+2 de dano físico em ataques corpo a corpo."},
            {"assado_do_cacador", "Assado do Caçador", 6, 22, 4, 1, "+1 de estado em Percepção e Sobrevivência por 24h.", "Ekundayo", "+2 em ataques à distância na 1ª rodada."},
            {"buchada_de_ovelha", "Buchada de Ovelha", 1, 15, 2, 0, "+1 de estado em Fortitude contra venenos e fadiga por 24h.", "Harrim", "Imunidade a efeitos de medo ou desespero."},
            {"arroz_colorido", "Arroz Colorido", 0, 14, 1, 0, "Recupera PVs extras (+nível) no descanso.", "Jaethal", "+1 de resistência contra dano de vitalidade/positivo."},
            {"sopa_de_cebola", "Sopa de Cebola", 8, 24, 4, 1, "+1 de estado em Conhecimento e Vontade por 24h.", "Jubiloso", "+2 em testes de Manufatura Alquímica."},
            {"sorvete_de_chocolate", "Sorvete de Chocolate", 4, 19, 3, 1, "Resistência 2 a Frio e Fogo por 24h.", "Kalikke", "+1 de estado em ataques e impulsos de água."},
            {"coxas_e_asas_temperadas", "Coxas e Asas Temperadas", 12, 30, 8, 2, "+2 de estado em Iniciativa e ataques ígneos por 24h.", "Kanerah", "Ataques de fogo causam +1d4 de fogo persistente."},
            {"panqueca_doce", "Panqueca Doce", 7, 23, 4, 1, "+1 de estado em Performance e Diplomacia por 24h.", "Linzi", "Re-rolagem de uma falha em teste social por dia (fortuna)."},
            {"perna_de_aranha_assada", "Perna de Aranha Assada", 5, 20, 3, 1, "+1 de estado em Reflexos e Acrobacia por 24h.", "Nok-Nok", "Ataques Furtivos causam +1d6 de dano adicional na 1ª rodada."},
            {"arroz_doce_com_nozes", "Arroz Doce com Nozes", 2, 16, 2, 0, "+1 de estado em Arcanismo e salvamentos contra ilusões por 24h.", "Otávia", "+1 espaço temporário de magia de 1º círculo."},
            {"linguica_suculenta", "Linguiça Suculenta", 3, 18, 2, 1, "+1 de estado em dano físico de armas por 24h.", "Regongar", "+2 de circunstância em manobras de Atletismo (Agarrar/Derrubar)."},
            {"torta_de_frutakame", "Torta de Frutakame", 10, 27, 6, 2, "+2 de estado em Medicina e salvamentos contra venenos por 24h.", "Tristian", "Magias de cura concedem +1d8 de cura adicional."},
            {"ostras_de_rosalva", "Ostras de Rosalva", 9, 26, 6, 1, "+1 de estado na CA e Fortitude por 24h.", "Valerie", "Bloqueio com Escudo concede +2 de Dureza temporária."}
        };
        for (Object[] r : recs) {
            JSONObject o = new JSONObject();
            try {
                o.put("key", r[0]).put("name", r[1]).put("level", r[2]).put("dc", r[3])
                 .put("basic_cost", r[4]).put("special_cost", r[5])
                 .put("effect_description", r[6]).put("favorite_companion", r[7]).put("favorite_bonus", r[8]);
            } catch (Exception ignored) {}
            arr.put(o);
        }
        return arr;
    }

    public JSONObject recipe(String recipeKey) {
        String k = key(recipeKey);
        for (int i = 0; i < recipesCatalog().length(); i++) {
            JSONObject r = recipesCatalog().optJSONObject(i);
            if (r != null && (key(r.optString("key")).equals(k) || key(r.optString("name")).equals(k))) {
                return r;
            }
        }
        return recipesCatalog().optJSONObject(0); // refeicao_basica default
    }

    public JSONObject resolveCookingMeal(String recipeKey, String cookName, int naturalRoll, int modifier, int dinersCount, int stockBasic, int stockSpecial, JSONArray dinerCompanionKeys) {
        JSONObject r = recipe(recipeKey);
        int dc = r.optInt("dc", 14);
        int diners = Math.max(1, dinersCount);
        int neededBasic = r.optInt("basic_cost", 1) * diners;
        int neededSpecial = r.optInt("special_cost", 0) * (diners > 4 ? 2 : 1);

        if (stockBasic < neededBasic) {
            JSONObject err = new JSONObject();
            try {
                err.put("success", false).put("error", "Ingredientes básicos insuficientes (necessário: " + neededBasic + ", possui: " + stockBasic + ").");
            } catch (Exception ignored) {}
            return err;
        }
        if (stockSpecial < neededSpecial) {
            JSONObject err = new JSONObject();
            try {
                err.put("success", false).put("error", "Ingredientes especiais insuficientes (necessário: " + neededSpecial + ", possui: " + stockSpecial + ").");
            } catch (Exception ignored) {}
            return err;
        }

        JSONObject check = pf2.resolveCheck(naturalRoll, modifier, dc);
        String deg = check.optString("degree", "SUCCESS");
        boolean cooked = !"CRITICAL_FAILURE".equals(deg);

        int remBasic = stockBasic - neededBasic;
        int remSpecial = stockSpecial - neededSpecial;

        JSONArray buffs = new JSONArray();
        JSONArray favoriteMatches = new JSONArray();

        if (cooked) {
            JSONObject generalBuff = new JSONObject();
            try {
                generalBuff.put("recipe_name", r.optString("name"))
                           .put("duration", "24 horas")
                           .put("effect", r.optString("effect_description"))
                           .put("critical_success", "CRITICAL_SUCCESS".equals(deg));
            } catch (Exception ignored) {}
            buffs.put(generalBuff);

            String favComp = key(r.optString("favorite_companion"));
            if (dinerCompanionKeys != null && favComp.length() > 0 && !favComp.equals("nenhum")) {
                for (int i = 0; i < dinerCompanionKeys.length(); i++) {
                    String dKey = key(dinerCompanionKeys.optString(i));
                    if (dKey.contains(favComp) || favComp.contains(dKey)) {
                        JSONObject fav = new JSONObject();
                        try {
                            fav.put("companion", r.optString("favorite_companion"))
                               .put("bonus", r.optString("favorite_bonus"))
                               .put("duration", "24 horas");
                        } catch (Exception ignored) {}
                        favoriteMatches.put(fav);
                    }
                }
            }
        }

        JSONObject out = new JSONObject();
        try {
            out.put("step", 4)
               .put("step_name", "Refeições Especiais e Culinária")
               .put("recipe_key", r.optString("key"))
               .put("recipe_name", r.optString("name"))
               .put("cook_name", cookName)
               .put("natural_roll", naturalRoll)
               .put("modifier", modifier)
               .put("target_dc", dc)
               .put("degree", deg)
               .put("diners_count", diners)
               .put("consumed_basic", neededBasic)
               .put("consumed_special", neededSpecial)
               .put("remaining_basic", remBasic)
               .put("remaining_special", remSpecial)
               .put("success", cooked)
               .put("buffs_24h", buffs)
               .put("favorite_meal_matches", favoriteMatches)
               .put("description", cooked ? ("Refeição preparada com êxito (" + r.optString("name") + "). Efeitos ativos pelas próximas 24 horas.") : "A refeição queimou ou foi desperdiçada; sem bônus adicionais.");
        } catch (Exception ignored) {}
        return out;
    }

    // Passo 5: Sono, Descanso e Preparações Diárias
    public JSONObject resolveSleepAndRest(int partySize, int level, int conModifier, String comfort, JSONArray party, int currentEncounterDc, int baseEncounterDc, int hours) {
        int size = Math.max(1, partySize);
        int lvl = Math.max(1, level);
        int con = Math.max(0, conModifier);

        // Tabela Vigílias e Descanso (Guia de Companheiros p. 108)
        String watchDuration;
        int totalRestHours;
        if (size <= 2) { watchDuration = "8 horas"; totalRestHours = 16; }
        else if (size == 3) { watchDuration = "4 horas"; totalRestHours = 12; }
        else if (size == 4) { watchDuration = "2 horas e 40 minutos"; totalRestHours = 11; }
        else if (size == 5) { watchDuration = "2 horas"; totalRestHours = 10; }
        else if (size == 6) { watchDuration = "1 hora e 36 minutos"; totalRestHours = 10; }
        else if (size == 7) { watchDuration = "1 hora e 20 minutos"; totalRestHours = 9; }
        else if (size == 8) { watchDuration = "1 hora e 9 minutos"; totalRestHours = 9; }
        else { watchDuration = "1 hora"; totalRestHours = 9; }

        int healMultiplier = "confortavel".equalsIgnoreCase(comfort) || "bom".equalsIgnoreCase(comfort) ? 2 : 1;
        int naturalHealingHp = Math.max(1, con) * lvl * healMultiplier;

        JSONArray conditionsCleared = new JSONArray();
        conditionsCleared.put("Fatigado (removido)")
                         .put("Drenado (reduzido em 1)")
                         .put("Enfraquecido (reduzido em 1)")
                         .put("Debilitado (reduzido em 1)")
                         .put("Estupefato (reduzido em 1)");

        JSONObject out = new JSONObject();
        try {
            out.put("step", 5)
               .put("step_name", "Sono, Descanso e Preparações Diárias")
               .put("party_size", size)
               .put("total_rest_hours", totalRestHours)
               .put("watch_shift_duration", watchDuration)
               .put("natural_healing_hp", naturalHealingHp)
               .put("spells_prepared_restored", true)
               .put("focus_points_restored", true)
               .put("conditions_cleared", conditionsCleared)
               .put("daily_preparations_duration", "30 minutos")
               .put("encounter_dc_reset_to", baseEncounterDc)
               .put("description", "Descanso concluído. Recuperação natural de " + naturalHealingHp + " PVs, recuperação plena de magias e foco, e preparação diária de 30 minutos realizada.");
        } catch (Exception ignored) {}
        return out;
    }

    // Consolidação Mecânica Determinística do Acampamento (Sem source_specific)
    public JSONObject aggregateCampEffects(JSONObject prepareResult, JSONArray activities, JSONObject mealResult, JSONObject restResult) {
        JSONObject out = new JSONObject();
        try {
            JSONArray activeBuffs = new JSONArray();
            if (mealResult != null && mealResult.has("buffs_24h")) {
                JSONArray mb = mealResult.optJSONArray("buffs_24h");
                if (mb != null) for (int i = 0; i < mb.length(); i++) activeBuffs.put(mb.opt(i));
            }
            if (mealResult != null && mealResult.has("favorite_meal_matches")) {
                JSONArray fav = mealResult.optJSONArray("favorite_meal_matches");
                if (fav != null) for (int i = 0; i < fav.length(); i++) activeBuffs.put(fav.opt(i));
            }

            int naturalHealing = restResult != null ? restResult.optInt("natural_healing_hp", 0) : 0;
            JSONArray conditions = restResult != null ? restResult.optJSONArray("conditions_cleared") : new JSONArray();

            out.put("status", "complete")
               .put("resolution", "deterministic_mechanics")
               .put("phase", "COMPLETE")
               .put("buffs_24h", activeBuffs)
               .put("natural_healing_hp", naturalHealing)
               .put("conditions_cleared", conditions != null ? conditions : new JSONArray())
               .put("spells_and_focus_restored", true)
               .put("daily_preparations_complete", true)
               .put("notes", "Acampamento concluído conforme o procedimento oficial de 5 passos do Kingmaker.")
               .put("mechanical_effects", new JSONObject()
                   .put("active_buffs_count", activeBuffs.length())
                   .put("natural_healing_hp", naturalHealing)
                   .put("buffs", activeBuffs)
                   .put("buff_duration", "24 horas")
                   .put("full_recovery", true));
        } catch (Exception ignored) {}
        return out;
    }

    public JSONObject resolveClaimHex(int natural,int modifier,int controlDc){
        JSONObject check=pf2.resolveCheck(natural,modifier,controlDc);
        String deg=check.optString("degree","FAILURE");
        JSONObject out=new JSONObject();
        try{
            boolean success="CRITICAL_SUCCESS".equals(deg)||"SUCCESS".equals(deg);
            int rpCost=success?1:0;
            int sizeDelta=success?1:0;
            int xpGain=success?10:0;
            int unrestDelta="CRITICAL_SUCCESS".equals(deg)?-1:"CRITICAL_FAILURE".equals(deg)?1:0;
            out.put("check",check).put("activity","claim_hex").put("success",success)
               .put("size_delta",sizeDelta).put("rp_cost",rpCost).put("xp_gain",xpGain)
               .put("unrest_delta",unrestDelta);
        }catch(Exception ignored){}
        return out;
    }

    public JSONObject resolveBuildStructure(int natural,int modifier,int controlDc,int structureRpCost){
        JSONObject check=pf2.resolveCheck(natural,modifier,controlDc);
        String deg=check.optString("degree","FAILURE");
        JSONObject out=new JSONObject();
        try{
            boolean built="CRITICAL_SUCCESS".equals(deg)||"SUCCESS".equals(deg);
            int rpSpent="CRITICAL_SUCCESS".equals(deg)?Math.max(0,structureRpCost-1):"SUCCESS".equals(deg)?structureRpCost:"CRITICAL_FAILURE".equals(deg)?Math.max(1,structureRpCost/2):0;
            int xpGain="CRITICAL_SUCCESS".equals(deg)?20:built?10:0;
            int unrestDelta="CRITICAL_FAILURE".equals(deg)?1:0;
            out.put("check",check).put("activity","build_structure").put("built",built)
               .put("rp_cost",rpSpent).put("xp_gain",xpGain).put("unrest_delta",unrestDelta);
        }catch(Exception ignored){}
        return out;
    }

    public JSONObject resolveCollectTaxes(int natural,int modifier,int controlDc,int economyModifier){
        JSONObject check=pf2.resolveCheck(natural,modifier,controlDc);
        String deg=check.optString("degree","FAILURE");
        JSONObject out=new JSONObject();
        try{
            int base=Math.max(1,economyModifier);
            int rpGain="CRITICAL_SUCCESS".equals(deg)?base+2:"SUCCESS".equals(deg)?base:0;
            int unrestDelta="CRITICAL_FAILURE".equals(deg)?1:0;
            int xpGain="CRITICAL_SUCCESS".equals(deg)?10:0;
            out.put("check",check).put("activity","collect_taxes").put("rp_gain",rpGain)
               .put("unrest_delta",unrestDelta).put("xp_gain",xpGain).put("tax_riot","CRITICAL_FAILURE".equals(deg));
        }catch(Exception ignored){}
        return out;
    }

    public JSONObject resolveQuellUnrest(int natural,int modifier,int controlDc){
        JSONObject check=pf2.resolveCheck(natural,modifier,controlDc);
        String deg=check.optString("degree","FAILURE");
        JSONObject out=new JSONObject();
        try{
            int unrestDelta="CRITICAL_SUCCESS".equals(deg)?-2:"SUCCESS".equals(deg)?-1:"CRITICAL_FAILURE".equals(deg)?1:0;
            int fameGain="CRITICAL_SUCCESS".equals(deg)?1:0;
            out.put("check",check).put("activity","quell_unrest").put("unrest_delta",unrestDelta).put("fame_gain",fameGain);
        }catch(Exception ignored){}
        return out;
    }

    public JSONObject resolveEstablishTradeAgreement(int natural,int modifier,int dc){
        JSONObject check=pf2.resolveCheck(natural,modifier,dc);
        String deg=check.optString("degree","FAILURE");
        JSONObject out=new JSONObject();
        try{
            boolean active="CRITICAL_SUCCESS".equals(deg)||"SUCCESS".equals(deg);
            int bonusRp="CRITICAL_SUCCESS".equals(deg)?2:active?1:0;
            int unrestDelta="CRITICAL_FAILURE".equals(deg)?1:0;
            out.put("check",check).put("activity","trade_agreement").put("established",active)
               .put("bonus_rp",bonusRp).put("unrest_delta",unrestDelta);
        }catch(Exception ignored){}
        return out;
    }

    public JSONObject resolveCelebrateHoliday(int natural,int modifier,int controlDc){
        JSONObject check=pf2.resolveCheck(natural,modifier,controlDc);
        String deg=check.optString("degree","FAILURE");
        JSONObject out=new JSONObject();
        try{
            int unrestDelta="CRITICAL_SUCCESS".equals(deg)?-2:"SUCCESS".equals(deg)?-1:"CRITICAL_FAILURE".equals(deg)?1:0;
            int fameGain="CRITICAL_SUCCESS".equals(deg)?1:0;
            out.put("check",check).put("activity","celebrate_holiday").put("unrest_delta",unrestDelta).put("fame_gain",fameGain);
        }catch(Exception ignored){}
        return out;
    }


    public JSONObject applyUnrest(int currentUnrest,int delta){
        int next=Math.max(0,Math.min(99,currentUnrest+delta));
        boolean anarchy=isAnarchy(next);
        JSONObject out=new JSONObject();
        try{
            out.put("unrest",next).put("delta",delta).put("unrest_penalty",unrestPenalty(next)).put("anarchy",anarchy);
        }catch(Exception ignored){}
        return out;
    }

    public JSONObject applyRuin(int currentPoints,int currentPenalty,int threshold,int delta){
        int t=Math.max(1,threshold);
        int points=Math.max(0,currentPoints+delta);
        int penalty=Math.max(0,currentPenalty);
        while(points>=t){points-=t;penalty++;}
        JSONObject out=new JSONObject();
        try{
            out.put("points",points).put("penalty",penalty).put("threshold",t);
        }catch(Exception ignored){}
        return out;
    }

    public JSONObject fameInfamySummary(int fame,int infamy,String mode){
        JSONObject out=new JSONObject();
        try{
            String m="infamy".equalsIgnoreCase(mode)?"infamy":"fame";
            out.put("mode",m).put("fame",Math.max(0,fame)).put("infamy",Math.max(0,infamy))
               .put("active_points","infamy".equals(m)?Math.max(0,infamy):Math.max(0,fame));
        }catch(Exception ignored){}
        return out;
    }

    public JSONObject resolvePrepareCamp(int natural,int modifier,int zoneDc){
        JSONObject check=pf2.resolveCheck(natural,modifier,zoneDc);
        String deg=check.optString("degree","FAILURE");
        int safety=50,comfort=50;
        if("CRITICAL_SUCCESS".equals(deg)){safety=75;comfort=75;}
        else if("SUCCESS".equals(deg)){safety=60;comfort=60;}
        else if("CRITICAL_FAILURE".equals(deg)){safety=25;comfort=25;}
        else {safety=40;comfort=40;}
        JSONObject out=new JSONObject();
        try{
            out.put("step",1).put("step_name","prepare_camp").put("check",check)
               .put("degree",deg).put("safety",safety).put("comfort",comfort);
        }catch(Exception ignored){}
        return out;
    }

    public JSONArray campingRolesCatalog(){
        JSONArray arr=new JSONArray();
        try{
            arr.put(new JSONObject().put("role","cook").put("name","Cozinheiro").put("skill","survival").put("purpose","Preparar refeição e conceder bônus"));
            arr.put(new JSONObject().put("role","guard").put("name","Vigia").put("skill","perception").put("purpose","Detectar aproximação e emboscadas"));
            arr.put(new JSONObject().put("role","camouflage").put("name","Camuflador").put("skill","stealth").put("purpose","Ocultar o acampamento de patrulhas"));
            arr.put(new JSONObject().put("role","healer").put("name","Curandeiro").put("skill","medicine").put("purpose","Tratar ferimentos e acelerar recuperação"));
            arr.put(new JSONObject().put("role","scout").put("name","Batedor").put("skill","survival").put("purpose","Reconhecer arredores imediatos"));
        }catch(Exception ignored){}
        return arr;
    }

    public JSONObject resolveCookMeal(int natural,int modifier,int dc,int specialIngredients){
        int bonus=Math.max(0,specialIngredients)>0?2:0;
        JSONObject check=pf2.resolveCheck(natural,modifier+bonus,dc);
        String deg=check.optString("degree","FAILURE");
        JSONObject out=new JSONObject();
        try{
            boolean success="CRITICAL_SUCCESS".equals(deg)||"SUCCESS".equals(deg);
            int tempHp="CRITICAL_SUCCESS".equals(deg)?5:success?2:0;
            int comfortDelta="CRITICAL_SUCCESS".equals(deg)?15:success?10:"CRITICAL_FAILURE".equals(deg)?-10:-5;
            out.put("step",3).put("step_name","cook_meal").put("check",check)
               .put("success",success).put("temp_hp",tempHp).put("comfort_delta",comfortDelta);
        }catch(Exception ignored){}
        return out;
    }

    public JSONObject checkCampingEncounter(int naturalRoll,int currentEncounterDc){
        int dc=Math.max(1,currentEncounterDc);
        int n=Math.max(1,Math.min(20,naturalRoll));
        boolean triggered=dc>0&&n>=dc;
        JSONObject out=new JSONObject();
        try{
            out.put("step",4).put("step_name","watch_check").put("roll",n).put("dc",dc).put("triggered",triggered);
        }catch(Exception ignored){}
        return out;
    }

    public JSONObject resolveCampRecovery(int conMod,int level,int comfortLevel){
        int baseHealing=pf2.naturalHealing(conMod,level,false);
        int bonus=comfortLevel>=70?level:0;
        int totalHealing=baseHealing+bonus;
        JSONObject out=new JSONObject();
        try{
            out.put("step",5).put("step_name","recovery").put("base_healing",baseHealing)
               .put("comfort_bonus",bonus).put("total_healing",totalHealing).put("fatigued_removed",true);
        }catch(Exception ignored){}
        return out;
    }

    public int resourceDieSize(int size){int value=Math.max(1,size);return value>=100?12:value>=50?10:value>=25?8:value>=10?6:4;}
    public int sizeControlModifier(int size){int value=Math.max(1,size);return value>=100?4:value>=50?3:value>=25?2:value>=10?1:0;}
    public int commodityStorage(int size){int value=Math.max(1,size);return value>=100?20:value>=50?16:value>=25?12:value>=10?8:4;}
    public int investedLeadershipBonus(int level){return level>=16?3:level>=8?2:1;}
    public String roleAbility(String role){String value=ROLE_ABILITIES.get(key(role));return value==null?"":value;}

    // =========================================================================
    // Catálogo Canônico de Atividades do Reino (Kingmaker PF2e)
    // =========================================================================
    /**
     * Metadados didáticos fazem parte do próprio motor de regras. A UI do guia
     * lê estes dados; não mantém um segundo catálogo.
     */
    public JSONArray turnProcedureGuide(){
        JSONArray phases=new JSONArray();
        try{
            phases.put(new JSONObject().put("key","UPKEEP").put("name","1. Manutenção").put("steps",new JSONArray()
                    .put(step("leadership","Verificar liderança e vacâncias",true,"O motor verifica cargos vagos e aplica as consequências configuradas antes de seguir.","Unrest, CD de Controle e penalidades de vacância"))
                    .put(step("resources","Gerar Recursos do Reino",true,"Ao iniciar o turno, os dados de recurso são rolados/validados pelo motor local.","Resource Points disponíveis no turno"))
                    .put(step("consumption","Cobrir Consumo",true,"Escolha como pagar o Consumo: Alimento, RP ou aceitar Unrest quando permitido.","Alimento, RP ou Unrest"))));
            phases.put(new JSONObject().put("key","COMMERCE").put("name","2. Comércio").put("steps",new JSONArray()
                    .put(step("collect_taxes","Coletar Impostos",false,"Você decide se arrecada impostos; o resolver específico aplica o grau de sucesso.","RP e, em falha crítica, Unrest"))
                    .put(step("lifestyle_or_treasury","Estilo de Vida / Tesouro",false,"Escolha investir no bem-estar do Reino ou recorrer ao tesouro quando fizer sentido.","RP, Unrest e Crime"))
                    .put(step("trade_commodities","Negociar Commodities",false,"Compre ou venda mercadorias dentro de estoque e capacidade válidos.","RP e Food/Lumber/Luxuries/Ore/Stone"))
                    .put(step("trade_agreements","Gerenciar Acordos Comerciais",false,"Gerencie acordos existentes ou tente estabelecer um novo acordo válido.","RP, Unrest e acordos comerciais"))));
            phases.put(new JSONObject().put("key","ACTIVITY").put("name","3. Atividades").put("steps",new JSONArray()
                    .put(step("leadership_activities","Atividades de Liderança",false,"Escolha entre as atividades de liderança permitidas pelo estado atual.","Liderança, Unrest, RP, relações e outros efeitos específicos"))
                    .put(step("region_activities","Atividades de Região",false,"Escolha ações sobre hexes válidos: reconhecimento, reivindicação, estrada, irrigação e outras.","Território, tamanho, RP, melhorias e hexes"))
                    .put(step("civic_activities","Atividades Cívicas",false,"Escolha ações válidas em assentamentos e estruturas.","RP, commodities, lotes, estruturas e assentamentos"))));
            phases.put(new JSONObject().put("key","EVENT").put("name","4. Evento").put("steps",new JSONArray()
                    .put(step("event_flat_check","Teste simples de Evento",true,"Use a CD de Evento persistida. Sem evento, a próxima CD cai 5; quando ocorre, volta a 16.","CD de Evento e possível evento pendente"))
                    .put(step("event_resolution","Resolver evento ocorrido",true,"Se um evento surgir, ele precisa ser resolvido pelo resolver antes do fechamento do turno.","Estado indicado pelo evento, como Unrest/Ruin/recursos"))
                    .put(step("complete_turn","Fechar o turno",true,"Somente depois dos passos obrigatórios o turno é concluído e persistido.","Número do turno e estado de continuidade"))));
        }catch(Exception ignored){}
        return phases;
    }

    private static JSONObject step(String key,String name,boolean required,String explanation,String changes)throws Exception{
        return new JSONObject().put("key",key).put("name",name).put("required",required).put("explanation",explanation).put("changes",changes);
    }

    public JSONArray activityGuideCatalog(){
        JSONArray out=new JSONArray();
        for(String key:ACTIVITIES){
            JSONObject a=activityGuide(key);
            if(a!=null&&a.length()>0)out.put(a);
        }
        return out;
    }

    /**
     * Enriquecimento do catálogo executável. Os nomes de parâmetros refletem
     * resolveKingdomActivity(), e "changes" documenta apenas famílias de estado
     * que os resolvers oficiais podem alterar.
     */
    public JSONObject activityGuide(String activityKey){
        JSONObject a=activity(activityKey);
        if(a==null||a.length()==0)return new JSONObject();
        String k=key(activityKey);
        try{
            JSONArray params=new JSONArray();
            String requirements="";
            String changes="";
            if("new_leadership".equals(k)){
                params.put("cargo").put("personagem/companheiro").put("investido ou não");
                requirements="Escolher um cargo e um candidato válido.";
                changes="liderança, vacância e bônus/penalidades associados";
            }else if("proclamation".equals(k)){
                params.put("perícia").put("rolagem");
                requirements="Fase de Atividades; atividade permitida pelo estado atual.";
                changes="Unrest e efeitos de lealdade conforme o grau";
            }else if("rest_and_relax".equals(k)){
                params.put("perícia").put("rolagem");
                requirements="Fase de Atividades; atividade permitida pelo estado atual.";
                changes="Unrest e bem-estar do Reino";
            }else if("supernatural_solution".equals(k)){
                params.put("perícia").put("rolagem");
                requirements="Fase de Atividades; acesso ficcional compatível.";
                changes="pressões/Unrest e efeitos específicos do resultado";
            }else if("take_charge".equals(k)){
                params.put("cargo").put("perícia").put("rolagem");
                requirements="Escolher cargo/líder aplicável.";
                changes="resultado da atividade liderada e estado associado";
            }else if("send_diplomatic_envoy".equals(k)){
                params.put("nação/facção alvo").put("perícia").put("rolagem");
                requirements="Alvo diplomático identificável.";
                changes="relações diplomáticas, RP/Unrest quando o resolver determinar";
            }else if("pledge_of_fealty".equals(k)){
                params.put("comunidade alvo").put("perícia").put("rolagem");
                requirements="Comunidade alvo válida.";
                changes="relação política, território/estado quando o resolver determinar";
            }else if("request_foreign_aid".equals(k)){
                params.put("aliado/nação").put("perícia").put("rolagem");
                requirements="Aliado ou nação alvo válida.";
                changes="recursos/pressões e relações externas";
            }else if("quell_unrest".equals(k)){
                params.put("perícia").put("rolagem");
                requirements="Pode ser usada para combater Agitação; em Anarquia é uma das opções restaurativas permitidas.";
                changes="Unrest";
            }else if("claim_hex".equals(k)){
                params.put("hex").put("perícia").put("rolagem");
                requirements="Hex válido, adjacente e preparado conforme o estado de exploração.";
                changes="território, tamanho, RP, XP e Unrest conforme o grau";
            }else if("abandon_hex".equals(k)){
                params.put("hex");
                requirements="Hex atualmente reivindicado.";
                changes="território, tamanho e estado do hex";
            }else if("clear_hex".equals(k)){
                params.put("hex").put("perícia").put("rolagem");
                requirements="Hex válido e reconhecido quando exigido.";
                changes="estado de limpeza do hex e efeitos do resultado";
            }else if("reconnoiter_hex".equals(k)){
                params.put("hex").put("perícia").put("rolagem");
                requirements="Hex explorável pelo grupo/Reino.";
                changes="estado de reconhecimento e informação territorial";
            }else if("build_roads".equals(k)){
                params.put("hex").put("perícia").put("rolagem");
                requirements="Hex controlado e elegível para melhoria.";
                changes="RP/commodities quando aplicáveis e melhoria de estrada do hex";
            }else if("irrigate".equals(k)){
                params.put("hex").put("perícia").put("rolagem");
                requirements="Hex controlado e elegível.";
                changes="melhoria/farmland e estado territorial";
            }else if("establish_settlement".equals(k)){
                params.put("hex").put("nome do assentamento");
                requirements="Hex válido para fundação.";
                changes="assentamentos, território e consumo";
            }else if("work_sites".equals(k)){
                params.put("hex").put("tipo de sítio").put("perícia").put("rolagem");
                requirements="Hex com recurso/terreno compatível.";
                changes="resource site, commodities e estado do hex";
            }else if("fortify_hex".equals(k)){
                params.put("hex").put("perícia").put("rolagem");
                requirements="Hex controlado elegível.";
                changes="fortificação/melhoria defensiva do hex";
            }else if("build_structure".equals(k)){
                params.put("assentamento").put("estrutura").put("perícia").put("rolagem");
                requirements="Assentamento, nível, lotes, RP, commodities e pré-requisitos válidos.";
                changes="RP, commodities, lotes, estrutura e bônus do assentamento";
            }else if("demolish".equals(k)){
                params.put("assentamento").put("estrutura");
                requirements="Estrutura existente no assentamento.";
                changes="estrutura e lotes ocupados";
            }else if("repair_structure".equals(k)){
                params.put("assentamento").put("estrutura");
                requirements="Estrutura existente e reparável.";
                changes="estado operacional da estrutura";
            }else if("upgrade_settlement".equals(k)){
                params.put("assentamento").put("próxima categoria");
                requirements="Assentamento existente e categoria seguinte válida.";
                changes="nível/tipo, capacidade e efeitos do assentamento";
            }else if("collect_taxes".equals(k)){
                params.put("perícia").put("rolagem");
                requirements="Fase de Comércio.";
                changes="RP; falha crítica pode aumentar Unrest";
            }else if("improve_lifestyle".equals(k)){
                params.put("RP a investir").put("perícia").put("rolagem");
                requirements="RP suficiente.";
                changes="RP e Unrest";
            }else if("tap_treasury".equals(k)){
                params.put("quantidade de RP");
                requirements="Fase de Comércio e decisão explícita.";
                changes="RP e Crime";
            }else if("trade_commodities".equals(k)){
                params.put("comprar/vender").put("commodity").put("quantidade");
                requirements="Estoque/capacidade e RP suficientes para a operação.";
                changes="RP e Food/Lumber/Luxuries/Ore/Stone";
            }else if("manage_trade_agreements".equals(k)){
                params.put("perícia").put("rolagem");
                requirements="Acordos comerciais existentes quando a operação depender deles.";
                changes="RP; falha crítica pode aumentar Unrest";
            }else if("establish_trade_agreement".equals(k)){
                params.put("assentamento de origem").put("destino").put("perícia").put("rolagem");
                requirements="Origem válida e destino identificável.";
                changes="acordos comerciais e, conforme resultado, Unrest";
            }else if("capital_investment".equals(k)){
                params.put("PO a investir");
                requirements="Ouro suficiente; custo por RP depende do nível do Reino.";
                changes="RP";
            }
            a.put("parameters",params).put("requirements",requirements).put("changes",changes)
                    .put("resolver","KingmakerRepository.resolveKingdomActivity")
                    .put("mechanical_authority",true);
        }catch(Exception ignored){}
        return a;
    }

    public JSONArray activityCatalog(){
        JSONArray arr=new JSONArray();
        for(String key:ACTIVITIES){
            JSONObject act=activity(key);
            if(act!=null&&act.length()>0)arr.put(act);
        }
        return arr;
    }

    public JSONObject activity(String key){
        String k=key(key);
        JSONObject a=new JSONObject();
        try{
            a.put("key",k);
            if("new_leadership".equals(k)){
                a.put("name","Nova Liderança").put("category","leadership").put("phase","ACTIVITY").put("skill","").put("ability","")
                 .put("restorative",false).put("description","Altera ou preenche um cargo vago de liderança.");
            }else if("proclamation".equals(k)){
                a.put("name","Proclamação").put("category","leadership").put("phase","ACTIVITY").put("skill","politics").put("ability","loyalty")
                 .put("restorative",true).put("description","Faz um decreto régio oficial para acalmar o povo e reforçar a lealdade.");
            }else if("rest_and_relax".equals(k)){
                a.put("name","Descansar e Relaxar").put("category","leadership").put("phase","ACTIVITY").put("skill","arts").put("ability","culture")
                 .put("restorative",true).put("description","Os líderes do Reino reservam tempo para descanso e lazer comunitário, reduzindo Unrest.");
            }else if("supernatural_solution".equals(k)){
                a.put("name","Solução Sobrenatural").put("category","leadership").put("phase","ACTIVITY").put("skill","magic").put("ability","culture")
                 .put("restorative",true).put("description","Consulta forças mágicas, arcanas ou divinas para intervir em crises e aliviar pressões.");
            }else if("take_charge".equals(k)){
                a.put("name","Assumir o Controle").put("category","leadership").put("phase","ACTIVITY").put("skill","statecraft").put("ability","loyalty")
                 .put("restorative",false).put("description","Um líder assume comando direto de uma atividade ou projeto nacional.");
            }else if("send_diplomatic_envoy".equals(k)){
                a.put("name","Enviar Enviado Diplomático").put("category","leadership").put("phase","ACTIVITY").put("skill","politics").put("ability","loyalty")
                 .put("restorative",false).put("description","Envia embaixador a uma nação vizinha ou facção para estreitar laços.");
            }else if("pledge_of_fealty".equals(k)){
                a.put("name","Juramento de Fidelidade").put("category","leadership").put("phase","ACTIVITY").put("skill","politics").put("ability","loyalty")
                 .put("restorative",false).put("description","Oferece ou aceita suserania voluntária com uma comunidade.");
            }else if("request_foreign_aid".equals(k)){
                a.put("name","Solicitar Ajuda Externa").put("category","leadership").put("phase","ACTIVITY").put("skill","trade").put("ability","economy")
                 .put("restorative",false).put("description","Solicita auxílio emergencial de recursos ou contingente de aliados.");
            }else if("quell_unrest".equals(k)){
                a.put("name","Acalmar Agitação").put("category","leadership").put("phase","ACTIVITY").put("skill","politics").put("ability","loyalty")
                 .put("restorative",true).put("description","Ações cívicas e discursos pacificadores para conter revoltas e encerrar a Anarquia.");
            }else if("claim_hex".equals(k)){
                a.put("name","Reivindicar Hex").put("category","region").put("phase","ACTIVITY").put("skill","wilderness").put("ability","stability")
                 .put("restorative",false).put("description","Reivindica a posse formal de um hex adjacente reconnoiterado e limpo.");
            }else if("abandon_hex".equals(k)){
                a.put("name","Abandonar Hex").put("category","region").put("phase","ACTIVITY").put("skill","statecraft").put("ability","loyalty")
                 .put("restorative",false).put("description","Renuncia à soberania sobre um hex para diminuir o tamanho e fardo do Reino.");
            }else if("clear_hex".equals(k)){
                a.put("name","Limpar Hex").put("category","region").put("phase","ACTIVITY").put("skill","wilderness").put("ability","stability")
                 .put("restorative",false).put("description","Elimina perigos e monstros locais de um hex mapeado.");
            }else if("reconnoiter_hex".equals(k)){
                a.put("name","Reconhecer Hex").put("category","region").put("phase","ACTIVITY").put("skill","exploration").put("ability","economy")
                 .put("restorative",false).put("description","Explora e cataloga o terreno, recursos e marcos de um hex inexplorado.");
            }else if("build_roads".equals(k)){
                a.put("name","Construir Estradas").put("category","region").put("phase","ACTIVITY").put("skill","engineering").put("ability","stability")
                 .put("restorative",false).put("description","Abre e pavimenta estradas em um hex reivindicado.");
            }else if("irrigate".equals(k)){
                a.put("name","Irrigar").put("category","region").put("phase","ACTIVITY").put("skill","agriculture").put("ability","stability")
                 .put("restorative",false).put("description","Abre canais de irrigação e estabelece fazendas, reduzindo o Consumo do Reino.");
            }else if("establish_settlement".equals(k)){
                a.put("name","Fundar Assentamento").put("category","region").put("phase","ACTIVITY").put("skill","statecraft").put("ability","loyalty")
                 .put("restorative",false).put("description","Estabelece uma nova aldeia de nível 1 em um hex reivindicado e limpo.");
            }else if("work_sites".equals(k)){
                a.put("name","Canteiro de Trabalho").put("category","region").put("phase","ACTIVITY").put("skill","industry").put("ability","economy")
                 .put("restorative",false).put("description","Desenvolve um sítio de mineração, extração florestal ou pedreira.");
            }else if("fortify_hex".equals(k)){
                a.put("name","Fortificar Hex").put("category","region").put("phase","ACTIVITY").put("skill","defense").put("ability","stability")
                 .put("restorative",false).put("description","Constrói paliçadas, guaritas e baluartes em um hex defensivo.");
            }else if("build_structure".equals(k)){
                a.put("name","Construir Estrutura").put("category","civic").put("phase","ACTIVITY").put("skill","engineering").put("ability","stability")
                 .put("restorative",false).put("description","Econômico e cívico: valida lotes, custos de RP e commodities e constrói uma estrutura.");
            }else if("demolish".equals(k)){
                a.put("name","Demolir Estrutura").put("category","civic").put("phase","ACTIVITY").put("skill","engineering").put("ability","stability")
                 .put("restorative",false).put("description","Demole com segurança uma estrutura para liberar lotes urbanos.");
            }else if("repair_structure".equals(k)){
                a.put("name","Reparar Estrutura").put("category","civic").put("phase","ACTIVITY").put("skill","engineering").put("ability","stability")
                 .put("restorative",false).put("description","Repara uma estrutura danificada por eventos ou ataques.");
            }else if("upgrade_settlement".equals(k)){
                a.put("name","Melhorar Assentamento").put("category","civic").put("phase","ACTIVITY").put("skill","statecraft").put("ability","loyalty")
                 .put("restorative",false).put("description","Promove a categoria do assentamento (aldeia -> vila -> cidade -> metrópole).");
            }else if("collect_taxes".equals(k)){
                a.put("name","Coletar Impostos").put("category","commerce").put("phase","COMMERCE").put("skill","trade").put("ability","economy")
                 .put("restorative",false).put("description","Arrecada impostos e tarifas alfandegárias para gerar RP.");
            }else if("improve_lifestyle".equals(k)){
                a.put("name","Melhorar Estilo de Vida").put("category","commerce").put("phase","COMMERCE").put("skill","arts").put("ability","culture")
                 .put("restorative",true).put("description","Investe recursos em festas, lazer e serviços públicos para reduzir Unrest.");
            }else if("tap_treasury".equals(k)){
                a.put("name","Recorrer ao Tesouro").put("category","commerce").put("phase","COMMERCE").put("skill","trade").put("ability","economy")
                 .put("restorative",false).put("description","Retira fundos do tesouro para despesas emergenciais.");
            }else if("trade_commodities".equals(k)){
                a.put("name","Comercializar Mercadorias").put("category","commerce").put("phase","COMMERCE").put("skill","trade").put("ability","economy")
                 .put("restorative",false).put("description","Compra ou vende commodities no mercado dentro dos limites de armazenagem.");
            }else if("manage_trade_agreements".equals(k)){
                a.put("name","Gerenciar Acordos Comerciais").put("category","commerce").put("phase","COMMERCE").put("skill","trade").put("ability","economy")
                 .put("restorative",false).put("description","Fiscaliza e recolhe o dividendo de rotas comerciais ativas.");
            }else if("establish_trade_agreement".equals(k)){
                a.put("name","Estabelecer Acordo Comercial").put("category","commerce").put("phase","COMMERCE").put("skill","trade").put("ability","economy")
                 .put("restorative",false).put("description","Firma um novo tratado comercial bilateral com um parceiro externo.");
            }else if("capital_investment".equals(k)){
                a.put("name","Investimento de Capital").put("category","commerce").put("phase","COMMERCE").put("skill","trade").put("ability","economy")
                 .put("restorative",false).put("description","Converte ouro dos aventureiros diretamente em RP para o Reino.");
            }else{
                return null;
            }
        }catch(Exception ignored){}
        return a;
    }

    // =========================================================================
    // Catálogo Canônico de Estruturas (Kingmaker PF2e)
    // =========================================================================
    public JSONArray structuresCatalog(){
        JSONArray arr=new JSONArray();
        String[] structureKeys={
            "house","tavern","mill","smithy","market","temple","cathedral","barracks",
            "wooden_palisade","stone_wall","castle","brewery","library","academy","bank",
            "granary","stables","town_hall","festival_ground","garrison","embassy","theater",
            "pier","alchemist","graveyard","monument"
        };
        for(String key:structureKeys){
            JSONObject st=structure(key);
            if(st!=null)arr.put(st);
        }
        return arr;
    }

        // R36_OFFICIAL_CATALOG_73: enrich/preserve baseline effects; replace only official fields.
    public JSONObject structure(String key){
        JSONObject officialBase=structureR35Baseline(key);
        boolean hadR35Structure=officialBase!=null && officialBase.length()>0;
        if(officialBase==null)officialBase=new JSONObject();
        String r36OfficialKey=key==null?"":key.trim().toLowerCase(java.util.Locale.US)
            .replaceAll("[^a-z0-9]+","_").replaceAll("^_+|_+$","");
        try {
            if("academy".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING,EDIFICE");
                officialBase.put("official_item_bonus_rule","+2 item bonus to Creative Solution");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","While in a settlement with an Academy, you gain a +2 item bonus to Lore checks made to Recall Knowledge while Investigating, to all checks made while Researching (Gamemastery Guide 154), and to Decipher Writing.");
                officialBase.put("official_upgrade_from","library");
                officialBase.put("official_upgrade_to","military academy, university");
            }
            if("alchemist".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Demolish");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","Treat the settlement\u2019s level as one level higher than its actual level for the purposes of determining which alchemical items are readily available for sale in that settlement. This effect stacks up to three times. Checks attempted to Identify Alchemy in any settlement with at least one alchemy laboratory gain a +1 item bonus.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("arcanists_tower".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Quell Unrest using Magic");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","Treat the settlement\u2019s level as one level higher than its actual level for the purposes of determining which arcane magic items are readily available for sale in that settlement. This effect stacks up to three times. While in a settlement with an arcanist\u2019s tower, you gain a +1 item bonus to checks made to Borrow an Arcane Spell or Learn a Spell.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("arena".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","EDIFICE,YARD");
                officialBase.put("official_item_bonus_rule","+2 item bonus to Celebrate Holiday and to Warfare checks made to Quell Unrest");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","An arena lets you to retrain combat-themed feats more efficiently while in the settlement; doing so takes only 5 days rather than a week of downtime.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("bank".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Tap Treasury");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The Capital Investment Leadership activity can be used only within the influence area of a settlement with a bank.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("barracks".equals(r36OfficialKey)){
                officialBase.put("residential_lots",1);
                officialBase.put("official_traits","BUILDING,RESIDENTIAL");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Garrison Army, Recover Army, or Recruit Army (see Appendix 3) 544545");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","Barracks aid in the recruitment of armies and in helping soldiers recover from battle. The first time you build a barracks in any settlement, reduce Unrest by 1.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","garrison");
            }
            if("brewery".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Establish Trade Agreement");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","When you build a brewery, reduce Unrest by 1 as long as you have fewer than 4 breweries in the settlement at that time.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("bridge".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","INFRASTRUCTURE");
                officialBase.put("official_item_bonus_rule","");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","A bridge allows an island settlement to provide influence (see Influence on page 542), negates the Trade penalty for island settlements (see Land Borders on page 540), and allows travel over its associated Water Border with ease (see Navigating an Urban Grid on page 541). Bridges can only be built on Water Borders. When you build a bridge, check the \u201cBridge\u201d box on one of the Water Borders on your Urban Grid to indicate its location.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("castle".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING,EDIFICE,FAMOUS,INFAMOUS");
                officialBase.put("official_item_bonus_rule","+2 item bonus to New Leadership, Pledge of Fealty, Send Diplomatic Envoy, and +2 item bonus to Garrison Army, Recover Army, or Recruit Army (see Appendix 3: Warfare)");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The first time you build a castle each Kingdom turn, reduce Unrest by 1d4. A castle in a capital allows PC leaders to take 3 Leadership activities during the Activity phase of a Kingdom turn rather than 2.");
                officialBase.put("official_upgrade_from","town hall");
                officialBase.put("official_upgrade_to","palace");
            }
            if("cathedral".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING,EDIFICE,FAMOUS,INFAMOUS");
                officialBase.put("official_item_bonus_rule","+3 item bonus to Celebrate Holiday, Provide Care, and Repair Reputation (Corruption)");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The first time you build a cathedral in a turn, reduce Unrest by 4. While in a settlement with a cathedral, you gain a +3 item bonus to Lore and Religion checks made to Recall Knowledge while Investigating, and to all faith-themed checks made while Researching (Gamemastery Guide 154). Treat the settlement\u2019s level as three levels higher than its actual level for the purposes of determining what divine magic items are available for sale in that settlement. This effect does not stack with the similar effect granted by shrines or temples.");
                officialBase.put("official_upgrade_from","temple");
                officialBase.put("official_upgrade_to","");
            }
            if("cemetery".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","YARD");
                officialBase.put("official_item_bonus_rule","");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","Giving the citizens a place to bury and remember their departed loved ones helps to temper Unrest gained A FLYING OWLBEAR 30 XP Loris Shadwest, eager taxidermist and avid collector of strangeness, wants to stuff a winged owlbear. While lesser taxidermists might stick a giant bird\u2019s wings onto a normal owlbear, Loris wants his masterpiece to be \u201creal.\u201d He\u2019s promised a powerful magic item to the first person to deliver to him a real winged owlbear carcass... in good condition! Source: Once the kingdom reaches 17th level, Loris gets this idea in his head and approaches the PCs with his request. Completion: Winged owlbears do exist... just not in this world. The PCs will encounter these creatures once Chapter 10 begins. Reward: The magic item Loris promises is legitimately powerful: a moonstone crystal ball that has been in his family for years. 545 APPENDICES APPENDIX 1: BEYOND THE CAMPAIGN APPENDIX 2: KINGDOMS APPENDIX 3: WARFARE APPENDIX 4: KINGMAKER TREASURES APPENDIX 5: NPCS AND MONSTERS ADDITIONAL RESOURCES LORIS SHADWEST546 from dangerous events. If you have at least one cemetery in a settlement, reduce Unrest gained from any dangerous settlement events in that particular settlement by 1 (to a maximum of 4 for four cemeteries).");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("construction_yard".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","YARD");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Build Structure and to Repair Reputation (Decay)");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("dump".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","YARD");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Demolish");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","Certain events have a more dangerous impact on settlements that don\u2019t include a dump. A dump can\u2019t be located in a block with any Residential structures.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("embassy".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Send Diplomatic Envoy and Request Foreign Aid");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("festival_hall".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Celebrate Holiday");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","theater");
            }
            if("foundry".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Establish Work Site (mine)");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","By processing ore in a foundry, your settlements grow more efficient at storing your kingdom\u2019s Commodities. Each foundry in your kingdom increases your maximum Ore Commodity capacity by 1. A foundry cannot share a block with a Residential structure.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("garrison".equals(r36OfficialKey)){
                officialBase.put("residential_lots",2);
                officialBase.put("official_traits","BUILDING,RESIDENTIAL");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Outfit Army or Train Army (see Appendix 3: Warfare)");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","A garrison helps outfit armies with new gear or trains them. When you build a garrison, reduce Unrest by 1.");
                officialBase.put("official_upgrade_from","barracks");
                officialBase.put("official_upgrade_to","");
            }
            if("general_store".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","A settlement without a general store or marketplace reduces its effective level for the purposes of determining what items can be purchased there by 2.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","luxury store, marketplace");
            }
            if("granary".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","Each granary in your kingdom increases your maximum Food Commodity capacity by 1.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("guildhall".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Economy skill checks associated with the guildhall\u2019s specific trade focus");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","When you build a guildhall, indicate what sort of organization (such as bakers, grocers, smiths, etc.) it serves as a headquarters for. While in a settlement with a guildhall, you gain a +1 item bonus to all related skill checks to Earn Income or to Repair. 546547");
                officialBase.put("official_upgrade_from","trade shop");
                officialBase.put("official_upgrade_to","");
            }
            if("herbalist".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Provide Care");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","hospital");
            }
            if("hospital".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Provide Care and Quell Unrest");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","While in a settlement with a hospital, you gain a +2 item bonus to Medicine checks to Treat Disease and Treat Wounds.");
                officialBase.put("official_upgrade_from","herbalist");
                officialBase.put("official_upgrade_to","");
            }
            if("houses".equals(r36OfficialKey)){
                officialBase.put("residential_lots",1);
                officialBase.put("official_traits","BUILDING,RESIDENTIAL");
                officialBase.put("official_item_bonus_rule","");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The first time you build houses each Kingdom turn, reduce Unrest by 1.");
                officialBase.put("official_upgrade_from","tenement");
                officialBase.put("official_upgrade_to","mansion or orphanage");
            }
            if("illicit_market".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING,INFAMOUS");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Clandestine Business");
                officialBase.put("official_ruin_rule","+1 Crime");
                officialBase.put("official_effects_rule","Treat the settlement\u2019s level as one level higher than its actual level for the purposes of determining what items are readily available for sale in that settlement. This effect stacks up to three times.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("inn".equals(r36OfficialKey)){
                officialBase.put("residential_lots",1);
                officialBase.put("official_traits","BUILDING,RESIDENTIAL");
                officialBase.put("official_item_bonus_rule","+1 Item bonus to Hire Adventurers");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("jail".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Quell Unrest using Intrigue");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The first time you build a jail each a Kingdom turn, reduce Crime by 1.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("keep".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING,EDIFICE");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Deploy Army, Garrison Army, or Train Army (see Appendix 3: Warfare)");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The first time you build a keep each Kingdom turn, reduce Unrest by 1.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("library".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Rest and Relax using Scholarship checks");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","While in a settlement with a library, you gain a +1");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","academy");
            }
            if("lumberyard".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","YARD");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Establish Work Site (lumber camp)");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","Each lumberyard in your kingdom increases maximum Lumber Commodity capacity by 1. A lumberyard must be built in a lot next to a Water border, both to give the yard a source of power to run saws to process timber, but more importantly to facilitate the shipment of trees to the yard. 547 APPENDICES APPENDIX 1: BEYOND THE CAMPAIGN APPENDIX 2: KINGDOMS APPENDIX 3: WARFARE APPENDIX 4: KINGMAKER TREASURES APPENDIX 5: NPCS AND MONSTERS ADDITIONAL RESOURCES548");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("luxury_store".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Establish Trade Agreement");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","A luxury store must be built on a block that has either a mansion or a noble villa. Treat the settlement\u2019s level as one level higher than its actual level for determining what luxury\u0002themed magic items (subject to GM approval) are readily available for sale in that settlement. This effect stacks up to three times and overlaps with other stores that function in this way for more specific categories of magic items.");
                officialBase.put("official_upgrade_from","general store");
                officialBase.put("official_upgrade_to","magic shop");
            }
            if("magic_shop".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Supernatural Solution");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","Treat the settlement\u2019s level as one level higher than its actual level for the purposes of determining what magic items are readily available for sale in that settlement. This effect stacks up to three times and overlaps with other stores that function in this way for more specific categories of magic items.");
                officialBase.put("official_upgrade_from","luxury store");
                officialBase.put("official_upgrade_to","occult shop");
            }
            if("magical_streetlamps".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","INFRASTRUCTURE");
                officialBase.put("official_item_bonus_rule","");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","Magical streetlamps provide nighttime illumination for an entire Urban Grid. When you build magical streetlamps, check the magical streetlamps checkbox on your Urban Grid. The first time you build magical streetlamps in a Kingdom turn, reduce Crime by 1.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("mansion".equals(r36OfficialKey)){
                officialBase.put("residential_lots",1);
                officialBase.put("official_traits","BUILDING,RESIDENTIAL");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Improve Lifestyle");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","");
                officialBase.put("official_upgrade_from","houses");
                officialBase.put("official_upgrade_to","noble villa");
            }
            if("marketplace".equals(r36OfficialKey)){
                officialBase.put("residential_lots",2);
                officialBase.put("official_traits","BUILDING,RESIDENTIAL");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Establish Trade Agreement");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","A town without a general store or marketplace reduces its effective level for the purposes of determining what items can be purchased there by 2.");
                officialBase.put("official_upgrade_from","general store");
                officialBase.put("official_upgrade_to","");
            }
            if("menagerie".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING,EDIFICE");
                officialBase.put("official_item_bonus_rule","+2 item bonus to Rest and Relax using Wilderness");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","A menagerie typically contains a selection of level 5 or lower animals. If your party captures a living creature of level 6 or higher and can transport the creature back to a settlement with a menagerie, you can add that creature to the menagerie as long as your kingdom level is at least 4 higher than the creature\u2019s level. Each time such a creature is added to a menagerie, gain 1 Fame or Infamy point (as appropriate) or reduce one Ruin of your choice by 1. Only creatures with Intelligence modifiers of \u20134 or \u20135 are appropriate to place in a menagerie. A kingdom gains 1 Unrest at the start of a Kingdom turn for each sapient creature (anything with an Intelligence modifier of \u20133 or higher) on display in a menagerie.");
                officialBase.put("official_upgrade_from","park");
                officialBase.put("official_upgrade_to","");
            }
            if("military_academy".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING,EDIFICE");
                officialBase.put("official_item_bonus_rule","+2 item bonus to Pledge of Fealty using Warfare, +2 item bonus to Train Army (see Appendix 3: Warfare)");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","");
                officialBase.put("official_upgrade_from","academy");
                officialBase.put("official_upgrade_to","");
            }
            if("mill".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Harvest Crops");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","If a settlement includes at least one mill built on a lot adjacent to a Water border, the increased efficiency of these mills reduces the settlement\u2019s Consumption by 1 (to a minimum of 0).");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("mint".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING,EDIFICE");
                officialBase.put("official_item_bonus_rule","+3 item bonus to Capital Investment, Collect Taxes, and to Repair Reputation (Crime)");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("monument".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING,EDIFICE");
                officialBase.put("official_item_bonus_rule","");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The first time you build a monument each Kingdom turn, reduce Unrest by 1 and reduce one Ruin of your choice by 1.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("museum".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING,FAMOUS,INFAMOUS");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Rest and Relax using Arts");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","A magic item of level 6 or higher that has a particular import or bears significant historical or regional value (at the GM\u2019s discretion) can be donated to a museum. Each time such an item is donated, reduce Unrest by 1. If that item is later removed from display, increase Unrest by 1.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("noble_villa".equals(r36OfficialKey)){
                officialBase.put("residential_lots",2);
                officialBase.put("official_traits","BUILDING,RESIDENTIAL");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Improve Lifestyle and to Quell Unrest using Politics");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The first time you build a noble villa each Kingdom turn, reduce Unrest by 2.");
                officialBase.put("official_upgrade_from","mansion");
                officialBase.put("official_upgrade_to","");
            }
            if("occult_shop".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+2 item bonus to Prognostication");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","Treat the settlement\u2019s level as one level higher than its actual level for the purposes of determining what magic items are readily available for sale in that settlement. This effect stacks up to three times and overlaps with other stores that function in this way for more specific categories of magic items. While in a settlement with an occult shop, you gain a +2 item bonus to all checks made to Research esoteric subjects or to Recall Knowledge about the same.");
                officialBase.put("official_upgrade_from","magic shop");
                officialBase.put("official_upgrade_to","");
            }
            if("opera_house".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING,EDIFICE,FAMOUS,INFAMOUS");
                officialBase.put("official_item_bonus_rule","+3 item bonus to Celebrate Holiday and Create a Masterpiece");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The first time you build an opera house each Kingdom turn, reduce Unrest by 4. While in a settlement with an opera house, you gain a +3 item bonus to Performance checks made to Earn Income.");
                officialBase.put("official_upgrade_from","theater");
                officialBase.put("official_upgrade_to","");
            }
            if("orphanage".equals(r36OfficialKey)){
                officialBase.put("residential_lots",1);
                officialBase.put("official_traits","BUILDING,RESIDENTIAL");
                officialBase.put("official_item_bonus_rule","");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The first time you build an orphanage each Kingdom turn, reduce Unrest by 1. 549 APPENDICES APPENDIX 1: BEYOND THE CAMPAIGN APPENDIX 2: KINGDOMS APPENDIX 3: WARFARE APPENDIX 4: KINGMAKER TREASURES APPENDIX 5: NPCS AND MONSTERS ADDITIONAL RESOURCES550");
                officialBase.put("official_upgrade_from","houses");
                officialBase.put("official_upgrade_to","");
            }
            if("palace".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING,EDIFICE,FAMOUS,INFAMOUS");
                officialBase.put("official_item_bonus_rule","+3 item bonus to New Leadership, Pledge of Fealty, and Send Diplomatic Envoy, and +3 item bonus to Garrison Army, Recover Army, or Recruit Army (see Appendix 3: Warfare)");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","A palace can only be built in your capital. The first time you build a palace, reduce Unrest by 10. If you Relocate your Capital, a palace left behind in that capital instead functions as a noble villa that takes up 4 lots. (If you represent this by placing two noble villas in these lots, make sure to note that they constitute a single building and aren\u2019t two separate structures.) A palace in a capital allows PC leaders to take 3 Leadership activities during the Activity phase of a Kingdom turn rather than just 2. In addition, once your kingdom has a palace, a PC in the Ruler leadership role gains a +3 item bonus to checks made to resolve Leadership activities.");
                officialBase.put("official_upgrade_from","castle");
                officialBase.put("official_upgrade_to","");
            }
            if("park".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","YARD");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Rest and Relax using Wilderness checks");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The first time you build a park each Kingdom turn, reduce Unrest by 1.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","menagerie");
            }
            if("paved_streets".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","INFRASTRUCTURE");
                officialBase.put("official_item_bonus_rule","");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","It takes a character only 5 minutes to move from one lot to an adjacent lot in an Urban Grid when moving on paved streets. When you build paved streets, check the paved streets checkbox on your Urban Grid.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("pier".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","YARD");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Go Fishing");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","A pier must be built in a lot next to a Water border. RUBBLE STRUCTURE \u2014 YARD An unsightly heap of rubble fills this lot. Lots 1; Cost \u2014");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","waterfront");
            }
            if("sacred_grove".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","YARD");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Quell Unrest using Folklore");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","Treat the settlement\u2019s level as one level higher than its actual level for the purposes of determining what primal magic items are readily available for sale in that settlement. This effect stacks up to three times.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("secure_warehouse".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Craft Luxuries");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","Each secure warehouse in your kingdom increases your maximum Luxuries Commodity capacity by 1.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("sewer_system".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","INFRASTRUCTURE");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Clandestine Business");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","A sewer system reduces the settlement\u2019s Consumption by 1. Having a sewer system can also affect certain kingdom events. When you build a sewer system, check the sewer system checkbox on its Urban Grid. (For metropolises, this infrastructure automatically applies to all of its Urban Grids.)");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("shrine".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Celebrate Holiday");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","Treat the settlement\u2019s level as one level higher than its actual level when determining what divine magic items are readily available for sale in that settlement. This effect stacks up to three times but does not stack with the same effect granted by temples or cathedrals.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","temple");
            }
            if("smithy".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Trade Commodities, +1 item bonus to Outfit Army (see Appendix 3: Warfare)");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","While in a settlement with a smithy, you gain a +1");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("specialized_artisan".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Craft Luxuries");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","While in a settlement with a specialized artisan, you gain a +1 item bonus to Craft checks made to craft specialized goods like jewelry.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("stables".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","YARD");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Establish Trade Agreement");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("stockyard".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","YARD");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Gather Livestock");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","A settlement with at least one stockyard reduces its Consumption by 1.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("stonemason".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Establish Work Site (quarry).");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","Each stonemason in your kingdom increases your maximum Stone Commodity capacity by 1.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("tannery".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 to Trade Commodities");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","A tannery cannot share a block with any Residential structure except tenements.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("tavern_dive".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The first time you build a dive tavern in a Kingdom turn, reduce Unrest by 1 but increase Crime by 1.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","tavern, popular");
            }
            if("tavern_popular".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Hire Adventurers and to Rest and Relax using Trade");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The first time you build a popular tavern in a Kingdom turn, reduce Unrest by 2. If you attempt a Performance check to Earn Income in a settlement with a popular tavern, you gain a +1 item bonus to the check. All checks made to Gather Information in a settlement with at least one popular tavern gain a +1 item bonus.");
                officialBase.put("official_upgrade_from","tavern, dive");
                officialBase.put("official_upgrade_to","tavern, luxury");
            }
            if("tavern_luxury".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING,FAMOUS");
                officialBase.put("official_item_bonus_rule","+2 item bonus to Hire Adventurers and to Rest and Relax using Trade");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The first time you build a luxury tavern in a Kingdom 551 APPENDICES APPENDIX 1: BEYOND THE CAMPAIGN APPENDIX 2: KINGDOMS APPENDIX 3: WARFARE APPENDIX 4: KINGMAKER TREASURES APPENDIX 5: NPCS AND MONSTERS ADDITIONAL RESOURCES552 turn, reduce Unrest by 1d4+1. If attempt a Performance check to Earn Income in a settlement with a luxury tavern, you gain a +2 item bonus to the check. All checks made to Gather Information in a settlement with at least one luxury tavern gain a +2 item bonus.");
                officialBase.put("official_upgrade_from","tavern, popular");
                officialBase.put("official_upgrade_to","tavern, world-class");
            }
            if("tavern_world_class".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING,EDIFICE,FAMOUS");
                officialBase.put("official_item_bonus_rule","+3 item bonus to Hire Adventurers, to Rest and Relax using Trade, and to Repair Reputation (Strife)");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The first time you build a world-class tavern in a Kingdom turn, reduce Unrest by 2d4. If you attempt a Performance check to Earn Income in a settlement with a world-class tavern, you gain a +3 item bonus to the check. All checks made to Gather Information in a settlement with a world-class tavern gain a +3 item bonus.");
                officialBase.put("official_upgrade_from","tavern, luxury");
                officialBase.put("official_upgrade_to","");
            }
            if("temple".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING,FAMOUS,INFAMOUS");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Celebrate Holiday and Provide Care");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The first time you build a temple each Kingdom turn, reduce Unrest by 2. Treat the settlement\u2019s level as one level higher than its actual level for the purposes of determining what divine magic items are readily available for sale in that settlement. This effect stacks up to three times but does not stack with the same effect granted by shrines or cathedrals.");
                officialBase.put("official_upgrade_from","shrine");
                officialBase.put("official_upgrade_to","cathedral");
            }
            if("tenement".equals(r36OfficialKey)){
                officialBase.put("residential_lots",1);
                officialBase.put("official_traits","BUILDING,RESIDENTIAL");
                officialBase.put("official_item_bonus_rule","");
                officialBase.put("official_ruin_rule","+1 to a Ruin of your choice");
                officialBase.put("official_effects_rule","The first time you build tenements each Kingdom turn, reduce Unrest by 1.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","Houses");
            }
            if("theater".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+2 item bonus to Celebrate Holiday.");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The first time you build a theater each Kingdom turn, reduce Unrest by 1. While in a settlement with a theater, you gain a +2 item bonus to Performance checks made to Earn Income.");
                officialBase.put("official_upgrade_from","festival hall");
                officialBase.put("official_upgrade_to","opera house");
            }
            if("thieves_guild".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING,INFAMOUS");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Infiltration");
                officialBase.put("official_ruin_rule","+1 Crime");
                officialBase.put("official_effects_rule","While in a settlement with a thieves\u2019 guild, you gain a +1 item bonus to Create Forgeries.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("town_hall".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING,EDIFICE");
                officialBase.put("official_item_bonus_rule","");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The first time you build a town hall each Kingdom turn, reduce Unrest by 1. A town hall in a capital allows PC leaders to take 3 Leadership activities during the Activity phase of a Kingdom turn rather than just 2.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","castle");
            }
            if("trade_shop".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Purchase Commodities");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","When you build a trade shop, indicate the kind of shop it is, such as a bakery, carpenter, tailor, and so on. While in a settlement with a trade shop, you gain a +1");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","guildhall");
            }
            if("university".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING,EDIFICE,FAMOUS");
                officialBase.put("official_item_bonus_rule","+3 item bonus to Creative Solution 552553");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","While in a settlement with a university, you gain a +3 item bonus to Lore checks made to Recall Knowledge while Investigating, to Research checks (Gamemastery Guide 154), and to Decipher Writing.");
                officialBase.put("official_upgrade_from","academy");
                officialBase.put("official_upgrade_to","");
            }
            if("wall_stone".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","INFRASTRUCTURE");
                officialBase.put("official_item_bonus_rule","");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","A stone wall is built along the border of your settlement. The first time you build a stone wall in each settlement, reduce Unrest by 1. When you build a stone wall, choose a border on your Urban Grid and check the appropriate checkbox; if you\u2019re upgrading from a wooden wall, uncheck that box.");
                officialBase.put("official_upgrade_from","wooden wall");
                officialBase.put("official_upgrade_to","");
            }
            if("wall_wooden".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","INFRASTRUCTURE");
                officialBase.put("official_item_bonus_rule","");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","A wooden wall is built along the border of your settlement. The first time you build a wooden wall in each settlement, reduce Unrest by 1. When you build a wooden wall, choose a border on your Urban Grid and check the appropriate checkbox.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","stone wall");
            }
            if("watchtower".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","BUILDING");
                officialBase.put("official_item_bonus_rule","+1 item bonus to checks to resolve events affecting the settlement.");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","The first time you build a watchtower each Kingdom turn, decrease Unrest by 1.");
                officialBase.put("official_upgrade_from","");
                officialBase.put("official_upgrade_to","");
            }
            if("waterfront".equals(r36OfficialKey)){
                officialBase.put("residential_lots",0);
                officialBase.put("official_traits","YARD");
                officialBase.put("official_item_bonus_rule","+1 item bonus to Go Fishing, and to Establish Trade Agreement and Rest and Relax using Boating");
                officialBase.put("official_ruin_rule","");
                officialBase.put("official_effects_rule","A waterfront must be constructed next to a Water Border. A settlement with at least 1 waterfront increases its effective level by 1 for the purposes of determining what level of items can be purchased in that settlement; this bonus stacks with similar bonuses in the settlement. KINGDOM EVENTS As the PCs\u2019 kingdom grows, all manner of unusual or irregular events will affect its fortunes and guide its growth. There are two categories of kingdom events: story events that occur as a result of the campaign plotline and random events. Story events are resolved when they occur, as detailed in the earlier chapters of this Adventure Path and often include greater details for how the PCs can take part in resolving the event. These events take place during regular play even though they draw upon kingdom statistics. They\u2019re usually resolved during downtime. Random events are resolved entirely by the kingdom itself and take place within the Event phase of a Kingdom turn. A flat check at the start of this phase determines whether a random event occurs (page 539), although you could roll this check in advance for any number of Kingdom turns or even just choose which events are going to take place for a span of time. Determining the events that wi");
                officialBase.put("official_upgrade_from","pier");
                officialBase.put("official_upgrade_to","");
            }
            if("arcanists_tower".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","ARCANIST\u2019S TOWER").put("level",5).put("lots",1).put("rp",30).put("lumber",0).put("stone",6).put("ore",0).put("luxuries",0).put("construction_rank","trained").put("construction_dc",20).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","arcanists_tower").put("construction_skill","magic").put("special_effects_semantic_certification","PENDING");
            }
            if("arena".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","ARENA").put("level",9).put("lots",4).put("rp",40).put("lumber",6).put("stone",12).put("ore",0).put("luxuries",0).put("construction_rank","expert").put("construction_dc",26).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","arena").put("construction_skill","warfare").put("special_effects_semantic_certification","PENDING");
            }
            if("bridge".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","BRIDGE").put("level",2).put("lots",0).put("rp",6).put("lumber",1).put("stone",1).put("ore",0).put("luxuries",0).put("construction_rank","untrained").put("construction_dc",16).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","bridge").put("construction_skill","engineering").put("requires_special_placement",true).put("requires_cost_choice",true).put("official_alternative_cost_text","6 RP, 1 Lumber or 1 Stone").put("special_effects_semantic_certification","PENDING");
            }
            if("cemetery".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","CEMETERY").put("level",1).put("lots",1).put("rp",4).put("lumber",0).put("stone",1).put("ore",0).put("luxuries",0).put("construction_rank","untrained").put("construction_dc",15).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","cemetery").put("construction_skill","folklore").put("special_effects_semantic_certification","PENDING");
            }
            if("construction_yard".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","CONSTRUCTION YARD").put("level",10).put("lots",4).put("rp",40).put("lumber",10).put("stone",10).put("ore",0).put("luxuries",0).put("construction_rank","untrained").put("construction_dc",27).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","construction_yard").put("construction_skill","engineering").put("special_effects_semantic_certification","PENDING");
            }
            if("dump".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","DUMP").put("level",2).put("lots",1).put("rp",4).put("lumber",0).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","untrained").put("construction_dc",16).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","dump").put("construction_skill","industry").put("special_effects_semantic_certification","PENDING");
            }
            if("festival_hall".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","FESTIVAL HALL").put("level",3).put("lots",1).put("rp",7).put("lumber",3).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","untrained").put("construction_dc",18).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","festival_hall").put("construction_skill","arts").put("special_effects_semantic_certification","PENDING");
            }
            if("foundry".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","FOUNDRY").put("level",3).put("lots",2).put("rp",16).put("lumber",5).put("stone",3).put("ore",2).put("luxuries",0).put("construction_rank","trained").put("construction_dc",18).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","foundry").put("construction_skill","industry").put("special_effects_semantic_certification","PENDING");
            }
            if("general_store".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","GENERAL STORE").put("level",1).put("lots",1).put("rp",8).put("lumber",1).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","untrained").put("construction_dc",15).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","general_store").put("construction_skill","trade").put("special_effects_semantic_certification","PENDING");
            }
            if("guildhall".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","GUILDHALL").put("level",5).put("lots",2).put("rp",34).put("lumber",8).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","expert").put("construction_dc",20).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","guildhall").put("construction_skill","trade").put("special_effects_semantic_certification","PENDING");
            }
            if("herbalist".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","HERBALIST").put("level",1).put("lots",1).put("rp",10).put("lumber",1).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","untrained").put("construction_dc",15).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","herbalist").put("construction_skill","wilderness").put("special_effects_semantic_certification","PENDING");
            }
            if("hospital".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","HOSPITAL").put("level",9).put("lots",2).put("rp",30).put("lumber",10).put("stone",6).put("ore",0).put("luxuries",0).put("construction_rank","expert").put("construction_dc",26).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","hospital").put("construction_skill","defense").put("special_effects_semantic_certification","PENDING");
            }
            if("houses".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","HOUSES").put("level",1).put("lots",1).put("rp",3).put("lumber",1).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","untrained").put("construction_dc",15).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","houses").put("construction_skill","industry").put("special_effects_semantic_certification","PENDING");
            }
            if("illicit_market".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","ILLICIT MARKET").put("level",6).put("lots",1).put("rp",50).put("lumber",5).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","trained").put("construction_dc",22).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","illicit_market").put("construction_skill","intrigue").put("special_effects_semantic_certification","PENDING");
            }
            if("inn".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","INN").put("level",1).put("lots",1).put("rp",10).put("lumber",2).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","untrained").put("construction_dc",15).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","inn").put("construction_skill","trade").put("special_effects_semantic_certification","PENDING");
            }
            if("jail".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","JAIL").put("level",2).put("lots",1).put("rp",14).put("lumber",4).put("stone",4).put("ore",2).put("luxuries",0).put("construction_rank","untrained").put("construction_dc",16).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","jail").put("construction_skill","defense").put("special_effects_semantic_certification","PENDING");
            }
            if("keep".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","KEEP").put("level",3).put("lots",2).put("rp",32).put("lumber",8).put("stone",8).put("ore",0).put("luxuries",0).put("construction_rank","trained").put("construction_dc",18).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","keep").put("construction_skill","defense").put("special_effects_semantic_certification","PENDING");
            }
            if("lumberyard".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","LUMBERYARD").put("level",3).put("lots",2).put("rp",16).put("lumber",5).put("stone",0).put("ore",1).put("luxuries",0).put("construction_rank","trained").put("construction_dc",18).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","lumberyard").put("construction_skill","industry").put("special_effects_semantic_certification","PENDING");
            }
            if("luxury_store".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","LUXURY STORE").put("level",6).put("lots",1).put("rp",28).put("lumber",10).put("stone",0).put("ore",0).put("luxuries",6).put("construction_rank","expert").put("construction_dc",22).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","luxury_store").put("construction_skill","trade").put("special_effects_semantic_certification","PENDING");
            }
            if("magic_shop".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","MAGIC SHOP").put("level",8).put("lots",1).put("rp",44).put("lumber",8).put("stone",6).put("ore",0).put("luxuries",6).put("construction_rank","expert").put("construction_dc",24).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","magic_shop").put("construction_skill","magic").put("special_effects_semantic_certification","PENDING");
            }
            if("magical_streetlamps".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","MAGICAL STREETLAMPS").put("level",5).put("lots",0).put("rp",20).put("lumber",0).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","expert").put("construction_dc",20).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","magical_streetlamps").put("construction_skill","magic").put("requires_special_placement",true).put("special_effects_semantic_certification","PENDING");
            }
            if("mansion".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","MANSION").put("level",5).put("lots",1).put("rp",10).put("lumber",6).put("stone",3).put("ore",0).put("luxuries",6).put("construction_rank","trained").put("construction_dc",20).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","mansion").put("construction_skill","industry").put("special_effects_semantic_certification","PENDING");
            }
            if("marketplace".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","MARKETPLACE").put("level",4).put("lots",2).put("rp",48).put("lumber",4).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","trained").put("construction_dc",19).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","marketplace").put("construction_skill","trade").put("special_effects_semantic_certification","PENDING");
            }
            if("menagerie".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","MENAGERIE").put("level",12).put("lots",4).put("rp",26).put("lumber",14).put("stone",10).put("ore",10).put("luxuries",0).put("construction_rank","expert").put("construction_dc",30).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","menagerie").put("construction_skill","wilderness").put("special_effects_semantic_certification","PENDING");
            }
            if("military_academy".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","MILITARY ACADEMY").put("level",12).put("lots",2).put("rp",36).put("lumber",12).put("stone",10).put("ore",6).put("luxuries",0).put("construction_rank","expert").put("construction_dc",30).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","military_academy").put("construction_skill","warfare").put("special_effects_semantic_certification","PENDING");
            }
            if("mill".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","MILL").put("level",2).put("lots",1).put("rp",6).put("lumber",2).put("stone",1).put("ore",0).put("luxuries",0).put("construction_rank","trained").put("construction_dc",16).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","mill").put("construction_skill","industry").put("special_effects_semantic_certification","PENDING");
            }
            if("mint".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","MINT").put("level",15).put("lots",1).put("rp",30).put("lumber",12).put("stone",16).put("ore",20).put("luxuries",0).put("construction_rank","master").put("construction_dc",34).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","mint").put("construction_skill","trade").put("special_effects_semantic_certification","PENDING");
            }
            if("museum".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","MUSEUM").put("level",5).put("lots",2).put("rp",30).put("lumber",6).put("stone",2).put("ore",0).put("luxuries",0).put("construction_rank","trained").put("construction_dc",20).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","museum").put("construction_skill","exploration").put("special_effects_semantic_certification","PENDING");
            }
            if("noble_villa".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","NOBLE VILLA").put("level",9).put("lots",2).put("rp",24).put("lumber",10).put("stone",8).put("ore",0).put("luxuries",6).put("construction_rank","expert").put("construction_dc",19).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","noble_villa").put("construction_skill","politics").put("special_effects_semantic_certification","PENDING");
            }
            if("occult_shop".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","OCCULT SHOP").put("level",13).put("lots",1).put("rp",68).put("lumber",12).put("stone",6).put("ore",0).put("luxuries",12).put("construction_rank","master").put("construction_dc",32).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","occult_shop").put("construction_skill","magic").put("special_effects_semantic_certification","PENDING");
            }
            if("opera_house".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","OPERA HOUSE").put("level",15).put("lots",2).put("rp",40).put("lumber",20).put("stone",16).put("ore",0).put("luxuries",18).put("construction_rank","master").put("construction_dc",34).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","opera_house").put("construction_skill","arts").put("special_effects_semantic_certification","PENDING");
            }
            if("orphanage".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","ORPHANAGE").put("level",2).put("lots",1).put("rp",6).put("lumber",2).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","untrained").put("construction_dc",16).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","orphanage").put("construction_skill","industry").put("special_effects_semantic_certification","PENDING");
            }
            if("palace".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","PALACE").put("level",15).put("lots",4).put("rp",108).put("lumber",20).put("stone",20).put("ore",15).put("luxuries",12).put("construction_rank","master").put("construction_dc",34).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","palace").put("construction_skills","defense,industry,magic,statecraft").put("special_effects_semantic_certification","PENDING");
            }
            if("park".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","PARK").put("level",3).put("lots",1).put("rp",5).put("lumber",0).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","untrained").put("construction_dc",18).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","park").put("construction_skill","wilderness").put("special_effects_semantic_certification","PENDING");
            }
            if("paved_streets".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","PAVED STREETS").put("level",4).put("lots",0).put("rp",12).put("lumber",0).put("stone",6).put("ore",0).put("luxuries",0).put("construction_rank","trained").put("construction_dc",19).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","paved_streets").put("construction_skill","industry").put("requires_special_placement",true).put("special_effects_semantic_certification","PENDING");
            }
            if("sacred_grove".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","SACRED GROVE").put("level",5).put("lots",1).put("rp",36).put("lumber",0).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","trained").put("construction_dc",20).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","sacred_grove").put("construction_skill","wilderness").put("special_effects_semantic_certification","PENDING");
            }
            if("secure_warehouse".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","SECURE WAREHOUSE").put("level",6).put("lots",2).put("rp",24).put("lumber",6).put("stone",6).put("ore",4).put("luxuries",0).put("construction_rank","expert").put("construction_dc",22).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","secure_warehouse").put("construction_skill","industry").put("special_effects_semantic_certification","PENDING");
            }
            if("sewer_system".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","SEWER SYSTEM").put("level",7).put("lots",0).put("rp",24).put("lumber",8).put("stone",8).put("ore",0).put("luxuries",0).put("construction_rank","expert").put("construction_dc",23).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","sewer_system").put("construction_skill","engineering").put("requires_special_placement",true).put("special_effects_semantic_certification","PENDING");
            }
            if("shrine".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","SHRINE").put("level",1).put("lots",1).put("rp",8).put("lumber",2).put("stone",1).put("ore",0).put("luxuries",0).put("construction_rank","trained").put("construction_dc",15).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","shrine").put("construction_skill","folklore").put("special_effects_semantic_certification","PENDING");
            }
            if("specialized_artisan".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","SPECIALIZED ARTISAN").put("level",4).put("lots",1).put("rp",10).put("lumber",4).put("stone",0).put("ore",0).put("luxuries",1).put("construction_rank","expert").put("construction_dc",19).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","specialized_artisan").put("construction_skill","trade").put("special_effects_semantic_certification","PENDING");
            }
            if("stockyard".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","STOCKYARD").put("level",3).put("lots",4).put("rp",20).put("lumber",4).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","untrained").put("construction_dc",18).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","stockyard").put("construction_skill","industry").put("special_effects_semantic_certification","PENDING");
            }
            if("stonemason".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","STONEMASON").put("level",3).put("lots",2).put("rp",16).put("lumber",2).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","trained").put("construction_dc",18).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","stonemason").put("construction_skill","industry").put("special_effects_semantic_certification","PENDING");
            }
            if("tannery".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","TANNERY").put("level",3).put("lots",1).put("rp",6).put("lumber",2).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","trained").put("construction_dc",18).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","tannery").put("construction_skill","industry").put("special_effects_semantic_certification","PENDING");
            }
            if("tavern_dive".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","TAVERN, DIVE").put("level",1).put("lots",1).put("rp",12).put("lumber",1).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","trained").put("construction_dc",15).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","tavern_dive").put("construction_skill","trade").put("special_effects_semantic_certification","PENDING");
            }
            if("tavern_popular".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","TAVERN, POPULAR").put("level",3).put("lots",1).put("rp",24).put("lumber",6).put("stone",2).put("ore",0).put("luxuries",0).put("construction_rank","expert").put("construction_dc",18).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","tavern_popular").put("construction_skill","trade").put("special_effects_semantic_certification","PENDING");
            }
            if("tavern_luxury".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","TAVERN, LUXURY").put("level",9).put("lots",2).put("rp",48).put("lumber",10).put("stone",8).put("ore",0).put("luxuries",8).put("construction_rank","master").put("construction_dc",26).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","tavern_luxury").put("construction_skill","trade").put("special_effects_semantic_certification","PENDING");
            }
            if("tavern_world_class".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","TAVERN, WORLD-CLASS").put("level",15).put("lots",2).put("rp",64).put("lumber",18).put("stone",15).put("ore",0).put("luxuries",15).put("construction_rank","master").put("construction_dc",34).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","tavern_world_class").put("construction_skill","trade").put("special_effects_semantic_certification","PENDING");
            }
            if("tenement".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","TENEMENT").put("level",0).put("lots",1).put("rp",1).put("lumber",1).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","untrained").put("construction_dc",14).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","tenement").put("construction_skill","industry").put("special_effects_semantic_certification","PENDING");
            }
            if("thieves_guild".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","THIEVES\u2019 GUILD").put("level",5).put("lots",1).put("rp",25).put("lumber",4).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","trained").put("construction_dc",20).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","thieves_guild").put("construction_skill","intrigue").put("special_effects_semantic_certification","PENDING");
            }
            if("trade_shop".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","TRADE SHOP").put("level",3).put("lots",1).put("rp",10).put("lumber",2).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","trained").put("construction_dc",18).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","trade_shop").put("construction_skill","trade").put("special_effects_semantic_certification","PENDING");
            }
            if("university".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","UNIVERSITY").put("level",15).put("lots",4).put("rp",78).put("lumber",18).put("stone",18).put("ore",0).put("luxuries",18).put("construction_rank","master").put("construction_dc",34).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","university").put("construction_skill","scholarship").put("special_effects_semantic_certification","PENDING");
            }
            if("wall_stone".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","WALL, STONE").put("level",5).put("lots",0).put("rp",4).put("lumber",0).put("stone",8).put("ore",0).put("luxuries",0).put("construction_rank","trained").put("construction_dc",20).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","wall_stone").put("construction_skill","defense").put("requires_special_placement",true).put("special_effects_semantic_certification","PENDING");
            }
            if("wall_wooden".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","WALL, WOODEN").put("level",1).put("lots",0).put("rp",2).put("lumber",4).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","untrained").put("construction_dc",15).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","wall_wooden").put("construction_skill","defense").put("requires_special_placement",true).put("special_effects_semantic_certification","PENDING");
            }
            if("watchtower".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","WATCHTOWER").put("level",3).put("lots",1).put("rp",12).put("lumber",4).put("stone",4).put("ore",0).put("luxuries",0).put("construction_rank","trained").put("construction_dc",18).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","watchtower").put("construction_skill","defense").put("requires_cost_choice",true).put("official_alternative_cost_text","12 RP, 4 Lumber or 4 Stone").put("special_effects_semantic_certification","PENDING");
            }
            if("waterfront".equals(r36OfficialKey)) {
                officialBase.put("construction_skill","");
                officialBase.put("construction_skills","");
                return officialBase.put("name","WATERFRONT").put("level",8).put("lots",4).put("rp",90).put("lumber",10).put("stone",0).put("ore",0).put("luxuries",0).put("construction_rank","expert").put("construction_dc",24).put("official_source","Kingmaker.pdf Appendix 2 pp. 544-552").put("official_catalog_key","waterfront").put("construction_skill","boating").put("special_effects_semantic_certification","PENDING");
            }
        } catch(Exception officialCatalogFailure) {
            throw new IllegalStateException("Kingmaker official catalog failure",officialCatalogFailure);
        }
        // Invalid keys must still be rejected, never free 0-RP buildings.
        return hadR35Structure?officialBase:null;
    }
    private JSONObject structureR35Baseline(String key){
        String k=key(key);
        JSONObject s=new JSONObject();
        try{
            s.put("key",k);
            if("house".equals(k)){
                s.put("name","Casa Residencial").put("lots",1).put("level",1).put("rp",3).put("lumber",1).put("stone",0).put("ore",0).put("luxuries",0)
                 .put("residential_lots",1).put("item_bonus",0).put("unrest_reduction",0).put("prerequisites","")
                 .put("skill_bonuses",new JSONObject()).put("description","Habitação para cidadãos do assentamento. Evita a superlotação urbana.");
            }else if("tavern".equals(k)){
                s.put("name","Taberna").put("lots",1).put("level",1).put("rp",12).put("lumber",1).put("stone",0).put("ore",0).put("luxuries",1)
                 .put("residential_lots",0).put("item_bonus",1).put("unrest_reduction",0).put("prerequisites","")
                 .put("skill_bonuses",new JSONObject().put("arts",1).put("intrigue",1)).put("description","Ponto de encontro comunitário, apresentações musicais e rumores.");
            }else if("mill".equals(k)){
                s.put("name","Moinho").put("lots",1).put("level",1).put("rp",6).put("lumber",1).put("stone",1).put("ore",0).put("luxuries",0)
                 .put("residential_lots",0).put("item_bonus",1).put("unrest_reduction",0).put("prerequisites","")
                 .put("skill_bonuses",new JSONObject().put("agriculture",1).put("industry",1)).put("description","Moagem de grãos e serragem de troncos.");
            }else if("smithy".equals(k)){
                s.put("name","Ferreiro").put("lots",1).put("level",3).put("rp",8).put("lumber",2).put("stone",1).put("ore",1).put("luxuries",0)
                 .put("construction_skill","industry").put("construction_rank","trained").put("construction_dc",18).put("residential_lots",0).put("item_bonus",1).put("unrest_reduction",0).put("prerequisites","")
                 .put("skill_bonuses",new JSONObject().put("defense",1).put("industry",1)).put("description","Forja de ferragens, lâminas, armaduras e ferramentas.");
            }else if("market".equals(k)){
                s.put("name","Mercado").put("lots",2).put("level",1).put("rp",16).put("lumber",2).put("stone",0).put("ore",0).put("luxuries",0)
                 .put("residential_lots",0).put("item_bonus",1).put("unrest_reduction",0).put("prerequisites","")
                 .put("skill_bonuses",new JSONObject().put("trade",1).put("boating",1)).put("description","Praça mercantil aberta para tráfego comercial intenso.");
            }else if("temple".equals(k)){
                s.put("name","Templo").put("lots",2).put("level",7).put("rp",32).put("lumber",6).put("stone",6).put("ore",0).put("luxuries",0)
                 .put("construction_skill","folklore").put("construction_rank","trained").put("construction_dc",23).put("residential_lots",0).put("item_bonus",1).put("unrest_reduction",1).put("prerequisites","")
                 .put("skill_bonuses",new JSONObject().put("folklore",1).put("magic",1)).put("description","Casa sagrada de culto, assistência aos enfermos e união espiritual.");
            }else if("cathedral".equals(k)){
                s.put("name","Catedral").put("lots",4).put("level",15).put("rp",58).put("lumber",20).put("stone",20).put("ore",0).put("luxuries",0)
                 .put("construction_skill","folklore").put("construction_rank","master").put("construction_dc",34).put("residential_lots",0).put("item_bonus",2).put("unrest_reduction",2).put("prerequisites","temple;city")
                 .put("skill_bonuses",new JSONObject().put("folklore",2).put("magic",2)).put("description","Imponente santuário de arquitetura sagrada e poder teológico.");
            }else if("barracks".equals(k)){
                s.put("name","Quartel").put("lots",1).put("level",3).put("rp",6).put("lumber",2).put("stone",1).put("ore",0).put("luxuries",0)
                 .put("construction_skill","defense").put("construction_rank","untrained").put("construction_dc",16).put("residential_lots",0).put("item_bonus",1).put("unrest_reduction",0).put("prerequisites","")
                 .put("skill_bonuses",new JSONObject().put("defense",1).put("warfare",1)).put("description","Alojamento militar e campo de instrução para a guarnição.");
            }else if("wooden_palisade".equals(k)){
                s.put("name","Muralhas de Madeira").put("lots",0).put("level",1).put("rp",2).put("lumber",2).put("stone",0).put("ore",0).put("luxuries",0)
                 .put("residential_lots",0).put("item_bonus",1).put("unrest_reduction",0).put("prerequisites","")
                 .put("skill_bonuses",new JSONObject().put("defense",1)).put("description","Cerca de estacas pontiagudas protegendo o perímetro do assentamento.");
            }else if("stone_wall".equals(k)){
                s.put("name","Muralhas de Pedra").put("lots",0).put("level",3).put("rp",6).put("lumber",0).put("stone",4).put("ore",0).put("luxuries",0)
                 .put("residential_lots",0).put("item_bonus",2).put("unrest_reduction",0).put("prerequisites","town")
                 .put("skill_bonuses",new JSONObject().put("defense",2)).put("description","Muralhas reforçadas de cantaria com ameias e seteiras.");
            }else if("castle".equals(k)){
                s.put("name","Castelo").put("lots",4).put("level",9).put("rp",54).put("lumber",12).put("stone",12).put("ore",0).put("luxuries",0)
                 .put("construction_skills","defense,industry,magic,statecraft").put("construction_rank","expert").put("construction_dc",26).put("residential_lots",0).put("item_bonus",2).put("unrest_reduction",2).put("prerequisites","city")
                 .put("skill_bonuses",new JSONObject().put("defense",2).put("warfare",2).put("politics",2).put("statecraft",2))
                 .put("description","Fortaleza governamental suprema com torres e salão do trono.");
            }else if("brewery".equals(k)){
                s.put("name","Cervejaria").put("lots",1).put("level",1).put("rp",6).put("lumber",2).put("stone",0).put("ore",0).put("luxuries",0)
                 .put("construction_skill","agriculture").put("construction_rank","untrained").put("construction_dc",15).put("residential_lots",0).put("item_bonus",1).put("unrest_reduction",0).put("prerequisites","tavern")
                 .put("skill_bonuses",new JSONObject().put("trade",1).put("agriculture",1)).put("description","Produção de cervejas e bebidas fermentadas para abastecimento e exportação.");
            }else if("library".equals(k)){
                s.put("name","Biblioteca").put("lots",1).put("level",2).put("rp",6).put("lumber",4).put("stone",2).put("ore",0).put("luxuries",0)
                 .put("construction_skill","scholarship").put("construction_rank","trained").put("construction_dc",16).put("residential_lots",0).put("item_bonus",1).put("unrest_reduction",0).put("prerequisites","")
                 .put("skill_bonuses",new JSONObject().put("scholarship",1).put("magic",1)).put("description","Repositório de códices, mapas antigos e tratados acadêmicos.");
            }else if("academy".equals(k)){
                s.put("name","Academia").put("lots",2).put("level",10).put("rp",52).put("lumber",12).put("stone",12).put("ore",0).put("luxuries",6)
                 .put("construction_skill","scholarship").put("construction_rank","expert").put("construction_dc",27).put("residential_lots",0).put("item_bonus",2).put("unrest_reduction",0).put("prerequisites","library;town")
                 .put("skill_bonuses",new JSONObject().put("scholarship",2).put("magic",2).put("arts",1)).put("description","Centro de altos estudos de magia, ciência e filosofia.");
            }else if("bank".equals(k)){
                s.put("name","Banco").put("lots",1).put("level",5).put("rp",28).put("lumber",0).put("stone",6).put("ore",4).put("luxuries",0)
                 .put("construction_skill","trade").put("construction_rank","trained").put("construction_dc",20).put("residential_lots",0).put("item_bonus",1).put("unrest_reduction",0).put("prerequisites","town")
                 .put("skill_bonuses",new JSONObject().put("trade",1).put("industry",1)).put("description","Cofre-forte, custódia e fomento econômico de mercadores.");
            }else if("granary".equals(k)){
                s.put("name","Celeiro").put("lots",1).put("level",1).put("rp",12).put("lumber",2).put("stone",0).put("ore",0).put("luxuries",0)
                 .put("construction_skill","agriculture").put("construction_rank","untrained").put("construction_dc",15).put("residential_lots",0).put("item_bonus",1).put("unrest_reduction",0).put("prerequisites","")
                 .put("skill_bonuses",new JSONObject().put("agriculture",1)).put("description","Armazém de cereais e secos para segurança alimentar.");
            }else if("stables".equals(k)){
                s.put("name","Estábulos").put("lots",1).put("level",3).put("rp",10).put("lumber",2).put("stone",0).put("ore",0).put("luxuries",0)
                 .put("construction_skill","wilderness").put("construction_rank","trained").put("construction_dc",18).put("residential_lots",0).put("item_bonus",1).put("unrest_reduction",0).put("prerequisites","")
                 .put("skill_bonuses",new JSONObject().put("wilderness",1).put("warfare",1)).put("description","Cocheiras e pastos para montarias de guerra e caravanas.");
            }else if("town_hall".equals(k)){
                s.put("name","Prefeitura").put("lots",2).put("level",2).put("rp",22).put("lumber",4).put("stone",4).put("ore",0).put("luxuries",0)
                 .put("construction_skills","defense,industry,magic,statecraft").put("construction_rank","trained").put("construction_dc",16).put("residential_lots",0).put("item_bonus",1).put("unrest_reduction",0).put("prerequisites","town")
                 .put("skill_bonuses",new JSONObject().put("statecraft",1).put("politics",1)).put("description","Sede civil dos magistrados e cartório de registro público.");
            }else if("festival_ground".equals(k)){
                s.put("name","Praça de Festividades").put("lots",1).put("level",1).put("rp",4).put("lumber",0).put("stone",0).put("ore",0).put("luxuries",0)
                 .put("residential_lots",0).put("item_bonus",1).put("unrest_reduction",1).put("prerequisites","")
                 .put("skill_bonuses",new JSONObject().put("arts",1)).put("description","Área arborizada para danças, feiras livres e festejos sazonais.");
            }else if("garrison".equals(k)){
                s.put("name","Guarnição").put("lots",2).put("level",5).put("rp",28).put("lumber",6).put("stone",3).put("ore",0).put("luxuries",0)
                 .put("construction_skill","warfare").put("construction_rank","trained").put("construction_dc",20).put("residential_lots",0).put("item_bonus",2).put("unrest_reduction",0).put("prerequisites","barracks;town")
                 .put("skill_bonuses",new JSONObject().put("defense",2).put("warfare",1)).put("description","Reduto de armas pesadas, cavalaria e defensores experientes.");
            }else if("embassy".equals(k)){
                s.put("name","Embaixada").put("lots",2).put("level",8).put("rp",26).put("lumber",10).put("stone",4).put("ore",0).put("luxuries",6)
                 .put("construction_skill","politics").put("construction_rank","untrained").put("construction_dc",24).put("residential_lots",0).put("item_bonus",1).put("unrest_reduction",0).put("prerequisites","town")
                 .put("skill_bonuses",new JSONObject().put("politics",1).put("statecraft",1)).put("description","Representação oficial e residência de diplomatas estrangeiros.");
            }else if("theater".equals(k)){
                s.put("name","Teatro").put("lots",2).put("level",9).put("rp",24).put("lumber",8).put("stone",3).put("ore",0).put("luxuries",0)
                 .put("construction_skill","arts").put("construction_rank","expert").put("construction_dc",26).put("residential_lots",0).put("item_bonus",2).put("unrest_reduction",0).put("prerequisites","town")
                 .put("skill_bonuses",new JSONObject().put("arts",2).put("folklore",1)).put("description","Casa de óperas, encenações trágicas e poetas.");
            }else if("pier".equals(k)){
                s.put("name","Cais").put("lots",1).put("level",3).put("rp",16).put("lumber",2).put("stone",0).put("ore",0).put("luxuries",0)
                 .put("construction_skill","boating").put("construction_rank","untrained").put("construction_dc",18).put("residential_lots",0).put("item_bonus",2).put("unrest_reduction",0).put("prerequisites","")
                 .put("skill_bonuses",new JSONObject().put("boating",2).put("trade",1)).put("description","Doca para desembarque de cargas fluviais e costeiras.");
            }else if("alchemist".equals(k)){
                s.put("name","Laboratório Alquímico").put("lots",1).put("level",3).put("rp",18).put("lumber",0).put("stone",5).put("ore",2).put("luxuries",0)
                 .put("construction_skill","industry").put("construction_rank","trained").put("construction_dc",16).put("residential_lots",0).put("item_bonus",1).put("unrest_reduction",0).put("prerequisites","")
                 .put("skill_bonuses",new JSONObject().put("magic",1).put("industry",1)).put("description","Oficina de destilação de elixires, poções e substâncias voláteis.");
            }else if("graveyard".equals(k)){
                s.put("name","Cemitério").put("lots",1).put("level",1).put("rp",4).put("lumber",0).put("stone",1).put("ore",0).put("luxuries",0)
                 .put("residential_lots",0).put("item_bonus",1).put("unrest_reduction",0).put("prerequisites","")
                 .put("skill_bonuses",new JSONObject().put("folklore",1)).put("description","Campos de repouso consagrado e lápides memoriais.");
            }else if("monument".equals(k)){
                s.put("name","Monumento").put("lots",1).put("level",3).put("rp",6).put("lumber",0).put("stone",1).put("ore",0).put("luxuries",0)
                 .put("construction_skill","arts").put("construction_rank","trained").put("construction_dc",18).put("residential_lots",0).put("item_bonus",1).put("unrest_reduction",1).put("prerequisites","")
                 .put("skill_bonuses",new JSONObject().put("politics",1)).put("description","Obelisco ou estátua régia comemorando heróis e virtudes da nação.");
            }else{
                return null;
            }
        }catch(Exception ignored){}
        return s;
    }

    // =========================================================================
    // Catálogo Canônico de Eventos do Reino (Kingmaker PF2e)
    // =========================================================================
    public JSONArray eventsCatalog(){
        JSONArray arr=new JSONArray();
        String[] eventKeys={
            "good_harvest","mineral_discovery","trade_fair","visiting_scholar","visiting_dignitaries",
            "new_settlers","patriotic_inspiration","wandering_monsters","bandit_activity","crop_blight",
            "epidemic","political_unrest","building_collapse","court_scandal","settlement_fire","natural_disaster"
        };
        for(String key:eventKeys){
            JSONObject ev=event(key);
            if(ev!=null)arr.put(ev);
        }
        return arr;
    }

    public JSONObject event(String key){
        String k=key(key);
        JSONObject e=new JSONObject();
        try{
            e.put("key",k);
            if("good_harvest".equals(k)){
                e.put("title","Boa Colheita").put("event_type","beneficial").put("skill","agriculture").put("ability","stability")
                 .put("description","As condições climáticas propiciaram safras abundantes nos campos do Reino.");
            }else if("mineral_discovery".equals(k)){
                e.put("title","Descoberta Mineral").put("event_type","beneficial").put("skill","industry").put("ability","economy")
                 .put("description","Mineiros e garimpeiros encontraram veios ricos de minério ou pedra de alta qualidade.");
            }else if("trade_fair".equals(k)){
                e.put("title","Feira Mercantil").put("event_type","beneficial").put("skill","trade").put("ability","economy")
                 .put("description","Caravanas estrangeiras e mercadores locais afluem ao assentamento com mercadorias finas.");
            }else if("visiting_scholar".equals(k)){
                e.put("title","Visita de Erudito").put("event_type","beneficial").put("skill","scholarship").put("ability","culture")
                 .put("description","Um filósofo ou arcanista renomado oferece palestras e ensinamentos nas academias.");
            }else if("visiting_dignitaries".equals(k)){
                e.put("title","Visita de Dignitários").put("event_type","beneficial").put("skill","politics").put("ability","loyalty")
                 .put("description","Representantes de cortes vizinhas chegam para negociar acordos de amizade.");
            }else if("new_settlers".equals(k)){
                e.put("title","Novos Colonos").put("event_type","beneficial").put("skill","statecraft").put("ability","loyalty")
                 .put("description","Famílias de pioneiros e artífices chegam buscando oportunidades em suas terras.");
            }else if("patriotic_inspiration".equals(k)){
                e.put("title","Inspiração Patriótica").put("event_type","beneficial").put("skill","arts").put("ability","culture")
                 .put("description","Uma canção, poema ou monumento heroico inflama o orgulho nacional do povo.");
            }else if("wandering_monsters".equals(k)){
                e.put("title","Monstros Errantes").put("event_type","harmful").put("skill","defense").put("ability","stability")
                 .put("description","Feras ou predadores monstruosos rondam as estradas e ameaçam assentamentos.");
            }else if("bandit_activity".equals(k)){
                e.put("title","Bandidos nos Caminhos").put("event_type","harmful").put("skill","intrigue").put("ability","loyalty")
                 .put("description","Bandos de salteadores assaltam caravanas mercantis e cobram pedágios ilegais.");
            }else if("crop_blight".equals(k)){
                e.put("title","Praga nas Lavouras").put("event_type","harmful").put("skill","agriculture").put("ability","stability")
                 .put("description","Fungos e insetos vorazes ameaçam devastar os campos produtivos do território.");
            }else if("epidemic".equals(k)){
                e.put("title","Epidemia").put("event_type","harmful").put("skill","wilderness").put("ability","stability")
                 .put("description","Uma febre contagiosa atinge moradores nos assentamentos mais densos.");
            }else if("political_unrest".equals(k)){
                e.put("title","Agitação Política").put("event_type","harmful").put("skill","politics").put("ability","loyalty")
                 .put("description","Facções e oradores inflamatórios espalham discórdia e desafiam as leis do Reino.");
            }else if("building_collapse".equals(k)){
                e.put("title","Falha Estrutural no Assentamento").put("event_type","harmful").put("skill","engineering").put("ability","stability")
                 .put("description","Estruturas antigas ou mal planejadas dão sinais graves de trincas e risco de colapso.");
            }else if("court_scandal".equals(k)){
                e.put("title","Escândalo na Corte").put("event_type","harmful").put("skill","intrigue").put("ability","loyalty")
                 .put("description","Fofocas venenosas e acusações de favorecimento atingem a reputação dos líderes.");
            }else if("settlement_fire".equals(k)){
                e.put("title","Incêndio no Assentamento").put("event_type","harmful").put("skill","defense").put("ability","stability")
                 .put("description","Fogo acidental irrompe em oficinas e ameaça quarteirões vizinhos.");
            }else if("natural_disaster".equals(k)){
                e.put("title","Inundação ou Tempestade").put("event_type","harmful").put("skill","engineering").put("ability","stability")
                 .put("description","Chuvas torrenciais transbordam rios e causam estragos em pontes e estradas.");
            }else{
                return null;
            }
        }catch(Exception ignored){}
        return e;
    }

    public String authority(){return "Kingmaker PF2e: turno de Reino em Upkeep, Commerce, Activity e Event; perícias do Reino usam quatro graus PF2e; posse de hexes, assentamentos, estruturas, recursos, Unrest/Ruin, liderança, exércitos, influência de companheiros e acampamento são persistidos e resolvidos localmente; o Atlas oficial é somente leitura; a IA apenas narra ou propõe.";}

    public static String abilityLabel(String ability){String k=key(ability);if("culture".equals(k))return "Cultura";if("economy".equals(k))return "Economia";if("loyalty".equals(k))return "Lealdade";return "Estabilidade";}
    public static String skillLabel(String skill){String k=key(skill);if("agriculture".equals(k))return "Agricultura";if("arts".equals(k))return "Artes";if("boating".equals(k))return "Navegação";if("defense".equals(k))return "Defesa";if("engineering".equals(k))return "Engenharia";if("exploration".equals(k))return "Exploração";if("folklore".equals(k))return "Folclore";if("industry".equals(k))return "Indústria";if("intrigue".equals(k))return "Intriga";if("magic".equals(k))return "Magia";if("politics".equals(k))return "Política";if("scholarship".equals(k))return "Erudição";if("statecraft".equals(k))return "Administração";if("trade".equals(k))return "Comércio";if("warfare".equals(k))return "Guerra";if("wilderness".equals(k))return "Ermos";return skill==null?"":skill;}
    private static String key(String value){if(value==null)return "";return Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-z0-9]+","_").replaceAll("^_+|_+$","");}
}
