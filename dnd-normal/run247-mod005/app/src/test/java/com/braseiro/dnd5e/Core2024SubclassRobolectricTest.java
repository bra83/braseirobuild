package com.braseiro.dnd5e;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class Core2024SubclassRobolectricTest {

    private static String databaseName() {
        return "subc_" + Long.toHexString(System.nanoTime() & 0xFFFFFL) + ".db";
    }

    @Test
    public void exactlyFortyEightOfficialPhb2024SubclassesAcrossTwelveClasses() {
        assertEquals("Exatamente 48 subclasses oficiais no PHB 2024", 48,
            Core2024SubclassRuntimeEngine.totalSubclassesCount());

        String[] coreClasses = {
            "Bárbaro", "Bardo", "Clérigo", "Druida",
            "Guerreiro", "Monge", "Paladino", "Guardião",
            "Ladino", "Feiticeiro", "Bruxo", "Mago"
        };

        for (String className : coreClasses) {
            List<Core2024SubclassRuntimeEngine.SubclassDef> list =
                Core2024SubclassRuntimeEngine.getSubclassesForClass(className);
            assertEquals("Classe " + className + " deve ter exatamente 4 subclasses oficiais no PHB 2024",
                4, list.size());
            for (Core2024SubclassRuntimeEngine.SubclassDef sub : list) {
                assertNotNull(sub.name);
                assertNotNull(sub.nameEn);
                assertNotNull(sub.locator);
                assertTrue("Subclasse " + sub.name + " deve ter ao menos 2 features definidas",
                    sub.features.size() >= 2);
                boolean hasLevel3 = false;
                for (Core2024SubclassRuntimeEngine.SubclassFeature feat : sub.features) {
                    if (feat.level == 3) hasLevel3 = true;
                }
                assertTrue("Subclasse " + sub.name + " deve ter obrigatoriamente recurso no nível 3",
                    hasLevel3);
            }
        }
    }

    @Test
    public void levelGateThreeIsRequiredFor2024Subclasses() {
        assertFalse("Nível 1 não pode ter subclasse no PHB 2024",
            Core2024SubclassRuntimeEngine.isSubclassLevelValid(1));
        assertFalse("Nível 2 não pode ter subclasse no PHB 2024",
            Core2024SubclassRuntimeEngine.isSubclassLevelValid(2));
        assertTrue("Nível 3 desbloqueia subclasse no PHB 2024",
            Core2024SubclassRuntimeEngine.isSubclassLevelValid(3));
        assertTrue("Nível 5 mantém subclasse válida",
            Core2024SubclassRuntimeEngine.isSubclassLevelValid(5));
        assertTrue("Nível 20 mantém subclasse válida",
            Core2024SubclassRuntimeEngine.isSubclassLevelValid(20));
    }

    @Test
    public void lookupByPtBrAndEnglishNamesBothResolve() {
        Core2024SubclassRuntimeEngine.SubclassDef pt =
            Core2024SubclassRuntimeEngine.getSubclass("Mestre da Batalha");
        assertNotNull(pt);
        assertEquals("Guerreiro", pt.className);

        Core2024SubclassRuntimeEngine.SubclassDef en =
            Core2024SubclassRuntimeEngine.getSubclass("Battle Master");
        assertNotNull(en);
        assertEquals(pt.name, en.name);

        Core2024SubclassRuntimeEngine.SubclassDef champion =
            Core2024SubclassRuntimeEngine.getSubclass("Campeão");
        assertNotNull(champion);
        assertEquals(3, champion.features.get(0).level);
        assertEquals("improved_critical", champion.features.get(0).id);
    }

    @Test
    public void persistedCharacterSubclassIntegrationAndReopen() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String dbName = databaseName();
        AppDb db = new AppDb(context, dbName);
        try {
            db.setActiveMapProfile("dnd");
            long charId = db.addCharacter(
                "Kaelen", "Humano", "Guerreiro", 3, 28, 28, 18,
                16, 12, 14, 10, 12, 10, true, "Soldado");

            // Associa a subclasse oficial Campeão no sheet_json
            JSONObject ch = db.getCharacter(charId);
            JSONObject sheet = new JSONObject(ch.optString("sheet_json", "{}"));
            sheet.put("subclass_2024", "Campeão");
            sheet.put("crit_threshold", 19);
            db.updateCharacterSheet(charId, sheet.toString());

            // Reabertura
            db.close();
            db = new AppDb(context, dbName);
            db.setActiveMapProfile("dnd");

            JSONObject reopened = db.getCharacter(charId);
            JSONObject reopenedSheet = new JSONObject(reopened.optString("sheet_json", "{}"));
            assertEquals("Campeão", reopenedSheet.optString("subclass_2024"));
            assertEquals(19, reopenedSheet.optInt("crit_threshold"));
        } finally {
            db.close();
            context.deleteDatabase(dbName);
        }
    }
    @Test
    public void selectionMustMatchClassAndLegalCharacterLevel() {
        assertTrue(Core2024SubclassRuntimeEngine.isSubclassSelectionValid("Guerreiro", 3, "Campeão"));
        assertTrue(Core2024SubclassRuntimeEngine.isSubclassSelectionValid("Fighter", 20, "Champion"));
        assertFalse(Core2024SubclassRuntimeEngine.isSubclassSelectionValid("Mago", 3, "Campeão"));
        assertFalse(Core2024SubclassRuntimeEngine.isSubclassSelectionValid("Guerreiro", 2, "Campeão"));
        assertFalse(Core2024SubclassRuntimeEngine.isSubclassSelectionValid("Guerreiro", 21, "Campeão"));
        assertFalse(Core2024SubclassRuntimeEngine.isSubclassSelectionValid("Guerreiro", 3, "Subclasse inventada"));
        assertFalse(Core2024SubclassRuntimeEngine.isSubclassLevelValid(21));
    }

    private static JSONObject subclassCandidate(String characterClass, int level, String choice)
            throws Exception {
        return new JSONObject()
            .put("name", "Atala").put("species", "Humano")
            .put("class_name", characterClass).put("level", level)
            .put("hp", 20).put("max_hp", 20).put("ac", 16)
            .put("str", 16).put("dex", 12).put("con", 14)
            .put("int_score", 10).put("wis", 10).put("cha", 10)
            .put("rules_source", DndRulesEngine.PROFILE_2024)
            .put("subclass_2024", choice);
    }

    @Test
    public void characterSaveRejectsWrongClassAndPrematureSubclassWithoutCreatingHero() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String dbName = databaseName();
        AppDb db = new AppDb(context, dbName);
        try {
            db.setActiveMapProfile("dnd");
            JSONObject wrongClass = db.saveCharacterSheetAtomic(0, false,
                subclassCandidate("Mago", 3, "Campeão"), "", "manual");
            assertEquals("invalid_2024_subclass_for_class_or_level",
                wrongClass.optString("subclass_blocked"));
            assertTrue(db.listCharacters().isEmpty());

            JSONObject tooEarly = db.saveCharacterSheetAtomic(0, false,
                subclassCandidate("Guerreiro", 2, "Campeão"), "", "manual");
            assertEquals("invalid_2024_subclass_for_class_or_level",
                tooEarly.optString("subclass_blocked"));
            assertTrue(db.listCharacters().isEmpty());
        } finally {
            db.close();
            context.deleteDatabase(dbName);
        }
    }

    @Test
    public void acceptedEnglishSubclassAliasPersistsCanonicalPtBrChoice() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String dbName = databaseName();
        AppDb db = new AppDb(context, dbName);
        try {
            db.setActiveMapProfile("dnd");
            JSONObject saved = db.saveCharacterSheetAtomic(0, false,
                subclassCandidate("Guerreiro", 3, "Champion"), "", "manual");
            assertTrue("The 2024 character should be saved: " + saved,
                saved.optBoolean("character_saved"));
            long characterId = saved.optLong("character_id");
            assertTrue(characterId > 0);
            for (int i = 0; i < 3; i++) {
                db.close();
                db = new AppDb(context, dbName);
                db.setActiveMapProfile("dnd");
                JSONObject persisted = db.getCharacter(characterId);
                assertEquals("2024 profile must remain pinned",
                    DndRulesEngine.PROFILE_2024, persisted.optString("rules_source"));
                JSONObject sheet = new JSONObject(persisted.optString("sheet_json", "{}"));
                assertEquals("Campeão", sheet.optString("subclass_2024"));
            }
        } finally {
            db.close();
            context.deleteDatabase(dbName);
        }
    }

    @Test
    public void championCriticalNeverAppliesToWrongClassOrPrematureLevel() throws Exception {
        JSONObject wizard = new JSONObject().put("class_name", "Mago")
            .put("subclass_2024", "Campeão").put("level", 15);
        assertEquals("Wizard must not inherit Fighter Champion crit range", 20,
            Core2024SubclassRuntimeEngine.criticalThreshold(wizard));
        JSONObject tooEarly = new JSONObject().put("class_name", "Guerreiro")
            .put("subclass_2024", "Campeão").put("level", 2);
        assertEquals(20, Core2024SubclassRuntimeEngine.criticalThreshold(tooEarly));
    }

    @Test
    public void liveGatewayRejectsUnofficialSubclassOwnershipWithoutAnySheetWrite() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String name = databaseName();
        AppDb db = new AppDb(context, name);
        try {
            db.setActiveMapProfile("dnd");
            JSONObject saved = db.saveCharacterSheetAtomic(0, false,
                subclassCandidate("Mago", 3, ""), "", "manual");
            assertTrue(saved.toString(), saved.optBoolean("character_saved"));
            long id = saved.optLong("character_id");
            assertTrue(id > 0);
            JSONObject original = db.getCharacter(id);
            JSONObject sheet = new JSONObject(original.optString("sheet_json", "{}"));
            sheet.put("subclass_2024", "Campeão");
            db.updateCharacterSheet(id, sheet.toString());
            String before = db.getCharacter(id).optString("sheet_json", "");
            JSONObject blocked = db.resolveCharacterHomebrewSubclassAction(id,
                "Crítico Aprimorado", new JSONObject());
            assertFalse(blocked.toString(), blocked.optBoolean("subclass_action_resolved"));
            assertEquals("official_2024_subclass_not_owned_by_character",
                blocked.optString("subclass_action_reason"));
            assertEquals("Unowned subclass must not change resource or sheet state",
                before, db.getCharacter(id).optString("sheet_json", ""));
        } finally {
            db.close();
            context.deleteDatabase(name);
        }
    }

    @Test
    public void liveGatewayAcceptsProperFighterChampionAndPersistsEffect() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String name = databaseName();
        AppDb db = new AppDb(context, name);
        try {
            db.setActiveMapProfile("dnd");
            JSONObject saved = db.saveCharacterSheetAtomic(0, false,
                subclassCandidate("Guerreiro", 3, "Campeão"), "", "manual");
            assertTrue(saved.toString(), saved.optBoolean("character_saved"));
            long id = saved.optLong("character_id");
            JSONObject resolved = db.resolveCharacterHomebrewSubclassAction(id,
                "Crítico Aprimorado", new JSONObject());
            assertTrue("Official owner must be allowed: " + resolved,
                resolved.optBoolean("subclass_action_resolved"));
            assertEquals("champion_improved_critical",
                resolved.optString("subclass_action_event"));
            JSONObject reopened = new JSONObject(db.getCharacter(id).optString("sheet_json", "{}"));
            assertEquals("champion_improved_critical",
                reopened.optString("subclass_action_event"));
        } finally {
            db.close();
            context.deleteDatabase(name);
        }
    }

    @Test
    public void criticalThresholdAccuratelyReflectsChampionLevels() throws Exception {
        JSONObject normalSheet = new JSONObject().put("class_name", "Guerreiro").put("level", 5);
        assertEquals(20, Core2024SubclassRuntimeEngine.criticalThreshold(normalSheet));

        JSONObject championLvl3 = new JSONObject().put("class_name", "Guerreiro")
            .put("subclass_name", "Campeão").put("level", 3);
        assertEquals(19, Core2024SubclassRuntimeEngine.criticalThreshold(championLvl3));

        JSONObject championLvl14 = new JSONObject().put("class_name", "Guerreiro")
            .put("subclass", "Champion").put("level", 14);
        assertEquals(19, Core2024SubclassRuntimeEngine.criticalThreshold(championLvl14));

        JSONObject championLvl15 = new JSONObject().put("class_name", "Guerreiro")
            .put("subclass_name", "Campeão").put("level", 15);
        assertEquals(18, Core2024SubclassRuntimeEngine.criticalThreshold(championLvl15));
    }

    @Test
    public void actionsForReturnsValidOfficialFeatures() {
        org.json.JSONArray barbActs = Core2024SubclassRuntimeEngine.actionsFor("Caminho do Berserker", 14);
        assertTrue(barbActs.toString().contains("Frenesi"));
        assertTrue(barbActs.toString().contains("Fúria Descerebrada"));
        assertTrue(barbActs.toString().contains("Retaliação"));
        assertTrue(barbActs.toString().contains("Presença Intimidadora"));

        org.json.JSONArray bmActs = Core2024SubclassRuntimeEngine.actionsFor("Mestre da Batalha", 7);
        assertTrue(bmActs.toString().contains("Manobra: Ataque Ameaçador"));
        assertTrue(bmActs.toString().contains("Manobra: Derrubar"));
        assertTrue(bmActs.toString().contains("Conheça Seu Inimigo"));

        org.json.JSONArray clericActs = Core2024SubclassRuntimeEngine.actionsFor("Domínio da Luz", 3);
        assertTrue(clericActs.toString().contains("Resplendor do Alvorecer"));
        assertTrue(clericActs.toString().contains("Clarão Protetor"));
    }

    @Test
    public void berserkerFrenzyAndMindlessRageExecuteDeterministically() throws Exception {
        JSONObject sheet = new JSONObject()
            .put("class_name", "Bárbaro")
            .put("subclass_name", "Caminho do Berserker")
            .put("level", 6)
            .put("rage_active", false)
            .put("conditions", "frightened");

        // Frenesi requires rage active
        JSONObject blockedRage = Core2024SubclassRuntimeEngine.resolve(
            sheet, "Caminho do Berserker", 6, "Frenesi", null);
        assertFalse(blockedRage.optBoolean("subclass_action_resolved"));
        assertEquals("rage_inactive", blockedRage.optString("subclass_action_reason"));

        // Activate rage and resolve Frenesi
        sheet.put("rage_active", true);
        JSONObject frenzy = Core2024SubclassRuntimeEngine.resolve(
            sheet, "Caminho do Berserker", 6, "Frenesi", null);
        assertTrue(frenzy.optBoolean("subclass_action_resolved"));
        assertEquals("barbarian_frenzy", frenzy.optString("subclass_action_event"));
        assertEquals("1d6", frenzy.optJSONObject("subclass_action_effect").optString("extra_damage_dice"));

        // Mindless rage cleanses frightened
        JSONObject mindless = Core2024SubclassRuntimeEngine.resolve(
            sheet, "Caminho do Berserker", 6, "Fúria Descerebrada", null);
        assertTrue(mindless.optBoolean("subclass_action_resolved"));
        assertEquals("barbarian_mindless_rage", mindless.optString("subclass_action_event"));
        assertFalse(mindless.optString("conditions").contains("frightened"));
    }

    @Test
    public void battleMasterSuperiorityDiceSpendAndExecuteViaAppDb() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String dbName = databaseName();
        AppDb db = new AppDb(context, dbName);
        try {
            db.setActiveMapProfile("dnd");
            long charId = db.addCharacter(
                "Marcus", "Humano", "Guerreiro", 5, 44, 44, 18,
                16, 12, 14, 10, 12, 10, true, "Soldado");
            JSONObject ch = db.getCharacter(charId);
            JSONObject sheet = new JSONObject(ch.optString("sheet_json", "{}"));
            sheet.put("subclass_name", "Mestre da Batalha");
            sheet.put("rules_source", DndRulesEngine.PROFILE_2024);
            Core2024SubclassRuntimeEngine.applyToSheet(sheet);
            db.updateCharacterSheet(charId, sheet.toString());

            // Resolve maneuver
            JSONObject payload = new JSONObject().put("die_roll", 6);
            JSONObject result = db.resolveCharacterHomebrewSubclassAction(
                charId, "Manobra: Derrubar", payload);
            assertTrue("Maneuver must resolve: " + result,
                result.optBoolean("subclass_action_resolved"));
            assertEquals("battlemaster_maneuver", result.optString("subclass_action_event"));
            JSONObject effect = result.optJSONObject("subclass_action_effect");
            assertNotNull(effect);
            assertEquals(6, effect.optInt("superiority_die_roll"));

            // Check resource deducted
            JSONObject updatedSheet = new JSONObject(db.getCharacter(charId).optString("sheet_json", "{}"));
            JSONObject res = updatedSheet.optJSONObject("subclass_resources").optJSONObject("superiority_dice");
            assertNotNull(res);
            assertEquals(3, res.optInt("current")); // Started with 4 at level 5, spent 1
        } finally {
            db.close();
            context.deleteDatabase(dbName);
        }
    }

    @Test
    public void lifeClericPreserveLifeHealsAndSpendsChannelDivinityViaAppDb() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String dbName = databaseName();
        AppDb db = new AppDb(context, dbName);
        try {
            db.setActiveMapProfile("dnd");
            long charId = db.addCharacter(
                "Elysia", "Humano", "Clérigo", 3, 24, 24, 16,
                10, 12, 14, 10, 16, 12, true, "Acólito");
            JSONObject ch = db.getCharacter(charId);
            JSONObject sheet = new JSONObject(ch.optString("sheet_json", "{}"));
            sheet.put("subclass_name", "Domínio da Vida");
            sheet.put("rules_source", DndRulesEngine.PROFILE_2024);
            Core2024ClassRuntimeEngine.ensure(sheet, "Clérigo", 3);
            Core2024SubclassRuntimeEngine.applyToSheet(sheet);
            db.updateCharacterSheet(charId, sheet.toString());

            // Channel divinity budget = 5 * 3 = 15 HP
            org.json.JSONArray dist = new org.json.JSONArray()
                .put(new JSONObject().put("target_id", "ally-1").put("healing", 10))
                .put(new JSONObject().put("target_id", "ally-2").put("healing", 5));
            JSONObject payload = new JSONObject().put("distribution", dist);

            JSONObject result = db.resolveCharacterHomebrewSubclassAction(
                charId, "Preservar a Vida", payload);
            assertTrue("Preserve Life must resolve: " + result,
                result.optBoolean("subclass_action_resolved"));
            assertEquals("cleric_preserve_life", result.optString("subclass_action_event"));
            assertEquals(15, result.optJSONObject("subclass_action_effect").optInt("healing_total"));

            // Check Channel Divinity spent in class_resources
            JSONObject updatedSheet = new JSONObject(db.getCharacter(charId).optString("sheet_json", "{}"));
            JSONObject cd = updatedSheet.optJSONObject("class_resources").optJSONObject("channel_divinity");
            assertNotNull(cd);
            assertEquals(1, cd.optInt("current")); // Started with 2 at level 3, spent 1
        } finally {
            db.close();
            context.deleteDatabase(dbName);
        }
    }

    @Test
    public void wildHeartBarbarianAspectSelectionAndRageResistances() throws Exception {
        JSONObject sheet = new JSONObject()
            .put("class_name", "Bárbaro")
            .put("subclass_name", "Caminho do Coração Selvagem")
            .put("level", 6)
            .put("rage_active", true);

        JSONObject payload = new JSONObject().put("aspect", "urso");
        JSONObject res = Core2024SubclassRuntimeEngine.resolve(
            sheet, "Caminho do Coração Selvagem", 6, "Fúria das Selvas", payload);
        assertTrue(res.optBoolean("subclass_action_resolved"));
        assertEquals("urso", res.optString("wild_heart_aspect"));
        assertEquals("all_except_psychic", res.optString("damage_resistances"));
    }

    @Test
    public void danceBardDazzlingFootworkUnarmoredAcAndInspiringMovement() throws Exception {
        JSONObject sheet = new JSONObject()
            .put("class_name", "Bardo")
            .put("subclass_name", "Colégio da Dança")
            .put("level", 6)
            .put("dex", 14)
            .put("cha", 16);

        JSONObject dance = Core2024SubclassRuntimeEngine.resolve(
            sheet, "Colégio da Dança", 6, "Passos Deslumbrantes", null);
        assertTrue(dance.optBoolean("subclass_action_resolved"));
        assertEquals(3, dance.optInt("dance_unarmored_ac_bonus"));

        JSONObject move = Core2024SubclassRuntimeEngine.resolve(
            sheet, "Colégio da Dança", 6, "Movimento Inspirador", null);
        assertTrue(move.optBoolean("subclass_action_resolved"));
        assertEquals("dance_inspiring_movement", move.optString("subclass_action_event"));
    }

    @Test
    public void moonDruidCombatWildShapeTempHpAndMoonlightStep() throws Exception {
        JSONObject sheet = new JSONObject()
            .put("class_name", "Druida")
            .put("subclass_name", "Círculo da Lua")
            .put("level", 6)
            .put("wis", 16);
        sheet = Core2024ClassRuntimeEngine.ensure(sheet, "Druida", 6);
        Core2024SubclassRuntimeEngine.applyToSheet(sheet);

        JSONObject form = Core2024SubclassRuntimeEngine.resolve(
            sheet, "Círculo da Lua", 6, "Forma Selvagem de Combate", null);
        assertTrue(form.optBoolean("subclass_action_resolved"));
        assertEquals(18, form.optInt("temp_hp")); // 3 * 6 = 18 temp hp
        assertEquals(16, form.optInt("moon_wild_shape_ac")); // 13 + 3 = 16 ac

        JSONObject step = Core2024SubclassRuntimeEngine.resolve(
            form, "Círculo da Lua", 6, "Passo do Luar", null);
        assertTrue(step.optBoolean("subclass_action_resolved"));
        assertEquals(30, step.optJSONObject("subclass_action_effect").optInt("teleport_range_ft"));
    }

    @Test
    public void vengeancePaladinVowOfEnmityAdvantageViaAppDb() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String dbName = databaseName();
        AppDb db = new AppDb(context, dbName);
        try {
            db.setActiveMapProfile("dnd");
            long charId = db.addCharacter(
                "Theron", "Humano", "Paladino", 3, 28, 28, 18,
                16, 10, 14, 10, 12, 16, true, "Nobre");
            JSONObject ch = db.getCharacter(charId);
            JSONObject sheet = new JSONObject(ch.optString("sheet_json", "{}"));
            sheet.put("subclass_name", "Juramento de Vingança");
            sheet.put("rules_source", DndRulesEngine.PROFILE_2024);
            sheet = Core2024ClassRuntimeEngine.ensure(sheet, "Paladino", 3);
            Core2024SubclassRuntimeEngine.applyToSheet(sheet);
            db.updateCharacterSheet(charId, sheet.toString());

            JSONObject payload = new JSONObject().put("target_id", "dragon-1");
            JSONObject res = db.resolveCharacterHomebrewSubclassAction(
                charId, "Voto de Inimizade", payload);
            assertTrue("Vow of enmity must resolve: " + res,
                res.optBoolean("subclass_action_resolved"));
            assertEquals("paladin_vow_of_enmity", res.optString("subclass_action_event"));
            assertEquals("dragon-1", res.optString("vow_of_enmity_target"));

            // Verify channel divinity spent
            JSONObject updatedSheet = new JSONObject(db.getCharacter(charId).optString("sheet_json", "{}"));
            assertEquals(1, updatedSheet.optJSONObject("class_resources").optJSONObject("channel_divinity").optInt("current"));
        } finally {
            db.close();
            context.deleteDatabase(dbName);
        }
    }

    @Test
    public void shadowMonkShadowArtsDarknessAndShadowStep() throws Exception {
        JSONObject sheet = new JSONObject()
            .put("class_name", "Monge")
            .put("subclass_name", "Caminho das Sombras")
            .put("level", 6)
            .put("dex", 16)
            .put("wis", 16);
        sheet = Core2024ClassRuntimeEngine.ensure(sheet, "Monge", 6);
        Core2024SubclassRuntimeEngine.applyToSheet(sheet);

        JSONObject darkness = Core2024SubclassRuntimeEngine.resolve(
            sheet, "Caminho das Sombras", 6, "Artes das Sombras", null);
        assertTrue(darkness.optBoolean("subclass_action_resolved"));
        assertTrue(darkness.optBoolean("see_through_magical_darkness"));
        assertEquals(5, darkness.optJSONObject("class_resources").optJSONObject("focus_points").optInt("current"));

        JSONObject step = Core2024SubclassRuntimeEngine.resolve(
            darkness, "Caminho das Sombras", 6, "Passo das Sombras", null);
        assertTrue(step.optBoolean("subclass_action_resolved"));
        assertEquals(60, step.optJSONObject("subclass_action_effect").optInt("teleport_range_ft"));
    }

    @Test
    public void feyWandererRangerDreadfulStrikesAndOtherworldlyPresence() throws Exception {
        JSONObject sheet = new JSONObject()
            .put("class_name", "Guardião")
            .put("subclass_name", "Andarilho Feérico")
            .put("level", 3)
            .put("wis", 16);
        Core2024SubclassRuntimeEngine.applyToSheet(sheet);

        JSONObject strikes = Core2024SubclassRuntimeEngine.resolve(
            sheet, "Andarilho Feérico", 3, "Golpes Pavorosos", null);
        assertTrue(strikes.optBoolean("subclass_action_resolved"));
        assertEquals("1d4", strikes.optJSONObject("subclass_action_effect").optString("extra_damage_dice"));

        JSONObject glamour = Core2024SubclassRuntimeEngine.resolve(
            sheet, "Andarilho Feérico", 3, "Glamour Sobrenatural", null);
        assertTrue(glamour.optBoolean("subclass_action_resolved"));
        assertEquals(3, glamour.optInt("fey_otherworldly_presence_bonus"));
    }

    @Test
    public void soulknifeRoguePsychicBladesAndPsiBolsteredKnack() throws Exception {
        JSONObject sheet = new JSONObject()
            .put("class_name", "Ladino")
            .put("subclass_name", "Mente de Alma")
            .put("level", 3)
            .put("dex", 16);
        Core2024SubclassRuntimeEngine.applyToSheet(sheet);

        JSONObject blades = Core2024SubclassRuntimeEngine.resolve(
            sheet, "Mente de Alma", 3, "Lâminas Psíquicas", null);
        assertTrue(blades.optBoolean("subclass_action_resolved"));
        assertEquals("1d6", blades.optJSONObject("subclass_action_effect").optString("main_blade_damage"));

        JSONObject knack = Core2024SubclassRuntimeEngine.resolve(
            sheet, "Mente de Alma", 3, "Poder Psiônico", new JSONObject().put("die_roll", 4));
        assertTrue(knack.optBoolean("subclass_action_resolved"));
        assertEquals(4, knack.optJSONObject("subclass_action_effect").optInt("die_roll"));
        assertEquals(3, knack.optJSONObject("subclass_resources").optJSONObject("psionic_energy_dice").optInt("current")); // Started with 4 (2*PB), spent 1
    }

    @Test
    public void draconicSorcererResilienceAcAndElementalAffinity() throws Exception {
        JSONObject sheet = new JSONObject()
            .put("class_name", "Feiticeiro")
            .put("subclass_name", "Feitiçaria Dracônica")
            .put("level", 6)
            .put("dex", 14)
            .put("cha", 18);
        Core2024SubclassRuntimeEngine.applyToSheet(sheet);

        JSONObject res = Core2024SubclassRuntimeEngine.resolve(
            sheet, "Feitiçaria Dracônica", 6, "Resiliência Dracônica", null);
        assertTrue(res.optBoolean("subclass_action_resolved"));
        assertEquals(15, res.optInt("draconic_ac")); // 13 + 2 = 15
        assertEquals(6, res.optInt("draconic_hp_bonus"));

        JSONObject aff = Core2024SubclassRuntimeEngine.resolve(
            sheet, "Feitiçaria Dracônica", 6, "Afinidade Elemental", null);
        assertTrue(aff.optBoolean("subclass_action_resolved"));
        assertEquals(4, aff.optJSONObject("subclass_action_effect").optInt("elemental_damage_bonus"));
    }

    @Test
    public void celestialWarlockHealingLightSpendsPool() throws Exception {
        JSONObject sheet = new JSONObject()
            .put("class_name", "Bruxo")
            .put("subclass_name", "Patrono Celestial")
            .put("level", 3)
            .put("cha", 16);
        Core2024SubclassRuntimeEngine.applyToSheet(sheet);

        JSONObject payload = new JSONObject().put("dice_spent", 2).put("heal_roll", 9);
        JSONObject heal = Core2024SubclassRuntimeEngine.resolve(
            sheet, "Patrono Celestial", 3, "Luz Curativa", payload);
        assertTrue(heal.optBoolean("subclass_action_resolved"));
        assertEquals(9, heal.optJSONObject("subclass_action_effect").optInt("healing_amount"));
        assertEquals(2, heal.optJSONObject("subclass_resources").optJSONObject("healing_light").optInt("current")); // Started with 4 (1+3), spent 2
    }

    @Test
    public void abjurationWizardArcaneWardAndProjectedWard() throws Exception {
        JSONObject sheet = new JSONObject()
            .put("class_name", "Mago")
            .put("subclass_name", "Escola de Abjuração")
            .put("level", 6)
            .put("int_score", 18);
        Core2024SubclassRuntimeEngine.applyToSheet(sheet);

        JSONObject ward = Core2024SubclassRuntimeEngine.resolve(
            sheet, "Escola de Abjuração", 6, "Proteção Arcana", null);
        assertTrue(ward.optBoolean("subclass_action_resolved"));
        assertEquals(16, ward.optInt("arcane_ward_max_hp")); // 2*6 + 4 = 16

        JSONObject proj = Core2024SubclassRuntimeEngine.resolve(
            sheet, "Escola de Abjuração", 6, "Proteção Projetada", new JSONObject().put("ally_id", "fighter-1"));
        assertTrue(proj.optBoolean("subclass_action_resolved"));
        assertEquals("fighter-1", proj.optJSONObject("subclass_action_effect").optString("protected_ally_id"));
    }

    @Test
    public void divinationWizardPortentSubstitution() throws Exception {
        JSONObject sheet = new JSONObject()
            .put("class_name", "Mago")
            .put("subclass_name", "Escola de Adivinhação")
            .put("level", 3);
        Core2024SubclassRuntimeEngine.applyToSheet(sheet);

        JSONObject portent = Core2024SubclassRuntimeEngine.resolve(
            sheet, "Escola de Adivinhação", 3, "Presságio", new JSONObject().put("portent_roll", 18));
        assertTrue(portent.optBoolean("subclass_action_resolved"));
        assertEquals(18, portent.optJSONObject("subclass_action_effect").optInt("substituted_roll"));
        assertEquals(1, portent.optJSONObject("subclass_resources").optJSONObject("portent_dice").optInt("current")); // Started with 2, spent 1
    }
}
