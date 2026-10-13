package com.braseiro.dnd5e;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/**
 * Source-derived, independently articulated interpretive witnesses for all 163 XSAC Q/As.
 * Each case asserts its own conclusion. This does not certify mechanical changes
 * in other engines, only the accuracy of advisory responses in XSAC's runtime.
 */
@RunWith(Parameterized.class)
public final class SageAdvice163MeaningWitnessTest {
    @Parameterized.Parameters(name="{index}: {0} source-meaning")
    public static Collection<Object[]> cases() {
        return Arrays.asList(new Object[][] {
            {"d20_test_types", "tipos distintos de D20 Test"},
            {"ability_check_critical", "não geram Critical Hit"},
            {"natural_1_ability_or_save", "não é falha automática"},
            {"hidden_vs_special_senses", "hidden termina"},
            {"stunned_movement", "Stunned não impede movimento"},
            {"magical_darkness_darkvision", "só bloqueia Darkvision quando"},
            {"short_rest_daily_limit", "Não há teto fixo além do tempo disponível"},
            {"extradimensional_space_plane", "não estão no mesmo plano"},
            {"is_magical_effect", "efeitos de spells e magic items são mágicos"},
            {"reduce_exhaustion", "Greater Restoration"},
            {"level_cap", "nível máximo de personagem é 20"},
            {"frightened_line_of_sight", "exige que a fonte seja visível"},
            {"delay_turn", "Não existe delay turn"},
            {"ready_vs_other_reaction", "apenas uma Reaction disponível"},
            {"action_bonus_action_interchange", "não são intercambiáveis"},
            {"flight_hover_required", "sem hover pode permanecer no ar"},
            {"move_through_prone_enemy", "Prone sozinho não autoriza"},
            {"travel_pace_in_combat", "não se aplica ao movimento em combate"},
            {"multiple_dash", "Dash pode ser usado mais de uma vez"},
            {"multiple_bonus_actions", "Só uma Bonus Action"},
            {"help_move_away", "não é necessário permanecer adjacente"},
            {"rehide_action", "requer Hide action"},
            {"ready_damage_stops_movement", "não interrompe movimento sem efeito de parada"},
            {"ready_with_charger", "Ready não combina movimento"},
            {"ready_target_range_timing", "verificado quando a Reaction acontece"},
            {"ready_spell_resource", "gastos imediatamente e não retornam"},
            {"ready_trigger_order", "imediatamente após o evento gatilho"},
            {"reach_opportunity_attack", "usa seu alcance próprio"},
            {"blinded_opportunity_attack", "Blindsight pode satisfazer"},
            {"opportunity_grapple_shove", "pode usar Unarmed Strike e escolher Grapple ou Shove"},
            {"grappled_actions", "Grappled restringe movimento"},
            {"grapple_shove_riposte", "sem attack roll não acionam Riposte"},
            {"grappler_prone_target", "não torna automaticamente"},
            {"ranged_target_cover", "pode fornecer Cover"},
            {"damage_floor_zero", "nunca abaixo de 0"},
            {"save_then_resistance", "Resistance é aplicada ao dano já ajustado"},
            {"nonlethal_melee_spell_attack", "pode ser usado para nocautear"},
            {"improvised_disarm", "resolução é do DM"},
            {"light_weapon_draw_second", "outra arma Light diferente"},
            {"multiclass_prereq_temporary_score", "score-base"},
            {"multiclass_cantrips", "Cantrips concedidos por cada classe"},
            {"multiclass_saving_throw_proficiency", "não concede automaticamente"},
            {"maneuvers_while_raging", "Rage não proíbe maneuvers"},
            {"sneak_attack_unarmed", "Unarmed Strike não é weapon"},
            {"wild_magic_multiclass_spell", "associados/preparados pela classe Sorcerer"},
            {"extra_attack_mixed_weapons", "permite misturar weapons"},
            {"reckless_attack_incoming_advantage", "até o início do próximo turno"},
            {"bardic_inspiration_guidance_stack", "Bardic Inspiration e Guidance podem acumular"},
            {"cutting_words_damage_roll", "inclusive Magic Missile"},
            {"bard_spell_scroll_magical_secrets", "Magical Secrets conta como Bard spell"},
            {"combat_inspiration_critical", "duplicado por Critical Hit"},
            {"wild_shape_escape_bonds", "Mudança de tamanho pode escapar"},
            {"riposte_on_missed_opportunity_attack", "se a Reaction estiver disponível"},
            {"disarming_attack_shield", "Shield vestido (donned) não é apenas segurado"},
            {"martial_arts_finesse", "não concede a propriedade Finesse"},
            {"open_hand_size_limit", "não impõe limite de tamanho"},
            {"divine_sense_heritage", "Celestial, Fiend e Undead"},
            {"sneak_attack_frequency", "uma vez por turno, não uma vez por rodada"},
            {"uncanny_dodge_scope", "não se aplica a dano entregue só por saving throw"},
            {"evasion_while_surprised", "Incapacitated sim"},
            {"reliable_talent_interactions", "Jack of All Trades"},
            {"metamagic_repeat_same_option", "uma vez à mesma conjuração"},
            {"elemental_affinity_damage_roll", "um damage roll por conjuração"},
            {"empowered_spell_multi_ray", "múltiplos rays até o limite"},
            {"quickened_spell_slot_limit", "um spell slot"},
            {"twinned_spell_wish_duplicate", "se o spell duplicado satisfizer"},
            {"wild_magic_surge_and_spell", "Wild Magic Surge acontecem"},
            {"pact_blade_magic_weapon_form", "sua forma não é alterada"},
            {"pact_weapon_other_wielder", "desaparece após ficar longe"},
            {"awakened_mind_two_way", "comunicação telepática de duas vias"},
            {"arcane_ward_heal", "usando spell slot, até o máximo"},
            {"alarm_ritual_arcane_ward", "não recupera Arcane Ward"},
            {"arcane_ward_damage_order", "depois Temporary HP e HP"},
            {"illusory_reality_frequency", "não um novo objeto a cada rodada"},
            {"portent_cross_plane", "não tem limite de range próprio"},
            {"wizard_ritual_spellbook", "exige consultar spellbook"},
            {"multiple_backgrounds", "apenas um background"},
            {"naturally_stealthy_hide", "ainda exige Dexterity (Stealth) check"},
            {"archery_thrown_melee", "Melee weapon arremessada não ganha"},
            {"charger_mounted", "controlled mount"},
            {"crossbow_expert_light_requirement", "arma diferente"},
            {"dual_wielder_heavy", "Two-Handed"},
            {"dueling_with_shield", "na outra mão"},
            {"great_weapon_fighting_extra_damage", "não dados extras de Divine Smite"},
            {"magic_initiate_own_class", "própria classe"},
            {"polearm_master_ability_mod", "adiciona o ability modifier"},
            {"savage_attacker_extra_damage", "apenas weapon damage dice"},
            {"heavy_armor_dex", "não penaliza Dex saves"},
            {"shield_as_improvised_weapon_ac", "não remove automaticamente seu bônus de AC"},
            {"class_spell_identity", "explicitamente o torna spell dessa classe"},
            {"aoe_origin_placement", "ponto de origem de AoE"},
            {"permanent_spell_dispel", "continua dispensável"},
            {"ritual_same_spell", "não é versão separada"},
            {"prepare_spells_timing", "não em lotes arbitrários"},
            {"touch_range_melee_attack", "Range Touch não transforma"},
            {"spell_attack_critical", "porque usa attack roll"},
            {"multi_target_spell_resolution", "resolvidos consecutivamente"},
            {"spell_attack_vs_attack_action", "não equivale a tomar Attack action"},
            {"melee_spell_opportunity_attack", "salvo feature que altere isso"},
            {"spell_target_types", "spell que diz creature"},
            {"slotless_spell_level", "seu menor nível possível"},
            {"spells_per_turn", "Não há número fixo de spells por turno"},
            {"reaction_spell_on_own_turn", "Reaction spell pode ocorrer"},
            {"ritual_concentration_casting", "durante a conjuração"},
            {"material_consumption", "só é consumido quando"},
            {"focus_consumed_or_costly_material", "não substitui componente com custo"},
            {"focus_somatic_hand", "mesma mão pode manipular"},
            {"concentration_range_los", "não exige permanecer em range"},
            {"temp_hp_concentration_dc", "Temporary HP absorvem dano"},
            {"dismiss_spell", "Spell não-instantâneo pode ser encerrado"},
            {"animate_objects_time_stop", "comandar as criaturas pode encerrar"},
            {"armor_of_agathys_temp_hp_source", "eles não precisam vir do próprio spell"},
            {"barkskin_ac_floor", "piso de AC 17"},
            {"chill_touch_regeneration", "mesmo quando o alvo não sofre Necrotic"},
            {"cloud_of_daggers_grid", "snapping à grid é opção do DM"},
            {"compelled_duel_teleport", "também via teleport"},
            {"disintegrate_relentless_endurance", "troca 0 HP por 1"},
            {"dispel_magic_nonspell", "não remove efeito mágico não criado por spell"},
            {"instantaneous_spell_dispel", "não permanece sustentado por magia"},
            {"readied_dispel_magic", "não substitui Counterspell"},
            {"dispel_globe_invulnerability", "não dissipa automaticamente efeitos mágicos ativos"},
            {"dispel_bless_one_target", "não automaticamente de todos os alvos"},
            {"familiar_help", "familiar pode usar Help"},
            {"familiar_sneak_attack_ally", "Familiar é ally"},
            {"guardian_of_faith_damage_pool", "dano efetivamente causado após o save"},
            {"heroism_temp_hp_stack", "não se acumulam entre rodadas"},
            {"heroism_existing_frightened", "pode retornar depois"},
            {"mage_armor_shield", "Mage Armor funciona com Shield"},
            {"mage_armor_unarmored_defense", "não se somam"},
            {"mage_armor_name_is_not_literal_armor", "nome Mage Armor é figurativo"},
            {"minor_illusion_move_object", "não se move por si só"},
            {"minor_illusion_fog", "não efeito atmosférico"},
            {"planar_binding_summon", "não invoca a criatura"},
            {"polymorph_concentration", "não encerra automaticamente Concentration"},
            {"polymorph_willing_mental_stats", "inclusive atributos mentais"},
            {"polymorph_leftover_resistance", "inclusive Resistance aplicável"},
            {"polymorph_other_effects", "não apaga outros spell effects"},
            {"prestidigitation_trinket", "depende da escolha do caster/DM"},
            {"sanctuary_grapple_shove", "não encerram Sanctuary"},
            {"shillelagh_polearm_master_die", "usa d4 mesmo com Quarterstaff"},
            {"storm_of_vengeance_rounds", "substitui o efeito específico da rodada anterior"},
            {"suggestion_verbal_component", "separada do Verbal component místico"},
            {"telekinesis_resilient_sphere", "Sphere impede escolher novos alvos"},
            {"true_strike_interactions", "usa Magic action"},
            {"unseen_servant_ally", "não é creature/enemy/ally"},
            {"wish_stress", "0–8 com Wish não causa"},
            {"monster_opportunity_attack", "Multiattack e efeitos sem melee attack roll"},
            {"monster_languages", "não é exaustiva"},
            {"monster_dex_melee_finesse", "não concede Finesse"},
            {"magic_resistance_channel_divinity", "Channel Divinity quando o efeito é mágico"},
            {"shadow_critical_strength_drain", "redução de Strength que não é dano"},
            {"vampire_humanoid_spells", "Vampire não é Humanoid"},
            {"burrow_drag_grappled", "deixar túnel/espaço para o alvo"},
            {"hydra_reactive_heads_oa", "múltiplas Opportunity Attacks"},
            {"magic_item_spell_save_dc", "proficiency"},
            {"administer_potion_unconscious", "inclusive Unconscious"},
            {"potion_concentration", "se duplicar spell que exige Concentration"},
            {"spell_scroll_class_list", "Scroll comum pode ser tentado por leitor alfabetizado"},
            {"ring_bracers_ac_stack", "respeitando restrições de attunement"},
            {"magic_bow_nonmagic_ammo", "mesmo com ammunition não-mágica"},
            {"thrown_magic_weapon_return", "só retorna automaticamente se sua descrição disser"},
            {"sphere_annihilation_tiny_hut", "pode entrar/destruir a barreira"},
            {"magic_shield_must_don", "apenas segurá-lo não concede o bônus"}
        });
    }
    private final String id;
    private final String sourceMeaning;
    public SageAdvice163MeaningWitnessTest(String id, String sourceMeaning) {
        this.id=id; this.sourceMeaning=sourceMeaning;
    }
    @Test public void rulingConveysItsSpecificSourceConclusion() throws Exception {
        JSONObject input = new JSONObject().put("do_not_mutate", true);
        JSONObject response=new SageAdvice2025RulesEngine().resolve(id,input);
        assertTrue(id+" MUST resolve",response.getBoolean("resolved"));
        assertTrue(id+" source concept missing: "+sourceMeaning,
             response.getString("rule").toLowerCase(Locale.ROOT)
              .contains(sourceMeaning.toLowerCase(Locale.ROOT)));
        assertFalse("No automatic rolls",response.getBoolean("auto_rolls"));
        assertFalse("No hidden game mutations",response.getBoolean("auto_mutation_allowed"));
        assertTrue("Source QA must have pinned pointer",response.getString("source_pointer").startsWith("/bookData/"));
        assertTrue(input.getBoolean("do_not_mutate"));
    }
}
