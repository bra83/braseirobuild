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

/** The acquisition must survive SQLite reopen, export and receipt replay. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public class InventoryPersistence2024Test {
    @Test public void lootAndPurchaseSurviveReopenWithoutDuplicateItems() throws Exception{
        Context context=RuntimeEnvironment.getApplication();
        String name="dnd-inventory-2024-"+System.nanoTime()+".db";
        AppDb db=null;
        try{
            db=new AppDb(context,name);
            db.setActiveMapProfile("dnd");
            JSONObject base=new JSONObject().put("name","Kvothe")
                .put("rules_source","5e 2024").put("class_name","Mago")
                .put("species","Humano").put("background","Andarilho")
                .put("level",1).put("size","Médio")
                .put("str",8).put("dex",16).put("con",13)
                .put("int_score",15).put("wis",10).put("cha",13)
                .put("hp",7).put("max_hp",7).put("ac",13)
                .put("inventory","")
                .put("inventory_ledger",new JSONArray().put(new JSONObject()
                    .put("id","adaga").put("name","Adaga").put("quantity",2)))
                .put("inventory_ledger_status","structured_authoritative")
                .put("coin_pieces",new JSONObject().put("gp",10))
                .put("personality_profile",new JSONObject().put("traits",
                    new JSONArray().put(new JSONObject().put("id","agency")
                        .put("value","player_first"))));
            JSONObject created=db.saveCharacterSheetAtomic(0,false,base,"","manual");
            assertTrue(created.toString(),created.optBoolean("character_saved"));
            long id=created.getLong("character_id");
            JSONObject potion=new JSONObject().put("id","potion-j1")
                .put("name","Poção de cura").put("quantity",1);
            InventoryPersistence2024.commitApprovedAcquisition(db,id,potion,"loot",
                "loot#1",new JSONObject().put("from","baú").put("actor","mestre"),"master");
            db.close();
            db=new AppDb(context,name);
            db.setActiveMapProfile("dnd");
            JSONObject saved=new JSONObject(db.getCharacter(id).getString("sheet_json"));
            assertEquals(2.7,saved.getDouble("inventory_total_weight_lb"),0.00001);
            assertEquals("loot",saved.getJSONArray("inventory_ledger")
                .getJSONObject(1).getString("source"));
            assertEquals("player_first",saved.getJSONObject("personality_profile")
                .getJSONArray("traits").getJSONObject(0).getString("value"));
            InventoryPersistence2024.commitApprovedAcquisition(db,id,potion,"loot",
                "loot#1",new JSONObject().put("from","baú").put("actor","mestre"),"master");
            saved=new JSONObject(db.getCharacter(id).getString("sheet_json"));
            assertEquals(2,saved.getJSONArray("inventory_ledger").length());
            assertEquals(2.7,saved.getDouble("inventory_total_weight_lb"),0.00001);
            JSONObject rope=new JSONObject().put("id","rope-j1")
                .put("name","Corda").put("quantity",1);
            InventoryPersistence2024.commitApprovedAcquisition(db,id,rope,"purchase",
                "buy#1",new JSONObject().put("cost_cp",100).put("actor","jogador"),"manual");
            saved=new JSONObject(db.getCharacter(id).getString("sheet_json"));
            assertEquals(900,CharacterInventory2024.coinValueCp(
                saved.getJSONObject("coin_pieces")));
            assertEquals(7.68,saved.getDouble("inventory_total_weight_lb"),0.00001);
        }finally{
            if(db!=null)db.close();
            context.deleteDatabase(name);
        }
    }
}
