package com.braseiro.dnd5e;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Suite de Regressão Automatizada RUN247 MOD005 para Defeitos P0.1, P0.2 e P0.3.
 * Execução com Android SQLite e Robolectric Runner.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class Run247P0RegressionRobolectricTest {

    @Test
    public void testP01_ForcedMovementAndTeleportCoordinatesMutation() throws Exception {
        StatblockTurnEngine.reset();
        String attackerBlock = "run247-p01-attacker-" + System.nanoTime();
        String targetBlock = "run247-p01-target-" + System.nanoTime();

        JSONObject basisAttacker = new JSONObject().put("source_type", "compiled_statblock")
                .put("content_kind", "statblock").put("content_name", attackerBlock)
                .put("source_book", "RUN247 P0 Test").put("source_locator", "test-p01-attacker");
        JSONObject basisTarget = new JSONObject().put("source_type", "compiled_statblock")
                .put("content_kind", "statblock").put("content_name", targetBlock)
                .put("source_book", "RUN247 P0 Test").put("source_locator", "test-p01-target");

        JSONArray actionsAttacker = new JSONArray()
                .put(new JSONObject().put("name", "Gale Push").put("category", "legendary_action")
                        .put("legendary_cost", 1).put("effect_on_fail", "push:10ft"))
                .put(new JSONObject().put("name", "Gravitational Pull").put("category", "legendary_action")
                        .put("legendary_cost", 1).put("effect_on_fail", "pull:10ft"))
                .put(new JSONObject().put("name", "Misty Step").put("category", "legendary_action")
                        .put("legendary_cost", 1).put("effect_on_fail", "teleport:15ft"));

        CompiledStatblockEngine.registerAuthorized(new JSONObject().put("id", attackerBlock)
                .put("source_book", "RUN247 P0 Test").put("creature_type", "monstrosity")
                .put("armor_class", 16).put("hit_points", 120).put("actions", actionsAttacker)
                .put("mechanical_basis", basisAttacker));

        CompiledStatblockEngine.registerAuthorized(new JSONObject().put("id", targetBlock)
                .put("source_book", "RUN247 P0 Test").put("creature_type", "humanoid")
                .put("armor_class", 14).put("hit_points", 80).put("actions", new JSONArray())
                .put("source_actionless", true).put("mechanical_basis", basisTarget));

        String atkToken = "atk-p01";
        String tgtToken = "tgt-p01";

        StatblockEncounterStateEngine.spawn(atkToken, attackerBlock, 2, 2, 0);
        StatblockEncounterStateEngine.spawn(tgtToken, targetBlock, 4, 2, 0);

        assertEquals(2, StatblockEncounterStateEngine.getPosition(atkToken).optInt("x"));
        assertEquals(2, StatblockEncounterStateEngine.getPosition(atkToken).optInt("y"));
        assertEquals(4, StatblockEncounterStateEngine.getPosition(tgtToken).optInt("x"));
        assertEquals(2, StatblockEncounterStateEngine.getPosition(tgtToken).optInt("y"));

        // Inicializa iniciativa: tgtToken tem 20 (turno atual), atkToken tem 10 (fora do seu turno pode usar ação lendária)
        StatblockTurnEngine.setInitiative(tgtToken, 20);
        StatblockTurnEngine.setInitiative(atkToken, 10);
        JSONObject startState = StatblockTurnEngine.start();
        assertTrue(startState.optBoolean("started"));

        // 1. PUSH: O alvo está em (4, 2) e o atacante em (2, 2). Vetor dx = +2 -> direção +1 no eixo X.
        // 10ft = 2 quadrados. Alvo deve ir para (4 + 2 = 6, 2).
        JSONObject pushRes = StatblockCombatFlowEngine.legendaryAction(atkToken, tgtToken, "Gale Push", 10);
        assertFalse(pushRes.toString(), pushRes.optBoolean("blocked", false));
        assertTrue(pushRes.optBoolean("legendary_action"));
        assertTrue(pushRes.optBoolean("forced_movement_applied"));
        assertEquals(4, pushRes.optInt("origin_x"));
        assertEquals(6, pushRes.optInt("dest_x"));
        assertEquals(6, StatblockEncounterStateEngine.getPosition(tgtToken).optInt("x"));
        assertEquals(2, StatblockEncounterStateEngine.getPosition(tgtToken).optInt("y"));

        // 2. PULL: O alvo agora está em (6, 2) e o atacante em (2, 2). Pull:10ft (2 quadrados).
        // Alvo deve recuar em direção ao atacante: (6 - 2 = 4, 2).
        JSONObject pullRes = StatblockCombatFlowEngine.legendaryAction(atkToken, tgtToken, "Gravitational Pull", 10);
        assertFalse(pullRes.toString(), pullRes.optBoolean("blocked", false));
        assertTrue(pullRes.optBoolean("forced_movement_applied"));
        assertEquals(6, pullRes.optInt("origin_x"));
        assertEquals(4, pullRes.optInt("dest_x"));
        assertEquals(4, StatblockEncounterStateEngine.getPosition(tgtToken).optInt("x"));
        assertEquals(2, StatblockEncounterStateEngine.getPosition(tgtToken).optInt("y"));

        // 3. TELEPORT: Atacante está em (2, 2). Teleport 15ft (3 quadrados).
        JSONObject teleRes = StatblockCombatFlowEngine.legendaryAction(atkToken, tgtToken, "Misty Step", 10);
        assertFalse(teleRes.toString(), teleRes.optBoolean("blocked", false));
        assertTrue(teleRes.optBoolean("teleport_applied"));
        assertEquals(2, teleRes.optInt("origin_x"));
        assertNotEquals(2, teleRes.optInt("dest_x"));
        assertEquals(teleRes.optInt("dest_x"), StatblockEncounterStateEngine.getPosition(atkToken).optInt("x"));

        // 4. Limites do tabuleiro: mover alvo para a borda (99, 99) e testar bloqueio
        StatblockTurnEngine.next(); // avança rodada para renovar recursos lendários
        StatblockTurnEngine.next(); // volta para o turno de tgtToken
        StatblockEncounterStateEngine.setPosition(tgtToken, 99, 99, 0);
        StatblockEncounterStateEngine.setPosition(atkToken, 98, 99, 0);
        JSONObject edgePush = StatblockCombatFlowEngine.legendaryAction(atkToken, tgtToken, "Gale Push", 10);
        assertFalse(edgePush.toString(), edgePush.optBoolean("blocked", false));
        assertEquals("destination_blocked_or_boundary", edgePush.optString("forced_movement_blocked"));
        assertEquals(99, StatblockEncounterStateEngine.getPosition(tgtToken).optInt("x"));
    }

    @Test
    public void testP02_LegendarySpellsPreflightRejectionAndRealSaveBonus() throws Exception {
        StatblockTurnEngine.reset();
        String mageBlock = "run247-p02-mage-" + System.nanoTime();
        String knightBlock = "run247-p02-knight-" + System.nanoTime();

        JSONObject basisMage = new JSONObject().put("source_type", "compiled_statblock")
                .put("content_kind", "statblock").put("content_name", mageBlock)
                .put("source_book", "RUN247 P0 Test").put("source_locator", "test-p02-mage");
        JSONObject basisKnight = new JSONObject().put("source_type", "compiled_statblock")
                .put("content_kind", "statblock").put("content_name", knightBlock)
                .put("source_book", "RUN247 P0 Test").put("source_locator", "test-p02-knight");

        JSONArray actionsMage = new JSONArray()
                .put(new JSONObject().put("name", "Unknown Spell").put("category", "legendary_action")
                        .put("legendary_cost", 1).put("spells", new JSONArray().put("Wish")))
                .put(new JSONObject().put("name", "Shatter Action").put("category", "legendary_action")
                        .put("legendary_cost", 1).put("save_dc", 14).put("spells", new JSONArray().put("Shatter")))
                .put(new JSONObject().put("name", "Vanish").put("category", "legendary_action")
                        .put("legendary_cost", 1).put("spells", new JSONArray().put("Invisibility")));

        // Cavaleiro com CON alta (+3) e proficiência em CON (+5)
        JSONObject knightAbilities = new JSONObject().put("str", 16).put("dex", 12).put("con", 16).put("int", 10).put("wis", 12).put("cha", 14);
        JSONObject knightSaves = new JSONObject().put("con", "+5").put("wis", "+3");

        CompiledStatblockEngine.registerAuthorized(new JSONObject().put("id", mageBlock)
                .put("source_book", "RUN247 P0 Test").put("creature_type", "humanoid")
                .put("armor_class", 15).put("hit_points", 90).put("actions", actionsMage)
                .put("mechanical_basis", basisMage));

        CompiledStatblockEngine.registerAuthorized(new JSONObject().put("id", knightBlock)
                .put("source_book", "RUN247 P0 Test").put("creature_type", "humanoid")
                .put("armor_class", 18).put("hit_points", 100).put("actions", new JSONArray())
                .put("abilities", knightAbilities).put("saving_throws", knightSaves)
                .put("source_actionless", true).put("mechanical_basis", basisKnight));

        String mageToken = "mage-p02";
        String knightToken = "knight-p02";

        StatblockEncounterStateEngine.spawn(mageToken, mageBlock);
        StatblockEncounterStateEngine.spawn(knightToken, knightBlock);
        StatblockTurnEngine.setInitiative(knightToken, 20);
        StatblockTurnEngine.setInitiative(mageToken, 10);
        StatblockTurnEngine.start();

        // 1. Magia desconhecida DEVE ser bloqueada no preflight SEM consumir pontos lendários!
        JSONObject badSpell = StatblockCombatFlowEngine.legendaryAction(mageToken, knightToken, "Unknown Spell", 15);
        assertTrue(badSpell.optBoolean("blocked"));
        assertEquals("unsupported_legendary_spell", badSpell.optString("reason"));
        // Pontos lendários continuam intactos: 3 pontos
        assertEquals(3, StatblockTurnEngine.state().optJSONArray("order").optJSONObject(1)
                .optJSONObject("economy").optInt("legendary_points_remaining"));

        // 2. Magia Shatter: CD 14. D20 rolado: 10.
        // O alvo tem bônus de CON +5. Total = 10 + 5 = 15 >= 14 (SUCESSO!).
        // Sem o bônus de salvaguarda, 10 < 14 seria falha.
        JSONObject shatter = StatblockCombatFlowEngine.legendaryAction(mageToken, knightToken, "Shatter Action", 10);
        assertFalse(shatter.toString(), shatter.optBoolean("blocked", false));
        assertEquals("Shatter", shatter.optString("spell_executed"));
        assertEquals(5, shatter.optInt("save_bonus"));
        assertEquals(15, shatter.optInt("save_roll"));
        assertTrue("Deveria passar com o bônus de +5 em CON", shatter.optBoolean("save_success"));
        // Como passou na salvaguarda de Shatter, toma metade do dano
        assertTrue(shatter.optInt("applied_damage") > 0);

        // 3. Magia Invisibility: aplica concentração durável e condição invisível
        JSONObject vanish = StatblockCombatFlowEngine.legendaryAction(mageToken, knightToken, "Vanish", 10);
        assertFalse(vanish.toString(), vanish.optBoolean("blocked", false));
        assertEquals("invisible", vanish.optString("applied_condition"));
        JSONObject mageState = StatblockEncounterStateEngine.view(mageToken);
        assertTrue(mageState.optJSONArray("conditions").toString().contains("invisible"));
        assertEquals("Invisibility", mageState.optJSONObject("effects").optString("concentration"));
    }

    @Test
    public void testP03_MasterOrchestratorDurablePersistence() throws Exception {
        StatblockTurnEngine.reset();
        Context context = RuntimeEnvironment.getApplication();
        String dbName = "p03_legendary_" + System.nanoTime() + ".db";
        AppDb db = new AppDb(context, dbName);
        try {
            db.setActiveMapProfile("dnd");
            MasterOrchestrator orch = new MasterOrchestrator(db, new PartyDirector(db), new WorldStateEngine(db), new AiQuotaGovernor(context));

            String dragonBlock = "run247-p03-dragon-" + System.nanoTime();
            JSONObject basisDragon = new JSONObject().put("source_type", "compiled_statblock")
                    .put("content_kind", "statblock").put("content_name", dragonBlock)
                    .put("source_book", "RUN247 P0 Test").put("source_locator", "test-p03-dragon");

            JSONArray actionsDragon = new JSONArray()
                    .put(new JSONObject().put("name", "Tail Swipe").put("category", "legendary_action")
                            .put("legendary_cost", 1).put("to_hit", 10).put("damage", "2d6 + 4")
                            .put("damage_type", "bludgeoning"));

            CompiledStatblockEngine.registerAuthorized(new JSONObject().put("id", dragonBlock)
                    .put("source_book", "RUN247 P0 Test").put("creature_type", "dragon")
                    .put("armor_class", 18).put("hit_points", 200).put("actions", actionsDragon)
                    .put("mechanical_basis", basisDragon));

            String heroBlock = "run247-p03-hero-" + System.nanoTime();
            JSONObject basisHero = new JSONObject().put("source_type", "compiled_statblock")
                    .put("content_kind", "statblock").put("content_name", heroBlock)
                    .put("source_book", "RUN247 P0 Test").put("source_locator", "test-p03-hero");
            CompiledStatblockEngine.registerAuthorized(new JSONObject().put("id", heroBlock)
                    .put("source_book", "RUN247 P0 Test").put("creature_type", "humanoid")
                    .put("armor_class", 15).put("hit_points", 50).put("actions", new JSONArray())
                    .put("source_actionless", true).put("mechanical_basis", basisHero));

            long heroId = db.addCombatant("Hero", 15, 50, 50, 15, "", true);

            String dragonToken = "dragon-p03";
            String heroToken = "Hero";

            StatblockEncounterStateEngine.spawn(dragonToken, dragonBlock);
            StatblockEncounterStateEngine.spawn(heroToken, heroBlock);
            StatblockTurnEngine.setInitiative(heroToken, 20);
            StatblockTurnEngine.setInitiative(dragonToken, 10);
            StatblockTurnEngine.start();

            // Executa a ação lendária através do MasterOrchestrator conectado ao AppDb
            JSONObject result = orch.resolveStatblockLegendaryAction(dragonToken, heroToken, "Tail Swipe", 15);
            assertFalse(result.toString(), result.optBoolean("blocked", false));
            assertTrue(result.optBoolean("db_persisted"));
            assertTrue(result.has("durable_receipt_key"));

            String receiptKey = result.optString("durable_receipt_key");
            assertTrue(db.hasEffectReceipt(receiptKey));

            // Verifica se o dano foi debitado do combatente persistido no SQLite
            JSONObject updatedHero = db.getCombatant(heroId);
            assertTrue("O HP do herói deve ter sido reduzido", updatedHero.optInt("hp") < 50);
        } finally {
            db.close();
            context.deleteDatabase(dbName);
        }
    }
}
