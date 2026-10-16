package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.*;

/**
 * PHB 2024 subclass definitions and selection gates, NOT a complete effects executor.
 * The feature descriptors do not apply damage, resources, conditions or combat actions.
 * Individual subclass features require separately tested host/SQLite implementations
 * before their mechanical coverage can be marked VERIFIED_E2E.
 */
public final class Core2024SubclassRuntimeEngine {

    public static final String SOURCE_BOOK = "Player's Handbook 2024";

    public static final class SubclassFeature {
        public final int level;
        public final String id;
        public final String name;
        public final String activation; // bonus_action, action, reaction, passive, rest
        public final String recharge;   // short_rest, long_rest, none
        public final String description;
        public final JSONObject ruleData;

        public SubclassFeature(int level, String id, String name, String activation,
                               String recharge, String description, JSONObject ruleData) {
            this.level = level;
            this.id = id;
            this.name = name;
            this.activation = activation;
            this.recharge = recharge;
            this.description = description;
            this.ruleData = ruleData == null ? new JSONObject() : ruleData;
        }
    }

    public static final class SubclassDef {
        public final String className;
        public final String name;
        public final String nameEn;
        public final String locator;
        public final List<SubclassFeature> features = new ArrayList<>();

        public SubclassDef(String className, String name, String nameEn, String locator) {
            this.className = className;
            this.name = name;
            this.nameEn = nameEn;
            this.locator = locator;
        }

        public SubclassDef addFeature(SubclassFeature f) {
            this.features.add(f);
            return this;
        }
    }

    private static final Map<String, SubclassDef> SUBCLASSES_BY_KEY = new LinkedHashMap<>();
    private static final Map<String, List<SubclassDef>> SUBCLASSES_BY_CLASS = new LinkedHashMap<>();

