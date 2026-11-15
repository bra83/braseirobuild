package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/** The first ready-to-import adventure must use the ALREADY SHIPPED adventure/v1 host. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public final class FaerunSeasonAdventureImportTest {
    private JSONObject source() throws Exception {
        String file = "faerun_seasons/adventures/as_mulas_voltaram_v1.json";
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                RuntimeEnvironment.getApplication().getAssets().open(file),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line=reader.readLine()) != null) text.append(line).append('\n');
        }
        return new JSONObject(text.toString());
    }

    @Test public void importsThroughExistingAdventureContract() throws Exception {
        JSONObject adventure = source();
        BraseiroAdventureStore.validatePackage(adventure);
        assertEquals("braseiro.adventure/v1", adventure.getString("schema"));
        assertEquals("dnd5e-2024", adventure.getString("system"));
        assertEquals("faerun-campanha-principal", adventure.getString("worldId"));
        assertEquals("yartar_dm28", adventure.getJSONObject("start").getString("locationId"));
        assertEquals("DM28", adventure.getJSONObject("canonicalGeography")
                .getString("start_verified_hex"));
        assertEquals(5, adventure.getJSONArray("locations").length());
        assertEquals(4, adventure.getJSONArray("npcs").length());
        assertEquals(6, adventure.getJSONArray("encounters").length());
        assertEquals(4, adventure.getJSONArray("secrets").length());
    }

    @Test public void noCombatRewardOrSuccessIsHardcoded() throws Exception {
        JSONObject adventure = source();
        JSONObject restrictions=adventure.getJSONObject("masterRuntimeRestrictions");
        for (String rule : new String[] {
                "noAutomaticTests", "noAutomaticLoot", "noAutomaticXP",
                "noAutomaticTravel", "noAutomaticNPCSpawns", "noPredeclaredSuccess",
                "allowPlayerDecline", "keepWorldStateTransactionAuthority" })
            assertTrue(rule, restrictions.getBoolean(rule));
        assertEquals(0, adventure.optJSONArray("enemies")==null
                ? 0 : adventure.getJSONArray("enemies").length());
        assertTrue(adventure.getJSONObject("start").getBoolean("notAccepted"));
        for (int i=0;i<adventure.getJSONArray("encounters").length();i++)
            assertFalse(adventure.getJSONArray("encounters")
                    .getJSONObject(i).getBoolean("autoresolve"));
    }

    @Test public void secretsStayGmOnlyAndCannotBeLeakedByImportSchema() throws Exception {
        JSONObject adventure = source();
        JSONArray secrets = adventure.getJSONArray("secrets");
        for (int i=0;i<secrets.length();i++)
            assertEquals("gm_only", secrets.getJSONObject(i).getString("visibility"));

        JSONObject invalid = new JSONObject(adventure.toString());
        invalid.getJSONArray("secrets").getJSONObject(0)
                .put("visibility", "public");
        try {
            BraseiroAdventureStore.validatePackage(invalid);
            fail("Existing adventure validator accepted GM secret leak");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("gm_only"));
        }
    }
}
