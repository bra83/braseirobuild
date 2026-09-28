package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Harness de validação semântica e determinística do KingmakerWarfareEngine.
 * Executa em JVM pura (sem Android framework) com org.json.
 */
public final class KingmakerWarfareEngineHarness {

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("FALHA DE VALIDAÇÃO: " + message);
        }
    }

    private static JSONObject makeArmy(String key, String name, int level, int maxHp, int ac) {
        JSONObject a = new JSONObject();
        try {
            a.put("army_key", key);
            a.put("name", name);
            a.put("level", level);
            a.put("max_hp", maxHp);
            a.put("hp", maxHp);
            a.put("ac", ac);
            a.put("attack_modifier", level + 3);
            a.put("morale_modifier", level + 1);
            a.put("morale_dc", 10 + level + 1);
            a.put("maneuver_modifier", level + 1);
            a.put("maneuver_dc", 10 + level + 1);
            a.put("scouting", level);
            a.put("ammunition", 5);
            a.put("supplies", 5);
            a.put("consumption", 1);
            a.put("rout_threshold", maxHp / 2);
            a.put("conditions_json", "[]");
            a.put("tactics_json", "[]");
        } catch (Exception ignored) {}
        return a;
    }

    public static void main(String[] args) throws Exception {
        KingmakerWarfareEngine engine = new KingmakerWarfareEngine();
        int checks = 0;

        System.out.println("Iniciando testes do KingmakerWarfareEngine...");

        // 1. Normalização e Estatísticas Completas
        JSONObject raw = makeArmy("army_1", "Primeira Legião de Restov", 3, 12, 18);
        JSONObject normalized = engine.normalizeArmy(raw);
        require(normalized.optInt("level") == 3, "level incorreto");
        require(normalized.optInt("hp") == 12, "hp incorreto");
        require(normalized.optInt("max_hp") == 12, "max_hp incorreto");
        require(normalized.optInt("ac") == 18, "ac incorreto");
        require(normalized.optInt("attack_modifier") == 6, "attack_modifier");
        require(normalized.optInt("morale_modifier") == 4, "morale_modifier");
        require(normalized.optInt("morale_dc") == 14, "morale_dc");
        require(normalized.optInt("maneuver_modifier") == 4, "maneuver_modifier");
        require(normalized.optInt("maneuver_dc") == 14, "maneuver_dc");
        require(normalized.optInt("scouting") == 3, "scouting");
        require(normalized.optInt("ammunition") == 5, "ammunition");
        require(normalized.optInt("supplies") == 5, "supplies");
        require(normalized.optInt("consumption") == 1, "consumption");
        require(normalized.optInt("rout_threshold") == 6, "rout_threshold");
        checks += 14;

        // 2. Condições: todas as 14 condições suportadas
        String[] all14 = KingmakerWarfareEngine.ALL_CONDITIONS;
        require(all14.length == 14, "Deve conter exatamente 14 condições");
        for (String cond : all14) {
            String label = KingmakerWarfareEngine.conditionLabel(cond);
            String desc = KingmakerWarfareEngine.conditionDescription(cond);
            require(label != null && !label.isEmpty(), "Label pt-BR ausente para " + cond);
            require(desc != null && !desc.isEmpty(), "Descrição pt-BR ausente para " + cond);
            checks += 2;
        }

        // Teste de aplicação e remoção de condições
        JSONObject testCondArmy = makeArmy("test_c", "Tropa de Teste", 1, 10, 15);
        engine.addCondition(testCondArmy, KingmakerWarfareEngine.COND_FORTIFIED);
        require(engine.hasCondition(testCondArmy, KingmakerWarfareEngine.COND_FORTIFIED), "Fortified não aplicado");
        require(engine.effectiveAc(testCondArmy) == 17, "Fortified deve conceder +2 CA");
        engine.addCondition(testCondArmy, KingmakerWarfareEngine.COND_OUTFLANKED);
        require(engine.effectiveAc(testCondArmy) == 15, "Fortified (+2) e Outflanked (-2) devem se anular");

        // Teste de Abalado (Shaken) progressivo
        engine.setShaken(testCondArmy, 2);
        require(engine.getShakenValue(testCondArmy) == 2, "Shaken deve ser 2");
        require(engine.effectiveAc(testCondArmy) == 13, "Shaken 2 deve reduzir CA em 2");
        require(engine.effectiveAttackModifier(testCondArmy) == 2, "Attack mod deve reduzir em 2");

        // Limiar de Rota pelo Abalo: Shaken 4 vira Desbaratado (Routed)!
        engine.setShaken(testCondArmy, 4);
        require(engine.hasCondition(testCondArmy, KingmakerWarfareEngine.COND_ROUTED), "Shaken 4 deve aplicar Routed");
        checks += 7;

        // 3. Ações Básicas: Advance
        JSONObject attacker = makeArmy("atk", "Vanguardeiros de Varn", 2, 8, 16);
        JSONObject defender = makeArmy("def", "Bando Troll", 2, 10, 15);
        engine.addCondition(attacker, KingmakerWarfareEngine.COND_DISTANT);
        engine.addCondition(defender, KingmakerWarfareEngine.COND_DISTANT);

        JSONObject advResult = engine.advance(attacker, defender, 20); // Sucesso crítico
        require("advance".equals(advResult.optString("action")), "Ação advance incorreta");
        require(!engine.hasCondition(attacker, KingmakerWarfareEngine.COND_DISTANT), "Advance deve remover Distant");
        require(engine.hasCondition(attacker, KingmakerWarfareEngine.COND_ENGAGED), "Advance deve aplicar Engaged");
        require(engine.hasCondition(defender, KingmakerWarfareEngine.COND_OUTFLANKED), "Crit deve aplicar Outflanked");
        checks += 4;

        // 4. Ações Básicas: Battle Melee com MAP
        JSONObject strike1 = engine.battle(attacker, defender, false, 15, 1, false, 0, 0, 10);
        require(strike1.optInt("hits") >= 1, "Ataque com d20=15 deve acertar CA 13 (15 - 2 outflanked)");
        require(defender.optInt("hp") < 10, "Defensor deve ter sofrido dano");

        JSONObject strike2Map = engine.battle(attacker, defender, false, 15, 2, false, 0, 0, 10);
        JSONObject check2 = strike2Map.optJSONObject("check");
        require(check2.optInt("modifier") == attacker.optInt("attack_modifier") - 5, "2º ataque deve ter MAP -5");
        checks += 3;

        // 5. Ações Básicas: Battle Ranged (consome munição)
        JSONObject archers = makeArmy("arc", "Arqueiros de Oleg", 2, 8, 15);
        archers.put("ammunition", 3);
        JSONObject rangedStrike = engine.battle(archers, defender, true, 16, 1, false, 0, 0, 10);
        require(archers.optInt("ammunition") == 2, "Disparo à distância deve consumir 1 munição");
        require(rangedStrike.optInt("hits") >= 1, "Disparo deve acertar");

        // Alvo Oculto (Concealed) - teste puro CD 5
        engine.addCondition(defender, KingmakerWarfareEngine.COND_CONCEALED);
        JSONObject concealedMiss = engine.battle(archers, defender, true, 18, 1, false, 0, 0, 3); // flat check 3 < 5
        require(concealedMiss.optInt("hits") == 0, "Flat check 3 < 5 em alvo oculto deve errar");
        require(concealedMiss.optJSONObject("check").optBoolean("concealed_miss"), "Flag concealed_miss ausente");
        engine.removeCondition(defender, KingmakerWarfareEngine.COND_CONCEALED);
        checks += 4;

        // 6. Ações Básicas: Guard, Rally, Disengage, Retreat
        JSONObject guardRes = engine.guard(archers);
        require(engine.hasCondition(archers, KingmakerWarfareEngine.COND_FORTIFIED), "Guard deve conceder Fortified");

        engine.setShaken(archers, 2);
        JSONObject rallyRes = engine.rally(archers, 20, 10); // Sucesso crítico
        require(engine.getShakenValue(archers) == 0, "Rally crítico deve reduzir Shaken em 2");

        JSONObject disResult = engine.disengage(attacker, defender, 18);
        require(engine.hasCondition(attacker, KingmakerWarfareEngine.COND_DISTANT), "Disengage crítico deve deixar Distant");
        require(!engine.hasCondition(attacker, KingmakerWarfareEngine.COND_ENGAGED), "Disengage deve remover Engaged");

        JSONObject retResult = engine.retreat(attacker, defender, 15);
        require(retResult.optBoolean("retreated"), "Retreat com sucesso deve evacuar o exército");
        checks += 6;

        // 7. Ações Táticas (11 Táticas)
        JSONObject tacticalArmy = makeArmy("tac_a", "Guarda de Restov", 3, 12, 17);
        JSONObject enemyArmy = makeArmy("tac_b", "Hoste Bárbara", 3, 12, 16);

        // All-Out Assault
        JSONObject aoa = engine.allOutAssault(tacticalArmy, enemyArmy, 16, 1, false, 0, 0);
        require(aoa.optInt("hits") >= 2, "All-out assault deve conceder +1 hit no sucesso");
        require(engine.hasCondition(tacticalArmy, KingmakerWarfareEngine.COND_OUTFLANKED), "All-out assault deve aplicar Outflanked ao atacante");

        // Battlefield Medicine
        tacticalArmy.put("hp", 8);
        engine.addCondition(tacticalArmy, KingmakerWarfareEngine.COND_WEARY);
        JSONObject med = engine.battlefieldMedicine(tacticalArmy, 19, 10); // Crítico
        require(tacticalArmy.optInt("hp") == 10, "Medicina crítica cura 2 PV");
        require(!engine.hasCondition(tacticalArmy, KingmakerWarfareEngine.COND_WEARY), "Medicina crítica limpa Weary");

        // Counterattack
        JSONObject ca = engine.counterattack(tacticalArmy, enemyArmy, 18, 0, 0);
        require(ca.optInt("hits") >= 1, "Contra-ataque com roll alto deve acertar");

        // Covering Fire
        tacticalArmy.put("ammunition", 4);
        JSONObject cf = engine.coveringFire(tacticalArmy, enemyArmy, 17);
        require(engine.hasCondition(enemyArmy, KingmakerWarfareEngine.COND_PINNED), "Covering fire deve aplicar Pinned");

        // Defensive Stance
        JSONObject ds = engine.defensiveStance(tacticalArmy);
        require(engine.hasCondition(tacticalArmy, KingmakerWarfareEngine.COND_FORTIFIED), "Defensive stance aplica Fortified");

        // Dirty Fighting
        JSONObject df = engine.dirtyFighting(tacticalArmy, enemyArmy, 19);
        require(engine.hasCondition(enemyArmy, KingmakerWarfareEngine.COND_OUTFLANKED), "Dirty fighting aplica Outflanked");

        // False Retreat
        JSONObject fr = engine.falseRetreat(tacticalArmy, enemyArmy, 20);
        require(fr.optJSONObject("check").optString("degree").equals("CRITICAL_SUCCESS"), "False retreat crítico");

        // Feint
        JSONObject feint = engine.feint(tacticalArmy, enemyArmy, 18);
        require(engine.hasCondition(enemyArmy, KingmakerWarfareEngine.COND_OUTFLANKED), "Feint aplica Outflanked");

        // Outflank
        JSONObject of = engine.outflank(tacticalArmy, enemyArmy, 18);
        require(engine.hasCondition(enemyArmy, KingmakerWarfareEngine.COND_OUTFLANKED), "Outflank aplica Outflanked");

        // Overwhelming Bombardment
        tacticalArmy.put("ammunition", 5);
        JSONObject ob = engine.overwhelmingBombardment(tacticalArmy, enemyArmy, 17, 0, 0);
        require(tacticalArmy.optInt("ammunition") == 3, "Overwhelming bombardment consome 2 munições");
        require(engine.getShakenValue(enemyArmy) >= 1, "Bombardeio aplica Abalado");

        // Taunt
        JSONObject taunt = engine.taunt(tacticalArmy, enemyArmy, 16);
        require(taunt.optJSONObject("check") != null, "Taunt deve resolver teste de Moral");
        checks += 14;

        // 8. Derrota e Destruição
        JSONObject doomedArmy = makeArmy("doom", "Cultistas de Gyronna", 1, 4, 14);
        doomedArmy.put("hp", 0);
        doomedArmy = engine.normalizeArmy(doomedArmy);
        require(engine.hasCondition(doomedArmy, KingmakerWarfareEngine.COND_DEFEATED), "HP 0 deve aplicar Defeated");

        // Dano em exército Derrotado -> Flat Check CD 15 para Destruição
        JSONObject destroyCheckFail = engine.checkArmyDestruction(doomedArmy, 10); // 10 < 15 -> Destruído!
        require(destroyCheckFail.optBoolean("destroyed"), "Flat check 10 < 15 deve Destruir");
        require(engine.hasCondition(doomedArmy, KingmakerWarfareEngine.COND_DESTROYED), "Condição Destroyed aplicada");

        JSONObject survivorArmy = makeArmy("surv", "Mercenários Fluviais", 1, 4, 14);
        survivorArmy.put("hp", 0);
        engine.addCondition(survivorArmy, KingmakerWarfareEngine.COND_DEFEATED);
        JSONObject destroyCheckPass = engine.checkArmyDestruction(survivorArmy, 16); // 16 >= 15 -> Sobrevive!
        require(!destroyCheckPass.optBoolean("destroyed"), "Flat check 16 >= 15 não deve Destruir");
        require(engine.hasCondition(survivorArmy, KingmakerWarfareEngine.COND_DEFEATED), "Permanece Derrotado");
        checks += 5;

        // 9. Recuperação Pós-Batalha, Baixas e Reforço
        JSONObject postRec = engine.postBattleRecovery(survivorArmy, 18, 10); // Sucesso crítico
        require(survivorArmy.optInt("hp") == 2, "Recuperação crítica deve restaurar metade do PV");
        require(!engine.hasCondition(survivorArmy, KingmakerWarfareEngine.COND_DEFEATED), "Recuperação remove Defeated");

        JSONObject casualties = engine.calculateCasualties(survivorArmy);
        require(casualties.optInt("casualty_percent") == 50, "50% de baixas");
        require(casualties.optString("severity").contains("Severas"), "Severidade Severas");

        JSONObject reinforced = engine.reinforceArmy(survivorArmy, 2, 5, 5, 5);
        require(survivorArmy.optInt("hp") == 4, "Reforço restaura PV máximo");
        require(reinforced.optInt("rp_cost") == 5, "Custo de RP 5");
        checks += 6;

        // 10. Catálogo e Termos em Português
        JSONObject cat = engine.catalog();
        require(cat.optJSONArray("conditions").length() == 14, "Catálogo deve conter 14 condições");
        require(cat.optJSONArray("basic_actions").length() == 7, "Catálogo deve conter ações básicas");
        require(cat.optJSONArray("tactical_actions").length() == 11, "Catálogo deve conter 11 táticas");
        checks += 3;

        System.out.println("KINGMAKER_WARFARE_ENGINE_PASS checks=" + checks);
    }
}
