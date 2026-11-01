package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class MasterInventoryStateUpdate2024Test {
    private static JSONObject hero()throws Exception {
        return new JSONObject().put("name","Kvothe").put("id",1)
            .put("rules_source","5e 2024").put("species","Humano")
            .put("class_name","Mago").put("size","Médio")
            .put("str",8).put("level",1)
            .put("inventory","")
            .put("inventory_ledger_status","structured_authoritative")
            .put("inventory_ledger",new JSONArray().put(
                new JSONObject().put("id","dagger").put("name","Adaga")
                    .put("quantity",2)))
            .put("coin_pieces",new JSONObject().put("gp",10))
            .put("personality_profile",new JSONObject().put("traits",
                new JSONArray().put(new JSONObject().put("id","owner")
                    .put("value","player"))));
    }

    @Test public void masterRewardLootAndCoinGainUpdateSheetNotJustText()throws Exception {
        JSONObject original=hero();
        JSONObject s=MasterInventoryStateUpdate2024.apply(original,
            new JSONArray().put("1 Corda").put("5 PO"),
            new JSONArray().put("1 Adaga"),"scene-21");
        assertEquals(2,s.getJSONArray("inventory_ledger").length());
        assertEquals(1,s.getJSONArray("inventory_ledger").getJSONObject(0)
            .getInt("quantity"));
        assertEquals("Corda",s.getJSONArray("inventory_ledger")
            .getJSONObject(1).getString("name"));
        assertEquals(6.3,s.getDouble("inventory_total_weight_lb"),0.00001);
        assertEquals(1500,CharacterInventory2024.coinValueCp(
            s.getJSONObject("coin_pieces")));
        assertEquals("player",s.getJSONObject("personality_profile")
            .getJSONArray("traits").getJSONObject(0).getString("value"));
        assertTrue(s.getString("inventory").contains("Corda"));
        assertTrue(s.getString("inventory").contains("15 PO"));
        assertEquals(2,original.getJSONArray("inventory_ledger")
            .getJSONObject(0).getInt("quantity"));
    }

    @Test public void unknownMasterItemNeverHasInventedWeight()throws Exception {
        JSONObject s=MasterInventoryStateUpdate2024.apply(hero(),
            new JSONArray().put(new JSONObject().put("name","Máscara alienígena")
                .put("quantity",1)),new JSONArray(),"scene-22");
        assertFalse(s.getBoolean("inventory_weight_complete"));
        assertTrue(s.isNull("inventory_total_weight_lb"));
        assertEquals(1,s.getInt("inventory_unknown_weight_count"));
    }

    @Test public void insufficientAndAmbiguousLossFailWithoutMutatingCharacter()throws Exception {
        JSONObject s=hero();String before=s.toString();
        try {
            MasterInventoryStateUpdate2024.apply(s,new JSONArray(),
                new JSONArray().put("3 Adaga"),"scene-23");
            fail("overspend must be blocked");
        }catch(IllegalArgumentException expected){}
        assertEquals(before,s.toString());
        JSONArray withDuplicate=new JSONArray(s.getJSONArray("inventory_ledger").toString());
        withDuplicate.put(new JSONObject().put("id","dagger-other")
            .put("name","Adaga").put("quantity",1));
        s.put("inventory_ledger",withDuplicate);
        try {
            MasterInventoryStateUpdate2024.apply(s,new JSONArray(),
                new JSONArray().put("1 Adaga"),"scene-24");
            fail("duplicate name must require unique id");
        }catch(IllegalArgumentException expected){}
    }

    @Test public void sameReceiptCannotGrantLootOrMoneyTwice() throws Exception {
        JSONObject s=MasterInventoryStateUpdate2024.apply(hero(),
            new JSONArray().put("1 Corda").put("5 PO"),new JSONArray(),"scene-31");
        JSONObject again=MasterInventoryStateUpdate2024.apply(s,
            new JSONArray().put("1 Corda").put("5 PO"),new JSONArray(),"scene-31");
        assertEquals(2,again.getJSONArray("inventory_ledger").length());
        assertEquals(1500,CharacterInventory2024.coinValueCp(
            again.getJSONObject("coin_pieces")));
    }
}
