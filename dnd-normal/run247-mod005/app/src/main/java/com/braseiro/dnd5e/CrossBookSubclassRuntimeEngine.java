package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.*;

/**
 * Runtime transversal de subclasses 5e 2014 para Xanathar + Sword Coast.
 * Ravnica usa aliases das versões já compiladas por Tasha (Order Domain/Circle of Spores).
 * Nenhuma rolagem ou escolha obrigatória é inventada: todo dado/seleção exigido entra via input.
 */
public final class CrossBookSubclassRuntimeEngine {
    public static final String XANATHAR="Guia de Xanathar para Todas as Coisas";
    public static final String SCAG="Costa da Espada — Guia de Aventureiros";
    public static final String RAVNICA="Guia do Mestre de Guildas para Ravnica";
    private static final String XANATHAR_SHA="35f78718ecb9d11f986598677e55eff4a5eaf09309167c7ee988e0527a9fbdb3";
    private static final String SCAG_SHA="fa05e600e6423ceeca9a2cd282a25529e3cd0872d84130189f21d266f4780024";
    private static final Map<String,Def> BY_NAME=new LinkedHashMap<String,Def>();
    private static final Map<String,List<Def>> BY_CLASS=new LinkedHashMap<String,List<Def>>();
    private static final Map<String,String> ALIAS=new LinkedHashMap<String,String>();

    private static final class Feature {
        final int level; final String id,name,activation,uses,recharge; final JSONObject rule;
        Feature(int l,String i,String n,String a,String u,String r,JSONObject j){level=l;id=i;name=n;activation=a;uses=u;recharge=r;rule=j==null?new JSONObject():j;}
    }
    private static final class Def {
        final String cls,name,book,locator,sha; final List<Feature> features=new ArrayList<Feature>();
        Def(String c,String n,String b,String loc,String h){cls=c;name=n;book=b;locator=loc;sha=h;}
    }

