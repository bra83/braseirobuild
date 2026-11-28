package com.braseiro.dnd5e;
import org.json.JSONObject;
final class RemoteMasterTransportPolicy {
 private RemoteMasterTransportPolicy(){}
 static boolean retryableHttp(int code){return code==408||code==429||code==500||code==502||code==503||code==504;}
 static boolean expired(long started,long progress,long now){return now-started>=360000L||now-Math.max(started,progress)>=180000L;}
 static boolean completeEnvelope(String raw){
  if(raw==null)return false;
  String text=raw.trim();
  if(text.startsWith("```")){int start=text.indexOf('\n'),end=text.lastIndexOf("```");if(start<0||end<=start)return false;text=text.substring(start+1,end).trim();}
  if(!text.startsWith("{")||!text.endsWith("}"))return false;
  try{return !new JSONObject(text).optString("narration","").trim().isEmpty();}catch(Exception e){return false;}
 }
}