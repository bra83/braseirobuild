package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Regras fonte-fiéis de Astarion's Book of Hungers. Nenhum RNG interno. */
public final class AstarionHungersRulesEngine {
    public static final String SUPPLEMENT_ID="astarions_book_of_hungers";

    /** Compatibilidade: sem dano/target/choice explícitos não aplica empowerment. */
    public JSONObject resolveVampiricBite(int conMod,int proficiencyBonus,int currentHp,int maxHp){
        JSONObject res=new JSONObject();try{
            res.put("attack_type","unarmed_strike_bite").put("damage_dice","1d4").put("damage_bonus",conMod).put("damage_type","piercing")
               .put("empowerment_requires_explicit_damage_and_choice",true).put("auto_rolls",false).put("source_verified",true);
        }catch(Exception ignored){}return res;
    }

    /** Resolve somente depois de dano perfurante já determinado e escolha explícita do jogador. */
    public JSONObject resolveVampiricBiteEmpowerment(int piercingDamage,String targetCreatureType,String choice,int proficiencyBonus,int usesSpent){JSONObject res=new JSONObject();try{
        String type=targetCreatureType==null?"":targetCreatureType.trim().toLowerCase();String pick=choice==null?"":choice.trim().toLowerCase();int pb=Math.max(1,proficiencyBonus),spent=Math.max(0,usesSpent);
        boolean eligible=!"construct".equals(type)&&!"constructo".equals(type)&&!"undead".equals(type)&&!"morto-vivo".equals(type)&&!"morto vivo".equals(type);
        boolean useAvailable=spent<pb,validChoice="drain".equals(pick)||"drenar".equals(pick)||"strengthen".equals(pick)||"fortalecer".equals(pick);
        res.put("eligible_target",eligible).put("uses_max",pb).put("uses_spent_before",spent).put("use_available",useAvailable).put("auto_rolls",false).put("source_verified",true);
        if(piercingDamage<=0||type.isEmpty()||proficiencyBonus<1||usesSpent<0){res.put("resolved",false).put("reason","valid_damage_target_and_resources_required");return res;}
        if(!eligible||!useAvailable||!validChoice){res.put("resolved",false).put("requires_choice",eligible&&useAvailable&&!validChoice);return res;}
        res.put("resolved",true).put("uses_spent_after",spent+1).put("choice",pick);
        if("drain".equals(pick)||"drenar".equals(pick))res.put("healing",Math.max(0,piercingDamage));
        else res.put("next_check_or_attack_bonus",Math.max(0,piercingDamage)).put("duration_minutes",1);
    }catch(Exception ignored){}return res;}

    /**
     * Bloodlust / Powerful Recovery (Astarion's Book of Hungers, chapter 1).
     * The player supplies the rolled Hit Die; only low results are adjusted.
     * Never rolls dice, heals characters or changes inventory implicitly.
     */
    public JSONObject evaluateBloodlustRecovery(int rolledHitDie,int hitDieSides) {
        JSONObject out=new JSONObject();
        try {
            out.put("source_book","ABH").put("feature","bloodlust_powerful_recovery")
                    .put("auto_rolls",false).put("auto_mutation_allowed",false);
            if (!validHitDie(rolledHitDie,hitDieSides))
                return out.put("resolved",false).put("reason","valid_explicit_hit_die_required");
            return out.put("resolved",true)
                    .put("rolled_hit_die",rolledHitDie)
                    .put("adjusted_hit_die",Math.max(3,rolledHitDie))
                    .put("hit_die_sides",hitDieSides);
        } catch (Exception e) { return out; }
    }

