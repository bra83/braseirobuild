package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/** Focused G10 tests for keeping curated companion intentions mechanically isolated. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public class CompanionProposalIsolationRulesTest {
    private static Set<Long> curated(long id) {
        Set<Long> ids = new HashSet<Long>();
        ids.add(id);
        return ids;
    }

    private static JSONObject envelope(long id) throws Exception {
        return new JSONObject().put("companion_proposals", new JSONArray()
                .put(new JSONObject().put("character_id", id)));
    }

    @Test public void curatedProposalCannotCarryStateMutation() throws Exception {
        JSONObject input = envelope(9).put("state_updates", new JSONArray().put(
                new JSONObject().put("character_id", 9).put("hp", 1)));
        assertEquals("curated_intention_mixed_with_state_updates",
                CompanionProposalIsolationRules.reject(input, curated(9)));
    }

    @Test public void curatedProposalCannotAdvanceLocalWorldTick() throws Exception {
        JSONObject input = envelope(9).put("_local_tick", new JSONObject().put("hours_advanced", 1));
        assertEquals("curated_intention_mixed_with_local_tick",
                CompanionProposalIsolationRules.reject(input, curated(9)));
    }

    @Test public void effectsForNonCuratedCompanionAreOutsideThisIsolationRule() throws Exception {
        JSONObject input = envelope(10).put("state_updates", new JSONArray().put(
                new JSONObject().put("character_id", 10).put("hp", 1)));
        assertEquals("", CompanionProposalIsolationRules.reject(input, curated(9)));
    }

    @Test public void pureCuratedIntentionAndEmptyCuratedSetAreAllowed() throws Exception {
        assertEquals("", CompanionProposalIsolationRules.reject(envelope(9), curated(9)));
        assertEquals("", CompanionProposalIsolationRules.reject(envelope(9),
                Collections.<Long>emptySet()));
    }
}
