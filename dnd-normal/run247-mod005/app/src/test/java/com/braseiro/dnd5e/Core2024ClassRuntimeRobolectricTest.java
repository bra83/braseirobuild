package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class Core2024ClassRuntimeRobolectricTest {

    private static final String[] CLASSES_PT = {
            "Bárbaro", "Bardo", "Bruxo", "Clérigo", "Druida", "Feiticeiro",
            "Guardião", "Guerreiro", "Ladino", "Mago", "Monge", "Paladino"
    };

    private static final String[] CLASSES_EN = {
            "Barbarian", "Bard", "Warlock", "Cleric", "Druid", "Sorcerer",
            "Ranger", "Fighter", "Rogue", "Wizard", "Monk", "Paladin"
    };

    @Test
    public void testSupportsAll12ClassesInBothLanguages() {
        for (String c : CLASSES_PT) {
            assertTrue("Deveria suportar " + c, Core2024ClassRuntimeEngine.supports(c));
        }
        for (String c : CLASSES_EN) {
            assertTrue("Deveria suportar " + c, Core2024ClassRuntimeEngine.supports(c));
        }
        assertFalse(Core2024ClassRuntimeEngine.supports("UnknownClassXYZ"));
    }

    @Test
    public void testActionsForReturnsCoreOptionsForAll12Classes() {
        // Barbarian
        JSONArray barbActs = Core2024ClassRuntimeEngine.actionsFor("Bárbaro", 11);
        assertTrue(contains(barbActs, "Fúria"));
        assertTrue(contains(barbActs, "Fúria Implacável"));

        // Fighter
        JSONArray fightActs = Core2024ClassRuntimeEngine.actionsFor("Guerreiro", 9);
        assertTrue(contains(fightActs, "Retomar o Fôlego"));
        assertTrue(contains(fightActs, "Surto de Ação"));
        assertTrue(contains(fightActs, "Mente Tática"));
        assertTrue(contains(fightActs, "Indomável"));

        // Bard
        JSONArray bardActs = Core2024ClassRuntimeEngine.actionsFor("Bardo", 5);
        assertTrue(contains(bardActs, "Inspiração de Bardo"));
        assertTrue(contains(bardActs, "Fonte de Inspiração"));

        // Cleric
        JSONArray clericActs = Core2024ClassRuntimeEngine.actionsFor("Clérigo", 10);
        assertTrue(contains(clericActs, "Canalizar Divindade: Expulsar Mortos-Vivos"));
        assertTrue(contains(clericActs, "Canalizar Divindade: Centelha Divina"));
        assertTrue(contains(clericActs, "Intervenção Divina"));

        // Druid
        JSONArray druidActs = Core2024ClassRuntimeEngine.actionsFor("Druida", 5);
        assertTrue(contains(druidActs, "Forma Selvagem"));
        assertTrue(contains(druidActs, "Ressurgimento Selvagem Espaço para Forma"));

        // Monk
        JSONArray monkActs = Core2024ClassRuntimeEngine.actionsFor("Monge", 5);
        assertTrue(contains(monkActs, "Rajada de Golpes"));
        assertTrue(contains(monkActs, "Defesa Paciente"));
        assertTrue(contains(monkActs, "Passo do Vento"));
        assertTrue(contains(monkActs, "Metabolismo Incomum"));
        assertTrue(contains(monkActs, "Ataque Atordoante"));

        // Paladin
        JSONArray palActs = Core2024ClassRuntimeEngine.actionsFor("Paladino", 5);
        assertTrue(contains(palActs, "Imposição das Mãos"));
        assertTrue(contains(palActs, "Golpe Divino Gratuito"));
        assertTrue(contains(palActs, "Canalizar Divindade"));
        assertTrue(contains(palActs, "Convocar Montaria Gratuito"));

        // Ranger
        JSONArray rngActs = Core2024ClassRuntimeEngine.actionsFor("Guardião", 5);
        assertTrue(contains(rngActs, "Marca do Predador Gratuita"));

        // Rogue
        JSONArray rogActs = Core2024ClassRuntimeEngine.actionsFor("Ladino", 5);
        assertTrue(contains(rogActs, "Golpe Astuto"));

        // Sorcerer
        JSONArray sorcActs = Core2024ClassRuntimeEngine.actionsFor("Feiticeiro", 7);
        assertTrue(contains(sorcActs, "Feitiçaria Inata"));
        assertTrue(contains(sorcActs, "Criar Espaço de Magia"));
        assertTrue(contains(sorcActs, "Converter Espaço em Pontos"));
        assertTrue(contains(sorcActs, "Restauração Feiticeira"));
        assertTrue(contains(sorcActs, "Feitiçaria Inata com Pontos"));

        // Warlock
        JSONArray warActs = Core2024ClassRuntimeEngine.actionsFor("Bruxo", 2);
        assertTrue(contains(warActs, "Astúcia Mágica"));

        // Wizard
        JSONArray wizActs = Core2024ClassRuntimeEngine.actionsFor("Mago", 1);
        assertTrue(contains(wizActs, "Recuperação Arcana"));
    }

    @Test
    public void testBarbarianRageAndRelentlessRage() throws Exception {
        JSONObject sheet = new JSONObject().put("hp", 0).put("max_hp", 80);
        sheet = Core2024ClassRuntimeEngine.ensure(sheet, "Bárbaro", 11);

        // Rage
        JSONObject rageRes = Core2024ClassRuntimeEngine.execute(sheet, "Bárbaro", 11, "Fúria", null);
        assertTrue(rageRes.optBoolean("class_action_resolved"));
        assertEquals("barbarian_rage", rageRes.optString("class_action_event"));
        assertTrue(rageRes.optJSONObject("class_action_effect").optBoolean("rage_active"));
        assertEquals(3, rageRes.optJSONObject("class_action_effect").optInt("damage_bonus"));

        // Relentless Rage - fails Con save
        JSONObject failSave = Core2024ClassRuntimeEngine.execute(sheet, "Bárbaro", 11, "Fúria Implacável",
                new JSONObject().put("con_save_roll", 8));
        assertTrue(failSave.optBoolean("class_action_resolved"));
        assertFalse(failSave.optJSONObject("class_action_effect").optBoolean("success"));
        assertEquals(0, failSave.optInt("hp"));

        // Relentless Rage - succeeds Con save
        JSONObject passSave = Core2024ClassRuntimeEngine.execute(sheet, "Bárbaro", 11, "Fúria Implacável",
                new JSONObject().put("con_save_roll", 15));
        assertTrue(passSave.optBoolean("class_action_resolved"));
        assertTrue(passSave.optJSONObject("class_action_effect").optBoolean("success"));
        assertEquals(1, passSave.optInt("hp"));
        assertEquals(15, passSave.optJSONObject("class_action_effect").optInt("next_dc"));
    }

    @Test
    public void testFighterSecondWindAndActionSurge() throws Exception {
        JSONObject sheet = new JSONObject().put("hp", 10).put("max_hp", 50);
        sheet = Core2024ClassRuntimeEngine.ensure(sheet, "Guerreiro", 5);

        // Second wind requires d10_roll
        JSONObject pending = Core2024ClassRuntimeEngine.execute(sheet, "Guerreiro", 5, "Retomar o Fôlego", new JSONObject());
        assertFalse(pending.optBoolean("class_action_resolved"));
        assertEquals("requires_player_or_gm_resolution", pending.optString("class_action_status"));

        // Second wind with roll=7: heals 7 + 5 = 12
        JSONObject healed = Core2024ClassRuntimeEngine.execute(sheet, "Guerreiro", 5, "Retomar o Fôlego",
                new JSONObject().put("d10_roll", 7));
        assertTrue(healed.optBoolean("class_action_resolved"));
        assertEquals(22, healed.optInt("hp"));
        assertEquals(12, healed.optJSONObject("class_action_effect").optInt("healing"));

        // Action Surge
        JSONObject surge = Core2024ClassRuntimeEngine.execute(sheet, "Guerreiro", 5, "Surto de Ação", null);
        assertTrue(surge.optBoolean("class_action_resolved"));
        assertEquals("fighter_action_surge", surge.optString("class_action_event"));
    }

    @Test
    public void testMonkFlurryPatientDefenseAndStunningStrike() throws Exception {
        JSONObject sheet = new JSONObject().put("hp", 30).put("max_hp", 30);
        sheet = Core2024ClassRuntimeEngine.ensure(sheet, "Monge", 5);

        // Spend focus on Flurry of Blows
        JSONObject flurry = Core2024ClassRuntimeEngine.execute(sheet, "Monge", 5, "Rajada de Golpes", null);
        assertTrue(flurry.optBoolean("class_action_resolved"));
        assertEquals(2, flurry.optJSONObject("class_action_effect").optInt("unarmed_strikes"));

        // Spend focus on Stunning Strike
        JSONObject stun = Core2024ClassRuntimeEngine.execute(flurry, "Monge", 5, "Ataque Atordoante", null);
        assertTrue(stun.optBoolean("class_action_resolved"));
        assertEquals("Stunned", stun.optJSONObject("class_action_effect").optString("condition"));

        // Focus points should have dropped from 5 to 3
        int remainingFocus = stun.getJSONObject("class_resources").getJSONObject("focus_points").getInt("current");
        assertEquals(3, remainingFocus);

        // Short rest restores all focus points
        JSONObject rested = Core2024ClassRuntimeEngine.shortRest(stun, "Monge", 5);
        int restoredFocus = rested.getJSONObject("class_resources").getJSONObject("focus_points").getInt("current");
        assertEquals(5, restoredFocus);
    }

    @Test
    public void testSorcererFontOfMagicCreationAndConversion() throws Exception {
        JSONObject sheet = new JSONObject()
                .put("spell_slots_current", new JSONArray().put(4).put(3).put(2))
                .put("spell_slots_max", new JSONArray().put(4).put(3).put(2));
        sheet = Core2024ClassRuntimeEngine.ensure(sheet, "Feiticeiro", 5);

        // Innate Sorcery
        JSONObject innate = Core2024ClassRuntimeEngine.execute(sheet, "Feiticeiro", 5, "Feitiçaria Inata", null);
        assertTrue(innate.optBoolean("class_action_resolved"));
        assertTrue(innate.optJSONObject("class_action_effect").optBoolean("active"));

        // Create 1st-level slot (costs 2 sorcery points)
        // First spend one slot so there is room
        innate.getJSONArray("spell_slots_current").put(0, 3);
        JSONObject create = Core2024ClassRuntimeEngine.execute(innate, "Feiticeiro", 5, "Criar Espaço de Magia",
                new JSONObject().put("spell_slot_level", 1));
        assertTrue(create.optBoolean("class_action_resolved"));
        assertEquals(4, create.getJSONArray("spell_slots_current").getInt(0));
        assertEquals(2, create.optJSONObject("class_action_effect").optInt("points_spent"));
    }

    private static boolean contains(JSONArray arr, String val) {
        if (arr == null) return false;
        for (int i = 0; i < arr.length(); i++) {
            if (val.equals(arr.optString(i))) return true;
        }
        return false;
    }
}
