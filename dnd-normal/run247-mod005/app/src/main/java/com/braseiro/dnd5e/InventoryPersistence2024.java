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
        // saveCharacterSheetAtomic(..., notes="", controlMode="manual") would
        // wipe existing notes and companion agency on every purchase or loot.
        // Equipment transactions must only mutate sheet_json and equipment,
        // retaining the unchanged character identity, notes and control.
        JSONObject original=db.getCharacter(id);
        if(original.length()==0)throw new IllegalArgumentException("character_not_found");
        String notes=original.optString("notes","");
        db.updateCharacterSheetChecked(id,sheet.toString());
        db.setCharacterEquipmentAndNotesChecked(id,sheet.optString("inventory",""),notes);
        JSONObject saved=db.getCharacter(id);
        if(!notes.equals(saved.optString("notes","")))
            throw new IllegalStateException("character_notes_changed");
        JSONObject persisted=new JSONObject(saved.optString("sheet_json","{}"));
        if(!sheet.optString("inventory","").equals(persisted.optString("inventory","")))
            throw new IllegalStateException("inventory_projection_not_persisted");
        return new JSONObject().put("saved",true).put("character_id",id)
                .put("sheet",persisted);
    }
    public static synchronized JSONObject commitApprovedChange(AppDb db,long id,
                       JSONObject mutation,String actor){
        try{
            final JSONObject[] result={new JSONObject()};
            db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
                JSONObject current=load(db,id);
                JSONObject changed=CharacterInventory2024.applyMutation(current,mutation);
                result[0]=commit(db,id,changed,actor);
            }});
            return result[0];
        }catch(IllegalArgumentException e){throw e;}
        catch(Exception e){throw new IllegalStateException("inventory_commit_failed",e);}
    }
    public static synchronized JSONObject commitApprovedAcquisition(AppDb db,long id,
            JSONObject item,String source,String eventId,JSONObject authorization,String actor){
        try{
            final JSONObject[] result={new JSONObject()};
            db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
                JSONObject current=load(db,id);
                JSONObject changed=InventoryTransactions2024.acquire(current,item,source,
                    eventId,authorization);
                result[0]=commit(db,id,changed,actor);
            }});
            return result[0];
        }catch(IllegalArgumentException e){throw e;}
        catch(Exception e){throw new IllegalStateException("inventory_acquisition_commit_failed",e);}
    }
    public static synchronized JSONObject commitApprovedCurrencyChange(AppDb db,long id,
                      long deltaCp,String source,String eventId,String actor){
        try{
            final JSONObject[] result={new JSONObject()};
            db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
                JSONObject current=load(db,id);
                JSONObject changed=InventoryTransactions2024.adjustCoins(
                    current,deltaCp,source,eventId,actor);
                result[0]=commit(db,id,changed,actor);
            }});
            return result[0];
        }catch(IllegalArgumentException e){throw e;}
        catch(Exception e){throw new IllegalStateException("inventory_currency_commit_failed",e);}
    }
    /**
     * Truly atomic party transfer: two independent sheets, one SQLite transaction.
     * A failed second save rolls the first one back.
     */
    public static synchronized JSONObject commitApprovedTransfer(final AppDb db,
            final long fromId,final long toId,final String itemId,final int quantity,
            final String eventId,final String actor){
        if(db==null || fromId<=0 || toId<=0 || fromId==toId)
            throw new IllegalArgumentException("distinct_character_ids_required");
        final JSONObject[] committed={new JSONObject()};
        try{
            db.runInTransactionChecked(new AppDb.CheckedTransaction(){
                public void run()throws Exception{
                    JSONObject source=load(db,fromId);
                    JSONObject destination=load(db,toId);
                    JSONObject change=InventoryTransactions2024.transfer(
                        source,destination,itemId,quantity,eventId,actor);
                    if(!change.optBoolean("replayed",false)){
                        commit(db,fromId,change.getJSONObject("source"),actor);
                        commit(db,toId,change.getJSONObject("target"),actor);
                    }
                    committed[0]=new JSONObject()
                        .put("saved",true).put("replayed",change.optBoolean("replayed",false))
                        .put("source_id",fromId).put("target_id",toId)
                        .put("transfer_event_id",eventId);
                }
            });
            return committed[0];
        }catch(IllegalArgumentException e){throw e;}
        catch(Exception e){throw new IllegalStateException("inventory_transfer_rolled_back",e);}
    }

}
