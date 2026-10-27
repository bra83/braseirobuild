package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Fictionalized golden fixture shaped like the real 6.6.16 export. */
public class Character2024SheetReconcilerTest {
    private static JSONObject arcanista() throws Exception {
        JSONObject o=new JSONObject()
            .put("id",4)
            .put("rules_source","5e 2024")
            .put("name","Arcanista Andarilho (nome provisório)")
            .put("species","Humano")
            .put("class_name","Mago")
            .put("background","Andarilho")
            .put("level",1)
            .put("size","Médio")
            .put("str",8).put("dex",16).put("con",13)
            .put("int_score",15).put("wis",10).put("cha",13)
            .put("saving_throws","Força -1 • Destreza +3 • Constituição +1 • Inteligência +2 • Sabedoria +0 • Carisma +1")
            .put("skills","Arcanismo +2 • Furtividade +3 • Atuação +1 • Intuição +0 • Investigação +2")
            .put("passive_senses","Percepção 10 • Investigação 12 • Intuição 10")
            .put("proficiencies","Armas simples; ferramentas de ladrão; alaúde, flauta e lira; Arcanismo, Investigação, Intuição, Furtividade e Atuação.")
            .put("features","Humano: Habilidoso (Atuação); Versátil (Músico); Engenhoso. Mago: Conjuração. Antecedente: Sortudo.")
            .put("selected_feats","Sortudo (Andarilho); Músico (Humano).")
            .put("selected_origin_feats",new JSONArray().put("Sortudo"))
            .put("species_choices_complete",false)
            .put("species_choice_pending","Tamanho Pequeno/Médio + uma perícia + Talento de Origem adicional")
            .put("human_extra_origin_feat_pending",true)
            .put("origin_ability_choice_pending",true)
            .put("alignment","Neutro e Bom")
            .put("alignment_pending",true)
            .put("languages_pending",true)
            .put("life_path_pending",true)
            .put("inventory","2 adagas, foco arcano (bordão), grimório, alaúde e flauta; 14 PO e 5 PP; lira não comprada")
            .put("spells","Escudo; Armadura Arcana; Detectar Magia; Encontrar Familiar")
            .put("hp",7).put("max_hp",7).put("ac",13);
        o.put("ability_purchase",new JSONObject().put("method","point_buy_27")
          .put("cost",27)
          .put("base",new JSONObject().put("str",8).put("dex",14).put("con",13)
              .put("int_score",15).put("wis",10).put("cha",12))
          .put("background_bonus",new JSONObject().put("dex",2).put("cha",1)));
        JSONArray traits=new JSONArray();
        for(int i=0;i<39;i++)
            traits.put(new JSONObject().put("id","player_trait_"+i)
                .put("value","O jogador controla as próprias ações "+i)
                .put("priority",4).put("enabled",true));
        o.put("personality_profile",new JSONObject()
            .put("schema","braseiro.personality/v1")
            .put("agency_policy","player_authoritative")
            .put("traits",traits));
        return o;
    }
    private static boolean has(JSONArray a,String name){
        if(a==null)return false;
        for(int i=0;i<a.length();i++){
            JSONObject object=a.optJSONObject(i);
            if(name.equals(object==null?a.optString(i):object.optString("name")))return true;
        }
        return false;
    }
    @Test public void fixesLegacySkillsSavesAndPassiveValues() throws Exception {
        JSONObject o=Character2024SheetReconciler.reconcile(arcanista());
        assertTrue(o.getString("saving_throws").contains("Inteligência +4 ★"));
        assertTrue(o.getString("saving_throws").contains("Sabedoria +2 ★"));
        assertTrue(o.getString("skills").contains("Arcanismo +4 ★"));
        assertTrue(o.getString("skills").contains("Investigação +4 ★"));
        assertTrue(o.getString("skills").contains("Intuição +2 ★"));
        assertTrue(o.getString("skills").contains("Furtividade +5 ★"));
        assertTrue(o.getString("skills").contains("Atuação +3 ★"));
        assertTrue(o.getString("skills").contains("Percepção +0"));
        assertEquals("Percepção 10 • Investigação 14 • Intuição 12",o.getString("passive_senses"));
        assertEquals(5,o.getJSONArray("skill_proficiencies").length());
        assertEquals(2,o.getJSONArray("saving_throw_proficiencies").length());
    }
    @Test public void reconcilesConfirmedChoicesWithoutAddingAbilityBonusesAgain() throws Exception {
        JSONObject o=Character2024SheetReconciler.reconcile(arcanista());
        assertEquals(16,o.getInt("dex"));
        assertEquals(13,o.getInt("cha"));
        assertEquals(15,o.getInt("int_score"));
        assertFalse(o.getBoolean("origin_ability_choice_pending"));
        assertEquals("Destreza",o.getString("origin_ability_choice_plus2"));
        assertEquals("Carisma",o.getString("origin_ability_choice_plus1"));
        assertTrue(o.getBoolean("species_choices_complete"));
        assertFalse(o.getBoolean("human_extra_origin_feat_pending"));
        assertEquals("",o.getString("species_choice_pending"));
        assertTrue(has(o.getJSONArray("selected_origin_feats"),"Sortudo"));
        assertTrue(has(o.getJSONArray("selected_origin_feats"),"Músico"));
        assertTrue(o.getBoolean("alignment_pending"));
        assertTrue(o.getBoolean("languages_pending"));
        assertTrue(o.getBoolean("life_path_pending"));
    }
    @Test public void migrationIsIdempotentAndPreservesThirtyNineTraitsAndItems() throws Exception {
        JSONObject original=arcanista();
        String profile=original.getJSONObject("personality_profile").toString();
        String inventory=original.getString("inventory");
        String spells=original.getString("spells");
        JSONObject once=Character2024SheetReconciler.reconcile(original);
        JSONObject twice=Character2024SheetReconciler.reconcile(new JSONObject(once.toString()));
        assertEquals(profile,twice.getJSONObject("personality_profile").toString());
        assertEquals(39,twice.getJSONObject("personality_profile").getJSONArray("traits").length());
        assertEquals(inventory,twice.getString("inventory"));
        assertEquals(spells,twice.getString("spells"));
        assertEquals(4,twice.getInt("id"));
        assertEquals(once.getString("skills"),twice.getString("skills"));
        assertEquals(once.getString("saving_throws"),twice.getString("saving_throws"));
        assertEquals(once.getJSONArray("selected_origin_feats").toString(),
            twice.getJSONArray("selected_origin_feats").toString());
    }
    @Test public void backgroundAndSpeciesReapplicationCannotReopenConfirmedChoices() throws Exception {
        JSONObject o=Character2024SheetReconciler.reconcile(arcanista());
        Core2024EberronCharacterEngine.apply2024SpeciesMechanics(o);
        Core2024EberronCharacterEngine.prepare2024Background(o);
        assertTrue(o.getBoolean("species_choices_complete"));
        assertFalse(o.getBoolean("human_extra_origin_feat_pending"));
        assertFalse(o.getBoolean("origin_ability_choice_pending"));
        assertTrue(has(o.getJSONArray("selected_origin_feats"),"Músico"));
    }
    @Test public void genuinelyUnchosenValuesRemainPending() throws Exception {
        JSONObject o=arcanista();
        o.remove("ability_purchase");
        o.remove("selected_feats");
        o.remove("features");
        o.put("selected_origin_feats",new JSONArray().put("Sortudo"));
        o.remove("human_skill_choice");
        Character2024SheetReconciler.reconcile(o);
        assertTrue(o.getBoolean("origin_ability_choice_pending"));
        assertTrue(o.getBoolean("human_extra_origin_feat_pending"));
        assertFalse(o.getBoolean("species_choices_complete"));
    }
    @Test public void otherEditionsAreNotTouched() throws Exception {
        JSONObject o=arcanista();o.put("rules_source","5e 2014");
        String before=o.toString();
        Character2024SheetReconciler.reconcile(o);
        assertEquals(before,o.toString());
    }
}
