package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import static org.junit.Assert.*;

public class BraseiroRandomPersonalityEngineTest {
    @Test public void everyArchetypeCompletesSame39DimensionsWithUniqueIdsAndNarrative() throws Exception{
        HashSet<String> archetypes=new HashSet<>();
        for(int style=0;style<4;style++){
            JSONObject sheet=new JSONObject();
            JSONObject personality=BraseiroRandomPersonalityEngine.generateForArchetype(style);
            assertTrue(personality.toString(),BraseiroRandomPersonalityEngine.validateCompleteRandom(personality));
            archetypes.add(personality.getString("archetype"));
            assertFalse(personality.getBoolean("mechanical_modifiers"));
            assertEquals("player_authoritative",personality.getString("agency_policy"));
            assertEquals("somente_observado_ou_informado_em_jogo",personality.getString("npc_knowledge_policy"));
            JSONArray traits=personality.getJSONArray("traits");
            assertEquals(39,traits.length());
            HashSet<String> ids=new HashSet<>();
            for(int i=0;i<traits.length();i++){
                JSONObject trait=traits.getJSONObject(i);
                assertTrue(ids.add(trait.getString("id")));
                assertFalse(trait.getString("value").trim().isEmpty());
                assertFalse(trait.getString("label").trim().isEmpty());
                assertTrue(trait.getJSONArray("triggers").length()>0);
            }
            assertEquals(39,ids.size());
            assertTrue(BraseiroPersonalityProfile.forSheetDisplay(
                new JSONObject().put("personality_profile",personality)).contains("Relação com música"));
        }
        assertEquals(4,archetypes.size());
    }
    @Test public void playerAuthored39TraitsAreNeverReplacedOrEditedByRandomFill()throws Exception{
        JSONArray traits=new JSONArray();
        for(int i=0;i<39;i++)traits.put(new JSONObject()
            .put("id","trait_"+i).put("label","Confirmado pelo jogador "+i)
            .put("value","Escolha confirmada e personalizada "+i)
            .put("triggers",new JSONArray().put("personalizado")).put("priority",5));
        JSONObject authored=new JSONObject()
            .put("schema","braseiro.personality/v1")
            .put("agency_policy","player_authoritative")
            .put("profile_id","arcanista_andarilho_kvothe_inspirado_20261027")
            .put("custom_extension",new JSONObject().put("last_manual_decision","confirmed"))
            .put("traits",traits);
        JSONObject sheet=new JSONObject().put("personality_profile",authored)
            .put("ideal","Escolha manual do jogador.")
            .put("fear","Medo confirmado pelo jogador.");
        String before=sheet.toString();
        BraseiroRandomPersonalityEngine.applyToNewRandom(sheet,new Random(5));
        assertEquals(before,sheet.toString());
        assertEquals(39,sheet.getJSONObject("personality_profile").getJSONArray("traits").length());
    }
    @Test public void generatedProfileFailsClosedForMissingDuplicateOrEmptyTrait()throws Exception{
        JSONObject profile=BraseiroRandomPersonalityEngine.generate(new Random(3));
        assertTrue(BraseiroRandomPersonalityEngine.validateCompleteRandom(profile));
        profile.getJSONArray("traits").getJSONObject(12).put("value","");
        assertFalse(BraseiroRandomPersonalityEngine.validateCompleteRandom(profile));
        profile=BraseiroRandomPersonalityEngine.generate(new Random(3));
        profile.getJSONArray("traits").getJSONObject(5).put("id",
            profile.getJSONArray("traits").getJSONObject(4).getString("id"));
        assertFalse(BraseiroRandomPersonalityEngine.validateCompleteRandom(profile));
    }
    @Test public void fullyRandom2024UsesSamePersonalityForTraitsAndSummary()throws Exception{
        CharacterSheetGenerator generator=new CharacterSheetGenerator(new DndRulesEngine());
        JSONObject sheet=generator.randomComplete(DndRulesEngine.PROFILE_2024,new String[]{"Humano"},
            new ArrayList<JSONObject>());
        assertTrue(sheet.toString(),sheet.optBoolean("generation_valid",false));
        JSONObject profile=sheet.getJSONObject("personality_profile");
        assertTrue(profile.toString(),BraseiroRandomPersonalityEngine.validateCompleteRandom(profile));
        String current=sheet.optString("personality");
        assertFalse(current.trim().isEmpty());
        assertFalse(sheet.optString("ideal").trim().isEmpty());
        assertFalse(sheet.optString("bond").trim().isEmpty());
        assertFalse(sheet.optString("fear").trim().isEmpty());
        assertFalse(sheet.optString("flaw").trim().isEmpty());
        assertFalse(sheet.optString("motivation").trim().isEmpty());
        String serialized=profile.toString();
        Character2024SheetReconciler.reconcile(sheet);
        assertEquals(serialized,sheet.getJSONObject("personality_profile").toString());
        assertEquals(current,sheet.optString("personality"));
    }
}