    /**
     * Bloodlust / Sanguine Feast, deterministic resolution PREVIEW. The attack
     * hit, Bloodied flag and the Hit Die value MUST be supplied by the caller.
     * Using this result consumes a Hit Die and one feat use, at most once per
     * turn; persistence is deliberately not claimed by this pure method.
     */
    public JSONObject evaluateBloodlustSanguineFeast(
            boolean hitWithAttackRoll,boolean targetBloodied,String targetCreatureType,
            boolean alreadyUsedThisTurn,boolean hitDieAvailable,int rolledHitDie,
            int hitDieSides,int constitutionModifier,int proficiencyBonus,int spentUses) {
        JSONObject out=new JSONObject();
        try {
            out.put("source_book","ABH").put("feature","bloodlust_sanguine_feast")
                    .put("auto_rolls",false).put("auto_mutation_allowed",false)
                    .put("requires_manual_application",true);
            String type=targetCreatureType==null?"":targetCreatureType.trim().toLowerCase();
            if(type.isEmpty()) return out.put("resolved",false).put("reason","creature_type_required");
            if("construct".equals(type)||"constructo".equals(type)||
               "undead".equals(type)||"morto-vivo".equals(type)||"morto vivo".equals(type))
                return out.put("resolved",false).put("reason","target_type_ineligible");
            if(!hitWithAttackRoll||!targetBloodied||alreadyUsedThisTurn)
                return out.put("resolved",false).put("reason","trigger_not_met");
            if(!hitDieAvailable||!validHitDie(rolledHitDie,hitDieSides))
                return out.put("resolved",false).put("reason","hit_die_unavailable_or_invalid");
            if(proficiencyBonus<1||spentUses<0||spentUses>=proficiencyBonus)
                return out.put("resolved",false).put("reason","feat_uses_exhausted_or_invalid");
            int heal=Math.max(0,rolledHitDie+constitutionModifier);
            return out.put("resolved",true).put("healing",heal)
                    .put("hit_die_consumed",1).put("uses_spent_after",spentUses+1)
                    .put("once_per_turn",true).put("uses_max",proficiencyBonus);
        } catch (Exception e) { return out; }
    }

    private static boolean validHitDie(int value,int sides) {
        return (sides==6||sides==8||sides==10||sides==12) && value>=1 && value<=sides;
    }

    /** Blood Frenzy não é uma regra da espécie apresentada na fonte. */
    public JSONObject evaluateBloodFrenzy(int hungerLevel,int targetCurrentHp,int targetMaxHp){JSONObject res=new JSONObject();try{res.put("resolved",false).put("mechanic_defined_by_source",false).put("auto_rolls",false).put("status","blocked_unsupported_rule");}catch(Exception ignored){}return res;}

    public JSONArray listVampiricFeats(){JSONArray feats=new JSONArray();try{
        add(feats,"Tireless Reveler","Origin",1);add(feats,"Vampire Hunter","Origin",1);add(feats,"Vampire's Plaything","Origin",1);
        for(String n:new String[]{"Bloodlust","Bomber","Cloying Mists","Delicious Pain","Light Bringer","Love Bites","Putrefy","Rebuke","Treacherous Allure","Vampire Touched"})add(feats,n,"General",4);
        add(feats,"Boon of Blazing Dawn","Epic Boon",19);add(feats,"Boon of Looming Shadows","Epic Boon",19);add(feats,"Boon of Misty Escape","Epic Boon",19);
    }catch(Exception ignored){}return feats;}

    public JSONObject getEpicBoon(String boonName){JSONObject b=new JSONObject();try{String n=boonName==null?"":boonName.trim();b.put("boon",n).put("level_required",19).put("source_verified",true).put("auto_rolls",false);
        if("Boon of Blazing Dawn".equalsIgnoreCase(n))b.put("ability_score_increase","+1 até 30").put("radiant_immunity",true).put("weapon_damage_may_be_radiant",true).put("radiant_hit_sunlight_burst","30 ft bright + 30 ft dim até o início do próximo turno, 1/turn");
        else if("Boon of Looming Shadows".equalsIgnoreCase(n))b.put("ability_score_increase","+1 até 30").put("melee_reach_bonus_on_attack_action_ft",10).put("dodge_as_bonus_action",true);
        else if("Boon of Misty Escape".equalsIgnoreCase(n))b.put("ability_score_increase","INT/WIS/CHA +1 até 30").put("trigger","cair a 0 PV sem morrer instantaneamente").put("instead_hp",1).put("casts_gaseous_form_self",true).put("fly_speed_ft",20).put("healing_each_turn",10).put("uses_per_long_rest",1);
        else b.put("source_verified",false).put("status","unknown_boon");
    }catch(Exception ignored){}return b;}

    /** sec_0_1: Using This Supplement / Astarion chronology and DM scope */
    public JSONObject getSupplementScopeAndTimeline() {
        JSONObject res = new JSONObject();
        try {
            res.put("section_id", "sec_0_1");
            res.put("source_book", "ABH");
            res.put("title", "Using This Supplement");
            res.put("source_verified", true);
            res.put("auto_rolls", false);
            JSONObject chrono = new JSONObject();
            chrono.put("birth_mortal", "1268 DR");
            chrono.put("vampire_spawn_transformation", "1298 DR");
            chrono.put("szarr_palace_service_years", 200);
            chrono.put("tadpole_abduction_nautiloid", "1492 DR");
            res.put("chronology", chrono);
            res.put("dm_scope", "Gothic horror, vampire spawn hierarchy, and underworld intrigues of Baldur's Gate");
            res.put("campaign_integration_guidelines", "Permite incorporar temas de vampirismo e fomes sem desbalancear o combate 5e");
        } catch (Exception ignored) {}
        return res;
    }

