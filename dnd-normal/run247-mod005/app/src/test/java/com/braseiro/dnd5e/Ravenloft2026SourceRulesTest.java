package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Source-checked 2026 supplement: Ravenloft: The Horrors Within (RHW).
 * 5etools-mirror-3/5etools-src data/feats.json, immutable Git blob
 * 99b18fd68c050cbc6b5a8d2e1f6da3a5c175ec08, Gathered Whispers,
 * page 33, Unearthly Scream: reaction trigger requires a HIT, not a miss.
 * Species: races.json blob 853a6e0f5b25075d826fd005e45276d1e91fe328,
 * Dhampir Vampiric Bite requires an eligible creature and actual damage.
 * The reference files are QA inputs only; no rule text is embedded.
 */
public final class Ravenloft2026SourceRulesTest {
    private final RavenloftHorrorsWithinRulesEngine engine =
            new RavenloftHorrorsWithinRulesEngine();
    private JSONObject character(int level) throws Exception {
        return new JSONObject().put("level",level).put("con",14);
    }

    @Test public void unearthlyScreamCannotTriggerAgainstAttackThatAlreadyMissed() throws Exception {
        JSONObject in=new JSONObject().put("attack_total",14).put("base_ac",15);
        JSONObject result=engine.resolveCharacterAction(
                "gathered_whispers_scream",in,character(5),new JSONObject());
        assertFalse("RHW page 33: reaction only when an attack HITS",result.optBoolean("resolved"));
        assertEquals("requires_attack_roll_that_hit_before_reaction",result.optString("reason"));
        assertFalse(result.has("state_after"));
    }

    @Test public void unearthlyScreamCanChangeHitIntoMissOnceAndSpendOneUse() throws Exception {
        JSONObject incoming=new JSONObject().put("attack_total",16).put("base_ac",15);
        JSONObject first=engine.resolveCharacterAction(
                "gathered_whispers_scream",incoming,character(5),new JSONObject());
        assertTrue(first.optBoolean("resolved"));
        assertTrue(first.optBoolean("triggering_attack_hit_before_reaction"));
        assertEquals(18,first.optInt("adjusted_ac"));
        assertTrue(first.optBoolean("attack_misses"));
        assertTrue(first.optBoolean("reaction_required"));
        JSONObject state=first.getJSONObject("state_after");
        assertEquals(1,state.optInt("gathered_whispers_scream_uses"));
        assertFalse(first.optBoolean("auto_rolls",true));
        JSONObject next=engine.resolveCharacterAction(
                "gathered_whispers_scream",
                new JSONObject().put("attack_total",20).put("base_ac",15),
                character(5),state);
        assertTrue(next.optBoolean("resolved"));
        assertFalse(next.optBoolean("attack_misses"));
        assertEquals(2,next.getJSONObject("state_after")
                .optInt("gathered_whispers_scream_uses"));
    }

    @Test public void dhampirBiteRequiresPositiveDamageAndSpecifiedEligibleCreature() throws Exception {
        JSONObject base=new JSONObject().put("choice","drain")
                .put("target_creature_type","humanoid").put("piercing_damage",4);
        JSONObject badDamage=engine.resolveCharacterAction(
                "dhampir_bite_empower",
                new JSONObject(base.toString()).put("piercing_damage",0),
                character(4),new JSONObject());
        assertFalse(badDamage.optBoolean("resolved"));
        assertFalse(badDamage.has("state_after"));
        JSONObject noType=engine.resolveCharacterAction(
                "dhampir_bite_empower",
                new JSONObject(base.toString()).put("target_creature_type",""),
                character(4),new JSONObject());
        assertFalse(noType.optBoolean("resolved"));
        JSONObject good=engine.resolveCharacterAction(
                "dhampir_bite_empower",base,character(4),new JSONObject());
        assertTrue(good.optBoolean("resolved"));
        assertEquals(4,good.optInt("healing"));
        assertEquals(1,good.getJSONObject("state_after")
                .optInt("dhampir_bite_empower_uses"));
    }
}
