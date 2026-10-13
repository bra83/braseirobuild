package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** RHW 2026 / Touch of Death (page 35, 5etools data/feats.json
 * Git blob 99b18fd68c050cbc6b5a8d2e1f6da3a5c175ec08).
 * Tests actual manual spell/death-save behavior; no hidden dice or mutation.
 */
public final class Ravenloft2026TouchOfDeathSourceTest {
    private final RavenloftHorrorsWithinRulesEngine engine =
            new RavenloftHorrorsWithinRulesEngine();
    private JSONObject character() throws Exception {
        return new JSONObject().put("level",5).put("wis",14);
    }

    @Test public void chillTouchBypassesNecroticResistanceOnlyAfterExplicitCast() throws Exception {
        JSONObject blocked=engine.resolveCharacterAction(
                "touch_of_death_chill_touch",new JSONObject(),character(),new JSONObject());
        assertFalse(blocked.optBoolean("resolved"));
        assertEquals("spell_cast_confirmation_required",blocked.optString("reason"));

        JSONObject cast=engine.resolveCharacterAction(
                "touch_of_death_chill_touch",
                new JSONObject().put("spell_cast_confirmed",true),character(),new JSONObject());
        assertTrue(cast.optBoolean("resolved"));
        assertEquals("Chill Touch",cast.optString("spell"));
        assertFalse(cast.optBoolean("spell_components_required",true));
        assertTrue(cast.optBoolean("necrotic_damage_ignores_resistance"));
        assertFalse(cast.optBoolean("auto_rolls",true));
    }

    @Test public void twoExplicitD20RollsUseLowerForDisadvantagedDeathSave() throws Exception {
        JSONObject result=engine.resolveCharacterAction(
                "touch_of_death_death_save",
                new JSONObject().put("first_d20",18).put("second_d20",3),
                character(),new JSONObject());
        assertTrue(result.optBoolean("resolved"));
        assertTrue(result.optBoolean("death_save_disadvantage"));
        assertEquals(3,result.optInt("selected_d20"));
        assertTrue(result.optBoolean("requires_core_death_save_application"));
        assertFalse(result.optBoolean("auto_rolls",true));
    }

    @Test public void advantageCancelsDisadvantageAndRequiresBothManualRolls() throws Exception {
        JSONObject incomplete=engine.resolveCharacterAction(
                "touch_of_death_death_save",
                new JSONObject().put("first_d20",12),character(),new JSONObject());
        assertFalse(incomplete.optBoolean("resolved"));

        JSONObject cancel=engine.resolveCharacterAction(
                "touch_of_death_death_save",
                new JSONObject().put("first_d20",12).put("second_d20",2)
                    .put("has_advantage",true),
                character(),new JSONObject());
        assertTrue(cancel.optBoolean("resolved"));
        assertTrue(cancel.optBoolean("advantage_cancels_disadvantage"));
        assertEquals(12,cancel.optInt("selected_d20"));

        JSONObject invalid=engine.resolveCharacterAction(
                "touch_of_death_death_save",
                new JSONObject().put("first_d20",21).put("second_d20",1),
                character(),new JSONObject());
        assertFalse(invalid.optBoolean("resolved"));
    }
}
