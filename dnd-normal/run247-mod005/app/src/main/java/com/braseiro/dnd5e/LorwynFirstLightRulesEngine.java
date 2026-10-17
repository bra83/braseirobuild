package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/**
 * Lorwyn: First Light (LFL) — runtime compilado, source-bound e fail-closed.
 *
 * Fontes estruturadas revisadas:
 * - book-lfl.json blob 34899081b3352ebcd1cc8201c59d37dfe0c0c8c7
 * - races.json blob 2355cf58a87d4dc15aaaf9232f8cb3330d93ae95
 * - backgrounds.json blob e2dacedcf21fee9481fb3e760fc4c97a52cc8dc9
 * - feats.json blob 4dadd905268e42be9c450edebd86026162fdec10
 * - bestiary-lfl.json blob 8f13baea417997bd64c0d2838c178bedc1ab0777
 *
 * Nenhum RAG é consultado em gameplay. Nenhuma rolagem é automática.
 */
public final class LorwynFirstLightRulesEngine {
    public static final String SUPPLEMENT_ID="lfl";
    public static final String SOURCE="Lorwyn: First Light";
    public static final String SOURCE_CODE="LFL";
    public static final String BOOK_BLOB="34899081b3352ebcd1cc8201c59d37dfe0c0c8c7";
    public static final String RACES_BLOB="2355cf58a87d4dc15aaaf9232f8cb3330d93ae95";
    public static final String BACKGROUNDS_BLOB="e2dacedcf21fee9481fb3e760fc4c97a52cc8dc9";
    public static final String FEATS_BLOB="4dadd905268e42be9c450edebd86026162fdec10";
    public static final String BESTIARY_BLOB="8f13baea417997bd64c0d2838c178bedc1ab0777";

    private static final String[][] BACKGROUNDS={
        {"Lorwyn Expert","Especialista de Lorwyn"},{"Shadowmoor Expert","Especialista de Shadowmoor"}
    };
    private static final String[][] FEATS={
        {"Child of the Sun","Filho do Sol"},{"Shadowmoor Hexer","Hexer de Shadowmoor"}
    };
    private static final String[][] SPECIES={
        {"Boggart","Boggart"},{"Elf","Elfo de Lorwyn-Shadowmoor"},{"Faerie","Faerie"},{"Flamekin","Flamekin"},
        {"Kithkin","Kithkin"},{"Lorwyn Changeling","Changeling de Lorwyn"},{"Rimekin","Rimekin"}
    };
    private static final String[][] ITEMS={
        {"Reliquary of Dawn","Relicário da Aurora"},{"Reliquary of Twilight","Relicário do Crepúsculo"}
    };

    public JSONObject descriptor(){JSONObject o=new JSONObject();try{return o
        .put("id",SUPPLEMENT_ID).put("source",SOURCE).put("source_code",SOURCE_CODE).put("language","pt-BR")
        .put("auto_rolls",false).put("rag_required",false).put("runtime_external_dependency",false)
        .put("mechanics_authority","compiled_source_rules")
        .put("background_count",BACKGROUNDS.length).put("feat_count",FEATS.length).put("species_count",SPECIES.length)
        .put("reliquary_count",ITEMS.length).put("bestiary_count",8).put("sample_adventure_count",2)
        .put("book_blob",BOOK_BLOB).put("races_blob",RACES_BLOB).put("backgrounds_blob",BACKGROUNDS_BLOB)
        .put("feats_blob",FEATS_BLOB).put("bestiary_blob",BESTIARY_BLOB);
    }catch(Exception e){return o;}}

    public JSONArray listBackgrounds(){JSONArray a=new JSONArray();for(String[] x:BACKGROUNDS)a.put(background(x[0]));return a;}
    public JSONArray listFeats(){JSONArray a=new JSONArray();for(String[] x:FEATS)a.put(feat(x[0]));return a;}
    public JSONArray listSpecies(){JSONArray a=new JSONArray();for(String[] x:SPECIES)a.put(species(x[0]));return a;}
    public JSONArray listItems(){JSONArray a=new JSONArray();for(String[] x:ITEMS)a.put(item(x[0]));return a;}

