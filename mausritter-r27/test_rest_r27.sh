#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
find "$ROOT/tools/runtime_json" -name '*.java' > "$TMP/sources"
echo "$ROOT/android/app/src/main/java/com/braseiro/dnd/MausritterRestEngine.java" >> "$TMP/sources"
cat > "$TMP/RestProbe.java" <<'JAVA'
import org.json.JSONObject;
import org.json.JSONArray;
import com.braseiro.dnd.MausritterRestEngine;
public class RestProbe {
 static void yes(boolean x,String m){if(!x)throw new AssertionError(m);}
 static JSONObject item(String id,int spent) throws Exception{
   return new JSONObject().put("instance_id","r-"+id).put("item_id",id)
          .put("type","pack").put("uses",3).put("spent",spent);
 }
 static JSONObject condition(String id) throws Exception{
   return new JSONObject().put("condition_id",id).put("instance_id","c-"+id).put("type","conditions");
 }
 static JSONObject sheet()throws Exception{
   return new JSONObject().put("hp",2).put("max_hp",9)
          .put("str",4).put("max_str",10)
          .put("dex",5).put("max_dex",9)
          .put("wil",3).put("max_wil",8)
          .put("grains",25).put("inventory",new JSONArray()
           .put(item("rations",0)).put(condition("hungry"))
           .put(condition("exhausted")).put(condition("cursed")));
 }
 static int uses(JSONObject sheet){return sheet.getJSONArray("inventory").getJSONObject(0).optInt("spent",0);}
 static boolean has(JSONObject sheet,String id){
   JSONArray a=sheet.optJSONArray("inventory");for(int i=0;i<a.length();i++)
     if(id.equals(a.optJSONObject(i).optString("condition_id")))return true;
   return false;
 }
 public static void main(String[] args)throws Exception{
   JSONObject s=sheet(),r=MausritterRestEngine.apply(s,"short","",4,false);
   yes(s.optInt("hp")==7,"short d6+1");yes(r.optInt("durationTurns")==1&&r.optInt("durationWatches")==0,"short clock");
   yes(uses(s)==0,"short does not eat");
   s=sheet();s.getJSONArray("inventory").put(condition("sick"));
   r=MausritterRestEngine.apply(s,"short","",6,false);
   yes(s.optInt("hp")==3,"sickness restricts short healing to 1");
   s=sheet();r=MausritterRestEngine.apply(s,"long","str",4,false);
   yes(s.optInt("hp")==9&&uses(s)==1&&r.optInt("durationWatches")==1,"long healing and ration");
   yes(!has(s,"hungry")&&!has(s,"exhausted"),"long removes hungry and exhausted");
   s=sheet();s.put("hp",9);r=MausritterRestEngine.apply(s,"long","str",4,false);
   yes(s.optInt("str")==8&&r.optInt("restoredPoints")==4&&r.optInt("healedHp")==0,"long fullHP restores stat");
   s=sheet();s.getJSONArray("inventory").getJSONObject(0).put("spent",3);
   boolean blocked=false;try{MausritterRestEngine.apply(s,"long","str",6,false);}catch(IllegalStateException e){blocked=true;}
   yes(blocked&&s.optInt("hp")==2,"no ration blocks without mutation");
   s=sheet();blocked=false;try{MausritterRestEngine.apply(s,"full","",4,false);}catch(IllegalStateException e){blocked=true;}
   yes(blocked&&s.optInt("grains")==25,"full requires safe location");
   r=MausritterRestEngine.apply(s,"full","",4,true);
   yes(s.optInt("grains")==5&&s.optInt("hp")==9&&s.optInt("str")==10&&r.optInt("durationWatches")==28,"full safe week cost/healing");
   yes(has(s,"cursed")&&!has(s,"hungry")&&!has(s,"exhausted"),"full preserves unresolved special curse");
   s=sheet();s.put("grains",19);blocked=false;
   try{MausritterRestEngine.apply(s,"full","",4,true);}catch(IllegalStateException e){blocked=true;}
   yes(blocked&&s.optInt("grains")==19,"full insufficient funds");
   System.out.println("R27_REST_EXECUTABLE_PASS short/long/full/conditions/ration/stats/safe/cost");
 }
}
JAVA
javac -encoding UTF-8 -d "$TMP" @"$TMP/sources" "$TMP/RestProbe.java"
java -cp "$TMP" RestProbe
