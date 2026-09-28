package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Motor determinístico de Combate em Massa, Exércitos e Guerra (Warfare)
 * para Pathfinder 2e Kingmaker (Braseiro PF2e).
 *
 * Todas as regras são resolvidas matematicamente com os 4 graus de sucesso do PF2e.
 * Termos voltados ao usuário são apresentados em Português do Brasil (pt-BR).
 */
public final class KingmakerWarfareEngine {

    // --- 14 CONDIÇÕES DE GUERRA ---
    public static final String COND_CONCEALED = "concealed";
    public static final String COND_DEFEATED = "defeated";
    public static final String COND_DESTROYED = "destroyed";
    public static final String COND_DISTANT = "distant";
    public static final String COND_EFFICIENT = "efficient";
    public static final String COND_ENGAGED = "engaged";
    public static final String COND_FORTIFIED = "fortified";
    public static final String COND_LOST = "lost";
    public static final String COND_MIRED = "mired";
    public static final String COND_OUTFLANKED = "outflanked";
    public static final String COND_PINNED = "pinned";
    public static final String COND_ROUTED = "routed";
    public static final String COND_SHAKEN = "shaken";
    public static final String COND_WEARY = "weary";

    public static final String[] ALL_CONDITIONS = {
            COND_CONCEALED, COND_DEFEATED, COND_DESTROYED, COND_DISTANT,
            COND_EFFICIENT, COND_ENGAGED, COND_FORTIFIED, COND_LOST,
            COND_MIRED, COND_OUTFLANKED, COND_PINNED, COND_ROUTED,
            COND_SHAKEN, COND_WEARY
    };

    // --- AÇÕES BÁSICAS DE GUERRA ---
    public static final String ACT_ADVANCE = "advance";
    public static final String ACT_BATTLE = "battle";
    public static final String ACT_BATTLE_MELEE = "battle_melee";
    public static final String ACT_BATTLE_RANGED = "battle_ranged";
    public static final String ACT_DISENGAGE = "disengage";
    public static final String ACT_GUARD = "guard";
    public static final String ACT_RALLY = "rally";
    public static final String ACT_RETREAT = "retreat";

    // --- AÇÕES TÁTICAS DE GUERRA ---
    public static final String TACTIC_ALL_OUT_ASSAULT = "all_out_assault";
    public static final String TACTIC_BATTLEFIELD_MEDICINE = "battlefield_medicine";
    public static final String TACTIC_COUNTERATTACK = "counterattack";
    public static final String TACTIC_COVERING_FIRE = "covering_fire";
    public static final String TACTIC_DEFENSIVE_STANCE = "defensive_stance";
    public static final String TACTIC_DIRTY_FIGHTING = "dirty_fighting";
    public static final String TACTIC_FALSE_RETREAT = "false_retreat";
    public static final String TACTIC_FEINT = "feint";
    public static final String TACTIC_OUTFLANK = "outflank";
    public static final String TACTIC_OVERWHELMING_BOMBARDMENT = "overwhelming_bombardment";
    public static final String TACTIC_TAUNT = "taunt";

    public static final String[] ALL_TACTICS = {
            TACTIC_ALL_OUT_ASSAULT, TACTIC_BATTLEFIELD_MEDICINE, TACTIC_COUNTERATTACK,
            TACTIC_COVERING_FIRE, TACTIC_DEFENSIVE_STANCE, TACTIC_DIRTY_FIGHTING,
            TACTIC_FALSE_RETREAT, TACTIC_FEINT, TACTIC_OUTFLANK,
            TACTIC_OVERWHELMING_BOMBARDMENT, TACTIC_TAUNT
    };

    private static final Map<String, String> CONDITION_LABELS_PT = new LinkedHashMap<String, String>();
    private static final Map<String, String> CONDITION_DESCRIPTIONS_PT = new LinkedHashMap<String, String>();
    private static final Map<String, String> ACTION_LABELS_PT = new LinkedHashMap<String, String>();
    private static final Map<String, String> ACTION_DESCRIPTIONS_PT = new LinkedHashMap<String, String>();

    static {
        CONDITION_LABELS_PT.put(COND_CONCEALED, "Oculto");
        CONDITION_LABELS_PT.put(COND_DEFEATED, "Derrotado");
        CONDITION_LABELS_PT.put(COND_DESTROYED, "Destruído");
        CONDITION_LABELS_PT.put(COND_DISTANT, "Distante");
        CONDITION_LABELS_PT.put(COND_EFFICIENT, "Eficiente");
        CONDITION_LABELS_PT.put(COND_ENGAGED, "Engajado");
        CONDITION_LABELS_PT.put(COND_FORTIFIED, "Fortificado");
        CONDITION_LABELS_PT.put(COND_LOST, "Perdido");
        CONDITION_LABELS_PT.put(COND_MIRED, "Atolado");
        CONDITION_LABELS_PT.put(COND_OUTFLANKED, "Flanqueado");
        CONDITION_LABELS_PT.put(COND_PINNED, "Encurralado");
        CONDITION_LABELS_PT.put(COND_ROUTED, "Desbaratado");
        CONDITION_LABELS_PT.put(COND_SHAKEN, "Abalado");
        CONDITION_LABELS_PT.put(COND_WEARY, "Exausto");

        CONDITION_DESCRIPTIONS_PT.put(COND_CONCEALED, "O exército está oculto no terreno; ataques contra ele exigem sucesso em teste puro CD 5.");
        CONDITION_DESCRIPTIONS_PT.put(COND_DEFEATED, "O exército atingiu 0 PV e está incapacitado. Sofrer dano adicional exige teste puro CD 15 para não ser Destruído.");
        CONDITION_DESCRIPTIONS_PT.put(COND_DESTROYED, "O exército foi completamente aniquilado e eliminado da campanha.");
        CONDITION_DESCRIPTIONS_PT.put(COND_DISTANT, "O exército está a longa distância; não pode realizar nem sofrer ataques corpo a corpo.");
        CONDITION_DESCRIPTIONS_PT.put(COND_EFFICIENT, "O exército opera com suprimentos otimizados; +1 de status em testes de Manobra e consumo reduzido.");
        CONDITION_DESCRIPTIONS_PT.put(COND_ENGAGED, "O exército está engajado em combate corpo a corpo direto.");
        CONDITION_DESCRIPTIONS_PT.put(COND_FORTIFIED, "Posições defensivas preparadas: +2 de circunstância na CA, CD de Manobra e CD de Moral.");
        CONDITION_DESCRIPTIONS_PT.put(COND_LOST, "Exército desorientado; não pode Avançar até se orientar.");
        CONDITION_DESCRIPTIONS_PT.put(COND_MIRED, "Terreno difícil ou lamaçal: penalidade de circunstância de -2 em testes de Manobra e movimento restrito.");
        CONDITION_DESCRIPTIONS_PT.put(COND_OUTFLANKED, "Penalidade de circunstância de -2 na CA.");
        CONDITION_DESCRIPTIONS_PT.put(COND_PINNED, "Sob fogo pesado ou cercado; não pode Avançar nem Retirar sem antes Desengajar.");
        CONDITION_DESCRIPTIONS_PT.put(COND_ROUTED, "Em pânico descontrolado; deve gastar suas ações em Retirada e não pode atacar.");
        CONDITION_DESCRIPTIONS_PT.put(COND_SHAKEN, "Abalado moralmente; penalidade de status igual ao nível do abalo (1-4) em ataques, CA, testes e CDs.");
        CONDITION_DESCRIPTIONS_PT.put(COND_WEARY, "Exaustão ou falta de suprimentos; penalidade de status de -1 em CA, ataques e testes.");

        ACTION_LABELS_PT.put(ACT_ADVANCE, "Avançar");
        ACTION_LABELS_PT.put(ACT_BATTLE, "Batalhar");
        ACTION_LABELS_PT.put(ACT_BATTLE_MELEE, "Ataque Corpo a Corpo");
        ACTION_LABELS_PT.put(ACT_BATTLE_RANGED, "Ataque à Distância");
        ACTION_LABELS_PT.put(ACT_DISENGAGE, "Desengajar");
        ACTION_LABELS_PT.put(ACT_GUARD, "Guardar");
        ACTION_LABELS_PT.put(ACT_RALLY, "Reagrupar");
        ACTION_LABELS_PT.put(ACT_RETREAT, "Retirar");

        ACTION_LABELS_PT.put(TACTIC_ALL_OUT_ASSAULT, "Assalto Total");
        ACTION_LABELS_PT.put(TACTIC_BATTLEFIELD_MEDICINE, "Medicina de Campanha");
        ACTION_LABELS_PT.put(TACTIC_COUNTERATTACK, "Contra-ataque");
        ACTION_LABELS_PT.put(TACTIC_COVERING_FIRE, "Fogo de Cobertura");
        ACTION_LABELS_PT.put(TACTIC_DEFENSIVE_STANCE, "Postura Defensiva");
        ACTION_LABELS_PT.put(TACTIC_DIRTY_FIGHTING, "Combate Sujo");
        ACTION_LABELS_PT.put(TACTIC_FALSE_RETREAT, "Falsa Retirada");
        ACTION_LABELS_PT.put(TACTIC_FEINT, "Finta");
        ACTION_LABELS_PT.put(TACTIC_OUTFLANK, "Flanquear");
        ACTION_LABELS_PT.put(TACTIC_OVERWHELMING_BOMBARDMENT, "Bombardeio Avassalador");
        ACTION_LABELS_PT.put(TACTIC_TAUNT, "Provocar");

        ACTION_DESCRIPTIONS_PT.put(ACT_ADVANCE, "Move o exército para engajar um inimigo ou reposicionar-se no campo de batalha.");
        ACTION_DESCRIPTIONS_PT.put(ACT_BATTLE, "Resolve um ataque militar (corpo a corpo ou à distância) aplicando Penalidade de Ataque Múltiplo (MAP).");
        ACTION_DESCRIPTIONS_PT.put(ACT_BATTLE_MELEE, "Ataque corpo a corpo contra um exército engajado.");
        ACTION_DESCRIPTIONS_PT.put(ACT_BATTLE_RANGED, "Ataque com armas de projéteis consumindo 1 unidade de munição.");
        ACTION_DESCRIPTIONS_PT.put(ACT_DISENGAGE, "Manobra para romper o contato corpo a corpo e recuar para distância segura.");
        ACTION_DESCRIPTIONS_PT.put(ACT_GUARD, "Assume posição de guarda, recebendo a condição Fortificado (+2 CA e CDs de defesa).");
        ACTION_DESCRIPTIONS_PT.put(ACT_RALLY, "Teste de Moral para restaurar a ordem, reduzir o abalo moral ou conter a deserção.");
        ACTION_DESCRIPTIONS_PT.put(ACT_RETREAT, "Retirada ordenada do campo de batalha para preservar as forças.");

        ACTION_DESCRIPTIONS_PT.put(TACTIC_ALL_OUT_ASSAULT, "Ataque temerário: +2 de circunstância no ataque e dano adicional (+1 hit), mas fica Flanqueado até a próxima rodada.");
        ACTION_DESCRIPTIONS_PT.put(TACTIC_BATTLEFIELD_MEDICINE, "Primeiros socorros rápidos em combate: recupera 1 a 2 PV e alivia a exaustão.");
        ACTION_DESCRIPTIONS_PT.put(TACTIC_COUNTERATTACK, "Reação preparada para golpear o inimigo que acabou de desferir um ataque.");
        ACTION_DESCRIPTIONS_PT.put(TACTIC_COVERING_FIRE, "Rajada de projéteis que consome 1 munição e encurrala o alvo, impedindo seu avanço.");
        ACTION_DESCRIPTIONS_PT.put(TACTIC_DEFENSIVE_STANCE, "Muralha de escudos ou falange: +2 CA e defesas, com -2 em ataques e sem avanço.");
        ACTION_DESCRIPTIONS_PT.put(TACTIC_DIRTY_FIGHTING, "Táticas desleais: teste de Manobra para deixar o inimigo Flanqueado e atolado.");
        ACTION_DESCRIPTIONS_PT.put(TACTIC_FALSE_RETREAT, "Recuo simulado para atrair o inimigo a uma armadilha, deixando-o desprotegido.");
        ACTION_DESCRIPTIONS_PT.put(TACTIC_FEINT, "Manobra diversionista para induzir erro no inimigo, tornando-o Flanqueado contra o próximo ataque.");
        ACTION_DESCRIPTIONS_PT.put(TACTIC_OUTFLANK, "Movimento tático para cercar o inimigo pelas alas, aplicando a condição Flanqueado.");
        ACTION_DESCRIPTIONS_PT.put(TACTIC_OVERWHELMING_BOMBARDMENT, "Barragem maciça de cerco: consome 2 munições e inflige dano com abalo moral devastador.");
        ACTION_DESCRIPTIONS_PT.put(TACTIC_TAUNT, "Guerra psicológica: teste de Moral para abalar e desestabilizar as fileiras inimigas.");
    }