    public JSONObject background(String name){String n=canon(BACKGROUNDS,name);JSONObject o=base("background",n);try{
        if(n.isEmpty())return unknown(o,"background_unknown");
        if("Lorwyn Expert".equals(n))return o.put("label_ptbr","Especialista de Lorwyn")
            .put("ability_pool","STR|CON|WIS").put("ability_assignment","+2/+1 or +1/+1/+1; manual choice")
            .put("origin_feat","Child of the Sun").put("skills","Athletics|Nature").put("tool","Cartographer's Tools")
            .put("equipment_option_a","Quarterstaff|Cartographer's Tools|Backpack|Basket|Parchment x4|Rope|Traveler's Clothes|29 GP")
            .put("equipment_option_b","50 GP");
        if("Shadowmoor Expert".equals(n))return o.put("label_ptbr","Especialista de Shadowmoor")
            .put("ability_pool","DEX|INT|CHA").put("ability_assignment","+2/+1 or +1/+1/+1; manual choice")
            .put("origin_feat","Shadowmoor Hexer").put("skills","Acrobatics|Deception").put("tool","Glassblower's Tools")
            .put("equipment_option_a","Dagger|Glassblower's Tools|Backpack|Hunting Trap|Lamp|Oil x3|Traveler's Clothes|Waterskin|8 GP")
            .put("equipment_option_b","50 GP");
    }catch(Exception ignored){}return o;}

    public JSONObject feat(String name){String n=canon(FEATS,name);JSONObject o=base("feat",n);try{
        if(n.isEmpty())return unknown(o,"feat_unknown");
        if("Child of the Sun".equals(n))return o.put("label_ptbr","Filho do Sol").put("category","Origin")
            .put("eyes_of_eirdu_radius_ft",10).put("eyes_of_eirdu_effect","advantage on saves to avoid or end Blinded")
            .put("spell","Faerie Fire").put("spellcasting_ability_choice","INT|WIS|CHA")
            .put("free_casts_per_long_rest",1).put("free_cast_damage_cannot_break_concentration",true);
        if("Shadowmoor Hexer".equals(n))return o.put("label_ptbr","Hexer de Shadowmoor").put("category","Origin")
            .put("spell","Hex").put("spell_always_prepared",true).put("spellcasting_ability_choice","INT|WIS|CHA")
            .put("free_casts_per_long_rest",1).put("curse_magic","hexed creature that hits you takes psychic damage = PB; once per turn");
    }catch(Exception ignored){}return o;}

