package org.json;
import java.util.*;
public class JSONArray {
 static final Map<String,JSONArray> snapshots=new HashMap<>();
 final List<Object> data=new ArrayList<>();
 public JSONArray(){}
 public JSONArray(String source){JSONArray a=snapshots.get(source);if(a==null)throw new IllegalArgumentException("array snapshot not found "+source);data.addAll(a.data);}
 public JSONArray put(Object value){data.add(value);return this;}
 public int length(){return data.size();}
 public Object opt(int i){return i<0||i>=data.size()?null:data.get(i);}
 public String optString(int i){Object v=opt(i);return v==null?"":String.valueOf(v);}
 public JSONObject optJSONObject(int i){Object v=opt(i);return v instanceof JSONObject?(JSONObject)v:null;}
 public JSONArray optJSONArray(int i){Object v=opt(i);return v instanceof JSONArray?(JSONArray)v:null;}
 public JSONObject getJSONObject(int i){return optJSONObject(i);}
 @Override public String toString(){String id="arraysnapshot:"+UUID.randomUUID();JSONArray copy=new JSONArray();copy.data.addAll(data);snapshots.put(id,copy);return id;}
}
