package com.braseiro.dnd5e;
import org.json.JSONObject;
/** Dependency-light scale guard usable even when baseline-only WorldStateEngine is unavailable. */
public final class WorldScaleSafetyEngine{
 private WorldScaleSafetyEngine(){}
 public static JSONObject validate(int npcs,int factions,int missions,int projects,int clocks,int snapshots){JSONObject o=new JSONObject();try{
  boolean nonnegative=npcs>=0&&factions>=0&&missions>=0&&projects>=0&&clocks>=0&&snapshots>=0;
  long total=(long)npcs+factions+missions+projects+clocks+snapshots;
  boolean bounded=npcs<=10000&&factions<=2000&&missions<=10000&&projects<=10000&&clocks<=20000&&snapshots<=100000;
  o.put("valid",nonnegative&&bounded).put("total_entities",total).put("npcs",npcs).put("factions",factions).put("missions",missions).put("projects",projects).put("clocks",clocks).put("snapshots",snapshots).put("policy",bounded?"within_local_safety_envelope":"reject_outside_local_safety_envelope");
 }catch(Exception ignored){}return o;}
}