    public JSONObject species(String name){String n=canon(SPECIES,name);JSONObject o=base("species",n);try{
        if(n.isEmpty())return unknown(o,"species_unknown");
        if("Boggart".equals(n))return o.put("label_ptbr","Boggart").put("base_species","Goblin|MPMM")
            .put("creature_type","Humanoid; goblinoid for prerequisites/effects").put("size","Small").put("walk_ft",30).put("darkvision_ft",60)
            .put("fey_ancestry","advantage on saves to avoid/end Charmed")
            .put("fury_of_small","extra damage = PB vs larger target; PB uses/Long Rest; max once/turn")
            .put("nimble_escape","Disengage or Hide as Bonus Action").put("base_mechanics_compiled",true);
        if("Elf".equals(n))return o.put("label_ptbr","Elfo de Lorwyn-Shadowmoor").put("base_species","Elf|XPHB")
            .put("creature_type","Humanoid").put("size","Medium").put("walk_ft",30).put("base_darkvision_ft",60)
            .put("fey_ancestry","advantage on saves to avoid/end Charmed").put("keen_senses_choice","Insight|Perception|Survival")
            .put("trance_long_rest_hours",4).put("magic_sleep_immune",true)
            .put("lineages",new JSONArray().put(new JSONObject().put("name","Lorwyn").put("level1","Thorn Whip or another Druid cantrip after each Long Rest").put("level3","Command 1/Long Rest free").put("level5","Silence 1/Long Rest free").put("ability","INT|WIS|CHA"))
                .put(new JSONObject().put("name","Shadowmoor").put("darkvision_ft",120).put("level1","Starry Wisp").put("level3","Heroism 1/Long Rest free").put("level5","Gentle Repose 1/Long Rest free").put("ability","INT|WIS|CHA")))
            .put("base_mechanics_compiled",true);
        if("Faerie".equals(n))return o.put("label_ptbr","Faerie").put("base_species","Fairy|MPMM").put("creature_type","Fey")
            .put("size","Small").put("walk_ft",30).put("fly_ft",30).put("flight_armor_limit","no medium/heavy armor")
            .put("base_spells","Druidcraft; Faerie Fire at level 3; Enlarge/Reduce at level 5")
            .put("lineage_lorwyn","no additional trait").put("lineage_shadowmoor_darkvision_ft",120).put("base_mechanics_compiled",true);
        if("Flamekin".equals(n))return o.put("label_ptbr","Flamekin").put("base_species","Genasi|MPMM")
            .put("size","Small|Medium").put("walk_ft",30).put("darkvision_ft",60).put("fire_resistance",true)
            .put("spells","Produce Flame; Burning Hands at level 3; Flame Blade at level 5")
            .put("free_casts","Burning Hands and Flame Blade each 1/Long Rest").put("flame_blade_material_component_required",false)
            .put("spellcasting_ability_choice","INT|WIS|CHA").put("base_mechanics_compiled",true);
        if("Kithkin".equals(n))return o.put("label_ptbr","Kithkin").put("base_species","Halfling|XPHB").put("creature_type","Humanoid")
            .put("size","Small").put("walk_ft",30).put("brave","advantage on saves to avoid/end Frightened")
            .put("halfling_nimbleness",true).put("luck","reroll natural 1 on D20 Test; must use new roll").put("naturally_stealthy",true)
            .put("lineage_lorwyn","no additional trait").put("lineage_shadowmoor_darkvision_ft",120).put("base_mechanics_compiled",true);
        if("Lorwyn Changeling".equals(n))return o.put("label_ptbr","Changeling de Lorwyn").put("creature_type","Fey")
            .put("size","Small|Medium").put("walk_ft",30).put("darkvision_ft",120).put("skill_choice","Deception|Performance")
            .put("shape_self","Action: two-legged Humanoid shape or four-legged Beast shape; Humanoid shape can wear Humanoid clothing/armor of size")
            .put("unpredictable_movement","on Initiative, if no Disadvantage, immediately move up to half Speed");
        if("Rimekin".equals(n))return o.put("label_ptbr","Rimekin").put("creature_type","Humanoid")
            .put("size","Small|Medium").put("walk_ft",30).put("darkvision_ft",60).put("cold_resistance",true)
            .put("spells","Ray of Frost; Ice Knife at level 3; Flame Blade at level 5")
            .put("free_casts","Ice Knife and Flame Blade each 1/Long Rest").put("flame_blade_damage_override","Cold")
            .put("spellcasting_ability_choice","INT|WIS|CHA");
    }catch(Exception ignored){}return o;}

    public JSONObject item(String name){String n=canon(ITEMS,name);JSONObject o=base("item",n);try{
        if(n.isEmpty())return unknown(o,"item_unknown");
        o.put("rarity","Uncommon").put("type","Wondrous Item").put("worn_required",true);
        if("Reliquary of Dawn".equals(n))return o.put("label_ptbr","Relicário da Aurora").put("immune_to","Eventide").put("eclipsed_realm_protection",true);
        if("Reliquary of Twilight".equals(n))return o.put("label_ptbr","Relicário do Crepúsculo").put("immune_to","Morningtide").put("eclipsed_realm_protection",true);
    }catch(Exception ignored){}return o;}

