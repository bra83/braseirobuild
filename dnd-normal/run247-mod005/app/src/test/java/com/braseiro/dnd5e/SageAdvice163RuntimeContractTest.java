package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.util.Arrays;
import java.util.Collection;
import static org.junit.Assert.*;

/**
 * 163 real invocations against SageAdvice2025RulesEngine.resolve().
 * These cases are a frozen implementation-contract inventory, not independent
 * verification of copyrighted source wording or a claim of 483 sections certified.
 * Every registered ruling is invoked and the promised manual/deterministic
 * behavior, category, no-hidden-roll and no-mutation contract are checked.
 */
@RunWith(Parameterized.class)
public final class SageAdvice163RuntimeContractTest {
    @Parameterized.Parameters(name="{index} [{0}] {2}")
    public static Collection<Object[]> cases() {
        return Arrays.asList(new Object[][] {
        {"d20_test_types", "ability_checks", "DETERMINISTIC"},
        {"ability_check_critical", "ability_checks", "DETERMINISTIC"},
        {"natural_1_ability_or_save", "ability_checks", "DETERMINISTIC"},
        {"hidden_vs_special_senses", "adventuring", "DETERMINISTIC"},
        {"stunned_movement", "adventuring", "DETERMINISTIC"},
        {"magical_darkness_darkvision", "adventuring", "DETERMINISTIC"},
        {"short_rest_daily_limit", "adventuring", "MANUAL_DM"},
        {"extradimensional_space_plane", "adventuring", "DETERMINISTIC"},
        {"is_magical_effect", "adventuring", "DETERMINISTIC"},
        {"reduce_exhaustion", "adventuring", "DETERMINISTIC"},
        {"level_cap", "adventuring", "DETERMINISTIC"},
        {"frightened_line_of_sight", "adventuring", "DETERMINISTIC"},
        {"delay_turn", "combat", "DETERMINISTIC"},
        {"ready_vs_other_reaction", "combat", "DETERMINISTIC"},
        {"action_bonus_action_interchange", "combat", "DETERMINISTIC"},
        {"flight_hover_required", "combat", "DETERMINISTIC"},
        {"move_through_prone_enemy", "combat", "DETERMINISTIC"},
        {"travel_pace_in_combat", "combat", "DETERMINISTIC"},
        {"multiple_dash", "combat", "DETERMINISTIC"},
        {"multiple_bonus_actions", "combat", "DETERMINISTIC"},
        {"help_move_away", "combat", "DETERMINISTIC"},
        {"rehide_action", "combat", "DETERMINISTIC"},
        {"ready_damage_stops_movement", "combat", "DETERMINISTIC"},
        {"ready_with_charger", "combat", "DETERMINISTIC"},
        {"ready_target_range_timing", "combat", "DETERMINISTIC"},
        {"ready_spell_resource", "combat", "DETERMINISTIC"},
        {"ready_trigger_order", "combat", "DETERMINISTIC"},
        {"reach_opportunity_attack", "combat", "DETERMINISTIC"},
        {"blinded_opportunity_attack", "combat", "DETERMINISTIC"},
        {"opportunity_grapple_shove", "combat", "DETERMINISTIC"},
        {"grappled_actions", "combat", "DETERMINISTIC"},
        {"grapple_shove_riposte", "combat", "DETERMINISTIC"},
        {"grappler_prone_target", "combat", "DETERMINISTIC"},
        {"ranged_target_cover", "combat", "DETERMINISTIC"},
        {"damage_floor_zero", "combat", "DETERMINISTIC"},
        {"save_then_resistance", "combat", "DETERMINISTIC"},
        {"nonlethal_melee_spell_attack", "combat", "DETERMINISTIC"},
        {"improvised_disarm", "combat", "MANUAL_DM"},
        {"light_weapon_draw_second", "combat", "DETERMINISTIC"},
        {"multiclass_prereq_temporary_score", "multiclassing", "DETERMINISTIC"},
        {"multiclass_cantrips", "multiclassing", "DETERMINISTIC"},
        {"multiclass_saving_throw_proficiency", "multiclassing", "DETERMINISTIC"},
        {"maneuvers_while_raging", "multiclassing", "DETERMINISTIC"},
        {"sneak_attack_unarmed", "multiclassing", "DETERMINISTIC"},
        {"wild_magic_multiclass_spell", "multiclassing", "DETERMINISTIC"},
        {"extra_attack_mixed_weapons", "class_features", "DETERMINISTIC"},
        {"reckless_attack_incoming_advantage", "class_features", "DETERMINISTIC"},
        {"bardic_inspiration_guidance_stack", "class_features", "DETERMINISTIC"},
        {"cutting_words_damage_roll", "class_features", "DETERMINISTIC"},
        {"bard_spell_scroll_magical_secrets", "class_features", "DETERMINISTIC"},
        {"combat_inspiration_critical", "class_features", "DETERMINISTIC"},
        {"wild_shape_escape_bonds", "class_features", "MANUAL_DM"},
        {"riposte_on_missed_opportunity_attack", "class_features", "DETERMINISTIC"},
        {"disarming_attack_shield", "class_features", "DETERMINISTIC"},
        {"martial_arts_finesse", "class_features", "DETERMINISTIC"},
        {"open_hand_size_limit", "class_features", "DETERMINISTIC"},
        {"divine_sense_heritage", "class_features", "DETERMINISTIC"},
        {"sneak_attack_frequency", "class_features", "DETERMINISTIC"},
        {"uncanny_dodge_scope", "class_features", "DETERMINISTIC"},
        {"evasion_while_surprised", "class_features", "DETERMINISTIC"},
        {"reliable_talent_interactions", "class_features", "DETERMINISTIC"},
        {"metamagic_repeat_same_option", "class_features", "DETERMINISTIC"},
        {"elemental_affinity_damage_roll", "class_features", "DETERMINISTIC"},
        {"empowered_spell_multi_ray", "class_features", "DETERMINISTIC"},
        {"quickened_spell_slot_limit", "class_features", "DETERMINISTIC"},
        {"twinned_spell_wish_duplicate", "class_features", "DETERMINISTIC"},
        {"wild_magic_surge_and_spell", "class_features", "DETERMINISTIC"},
        {"pact_blade_magic_weapon_form", "class_features", "DETERMINISTIC"},
        {"pact_weapon_other_wielder", "class_features", "DETERMINISTIC"},
        {"awakened_mind_two_way", "class_features", "DETERMINISTIC"},
        {"arcane_ward_heal", "class_features", "DETERMINISTIC"},
        {"alarm_ritual_arcane_ward", "class_features", "DETERMINISTIC"},
        {"arcane_ward_damage_order", "class_features", "DETERMINISTIC"},
        {"illusory_reality_frequency", "class_features", "DETERMINISTIC"},
        {"portent_cross_plane", "class_features", "DETERMINISTIC"},
        {"wizard_ritual_spellbook", "class_features", "DETERMINISTIC"},
        {"multiple_backgrounds", "backgrounds", "DETERMINISTIC"},
        {"naturally_stealthy_hide", "species_traits", "DETERMINISTIC"},
        {"archery_thrown_melee", "feats", "DETERMINISTIC"},
        {"charger_mounted", "feats", "DETERMINISTIC"},
        {"crossbow_expert_light_requirement", "feats", "DETERMINISTIC"},
        {"dual_wielder_heavy", "feats", "DETERMINISTIC"},
        {"dueling_with_shield", "feats", "DETERMINISTIC"},
        {"great_weapon_fighting_extra_damage", "feats", "DETERMINISTIC"},
        {"magic_initiate_own_class", "feats", "DETERMINISTIC"},
        {"polearm_master_ability_mod", "feats", "DETERMINISTIC"},
        {"savage_attacker_extra_damage", "feats", "DETERMINISTIC"},
        {"heavy_armor_dex", "equipment", "DETERMINISTIC"},
        {"shield_as_improvised_weapon_ac", "equipment", "DETERMINISTIC"},
        {"class_spell_identity", "spellcasting", "DETERMINISTIC"},
        {"aoe_origin_placement", "spellcasting", "DETERMINISTIC"},
        {"permanent_spell_dispel", "spellcasting", "DETERMINISTIC"},
        {"ritual_same_spell", "spellcasting", "DETERMINISTIC"},
        {"prepare_spells_timing", "spellcasting", "DETERMINISTIC"},
        {"touch_range_melee_attack", "spellcasting", "DETERMINISTIC"},
        {"spell_attack_critical", "spellcasting", "DETERMINISTIC"},
        {"multi_target_spell_resolution", "spellcasting", "DETERMINISTIC"},
        {"spell_attack_vs_attack_action", "spellcasting", "DETERMINISTIC"},
        {"melee_spell_opportunity_attack", "spellcasting", "DETERMINISTIC"},
        {"spell_target_types", "spellcasting", "DETERMINISTIC"},
        {"slotless_spell_level", "spellcasting", "DETERMINISTIC"},
        {"spells_per_turn", "spellcasting", "DETERMINISTIC"},
        {"reaction_spell_on_own_turn", "spellcasting", "DETERMINISTIC"},
        {"ritual_concentration_casting", "spellcasting", "DETERMINISTIC"},
        {"material_consumption", "spellcasting", "DETERMINISTIC"},
        {"focus_consumed_or_costly_material", "spellcasting", "DETERMINISTIC"},
        {"focus_somatic_hand", "spellcasting", "DETERMINISTIC"},
        {"concentration_range_los", "spellcasting", "DETERMINISTIC"},
        {"temp_hp_concentration_dc", "spellcasting", "DETERMINISTIC"},
        {"dismiss_spell", "spellcasting", "DETERMINISTIC"},
        {"animate_objects_time_stop", "spellcasting", "DETERMINISTIC"},
        {"armor_of_agathys_temp_hp_source", "spellcasting", "DETERMINISTIC"},
        {"barkskin_ac_floor", "spellcasting", "DETERMINISTIC"},
        {"chill_touch_regeneration", "spellcasting", "DETERMINISTIC"},
        {"cloud_of_daggers_grid", "spellcasting", "MANUAL_DM"},
        {"compelled_duel_teleport", "spellcasting", "DETERMINISTIC"},
        {"disintegrate_relentless_endurance", "spellcasting", "DETERMINISTIC"},
        {"dispel_magic_nonspell", "spellcasting", "DETERMINISTIC"},
        {"instantaneous_spell_dispel", "spellcasting", "DETERMINISTIC"},
        {"readied_dispel_magic", "spellcasting", "DETERMINISTIC"},
        {"dispel_globe_invulnerability", "spellcasting", "DETERMINISTIC"},
        {"dispel_bless_one_target", "spellcasting", "DETERMINISTIC"},
        {"familiar_help", "spellcasting", "DETERMINISTIC"},
        {"familiar_sneak_attack_ally", "spellcasting", "DETERMINISTIC"},
        {"guardian_of_faith_damage_pool", "spellcasting", "DETERMINISTIC"},
        {"heroism_temp_hp_stack", "spellcasting", "DETERMINISTIC"},
        {"heroism_existing_frightened", "spellcasting", "DETERMINISTIC"},
        {"mage_armor_shield", "spellcasting", "DETERMINISTIC"},
        {"mage_armor_unarmored_defense", "spellcasting", "DETERMINISTIC"},
        {"mage_armor_name_is_not_literal_armor", "spellcasting", "DETERMINISTIC"},
        {"minor_illusion_move_object", "spellcasting", "DETERMINISTIC"},
        {"minor_illusion_fog", "spellcasting", "DETERMINISTIC"},
        {"planar_binding_summon", "spellcasting", "DETERMINISTIC"},
        {"polymorph_concentration", "spellcasting", "DETERMINISTIC"},
        {"polymorph_willing_mental_stats", "spellcasting", "DETERMINISTIC"},
        {"polymorph_leftover_resistance", "spellcasting", "DETERMINISTIC"},
        {"polymorph_other_effects", "spellcasting", "DETERMINISTIC"},
        {"prestidigitation_trinket", "spellcasting", "MANUAL_DM"},
        {"sanctuary_grapple_shove", "spellcasting", "DETERMINISTIC"},
        {"shillelagh_polearm_master_die", "spellcasting", "DETERMINISTIC"},
        {"storm_of_vengeance_rounds", "spellcasting", "DETERMINISTIC"},
        {"suggestion_verbal_component", "spellcasting", "DETERMINISTIC"},
        {"telekinesis_resilient_sphere", "spellcasting", "DETERMINISTIC"},
        {"true_strike_interactions", "spellcasting", "DETERMINISTIC"},
        {"unseen_servant_ally", "spellcasting", "DETERMINISTIC"},
        {"wish_stress", "spellcasting", "DETERMINISTIC"},
        {"monster_opportunity_attack", "monsters", "DETERMINISTIC"},
        {"monster_languages", "monsters", "DETERMINISTIC"},
        {"monster_dex_melee_finesse", "monsters", "DETERMINISTIC"},
        {"magic_resistance_channel_divinity", "monsters", "DETERMINISTIC"},
        {"shadow_critical_strength_drain", "monsters", "DETERMINISTIC"},
        {"vampire_humanoid_spells", "monsters", "DETERMINISTIC"},
        {"burrow_drag_grappled", "monsters", "DETERMINISTIC"},
        {"hydra_reactive_heads_oa", "monsters", "DETERMINISTIC"},
        {"magic_item_spell_save_dc", "magic_items", "DETERMINISTIC"},
        {"administer_potion_unconscious", "magic_items", "DETERMINISTIC"},
        {"potion_concentration", "magic_items", "DETERMINISTIC"},
        {"spell_scroll_class_list", "magic_items", "DETERMINISTIC"},
        {"ring_bracers_ac_stack", "magic_items", "DETERMINISTIC"},
        {"magic_bow_nonmagic_ammo", "magic_items", "DETERMINISTIC"},
        {"thrown_magic_weapon_return", "magic_items", "DETERMINISTIC"},
        {"sphere_annihilation_tiny_hut", "magic_items", "DETERMINISTIC"},
        {"magic_shield_must_don", "magic_items", "DETERMINISTIC"}
        });
    }

