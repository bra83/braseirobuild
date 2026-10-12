package org.json;
import java.util.*;
public class JSONObject {
 public static final Object NULL=new Object();
 final java.util.LinkedHashMap<String,Object> values=new java.util.LinkedHashMap<String,Object>();
 public JSONObject(){}
 public JSONObject(String json){Object parsed=Parser.parse(json);if(!(parsed instanceof JSONObject))throw new IllegalArgumentException("JSON não é objeto");values.putAll(((JSONObject)parsed).values);}
 public JSONObject put(String k,Object v) throws JSONException {if(k==null)throw new JSONException("null key");values.put(k,v);return this;}
 public int length(){return values.size();}
 public boolean has(String k){return values.containsKey(k);}
 public boolean isNull(String k){Object v=values.get(k);return v==null||v==NULL;}
 public Object opt(String k){return values.get(k);}
 public String optString(String k){return optString(k,"");}
 public String optString(String k,String d){Object v=values.get(k);return v==null?d:String.valueOf(v);}
 public int optInt(String k){return optInt(k,0);}
 public int optInt(String k,int d){Object v=values.get(k);if(v instanceof Number)return ((Number)v).intValue();try{return Integer.parseInt(String.valueOf(v));}catch(Exception e){return d;}}
 public long optLong(String k){return optLong(k,0L);}
 public long optLong(String k,long d){Object v=values.get(k);if(v instanceof Number)return ((Number)v).longValue();try{return Long.parseLong(String.valueOf(v));}catch(Exception e){return d;}}
 public double optDouble(String k){return optDouble(k,0d);}
 public double optDouble(String k,double d){Object v=values.get(k);if(v instanceof Number)return ((Number)v).doubleValue();try{return Double.parseDouble(String.valueOf(v));}catch(Exception e){return d;}}
 public boolean optBoolean(String k,boolean d){Object v=values.get(k);if(v instanceof Boolean)return (Boolean)v;if(v==null)return d;return Boolean.parseBoolean(String.valueOf(v));}
 public boolean optBoolean(String k){return optBoolean(k,false);}
 public JSONObject optJSONObject(String k){Object v=values.get(k);return v instanceof JSONObject?(JSONObject)v:null;}
 public JSONArray optJSONArray(String k){Object v=values.get(k);return v instanceof JSONArray?(JSONArray)v:null;}
 public java.util.Iterator<String> keys(){return values.keySet().iterator();}
 public String toString(){StringBuilder b=new StringBuilder("{");boolean first=true;for(Map.Entry<String,Object> e:values.entrySet()){if(!first)b.append(',');first=false;b.append(render(e.getKey())).append(':').append(render(e.getValue()));}return b.append('}').toString();}
 static String render(Object v){if(v==null||v==NULL)return "null";if(v instanceof String)return "\""+((String)v).replace("\\","\\\\").replace("\"","\\\"")+"\"";if(v instanceof Number||v instanceof Boolean)return String.valueOf(v);return String.valueOf(v);}

 static final class Parser {
  private final String text;private int pos;
  private Parser(String source){text=source==null?"":source.trim();}
  static Object parse(String source){Parser p=new Parser(source);Object value=p.value();p.space();if(p.pos!=p.text.length())throw new IllegalArgumentException("JSON possui conteúdo extra");return value;}
  private Object value(){space();if(pos>=text.length())throw new IllegalArgumentException("JSON vazio");char c=text.charAt(pos);if(c=='{')return object();if(c=='[')return array();if(c=='\"')return string();if(text.startsWith("true",pos)){pos+=4;return Boolean.TRUE;}if(text.startsWith("false",pos)){pos+=5;return Boolean.FALSE;}if(text.startsWith("null",pos)){pos+=4;return NULL;}return number();}
  private JSONObject object(){JSONObject out=new JSONObject();expect('{');space();if(take('}'))return out;while(true){space();String key=string();space();expect(':');out.values.put(key,value());space();if(take('}'))return out;expect(',');}}
  private JSONArray array(){JSONArray out=new JSONArray();expect('[');space();if(take(']'))return out;while(true){out.values.add(value());space();if(take(']'))return out;expect(',');}}
  private String string(){expect('\"');StringBuilder out=new StringBuilder();while(pos<text.length()){char c=text.charAt(pos++);if(c=='\"')return out.toString();if(c!='\\'){out.append(c);continue;}if(pos>=text.length())throw new IllegalArgumentException("escape incompleto");char e=text.charAt(pos++);switch(e){case '\"':case '\\':case '/':out.append(e);break;case 'b':out.append('\b');break;case 'f':out.append('\f');break;case 'n':out.append('\n');break;case 'r':out.append('\r');break;case 't':out.append('\t');break;case 'u':if(pos+4>text.length())throw new IllegalArgumentException("unicode incompleto");out.append((char)Integer.parseInt(text.substring(pos,pos+4),16));pos+=4;break;default:throw new IllegalArgumentException("escape inválido");}}throw new IllegalArgumentException("string não encerrada");}
  private Number number(){int start=pos;if(pos<text.length()&&(text.charAt(pos)=='-'||text.charAt(pos)=='+'))pos++;while(pos<text.length()&&Character.isDigit(text.charAt(pos)))pos++;boolean decimal=false;if(pos<text.length()&&text.charAt(pos)=='.'){decimal=true;pos++;while(pos<text.length()&&Character.isDigit(text.charAt(pos)))pos++;}if(pos<text.length()&&(text.charAt(pos)=='e'||text.charAt(pos)=='E')){decimal=true;pos++;if(pos<text.length()&&(text.charAt(pos)=='+'||text.charAt(pos)=='-'))pos++;while(pos<text.length()&&Character.isDigit(text.charAt(pos)))pos++;}if(start==pos)throw new IllegalArgumentException("valor JSON inválido");String raw=text.substring(start,pos);try{return decimal?Double.valueOf(raw):Long.valueOf(raw);}catch(Exception error){throw new IllegalArgumentException("número inválido",error);}}
  private void space(){while(pos<text.length()&&Character.isWhitespace(text.charAt(pos)))pos++;}
  private boolean take(char expected){if(pos<text.length()&&text.charAt(pos)==expected){pos++;return true;}return false;}
  private void expect(char expected){space();if(!take(expected))throw new IllegalArgumentException("esperado '"+expected+"'");}
 }
}
