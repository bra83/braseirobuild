package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.*;

/**
 * Deterministic engine for the 29 DMG 2024 Bastion Special Facilities.
 * Handles the complete catalog, prerequisite verification, and deterministic order resolution.
 * Never rolls dice automatically. Preserves PT-BR interface and receipt contracts.
 */
public final class Bastion2024SpecialFacilitiesEngine {
    public static final String BOOK = "Dungeon Master's Guide (2024)";
    public static final int TOTAL_SPECIAL_FACILITIES = 29;

    public static final class FacilityDef {
        public final String id;
        public final String nameEn;
        public final String namePt;
        public final int levelReq;
        public final String space;
        public final int hirelings;
        public final String prereqType;
        public final String prereqDescPt;
        public final List<String> allowedOrders;
        public final String summaryPt;

        public FacilityDef(String id, String nameEn, String namePt, int levelReq, String space,
                           int hirelings, String prereqType, String prereqDescPt,
                           String[] allowedOrders, String summaryPt) {
            this.id = id;
            this.nameEn = nameEn;
            this.namePt = namePt;
            this.levelReq = levelReq;
            this.space = space;
            this.hirelings = hirelings;
            this.prereqType = prereqType;
            this.prereqDescPt = prereqDescPt;
            this.allowedOrders = Collections.unmodifiableList(Arrays.asList(allowedOrders));
            this.summaryPt = summaryPt;
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", id);
                obj.put("name_en", nameEn);
                obj.put("name_pt", namePt);
                obj.put("level_req", levelReq);
                obj.put("space", space);
                obj.put("hirelings", hirelings);
                obj.put("prerequisite_type", prereqType);
                obj.put("prerequisite_desc_pt", prereqDescPt);
                JSONArray orders = new JSONArray();
                for (String o : allowedOrders) orders.put(o);
                obj.put("allowed_orders", orders);
                obj.put("summary_pt", summaryPt);
            } catch (Exception ignored) {}
            return obj;
        }
    }

    private static final Map<String, FacilityDef> BY_ID = new LinkedHashMap<>();
    private static final Map<String, String> ALIASES = new HashMap<>();

    private static void register(FacilityDef f) {
        BY_ID.put(f.id, f);
        ALIASES.put(norm(f.id), f.id);
        ALIASES.put(norm(f.nameEn), f.id);
        ALIASES.put(norm(f.namePt), f.id);
    }

    static {
        // Level 5 (9 facilities)
        register(new FacilityDef("arcane_study", "Arcane Study", "Estudo Arcano", 5, "Roomy", 1,
                "spellcasting_arcane", "Conjurador com foco arcano (Mago, Bruxo, Feiticeiro, Bardo ou Artífice)",
                new String[]{"Craft"},
                "Permite fabricar focos arcanos, livros em branco e pergaminhos mágicos de truques ou níveis 1-2."));
        register(new FacilityDef("armory", "Armory", "Armaria", 5, "Roomy", 1,
                "none", "Nenhum pré-requisito",
                new String[]{"Maintain"},
                "Armazena e equipa armas e armaduras dos defensores do Bastião, concedendo bônus de defesa."));
        register(new FacilityDef("barracks", "Barracks", "Quartel", 5, "Roomy", 1,
                "none", "Nenhum pré-requisito",
                new String[]{"Recruit"},
                "Acomoda contratados e defensores. Permite recrutar 4 novos defensores por ordem de Recrutar."));
        register(new FacilityDef("garden", "Garden", "Jardim", 5, "Roomy", 1,
                "none", "Nenhum pré-requisito",
                new String[]{"Harvest"},
                "Cultivo de ervas e plantas medicinais. Permite colher kits de curandeiro, antitoxinas e poções de cura."));
        register(new FacilityDef("library", "Library", "Biblioteca", 5, "Roomy", 1,
                "none", "Nenhum pré-requisito",
                new String[]{"Research"},
                "Acervo de tomos e tratados. Concede vantagem em testes de Inteligência para pesquisa e investigação."));
        register(new FacilityDef("sanctuary", "Sanctuary", "Santuário", 5, "Roomy", 1,
                "spellcasting_divine_primal", "Conjurador com foco divino ou primal (Clérigo, Druida, Paladino ou Patrulheiro)",
                new String[]{"Empower", "Harvest"},
                "Espaço consagrado a divindades ou forças da natureza. Permite colher água benta ou conceder bênçãos."));
        register(new FacilityDef("smithy", "Smithy", "Ferraria", 5, "Roomy", 1,
                "none", "Nenhum pré-requisito",
                new String[]{"Craft"},
                "Forja de metalurgia. Permite fabricar armas e armaduras metálicas não-mágicas e melhorias de equipamento."));
        register(new FacilityDef("storehouse", "Storehouse", "Armazém", 5, "Roomy", 1,
                "none", "Nenhum pré-requisito",
                new String[]{"Maintain", "Trade"},
                "Depósito seguro para bens comerciais, provisões e preservação de mercadorias sem deterioração."));
        register(new FacilityDef("workshop", "Workshop", "Oficina", 5, "Roomy", 1,
                "none", "Nenhum pré-requisito",
                new String[]{"Craft"},
                "Bancada para ferramentas de artesão. Permite fabricar itens de equipamento de aventura e ferramentas."));

        // Level 9 (10 facilities)
        register(new FacilityDef("gaming_hall", "Gaming Hall", "Salão de Jogos", 9, "Roomy", 4,
                "none", "Nenhum pré-requisito",
                new String[]{"Trade"},
                "Casa de apostas e entretenimento. Gera receita em PO ou prejuízo comercial por meio de negócios locais."));
        register(new FacilityDef("greenhouse", "Greenhouse", "Estufa", 9, "Roomy", 1,
                "none", "Nenhum pré-requisito",
                new String[]{"Harvest"},
                "Ambiente climatizado para flora exótica. Permite colher ingredientes para Poções de Cura Maior e venenos."));
        register(new FacilityDef("laboratory", "Laboratory", "Laboratório", 9, "Roomy", 1,
                "none", "Nenhum pré-requisito",
                new String[]{"Craft"},
                "Bancada de alquimia avançada. Permite fabricar Fogo Alquímico, Ácido, bolsas de componentes e poções."));
        register(new FacilityDef("sacristy", "Sacristy", "Sacristia", 9, "Roomy", 2,
                "spellcasting_divine_primal", "Conjurador com foco divino ou primal",
                new String[]{"Craft", "Empower"},
                "Câmara de consagração e relíquias menores. Permite fabricar símbolos sagrados e pergaminhos divinos de 3º nível."));
        register(new FacilityDef("scriptorium", "Scriptorium", "Scriptório", 9, "Roomy", 2,
                "none", "Nenhum pré-requisito",
                new String[]{"Craft"},
                "Ateliê de copistas. Permite duplicar livros, mapas, cartas náuticas e tratados com fidelidade absoluta."));
        register(new FacilityDef("stable", "Stable", "Estábulo", 9, "Roomy", 1,
                "none", "Nenhum pré-requisito",
                new String[]{"Maintain"},
                "Acomoda montarias de guerra, animais de tração e bestas auxiliares com manutenção exemplar."));
        register(new FacilityDef("teleportation_circle", "Teleportation Circle", "Círculo de Teletransporte", 9, "Roomy", 1,
                "spellcasting_arcane", "Conjurador com foco arcano",
                new String[]{"Empower"},
                "Círculo de runas permanente vinculado à rede planar de círculos de teletransporte."));
        register(new FacilityDef("theater", "Theater", "Teatro", 9, "Vast", 4,
                "none", "Nenhum pré-requisito",
                new String[]{"Trade"},
                "Palco de apresentações artísticas e peças. Gera receita em PO e prestígio cultural regional."));
        register(new FacilityDef("training_area", "Training Area", "Área de Treinamento", 9, "Roomy", 1,
                "none", "Nenhum pré-requisito",
                new String[]{"Empower"},
                "Pátio de armas e manobras. Concede inspiração marcial e prepara defensores veteranos."));
        register(new FacilityDef("trophy_room", "Trophy Room", "Sala de Troféus", 9, "Roomy", 1,
                "none", "Nenhum pré-requisito",
                new String[]{"Maintain", "Empower"},
                "Exposição de troféus de monstros derrotados. Inspira os defensores e visitantes com a bravura do herói."));

        // Level 13 (6 facilities)
        register(new FacilityDef("archive", "Archive", "Arquivo", 13, "Roomy", 3,
                "facility:library", "Exige possuir previamente uma Biblioteca (Library)",
                new String[]{"Research"},
                "Câmara de registros históricos profundos, segredos planares e recuperação de conhecimento esquecido."));
        register(new FacilityDef("meditation_chamber", "Meditation Chamber", "Câmara de Meditação", 13, "Roomy", 1,
                "none", "Nenhum pré-requisito",
                new String[]{"Empower"},
                "Recinto de silêncio e introspecção. Concede vantagem em salvaguardas mentais e clareza de foco."));
        register(new FacilityDef("menagerie", "Menagerie", "Menagerie", 13, "Vast", 4,
                "facility:stable", "Exige possuir previamente um Estábulo (Stable)",
                new String[]{"Harvest", "Maintain"},
                "Recinto para criaturas exóticas e monstruosas. Permite colher peçonhas, penas místicas e escamas."));
        register(new FacilityDef("observatory", "Observatory", "Observatório", 13, "Roomy", 2,
                "none", "Nenhum pré-requisito",
                new String[]{"Research"},
                "Torre astronômica e astrológica para predição de eventos celestes, portentos e adivinhações."));
        register(new FacilityDef("pub", "Pub", "Pub", 13, "Roomy", 4,
                "none", "Nenhum pré-requisito",
                new String[]{"Trade"},
                "Taverna movimentada no coração do Bastião. Reúne boatos regionais, pistas e renda comercial estável."));
        register(new FacilityDef("reliquary", "Reliquary", "Relicário", 13, "Roomy", 2,
                "spellcasting_divine_primal", "Capacidade de usar Símbolo Sagrado ou Foco Druídico como foco de conjuração",
                new String[]{"Empower", "Harvest"},
                "Câmara que guarda relíquias sagradas de poder elevado. Concede bênçãos miraculosas e curas profundas."));

        // Level 17 (4 facilities)
        register(new FacilityDef("demiplane", "Demiplane", "Semiplano", 17, "Roomy", 0,
                "spellcasting_arcane", "Conjurador com foco arcano ou ferramentas como foco",
                new String[]{"Empower", "Maintain"},
                "Dimensão de bolso extradimensional imune a cercos materiais. Concede proteção planar e pontos de vida temporários."));
        register(new FacilityDef("guildhall", "Guildhall", "Salão de Guilda", 17, "Vast", 8,
                "feature:expertise", "Especialização (Expertise) em ao menos uma perícia",
                new String[]{"Recruit", "Trade"},
                "Sede regional de uma guilda ou facção leal ao herói. Permite recrutar 8 defensores de elite e renda comercial maciça."));
        register(new FacilityDef("sanctum", "Sanctum", "Sanctum", 17, "Roomy", 3,
                "spellcasting_divine_primal", "Capacidade de usar Símbolo Sagrado ou Foco Druídico como foco de conjuração",
                new String[]{"Empower", "Craft"},
                "Cidadela espiritual de poder divino transcendente. Protege contra influências extraplanares hostis."));
        register(new FacilityDef("war_room", "War Room", "Sala de Guerra", 17, "Roomy", 4,
                "feature:fighting_style_or_unarmored_defense", "Estilo de Luta (Fighting Style) ou Defesa sem Armadura (Unarmored Defense)",
                new String[]{"Empower", "Maintain"},
                "Mesa estratégica de campanha militar. Coordena exércitos, planos de batalha e fortifica todos os defensores."));

        // Aliases for common variations
        ALIASES.put("barrack", "barracks");
        ALIASES.put("sanctum_sanctorum", "sanctum");
        ALIASES.put("semi_plano", "demiplane");
        ALIASES.put("semiplano", "demiplane");
        ALIASES.put("gabinete_arcano", "arcane_study");
        ALIASES.put("deposito", "storehouse");
        ALIASES.put("taverna", "pub");
    }

    private Bastion2024SpecialFacilitiesEngine() {}

    public static FacilityDef getFacility(String identifier) {
        if (identifier == null) return null;
        String key = norm(identifier);
        String resolvedId = ALIASES.get(key);
        if (resolvedId == null) {
            // direct check
            resolvedId = key;
        }
        return BY_ID.get(resolvedId);
    }

    public static int maxSpecialFacilitiesForLevel(int level) {
        if (level >= 17) return 6;
        if (level >= 13) return 5;
        if (level >= 9) return 4;
        if (level >= 5) return 2;
        return 0;
    }

    /**
     * Catalog lookup endpoint for rules engine: core2024.bastion.special_facility_catalog
     */
    public static JSONObject catalog(JSONObject in) {
        JSONObject out = base(335);
        try {
            int levelFilter = in != null ? in.optInt("character_level", 0) : 0;
            String orderFilter = in != null ? norm(in.optString("order")) : "";
            JSONArray list = new JSONArray();
            for (FacilityDef f : BY_ID.values()) {
                if (levelFilter > 0 && f.levelReq > levelFilter) continue;
                if (!orderFilter.isEmpty()) {
                    boolean orderMatches = false;
                    for (String ord : f.allowedOrders) {
                        if (norm(ord).equals(orderFilter)) {
                            orderMatches = true;
                            break;
                        }
                    }
                    if (!orderMatches) continue;
                }
                list.put(f.toJson());
            }
            out.put("facilities", list);
            out.put("total_in_catalog", TOTAL_SPECIAL_FACILITIES);
            out.put("matching_facilities", list.length());
            return out;
        } catch (Exception e) {
            return blocked(out, "invalid_catalog_query");
        }
    }

    /**
     * Eligibility validation endpoint: core2024.bastion.special_facility_validate
     */
    public static JSONObject validate(JSONObject in) {
        JSONObject out = base(335);
        if (in == null) return blocked(out, "missing_input");
        try {
            String facilityKey = in.optString("facility_id", in.optString("facility", ""));
            FacilityDef def = getFacility(facilityKey);
            if (def == null) return blocked(out, "unknown_special_facility");

            int level = in.optInt("character_level", 0);
            if (level < 5) return blocked(out, "hero_level_five_required");
            if (level < def.levelReq) {
                return blocked(out, "insufficient_level_for_facility")
                        .put("required_level", def.levelReq)
                        .put("character_level", level);
            }

            // Check capacity limit
            JSONArray existing = in.optJSONArray("existing_facilities");
            int existingCount = existing != null ? existing.length() : in.optInt("existing_facility_count", 0);
            int maxAllowed = maxSpecialFacilitiesForLevel(level);
            boolean isReplacement = in.optBoolean("is_replacement", false);
            if (!isReplacement && existingCount >= maxAllowed) {
                return blocked(out, "bastion_special_facility_capacity_reached")
                        .put("current_count", existingCount)
                        .put("max_allowed", maxAllowed);
            }

            // Check uniqueness
            if (existing != null) {
                for (int i = 0; i < existing.length(); i++) {
                    String existingId = existing.optString(i);
                    FacilityDef existingDef = getFacility(existingId);
                    if (existingDef != null && existingDef.id.equals(def.id)) {
                        return blocked(out, "duplicate_special_facility_not_allowed")
                                .put("facility_id", def.id);
                    }
                }
            }

            // Check specific prerequisites
            if ("spellcasting_arcane".equals(def.prereqType)) {
                boolean arcane = in.optBoolean("has_spellcasting_arcane", false)
                        || in.optBoolean("has_spellcasting", false)
                        || isArcaneClass(in.optString("character_class"));
                if (!arcane) {
                    return blocked(out, "arcane_spellcasting_focus_required")
                            .put("prerequisite_desc_pt", def.prereqDescPt);
                }
            } else if ("spellcasting_divine_primal".equals(def.prereqType)) {
                boolean divine = in.optBoolean("has_spellcasting_divine_primal", false)
                        || in.optBoolean("has_spellcasting", false)
                        || isDivinePrimalClass(in.optString("character_class"));
                if (!divine) {
                    return blocked(out, "divine_or_primal_focus_required")
                            .put("prerequisite_desc_pt", def.prereqDescPt);
                }
            } else if ("facility:library".equals(def.prereqType)) {
                boolean hasLib = hasExistingFacility(existing, "library");
                if (!hasLib) {
                    return blocked(out, "prerequisite_library_required")
                            .put("prerequisite_desc_pt", def.prereqDescPt);
                }
            } else if ("facility:stable".equals(def.prereqType)) {
                boolean hasStable = hasExistingFacility(existing, "stable");
                if (!hasStable) {
                    return blocked(out, "prerequisite_stable_required")
                            .put("prerequisite_desc_pt", def.prereqDescPt);
                }
            } else if ("feature:expertise".equals(def.prereqType)) {
                boolean expertise = in.optBoolean("has_expertise", false)
                        || isExpertiseClass(in.optString("character_class"));
                if (!expertise) {
                    return blocked(out, "expertise_feature_required")
                            .put("prerequisite_desc_pt", def.prereqDescPt);
                }
            } else if ("feature:fighting_style_or_unarmored_defense".equals(def.prereqType)) {
                boolean mart = in.optBoolean("has_fighting_style_or_unarmored_defense", false)
                        || in.optBoolean("has_fighting_style", false)
                        || in.optBoolean("has_unarmored_defense", false)
                        || isMartialDefenseClass(in.optString("character_class"));
                if (!mart) {
                    return blocked(out, "fighting_style_or_unarmored_defense_required")
                            .put("prerequisite_desc_pt", def.prereqDescPt);
                }
            }

            out.put("valid", true);
            out.put("facility", def.toJson());
            out.put("character_level", level);
            out.put("max_allowed_facilities", maxAllowed);
            return out;
        } catch (Exception e) {
            return blocked(out, "validation_failed");
        }
    }

    /**
     * Deterministic order resolution endpoint: core2024.bastion.special_facility_order_resolve
     */
    public static JSONObject resolveOrder(JSONObject in) {
        JSONObject out = base(336);
        if (in == null) return blocked(out, "missing_input");
        try {
            String facilityKey = in.optString("facility_id", in.optString("facility", ""));
            FacilityDef def = getFacility(facilityKey);
            if (def == null) return blocked(out, "unknown_special_facility");

            String rawOrder = in.optString("order", "");
            String canonicalOrder = canonicalizeOrder(rawOrder);
            if (canonicalOrder.isEmpty()) return blocked(out, "unknown_bastion_order");

            // Verify if facility allows this order
            if (!def.allowedOrders.contains(canonicalOrder)) {
                return blocked(out, "order_not_supported_by_facility")
                        .put("facility_id", def.id)
                        .put("attempted_order", canonicalOrder)
                        .put("allowed_orders", new JSONArray(def.allowedOrders));
            }

            // Check if facility is currently shut down/unavailable
            if (in.optBoolean("is_shut_down", false) || in.optInt("unavailable_turn", 0) > in.optInt("current_turn", 0)) {
                return blocked(out, "facility_unavailable_this_turn")
                        .put("facility_id", def.id);
            }

            JSONObject details = new JSONObject();
            JSONObject deltas = new JSONObject();
            deltas.put("gold_delta_gp", 0);
            deltas.put("defenders_delta", 0);

            String effectDescPt = "";

            if ("Recruit".equals(canonicalOrder)) {
                if ("barracks".equals(def.id)) {
                    int gained = 4;
                    deltas.put("defenders_delta", gained);
                    effectDescPt = "Quartel recrutou 4 novos defensores treinados para a guarnição do Bastião.";
                    details.put("defenders_recruited", gained);
                } else if ("guildhall".equals(def.id)) {
                    int gained = 8;
                    deltas.put("defenders_delta", gained);
                    effectDescPt = "Salão de Guilda mobilizou 8 guardas veteranos de elite para o Bastião.";
                    details.put("defenders_recruited", gained);
                } else {
                    int gained = 2;
                    deltas.put("defenders_delta", gained);
                    effectDescPt = "Recrutamento concluído com sucesso: +2 defensores.";
                    details.put("defenders_recruited", gained);
                }
            } else if ("Harvest".equals(canonicalOrder)) {
                String itemHarvested = in.optString("item_name", "");
                if ("garden".equals(def.id)) {
                    if (itemHarvested.isEmpty()) itemHarvested = "Poção de Cura (Potion of Healing)";
                    effectDescPt = "Jardim colheu insumos botânicos frescos: item obtido [" + itemHarvested + "].";
                } else if ("greenhouse".equals(def.id)) {
                    if (itemHarvested.isEmpty()) itemHarvested = "Poção de Cura Maior (Potion of Greater Healing)";
                    effectDescPt = "Estufa colheu espécimes raros climatizados: item obtido [" + itemHarvested + "].";
                } else if ("menagerie".equals(def.id)) {
                    if (itemHarvested.isEmpty()) itemHarvested = "Peçonha ou Escamas de Criatura Exótica";
                    effectDescPt = "Menagerie extraiu componentes biológicos com segurança: [" + itemHarvested + "].";
                } else if ("sanctuary".equals(def.id) || "reliquary".equals(def.id)) {
                    if (itemHarvested.isEmpty()) itemHarvested = "Frasco de Água Benta Consagrada";
                    effectDescPt = "Consagração concluída: 1 frasco de Água Benta produzido sem custo de material.";
                } else {
                    effectDescPt = "Colheita realizada com sucesso na instalação.";
                }
                details.put("item_harvested", itemHarvested);
            } else if ("Craft".equals(canonicalOrder)) {
                String itemToCraft = in.optString("item_name", "");
                int costGp = in.optInt("gold_cost_gp", 0);
                if (itemToCraft.isEmpty()) {
                    if ("arcane_study".equals(def.id)) itemToCraft = "Pergaminho de Magia (Nível 1)";
                    else if ("smithy".equals(def.id)) itemToCraft = "Arma ou Armadura Metálica Marcial";
                    else if ("laboratory".equals(def.id)) itemToCraft = "Fogo Alquímico (Frasco)";
                    else if ("scriptorium".equals(def.id)) itemToCraft = "Cópia Iluminada de Tomo / Mapa";
                    else itemToCraft = "Item de Ferramenta de Artesão";
                }
                if (costGp > 0) {
                    deltas.put("gold_delta_gp", -costGp);
                }
                details.put("crafted_item", itemToCraft);
                details.put("gold_invested_gp", costGp);
                effectDescPt = "Fabricação concluída em " + def.namePt + ": item [" + itemToCraft + "] pronto para uso.";
            } else if ("Trade".equals(canonicalOrder)) {
                // Trade produces profit or requires trade flow validated by DM
                int netProfit = in.optInt("trade_gold_delta_gp", 0);
                if (!in.has("trade_gold_delta_gp")) {
                    // Standard baseline trade yield if no manual roll/profit override is passed
                    if ("gaming_hall".equals(def.id)) netProfit = 100;
                    else if ("pub".equals(def.id)) netProfit = 150;
                    else if ("theater".equals(def.id)) netProfit = 250;
                    else if ("guildhall".equals(def.id)) netProfit = 500;
                    else netProfit = 50;
                }
                deltas.put("gold_delta_gp", netProfit);
                details.put("trade_profit_gp", netProfit);
                effectDescPt = "Comércio efetuado em " + def.namePt + ": balanço econômico de " + (netProfit >= 0 ? "+" : "") + netProfit + " PO.";
            } else if ("Research".equals(canonicalOrder)) {
                String topic = in.optString("research_topic", "Segredos regionais e históricos");
                details.put("topic", topic);
                details.put("granted_advantage", true);
                effectDescPt = "Pesquisa concluída em " + def.namePt + " sobre [" + topic + "]. Concede vantagem na próxima verificação relacionada.";
            } else if ("Empower".equals(canonicalOrder)) {
                String boon = "";
                if ("sanctuary".equals(def.id)) boon = "Bênção Divina: o herói recebe o efeito de Proteção contra o Mal e Bem ou Água Benta.";
                else if ("training_area".equals(def.id)) boon = "Inspiração Marcial: +1 dado de maestria ou vantagem em iniciativa no próximo combate.";
                else if ("sacristy".equals(def.id)) boon = "Proteção Sacra: bênção contra possessão e mortos-vivos.";
                else if ("teleportation_circle".equals(def.id)) boon = "Sintonia de Teletransporte: círculo ativo e sintonizado com a rede de círculos do reino.";
                else if ("meditation_chamber".equals(def.id)) boon = "Clareza Mental: vantagem em salvaguardas de Sabedoria ou Inteligência durante 7 dias.";
                else if ("reliquary".equals(def.id)) boon = "Bênção da Relíquia: permite conjurar Restauração Maior sem consumir espaço de magia uma vez.";
                else if ("demiplane".equals(def.id)) boon = "Refúgio Planar: descanso longo no semiplano concede 25 Pontos de Vida Temporários.";
                else if ("sanctum".equals(def.id)) boon = "Sanctum Consagrado: santuário inviolável contra espionagem e teleportes hostis.";
                else if ("war_room".equals(def.id)) boon = "Comando Tático: todos os defensores do Bastião recebem +2 em jogadas de defesa neste turno.";
                else boon = "Empoderamento ativado na instalação com sucesso.";
                details.put("boon_granted", boon);
                effectDescPt = def.namePt + " fortalecido: " + boon;
            } else if ("Maintain".equals(canonicalOrder)) {
                effectDescPt = "Manutenção e supervisão operacional concluídas em " + def.namePt + ". Contratados e instalações preservados.";
                details.put("maintenance_complete", true);
            }

            out.put("status", "ok");
            out.put("facility_id", def.id);
            out.put("facility_name_en", def.nameEn);
            out.put("facility_name_pt", def.namePt);
            out.put("canonical_order", canonicalOrder);
            out.put("effect_description_pt", effectDescPt);
            out.put("order_details", details);
            out.put("resource_deltas", deltas);
            out.put("gold_delta_gp", deltas.optInt("gold_delta_gp", 0));
            out.put("defenders_delta", deltas.optInt("defenders_delta", 0));
            out.put("requires_dm_confirmation", true);
            out.put("persisted_to_world", false);
            out.put("auto_rolls", false);
            return out;
        } catch (Exception e) {
            return blocked(out, "order_resolution_failed");
        }
    }

    private static String canonicalizeOrder(String s) {
        String x = norm(s);
        if (any(x, "craft", "fabricar")) return "Craft";
        if (any(x, "empower", "fortalecer", "empoderar")) return "Empower";
        if (any(x, "harvest", "colher")) return "Harvest";
        if (any(x, "maintain", "manter")) return "Maintain";
        if (any(x, "recruit", "recrutar")) return "Recruit";
        if (any(x, "research", "pesquisar")) return "Research";
        if (any(x, "trade", "comerciar", "negociar")) return "Trade";
        return "";
    }

    private static boolean isArcaneClass(String cls) {
        if (cls == null) return false;
        String c = norm(cls);
        return any(c, "wizard", "mago", "sorcerer", "feiticeiro", "warlock", "bruxo", "bard", "bardo", "artificer", "artifice");
    }

    private static boolean isDivinePrimalClass(String cls) {
        if (cls == null) return false;
        String c = norm(cls);
        return any(c, "cleric", "clerigo", "druid", "druida", "paladin", "paladino", "ranger", "patrulheiro");
    }

    private static boolean isExpertiseClass(String cls) {
        if (cls == null) return false;
        String c = norm(cls);
        return any(c, "rogue", "ladino", "bard", "bardo", "ranger", "patrulheiro");
    }

    private static boolean isMartialDefenseClass(String cls) {
        if (cls == null) return false;
        String c = norm(cls);
        return any(c, "fighter", "guerreiro", "paladin", "paladino", "barbarian", "barbaro", "monk", "monge", "ranger", "patrulheiro");
    }

    private static boolean hasExistingFacility(JSONArray arr, String facilityId) {
        if (arr == null) return false;
        for (int i = 0; i < arr.length(); i++) {
            FacilityDef f = getFacility(arr.optString(i));
            if (f != null && f.id.equals(facilityId)) return true;
        }
        return false;
    }

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

    private static JSONObject blocked(JSONObject o, String reason) {
        try {
            o.put("status", "blocked");
            o.put("requires_resolution", true);
            o.put("reason", reason);
        } catch (Exception ignored) {}
        return o;
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
