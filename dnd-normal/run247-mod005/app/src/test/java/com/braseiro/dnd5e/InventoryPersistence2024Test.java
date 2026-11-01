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

    @Test public void partyItemTransferIsAtomicAndReopenSafe()throws Exception{
        Context context=RuntimeEnvironment.getApplication();
        String dbName="dnd-inventory-transfer-"+System.nanoTime()+".db";
        AppDb db=null;
        try{
            db=new AppDb(context,dbName);
            db.setActiveMapProfile("dnd");
            JSONObject sender=new JSONObject().put("name","Kvothe")
                .put("rules_source","5e 2024").put("class_name","Mago")
                .put("species","Humano").put("background","Andarilho")
                .put("level",1).put("size","Médio").put("str",8)
                .put("inventory_ledger_status","structured_authoritative")
                .put("inventory_ledger",new JSONArray().put(
                    new JSONObject().put("id","dagger-1")
                        .put("name","Adaga").put("quantity",2)));
            JSONObject receiver=new JSONObject(sender.toString()).put("name","Wilem")
                .put("class_name","Bárbaro").put("str",16)
                .put("inventory_ledger",new JSONArray());
            JSONObject s=db.saveCharacterSheetAtomic(0,false,sender,"","manual");
            JSONObject t=db.saveCharacterSheetAtomic(0,false,receiver,"","manual");
            assertTrue(s.toString(),s.optBoolean("character_saved"));
            assertTrue(t.toString(),t.optBoolean("character_saved"));
            long sid=s.getLong("character_id"),tid=t.getLong("character_id");
            JSONObject receipt=InventoryPersistence2024.commitApprovedTransfer(
                db,sid,tid,"dagger-1",1,"party-transfer-1","player");
            assertTrue(receipt.toString(),receipt.getBoolean("saved"));
            db.close();
            db=new AppDb(context,dbName);
            db.setActiveMapProfile("dnd");
            JSONObject old=new JSONObject(db.getCharacter(sid).getString("sheet_json"));
            JSONObject now=new JSONObject(db.getCharacter(tid).getString("sheet_json"));
            assertEquals(1,old.getJSONArray("inventory_ledger")
                .getJSONObject(0).getInt("quantity"));
            assertEquals("Adaga",now.getJSONArray("inventory_ledger")
                .getJSONObject(0).getString("name"));
            assertEquals(1,now.getJSONArray("inventory_ledger")
                .getJSONObject(0).getInt("quantity"));
            JSONObject again=InventoryPersistence2024.commitApprovedTransfer(
                db,sid,tid,"dagger-1",1,"party-transfer-1","player");
            assertTrue(again.getBoolean("replayed"));
            JSONObject failed=null;
            try {
                failed=InventoryPersistence2024.commitApprovedTransfer(
                    db,sid,tid,"dagger-1",3,"party-transfer-2","player");
                fail("invalid transfer must roll back");
            }catch(IllegalStateException expected){}
            JSONObject unchanged=new JSONObject(db.getCharacter(sid).getString("sheet_json"));
            assertEquals(1,unchanged.getJSONArray("inventory_ledger")
                .getJSONObject(0).getInt("quantity"));
        }finally{
            if(db!=null)db.close();
            context.deleteDatabase(dbName);
        }
    }

}