    /** sec_0_1: Welcome to Lorwyn */
    public JSONObject welcomeToLorwyn() {
        JSONObject o = new JSONObject();
        try {
            o.put("section_id", "sec_0_1").put("source_book", "LFL").put("title", "Welcome to Lorwyn")
             .put("nature", "Pastoral fey realm bathed in perpetual midsummer light, untouched by standard human civilizations")
             .put("core_themes", "Fey folklore, transformation cycles, isolation, and contrasting dualities with Shadowmoor")
             .put("source_verified", true).put("auto_rolls", false);
        } catch (Exception ignored) {}
        return o;
    }

    /** sec_0_2: The Vibrant Wilds of Lorwyn */
    public JSONObject vibrantWildsOfLorwyn() {
        JSONObject o = new JSONObject();
        try {
            o.put("section_id", "sec_0_2").put("source_book", "LFL").put("title", "The Vibrant Wilds of Lorwyn")
             .put("flora_and_fauna", "Idyllic meadows, sentient wanderers, playful boggarts and luminescent flamekin")
             .put("aurora_influence", "Perpetual daylight keeping despair and decay dormant")
             .put("source_verified", true).put("auto_rolls", false);
        } catch (Exception ignored) {}
        return o;
    }

    /** sec_0_3: The Sinister Wilds of Shadowmoor */
    public JSONObject sinisterWildsOfShadowmoor() {
        JSONObject o = new JSONObject();
        try {
            o.put("section_id", "sec_0_3").put("source_book", "LFL").put("title", "The Sinister Wilds of Shadowmoor")
             .put("transformation_nature", "The dark mirror realm where daylight fades into twilight and beings transform into bitter reflections")
             .put("hazards", "Thorned brambles, treacherous bogs, hostile scarecrows and rimekin")
             .put("source_verified", true).put("auto_rolls", false);
        } catch (Exception ignored) {}
        return o;
    }

    /** sec_0_4: Eclipsed Realms */
    public JSONObject eclipsedRealms() {
        JSONObject o = new JSONObject();
        try {
            o.put("section_id", "sec_0_4").put("source_book", "LFL").put("title", "Eclipsed Realms")
             .put("mechanic", "Boundary pockets where Lorwyn and Shadowmoor overlap")
             .put("suppression_rules", "Creatures without reliquary protection risk emotional erosion or class feature suppression")
             .put("protection_items", "Reliquary of Dawn, Reliquary of Twilight")
             .put("source_verified", true).put("auto_rolls", false);
        } catch (Exception ignored) {}
        return o;
    }

    /** sec_0_5: Traversing Lorwyn-Shadowmoor */
    public JSONObject traversingLorwynShadowmoor() {
        JSONObject o = new JSONObject();
        try {
            o.put("section_id", "sec_0_5").put("source_book", "LFL").put("title", "Traversing Lorwyn-Shadowmoor")
             .put("crossing_event_lorwyn_to_shadowmoor", "Eventide")
             .put("crossing_event_shadowmoor_to_lorwyn", "Morningtide")
             .put("transformation_rule", "Native beings invert aspects unless protected by corresponding reliquary")
             .put("source_verified", true).put("auto_rolls", false);
        } catch (Exception ignored) {}
        return o;
    }

    /** sec_0_6: Lorwyn-Shadowmoor's History */
    public JSONObject lorwynShadowmoorHistory() {
        JSONObject o = new JSONObject();
        try {
            o.put("section_id", "sec_0_6").put("source_book", "LFL").put("title", "Lorwyn-Shadowmoor's History")
             .put("great_aurora_cycle", "Cyclical cosmic transformation turning Lorwyn to Shadowmoor and back")
             .put("isolation_from_multiverse", "Planar isolation prevents direct Planar Travel without specialized catalysts")
             .put("source_verified", true).put("auto_rolls", false);
        } catch (Exception ignored) {}
        return o;
    }

