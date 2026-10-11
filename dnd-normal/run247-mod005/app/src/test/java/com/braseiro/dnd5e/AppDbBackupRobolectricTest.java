package com.braseiro.dnd5e;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/** Exercises AppDb through Android's SQLite API under Robolectric. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class AppDbBackupRobolectricTest {
    @Test public void backup21RoundTripReopensAndPreservesPortableSections() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String dbName = "r221_roundtrip_" + UUID.randomUUID().toString().substring(0,8) + ".db";
        AppDb db = newSandbox(dbName);
        try {
            db.getWritableDatabase();
            db.saveCampaign("Mesa de round-trip", "5e 2014", "Fantasia", true, true, false, "preservar");
            long hero = addHero(db, "Lia Teste");
            assertTrue(hero > 0);
            db.addJournal("Marco", "A ponte foi aberta", "story");
            db.saveSceneState(new JSONObject().put("day_number", 2).put("clock_hour", 11)
                .put("location", "Ponte antiga").put("weather", "neblina"));
            db.addWorldClock("Patrulha da ponte", 4, "", true);
            db.upsertFaction("Guarda da ponte", "Manter a travessia", "suprimentos", 5, "", "Ponte");
            db.upsertNpcMemory(new JSONObject().put("name", "Ivo Vigia").put("active", 1)
                .put("current_location", "Ponte").put("summary", "Vigia local"));
            db.upsertNpcCognitiveState(new JSONObject().put("name", "Ivo Vigia").put("trust", 20)
                .put("current_location", "Ponte").put("simulation_cursor_hours", 35)
                .put("simulation_cursor_day", 2));

            JSONObject backup = db.exportAll();
            assertEquals("", Backup21IntegrityEnvelope.check(backup));
            assertTrue("export omitiu seção portátil", Backup21RequiredSections.missing(21,
                new Backup21RequiredSections.Presence() {
                    public boolean object(String key) { return backup.optJSONObject(key) != null; }
                    public boolean array(String key) { return backup.optJSONArray(key) != null; }
                }).isEmpty());
            assertEquals(21, backup.optInt("version"));

            db.saveCampaign("Estado posterior", "5e 2014", "Sombria", false, false, true, "descartar");
            addHero(db, "Personagem posterior");
            db.importAll(backup);
            assertEquals("Mesa de round-trip", db.getCampaign().optString("name"));
            assertEquals(1, db.listCharacters().size());
            assertEquals("Lia Teste", db.listCharacters().get(0).optString("name"));
            assertEquals(1, db.listJournal().size());
            assertEquals(1, db.listWorldClocks(true, 10).size());
            assertEquals(1, db.listFactions(10).size());
            assertEquals("Ivo Vigia", db.getNpcCognitiveState("Ivo Vigia").optString("npc_name"));
            JSONObject roundTrip = BackupRoundTripVerifier.compareSnapshots(backup, db.exportAll());
            assertTrue("round-trip verifier must preserve portable sections: " + roundTrip,
                roundTrip.optBoolean("ok"));

            db.close();
            db = newSandbox(dbName);
            assertEquals("Mesa de round-trip", db.getCampaign().optString("name"));
            assertEquals("Lia Teste", db.listCharacters().get(0).optString("name"));
            assertEquals("", Backup21IntegrityEnvelope.check(db.exportAll()));
        } finally {
            try { if (db != null) db.close(); } finally { context.deleteDatabase(dbName); }
        }
    }

    @Test public void deferredCommitFailureRollsBackSqliteAndCompiledBundle() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String dbName = "r221_fail_" + UUID.randomUUID().toString().substring(0,8) + ".db";
        AppDb db = newSandbox(dbName);
        JSONObject compiledPrior = null;
        try {
            db.getWritableDatabase();
            db.saveCampaign("Antes do import", "5e 2014", "Fantasia", true, true, false, "");
            addHero(db, "Sobrevivente");
            db.addAuditEvent("teste", "marcador", "fixture", "{}", "{}", false);
            registerFixtureStatblock();
            compiledPrior = CompiledContentPersistenceBundle.snapshot();

            JSONObject importCandidate = db.exportAll();
            JSONObject compiled = importCandidate.getJSONObject(CompiledContentBackupBridge.BACKUP_KEY);
            compiled.put("statblocks", new JSONObject().put("schema", "compiled_statblocks_v1")
                .put("records", new JSONArray()).put("count", 0));
            importCandidate.put("campaign", new JSONObject(importCandidate.getJSONObject("campaign").toString())
                .put("name", "Estado que não deve ser confirmado"));
            Backup21IntegrityEnvelope.seal(importCandidate);

            SQLiteDatabase sql = db.getWritableDatabase();
            sql.execSQL("CREATE TABLE run221_deferred_parent(id INTEGER PRIMARY KEY)");
            sql.execSQL("CREATE TABLE run221_deferred_child(parent_id INTEGER, FOREIGN KEY(parent_id) REFERENCES run221_deferred_parent(id) DEFERRABLE INITIALLY DEFERRED)");
            sql.execSQL("CREATE TRIGGER run221_fail_commit AFTER INSERT ON audit_log WHEN NEW.title='Restauração auditada' BEGIN INSERT INTO run221_deferred_child(parent_id) VALUES(7331); END");

            boolean failed = false;
            try { db.importAll(importCandidate); }
            catch (Exception expected) { failed = true; }
            assertTrue("deferred foreign key did not fail transaction commit", failed);
            assertEquals("Antes do import", db.getCampaign().optString("name"));
            assertEquals("Sobrevivente", db.listCharacters().get(0).optString("name"));
            assertTrue("compiled registry was not restored after commit failure",
                CompiledStatblockEngine.has("run221-backup-rollback-fixture"));
            assertEquals(compiledPrior.toString(), CompiledContentPersistenceBundle.snapshot().toString());
        } finally {
            try {
                if (db != null) db.close();
            } finally {
                try {
                    if (compiledPrior != null) CompiledContentPersistenceBundle.restore(compiledPrior);
                } finally {
                    context.deleteDatabase(dbName);
                }
            }
        }
    }

    @Test public void importingFullAuditWindowDoesNotDropOldestPortableEvent() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String dbName = "r221_audit_" + UUID.randomUUID().toString().substring(0,8) + ".db";
        AppDb db = newSandbox(dbName);
        try {
            SQLiteDatabase sql = db.getWritableDatabase();
            sql.beginTransaction();
            try {
                for (int i = 0; i < 1000; i++) {
                    ContentValues row = new ContentValues();
                    row.put("world_profile", "dnd"); row.put("category", "fixture");
                    row.put("title", String.format(java.util.Locale.ROOT, "preserve-%04d", i));
                    row.put("summary", "portable audit row"); row.put("before_json", "{}");
                    row.put("after_json", "{}"); row.put("reversible", 0); row.put("created_at", i + 1L);
                    sql.insertOrThrow("audit_log", null, row);
                }
                sql.setTransactionSuccessful();
            } finally { sql.endTransaction(); }
            JSONObject backup = db.exportAll();
            assertEquals(1000, backup.getJSONArray("auditLog").length());
            db.importAll(backup);
            Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM audit_log WHERE world_profile='dnd' AND title LIKE 'preserve-%'", null);
            try { assertTrue(c.moveToFirst()); assertEquals("import dropped a backed-up audit row", 1000, c.getInt(0)); }
            finally { c.close(); }
            assertEquals("restoration audit event must be additive", 1001, db.countAuditEvents());
        } finally {
            try { if (db != null) db.close(); } finally { context.deleteDatabase(dbName); }
        }
    }

    @Test public void levelUpPersistsOneClassChoiceAtomicallyAndReopens() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String dbName = "r221_lvl_" + UUID.randomUUID().toString().substring(0,8) + ".db";
        AppDb db = newSandbox(dbName);
        try {
            db.getWritableDatabase();
            db.saveCampaign("Multiclasse", "5e 2014", "Fantasia", true, true, false, "");
            long hero = db.addCharacter("Ada Artífice", "Humano", "Fighter", 1, 10, 10, 16,
                16, 12, 14, 14, 10, 10, true, "fixture");
            db.updateCharacterSheet(hero, new JSONObject().put("xp", 300).put("class_levels",
                new JSONArray().put(new JSONObject().put("class_name", "Fighter").put("level", 1)))
                .toString());

            JSONObject result = db.applyCharacterLevelUpFixedHp(hero, "Artífice");
            assertTrue(result.toString(), result.optBoolean("level_up_applied"));
            assertEquals(2, db.getCharacter(hero).optInt("level"));
            assertEquals(2, new JSONObject(db.getCharacter(hero).optString("sheet_json"))
                .getJSONArray("class_levels").getJSONObject(0).optInt("level")
                + new JSONObject(db.getCharacter(hero).optString("sheet_json"))
                    .getJSONArray("class_levels").getJSONObject(1).optInt("level"));
            JSONObject persisted = new JSONObject(db.getCharacter(hero).optString("sheet_json"));
            assertTrue(persisted.optBoolean("level_up_choices_pending"));
            assertEquals("artificer", persisted.optString("level_up_choice_class"));
            assertEquals(7, result.optInt("hp_gain"));

            db.close();
            db = newSandbox(dbName);
            JSONObject reopened = new JSONObject(db.getCharacter(hero).optString("sheet_json"));
            assertEquals(2, db.getCharacter(hero).optInt("level"));
            assertEquals("artificer", reopened.optString("level_up_choice_class"));
            assertEquals(2, reopened.getJSONArray("class_levels").length());
        } finally {
            try { if (db != null) db.close(); } finally { context.deleteDatabase(dbName); }
        }
    }

    @Test public void companionProposalApprovalUsesCurrentPersistedAuthorityAndSnapshot() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String dbName = "r221_comp_" + UUID.randomUUID().toString().substring(0,8) + ".db";
        AppDb db = newSandbox(dbName);
        try {
            db.getWritableDatabase();
            long heroId = addHero(db, "Lia Curadoria");
            db.saveCharacterControl(heroId, "curated", "{\"instruction\":\"preservar limites\"}");
            db.saveSceneState(new JSONObject().put("location", "Estalagem")
                .put("weather", "chuva").put("clock_hour", 15));
            JSONObject hero = db.getCharacter(heroId);
            JSONObject proposal = new JSONObject().put("character_id", heroId)
                .put("action", "Examinar a porta").put("reason_observed", "Há marcas recentes na fechadura")
                .put("cost_risk", "Pode alertar alguém");
            JSONObject envelope = new JSONObject().put("companion_proposals", new JSONArray().put(proposal));
            assertEquals(1, CompanionAgencyEngine.stageCurated(db, envelope, db.listActiveCharacters()));
            JSONObject stored = db.listCompanionProposals("pending", 10).get(0);
            String proposalId = stored.optString("proposal_id");
            String reviewed = stored.optString("scene_fingerprint");

            assertTrue(db.updateCompanionProposal(proposalId, "approved", null, null, reviewed));
            assertEquals(1, queuedCompanionActions(db));
            db.close();
            db = newSandbox(dbName);
            assertEquals("approved", db.listCompanionProposals("approved", 10).get(0).optString("status"));
            assertEquals(1, queuedCompanionActions(db));
            assertFalse("approved proposal cannot be replayed", db.updateCompanionProposal(
                proposalId, "approved", null, null, reviewed));

            JSONObject staleProposal = new JSONObject(proposal.toString()).put("action", "Abrir a janela");
            assertEquals(1, CompanionAgencyEngine.stageCurated(db,
                new JSONObject().put("companion_proposals", new JSONArray().put(staleProposal)),
                db.listActiveCharacters()));
            JSONObject pending = db.listCompanionProposals("pending", 10).get(0);
            String staleId = pending.optString("proposal_id");
            String staleSnapshot = pending.optString("scene_fingerprint");
            db.saveCharacterControl(heroId, "companion", "{}");
            assertFalse("MESTRE CONTROLA cannot consume a curated proposal", db.updateCompanionProposal(
                staleId, "approved", null, null, staleSnapshot));
            db.saveCharacterControl(heroId, "manual", "{}");
            assertFalse("manual control refuses a delayed approval", db.updateCompanionProposal(
                staleId, "approved", null, null, staleSnapshot));
            db.saveCharacterControl(heroId, "curated", "{\"instruction\":\"preservar limites\"}");
            db.saveSceneState(new JSONObject().put("location", "Estalagem")
                .put("weather", "tempestade").put("clock_hour", 15));
            assertFalse("changed weather invalidates the reviewed fingerprint", db.updateCompanionProposal(
                staleId, "approved", null, null, staleSnapshot));
            assertEquals("pending", db.listCompanionProposals("pending", 10).get(0).optString("status"));
            assertEquals("stale proposals never enqueue an action", 1, queuedCompanionActions(db));
        } finally {
            try { if (db != null) db.close(); } finally { context.deleteDatabase(dbName); }
        }
    }

    @Test public void worldTickRunsClockAndBackgroundWorldThroughAppDbTransaction() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String dbName = "r221_world_" + UUID.randomUUID().toString().substring(0,8) + ".db";
        AppDb db = newSandbox(dbName);
        try {
            db.getWritableDatabase();
            db.saveSceneState(new JSONObject().put("day_number", 1).put("clock_hour", 9)
                .put("time_of_day", "Manhã").put("location", "Vila do Rio").put("weather", "nublado"));
            long clockId = db.addWorldClock("Alerta da vila", 4, "", true);
            long factionId = db.upsertFaction("Guarda do Rio", "Proteger a travessia", "suprimentos", 0, "", "Vila");
            db.upsertNpcMemory(new JSONObject().put("name", "Mara").put("active", 1)
                .put("current_location", "Mercado").put("summary", "Comerciante local"));
            db.upsertNpcCognitiveState(new JSONObject().put("name", "Mara").put("urgency", 10)
                .put("next_action", "visitar o mercado").put("simulation_cursor_hours", 9)
                .put("simulation_cursor_day", 1));
            assertTrue(db.addWorldEvent("Aviso da patrulha", "A ronda deve chegar à vila",
                System.currentTimeMillis() + 60L * 60L * 1000L, 0, "{}") > 0);

            WorldStateEngine engine = new WorldStateEngine(db);
            JSONObject stale = engine.preview("esperar por 8 horas");
            assertEquals("", stale.optString("error"));
            db.saveSceneState(new JSONObject(db.getSceneState().toString()).put("weather", "tempestade"));
            assertFalse("stale scene cannot commit", engine.commit(stale));
            assertEquals(0, findClock(db, clockId).optInt("current_value"));

            JSONObject preview = engine.preview("esperar por 8 horas");
            assertEquals("", preview.optString("error"));
            assertTrue("due world event is part of the preview", preview.optJSONArray("events_triggered").length() > 0);
            assertTrue("NPC participates in the persisted world simulation",
                preview.optJSONObject("living_world_simulation").optJSONArray("npc_actions").length() > 0);
            assertTrue("faction participates in the persisted world simulation",
                preview.optJSONObject("living_world_simulation").optJSONArray("faction_actions").length() > 0);
            assertTrue(engine.commit(preview));
            assertFalse("a world preview is single-use", engine.commit(preview));
            assertEquals(1, findClock(db, clockId).optInt("current_value"));
            assertEquals(17d, db.getSceneState().optDouble("clock_hour"), 0.01d);
            JSONObject faction = findFaction(db, factionId);
            assertTrue(faction.optDouble("simulation_cursor_hours", -1d) >= 17d);
            JSONObject npc = db.getNpcCognitiveState("Mara");
            assertTrue(npc.optDouble("simulation_cursor_hours", -1d) >= 17d);
            assertTrue(npc.optInt("urgency") > 10);

            db.close();
            db = newSandbox(dbName);
            assertEquals(1, findClock(db, clockId).optInt("current_value"));
            assertTrue(findFaction(db, factionId).optDouble("simulation_cursor_hours", -1d) >= 17d);
            assertTrue(db.getNpcCognitiveState("Mara").optDouble("simulation_cursor_hours", -1d) >= 17d);

            double hourBeforeFailure = db.getSceneState().optDouble("clock_hour");
            double factionBeforeFailure = findFaction(db, factionId).optDouble("simulation_cursor_hours");
            double npcBeforeFailure = db.getNpcCognitiveState("Mara").optDouble("simulation_cursor_hours");
            db.getWritableDatabase().execSQL("CREATE TRIGGER run221_fail_world_tick BEFORE UPDATE ON world_clocks "
                + "BEGIN SELECT RAISE(ABORT, 'run221 injected world rollback'); END");
            WorldStateEngine retryEngine = new WorldStateEngine(db);
            JSONObject failed = retryEngine.preview("esperar por 8 horas");
            assertFalse("injected SQL error rolls back the world tick", retryEngine.commit(failed));
            assertEquals(17d, db.getSceneState().optDouble("clock_hour"), 0.01d);
            assertEquals(hourBeforeFailure, db.getSceneState().optDouble("clock_hour"), 0.01d);
            assertEquals(1, findClock(db, clockId).optInt("current_value"));
            assertEquals(factionBeforeFailure, findFaction(db, factionId).optDouble("simulation_cursor_hours"), 0.01d);
            assertEquals(npcBeforeFailure, db.getNpcCognitiveState("Mara").optDouble("simulation_cursor_hours"), 0.01d);
            db.getWritableDatabase().execSQL("DROP TRIGGER run221_fail_world_tick");
            db.close();
            db = newSandbox(dbName);
            assertEquals(17d, db.getSceneState().optDouble("clock_hour"), 0.01d);
            assertEquals(1, findClock(db, clockId).optInt("current_value"));
        } finally {
            try { if (db != null) db.close(); } finally { context.deleteDatabase(dbName); }
        }
    }

    @Test public void legacyBackupRestoresCoreSaveWithoutBackup21Seal() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String dbName = "r221_legacy_" + UUID.randomUUID().toString().substring(0,8) + ".db";
        AppDb db = newSandbox(dbName);
        try {
            db.getWritableDatabase();
            db.saveCampaign("Campanha antiga", "5e 2014", "Fantasia", true, true, false, "legado");
            addHero(db, "Ravi Legado");
            JSONObject current = db.exportAll();
            JSONObject legacy = new JSONObject().put("format", "braseiro-dnd-backup")
                .put("version", 11).put("worldProfile", "dnd")
                .put("campaign", current.getJSONObject("campaign"))
                .put("characters", current.getJSONArray("characters"))
                .put("storyState", current.optJSONObject("storyState"));

            db.saveCampaign("Campanha temporária", "5e 2014", "Sombria", false, false, true, "");
            addHero(db, "Personagem temporário");
            db.importAll(legacy);
            assertEquals("Campanha antiga", db.getCampaign().optString("name"));
            assertEquals(1, db.listCharacters().size());
            assertEquals("Ravi Legado", db.listCharacters().get(0).optString("name"));
            assertEquals(11, legacy.optInt("version"));
            assertFalse(legacy.has(Backup21IntegrityEnvelope.KEY));
        } finally {
            try { if (db != null) db.close(); } finally { context.deleteDatabase(dbName); }
        }
    }

    @Test public void guidedCharacterActionPersistsTypedEffectAndRejectsPendingReplay() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String dbName = "r221_guided_" + UUID.randomUUID().toString().substring(0,8) + ".db";
        AppDb db = newSandbox(dbName);
        try {
            db.getWritableDatabase();
            long hero = db.addCharacter("Nilo Monge", "Humano", "Monge", 3, 20, 20, 16,
                12, 16, 14, 10, 14, 10, true, "fixture");
            String subclass = AncestralStancesRuntimeEngine.SUBCLASS;
            db.updateCharacterSheet(hero, new JSONObject().put("species", "Humano")
                .put("class_name", "Monge").put("level", 3).put("subclass_name", subclass).toString());
            String before = db.getCharacter(hero).optString("sheet_json");

            JSONObject pending = db.executeGuidedCharacterAction(hero, "subclass", "Preparar Posturas",
                subclass, new JSONObject(), "PLAYER_REACTION", "stance-pending-001");
            assertFalse(pending.toString(), pending.optBoolean("accepted"));
            assertEquals("pending resolver state must be rolled back", before,
                db.getCharacter(hero).optString("sheet_json"));
            assertEquals(0, countEffectReceipt(db, "guided-class-effect:stance-pending-001"));

            JSONObject applied = db.executeGuidedCharacterAction(hero, "subclass", "Preparar Posturas",
                subclass, new JSONObject().put("stances", new JSONArray().put("Galo"))
                    .put("long_rest_preparation_confirmed", true),
                "PLAYER_REACTION", "stance-applied-001");
            assertTrue(applied.toString(), applied.optBoolean("accepted"));
            assertTrue(applied.optBoolean("effect_applied"));
            assertEquals("guided_character_atomic_commit", applied.optString("authority"));
            JSONObject result = applied.getJSONObject("result");
            assertEquals("run141_stances_prepared", result.optString("subclass_action_event"));
            assertEquals(1, result.getJSONObject("subclass_action_effect").getJSONArray("stances").length());
            assertEquals("Galo", new JSONObject(db.getCharacter(hero).optString("sheet_json"))
                .getJSONArray("ancestral_stances_prepared").optString(0));
            assertEquals(1, countEffectReceipt(db, "guided-class-effect:stance-applied-001"));

            db.close();
            db = newSandbox(dbName);
            assertEquals("Galo", new JSONObject(db.getCharacter(hero).optString("sheet_json"))
                .getJSONArray("ancestral_stances_prepared").optString(0));
            JSONObject replay = db.executeGuidedCharacterAction(hero, "subclass", "Preparar Posturas",
                subclass, new JSONObject().put("stances", new JSONArray().put("Galo")),
                "PLAYER_REACTION", "stance-applied-001");
            assertFalse(replay.toString(), replay.optBoolean("accepted"));
            assertTrue(replay.optBoolean("replayed"));
            assertEquals(1, countEffectReceipt(db, "guided-class-effect:stance-applied-001"));
        } finally {
            try { if (db != null) db.close(); } finally { context.deleteDatabase(dbName); }
        }
    }

    private static void placeTokenForAttackGeometryFixture(AppDb db,long combatantId,int cellX,int cellY,int width,int height) {
        ContentValues position = new ContentValues();
        position.put("x", (cellX + 0.5d) / width); position.put("y", (cellY + 0.5d) / height);
        assertEquals(1, db.getWritableDatabase().update("tactical_tokens", position,
            "world_profile=? AND combatant_id=?", new String[]{"dnd", String.valueOf(combatantId)}));
    }

    @Test public void compiledStatblockAttackUsesPersistedCombatTurnAndRejectsReplay() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String dbName = "r221_stat_" + UUID.randomUUID().toString().substring(0,8) + ".db";
        AppDb db = newSandbox(dbName);
        try {
            db.getWritableDatabase();
            String blockId = "run221-combat-fixture";
            registerAttackFixtureStatblock(blockId);
            long monster = db.addCombatant("Guardião de teste", 20, 20, 20, 12, "", false);
            long hero = db.addCombatant("Alvo de teste", 10, 15, 15, 10, "", true);
            assertTrue(monster > 0 && hero > 0);
            JSONObject monsterRow = db.getCombatant(monster);
            JSONObject resources = new JSONObject(monsterRow.optString("resources_json", "{}"));
            resources.put("block_id", blockId);
            ContentValues update = new ContentValues();
            update.put("resources_json", resources.toString());
            assertEquals(1, db.getWritableDatabase().update("combatants", update,
                "id=? AND world_profile=?", new String[]{String.valueOf(monster), "dnd"}));

            db.setCombatRunning(true);
            JSONObject turn = db.nextTurn();
            assertEquals(monster, turn.optLong("id"));
            assertEquals(1, db.getCombatant(monster).optInt("active_turn"));
            JSONObject scene = new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            int width = scene.optInt("width_cells"), height = scene.optInt("height_cells");
            assertTrue("test fixture needs a ready tactical grid", width > 1 && height > 1);
            db.ensureTacticalTokensForCombat();
            placeTokenForAttackGeometryFixture(db,monster,2,2,width,height);
            placeTokenForAttackGeometryFixture(db,hero,2,2,width,height);
            // Put target four cells away; this melee attack must not spend the
            // turn action or alter hit points when the range check fails.
            int farCell = Math.min(width - 1, 7);
            placeTokenForAttackGeometryFixture(db,hero,farCell,2,width,height);
            JSONObject outOfReach = db.applyCompiledStatblockAttack(monster, hero, blockId,
                "Mordida", 15, 5, 0, "ui-monster-run221-out-of-reach");
            assertEquals(outOfReach.toString(), "target_out_of_reach", outOfReach.optString("statblock_attack_blocked"));
            assertEquals(15, db.getCombatant(hero).optInt("hp"));
            assertTrue(new JSONObject(db.getCombatant(monster).optString("resources_json", "{}"))
                .optBoolean("action_available", false));

            JSONArray wall = new JSONArray().put(new JSONObject().put("x", 3).put("y", 2)
                .put("type", "wall").put("blocks_sight", true).put("cover", "none"));
            ContentValues obstacleUpdate = new ContentValues(); obstacleUpdate.put("obstacles_json", wall.toString());
            assertEquals(1, db.getWritableDatabase().update("tactical_scenes", obstacleUpdate,
                "world_profile=? AND scene_key=?", new String[]{"dnd", scene.optString("scene_key")}));
            placeTokenForAttackGeometryFixture(db,hero,4,2,width,height);
            JSONObject throughWall = db.applyCompiledStatblockAttack(monster, hero, blockId,
                "Mordida", 15, 5, 0, "ui-monster-run221-through-wall");
            assertEquals(throughWall.toString(), "line_of_sight_blocked",
                throughWall.optString("statblock_attack_blocked"));
            assertEquals(15, db.getCombatant(hero).optInt("hp"));
            assertTrue(new JSONObject(db.getCombatant(monster).optString("resources_json", "{}"))
                .optBoolean("action_available", false));

            obstacleUpdate.put("obstacles_json", "[]");
            assertEquals(1, db.getWritableDatabase().update("tactical_scenes", obstacleUpdate,
                "world_profile=? AND scene_key=?", new String[]{"dnd", scene.optString("scene_key")}));
            placeTokenForAttackGeometryFixture(db,hero,3,2,width,height);

            String token = "ui-monster-run221-hit-001";
            JSONObject result = db.applyCompiledStatblockAttack(monster, hero, blockId,
                "Mordida", 15, 5, 0, token);
            assertTrue(result.toString(), result.optBoolean("statblock_attack_committed"));
            assertEquals(10, db.getCombatant(hero).optInt("hp"));
            JSONObject spent = new JSONObject(db.getCombatant(monster).optString("resources_json", "{}"));
            assertFalse("attack consumes the active turn action", spent.optBoolean("action_available", true));
            assertEquals(1, countEffectReceipt(db, result.optString("statblock_attack_receipt")));
            JSONObject storedProof = new JSONObject(effectReceiptSource(db, result.optString("statblock_attack_receipt")));
            assertTrue(storedProof.optJSONObject("attack_geometry").optBoolean("attack_geometry_valid"));
            assertTrue(storedProof.optJSONObject("attack_geometry").optString("attack_geometry_receipt").startsWith("attackgeom-v1:"));

            db.close();
            db = newSandbox(dbName);
            JSONObject replay = db.applyCompiledStatblockAttack(monster, hero, blockId,
                "Mordida", 15, 5, 0, token);
            assertEquals("already_resolved_or_replayed", replay.optString("statblock_attack_blocked"));
            assertEquals(10, db.getCombatant(hero).optInt("hp"));
            assertEquals(1, countEffectReceipt(db, result.optString("statblock_attack_receipt")));
        } finally {
            try { if (db != null) db.close(); } finally { context.deleteDatabase(dbName); }
        }
    }

    private static JSONObject findClock(AppDb db, long id) {
        for (JSONObject clock : db.listWorldClocks(true, 100)) if (clock.optLong("id") == id) return clock;
        return new JSONObject();
    }

    private static JSONObject findFaction(AppDb db, long id) {
        for (JSONObject faction : db.listFactions(100)) if (faction.optLong("id") == id) return faction;
        return new JSONObject();
    }

    private static int countEffectReceipt(AppDb db, String key) {
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT COUNT(*) FROM effect_receipts WHERE effect_key=?", new String[]{key});
        try { return c.moveToFirst() ? c.getInt(0) : 0; }
        finally { c.close(); }
    }

    private static String effectReceiptSource(AppDb db, String key) {
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT source FROM effect_receipts WHERE world_profile='dnd' AND effect_key=?",
            new String[]{key});
        try { return c.moveToFirst() ? c.getString(0) : "{}"; }
        finally { c.close(); }
    }

    private static int queuedCompanionActions(AppDb db) {
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT COUNT(*) FROM party_action_queue WHERE world_profile='dnd'", null);
        try { return c.moveToFirst() ? c.getInt(0) : 0; }
        finally { c.close(); }
    }

    private static AppDb newSandbox(String dbName) {
        Context context = RuntimeEnvironment.getApplication();
        AppDb db = new AppDb(context, dbName);
        db.setActiveMapProfile("dnd");
        return db;
    }

    private static long addHero(AppDb db, String name) throws Exception {
        long id = db.addCharacter(name, "Humano", "Fighter", 1, 10, 10, 16,
            16, 12, 14, 10, 10, 10, true, "fixture");
        db.updateCharacterSheet(id, new JSONObject().put("species", "Humano")
            .put("class_name", "Fighter").toString());
        return id;
    }

    private static void registerFixtureStatblock() throws Exception {
        JSONObject source = new JSONObject().put("source_type", "compiled_statblock")
            .put("content_kind", "statblock").put("content_name", "run221-backup-rollback-fixture")
            .put("source_book", "RUN221 test fixture").put("source_locator", "local-test-only");
        JSONObject statblock = new JSONObject().put("id", "run221-backup-rollback-fixture")
            .put("source_book", "RUN221 test fixture").put("creature_type", "construct")
            .put("armor_class", 10).put("hit_points", 1).put("actions", new JSONArray())
            .put("mechanical_basis", source);
        CompiledStatblockEngine.registerAuthorized(statblock);
    }

    private static void registerAttackFixtureStatblock(String id) throws Exception {
        JSONObject source = new JSONObject().put("source_type", "compiled_statblock")
            .put("content_kind", "statblock").put("content_name", id)
            .put("source_book", "RUN221 test fixture").put("source_locator", "local-test-only");
        JSONObject attack = new JSONObject().put("name", "Mordida").put("category", "action")
            .put("to_hit", 4).put("damage", "1d6 + 2").put("damage_type", "perfurante").put("reach_ft", 10);
        JSONObject statblock = new JSONObject().put("id", id).put("source_book", "RUN221 test fixture")
            .put("creature_type", "construct").put("armor_class", 12).put("hit_points", 20)
            .put("actions", new JSONArray().put(attack)).put("mechanical_basis", source);
        CompiledStatblockEngine.registerAuthorized(statblock);
    }
}
