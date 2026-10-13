package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Runtime determinístico para as clarificações oficiais do Sage Advice Compendium (2025).
 *
 * Princípios:
 * - nenhuma rolagem automática;
 * - nenhuma mutação de estado;
 * - nenhum RAG/LLM no caminho mecânico;
 * - decisões que o próprio material entrega ao DM ficam explicitamente MANUAL_DM;
 * - regras objetivas são retornadas como contratos estruturados, fail-closed.
 */
public final class SageAdvice2025RulesEngine {
    public static final String SOURCE_ID = "xsac";
    public static final String SOURCE_TITLE = "Sage Advice Compendium (2025)";
    public static final String SOURCE_SHA = XsacSourceLocator.SOURCE_BLOB_SHA1;
    public static final int SOURCE_RULINGS = 163;

    private static JSONObject ok(String id, String category, String rule) throws Exception {
        return XsacSourceLocator.annotate(new JSONObject()
                .put("resolved", true)
                .put("status", "resolved")
                .put("source_id", SOURCE_ID)
                .put("rule_id", id)
                .put("category", category)
                .put("rule", rule)
                .put("resolution_mode", "DETERMINISTIC")
                .put("auto_rolls", false)
                .put("auto_mutation_allowed", false), id);
    }

    private static JSONObject manual(String id, String category, String rule) throws Exception {
        return XsacSourceLocator.annotate(new JSONObject()
                .put("resolved", true)
                .put("status", "manual_dm")
                .put("source_id", SOURCE_ID)
                .put("rule_id", id)
                .put("category", category)
                .put("rule", rule)
                .put("resolution_mode", "MANUAL_DM")
                .put("auto_rolls", false)
                .put("auto_mutation_allowed", false), id);
    }

    private static JSONObject blocked(String id) throws Exception {
        return new JSONObject()
                .put("resolved", false)
                .put("status", "blocked")
                .put("source_id", SOURCE_ID)
                .put("rule_id", id == null ? "" : id)
                .put("reason", "xsac_rule_not_registered")
                .put("auto_rolls", false)
                .put("auto_mutation_allowed", false);
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase().replace(' ', '_').replace('-', '_');
    }

    public JSONObject coverage() throws Exception {
        JSONObject counts = new JSONObject()
                .put("ability_checks", 3)
                .put("adventuring", 9)
                .put("combat", 27)
                .put("multiclassing", 6)
                .put("class_features", 31)
                .put("backgrounds", 1)
                .put("species_traits", 1)
                .put("feats", 9)
                .put("equipment", 2)
                .put("spellcasting", 57)
                .put("monsters", 8)
                .put("magic_items", 9);
        return new JSONObject()
                .put("source_id", SOURCE_ID)
                .put("source_title", SOURCE_TITLE)
                .put("source_sha", SOURCE_SHA)
                .put("source_rulings", SOURCE_RULINGS)
                .put("classified_rulings", SOURCE_RULINGS)
                .put("category_counts", counts)
                .put("runtime_mode", "LOCAL_DETERMINISTIC_OR_MANUAL_DM")
                .put("rag_required", false)
                .put("auto_rolls", false)
                .put("auto_mutation_allowed", false)
                .put("dm_final_say", true)
                .put("complete", true)
                .put("source_fidelity_semantically_certified", false)
                .put("editorial_qas", 163)
                .put("editorial_policy_endpoints", 1);
    }

    public JSONArray categories() throws Exception {
        return new JSONArray()
                .put("ability_checks").put("adventuring").put("combat")
                .put("multiclassing").put("class_features").put("backgrounds")
                .put("species_traits").put("feats").put("equipment")
                .put("spellcasting").put("monsters").put("magic_items");
    }

