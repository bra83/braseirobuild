package com.braseiro.dnd5e;

import org.json.JSONObject;

/**
 * Android SQLite boundary for approved 2024 equipment mutations.
 * Invoke off the UI thread after the master/player approval gate.
 * Uses the app's existing atomic single-character persistence contract.
 */
public final class InventoryPersistence2024 {
    private InventoryPersistence2024(){}

    private static JSONObject load(AppDb db,long id)throws Exception{
        if(db==null||id<=0)throw new IllegalArgumentException("db_and_hero_id_required");
        JSONObject row=db.getCharacter(id);
        if(row==null||row.length()==0)throw new IllegalArgumentException("character_not_found");
        String raw=row.optString("sheet_json","");
        if(raw.trim().isEmpty())throw new IllegalStateException("character_sheet_missing");
        JSONObject sheet=new JSONObject(raw);
        sheet.put("id",id);
        if(!Character2024SheetReconciler.applies(sheet))
            throw new IllegalArgumentException("requires_2024_character");
        return sheet;
    }
    private static JSONObject commit(AppDb db,long id,JSONObject sheet,String actor)throws Exception{
        JSONObject receipt=db.saveCharacterSheetAtomic(id,true,sheet,"",
            actor==null||actor.trim().isEmpty()?"master":actor);
        if(!receipt.optBoolean("character_saved",false) ||
                 receipt.optLong("character_id",-1)!=id)
            throw new IllegalStateException("character_sheet_commit_failed");
        return new JSONObject().put("saved",true).put("character_id",id)
                .put("receipt",receipt).put("sheet",sheet);
    }
    public static synchronized JSONObject commitApprovedChange(AppDb db,long id,
                       JSONObject mutation,String actor){
        try{
            JSONObject current=load(db,id);
            JSONObject changed=CharacterInventory2024.applyMutation(current,mutation);
            return commit(db,id,changed,actor);
        }catch(IllegalArgumentException e){throw e;}
        catch(Exception e){throw new IllegalStateException("inventory_commit_failed",e);}
    }
    public static synchronized JSONObject commitApprovedAcquisition(AppDb db,long id,
            JSONObject item,String source,String eventId,JSONObject authorization,String actor){
        try{
            JSONObject current=load(db,id);
            JSONObject changed=InventoryTransactions2024.acquire(current,item,source,
                eventId,authorization);
            return commit(db,id,changed,actor);
        }catch(IllegalArgumentException e){throw e;}
        catch(Exception e){throw new IllegalStateException("inventory_acquisition_commit_failed",e);}
    }
    public static synchronized JSONObject commitApprovedCurrencyChange(AppDb db,long id,
                      long deltaCp,String source,String eventId,String actor){
        try{
            JSONObject current=load(db,id);
            JSONObject changed=InventoryTransactions2024.adjustCoins(
                current,deltaCp,source,eventId,actor);
            return commit(db,id,changed,actor);
        }catch(IllegalArgumentException e){throw e;}
        catch(Exception e){throw new IllegalStateException("inventory_currency_commit_failed",e);}
    }
}
