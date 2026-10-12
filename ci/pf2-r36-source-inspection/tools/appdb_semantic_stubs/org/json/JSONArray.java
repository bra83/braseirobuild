package org.json;
public class JSONArray {
 public JSONArray(){} public JSONArray(String s) throws JSONException {} public JSONArray(java.util.Collection<?> c){} public JSONArray(Object array) throws JSONException {}
 public JSONArray put(Object v){return this;} public int length(){return 0;} public Object opt(int i){return null;}
 public JSONObject getJSONObject(int i) throws JSONException {return null;} public JSONObject optJSONObject(int i){return null;} public JSONArray optJSONArray(int i){return null;}
 public String optString(int i){return "";} public String optString(int i,String d){return d;}
 public long optLong(int i){return 0;} public int optInt(int i){return 0;} public int optInt(int i,int d){return d;} public Object remove(int i){return null;} public String toString(){return "[]";}
}