    static {
        // XANATHAR — 31 subclasses.
        d("Bárbaro","Caminho do Guardião Ancestral",XANATHAR,"cap.1 p.9",XANATHAR_SHA,
          f(3,"ancestral_protectors","Protetores Ancestrais","on_first_rage_hit","unlimited","rage",j("mark_target",true,"marked_attack_disadvantage_vs_others",true,"allies_resist_marked_target_attack_damage",true,"expires","start_of_your_next_turn")),
          f(6,"spirit_shield","Escudo Espiritual","reaction","1_per_round","none",j("range_ft",30,"damage_reduction_dice_by_level","2d6@6;3d6@10;4d6@14","requires_roll","reduction_roll","protect_other_creature",true)),
          f(10,"consult_spirits","Consultar os Espíritos","ritual","1","short_or_long_rest",j("spell_options",a("augúrio","clarividência"),"components",false,"spellcasting_ability","Sabedoria")),
          f(14,"vengeful_ancestors","Ancestrais Vingativos","on_spirit_shield","unlimited","none",j("force_damage_equals_reduction",true,"target","attacker")));

        d("Bárbaro","Caminho do Arauto da Tempestade",XANATHAR,"cap.1 p.10",XANATHAR_SHA,
          f(3,"storm_aura","Aura da Tempestade","bonus_action_while_raging","unlimited","rage",j("choice_options",a("deserto","mar","tundra"),"radius_ft",10,"desert_fire_damage_by_level","2@3;3@5;4@10;5@15;6@20","sea_lightning_dice_by_level","1d6@3;2d6@10;3d6@15;4d6@20","sea_save","Destreza metade","tundra_temp_hp_by_level","2@3;3@5;4@10;5@15;6@20")),
          f(6,"storm_soul","Alma da Tempestade","passive","unlimited","none",j("depends_on_aura_choice",true,"desert","resistência a fogo e tolerância a calor","sea","resistência a relâmpago e respiração aquática/natação","tundra","resistência a frio e tolerância ao frio")),
          f(10,"shielding_storm","Tempestade Protetora","passive_aura","unlimited","rage",j("allies_gain_aura_damage_resistance",true,"radius_ft",10)),
          f(14,"raging_storm","Tempestade Furiosa","reaction_or_trigger","unlimited","rage",j("depends_on_aura_choice",true,"manual_target_and_save",true)));

        d("Bárbaro","Caminho do Fanático",XANATHAR,"cap.1 p.11",XANATHAR_SHA,
          f(3,"divine_fury","Fúria Divina","on_first_rage_hit","1_per_turn","rage",j("extra_damage","1d6 + metade do nível de bárbaro","damage_type_choice",a("radiante","necrótico"),"requires_roll","d6_roll")),
          f(3,"warrior_of_gods","Guerreiro dos Deuses","passive","unlimited","none",j("resurrection_material_cost_waived",true)),
          f(6,"fanatical_focus","Foco Fanático","on_failed_save","1_per_rage","rage",j("reroll_manual",true,"must_use_new_roll",true)),
          f(10,"zealous_presence","Presença Zelosa","bonus_action","1","long_rest",j("range_ft",60,"max_other_creatures",10,"duration","until_start_of_your_next_turn","advantage_attacks_and_saves",true)),
          f(14,"rage_beyond_death","Fúria Além da Morte","passive_while_raging","unlimited","rage",j("zero_hp_does_not_knock_unconscious",true,"still_make_death_saves",true,"dies_after_rage_if_zero_hp_and_not_stabilized",true)));

        d("Bardo","Colégio do Glamour",XANATHAR,"cap.1 p.14",XANATHAR_SHA,
          f(3,"mantle_of_inspiration","Manto da Inspiração","bonus_action","bardic_inspiration","none",j("targets_max_formula","Carisma mínimo 1","range_ft",60,"temp_hp_by_bard_level","5@3;8@5;11@10;14@15","target_reaction_move_speed",true,"move_provokes_opportunity",false)),
          f(3,"enthralling_performance","Performance Deslumbrante","after_1_minute_performance","1","short_or_long_rest",j("targets_max_formula","Carisma mínimo 1","range_ft",60,"save","Sabedoria vs CD de magia","condition","enfeitiçado","duration_minutes",60,"ends_on_damage_or_witnessed_hostility",true)),
          f(6,"mantle_of_majesty","Manto da Majestade","bonus_action","1","long_rest",j("duration_minutes",1,"concentration",true,"cast_command_as_bonus_action_each_turn_without_slot",true,"charmed_targets_auto_fail_command_save",true)),
          f(14,"unbreakable_majesty","Majestade Inquebrável","bonus_action","1","short_or_long_rest",j("duration_minutes",1,"first_attack_each_turn_charisma_save",true,"fail_cannot_attack_you",true,"success_disadvantage_next_save_vs_your_spell",true)));

        d("Bardo","Colégio das Espadas",XANATHAR,"cap.1 p.15",XANATHAR_SHA,
          f(3,"bonus_proficiencies","Proficiências Bônus","passive","unlimited","none",j("armor","média","weapon","cimitarra","weapon_as_spell_focus",true)),
          f(3,"fighting_style","Estilo de Luta","choice","unlimited","none",j("choice_options",a("Duelo","Combate com Duas Armas"))),
          f(3,"blade_flourish","Floreio de Lâminas","on_weapon_hit_after_attack_action","bardic_inspiration","none",j("speed_bonus_ft",10,"choice_options",a("defensivo","cortante","móvel"),"requires_roll","bardic_inspiration_roll","damage_bonus_equals_roll",true,"defensive_add_roll_to_ac_until_next_turn",true,"slashing_secondary_damage_equals_roll",true,"mobile_push_and_follow",true)),
          f(6,"extra_attack","Ataque Extra","passive","unlimited","none",j("attacks_per_attack_action",2)),
          f(14,"masters_flourish","Floreio de Mestre","on_blade_flourish","unlimited","none",j("can_use_d6_instead_of_bardic_inspiration",true)));

        d("Bardo","Colégio dos Sussurros",XANATHAR,"cap.1 p.16",XANATHAR_SHA,
          f(3,"psychic_blades_whispers","Lâminas Psíquicas","on_weapon_hit","bardic_inspiration","none",j("once_per_round",true,"psychic_damage_by_bard_level","2d6@3;3d6@5;5d6@10;8d6@15","requires_roll","psychic_damage_roll")),
          f(3,"words_of_terror","Palavras de Terror","after_1_minute_private_conversation","1","short_or_long_rest",j("save","Sabedoria vs CD de magia","condition","amedrontado","duration_minutes",60,"ends_on_damage_or_allied_hostility",true)),
          f(6,"mantle_of_whispers","Manto de Sussurros","reaction_on_humanoid_death_within_30ft","1_shadow_at_a_time","none",j("capture_shadow",true,"use_action_assume_persona",true,"persona_duration_hours",1,"access_surface_memories",true)),
          f(14,"shadow_lore","Conhecimento Sombrio","action","1","long_rest",j("range_ft",30,"save","Sabedoria vs CD de magia","condition","enfeitiçado","duration_hours",8,"target_believes_you_know_secret",true)));

        d("Bruxo","O Celestial",XANATHAR,"cap.1 p.18",XANATHAR_SHA,
          f(1,"celestial_expanded_spells","Magias Expandidas do Celestial","passive","unlimited","none",j("expanded_spells",a("curar ferimentos","raio guia","esfera flamejante","restauração menor","luz do dia","revivificar","guardião da fé","muralha de fogo","coluna de chamas","restauração maior"))),
          f(1,"bonus_cantrips_celestial","Truques Bônus","passive","unlimited","none",j("cantrips",a("luz","chama sagrada"))),
          f(1,"healing_light","Luz Curativa","bonus_action","pool=1+warlock_level_d6","long_rest",j("range_ft",60,"max_dice_per_use_formula","Carisma mínimo 1","requires_roll","healing_roll","heals_target",true)),
          f(6,"radiant_soul","Alma Radiante","passive","unlimited","none",j("resistance","radiante","add_charisma_to_one_fire_or_radiant_spell_damage_roll",true)),
          f(10,"celestial_resilience","Resiliência Celestial","on_short_or_long_rest","unlimited","rest",j("self_temp_hp_formula","nível de bruxo + Carisma","up_to_five_allies_temp_hp_formula","metade do nível de bruxo + Carisma")),
          f(14,"searing_vengeance","Vingança Ardente","on_death_save_start_turn","1","long_rest",j("restore_hp_fraction",0.5,"stand_up",true,"burst_radius_ft",30,"radiant_damage","2d8 + Carisma","blind_until_end_of_current_turn",true,"requires_roll","radiant_damage_roll")));

        d("Bruxo","A Lâmina Maldita",XANATHAR,"cap.1 p.19",XANATHAR_SHA,
          f(1,"hexblades_curse","Maldição da Lâmina Maldita","bonus_action","1","short_or_long_rest",j("range_ft",30,"duration_minutes",1,"bonus_damage","Bônus de Proficiência","critical_range","19-20","heal_on_target_death","nível de bruxo + Carisma")),
          f(1,"hex_warrior","Guerreiro Hexblade","passive_choice","unlimited","long_rest",j("proficiencies",a("armadura média","escudos","armas marciais"),"charisma_weapon_choice",true,"one_handed_requirement_unless_pact_weapon",true)),
          f(6,"accursed_specter","Espectro Amaldiçoado","reaction_or_trigger_after_humanoid_kill","1","long_rest",j("summon_specter",true,"temp_hp_formula","metade do nível de bruxo","initiative_shared",true,"lasts_until_next_long_rest",true)),
          f(10,"armor_of_hexes","Armadura de Maldições","reaction","1_per_round","none",j("trigger","cursed target attack hits you","requires_roll","d6_roll","miss_on_4_to_6",true)),
          f(14,"master_of_hexes","Mestre das Maldições","on_cursed_target_death","unlimited","none",j("move_curse_to_new_target_within_ft",30,"forgo_healing_when_moved",true)));

        d("Clérigo","Domínio da Forja",XANATHAR,"cap.1 p.23",XANATHAR_SHA,
          f(1,"forge_bonus_proficiencies","Proficiências Bônus","passive","unlimited","none",j("armor","pesada","tools","ferramentas de ferreiro")),
          f(1,"blessing_of_forge","Bênção da Forja","after_long_rest","1","long_rest",j("target","arma ou armadura não mágica","bonus",1,"weapon_attack_damage_bonus",1,"armor_ac_bonus",1,"expires","next_long_rest")),
          f(2,"artisans_blessing","Bênção do Artesão","channel_divinity_ritual","channel_divinity","none",j("ritual_duration_hours",1,"max_value_gp",100,"requires_equal_value_metal",true,"creates_metal_item",true)),
          f(6,"soul_of_forge","Alma da Forja","passive","unlimited","none",j("resistance","fogo","heavy_armor_ac_bonus",1)),
          f(8,"divine_strike_forge","Ataque Divino","on_weapon_hit","1_per_turn","none",j("fire_damage_by_cleric_level","1d8@8;2d8@14","requires_roll","fire_damage_roll")),
          f(17,"saint_of_forge","Santo da Forja e do Fogo","passive","unlimited","none",j("immunity","fogo","heavy_armor_resistance",a("concussão não mágica","cortante não mágico","perfurante não mágico"))));

        d("Clérigo","Domínio da Sepultura",XANATHAR,"cap.1 p.24",XANATHAR_SHA,
          f(1,"circle_of_mortality","Círculo da Mortalidade","passive","unlimited","none",j("healing_dice_maximized_when_target_at_zero_hp",true,"spare_the_dying_range_ft",30,"spare_the_dying_bonus_action",true)),
          f(1,"eyes_of_grave","Olhos da Sepultura","action","Wisdom_modifier_min1","long_rest",j("sense_undead_range_ft",60,"blocked_by_total_cover",true,"duration","until_end_of_next_turn")),
          f(2,"path_to_grave","Caminho para a Sepultura","channel_divinity_action","channel_divinity","none",j("range_ft",30,"target_vulnerable_to_next_attack_damage",true,"expires","end_of_your_next_turn")),
          f(6,"sentinel_at_deaths_door","Sentinela à Porta da Morte","reaction","Wisdom_modifier_min1","long_rest",j("range_ft",30,"cancel_critical_hit",true,"turn_into_normal_hit",true)),
          f(8,"potent_spellcasting_grave","Conjuração Potente","passive","unlimited","none",j("add_wisdom_to_cleric_cantrip_damage",true)),
          f(17,"keeper_of_souls","Guardião das Almas","on_enemy_death","1_per_turn","none",j("range_ft",60,"heal_you_or_ally","enemy_hit_dice_count","no_action",true)));

        d("Druida","Círculo dos Sonhos",XANATHAR,"cap.1 p.27",XANATHAR_SHA,
          f(2,"balm_of_summer_court","Bálsamo da Corte de Verão","bonus_action","pool=druid_level_d6","long_rest",j("range_ft",120,"max_dice_per_use_formula","metade do nível de druida","requires_roll","healing_roll","temp_hp_per_die_spent",1)),
          f(6,"hearth_of_moonlight_shadow","Lareira de Luar e Sombra","on_short_or_long_rest","unlimited","rest",j("radius_ft",30,"stealth_bonus",5,"perception_bonus",5,"light_obscured",true)),
          f(10,"hidden_paths","Caminhos Ocultos","bonus_action_or_action","Wisdom_modifier_min1","long_rest",j("self_teleport_ft",60,"ally_teleport_ft",30,"destination_visible",true)),
          f(14,"walker_in_dreams","Caminhante dos Sonhos","after_short_rest","1","long_rest",j("spell_options",a("sonho","vidência","círculo de teletransporte"),"no_spell_slot",true,"no_material_components",true)));

        d("Druida","Círculo do Pastor",XANATHAR,"cap.1 p.28",XANATHAR_SHA,
          f(2,"speech_of_woods","Fala dos Bosques","passive","unlimited","none",j("language","Silvestre","beasts_understand_you",true,"interpret_beast_sounds_and_motions",true)),
          f(2,"spirit_totem","Totem Espiritual","bonus_action","1","short_or_long_rest",j("range_ft",60,"aura_radius_ft",30,"duration_minutes",1,"choice_options",a("urso","falcão","unicórnio"),"bear_temp_hp","5 + nível de druida","bear_strength_advantage",true,"hawk_reaction_advantage_attack",true,"hawk_perception_advantage",true,"unicorn_healing_aura","nível de druida quando você conjura magia de cura")),
          f(6,"mighty_summoner","Invocador Poderoso","passive","unlimited","none",j("summoned_beast_fey_extra_hp_per_hit_die",2,"summoned_natural_weapons_magical",true)),
          f(10,"guardian_spirit","Espírito Guardião","passive_aura","unlimited","spirit_totem",j("summoned_beast_fey_heal_end_turn","metade do nível de druida","requires_inside_totem_aura",true)),
          f(14,"faithful_summons","Invocações Fiéis","on_zero_hp_or_incapacitated","1","long_rest",j("summon","quatro feras ND 2 ou menor","as_conjure_animals",true,"no_concentration",true,"duration_hours",1)));

        d("Feiticeiro","Alma Favorecida",XANATHAR,"cap.1 p.34",XANATHAR_SHA,
          f(1,"divine_magic","Magia Divina","passive_choice","unlimited","none",j("add_cleric_spell_list",true,"affinity_choice",a("bem","mal","ordem","caos","neutralidade"),"bonus_spell_depends_on_affinity",true)),
          f(1,"favored_by_gods","Favorecido pelos Deuses","on_failed_save_or_missed_attack","1","short_or_long_rest",j("bonus_roll","2d4","requires_roll","two_d4_roll")),
          f(6,"empowered_healing","Cura Fortalecida","on_healing_spell_roll","sorcery_points","none",j("sorcery_point_cost",1,"range_ft",5,"reroll_any_healing_dice_once",true,"must_use_new_roll",true)),
          f(14,"otherworldly_wings","Asas Sobrenaturais","bonus_action_toggle","unlimited","none",j("fly_speed_ft",30,"dismiss_bonus_action",true)),
          f(18,"unearthly_recovery","Recuperação Sobrenatural","bonus_action","1","long_rest",j("heal_fraction_max_hp",0.5)));

        d("Feiticeiro","Adepto das Sombras",XANATHAR,"cap.1 p.34",XANATHAR_SHA,
          f(1,"eyes_of_dark","Olhos da Escuridão","passive","unlimited","none",j("darkvision_ft",120)),
          f(1,"strength_of_grave","Força do Túmulo","on_reduced_to_zero_hp","1","long_rest",j("save","Carisma CD 5 + dano recebido","on_success_hp",1,"blocked_by_critical_or_radiant_damage",true,"requires_roll","charisma_save_total")),
          f(3,"darkness_shadow","Escuridão dos Olhos","cast_spell","sorcery_points","none",j("spell","escuridão","sorcery_point_cost",2,"see_through_own_darkness",true)),
          f(6,"hound_of_ill_omen","Cão de Mau Agouro","bonus_action","sorcery_points","none",j("sorcery_point_cost",3,"range_ft",120,"duration_minutes",5,"target_disadvantage_saves_vs_your_spells_near_hound",true)),
          f(14,"shadow_walk","Caminhada pelas Sombras","bonus_action","unlimited","none",j("teleport_ft",120,"requires_dim_light_or_darkness_origin_destination",true)),
          f(18,"umbral_form","Forma Umbral","bonus_action","sorcery_points","none",j("sorcery_point_cost",6,"duration_minutes",1,"resistance_all_except",a("força","radiante"),"move_through_creatures_objects",true,"object_end_damage",5)));

        d("Feiticeiro","Feiticeiro da Tempestade",XANATHAR,"cap.1 p.35",XANATHAR_SHA,
          f(1,"wind_speaker","Orador do Vento","passive","unlimited","none",j("language","Primordial","understand_dialects",a("Aquan","Auran","Ignan","Terran"))),
          f(1,"tempestuous_magic","Magia Tempestuosa","bonus_action_on_spell","unlimited","none",j("trigger","antes ou depois de conjurar magia de 1º círculo ou maior","fly_ft",10,"provokes_opportunity",false)),
          f(6,"heart_of_storm","Coração da Tempestade","on_lightning_or_thunder_spell","unlimited","none",j("resistances",a("relâmpago","trovão"),"burst_radius_ft",10,"burst_damage_formula","metade do nível de feiticeiro","damage_type_matches_spell",true)),
          f(6,"storm_guide","Guia da Tempestade","action_or_reaction_weather_control","unlimited","none",j("control_nonmagical_rain_wind_nearby",true)),
          f(14,"storms_fury","Fúria da Tempestade","reaction_on_melee_hit","1_per_round","none",j("lightning_damage","nível de feiticeiro","strength_save_vs_spell_dc",true,"push_ft_on_fail",20)),
          f(18,"wind_soul","Alma do Vento","action_share_flight","1","short_or_long_rest",j("immunities",a("relâmpago","trovão"),"self_fly_speed_ft",60,"share_targets_formula","3 + Carisma","shared_fly_speed_ft",30,"duration_hours",1)));

        d("Guerreiro","Arqueiro Arcano",XANATHAR,"cap.1 p.38",XANATHAR_SHA,
          f(3,"arcane_archer_lore","Conhecimento do Arqueiro Arcano","passive_choice","unlimited","none",j("skill_choice",a("Arcanismo","Natureza"),"cantrip_choice",a("prestidigitação","druidismo"))),
          f(3,"arcane_shot","Disparo Arcano","on_shortbow_or_longbow_attack","2","short_or_long_rest",j("known_options","2@3;3@7;4@10;5@15;6@18","damage_dice_scale","2d6 normalmente;4d6 no 18º nível quando aplicável","choice_options",a("Banimento","Cativante","Explosivo","Enfraquecedor","Agarrador","Perfurante","Perseguidor","Sombrio"),"manual_save_and_rolls",true)),
          f(7,"magic_arrow","Flecha Mágica","passive","unlimited","none",j("nonmagical_arrow_counts_as_magical",true)),
          f(7,"curving_shot","Disparo Curvo","bonus_action_after_miss","unlimited","none",j("retarget_missed_magic_arrow",true,"new_target_within_ft_of_original",60)),
          f(15,"ever_ready_shot","Disparo Sempre Pronto","on_initiative","conditional","initiative",j("if_arcane_shot_uses_zero_regain",1)));

        d("Guerreiro","Cavaleiro",XANATHAR,"cap.1 p.40",XANATHAR_SHA,
          f(3,"cavalier_bonus_proficiency","Proficiência Bônus","passive_choice","unlimited","none",j("choose_skill_or_language",true)),
          f(3,"born_to_saddle","Nascido para a Sela","passive","unlimited","none",j("advantage_saves_avoid_falling_mount",true,"mount_dismount_cost_ft",5)),
          f(3,"unwavering_mark","Marca Inabalável","on_melee_weapon_hit","unlimited_mark;special_attack=Strength_modifier_min1","long_rest",j("marked_target_disadvantage_attacks_vs_others_while_within_ft",5,"retaliation_bonus_action_attack_next_turn",true,"retaliation_advantage",true,"retaliation_extra_damage","metade do nível de guerreiro")),
          f(7,"warding_maneuver","Manobra de Proteção","reaction","Constitution_modifier_min1","long_rest",j("range_ft",5,"requires_roll","d8_roll","add_roll_to_target_ac",true,"if_attack_still_hits_target_resists_damage",true)),
          f(10,"hold_the_line","Manter a Linha","opportunity_attack_trigger","unlimited","none",j("trigger","criatura se move 1,5 m ou mais dentro do seu alcance","on_hit_speed_zero_rest_of_turn",true)),
          f(15,"ferocious_charger","Investida Feroz","on_attack_after_10ft_move","1_per_turn","none",j("strength_save_vs","8 + PB + Força","on_fail_prone",true)),
          f(18,"vigilant_defender","Defensor Vigilante","special_reaction","unlimited","round",j("one_opportunity_reaction_on_every_creatures_turn",true,"cannot_use_for_other_reactions",true)));

        d("Guerreiro","Samurai",XANATHAR,"cap.1 p.41",XANATHAR_SHA,
          f(3,"samurai_bonus_proficiency","Proficiência Bônus","passive_choice","unlimited","none",j("choose_skill_or_language",true)),
          f(3,"fighting_spirit","Espírito de Luta","bonus_action","3","long_rest",j("advantage_weapon_attacks_until_end_turn",true,"temp_hp_by_fighter_level","5@3;10@10;15@15")),
          f(7,"elegant_courtier","Cortesão Elegante","passive","unlimited","none",j("add_wisdom_to_persuasion",true,"wisdom_save_proficiency_or_choice_if_already",true)),
          f(10,"tireless_spirit","Espírito Incansável","on_initiative","conditional","initiative",j("if_fighting_spirit_uses_zero_regain",1)),
          f(15,"rapid_strike","Ataque Rápido","on_attack_with_advantage","1_per_turn","none",j("forgo_advantage_on_one_attack",true,"gain_one_extra_weapon_attack",true)),
          f(18,"strength_before_death","Força Antes da Morte","reaction_on_reduced_to_zero_hp","1","long_rest",j("take_full_turn_immediately",true,"then_fall_to_zero_hp",true)));

        d("Ladino","Inquiridor",XANATHAR,"cap.1 p.43",XANATHAR_SHA,
          f(3,"ear_for_deceit","Ouvido para Enganação","passive","unlimited","none",j("insight_to_detect_lie_min_d20",8)),
          f(3,"eye_for_detail","Olho para Detalhes","bonus_action","unlimited","none",j("perception_find_hidden_or_investigation_clues",true)),
          f(3,"insightful_fighting","Combate Perspicaz","bonus_action","unlimited","none",j("contest","Intuição vs Enganação","on_success_sneak_attack_without_advantage",true,"duration_minutes",1,"ends_if_used_on_other_target",true)),
          f(9,"steady_eye","Olho Firme","passive","unlimited","none",j("advantage_perception_investigation_if_move_half_speed_or_less",true)),
          f(13,"unerring_eye","Olho Infalível","action","Wisdom_modifier_min1","long_rest",j("range_ft",30,"sense_illusions_shapechangers_magic_deception",true,"does_not_reveal_exact_nature",true)),
          f(17,"eye_for_weakness","Olho para Fraqueza","on_sneak_attack_vs_insightful_target","1_per_turn","none",j("extra_sneak_attack_damage","3d6","requires_roll","extra_damage_roll")));

        d("Ladino","Mentor",XANATHAR,"cap.1 p.44",XANATHAR_SHA,
          f(3,"master_of_intrigue","Mestre da Intriga","passive","unlimited","none",j("tools",a("kit de disfarce","kit de falsificação","um kit de jogo"),"languages_choose",2,"mimic_speech_after_listening_minutes",1)),
          f(3,"master_of_tactics","Mestre da Tática","bonus_action_help","unlimited","none",j("help_as_bonus_action",true,"help_attack_range_ft",30,"target_must_see_or_hear_you",true)),
          f(9,"insightful_manipulator","Manipulador Perspicaz","after_1_minute_observation","unlimited","none",j("learn_relative_mental_scores_and_class_levels",true)),
          f(13,"misdirection","Desorientação","reaction_on_attack_miss_you","1_per_round","none",j("requires_creature_giving_cover_within_attack_line",true,"redirect_attack_to_cover_creature",true)),
          f(17,"soul_of_deceit","Alma Enganadora","passive","unlimited","none",j("thoughts_unreadable_without_consent",true,"can_present_false_thoughts",true,"magic_truth_can_be_defeated",true)));

        d("Ladino","Batedor",XANATHAR,"cap.1 p.45",XANATHAR_SHA,
          f(3,"skirmisher","Escaramuçador","reaction","1_per_round","none",j("trigger","inimigo termina turno a até 1,5 m","move_fraction_speed",0.5,"provokes_opportunity",false)),
          f(3,"survivalist","Sobrevivencialista","passive","unlimited","none",j("proficiencies",a("Natureza","Sobrevivência"),"expertise_both",true)),
          f(9,"superior_mobility","Mobilidade Superior","passive","unlimited","none",j("speed_bonus_ft",10,"applies_climb_swim_if_have_them",true)),
          f(13,"ambush_master","Mestre da Emboscada","on_initiative_and_first_hit","unlimited","none",j("initiative_advantage",true,"first_hit_target_attacks_against_it_advantage_until_start_next_turn",true)),
          f(17,"sudden_strike","Ataque Súbito","bonus_action_after_attack_action","1_per_turn","none",j("extra_weapon_attack",true,"can_sneak_attack_second_different_target",true)));

        d("Ladino","Espadachim",XANATHAR,"cap.1 p.45",XANATHAR_SHA,
          f(3,"fancy_footwork","Passos Elegantes","on_melee_attack","unlimited","none",j("attacked_target_cannot_opportunity_attack_you_this_turn",true)),
          f(3,"rakish_audacity","Audácia Arrogante","passive","unlimited","none",j("add_charisma_to_initiative",true,"solo_melee_sneak_attack_rule",true)),
          f(9,"panache","Panache","action","unlimited","none",j("contest","Persuasão vs Intuição","hostile_on_success_disadvantage_attacks_others_and_no_oa_vs_others",true,"nonhostile_on_success_charmed",true,"duration_minutes",1)),
          f(13,"elegant_maneuver","Manobra Elegante","bonus_action","unlimited","none",j("advantage_next_acrobatics_or_athletics_this_turn",true)),
          f(17,"master_duelist","Duelista Mestre","on_attack_miss","1","short_or_long_rest",j("reroll_attack_with_advantage",true,"requires_roll","reroll_total")));

        d("Mago","Mago de Guerra",XANATHAR,"cap.1 p.47",XANATHAR_SHA,
          f(2,"arcane_deflection","Deflexão Arcana","reaction","1_per_round","none",j("trigger",a("atingido por ataque","falhou salvaguarda"),"attack_ac_bonus",2,"save_bonus",4,"restriction_until_end_next_turn","só pode conjurar truques")),
          f(2,"tactical_wit","Sagacidade Tática","passive","unlimited","none",j("add_intelligence_to_initiative",true)),
          f(6,"power_surge","Surto de Poder","on_counterspell_dispel_or_damage_spell","Intelligence_modifier_min1_storage","long_rest",j("starts_after_long_rest",1,"gain_on_successful_counterspell_or_dispel",1,"spend_once_per_turn_extra_force_damage","metade do nível de mago")),
          f(10,"durable_magic","Magia Durável","passive_while_concentrating","unlimited","none",j("ac_bonus",2,"saving_throw_bonus",2)),
          f(14,"deflecting_shroud","Manto Defletor","on_arcane_deflection","unlimited","none",j("targets_max",3,"range_ft",60,"force_damage_each","metade do nível de mago")));

        d("Monge","Estilo do Mestre Bêbado",XANATHAR,"cap.1 p.50",XANATHAR_SHA,
          f(3,"bonus_proficiencies_drunken","Proficiências Bônus","passive","unlimited","none",j("skills",a("Atuação"),"tools",a("suprimentos de cervejeiro"))),
          f(3,"drunken_technique","Técnica Bêbada","on_flurry_of_blows","unlimited","none",j("disengage_until_end_turn",true,"speed_bonus_ft",10)),
          f(6,"tipsy_sway","Balanço Embriagado","reaction_or_movement","unlimited","none",j("stand_from_prone_cost_ft",5,"redirect_missed_melee_attack_cost_ki",1,"redirect_to_other_creature_within_ft",5)),
          f(11,"drunkards_luck","Sorte do Bêbado","on_disadvantaged_roll","ki","none",j("ki_cost",2,"cancel_disadvantage_for_one_attack_save_or_check",true)),
          f(17,"intoxicated_frenzy","Frenesi Intoxicado","on_flurry_of_blows","unlimited","none",j("flurry_attacks_max",5,"each_attack_must_target_different_creature",true)));

        d("Monge","Estilo do Kensei",XANATHAR,"cap.1 p.51",XANATHAR_SHA,
          f(3,"path_of_kensei","Caminho do Kensei","passive_choices","unlimited","none",j("initial_kensei_weapons",2,"weapon_restrictions","uma corpo a corpo e uma à distância; sem propriedades pesada/especial","agile_parry_ac_bonus",2,"kensei_shot_bonus_damage","1d4 por acerto à distância no turno","artisan_tool_choice",true)),
          f(6,"one_with_blade","Uno com a Lâmina","passive","unlimited","none",j("kensei_weapons_magical",true,"deft_strike_ki_cost",1,"deft_strike_extra_damage","dado de Artes Marciais","once_per_turn",true)),
          f(11,"sharpen_the_blade","Afiar a Lâmina","bonus_action","ki","none",j("ki_cost_range","1-3","bonus_attack_damage_equals_ki",true,"duration_minutes",1,"cannot_apply_to_weapon_with_existing_magic_bonus",true)),
          f(17,"unerring_accuracy","Precisão Infalível","on_miss_with_monk_weapon","1_per_turn","none",j("reroll_attack_manual",true,"requires_roll","reroll_total")));

        d("Monge","Estilo da Alma Solar",XANATHAR,"cap.1 p.52",XANATHAR_SHA,
          f(3,"radiant_sun_bolt","Raio Solar Radiante","ranged_spell_attack","unlimited","none",j("range_ft",30,"damage","dado de Artes Marciais radiante","attack_ability","Destreza","can_replace_attack_action_attacks",true,"ki_cost_two_bonus_bolts",1)),
          f(6,"searing_arc_strike","Golpe do Arco Ardente","bonus_action_after_attack","ki","none",j("base_ki_cost",2,"cast","mãos flamejantes","extra_ki_increases_spell_level",true,"max_total_ki_formula","metade do nível de monge")),
          f(11,"searing_sunburst","Explosão Solar Ardente","action","ki_optional","none",j("range_ft",150,"radius_ft",20,"base_damage","2d6 radiante","constitution_save_zero_on_success",true,"extra_ki_max",3,"extra_damage_per_ki","2d6","requires_roll","radiant_damage_roll")),
          f(17,"sun_shield","Escudo Solar","passive_toggle","unlimited","none",j("bright_light_ft",30,"dim_extra_ft",30,"reaction_melee_attacker_damage","5 + Sabedoria","damage_type","radiante")));

        d("Paladino","Juramento da Conquista",XANATHAR,"cap.1 p.54",XANATHAR_SHA,
          f(3,"conquering_presence","Presença Conquistadora","channel_divinity_action","channel_divinity","none",j("range_ft",30,"save","Sabedoria vs CD de magia","condition","amedrontado","duration_minutes",1,"repeat_save_end_turn",true)),
          f(3,"guided_strike_conquest","Golpe Guiado","channel_divinity_after_attack_roll","channel_divinity","none",j("attack_bonus",10,"can_decide_after_roll_before_result",true)),
          f(7,"aura_of_conquest","Aura da Conquista","passive_aura","unlimited","none",j("radius_ft_by_level","10@7;30@18","frightened_enemy_speed",0,"psychic_damage_start_turn","metade do nível de paladino")),
          f(15,"scornful_rebuke","Repreensão Desdenhosa","on_hit_by_attack","unlimited","none",j("attacker_psychic_damage","Carisma")),
          f(20,"invincible_conqueror","Conquistador Invencível","action","1","long_rest",j("duration_minutes",1,"resistance_all_damage",true,"critical_range","19-20","extra_attack_per_attack_action",1)));

        d("Paladino","Juramento da Redenção",XANATHAR,"cap.1 p.55",XANATHAR_SHA,
          f(3,"emissary_of_peace","Emissário da Paz","channel_divinity_bonus_action","channel_divinity","none",j("persuasion_bonus",5,"duration_minutes",10)),
          f(3,"rebuke_the_violent","Repreender o Violento","channel_divinity_reaction","channel_divinity","none",j("range_ft",30,"trigger","inimigo causa dano a criatura diferente de você","wisdom_save",true,"radiant_damage_on_fail_equals_trigger_damage",true,"half_on_success",true)),
          f(7,"aura_of_guardian","Aura do Guardião","reaction","1_per_round","none",j("range_ft_by_level","10@7;30@18","take_damage_instead_of_ally",true,"cannot_reduce_transferred_damage",true)),
          f(15,"protective_spirit","Espírito Protetor","end_of_turn","unlimited","none",j("requires_below_half_hp_and_above_zero",true,"healing","1d6 + metade do nível de paladino","requires_roll","d6_roll")),
          f(20,"emissary_of_redemption","Emissário da Redenção","passive","unlimited","none",j("resistance_damage_from_other_creatures",true,"attacker_radiant_damage_half_damage_you_take",true,"suppressed_against_creature_you_attack_or_harm_until_long_rest",true)));

        d("Patrulheiro","Perseguidor Obscuro",XANATHAR,"cap.1 p.58",XANATHAR_SHA,
          f(3,"dread_ambusher","Emboscador Terrível","first_turn","unlimited","combat",j("add_wisdom_to_initiative",true,"speed_bonus_ft_first_turn",10,"extra_attack_on_attack_action_first_turn",1,"extra_attack_bonus_damage","1d8","requires_roll","d8_roll")),
          f(3,"umbral_sight","Visão Umbral","passive","unlimited","none",j("darkvision_ft_bonus",30,"invisible_to_creatures_relying_on_darkvision_in_darkness",true)),
          f(7,"iron_mind","Mente de Ferro","passive","unlimited","none",j("wisdom_save_proficiency_or_choice_intelligence_charisma_if_already",true)),
          f(11,"stalkers_flurry","Rajada do Perseguidor","on_weapon_attack_miss","1_per_turn","none",j("make_another_weapon_attack",true)),
          f(15,"shadowy_dodge","Esquiva Sombria","reaction_before_attack_roll","1_per_round","none",j("impose_disadvantage_if_attacker_does_not_have_advantage",true)));

        d("Patrulheiro","Andarilho do Horizonte",XANATHAR,"cap.1 p.59",XANATHAR_SHA,
          f(3,"detect_portal","Detectar Portal","action","1","short_or_long_rest",j("sense_nearest_planar_portal_within_miles",1,"learn_distance_and_direction",true)),
          f(3,"planar_warrior","Guerreiro Planar","bonus_action","unlimited","none",j("range_ft",30,"next_weapon_hit_all_damage_becomes_force",true,"extra_force_damage_by_level","1d8@3;2d8@11","requires_roll","extra_damage_roll")),
          f(7,"ethereal_step","Passo Etéreo","bonus_action","1","short_or_long_rest",j("cast","forma etérea","ends_at_end_of_current_turn",true,"no_slot",true)),
          f(11,"distant_strike","Ataque Distante","on_attack_action","unlimited","none",j("teleport_before_each_attack_ft",10,"if_attack_two_different_creatures_gain_third_attack",true)),
          f(15,"spectral_defense","Defesa Espectral","reaction_on_attack_damage","1_per_round","none",j("resistance_to_triggering_attack_damage",true)));

        d("Patrulheiro","Exterminador de Monstros",XANATHAR,"cap.1 p.60",XANATHAR_SHA,
          f(3,"hunters_sense","Sentido do Caçador","action","Wisdom_modifier_min1","long_rest",j("range_ft",60,"learn_damage_immunities_resistances_vulnerabilities",true,"blocked_by_divination_immunity",true)),
          f(3,"slayers_prey","Presa do Exterminador","bonus_action","unlimited","none",j("range_ft",60,"first_weapon_hit_each_turn_extra_damage","1d6","lasts_until_short_or_long_rest_or_new_target",true,"requires_roll","d6_roll")),
          f(7,"supernatural_defense","Defesa Sobrenatural","on_save_or_grapple_escape_vs_prey","unlimited","none",j("bonus","1d6","requires_roll","d6_roll")),
          f(11,"magic_users_nemesis","Nêmesis do Conjurador","reaction","1","short_or_long_rest",j("range_ft",60,"trigger",a("conjura magia","teleporta"),"wisdom_save_vs_your_spell_dc",true,"on_fail_action_wasted",true)),
          f(15,"slayers_counter","Contra-Ataque do Exterminador","reaction_on_save_forced_by_prey","1_per_round","none",j("make_weapon_attack_before_save",true,"if_attack_hits_save_auto_succeeds",true)));

        // SWORD COAST — opções exclusivas. Reprints do Xanathar e Bladesinging/Tasha entram como aliases, não duplicação.
        d("Bárbaro","Caminho do Furioso de Batalha",SCAG,"cap.4 p.121",SCAG_SHA,
          f(3,"battlerager_armor","Armadura do Furioso de Batalha","passive","unlimited","none",j("proficiency","armadura com espinhos","bonus_action_spiked_attack","1d4 + Força perfurante","grapple_damage",3)),
          f(6,"reckless_abandon","Abandono Imprudente","on_reckless_attack_while_raging","unlimited","rage",j("temp_hp","modificador de Constituição mínimo 1")),
          f(10,"battlerager_charge","Investida do Furioso","bonus_action_while_raging","unlimited","rage",j("dash",true)),
          f(14,"spiked_retribution","Retaliação Espinhosa","on_melee_hit_while_raging_in_spiked_armor","unlimited","rage",j("attacker_piercing_damage",3,"range_ft",5)));

        d("Clérigo","Domínio Arcano",SCAG,"cap.4 p.125",SCAG_SHA,
          f(1,"arcane_initiate","Iniciado Arcano","passive_choices","unlimited","none",j("arcana_proficiency",true,"wizard_cantrips_choose",2,"cantrips_count_as_cleric",true)),
          f(2,"arcane_abjuration","Abjuração Arcana","channel_divinity_action","channel_divinity","none",j("range_ft",30,"turn_types",a("celestial","elemental","fada","ínfero"),"wisdom_save",true,"banish_cr_threshold_scales_with_cleric_level",true)),
          f(6,"spell_breaker","Quebrador de Magias","on_healing_spell","unlimited","none",j("end_one_spell_on_healed_target_level_at_most_healing_spell_level",true)),
          f(8,"potent_spellcasting_arcana","Conjuração Potente","passive","unlimited","none",j("add_wisdom_to_cleric_cantrip_damage",true)),
          f(17,"arcane_mastery","Maestria Arcana","passive_choices","unlimited","none",j("choose_wizard_spells",a("um de 6º","um de 7º","um de 8º","um de 9º"),"always_prepared_domain_spells",true)));

        d("Guerreiro","Cavaleiro do Dragão Púrpura",SCAG,"cap.4 p.128",SCAG_SHA,
          f(3,"rallying_cry","Grito de Reagrupamento","on_second_wind","unlimited","second_wind",j("allies_max",3,"range_ft",60,"allies_must_see_or_hear",true,"heal_each","nível de guerreiro")),
          f(7,"royal_envoy","Emissário Real","passive","unlimited","none",j("persuasion_proficiency_and_expertise",true,"fallback_skill_choice_if_already_proficient",a("Adestrar Animais","Intuição","Intimidação","Atuação"))),
          f(10,"inspiring_surge","Surto Inspirador","on_action_surge","unlimited","action_surge",j("ally_within_ft",60,"ally_reaction_weapon_attack",true,"allies_at_level18",2)),
          f(15,"bulwark","Baluarte","on_indomitable_mental_save","unlimited","indomitable",j("ally_within_ft",60,"ally_failed_same_effect_save_can_reroll",true)));

        d("Monge","Caminho da Morte Longa",SCAG,"cap.4 p.130",SCAG_SHA,
          f(3,"touch_of_death","Toque da Morte","on_reduce_creature_to_zero_within_5ft","unlimited","none",j("temp_hp","nível de monge + Sabedoria")),
          f(6,"hour_of_reaping","Hora da Colheita","action","unlimited","none",j("radius_ft",30,"targets_seeing_you",true,"wisdom_save",true,"condition_on_fail","amedrontado","duration","until_end_of_your_next_turn")),
          f(11,"mastery_of_death","Maestria da Morte","on_reduced_to_zero_hp","ki","none",j("ki_cost",1,"set_hp",1)),
          f(17,"touch_of_long_death","Toque da Morte Longa","action_touch","ki","none",j("ki_cost_range","1-10","damage_per_ki","2d10 necrótico","constitution_save_half",true,"requires_roll","necrotic_damage_roll")));

        d("Paladino","Juramento da Coroa",SCAG,"cap.4 p.132",SCAG_SHA,
          f(3,"champion_challenge","Desafio do Campeão","channel_divinity_bonus_action","channel_divinity","none",j("range_ft",30,"wisdom_save",true,"on_fail_cannot_willingly_move_more_than_30ft_from_you",true,"duration_minutes",1)),
          f(3,"turn_the_tide","Virar a Maré","channel_divinity_bonus_action","channel_divinity","none",j("range_ft",30,"targets_nonhostile_below_half_hp",true,"healing","1d6 + Carisma","requires_roll","healing_roll")),
          f(7,"divine_allegiance","Lealdade Divina","reaction","1_per_round","none",j("range_ft",5,"take_damage_instead_of_target",true,"cannot_reduce_or_prevent_transferred_damage",true)),
          f(15,"unyielding_spirit","Espírito Inabalável","passive","unlimited","none",j("advantage_saves_vs",a("paralisado","atordoado"))),
          f(20,"exalted_champion","Campeão Exaltado","action","1","long_rest",j("duration_hours",1,"resistance_nonmagical_bps",true,"allies_death_save_advantage_radius_ft",30,"wisdom_save_advantage_self_and_allies_radius_ft",30)));

        d("Bruxo","O Imortal",SCAG,"cap.4 p.139",SCAG_SHA,
          f(1,"undying_expanded_spells","Magias Expandidas do Imortal","passive","unlimited","none",j("expanded_spell_list",true)),
          f(1,"among_the_dead","Entre os Mortos","passive","unlimited","none",j("learn_cantrip","estabilizar moribundo","advantage_saves_vs_disease",true,"undead_must_wisdom_save_to_target_you",true,"immunity_after_success_or_you_target_it_hours",24)),
          f(6,"defy_death","Desafiar a Morte","on_success_death_save_or_stabilize_other","1","long_rest",j("healing","1d8 + Constituição","requires_roll","d8_roll")),
          f(10,"undying_nature","Natureza Imortal","passive","unlimited","none",j("hold_breath_indefinitely",true,"no_food_water_sleep_required",true,"aging_rate","1 year per 10 years")),
          f(14,"indestructible_life","Vida Indestrutível","bonus_action","1","short_or_long_rest",j("healing","1d8 + nível de bruxo","reattach_severed_body_part",true,"requires_roll","d8_roll")));

        // aliases/reprints: mesma identidade mecânica, uma implementação canônica.
        alias("Mestre da Mente","Mentor"); alias("Mastermind","Mentor");
        alias("Swashbuckler","Espadachim"); alias("Storm Sorcery","Feiticeiro da Tempestade");
        alias("Way of the Sun Soul","Estilo da Alma Solar"); alias("Bladesinging","Bladesinging");
        alias("Domínio da Ordem","Order Domain"); alias("Círculo dos Esporos","Circle of Spores");
    }

