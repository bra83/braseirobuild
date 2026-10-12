package org.json;
import java.util.*;
public class JSONObject {
 public static final Object NULL=new Object();
 public JSONObject(){} public JSONObject(String s) throws JSONException {}
 public JSONObject put(String k,Object v) throws JSONException {return this;}
 public int length(){return 0;} public boolean has(String k){return false;} public boolean isNull(String k){return false;} public Object opt(String k){return null;}
 public String optString(String k){return "";} public String optString(String k,String d){return d;}
 public int optInt(String k){return 0;} public int optInt(String k,int d){return d;}
 public long optLong(String k){return 0;} public long optLong(String k,long d){return d;}
 public double optDouble(String k){return 0;} public double optDouble(String k,double d){return d;}
 public boolean optBoolean(String k){return false;} public boolean optBoolean(String k,boolean d){return d;}
 public JSONObject optJSONObject(String k){return null;} public JSONArray optJSONArray(String k){return null;}
 public Object remove(String k){return null;} public Object get(String k) throws JSONException {return null;}
 public Iterator<String> keys(){return Collections.emptyIterator();}
 public String toString(){return "{}";}
}
