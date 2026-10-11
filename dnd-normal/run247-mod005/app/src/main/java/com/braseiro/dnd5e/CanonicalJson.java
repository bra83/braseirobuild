package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Canonicalização determinística de JSON para hashes, bindings e idempotência. */
public final class CanonicalJson {
    private CanonicalJson(){}

    public static String stringify(Object value){
        if(value==null||value==JSONObject.NULL)return "null";
        if(value instanceof JSONObject){
            JSONObject o=(JSONObject)value;List<String> keys=new ArrayList<String>();Iterator<String> it=o.keys();while(it.hasNext())keys.add(it.next());Collections.sort(keys);
            StringBuilder b=new StringBuilder("{");for(int i=0;i<keys.size();i++){if(i>0)b.append(',');String k=keys.get(i);b.append(quote(k)).append(':').append(stringify(o.opt(k)));}return b.append('}').toString();
        }
        if(value instanceof JSONArray){JSONArray a=(JSONArray)value;StringBuilder b=new StringBuilder("[");for(int i=0;i<a.length();i++){if(i>0)b.append(',');b.append(stringify(a.opt(i)));}return b.append(']').toString();}
        if(value instanceof String||value instanceof Character)return quote(String.valueOf(value));
        if(value instanceof Boolean)return ((Boolean)value)?"true":"false";
        if(value instanceof Number){String s=String.valueOf(value);if("NaN".equals(s)||"Infinity".equals(s)||"-Infinity".equals(s))return "null";return s;}
        return quote(String.valueOf(value));
    }
    private static String quote(String raw){String s=raw==null?"":raw;StringBuilder b=new StringBuilder("\"");for(int i=0;i<s.length();i++){char c=s.charAt(i);switch(c){case '\\':b.append("\\\\");break;case '"':b.append("\\\"");break;case '\b':b.append("\\b");break;case '\f':b.append("\\f");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<0x20)b.append(String.format(java.util.Locale.US,"\\u%04x",(int)c));else b.append(c);}}return b.append('\"').toString();}
    public static String sha256(Object value){return IntegrityHash.sha256(stringify(value));}
    public static JSONObject copyObject(JSONObject value){try{return value==null?new JSONObject():new JSONObject(value.toString());}catch(Exception e){return new JSONObject();}}
    public static JSONArray copyArray(JSONArray value){try{return value==null?new JSONArray():new JSONArray(value.toString());}catch(Exception e){return new JSONArray();}}
}
