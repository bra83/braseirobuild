package com.braseiro.dnd5e;

import static org.junit.Assert.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Actual source-backed XMM arithmetic and safe batch dispatch without a DB write. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class HybridXmmBatchResolutionEngineTest {
    private static JSONObject input(String id, String monster, String ptr,
                                    String operation, JSONObject values) throws Exception {
        return new JSONObject().put("request_id", id)
                .put("book_id", "xmm").put("edition", "2025")
                .put("monster", monster).put("source_pointer", ptr)
                .put("operation", operation).put("inputs", values);
    }
    private static JSONObject packet(JSONArray rows) throws Exception {
        return new JSONObject().put("origin", "gemini")
                .put("source_blob_sha1", Xmm2025SourceIndex.SOURCE_BLOB)
                .put("requests", rows);
    }
    private JSONObject gateway(JSONObject packet) {
        return new OfficialBookRuntimeGateway(null).execute("xmm",
                "hybrid_batch_preview", packet);
    }
    private static JSONObject batch(JSONObject wrapper) {
        return wrapper.optJSONObject("result");
    }

    @Test
    public void twoFamiliesUsePinnedSaveDamageAndTraitExecutorsWithoutCommit()
            throws Exception {
        JSONObject breath = input("breath", "Abominable Yeti",
                "/monster/4/action/4/entries/0", "save_damage_resolve",
                new JSONObject().put("category", "action").put("action", "Cold Breath")
                        .put("target_confirmed", true).put("recharge_ready_confirmed", true)
                        .put("save_total", 18).put("damage_mode", "average"));
        JSONObject resistance = input("trait", "Aboleth",
                "/monster/3/trait/2/entries/0", "trait_resolve",
                new JSONObject().put("trait", "Legendary Resistance (3/Day, or 4/Day in Lair)")
                        .put("in_lair", false).put("save_failed", true)
                        .put("choose_to_use", true).put("uses_remaining", 3));
        JSONObject result = gateway(packet(new JSONArray().put(breath).put(resistance)));
        assertTrue(result.optBoolean("executed"));
        JSONObject report = batch(result);
        assertEquals(2, report.optInt("resolved_count"));
        assertEquals(0, report.optInt("blocked_count"));
        assertFalse(report.optBoolean("auto_mutation_allowed", true));
        assertFalse(report.optBoolean("persisted", true));
        JSONObject save = report.getJSONArray("results").getJSONObject(0)
                .getJSONObject("runtime_result");
        assertEquals(22, save.optInt("damage_before_defenses"));
        assertTrue(save.optBoolean("save_success"));
        assertFalse(save.optBoolean("damage_applied", true));
        JSONObject trait = report.getJSONArray("results").getJSONObject(1)
                .getJSONObject("runtime_result");
        assertEquals(2, trait.optInt("uses_remaining_proposed"));
        assertFalse(trait.optBoolean("resource_consumed", true));
    }

    @Test
    public void rejectsWrongEditionForeignSourceAndUnsignedMutation() throws Exception {
        JSONObject valid = input("a", "Aboleth", "/monster/3",
                "source_reference", new JSONObject());
        JSONObject wrongEdition = new JSONObject(valid.toString()).put("request_id", "b")
                .put("edition", "2014");
        JSONObject wrongSource = new JSONObject(valid.toString()).put("request_id", "c")
                .put("source_pointer", "/monster/4");
        JSONObject mutation = new JSONObject(valid.toString()).put("request_id", "d")
                .put("inputs", new JSONObject().put("state_updates", new JSONArray()));
        JSONObject raw = packet(new JSONArray().put(valid).put(wrongEdition)
                .put(wrongSource).put(mutation));
        String original = raw.toString();
        JSONObject result = batch(gateway(raw));
        assertEquals(1, result.optInt("resolved_count"));
        assertEquals(3, result.optInt("blocked_count"));
        assertEquals(original, raw.toString());
        assertEquals("wrong_book_or_rules_edition",
                result.getJSONArray("results").getJSONObject(1).optString("reason"));
    }

    @Test
    public void rejectsDuplicateIdsAndDisallowedOperationsButRetainsOtherResults()
            throws Exception {
        JSONObject one = input("dup", "Aboleth", "/monster/3",
                "source_reference", new JSONObject());
        JSONObject duplicate = new JSONObject(one.toString());
        JSONObject disallowed = input("commit", "Aboleth", "/monster/3",
                "bestiary_install", new JSONObject());
        JSONObject result = batch(gateway(packet(new JSONArray()
                .put(one).put(duplicate).put(disallowed))));
        assertEquals(1, result.optInt("resolved_count"));
        assertEquals(2, result.optInt("blocked_count"));
        assertEquals("missing_or_duplicate_request_id",
                result.getJSONArray("results").getJSONObject(1).optString("reason"));
        assertEquals("unapproved_runtime_operation",
                result.getJSONArray("results").getJSONObject(2).optString("reason"));
    }

    @Test
    public void refusesBadBatchPinAndUnauthorizedTopLevelCommands() throws Exception {
        JSONObject one = input("one", "Aboleth", "/monster/3",
                "source_reference", new JSONObject());
        JSONObject envelope = packet(new JSONArray().put(one));
        assertFalse(batch(gateway(new JSONObject(envelope.toString())
                .put("source_blob_sha1", "wrong"))).optBoolean("evaluated"));
        assertFalse(batch(gateway(new JSONObject(envelope.toString())
                .put("combat", new JSONObject()))).optBoolean("evaluated"));
        JSONArray many = new JSONArray();
        for (int i = 0; i <= HybridXmmBatchResolutionEngine.MAX_REQUESTS; i++) {
            many.put(new JSONObject(one.toString()).put("request_id", "id-" + i));
        }
        assertFalse(batch(gateway(packet(many))).optBoolean("evaluated"));
        assertFalse(batch(gateway(packet(new JSONArray()))).optBoolean("evaluated"));
    }
}