    /** Resolve uma clarificação por ID sem tocar no estado do jogo. */
    public JSONObject resolve(String rawId, JSONObject input) throws Exception {
        final String id = norm(rawId);
        final JSONObject in = input == null ? new JSONObject() : input;
        switch (id) {
            // Ability checks
            case "d20_test_types": return ok(id,"ability_checks","Ability checks, attack rolls e saving throws são tipos distintos de D20 Test; efeitos específicos de um tipo não migram para outro.");
            case "ability_check_critical": return ok(id,"ability_checks","Ability checks não geram Critical Hit.").put("critical",false);
            case "natural_1_ability_or_save": return ok(id,"ability_checks","Natural 1 não é falha automática em ability check ou saving throw; em attack roll é miss automático.");

            // Adventuring
            case "hidden_vs_special_senses": return ok(id,"adventuring","Se uma criatura encontra/percebe o personagem por Blindsight ou Truesight, o estado hidden termina.");
            case "stunned_movement": return ok(id,"adventuring","Stunned não impede movimento por si só.").put("movement_allowed",true);
            case "magical_darkness_darkvision": return ok(id,"adventuring","Escuridão mágica só bloqueia Darkvision quando o efeito específico diz que bloqueia.");
            case "short_rest_daily_limit": return manual(id,"adventuring","Não há teto fixo além do tempo disponível; pressão da ficção e interrupções são adjudicadas pelo DM.");
            case "extradimensional_space_plane": return ok(id,"adventuring","Espaço extradimensional fica fora dos outros planos; ocupantes não estão no mesmo plano de quem ficou fora.");
            case "is_magical_effect": return ok(id,"adventuring","Um efeito é mágico quando o texto o declara mágico; efeitos de spells e magic items são mágicos.");
            case "reduce_exhaustion": return ok(id,"adventuring","Greater Restoration é exemplo de efeito que reduz Exhaustion.");
            case "level_cap": return ok(id,"adventuring","O nível máximo de personagem é 20; progressão posterior pode usar feats conforme orientação do PHB.").put("max_level",20);
            case "frightened_line_of_sight": return ok(id,"adventuring","Para Frightened, line of sight exige que a fonte seja visível; uma fonte imperceptível não está em line of sight.");

            // Combat
            case "delay_turn": return ok(id,"combat","Não existe delay turn; use Ready para agir em resposta a um gatilho.").put("allowed",false);
            case "ready_vs_other_reaction": return ok(id,"combat","Ter uma Ready reaction preparada não obriga usá-la; outra Reaction pode ser escolhida, mas há apenas uma Reaction disponível.");
            case "action_bonus_action_interchange": return ok(id,"combat","Action e Bonus Action não são intercambiáveis.").put("interchangeable",false);
            case "flight_hover_required": return ok(id,"combat","Uma criatura voadora sem hover pode permanecer no ar sem se mover a cada rodada.");
            case "move_through_prone_enemy": return ok(id,"combat","Prone sozinho não autoriza atravessar o espaço inimigo; aplicam-se as regras normais de tamanho/Incapacitated/Tiny.");
            case "travel_pace_in_combat": return ok(id,"combat","Travel pace não se aplica ao movimento em combate.").put("applies",false);
            case "multiple_dash": return ok(id,"combat","Dash pode ser usado mais de uma vez se houver múltiplas ações aptas; isso não cria Bonus Actions extras.");
            case "multiple_bonus_actions": return ok(id,"combat","Só uma Bonus Action pode ser usada no turno.").put("max_bonus_actions",1);
            case "help_move_away": return ok(id,"combat","Depois de usar Help para distrair um inimigo, não é necessário permanecer adjacente para preservar o benefício.");
            case "rehide_action": return ok(id,"combat","Sem habilidade especial, esconder-se novamente em combate requer Hide action.");
            case "ready_damage_stops_movement": return ok(id,"combat","Dano de Ready não interrompe movimento sem efeito de parada, como Sentinel, ou redução do alvo a 0 HP.");
            case "ready_with_charger": return ok(id,"combat","Ready não combina movimento + Attack no mesmo turno como Charger exige.").put("allowed",false);
            case "ready_target_range_timing": return ok(id,"combat","Alcance/alvo de readied action é verificado quando a Reaction acontece, não quando Ready é declarado.");
            case "ready_spell_resource": return ok(id,"combat","Ao preparar um spell, os recursos de conjuração são gastos imediatamente e não retornam se o gatilho não ocorrer.");
            case "ready_trigger_order": return ok(id,"combat","A readied action ocorre imediatamente após o evento gatilho especificado.");
            case "reach_opportunity_attack": return ok(id,"combat","Opportunity Attack ocorre quando o alvo sai do alcance usado pelo ataque; uma Reach weapon usa seu alcance próprio.");
            case "blinded_opportunity_attack": return ok(id,"combat","Opportunity Attack exige ver o alvo; Blindsight pode satisfazer essa exigência dentro do raio.");
            case "opportunity_grapple_shove": return ok(id,"combat","Opportunity Attack pode usar Unarmed Strike e escolher Grapple ou Shove.").put("allowed",true);
            case "grappled_actions": return ok(id,"combat","Grappled restringe movimento, não impede por si só ataques, spellcasting ou outras habilidades.");
            case "grapple_shove_riposte": return ok(id,"combat","Grapple/Shove sem attack roll não acionam Riposte por miss de attack roll.");
            case "grappler_prone_target": return ok(id,"combat","O grappler ficar Prone não torna automaticamente o alvo Grappled também Prone.");
            case "ranged_target_cover": return ok(id,"combat","Uma criatura entre atacante à distância e alvo pode fornecer Cover conforme geometria aplicável.");
            case "damage_floor_zero": return ok(id,"combat","Redução de dano pode chegar a 0, nunca abaixo de 0.").put("minimum_damage",0);
            case "save_then_resistance": return ok(id,"combat","Quando um save reduz dano e depois Resistance se aplica, Resistance é aplicada ao dano já ajustado.");
            case "nonlethal_melee_spell_attack": return ok(id,"combat","Um melee spell attack que reduz a 0 HP pode ser usado para nocautear em vez de matar, como outros melee attacks.");
            case "improvised_disarm": return manual(id,"combat","Personagem sem a manobra Disarming Attack pode tentar desarmar como ação improvisada; resolução é do DM.");
            case "light_weapon_draw_second": return ok(id,"combat","O ataque extra de Light pode ser habilitado após atacar e então sacar outra arma Light diferente, respeitando regras de equipar/sacar.");

            // Multiclassing
            case "multiclass_prereq_temporary_score": return ok(id,"multiclassing","Pré-requisito de multiclass usa o score-base, não aumento temporário.");
            case "multiclass_cantrips": return ok(id,"multiclassing","Cantrips concedidos por cada classe são obtidos normalmente.");
            case "multiclass_saving_throw_proficiency": return ok(id,"multiclassing","Multiclass não concede automaticamente as saving throw proficiencies iniciais da nova classe; features posteriores ainda podem concedê-las.");
            case "maneuvers_while_raging": return ok(id,"multiclassing","Rage não proíbe maneuvers por si só.").put("allowed",true);
            case "sneak_attack_unarmed": return ok(id,"multiclassing","Unarmed Strike não é weapon e não satisfaz Sneak Attack; Monk weapon com propriedade exigida pode satisfazer.");
            case "wild_magic_multiclass_spell": return ok(id,"multiclassing","Wild Magic Surge usa spells associados/preparados pela classe Sorcerer, não qualquer spell que coincida com a lista.");

            // Class features
            case "extra_attack_mixed_weapons": return ok(id,"class_features","Extra Attack permite misturar weapons, improvised weapons e Unarmed Strikes, respeitando equip/unequip.");
            case "reckless_attack_incoming_advantage": return ok(id,"class_features","Após Reckless Attack, attack rolls contra o Barbarian têm Advantage até o início do próximo turno.");
            case "bardic_inspiration_guidance_stack": return ok(id,"class_features","Bardic Inspiration e Guidance podem acumular quando ambos se aplicam porque são efeitos diferentes.");
            case "cutting_words_damage_roll": return ok(id,"class_features","Cutting Words pode reduzir qualquer damage roll elegível, inclusive Magic Missile.");
            case "bard_spell_scroll_magical_secrets": return ok(id,"class_features","Bard usa Spell Scroll de Bard spell; um spell externo preparado via Magical Secrets conta como Bard spell para esse Bard.");
            case "combat_inspiration_critical": return ok(id,"class_features","Die de dano de Combat Inspiration conta como damage die e é duplicado por Critical Hit.");
            case "wild_shape_escape_bonds": return manual(id,"class_features","Mudança de tamanho pode escapar de vínculos físicos quando a geometria/requisito de tamanho deixa de se aplicar; não remove condições sem fundamento.");
            case "riposte_on_missed_opportunity_attack": return ok(id,"class_features","Riposte pode responder a Opportunity Attack que errou, se a Reaction estiver disponível.");
            case "disarming_attack_shield": return ok(id,"class_features","Disarming Attack afeta objeto segurado; Shield vestido (donned) não é apenas segurado.").put("can_drop_donned_shield",false);
            case "martial_arts_finesse": return ok(id,"class_features","Martial Arts não concede a propriedade Finesse; apenas oferece benefício similar para modificador.");
            case "open_hand_size_limit": return ok(id,"class_features","Open Hand Technique não impõe limite de tamanho salvo texto específico.");
            case "divine_sense_heritage": return ok(id,"class_features","Divine Sense detecta creature types Celestial, Fiend e Undead; herança de Aasimar/Tiefling Humanoid não basta.");
            case "sneak_attack_frequency": return ok(id,"class_features","Sneak Attack é uma vez por turno, não uma vez por rodada; pode ocorrer em turno alheio se requisitos forem satisfeitos.");
            case "uncanny_dodge_scope": return ok(id,"class_features","Uncanny Dodge usa Reaction contra um ataque visto que acertou, inclusive spell attack; não se aplica a dano entregue só por saving throw.");
            case "evasion_while_surprised": return ok(id,"class_features","Surprised não desliga Evasion; Incapacitated sim conforme a feature.");
            case "reliable_talent_interactions": return ok(id,"class_features","Reliable Talent combina apenas com efeitos cujos requisitos também sejam satisfeitos; não combina com Jack of All Trades no mesmo check, mas pode coexistir com Advantage de Remarkable Athlete.");
            case "metamagic_repeat_same_option": return ok(id,"class_features","Uma opção de Metamagic só pode ser aplicada uma vez à mesma conjuração salvo texto específico.");
            case "elemental_affinity_damage_roll": return ok(id,"class_features","Elemental Affinity beneficia um damage roll por conjuração, não cada ray.");
            case "empowered_spell_multi_ray": return ok(id,"class_features","Empowered Spell pode rerrolar dados distribuídos entre múltiplos rays até o limite de dados da feature.");
            case "quickened_spell_slot_limit": return ok(id,"class_features","Quickened Spell não ignora a limitação de gastar apenas um spell slot para conjurar spell no turno.");
            case "twinned_spell_wish_duplicate": return ok(id,"class_features","Twinned Spell pode afetar spell duplicado por Wish se o spell duplicado satisfizer os requisitos de Twinned Spell.");
            case "wild_magic_surge_and_spell": return ok(id,"class_features","O spell gatilho e o efeito de Wild Magic Surge acontecem; um não substitui automaticamente o outro.");
            case "pact_blade_magic_weapon_form": return ok(id,"class_features","Pact of the Blade conjura Melee weapon escolhida; magic weapon vinculada pode ser não-melee, mas sua forma não é alterada pela feature.");
            case "pact_weapon_other_wielder": return ok(id,"class_features","Outra criatura pode empunhar pact weapon; a versão conjurada desaparece após ficar longe do Warlock pelo tempo definido.");
            case "awakened_mind_two_way": return ok(id,"class_features","Awakened Mind permite comunicação telepática de duas vias conforme Telepathy.");
            case "arcane_ward_heal": return ok(id,"class_features","Arcane Ward recupera HP quando o Wizard conjura Abjuration usando spell slot, até o máximo, não apenas quando está em 0.");
            case "alarm_ritual_arcane_ward": return ok(id,"class_features","Alarm como Ritual não usa spell slot e portanto não recupera Arcane Ward.");
            case "arcane_ward_damage_order": return ok(id,"class_features","Ajustes gerais de dano aplicam-se antes; Ward absorve; imunidade relevante pode afetar sobra; depois Temporary HP e HP do Wizard.");
            case "illusory_reality_frequency": return ok(id,"class_features","Illusory Reality torna real um objeto ilusório por Illusion spell conforme a feature, não um novo objeto a cada rodada.");
            case "portent_cross_plane": return ok(id,"class_features","Portent exige ver a criatura e não tem limite de range próprio; estar em outro plano não bloqueia por si só se a visão for possível.");
            case "wizard_ritual_spellbook": return ok(id,"class_features","Ritual não preparado via Ritual Adept exige consultar spellbook; ritual de spell preparado segue regra normal.");

            // Background/species/feats/equipment
            case "multiple_backgrounds": return ok(id,"backgrounds","Personagem tem apenas um background.").put("max_backgrounds",1);
            case "naturally_stealthy_hide": return ok(id,"species_traits","Naturally Stealthy pode satisfazer pré-condições de Hide quando obscurecido por criatura maior, mas ainda exige Dexterity (Stealth) check.");
            case "archery_thrown_melee": return ok(id,"feats","Archery beneficia ataques com Ranged weapons; uma Melee weapon arremessada não ganha esse benefício só por ser arremessada.");
            case "charger_mounted": return ok(id,"feats","Charger funciona em controlled mount quando o movimento ocorre no seu turno e é seguido imediatamente pelo Attack; independent mount não satisfaz essa sequência.");
            case "crossbow_expert_light_requirement": return ok(id,"feats","Crossbow Expert permite múltiplos disparos conforme suas regras, mas o ataque extra da propriedade Light ainda exige arma diferente.");
            case "dual_wielder_heavy": return ok(id,"feats","Dual Wielder não permite contornar propriedade Two-Handed de armas Heavy.");
            case "dueling_with_shield": return ok(id,"feats","Dueling permite Shield, focus ou outro objeto não-weapon na outra mão.");
            case "great_weapon_fighting_extra_damage": return ok(id,"feats","Great Weapon Fighting altera apenas os damage dice da weapon, não dados extras de Divine Smite, Hex etc.");
            case "magic_initiate_own_class": return ok(id,"feats","Spellcaster pode escolher sua própria classe para Magic Initiate.");
            case "polearm_master_ability_mod": return ok(id,"feats","Ataque bônus de Polearm Master adiciona o ability modifier normalmente salvo regra específica que diga o contrário.");
            case "savage_attacker_extra_damage": return ok(id,"feats","Savage Attacker afeta apenas weapon damage dice, não dano extra de Sneak Attack/Divine Smite.");
            case "heavy_armor_dex": return ok(id,"equipment","Com treinamento apropriado, Heavy armor não penaliza Dex saves ou checks além de Stealth salvo outra regra.");
            case "shield_as_improvised_weapon_ac": return ok(id,"equipment","Atacar com Shield como improvised weapon não remove automaticamente seu bônus de AC.");

            // Spellcasting
            case "class_spell_identity": return ok(id,"spellcasting","Um spell pertence à classe quando está na spell list usada pela classe ou quando uma feature explicitamente o torna spell dessa classe.");
            case "aoe_origin_placement": return ok(id,"spellcasting","Caster escolhe o ponto de origem de AoE quando o spell permite, podendo posicioná-lo para poupar aliados dentro das regras.");
            case "permanent_spell_dispel": return ok(id,"spellcasting","Efeito de spell tornado permanente continua dispensável se o spell não disser o contrário.");
            case "ritual_same_spell": return ok(id,"spellcasting","Ritual não é versão separada a aprender; é modo de conjurar spell com Ritual tag.");
            case "prepare_spells_timing": return ok(id,"spellcasting","Prepared spells só mudam nos momentos e quantidades definidos pela feature de spellcasting; não em lotes arbitrários durante a aventura.");
            case "touch_range_melee_attack": return ok(id,"spellcasting","Range Touch não transforma um spell em melee attack; o spell precisa dizer que há ataque.");
            case "spell_attack_critical": return ok(id,"spellcasting","Spell attack pode gerar Critical Hit porque usa attack roll.").put("critical_allowed",true);
            case "multi_target_spell_resolution": return ok(id,"spellcasting","Ataques múltiplos de um spell são resolvidos consecutivamente com seus próprios attack/damage rolls.");
            case "spell_attack_vs_attack_action": return ok(id,"spellcasting","Fazer spell attack não equivale a tomar Attack action; Extra Attack não concede conjurações adicionais salvo feature explícita.");
            case "melee_spell_opportunity_attack": return ok(id,"spellcasting","Opportunity Attack não permite conjurar melee spell attack salvo feature que altere isso, como War Caster.");
            case "spell_target_types": return ok(id,"spellcasting","Restrições de target type do spell são vinculantes; spell que diz creature não passa a atingir object sem regra específica.");
            case "slotless_spell_level": return ok(id,"spellcasting","Spell conjurado sem slot usa seu menor nível possível, salvo texto que especifique outra versão.");
            case "spells_per_turn": return ok(id,"spellcasting","Não há número fixo de spells por turno; action economy e a regra de gasto de spell slot no turno limitam o total.");
            case "reaction_spell_on_own_turn": return ok(id,"spellcasting","Reaction spell pode ocorrer no próprio turno, respeitando a limitação de gasto de spell slot no turno.");
            case "ritual_concentration_casting": return ok(id,"spellcasting","Conjuração com duração maior que uma action exige Concentration durante a conjuração; depois, somente se Duration exigir.");
            case "material_consumption": return ok(id,"spellcasting","Material component só é consumido quando a descrição do spell diz que é consumido.");
            case "focus_consumed_or_costly_material": return ok(id,"spellcasting","Spellcasting Focus não substitui componente com custo nem componente consumido.");
            case "focus_somatic_hand": return ok(id,"spellcasting","A mesma mão pode manipular Material/Focus e Somatic quando o spell tem Material; spell apenas Somatic requer mão livre salvo exceção.");
            case "concentration_range_los": return ok(id,"spellcasting","Manter Concentration não exige permanecer em range ou line of sight salvo regra específica.");
            case "temp_hp_concentration_dc": {
                JSONObject ruling = ok(id,"spellcasting","Temporary HP absorvem dano mas o conjurador ainda o sofreu para Concentration; 30 de dano exigem DC 15. Arcane Ward pode absorver dano no lugar do conjurador.");
                ruling.put("example_incoming_damage",30).put("example_concentration_dc",15);
                if (in.has("damage_taken")) {
                    int damage = in.optInt("damage_taken",-1);
                    if (damage < 0) return blocked(id).put("reason","invalid_damage_taken");
                    if (damage == 0) return ruling.put("requires_check",false);
                    return ruling.put("requires_check",true)
                            .put("calculated_dc",Math.max(10,damage/2));
                }
                return ruling;
            }
            case "dismiss_spell": return ok(id,"spellcasting","Spell não-instantâneo pode ser encerrado pelo caster conforme as regras atuais de Duration, salvo restrição específica.");
            case "animate_objects_time_stop": return ok(id,"spellcasting","Conjurar Animate Objects durante Time Stop não encerra por si só; comandar as criaturas pode encerrar por afetar outras criaturas.");
            case "armor_of_agathys_temp_hp_source": return ok(id,"spellcasting","Armor of Agathys requer que o alvo tenha Temporary HP; eles não precisam vir do próprio spell.");
            case "barkskin_ac_floor": return ok(id,"spellcasting","Barkskin fornece piso de AC 17; use outra fórmula/modificadores somente quando resultarem em AC maior.").put("ac_floor",17);
            case "chill_touch_regeneration": return ok(id,"spellcasting","Efeito que impede recuperar HP de Chill Touch continua mesmo quando o alvo não sofre Necrotic damage por imunidade.");
            case "cloud_of_daggers_grid": return manual(id,"spellcasting","Um cubo de 5 ft pode cobrir partes de múltiplos quadrados; snapping à grid é opção do DM.");
            case "compelled_duel_teleport": return ok(id,"spellcasting","Compelled Duel impede aumentar a distância além do limite também via teleport, conforme o efeito.");
            case "disintegrate_relentless_endurance": return ok(id,"spellcasting","Se Relentless Endurance troca 0 HP por 1 antes da condição de desintegração se consolidar, a criatura não vira pó.");
            case "dispel_magic_nonspell": return ok(id,"spellcasting","Dispel Magic encerra spells; não remove efeito mágico não criado por spell salvo texto específico/decisão do DM.");
            case "instantaneous_spell_dispel": return ok(id,"spellcasting","Resultado de spell Instantaneous não permanece sustentado por magia e não pode ser depois dispensado apenas por sua origem mágica.");
            case "readied_dispel_magic": return ok(id,"spellcasting","Dispel Magic preparado não substitui Counterspell; ele pode remover um spell logo após o spell entrar em vigor.");
            case "dispel_globe_invulnerability": return ok(id,"spellcasting","Dispel Magic pode dissipar a barreira Globe of Invulnerability, mas não dissipa automaticamente efeitos mágicos ativos dentro dela.").put("dispels_globe_barrier",true).put("dispels_other_inside_effects",false);
            case "dispel_bless_one_target": return ok(id,"spellcasting","Dispel Magic remove o efeito de Bless do alvo afetado, não automaticamente de todos os alvos do mesmo casting.");
            case "familiar_help": return ok(id,"spellcasting","Find Familiar não permite atacar, mas familiar pode usar Help.");
            case "familiar_sneak_attack_ally": return ok(id,"spellcasting","Familiar é ally e pode satisfazer requisito de presença de aliado para Sneak Attack quando as demais condições valem.");
            case "guardian_of_faith_damage_pool": return ok(id,"spellcasting","Guardian of Faith reduz seu total restante pelo dano efetivamente causado após o save.");
            case "heroism_temp_hp_stack": return ok(id,"spellcasting","Temporary HP de Heroism não se acumulam entre rodadas; Temporary HP não acumulam salvo texto específico.");
            case "heroism_existing_frightened": return ok(id,"spellcasting","Heroism suprime Frightened durante a imunidade; a condição pode retornar depois se ainda não tiver expirado/removido.");
            case "mage_armor_shield": return ok(id,"spellcasting","Mage Armor funciona com Shield.").put("shield_allowed",true);
            case "mage_armor_unarmored_defense": return ok(id,"spellcasting","Mage Armor e Unarmored Defense são fórmulas alternativas de AC e não se somam.");
            case "mage_armor_name_is_not_literal_armor": return ok(id,"spellcasting","O nome Mage Armor é figurativo: o efeito do spell determina a regra, não a palavra armor no nome.").put("counts_as_worn_armor",false);
            case "minor_illusion_move_object": return ok(id,"spellcasting","Objeto criado por Minor Illusion não se move por si só.");
            case "minor_illusion_fog": return ok(id,"spellcasting","Minor Illusion cria objeto/som nos limites do spell, não efeito atmosférico como fog cloud.");
            case "planar_binding_summon": return ok(id,"spellcasting","Planar Binding não invoca a criatura; tenta vincular criatura já presente e em alcance.");
            case "polymorph_concentration": return ok(id,"spellcasting","Polymorph impede spellcasting da forma, mas não encerra automaticamente Concentration iniciada antes.");
            case "polymorph_willing_mental_stats": return ok(id,"spellcasting","Alvo willing de Polymorph recebe as game statistics da nova forma, inclusive atributos mentais quando o spell assim determina.");
            case "polymorph_leftover_resistance": return ok(id,"spellcasting","Dano excedente após reverter Polymorph é processado pela forma verdadeira, inclusive Resistance aplicável.");
            case "polymorph_other_effects": return ok(id,"spellcasting","Polymorph substitui as game statistics indicadas, não apaga outros spell effects ativos no alvo.");
            case "prestidigitation_trinket": return manual(id,"spellcasting","Natureza do nonmagical trinket de Prestidigitation é limitada a pequeno objeto/bauble e depende da escolha do caster/DM.");
            case "sanctuary_grapple_shove": return ok(id,"spellcasting","Grapple/Shove sem attack roll, spell ou dano não encerram Sanctuary por si sós.");
            case "shillelagh_polearm_master_die": return ok(id,"spellcasting","Ataque bônus de Polearm Master usa d4 mesmo com Quarterstaff afetado por Shillelagh.").put("damage_die","1d4");
            case "storm_of_vengeance_rounds": return ok(id,"spellcasting","O efeito de cada nova rodada de Storm of Vengeance substitui o efeito específico da rodada anterior conforme o spell.");
            case "suggestion_verbal_component": return ok(id,"spellcasting","A sentença inteligível de Suggestion é separada do Verbal component místico.");
            case "telekinesis_resilient_sphere": return ok(id,"spellcasting","Telekinesis já sustentada em alvo pode continuar; a Sphere impede escolher novos alvos externos por Cover.");
            case "true_strike_interactions": return ok(id,"spellcasting","True Strike usa Magic action, então não é Extra Attack/OA comum; o ataque do spell pode receber Sneak Attack se satisfizer seus requisitos.");
            case "unseen_servant_ally": return ok(id,"spellcasting","Unseen Servant não é creature/enemy/ally para features que exigem essas categorias.");
            case "wish_stress": return ok(id,"spellcasting","Duplicar spell de nível 0–8 com Wish não causa o stress especial; outros usos causam stress e o risco indicado pelo spell.").put("loss_risk_percent",33);

            // Monsters
            case "monster_opportunity_attack": return ok(id,"monsters","Monster Opportunity Attack usa um único melee attack elegível do statblock ou Unarmed Strike; Multiattack e efeitos sem melee attack roll não qualificam.");
            case "monster_languages": return ok(id,"monsters","A lista de languages do PHB não é exaustiva; statblocks podem usar languages adicionais.");
            case "monster_dex_melee_finesse": return ok(id,"monsters","Usar Dexterity em melee attack não concede Finesse; a propriedade precisa constar na weapon/regra.");
            case "magic_resistance_channel_divinity": return ok(id,"monsters","Magic Resistance aplica-se a magical saving-throw effects de Channel Divinity quando o efeito é mágico.");
            case "shadow_critical_strength_drain": return ok(id,"monsters","Critical Hit duplica damage dice; redução de Strength que não é dano não é duplicada como damage die.");
            case "vampire_humanoid_spells": return ok(id,"monsters","Vampire não é Humanoid; spells limitados a Humanoid não o afetam apenas por aparência.");
            case "burrow_drag_grappled": return ok(id,"monsters","Criatura só arrasta grappled target para dentro do solo ao burrow se sua habilidade permitir deixar túnel/espaço para o alvo.");
            case "hydra_reactive_heads_oa": return ok(id,"monsters","Reactive Heads pode permitir múltiplas Opportunity Attacks contra a mesma criatura que provoque, conforme as Reactions disponíveis pela trait.");

            // Magic items
            case "magic_item_spell_save_dc": return ok(id,"magic_items","Spell de magic item usa DC 8 + spellcasting ability modifier + proficiency; sem spellcasting ability, modifier é 0, salvo item definir DC própria.");
            case "administer_potion_unconscious": return ok(id,"magic_items","Potion pode ser administrada a outra criatura, inclusive Unconscious, usando a ação/Bonus Action prevista pela regra atual.");
            case "potion_concentration": return ok(id,"magic_items","Potion exige Concentration apenas se sua descrição disser ou se duplicar spell que exige Concentration.");
            case "spell_scroll_class_list": {
                JSONObject ruling = ok(id,"magic_items","Scroll comum pode ser tentado por leitor alfabetizado; Spell Scroll exige o spell na lista de magia usada pela classe/subclasse.").put("general_scroll_requires_spell_list",false).put("spell_scroll_requires_spell_list",true);
                if (in.has("scroll_kind")) {
                    String kind = norm(in.optString("scroll_kind"));
                    if ("general_scroll".equals(kind)) {
                        if (!in.has("literate")) return ruling.put("can_attempt_activation", JSONObject.NULL);
                        return ruling.put("can_attempt_activation",in.optBoolean("literate"));
                    }
                    if ("spell_scroll".equals(kind)) {
                        if (!in.has("spell_on_class_list")) return ruling.put("can_read_spell_scroll", JSONObject.NULL);
                        return ruling.put("can_read_spell_scroll",in.optBoolean("spell_on_class_list"));
                    }
                    return blocked(id).put("reason","unknown_scroll_kind");
                }
                return ruling;
            }
            case "ring_bracers_ac_stack": return ok(id,"magic_items","Ring of Protection e Bracers of Defense acumulam porque são efeitos diferentes, respeitando restrições de attunement/duplicata.");
            case "magic_bow_nonmagic_ammo": return ok(id,"magic_items","Ataque feito por magic ranged weapon é mágico mesmo com ammunition não-mágica.");
            case "thrown_magic_weapon_return": return ok(id,"magic_items","Magic weapon arremessada só retorna automaticamente se sua descrição disser que retorna.");
            case "sphere_annihilation_tiny_hut": return ok(id,"magic_items","Sphere of Annihilation pode entrar/destruir a barreira de Tiny Hut conforme a interação oficial.");
            case "magic_shield_must_don": return ok(id,"magic_items","Bônus mágico de Shield exige que o Shield esteja donned; apenas segurá-lo não concede o bônus.");

            // Meta/authority
            case "dm_final_say": return manual(id,"rules_role_meta","Sage Advice fornece rulings oficiais, mas o DM continua sendo a autoridade final de adjudicação da mesa.");
            default: return blocked(id);
        }
    }
}
