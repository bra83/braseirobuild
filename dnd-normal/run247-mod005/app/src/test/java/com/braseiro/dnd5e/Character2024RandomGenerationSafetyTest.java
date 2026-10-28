package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import static org.junit.Assert.*;

/** High-volume random 2024 creation: no cross-edition benefits or invented choices. */
public class Character2024RandomGenerationSafetyTest {
    private static final String PROFILE=DndRulesEngine.PROFILE_2024;
    private static final String[] SPECIES={
        "Humano","Anão","Elfo","Draconato","Gnomo","Golias","Orc","Pequenino","Tiferino","Aasimar"
    };
    private static final String[] BACKGROUNDS={
        "Acólito","Andarilho","Artesão","Artista","Charlatão","Criminoso",
        "Eremita","Escriba","Fazendeiro","Guarda","Guia","Marinheiro",
        "Mercador","Nobre","Sábio","Soldado"
    };
    private static String abilityKey(String ability){
        switch(ability){
            case "Força":return "str";case "Destreza":return "dex";
            case "Constituição":return "con";case "Inteligência":return "int_score";
            case "Sabedoria":return "wis";case "Carisma":return "cha";
            default:throw new IllegalArgumentException(ability);
        }
    }
    private static boolean includes(JSONArray array,String value){
        if(array==null)return false;
        for(int i=0;i<array.length();i++)if(value.equals(array.optString(i)))return true;
        return false;
    }
    @Test public void everyCore2024SpeciesAndRandomClassNeverClaimInvalidAsReady() throws Exception{
        CharacterSheetGenerator generator=new CharacterSheetGenerator(new DndRulesEngine());
        for(String species:SPECIES){
            for(int i=0;i<18;i++){
                JSONObject s=generator.randomComplete(PROFILE,new String[]{species},new ArrayList<JSONObject>());
                assertEquals(s.toString(),species,s.optString("species"));
                assertEquals(s.toString(),PROFILE,DndRulesEngine.canonicalProfile(s.optString("rules_source")));
                String debug="species="+species+" class="+s.optString("class_name")+
                    " background="+s.optString("background")+
                    " reason="+s.optString("generation_blocked_reason")+
                    " choices="+s.optJSONArray("creation_missing_choices")+
                    " feat="+s.optString("origin_feat")+
                    " originPending="+s.optBoolean("origin_feat_choice_pending");
                assertEquals(debug,"mechanically_valid",s.optString("generation_status"));
                assertTrue(s.toString(),s.optBoolean("generation_valid",false));
                assertTrue(s.toString(),s.optBoolean("creation_mechanically_ready",false));
                assertEquals(s.toString(),0,s.optInt("creation_missing_choice_count",-1));
                assertFalse(s.toString(),s.optBoolean("origin_ability_choice_pending",true));
                assertEquals(s.toString(),"new_character_class_A_background_B",
                    s.optString("inventory_ledger_status"));
                assertTrue(s.toString(),s.optInt("inventory_currency_gp",0)>=50);
                assertTrue(s.toString(),s.optJSONArray("inventory_ledger")!=null
                    && s.optJSONArray("inventory_ledger").length()>0);
                assertFalse(s.toString(),s.optString("inventory").contains("conforme o antecedente"));
                assertFalse(s.toString(),s.optString("inventory").contains("à escolha"));
                assertFalse(s.toString(),s.has("one_with_stone_carry_multiplier"));
                assertFalse(s.toString(),s.optString("features").contains("Urdunnir"));
                assertFalse(s.toString(),s.optString("history").contains("Urdunnir"));
                JSONArray origin=s.optJSONArray("origin_ability_allowed");
                assertNotNull(s.toString(),origin);
                assertTrue(s.toString(),includes(origin,s.optString("origin_ability_choice_plus2")));
                assertTrue(s.toString(),includes(origin,s.optString("origin_ability_choice_plus1")));
                assertNotEquals(s.toString(),s.optString("origin_ability_choice_plus2"),
                    s.optString("origin_ability_choice_plus1"));
                JSONObject purchase=s.optJSONObject("ability_purchase");
                assertNotNull(s.toString(),purchase);
                JSONObject base=purchase.getJSONObject("base"),bonus=purchase.getJSONObject("background_bonus");
                for(String key:new String[]{"str","dex","con","int_score","wis","cha"})
                    assertEquals(s.toString(),base.getInt(key)+bonus.optInt(key),s.getInt(key));
                JSONArray feats=s.optJSONArray("selected_origin_feats");
                assertNotNull(s.toString(),feats);
                String backgroundFeat=Core2024EberronCharacterEngine.originFeatForBackground(s.optString("background"));
                if(backgroundFeat.contains(" ("))backgroundFeat=backgroundFeat.substring(0,backgroundFeat.indexOf(" ("));
                assertTrue(s.toString(),includes(feats,backgroundFeat));
                if(species.equals("Humano")){
                    assertTrue(s.toString(),feats.length()>=2);
                    assertFalse(s.toString(),s.optBoolean("human_extra_origin_feat_pending",true));
                    assertFalse(s.toString(),s.optString("human_skill_choice").isEmpty());
                }
                String snap=s.optString("inventory")+s.optJSONArray("inventory_ledger")
                    +s.optString("origin_ability_bonus");
                Character2024SheetReconciler.reconcile(s);
                assertEquals(s.toString(),snap,s.optString("inventory")+s.optJSONArray("inventory_ledger")
                    +s.optString("origin_ability_bonus"));
            }
        }
    }
    @Test public void secondSpeciesCompilationMustNotUndoValidatedRandomChoices() throws Exception{
        CharacterSheetGenerator generator=new CharacterSheetGenerator(new DndRulesEngine());
        for(String species:SPECIES){
            JSONObject s=null;
            for(int i=0;i<35;i++){
                JSONObject candidate=generator.randomComplete(PROFILE,new String[]{species},
                    new ArrayList<JSONObject>());
                if(candidate.optBoolean("generation_valid",false)){s=candidate;break;}
            }
            assertNotNull("No valid random character for "+species,s);
            String feats=s.getJSONArray("selected_origin_feats").toString();
            String origin=s.optString("origin_ability_bonus");
            String inventory=s.optString("inventory");
            int hp=s.optInt("max_hp"),ac=s.optInt("ac");
            Core2024EberronCharacterEngine.apply2024SpeciesMechanics(s);
            Character2024SheetReconciler.reconcile(s);
            assertTrue(s.toString(),s.optBoolean("species_choices_complete",false));
            assertEquals(s.toString(),0,s.optInt("creation_missing_choice_count",99));
            assertEquals(s.toString(),feats,s.getJSONArray("selected_origin_feats").toString());
            assertEquals(s.toString(),origin,s.optString("origin_ability_bonus"));
            assertEquals(s.toString(),inventory,s.optString("inventory"));
            assertEquals(s.toString(),hp,s.optInt("max_hp"));
            assertEquals(s.toString(),ac,s.optInt("ac"));
            if(species.equals("Humano"))
                assertFalse(s.toString(),s.optBoolean("human_extra_origin_feat_pending",true));
        }
    }
    @Test public void incompatibleSpeciesCannotBeRolledAsHumanOrAssignedUnknownPowers()throws Exception{
        CharacterSheetGenerator generator=new CharacterSheetGenerator(new DndRulesEngine());
        JSONObject blocked=generator.randomComplete(PROFILE,new String[]{
            VoloSubraceRun139Engine.URDUNNIR},new ArrayList<JSONObject>());
        assertFalse(blocked.toString(),blocked.optBoolean("generation_valid",true));
        assertEquals(blocked.toString(),"blocked",blocked.optString("generation_status"));
        assertFalse(blocked.toString(),blocked.has("name"));
    }
    @Test public void backgroundMagicInitiateUsesOnlyItsOwnSpellList()throws Exception{
        for(String bg:new String[]{"Acólito","Guia","Sábio"}){
            JSONObject s=new JSONObject().put("rules_source",PROFILE)
                .put("species","Orc").put("class_name","Monge")
                .put("background",bg).put("level",1)
                .put("str",12).put("dex",15).put("con",13)
                .put("int_score",10).put("wis",14).put("cha",8)
                .put("hp",9).put("max_hp",9).put("ac",14)
                .put("inventory","Lança, 5 adagas, ferramentas de artesão ou instrumento musical à escolha, mochila de explorador, 11 PO\nAntecedente "+bg+": equipamento e moedas conforme o antecedente do perfil ativo.")
                .put("origin_ability_choice_pending",true)
                .put("skills","Medicina +2");
            CharacterRandom2024Rules.chooseBackgroundAbilitiesBeforeMechanics(s,new java.util.Random(11));
            Core2024EberronCharacterEngine.prepare2024Background(s);
            s=CharacterRandom2024Rules.finalizeNew2024(s,new java.util.Random(42));
            assertFalse(s.toString(),s.has("generation_blocked_reason"));
            String expected=bg.equals("Acólito")?"Clérigo":bg.equals("Guia")?"Druida":"Mago";
            assertEquals(s.toString(),expected,s.optString("magic_initiate_list"));
        }
    }
    @Test public void incomplete2014AndHybridRandomLoadoutsMustNotBeMarkedComplete()throws Exception{
        for(String profile:new String[]{"5e 2014","Híbrido compatível"}){
            CharacterSheetGenerator generator=new CharacterSheetGenerator(new DndRulesEngine());
            for(int i=0;i<12;i++){
                JSONObject sheet=generator.randomComplete(profile,new String[]{"Humano"},
                    new ArrayList<JSONObject>());
                assertFalse(profile+" "+sheet,sheet.optBoolean("generation_valid",true));
                assertEquals(profile+" "+sheet,"blocked",sheet.optString("generation_status"));
                assertEquals(profile,DndRulesEngine.canonicalProfile(sheet.optString("rules_source")));
                assertFalse("Must never cross to 2024",Character2024SheetReconciler.applies(sheet));
            }
        }
    }
    @Test public void unrelated2014SheetIsNotTransformedInto2024()throws Exception{
        String[] legal=CharacterRandom2024Rules.legalSpecies("5e 2014",new String[]{"Humano","Anão","Urdunnir"});
        assertEquals(2,legal.length);
        assertEquals(0,CharacterRandom2024Rules.legalSpecies(PROFILE,
            new String[]{"Anão — Urdunnir (Volo Subrace Handbook)"}).length);
    }
}