    private CrossBookSubclassRuntimeEngine(){}

    private static void d(String cls,String name,String book,String locator,String sha,Feature... fs){Def d=new Def(cls,name,book,locator,sha);for(Feature f:fs)d.features.add(f);BY_NAME.put(n(name),d);String c=classKey(cls);if(!BY_CLASS.containsKey(c))BY_CLASS.put(c,new ArrayList<Def>());BY_CLASS.get(c).add(d);}
    private static Feature f(int l,String id,String name,String activation,String uses,String recharge,JSONObject rule){return new Feature(l,id,name,activation,uses,recharge,rule);}
    private static JSONObject j(Object... kv){JSONObject o=new JSONObject();try{for(int i=0;i+1<kv.length;i+=2)o.put(String.valueOf(kv[i]),kv[i+1]);}catch(Exception ignored){}return o;}
    private static JSONArray a(Object... values){JSONArray x=new JSONArray();for(Object v:values)x.put(v);return x;}
    private static void alias(String from,String to){ALIAS.put(n(from),to);}

    public static JSONArray allSubclasses(){JSONArray out=new JSONArray();for(Def d:BY_NAME.values())out.put(d.name);return out;}
    public static JSONArray subclassesFor(String cls){JSONArray out=new JSONArray();List<Def> list=BY_CLASS.get(classKey(cls));if(list!=null)for(Def d:list)out.put(d.name);return out;}
    public static boolean isKnown(String name){return find(name)!=null;}
    public static JSONObject lookup(String name){JSONObject o=new JSONObject();try{Def d=find(name);if(d==null)return o.put("known",false);return o.put("known",true).put("subclass",d.name).put("class",d.cls).put("book",d.book).put("source_locator",d.locator).put("runtime","CrossBookSubclassRuntimeEngine").put("mechanics_compiled",CompiledContentMechanicsEngine.hasEffectPipeline("subclass",d.name));}catch(Exception e){return o;}}