    /** sec_1_3: Adapting Species */
    public JSONObject adaptingSpecies() {
        JSONObject o = new JSONObject();
        try {
            o.put("section_id", "sec_1_3").put("source_book", "LFL").put("title", "Adapting Species")
             .put("conversion_guidelines", "Adapts standard D&D 5e / MPMM species to Lorwyn/Shadowmoor lore without altering core math")
             .put("adapted_species_count", SPECIES.length)
             .put("source_verified", true).put("auto_rolls", false);
        } catch (Exception ignored) {}
        return o;
    }

    /** sec_2_1: Lorwyn Gazetteer */
    public JSONObject gazetteerLorwyn() {
        JSONObject o = new JSONObject();
        try {
            o.put("section_id", "sec_2_1").put("source_book", "LFL").put("title", "Lorwyn")
             .put("settlements", new JSONArray().put("Kithkin clachans").put("Treefolk groves").put("Elven gilded courts"))
             .put("climate", "Warm, sunlit pastoral landscape")
             .put("source_verified", true).put("auto_rolls", false);
        } catch (Exception ignored) {}
        return o;
    }

    /** sec_2_2: Shadowmoor Gazetteer */
    public JSONObject gazetteerShadowmoor() {
        JSONObject o = new JSONObject();
        try {
            o.put("section_id", "sec_2_2").put("source_book", "LFL").put("title", "Shadowmoor")
             .put("settlements", new JSONArray().put("Barricaded douns").put("Desolate cinders").put("Bog fortresses"))
             .put("climate", "Cold, moonless gloom with sinister waterways")
             .put("source_verified", true).put("auto_rolls", false);
        } catch (Exception ignored) {}
        return o;
    }

    /** sec_3_0: Adapting Other Creatures */
    public JSONObject adaptingOtherCreatures() {
        JSONObject o = new JSONObject();
        try {
            o.put("section_id", "sec_3_0").put("source_book", "LFL").put("title", "Adapting Other Creatures")
             .put("creature_adaptation_guidelines", "Standard D&D monsters transformed into folklore versions (scarecrows, hags, giants)")
             .put("tactical_monsters_count", 8)
             .put("source_verified", true).put("auto_rolls", false);
        } catch (Exception ignored) {}
        return o;
    }

    /** sec_3_1: Creature Descriptions */
    public JSONArray creatureDescriptions() {
        JSONArray arr = new JSONArray();
        try {
            for (String[] id : new String[][]{
                {"Incarnation of Transience", "CR 7 Fey"},
                {"Incarnation of Vibrance", "CR 10 Fey"},
                {"Lorwyn Giant", "CR 6 Giant"},
                {"Merrow Doublespeaker", "CR 4 Monstrosity"},
                {"Merrow Haranguer", "CR 5 Monstrosity"},
                {"Noggle Ransacker", "CR 1/4 Fey"},
                {"Noggle Wild Mage", "CR 1 Fey"},
                {"Shadowmoor Giant", "CR 6 Giant"}
            }) {
                arr.put(new JSONObject().put("name", id[0]).put("summary", id[1]).put("source_verified", true));
            }
        } catch (Exception ignored) {}
        return arr;
    }

