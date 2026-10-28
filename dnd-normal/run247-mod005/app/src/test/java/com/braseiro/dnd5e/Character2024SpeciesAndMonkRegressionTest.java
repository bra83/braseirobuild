package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Regression extracted from Mara (6.6.19) and the structured Arcanista export. */
public class Character2024SpeciesAndMonkRegressionTest {
    private static JSONObject mara() throws Exception {
        return new JSONObject().put("id",5).put("rules_source","5.5e revisada (2024/2025)")
            .put("name","Mara Escudo-Rubro").put("species","Humano")
            .put("class_name","Monge").put("background","Eremita")
            .put("level",1).put("str",12).put("dex",15).put("con",13)
            .put("int_score",10).put("wis",14).put("cha",8)
            .put("hp",9).put("max_hp",9).put("ac",14)
            .put("origin_ability_choice_pending",true)
            .put("human_extra_origin_feat_pending",true)
            .put("species_choices_complete",false)
            .put("saving_throws","Força +3 ★ • Destreza +4 ★")
            .put("skills","Acrobacia +4 ★ • Atletismo +3 ★ • Medicina +4 ★ • Religião +2 ★")
            .put("features","Recursos de classe de nível 1 conforme os manuais habilitados\nTraços de espécie: Humano — Urdunnir (Manual de Sub-raças de Volo) — visão, deslocamento, resistências e demais traços conforme o perfil de regras ativo.\nTalento de Origem: Curandeiro.\nEspécie 2024: Humano")
            .put("history","Cresceu numa comunidade humana — Urdunnir (Volo Subrace Handbook)")
            .put("spells","Sem magias").put("inventory","Lança, 5 adagas, 11 PO")
            .put("attacks","Lança | +3 | 1d6+1 perfurante | versátil\nAdaga | +4 | 1d4+2 perfurante | finesse, leve\nDesarmado | +3 / CD 11 | dano, agarrar ou empurrar | corpo a corpo")
            .put("volo_subrace_run139",true).put("volo_subrace_source",
                "D&D5e - Volo's Complete Subrace Handbook (v1.0)")
            .put("volo_subrace_auto_rolls",false)
            .put("species_resources",new JSONObject()
                .put("one_with_stone",new JSONObject().put("current",1).put("max",1))
                .put("urdunnir_stone_shape",new JSONObject().put("current",1).put("max",1)))
            .put("tool_proficiencies",new JSONArray().put("Ferramentas de ferreiro")
                .put("Ferramentas de pedreiro").put("Kit de herbalismo"))
            .put("one_with_stone_unworked_only",true).put("stone_shape_min_level",5);
    }
    @Test public void maraLosesOnlyStaleForeignSpeciesMechanics() throws Exception {
        JSONObject sheet=Character2024SheetReconciler.reconcile(mara());
        assertEquals("Humano",sheet.getString("species"));
        assertEquals(5,sheet.getInt("id"));
        assertFalse(sheet.has("volo_subrace_run139"));
        assertFalse(sheet.has("stone_shape_min_level"));
        assertFalse(sheet.has("one_with_stone_unworked_only"));
        assertFalse(sheet.getJSONObject("species_resources").has("one_with_stone"));
        assertFalse(sheet.getJSONObject("species_resources").has("urdunnir_stone_shape"));
        assertEquals(1,sheet.getJSONArray("tool_proficiencies").length());
        assertTrue(sheet.getString("features").contains("Artes Marciais; Defesa sem Armadura"));
        assertFalse(sheet.getString("features").contains("Urdunnir"));
        assertEquals("Lança, 5 adagas, 11 PO",sheet.getString("inventory"));
        assertEquals("Sem magias",sheet.getString("spells"));
        assertEquals(9,sheet.getInt("hp"));
        assertEquals(14,sheet.getInt("ac"));
        assertTrue(sheet.getBoolean("origin_ability_choice_pending"));
        assertTrue(sheet.getBoolean("human_extra_origin_feat_pending"));
        assertTrue(sheet.getBoolean("species_narrative_review_required"));
        assertTrue(sheet.getString("history").contains("Urdunnir")); // Never rewrite bio.
        assertEquals(2,sheet.getJSONArray("species_mechanics_repair_log").length());
    }
    @Test public void monk2024UsesDexAndMartialArtsDieNotStrength() throws Exception {
        JSONObject sheet=Character2024SheetReconciler.reconcile(mara());
        assertTrue(sheet.getString("attacks").contains("Lança | +4 | 1d6+2"));
        assertTrue(sheet.getString("attacks").contains("Adaga | +4 | 1d6+2"));
        assertTrue(sheet.getString("attacks").contains("Desarmado | +4 / CD 12 | 1d6+2"));
    }
    @Test public void migrationDoesNotModifyCustomAttacksOr2014() throws Exception {
        JSONObject original=mara().put("attacks","Minha arma caseira | +8 | dano customizado");
        Character2024SheetReconciler.reconcile(original);
        assertEquals("Minha arma caseira | +8 | dano customizado",original.getString("attacks"));
        JSONObject old=mara().put("rules_source","5e 2014");
        String snapshot=old.toString();
        assertSame(old,Character2024SheetReconciler.reconcile(old));
        assertEquals(snapshot,old.toString());
    }
    @Test public void legitimateUrdunnirStillKeepsItsOwnSpeciesResources()throws Exception {
        JSONObject old=mara().put("species",VoloSubraceRun139Engine.URDUNNIR);
        JSONObject result=Character2024SheetConsistencyGuard.reconcile(old);
        assertTrue(result.getBoolean("volo_subrace_run139"));
        assertTrue(result.getJSONObject("species_resources").has("one_with_stone"));
    }
    @Test public void arcanistaKeepsAll39PersonalityTraitsAndSkills() throws Exception {
        JSONObject wizard=new JSONObject().put("rules_source","5e 2024")
            .put("species","Humano").put("class_name","Mago")
            .put("background","Andarilho").put("level",1)
            .put("str",8).put("dex",16).put("con",13)
            .put("int_score",15).put("wis",10).put("cha",13)
            .put("saving_throws","Inteligência +4 ★ • Sabedoria +2 ★")
            .put("skills","Arcanismo +4 ★ • Atuação +3 ★ • Furtividade +5 ★ • Intuição +2 ★ • Investigação +4 ★")
            .put("inventory","Alaúde; flauta; 14 PO e 5 PP").put("spells","Armadura Arcana; Escudo")
            .put("selected_origin_feats",new JSONArray().put("Sortudo").put("Músico"))
            .put("human_skill_choice","Atuação").put("size","Médio");
        JSONArray traits=new JSONArray();
        for(int i=0;i<39;i++)traits.put(new JSONObject().put("id","trait"+i)
            .put("value","player confirmed "+i));
        wizard.put("personality_profile",new JSONObject()
            .put("schema","braseiro.personality/v1").put("traits",traits));
        Character2024SheetReconciler.reconcile(wizard);
        assertEquals(39,wizard.getJSONObject("personality_profile").getJSONArray("traits").length());
        assertEquals("Alaúde; flauta; 14 PO e 5 PP",wizard.getString("inventory"));
        assertEquals("Armadura Arcana; Escudo",wizard.getString("spells"));
        assertEquals(5,wizard.getJSONArray("skill_proficiencies").length());
    }
}