    static {
        // ------------------------------------------------------------------------
        // 1. BÁRBARO (4 subclasses)
        // ------------------------------------------------------------------------
        reg("Bárbaro", "Caminho do Berserker", "Path of the Berserker", "PHB 2024 p. 56",
            f(3, "frenzy", "Frenesi", "passive_rage", "none", "Aumenta dano em fúria", json("extra_damage_dice", "d6", "bonus_damage_in_rage", true)),
            f(6, "mindless_rage", "Fúria Descerebrada", "passive", "none", "Imunidade a Encantado e Amedrontado durante Fúria", json("immune_conditions", new JSONArray().put("charmed").put("frightened"))),
            f(10, "retaliation", "Retaliação", "reaction", "none", "Ataque corpo a corpo como reação a dano", json("reaction_attack_on_damage", true)),
            f(14, "intimidating_presence", "Presença Intimidadora", "bonus_action", "long_rest", "Amedronta criaturas em área", json("radius_ft", 30)));

        reg("Bárbaro", "Caminho do Coração Selvagem", "Path of the Wild Heart", "PHB 2024 p. 57",
            f(3, "rage_of_the_wilds", "Fúria das Selvas", "bonus_action_rage", "none", "Escolha entre Urso, Águia ou Lobo", json("aspects", new JSONArray().put("urso").put("aguia").put("lobo"))),
            f(6, "aspect_of_the_beast", "Aspecto da Besta", "passive", "none", "Bênçãos utilitárias selvagens", json("passive_benefit", true)),
            f(10, "nature_speaker", "Porta-Voz da Natureza", "action", "short_rest", "Conjuração ritual de magias naturais", json("rituals", true)),
            f(14, "totemic_attunement", "Sintonização Totêmica", "passive_rage", "none", "Poderes supremos de fúria selvagem", json("supreme_rage", true)));

        reg("Bárbaro", "Caminho do Fanático", "Path of the Zealot", "PHB 2024 p. 58",
            f(3, "divine_fury", "Fúria Divina", "passive_rage_hit", "none", "Dano radiante ou necrótico extra", json("damage_dice", "1d6", "half_barbarian_level_bonus", true)),
            f(3, "warrior_of_the_gods", "Guerreiro dos Deuses", "passive", "none", "Ressurreição sem custo de componentes", json("waive_revive_material_cost", true)),
            f(6, "fanatical_focus", "Foco Fanático", "reaction", "long_rest", "Rerolagem de salvaguarda em fúria", json("reroll_save", true)),
            f(10, "zealous_presence", "Presença Zelosa", "bonus_action", "long_rest", "Vantagem em ataques e salvaguardas para aliados", json("range_ft", 60)));

        reg("Bárbaro", "Caminho da Árvore do Mundo", "Path of the World Tree", "PHB 2024 p. 59",
            f(3, "vitality_of_the_tree", "Vitalidade da Árvore", "bonus_action_rage", "none", "PV temporários concedidos pela Yggdrasil", json("temp_hp_formula", "barbarian_level")),
            f(6, "branches_of_the_tree", "Ramos da Árvore", "reaction", "none", "Teleporte e reposicionamento forçado de criaturas", json("teleport_range_ft", 30)),
            f(10, "battering_roots", "Raízes Devastadoras", "passive", "none", "Aumento de alcance para armas pesadas e de versatilidade", json("reach_bonus_ft", 10)),
            f(14, "travel_along_the_tree", "Viagem pela Árvore", "action", "long_rest", "Teleporte planar e portal entre dimensões", json("planar_travel", true)));

        // ------------------------------------------------------------------------
        // 2. BARDO (4 subclasses)
        // ------------------------------------------------------------------------
        reg("Bardo", "Colégio da Dança", "College of Dance", "PHB 2024 p. 64",
            f(3, "dazzling_footwork", "Passos Deslumbrantes", "bonus_action", "none", "CA sem armadura baseada em Carisma e ataques desarmados rítmicos", json("ac_bonus_charisma", true)),
            f(6, "inspiring_movement", "Movimento Inspirador", "reaction", "none", "Reação para reposicionamento ágil sem ataques de oportunidade", json("move_without_opportunity", true)),
            f(14, "leading_the_dance", "Liderando a Dança", "action", "long_rest", "Dano partilhado e aceleração de iniciativa de aliados", json("dance_lead", true)));

        reg("Bardo", "Colégio do Glamour", "College of Glamour", "PHB 2024 p. 65",
            f(3, "beguiling_magic", "Magia Cativante", "passive_cast", "none", "Encantamento ou medo após conjurar magias de Ilusão/Encantamento", json("auto_charm_dc", true)),
            f(3, "mantle_of_inspiration", "Manto da Inspiração", "bonus_action", "none", "PV temporários e reposicionamento para múltiplos aliados", json("range_ft", 60)),
            f(6, "mantle_of_majesty", "Manto da Majestade", "bonus_action", "long_rest", "Conjura Comando a cada turno sem gastar espaços", json("command_free", true)),
            f(14, "unbreakable_majesty", "Majestade Inquebrável", "bonus_action", "short_rest", "Criaturas são forçadas a passar em Carisma antes de atacar", json("defensive_majesty", true)));

        reg("Bardo", "Colégio da Sabedoria", "College of Lore", "PHB 2024 p. 66",
            f(3, "cutting_words", "Palavras Cortantes", "reaction", "none", "Reduz jogadas de ataque, dano ou testes de perícia com Inspiração", json("subtract_bardic_die", true)),
            f(3, "bonus_proficiencies_lore", "Proficiências Bônus (Sabedoria)", "passive", "none", "3 perícias adicionais à escolha", json("extra_skills", 3)),
            f(6, "magical_discoveries", "Descobertas Mágicas", "passive", "none", "Acesso adiantado a magias de qualquer lista", json("magical_secrets_early", 2)),
            f(14, "peerless_skill", "Habilidade Inigualável", "reaction", "none", "Aplica Inspiração ao próprio teste sem gastar se falhar", json("peerless_die", true)));

        reg("Bardo", "Colégio do Valor", "College of Valor", "PHB 2024 p. 67",
            f(3, "combat_inspiration", "Inspiração de Combate", "passive", "none", "Adiciona dado de inspiração ao dano da arma ou CA", json("combat_die_use", true)),
            f(3, "martial_training", "Treinamento Marcial", "passive", "none", "Proficiência com armaduras médias, escudos e armas marciais", json("martial_proficiencies", true)),
            f(6, "extra_attack_valor", "Ataque Extra (Valor)", "passive", "none", "Permite substituir um ataque por um truque", json("cantrip_substitution", true)),
            f(14, "battle_magic", "Magia de Batalha", "passive", "none", "Ataque com arma como ação bônus após conjurar magia de nível", json("bonus_attack_after_spell", true)));

        // ------------------------------------------------------------------------
        // 3. CLÉRIGO (4 subclasses)
        // ------------------------------------------------------------------------
        reg("Clérigo", "Domínio da Vida", "Life Domain", "PHB 2024 p. 72",
            f(3, "disciple_of_life", "Discípulo da Vida", "passive_heal", "none", "Cura adicional igual a 2 + nível da magia", json("bonus_heal_formula", "2 + spell_level")),
            f(3, "preserve_life", "Preservar a Vida", "action", "short_rest", "Canalizar Divindade: restaura PV até metade do máximo em área", json("channel_divinity", true)),
            f(6, "blessed_healer", "Curandeiro Abençoado", "passive", "none", "Cura a si mesmo ao curar outro alvo", json("self_heal_on_ally_heal", true)),
            f(17, "supreme_healing", "Cura Suprema", "passive", "none", "Dados de cura sempre maximizados", json("maximize_healing_dice", true)));

        reg("Clérigo", "Domínio da Luz", "Light Domain", "PHB 2024 p. 73",
            f(3, "radiance_of_the_dawn", "Resplendor do Alvorecer", "action", "short_rest", "Canalizar Divindade: dispersa escuridão e causa dano radiante", json("damage_dice", "2d10 + cleric_level")),
            f(3, "warding_flare", "Clarão Protetor", "reaction", "long_rest", "Desvantagem em ataques inimigos via luz ofuscante", json("flare_disadvantage", true)),
            f(6, "improved_warding_flare", "Clarão Protetor Aprimorado", "passive", "none", "Clarão protege aliados adjacentes", json("protect_allies", true)),
            f(17, "corona_of_light", "Coroa de Luz", "action", "long_rest", "Aura radiante que impõe desvantagem em testes vs fogo/radiante", json("corona_aura", true)));

        reg("Clérigo", "Domínio da Trapaça", "Trickery Domain", "PHB 2024 p. 74",
            f(3, "invoke_duplicity", "Invocar Duplicidade", "bonus_action", "short_rest", "Canalizar Divindade: cria cópia ilusória que projeta magias e dá vantagem", json("illusion_token", true)),
            f(3, "blessing_of_the_trickster", "Bênção do Trapaceiro", "action", "none", "Concede vantagem em testes de Furtividade a outra criatura", json("stealth_advantage", true)),
            f(6, "tricksters_transposition", "Transposição do Trapaceiro", "bonus_action", "none", "Teleporte trocando de lugar com a duplicidade", json("teleport_swap", true)),
            f(17, "improved_duplicity", "Duplicidade Aprimorada", "passive", "none", "Cria até 4 duplicidades simultâneas com cura em área", json("multi_duplicity", 4)));

        reg("Clérigo", "Domínio da Guerra", "War Domain", "PHB 2024 p. 75",
            f(3, "war_priest", "Sacerdote da Guerra", "bonus_action", "short_rest", "Ataque adicional como ação bônus limitado a Sabedoria", json("extra_bonus_attacks", true)),
            f(3, "guided_strike", "Ataque Guiado", "reaction", "short_rest", "Canalizar Divindade: +10 na jogada de ataque", json("flat_attack_bonus", 10)),
            f(6, "war_gods_blessing", "Bênção do Deus da Guerra", "reaction", "none", "Concede o bônus de +10 de Ataque Guiado a um aliado", json("ally_guided_strike", true)),
            f(17, "avatar_of_battle", "Avatar da Batalha", "passive", "none", "Resistência permanente a dano contundente, perfurante e cortante não mágico", json("avatar_resistances", true)));

        // ------------------------------------------------------------------------
        // 4. DRUIDA (4 subclasses)
        // ------------------------------------------------------------------------
        reg("Druida", "Círculo da Terra", "Circle of the Land", "PHB 2024 p. 82",
            f(3, "land_aid", "Auxílio da Terra", "action", "short_rest", "Vitalidade floral que cura aliados e machuca inimigos", json("damage_heal_blend", true)),
            f(3, "natural_recovery_druid", "Recuperação Natural", "short_rest", "long_rest", "Recupera espaços de magia durante descanso curto", json("recover_slots", true)),
            f(6, "natural_sanctuary", "Santuário Natural", "passive", "none", "Proteção contra terreno difícil e elementos", json("terrain_ward", true)),
            f(14, "natures_ward_circle", "Proteção da Natureza", "passive", "none", "Imunidade a veneno e paralisia", json("immune_poison", true)));

        reg("Druida", "Círculo da Lua", "Circle of the Moon", "PHB 2024 p. 83",
            f(3, "combat_wild_shape", "Forma Selvagem de Combate", "bonus_action", "none", "Forma selvagem rápida com CA aprimorada e PV temporários altos", json("moon_hp_bonus", 3)),
            f(3, "moonbeam_attunement", "Raios Lunares Selvagens", "passive", "none", "Dano radiante incorporado a ataques naturais", json("radiant_beast_attacks", true)),
            f(6, "moonlight_step", "Passo do Luar", "bonus_action", "short_rest", "Teleporte de 30 pés sob a luz da lua", json("teleport_ft", 30)),
            f(14, "lunar_radiance", "Resplendor Lunar", "passive", "none", "Aura radiante em forma selvagem", json("radiant_aura", true)));

        reg("Druida", "Círculo do Mar", "Circle of the Sea", "PHB 2024 p. 84",
            f(3, "wrath_of_the_sea", "Fúria do Mar", "bonus_action", "none", "Aura de tempestade costeira com dano de frio/trovão e empurrão", json("sea_aura_damage", "cold_thunder")),
            f(6, "aquatic_affinity", "Afinidade Aquática", "passive", "none", "Velocidade de natação igual ao deslocamento e respiração sob a água", json("swim_speed_match", true)),
            f(10, "stormborn", "Nascido da Tempestade", "passive", "none", "Voo com deslocamento de natação e resistência a dano", json("flight_in_aura", true)),
            f(14, "oceanic_gift", "Dádiva Oceânica", "action", "long_rest", "Transfere a aura do mar para aliados", json("share_aura", true)));

        reg("Druida", "Círculo das Estrelas", "Circle of Stars", "PHB 2024 p. 85",
            f(3, "star_map", "Mapa Estelar", "passive", "none", "Guia astrológico que concede Orientação e Raio de Fogo/Guia gratuito", json("star_cantrips", true)),
            f(3, "starry_form", "Forma Estelar", "bonus_action", "none", "Constelações de arqueiro, cálice ou dragão", json("constellations", new JSONArray().put("archer").put("chalice").put("dragon"))),
            f(6, "cosmic_omen", "Presságio Cósmico", "reaction", "long_rest", "Soma ou subtrai 1d6 em rolagens de d20 (Weal/Woe)", json("omen_d6", true)),
            f(14, "full_of_stars", "Cheio de Estrelas", "passive", "none", "Forma estelar concede resistência física", json("star_resistances", true)));

        // ------------------------------------------------------------------------
        // 5. GUERREIRO (4 subclasses)
        // ------------------------------------------------------------------------
        reg("Guerreiro", "Mestre da Batalha", "Battle Master", "PHB 2024 p. 92",
            f(3, "combat_superiority", "Superioridade em Combate", "short_rest", "none", "Dados de Superioridade d8 para manobras de manobra tática", json("dice_count", 4, "dice_size", "d8")),
            f(3, "student_of_war", "Estudioso da Guerra", "passive", "none", "Proficiência em ferramentas de artesão", json("artisan_tool_prof", true)),
            f(7, "know_your_enemy", "Conheça Seu Inimigo", "action", "none", "Análise de atributos e defesas do oponente", json("enemy_assessment", true)),
            f(15, "relentless_superiority", "Superioridade Implacável", "passive", "none", "Regenera 1 dado de superioridade ao rolar iniciativa se zerado", json("recharge_on_initiative", true)));

        reg("Guerreiro", "Campeão", "Champion", "PHB 2024 p. 93",
            f(3, "improved_critical", "Crítico Aprimorado", "passive", "none", "Acertos críticos em 19-20", json("crit_range_min", 19)),
            f(3, "remarkable_athlete", "Atleta Notável", "passive", "none", "Vantagem em iniciativa e salto aprimorado", json("initiative_advantage", true)),
            f(7, "additional_fighting_style", "Estilo de Luta Adicional", "passive", "none", "Segundo estilo de combate", json("extra_style", true)),
            f(18, "survivor", "Sobrevivente", "passive", "none", "Regeneração passiva de PV no início de cada turno abaixo da metade", json("survivor_regen", true)));

        reg("Guerreiro", "Cavaleiro Arcano", "Eldritch Knight", "PHB 2024 p. 94",
            f(3, "weapon_bond", "Vínculo com Arma", "bonus_action", "none", "Arma não pode ser desarmada e é invocada com ação bônus", json("recall_weapon", true)),
            f(3, "spellcasting_ek", "Conjuração de Cavaleiro Arcano", "action", "long_rest", "Magias de Mago com Inteligência", json("ek_spells", true)),
            f(7, "war_magic_ek", "Magia de Guerra", "passive", "none", "Substitui um ataque por um truque", json("cantrip_attack_swap", true)),
            f(15, "arcane_charge", "Investida Arcana", "passive_action_surge", "none", "Teleporte de 30 pés ao usar Surto de Ação", json("surge_teleport_ft", 30)));

        reg("Guerreiro", "Psi Guerreiro", "Psi Warrior", "PHB 2024 p. 95",
            f(3, "psionic_power_warrior", "Poder Psiônico", "short_rest", "none", "Dados de Energia Psiônica d6 baseados em Inteligência", json("psionic_dice", "d6")),
            f(7, "telekinetic_movement", "Movimento Telecinético", "action", "short_rest", "Move objetos ou aliados grandes até 30 pés", json("telekinetic_ft", 30)),
            f(10, "guarded_mind", "Mente Protegida", "passive", "none", "Resistência a dano psíquico e remoção de Encantado/Amedrontado", json("immune_psionic_control", true)),
            f(18, "telekinetic_master", "Mestre Telecinético", "action", "long_rest", "Conjura Telecinésia sem gastar espaços", json("free_telekinesis", true)));

        // ------------------------------------------------------------------------
        // 6. MONGE (4 subclasses)
        // ------------------------------------------------------------------------
        reg("Monge", "Caminho da Misericórdia", "Warrior of Mercy", "PHB 2024 p. 101",
            f(3, "hands_of_healing", "Mãos da Cura", "action_or_flurry", "short_rest", "Cura PV usando dado de Arte Marcial sem gastar ação inteira", json("heal_die", "martial_arts")),
            f(3, "hands_of_harm", "Mãos do Dano", "on_unarmed_hit", "short_rest", "Dano necrótico extra ao acertar golpe desarmado", json("harm_die", "martial_arts")),
            f(6, "physicians_touch", "Toque do Médico", "passive", "none", "Cura remove envenenado, cegueira, surdez ou paralisia", json("cleanse_conditions", true)),
            f(17, "hand_of_ultimate_mercy", "Mão da Misericórdia Suprema", "action", "long_rest", "Revive criatura recém-morta sem custo de componentes", json("ultimate_revive", true)));

        reg("Monge", "Caminho das Sombras", "Warrior of Shadow", "PHB 2024 p. 102",
            f(3, "shadow_arts", "Artes das Sombras", "action", "none", "Escuridão móvel que o monge enxerga perfeitamente", json("see_in_magical_darkness", true)),
            f(6, "shadow_step", "Passo das Sombras", "bonus_action", "none", "Teleporte de 60 pés entre sombras com vantagem no ataque seguinte", json("shadow_teleport_ft", 60)),
            f(11, "improved_shadow_step", "Passo das Sombras Aprimorado", "passive", "none", "Ataque desarmado gratuito após o teleporte sombrio", json("free_attack_on_step", true)),
            f(17, "cloak_of_shadows", "Manto de Sombras", "bonus_action", "none", "Forma sombria com resistência e intangibilidade", json("shadow_form", true)));

        reg("Monge", "Caminho dos Elementos", "Warrior of the Elements", "PHB 2024 p. 103",
            f(3, "elemental_attunement", "Sintonização Elemental", "bonus_action", "none", "Alcance aumentado em 10 pés com dano de fogo, frio, raio ou ácido", json("elemental_reach_bonus_ft", 10)),
            f(6, "environmental_stride", "Passada Ambiental", "passive", "none", "Deslocamento de escalada e natação igual ao deslocamento base", json("climb_swim_speed", true)),
            f(11, "stride_of_the_elements", "Passo dos Elementos", "passive", "none", "Voo temporário alimentado pelo Chi elemental", json("elemental_flight", true)),
            f(17, "elemental_epitome", "Epítome Elemental", "passive", "none", "Resistências e rajadas elementais massivas em fúria", json("epitome_damage", true)));

        reg("Monge", "Caminho da Mão Aberta", "Warrior of the Open Hand", "PHB 2024 p. 104",
            f(3, "open_hand_technique", "Técnica da Mão Aberta", "on_flurry_hit", "none", "Derrubar (caído), empurrar 15 pés ou impedir reações", json("riders", new JSONArray().put("prone").put("push15").put("no_reactions"))),
            f(6, "wholeness_of_body", "Integridade Corporal", "bonus_action", "long_rest", "Cura equivalente a 3x o nível do monge", json("self_heal_multiplier", 3)),
            f(11, "fleet_step", "Passo Ligeiro", "bonus_action", "none", "Passo do Vento livre sem custo de Foco", json("free_step_of_wind", true)),
            f(17, "quivering_palm", "Palma Vibratória", "action", "long_rest", "Vibrações letais: reduz o alvo a 0 PV na falha ou causa 10d10", json("quivering_damage", "10d10")));

        // ------------------------------------------------------------------------
        // 7. PALADINO (4 subclasses)
        // ------------------------------------------------------------------------
        reg("Paladino", "Juramento de Devoção", "Oath of Devotion", "PHB 2024 p. 110",
            f(3, "sacred_weapon", "Arma Sagrada", "action", "short_rest", "Canalizar Divindade: soma Carisma no ataque e causa dano radiante", json("charisma_to_attack", true)),
            f(3, "turn_the_unholy", "Expulsar o Profano", "action", "short_rest", "Canalizar Divindade: expulsa corruptores e mortos-vivos", json("turn_fiends_undead", true)),
            f(7, "aura_of_devotion", "Aura de Devoção", "passive", "none", "Imunidade a Encantado em 10 pés", json("aura_immune_charmed", true)),
            f(20, "holy_nimbus", "Nimbo Sagrado", "action", "long_rest", "Aura solar radiante com dano contínuo", json("nimbus_damage", 10)));

        reg("Paladino", "Juramento dos Anciões", "Oath of the Ancients", "PHB 2024 p. 111",
            f(3, "natures_wrath", "Ira da Natureza", "action", "short_rest", "Canalizar Divindade: vinhas que impedem movimento (contido)", json("ensnare_condition", "restrained")),
            f(7, "aura_of_warding", "Aura de Proteção Arcana", "passive", "none", "Resistência a dano de magias para o paladino e aliados", json("magic_damage_resistance", true)),
            f(15, "undying_sentinel", "Sentinela Imortal", "passive", "long_rest", "Ao cair a 0 PV, fica com 1 PV", json("survive_at_1_hp", true)),
            f(20, "elder_champion", "Campeão Ancião", "action", "long_rest", "Regeneração passiva de 10 PV por turno e conjurações aceleradas", json("regen_per_turn", 10)));

        reg("Paladino", "Juramento de Vingança", "Oath of Vengeance", "PHB 2024 p. 112",
            f(3, "vow_of_enmity", "Voto de Inimizade", "bonus_action", "short_rest", "Canalizar Divindade: vantagem em todos os ataques contra o alvo", json("advantage_vs_target", true)),
            f(7, "relentless_avenger", "Vingador Implacável", "reaction", "none", "Movimento após ataque de oportunidade sem gastar reação", json("chase_movement", true)),
            f(15, "soul_of_vengeance", "Alma da Vingança", "reaction", "none", "Contra-ataque quando o inimigo do voto ataca", json("retaliation_vs_enmity", true)),
            f(20, "avenging_angel", "Anjo Vingador", "action", "long_rest", "Asas com deslocamento de voo de 60 pés e aura de medo", json("fly_speed_ft", 60)));

        reg("Paladino", "Juramento de Glória", "Oath of Glory", "PHB 2024 p. 113",
            f(3, "peerless_athlete", "Atleta Inigualável", "bonus_action", "short_rest", "Canalizar Divindade: vantagem em Atletismo e bônus no salto", json("athletic_jump_bonus", true)),
            f(3, "inspiring_smite", "Golpe Inspirador", "bonus_action_smite", "short_rest", "Distribui PV temporários aos aliados após aplicar destruição", json("smite_temp_hp", true)),
            f(7, "aura_of_alacrity", "Aura de Alacridade", "passive", "none", "+10 pés de deslocamento para si e aliados na aura", json("speed_bonus_ft", 10)),
            f(20, "living_legend", "Lenda Viva", "action", "long_rest", "Vantagem em testes de Carisma e rerolagens automáticas de acertos", json("reroll_failed_attacks", true)));

        // ------------------------------------------------------------------------
        // 8. GUARDIÃO (RANGER) (4 subclasses)
        // ------------------------------------------------------------------------
        reg("Guardião", "Mestre das Feras", "Beast Master", "PHB 2024 p. 120",
            f(3, "primal_companion", "Companheiro Primitivo", "action", "none", "Invoca besta terrestre, aquática ou aérea que age no combate", json("companion_statblock", true)),
            f(7, "exceptional_training", "Treinamento Excepcional", "bonus_action", "none", "Besta usa Disparada, Desengajar ou Esquiva com ação bônus", json("companion_action_economy", true)),
            f(11, "bestial_fury", "Fúria Bestial", "passive", "none", "A besta ganha Ataque Extra e dano mágico", json("companion_extra_attack", true)),
            f(15, "share_spells_ranger", "Partilha de Magias", "passive", "none", "Magias conjuradas no ranger também afetam a besta", json("share_buffs", true)));

        reg("Guardião", "Andarilho Feérico", "Fey Wanderer", "PHB 2024 p. 121",
            f(3, "dreadful_strikes", "Golpes Pavorosos", "passive_hit", "none", "1d4 dano psíquico extra por turno em cada criatura atingida", json("extra_psychic_die", "1d4")),
            f(3, "otherworldly_glamour", "Glamour Sobrenatural", "passive", "none", "Soma Sabedoria em todos os testes de perícias de Carisma", json("wisdom_to_charisma_checks", true)),
            f(7, "beguiling_twist", "Torção Cativante", "reaction", "none", "Redireciona encantamento ou medo falho para outro alvo", json("redirect_charm_fear", true)),
            f(15, "mist_wanderer", "Andarilho das Brumas", "bonus_action", "long_rest", "Passo Nebuloso sem gastar espaços de magia", json("free_misty_steps", true)));

        reg("Guardião", "Caçador", "Hunter", "PHB 2024 p. 122",
            f(3, "hunters_prey", "Presa do Caçador", "passive_hit", "none", "Escolha entre Colosso Devastador (+1d8 dano) ou Matador de Hordas", json("hunter_d8_bonus", true)),
            f(7, "defensive_tactics", "Táticas Defensivas", "passive", "none", "Evasão contra ataques múltiplos ou salvaguardas", json("defensive_options", true)),
            f(11, "superior_hunters_defense", "Defesa Superior do Caçador", "reaction", "none", "Reação para esquivar ou contra-atacar", json("superior_defense", true)),
            f(15, "superior_hunters_prey", "Presa Superior do Caçador", "passive", "none", "Aplica dano de Presa do Caçador a alvos adicionais", json("multi_prey_damage", true)));

        reg("Guardião", "Perseguidor Sombrio", "Gloom Stalker", "PHB 2024 p. 123",
            f(3, "dread_ambusher", "Emboscador Pavoroso", "passive_initiative", "none", "Bônus na iniciativa, deslocamento no primeiro turno e dano extra", json("init_wisdom_bonus", true, "first_turn_speed_ft", 10)),
            f(3, "umbral_sight", "Visão Umbral", "passive", "none", "Visão no escuro de 60 pés e invisibilidade total para criaturas com visão no escuro", json("invisible_to_darkvision", true)),
            f(7, "iron_mind", "Mente de Ferro", "passive", "none", "Proficiência em salvaguardas de Sabedoria", json("wisdom_save_prof", true)),
            f(15, "shadowy_dodge", "Esquiva Sombria", "reaction", "none", "Impõe desvantagem em ataque contra você como reação", json("dodge_reaction", true)));

        // ------------------------------------------------------------------------
        // 9. LADINO (4 subclasses)
        // ------------------------------------------------------------------------
        reg("Ladino", "Trapaceiro Arcano", "Arcane Trickster", "PHB 2024 p. 130",
            f(3, "mage_hand_legerdemain", "Mãos Mágicas Trapaceiras", "action", "none", "Mãos mágicas invisíveis que furtam, destrancam e usam ferramentas", json("invisible_mage_hand", true)),
            f(3, "spellcasting_rogue", "Conjuração de Trapaceiro", "action", "long_rest", "Magias de Ilusão e Encantamento de Mago", json("rogue_spells", true)),
            f(9, "magical_ambush", "Emboscada Mágica", "passive", "none", "Desvantagem em salvaguardas se o ladino estiver escondido", json("hidden_spell_disadvantage", true)),
            f(17, "spell_thief", "Ladrão de Magias", "reaction", "long_rest", "Rouba a magia conjurada pelo inimigo e anula o efeito", json("steal_spell", true)));

        reg("Ladino", "Assassino", "Assassin", "PHB 2024 p. 131",
            f(3, "assassinate", "Assassinar", "passive_first_round", "none", "Vantagem em criaturas que ainda não agiram e acerto crítico garantido na surpresa", json("auto_crit_surprised", true)),
            f(3, "bonus_proficiencies_assassin", "Proficiências do Assassino", "passive", "none", "Proficiência em kit de disfarce e kit de veneno", json("poison_kit_prof", true)),
            f(9, "infiltration_expertise", "Perícia de Infiltração", "passive", "none", "Identidades falsas infalíveis e documentos forjados", json("false_identity", true)),
            f(17, "death_strike", "Golpe Mortal", "on_surprise_hit", "none", "Salvaguarda de Constituição ou dobra todo o dano do ataque furtivo", json("double_sneak_damage", true)));

        reg("Ladino", "Ladrão", "Thief", "PHB 2024 p. 132",
            f(3, "fast_hands", "Mãos Rápidas", "bonus_action", "none", "Usar ferramentas de ladrão, destrancar ou Usar Objeto com ação bônus", json("use_object_bonus_action", true)),
            f(3, "second_story_work", "Trabalho nas Alturas", "passive", "none", "Escalar não consome movimento extra e salto baseado em Destreza", json("climb_speed_match", true)),
            f(9, "supreme_sneak", "Furtividade Suprema", "passive", "none", "Vantagem em Furtividade se mover até metade do deslocamento", json("sneak_advantage", true)),
            f(17, "thiefs_reflexes", "Reflexos de Ladrão", "passive_first_round", "none", "Dois turnos completos na primeira rodada do combate", json("two_turns_round_one", true)));

        reg("Ladino", "Mente de Alma", "Soulknife", "PHB 2024 p. 133",
            f(3, "psychic_blades", "Lâminas Psíquicas", "action_attack", "none", "Lâminas de pura energia psíquica (1d6 dano psíquico)", json("blade_damage", "1d6", "damage_type", "psychic")),
            f(3, "psionic_power_rogue", "Poder Psiônico (Ladino)", "short_rest", "none", "Dados de energia psiônica d6 para testes e telepatia", json("psionic_dice_rogue", "d6")),
            f(9, "soul_blades", "Lâminas da Alma", "bonus_action", "none", "Teleporte arremessando a lâmina psíquica", json("blade_teleport", true)),
            f(17, "rend_mind", "Dilacerar Mente", "on_blade_hit", "long_rest", "Atordoa o alvo por 1 minuto em falha de Sabedoria", json("stun_duration_minutes", 1)));

        // ------------------------------------------------------------------------
        // 10. FEITICEIRO (4 subclasses)
        // ------------------------------------------------------------------------
        reg("Feiticeiro", "Feitiçaria Aberrante", "Aberrant Sorcery", "PHB 2024 p. 140",
            f(3, "psionic_spells_sorcerer", "Magias Psiônicas", "passive", "none", "Lista adicional de magias da mente e tentáculos", json("psionic_spell_list", true)),
            f(3, "telepathic_speech", "Discurso Telepático", "bonus_action", "none", "Conexão mental sem barreira de idioma a até 30 pés", json("telepathy_ft", 30)),
            f(6, "psionic_sorcery", "Feitiçaria Psiônica", "passive", "none", "Gasta pontos de feitiçaria em vez de espaços sem componentes verbais/somáticos", json("subtle_cast_with_sorcery_points", true)),
            f(14, "revelation_in_flesh", "Revelação na Carne", "bonus_action", "none", "Adaptações corporais: voo, natação, invisibilidade ou maleabilidade", json("flesh_mutations", true)));

        reg("Feiticeiro", "Feitiçaria Mecânica", "Clockwork Sorcery", "PHB 2024 p. 141",
            f(3, "clockwork_spells", "Magias Mecânicas", "passive", "none", "Magias de restauração, ordem e proteção de Mechanus", json("clockwork_list", true)),
            f(3, "restore_balance", "Restaurar o Equilíbrio", "reaction", "long_rest", "Anula vantagens e desvantagens de qualquer rolagem em 60 pés", json("neutralize_advantage", true)),
            f(6, "bastion_of_law", "Bastião da Lei", "action", "long_rest", "Escudo protetor que absorve dano gastando pontos de feitiçaria", json("ward_absorb_damage", true)),
            f(14, "trance_of_order", "Transe da Ordem", "bonus_action", "long_rest", "Nenhum resultado menor que 10 no d20 por 1 minuto", json("minimum_d20_result", 10)));

        reg("Feiticeiro", "Feitiçaria Dracônica", "Draconic Sorcery", "PHB 2024 p. 142",
            f(3, "draconic_resilience", "Resiliência Dracônica", "passive", "none", "CA base 13 + Destreza e +1 PV por nível de feiticeiro", json("base_ac", 13, "bonus_hp_per_level", 1)),
            f(6, "elemental_affinity", "Afinidade Elemental", "passive", "none", "Soma Carisma ao dano do elemento do ancestral e resistência a dano", json("charisma_to_element_damage", true)),
            f(14, "dragon_wings", "Asas de Dragão", "bonus_action", "none", "Asas dracônicas com deslocamento de voo igual ao deslocamento base", json("flight_speed_match", true)),
            f(18, "dragon_companion", "Companheiro Dragão", "action", "long_rest", "Invoca dragão jovem para lutar ao seu lado", json("summon_dragon", true)));

        reg("Feiticeiro", "Magia Selvagem", "Wild Magic Sorcery", "PHB 2024 p. 143",
            f(3, "wild_magic_surge", "Surto de Magia Selvagem", "on_spell_cast", "none", "Rolagem na tabela d100 de efeitos caóticos imprevisíveis", json("d100_surge_table", true)),
            f(3, "tides_of_chaos", "Ondas do Caos", "reaction", "long_rest", "Garante vantagem imediata em um teste de d20", json("free_advantage", true)),
            f(6, "bend_luck", "Dobrar a Sorte", "reaction", "none", "Gasta 2 pontos de feitiçaria para somar ou subtrair 1d4 de rolagem alheia", json("luck_d4", true)),
            f(14, "controlled_chaos", "Caos Controlado", "passive", "none", "Rola duas vezes na tabela de surto e escolhe o efeito desejado", json("roll_twice_surge", true)));

        // ------------------------------------------------------------------------
        // 11. BRUXO (4 subclasses)
        // ------------------------------------------------------------------------
        reg("Bruxo", "Patrono Arquifada", "Archfey Patron", "PHB 2024 p. 150",
            f(3, "fey_presence", "Presença Feérica", "action", "short_rest", "Enfeitiça ou amedronta criaturas em cubo de 10 pés", json("fey_charm_fear", true)),
            f(3, "misty_escape_archfey", "Fuga Nebulosa", "reaction", "short_rest", "Fica invisível e se teleporta ao sofrer dano", json("teleport_on_damage", true)),
            f(10, "beguiling_defenses", "Defesas Cativantes", "passive", "none", "Imunidade a Encantado e reflete tentativas de encanto", json("reflect_charm", true)),
            f(14, "dark_delirium", "Delírio Sombrio", "action", "short_rest", "Mergulha o alvo em ilusão aterrorizante solitária", json("illusion_prison", true)));

        reg("Bruxo", "Patrono Celestial", "Celestial Patron", "PHB 2024 p. 151",
            f(3, "healing_light", "Luz Curativa", "bonus_action", "long_rest", "Pool de d6 de cura equivalente a 1 + nível de bruxo", json("healing_pool_d6", true)),
            f(3, "sacred_flame_bonus", "Chama Sagrada Gratuita", "passive", "none", "Concede o truque Chama Sagrada e Luz sem contar nas conhecidas", json("free_cantrips", true)),
            f(6, "radiant_soul", "Alma Radiante", "passive", "none", "Resistência a dano radiante e bônus de Carisma ao dano de fogo/radiante", json("charisma_bonus_fire_radiant", true)),
            f(14, "searing_vengeance", "Vingança Incandescente", "reaction", "long_rest", "Levanta com metade dos PV e causa 2d8 + Carisma cegando oponentes", json("revive_burst_damage", "2d8")));

        reg("Bruxo", "Patrono Corruptor", "Fiend Patron", "PHB 2024 p. 152",
            f(3, "dark_ones_blessing", "Bênção do Obscuro", "passive_kill", "none", "Recebe PV temporários iguais a Carisma + nível de bruxo ao matar criatura", json("temp_hp_formula", "charisma + warlock_level")),
            f(6, "dark_ones_own_luck", "Sorte do Próprio Obscuro", "reaction", "short_rest", "Soma 1d10 em uma salvaguarda ou teste de atributo", json("luck_d10", true)),
            f(10, "fiendish_resilience", "Resiliência Infernal", "short_rest", "none", "Escolhe uma resistência a dano que dura até o próximo descanso", json("choose_resistance", true)),
            f(14, "hurl_through_hell", "Arremessar ao Inferno", "on_hit", "long_rest", "Bane o alvo aos Nove Infernos causando 10d10 de dano psíquico", json("hell_damage", "10d10")));

        reg("Bruxo", "Patrono Grande Antigo", "Great Old One Patron", "PHB 2024 p. 153",
            f(3, "awakened_mind", "Mente Desperta", "passive", "none", "Telepatia bidirecional a 30 pés sem dependência de linguagem", json("telepathy_30ft", true)),
            f(6, "entropic_ward", "Proteção Entrópica", "reaction", "short_rest", "Impõe desvantagem em ataque contra você e ganha vantagem no próximo contra ele", json("entropic_advantage", true)),
            f(10, "thought_shield", "Escudo do Pensamento", "passive", "none", "Pensamentos não podem ser lidos e reflete dano psíquico", json("reflect_psychic", true)),
            f(14, "create_thrall", "Criar Lacaio", "action", "long_rest", "Encanta permanentemente uma criatura incapacitada", json("permanent_thrall", true)));

        // ------------------------------------------------------------------------
        // 12. MAGO (4 subclasses)
        // ------------------------------------------------------------------------
        reg("Mago", "Escola de Abjuração", "School of Abjuration", "PHB 2024 p. 160",
            f(3, "arcane_ward", "Proteção Arcana", "passive_cast", "long_rest", "Escudo que absorve dano (2x nível + Inteligência) e recarrega com abjurações", json("ward_hp_formula", "2 * wizard_level + intelligence")),
            f(6, "projected_ward", "Proteção Projetada", "reaction", "none", "Usa a Proteção Arcana para absorver dano que um aliado sofreria", json("share_ward", true)),
            f(10, "spell_breaker", "Quebrador de Magias", "passive", "none", "Vantagem e adição do bônus de proficiência em Dissipar Magia e Contramágica", json("counterspell_bonus", true)),
            f(14, "spell_resistance_abjurer", "Resistência a Magia", "passive", "none", "Vantagem em todas as salvaguardas contra magias e resistência a dano de magia", json("spell_resistance", true)));

        reg("Mago", "Escola de Adivinhação", "School of Divination", "PHB 2024 p. 161",
            f(3, "portent", "Presságio", "long_rest", "none", "Rola 2d20 ao final do descanso longo para substituir rolagens durante o dia", json("portent_dice_count", 2)),
            f(6, "expert_divination", "Adivinhação Perita", "passive_cast", "none", "Recupera espaço de magia inferior ao conjurar adivinhação de 2º nível+", json("refund_spell_slot", true)),
            f(10, "the_third_eye", "O Terceiro Olho", "action", "short_rest", "Visão no escuro, visão etérea ou leitura de qualquer idioma", json("third_eye_modes", true)),
            f(14, "greater_portent", "Presságio Maior", "passive", "none", "Agora rola 3d20 de Presságio a cada descanso longo", json("portent_dice_count", 3)));

        reg("Mago", "Escola de Evocação", "School of Evocation", "PHB 2024 p. 162",
            f(3, "sculpt_spells", "Esculpir Magias", "passive_cast", "none", "Aliados em área de efeito de evocações não sofrem dano e passam automaticamente", json("protect_allies_in_aoe", true)),
            f(6, "potent_cantrip", "Truque Potente", "passive", "none", "Truques que exigem salvaguarda causam metade do dano mesmo em caso de sucesso", json("half_damage_on_cantrip_save", true)),
            f(10, "empowered_evocation", "Evocação Potencializada", "passive", "none", "Soma Inteligência a uma rolagem de dano de qualquer magia de evocação", json("add_intelligence_to_damage", true)),
            f(14, "overchannel", "Sobrecanalizar", "passive_cast", "long_rest", "Maximiza o dano de magias de 1º a 5º nível (custo de dano necrótico se repetir)", json("maximize_damage_dice", true)));

        reg("Mago", "Escola de Ilusão", "School of Illusion", "PHB 2024 p. 163",
            f(3, "improved_minor_illusion", "Ilusão Menor Aprimorada", "passive", "none", "Cria som e imagem simultaneamente no mesmo truque Ilusão Menor", json("sound_and_image", true)),
            f(6, "malleable_illusions", "Ilusões Maleáveis", "action", "none", "Modifica a natureza de uma ilusão já conjurada enquanto durar", json("alter_active_illusion", true)),
            f(10, "illusory_self", "Eu Ilusório", "reaction", "short_rest", "Cria duplicata ilusória que faz um ataque bem-sucedido errar automaticamente", json("auto_miss_reaction", true)),
            f(14, "illusory_reality", "Realidade Ilusória", "bonus_action", "none", "Transforma um objeto inanimado ilusório em matéria sólida real por 1 minuto", json("solid_illusion_minutes", 1)));
    }