    /** Deterministic realm/feat/species procedures. State is persisted by the gateway. */
    public JSONObject resolve(String action,JSONObject in,JSONObject character,JSONObject state){String a=norm(action);JSONObject s=copy(state),o=resolvedBase(a);try{
        int level=Math.max(1,character==null?in.optInt("character_level",1):character.optInt("level",in.optInt("character_level",1)));
        int pb=2+(Math.min(20,level)-1)/4;
        if(a.equals("realm_crossing")){
            String from=realm(in.optString("from_realm")),to=realm(in.optString("to_realm")),nativeRealm=realm(in.optString("native_realm"));
            if(from.isEmpty()||to.isEmpty()||from.equals(to))return block(o,"distinct_known_realms_required");
            if("other".equals(nativeRealm))return o.put("resolved",true).put("state_after",s).put("transformation","none_non_native");
            if(nativeRealm.isEmpty())return pending(o,"native_realm_required");
            String effect="lorwyn".equals(from)&&"shadowmoor".equals(to)?"Eventide":"shadowmoor".equals(from)&&"lorwyn".equals(to)?"Morningtide":"";
            if(effect.isEmpty())return block(o,"crossing_must_be_lorwyn_shadowmoor");
            boolean protectedBy=("Eventide".equals(effect)&&in.optBoolean("wearing_reliquary_dawn",false))||("Morningtide".equals(effect)&&in.optBoolean("wearing_reliquary_twilight",false));
            if(protectedBy)return o.put("resolved",true).put("state_after",s).put("effect",effect).put("transformation_prevented",true).put("memory_change_prevented",true);
            s.put("current_aspect",to).put("aspect_memory_state","dim_distorted_opposite_aspect");
            return o.put("resolved",true).put("state_after",s).put("effect",effect).put("transformed",true).put("appearance_and_personality_change",true).put("memory_change","dim_distorted");
        }
        if(a.equals("eclipsed_realm_escalation")){
            boolean protectedBy=in.optBoolean("wearing_reliquary_dawn",false)||in.optBoolean("wearing_reliquary_twilight",false);
            if(protectedBy)return o.put("resolved",true).put("state_after",s).put("immune",true).put("reason","reliquary");
            String outcome=norm(in.optString("dm_outcome"));
            if(outcome.isEmpty())return pending(o,"dm_outcome_required:none|suppress_class_features|petrified");
            if(outcome.equals("none"))return o.put("resolved",true).put("state_after",s).put("effect","emotion_will_erosion_only");
            if(outcome.equals("suppress_class_features")){s.put("lfl_class_features_suppressed",true);return o.put("resolved",true).put("state_after",s).put("class_features_suppressed",true).put("dm_discretion",true);}
            if(outcome.equals("petrified")){s.put("lfl_petrified",true);return o.put("resolved",true).put("state_after",s).put("condition","Petrified").put("dm_discretion",true);}
            return block(o,"invalid_dm_outcome");
        }
        if(a.equals("leave_eclipsed_realm")){s.put("lfl_class_features_suppressed",false).put("lfl_petrified",false);return o.put("resolved",true).put("state_after",s).put("restored_lost_class_features",true).put("petrified_removed",true);}
        if(a.equals("child_sun_eyes"))return o.put("resolved",true).put("state_after",s).put("radius_ft",10).put("saving_throw_advantage","avoid_or_end_Blinded");
        if(a.equals("child_sun_faerie_fire_free")){
            if(s.optBoolean("child_sun_faerie_fire_used",false))return block(o,"no_free_cast_remaining");s.put("child_sun_faerie_fire_used",true);
            return o.put("resolved",true).put("state_after",s).put("spell","Faerie Fire").put("slot_spent",false).put("damage_cannot_break_concentration",true);
        }
        if(a.equals("shadowmoor_hex_free")){
            if(s.optBoolean("shadowmoor_hex_free_used",false))return block(o,"no_free_cast_remaining");s.put("shadowmoor_hex_free_used",true);
            return o.put("resolved",true).put("state_after",s).put("spell","Hex").put("slot_spent",false);
        }
        if(a.equals("shadowmoor_hex_curse_magic")){
            if(!in.optBoolean("target_hexed_by_user",false)||!in.optBoolean("target_hit_user",false))return block(o,"trigger_not_met");
            String turn=in.optString("turn_key").trim();if(turn.isEmpty())return pending(o,"turn_key_required");if(turn.equals(s.optString("shadowmoor_hex_last_turn")))return block(o,"once_per_turn_already_used");
            s.put("shadowmoor_hex_last_turn",turn);return o.put("resolved",true).put("state_after",s).put("psychic_damage",pb).put("auto_rolls",false);
        }
        if(a.equals("changeling_shape_self")){
            String shape=norm(in.optString("shape"));if(!shape.equals("humanoid")&&!shape.equals("beast"))return pending(o,"shape_required:humanoid|beast");
            s.put("lorwyn_changeling_shape",shape);return o.put("resolved",true).put("state_after",s).put("shape",shape).put("legs",shape.equals("humanoid")?2:4).put("statistics_unchanged",true);
        }
        if(a.equals("changeling_unpredictable_movement")){
            if(in.optBoolean("initiative_disadvantage",false))return block(o,"initiative_disadvantage_prevents_trait");int speed=Math.max(0,in.optInt("speed_ft",30));
            return o.put("resolved",true).put("state_after",s).put("immediate_move_max_ft",speed/2).put("trigger","initiative_roll");
        }
        return block(o,"action_not_implemented");
    }catch(Exception e){return block(o,"runtime_error:"+e.getClass().getSimpleName());}}

