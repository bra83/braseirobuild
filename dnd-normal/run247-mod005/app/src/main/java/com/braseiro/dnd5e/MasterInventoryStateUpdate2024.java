package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies already curator-approved master state_updates to the same canonical
 * inventory used by the player editor. No guesswork, no text-only inventory
 * mutations, and no partial mutation if any item is invalid.
 *
 * The caller must commit the resulting sheet in the scene SQLite transaction.
 */
public final class MasterInventoryStateUpdate2024 {
    private MasterInventoryStateUpdate2024(){}
    private static final Pattern COUNT=Pattern.compile("(?i)^\\s*(\\d+)\\s*[x×]?\\s+(.+?)\\s*$");
    private static final Pattern COINS=Pattern.compile("(?i)^\\s*(\\d+)\\s*(PO|PP|PC|PE|PL)\\s*$");

    private static final class Entry{
        String id,name;int quantity;
        JSONObject source;
    }
    private static Entry item(Object raw){
        Entry result=new Entry();
        result.source=raw instanceof JSONObject?(JSONObject)raw:new JSONObject();
        String text=raw instanceof JSONObject?result.source.optString("name",""):
            raw instanceof String?(String)raw:"";
        text=text.trim();
        result.id=result.source.optString("id","").trim();
        if(text.isEmpty())throw new IllegalArgumentException("master_item_name_required");
        int qty=1;
        if(result.source.has("quantity")){
            Object count=result.source.opt("quantity");
            if(!(count instanceof Number)||count instanceof Float||count instanceof Double)
                throw new IllegalArgumentException("master_item_quantity_invalid");
            qty=((Number)count).intValue();
            if(((Number)count).longValue()!=qty)
                throw new IllegalArgumentException("master_item_quantity_overflow");
        }else{
            Matcher matcher=COUNT.matcher(text);
            if(matcher.matches()){
                try{qty=Integer.parseInt(matcher.group(1));}
                catch(NumberFormatException bad){throw new IllegalArgumentException("master_item_quantity_invalid",bad);}
                text=matcher.group(2).trim();
            }
        }
        if(qty<1)throw new IllegalArgumentException("master_item_quantity_positive_required");
        result.name=text;
        result.quantity=qty;
        return result;
    }
    private static long moneyValue(Entry entry){
        Matcher m=COINS.matcher(entry.name);
        if(!m.matches())return Long.MIN_VALUE;
        long n;
        try{n=Long.parseLong(m.group(1));}
        catch(NumberFormatException bad){throw new IllegalArgumentException("master_coins_invalid",bad);}
        long factor=m.group(2).equalsIgnoreCase("PC")?1:
            m.group(2).equalsIgnoreCase("PP")?10:
            m.group(2).equalsIgnoreCase("PE")?50:
            m.group(2).equalsIgnoreCase("PO")?100:1000;
        return Math.multiplyExact(Math.multiplyExact(n,factor),entry.quantity);
    }
    private static Entry find(JSONObject sheet,Entry requested){
        JSONArray inventory=sheet.optJSONArray("inventory_ledger");
        if(inventory==null)throw new IllegalArgumentException("inventory_ledger_missing");
        Entry match=null;
        for(int i=0;i<inventory.length();i++){
            JSONObject row=inventory.optJSONObject(i);
            if(row==null)throw new IllegalArgumentException("invalid_inventory_row");
            if(row.optInt("quantity",1)<=0)continue;
            boolean same=(!requested.id.isEmpty()&&requested.id.equals(row.optString("id")))||
                (requested.id.isEmpty()&&CharacterInventory2024.normalize(requested.name)
                    .equals(CharacterInventory2024.normalize(row.optString("name"))));
            if(!same)continue;
            if(match!=null)throw new IllegalArgumentException("ambiguous_inventory_item");
            match=new Entry();
            match.id=row.optString("id");
            match.name=row.optString("name");
            match.quantity=row.optInt("quantity",1);
        }
        if(match==null)throw new IllegalArgumentException("master_item_not_owned:"+requested.name);
        return match;
    }
    public static JSONObject apply(JSONObject original,JSONArray added,JSONArray removed,String sceneReceipt){
        if(original==null||!Character2024SheetReconciler.applies(original))
            throw new IllegalArgumentException("master_inventory_requires_2024_sheet");
        if(sceneReceipt==null||sceneReceipt.trim().isEmpty())
            throw new IllegalArgumentException("scene_receipt_required");
        JSONObject sheet=CharacterInventory2024.reconcile(new JSONObject(original.toString()));
        try{
            for(int i=0;added!=null&&i<added.length();i++){
                Entry item=item(added.opt(i));
                String token=sceneReceipt+":added:"+i;
                long coins=moneyValue(item);
                if(coins!=Long.MIN_VALUE){
                    sheet=InventoryTransactions2024.adjustCoins(sheet,coins,"master_reward",token,"master");
                    continue;
                }
                JSONObject stack=new JSONObject().put("id",item.id.isEmpty()?
                     "scene_"+Integer.toHexString(token.hashCode())+"_"+i:item.id)
                    .put("name",item.name).put("quantity",item.quantity)
                    .put("source","master_grant");
                for(String key:new String[]{"unit_weight_lb","carried","location","equipped",
                        "container_id","weight_included_in_bundle_id","rarity"}){
                    if(item.source.has(key))stack.put(key,item.source.opt(key));
                }
                sheet=InventoryTransactions2024.acquire(sheet,stack,"master_grant",token,
                    new JSONObject().put("actor","master").put("reference",sceneReceipt));
            }
            for(int i=0;removed!=null&&i<removed.length();i++){
                Entry item=item(removed.opt(i));
                String token=sceneReceipt+":removed:"+i;
                long coins=moneyValue(item);
                if(coins!=Long.MIN_VALUE){
                    sheet=InventoryTransactions2024.adjustCoins(sheet,-coins,"master_expense",token,"master");
                    continue;
                }
                Entry existing=find(sheet,item);
                if(item.quantity>existing.quantity)
                    throw new IllegalArgumentException("master_item_quantity_insufficient:"+item.name);
                JSONObject mutation=new JSONObject().put("event_id",token)
                    .put("item_id",existing.id).put("operation",
                        item.quantity==existing.quantity?"remove":"adjust");
                if(item.quantity<existing.quantity)mutation.put("delta",-item.quantity);
                sheet=CharacterInventory2024.applyMutation(sheet,mutation);
            }
            return CharacterInventory2024.reconcile(sheet);
        }catch(IllegalArgumentException e){throw e;}
        catch(Exception e){throw new IllegalStateException("master_inventory_state_update_failed",e);}
    }
}