    private final String ruleId;
    private final String category;
    private final String mode;
    public SageAdvice163RuntimeContractTest(String ruleId, String category, String mode) {
        this.ruleId=ruleId;
        this.category=category;
        this.mode=mode;
    }

    @Test public void registeredRulingMustExecuteItsDeclaredContract() throws Exception {
        SageAdvice2025RulesEngine engine=new SageAdvice2025RulesEngine();
        JSONObject input=new JSONObject().put("client_sentinel", "unchanged");
        String before=input.toString();
        JSONObject answer=engine.resolve(ruleId,input);
        assertTrue(ruleId + " must resolve", answer.optBoolean("resolved",false));
        assertEquals(ruleId, answer.optString("rule_id"));
        assertEquals("xsac",answer.optString("source_id"));
        assertEquals(category,answer.optString("category"));
        assertEquals(mode,answer.optString("resolution_mode"));
        assertEquals("Manual decisions may not be silently automated",
                mode.equals("MANUAL_DM") ? "manual_dm":"resolved",
                answer.optString("status"));
        assertFalse(ruleId + " must never auto-roll", answer.optBoolean("auto_rolls",true));
        assertFalse(ruleId + " must never silently change game state",
                answer.optBoolean("auto_mutation_allowed",true));
        assertFalse(ruleId + " must supply a meaningful ruling",
                answer.optString("rule","").trim().isEmpty());
        assertEquals("Input mutated: "+ruleId,before,input.toString());
    }
}
