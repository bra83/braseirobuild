package com.braseiro.dnd5e;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.Iterator;
/** Progressão por XP: detecta nível devido sem inventar escolhas de classe. */
public final class DndProgressionEngine{
 private static final int[] XP={0,300,900,2700,6500,14000,23000,34000,48000,64000,85000,100000,120000,140000,165000,195000,225000,265000,305000,355000};
 private DndProgressionEngine(){}
 public static int levelForXp(int xp){int lvl=1;for(int i=0;i<XP.length;i++)if(xp>=XP[i])lvl=i+1;return lvl;}
 public static int thresholdForLevel(int level){return XP[Math.max(1,Math.min(20,level))-1];}
 /**
  * Retorna uma nova ficha com XP concedido, preservando todos os outros campos.
  * XP negativo é rejeitado (null) e a soma é saturada para não transbordar.
  */
 public static JSONObject awardXp(JSONObject sheet,int award){
  if(sheet==null||award<0)return null;
  try{
   JSONObject updated=new JSONObject();
   Iterator<String> keys=sheet.keys();
   while(keys.hasNext()){
    String key=keys.next();
    updated.put(key,sheet.get(key));
   }
   long current=Math.min((long)Integer.MAX_VALUE,Math.max(0L,sheet.optLong("xp",0L)));
   long total=Math.min((long)Integer.MAX_VALUE,current+(long)award);
   updated.put("xp",(int)total);
   return updated;
  }catch(JSONException ignored){return null;}
 }
 public static JSONObject state(JSONObject sheet,int currentLevel){JSONObject out=new JSONObject();try{int xp=Math.max(0,sheet==null?0:sheet.optInt("xp",0));int current=Math.max(1,Math.min(20,currentLevel));int due=Math.max(current,levelForXp(xp));out.put("xp",xp).put("current_level",current).put("level_due",due).put("level_up_pending",due>current).put("next_level_xp",current>=20?JSONObject.NULL:XP[current]);}catch(Exception ignored){}return out;}
}
