package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.*;

/**
 * Deterministic rules engine for the 2024 Dungeon Master's Guide (DMG).
 * Covers Magic Items (catalog, attunement, pricing, values),
 * Environmental Hazards, Traps, Falling, Suffocation, and Treasure Tables.
 * Never rolls dice automatically; requires explicit inputs and DM confirmation.
 */
public final class Dmg2024RulesEngine {
    public static final String BOOK = "Dungeon Master's Guide (2024)";

    public static final class MagicItemDef {
        public final String id;
        public final String nameEn;
        public final String namePt;
        public final String rarity;
        public final String category;
        public final boolean requiresAttunement;
        public final String attunementDetails;
        public final int baseCostGp;
        public final String descriptionPt;

        public MagicItemDef(String id, String nameEn, String namePt, String rarity,
                            String category, boolean requiresAttunement,
                            String attunementDetails, int baseCostGp, String descriptionPt) {
            this.id = id;
            this.nameEn = nameEn;
            this.namePt = namePt;
            this.rarity = rarity;
            this.category = category;
            this.requiresAttunement = requiresAttunement;
            this.attunementDetails = attunementDetails;
            this.baseCostGp = baseCostGp;
            this.descriptionPt = descriptionPt;
        }

        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("name_en", nameEn);
                o.put("name_pt", namePt);
                o.put("rarity", rarity);
                o.put("category", category);
                o.put("requires_attunement", requiresAttunement);
                o.put("attunement_details", attunementDetails);
                o.put("base_cost_gp", baseCostGp);
                o.put("description_pt", descriptionPt);
            } catch (Exception ignored) {}
            return o;
        }
    }

    private static final Map<String, MagicItemDef> ITEMS_BY_ID = new LinkedHashMap<>();
    private static final Map<String, String> ITEM_ALIASES = new HashMap<>();

    private static void reg(MagicItemDef item) {
        ITEMS_BY_ID.put(item.id, item);
        ITEM_ALIASES.put(norm(item.id), item.id);
        ITEM_ALIASES.put(norm(item.nameEn), item.id);
        ITEM_ALIASES.put(norm(item.namePt), item.id);
    }

    static {
        reg(new MagicItemDef("potion_of_healing", "Potion of Healing", "Poção de Cura",
                "Common", "Potion", false, "", 50,
                "Restaura 2d4 + 2 pontos de vida. No D&D 2024, beber a poção consome uma Ação Bônus; administrá-la a outra criatura consome uma Ação."));
        reg(new MagicItemDef("potion_of_greater_healing", "Potion of Greater Healing", "Poção de Cura Maior",
                "Uncommon", "Potion", false, "", 250,
                "Restaura 4d4 + 4 pontos de vida. Consome Ação Bônus para si ou Ação para outro."));
        reg(new MagicItemDef("potion_of_superior_healing", "Potion of Superior Healing", "Poção de Cura Superior",
                "Rare", "Potion", false, "", 2500,
                "Restaura 8d4 + 8 pontos de vida."));
        reg(new MagicItemDef("potion_of_supreme_healing", "Potion of Supreme Healing", "Poção de Cura Suprema",
                "Very Rare", "Potion", false, "", 20000,
                "Restaura 10d4 + 20 pontos de vida."));

        reg(new MagicItemDef("bag_of_holding", "Bag of Holding", "Mochila de Carga",
                "Uncommon", "Wondrous Item", false, "", 500,
                "Capacidade interna de até 250 kg (500 libras) e volume de até 1,8 metro cúbico (64 pés cúbicos)."));
        reg(new MagicItemDef("cloak_of_protection", "Cloak of Protection", "Manto de Proteção",
                "Uncommon", "Wondrous Item", true, "Exige sintonização", 400,
                "Concede +1 na Classe de Armadura e +1 em todas as salvaguardas enquanto sintonizado."));
        reg(new MagicItemDef("ring_of_protection", "Ring of Protection", "Anel de Proteção",
                "Rare", "Ring", true, "Exige sintonização", 3500,
                "Concede +1 na Classe de Armadura e +1 em todas as salvaguardas enquanto usado e sintonizado."));
        reg(new MagicItemDef("boots_of_elvenkind", "Boots of Elvenkind", "Botas Élficas",
                "Uncommon", "Wondrous Item", false, "", 350,
                "Seus passos não produzem som e você tem Vantagem em testes de Furtividade para se mover silenciosamente."));
        reg(new MagicItemDef("wand_of_magic_missiles", "Wand of Magic Missiles", "Varinha de Mísseis Mágicos",
                "Uncommon", "Wand", false, "", 450,
                "Possui 7 cargas. Permite conjurar Mísseis Mágicos gastando 1 ou mais cargas. Recupera 1d6 + 1 cargas diariamente ao amanhecer."));
        reg(new MagicItemDef("pearl_of_power", "Pearl of Power", "Pérola do Poder",
                "Uncommon", "Wondrous Item", true, "Exige sintonização por um conjurador", 500,
                "Uma vez por dia, permite recuperar um espaço de magia gasto de até 3º círculo."));
        reg(new MagicItemDef("immovable_rod", "Immovable Rod", "Bastão Imóvel",
                "Uncommon", "Rod", false, "", 400,
                "Fixa-se magicamente no ar ao pressionar um botão. Suporta até 3.600 kg (8.000 libras). Exige teste de Força CD 30 para deslocar 3 metros."));
        reg(new MagicItemDef("weapon_plus_1", "Weapon +1", "Arma +1",
                "Uncommon", "Weapon", false, "", 500,
                "Concede +1 nas jogadas de ataque e dano efetuadas com esta arma mágica."));
        reg(new MagicItemDef("weapon_plus_2", "Weapon +2", "Arma +2",
                "Rare", "Weapon", false, "", 4000,
                "Concede +2 nas jogadas de ataque e dano efetuadas com esta arma mágica."));
        reg(new MagicItemDef("weapon_plus_3", "Weapon +3", "Arma +3",
                "Very Rare", "Weapon", false, "", 25000,
                "Concede +3 nas jogadas de ataque e dano efetuadas com esta arma mágica."));
        reg(new MagicItemDef("armor_plus_1", "Armor +1", "Armadura +1",
                "Rare", "Armor", false, "", 1500,
                "Concede +1 na Classe de Armadura enquanto vestida."));
        reg(new MagicItemDef("armor_plus_2", "Armor +2", "Armadura +2",
                "Very Rare", "Armor", false, "", 12000,
                "Concede +2 na Classe de Armadura enquanto vestida."));
        reg(new MagicItemDef("shield_plus_1", "Shield +1", "Escudo +1",
                "Uncommon", "Armor", false, "", 450,
                "Concede +1 adicional na CA (total +3 de bônus na CA) enquanto empunhado."));
        reg(new MagicItemDef("sun_blade", "Sun Blade", "Lâmina Solar",
                "Rare", "Weapon", true, "Exige sintonização", 5000,
                "Espada longa luminosa que causa dano radiante, possui propriedade Acuidade e causa dano adicional contra mortos-vivos."));
        reg(new MagicItemDef("flame_tongue", "Flame Tongue", "Língua Flamejante",
                "Rare", "Weapon", true, "Exige sintonização", 4500,
                "Ao comando verbal, irrompe em chamas causando 2d6 de dano de fogo adicional em cada acerto."));
        reg(new MagicItemDef("holy_avenger", "Holy Avenger", "Vingadora Sagrada",
                "Legendary", "Weapon", true, "Exige sintonização por um Paladino", 60000,
                "Arma +3 que causa 2d10 de dano radiante extra contra corruptores e mortos-vivos e emite aura de proteção contra magias."));
    }

    private Dmg2024RulesEngine() {}

    public static JSONObject descriptor() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", "dmg2024_runtime")
             .put("book", BOOK)
             .put("status", "compiled")
             .put("auto_rolls", false)
             .put("procedures", new JSONArray()
                     .put("dmg2024.magic_item.catalog")
                     .put("dmg2024.magic_item.attune")
                     .put("dmg2024.magic_item.unattune")
                     .put("dmg2024.magic_item.value")
                     .put("dmg2024.hazard.resolve")
                     .put("dmg2024.trap.resolve")
                     .put("dmg2024.falling.resolve")
                     .put("dmg2024.suffocation.resolve")
                     .put("dmg2024.treasure.individual")
                     .put("dmg2024.treasure.hoard"));
        } catch (Exception ignored) {}
        return o;
    }

    public static JSONObject execute(String id, JSONObject in) {
        JSONObject x = in == null ? new JSONObject() : in;
        if ("dmg2024.magic_item.catalog".equals(id)) return magicItemCatalog(x);
        if ("dmg2024.magic_item.attune".equals(id)) return magicItemAttune(x);
        if ("dmg2024.magic_item.unattune".equals(id)) return magicItemUnattune(x);
        if ("dmg2024.magic_item.value".equals(id)) return magicItemValue(x);
        if ("dmg2024.hazard.resolve".equals(id)) return resolveHazard(x);
        if ("dmg2024.trap.resolve".equals(id)) return resolveTrap(x);
        if ("dmg2024.falling.resolve".equals(id)) return resolveFalling(x);
        if ("dmg2024.suffocation.resolve".equals(id)) return resolveSuffocation(x);
        if ("dmg2024.treasure.individual".equals(id)) return resolveTreasureIndividual(x);
        if ("dmg2024.treasure.hoard".equals(id)) return resolveTreasureHoard(x);
        return blocked("unknown_dmg2024_rule");
    }

    // --- Magic Items ---

    public static MagicItemDef getMagicItem(String idOrName) {
        if (idOrName == null) return null;
        String key = norm(idOrName);
        String resolvedId = ITEM_ALIASES.get(key);
        if (resolvedId == null) resolvedId = key;
        return ITEMS_BY_ID.get(resolvedId);
    }

    private static JSONObject magicItemCatalog(JSONObject in) {
        JSONObject o = base(214);
        try {
            String rarityFilter = in.optString("rarity", "");
            String categoryFilter = in.optString("category", "");
            JSONArray list = new JSONArray();
            for (MagicItemDef def : ITEMS_BY_ID.values()) {
                if (!rarityFilter.isEmpty() && !def.rarity.equalsIgnoreCase(rarityFilter)) continue;
                if (!categoryFilter.isEmpty() && !def.category.equalsIgnoreCase(categoryFilter)) continue;
                list.put(def.toJson());
            }
            o.put("items", list);
            o.put("total_in_catalog", ITEMS_BY_ID.size());
            o.put("matching_items", list.length());
            return o;
        } catch (Exception e) {
            return blocked(o, "invalid_catalog_query");
        }
    }

    private static JSONObject magicItemAttune(JSONObject in) {
        JSONObject o = base(213);
        try {
            String itemKey = in.optString("item_id", in.optString("item", ""));
            MagicItemDef def = getMagicItem(itemKey);
            if (def == null) return blocked(o, "unknown_magic_item");
            if (!def.requiresAttunement) return blocked(o, "item_does_not_require_attunement");

            int maxAttunement = in.optInt("max_attunement_slots", 3);
            JSONArray currentAttuned = in.optJSONArray("current_attuned_items");
            int currentCount = currentAttuned != null ? currentAttuned.length() : in.optInt("attuned_item_count", 0);

            if (currentCount >= maxAttunement) {
                return blocked(o, "attunement_capacity_exceeded")
                        .put("max_slots", maxAttunement)
                        .put("current_count", currentCount);
            }

            if (currentAttuned != null) {
                for (int i = 0; i < currentAttuned.length(); i++) {
                    if (def.id.equals(currentAttuned.optString(i))) {
                        return blocked(o, "already_attuned_to_item");
                    }
                }
            }

            if (!in.optBoolean("short_rest_completed", false)) {
                return blocked(o, "short_rest_required_to_attune");
            }

            o.put("attuned", true);
            o.put("item_id", def.id);
            o.put("item_name_pt", def.namePt);
            o.put("slots_used", currentCount + 1);
            o.put("max_slots", maxAttunement);
            return o;
        } catch (Exception e) {
            return blocked(o, "attunement_failed");
        }
    }

    private static JSONObject magicItemUnattune(JSONObject in) {
        JSONObject o = base(213);
        try {
            String itemKey = in.optString("item_id", in.optString("item", ""));
            MagicItemDef def = getMagicItem(itemKey);
            if (def == null) return blocked(o, "unknown_magic_item");

            o.put("unattuned", true);
            o.put("item_id", def.id);
            o.put("item_name_pt", def.namePt);
            return o;
        } catch (Exception e) {
            return blocked(o, "unattunement_failed");
        }
    }

    private static JSONObject magicItemValue(JSONObject in) {
        JSONObject o = base(214);
        try {
            String rarity = norm(in.optString("rarity", "common"));
            boolean consumable = in.optBoolean("consumable", false);
            int minGp, maxGp, baseGp;
            if (any(rarity, "common", "comum")) {
                minGp = consumable ? 25 : 50; maxGp = consumable ? 50 : 100; baseGp = consumable ? 40 : 80;
            } else if (any(rarity, "uncommon", "incomum")) {
                minGp = consumable ? 50 : 101; maxGp = consumable ? 250 : 500; baseGp = consumable ? 150 : 350;
            } else if (any(rarity, "rare", "raro")) {
                minGp = consumable ? 250 : 501; maxGp = consumable ? 2500 : 5000; baseGp = consumable ? 1200 : 3000;
            } else if (any(rarity, "very_rare", "muito_raro")) {
                minGp = consumable ? 2500 : 5001; maxGp = consumable ? 25000 : 50000; baseGp = consumable ? 12000 : 25000;
            } else if (any(rarity, "legendary", "lendario")) {
                minGp = consumable ? 25000 : 50001; maxGp = consumable ? 100000 : 250000; baseGp = consumable ? 50000 : 100000;
            } else {
                return blocked(o, "unknown_rarity");
            }
            o.put("rarity", rarity);
            o.put("consumable", consumable);
            o.put("price_range_gp", minGp + " - " + maxGp);
            o.put("base_price_gp", baseGp);
            return o;
        } catch (Exception e) {
            return blocked(o, "invalid_rarity_inputs");
        }
    }

    // --- Environmental Hazards (DMG 2024 Chapter 3) ---

    private static JSONObject resolveHazard(JSONObject in) {
        JSONObject o = base(60);
        try {
            String hazard = norm(in.optString("hazard", ""));
            if (hazard.isEmpty()) return blocked(o, "hazard_type_required");

            if (any(hazard, "extreme_cold", "frio_extremo")) {
                boolean hasGear = in.optBoolean("has_cold_weather_gear", false);
                boolean hasResistance = in.optBoolean("has_cold_resistance", false);
                if (hasGear || hasResistance) {
                    o.put("save_required", false).put("exhaustion_gained", 0)
                     .put("summary_pt", "Criatura protegida pelo frio extremo (agasalho ou resistência a frio).");
                } else {
                    o.put("save_required", true).put("save_dc", 10).put("save_ability", "Constitution");
                    if (in.has("save_success")) {
                        boolean pass = in.optBoolean("save_success", false);
                        o.put("exhaustion_gained", pass ? 0 : 1);
                        o.put("summary_pt", pass ? "Passou na salvaguarda contra o frio extremo." : "Falhou: ganha 1 nível de Exaustão.");
                    } else {
                        o.put("requires_player_or_gm_resolution", true).put("required_input", "save_success");
                    }
                }
            } else if (any(hazard, "extreme_heat", "calor_extremo")) {
                boolean hasResistance = in.optBoolean("has_fire_resistance", false);
                int hours = Math.max(1, in.optInt("hours_exposed", 1));
                boolean heavyArmor = in.optBoolean("wearing_medium_or_heavy_armor", false);
                int dc = (heavyArmor ? 10 : 5) + (hours - 1);
                if (hasResistance) {
                    o.put("save_required", false).put("exhaustion_gained", 0)
                     .put("summary_pt", "Criatura possui resistência a fogo; imune aos efeitos do calor extremo.");
                } else {
                    o.put("save_required", true).put("save_dc", dc).put("save_ability", "Constitution");
                    if (in.has("save_success")) {
                        boolean pass = in.optBoolean("save_success", false);
                        o.put("exhaustion_gained", pass ? 0 : 1);
                        o.put("summary_pt", pass ? "Passou na salvaguarda contra o calor extremo." : "Falhou: ganha 1 nível de Exaustão.");
                    } else {
                        o.put("requires_player_or_gm_resolution", true).put("required_input", "save_success");
                    }
                }
            } else if (any(hazard, "heavy_precipitation", "precipitacao_pesada", "chuva_forte")) {
                o.put("perception_disadvantage", true);
                o.put("open_flames_extinguished", true);
                o.put("summary_pt", "Chuva ou neve intensa: Desvantagem em testes de Percepção que dependem de visão ou audição; apaga chamas abertas.");
            } else if (any(hazard, "strong_wind", "vento_forte")) {
                o.put("ranged_weapon_attack_disadvantage", true);
                o.put("flying_creatures_must_land", true);
                o.put("summary_pt", "Vento forte: Desvantagem em ataques com armas à distância; criaturas voadoras devem pousar ou caem.");
            } else if (any(hazard, "quicksand", "areia_movedica")) {
                int dc = 10;
                o.put("dex_save_dc", dc);
                if (in.has("save_success")) {
                    boolean pass = in.optBoolean("save_success", false);
                    int sunkFeet = pass ? 0 : (in.optInt("sunk_roll_d4", 2) + 1);
                    o.put("sunk_feet", sunkFeet);
                    o.put("escape_athletics_dc", 10 + sunkFeet);
                    o.put("summary_pt", pass ? "Evitou a areia movediça." : "Afundou " + sunkFeet + " pés na areia movediça. CD de escape: " + (10 + sunkFeet));
                } else {
                    o.put("requires_player_or_gm_resolution", true).put("required_input", "save_success");
                }
            } else {
                return blocked(o, "unsupported_hazard_type");
            }
            return o;
        } catch (Exception e) {
            return blocked(o, "hazard_resolution_failed");
        }
    }

    // --- Traps (DMG 2024 Chapter 3) ---

    private static JSONObject resolveTrap(JSONObject in) {
        JSONObject o = base(70);
        try {
            String severity = norm(in.optString("severity", "dangerous"));
            int dc, attackBonus;
            String diceFormula;
            if (any(severity, "setback", "contratempo")) {
                dc = 11; attackBonus = 4; diceFormula = "1d10";
            } else if (any(severity, "dangerous", "perigosa")) {
                dc = 15; attackBonus = 7; diceFormula = "4d10";
            } else if (any(severity, "deadly", "mortal")) {
                dc = 18; attackBonus = 10; diceFormula = "10d10";
            } else {
                return blocked(o, "invalid_trap_severity");
            }

            o.put("severity", severity);
            o.put("detection_dc", dc);
            o.put("disarm_dc", dc);
            o.put("save_dc", dc);
            o.put("attack_bonus", attackBonus);
            o.put("damage_dice", diceFormula);

            if (in.has("damage_roll")) {
                int dmg = in.optInt("damage_roll", 0);
                boolean saveMade = in.optBoolean("save_success", false);
                int finalDmg = saveMade ? dmg / 2 : dmg;
                o.put("applied_damage", finalDmg);
            }
            return o;
        } catch (Exception e) {
            return blocked(o, "trap_resolution_failed");
        }
    }

    // --- Falling & Suffocation ---

    private static JSONObject resolveFalling(JSONObject in) {
        JSONObject o = base(65);
        try {
            int distanceFeet = Math.max(0, in.optInt("distance_feet", 0));
            int diceCount = Math.min(20, distanceFeet / 10);
            o.put("distance_feet", distanceFeet);
            o.put("damage_dice", diceCount + "d6");
            o.put("condition_applied", distanceFeet >= 10 ? "Prone" : "None");
            if (in.has("damage_roll")) {
                int dmg = Math.max(0, in.optInt("damage_roll", 0));
                o.put("falling_damage", dmg);
            }
            return o;
        } catch (Exception e) {
            return blocked(o, "falling_resolution_failed");
        }
    }

    private static JSONObject resolveSuffocation(JSONObject in) {
        JSONObject o = base(65);
        try {
            int conMod = in.optInt("constitution_modifier", 0);
            int holdMinutes = Math.max(1, 1 + conMod); // min 30s or 1 min
            int surviveRounds = Math.max(1, conMod);
            o.put("breath_held_minutes", holdMinutes);
            o.put("suffocation_survival_rounds", surviveRounds);
            o.put("after_rounds_consequence", "Reduzido a 0 PV e começa a morrer (jogadas de salvaguarda contra a morte).");
            return o;
        } catch (Exception e) {
            return blocked(o, "suffocation_resolution_failed");
        }
    }

    // --- Treasure Tables (DMG 2024 Chapter 7) ---

    private static JSONObject resolveTreasureIndividual(JSONObject in) {
        JSONObject o = base(210);
        try {
            int cr = in.optInt("cr", 0);
            if (!in.has("d100_roll")) return blocked(o, "d100_roll_required");
            int roll = bounded(in.optInt("d100_roll"), 1, 100);
            o.put("cr", cr);
            o.put("d100_roll", roll);

            if (cr <= 4) {
                if (roll <= 30) o.put("coins_formula", "5d6 CP");
                else if (roll <= 60) o.put("coins_formula", "4d6 SP");
                else if (roll <= 70) o.put("coins_formula", "3d6 EP");
                else if (roll <= 95) o.put("coins_formula", "3d6 GP");
                else o.put("coins_formula", "1d6 PP");
            } else if (cr <= 10) {
                if (roll <= 30) o.put("coins_formula", "4d6 * 100 CP + 1d6 * 10 EP");
                else if (roll <= 60) o.put("coins_formula", "6d6 * 10 SP + 2d6 * 10 GP");
                else if (roll <= 70) o.put("coins_formula", "1d6 * 100 EP + 1d6 * 100 GP");
                else if (roll <= 95) o.put("coins_formula", "4d6 * 10 GP");
                else o.put("coins_formula", "2d6 * 10 GP + 3d6 PP");
            } else if (cr <= 16) {
                if (roll <= 20) o.put("coins_formula", "4d6 * 100 SP + 1d6 * 100 GP");
                else if (roll <= 35) o.put("coins_formula", "1d6 * 100 EP + 1d6 * 100 GP");
                else if (roll <= 75) o.put("coins_formula", "2d6 * 100 GP + 1d6 * 10 PP");
                else o.put("coins_formula", "2d6 * 100 GP + 2d6 * 10 PP");
            } else {
                if (roll <= 15) o.put("coins_formula", "2d6 * 1000 EP + 8d6 * 100 GP");
                else if (roll <= 55) o.put("coins_formula", "1d6 * 1000 GP + 1d6 * 100 PP");
                else o.put("coins_formula", "1d6 * 1000 GP + 2d6 * 100 PP");
            }

            if (in.has("calculated_coins_gp")) {
                o.put("awarded_gold_gp", in.optInt("calculated_coins_gp"));
            }
            return o;
        } catch (Exception e) {
            return blocked(o, "individual_treasure_failed");
        }
    }

    private static JSONObject resolveTreasureHoard(JSONObject in) {
        JSONObject o = base(211);
        try {
            int cr = in.optInt("cr", 0);
            if (!in.has("d100_roll")) return blocked(o, "d100_roll_required");
            int roll = bounded(in.optInt("d100_roll"), 1, 100);
            o.put("cr", cr);
            o.put("d100_roll", roll);

            if (cr <= 4) {
                o.put("base_coins", "6d6 * 100 CP, 3d6 * 100 SP, 2d6 * 10 GP");
                if (roll <= 6) o.put("magic_items_formula", "None");
                else if (roll <= 16) o.put("gems_art_formula", "2d6 gemas de 10 PO");
                else if (roll <= 26) o.put("gems_art_formula", "2d4 objetos de arte de 25 PO");
                else if (roll <= 36) o.put("gems_art_formula", "2d6 gemas de 50 PO");
                else if (roll <= 44) o.put("magic_items_formula", "1d6 itens da Tabela A");
                else if (roll <= 52) o.put("magic_items_formula", "1d4 itens da Tabela B");
                else if (roll <= 60) o.put("magic_items_formula", "1d4 itens da Tabela C");
                else o.put("magic_items_formula", "1d4 itens da Tabela F");
            } else if (cr <= 10) {
                o.put("base_coins", "2d6 * 100 SP, 2d6 * 1000 GP, 3d6 * 100 PP");
                if (roll <= 28) o.put("gems_art_formula", "2d4 objetos de arte de 250 PO");
                else if (roll <= 44) o.put("magic_items_formula", "1d6 itens da Tabela A e 1d4 da Tabela B");
                else if (roll <= 63) o.put("magic_items_formula", "1d4 itens da Tabela C e 1d4 da Tabela D");
                else o.put("magic_items_formula", "1d4 itens da Tabela F e 1d4 da Tabela G");
            } else {
                o.put("base_coins", "4d6 * 1000 GP, 5d6 * 1000 PP");
                o.put("magic_items_formula", roll <= 50 ? "1d6 itens da Tabela C" : "1d4 itens da Tabela H ou I");
            }
            return o;
        } catch (Exception e) {
            return blocked(o, "hoard_treasure_failed");
        }
    }

    // --- Helpers ---

    private static JSONObject base(int page) {
        JSONObject o = new JSONObject();
        try {
            o.put("book", BOOK);
            o.put("page", page);
            o.put("status", "ok");
            o.put("auto_rolls", false);
        } catch (Exception ignored) {}
        return o;
    }

    private static JSONObject blocked(String reason) {
        return blocked(base(0), reason);
    }

    private static JSONObject blocked(JSONObject o, String reason) {
        try {
            o.put("status", "blocked");
            o.put("requires_resolution", true);
            o.put("reason", reason);
        } catch (Exception ignored) {}
        return o;
    }

    private static int bounded(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static String norm(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s.toLowerCase(new Locale("pt", "BR")), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .trim();
    }

    private static boolean any(String s, String... xs) {
        for (String x : xs) {
            if (s.equals(norm(x))) return true;
        }
        return false;
    }
}
