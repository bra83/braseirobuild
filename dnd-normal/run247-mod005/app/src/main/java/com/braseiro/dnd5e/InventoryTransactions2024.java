package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/**
 * Pure authorized inventory event reducer for purchases, treasure, discoveries,
 * crafting, gifts, master awards and player-to-player transfers.
 *
 * The caller MUST save the returned sheet(s) with the event receipt in one
 * AppDb SQLite transaction, and MUST NOT persist before player/master approval.
 */
public final class InventoryTransactions2024 {
    private InventoryTransactions2024(){}

    private static JSONObject copy(JSONObject input){
        if(input==null)throw new IllegalArgumentException("sheet_required");
        try{return new JSONObject(input.toString());}
        catch(Exception e){throw new IllegalArgumentException("invalid_sheet",e);}
    }
    private static boolean received(JSONObject s,String id){
        JSONArray receipts=s.optJSONArray("inventory_event_receipts");
        if(receipts==null)return false;
        for(int i=0;i<receipts.length();i++)if(id.equals(receipts.optString(i)))return true;
        return false;
    }
    private static boolean kind(String source){
        for(String value:new String[]{"purchase","find","loot","quest_reward",
                "battle_reward","gift","craft","trade","master_grant","initial_gear"})
            if(value.equals(source))return true;
        return false;
    }
    private static JSONObject action(String operation,String event,String id) throws Exception {
        JSONObject a=new JSONObject().put("operation",operation).put("event_id",event);
        if(id!=null)a.put("item_id",id);
        return a;
    }
    private static void record(JSONObject s,String event,String action,String reason,
                               String who,JSONObject details)throws Exception{
        JSONArray events=s.optJSONArray("inventory_journal");
        if(events==null)events=new JSONArray();
        JSONObject entry=new JSONObject().put("event_id",event).put("action",action)
            .put("reason",reason==null?"":reason)
            .put("actor",who==null?"":who);
        if(details!=null)entry.put("details",details);
        events.put(entry);
        s.put("inventory_journal",events);
    }
    private static long amount(Object v,String error){
        if(!(v instanceof Number)||v instanceof Float||v instanceof Double)
            throw new IllegalArgumentException(error);
        long n=((Number)v).longValue();
        if(n<0)throw new IllegalArgumentException(error);
        return n;
    }

    /**
     * Creates a new stack with provenance. Cost (if any) must be provided
     * explicitly in copper pieces, no hallucinated merchant prices.
     * source=purchase additionally spends the verified wallet atomically.
     */
    public static JSONObject acquire(JSONObject original,JSONObject item,
              String source,String eventId,JSONObject authorization){
        if(!kind(source))throw new IllegalArgumentException("unsupported_inventory_source");
        if(item==null)throw new IllegalArgumentException("acquired_item_required");
        if(eventId==null||eventId.trim().isEmpty())
            throw new IllegalArgumentException("inventory_event_id_required");
        try{
            JSONObject state=CharacterInventory2024.reconcile(copy(original));
            if(received(state,eventId))return state;
            JSONObject row=copy(item);
            if(!row.has("quantity"))row.put("quantity",1);
            row.put("source",source).put("acquisition_event_id",eventId);
            if(authorization!=null){
                if(authorization.has("from"))row.put("obtained_from",authorization.opt("from"));
                if(authorization.has("location"))row.put("acquisition_location",authorization.opt("location"));
                if(authorization.has("reference"))row.put("acquisition_reference",authorization.opt("reference"));
            }
            long costCp=0;
            if("purchase".equals(source)){
                if(authorization==null||!authorization.has("cost_cp"))
                    throw new IllegalArgumentException("purchase_price_required");
                costCp=amount(authorization.opt("cost_cp"),"invalid_purchase_price");
                JSONObject purse=CharacterInventory2024.coinPurse(state);
                long balance=CharacterInventory2024.coinValueCp(purse);
                if(costCp>balance)throw new IllegalArgumentException("insufficient_coins");
                state.put("coin_pieces",CharacterInventory2024.coinsFromCopper(balance-costCp));
                state.put("coin_pieces_migration_version",1);
            }
            JSONObject add=action("add",eventId,null).put("item",row).put("source",source);
            JSONObject result=CharacterInventory2024.applyMutation(state,add);
            record(result,eventId,source,
                authorization==null?"":authorization.optString("reason",""),
                authorization==null?"":authorization.optString("actor",""),
                new JSONObject().put("item_id",row.optString("id"))
                    .put("quantity",row.optInt("quantity",1)).put("cost_cp",costCp));
            return CharacterInventory2024.reconcile(result);
        }catch(IllegalArgumentException e){throw e;}
        catch(Exception e){throw new IllegalStateException("acquisition_failed",e);}
    }

