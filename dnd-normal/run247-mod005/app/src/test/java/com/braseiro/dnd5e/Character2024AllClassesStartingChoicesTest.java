package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

/** Twelve PHB 2024 classes, no fake selections, coherent level-1 ledgers. */
public class Character2024AllClassesStartingChoicesTest {
    private static final String[] ALL={
        "Bárbaro","Bardo","Bruxo","Clérigo","Druida","Feiticeiro",
        "Guerreiro","Ladino","Mago","Monge","Paladino","Patrulheiro"
    };
    private static boolean has(JSONArray a,String value){
        if(a==null)return false;
        for(int i=0;i<a.length();i++)if(value.equals(a.optString(i)))return true;
        return false;
    }
    @Test public void allTwelve2024ClassesCanBeChosenForRandomCreation() {
        assertEquals(12,CharacterRandom2024Rules.legalClasses(
            DndRulesEngine.PROFILE_2024,ALL).length);
        for(String cls:ALL)assertTrue(cls,Arrays.asList(
            CharacterRandom2024Rules.legalClasses(DndRulesEngine.PROFILE_2024,ALL)
        ).contains(cls));
    }
    @Test public void allTwelveRandomClassesAreReadyWithExplicitFirstLevelChoices() throws Exception{
        CharacterSheetGenerator g=new CharacterSheetGenerator(new DndRulesEngine());
        List<JSONObject> made=new ArrayList<>();
        for(int i=0;i<12;i++){
            JSONObject s=g.randomComplete(DndRulesEngine.PROFILE_2024,
                new String[]{"Humano","Orc","Elfo","Anão"},made);
            assertEquals(s.toString(),"mechanically_valid",s.optString("generation_status"));
            assertTrue(s.toString(),s.optBoolean("creation_mechanically_ready",false));
            assertEquals(s.toString(),0,s.optInt("creation_missing_choice_count",-1));
            assertEquals(s.toString(),39,
                s.optJSONObject("personality_profile").optJSONArray("traits").length());
            made.add(s);
        }
        java.util.HashSet<String> classes=new java.util.HashSet<>();
        for(JSONObject s:made)classes.add(s.optString("class_name"));
        assertEquals(made.toString(),12,classes.size());
        for(JSONObject s:made){
            String cls=s.optString("class_name");
            if("Clérigo".equals(cls)){
                assertEquals("Protetor",s.optString("cleric_divine_order_choice"));
                assertTrue(s.optBoolean("cleric_heavy_armor_training",false));
                assertTrue(s.optBoolean("cleric_martial_weapon_training",false));
                assertFalse(has(s.optJSONArray("class_choices_pending"),"ordem_divina"));
            }else if("Druida".equals(cls)){
                assertEquals("Guardião",s.optString("druid_primal_order_choice"));
                assertTrue(s.optBoolean("druid_medium_armor_training",false));
                assertTrue(s.optBoolean("druid_martial_weapon_training",false));
                assertFalse(has(s.optJSONArray("class_choices_pending"),"ordem_primal"));
            }else if("Guerreiro".equals(cls)){
                assertEquals("Defesa",s.optString("fighter_fighting_style_choice"));
                assertEquals(s.optInt("fighter_defense_base_ac",-9)+1,s.optInt("ac",-8));
                assertFalse(has(s.optJSONArray("class_choices_pending"),"fighting_style"));
                assertEquals(3,s.optInt("weapon_masteries_known",0));
                assertEquals(3,s.optJSONArray("selected_weapon_masteries").length());
            }else if("Bruxo".equals(cls)){
                assertEquals(1,s.optJSONArray("warlock_eldritch_invocations").length());
                assertTrue(s.optBoolean("concentration_constitution_save_advantage",false));
                assertFalse(has(s.optJSONArray("class_choices_pending"),"invocations"));
                assertNotNull(s.toString(),s.optJSONObject("pact_magic"));
            }
            Character2024SheetReconciler.reconcile(s);
            assertTrue(s.toString(),s.optBoolean("creation_mechanically_ready",false));
            assertEquals(s.toString(),0,s.optInt("creation_missing_choice_count",-1));
        }
    }
    @Test public void classesWithAbsentRequiredLevelOneChoiceMustRemainPending() throws Exception{
        for(String cls:new String[]{"Clérigo","Druida","Guerreiro","Bruxo"}){
            JSONObject s=new JSONObject().put("rules_source",DndRulesEngine.PROFILE_2024)
                .put("class_name",cls).put("species","Orc").put("background","Soldado")
                .put("level",1).put("str",13).put("dex",12).put("con",14)
                .put("int_score",10).put("wis",15).put("cha",8);
            s=Core2024ClassRuntimeEngine.ensure(s,cls,1);
            JSONArray pending=s.optJSONArray("class_choices_pending");
            assertNotNull(cls,pending);
            assertTrue(cls,pending.length()>0);
        }
    }
}
