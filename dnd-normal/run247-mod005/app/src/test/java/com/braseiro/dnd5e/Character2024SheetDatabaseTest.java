package com.braseiro.dnd5e;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Atomic SQLite edit, close/reopen, and re-export: no loss of player-owned state. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public class Character2024SheetDatabaseTest {
    @Test public void threeRoundTripsRetainIdentityProficienciesAndPersonality() throws Exception{
        Context app=RuntimeEnvironment.getApplication();
        String dbName="sheet2024-"+System.nanoTime()+".db";
        AppDb db=null;
        try{
            JSONObject sheet=new JSONObject()
                .put("name","Arcanista Andarilho (nome provisório)")
                .put("rules_source","5e 2024")
                .put("species","Humano")
                .put("class_name","Mago")
                .put("background","Andarilho")
                .put("level",1)
                .put("size","Médio")
                .put("str",8).put("dex",16).put("con",13)
                .put("int_score",15).put("wis",10).put("cha",13)
                .put("hp",7).put("max_hp",7).put("ac",13)
                .put("saving_throws","Força -1 • Destreza +3 • Constituição +1 • Inteligência +2 • Sabedoria +0 • Carisma +1")
                .put("skills","Arcanismo +2 • Investigação +2 • Intuição +0 • Furtividade +3 • Atuação +1")
                .put("proficiencies","Arcanismo, Investigação, Intuição, Furtividade e Atuação")
                .put("features","Humano: Habilidoso (Atuação); Versátil (Músico)")
                .put("selected_feats","Sortudo (Andarilho); Músico (Humano)")
                .put("inventory","2 adagas; bordão; grimório; alaúde; flauta; 14 PO e 5 PP. Lira não adquirida.")
                .put("spells","Escudo; Armadura Arcana; Detectar Magia; Encontrar Familiar")
                .put("selected_origin_feats",new JSONArray().put("Sortudo"))
                .put("origin_ability_choice_pending",true)
                .put("human_extra_origin_feat_pending",true)
                .put("species_choices_complete",false)
                .put("languages_pending",true)
                .put("life_path_pending",true);
            JSONObject base=new JSONObject().put("str",8).put("dex",14).put("con",13)
                .put("int_score",15).put("wis",10).put("cha",12);
            sheet.put("ability_purchase",new JSONObject().put("base",base)
                .put("background_bonus",new JSONObject().put("dex",2).put("cha",1))
                .put("cost",27));
            JSONArray traits=new JSONArray();
            for(int i=0;i<39;i++)traits.put(new JSONObject().put("id","trait"+i)
                .put("value","confirmed"+i).put("enabled",true));
            sheet.put("personality_profile",new JSONObject().put("schema","braseiro.personality/v1")
                .put("agency_policy","player_authoritative").put("traits",traits));
            String inventory=sheet.getString("inventory"),spells=sheet.getString("spells");
            db=new AppDb(app,dbName);
            db.setActiveMapProfile("dnd");
            JSONObject receipt=db.saveCharacterSheetAtomic(0,false,sheet,"","manual");
            assertTrue(receipt.toString(),receipt.optBoolean("character_saved"));
            long heroId=receipt.getLong("character_id");
            assertTrue(heroId>0);
            for(int cycle=0;cycle<3;cycle++){
                db.close();
                db=new AppDb(app,dbName);
                db.setActiveMapProfile("dnd");
                JSONObject row=db.getCharacter(heroId);
                assertEquals(heroId,row.getLong("id"));
                JSONObject persisted=new JSONObject(row.getString("sheet_json"));
                assertEquals(inventory,persisted.getString("inventory"));
                assertEquals(spells,persisted.getString("spells"));
                assertTrue(persisted.getString("saving_throws").contains("Inteligência +4 ★"));
                assertTrue(persisted.getString("skills").contains("Furtividade +5 ★"));
                assertTrue(persisted.getString("skills").contains("Investigação +4 ★"));
                assertFalse(persisted.getBoolean("origin_ability_choice_pending"));
                assertTrue(persisted.getBoolean("species_choices_complete"));
                assertEquals(39,persisted.getJSONObject("personality_profile").getJSONArray("traits").length());
                assertEquals("player_authoritative",
                    persisted.getJSONObject("personality_profile").getString("agency_policy"));
                receipt=db.saveCharacterSheetAtomic(heroId,true,persisted,"","manual");
                assertTrue(receipt.toString(),receipt.optBoolean("character_saved"));
                assertEquals(heroId,receipt.getLong("character_id"));
            }
        }finally{
            if(db!=null)db.close();
            app.deleteDatabase(dbName);
        }
    }
}