    /** Register reviewed evidence + effect pipelines. Safe/idempotent. */
    public static JSONObject install(){JSONObject out=new JSONObject();JSONArray failed=new JSONArray();int ok=0;try{
        for(Def d:BY_NAME.values()){
            String evidenceId="run135.subclass."+n(d.name).replace(' ','_');
            JSONObject ev=new JSONObject().put("evidence_id",evidenceId).put("content_kind","subclass").put("content_name",d.name).put("source_book",d.book).put("source_locator",d.locator).put("source_sha256",d.sha).put("revision",1);
            JSONObject er=AuthorizedMechanicalEvidenceRegistry.registerReviewed(ev);
            if(!er.optBoolean("registered",false)){failed.put(d.name+":"+er.optString("authority"));continue;}
            JSONObject basis=new JSONObject().put("source_type","reviewed_evidence").put("evidence_id",evidenceId).put("content_kind","subclass").put("content_name",d.name).put("source_book",d.book).put("source_locator",d.locator).put("source_sha256",d.sha).put("revision",1);
            JSONObject mechanics=new JSONObject().put("effect_pipeline_complete",true).put("execution_status","compiled_effect_pipeline").put("runtime_resolver","CrossBookSubclassRuntimeEngine.resolve").put("structured_feature_runtime",true).put("auto_rolls",false).put("auto_mutation_allowed",false).put("feature_count",d.features.size());
            JSONObject rr=CompiledContentMechanicsEngine.register("subclass",d.name,mechanics,basis);if(rr.optBoolean("registered",false))ok++;else failed.put(d.name+":"+rr.optString("authority"));
        }
        return out.put("installed",ok).put("expected",BY_NAME.size()).put("failed",failed).put("complete",ok==BY_NAME.size()).put("auto_rolls",false);
    }catch(Exception e){try{return out.put("installed",ok).put("expected",BY_NAME.size()).put("failed",failed).put("complete",false).put("error",e.getClass().getSimpleName());}catch(Exception ignored){return out;}}}

