package com.braseiro.dnd5e;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class V1CorePlayableFlowRobolectricTest {
    private Context context;
    private String dbName;
    private AppDb db;
    private JSONObject registrySnapshot;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        dbName = "v1_flow_" + Long.toHexString(System.nanoTime() & 0xFFFFFFFL) + ".db";
        db = new AppDb(context, dbName);
        db.setActiveMapProfile("dnd");
        registrySnapshot = CompiledStatblockEngine.snapshot();
        Xmm2025CompiledBestiaryEngine.install();
    }

    @After
    public void tearDown() {
        if (db != null) db.close();
        if (context != null && dbName != null) context.deleteDatabase(dbName);
        if (registrySnapshot != null) CompiledStatblockEngine.restoreReplacing(registrySnapshot);
    }

    private void reopenDb() {
        db.close();
        db = new AppDb(context, dbName);
        db.setActiveMapProfile("dnd");
    }

    private void placeToken(long combatantId, double x, double y) {
        ContentValues values = new ContentValues();
        values.put("x", x);
        values.put("y", y);
        assertEquals(1, db.getWritableDatabase().update(
                "tactical_tokens",
                values,
                "world_profile=? AND combatant_id=?",
                new String[]{"dnd", String.valueOf(combatantId)}));
    }

    @Test
    public void campaignCreationPreserves2014LegacyAndDefaultsNewCampaignTo2024() throws Exception {
        // Default new campaign is 5.5e revisada (2024/2025)
        assertTrue(new DndRulesEngine().is2024(db.getCampaign().optString("rules_profile")));

        // Legacy 2014 save remains intact across reopen and Backup21
        db.saveCampaign(
                "Crônicas de Costa da Espada 2014",
                "5e 2014",
                "Heroico",
                false,
                true,
                false,
                "Proteger Phandalin");
        assertEquals("5e 2014", db.getCampaign().optString("rules_profile"));

        JSONObject legacyBackup = db.exportAll();
        BackupContractValidator.requireValid(legacyBackup);

        reopenDb();
        assertEquals("5e 2014", db.getCampaign().optString("rules_profile"));
        db.importAll(legacyBackup);
        assertEquals("5e 2014", db.getCampaign().optString("rules_profile"));
        assertEquals("Crônicas de Costa da Espada 2014", db.getCampaign().optString("name"));
    }

    @Test
    public void fullPlayableJourneyAcrossCampaignPartyExplorationCombatProgressionThreeReopensAndTwoBackupImports() throws Exception {
        // 1. Campaign in 5e 2024 (PHB 2024 + DMG 2024 + MM 2025)
        db.saveCampaign(
                "V1 Fronteira Cinzenta",
                "5e 2024",
                "Heroico Tático",
                true,
                true,
                false,
                "Investigar o Forte Quebrado");
        assertTrue(new DndRulesEngine().is2024(db.getCampaign().optString("rules_profile")));
        assertEquals("V1 Fronteira Cinzenta", db.getCampaign().optString("name"));

        // 2. Create 2 distinct characters (Fighter Zeren + Cleric Aeliana)
        long fighterId = db.addCharacter(
                "Zeren", "Humano", "Guerreiro", 1, 12, 12, 16,
                16, 14, 14, 10, 12, 8, true, "Veterano da fronteira");
        assertTrue(fighterId > 0);
        db.updateCharacterRuleDetails(
                fighterId,
                "Soldado",
                "Armadura marcada",
                "Atletismo, Percepção",
                "Espada Longa, Escudo, Cota de Malha",
                "Retomar o Fôlego",
                DndRulesEngine.PROFILE_2024);
        JSONObject longsword = new JSONObject()
                .put("id", "wpn-longsword")
                .put("name", "Espada Longa")
                .put("equipped", true)
                .put("quantity", 1)
                .put("weapon_attack_bonus", 5)
                .put("weapon_damage_formula", "1d8+3")
                .put("weapon_damage_type", "cortante");
        JSONObject fighterSheet = db.ensureCharacterResourceState(fighterId);
        fighterSheet.put("inventory_ledger", new JSONArray().put(longsword));
        db.updateCharacterSheet(fighterId, fighterSheet.toString());
        assertTrue(db.configureEquippedWeaponAttack(
                fighterId, "wpn-longsword", 5, "1d8+3", "cortante", "melee", 5, 0, 0));

        long clericId = db.addCharacter(
                "Aeliana", "Anão", "Clérigo", 1, 10, 10, 18,
                14, 10, 14, 10, 16, 12, true, "Guardiã do relicário");
        assertTrue(clericId > fighterId);
        db.updateCharacterRuleDetails(
                clericId,
                "Acólito",
                "Símbolo solar",
                "Religião, Intuição",
                "Maça, Escudo Sagrado, Símbolo Sagrado",
                "Bênção, Palavra Curativa",
                DndRulesEngine.PROFILE_2024);
        db.ensureCharacterResourceState(clericId);

        // 3. Include & exclude companion via PartyDirector (verify Zeren is founder even though Aeliana is alphabetically first)
        PartyDirector director = new PartyDirector(db);
        long companionMembershipId = director.recruitHero(clericId, "companion");
        assertTrue(companionMembershipId > 0);

        List<JSONObject> activeRoster = director.activeHeroRoster();
        assertEquals(2, activeRoster.size());
        assertEquals(0, director.availableHeroesToRecruit().length());

        // Exclude companion temporarily and re-include
        assertTrue(director.releaseHero(clericId, "Acampamento Base", "Vigiando a entrada"));
        assertEquals(1, director.activeHeroRoster().size());
        assertEquals(fighterId, director.activeHeroRoster().get(0).optLong("id"));
        assertEquals(1, director.availableHeroesToRecruit().length());

        assertTrue(director.reactivateHero(clericId));
        assertEquals(2, director.activeHeroRoster().size());

        // 4. Manual roll persisted in dice_history
        long rollId = db.addDiceHistoryBound(
                "1d20+5",
                "Percepção na entrada da ruína",
                18,
                "manual",
                new JSONObject()
                        .put("actor_character_id", fighterId)
                        .put("actor_name", "Zeren")
                        .put("natural_roll", 13)
                        .put("modifier", 5)
                        .put("rolls_json", "[13]")
                        .put("render_mode", "manual_user"));
        assertTrue(rollId > 0);
        assertEquals(1, db.listDiceHistory().size());

        // 5. Exploration state + world clock + quest + WorldStateEngine tick
        db.saveSceneState(new JSONObject()
                .put("location", "Pátio do Forte Quebrado")
                .put("time_of_day", "Manhã")
                .put("weather", "Neblina fria")
                .put("danger", "Médio")
                .put("objective", "Localizar a patrulha desaparecida")
                .put("day_number", 2)
                .put("clock_hour", 9.0));
        long clockId = db.addWorldClock("Alarme Goblin", 4, "Reforços chegam do subterrâneo", true);
        assertTrue(clockId > 0);
        db.setWorldClockValue(clockId, 1);
        long questId = db.addQuest("Patrulha Perdida", "Vasculhar o pátio interno", "100 PO", fighterId);
        assertTrue(questId > 0);

        WorldStateEngine world = new WorldStateEngine(db);
        JSONObject exploreTick = world.preview("Explorar o pátio interno por 1 hora");
        assertTrue(world.commit(exploreTick));
        assertTrue(db.getSceneState().optDouble("clock_hour") >= 10.0);
        double clockHourBeforeCombat = db.getSceneState().optDouble("clock_hour");

        // 6. Start encounter with XMM 2025 monster, execute player attack + monster attack + saving throw + advance 2 turns
        long cFighter = db.addCombatantWithVisual("Zeren", 18, 12, 12, 16, "", true, "character", fighterId, "");
        long cCleric = db.addCombatantWithVisual("Aeliana", 14, 10, 10, 18, "", true, "character", clericId, "");
        JSONObject spawn = db.addCompiledStatblockCombatant("Goblin Warrior", "Goblin Warrior", 10, "ui-monster-spawn-v1-001");
        assertTrue(spawn.toString(), spawn.optBoolean("statblock_spawn_committed", false));
        long cGoblin = spawn.optLong("combatant_id");
        assertTrue(cFighter > 0 && cCleric > 0 && cGoblin > 0);
        assertEquals(1, db.getCombatState().optInt("running"));

        // Clear obstacles and place combatants adjacent on tactical board
        JSONObject tacticalScene = new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
        ContentValues clearObstacles = new ContentValues();
        clearObstacles.put("obstacles_json", "[]");
        db.getWritableDatabase().update(
                "tactical_scenes",
                clearObstacles,
                "world_profile=? AND scene_key=?",
                new String[]{"dnd", tacticalScene.optString("scene_key")});
        db.ensureTacticalTokensForCombat();
        placeToken(cFighter, 2.5 / 12d, 2.5 / 8d);
        placeToken(cGoblin, 3.5 / 12d, 2.5 / 8d);
        placeToken(cCleric, 2.5 / 12d, 3.5 / 8d);

        // Turn 1 is Zeren (highest initiative 18)
        assertEquals(1, db.getCombatant(cFighter).optInt("active_turn"));
        long partyActionId = db.addPartyAction(fighterId, "Zeren", "Atacar Goblin Warrior com Espada Longa", "combate");
        assertEquals(1, db.markPartyActionsNarrated(new JSONArray().put(partyActionId)));
        JSONObject geom = db.previewTacticalWeaponAttack(cFighter, cGoblin, "wpn-longsword");
        assertTrue(geom.toString(), geom.optBoolean("attack_geometry_valid", false));
        int goblinHpBefore = db.getCombatant(cGoblin).optInt("hp");
        JSONObject playerAttack = db.applySingleWeaponAttackForPartyAction(
                partyActionId,
                cFighter,
                cGoblin,
                16,
                5,
                4,
                "cortante",
                "ui-weapon2-v1-flow-001",
                "wpn-longsword",
                "1d8+3",
                geom.optInt("effective_ac"),
                geom.optString("attack_geometry_fingerprint"),
                "Normal");
        assertTrue(playerAttack.toString(), playerAttack.optBoolean("party_action_resolved", false));
        int goblinHpAfterHit = db.getCombatant(cGoblin).optInt("hp");
        assertEquals(goblinHpBefore - 4, goblinHpAfterHit);

        // Advance Turn 1 -> Turn 2 (Aeliana)
        JSONObject turn2 = db.nextTurn();
        assertEquals(cCleric, turn2.optLong("id"));
        // Resolve a saving throw effect during Aeliana's turn
        JSONObject saveInfo = db.combatantAreaSaveInfo(cGoblin, "dex");
        int goblinDexBonus = saveInfo.optInt("bonus", 2);
        JSONArray saves = new JSONArray().put(new JSONObject()
                .put("target_id", cGoblin)
                .put("save_ability", "dex")
                .put("dc", 13)
                .put("d20", 14)
                .put("save_bonus", goblinDexBonus)
                .put("manual_bonus_confirmed", true)
                .put("raw_damage", 4)
                .put("on_success", "none")
                .put("damage_type", "radiante")
                .put("expected_hp", goblinHpAfterHit)
                .put("expected_temp_hp", 0));
        JSONObject saveRes = db.applyCombatSaveDamageWithAction(cCleric, "action", saves, "ui-save-v1-flow-001");
        assertTrue(saveRes.toString(), saveRes.optBoolean("area_save_committed", false));

        // Advance Turn 2 -> Turn 3 (Goblin Warrior XMM)
        JSONObject turn3 = db.nextTurn();
        assertEquals(cGoblin, turn3.optLong("id"));

        // Monster XMM attack against Zeren
        JSONObject monsterAttack = db.applyCompiledStatblockAttack(
                cGoblin,
                cFighter,
                "Goblin Warrior",
                "Scimitar",
                16,
                0,
                5,
                0,
                0L,
                -1,
                -1,
                "ui-monster-v1-flow-001");
        assertTrue(monsterAttack.toString(), monsterAttack.optBoolean("statblock_attack_committed", false));
        assertEquals(7, db.getCombatant(cFighter).optInt("hp"));
        assertEquals(7, db.getCharacter(fighterId).optInt("hp"));

        // Verify mid-combat persistence across 3 SQLite reopens and 2 repeated Backup21 imports
        JSONObject midCombatBackup = db.exportAll();
        BackupContractValidator.requireValid(midCombatBackup);

        for (int i = 0; i < 3; i++) {
            reopenDb();
            assertEquals("V1 Fronteira Cinzenta", db.getCampaign().optString("name"));
            assertTrue(new DndRulesEngine().is2024(db.getCampaign().optString("rules_profile")));
            assertEquals(1, db.getCombatState().optInt("running"));
            assertEquals(7, db.getCharacter(fighterId).optInt("hp"));
            assertEquals(goblinHpAfterHit, db.getCombatant(cGoblin).optInt("hp"));
        }

        // Import the same Backup21 twice in a row: no action duplication, no phantom healing, no lost state
        db.importAll(midCombatBackup);
        db.importAll(midCombatBackup);

        assertEquals("V1 Fronteira Cinzenta", db.getCampaign().optString("name"));
        assertEquals(1, db.getCombatState().optInt("running"));
        JSONObject restoredFighterChar = db.findCharacterByName("Zeren");
        JSONObject restoredClericChar = db.findCharacterByName("Aeliana");
        assertEquals(7, restoredFighterChar.optInt("hp"));
        assertTrue(restoredFighterChar.optString("equipment").contains("Espada Longa"));
        assertTrue(restoredClericChar.optString("equipment").contains("Símbolo Sagrado"));
        assertEquals(clockHourBeforeCombat, db.getSceneState().optDouble("clock_hour"), 0.01);
        assertEquals(1, db.listWorldClocks(true, 10).get(0).optInt("current_value"));

        PartyDirector restoredDirector = new PartyDirector(db);
        assertEquals(2, restoredDirector.activeHeroRoster().size());

        // Find restored combatants
        long restoredFighterCombatantId = 0;
        long restoredGoblinCombatantId = 0;
        for (JSONObject c : db.listCombatants()) {
            if ("Zeren".equals(c.optString("name"))) restoredFighterCombatantId = c.optLong("id");
            if (c.optInt("is_player", 0) == 0) restoredGoblinCombatantId = c.optLong("id");
        }
        assertTrue(restoredFighterCombatantId > 0 && restoredGoblinCombatantId > 0);
        assertEquals(7, db.getCombatant(restoredFighterCombatantId).optInt("hp"));
        assertEquals(goblinHpAfterHit, db.getCombatant(restoredGoblinCombatantId).optInt("hp"));

        // Replay of the exact same monster attack token after Backup21 restore must be blocked/replayed without dealing extra damage
        JSONObject replayAttempt = db.applyCompiledStatblockAttack(
                restoredGoblinCombatantId,
                restoredFighterCombatantId,
                "Goblin Warrior",
                "Scimitar",
                16,
                0,
                5,
                0,
                0L,
                -1,
                -1,
                "ui-monster-v1-flow-001");
        assertEquals(7, db.getCombatant(restoredFighterCombatantId).optInt("hp"));
        assertFalse(replayAttempt.optBoolean("statblock_attack_committed", false));
        assertEquals("already_resolved_or_replayed", replayAttempt.optString("statblock_attack_blocked"));

        // 7. End combat encounter and return to exploration
        db.resetCombat();
        assertEquals(0, db.getCombatState().optInt("running", 0));
        assertTrue(db.listCombatants().isEmpty());
        assertEquals(7, db.findCharacterByName("Zeren").optInt("hp"));

        // 8. Short rest -> XP & Level Up progression
        long zerenId = db.findCharacterByName("Zeren").optLong("id");
        JSONObject zerenSheet = db.ensureCharacterResourceState(zerenId);
        zerenSheet.put("short_rest_hit_dice_choice_pending", true);
        db.updateCharacterSheet(zerenId, zerenSheet.toString());
        JSONObject shortRest = db.applyCharacterShortRestHitDice(zerenId, 1, 5);
        assertEquals(12, shortRest.optInt("hp"));
        assertEquals(12, db.getCharacter(zerenId).optInt("hp"));

        JSONObject xpRes = db.addCharacterXp(zerenId, 300);
        assertEquals(300, xpRes.optInt("xp"));

        JSONObject levelUpRes = db.applyCharacterLevelUpFixedHp(zerenId);
        assertTrue(levelUpRes.toString(), levelUpRes.optBoolean("level_up_applied", false));
        assertEquals(2, db.getCharacter(zerenId).optInt("level"));
        assertTrue(db.getCharacter(zerenId).optInt("max_hp") > 12);

        // Final 3 SQLite reopens + 2 Backup21 imports after progression
        JSONObject postFlowBackup = db.exportAll();
        BackupContractValidator.requireValid(postFlowBackup);
        for (int i = 0; i < 3; i++) {
            reopenDb();
            assertEquals(2, db.findCharacterByName("Zeren").optInt("level"));
            assertEquals(300, new JSONObject(db.findCharacterByName("Zeren").optString("sheet_json")).optInt("xp"));
        }
        db.importAll(postFlowBackup);
        db.importAll(postFlowBackup);
        assertEquals(2, db.findCharacterByName("Zeren").optInt("level"));
        assertEquals(300, new JSONObject(db.findCharacterByName("Zeren").optString("sheet_json")).optInt("xp"));
        assertEquals(2, new PartyDirector(db).activeHeroRoster().size());
    }
}