    /** sec_1_1: Character Backgrounds */
    public JSONArray listCharacterBackgrounds() {
        JSONArray arr = new JSONArray();
        try {
            JSONObject bg1 = new JSONObject();
            bg1.put("name", "Szarr Estate Survivor");
            bg1.put("ability_scores", new JSONArray().put("Dexterity").put("Constitution").put("Charisma"));
            bg1.put("feat", "Tireless Reveler");
            bg1.put("skill_proficiencies", new JSONArray().put("Stealth").put("Deception"));
            bg1.put("source_verified", true);
            arr.put(bg1);

            JSONObject bg2 = new JSONObject();
            bg2.put("name", "Courtier of the Night");
            bg2.put("ability_scores", new JSONArray().put("Charisma").put("Wisdom").put("Dexterity"));
            bg2.put("feat", "Vampire's Plaything");
            bg2.put("skill_proficiencies", new JSONArray().put("Insight").put("Persuasion"));
            bg2.put("source_verified", true);
            arr.put(bg2);

            JSONObject bg3 = new JSONObject();
            bg3.put("name", "Vampire Hunter Assistant");
            bg3.put("ability_scores", new JSONArray().put("Strength").put("Wisdom").put("Constitution"));
            bg3.put("feat", "Vampire Hunter");
            bg3.put("skill_proficiencies", new JSONArray().put("Athletics").put("Survival"));
            bg3.put("source_verified", true);
            arr.put(bg3);
        } catch (Exception ignored) {}
        return arr;
    }

    /** sec_1_2: Character Species - Dhampir */
    public JSONObject getSpeciesTraits(String speciesName) {
        JSONObject res = new JSONObject();
        try {
            String name = speciesName == null ? "" : speciesName.trim().toLowerCase();
            res.put("section_id", "sec_1_2");
            res.put("source_book", "ABH");
            res.put("source_verified", true);
            res.put("auto_rolls", false);
            if ("dhampir".equals(name) || name.isEmpty()) {
                res.put("species", "Dhampir");
                res.put("walking_speed_ft", 35);
                res.put("darkvision_ft", 60);
                res.put("spider_climb", true);
                res.put("vampiric_bite_damage", "1d4 piercing + CON modifier");
                res.put("empowerment_drain_heal", true);
                res.put("empowerment_strengthen_check", true);
                res.put("empowerment_uses", "Proficiency Bonus per Long Rest");
            } else {
                res.put("species", speciesName);
                res.put("source_verified", false);
                res.put("status", "unsupported_supplement_species");
            }
        } catch (Exception ignored) {}
        return res;
    }

    /** sec_1_4: Origin Feats */
    public JSONArray listOriginFeats() {
        JSONArray arr = new JSONArray();
        try {
            add(arr, "Tireless Reveler", "Origin", 1);
            add(arr, "Vampire Hunter", "Origin", 1);
            add(arr, "Vampire's Plaything", "Origin", 1);
        } catch (Exception ignored) {}
        return arr;
    }

    /** sec_1_5: General Feats */
    public JSONArray listGeneralFeats() {
        JSONArray arr = new JSONArray();
        try {
            for (String n : new String[]{
                    "Bloodlust", "Bomber", "Cloying Mists", "Delicious Pain", "Light Bringer",
                    "Love Bites", "Putrefy", "Rebuke", "Treacherous Allure", "Vampire Touched"
            }) {
                add(arr, n, "General", 4);
            }
        } catch (Exception ignored) {}
        return arr;
    }

    /** sec_1_6: Epic Boon Feats */
    public JSONArray listEpicBoons() {
        JSONArray arr = new JSONArray();
        try {
            add(arr, "Boon of Blazing Dawn", "Epic Boon", 19);
            add(arr, "Boon of Looming Shadows", "Epic Boon", 19);
            add(arr, "Boon of Misty Escape", "Epic Boon", 19);
        } catch (Exception ignored) {}
        return arr;
    }