    public JSONObject longRest(JSONObject state){JSONObject s=copy(state);try{s.put("child_sun_faerie_fire_used",false).put("shadowmoor_hex_free_used",false).remove("shadowmoor_hex_last_turn");}catch(Exception ignored){}return s;}

    public JSONObject coverage(){JSONObject o=descriptor();try{
        boolean bestiary=LorwynBestiaryRuntimeEngine.coverage().optBoolean("complete_tactical",false), adventure=adventureCoverageComplete();
        return o.put("realm_transformation_runtime",true).put("eclipsed_realm_runtime",true).put("reliquary_runtime",2)
        .put("background_runtime",2).put("feat_runtime",2).put("species_runtime",7)
        .put("bestiary_tactical_runtime",bestiary?8:0).put("sample_adventure_modules_runtime",adventure?2:0)
        .put("aeghith_location_runtime",adventure).put("lfl_closed",bestiary&&adventure)
        .put("remaining_within_lfl",bestiary&&adventure?"":"bestiary_or_adventure_gate");
    }catch(Exception e){return o;}}
    private static boolean adventureCoverageComplete(){try{Class<?> c=Class.forName("com.braseiro.dnd5e.adventure.LorwynAdventureModuleCatalog");Object x=c.getMethod("coverage").invoke(null);return x instanceof JSONObject&&((JSONObject)x).optBoolean("complete",false);}catch(Exception e){return false;}}

    private static String canon(String[][] a,String n){String q=norm(n);for(String[] x:a)if(norm(x[0]).equals(q)||norm(x[1]).equals(q))return x[0];return "";}
    private static String realm(String x){String n=norm(x);if(n.equals("lorwyn"))return "lorwyn";if(n.equals("shadowmoor"))return "shadowmoor";if(n.equals("other")||n.equals("outsider")||n.equals("outro"))return "other";return "";}
    private static String norm(String x){return x==null?"":x.trim().toLowerCase(Locale.ROOT);}
    private static JSONObject base(String kind,String name){JSONObject o=new JSONObject();try{return o.put("kind",kind).put("name",name).put("source",SOURCE).put("source_code",SOURCE_CODE).put("source_verified",!name.isEmpty()).put("auto_rolls",false).put("rag_required",false);}catch(Exception e){return o;}}
    private static JSONObject resolvedBase(String action){JSONObject o=new JSONObject();try{return o.put("action",action).put("resolved",false).put("auto_rolls",false).put("source",SOURCE).put("source_code",SOURCE_CODE);}catch(Exception e){return o;}}
    private static JSONObject pending(JSONObject o,String reason){try{return o.put("resolved",false).put("status","pending_input").put("reason",reason).put("auto_rolls",false);}catch(Exception e){return o;}}
    private static JSONObject block(JSONObject o,String reason){try{return o.put("resolved",false).put("status","blocked").put("reason",reason).put("auto_rolls",false);}catch(Exception e){return o;}}
    private static JSONObject unknown(JSONObject o,String reason){try{return o.put("source_verified",false).put("status","blocked").put("reason",reason);}catch(Exception e){return o;}}
    private static JSONObject copy(JSONObject o){try{return o==null?new JSONObject():new JSONObject(o.toString());}catch(Exception e){return new JSONObject();}}
}
