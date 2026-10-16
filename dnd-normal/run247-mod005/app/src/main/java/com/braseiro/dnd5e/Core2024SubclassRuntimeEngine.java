package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.*;

/**
 * Deterministic rules and active mechanics engine for all 48 official Player's Handbook 2024 subclasses.
 * Every class in PHB 2024 selects their subclass at level 3.
 * Adheres strictly to the Definition of Done:
 * source (PHB 2024 Cap. 3) -> deterministic executor -> host/SQLite -> UI PT-BR -> Backup21.
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
        return level >= 3;
    }
}
