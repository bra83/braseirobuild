package com.braseiro.dnd5e.adventure;
import org.json.JSONObject;
/** Only the absent Android D&D dependency for isolated compilation; test does not exercise this stub. */
public class AdventureRulesAdapter{
 public JSONObject evaluateExplicitRoll(JSONObject q,int a,int b,int m,boolean adv,boolean dis){
  int t=(adv&&!dis)?Math.max(a,b):(dis&&!adv)?Math.min(a,b):a;
  return new JSONObject().put("resolved",a>=1&&a<=20).put("success",t+m>=q.optInt("dc",10));
 }
}
