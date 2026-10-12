package org.json;
import java.util.*;
public class JSONArray {
 final java.util.List<Object> values=new java.util.ArrayList<Object>();
 public JSONArray(){}
 public JSONArray(String json){Object parsed=JSONObject.Parser.parse(json);if(!(parsed instanceof JSONArray))throw new IllegalArgumentException("JSON não é array");values.addAll(((JSONArray)parsed).values);}
 public JSONArray(int[] source){if(source!=null)for(int value:source)values.add(value);}
 public JSONArray(java.util.Collection<?> source){if(source!=null)values.addAll(source);}
 public JSONArray put(Object v){values.add(v);return this;}
 public int length(){return values.size();}
 public Object opt(int i){return i>=0&&i<values.size()?values.get(i):null;}
 public JSONObject optJSONObject(int i){Object v=opt(i);return v instanceof JSONObject?(JSONObject)v:null;}
 public JSONArray optJSONArray(int i){Object v=opt(i);return v instanceof JSONArray?(JSONArray)v:null;}
 public String optString(int i){return optString(i,"");}
 public String optString(int i,String d){Object v=opt(i);return v==null?d:String.valueOf(v);}
 public int optInt(int i){return optInt(i,0);}
 public int optInt(int i,int d){Object v=opt(i);if(v instanceof Number)return ((Number)v).intValue();try{return Integer.parseInt(String.valueOf(v));}catch(Exception e){return d;}}
 public double optDouble(int i){return optDouble(i,Double.NaN);}
 public double optDouble(int i,double d){Object v=opt(i);if(v instanceof Number)return ((Number)v).doubleValue();try{return Double.parseDouble(String.valueOf(v));}catch(Exception e){return d;}}
 public String toString(){StringBuilder b=new StringBuilder("[");for(int i=0;i<values.size();i++){if(i>0)b.append(',');b.append(JSONObject.render(values.get(i)));}return b.append(']').toString();}
}
