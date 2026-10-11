package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Mecânicas pré-compiladas do suplemento Cozinheiro.pdf. */
public final class CookingRulesEngine {
    public static final String BOOK="Cozinheiro.pdf";
    private CookingRulesEngine(){}

    public static int snackSaveDc(int proficiencyBonus,int constitutionModifier){
        return 8+Math.max(0,proficiencyBonus)+constitutionModifier;
    }

    public static int snacksPreparedPerRest(int constitutionModifier){return Math.max(0,constitutionModifier);}
    public static int extraOrderSnacks(int level,int constitutionModifier,boolean longRest){
        if(level<7)return 0;
        if(level<11&&!longRest)return 0;
        return Math.max(0,constitutionModifier);
    }
    public static boolean spiceCanRetryCondition(int level,String condition){
        String n=normalize(condition);
        if(n.contains("enfeit")||n.contains("charm")||n.contains("amedront")||n.contains("fright"))return level>=1;
        if(n.contains("atordo")||n.contains("stun"))return level>=7;
        if(n.contains("paralis")||n.contains("paraly"))return level>=13;
        return false;
    }
    public static boolean comfortMealCanDouble(int level,boolean sharedLongRest){return level>=17&&sharedLongRest;}
    public static boolean leftoversTriggers(int level,int snacksRemaining){return level>=20&&snacksRemaining<=0;}

    public static JSONArray classProgression(){
        JSONArray a=new JSONArray();try{
            a.put(level(1,"Bolsa de Cozinheiro; Petiscos; Temperos Aromáticos",3));a.put(level(2,"Especialização",3));a.put(level(3,"Arquétipo de Cozinheiro",4));a.put(level(4,"Incremento no Valor de Habilidade",5));a.put(level(5,"Reflexão Rotunda",6));a.put(level(6,"Característica de Arquétipo",7));a.put(level(7,"Aceitando Pedidos; Temperos Aromáticos aprimorados",8));a.put(level(8,"Incremento no Valor de Habilidade",9));a.put(level(9,"Especialização; Estômago de Avestruz",10));a.put(level(10,"Característica de Arquétipo",11));a.put(level(11,"Aceitando Pedidos aprimorado",12));a.put(level(12,"Incremento no Valor de Habilidade",12));a.put(level(13,"Temperos Aromáticos aprimorados",13));a.put(level(14,"Característica de Arquétipo",13));a.put(level(15,"Alma da Comida",14));a.put(level(16,"Incremento no Valor de Habilidade",14));a.put(level(17,"Refeição Reconfortante",14));a.put(level(18,"Característica de Arquétipo",15));a.put(level(19,"Incremento no Valor de Habilidade",15));a.put(level(20,"Sobras",15));
        }catch(Exception ignored){AppLog.ignored("CookingRulesEngine","Falha ao montar progressão.",ignored);}return a;
    }

