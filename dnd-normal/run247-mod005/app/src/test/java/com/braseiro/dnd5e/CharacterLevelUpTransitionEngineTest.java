package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/** Focused G07 tests for XP gating, fixed HP, queued choices, and multiclass prerequisites. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public class CharacterLevelUpTransitionEngineTest {
    private static JSONObject sheet(String className, int level, int xp) throws Exception {
        return new JSONObject()
                .put("name", "Ada")
                .put("class_name", className)
                .put("level", level)
                .put("xp", xp)
                .put("hp", 8)
                .put("max_hp", 8)
                .put("con", 14)
                .put("str", 14)
                .put("dex", 14)
                .put("int_score", 12)
                .put("wis", 10)
                .put("cha", 10)
                .put("class_levels", new JSONArray().put(
                        new JSONObject().put("class_name", className).put("level", level)));
    }

    @Test public void xpBoundaryAppliesExactlyOneLevelAndQueuesChoice() throws Exception {
        JSONObject before = sheet("Wizard", 1, 299);
        String unchanged = before.toString();
        JSONObject denied = CharacterLevelUpTransitionEngine.apply(before, "Wizard", 6, 2, true, false);
        assertFalse(denied.optBoolean("accepted"));
        assertEquals("xp_threshold_not_reached", denied.optString("level_up_blocked"));
        assertEquals(unchanged, before.toString());

        JSONObject due = sheet("Wizard", 1, 300);
        JSONObject applied = CharacterLevelUpTransitionEngine.apply(due, "Wizard", 6, 2, true, false);
        assertTrue(applied.toString(), applied.optBoolean("accepted"));
        assertEquals(2, applied.optInt("to_level"));
        assertEquals(6, applied.optInt("hp_gain")); // d6 fixed average (4) + CON modifier (+2)

        JSONObject next = applied.getJSONObject("state_after");
        assertEquals(14, next.optInt("max_hp"));
        assertTrue(next.optBoolean("level_up_choices_pending"));
        JSONArray pending = next.getJSONArray("pending_level_up_choices");
        assertEquals(1, pending.length());
        assertEquals(2, pending.getJSONObject(0).optInt("level"));
        assertEquals("wizard", pending.getJSONObject(0).optString("class"));
    }

    @Test public void multiclassChoiceRequiresItsAbilityPrerequisiteAndDoesNotMutateOnRefusal()
            throws Exception {
        JSONObject fighter = sheet("Fighter", 1, 300);
        String unchanged = fighter.toString();

        JSONObject denied = CharacterLevelUpTransitionEngine.apply(
                fighter, "Wizard", 6, 2, true, true);
        assertFalse(denied.optBoolean("accepted"));
        assertEquals("level_up_invalid:multiclass_intelligence_13_required",
                denied.optString("level_up_blocked"));
        assertEquals(unchanged, fighter.toString());

        fighter.put("int_score", 13);
        JSONObject applied = CharacterLevelUpTransitionEngine.apply(
                fighter, "Wizard", 6, 2, true, true);
        assertTrue(applied.toString(), applied.optBoolean("accepted"));
        JSONArray classes = applied.getJSONObject("state_after").getJSONArray("class_levels");
        assertEquals(2, classes.length());
        assertEquals("wizard", classes.getJSONObject(1).optString("class_name"));
    }

    @Test public void laterLevelUpAppendsPendingChoiceWithoutOverwritingEarlierChoice()
            throws Exception {
        JSONObject first = CharacterLevelUpTransitionEngine.apply(
                sheet("Wizard", 1, 900), "Wizard", 6, 2, true, false);
        assertTrue(first.toString(), first.optBoolean("accepted"));

        JSONObject afterFirst = first.getJSONObject("state_after");
        JSONObject second = CharacterLevelUpTransitionEngine.apply(
                afterFirst, "Wizard", 6, 2, true, false);
        assertTrue(second.toString(), second.optBoolean("accepted"));
        JSONArray pending = second.getJSONObject("state_after")
                .getJSONArray("pending_level_up_choices");
        assertEquals(2, pending.length());
        assertEquals(2, pending.getJSONObject(0).optInt("level"));
        assertEquals(3, pending.getJSONObject(1).optInt("level"));
    }

    @Test public void levelTwentyAndWrongHitDieAreRefusedWithoutChangingInput() throws Exception {
        JSONObject max = sheet("Wizard", 20, 355000);
        String maxBefore = max.toString();
        JSONObject deniedMax = CharacterLevelUpTransitionEngine.apply(max, "Wizard", 6, 2, true, false);
        assertFalse(deniedMax.optBoolean("accepted"));
        assertEquals("maximum_or_invalid_level", deniedMax.optString("level_up_blocked"));
        assertEquals(maxBefore, max.toString());

        JSONObject levelOne = sheet("Wizard", 1, 300);
        String before = levelOne.toString();
        JSONObject deniedDie = CharacterLevelUpTransitionEngine.apply(
                levelOne, "Wizard", 8, 2, true, false);
        assertFalse(deniedDie.optBoolean("accepted"));
        assertEquals("class_hit_die_mismatch", deniedDie.optString("level_up_blocked"));
        assertEquals(before, levelOne.toString());
    }
}
