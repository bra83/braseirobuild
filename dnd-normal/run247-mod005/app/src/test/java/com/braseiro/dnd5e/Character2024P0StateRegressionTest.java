package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** P0 states: no invented choices, casts, inventory ownership or personality edits. */
public class Character2024P0StateRegressionTest {
    private JSONObject mara()throws Exception{
        return new JSONObject().put("rules_source","5.5e revisada (2024/2025)")
            .put("name","Mara Escudo-Rubro").put("species","Humano")
            .put("class_name","Monge").put("background","Eremita")
            .put("level",1).put("str",12).put("dex",15).put("con",13)
            .put("int_score",10).put("wis",14).put("cha",8)
            .put("origin_ability_choice_pending",true)
            .put("species_choices_complete",false)
            .put("human_extra_origin_feat_pending",true)
            .put("selected_origin_feats",new JSONArray().put("Curandeiro"))
            .put("inventory","Lança, 5 adagas, ferramentas de artesão ou instrumento musical à escolha, 11 PO\nAntecedente Eremita: equipamento e moedas conforme o antecedente do perfil ativo.")
            .put("inventory_ledger",new JSONArray())
            .put("inventory_ledger_status","structured_entries_only");
    }
    private JSONObject arcanista()throws Exception{
        JSONObject sheet=new JSONObject().put("rules_source","5e 2024")
            .put("name","Arcanista Andarilho (nome provisório)")
            .put("species","Humano").put("class_name","Mago")
            .put("background","Andarilho").put("level",1)
            .put("str",8).put("dex",16).put("con",13)
            .put("int_score",15).put("wis",10).put("cha",13)
            .put("size","Médio").put("human_skill_choice","Atuação")
            .put("selected_origin_feats",new JSONArray().put("Sortudo").put("Músico"))
            .put("origin_ability_choice_pending",false)
            .put("human_extra_origin_feat_pending",false)
            .put("ac",13).put("spell_slots_current",new JSONArray().put(2))
            .put("spells","Preparadas: Escudo; Armadura Arcana; Mísseis Mágicos; Detectar Magia.\nArmadura Arcana ativa: CA 16 (base sem armadura 13).")
            .put("inventory","2 adagas, grimório; alaúde e flauta adquiridos; lira NÃO adquirida; 14 PO e 5 PP.")
            .put("inventory_ledger",new JSONArray())
            .put("languages_pending",true).put("life_path_pending",true);
        JSONObject base=new JSONObject().put("str",8).put("dex",14).put("con",13)
            .put("int_score",15).put("wis",10).put("cha",12);
        JSONObject bonus=new JSONObject().put("dex",2).put("cha",1);
        sheet.put("ability_purchase",new JSONObject().put("base",base).put("background_bonus",bonus));
        JSONArray traits=new JSONArray();
        for(int i=0;i<39;i++)traits.put(new JSONObject().put("id","trait"+i).put("value","confirmed"+i));
        sheet.put("personality_profile",new JSONObject().put("schema","braseiro.personality/v1")
            .put("agency_policy","player_authoritative").put("traits",traits));
        return sheet;
    }
    @Test public void generatedMonkCannotClaimCompletedWithMissingMandatoryChoices() throws Exception{
        JSONObject s=Character2024SheetReconciler.reconcile(mara());
        assertFalse(s.optBoolean("creation_mechanically_ready",true));
        assertTrue(s.getInt("creation_missing_choice_count")>=5);
        String problems=s.getJSONArray("creation_missing_choices").toString();
        assertTrue(problems,problems.contains("background_ability_plus2_plus1"));
        assertTrue(problems,problems.contains("human_size"));
        assertTrue(problems,problems.contains("human_skill"));
        assertTrue(problems,problems.contains("human_additional_origin_feat"));
        assertTrue(problems,problems.contains("monk_class_tool_or_instrument"));
        assertTrue(problems,problems.contains("equipment_package_or_choice"));
        assertEquals("legacy_text_pending_review",s.getString("inventory_ledger_status"));
        assertEquals(0,s.getJSONArray("inventory_ledger").length());
        assertEquals("Curandeiro",s.getJSONArray("selected_origin_feats").getString(0));
    }
    @Test public void arcanistaPreserves39TraitsMoneyAndSpellsWithoutImplicitCast()throws Exception{
        JSONObject s=arcanista();
        String personality=s.getJSONObject("personality_profile").toString();
        String inventory=s.getString("inventory"),spells=s.getString("spells");
        Character2024SheetReconciler.reconcile(s);
        assertEquals(personality,s.getJSONObject("personality_profile").toString());
        assertEquals(39,s.getJSONObject("personality_profile").getJSONArray("traits").length());
        assertEquals(inventory,s.getString("inventory"));
        assertEquals(spells,s.getString("spells"));
        assertEquals(13,s.getInt("ac"));
        assertEquals(2,s.getJSONArray("spell_slots_current").getInt(0));
        assertTrue(s.getBoolean("mage_armor_confirmation_pending"));
        assertEquals("legacy_text_pending_review",s.getString("inventory_ledger_status"));
        assertFalse(s.getBoolean("origin_ability_choice_pending"));
        assertEquals("Destreza",s.getString("origin_ability_choice_plus2"));
        assertTrue(s.getBoolean("languages_pending"));
        assertTrue(s.getBoolean("life_path_pending"));
        String snapshot=s.getJSONObject("personality_profile").toString();
        Character2024SheetReconciler.reconcile(s);
        assertEquals(snapshot,s.getJSONObject("personality_profile").toString());
    }
    @Test public void confirmedMageArmorSyncsNumericAcAndExpiresWithoutExtraSlotSpend()throws Exception{
        JSONObject s=arcanista();
        Character2024SheetReconciler.reconcile(s);
        assertFalse(CharacterMageArmorState.activate(s,0,false).optBoolean("ok",false)
            && s.optInt("ac")==13);
        assertEquals(16,s.getInt("ac"));
        assertEquals(1,s.getJSONArray("spell_slots_current").getInt(0));
        assertFalse(CharacterMageArmorState.activate(s,20,false).getBoolean("ok"));
        Character2024SheetReconciler.reconcile(s);
        assertEquals(16,s.getInt("ac"));
        assertEquals(1,s.getJSONArray("spell_slots_current").getInt(0));
        CharacterMageArmorState.advanceTo(s,479);
        assertEquals(16,s.getInt("ac"));
        CharacterMageArmorState.advanceTo(s,480);
        assertEquals(13,s.getInt("ac"));
        assertFalse(s.getJSONObject("mage_armor_effect").getBoolean("active"));
        assertEquals(1,s.getJSONArray("spell_slots_current").getInt(0));
    }
    @Test public void armorEquippedBlocksActivationAndDoesNotSpendSlot()throws Exception{
        JSONObject s=arcanista().put("equipped_armor","Armadura de couro");
        JSONObject result=CharacterMageArmorState.activate(s,20,false);
        assertFalse(result.toString(),result.getBoolean("ok"));
        assertEquals(13,s.getInt("ac"));
        assertEquals(2,s.getJSONArray("spell_slots_current").getInt(0));
    }
    @Test public void old2014SheetNotMigratedTo2024()throws Exception{
        JSONObject s=mara().put("rules_source","5e 2014");
        String before=s.toString();
        assertSame(s,Character2024SheetReconciler.reconcile(s));
        assertEquals(before,s.toString());
    }
}