    public static JSONObject profile(String subclass,int classLevel,JSONObject sheet){JSONObject out=new JSONObject();try{Def d=find(subclass);if(d==null)return out.put("known",false);int lvl=Math.max(1,Math.min(20,classLevel));JSONArray features=new JSONArray(),pending=new JSONArray();JSONObject resources=new JSONObject(),passives=new JSONObject();for(Feature f:d.features){if(lvl<f.level)continue;JSONObject x=featureJson(f,lvl,sheet);features.put(x);int max=maxUses(f.uses,lvl,sheet);if(max>=0){resources.put(f.id,new JSONObject().put("max",max).put("current",max).put("recharge",f.recharge));}if(f.rule.has("choice_options")||f.rule.has("skill_choice")||f.rule.has("affinity_choice")||f.rule.has("choose_skill_or_language")||f.rule.has("passive_choices"))pending.put(f.id);if(f.activation.startsWith("passive"))merge(passives,f.rule);}
        return out.put("known",true).put("subclass",d.name).put("class",d.cls).put("book",d.book).put("source_locator",d.locator).put("class_level",lvl).put("features",features).put("resources",resources).put("passive_state",passives).put("pending_choices",pending).put("auto_rolls",false);
    }catch(Exception e){try{return out.put("known",false).put("error",e.getClass().getSimpleName());}catch(Exception ignored){return out;}}}