    public static JSONArray snackCatalog(){
        JSONArray a=new JSONArray();try{
            a.put(snack("Caçarola de Cenoura",1,"","darkvision_18m_or_plus_18m",8,"hour"));
            a.put(snack("Chá Verde",1,"","plus_1d4_con_saves_int_wis_checks",1,"hour"));
            a.put(snack("Costeleta de Porco Defumada",1,"","cold_resistance_and_poisoned_immunity",10,"minute"));
            a.put(snack("Cristais de Açúcar",1,"","speed_bonus_by_cook_level",10,"minute"));
            a.put(snack("Entranhas de Vaca",1,"","con_save_then_charisma_disadvantage_and_suspend_charm",10,"minute"));
            a.put(snack("Gelo com Sabor",1,"","int_save_cold_damage_and_concentration_disadvantage",1,"round"));
            a.put(snack("Leite de Amêndoas",1,"","con_save_sleep_after_1_minute",10,"minute"));
            a.put(snack("Pão Duro",1,"","str_save_bludgeoning_and_attack_disadvantage",1,"round"));
            a.put(snack("Pimenta Ardida",1,"","fire_resistance_and_grappled_immunity",10,"minute"));
            a.put(snack("Proteína Concentrada",1,"","strength_damage_and_checks_bonus_die_by_level",1,"minute"));
            a.put(snack("Sucos Revigorantes",1,"","temporary_hp_d8_scaling_by_level",1,"minute"));
            a.put(snack("Faisão Assado",7,"","controlled_slow_fall_no_fall_damage",10,"minute"));
            a.put(snack("Lebre na Brasa",7,"","jump_spell_effect",1,"hour"));
            a.put(snack("Mingau Encorpado",7,"","barkskin_spell_effect",10,"minute"));
            a.put(snack("Ovos de Aranha",7,"","spider_climb_spell_effect",1,"hour"));
            a.put(snack("Truta Amanteigada",7,"","water_breathing_and_swim_speed",1,"hour"));
            a.put(snack("Beterrabas Agridoce",11,"","first_weapon_hit_each_turn_plus_1d6_against_target",1,"minute"));
            a.put(snack("Brotos Espectrais",11,"","see_invisibility_and_ethereal_shift",1,"minute"));
            a.put(snack("Chá de Camomila",11,"","weapon_damage_minus_1d6",1,"minute"));
            a.put(snack("Folhas de Cânhamo",11,"","wis_save_indifferent_to_all_creatures",1,"minute"));
            a.put(snack("Sementes Estelares",11,"","recover_spell_slots_total_level_1d4_use_within_1_minute",1,"minute"));
            a.put(snack("Cauda de Dragão Cozida",15,"","ac_plus_1_draconic_presence_or_fire_tail",1,"minute"));
            a.put(snack("Língua de Hidra",15,"","temp_hp_regeneration_limited_extra_reaction_and_action",1,"minute"));
            a.put(snack("Torta Elemental",15,"","acid_lightning_fire_thunder_poison_resistance_fly_swim",1,"minute"));
            a.put(snack("Uvas Cristalizadas",1,"Sous Chef","dexterity_damage_and_checks_bonus_die_by_level",1,"minute"));
            a.put(snack("Cerveja Forte",7,"Sous Chef","nonmagical_bps_resistance",1,"minute"));
            a.put(snack("Peixe no Mel",13,"Sous Chef","advantage_int_wis_cha_checks",1,"minute"));
            a.put(snack("Purê de Pinhões",17,"Sous Chef","all_saves_advantage_aura_3m",1,"minute"));
            a.put(snack("Brotos Fedorentos",1,"Sargento Alimentar","frightened_and_charmed_immunity",1,"minute"));
            a.put(snack("Feijões de Inchaço",7,"Sargento Alimentar","size_plus_one_strength_checks_and_saves_advantage",1,"minute"));
            a.put(snack("Café Tremulante",13,"Sargento Alimentar","drop_to_1_hp_once_then_end",1,"minute"));
            a.put(snack("Aveia Gorda",17,"Sargento Alimentar","all_damage_resistance",1,"round"));
            a.put(snack("Papa Gordurosa",1,"Mestre-Cuca","con_save_acid_damage_and_restrained",1,"minute"));
            a.put(snack("Sais de Fogo",7,"Mestre-Cuca","aoe_fire_damage_dex_save_scaling",1,"instant"));
            a.put(snack("Atum no Alho",13,"Mestre-Cuca","con_save_hostiles_near_target_saves_disadvantage",1,"minute"));
            a.put(snack("Cebola Rançosa",17,"Mestre-Cuca","con_save_paralyzed_one_round_and_blinded",1,"minute"));
            a.put(snack("Gole do Subterrâneo",1,"Mestre Cervejeiro","magical_darkness_3m_allies_can_see",1,"minute"));
            a.put(snack("Raspadinha de Vodka",7,"Mestre Cervejeiro","snow_difficult_terrain_4_5m_and_push_hostiles",1,"minute"));
            a.put(snack("Whisky de Fogo Infernal",13,"Mestre Cervejeiro","dim_light_aura_hostiles_con_save_exhaustion",1,"minute"));
            a.put(snack("Beijo de Anjo",17,"Mestre Cervejeiro","bright_light_aura_hostiles_con_save_blinded",1,"minute"));
        }catch(Exception ignored){AppLog.ignored("CookingRulesEngine","Falha ao montar catálogo de petiscos.",ignored);}return a;
    }

    public static JSONObject snackByName(String name){String wanted=normalize(name);JSONArray all=snackCatalog();for(int i=0;i<all.length();i++){JSONObject x=all.optJSONObject(i);if(x!=null&&normalize(x.optString("name")).equals(wanted))return x;}return new JSONObject();}
    public static boolean canPrepareSnack(String name,int cookLevel,String archetype){JSONObject x=snackByName(name);if(x.length()==0||cookLevel<x.optInt("min_level",1))return false;String required=normalize(x.optString("archetype"));return required.length()==0||normalize(archetype).equals(required);}

