package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.HashSet;
import static org.junit.Assert.*;

/** Regression for Orik Barba-de-Giz (6.6.22) and all twelve PHB 2024 classes. */
public class Character2024ClassContractTest {
    private static final String[][] CONTRACTS={
        {"Bárbaro","Força","Constituição"},
        {"Bardo","Destreza","Carisma"},
        {"Bruxo","Sabedoria","Carisma"},
        {"Clérigo","Sabedoria","Carisma"},
        {"Druida","Inteligência","Sabedoria"},
        {"Feiticeiro","Constituição","Carisma"},
        {"Guerreiro","Força","Constituição"},
        {"Ladino","Destreza","Inteligência"},
        {"Mago","Inteligência","Sabedoria"},
        {"Monge","Força","Destreza"},
        {"Paladino","Sabedoria","Carisma"},
        {"Patrulheiro","Força","Destreza"}
    };
    @Test public void allTwelveStartingClassesHaveOfficialFixedSavingThrows() throws Exception {
        CharacterSheetGenerator gen=new CharacterSheetGenerator(new DndRulesEngine());
        for(String[] fixture:CONTRACTS){
            String[] expected=Character2024ClassContract.initialSavingThrows(fixture[0]);
            assertArrayEquals(fixture[0],new String[]{fixture[1],fixture[2]},expected);
            JSONObject s=gen.baseComplete(DndRulesEngine.PROFILE_2024,"Anão",
                fixture[0],"Marinheiro",1);
            assertTrue(fixture[0]+" "+s.optString("saving_throws"),
                Character2024ClassContract.hasCorrectLevelOneSaves(s));
            assertFalse(fixture[0]+" "+s.optString("proficiencies"),
                s.optString("proficiencies").contains("conforme os manuais habilitados"));
            assertTrue(fixture[0]+" sem descrição de proficiência de arma",
                s.optString("proficiencies").contains("armas simples"));
            if("Feiticeiro".equals(fixture[0])){
                assertTrue(s.toString(),s.optString("spells").contains("Mísseis Mágicos"));
                assertFalse(s.toString(),s.optString("spells").contains("Orbe Cromática"));
                assertTrue(s.toString(),s.optString("features").contains("Feitiçaria Inata"));
            }
        }
    }
    private static JSONObject orikBug()throws Exception {
        return new JSONObject()
            .put("rules_source",DndRulesEngine.PROFILE_2024)
            .put("name","Orik Barba-de-Giz")
            .put("species","Anão").put("class_name","Feiticeiro")
            .put("background","Marinheiro").put("level",1)
            .put("str",8).put("dex",16).put("con",12)
            .put("int_score",10).put("wis",14).put("cha",15)
            .put("hp",8).put("max_hp",8).put("ac",13)
            .put("saving_throws","Força -1 • Destreza +3 • Constituição +1 • Inteligência +0 • Sabedoria +4 ★ • Carisma +4 ★")
            .put("saving_throw_proficiencies",new JSONArray()
                .put(new JSONObject().put("name","Sabedoria").put("source","class"))
                .put(new JSONObject().put("name","Carisma").put("source","class")))
            .put("proficiencies","Armas simples; focos de conjuração permitidos")
            .put("skills","Acrobacia +5 ★ • Arcanismo +2 ★ • Percepção +4 ★ • Persuasão +4 ★");
    }
    @Test public void corruptGeneratedSorcererIsRepairedWithoutChangingOtherData()throws Exception{
        JSONObject s=orikBug();
        s.put("generation_status","mechanically_valid").put("generation_valid",true);
        String originalName=s.getString("name");
        int originalHp=s.getInt("hp");
        Character2024SheetReconciler.reconcile(s);
        assertEquals(originalName,s.getString("name"));
        assertEquals(originalHp,s.getInt("hp"));
        assertTrue(Character2024ClassContract.hasCorrectLevelOneSaves(s));
        assertTrue(s.getString("saving_throws").contains("Constituição +3 ★"));
        assertTrue(s.getString("saving_throws").contains("Sabedoria +2 •"));
        assertFalse(s.getString("saving_throws").contains("Sabedoria +4 ★"));
    }
    @Test public void validatorRejectsClassIncompatibleSaveProfEvenWhenNumbersAgree()throws Exception{
        JSONObject s=orikBug();
        JSONObject result=new CharacterRulesValidator().validate(s,DndRulesEngine.PROFILE_2024);
        assertFalse(result.toString(),result.optBoolean("valid"));
        assertTrue(result.getJSONArray("errors").toString().contains("classe inicial 2024"));
    }
    @Test public void levelOneEditorCannotSelectWrongStartingSaves()throws Exception{
        JSONObject s=orikBug();
        s.put("generation_status","mechanically_valid");
        Character2024SheetReconciler.reconcile(s);
        Character2024SheetReconciler.applySelections(s,
            new boolean[]{false,false,false,false,true,true},
            new int[0],new String[0]);
        assertTrue(s.toString(),Character2024ClassContract.hasCorrectLevelOneSaves(s));
    }
    @Test public void legacy2014ProfilesAreNotRewritten() throws Exception {
        JSONObject s=orikBug();s.put("rules_source","5e 2014");
        String before=s.toString();
        Character2024SheetReconciler.reconcile(s);
        assertEquals(before,s.toString());
    }
}