    private static void reg(String cls, String name, String nameEn, String loc, SubclassFeature... feats) {
        SubclassDef def = new SubclassDef(cls, name, nameEn, loc);
        for (SubclassFeature f : feats) def.addFeature(f);
        SUBCLASSES_BY_KEY.put(norm(name), def);
        SUBCLASSES_BY_KEY.put(norm(nameEn), def);
        List<SubclassDef> list = SUBCLASSES_BY_CLASS.get(cls);
        if (list == null) {
            list = new ArrayList<>();
            SUBCLASSES_BY_CLASS.put(cls, list);
        }
        list.add(def);
    }

    private static SubclassFeature f(int level, String id, String name, String act,
                                     String rech, String desc, JSONObject data) {
        return new SubclassFeature(level, id, name, act, rech, desc, data);
    }

    private static JSONObject json(Object... pairs) {
        JSONObject o = new JSONObject();
        try {
            for (int i = 0; i < pairs.length; i += 2) {
                o.put(String.valueOf(pairs[i]), pairs[i + 1]);
            }
        } catch (Exception ignored) {}
        return o;
    }

    public static String norm(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return n.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }

    public static List<SubclassDef> getSubclassesForClass(String className) {
        List<SubclassDef> defs = SUBCLASSES_BY_CLASS.get(className);
        return defs == null ? Collections.<SubclassDef>emptyList() : Collections.unmodifiableList(defs);
    }

    public static SubclassDef getSubclass(String name) {
        return SUBCLASSES_BY_KEY.get(norm(name));
    }

    public static boolean hasSubclass(String name) {
        return SUBCLASSES_BY_KEY.containsKey(norm(name));
    }

    public static int totalSubclassesCount() {
        // Exactly 48 official subclasses across the 12 classes
        int count = 0;
        for (List<SubclassDef> list : SUBCLASSES_BY_CLASS.values()) {
            count += list.size();
        }
        return count;
    }

    /**
     * Valida se um personagem possui nível suficiente para desbloquear a subclasse.
     * Em D&D 2024, todas as 12 classes recebem subclasse no nível 3.
     */
    public static boolean isSubclassLevelValid(int level) {
        return level >= 3 && level <= 20;
    }

    /**
     * Selection only: does not claim that the selected subclass's features execute.
     * Avoids accepting a valid subclass from a different class or outside levels 3-20.
     */
    public static boolean isSubclassSelectionValid(String className, int level, String subclassName) {
        if (!isSubclassLevelValid(level)) return false;
        SubclassDef selected = getSubclass(subclassName);
        if (selected == null) return false;
        String expected = canonicalClass(selected.className);
        String provided = canonicalClass(className);
        return !expected.isEmpty() && expected.equals(provided);
    }

    public static String extractSubclass(JSONObject sheet) {
        if (sheet == null) return "";
        String s = sheet.optString("subclass_2024", sheet.optString("subclass_name", sheet.optString("subclass", "")));
        if (!s.isEmpty()) return s;
        String full = sheet.optString("class_name", "");
        int idx = full.indexOf(" — ");
        if (idx < 0) idx = full.indexOf(" - ");
        return idx > 0 ? full.substring(idx + 3).trim() : "";
    }

    public static int criticalThreshold(JSONObject sheet) {
        if (sheet == null) return 20;
        String sub = extractSubclass(sheet);
        if (sub.isEmpty()) return 20;
        String k = norm(sub);
        if (k.contains("campeao") || k.contains("champion")) {
            int lvl = sheet.optInt("level", 1);
            if (lvl >= 15) return 18;
            if (lvl >= 3) return 19;
        }
        return 20;
    }

