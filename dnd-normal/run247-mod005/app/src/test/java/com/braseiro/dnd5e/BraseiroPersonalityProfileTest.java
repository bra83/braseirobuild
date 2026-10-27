package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public class BraseiroPersonalityProfileTest {
    private JSONObject example() throws Exception {
        return new JSONObject().put("schema","braseiro.personality/v1")
            .put("version",1).put("agency","manual_player_choices_only")
            .put("traits",new JSONArray()
                .put(new JSONObject().put("id","orgulho_ferido")
                    .put("label","Orgulho ferido").put("category","identidade")
                    .put("value","O insulto atinge o personagem, sem decidir por ele.")
                    .put("triggers",new JSONArray().put("insulto")).put("priority",5).put("enabled",true))
                .put(new JSONObject().put("id","musica_alma")
                    .put("label","Música é alma").put("category","identidade")
                    .put("value","Ele ama seu alaúde, que não é foco arcano.")
                    .put("triggers",new JSONArray().put("alaúde")).put("priority",3).put("enabled",true)));
    }
    @Test public void profileRoundTripKeepsEveryTraitAndExtension() throws Exception {
        JSONObject input=example().put("future_metadata",new JSONObject().put("x",19));
        JSONObject checked=BraseiroPersonalityProfile.checked(input);
        JSONObject wrap=new JSONObject().put("sheet_json",new JSONObject().put("personality_profile",checked).toString());
        assertEquals(2,BraseiroPersonalityProfile.fromSheet(wrap).getJSONArray("traits").length());
        assertEquals(19,BraseiroPersonalityProfile.fromSheet(wrap).getJSONObject("future_metadata").getInt("x"));
    }
    @Test public void narratorContextIsConditionalAndProtectsPlayerAgency() throws Exception {
        JSONObject sheet=new JSONObject().put("name","Arcanista").put("personality_profile",example());
        String prompt=BraseiroPersonalityProfile.forNarrator(sheet,"O professor fez um insulto.");
        assertTrue(prompt.contains("Orgulho ferido"));
        assertTrue(prompt.contains("Nunca decidir ações"));
        assertFalse(prompt.contains("ganhe vantagem"));
        assertTrue(BraseiroPersonalityProfile.forSheetDisplay(sheet).contains("Música é alma"));
    }
    @Test public void incompatibleAndDuplicateTraitsAreRejected() throws Exception {
        JSONObject profile=example();
        profile.getJSONArray("traits").getJSONObject(1).put("id","orgulho_ferido");
        try {BraseiroPersonalityProfile.checked(profile);fail("duplicate should fail");}
        catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("Identificador"));}
        profile.put("schema","unknown");
        try {BraseiroPersonalityProfile.checked(profile);fail("schema should fail");}
        catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("incompatível"));}
    }
}
