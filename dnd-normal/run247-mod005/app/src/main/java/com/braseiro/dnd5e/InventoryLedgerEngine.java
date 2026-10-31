package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Quantitative item ledger. Unknown weights must never masquerade as zero load. */
public final class InventoryLedgerEngine {
    private InventoryLedgerEngine(){}

    /** No partial changes, silent clamps, first-match wins, or negative stacks. */
    public static JSONObject authorizeQuantityChange(JSONArray ledger,String item,int delta){
        if(ledger==null)return blocked("ledger_required");
        if(item==null||item.trim().isEmpty())return blocked("item_required");
        if(delta==0)return blocked("quantity_delta_zero");
        int found=-1,previous=0;
        String key=item.trim();
        for(int i=0;i<ledger.length();i++){
            JSONObject row=ledger.optJSONObject(i);
            if(row==null)return blocked("invalid_ledger_entry");
            Object q=row.opt("quantity");
            int qty=row.optInt("quantity",1);
            if(qty<0||(q!=null&&(!(q instanceof Number)||
                    ((Number)q).doubleValue()!=qty)))return blocked("invalid_ledger_entry");
            if(key.equals(row.optString("id",""))||
                    key.equalsIgnoreCase(row.optString("name",""))){
                if(found>=0)return blocked("ambiguous_item");
                found=i;previous=qty;
            }
        }
        if(found<0)return blocked("item_not_found");
        long next=(long)previous+delta;
        if(next<0)return blocked("quantity_insufficient");
        if(next>Integer.MAX_VALUE)return blocked("quantity_overflow");
        try{
            JSONArray updated=new JSONArray(ledger.toString());
            JSONObject changed=updated.getJSONObject(found);
            changed.put("quantity",(int)next);
            return new JSONObject().put("authorized",true).put("item",key)
                    .put("delta",delta).put("previous_quantity",previous)
                    .put("new_quantity",(int)next).put("ledger",updated)
                    .put("receipt","inventory:"+key+":"+previous+":"+delta+":"+next);
        }catch(Exception e){return blocked("invalid_ledger");}
    }

    public static JSONObject totals(JSONArray ledger,int strength,boolean variant){
        try{
            JSONObject ghost=new JSONObject().put("rules_source","5e 2024")
                    .put("str",strength).put("size","Médio")
                    .put("inventory_ledger",ledger==null?new JSONArray():new JSONArray(ledger.toString()))
                    .put("inventory_ledger_migration_version",1);
            CharacterInventory2024.reconcile(ghost);
            double known=ghost.optDouble("inventory_known_weight_lb",0d);
            JSONObject result=new DndRulesEngine().encumbrance(strength,known,variant);
            boolean complete=ghost.optBoolean("inventory_weight_complete",false);
            result.put("known_weight_lb",known);
            result.put("stack_count",countNonzeroStacks(ledger));
            result.put("unknown_weight_count",ghost.optInt("inventory_unknown_weight_count",0));
            result.put("unknown_weight_items",ghost.optJSONArray("inventory_weight_problems"));
            result.put("complete",complete);
            result.put("display_weight",ghost.optString("inventory_weight_display"));
            result.put("weight",complete?known:JSONObject.NULL);
            result.put("state",complete?result.optString("state","normal"):
                    (known>strength*15d?"over_capacity":"unverified"));
            return result;
        }catch(Exception e){throw new IllegalStateException("inventory_weight_calculation_failed",e);}
    }

    private static int countNonzeroStacks(JSONArray ledger){
        if(ledger==null)return 0;
        int count=0;
        for(int i=0;i<ledger.length();i++){
            JSONObject row=ledger.optJSONObject(i);
            if(row!=null&&row.optInt("quantity",1)>0)count++;
        }
        return count;
    }

    /** Compatibility API must fail closed instead of silently clamping/removing items. */
    public static JSONArray changeQuantity(JSONArray ledger,String item,int delta){
        JSONObject decision=authorizeQuantityChange(ledger,item,delta);
        if(!decision.optBoolean("authorized",false))
            throw new IllegalArgumentException(decision.optString("blocked","invalid_change"));
        return decision.optJSONArray("ledger");
    }

    private static JSONObject blocked(String reason){
        try{return new JSONObject().put("authorized",false).put("blocked",reason);}
        catch(Exception e){throw new IllegalStateException(e);}
    }
}