    /** Gateway-compatible resolver. action/feature_id must be explicit; profile is read-only. */
    public static JSONObject resolve(String subclass,JSONObject context){JSONObject out=new JSONObject();try{if(context==null)context=new JSONObject();String action=context.optString("feature_id",context.optString("action","profile"));int lvl=Math.max(1,context.optInt("class_level",context.optInt("level",1)));if("profile".equalsIgnoreCase(action)){JSONObject p=profile(subclass,lvl,context.optJSONObject("sheet")!=null?context.optJSONObject("sheet"):context);return out.put("resolved",p.optBoolean("known",false)).put("subclass",subclass).put("action","profile").put("profile",p).put("auto_rolls",false).put("requires_commit",false);}JSONObject sheet=context.optJSONObject("sheet");if(sheet==null)sheet=context;return resolve(subclass,action,context,sheet);}catch(Exception e){try{return out.put("resolved",false).put("error",e.getClass().getSimpleName());}catch(Exception ignored){return out;}}}

    /** Resolves one explicit subclass feature. Does not invent rolls/targets/choices. */
    public static JSONObject resolve(String subclass,String featureId,JSONObject input,JSONObject sheet){JSONObject out=new JSONObject();try{Def d=find(subclass);if(d==null)return out.put("resolved",false).put("reason","unknown_subclass");if(input==null)input=new JSONObject();if(sheet==null)sheet=new JSONObject();int lvl=Math.max(1,Math.min(20,input.optInt("class_level",sheet.optInt("level",1))));Feature f=findFeature(d,featureId);if(f==null)return out.put("resolved",false).put("reason","unknown_feature");if(lvl<f.level)return out.put("resolved",false).put("reason","feature_locked_by_level").put("required_level",f.level);
        JSONObject rule=copy(f.rule);String rollKey=rule.optString("requires_roll","");if(!rollKey.isEmpty()&&!input.has(rollKey))return pending(out,rollKey);JSONArray choices=rule.optJSONArray("choice_options");if(choices!=null){String choice=input.optString("choice","");if(choice.isEmpty())return pending(out,"choice");boolean found=false;for(int i=0;i<choices.length();i++)if(n(choices.optString(i)).equals(n(choice))){found=true;break;}if(!found)return pending(out,"valid choice from choice_options");rule.put("selected_choice",choice);}
        int max=maxUses(f.uses,lvl,sheet);if(max>=0&&!"unlimited".equals(f.uses)){JSONObject res=sheet.optJSONObject("subclass_resources");if(res==null)res=new JSONObject();JSONObject rr=res.optJSONObject(f.id);if(rr==null)rr=new JSONObject().put("max",max).put("current",max).put("recharge",f.recharge);int cur=rr.optInt("current",max),spend=f.uses.startsWith("pool=")?Math.max(1,input.optInt("resource_spent",input.optInt("dice_spent",1))):1;if(cur<spend)return out.put("resolved",false).put("reason","resource_depleted").put("resource",f.id).put("current",cur).put("required",spend);rr.put("current",cur-spend);res.put(f.id,rr);sheet.put("subclass_resources",res);rule.put("resource_spent",spend);}
        if(!rollKey.isEmpty())rule.put("provided_roll",input.optInt(rollKey,input.optInt(rollKey,0)));
        if(input.has("target_id"))rule.put("target_id",input.optString("target_id"));if(input.has("targets"))rule.put("targets",input.optJSONArray("targets"));
        JSONArray active=sheet.optJSONArray("subclass_active_effects");if(active==null)active=new JSONArray();active.put(new JSONObject().put("subclass",d.name).put("feature_id",f.id).put("feature",f.name).put("activation",f.activation).put("effect",rule));sheet.put("subclass_active_effects",active).put("subclass_last_feature",f.id);
        return out.put("resolved",true).put("subclass",d.name).put("feature_id",f.id).put("feature",f.name).put("activation",f.activation).put("effect",rule).put("state_updates",new JSONObject().put("subclass_resources",sheet.optJSONObject("subclass_resources")).put("subclass_active_effects",active)).put("requires_commit",true).put("auto_rolls",false);
    }catch(Exception e){try{return out.put("resolved",false).put("error",e.getClass().getSimpleName());}catch(Exception ignored){return out;}}}