    private final Pathfinder2eRulesEngine pf2;

    public KingmakerWarfareEngine() {
        this.pf2 = new Pathfinder2eRulesEngine();
    }

    public KingmakerWarfareEngine(Pathfinder2eRulesEngine rulesEngine) {
        this.pf2 = rulesEngine != null ? rulesEngine : new Pathfinder2eRulesEngine();
    }

    public Pathfinder2eRulesEngine rules() {
        return pf2;
    }

    // --- NORMALIZAÇÃO E ESTATÍSTICAS COMPLETAS ---

    /**
     * Garante que todas as 15+ estatísticas de um exército estejam devidamente preenchidas.
     */
    public JSONObject normalizeArmy(JSONObject raw) {
        JSONObject army;
        try {
            army = raw == null ? new JSONObject() : new JSONObject(raw.toString());
        } catch (Exception ignored) {
            army = new JSONObject();
        }
        try {
            int level = Math.max(1, Math.min(20, army.optInt("level", 1)));
            int size = Math.max(1, Math.min(5, army.optInt("size", 1)));
            int maxHp = Math.max(1, army.optInt("max_hp", army.optInt("hp", Math.max(4, level * 4))));
            int hp = Math.max(0, Math.min(maxHp, army.optInt("hp", maxHp)));
            int routThreshold = Math.max(1, army.optInt("rout_threshold", Math.max(1, maxHp / 2)));
            int ac = Math.max(10, army.optInt("ac", 14 + level));
            int attackMod = army.optInt("attack_modifier", level + 3);
            int moraleMod = army.optInt("morale_modifier", level + 1);
            int moraleDc = Math.max(10, army.optInt("morale_dc", 10 + moraleMod));
            int maneuverMod = army.optInt("maneuver_modifier", level + 1);
            int maneuverDc = Math.max(10, army.optInt("maneuver_dc", 10 + maneuverMod));
            int scouting = army.optInt("scouting", level);
            int ammunition = Math.max(0, army.optInt("ammunition", 5));
            int supplies = Math.max(0, army.optInt("supplies", 5));
            int consumption = Math.max(0, army.optInt("consumption", 1));

            army.put("level", level);
            army.put("size", size);
            army.put("hp", hp);
            army.put("max_hp", maxHp);
            army.put("rout_threshold", routThreshold);
            army.put("ac", ac);
            army.put("attack_modifier", attackMod);
            army.put("morale_modifier", moraleMod);
            army.put("morale_dc", moraleDc);
            army.put("maneuver_modifier", maneuverMod);
            army.put("maneuver_dc", maneuverDc);
            army.put("scouting", scouting);
            army.put("ammunition", ammunition);
            army.put("supplies", supplies);
            army.put("consumption", consumption);

            // Garante listas JSON
            JSONArray conditions = jsonArray(army.opt("conditions_json"));
            army.put("conditions_json", conditions.toString());

            JSONArray tactics = jsonArray(army.opt("tactics_json"));
            army.put("tactics_json", tactics.toString());

            // Garante defeated se HP == 0
            if (hp <= 0 && !hasCondition(army, COND_DEFEATED)) {
                addCondition(army, COND_DEFEATED);
            }
        } catch (Exception ignored) {}
        return army;
    }

    // --- GERENCIAMENTO DE CONDIÇÕES ---

    public boolean hasCondition(JSONObject army, String condition) {
        if (army == null || condition == null) return false;
        String targetKey = normKey(condition);
        JSONArray conds = jsonArray(army.opt("conditions_json"));
        for (int i = 0; i < conds.length(); i++) {
            String c = normKey(conds.optString(i));
            if (c.equals(targetKey)) return true;
            if (targetKey.equals(COND_SHAKEN) && c.startsWith("shaken")) return true;
        }
        return false;
    }

    public int getShakenValue(JSONObject army) {
        if (army == null) return 0;
        JSONArray conds = jsonArray(army.opt("conditions_json"));
        int highest = 0;
        for (int i = 0; i < conds.length(); i++) {
            String c = normKey(conds.optString(i));
            if (c.equals("shaken")) {
                highest = Math.max(highest, 1);
            } else if (c.startsWith("shaken_") || c.startsWith("shaken:")) {
                String[] parts = c.split("[:_]");
                if (parts.length >= 2) {
                    try {
                        highest = Math.max(highest, Integer.parseInt(parts[1]));
                    } catch (Exception ignored) {}
                }
            }
        }
        return highest;
    }

