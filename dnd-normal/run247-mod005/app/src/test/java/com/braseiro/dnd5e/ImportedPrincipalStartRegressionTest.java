package com.braseiro.dnd5e;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class ImportedPrincipalStartRegressionTest {
    @Test public void declaredStartWinsOverOtherCatalogLocations() throws Exception{
        JSONObject pkg=new JSONObject("{\"id\":\"principal\",\"start\":{\"locationId\":\"inn\"},\"locations\":[{\"id\":\"city\",\"atlasHex\":\"B12\"},{\"id\":\"inn\",\"name\":\"Marco do Percurso\",\"atlasHex\":\"DO40\"}]}");
        JSONObject resolved=BraseiroAdventureStore.resolvePrincipalStart(pkg);
        assertEquals("DO40",resolved.getString("hex_id"));
        assertEquals("Marco do Percurso",resolved.getString("location_name"));
    }
    @Test public void sublocationInheritsOnlyExplicitParent() throws Exception{
        JSONObject pkg=new JSONObject("{\"start\":{\"locationId\":\"room\"},\"locations\":[{\"id\":\"city\",\"atlasHex\":\"B12\"},{\"id\":\"room\",\"name\":\"Quarto\",\"parentPlaceId\":\"inn\"},{\"id\":\"inn\",\"atlasHex\":\"DO40\"}]}");
        JSONObject resolved=BraseiroAdventureStore.resolvePrincipalStart(pkg);
        assertEquals("DO40",resolved.getString("hex_id"));
        assertEquals("Quarto",resolved.getString("location_name"));
    }
    @Test public void missingHexNeverRandomizes() throws Exception{
        JSONObject pkg=new JSONObject("{\"start\":{\"locationId\":\"inn\"},\"locations\":[{\"id\":\"other\",\"atlasHex\":\"B12\"},{\"id\":\"inn\",\"atlasHex\":null}]}");
        assertEquals("",BraseiroAdventureStore.resolvePrincipalStart(pkg).getString("hex_id"));
        assertEquals(0,BraseiroAdventureStore.resolvePrincipalStart(new JSONObject()).length());
    }
}