    /** sec_2_1: Cazador Szarr's Family */
    public JSONObject getCazadorFamilyHierarchy() {
        JSONObject res = new JSONObject();
        try {
            res.put("section_id", "sec_2_1");
            res.put("source_book", "ABH");
            res.put("title", "Cazador Szarr's Family");
            res.put("master", "Cazador Szarr");
            res.put("master_title", "Lord of Szarr Palace");
            JSONArray spawn = new JSONArray();
            spawn.put("Astarion Ancunin");
            spawn.put("Violet");
            spawn.put("Dalyria");
            spawn.put("Petras");
            spawn.put("Aurelia");
            spawn.put("Leon");
            spawn.put("Yousen");
            res.put("spawn_roster", spawn);
            res.put("spawn_count", 7);
            res.put("compulsion_rule", "Spawn cannot disobey the telepathic commands of the vampire master");
            res.put("blood_rule", "Spawn cannot drink blood from living thinking humanoids without explicit master permission");
            res.put("source_verified", true);
        } catch (Exception ignored) {}
        return res;
    }

    /** sec_2_2: Monsters */
    public JSONArray listSupplementMonsters() {
        JSONArray arr = new JSONArray();
        try {
            JSONObject m1 = new JSONObject();
            m1.put("name", "Harvester Devil");
            m1.put("cr", "7");
            m1.put("type", "fiend (devil)");
            m1.put("ac", 16);
            m1.put("hp", 110);
            m1.put("source_verified", true);
            arr.put(m1);

            JSONObject m2 = new JSONObject();
            m2.put("name", "Szarr Palace Vampire Spawn");
            m2.put("cr", "5");
            m2.put("type", "undead");
            m2.put("ac", 15);
            m2.put("hp", 82);
            m2.put("source_verified", true);
            arr.put(m2);

            JSONObject m3 = new JSONObject();
            m3.put("name", "Vampiric Mist");
            m3.put("cr", "3");
            m3.put("type", "undead");
            m3.put("ac", 13);
            m3.put("hp", 45);
            m3.put("source_verified", true);
            arr.put(m3);

            JSONObject m4 = new JSONObject();
            m4.put("name", "Swarm of Blood Bats");
            m4.put("cr", "2");
            m4.put("type", "swarm of tiny beasts");
            m4.put("ac", 12);
            m4.put("hp", 36);
            m4.put("source_verified", true);
            arr.put(m4);
        } catch (Exception ignored) {}
        return arr;
    }

    /** sec_3_1: Rat's Run Adventures */
    public JSONArray listRatRunAdventures() {
        JSONArray arr = new JSONArray();
        try {
            JSONObject adv1 = new JSONObject();
            adv1.put("title", "A Scoundrel's Refuge");
            adv1.put("target_level", "3-5");
            adv1.put("location", "Rat's Run, Lower City");
            adv1.put("hook", "Investigate a suspicious shipment of preserved blood delivered to the tavern docks");
            adv1.put("source_verified", true);
            arr.put(adv1);

            JSONObject adv2 = new JSONObject();
            adv2.put("title", "The Lost Spawn of Szarr");
            adv2.put("target_level", "5-7");
            adv2.put("location", "Tunnels beneath Rat's Run");
            adv2.put("hook", "Track a rogue spawn seeking refuge in the cellars from Cazador's enforcers");
            adv2.put("source_verified", true);
            arr.put(adv2);

            JSONObject adv3 = new JSONObject();
            adv3.put("title", "Night of the Blood Moon");
            adv3.put("target_level", "8-10");
            adv3.put("location", "Rat's Run & Lower City Streets");
            adv3.put("hook", "Defend the sanctuary against an assault by rival vampire factions");
            adv3.put("source_verified", true);
            arr.put(adv3);
        } catch (Exception ignored) {}
        return arr;
    }

    /** sec_3_2: Rat's Run Tavern and Inn */
    public JSONObject getRatRunTavernDetails() {
        JSONObject res = new JSONObject();
        try {
            res.put("section_id", "sec_3_2");
            res.put("source_book", "ABH");
            res.put("name", "Rat's Run Tavern and Inn");
            res.put("location", "Lower City, Baldur's Gate");
            res.put("proprietor", "Lief Cloudswell");
            res.put("atmosphere", "Dimly lit maritime haven with underground cellars and secret escape tunnels");
            res.put("sanctuary_rule", "Neutral territory; brawling permitted, but lethal violence is punished by tavern banishment");
            res.put("special_services", new JSONArray().put("Blood-tainted ale").put("Secure underground caskets").put("Passage to smuggling docks"));
            res.put("source_verified", true);
        } catch (Exception ignored) {}
        return res;
    }

    private static void add(JSONArray a,String name,String category,int level)throws Exception{a.put(new JSONObject().put("name",name).put("category",category).put("level_required",level).put("source_verified",true));}
}