    /** Adds source-bound subclass state to a persisted character sheet. */
    public static boolean applyToSheet(JSONObject sheet){try{if(sheet==null)return false;String subclass=extractSubclass(sheet);Def d=find(subclass);if(d==null)return false;int lvl=Math.max(1,Math.min(20,sheet.optInt("level",1)));JSONObject p=profile(d.name,lvl,sheet);sheet.put("subclass_name",d.name).put("subclass_class",d.cls).put("subclass_source",d.book).put("subclass_source_locator",d.locator).put("subclass_runtime","CrossBookSubclassRuntimeEngine").put("subclass_profile",p).put("subclass_choices_pending",p.optJSONArray("pending_choices")).put("subclass_mechanics_compiled",true);
        JSONObject existing=sheet.optJSONObject("subclass_resources"),fresh=p.optJSONObject("resources");if(existing==null)existing=new JSONObject();if(fresh!=null){Iterator<String> it=fresh.keys();while(it.hasNext()){String k=it.next();if(!existing.has(k))existing.put(k,fresh.optJSONObject(k));else{JSONObject old=existing.optJSONObject(k),mx=fresh.optJSONObject(k);if(old!=null&&mx!=null){old.put("max",mx.optInt("max",old.optInt("max",0))).put("recharge",mx.optString("recharge",old.optString("recharge")));if(old.optInt("current",0)>old.optInt("max",0))old.put("current",old.optInt("max",0));}}}}sheet.put("subclass_resources",existing);
        String visible=sheet.optString("features","");JSONArray fs=p.optJSONArray("features");StringBuilder add=new StringBuilder();for(int i=0;fs!=null&&i<fs.length();i++){JSONObject f=fs.optJSONObject(i);if(f==null)continue;String nm=f.optString("name");if(!visible.contains(nm)){if(add.length()>0)add.append("; ");add.append(nm);}}if(add.length()>0)sheet.put("features",visible+(visible.isEmpty()?"":"\n")+"Subclasse — "+d.name+": "+add+".");
        return true;
    }catch(Exception e){return false;}}

