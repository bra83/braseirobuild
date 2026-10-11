package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** RUN225: only an owned, accessible, valued item from the persisted ledger can
 * satisfy Bless's PHB 2024 5 GP holy-symbol component. Never trust a cast payload. */
public final class Core2024BlessInventory {
    private Core2024BlessInventory() {}
    public static JSONArray fromSheet(JSONObject sheet) {
        JSONArray verified=new JSONArray();
        JSONArray ledger=sheet==null?null:sheet.optJSONArray("inventory_ledger");
        if(ledger==null)return verified; // legacy free-text equipment is not value proof
        for(int i=0;i<ledger.length();i++){
            JSONObject item=ledger.optJSONObject(i);
            if(item==null||item.optInt("quantity",0)<1||!item.optBoolean("accessible",false))continue;
            String name=Core2024SpellRules.norm(item.optString("name",item.optString("nome","")));
            if(!name.contains("holy symbol")&&!name.contains("simbolo sagrado"))continue;
            Object raw=item.opt("value_gp");
            if(!(raw instanceof Number))continue;
            double value=((Number)raw).doubleValue();
            if(Double.isNaN(value)||Double.isInfinite(value)||value<5)continue;
            try{verified.put(new JSONObject().put("name",item.optString("name",item.optString("nome","")))
                .put("accessible",true).put("value_gp",value));}
            catch(Exception ignored){/* malformed entry never grants material access */}
        }
        return verified;
    }
}
