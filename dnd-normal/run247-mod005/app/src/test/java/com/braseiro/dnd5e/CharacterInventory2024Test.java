package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class CharacterInventory2024Test {
    private static JSONObject kvothe() throws Exception{
        JSONArray items=new JSONArray();
        String[][] source={
            {"adaga","Adaga","2"},{"bordao_foco","Bordão (foco arcano)","1"},
            {"tunica","Túnica","1"},{"grimorio","Grimório de Mago","1"},
            {"pacote_estudioso","Pacote de estudioso","1"},
            {"alaude","Alaúde","1"},{"flauta","Flauta","1"},
            {"roupas_viajante","Roupas de viajante","1"},
            {"saco_dormir","Saco de dormir","1"},{"cobertor","Cobertor","1"}
        };
        for(String[] item:source)
            items.put(new JSONObject().put("id",item[0]).put("name",item[1])
                .put("quantity",Integer.parseInt(item[2])));
        return new JSONObject().put("id",41).put("rules_source","5e 2024")
            .put("name","Kvothe").put("class_name","Mago").put("level",1)
            .put("species","Humano").put("size","Médio")
            .put("str",8).put("dex",16).put("con",13)
            .put("int_score",15).put("wis",10).put("cha",13)
            .put("hp",7).put("max_hp",7).put("ac",13)
            .put("spells","Escudo, Armadura Arcana, Detectar Magia")
            .put("personality_profile",new JSONObject()
                .put("traits",new JSONArray().put(
                    new JSONObject().put("id","loyalty").put("value","never abandoned"))))
            .put("inventory","Moedas atuais: 14 PO e 5 PP.")
            .put("inventory_ledger",items)
            .put("inventory_ledger_status","structured_entries_only");
    }
    private static JSONObject action(String op,String receipt,String id)throws Exception{
        JSONObject result=new JSONObject().put("operation",op).put("event_id",receipt);
        if(id!=null)result.put("item_id",id);
        return result;
    }
    private static JSONObject item(String id,String name,int quantity)throws Exception{
        return new JSONObject().put("id",id).put("name",name).put("quantity",quantity);
    }
    private static JSONObject find(JSONObject s,String id){
        JSONArray rows=s.optJSONArray("inventory_ledger");
        if(rows==null)return null;
        for(int i=0;i<rows.length();i++){
            JSONObject x=rows.optJSONObject(i);
            if(x!=null&&id.equals(x.optString("id")))return x;
        }
        return null;
    }

    @Test public void realKvotheExportIsNotZeroAndDoesNotChangePlayerFields()throws Exception{
        JSONObject s=kvothe();
        String personality=s.getJSONObject("personality_profile").toString();
        String spells=s.getString("spells");
        JSONObject weighted=CharacterInventory2024.reconcile(s);
        assertTrue(weighted.getBoolean("inventory_weight_complete"));
        assertEquals(52.38,weighted.getDouble("inventory_total_weight_lb"),0.00001);
        assertEquals(120,weighted.getDouble("inventory_capacity_lb"),0.00001);
        assertEquals(14,weighted.getJSONObject("coin_pieces").getInt("gp"));
        assertEquals(5,weighted.getJSONObject("coin_pieces").getInt("sp"));
        assertEquals(personality,weighted.getJSONObject("personality_profile").toString());
        assertEquals(spells,weighted.getString("spells"));
        assertEquals(10,weighted.getJSONArray("inventory_ledger").length());
        assertEquals(52.38,CharacterInventory2024.reconcile(weighted)
                .getDouble("inventory_total_weight_lb"),0.00001);
    }

    @Test public void masterAddRemoveQuantityMoveAndEquipImmediatelyChangeLoad()throws Exception{
        JSONObject s=CharacterInventory2024.reconcile(kvothe());
        s=CharacterInventory2024.applyMutation(s,action("add","discover-1",null)
            .put("item",item("torch-loot","Tocha",4).put("unit_weight_lb",1)));
        assertEquals(56.38,s.getDouble("inventory_total_weight_lb"),0.00001);
        assertEquals("manual",find(s,"torch-loot").getString("source"));
        s=CharacterInventory2024.applyMutation(s,action("adjust","use-1","torch-loot")
            .put("delta",-2));
        assertEquals(54.38,s.getDouble("inventory_total_weight_lb"),0.00001);
        s=CharacterInventory2024.applyMutation(s,action("update","equip-1","torch-loot")
            .put("equipped",true));
        assertEquals(54.38,s.getDouble("inventory_total_weight_lb"),0.00001);
        s=CharacterInventory2024.applyMutation(s,action("move","stash-1","torch-loot")
            .put("carried",false).put("location","storage"));
        assertEquals(52.38,s.getDouble("inventory_total_weight_lb"),0.00001);
        s=CharacterInventory2024.applyMutation(s,action("move","retrieve-1","torch-loot")
            .put("carried",true).put("location","backpack"));
        assertEquals(54.38,s.getDouble("inventory_total_weight_lb"),0.00001);
        s=CharacterInventory2024.applyMutation(s,action("remove","discard-1","torch-loot"));
        assertEquals(52.38,s.getDouble("inventory_total_weight_lb"),0.00001);
        assertNull(find(s,"torch-loot"));
    }

    @Test public void purchasedPotionDebitsWalletOnceAndWeighsHalfPound()throws Exception{
        JSONObject s=kvothe();
        JSONObject auth=new JSONObject().put("cost_cp",5000).put("from","mercador")
            .put("actor","player").put("reason","comprou na feira");
        JSONObject p=InventoryTransactions2024.acquire(s,
            item("potion-1","Poção de cura",1),"purchase","shop-77",auth);
        assertEquals(950,CharacterInventory2024.coinValueCp(
            p.getJSONObject("coin_pieces")));
        assertEquals(1,find(p,"potion-1").getInt("quantity"));
        assertEquals("purchase",find(p,"potion-1").getString("source"));
        assertEquals(52.76,p.getDouble("inventory_total_weight_lb"),0.00001);
        JSONObject replay=InventoryTransactions2024.acquire(p,
            item("potion-1","Poção de cura",1),"purchase","shop-77",auth);
        assertEquals(950,CharacterInventory2024.coinValueCp(
            replay.getJSONObject("coin_pieces")));
        assertEquals(11,replay.getJSONArray("inventory_ledger").length());
        assertEquals(1,replay.getJSONArray("inventory_journal").length());
    }

    @Test public void unknownTreasureWeightMustBeUnverifiedRatherThanZero()throws Exception{
        JSONObject s=InventoryTransactions2024.acquire(kvothe(),
            item("artifact-1","Relicário estranho",1),"find","discovery-9",
            new JSONObject().put("location","ruínas"));
        assertFalse(s.getBoolean("inventory_weight_complete"));
        assertTrue(s.isNull("inventory_total_weight_lb"));
        assertTrue(s.getString("inventory_weight_display").contains("parcial"));
        assertEquals(1,s.getInt("inventory_unknown_weight_count"));
        s=CharacterInventory2024.applyMutation(s,
            action("update","weigh-artifact-1","artifact-1").put("unit_weight_lb",2.5));
        assertTrue(s.getBoolean("inventory_weight_complete"));
        assertEquals(54.88,s.getDouble("inventory_total_weight_lb"),0.00001);
    }

    @Test public void lootCraftGiftAndBattleRewardKeepProvenance()throws Exception{
        String[] sources={"loot","craft","gift","battle_reward","quest_reward","master_grant"};
        JSONObject s=kvothe();
        for(int i=0;i<sources.length;i++){
            String id="item-"+i;
            s=InventoryTransactions2024.acquire(s,item(id,"Corda",1),
                sources[i],"event-"+i,new JSONObject()
                    .put("from","NPC").put("actor","mestre").put("reference","scene-"+i));
            assertEquals(sources[i],find(s,id).getString("source"));
            assertEquals("NPC",find(s,id).getString("obtained_from"));
        }
        assertEquals(82.38,s.getDouble("inventory_total_weight_lb"),0.00001);
        assertEquals(6,s.getJSONArray("inventory_journal").length());
    }

    @Test public void transferBetweenPartyMembersChangesBothSheetsExactlyOnce()throws Exception{
        JSONObject w=kvothe();w.put("id",42).put("name","Wilem").put("str",16);
        w.put("inventory_ledger",new JSONArray());
        w.put("inventory_ledger_status","structured_authoritative");
        w.put("inventory","");w.put("coin_pieces",new JSONObject());
        JSONObject event=InventoryTransactions2024.transfer(kvothe(),w,
            "adaga",1,"transfer-11","player");
        JSONObject from=event.getJSONObject("source"),to=event.getJSONObject("target");
        assertEquals(1,find(from,"adaga").getInt("quantity"));
        assertEquals(1,find(to,"transfer:transfer-11").getInt("quantity"));
        assertEquals(51.38,from.getDouble("inventory_total_weight_lb"),0.00001);
        assertEquals(1,to.getDouble("inventory_total_weight_lb"),0.00001);
        JSONObject replay=InventoryTransactions2024.transfer(from,to,
            "adaga",1,"transfer-11","player");
        assertTrue(replay.getBoolean("replayed"));
        assertEquals(1,find(replay.getJSONObject("source"),"adaga").getInt("quantity"));
        assertEquals(1,replay.getJSONObject("target").getJSONArray("inventory_ledger").length());
    }

    @Test public void unknownItemNeverConvertsToZeroAndNoLegacyResurrection()throws Exception{
        JSONObject s=kvothe();
        s.put("inventory_ledger",new JSONArray());
        s.put("inventory_ledger_status","structured_authoritative");
        s=CharacterInventory2024.reconcile(s);
        assertEquals(0,s.getJSONArray("inventory_ledger").length());
        assertEquals(0.38,s.getDouble("inventory_total_weight_lb"),0.00001);
        s=CharacterInventory2024.applyMutation(s,action("add","p-1",null)
            .put("item",item("mystery","Arco de origem desconhecida",1)));
        assertFalse(s.getBoolean("inventory_weight_complete"));
        assertTrue(s.isNull("inventory_total_weight_lb"));
        s=CharacterInventory2024.applyMutation(s,action("remove","p-2","mystery"));
        assertEquals(0,s.getJSONArray("inventory_ledger").length());
        assertEquals(0.38,s.getDouble("inventory_total_weight_lb"),0.00001);
    }

    @Test public void negativeQuantityInsufficientFundsAndBadWeightDoNotMutateOriginal()throws Exception{
        JSONObject s=kvothe();
        String before=s.toString();
        try{
            CharacterInventory2024.applyMutation(s,action("adjust","bad-1","adaga")
                .put("delta",-50));
            fail("must reject negative quantity");
        }catch(IllegalArgumentException expected){}
        try{
            InventoryTransactions2024.acquire(s,item("expensive","Lira",1),
                "purchase","bad-2",new JSONObject().put("cost_cp",500000));
            fail("must reject insufficient funds");
        }catch(IllegalArgumentException expected){}
        try{
            CharacterInventory2024.applyMutation(s,action("add","bad-3",null)
                .put("item",item("wrong","Relíquia",1).put("unit_weight_lb",-10)));
            fail("must reject negative weight");
        }catch(IllegalArgumentException expected){}
        assertEquals(before,s.toString());
    }
}