    /**
     * Actual loot money, quest payments, gold spent at inns, etc.
     * Will never create a wallet from an unidentified/blank legacy field.
     */
    public static JSONObject adjustCoins(JSONObject original,long deltaCp,
             String source,String eventId,String actor){
        if(eventId==null||eventId.trim().isEmpty())
            throw new IllegalArgumentException("inventory_event_id_required");
        if(source==null||source.trim().isEmpty())
            throw new IllegalArgumentException("money_change_reason_required");
        JSONObject state=CharacterInventory2024.reconcile(copy(original));
        if(received(state,eventId))return state;
        try{
            long before=CharacterInventory2024.coinValueCp(
                CharacterInventory2024.coinPurse(state));
            long after=Math.addExact(before,deltaCp);
            if(after<0)throw new IllegalArgumentException("insufficient_coins");
            state.put("coin_pieces",CharacterInventory2024.coinsFromCopper(after));
            state.put("coin_pieces_migration_version",1);
            JSONArray receipts=state.optJSONArray("inventory_event_receipts");
            if(receipts==null)receipts=new JSONArray();
            receipts.put(eventId);
            state.put("inventory_event_receipts",receipts);
            record(state,eventId,"coins_adjust",source,actor,
                new JSONObject().put("delta_cp",deltaCp)
                    .put("before_cp",before).put("after_cp",after));
            return CharacterInventory2024.reconcile(state);
        }catch(IllegalArgumentException e){throw e;}
        catch(Exception e){throw new IllegalStateException("coin_transaction_failed",e);}
    }

    /**
     * Moves some or all of one item stack to another character.
     * The result is deliberately an envelope: source and recipient MUST be
     * committed in one database transaction or neither is committed.
     * Caller must validate distinct persistent sheet IDs.
     */
    public static JSONObject transfer(JSONObject from,JSONObject to,
                   String itemId,int quantity,String eventId,String actor){
        if(from==null||to==null)throw new IllegalArgumentException("two_sheets_required");
        if(eventId==null||eventId.trim().isEmpty())
            throw new IllegalArgumentException("inventory_event_id_required");
        if(quantity<=0)throw new IllegalArgumentException("positive_transfer_quantity_required");
        if(itemId==null||itemId.trim().isEmpty())
            throw new IllegalArgumentException("item_id_required");
        JSONObject sender=CharacterInventory2024.reconcile(copy(from));
        JSONObject recipient=CharacterInventory2024.reconcile(copy(to));
        if(!sender.has("id")||!recipient.has("id")
                 ||sender.optString("id").equals(recipient.optString("id")))
            throw new IllegalArgumentException("distinct_character_ids_required");
        String outEvent=eventId+":out",inEvent=eventId+":in";
        boolean outDone=received(sender,outEvent),inDone=received(recipient,inEvent);
        if(outDone!=inDone)throw new IllegalStateException("incomplete_prior_transfer_review");
        try{
            if(outDone)return new JSONObject().put("source",sender).put("target",recipient)
                .put("transfer_event_id",eventId).put("replayed",true);
            JSONArray ledger=sender.optJSONArray("inventory_ledger");
            JSONObject original=null;
            for(int i=0;i<ledger.length();i++){
                JSONObject x=ledger.optJSONObject(i);
                if(x!=null&&itemId.equals(x.optString("id",""))){
                    if(original!=null)throw new IllegalArgumentException("ambiguous_item_id");
                    original=x;
                }
            }
            if(original==null)throw new IllegalArgumentException("item_not_found");
            if(original.optInt("quantity",1)<quantity)
                throw new IllegalArgumentException("quantity_insufficient");
            JSONObject split=copy(original);
            split.put("id","transfer:"+eventId);
            split.put("quantity",quantity);
            split.put("source","transfer");
            split.put("obtained_from_character_id",sender.optString("id"));
            split.put("acquisition_event_id",eventId);
            split.put("carried",true);
            split.remove("location");
            JSONObject outgoing=action("adjust",outEvent,itemId).put("delta",-quantity);
            JSONObject incoming=action("add",inEvent,null).put("item",split)
                .put("source","transfer");
            JSONObject newSource=CharacterInventory2024.applyMutation(sender,outgoing);
            JSONObject newTarget=CharacterInventory2024.applyMutation(recipient,incoming);
            record(newSource,outEvent,"transfer_out",itemId,actor,
                new JSONObject().put("to",recipient.optString("id")).put("quantity",quantity));
            record(newTarget,inEvent,"transfer_in",itemId,actor,
                new JSONObject().put("from",sender.optString("id")).put("quantity",quantity));
            return new JSONObject().put("source",CharacterInventory2024.reconcile(newSource))
                    .put("target",CharacterInventory2024.reconcile(newTarget))
                    .put("transfer_event_id",eventId)
                    .put("requires_atomic_commit",true);
        }catch(IllegalArgumentException e){throw e;}
        catch(Exception e){throw new IllegalStateException("transfer_failed",e);}
    }
}