    public static JSONObject archetypeFeatures(){JSONObject o=new JSONObject();try{
        o.put("Sous Chef",new JSONArray().put("3: arremessa petisco benéfico a 18 m; em toque pode alimentar aliado como ação bônus").put("6: petisco benéfico cura também modificador de Constituição").put("10: ação 1/descanso longo cura aliados visíveis em 9 m pelo nível e permite ataque corpo a corpo com reação").put("14: Banquete de Heróis 1/descanso longo com 500 po em comida fina").put("18: petisco lendário Cérebro de Macaco"));
        o.put("Sargento Alimentar",new JSONArray().put("3: comer próprio petisco como ação bônus; PV máximo +3 e +1 por nível da classe").put("6: cone de 3 m; Sabedoria contra CD de Petisco; falha força desvantagem em ataques contra outros").put("10: pode manter dois petiscos ativos em si").put("14: +2 em Força, Destreza ou Constituição e máximo 22").put("18: petisco lendário Coração Cristalino"));
        o.put("Mestre-Cuca",new JSONArray().put("3: proficiência em armas marciais e pode alimentar hostil a 18 m").put("6: Ataque Extra").put("10: hostis a 3 m subtraem 1d4 de testes de resistência").put("14: 1/descanso curto ou longo impõe desvantagem no save contra petisco").put("18: petisco lendário Pílula Gasosa"));
        o.put("Mestre Cervejeiro",new JSONArray().put("3: suprimentos de cervejeiro; petiscos adulterados podem acumular e intoxicar").put("6: intoxicados +3 m movimento e inimigos têm desvantagem em ataques de oportunidade").put("10: intoxicados resistem a concussão").put("14: 1/turno intoxicado adiciona mod. Constituição do cozinheiro ao dano de ataque").put("18: petisco lendário Criação do Mestre"));
        o.put("Amassador de Monstros",new JSONArray().put("3: Colheita Vital de criatura recém-morta apropriada").put("6: Bolsa Autônoma cria Corte Fresco ao ferir criatura elegível em corpo a corpo").put("10: Bolsa de Combate, arma mágica 1d12, alcance 9/36, usa petiscos como munição").put("14: Observância Vital revela vulnerabilidades, resistências e imunidades após 1 minuto").put("18: petisco lendário Carne Vital"));
    }catch(Exception ignored){AppLog.ignored("CookingRulesEngine","Falha ao montar arquétipos.",ignored);}return o;}

    private static JSONObject level(int level,String features,int snacks)throws Exception{return new JSONObject().put("level",level).put("features",features).put("snacks_known",snacks);}
    private static JSONObject snack(String name,int level,String archetype,String effect,int duration,String unit)throws Exception{return new JSONObject().put("name",name).put("min_level",level).put("archetype",archetype).put("effect_key",effect).put("duration",duration).put("duration_unit",unit);}

    public static JSONObject descriptor(){
        JSONObject out=new JSONObject();
        try{
            out.put("pack_id","cozinheiro").put("book",BOOK).put("status","compiled_phase_1").put("class_name","Cozinheiro");
            out.put("class",new JSONObject().put("hit_die","d8").put("saving_throws",new JSONArray().put("Constituição").put("Carisma"))
                    .put("skill_choices",3).put("subclass_level",3)
                    .put("archetypes",new JSONArray().put("Sous Chef").put("Sargento Alimentar").put("Mestre-Cuca").put("Mestre Cervejeiro").put("Amassador de Monstros")));
            out.put("cook_pouch",new JSONObject().put("restock_interval_days",30).put("market_restock_gp",10).put("forage_restock_hours",8).put("replacement_gp",100).put("replacement_hours",8));
            out.put("snacks",new JSONObject().put("prepared_per_short_or_long_rest","constitution_modifier").put("single_active_snack_per_creature",true)
                    .put("feed_action","action").put("range","touch").put("free_hand_required",true).put("save_dc","8 + proficiency_bonus + constitution_modifier"));
            out.put("aromatic_spices",new JSONObject().put("action","bonus_action").put("range","touch")
                    .put("conditions_level_1",new JSONArray().put("enfeitiçado").put("amedrontado"))
                    .put("conditions_level_7",new JSONArray().put("atordoado")).put("conditions_level_13",new JSONArray().put("paralisado")));
            out.put("accepting_orders",new JSONObject().put("level",7).put("extra_snacks","constitution_modifier").put("long_rest_only_until_level",10).put("short_or_long_rest_from_level",11).put("extra_snacks_cannot_have_prerequisites",true));
            out.put("comfort_meal",new JSONObject().put("level",17).put("requires_shared_long_rest",true).put("effect","double_snack_duration"));
            out.put("leftovers",new JSONObject().put("level",20).put("trigger","initiative_with_zero_snacks").put("effect","create_one_known_snack_in_pouch"));
            out.put("class_progression",classProgression()).put("archetype_features",archetypeFeatures()).put("snack_catalog",snackCatalog());
            out.put("multiclass",new JSONObject().put("minimum_constitution",13).put("gains",new JSONArray().put("armaduras leves").put("armaduras pesadas").put("armas simples").put("uma perícia da lista de cozinheiro").put("utensílios de cozinheiro")));
        }catch(Exception ignored){AppLog.ignored("CookingRulesEngine","Falha ao montar descritor compilado.",ignored);}return out;
    }

    private static String normalize(String value){return value==null?"":java.text.Normalizer.normalize(value.toLowerCase(java.util.Locale.ROOT),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+","");}
}
