package com.braseiro.dnd5e;

import org.json.JSONObject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exact, pinned XSAC 2025 Q/A locators; no copyrighted answer text embedded.
 * Source location is not proof of semantic fidelity or redistribution rights.
 */
public final class XsacSourceLocator {
    public static final String SOURCE_COMMIT = "8c026b807fac21862a309379a0c3228a0683198b";
    public static final String SOURCE_BLOB_SHA1 = "b99ffecf35c09ad8efc5f3b1ab6b3df2b6562a06";
    public static final String SOURCE_FILE = "data/book/book-xsac.json";

    private static final class Entry {
        final String sectionId, sourceId, pointer;
        Entry(String sectionId, String sourceId, String pointer) {
            this.sectionId = sectionId;
            this.sourceId = sourceId;
            this.pointer = pointer;
        }
    }
    private static final Map<String, Entry> ENTRIES;
    static {
        Map<String, Entry> values = new LinkedHashMap<>();
        add(values, "d20_test_types", "sec_0_2", "00e", "/bookData/0/data/0/entries/3/entries/0/entries/0/entries/0");
        add(values, "ability_check_critical", "sec_0_2", "00f", "/bookData/0/data/0/entries/3/entries/0/entries/0/entries/1");
        add(values, "natural_1_ability_or_save", "sec_0_2", "010", "/bookData/0/data/0/entries/3/entries/0/entries/0/entries/2");
        add(values, "hidden_vs_special_senses", "sec_0_3", "014", "/bookData/0/data/0/entries/4/entries/0/entries/0/entries/0");
        add(values, "stunned_movement", "sec_0_3", "015", "/bookData/0/data/0/entries/4/entries/0/entries/0/entries/1");
        add(values, "magical_darkness_darkvision", "sec_0_3", "016", "/bookData/0/data/0/entries/4/entries/0/entries/0/entries/2");
        add(values, "short_rest_daily_limit", "sec_0_3", "017", "/bookData/0/data/0/entries/4/entries/0/entries/0/entries/3");
        add(values, "extradimensional_space_plane", "sec_0_3", "018", "/bookData/0/data/0/entries/4/entries/0/entries/0/entries/4");
        add(values, "is_magical_effect", "sec_0_3", "019", "/bookData/0/data/0/entries/4/entries/0/entries/0/entries/5");
        add(values, "reduce_exhaustion", "sec_0_3", "01a", "/bookData/0/data/0/entries/4/entries/0/entries/0/entries/6");
        add(values, "level_cap", "sec_0_3", "01b", "/bookData/0/data/0/entries/4/entries/0/entries/0/entries/7");
        add(values, "frightened_line_of_sight", "sec_0_3", "01c", "/bookData/0/data/0/entries/4/entries/0/entries/0/entries/8");
        add(values, "delay_turn", "sec_0_4", "020", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/0");
        add(values, "ready_vs_other_reaction", "sec_0_4", "021", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/1");
        add(values, "action_bonus_action_interchange", "sec_0_4", "022", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/2");
        add(values, "flight_hover_required", "sec_0_4", "023", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/3");
        add(values, "move_through_prone_enemy", "sec_0_4", "024", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/4");
        add(values, "travel_pace_in_combat", "sec_0_4", "025", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/5");
        add(values, "multiple_dash", "sec_0_4", "026", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/6");
        add(values, "multiple_bonus_actions", "sec_0_4", "027", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/7");
        add(values, "help_move_away", "sec_0_4", "028", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/8");
        add(values, "rehide_action", "sec_0_4", "029", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/9");
        add(values, "ready_damage_stops_movement", "sec_0_4", "02a", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/10");
        add(values, "ready_with_charger", "sec_0_4", "02b", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/11");
        add(values, "ready_target_range_timing", "sec_0_4", "02c", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/12");
        add(values, "ready_spell_resource", "sec_0_4", "02d", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/13");
        add(values, "ready_trigger_order", "sec_0_4", "02e", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/14");
        add(values, "reach_opportunity_attack", "sec_0_4", "02f", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/15");
        add(values, "blinded_opportunity_attack", "sec_0_4", "030", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/16");
        add(values, "opportunity_grapple_shove", "sec_0_4", "031", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/17");
        add(values, "grappled_actions", "sec_0_4", "032", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/18");
        add(values, "grapple_shove_riposte", "sec_0_4", "033", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/19");
        add(values, "grappler_prone_target", "sec_0_4", "034", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/20");
        add(values, "ranged_target_cover", "sec_0_4", "035", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/21");
        add(values, "damage_floor_zero", "sec_0_4", "036", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/22");
        add(values, "save_then_resistance", "sec_0_4", "037", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/23");
        add(values, "nonlethal_melee_spell_attack", "sec_0_4", "038", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/24");
        add(values, "improvised_disarm", "sec_0_4", "039", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/25");
        add(values, "light_weapon_draw_second", "sec_0_4", "03a", "/bookData/0/data/0/entries/5/entries/0/entries/0/entries/26");
        add(values, "multiclass_prereq_temporary_score", "sec_0_5", "03e", "/bookData/0/data/0/entries/6/entries/0/entries/0/entries/0");
        add(values, "multiclass_cantrips", "sec_0_5", "03f", "/bookData/0/data/0/entries/6/entries/0/entries/0/entries/1");
        add(values, "multiclass_saving_throw_proficiency", "sec_0_5", "040", "/bookData/0/data/0/entries/6/entries/0/entries/0/entries/2");
        add(values, "maneuvers_while_raging", "sec_0_5", "041", "/bookData/0/data/0/entries/6/entries/0/entries/0/entries/3");
        add(values, "sneak_attack_unarmed", "sec_0_5", "042", "/bookData/0/data/0/entries/6/entries/0/entries/0/entries/4");
        add(values, "wild_magic_multiclass_spell", "sec_0_5", "043", "/bookData/0/data/0/entries/6/entries/0/entries/0/entries/5");
        add(values, "extra_attack_mixed_weapons", "sec_0_6", "047", "/bookData/0/data/0/entries/7/entries/0/entries/0/entries/0");
        add(values, "reckless_attack_incoming_advantage", "sec_0_6", "04a", "/bookData/0/data/0/entries/7/entries/1/entries/0/entries/0");
        add(values, "bardic_inspiration_guidance_stack", "sec_0_6", "04d", "/bookData/0/data/0/entries/7/entries/2/entries/0/entries/0");
        add(values, "cutting_words_damage_roll", "sec_0_6", "04e", "/bookData/0/data/0/entries/7/entries/2/entries/0/entries/1");
        add(values, "bard_spell_scroll_magical_secrets", "sec_0_6", "04f", "/bookData/0/data/0/entries/7/entries/2/entries/0/entries/2");
        add(values, "combat_inspiration_critical", "sec_0_6", "050", "/bookData/0/data/0/entries/7/entries/2/entries/0/entries/3");
        add(values, "wild_shape_escape_bonds", "sec_0_6", "053", "/bookData/0/data/0/entries/7/entries/3/entries/0/entries/0");
        add(values, "riposte_on_missed_opportunity_attack", "sec_0_6", "056", "/bookData/0/data/0/entries/7/entries/4/entries/0/entries/0");
        add(values, "disarming_attack_shield", "sec_0_6", "057", "/bookData/0/data/0/entries/7/entries/4/entries/0/entries/1");
        add(values, "martial_arts_finesse", "sec_0_6", "05a", "/bookData/0/data/0/entries/7/entries/5/entries/0/entries/0");
        add(values, "open_hand_size_limit", "sec_0_6", "05b", "/bookData/0/data/0/entries/7/entries/5/entries/0/entries/1");
        add(values, "divine_sense_heritage", "sec_0_6", "05e", "/bookData/0/data/0/entries/7/entries/6/entries/0/entries/0");
        add(values, "sneak_attack_frequency", "sec_0_6", "061", "/bookData/0/data/0/entries/7/entries/7/entries/0/entries/0");
        add(values, "uncanny_dodge_scope", "sec_0_6", "062", "/bookData/0/data/0/entries/7/entries/7/entries/0/entries/1");
        add(values, "evasion_while_surprised", "sec_0_6", "063", "/bookData/0/data/0/entries/7/entries/7/entries/0/entries/2");
        add(values, "reliable_talent_interactions", "sec_0_6", "064", "/bookData/0/data/0/entries/7/entries/7/entries/0/entries/3");
        add(values, "metamagic_repeat_same_option", "sec_0_6", "067", "/bookData/0/data/0/entries/7/entries/8/entries/0/entries/0");
        add(values, "elemental_affinity_damage_roll", "sec_0_6", "068", "/bookData/0/data/0/entries/7/entries/8/entries/0/entries/1");
        add(values, "empowered_spell_multi_ray", "sec_0_6", "069", "/bookData/0/data/0/entries/7/entries/8/entries/0/entries/2");
        add(values, "quickened_spell_slot_limit", "sec_0_6", "06a", "/bookData/0/data/0/entries/7/entries/8/entries/0/entries/3");
        add(values, "twinned_spell_wish_duplicate", "sec_0_6", "06b", "/bookData/0/data/0/entries/7/entries/8/entries/0/entries/4");
        add(values, "wild_magic_surge_and_spell", "sec_0_6", "06c", "/bookData/0/data/0/entries/7/entries/8/entries/0/entries/5");
        add(values, "pact_blade_magic_weapon_form", "sec_0_6", "06f", "/bookData/0/data/0/entries/7/entries/9/entries/0/entries/0");
        add(values, "pact_weapon_other_wielder", "sec_0_6", "070", "/bookData/0/data/0/entries/7/entries/9/entries/0/entries/1");
        add(values, "awakened_mind_two_way", "sec_0_6", "071", "/bookData/0/data/0/entries/7/entries/9/entries/0/entries/2");
        add(values, "arcane_ward_heal", "sec_0_6", "074", "/bookData/0/data/0/entries/7/entries/10/entries/0/entries/0");
        add(values, "alarm_ritual_arcane_ward", "sec_0_6", "075", "/bookData/0/data/0/entries/7/entries/10/entries/0/entries/1");
        add(values, "arcane_ward_damage_order", "sec_0_6", "076", "/bookData/0/data/0/entries/7/entries/10/entries/0/entries/2");
        add(values, "illusory_reality_frequency", "sec_0_6", "077", "/bookData/0/data/0/entries/7/entries/10/entries/0/entries/3");
        add(values, "portent_cross_plane", "sec_0_6", "078", "/bookData/0/data/0/entries/7/entries/10/entries/0/entries/4");
        add(values, "wizard_ritual_spellbook", "sec_0_6", "079", "/bookData/0/data/0/entries/7/entries/10/entries/0/entries/5");
        add(values, "multiple_backgrounds", "sec_0_7", "07d", "/bookData/0/data/0/entries/8/entries/0/entries/0/entries/0");
        add(values, "naturally_stealthy_hide", "sec_0_8", "081", "/bookData/0/data/0/entries/9/entries/0/entries/0/entries/0");
        add(values, "archery_thrown_melee", "sec_0_9", "085", "/bookData/0/data/0/entries/10/entries/0/entries/0/entries/0");
        add(values, "charger_mounted", "sec_0_9", "088", "/bookData/0/data/0/entries/10/entries/1/entries/0/entries/0");
        add(values, "crossbow_expert_light_requirement", "sec_0_9", "08b", "/bookData/0/data/0/entries/10/entries/2/entries/0/entries/0");
        add(values, "dual_wielder_heavy", "sec_0_9", "08e", "/bookData/0/data/0/entries/10/entries/3/entries/0/entries/0");
        add(values, "dueling_with_shield", "sec_0_9", "091", "/bookData/0/data/0/entries/10/entries/4/entries/0/entries/0");
        add(values, "great_weapon_fighting_extra_damage", "sec_0_9", "094", "/bookData/0/data/0/entries/10/entries/5/entries/0/entries/0");
        add(values, "magic_initiate_own_class", "sec_0_9", "097", "/bookData/0/data/0/entries/10/entries/6/entries/0/entries/0");
        add(values, "polearm_master_ability_mod", "sec_0_9", "09a", "/bookData/0/data/0/entries/10/entries/7/entries/0/entries/0");
        add(values, "savage_attacker_extra_damage", "sec_0_9", "09d", "/bookData/0/data/0/entries/10/entries/8/entries/0/entries/0");
        add(values, "heavy_armor_dex", "sec_0_10", "0a1", "/bookData/0/data/0/entries/11/entries/0/entries/0/entries/0");
        add(values, "shield_as_improvised_weapon_ac", "sec_0_10", "0a2", "/bookData/0/data/0/entries/11/entries/0/entries/0/entries/1");
        add(values, "class_spell_identity", "sec_0_11", "0a6", "/bookData/0/data/0/entries/12/entries/0/entries/0/entries/0");
        add(values, "aoe_origin_placement", "sec_0_11", "0a7", "/bookData/0/data/0/entries/12/entries/0/entries/0/entries/1");
        add(values, "permanent_spell_dispel", "sec_0_11", "0a8", "/bookData/0/data/0/entries/12/entries/0/entries/0/entries/2");
        add(values, "ritual_same_spell", "sec_0_11", "0a9", "/bookData/0/data/0/entries/12/entries/0/entries/0/entries/3");
        add(values, "prepare_spells_timing", "sec_0_11", "0aa", "/bookData/0/data/0/entries/12/entries/0/entries/0/entries/4");
        add(values, "touch_range_melee_attack", "sec_0_11", "0ad", "/bookData/0/data/0/entries/12/entries/1/entries/0/entries/0");
        add(values, "spell_attack_critical", "sec_0_11", "0ae", "/bookData/0/data/0/entries/12/entries/1/entries/0/entries/1");
        add(values, "multi_target_spell_resolution", "sec_0_11", "0af", "/bookData/0/data/0/entries/12/entries/1/entries/0/entries/2");
        add(values, "spell_attack_vs_attack_action", "sec_0_11", "0b0", "/bookData/0/data/0/entries/12/entries/1/entries/0/entries/3");
        add(values, "melee_spell_opportunity_attack", "sec_0_11", "0b1", "/bookData/0/data/0/entries/12/entries/1/entries/0/entries/4");
        add(values, "spell_target_types", "sec_0_11", "0b2", "/bookData/0/data/0/entries/12/entries/1/entries/0/entries/5");
        add(values, "slotless_spell_level", "sec_0_11", "0b5", "/bookData/0/data/0/entries/12/entries/2/entries/0/entries/0");
        add(values, "spells_per_turn", "sec_0_11", "0b8", "/bookData/0/data/0/entries/12/entries/3/entries/0/entries/0");
        add(values, "reaction_spell_on_own_turn", "sec_0_11", "0b9", "/bookData/0/data/0/entries/12/entries/3/entries/0/entries/1");
        add(values, "ritual_concentration_casting", "sec_0_11", "0ba", "/bookData/0/data/0/entries/12/entries/3/entries/0/entries/2");
        add(values, "material_consumption", "sec_0_11", "0bd", "/bookData/0/data/0/entries/12/entries/4/entries/0/entries/0");
        add(values, "focus_consumed_or_costly_material", "sec_0_11", "0be", "/bookData/0/data/0/entries/12/entries/4/entries/0/entries/1");
        add(values, "focus_somatic_hand", "sec_0_11", "0bf", "/bookData/0/data/0/entries/12/entries/4/entries/0/entries/2");
        add(values, "concentration_range_los", "sec_0_11", "0c2", "/bookData/0/data/0/entries/12/entries/5/entries/0/entries/0");
        add(values, "temp_hp_concentration_dc", "sec_0_11", "0c3", "/bookData/0/data/0/entries/12/entries/5/entries/0/entries/1");
        add(values, "dismiss_spell", "sec_0_11", "0c4", "/bookData/0/data/0/entries/12/entries/5/entries/0/entries/2");
        add(values, "animate_objects_time_stop", "sec_0_11", "0c7", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/0");
        add(values, "armor_of_agathys_temp_hp_source", "sec_0_11", "0c8", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/1");
        add(values, "barkskin_ac_floor", "sec_0_11", "0c9", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/2");
        add(values, "chill_touch_regeneration", "sec_0_11", "0ca", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/3");
        add(values, "cloud_of_daggers_grid", "sec_0_11", "0cb", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/4");
        add(values, "compelled_duel_teleport", "sec_0_11", "0cc", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/5");
        add(values, "disintegrate_relentless_endurance", "sec_0_11", "0cd", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/6");
        add(values, "dispel_magic_nonspell", "sec_0_11", "0ce", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/7");
        add(values, "instantaneous_spell_dispel", "sec_0_11", "0cf", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/8");
        add(values, "readied_dispel_magic", "sec_0_11", "0d0", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/9");
        add(values, "dispel_globe_invulnerability", "sec_0_11", "0d1", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/10");
        add(values, "dispel_bless_one_target", "sec_0_11", "0d2", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/11");
        add(values, "familiar_help", "sec_0_11", "0d3", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/12");
        add(values, "familiar_sneak_attack_ally", "sec_0_11", "0d4", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/13");
        add(values, "guardian_of_faith_damage_pool", "sec_0_11", "0d5", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/14");
        add(values, "heroism_temp_hp_stack", "sec_0_11", "0d6", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/15");
        add(values, "heroism_existing_frightened", "sec_0_11", "0d7", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/16");
        add(values, "mage_armor_shield", "sec_0_11", "0d8", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/17");
        add(values, "mage_armor_unarmored_defense", "sec_0_11", "0d9", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/18");
        add(values, "mage_armor_name_is_not_literal_armor", "sec_0_11", "0da", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/19");
        add(values, "minor_illusion_move_object", "sec_0_11", "0db", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/20");
        add(values, "minor_illusion_fog", "sec_0_11", "0dc", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/21");
        add(values, "planar_binding_summon", "sec_0_11", "0dd", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/22");
        add(values, "polymorph_concentration", "sec_0_11", "0de", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/23");
        add(values, "polymorph_willing_mental_stats", "sec_0_11", "0df", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/24");
        add(values, "polymorph_leftover_resistance", "sec_0_11", "0e0", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/25");
        add(values, "polymorph_other_effects", "sec_0_11", "0e1", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/26");
        add(values, "prestidigitation_trinket", "sec_0_11", "0e2", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/27");
        add(values, "sanctuary_grapple_shove", "sec_0_11", "0e3", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/28");
        add(values, "shillelagh_polearm_master_die", "sec_0_11", "0e4", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/29");
        add(values, "storm_of_vengeance_rounds", "sec_0_11", "0e5", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/30");
        add(values, "suggestion_verbal_component", "sec_0_11", "0e6", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/31");
        add(values, "telekinesis_resilient_sphere", "sec_0_11", "0e7", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/32");
        add(values, "true_strike_interactions", "sec_0_11", "0e8", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/33");
        add(values, "unseen_servant_ally", "sec_0_11", "0e9", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/34");
        add(values, "wish_stress", "sec_0_11", "0ea", "/bookData/0/data/0/entries/12/entries/6/entries/0/entries/35");
        add(values, "monster_opportunity_attack", "sec_0_12", "0ee", "/bookData/0/data/0/entries/13/entries/0/entries/0/entries/0");
        add(values, "monster_languages", "sec_0_12", "0ef", "/bookData/0/data/0/entries/13/entries/0/entries/0/entries/1");
        add(values, "monster_dex_melee_finesse", "sec_0_12", "0f0", "/bookData/0/data/0/entries/13/entries/0/entries/0/entries/2");
        add(values, "magic_resistance_channel_divinity", "sec_0_12", "0f1", "/bookData/0/data/0/entries/13/entries/0/entries/0/entries/3");
        add(values, "shadow_critical_strength_drain", "sec_0_12", "0f2", "/bookData/0/data/0/entries/13/entries/0/entries/0/entries/4");
        add(values, "vampire_humanoid_spells", "sec_0_12", "0f3", "/bookData/0/data/0/entries/13/entries/0/entries/0/entries/5");
        add(values, "burrow_drag_grappled", "sec_0_12", "0f4", "/bookData/0/data/0/entries/13/entries/0/entries/0/entries/6");
        add(values, "hydra_reactive_heads_oa", "sec_0_12", "0f5", "/bookData/0/data/0/entries/13/entries/0/entries/0/entries/7");
        add(values, "magic_item_spell_save_dc", "sec_0_13", "0f9", "/bookData/0/data/0/entries/14/entries/0/entries/0/entries/0");
        add(values, "administer_potion_unconscious", "sec_0_13", "0fa", "/bookData/0/data/0/entries/14/entries/0/entries/0/entries/1");
        add(values, "potion_concentration", "sec_0_13", "0fb", "/bookData/0/data/0/entries/14/entries/0/entries/0/entries/2");
        add(values, "spell_scroll_class_list", "sec_0_13", "0fc", "/bookData/0/data/0/entries/14/entries/0/entries/0/entries/3");
        add(values, "ring_bracers_ac_stack", "sec_0_13", "0fd", "/bookData/0/data/0/entries/14/entries/0/entries/0/entries/4");
        add(values, "magic_bow_nonmagic_ammo", "sec_0_13", "0fe", "/bookData/0/data/0/entries/14/entries/0/entries/0/entries/5");
        add(values, "thrown_magic_weapon_return", "sec_0_13", "0ff", "/bookData/0/data/0/entries/14/entries/0/entries/0/entries/6");
        add(values, "sphere_annihilation_tiny_hut", "sec_0_13", "100", "/bookData/0/data/0/entries/14/entries/0/entries/0/entries/7");
        add(values, "magic_shield_must_don", "sec_0_13", "101", "/bookData/0/data/0/entries/14/entries/0/entries/0/entries/8");
        ENTRIES = Collections.unmodifiableMap(values);
    }
    private XsacSourceLocator() {}

    private static void add(Map<String, Entry> values, String id,
                            String section, String sourceId, String pointer) {
        if (values.put(id, new Entry(section, sourceId, pointer)) != null) {
            throw new IllegalStateException("Duplicate XSAC source id: " + id);
        }
    }
    public static int mappedQas() { return ENTRIES.size(); }
    public static boolean hasQa(String id) { return ENTRIES.containsKey(id); }

    public static JSONObject annotate(JSONObject result, String id) throws Exception {
        Entry entry = ENTRIES.get(id);
        if (entry == null && !"dm_final_say".equals(id)) {
            throw new IllegalArgumentException("XSAC source ID not registered: " + id);
        }
        final boolean policy = entry == null;
        return result.put("source_commit", SOURCE_COMMIT)
                .put("source_blob_sha1", SOURCE_BLOB_SHA1)
                .put("source_file", SOURCE_FILE)
                .put("source_pointer", policy
                        ? "/bookData/0/data/0/entries/2"
                        : entry.pointer)
                .put("source_section_id", policy
                        ? "sec_0_1" : entry.sectionId)
                .put("source_qa_entry_id", policy ? "POLICY" : entry.sourceId)
                .put("source_record_kind", policy ? "EDITORIAL_DM_POLICY" : "OFFICIAL_QA")
                .put("source_rights_review_required", true)
                .put("source_semantics_certified", false);
    }
}