    public static boolean applyToSheet(JSONObject sheet) {
        if (sheet == null) return false;
        String sub = extractSubclass(sheet);
        if (sub.isEmpty() || !hasSubclass(sub)) return false;
        int level = sheet.optInt("level", 1);
        if (!isSubclassLevelValid(level)) return false;
        SubclassDef def = getSubclass(sub);
        try {
            sheet.put("subclass_name", def.name);
            sheet.put("subclass_class", def.className);
            sheet.put("subclass_source", SOURCE_BOOK);
            sheet.put("subclass_runtime", "Core2024SubclassRuntimeEngine");
            sheet.put("subclass_mechanics_compiled", true);
            sheet.put("subclass_auto_rolls", false);
            JSONObject res = sheet.optJSONObject("subclass_resources");
            if (res == null) res = new JSONObject();
            String k = norm(def.name);
            DndRulesEngine rules = new DndRulesEngine();
            int pb = rules.proficiencyBonus(level);
            int wis = Math.max(1, rules.modifier(sheet.optInt("wis", 10)));
            int cha = Math.max(1, rules.modifier(sheet.optInt("cha", 10)));
            int intMod = Math.max(1, rules.modifier(sheet.optInt("int_score", sheet.optInt("int", 10))));

            if (k.contains("mestre da batalha") || k.contains("battle master")) {
                int count = level >= 15 ? 6 : level >= 7 ? 5 : 4;
                String die = level >= 18 ? "d12" : level >= 10 ? "d10" : "d8";
                resource(res, "superiority_dice", count, "short_or_long_rest").put("die", die);
            } else if (k.contains("psi guerreiro") || k.contains("psi warrior")) {
                String die = level >= 17 ? "d12" : level >= 11 ? "d10" : level >= 5 ? "d8" : "d6";
                resource(res, "psionic_energy_dice", 2 * pb, "long_rest").put("die", die);
            } else if (k.contains("mente de alma") || k.contains("soulknife")) {
                String die = level >= 17 ? "d12" : level >= 11 ? "d10" : level >= 5 ? "d8" : "d6";
                resource(res, "psionic_energy_dice", 2 * pb, "long_rest").put("die", die);
            } else if (k.contains("dominio da luz") || k.contains("light domain")) {
                resource(res, "warding_flare", wis, "long_rest");
            } else if (k.contains("dominio da guerra") || k.contains("war domain")) {
                resource(res, "war_priest", wis, "short_or_long_rest");
            } else if (k.contains("corruptor") || k.contains("fiend")) {
                if (level >= 6) resource(res, "dark_ones_own_luck", 1, "short_or_long_rest");
            } else if (k.contains("mao aberta") || k.contains("open hand")) {
                if (level >= 6) resource(res, "wholeness_of_body", 1, "long_rest");
            } else if (k.contains("fanatico") || k.contains("zealot")) {
                if (level >= 6) resource(res, "fanatical_focus", 1, "long_rest");
            } else if (k.contains("glamour")) {
                if (level >= 6) resource(res, "mantle_of_majesty", 1, "long_rest");
            } else if (k.contains("mecanica") || k.contains("clockwork")) {
                resource(res, "restore_balance", pb, "long_rest");
                if (level >= 6) resource(res, "bastion_of_law_pool", level, "long_rest");
            } else if (k.contains("magia selvagem") || k.contains("wild magic")) {
                resource(res, "tides_of_chaos", 1, "long_rest");
            } else if (k.contains("celestial")) {
                resource(res, "healing_light", 1 + level, "long_rest");
            } else if (k.contains("arquifada") || k.contains("archfey")) {
                resource(res, "misty_step_archfey", pb, "long_rest");
            } else if (k.contains("grande antigo") || k.contains("great old one")) {
                if (level >= 6) resource(res, "entropic_ward", 1, "short_or_long_rest");
            } else if (k.contains("abjuracao") || k.contains("abjuration")) {
                resource(res, "arcane_ward_hp", 2 * level + intMod, "long_rest");
            } else if (k.contains("adivinhacao") || k.contains("divination")) {
                resource(res, "portent_dice", level >= 14 ? 3 : 2, "long_rest");
            } else if (k.contains("evocacao") || k.contains("evoker")) {
                if (level >= 14) resource(res, "overchannel", 1, "long_rest");
            } else if (k.contains("ilusao") || k.contains("illusion")) {
                if (level >= 10) resource(res, "illusory_self", 1, "short_or_long_rest");
            } else if (k.contains("andarilho feerico") || k.contains("fey wanderer")) {
                if (level >= 11) resource(res, "misty_step_fey", wis, "long_rest");
            }
            sheet.put("subclass_resources", res);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static JSONArray actionsFor(String subclass, int level) {
        JSONArray a = new JSONArray();
        if (level < 3 || subclass == null) return a;
        String k = norm(subclass);

        // 1. BÁRBARO
        if (k.contains("berserker")) {
            a.put("Frenesi");
            if (level >= 6) a.put("Fúria Descerebrada");
            if (level >= 10) a.put("Retaliação");
            if (level >= 14) a.put("Presença Intimidadora");
        } else if (k.contains("coracao selvagem") || k.contains("wild heart")) {
            a.put("Fúria das Selvas");
            if (level >= 6) a.put("Aspecto da Besta");
            if (level >= 10) a.put("Porta-Voz da Natureza");
            if (level >= 14) a.put("Sintonização Totêmica");
        } else if (k.contains("fanatico") || k.contains("zealot")) {
            a.put("Fúria Divina");
            a.put("Guerreiro dos Deuses");
            if (level >= 6) a.put("Foco Fanático");
            if (level >= 10) a.put("Presença Zelosa");
        } else if (k.contains("arvore do mundo") || k.contains("world tree")) {
            a.put("Vitalidade da Árvore");
            if (level >= 6) a.put("Ramos da Árvore");
            if (level >= 10) a.put("Raízes Devastadoras");
            if (level >= 14) a.put("Viagem pela Árvore");
        }

        // 2. BARDO
        else if (k.contains("danca") || k.contains("dance")) {
            a.put("Passos Deslumbrantes");
            if (level >= 6) a.put("Movimento Inspirador");
            if (level >= 14) a.put("Liderando a Dança");
        } else if (k.contains("glamour")) {
            a.put("Manto da Inspiração");
            a.put("Magia Cativante");
            if (level >= 6) a.put("Manto da Majestade");
            if (level >= 14) a.put("Majestade Inquebrável");
        } else if (k.contains("sabedoria") || k.contains("lore")) {
            a.put("Palavras Cortantes");
            a.put("Proficiências Bônus");
            if (level >= 6) a.put("Descobertas Mágicas");
            if (level >= 14) a.put("Habilidade Inigualável");
        } else if (k.contains("valor")) {
            a.put("Inspiração de Combate");
            a.put("Treinamento Marcial");
            if (level >= 6) a.put("Ataque Extra");
            if (level >= 14) a.put("Magia de Batalha");
        }

        // 3. CLÉRIGO
        else if (k.contains("dominio da vida") || k.contains("life domain")) {
            a.put("Discípulo da Vida");
            a.put("Preservar a Vida");
            if (level >= 6) a.put("Curandeiro Abençoado");
            if (level >= 17) a.put("Cura Suprema");
        } else if (k.contains("dominio da luz") || k.contains("light domain")) {
            a.put("Resplendor do Alvorecer");
            a.put("Clarão Protetor");
            if (level >= 6) a.put("Clarão Protetor Aprimorado");
            if (level >= 17) a.put("Coroa de Luz");
        } else if (k.contains("dominio da trapaca") || k.contains("trickery domain")) {
            a.put("Invocar Duplicidade");
            a.put("Bênção do Trapaceiro");
            if (level >= 6) a.put("Transposição do Trapaceiro");
            if (level >= 17) a.put("Duplicidade Aprimorada");
        } else if (k.contains("dominio da guerra") || k.contains("war domain")) {
            a.put("Sacerdote da Guerra");
            a.put("Ataque Guiado");
            if (level >= 6) a.put("Bênção do Deus da Guerra");
            if (level >= 17) a.put("Avatar da Batalha");
        }

        // 4. DRUIDA
        else if (k.contains("terra") || k.contains("land")) {
            a.put("Auxílio da Terra");
            a.put("Recuperação Natural");
            if (level >= 6) a.put("Santuário Natural");
            if (level >= 14) a.put("Proteção da Natureza");
        } else if (k.contains("lua") || k.contains("moon")) {
            a.put("Forma Selvagem de Combate");
            a.put("Raios Lunares Selvagens");
            if (level >= 6) a.put("Passo do Luar");
            if (level >= 14) a.put("Resplendor Lunar");
        } else if (k.contains("mar") || k.contains("sea")) {
            a.put("Fúria do Mar");
            if (level >= 6) a.put("Afinidade Aquática");
            if (level >= 10) a.put("Nascido da Tempestade");
            if (level >= 14) a.put("Dádiva Oceânica");
        } else if (k.contains("estrelas") || k.contains("stars")) {
            a.put("Forma Estelar");
            a.put("Mapa Estelar");
            if (level >= 6) a.put("Presságio Cósmico");
            if (level >= 14) a.put("Cheio de Estrelas");
        }

        // 5. GUERREIRO
        else if (k.contains("campeao") || k.contains("champion")) {
            a.put("Crítico Aprimorado");
            if (level >= 7) { a.put("Atleta Notável"); a.put("Estilo de Luta Adicional"); }
            if (level >= 10) a.put("Campeão Heroico");
            if (level >= 18) a.put("Sobrevivente");
        } else if (k.contains("mestre da batalha") || k.contains("battle master")) {
            a.put("Superioridade em Combate");
            a.put("Estudioso da Guerra");
            a.put("Manobra: Ataque Ameaçador");
            a.put("Manobra: Derrubar");
            a.put("Manobra: Desarmar");
            a.put("Manobra: Empurrão");
            a.put("Manobra: Riposta");
            if (level >= 7) a.put("Conheça Seu Inimigo");
            if (level >= 15) a.put("Superioridade Implacável");
        } else if (k.contains("cavaleiro arcano") || k.contains("eldritch knight")) {
            a.put("Vínculo com Arma");
            a.put("Conjuração de Cavaleiro Arcano");
            if (level >= 7) a.put("Magia de Guerra");
            if (level >= 15) a.put("Investida Arcana");
        } else if (k.contains("psi guerreiro") || k.contains("psi warrior")) {
            a.put("Poder Psiônico");
            a.put("Golpe Telecinético");
            a.put("Campo de Força Protetor");
            if (level >= 7) a.put("Movimento Telecinético");
            if (level >= 10) a.put("Mente Protegida");
            if (level >= 18) a.put("Mestre Telecinético");
        }

        // 6. MONGE
        else if (k.contains("misericordia") || k.contains("mercy")) {
            a.put("Mãos da Cura");
            a.put("Mãos do Dano");
            if (level >= 6) a.put("Toque do Médico");
            if (level >= 17) a.put("Mão da Misericórdia Suprema");
        } else if (k.contains("sombras") || k.contains("shadow")) {
            a.put("Artes das Sombras");
            if (level >= 6) a.put("Passo das Sombras");
            if (level >= 11) a.put("Passo das Sombras Aprimorado");
            if (level >= 17) a.put("Manto de Sombras");
        } else if (k.contains("elementos") || k.contains("elements")) {
            a.put("Sintonização Elemental");
            if (level >= 6) a.put("Passada Ambiental");
            if (level >= 11) a.put("Passo dos Elementos");
            if (level >= 17) a.put("Epítome Elemental");
        } else if (k.contains("mao aberta") || k.contains("open hand")) {
            a.put("Técnica da Mão Aberta: Derrubar");
            a.put("Técnica da Mão Aberta: Empurrar");
            a.put("Técnica da Mão Aberta: Sem Reação");
            if (level >= 6) a.put("Integridade Corporal");
            if (level >= 11) a.put("Passo Ligeiro");
            if (level >= 17) a.put("Palma Vibratória");
        }

        // 7. PALADINO
        else if (k.contains("devocao") || k.contains("devotion")) {
            a.put("Arma Sagrada");
            a.put("Expulsar o Profano");
            if (level >= 7) a.put("Aura de Devoção");
            if (level >= 20) a.put("Nimbo Sagrado");
        } else if (k.contains("ancioes") || k.contains("ancients")) {
            a.put("Ira da Natureza");
            if (level >= 7) a.put("Aura de Proteção Arcana");
            if (level >= 15) a.put("Sentinela Imortal");
            if (level >= 20) a.put("Campeão Ancião");
        } else if (k.contains("vinganca") || k.contains("vengeance")) {
            a.put("Voto de Inimizade");
            if (level >= 7) a.put("Vingador Implacável");
            if (level >= 15) a.put("Alma da Vingança");
            if (level >= 20) a.put("Anjo Vingador");
        } else if (k.contains("gloria") || k.contains("glory")) {
            a.put("Atleta Inigualável");
            a.put("Golpe Inspirador");
            if (level >= 7) a.put("Aura de Alacridade");
            if (level >= 20) a.put("Lenda Viva");
        }

        // 8. GUARDIÃO
        else if (k.contains("mestre das feras") || k.contains("beast master")) {
            a.put("Companheiro Primitivo");
            if (level >= 7) a.put("Treinamento Excepcional");
            if (level >= 11) a.put("Fúria Bestial");
            if (level >= 15) a.put("Partilha de Magias");
        } else if (k.contains("andarilho feerico") || k.contains("fey wanderer")) {
            a.put("Golpes Pavorosos");
            a.put("Glamour Sobrenatural");
            if (level >= 7) a.put("Torção Cativante");
            if (level >= 15) a.put("Andarilho das Brumas");
        } else if (k.contains("cacador") || k.contains("hunter")) {
            a.put("Presa do Caçador");
            if (level >= 7) a.put("Táticas Defensivas");
            if (level >= 11) a.put("Defesa Superior do Caçador");
            if (level >= 15) a.put("Presa Superior do Caçador");
        } else if (k.contains("perseguidor sombrio") || k.contains("gloom stalker")) {
            a.put("Emboscador Pavoroso");
            a.put("Visão Umbral");
            if (level >= 7) a.put("Mente de Ferro");
            if (level >= 15) a.put("Esquiva Sombria");
        }

        // 9. LADINO
        else if (k.contains("trapaceiro arcano") || k.contains("arcane trickster")) {
            a.put("Mãos Mágicas Trapaceiras");
            a.put("Conjuração de Trapaceiro");
            if (level >= 9) a.put("Emboscada Mágica");
            if (level >= 17) a.put("Ladrão de Magias");
        } else if (k.contains("assassino") || k.contains("assassin")) {
            a.put("Assassinar");
            a.put("Proficiências do Assassino");
            if (level >= 9) a.put("Perícia de Infiltração");
            if (level >= 17) a.put("Golpe Mortal");
        } else if (k.contains("ladrao") || k.contains("thief")) {
            a.put("Mãos Rápidas");
            a.put("Trabalho nas Alturas");
            if (level >= 9) a.put("Furtividade Suprema");
            if (level >= 17) a.put("Reflexos de Ladrão");
        } else if (k.contains("mente de alma") || k.contains("soulknife")) {
            a.put("Lâminas Psíquicas");
            a.put("Poder Psiônico");
            if (level >= 9) a.put("Lâminas da Alma");
            if (level >= 17) a.put("Dilacerar Mente");
        }

        // 10. FEITICEIRO
        else if (k.contains("aberrante") || k.contains("aberrant")) {
            a.put("Magias Psiônicas e Fala Telepática");
            if (level >= 6) a.put("Feitiçaria Psiônica");
            if (level >= 14) a.put("Revelação na Carne");
        } else if (k.contains("mecanica") || k.contains("clockwork")) {
            a.put("Magias Mecânicas e Restaurar o Equilíbrio");
            if (level >= 6) a.put("Bastião da Lei");
            if (level >= 14) a.put("Transe da Ordem");
        } else if (k.contains("draconica") || k.contains("draconic")) {
            a.put("Resiliência Dracônica");
            if (level >= 6) a.put("Afinidade Elemental");
            if (level >= 14) a.put("Asas de Dragão");
            if (level >= 18) a.put("Companheiro Dragão");
        } else if (k.contains("magia selvagem") || k.contains("wild magic")) {
            a.put("Surto de Magia Selvagem e Ondas do Caos");
            if (level >= 6) a.put("Dobrar a Sorte");
            if (level >= 14) a.put("Caos Controlado");
        }

        // 11. BRUXO
        else if (k.contains("arquifada") || k.contains("archfey")) {
            a.put("Presença Feérica e Fuga Nebulosa");
            if (level >= 10) a.put("Defesas Cativantes");
            if (level >= 14) a.put("Delírio Sombrio");
        } else if (k.contains("celestial")) {
            a.put("Luz Curativa");
            if (level >= 6) a.put("Alma Radiante");
            if (level >= 14) a.put("Vingança Incandescente");
        } else if (k.contains("corruptor") || k.contains("fiend")) {
            a.put("Bênção do Obscuro");
            if (level >= 6) a.put("Sorte do Próprio Obscuro");
            if (level >= 10) a.put("Resiliência Infernal");
            if (level >= 14) a.put("Arremessar ao Inferno");
        } else if (k.contains("grande antigo") || k.contains("great old one")) {
            a.put("Mente Desperta");
            if (level >= 6) a.put("Proteção Entrópica");
            if (level >= 10) a.put("Escudo do Pensamento");
            if (level >= 14) a.put("Criar Lacaio");
        }

        // 12. MAGO
        else if (k.contains("abjuracao") || k.contains("abjuration")) {
            a.put("Proteção Arcana");
            if (level >= 6) a.put("Proteção Projetada");
            if (level >= 10) a.put("Quebrador de Magias");
            if (level >= 14) a.put("Resistência a Magia");
        } else if (k.contains("adivinhacao") || k.contains("divination")) {
            a.put("Presságio");
            if (level >= 6) a.put("Adivinhação Perita");
            if (level >= 10) a.put("O Terceiro Olho");
            if (level >= 14) a.put("Presságio Maior");
        } else if (k.contains("evocacao") || k.contains("evoker")) {
            a.put("Esculpir Magias");
            if (level >= 6) a.put("Truque Potente");
            if (level >= 10) a.put("Evocação Potencializada");
            if (level >= 14) a.put("Sobrecanalizar");
        } else if (k.contains("ilusao") || k.contains("illusion")) {
            a.put("Ilusão Menor Aprimorada");
            if (level >= 6) a.put("Ilusões Maleáveis");
            if (level >= 10) a.put("Eu Ilusório");
            if (level >= 14) a.put("Realidade Ilusória");
        }
        return a;
    }

    public static JSONObject resolve(JSONObject input, String subclass, int level, String feature, JSONObject payload) {
        JSONObject s = copy(input);
        try {
            if (!s.has("level") || s.optInt("level", 0) <= 0) s.put("level", level);
            if (!s.has("class_resources") && s.has("class_name")) {
                s = Core2024ClassRuntimeEngine.ensure(s, s.optString("class_name"), level);
            }
            applyToSheet(s);
            if (payload == null) payload = new JSONObject();
            if (level < 3) return blocked(s, "requires_level_3");
            String k = norm(subclass), f = norm(feature);
            DndRulesEngine rules = new DndRulesEngine();

            // ================================================================
            // 1. BÁRBARO
            // ================================================================
            // Berserker
            if (k.contains("berserker") && f.contains("frenesi")) {
                if (!s.optBoolean("rage_active", false)) return blocked(s, "rage_inactive");
                s.put("frenzy_bonus_active", true);
                return mark(s, "barbarian_frenzy", new JSONObject().put("extra_damage_dice", "1d6").put("frenzy_active", true));
            }
            if (k.contains("berserker") && f.contains("descerebrada")) {
                if (level < 6) return blocked(s, "requires_level_6");
                if (!s.optBoolean("rage_active", false)) return blocked(s, "rage_inactive");
                String conds = s.optString("conditions", "").replace("charmed", "").replace("frightened", "").replace("  ", " ").trim();
                s.put("conditions", conds);
                return mark(s, "barbarian_mindless_rage", new JSONObject().put("immune_charmed", true).put("immune_frightened", true));
            }
            if (k.contains("berserker") && f.contains("retaliacao")) {
                if (level < 10) return blocked(s, "requires_level_10");
                return mark(s, "barbarian_retaliation", new JSONObject().put("reaction_melee_attack_granted", true));
            }
            if (k.contains("berserker") && f.contains("intimidadora")) {
                if (level < 14) return blocked(s, "requires_level_14");
                String target = payload.optString("target_id", "");
                if (target.isEmpty()) return pending(s, "target_id");
                int dc = 8 + rules.proficiencyBonus(level) + rules.modifier(s.optInt("str", 10));
                int saveRoll = payload.optInt("target_wis_save", -1);
                if (saveRoll < 0) return pending(s, "target_wis_save");
                boolean success = saveRoll >= dc;
                return mark(s, "barbarian_intimidating_presence", new JSONObject().put("target_id", target).put("dc", dc).put("save_roll", saveRoll).put("frightened", !success));
            }

            // Coração Selvagem (Wild Heart)
            if (k.contains("coracao selvagem") || k.contains("wild heart")) {
                if (f.contains("furia das selvas") || f.contains("selvas")) {
                    if (!s.optBoolean("rage_active", false)) return blocked(s, "rage_inactive");
                    String aspect = payload.optString("aspect", "urso").toLowerCase();
                    s.put("wild_heart_aspect", aspect);
                    if ("urso".equals(aspect) || "bear".equals(aspect)) s.put("damage_resistances", "all_except_psychic");
                    else if ("aguia".equals(aspect) || "eagle".equals(aspect)) s.put("bonus_action_dash_disengage", true);
                    else if ("lobo".equals(aspect) || "wolf".equals(aspect)) s.put("wolf_allies_melee_advantage", true);
                    return mark(s, "wild_heart_rage_aspect", new JSONObject().put("aspect", aspect));
                }
                if (f.contains("aspecto da besta") || f.contains("besta")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    String blessing = payload.optString("blessing", "atletismo");
                    s.put("aspect_of_the_beast_blessing", blessing);
                    return mark(s, "wild_heart_aspect_of_the_beast", new JSONObject().put("blessing", blessing));
                }
                if (f.contains("porta-voz") || f.contains("natureza")) {
                    if (level < 10) return blocked(s, "requires_level_10");
                    String ritual = payload.optString("ritual", "sentido_bestial");
                    return mark(s, "wild_heart_nature_speaker", new JSONObject().put("ritual_cast_free", ritual));
                }
                if (f.contains("totemica") || f.contains("sintonizacao")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    return mark(s, "wild_heart_totemic_attunement", new JSONObject().put("supreme_rage_active", true));
                }
            }

            // Fanático (Zealot)
            if (k.contains("fanatico") || k.contains("zealot")) {
                if (f.contains("furia divina") || f.contains("divina")) {
                    if (!s.optBoolean("rage_active", false)) return blocked(s, "rage_inactive");
                    String type = payload.optString("damage_type", "radiant");
                    return mark(s, "zealot_divine_fury", new JSONObject().put("extra_damage_dice", "1d6").put("extra_damage_flat", level / 2).put("damage_type", type));
                }
                if (f.contains("guerreiro dos deuses")) {
                    s.put("waive_revive_material_cost", true);
                    return mark(s, "zealot_warrior_of_the_gods", new JSONObject().put("waive_revive_material_cost", true));
                }
                if (f.contains("foco fanatico")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    if (!spend(s, "fanatical_focus", 1)) return blocked(s, "fanatical_focus");
                    return mark(s, "zealot_fanatical_focus", new JSONObject().put("reroll_save_granted", true));
                }
                if (f.contains("presenca zelosa") || f.contains("zelosa")) {
                    if (level < 10) return blocked(s, "requires_level_10");
                    return mark(s, "zealot_zealous_presence", new JSONObject().put("allies_advantage_granted", true).put("duration_rounds", 1));
                }
            }

            // Árvore do Mundo (World Tree)
            if (k.contains("arvore do mundo") || k.contains("world tree")) {
                if (f.contains("vitalidade")) {
                    int curTemp = s.optInt("temp_hp", 0);
                    s.put("temp_hp", Math.max(curTemp, level));
                    return mark(s, "world_tree_vitality", new JSONObject().put("temp_hp_granted", level));
                }
                if (f.contains("ramos da arvore") || f.contains("ramos")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    String target = payload.optString("target_id", "");
                    if (target.isEmpty()) return pending(s, "target_id");
                    return mark(s, "world_tree_branches", new JSONObject().put("target_id", target).put("teleport_range_ft", 30));
                }
                if (f.contains("raizes devastadoras") || f.contains("raizes")) {
                    if (level < 10) return blocked(s, "requires_level_10");
                    s.put("heavy_weapons_reach_bonus_ft", 10);
                    return mark(s, "world_tree_battering_roots", new JSONObject().put("reach_bonus_ft", 10).put("push_topple_mastery", true));
                }
                if (f.contains("viagem pela arvore") || f.contains("viagem")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    return mark(s, "world_tree_travel", new JSONObject().put("planar_travel_range_ft", 60));
                }
            }

            // ================================================================
            // 2. BARDO
            // ================================================================
            // Dança (Dance)
            if (k.contains("danca") || k.contains("dance")) {
                if (f.contains("passos deslumbrantes")) {
                    int chaMod = Math.max(0, rules.modifier(s.optInt("cha", 10)));
                    s.put("dance_unarmored_ac_bonus", chaMod);
                    return mark(s, "dance_dazzling_footwork", new JSONObject().put("ac_bonus_charisma", chaMod));
                }
                if (f.contains("movimento inspirador")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    return mark(s, "dance_inspiring_movement", new JSONObject().put("reaction_half_speed_move", true).put("avoids_opportunity_attacks", true));
                }
                if (f.contains("liderando a danca")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    int die = payload.optInt("die_roll", 1);
                    return mark(s, "dance_leading_the_dance", new JSONObject().put("initiative_bonus", die).put("radius_ft", 30));
                }
            }

            // Glamour
            if (k.contains("glamour")) {
                if (f.contains("manto da inspiracao")) {
                    if (!spend(s, "bardic_inspiration", 1)) return blocked(s, "bardic_inspiration");
                    int die = payload.optInt("die_roll", 6);
                    int temp = 2 * die;
                    return mark(s, "glamour_mantle_of_inspiration", new JSONObject().put("temp_hp_granted", temp).put("targets_count_max", Math.max(1, rules.modifier(s.optInt("cha", 10)))));
                }
                if (f.contains("magia cativante")) {
                    return mark(s, "glamour_beguiling_magic", new JSONObject().put("charm_or_fear_dc", 8 + rules.proficiencyBonus(level) + rules.modifier(s.optInt("cha", 10))));
                }
                if (f.contains("manto da majestade")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    if (!spend(s, "mantle_of_majesty", 1)) return blocked(s, "mantle_of_majesty");
                    return mark(s, "glamour_mantle_of_majesty", new JSONObject().put("command_bonus_action_free", true));
                }
                if (f.contains("majestade inquebravel")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    return mark(s, "glamour_unbreakable_majesty", new JSONObject().put("attacker_cha_save_required", true));
                }
            }

            // Sabedoria (Lore)
            if (k.contains("sabedoria") || k.contains("lore")) {
                if (f.contains("palavras cortantes") || f.contains("cutting words")) {
                    if (!spend(s, "bardic_inspiration", 1)) return blocked(s, "bardic_inspiration");
                    if (!payload.has("die_roll")) return pending(s, "die_roll");
                    int die = payload.optInt("die_roll", 1);
                    return mark(s, "bard_cutting_words", new JSONObject().put("subtracted_amount", die).put("bardic_inspiration_spent", 1));
                }
                if (f.contains("proficiencias bonus")) {
                    return mark(s, "lore_bonus_proficiencies", new JSONObject().put("extra_skills_count", 3));
                }
                if (f.contains("descobertas magicas")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    return mark(s, "lore_magical_discoveries", new JSONObject().put("extra_spells_any_class", 2));
                }
                if (f.contains("habilidade inigualavel")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    int die = payload.optInt("die_roll", 1);
                    return mark(s, "lore_peerless_skill", new JSONObject().put("bonus_to_check", die));
                }
            }

            // Valor
            if (k.contains("valor")) {
                if (f.contains("inspiracao de combate")) {
                    if (!spend(s, "bardic_inspiration", 1)) return blocked(s, "bardic_inspiration");
                    int die = payload.optInt("die_roll", 1);
                    return mark(s, "valor_combat_inspiration", new JSONObject().put("bonus_to_damage_or_ac", die));
                }
                if (f.contains("treinamento marcial")) {
                    return mark(s, "valor_martial_training", new JSONObject().put("proficiencies_granted", "medium_armor, shields, martial_weapons"));
                }
                if (f.contains("ataque extra")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    return mark(s, "valor_extra_attack", new JSONObject().put("cantrip_substitution_permitted", true));
                }
                if (f.contains("magia de batalha")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    return mark(s, "valor_battle_magic", new JSONObject().put("bonus_action_weapon_attack_granted", true));
                }
            }

            // ================================================================
            // 3. CLÉRIGO
            // ================================================================
            // Domínio da Vida
            if (k.contains("dominio da vida") || k.contains("life domain")) {
                if (f.contains("discipulo da vida")) {
                    int spLvl = payload.optInt("spell_level", 1);
                    return mark(s, "life_disciple_of_life", new JSONObject().put("extra_healing", 2 + spLvl));
                }
                if (f.contains("preservar a vida") || f.contains("preservar")) {
                    if (!spend(s, "channel_divinity", 1)) return blocked(s, "channel_divinity");
                    int budget = 5 * level;
                    JSONArray dist = payload.optJSONArray("distribution");
                    if (dist == null) return pending(s, "distribution [{target_id, healing}] up to budget " + budget);
                    int total = 0;
                    for (int i = 0; i < dist.length(); i++) {
                        JSONObject d = dist.optJSONObject(i);
                        if (d != null) total += d.optInt("healing", 0);
                    }
                    if (total > budget) return blocked(s, "healing_exceeds_budget_" + budget);
                    return mark(s, "cleric_preserve_life", new JSONObject().put("healing_budget", budget).put("healing_total", total).put("channel_divinity_spent", 1));
                }
                if (f.contains("curandeiro abencoado")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    int spLvl = payload.optInt("spell_level", 1);
                    int heal = 2 + spLvl;
                    int curHp = s.optInt("hp", s.optInt("max_hp", 1));
                    s.put("hp", Math.min(s.optInt("max_hp", curHp), curHp + heal));
                    return mark(s, "life_blessed_healer", new JSONObject().put("self_heal", heal));
                }
                if (f.contains("cura suprema")) {
                    if (level < 17) return blocked(s, "requires_level_17");
                    return mark(s, "life_supreme_healing", new JSONObject().put("maximize_healing_dice", true));
                }
            }

            // Domínio da Luz
            if (k.contains("dominio da luz") || k.contains("light domain")) {
                if (f.contains("resplendor") || f.contains("alvorecer")) {
                    if (!spend(s, "channel_divinity", 1)) return blocked(s, "channel_divinity");
                    return mark(s, "cleric_radiance_of_the_dawn", new JSONObject().put("damage_dice", "2d10 + " + level).put("damage_type", "radiant").put("radius_ft", 30).put("channel_divinity_spent", 1));
                }
                if (f.contains("clarao protetor aprimorado")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    if (!spend(s, "warding_flare", 1)) return blocked(s, "warding_flare");
                    return mark(s, "cleric_improved_warding_flare", new JSONObject().put("protect_ally_disadvantage_applied", true));
                }
                if (f.contains("clarao") || f.contains("flare")) {
                    if (!spend(s, "warding_flare", 1)) return blocked(s, "warding_flare");
                    return mark(s, "cleric_warding_flare", new JSONObject().put("attacker_disadvantage_applied", true));
                }
                if (f.contains("coroa de luz")) {
                    if (level < 17) return blocked(s, "requires_level_17");
                    return mark(s, "cleric_corona_of_light", new JSONObject().put("disadvantage_vs_fire_radiant", true).put("radius_ft", 60));
                }
            }

            // Domínio da Trapaça
            if (k.contains("dominio da trapaca") || k.contains("trickery domain")) {
                if (f.contains("invocar duplicidade") || f.contains("duplicidade")) {
                    if (!spend(s, "channel_divinity", 1)) return blocked(s, "channel_divinity");
                    s.put("duplicate_illusion_active", true);
                    return mark(s, "trickery_invoke_duplicity", new JSONObject().put("duplicate_created", true).put("advantage_within_5ft", true));
                }
                if (f.contains("bencao do trapaceiro")) {
                    String target = payload.optString("target_id", "");
                    if (target.isEmpty()) return pending(s, "target_id");
                    return mark(s, "trickery_blessing_of_the_trickster", new JSONObject().put("target_id", target).put("stealth_advantage_granted", true));
                }
                if (f.contains("transposicao do trapaceiro") || f.contains("transposicao")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    return mark(s, "trickery_transposition", new JSONObject().put("position_swap_performed", true).put("range_ft", 30));
                }
                if (f.contains("duplicidade aprimorada")) {
                    if (level < 17) return blocked(s, "requires_level_17");
                    return mark(s, "trickery_improved_duplicity", new JSONObject().put("duplicates_max", 4));
                }
            }

            // Domínio da Guerra
            if (k.contains("dominio da guerra") || k.contains("war domain")) {
                if (f.contains("sacerdote") || f.contains("war priest")) {
                    if (!spend(s, "war_priest", 1)) return blocked(s, "war_priest");
                    return mark(s, "cleric_war_priest", new JSONObject().put("bonus_action_attack_granted", true));
                }
                if (f.contains("ataque guiado") || f.contains("guided strike")) {
                    if (!spend(s, "channel_divinity", 1)) return blocked(s, "channel_divinity");
                    return mark(s, "cleric_guided_strike", new JSONObject().put("attack_roll_bonus", 10).put("channel_divinity_spent", 1));
                }
                if (f.contains("bencao do deus da guerra")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    if (!spend(s, "channel_divinity", 1)) return blocked(s, "channel_divinity");
                    String ally = payload.optString("ally_target_id", "");
                    return mark(s, "cleric_war_gods_blessing", new JSONObject().put("ally_target_id", ally).put("attack_bonus", 10));
                }
                if (f.contains("avatar da batalha")) {
                    if (level < 17) return blocked(s, "requires_level_17");
                    s.put("resistance_physical_nonmagical", true);
                    return mark(s, "cleric_avatar_of_battle", new JSONObject().put("avatar_resistances_active", true));
                }
            }

            // ================================================================
            // 4. DRUIDA
            // ================================================================
            // Círculo da Terra
            if (k.contains("terra") || k.contains("land")) {
                if (f.contains("auxilio da terra") || f.contains("aid")) {
                    if (!spend(s, "wild_shape", 1)) return blocked(s, "wild_shape");
                    int heal = payload.optInt("heal_roll", 7) + rules.modifier(s.optInt("wis", 10));
                    return mark(s, "druid_land_aid", new JSONObject().put("healing_amount", heal).put("wild_shape_spent", 1));
                }
                if (f.contains("recuperacao natural")) {
                    int slotBudget = (level + 1) / 2;
                    return mark(s, "druid_natural_recovery", new JSONObject().put("slot_level_budget", slotBudget));
                }
                if (f.contains("santuario natural")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    s.put("ignore_difficult_terrain_nature", true);
                    return mark(s, "druid_natural_sanctuary", new JSONObject().put("difficult_terrain_ignored", true));
                }
                if (f.contains("protecao da natureza")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    s.put("immune_poison", true);
                    s.put("immune_paralyzed", true);
                    return mark(s, "druid_natures_ward", new JSONObject().put("immune_poison", true).put("immune_paralyzed", true));
                }
            }

            // Círculo da Lua
            if (k.contains("lua") || k.contains("moon")) {
                if (f.contains("forma selvagem de combate") || f.contains("combate")) {
                    if (!spend(s, "wild_shape", 1)) return blocked(s, "wild_shape");
                    int temp = 3 * level;
                    int curTemp = s.optInt("temp_hp", 0);
                    s.put("temp_hp", Math.max(curTemp, temp));
                    int ac = 13 + rules.modifier(s.optInt("wis", 10));
                    s.put("moon_wild_shape_ac", ac);
                    return mark(s, "moon_combat_wild_shape", new JSONObject().put("temp_hp_granted", temp).put("ac", ac).put("wild_shape_spent", 1));
                }
                if (f.contains("raios lunares selvagens") || f.contains("lunares")) {
                    return mark(s, "moon_lunar_radiance", new JSONObject().put("extra_radiant_dice", "1d8"));
                }
                if (f.contains("passo do luar") || f.contains("moonlight step")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    return mark(s, "moon_moonlight_step", new JSONObject().put("teleport_range_ft", 30).put("next_attack_advantage", true));
                }
                if (f.contains("resplendor lunar")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    return mark(s, "moon_lunar_radiance_supreme", new JSONObject().put("radiant_damage_dice", "2d10"));
                }
            }

            // Círculo do Mar
            if (k.contains("mar") || k.contains("sea")) {
                if (f.contains("furia do mar")) {
                    if (!spend(s, "wild_shape", 1)) return blocked(s, "wild_shape");
                    int die = payload.optInt("die_roll", 6) + rules.modifier(s.optInt("wis", 10));
                    return mark(s, "sea_wrath_of_the_sea", new JSONObject().put("damage_cold_thunder", die).put("radius_ft", 10));
                }
                if (f.contains("afinidade aquatica")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    s.put("swim_speed_equals_walk", true);
                    s.put("water_breathing", true);
                    return mark(s, "sea_aquatic_affinity", new JSONObject().put("swim_speed_match", true).put("water_breathing", true));
                }
                if (f.contains("nascido da tempestade")) {
                    if (level < 10) return blocked(s, "requires_level_10");
                    s.put("resistance_cold", true);
                    s.put("resistance_lightning", true);
                    s.put("resistance_thunder", true);
                    s.put("fly_speed_equals_swim", true);
                    return mark(s, "sea_stormborn", new JSONObject().put("flying_granted", true).put("resistances_granted", true));
                }
                if (f.contains("dadiva oceanica")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    String ally = payload.optString("ally_id", "");
                    return mark(s, "sea_oceanic_gift", new JSONObject().put("shared_ally_id", ally).put("damage_dice", "3d8"));
                }
            }

            // Círculo das Estrelas
            if (k.contains("estrelas") || k.contains("stars")) {
                if (f.contains("forma estelar")) {
                    if (!spend(s, "wild_shape", 1)) return blocked(s, "wild_shape");
                    String constellation = payload.optString("constellation", "arqueiro").toLowerCase();
                    s.put("starry_form_constellation", constellation);
                    return mark(s, "stars_starry_form", new JSONObject().put("constellation", constellation).put("wild_shape_spent", 1));
                }
                if (f.contains("mapa estelar")) {
                    return mark(s, "stars_star_map", new JSONObject().put("free_guiding_bolt_granted", true));
                }
                if (f.contains("pressagio cosmico")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    String mode = payload.optString("mode", "weal").toLowerCase();
                    int roll = payload.optInt("die_roll", 3);
                    return mark(s, "stars_cosmic_omen", new JSONObject().put("mode", mode).put("d6_roll", roll));
                }
                if (f.contains("cheio de estrelas")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    s.put("resistance_physical_in_starry_form", true);
                    return mark(s, "stars_full_of_stars", new JSONObject().put("physical_resistance_active", true));
                }
            }

            // ================================================================
            // 5. GUERREIRO
            // ================================================================
            // Campeão
            if (k.contains("campeao") || k.contains("champion")) {
                if (f.contains("critico aprimorado")) {
                    int thresh = level >= 15 ? 18 : 19;
                    return mark(s, "champion_improved_critical", new JSONObject().put("critical_threshold", thresh));
                }
                if (f.contains("atleta notavel")) {
                    if (level < 7) return blocked(s, "requires_level_7");
                    return mark(s, "champion_remarkable_athlete", new JSONObject().put("initiative_advantage", true));
                }
                if (f.contains("estilo de luta adicional")) {
                    if (level < 7) return blocked(s, "requires_level_7");
                    return mark(s, "champion_additional_fighting_style", new JSONObject().put("style_granted", true));
                }
                if (f.contains("campeao heroico")) {
                    if (level < 10) return blocked(s, "requires_level_10");
                    return mark(s, "champion_heroic_warrior", new JSONObject().put("reroll_save_granted", true));
                }
                if (f.contains("sobrevivente")) {
                    if (level < 18) return blocked(s, "requires_level_18");
                    int maxHp = Math.max(1, s.optInt("max_hp", 1)), hp = s.optInt("hp", maxHp);
                    if (hp <= 0) return blocked(s, "character_unconscious_or_dead");
                    if (hp > maxHp / 2) return blocked(s, "hp_above_half_threshold");
                    int conMod = Math.max(0, rules.modifier(s.optInt("con", 10)));
                    int heal = Math.min(maxHp - hp, 5 + conMod);
                    s.put("hp", hp + heal);
                    return mark(s, "champion_survivor", new JSONObject().put("healing", heal).put("new_hp", hp + heal));
                }
            }

            // Mestre da Batalha
            if (k.contains("mestre da batalha") || k.contains("battle master")) {
                if (f.contains("superioridade em combate") || f.contains("manobra")) {
                    if (!spend(s, "superiority_dice", 1)) return blocked(s, "superiority_dice");
                    if (!payload.has("die_roll")) return pending(s, "die_roll");
                    int dieRoll = payload.optInt("die_roll", 1);
                    String subAction = f.replace("manobra:", "").replace("manobra", "").trim();
                    return mark(s, "battlemaster_maneuver", new JSONObject().put("maneuver", subAction).put("superiority_die_roll", dieRoll).put("superiority_dice_spent", 1));
                }
                if (f.contains("estudioso da guerra")) {
                    return mark(s, "battlemaster_student_of_war", new JSONObject().put("artisan_tool_proficiency", true));
                }
                if (f.contains("conheca seu inimigo")) {
                    if (level < 7) return blocked(s, "requires_level_7");
                    String target = payload.optString("target_id", "");
                    return mark(s, "battlemaster_know_your_enemy", new JSONObject().put("target_id", target).put("inspected", true));
                }
                if (f.contains("superioridade implacavel")) {
                    if (level < 15) return blocked(s, "requires_level_15");
                    JSONObject res = res(s, "superiority_dice");
                    if (res != null && res.optInt("current", 0) == 0) res.put("current", 1);
                    return mark(s, "battlemaster_relentless", new JSONObject().put("superiority_die_restored", 1));
                }
            }

            // Cavaleiro Arcano
            if (k.contains("cavaleiro arcano") || k.contains("eldritch knight")) {
                if (f.contains("vinculo com arma") || f.contains("vinculo")) {
                    String weaponId = payload.optString("weapon_item_id", "");
                    if (weaponId.isEmpty()) return pending(s, "weapon_item_id");
                    s.put("bonded_weapon_id", weaponId);
                    return mark(s, "eldritch_knight_weapon_bond", new JSONObject().put("bonded_weapon_id", weaponId));
                }
                if (f.contains("conjuracao de cavaleiro arcano")) {
                    return mark(s, "eldritch_knight_spellcasting", new JSONObject().put("ability", "intelligence"));
                }
                if (f.contains("magia de guerra")) {
                    if (level < 7) return blocked(s, "requires_level_7");
                    return mark(s, "eldritch_knight_war_magic", new JSONObject().put("cantrip_attack_substitution", true));
                }
                if (f.contains("investida arcana")) {
                    if (level < 15) return blocked(s, "requires_level_15");
                    return mark(s, "eldritch_knight_arcane_charge", new JSONObject().put("teleport_range_ft", 30));
                }
            }

            // Psi Guerreiro
            if (k.contains("psi guerreiro") || k.contains("psi warrior")) {
                if (f.contains("golpe telecinetico") || f.contains("campo de forca")) {
                    if (!spend(s, "psionic_energy_dice", 1)) return blocked(s, "psionic_energy_dice");
                    if (!payload.has("die_roll")) return pending(s, "die_roll");
                    int roll = payload.optInt("die_roll", 1);
                    int intMod = Math.max(0, rules.modifier(s.optInt("int_score", s.optInt("int", 10))));
                    return mark(s, "psi_warrior_psionic_feature", new JSONObject().put("feature", feature).put("die_roll", roll).put("int_mod", intMod).put("total", roll + intMod));
                }
                if (f.contains("poder psionico")) {
                    return mark(s, "psi_warrior_psionic_power", new JSONObject().put("dice_pool", 2 * rules.proficiencyBonus(level)));
                }
                if (f.contains("movimento telecinetico")) {
                    if (level < 7) return blocked(s, "requires_level_7");
                    return mark(s, "psi_warrior_telekinetic_movement", new JSONObject().put("range_ft", 30));
                }
                if (f.contains("mente protegida")) {
                    if (level < 10) return blocked(s, "requires_level_10");
                    s.put("resistance_psychic", true);
                    return mark(s, "psi_warrior_guarded_mind", new JSONObject().put("psychic_resistance", true));
                }
                if (f.contains("mestre telecinetico")) {
                    if (level < 18) return blocked(s, "requires_level_18");
                    return mark(s, "psi_warrior_telekinetic_master", new JSONObject().put("free_telekinesis_granted", true));
                }
            }

            // ================================================================
            // 6. MONGE
            // ================================================================
            // Caminho da Misericórdia
            if (k.contains("misericordia") || k.contains("mercy")) {
                if (f.contains("maos da cura")) {
                    if (!spend(s, "focus_points", 1)) return blocked(s, "focus_points");
                    int roll = payload.optInt("die_roll", 4) + rules.modifier(s.optInt("wis", 10));
                    return mark(s, "mercy_hands_of_healing", new JSONObject().put("healing_amount", roll).put("focus_spent", 1));
                }
                if (f.contains("maos do dano")) {
                    if (!spend(s, "focus_points", 1)) return blocked(s, "focus_points");
                    int roll = payload.optInt("die_roll", 4) + rules.modifier(s.optInt("wis", 10));
                    return mark(s, "mercy_hands_of_harm", new JSONObject().put("necrotic_damage", roll).put("focus_spent", 1));
                }
                if (f.contains("toque do medico")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    return mark(s, "mercy_physicians_touch", new JSONObject().put("cleanses_conditions", true).put("inflicts_poisoned", true));
                }
                if (f.contains("misericordia suprema")) {
                    if (level < 17) return blocked(s, "requires_level_17");
                    if (!spend(s, "focus_points", 5)) return blocked(s, "focus_points");
                    return mark(s, "mercy_hand_of_ultimate_mercy", new JSONObject().put("revive_with_1hp", true).put("focus_spent", 5));
                }
            }

            // Caminho das Sombras
            if (k.contains("sombras") || k.contains("shadow")) {
                if (f.contains("artes das sombras")) {
                    if (!spend(s, "focus_points", 1)) return blocked(s, "focus_points");
                    s.put("darkvision_ft", Math.max(s.optInt("darkvision_ft", 0), 60));
                    s.put("see_through_magical_darkness", true);
                    return mark(s, "shadow_arts", new JSONObject().put("darkness_cast", true).put("focus_spent", 1));
                }
                if (f.contains("passo das sombras aprimorado")) {
                    if (level < 11) return blocked(s, "requires_level_11");
                    return mark(s, "shadow_step_improved", new JSONObject().put("unarmed_strike_free_after_teleport", true));
                }
                if (f.contains("passo das sombras")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    return mark(s, "shadow_step", new JSONObject().put("teleport_range_ft", 60).put("next_attack_advantage", true));
                }
                if (f.contains("manto de sombras")) {
                    if (level < 17) return blocked(s, "requires_level_17");
                    return mark(s, "shadow_cloak_of_shadows", new JSONObject().put("invisibility_active", true));
                }
            }

            // Caminho dos Elementos
            if (k.contains("elementos") || k.contains("elements")) {
                if (f.contains("sintonizacao elemental")) {
                    String elem = payload.optString("element", "fire");
                    s.put("elemental_attunement_type", elem);
                    s.put("unarmed_reach_bonus_ft", 10);
                    return mark(s, "elements_attunement", new JSONObject().put("reach_bonus_ft", 10).put("damage_type", elem));
                }
                if (f.contains("passada ambiental")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    s.put("climb_speed_equals_walk", true);
                    s.put("swim_speed_equals_walk", true);
                    return mark(s, "elements_environmental_stride", new JSONObject().put("climb_swim_match", true));
                }
                if (f.contains("passo dos elementos")) {
                    if (level < 11) return blocked(s, "requires_level_11");
                    s.put("fly_speed_equals_walk", true);
                    return mark(s, "elements_stride_of_the_elements", new JSONObject().put("flying_active", true));
                }
                if (f.contains("epitome elemental")) {
                    if (level < 17) return blocked(s, "requires_level_17");
                    return mark(s, "elements_epitome", new JSONObject().put("elemental_resistances_active", true));
                }
            }

            // Caminho da Mão Aberta
            if (k.contains("mao aberta") || k.contains("open hand")) {
                if (f.contains("tecnica")) {
                    String tech = f.contains("derrubar") ? "derrubar" : f.contains("empurrar") ? "empurrar" : "sem_reacao";
                    return mark(s, "monk_open_hand_technique", new JSONObject().put("technique", tech).put("trigger", "flurry_of_blows_hit"));
                }
                if (f.contains("integridade corporal") || f.contains("wholeness")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    if (!spend(s, "wholeness_of_body", 1)) return blocked(s, "wholeness_of_body");
                    int d10 = payload.optInt("d10_roll", 5);
                    int maxHp = Math.max(1, s.optInt("max_hp", 1)), hp = s.optInt("hp", maxHp);
                    int heal = Math.min(maxHp - hp, d10 + level);
                    s.put("hp", hp + heal);
                    return mark(s, "monk_wholeness_of_body", new JSONObject().put("d10_roll", d10).put("healing", heal).put("new_hp", hp + heal));
                }
                if (f.contains("passo ligeiro")) {
                    if (level < 11) return blocked(s, "requires_level_11");
                    return mark(s, "monk_fleet_step", new JSONObject().put("free_patient_defense_granted", true));
                }
                if (f.contains("palma vibratoria") || f.contains("palma tremula") || f.contains("quivering palm")) {
                    if (level < 17) return blocked(s, "requires_level_17");
                    if (!spend(s, "focus_points", 4)) return blocked(s, "focus_points");
                    int conSave = payload.optInt("target_con_save", -1);
                    int dc = 8 + rules.proficiencyBonus(level) + rules.modifier(s.optInt("wis", 10));
                    boolean fail = conSave < dc;
                    return mark(s, "monk_quivering_palm", new JSONObject().put("dc", dc).put("con_save", conSave).put("drop_to_zero_hp", fail).put("necrotic_damage", fail ? 0 : 55).put("focus_spent", 4));
                }
            }

            // ================================================================
            // 7. PALADINO
            // ================================================================
            // Juramento de Devoção
            if (k.contains("devocao") || k.contains("devotion")) {
                if (f.contains("arma sagrada") || f.contains("sacred weapon")) {
                    if (!spend(s, "channel_divinity", 1)) return blocked(s, "channel_divinity");
                    int chaMod = Math.max(0, rules.modifier(s.optInt("cha", 10)));
                    s.put("sacred_weapon_active", true);
                    s.put("sacred_weapon_bonus", chaMod);
                    return mark(s, "paladin_sacred_weapon", new JSONObject().put("attack_bonus", chaMod).put("duration_minutes", 1).put("channel_divinity_spent", 1));
                }
                if (f.contains("expulsar") || f.contains("profano")) {
                    if (!spend(s, "channel_divinity", 1)) return blocked(s, "channel_divinity");
                    return mark(s, "paladin_turn_the_unholy", new JSONObject().put("turned_targets", "fiends_and_undead").put("radius_ft", 30).put("channel_divinity_spent", 1));
                }
                if (f.contains("aura de devocao")) {
                    if (level < 7) return blocked(s, "requires_level_7");
                    s.put("aura_of_devotion_immune_charmed", true);
                    return mark(s, "paladin_aura_of_devotion", new JSONObject().put("radius_ft", 10).put("immune_charmed", true));
                }
                if (f.contains("nimbo sagrado")) {
                    if (level < 20) return blocked(s, "requires_level_20");
                    return mark(s, "paladin_holy_nimbus", new JSONObject().put("sunlight_aura_radiant_damage", 10).put("radius_ft", 30));
                }
            }

            // Juramento dos Anciões
            if (k.contains("ancioes") || k.contains("ancients")) {
                if (f.contains("ira da natureza")) {
                    if (!spend(s, "channel_divinity", 1)) return blocked(s, "channel_divinity");
                    String target = payload.optString("target_id", "");
                    return mark(s, "paladin_natures_wrath", new JSONObject().put("target_id", target).put("restrained_applied", true).put("channel_divinity_spent", 1));
                }
                if (f.contains("aura de protecao arcana")) {
                    if (level < 7) return blocked(s, "requires_level_7");
                    s.put("aura_of_warding_spell_resistance", true);
                    return mark(s, "paladin_aura_of_warding", new JSONObject().put("spell_damage_resistance", true).put("radius_ft", 10));
                }
                if (f.contains("sentinela imortal")) {
                    if (level < 15) return blocked(s, "requires_level_15");
                    return mark(s, "paladin_undying_sentinel", new JSONObject().put("drop_to_1hp_instead_of_zero", true));
                }
                if (f.contains("campeao anciao")) {
                    if (level < 20) return blocked(s, "requires_level_20");
                    return mark(s, "paladin_elder_champion", new JSONObject().put("turn_hp_regeneration", 10).put("bonus_action_spells", true));
                }
            }

            // Juramento de Vingança
            if (k.contains("vinganca") || k.contains("vengeance")) {
                if (f.contains("voto de inimizade") || f.contains("inimizade")) {
                    if (!spend(s, "channel_divinity", 1)) return blocked(s, "channel_divinity");
                    String target = payload.optString("target_id", "");
                    s.put("vow_of_enmity_target", target);
                    return mark(s, "paladin_vow_of_enmity", new JSONObject().put("target_id", target).put("advantage_granted", true).put("channel_divinity_spent", 1));
                }
                if (f.contains("vingador implacavel")) {
                    if (level < 7) return blocked(s, "requires_level_7");
                    return mark(s, "paladin_relentless_avenger", new JSONObject().put("move_after_opportunity_attack", true));
                }
                if (f.contains("alma da vinganca")) {
                    if (level < 15) return blocked(s, "requires_level_15");
                    return mark(s, "paladin_soul_of_vengeance", new JSONObject().put("reaction_counterattack_granted", true));
                }
                if (f.contains("anjo vingador")) {
                    if (level < 20) return blocked(s, "requires_level_20");
                    s.put("fly_speed_ft", Math.max(s.optInt("fly_speed_ft", 0), 60));
                    return mark(s, "paladin_avenging_angel", new JSONObject().put("flying_speed_ft", 60).put("frighten_aura", true));
                }
            }

            // Juramento de Glória
            if (k.contains("gloria") || k.contains("glory")) {
                if (f.contains("atleta inigualavel")) {
                    if (!spend(s, "channel_divinity", 1)) return blocked(s, "channel_divinity");
                    s.put("peerless_athlete_active", true);
                    return mark(s, "paladin_peerless_athlete", new JSONObject().put("advantage_athletics_acrobatics", true).put("channel_divinity_spent", 1));
                }
                if (f.contains("golpe inspirador")) {
                    int roll = payload.optInt("die_roll", 8);
                    int temp = roll + level;
                    return mark(s, "paladin_inspiring_smite", new JSONObject().put("temp_hp_pool", temp));
                }
                if (f.contains("aura de alacridade")) {
                    if (level < 7) return blocked(s, "requires_level_7");
                    return mark(s, "paladin_aura_of_alacrity", new JSONObject().put("speed_bonus_ft", 10).put("radius_ft", 10));
                }
                if (f.contains("lenda viva")) {
                    if (level < 20) return blocked(s, "requires_level_20");
                    return mark(s, "paladin_living_legend", new JSONObject().put("cha_checks_advantage", true).put("reroll_attack_turn", true));
                }
            }

            // ================================================================
            // 8. GUARDIÃO
            // ================================================================
            // Mestre das Feras (Beast Master)
            if (k.contains("mestre das feras") || k.contains("beast master")) {
                if (f.contains("companheiro primitivo")) {
                    int hp = 5 + 5 * level;
                    int ac = 13 + rules.proficiencyBonus(level);
                    return mark(s, "beast_master_primal_companion", new JSONObject().put("beast_hp", hp).put("beast_ac", ac).put("pb", rules.proficiencyBonus(level)));
                }
                if (f.contains("treinamento excepcional")) {
                    if (level < 7) return blocked(s, "requires_level_7");
                    return mark(s, "beast_master_exceptional_training", new JSONObject().put("bonus_action_command", true).put("magic_attacks", true));
                }
                if (f.contains("furia bestial")) {
                    if (level < 11) return blocked(s, "requires_level_11");
                    return mark(s, "beast_master_bestial_fury", new JSONObject().put("beast_two_attacks", true));
                }
                if (f.contains("partilha de magias")) {
                    if (level < 15) return blocked(s, "requires_level_15");
                    return mark(s, "beast_master_share_spells", new JSONObject().put("share_spells_active", true));
                }
            }

            // Andarilho Feérico (Fey Wanderer)
            if (k.contains("andarilho feerico") || k.contains("fey wanderer")) {
                if (f.contains("golpes pavorosos") || f.contains("dreadful strikes")) {
                    return mark(s, "fey_wanderer_dreadful_strikes", new JSONObject().put("extra_damage_dice", "1d4").put("damage_type", "psychic"));
                }
                if (f.contains("glamour sobrenatural")) {
                    int wisMod = Math.max(0, rules.modifier(s.optInt("wis", 10)));
                    s.put("fey_otherworldly_presence_bonus", wisMod);
                    return mark(s, "fey_wanderer_otherworldly_presence", new JSONObject().put("cha_check_bonus", wisMod));
                }
                if (f.contains("torcao cativante") || f.contains("distorcao")) {
                    if (level < 7) return blocked(s, "requires_level_7");
                    return mark(s, "fey_wanderer_beguiling_twist", new JSONObject().put("redirect_charm_fear", true));
                }
                if (f.contains("andarilho das brumas") || f.contains("passo feerico")) {
                    if (level < 15) return blocked(s, "requires_level_15");
                    if (!spend(s, "misty_step_fey", 1)) return blocked(s, "misty_step_fey");
                    return mark(s, "fey_wanderer_misty_wanderer", new JSONObject().put("free_misty_step_cast", true));
                }
            }

            // Caçador (Hunter)
            if (k.contains("cacador") || k.contains("hunter")) {
                if (f.contains("presa do cacador") || f.contains("colosso")) {
                    return mark(s, "hunter_colossus_slayer", new JSONObject().put("extra_damage_dice", "1d8").put("condition", "target_below_max_hp"));
                }
                if (f.contains("taticas defensivas")) {
                    if (level < 7) return blocked(s, "requires_level_7");
                    return mark(s, "hunter_defensive_tactics", new JSONObject().put("evasion_multiattack_defense", true));
                }
                if (f.contains("defesa superior do cacador")) {
                    if (level < 11) return blocked(s, "requires_level_11");
                    return mark(s, "hunter_superior_hunters_defense", new JSONObject().put("uncanny_dodge_granted", true));
                }
                if (f.contains("presa superior do cacador")) {
                    if (level < 15) return blocked(s, "requires_level_15");
                    return mark(s, "hunter_superior_hunters_prey", new JSONObject().put("multi_target_slayer", true));
                }
            }

            // Perseguidor Sombrio (Gloom Stalker)
            if (k.contains("perseguidor sombrio") || k.contains("gloom stalker")) {
                if (f.contains("emboscador pavoroso") || f.contains("dread ambusher")) {
                    int wisMod = Math.max(0, rules.modifier(s.optInt("wis", 10)));
                    return mark(s, "gloom_stalker_dread_ambusher", new JSONObject().put("initiative_bonus", wisMod).put("turn_one_speed_bonus_ft", 10).put("turn_one_extra_damage", "1d8"));
                }
                if (f.contains("visao umbral")) {
                    s.put("darkvision_ft", Math.max(s.optInt("darkvision_ft", 0), 60));
                    s.put("invisible_to_darkvision", true);
                    return mark(s, "gloom_stalker_umbral_sight", new JSONObject().put("darkvision_ft", 60).put("invisible_to_darkvision", true));
                }
                if (f.contains("mente de ferro")) {
                    if (level < 7) return blocked(s, "requires_level_7");
                    s.put("wis_save_proficiency", true);
                    return mark(s, "gloom_stalker_iron_mind", new JSONObject().put("wis_save_proficient", true));
                }
                if (f.contains("esquiva sombria")) {
                    if (level < 15) return blocked(s, "requires_level_15");
                    return mark(s, "gloom_stalker_shadowy_dodge", new JSONObject().put("reaction_disadvantage_imposed", true));
                }
            }

            // ================================================================
            // 9. LADINO
            // ================================================================
            // Trapaceiro Arcano (Arcane Trickster)
            if (k.contains("trapaceiro arcano") || k.contains("arcane trickster")) {
                if (f.contains("maos magicas trapaceiras") || f.contains("legerdemain")) {
                    s.put("mage_hand_legerdemain", true);
                    return mark(s, "arcane_trickster_mage_hand", new JSONObject().put("bonus_action_mage_hand", true));
                }
                if (f.contains("conjuracao de trapaceiro")) {
                    return mark(s, "arcane_trickster_spellcasting", new JSONObject().put("ability", "intelligence"));
                }
                if (f.contains("emboscada magica")) {
                    if (level < 9) return blocked(s, "requires_level_9");
                    return mark(s, "arcane_trickster_magical_ambush", new JSONObject().put("targets_save_disadvantage", true));
                }
                if (f.contains("ladrao de magias")) {
                    if (level < 17) return blocked(s, "requires_level_17");
                    String spell = payload.optString("stolen_spell", "Bola de Fogo");
                    return mark(s, "arcane_trickster_spell_thief", new JSONObject().put("stolen_spell", spell));
                }
            }

            // Assassino (Assassin)
            if (k.contains("assassino") || k.contains("assassin")) {
                if (f.contains("assassinar")) {
                    return mark(s, "assassin_assassinate", new JSONObject().put("advantage_on_unacted", true).put("auto_crit_on_surprised", true));
                }
                if (f.contains("proficiencias do assassino")) {
                    return mark(s, "assassin_proficiencies", new JSONObject().put("kits_granted", "disguise_kit, poisoners_kit"));
                }
                if (f.contains("pericia de infiltracao")) {
                    if (level < 9) return blocked(s, "requires_level_9");
                    return mark(s, "assassin_infiltration_expertise", new JSONObject().put("identity_forged", true));
                }
                if (f.contains("golpe mortal")) {
                    if (level < 17) return blocked(s, "requires_level_17");
                    int dc = 8 + rules.proficiencyBonus(level) + rules.modifier(s.optInt("dex", 10));
                    int save = payload.optInt("con_save", -1);
                    boolean fail = save < dc;
                    return mark(s, "assassin_death_strike", new JSONObject().put("dc", dc).put("con_save", save).put("double_damage_applied", fail));
                }
            }

            // Ladrão (Thief)
            if (k.contains("ladrao") || k.contains("thief")) {
                if (f.contains("maos rapidas")) {
                    return mark(s, "thief_fast_hands", new JSONObject().put("bonus_action_use_object_granted", true));
                }
                if (f.contains("trabalho nas alturas") || f.contains("segundo andar")) {
                    s.put("climb_speed_equals_walk", true);
                    return mark(s, "thief_second_story_work", new JSONObject().put("climb_speed_match", true));
                }
                if (f.contains("furtividade suprema")) {
                    if (level < 9) return blocked(s, "requires_level_9");
                    return mark(s, "thief_supreme_sneak", new JSONObject().put("stealth_advantage_granted", true));
                }
                if (f.contains("reflexos de ladrao")) {
                    if (level < 17) return blocked(s, "requires_level_17");
                    return mark(s, "thief_reflexes", new JSONObject().put("two_turns_first_round", true));
                }
            }

            // Mente de Alma (Soulknife)
            if (k.contains("mente de alma") || k.contains("soulknife")) {
                if (f.contains("laminas psiquicas")) {
                    return mark(s, "soulknife_psychic_blades", new JSONObject().put("main_blade_damage", "1d6").put("bonus_blade_damage", "1d4").put("damage_type", "psychic"));
                }
                if (f.contains("poder psionico") || f.contains("genio psionico")) {
                    if (!spend(s, "psionic_energy_dice", 1)) return blocked(s, "psionic_energy_dice");
                    int roll = payload.optInt("die_roll", 3);
                    return mark(s, "soulknife_psi_bolstered_knack", new JSONObject().put("die_roll", roll).put("psionic_energy_dice_spent", 1));
                }
                if (f.contains("laminas da alma")) {
                    if (level < 9) return blocked(s, "requires_level_9");
                    return mark(s, "soulknife_soul_blades", new JSONObject().put("teleport_range_ft", 60));
                }
                if (f.contains("dilacerar mente")) {
                    if (level < 17) return blocked(s, "requires_level_17");
                    if (!spend(s, "psionic_energy_dice", 3)) return blocked(s, "psionic_energy_dice");
                    return mark(s, "soulknife_rend_mind", new JSONObject().put("stun_duration_minutes", 1).put("psionic_energy_dice_spent", 3));
                }
            }

            // ================================================================
            // 10. FEITICEIRO
            // ================================================================
            // Aberrante
            if (k.contains("aberrante") || k.contains("aberrant")) {
                if (f.contains("fala telepatica")) {
                    return mark(s, "aberrant_telepathic_speech", new JSONObject().put("range_ft", 30));
                }
                if (f.contains("feiticaria psionica")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    int pts = payload.optInt("points_spent", 1);
                    if (!spend(s, "sorcery_points", pts)) return blocked(s, "sorcery_points");
                    return mark(s, "aberrant_psionic_sorcery", new JSONObject().put("points_spent", pts).put("no_components", true));
                }
                if (f.contains("revelacao na carne")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    return mark(s, "aberrant_revelation_in_flesh", new JSONObject().put("fly_swim_see_invisible", true));
                }
            }

            // Mecânica (Clockwork)
            if (k.contains("mecanica") || k.contains("clockwork")) {
                if (f.contains("restaurar o equilibrio") || f.contains("restauracao")) {
                    if (!spend(s, "restore_balance", 1)) return blocked(s, "restore_balance");
                    return mark(s, "clockwork_restore_balance", new JSONObject().put("cancel_advantage_disadvantage", true).put("range_ft", 60));
                }
                if (f.contains("bastiao da lei")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    int pts = Math.min(5, Math.max(1, payload.optInt("points_spent", 1)));
                    if (!spend(s, "sorcery_points", pts)) return blocked(s, "sorcery_points");
                    return mark(s, "clockwork_bastion_of_law", new JSONObject().put("damage_ward_dice", pts + "d8").put("sorcery_points_spent", pts));
                }
                if (f.contains("transe da ordem")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    if (!spend(s, "sorcery_points", 5)) return blocked(s, "sorcery_points");
                    return mark(s, "clockwork_trance_of_order", new JSONObject().put("minimum_d20_roll", 10).put("duration_minutes", 1).put("sorcery_points_spent", 5));
                }
            }

            // Dracônica
            if (k.contains("draconica") || k.contains("draconic")) {
                if (f.contains("resiliencia draconica")) {
                    int dexMod = rules.modifier(s.optInt("dex", 10));
                    int ac = 13 + dexMod;
                    s.put("draconic_ac", ac);
                    s.put("draconic_hp_bonus", level);
                    return mark(s, "draconic_resilience", new JSONObject().put("base_ac", ac).put("hp_bonus", level));
                }
                if (f.contains("afinidade elemental")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    int chaMod = Math.max(0, rules.modifier(s.optInt("cha", 10)));
                    return mark(s, "draconic_elemental_affinity", new JSONObject().put("elemental_damage_bonus", chaMod));
                }
                if (f.contains("asas de dragao")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    s.put("fly_speed_equals_walk", true);
                    return mark(s, "draconic_dragon_wings", new JSONObject().put("flying_active", true));
                }
                if (f.contains("companheiro dragao") || f.contains("presenca draconica")) {
                    if (level < 18) return blocked(s, "requires_level_18");
                    return mark(s, "draconic_presence", new JSONObject().put("aura_charm_fear", true).put("radius_ft", 60));
                }
            }

            // Magia Selvagem
            if (k.contains("magia selvagem") || k.contains("wild magic")) {
                if (f.contains("surto de magia selvagem") || f.contains("ondas do caos") || f.contains("mares do caos")) {
                    if (!spend(s, "tides_of_chaos", 1)) return blocked(s, "tides_of_chaos");
                    return mark(s, "wild_magic_tides_of_chaos", new JSONObject().put("advantage_granted", true).put("tides_of_chaos_spent", 1));
                }
                if (f.contains("dobrar a sorte")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    if (!spend(s, "sorcery_points", 2)) return blocked(s, "sorcery_points");
                    int d4 = payload.optInt("die_roll", 2);
                    return mark(s, "wild_magic_bend_luck", new JSONObject().put("die_roll_d4", d4).put("sorcery_points_spent", 2));
                }
                if (f.contains("caos controlado")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    return mark(s, "wild_magic_controlled_chaos", new JSONObject().put("roll_twice_choose_surge", true));
                }
            }

            // ================================================================
            // 11. BRUXO
            // ================================================================
            // Arquifada (Archfey)
            if (k.contains("arquifada") || k.contains("archfey")) {
                if (f.contains("presenca feerica") || f.contains("fuga nebulosa") || f.contains("passo feerico")) {
                    if (!spend(s, "misty_step_archfey", 1)) return blocked(s, "misty_step_archfey");
                    return mark(s, "archfey_misty_step", new JSONObject().put("free_misty_step", true).put("taunt_frighten_effect", true));
                }
                if (f.contains("defesas cativantes")) {
                    if (level < 10) return blocked(s, "requires_level_10");
                    s.put("immune_charmed", true);
                    return mark(s, "archfey_beguiling_defense", new JSONObject().put("immune_charmed", true).put("reflect_charm", true));
                }
                if (f.contains("delirio sombrio")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    return mark(s, "archfey_dark_delirium", new JSONObject().put("illusion_trap_active", true));
                }
            }

            // Celestial
            if (k.contains("celestial")) {
                if (f.contains("luz curativa")) {
                    int diceCount = Math.min(Math.max(1, rules.modifier(s.optInt("cha", 10))), payload.optInt("dice_spent", 1));
                    if (!spend(s, "healing_light", diceCount)) return blocked(s, "healing_light");
                    int roll = payload.optInt("heal_roll", 4 * diceCount);
                    return mark(s, "celestial_healing_light", new JSONObject().put("healing_amount", roll).put("dice_spent", diceCount));
                }
                if (f.contains("alma radiante")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    int chaMod = Math.max(0, rules.modifier(s.optInt("cha", 10)));
                    s.put("resistance_radiant", true);
                    return mark(s, "celestial_radiant_soul", new JSONObject().put("damage_bonus_cha", chaMod).put("resistance_radiant", true));
                }
                if (f.contains("vinganca incandescente")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    int maxHp = s.optInt("max_hp", 20);
                    s.put("hp", maxHp / 2);
                    int chaMod = Math.max(0, rules.modifier(s.optInt("cha", 10)));
                    return mark(s, "celestial_searing_vengeance", new JSONObject().put("new_hp", maxHp / 2).put("burst_damage_dice", "2d8 + " + chaMod).put("blinded_enemies", true));
                }
            }

            // Corruptor (Fiend)
            if (k.contains("corruptor") || k.contains("fiend")) {
                if (f.contains("bencao do obscuro") || f.contains("blessing")) {
                    int chaMod = Math.max(0, rules.modifier(s.optInt("cha", 10)));
                    int temp = chaMod + level;
                    s.put("temp_hp", Math.max(s.optInt("temp_hp", 0), temp));
                    return mark(s, "warlock_dark_ones_blessing", new JSONObject().put("temp_hp_granted", temp));
                }
                if (f.contains("sorte do obscuro") || f.contains("sorte do proprio obscuro") || f.contains("luck")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    if (!spend(s, "dark_ones_own_luck", 1)) return blocked(s, "dark_ones_own_luck");
                    if (!payload.has("d10_roll")) return pending(s, "d10_roll");
                    int d10 = payload.optInt("d10_roll", 1);
                    return mark(s, "warlock_dark_ones_luck", new JSONObject().put("d10_bonus", d10));
                }
                if (f.contains("resiliencia infernal")) {
                    if (level < 10) return blocked(s, "requires_level_10");
                    String resType = payload.optString("damage_type", "fire");
                    s.put("fiendish_resilience_type", resType);
                    return mark(s, "fiend_fiendish_resilience", new JSONObject().put("resistance_granted", resType));
                }
                if (f.contains("arremessar ao inferno") || f.contains("projetar no inferno")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    return mark(s, "fiend_hurl_through_hell", new JSONObject().put("psychic_damage_dice", "10d10").put("banished_until_next_turn", true));
                }
            }

            // Grande Antigo (Great Old One)
            if (k.contains("grande antigo") || k.contains("great old one")) {
                if (f.contains("mente desperta")) {
                    return mark(s, "great_old_one_awakened_mind", new JSONObject().put("telepathy_range_ft", 30));
                }
                if (f.contains("protecao entropica")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    if (!spend(s, "entropic_ward", 1)) return blocked(s, "entropic_ward");
                    return mark(s, "great_old_one_entropic_ward", new JSONObject().put("attacker_disadvantage", true).put("next_attack_advantage", true));
                }
                if (f.contains("escudo do pensamento")) {
                    if (level < 10) return blocked(s, "requires_level_10");
                    s.put("reflect_psychic_damage", true);
                    return mark(s, "great_old_one_thought_shield", new JSONObject().put("immune_mind_reading", true).put("reflect_psychic", true));
                }
                if (f.contains("criar lacaio")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    String target = payload.optString("target_id", "");
                    return mark(s, "great_old_one_create_thrall", new JSONObject().put("target_id", target).put("permanent_thrall_active", true));
                }
            }

            // ================================================================
            // 12. MAGO
            // ================================================================
            // Abjuração
            if (k.contains("abjuracao") || k.contains("abjuration")) {
                if (f.contains("protecao arcana")) {
                    int intMod = Math.max(0, rules.modifier(s.optInt("int_score", s.optInt("int", 10))));
                    int wardHp = 2 * level + intMod;
                    s.put("arcane_ward_max_hp", wardHp);
                    return mark(s, "abjuration_arcane_ward", new JSONObject().put("ward_max_hp", wardHp));
                }
                if (f.contains("protecao projetada")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    String ally = payload.optString("ally_id", "");
                    return mark(s, "abjuration_projected_ward", new JSONObject().put("protected_ally_id", ally).put("range_ft", 30));
                }
                if (f.contains("quebrador de magias")) {
                    if (level < 10) return blocked(s, "requires_level_10");
                    return mark(s, "abjuration_spell_breaker", new JSONObject().put("pb_bonus_counterspell_dispel", rules.proficiencyBonus(level)));
                }
                if (f.contains("resistencia a magia")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    s.put("spell_resistance_advantage", true);
                    return mark(s, "abjuration_spell_resistance", new JSONObject().put("spell_saves_advantage", true).put("spell_damage_resistance", true));
                }
            }

            // Adivinhação
            if (k.contains("adivinhacao") || k.contains("divination")) {
                if (f.contains("pressagio maior")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    return mark(s, "divination_greater_portent", new JSONObject().put("portent_dice_count", 3));
                }
                if (f.contains("pressagio")) {
                    if (!spend(s, "portent_dice", 1)) return blocked(s, "portent_dice");
                    int stored = payload.optInt("portent_roll", 15);
                    return mark(s, "divination_portent", new JSONObject().put("substituted_roll", stored).put("portent_dice_spent", 1));
                }
                if (f.contains("adivinhacao perita")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    int spLvl = payload.optInt("spell_level", 2);
                    return mark(s, "divination_expert_divination", new JSONObject().put("refunded_slot_level", spLvl - 1));
                }
                if (f.contains("o terceiro olho")) {
                    if (level < 10) return blocked(s, "requires_level_10");
                    String mode = payload.optString("mode", "darkvision");
                    return mark(s, "divination_third_eye", new JSONObject().put("mode", mode));
                }
            }

            // Evocação
            if (k.contains("evocacao") || k.contains("evoker")) {
                if (f.contains("esculpir magias") || f.contains("sculpt")) {
                    int spellLevel = payload.optInt("spell_level", 1);
                    int safeCount = 1 + spellLevel;
                    return mark(s, "wizard_sculpt_spells", new JSONObject().put("safe_creatures_max", safeCount).put("spell_level", spellLevel));
                }
                if (f.contains("truque potente")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    return mark(s, "evocation_potent_cantrip", new JSONObject().put("half_damage_on_cantrip_save", true));
                }
                if (f.contains("evocacao potencializada")) {
                    if (level < 10) return blocked(s, "requires_level_10");
                    int intMod = Math.max(0, rules.modifier(s.optInt("int_score", s.optInt("int", 10))));
                    return mark(s, "evocation_empowered_evocation", new JSONObject().put("damage_bonus_int", intMod));
                }
                if (f.contains("sobrecanalizar") || f.contains("sobrecarga")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    if (!spend(s, "overchannel", 1)) return blocked(s, "overchannel");
                    return mark(s, "evocation_overchannel", new JSONObject().put("maximize_spell_damage", true).put("max_spell_level", 5));
                }
            }

            // Ilusão
            if (k.contains("ilusao") || k.contains("illusion")) {
                if (f.contains("ilusao menor aprimorada")) {
                    return mark(s, "illusion_improved_minor_illusion", new JSONObject().put("sound_and_image_simultaneous", true));
                }
                if (f.contains("ilusoes maleaveis")) {
                    if (level < 6) return blocked(s, "requires_level_6");
                    return mark(s, "illusion_malleable_illusions", new JSONObject().put("modify_active_illusion_free", true));
                }
                if (f.contains("eu ilusorio")) {
                    if (level < 10) return blocked(s, "requires_level_10");
                    if (!spend(s, "illusory_self", 1)) return blocked(s, "illusory_self");
                    return mark(s, "illusion_illusory_self", new JSONObject().put("auto_miss_reaction", true));
                }
                if (f.contains("realidade ilusoria")) {
                    if (level < 14) return blocked(s, "requires_level_14");
                    return mark(s, "illusion_illusory_reality", new JSONObject().put("solid_illusion_minutes", 1));
                }
            }

            return s.put("subclass_action_resolved", false).put("subclass_action_reason", "unknown_subclass_feature").put("feature", feature);
        } catch (Exception e) {
            return s;
        }
    }
    private static JSONObject mark(JSONObject s, String event, JSONObject effect) throws Exception {
        return s.put("subclass_action_resolved", true)
                .put("subclass_action_event", event)
                .put("subclass_action_effect", effect)
                .put("subclass_action_auto_rolls", false);
    }

    private static JSONObject pending(JSONObject s, String need) throws Exception {
        return s.put("subclass_action_resolved", false)
                .put("subclass_action_status", "requires_player_or_gm_resolution")
                .put("subclass_action_required_input", need)
                .put("subclass_action_auto_rolls", false);
    }

    private static JSONObject blocked(JSONObject s, String why) throws Exception {
        return s.put("subclass_action_resolved", false)
                .put("subclass_action_status", "blocked")
                .put("subclass_action_reason", why)
                .put("subclass_action_auto_rolls", false);
    }

    private static JSONObject resource(JSONObject all, String key, int max, String recharge) throws Exception {
        JSONObject r = all.optJSONObject(key);
        if (r == null) r = new JSONObject().put("current", max);
        int cur = Math.max(0, Math.min(max, r.optInt("current", max)));
        r.put("max", max).put("current", cur).put("recharge", recharge).put("source", SOURCE_BOOK);
        all.put(key, r);
        return r;
    }

    private static JSONObject res(JSONObject s, String key) {
        JSONObject all = s.optJSONObject("subclass_resources");
        if (all != null && all.has(key)) return all.optJSONObject(key);
        JSONObject classRes = s.optJSONObject("class_resources");
        if (classRes != null && classRes.has(key)) return classRes.optJSONObject(key);
        return null;
    }

    private static boolean spend(JSONObject s, String key, int n) throws Exception {
        JSONObject r = res(s, key);
        if (r == null || r.optInt("current", 0) < n) return false;
        r.put("current", r.optInt("current", 0) - n);
        return true;
    }

    private static JSONObject copy(JSONObject o) {
        try {
            return o == null ? new JSONObject() : new JSONObject(o.toString());
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static String canonicalClass(String className) {
        String cls = norm(className);
        switch (cls) {
            case "barbaro": case "barbarian": return "barbaro";
            case "bardo": case "bard": return "bardo";
            case "clerigo": case "cleric": return "clerigo";
            case "druida": case "druid": return "druida";
            case "guerreiro": case "fighter": return "guerreiro";
            case "monge": case "monk": return "monge";
            case "paladino": case "paladin": return "paladino";
            case "guardiao": case "ranger": return "guardiao";
            case "ladino": case "rogue": return "ladino";
            case "feiticeiro": case "sorcerer": return "feiticeiro";
            case "bruxo": case "warlock": return "bruxo";
            case "mago": case "wizard": return "mago";
            default: return "";
        }
    }
}