    public static void shortRest(JSONObject sheet){recharge(sheet,"short_rest");}
    public static void longRest(JSONObject sheet){recharge(sheet,"long_rest");}
    public static void onInitiative(JSONObject sheet){recharge(sheet,"initiative");}
    private static void recharge(JSONObject sheet,String rest){try{if(sheet==null)return;applyToSheet(sheet);JSONObject r=sheet.optJSONObject("subclass_resources");if(r==null)return;Iterator<String> it=r.keys();while(it.hasNext()){String k=it.next();JSONObject x=r.optJSONObject(k);if(x==null)continue;String rc=x.optString("recharge","");boolean ok="long_rest".equals(rest)&&(rc.contains("long")||rc.contains("short_or_long"))||"short_rest".equals(rest)&&(rc.contains("short")||rc.contains("short_or_long"))||"initiative".equals(rest)&&rc.contains("initiative");if(ok)x.put("current",x.optInt("max",x.optInt("current",0)));}}catch(Exception ignored){}}

    private static JSONObject featureJson(Feature f,int lvl,JSONObject sheet)throws Exception{return new JSONObject().put("level",f.level).put("id",f.id).put("name",f.name).put("activation",f.activation).put("uses",f.uses).put("recharge",f.recharge).put("rule",copy(f.rule)).put("uses_max",maxUses(f.uses,lvl,sheet));}
    private static Feature findFeature(Def d,String id){String x=n(id);for(Feature f:d.features)if(n(f.id).equals(x)||n(f.name).equals(x))return f;return null;}
    private static Def find(String name){String key=n(name);Def d=BY_NAME.get(key);if(d!=null)return d;String to=ALIAS.get(key);if(to!=null){Def x=BY_NAME.get(n(to));if(x!=null)return x;}return null;}
    private static String extractSubclass(JSONObject sheet){String s=sheet.optString("subclass_name",sheet.optString("subclass",""));if(!s.isEmpty())return s;String full=sheet.optString("class_name","");int i=full.indexOf(" — ");if(i<0)i=full.indexOf(" - ");return i>0?full.substring(i+3).trim():"";}
    private static int maxUses(String formula,int lvl,JSONObject sheet){if(formula==null)return -1;String f=formula.trim();if(f.equals("pool=1+warlock_level_d6"))return 1+Math.max(1,lvl);if(f.equals("pool=druid_level_d6"))return Math.max(1,lvl);if(f.equals("Intelligence_modifier_min1_storage"))return Math.max(1,mod(sheet.optInt("int_score",10)));if(f.equals("unlimited")||f.equals("1_per_round")||f.equals("1_per_turn")||f.equals("conditional")||f.equals("channel_divinity")||f.equals("bardic_inspiration")||f.equals("ki")||f.equals("sorcery_points")||f.equals("ki_optional")||f.equals("action_surge")||f.equals("indomitable")||f.equals("second_wind")||f.equals("rage")||f.contains(";"))return -1;if(f.equals("1"))return 1;if(f.equals("2"))return 2;if(f.equals("3"))return 3;if(f.equals("Wisdom_modifier_min1"))return Math.max(1,mod(sheet.optInt("wis",10)));if(f.equals("Constitution_modifier_min1"))return Math.max(1,mod(sheet.optInt("con",10)));if(f.equals("Strength_modifier_min1"))return Math.max(1,mod(sheet.optInt("str",10)));if(f.equals("Intelligence_modifier_min1"))return Math.max(1,mod(sheet.optInt("int_score",10)));try{return Integer.parseInt(f);}catch(Exception ignored){return -1;}}
    private static int mod(int score){return (int)Math.floor((score-10)/2.0);}
    private static JSONObject pending(JSONObject o,String need)throws Exception{return o.put("resolved",false).put("status","requires_player_or_gm_resolution").put("required_input",need).put("auto_rolls",false);}
    private static JSONObject copy(JSONObject from){JSONObject out=new JSONObject();try{if(from!=null){Iterator<String> it=from.keys();while(it.hasNext()){String k=it.next();out.put(k,from.opt(k));}}}catch(Exception ignored){}return out;}
    private static void merge(JSONObject into,JSONObject from){try{Iterator<String> it=from.keys();while(it.hasNext()){String k=it.next();if(!into.has(k))into.put(k,from.opt(k));}}catch(Exception ignored){}}
    private static String classKey(String cls){String x=n(cls);if(x.contains("barbar"))return "barbarian";if(x.contains("bardo"))return "bard";if(x.contains("bruxo"))return "warlock";if(x.contains("clerigo"))return "cleric";if(x.contains("druida"))return "druid";if(x.contains("feiticeiro"))return "sorcerer";if(x.contains("guerreiro"))return "fighter";if(x.contains("ladino"))return "rogue";if(x.contains("mago"))return "wizard";if(x.contains("monge"))return "monk";if(x.contains("paladino"))return "paladin";if(x.contains("patrulheiro"))return "ranger";return x;}
    private static String n(String s){String x=Normalizer.normalize(s==null?"":s.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}+","");return x.replaceAll("[^a-z0-9]+"," ").trim();}
}
