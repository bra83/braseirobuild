package org.json;
import java.util.*;
public class JSONObject {
 static final Map<String,JSONObject> snapshots = new HashMap<>();
 final LinkedHashMap<String,Object> data=new LinkedHashMap<>();
 public JSONObject(){}
 public JSONObject(String source){JSONObject p=snapshots.get(source);if(p==null)throw new IllegalArgumentException("test snapshot not found "+source);for(Map.Entry<String,Object> e:p.data.entrySet())data.put(e.getKey(),deep(e.getValue()));}
 private static Object deep(Object o){if(o instanceof JSONObject){JSONObject p=new JSONObject();for(Map.Entry<String,Object> e:((JSONObject)o).data.entrySet())p.data.put(e.getKey(),deep(e.getValue()));return p;}if(o instanceof JSONArray){JSONArray p=new JSONArray();for(Object v:((JSONArray)o).data)p.put(deep(v));return p;}return o;}
 public JSONObject put(String k,Object value){data.put(k,value);return this;}
 public Object opt(String k){return data.get(k);}
 public String optString(String k){return optString(k,"");}
 public String optString(String k,String defaultValue){Object v=data.get(k);return v==null?defaultValue:String.valueOf(v);}
 public boolean optBoolean(String k,boolean def){Object v=data.get(k);return v instanceof Boolean?(Boolean)v:def;}
 public boolean optBoolean(String k){return optBoolean(k,false);}
 public int optInt(String k,int def){Object v=data.get(k);return v instanceof Number?((Number)v).intValue():def;}
 public int optInt(String k){return optInt(k,0);}
 public JSONArray optJSONArray(String k){return data.get(k) instanceof JSONArray?(JSONArray)data.get(k):null;}
 public JSONObject optJSONObject(String k){return data.get(k) instanceof JSONObject?(JSONObject)data.get(k):null;}
 public String getString(String k){return optString(k);}
 public JSONObject getJSONObject(String k){return optJSONObject(k);}
 public JSONArray getJSONArray(String k){return optJSONArray(k);}
 public boolean getBoolean(String k){return optBoolean(k,false);}
 public JSONArray names(){JSONArray a=new JSONArray();for(String k:data.keySet())a.put(k);return a;}
 public int length(){return data.size();}
 public boolean has(String k){return data.containsKey(k);}
 public Object remove(String k){return data.remove(k);}
 @Override public String toString(){String id="jsonsnapshot:"+UUID.randomUUID();JSONObject clone=new JSONObject();for(Map.Entry<String,Object> e:data.entrySet())clone.data.put(e.getKey(),deep(e.getValue()));snapshots.put(id,clone);return id;}
}