    public JSONObject addCondition(JSONObject army, String condition) {
        if (army == null || condition == null) return army;
        String raw = condition.trim().toLowerCase(Locale.US);
        String base = normKey(raw);
        if (base.startsWith("shaken")) {
            int val = 1;
            if (raw.contains(":") || raw.contains("_")) {
                try {
                    String[] parts = raw.split("[:_]");
                    val = Integer.parseInt(parts[1]);
                } catch (Exception ignored) {}
            }
            return setShaken(army, Math.max(getShakenValue(army), val));
        }
        JSONArray conds = jsonArray(army.opt("conditions_json"));
        boolean exists = false;
        for (int i = 0; i < conds.length(); i++) {
            if (normKey(conds.optString(i)).equals(base)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            conds.put(base);
            try { army.put("conditions_json", conds.toString()); } catch (Exception ignored) {}
        }
        return army;
    }

    public JSONObject removeCondition(JSONObject army, String condition) {
        if (army == null || condition == null) return army;
        String target = normKey(condition);
        JSONArray conds = jsonArray(army.opt("conditions_json"));
        JSONArray next = new JSONArray();
        for (int i = 0; i < conds.length(); i++) {
            String c = conds.optString(i);
            String k = normKey(c);
            if (target.equals(COND_SHAKEN) && k.startsWith("shaken")) {
                continue;
            }
            if (!k.equals(target)) {
                next.put(c);
            }
        }
        try { army.put("conditions_json", next.toString()); } catch (Exception ignored) {}
        return army;
    }

    public JSONObject setShaken(JSONObject army, int value) {
        if (army == null) return army;
        removeCondition(army, COND_SHAKEN);
        if (value <= 0) return army;
        int clamped = Math.min(4, value);
        JSONArray conds = jsonArray(army.opt("conditions_json"));
        conds.put("shaken:" + clamped);
        try { army.put("conditions_json", conds.toString()); } catch (Exception ignored) {}
        // Limiar de Rota: se o Abalo atinge 4, o exército fica Desbaratado (Routed)!
        if (clamped >= 4 && !hasCondition(army, COND_ROUTED)) {
            addCondition(army, COND_ROUTED);
        }
        return army;
    }

    // --- CÁLCULO DINÂMICO DE MODIFICADORES ---

    public int effectiveAc(JSONObject army) {
        if (army == null) return 10;
        int ac = army.optInt("ac", 15);
        int circ = 0;
        if (hasCondition(army, COND_FORTIFIED)) circ += 2;
        if (hasCondition(army, COND_OUTFLANKED)) circ -= 2;

        int status = 0;
        int shaken = getShakenValue(army);
        if (shaken > 0) status -= shaken;
        if (hasCondition(army, COND_WEARY)) status -= 1;

        return Math.max(0, ac + circ + status);
    }

    public int effectiveAttackModifier(JSONObject army) {
        if (army == null) return 0;
        int base = army.optInt("attack_modifier", 0);
        int status = 0;
        if (hasCondition(army, COND_EFFICIENT)) status += 1;
        int shaken = getShakenValue(army);
        if (shaken > 0) status -= shaken;
        if (hasCondition(army, COND_WEARY)) status -= 1;
        return base + status;
    }

    public int effectiveManeuverModifier(JSONObject army) {
        if (army == null) return 0;
        int base = army.optInt("maneuver_modifier", 0);
        int circ = hasCondition(army, COND_MIRED) ? -2 : 0;
        int status = 0;
        if (hasCondition(army, COND_EFFICIENT)) status += 1;
        int shaken = getShakenValue(army);
        if (shaken > 0) status -= shaken;
        if (hasCondition(army, COND_WEARY)) status -= 1;
        return base + circ + status;
    }

    public int effectiveManeuverDc(JSONObject army) {
        if (army == null) return 15;
        int dc = army.optInt("maneuver_dc", 15);
        int circ = hasCondition(army, COND_FORTIFIED) ? 2 : 0;
        int status = 0;
        int shaken = getShakenValue(army);
        if (shaken > 0) status -= shaken;
        if (hasCondition(army, COND_WEARY)) status -= 1;
        return Math.max(10, dc + circ + status);
    }

    public int effectiveMoraleModifier(JSONObject army) {
        if (army == null) return 0;
        int base = army.optInt("morale_modifier", 0);
        int status = 0;
        int shaken = getShakenValue(army);
        if (shaken > 0) status -= shaken;
        if (hasCondition(army, COND_WEARY)) status -= 1;
        return base + status;
    }

    public int effectiveMoraleDc(JSONObject army) {
        if (army == null) return 15;
        int dc = army.optInt("morale_dc", 15);
        int circ = hasCondition(army, COND_FORTIFIED) ? 2 : 0;
        int status = 0;
        int shaken = getShakenValue(army);
        if (shaken > 0) status -= shaken;
        if (hasCondition(army, COND_WEARY)) status -= 1;
        return Math.max(10, dc + circ + status);
    }

    public int armyMapPenalty(int attackIndex, boolean agile) {
        return pf2.mapPenalty(attackIndex, agile);
    }

    public int armyHits(String degree) {
        if ("CRITICAL_SUCCESS".equalsIgnoreCase(degree)) return 2;
        if ("SUCCESS".equalsIgnoreCase(degree)) return 1;
        return 0;
    }

    // --- AÇÕES BÁSICAS DE GUERRA ---

    /**
     * Avançar (Advance): Move o exército para engajar um inimigo ou romper distâncias.
     */
    public JSONObject advance(JSONObject actor, JSONObject target, int naturalRoll) {
        validateActive(actor);
        if (hasCondition(actor, COND_ROUTED)) {
            throw new IllegalStateException("Exército Desbaratado não pode Avançar; apenas Retirar.");
        }
        if (hasCondition(actor, COND_PINNED)) {
            throw new IllegalStateException("Exército Encurralado não pode Avançar; desengaje primeiro.");
        }

        int dc = target != null ? effectiveManeuverDc(target) : 15;
        int mod = effectiveManeuverModifier(actor);
        JSONObject check = pf2.resolveCheck(naturalRoll, mod, dc);
        String degree = check.optString("degree");

        JSONObject out = new JSONObject();
        StringBuilder narrative = new StringBuilder();

        try {
            out.put("action", ACT_ADVANCE);
            out.put("action_pt", ACTION_LABELS_PT.get(ACT_ADVANCE));
            out.put("check", check);

            if ("CRITICAL_SUCCESS".equals(degree)) {
                removeCondition(actor, COND_DISTANT);
                addCondition(actor, COND_ENGAGED);
                if (target != null) {
                    removeCondition(target, COND_DISTANT);
                    addCondition(target, COND_ENGAGED);
                    addCondition(target, COND_OUTFLANKED);
                }
                narrative.append("Avanço magistral! As fileiras avançam com precisão, engajam o alvo e o deixam Flanqueado.");
            } else if ("SUCCESS".equals(degree)) {
                removeCondition(actor, COND_DISTANT);
                addCondition(actor, COND_ENGAGED);
                if (target != null) {
                    removeCondition(target, COND_DISTANT);
                    addCondition(target, COND_ENGAGED);
                }
                narrative.append("Avanço bem-sucedido. O exército fecha a distância e engaja o inimigo em combate direto.");
            } else if ("FAILURE".equals(degree)) {
                narrative.append("Avanço frustrado pelas fileiras adversárias ou terreno desfavorável; posição mantida.");
            } else { // CRITICAL_FAILURE
                addCondition(actor, COND_MIRED);
                narrative.append("Avanço desastroso! O exército perde a coesão no terreno e fica Atolado.");
            }

            out.put("narrative", narrative.toString());
            out.put("actor", actor);
            if (target != null) out.put("target", target);
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Batalhar (Battle): Ataque militar (corpo a corpo ou à distância) com suporte a MAP,
     * fraquezas, resistências, limiar de rota e derrota.
     */
    public JSONObject battle(JSONObject actor, JSONObject target, boolean ranged, int naturalRoll,
                             int attackIndex, boolean agile, int weakness, int resistance, int flatCheckRoll) {
        validateActive(actor);
        if (target == null) throw new IllegalArgumentException("Alvo ausente para batalha.");
        if (hasCondition(actor, COND_ROUTED)) {
            throw new IllegalStateException("Exército Desbaratado não pode Batalhar; deve Retirar.");
        }

        // Se o alvo já está derrotado e toma novo dano, é um teste de aniquilação/destruição
        if (hasCondition(target, COND_DEFEATED)) {
            return resolveDamageAgainstDefeated(actor, target, naturalRoll, flatCheckRoll);
        }

        // Ataque à Distância exige munição
        if (ranged) {
            int ammo = actor.optInt("ammunition", 0);
            if (ammo <= 0) {
                throw new IllegalStateException("Exército sem munição para ataques à distância.");
            }
            try { actor.put("ammunition", ammo - 1); } catch (Exception ignored) {}
        } else {
            // Ataque corpo a corpo não pode ser feito por exército Distante sem antes engajar
            if (hasCondition(actor, COND_DISTANT)) {
                throw new IllegalStateException("Exército Distante não pode realizar ataque corpo a corpo sem Avançar.");
            }
        }

        // Verificação de Oculto (Concealed): teste puro CD 5
        boolean missFromConcealed = false;
        if (hasCondition(target, COND_CONCEALED)) {
            int flatRoll = flatCheckRoll > 0 ? flatCheckRoll : 10;
            if (flatRoll < 5) {
                missFromConcealed = true;
            }
        }

        int map = armyMapPenalty(attackIndex, agile);
        int baseMod = effectiveAttackModifier(actor);
        int totalMod = baseMod + map;
        if (ranged && hasCondition(actor, COND_ENGAGED)) {
            totalMod -= 2; // Penalidade de disparo sob engajamento direto
        }

        int targetAc = effectiveAc(target);
        JSONObject check = pf2.resolveCheck(naturalRoll, totalMod, targetAc);
        String degree = check.optString("degree");

        int hits = armyHits(degree);
        if (missFromConcealed) {
            hits = 0;
            try { check.put("concealed_miss", true); } catch (Exception ignored) {}
        }

        int rawDmg = Math.max(0, hits) + Math.max(0, weakness);
        int damage = hits > 0 ? Math.max(0, rawDmg - Math.max(0, resistance)) : 0;

        int currentHp = target.optInt("hp", 4);
        int maxHp = target.optInt("max_hp", 4);
        int nextHp = Math.max(0, currentHp - damage);
        try { target.put("hp", nextHp); } catch (Exception ignored) {}

        boolean becameDefeated = false;
        if (nextHp <= 0) {
            becameDefeated = true;
            addCondition(target, COND_DEFEATED);
            removeCondition(target, COND_ENGAGED);
            removeCondition(target, COND_PINNED);
            removeCondition(target, COND_FORTIFIED);
        }

        // Teste de Limiar de Rota (Rout Threshold) se o PV descer até o limiar
        int routThreshold = target.optInt("rout_threshold", Math.max(1, maxHp / 2));
        boolean triggeredRoutCheck = nextHp > 0 && nextHp <= routThreshold;
        if (triggeredRoutCheck && !hasCondition(target, COND_ROUTED)) {
            // Aplica Abalado inicial ou avisa da necessidade de teste de Moral
            int curShaken = getShakenValue(target);
            if (curShaken == 0) {
                setShaken(target, 1);
            }
        }

        // Ambas as forças ficam engajadas se foi ataque corpo a corpo
        if (!ranged && !becameDefeated) {
            removeCondition(actor, COND_DISTANT);
            addCondition(actor, COND_ENGAGED);
            removeCondition(target, COND_DISTANT);
            addCondition(target, COND_ENGAGED);
        }

        JSONObject out = new JSONObject();
        StringBuilder narrative = new StringBuilder();
        try {
            out.put("action", ranged ? ACT_BATTLE_RANGED : ACT_BATTLE_MELEE);
            out.put("action_pt", ranged ? ACTION_LABELS_PT.get(ACT_BATTLE_RANGED) : ACTION_LABELS_PT.get(ACT_BATTLE_MELEE));
            out.put("check", check);
            out.put("hits", hits);
            out.put("damage", damage);
            out.put("target_hp", nextHp);
            out.put("target_max_hp", maxHp);
            out.put("defeated", becameDefeated);
            out.put("rout_threshold_triggered", triggeredRoutCheck);
            out.put("actor", actor);
            out.put("target", target);

            if (missFromConcealed) {
                narrative.append("O ataque falhou devido à camuflagem do alvo (teste puro CD 5 não atingido). ");
            } else if (hits > 0) {
                narrative.append(check.optString("degree_pt")).append(": ")
                        .append(hits).append(" acerto(s) militar(es), causando ")
                        .append(damage).append(" de dano a ").append(target.optString("name", "alvo")).append(". ");
                if (becameDefeated) {
                    narrative.append("O exército inimigo foi DERROTADO (0 PV)! ");
                } else if (triggeredRoutCheck) {
                    narrative.append("O exército atingiu o Limiar de Rota (PV <= ").append(routThreshold).append(")! Fica Abalado. ");
                }
            } else {
                narrative.append(check.optString("degree_pt")).append(": O ataque falhou contra a CA ").append(targetAc).append(". ");
            }
            out.put("narrative", narrative.toString().trim());
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Desengajar (Disengage): Rompe o combate corpo a corpo.
     */
    public JSONObject disengage(JSONObject actor, JSONObject target, int naturalRoll) {
        validateActive(actor);
        int dc = target != null ? effectiveManeuverDc(target) : 15;
        int mod = effectiveManeuverModifier(actor);
        JSONObject check = pf2.resolveCheck(naturalRoll, mod, dc);
        String degree = check.optString("degree");

        JSONObject out = new JSONObject();
        StringBuilder narrative = new StringBuilder();
        try {
            out.put("action", ACT_DISENGAGE);
            out.put("action_pt", ACTION_LABELS_PT.get(ACT_DISENGAGE));
            out.put("check", check);

            if ("CRITICAL_SUCCESS".equals(degree)) {
                removeCondition(actor, COND_ENGAGED);
                removeCondition(actor, COND_PINNED);
                removeCondition(actor, COND_OUTFLANKED);
                addCondition(actor, COND_DISTANT);
                narrative.append("Desengajamento crítico! O exército recua em formação impecável, limpando ameaças e ficando Distante.");
            } else if ("SUCCESS".equals(degree)) {
                removeCondition(actor, COND_ENGAGED);
                removeCondition(actor, COND_PINNED);
                addCondition(actor, COND_DISTANT);
                narrative.append("Desengajamento bem-sucedido. O exército rompe contato corpo a corpo e recua para Distante.");
            } else if ("FAILURE".equals(degree)) {
                narrative.append("Tentativa de desengajar frustrada; o exército continua engajado.");
            } else { // CRITICAL_FAILURE
                addCondition(actor, COND_OUTFLANKED);
                // Dano de oportunidade
                int hp = Math.max(0, actor.optInt("hp", 4) - 1);
                actor.put("hp", hp);
                if (hp <= 0) addCondition(actor, COND_DEFEATED);
                narrative.append("Falha crítica ao desengajar! O exército sofre 1 de dano de oportunidade e fica Flanqueado.");
            }

            out.put("narrative", narrative.toString());
            out.put("actor", actor);
            if (target != null) out.put("target", target);
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Guardar (Guard): Postura de guarda, concedendo a condição Fortificado.
     */
    public JSONObject guard(JSONObject actor) {
        validateActive(actor);
        addCondition(actor, COND_FORTIFIED);
        JSONObject out = new JSONObject();
        try {
            out.put("action", ACT_GUARD);
            out.put("action_pt", ACTION_LABELS_PT.get(ACT_GUARD));
            out.put("actor", actor);
            out.put("narrative", "O exército estabelece posições de guarda, recebendo a condição Fortificado (+2 CA e CDs de defesa).");
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Reagrupar (Rally): Teste de Moral para recuperar abalo, conter rota ou erguer tropas.
     */
    public JSONObject rally(JSONObject actor, int naturalRoll, int dc) {
        int targetDc = dc > 0 ? dc : 15;
        int mod = effectiveMoraleModifier(actor);
        JSONObject check = pf2.resolveCheck(naturalRoll, mod, targetDc);
        String degree = check.optString("degree");

        JSONObject out = new JSONObject();
        StringBuilder narrative = new StringBuilder();
        try {
            out.put("action", ACT_RALLY);
            out.put("action_pt", ACTION_LABELS_PT.get(ACT_RALLY));
            out.put("check", check);

            int shaken = getShakenValue(actor);
            if ("CRITICAL_SUCCESS".equals(degree)) {
                removeCondition(actor, COND_ROUTED);
                setShaken(actor, Math.max(0, shaken - 2));
                if (hasCondition(actor, COND_DEFEATED) && actor.optInt("hp") <= 0) {
                    actor.put("hp", 1);
                    removeCondition(actor, COND_DEFEATED);
                    narrative.append("Reagrupamento heroico! O exército recupera 1 PV, sai da derrota, dissipa o pânico e reduz o abalo em 2!");
                } else {
                    narrative.append("Reagrupamento magnífico! O pânico foi debelado e o abalo moral foi reduzido em 2.");
                }
            } else if ("SUCCESS".equals(degree)) {
                if (hasCondition(actor, COND_ROUTED)) {
                    removeCondition(actor, COND_ROUTED);
                    setShaken(actor, 2);
                    narrative.append("Reagrupamento bem-sucedido! O exército cessa a rota e estabiliza em Abalado 2.");
                } else {
                    setShaken(actor, Math.max(0, shaken - 1));
                    narrative.append("Reagrupamento bem-sucedido! O abalo moral foi reduzido em 1.");
                }
            } else if ("FAILURE".equals(degree)) {
                narrative.append("O teste de reagrupamento falhou; o estado moral das tropas permanece inalterado.");
            } else { // CRITICAL_FAILURE
                setShaken(actor, Math.min(4, shaken + 1));
                narrative.append("Falha crítica no reagrupamento! O abalo moral piorou em +1.");
            }

            out.put("narrative", narrative.toString());
            out.put("actor", actor);
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Retirar (Retreat): Retirada ordenada do campo de batalha.
     */
    public JSONObject retreat(JSONObject actor, JSONObject target, int naturalRoll) {
        validateActive(actor);
        if (hasCondition(actor, COND_PINNED)) {
            throw new IllegalStateException("Exército Encurralado não pode Retirar; desengaje primeiro.");
        }

        int dc = target != null ? effectiveManeuverDc(target) : 15;
        int mod = effectiveMoraleModifier(actor);
        JSONObject check = pf2.resolveCheck(naturalRoll, mod, dc);
        String degree = check.optString("degree");

        JSONObject out = new JSONObject();
        StringBuilder narrative = new StringBuilder();
        try {
            out.put("action", ACT_RETREAT);
            out.put("action_pt", ACTION_LABELS_PT.get(ACT_RETREAT));
            out.put("check", check);

            if ("CRITICAL_SUCCESS".equals(degree) || "SUCCESS".equals(degree)) {
                removeCondition(actor, COND_ENGAGED);
                addCondition(actor, COND_DISTANT);
                out.put("retreated", true);
                narrative.append("Retirada ordenada executada com sucesso. As forças recuam do campo sem perdas desnecessárias.");
            } else if ("FAILURE".equals(degree)) {
                int hp = Math.max(0, actor.optInt("hp", 4) - 1);
                actor.put("hp", hp);
                if (hp <= 0) addCondition(actor, COND_DEFEATED);
                out.put("retreated", true);
                narrative.append("Retirada acidentada sob pressão inimiga: o exército sofre 1 de dano de fustigamento durante a evasão.");
            } else { // CRITICAL_FAILURE
                addCondition(actor, COND_ROUTED);
                int hp = Math.max(0, actor.optInt("hp", 4) - 1);
                actor.put("hp", hp);
                if (hp <= 0) addCondition(actor, COND_DEFEATED);
                out.put("retreated", false);
                narrative.append("Falha crítica na retirada! As fileiras entram em pânico (Desbaratado), sofrem 1 de dano e não conseguem fugir.");
            }

            out.put("narrative", narrative.toString());
            out.put("actor", actor);
            if (target != null) out.put("target", target);
        } catch (Exception ignored) {}
        return out;
    }

    // --- AÇÕES TÁTICAS DE GUERRA ---

    /**
     * Assalto Total (All-Out Assault): Ataque inconsequente e violento.
     */
    public JSONObject allOutAssault(JSONObject actor, JSONObject target, int naturalRoll,
                                   int attackIndex, boolean agile, int weakness, int resistance) {
        validateActive(actor);
        if (target == null) throw new IllegalArgumentException("Alvo ausente.");
        int map = armyMapPenalty(attackIndex, agile);
        int totalMod = effectiveAttackModifier(actor) + map + 2; // +2 de circunstância
        int targetAc = effectiveAc(target);

        JSONObject check = pf2.resolveCheck(naturalRoll, totalMod, targetAc);
        String degree = check.optString("degree");

        int hits = 0;
        if ("CRITICAL_SUCCESS".equals(degree)) hits = 3;
        else if ("SUCCESS".equals(degree)) hits = 2;

        int rawDmg = Math.max(0, hits) + Math.max(0, weakness);
        int damage = hits > 0 ? Math.max(0, rawDmg - Math.max(0, resistance)) : 0;

        int currentHp = target.optInt("hp", 4);
        int nextHp = Math.max(0, currentHp - damage);
        try { target.put("hp", nextHp); } catch (Exception ignored) {}

        boolean becameDefeated = nextHp <= 0;
        if (becameDefeated) addCondition(target, COND_DEFEATED);

        // Preço tático: o atacante fica Flanqueado
        addCondition(actor, COND_OUTFLANKED);

        JSONObject out = new JSONObject();
        try {
            out.put("tactic", TACTIC_ALL_OUT_ASSAULT);
            out.put("action_pt", ACTION_LABELS_PT.get(TACTIC_ALL_OUT_ASSAULT));
            out.put("check", check);
            out.put("hits", hits);
            out.put("damage", damage);
            out.put("target_hp", nextHp);
            out.put("defeated", becameDefeated);
            out.put("actor", actor);
            out.put("target", target);
            out.put("narrative", "Assalto Total executado! " + hits + " acerto(s) causado(s) com " + damage +
                    " de dano. O exército atacante fica Flanqueado devido à vulnerabilidade aberta.");
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Medicina de Campanha (Battlefield Medicine): Tratamento rápido de baixas.
     */
    public JSONObject battlefieldMedicine(JSONObject actor, int naturalRoll, int dc) {
        validateActive(actor);
        int targetDc = dc > 0 ? dc : 15 + actor.optInt("level", 1) / 2;
        int mod = effectiveManeuverModifier(actor);
        JSONObject check = pf2.resolveCheck(naturalRoll, mod, targetDc);
        String degree = check.optString("degree");

        JSONObject out = new JSONObject();
        StringBuilder narrative = new StringBuilder();
        try {
            out.put("tactic", TACTIC_BATTLEFIELD_MEDICINE);
            out.put("action_pt", ACTION_LABELS_PT.get(TACTIC_BATTLEFIELD_MEDICINE));
            out.put("check", check);

            int hp = actor.optInt("hp", 4);
            int maxHp = actor.optInt("max_hp", 4);

            if ("CRITICAL_SUCCESS".equals(degree)) {
                int healed = Math.min(maxHp, hp + 2);
                actor.put("hp", healed);
                removeCondition(actor, COND_WEARY);
                narrative.append("Medicina de Campanha crítica! Recupera 2 PV (atual: ").append(healed).append(") e remove a condição Exausto.");
            } else if ("SUCCESS".equals(degree)) {
                int healed = Math.min(maxHp, hp + 1);
                actor.put("hp", healed);
                narrative.append("Medicina de Campanha bem-sucedida! Recupera 1 PV (atual: ").append(healed).append(").");
            } else if ("FAILURE".equals(degree)) {
                narrative.append("Os socorristas não conseguiram estabilizar baixas adicionais nesta rodada.");
            } else { // CRITICAL_FAILURE
                int lost = Math.max(0, hp - 1);
                actor.put("hp", lost);
                if (lost <= 0) addCondition(actor, COND_DEFEATED);
                narrative.append("Falha crítica no socorro: infecção ou erro de triagem custa 1 PV adicional!");
            }

            out.put("narrative", narrative.toString());
            out.put("actor", actor);
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Contra-ataque (Counterattack): Golpe de oportunidade reativo.
     */
    public JSONObject counterattack(JSONObject actor, JSONObject target, int naturalRoll, int weakness, int resistance) {
        validateActive(actor);
        if (target == null) throw new IllegalArgumentException("Alvo ausente para contra-ataque.");

        int mod = effectiveAttackModifier(actor) - 2; // -2 de circunstância por reação rápida
        int targetAc = effectiveAc(target);
        JSONObject check = pf2.resolveCheck(naturalRoll, mod, targetAc);
        String degree = check.optString("degree");

        int hits = armyHits(degree);
        int rawDmg = Math.max(0, hits) + Math.max(0, weakness);
        int damage = hits > 0 ? Math.max(0, rawDmg - Math.max(0, resistance)) : 0;

        int currentHp = target.optInt("hp", 4);
        int nextHp = Math.max(0, currentHp - damage);
        try { target.put("hp", nextHp); } catch (Exception ignored) {}

        boolean defeated = nextHp <= 0;
        if (defeated) addCondition(target, COND_DEFEATED);

        if ("CRITICAL_SUCCESS".equals(degree)) {
            setShaken(target, Math.max(1, getShakenValue(target) + 1));
        }

        JSONObject out = new JSONObject();
        try {
            out.put("tactic", TACTIC_COUNTERATTACK);
            out.put("action_pt", ACTION_LABELS_PT.get(TACTIC_COUNTERATTACK));
            out.put("check", check);
            out.put("hits", hits);
            out.put("damage", damage);
            out.put("target_hp", nextHp);
            out.put("defeated", defeated);
            out.put("actor", actor);
            out.put("target", target);
            out.put("narrative", "Contra-ataque reativo! " + hits + " acerto(s) causando " + damage + " de dano ao atacante.");
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Fogo de Cobertura (Covering Fire): Salva de projéteis para fixar o alvo.
     */
    public JSONObject coveringFire(JSONObject actor, JSONObject target, int naturalRoll) {
        validateActive(actor);
        if (target == null) throw new IllegalArgumentException("Alvo ausente.");
        int ammo = actor.optInt("ammunition", 0);
        if (ammo <= 0) throw new IllegalStateException("Sem munição para Fogo de Cobertura.");
        try { actor.put("ammunition", ammo - 1); } catch (Exception ignored) {}

        int mod = effectiveAttackModifier(actor);
        int targetAc = effectiveAc(target);
        JSONObject check = pf2.resolveCheck(naturalRoll, mod, targetAc);
        String degree = check.optString("degree");

        JSONObject out = new JSONObject();
        StringBuilder narrative = new StringBuilder();
        try {
            out.put("tactic", TACTIC_COVERING_FIRE);
            out.put("action_pt", ACTION_LABELS_PT.get(TACTIC_COVERING_FIRE));
            out.put("check", check);

            if ("CRITICAL_SUCCESS".equals(degree)) {
                addCondition(target, COND_PINNED);
                addCondition(target, COND_OUTFLANKED);
                narrative.append("Fogo de Cobertura crítico! O alvo fica Encurralado e Flanqueado sob a saraivada.");
            } else if ("SUCCESS".equals(degree)) {
                addCondition(target, COND_PINNED);
                narrative.append("Fogo de Cobertura bem-sucedido! O alvo fica Encurralado e incapaz de avançar livremente.");
            } else if ("FAILURE".equals(degree)) {
                narrative.append("Fogo de Cobertura ineficaz; o alvo abrigou-se sem perder mobilidade.");
            } else { // CRITICAL_FAILURE
                int extraAmmo = Math.max(0, actor.optInt("ammunition", 0) - 1);
                actor.put("ammunition", extraAmmo);
                narrative.append("Falha crítica no Fogo de Cobertura: desperdício duplo de munição sem conter o inimigo.");
            }

            out.put("narrative", narrative.toString());
            out.put("actor", actor);
            out.put("target", target);
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Postura Defensiva (Defensive Stance): Parede de escudos impenetrável.
     */
    public JSONObject defensiveStance(JSONObject actor) {
        validateActive(actor);
        addCondition(actor, COND_FORTIFIED);
        JSONObject out = new JSONObject();
        try {
            out.put("tactic", TACTIC_DEFENSIVE_STANCE);
            out.put("action_pt", ACTION_LABELS_PT.get(TACTIC_DEFENSIVE_STANCE));
            out.put("actor", actor);
            out.put("narrative", "O exército adota Postura Defensiva: recebe Fortificado (+2 CA e CDs), mas sofre -2 em ataques e não pode Avançar.");
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Combate Sujo (Dirty Fighting): Manobras desleais e terreno traiçoeiro.
     */
    public JSONObject dirtyFighting(JSONObject actor, JSONObject target, int naturalRoll) {
        validateActive(actor);
        if (target == null) throw new IllegalArgumentException("Alvo ausente.");
        int dc = effectiveManeuverDc(target);
        int mod = effectiveManeuverModifier(actor);
        JSONObject check = pf2.resolveCheck(naturalRoll, mod, dc);
        String degree = check.optString("degree");

        JSONObject out = new JSONObject();
        StringBuilder narrative = new StringBuilder();
        try {
            out.put("tactic", TACTIC_DIRTY_FIGHTING);
            out.put("action_pt", ACTION_LABELS_PT.get(TACTIC_DIRTY_FIGHTING));
            out.put("check", check);

            if ("CRITICAL_SUCCESS".equals(degree)) {
                addCondition(target, COND_OUTFLANKED);
                addCondition(target, COND_MIRED);
                narrative.append("Combate Sujo crítico! O inimigo cai na armadilha, ficando Flanqueado e Atolado.");
            } else if ("SUCCESS".equals(degree)) {
                addCondition(target, COND_OUTFLANKED);
                narrative.append("Combate Sujo bem-sucedido! O inimigo tem sua guarda rompida e fica Flanqueado.");
            } else if ("FAILURE".equals(degree)) {
                narrative.append("A artimanha foi antecipada pelo inimigo e não surtiu efeito.");
            } else { // CRITICAL_FAILURE
                addCondition(actor, COND_OUTFLANKED);
                narrative.append("O truque deu errado e colocou as próprias fileiras em desvantagem (Flanqueado).");
            }

            out.put("narrative", narrative.toString());
            out.put("actor", actor);
            out.put("target", target);
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Falsa Retirada (False Retreat): Simulação de fuga para atrair o oponente a uma emboscada.
     */
    public JSONObject falseRetreat(JSONObject actor, JSONObject target, int naturalRoll) {
        validateActive(actor);
        if (target == null) throw new IllegalArgumentException("Alvo ausente.");
        int dc = effectiveMoraleDc(target);
        int mod = effectiveMoraleModifier(actor);
        JSONObject check = pf2.resolveCheck(naturalRoll, mod, dc);
        String degree = check.optString("degree");

        JSONObject out = new JSONObject();
        StringBuilder narrative = new StringBuilder();
        try {
            out.put("tactic", TACTIC_FALSE_RETREAT);
            out.put("action_pt", ACTION_LABELS_PT.get(TACTIC_FALSE_RETREAT));
            out.put("check", check);

            if ("CRITICAL_SUCCESS".equals(degree)) {
                removeCondition(target, COND_FORTIFIED);
                addCondition(target, COND_OUTFLANKED);
                // Dano surpresa gratuito
                int curHp = target.optInt("hp", 4);
                int nextHp = Math.max(0, curHp - 1);
                target.put("hp", nextHp);
                if (nextHp <= 0) addCondition(target, COND_DEFEATED);
                narrative.append("Falsa Retirada magistral! O inimigo perseguiu desordenadamente, perdeu posições defensivas, ficou Flanqueado e tomou 1 de dano.");
            } else if ("SUCCESS".equals(degree)) {
                removeCondition(target, COND_FORTIFIED);
                addCondition(target, COND_OUTFLANKED);
                narrative.append("Falsa Retirada bem-sucedida! O inimigo foi atraído para fora de posição e fica Flanqueado.");
            } else if ("FAILURE".equals(degree)) {
                narrative.append("O inimigo manteve a disciplina e não mordeu a isca.");
            } else { // CRITICAL_FAILURE
                setShaken(actor, Math.max(1, getShakenValue(actor) + 1));
                narrative.append("A farsa transformou-se em pânico real: o exército fica Abalado.");
            }

            out.put("narrative", narrative.toString());
            out.put("actor", actor);
            out.put("target", target);
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Finta (Feint): Manobra diversionista.
     */
    public JSONObject feint(JSONObject actor, JSONObject target, int naturalRoll) {
        validateActive(actor);
        if (target == null) throw new IllegalArgumentException("Alvo ausente.");
        int dc = effectiveManeuverDc(target);
        int mod = effectiveManeuverModifier(actor);
        JSONObject check = pf2.resolveCheck(naturalRoll, mod, dc);
        String degree = check.optString("degree");

        JSONObject out = new JSONObject();
        StringBuilder narrative = new StringBuilder();
        try {
            out.put("tactic", TACTIC_FEINT);
            out.put("action_pt", ACTION_LABELS_PT.get(TACTIC_FEINT));
            out.put("check", check);

            if ("CRITICAL_SUCCESS".equals(degree)) {
                addCondition(target, COND_OUTFLANKED);
                narrative.append("Finta crítica! A linha inimiga abre brecha total, ficando Flanqueada.");
            } else if ("SUCCESS".equals(degree)) {
                addCondition(target, COND_OUTFLANKED);
                narrative.append("Finta bem-sucedida! O alvo fica Flanqueado.");
            } else if ("FAILURE".equals(degree)) {
                narrative.append("A finta não enganou o comando inimigo.");
            } else { // CRITICAL_FAILURE
                narrative.append("Finta desajeitada expôs a formação às armas inimigas.");
            }

            out.put("narrative", narrative.toString());
            out.put("actor", actor);
            out.put("target", target);
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Flanquear (Outflank): Movimento para envelopar as alas inimigas.
     */
    public JSONObject outflank(JSONObject actor, JSONObject target, int naturalRoll) {
        validateActive(actor);
        if (target == null) throw new IllegalArgumentException("Alvo ausente.");
        int dc = effectiveManeuverDc(target);
        int mod = effectiveManeuverModifier(actor);
        JSONObject check = pf2.resolveCheck(naturalRoll, mod, dc);
        String degree = check.optString("degree");

        JSONObject out = new JSONObject();
        StringBuilder narrative = new StringBuilder();
        try {
            out.put("tactic", TACTIC_OUTFLANK);
            out.put("action_pt", ACTION_LABELS_PT.get(TACTIC_OUTFLANK));
            out.put("check", check);

            if ("CRITICAL_SUCCESS".equals(degree)) {
                addCondition(target, COND_OUTFLANKED);
                addCondition(target, COND_PINNED);
                narrative.append("Envelopamento perfeito! O alvo fica Flanqueado e Encurralado.");
            } else if ("SUCCESS".equals(degree)) {
                addCondition(target, COND_OUTFLANKED);
                narrative.append("Manobra de flanco bem-sucedida: o alvo fica Flanqueado (-2 CA).");
            } else if ("FAILURE".equals(degree)) {
                narrative.append("O inimigo girou suas linhas a tempo de bloquear o flanco.");
            } else { // CRITICAL_FAILURE
                addCondition(actor, COND_OUTFLANKED);
                narrative.append("A coluna de flanco foi contra-envelopada e ficou Flanqueada!");
            }

            out.put("narrative", narrative.toString());
            out.put("actor", actor);
            out.put("target", target);
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Bombardeio Avassalador (Overwhelming Bombardment): Artilharia pesada consumindo 2 munições.
     */
    public JSONObject overwhelmingBombardment(JSONObject actor, JSONObject target, int naturalRoll,
                                             int weakness, int resistance) {
        validateActive(actor);
        if (target == null) throw new IllegalArgumentException("Alvo ausente.");
        int ammo = actor.optInt("ammunition", 0);
        if (ammo < 2) throw new IllegalStateException("Munição insuficiente para Bombardeio Avassalador (exige 2).");
        try { actor.put("ammunition", ammo - 2); } catch (Exception ignored) {}

        int mod = effectiveAttackModifier(actor) + 1; // +1 de circunstância pelo volume maciço
        int targetAc = effectiveAc(target);
        JSONObject check = pf2.resolveCheck(naturalRoll, mod, targetAc);
        String degree = check.optString("degree");

        int hits = armyHits(degree);
        int rawDmg = Math.max(0, hits) + Math.max(0, weakness);
        int damage = hits > 0 ? Math.max(0, rawDmg - Math.max(0, resistance)) : 0;

        int curHp = target.optInt("hp", 4);
        int nextHp = Math.max(0, curHp - damage);
        try { target.put("hp", nextHp); } catch (Exception ignored) {}

        boolean defeated = nextHp <= 0;
        if (defeated) addCondition(target, COND_DEFEATED);

        if ("CRITICAL_SUCCESS".equals(degree)) {
            setShaken(target, Math.max(2, getShakenValue(target) + 2));
        } else if ("SUCCESS".equals(degree)) {
            setShaken(target, Math.max(1, getShakenValue(target) + 1));
        }

        JSONObject out = new JSONObject();
        try {
            out.put("tactic", TACTIC_OVERWHELMING_BOMBARDMENT);
            out.put("action_pt", ACTION_LABELS_PT.get(TACTIC_OVERWHELMING_BOMBARDMENT));
            out.put("check", check);
            out.put("hits", hits);
            out.put("damage", damage);
            out.put("target_hp", nextHp);
            out.put("defeated", defeated);
            out.put("actor", actor);
            out.put("target", target);
            out.put("narrative", "Bombardeio Avassalador! 2 munições gastas, " + hits + " acerto(s), " +
                    damage + " de dano e abalo moral severo infligido às tropas inimigas.");
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Provocar (Taunt): Guerra psicológica para desmoralizar e forçar o inimigo a reagir.
     */
    public JSONObject taunt(JSONObject actor, JSONObject target, int naturalRoll) {
        validateActive(actor);
        if (target == null) throw new IllegalArgumentException("Alvo ausente.");
        int dc = effectiveMoraleDc(target);
        int mod = effectiveMoraleModifier(actor);
        JSONObject check = pf2.resolveCheck(naturalRoll, mod, dc);
        String degree = check.optString("degree");

        JSONObject out = new JSONObject();
        StringBuilder narrative = new StringBuilder();
        try {
            out.put("tactic", TACTIC_TAUNT);
            out.put("action_pt", ACTION_LABELS_PT.get(TACTIC_TAUNT));
            out.put("check", check);

            if ("CRITICAL_SUCCESS".equals(degree)) {
                setShaken(target, Math.max(1, getShakenValue(target) + 1));
                removeCondition(target, COND_FORTIFIED);
                narrative.append("Provocação crítica! O exército inimigo perde a calma, abandona posições defensivas e fica Abalado.");
            } else if ("SUCCESS".equals(degree)) {
                setShaken(target, Math.max(1, getShakenValue(target) + 1));
                narrative.append("Provocação bem-sucedida! O moral inimigo foi abalado (+1 Abalado).");
            } else if ("FAILURE".equals(degree)) {
                narrative.append("A provocação foi ignorada pelos soldados inimigos.");
            } else { // CRITICAL_FAILURE
                setShaken(actor, Math.max(1, getShakenValue(actor) + 1));
                narrative.append("A provocação foi ridicularizada e constrangeu as próprias tropas (Abalado).");
            }

            out.put("narrative", narrative.toString());
            out.put("actor", actor);
            out.put("target", target);
        } catch (Exception ignored) {}
        return out;
    }

    // --- DERROTA, DESTRUIÇÃO, ROTAS E RECUPERAÇÃO ---

    /**
     * Regra oficial Kingmaker: Quando um exército que já está Derrotado sofre dano adicional,
     * realiza um teste puro CD 15 (flat check). Se falhar, é DESTRUÍDO.
     */
    public JSONObject resolveDamageAgainstDefeated(JSONObject attacker, JSONObject target, int attackRoll, int flatCheckRoll) {
        if (!hasCondition(target, COND_DEFEATED)) {
            throw new IllegalStateException("O exército-alvo não está Derrotado.");
        }
        int flat = flatCheckRoll > 0 ? flatCheckRoll : attackRoll;
        boolean destroyed = flat < 15;

        JSONObject out = new JSONObject();
        try {
            out.put("action", "damage_defeated");
            out.put("action_pt", "Ataque a Exército Derrotado");
            out.put("flat_check", flat);
            out.put("flat_check_dc", 15);
            out.put("destroyed", destroyed);

            if (destroyed) {
                removeCondition(target, COND_DEFEATED);
                addCondition(target, COND_DESTROYED);
                out.put("narrative", "O exército derrotado recebeu novo ataque e FALHOU no teste puro CD 15 (rolagem " +
                        flat + "). Foi completamente DESTRUÍDO!");
            } else {
                out.put("narrative", "O exército derrotado sobreviveu ao teste puro CD 15 de aniquilação (rolagem " +
                        flat + "), permanecendo Derrotado mas intacto.");
            }
            out.put("actor", attacker);
            out.put("target", target);
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Teste explícito de Destruição (Flat Check CD 15).
     */
    public JSONObject checkArmyDestruction(JSONObject army, int naturalFlatCheck) {
        if (army == null) throw new IllegalArgumentException("Exército ausente.");
        int flat = Math.max(1, Math.min(20, naturalFlatCheck));
        boolean destroyed = flat < 15;
        JSONObject out = new JSONObject();
        try {
            out.put("flat_check", flat);
            out.put("dc", 15);
            out.put("destroyed", destroyed);
            if (destroyed) {
                removeCondition(army, COND_DEFEATED);
                addCondition(army, COND_DESTROYED);
                out.put("narrative", "Teste de destruição falhou (" + flat + " < 15). O exército foi DESTRUÍDO.");
            } else {
                out.put("narrative", "Teste de destruição superado (" + flat + " >= 15). O exército permanece Derrotado, mas recuperável.");
            }
            out.put("army", army);
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Recuperação Pós-Batalha (Post-Battle Recovery).
     */
    public JSONObject postBattleRecovery(JSONObject army, int naturalRoll, int dc) {
        if (army == null) throw new IllegalArgumentException("Exército ausente.");
        if (hasCondition(army, COND_DESTROYED)) {
            throw new IllegalStateException("Exército Destruído não pode ser recuperado; foi aniquilado.");
        }

        int targetDc = dc > 0 ? dc : 15 + army.optInt("level", 1) / 2;
        int mod = effectiveMoraleModifier(army);
        JSONObject check = pf2.resolveCheck(naturalRoll, mod, targetDc);
        String degree = check.optString("degree");

        JSONObject out = new JSONObject();
        StringBuilder narrative = new StringBuilder();
        try {
            out.put("action", "post_battle_recovery");
            out.put("action_pt", "Recuperação Pós-Batalha");
            out.put("check", check);

            int maxHp = army.optInt("max_hp", 4);
            boolean wasDefeated = hasCondition(army, COND_DEFEATED);

            if ("CRITICAL_SUCCESS".equals(degree)) {
                army.put("hp", Math.max(1, maxHp / 2));
                removeCondition(army, COND_DEFEATED);
                removeCondition(army, COND_ROUTED);
                removeCondition(army, COND_PINNED);
                removeCondition(army, COND_OUTFLANKED);
                removeCondition(army, COND_ENGAGED);
                removeCondition(army, COND_DISTANT);
                removeCondition(army, COND_MIRED);
                setShaken(army, 0);
                narrative.append("Recuperação pós-batalha magistral! O exército recupera 50% dos PV (").append(army.optInt("hp"))
                        .append("/").append(maxHp).append(") e dissipa todas as desordens de combate.");
            } else if ("SUCCESS".equals(degree)) {
                army.put("hp", Math.max(1, Math.max(1, maxHp / 4)));
                removeCondition(army, COND_DEFEATED);
                removeCondition(army, COND_ROUTED);
                removeCondition(army, COND_PINNED);
                removeCondition(army, COND_OUTFLANKED);
                removeCondition(army, COND_ENGAGED);
                removeCondition(army, COND_DISTANT);
                setShaken(army, 1);
                narrative.append("Recuperação bem-sucedida. O exército recupera forças mínimas (").append(army.optInt("hp"))
                        .append(" PV), sai do estado de Derrota e estabiliza em Abalado 1.");
            } else if ("FAILURE".equals(degree)) {
                narrative.append("A recuperação pós-batalha não surtiu efeito; as tropas continuam desarticuladas.");
            } else { // CRITICAL_FAILURE
                if (wasDefeated) {
                    removeCondition(army, COND_DEFEATED);
                    addCondition(army, COND_DESTROYED);
                    narrative.append("Catástrofe na recuperação! Complicações pós-combate e deserção massiva resultaram na DESTRUIÇÃO total do exército.");
                } else {
                    addCondition(army, COND_WEARY);
                    setShaken(army, Math.min(4, getShakenValue(army) + 1));
                    narrative.append("Falha crítica na recuperação: soldados exaustos e desmoralizados.");
                }
            }

            out.put("narrative", narrative.toString());
            out.put("army", army);
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Cálculo de Baixas e Severidade de Batalha (Casualties).
     */
    public JSONObject calculateCasualties(JSONObject army) {
        if (army == null) return new JSONObject();
        int maxHp = Math.max(1, army.optInt("max_hp", 4));
        int hp = Math.max(0, army.optInt("hp", 0));
        int lost = maxHp - hp;
        int pct = (lost * 100) / maxHp;

        String severity;
        if (hasCondition(army, COND_DESTROYED)) {
            severity = "Aniquilação Total";
            pct = 100;
        } else if (hasCondition(army, COND_DEFEATED) || hp <= 0) {
            severity = "Derrota / Baixas Críticas";
        } else if (pct >= 50) {
            severity = "Baixas Severas";
        } else if (pct >= 25) {
            severity = "Baixas Moderadas";
        } else {
            severity = "Baixas Leves";
        }

        JSONObject out = new JSONObject();
        try {
            out.put("hp", hp);
            out.put("max_hp", maxHp);
            out.put("lost_hp", lost);
            out.put("casualty_percent", pct);
            out.put("severity", severity);
            out.put("defeated", hasCondition(army, COND_DEFEATED));
            out.put("destroyed", hasCondition(army, COND_DESTROYED));
            out.put("status_pt", severity + " (" + pct + "% de perdas operacionais)");
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Reforço e Reabastecimento (Reinforcement):
     * Restaura PV, munição, suprimentos e consome RP do Reino.
     */
    public JSONObject reinforceArmy(JSONObject army, int hpToRestore, int ammunition, int supplies, int rpCost) {
        if (army == null) throw new IllegalArgumentException("Exército ausente.");
        if (hasCondition(army, COND_DESTROYED)) {
            throw new IllegalStateException("Exército Destruído não pode ser reforçado; é necessário recrutar um novo exército.");
        }

        int maxHp = army.optInt("max_hp", 4);
        int curHp = army.optInt("hp", 0);
        int nextHp = Math.min(maxHp, curHp + Math.max(0, hpToRestore));
        int nextAmmo = Math.max(0, army.optInt("ammunition", 0) + ammunition);
        int nextSupplies = Math.max(0, army.optInt("supplies", 0) + supplies);

        try {
            army.put("hp", nextHp);
            army.put("ammunition", nextAmmo);
            army.put("supplies", nextSupplies);

            if (nextHp > 0) {
                removeCondition(army, COND_DEFEATED);
                removeCondition(army, COND_ROUTED);
            }
            removeCondition(army, COND_WEARY);
            removeCondition(army, COND_MIRED);
            removeCondition(army, COND_LOST);
            removeCondition(army, COND_OUTFLANKED);
            removeCondition(army, COND_PINNED);
            setShaken(army, 0);
        } catch (Exception ignored) {}

        JSONObject out = new JSONObject();
        try {
            out.put("action", "reinforce");
            out.put("action_pt", "Reforço e Reabastecimento");
            out.put("rp_cost", Math.max(0, rpCost));
            out.put("hp_restored", nextHp - curHp);
            out.put("hp", nextHp);
            out.put("max_hp", maxHp);
            out.put("ammunition", nextAmmo);
            out.put("supplies", nextSupplies);
            out.put("army", army);
            out.put("narrative", "Exército reforçado com sucesso: PV " + nextHp + "/" + maxHp +
                    ", munição " + nextAmmo + ", suprimentos " + nextSupplies + ".");
        } catch (Exception ignored) {}
        return out;
    }

    // --- DISPATCHER GENÉRICO DE AÇÕES DE GUERRA ---

    public JSONObject resolveWarAction(String actionName, JSONObject actor, JSONObject target,
                                       int naturalRoll, JSONObject options) {
        String act = normKey(actionName);
        JSONObject opts = options != null ? options : new JSONObject();
        int attackIndex = opts.optInt("attack_index", 1);
        boolean agile = opts.optBoolean("agile", false);
        int weakness = opts.optInt("weakness", 0);
        int resistance = opts.optInt("resistance", 0);
        int dc = opts.optInt("dc", 0);
        int flatCheck = opts.optInt("flat_check", 0);

        if (act.contains("advance") || act.contains("avancar")) {
            return advance(actor, target, naturalRoll);
        }
        if (act.contains("battle_ranged") || act.contains("ranged") || act.contains("distancia") || act.contains("tiro")) {
            return battle(actor, target, true, naturalRoll, attackIndex, agile, weakness, resistance, flatCheck);
        }
        if (act.contains("battle") || act.contains("strike") || act.contains("ataque") || act.contains("melee") || act.contains("corpo")) {
            return battle(actor, target, false, naturalRoll, attackIndex, agile, weakness, resistance, flatCheck);
        }
        if (act.contains("disengage") || act.contains("desengajar")) {
            return disengage(actor, target, naturalRoll);
        }
        if (act.contains("guard") || act.contains("guardar") || act.contains("proteger")) {
            return guard(actor);
        }
        if (act.contains("rally") || act.contains("reagrupar")) {
            return rally(actor, naturalRoll, dc);
        }
        if (act.contains("retreat") || act.contains("retirar")) {
            return retreat(actor, target, naturalRoll);
        }
        if (act.contains("all_out") || act.contains("assalto_total")) {
            return allOutAssault(actor, target, naturalRoll, attackIndex, agile, weakness, resistance);
        }
        if (act.contains("medicine") || act.contains("medicina")) {
            return battlefieldMedicine(actor, naturalRoll, dc);
        }
        if (act.contains("counterattack") || act.contains("contra_ataque")) {
            return counterattack(actor, target, naturalRoll, weakness, resistance);
        }
        if (act.contains("covering") || act.contains("fogo_de_cobertura")) {
            return coveringFire(actor, target, naturalRoll);
        }
        if (act.contains("defensive_stance") || act.contains("postura_defensiva")) {
            return defensiveStance(actor);
        }
        if (act.contains("dirty") || act.contains("combate_sujo")) {
            return dirtyFighting(actor, target, naturalRoll);
        }
        if (act.contains("false_retreat") || act.contains("falsa_retirada")) {
            return falseRetreat(actor, target, naturalRoll);
        }
        if (act.contains("feint") || act.contains("finta")) {
            return feint(actor, target, naturalRoll);
        }
        if (act.contains("outflank") || act.contains("flanquear")) {
            return outflank(actor, target, naturalRoll);
        }
        if (act.contains("overwhelming") || act.contains("bombardeio")) {
            return overwhelmingBombardment(actor, target, naturalRoll, weakness, resistance);
        }
        if (act.contains("taunt") || act.contains("provocar")) {
            return taunt(actor, target, naturalRoll);
        }
        if (act.contains("post_battle") || act.contains("recuperacao")) {
            return postBattleRecovery(actor, naturalRoll, dc);
        }
        if (act.contains("destruction") || act.contains("destruicao")) {
            return checkArmyDestruction(actor, naturalRoll);
        }
        throw new IllegalArgumentException("Ação de guerra desconhecida: " + actionName);
    }

    // --- CATÁLOGO DE CONDIÇÕES E AÇÕES ---

    public static String conditionLabel(String condition) {
        String k = normKey(condition);
        if (k.startsWith("shaken")) {
            int val = 1;
            if (k.contains(":") || k.contains("_")) {
                try { val = Integer.parseInt(k.split("[:_]")[1]); } catch (Exception ignored) {}
            }
            return "Abalado " + val;
        }
        String label = CONDITION_LABELS_PT.get(k);
        return label != null ? label : condition;
    }

    public static String conditionDescription(String condition) {
        String k = normKey(condition);
        if (k.startsWith("shaken")) return CONDITION_DESCRIPTIONS_PT.get(COND_SHAKEN);
        String desc = CONDITION_DESCRIPTIONS_PT.get(k);
        return desc != null ? desc : "";
    }

    public static String actionLabel(String action) {
        String k = normKey(action);
        String label = ACTION_LABELS_PT.get(k);
        return label != null ? label : action;
    }

    public static String actionDescription(String action) {
        String k = normKey(action);
        String desc = ACTION_DESCRIPTIONS_PT.get(k);
        return desc != null ? desc : "";
    }

    public JSONObject catalog() {
        JSONObject out = new JSONObject();
        try {
            JSONArray conditions = new JSONArray();
            for (String c : ALL_CONDITIONS) {
                conditions.put(new JSONObject()
                        .put("key", c)
                        .put("label_pt", conditionLabel(c))
                        .put("description_pt", conditionDescription(c)));
            }
            out.put("conditions", conditions);

            JSONArray basics = new JSONArray();
            for (String a : new String[]{ACT_ADVANCE, ACT_BATTLE_MELEE, ACT_BATTLE_RANGED, ACT_DISENGAGE, ACT_GUARD, ACT_RALLY, ACT_RETREAT}) {
                basics.put(new JSONObject()
                        .put("key", a)
                        .put("label_pt", actionLabel(a))
                        .put("description_pt", actionDescription(a)));
            }
            out.put("basic_actions", basics);

            JSONArray tactics = new JSONArray();
            for (String t : ALL_TACTICS) {
                tactics.put(new JSONObject()
                        .put("key", t)
                        .put("label_pt", actionLabel(t))
                        .put("description_pt", actionDescription(t)));
            }
            out.put("tactical_actions", tactics);
        } catch (Exception ignored) {}
        return out;
    }

    // --- MÉTODOS INTERNOS E VALIDAÇÃO ---

    private void validateActive(JSONObject army) {
        if (army == null) throw new IllegalArgumentException("Exército nulo.");
        if (hasCondition(army, COND_DESTROYED)) {
            throw new IllegalStateException("O exército foi Destruído e não pode agir.");
        }
        if (hasCondition(army, COND_DEFEATED) || army.optInt("hp", 0) <= 0) {
            throw new IllegalStateException("O exército está Derrotado e não pode agir até ser recuperado.");
        }
    }

    private static JSONArray jsonArray(Object value) {
        if (value instanceof JSONArray) return (JSONArray) value;
        if (value instanceof String) {
            String s = ((String) value).trim();
            if (s.startsWith("[")) {
                try { return new JSONArray(s); } catch (Exception ignored) {}
            }
        }
        return new JSONArray();
    }

    private static String normKey(String val) {
        if (val == null) return "";
        return Normalizer.normalize(val.toLowerCase(Locale.US), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
