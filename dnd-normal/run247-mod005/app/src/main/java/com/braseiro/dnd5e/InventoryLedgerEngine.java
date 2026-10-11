package com.braseiro.dnd5e;
import org.json.*;
/** Ledger quantitativo de inventário. Somente pesos verificados entram na carga automática. */
public final class InventoryLedgerEngine{
 private InventoryLedgerEngine(){}
 /** Authorizes a quantity change without mutating the input ledger. */
 public static JSONObject authorizeQuantityChange(JSONArray ledger,String item,int delta){
  JSONObject out=new JSONObject();
  try{
   if(ledger==null)return blocked("ledger_required");
   if(item==null||item.trim().isEmpty())return blocked("item_required");
   if(delta==0)return blocked("quantity_delta_zero");
   int found=-1,previous=0;String key=item.trim();
   for(int i=0;i<ledger.length();i++){
    JSONObject x=ledger.optJSONObject(i);if(x==null)return blocked("invalid_ledger_entry");
    if(key.equals(x.optString("id",""))||key.equalsIgnoreCase(x.optString("name",""))){
     if(found>=0)return blocked("ambiguous_item");
     if(x.optInt("quantity",1)<0)return blocked("invalid_ledger_entry");
     found=i;previous=x.optInt("quantity",1);
    }
   }
   if(found<0)return blocked("item_not_found");
   long next=(long)previous+delta;if(next<0)return blocked("quantity_insufficient");
   if(next>Integer.MAX_VALUE)return blocked("quantity_overflow");
   JSONArray updated=new JSONArray(ledger.toString());JSONObject changed=updated.getJSONObject(found);changed.put("quantity",(int)next);updated.put(found,changed);
   return out.put("authorized",true).put("item",key).put("delta",delta).put("previous_quantity",previous).put("new_quantity",(int)next).put("ledger",updated).put("receipt","inventory:"+key+":"+previous+":"+delta+":"+next);
  }catch(Exception e){return blocked("invalid_ledger");}
 }
 private static JSONObject blocked(String reason){try{return new JSONObject().put("authorized",false).put("blocked",reason);}catch(Exception e){return new JSONObject();}}
 public static JSONObject totals(JSONArray ledger,int strength,boolean variant){double weight=0d;int stacks=0,unknown=0;JSONArray problems=new JSONArray();if(ledger==null)ledger=new JSONArray();for(int i=0;i<ledger.length();i++){JSONObject x=ledger.optJSONObject(i);if(x==null)continue;int qty=Math.max(0,x.optInt("quantity",1));if(qty==0)continue;stacks++;if(x.has("unit_weight_lb")&&x.optDouble("unit_weight_lb",-1d)>=0d)weight+=qty*x.optDouble("unit_weight_lb",0d);else{unknown++;problems.put(x.optString("name","item "+(i+1)));}}JSONObject out=new DndRulesEngine().encumbrance(strength,weight,variant);try{out.put("known_weight_lb",weight).put("stack_count",stacks).put("unknown_weight_count",unknown).put("unknown_weight_items",problems).put("complete",unknown==0);}catch(Exception ignored){}return out;}
 public static JSONArray changeQuantity(JSONArray ledger,String item,int delta){JSONArray out=new JSONArray();boolean found=false;if(ledger==null)ledger=new JSONArray();for(int i=0;i<ledger.length();i++){JSONObject x=ledger.optJSONObject(i);if(x==null)continue;try{JSONObject c=new JSONObject(x.toString());if(!found&&(c.optString("id").equals(item)||c.optString("name").equalsIgnoreCase(item))){c.put("quantity",Math.max(0,c.optInt("quantity",1)+delta));found=true;}out.put(c);}catch(Exception ignored){}}return out;}
}
