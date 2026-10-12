package com.braseiro.pathfinder2e;
import java.util.*;import org.json.*;
public class AppDb {
 public final java.util.List<JSONObject> events=new java.util.ArrayList<JSONObject>();
 public final java.util.List<JSONObject> clocks=new java.util.ArrayList<JSONObject>();
 public final java.util.List<JSONObject> quests=new java.util.ArrayList<JSONObject>();
 public final java.util.List<JSONObject> factions=new java.util.ArrayList<JSONObject>();
 public JSONObject scene=new JSONObject();
 public java.util.List<JSONObject> listWorldEventsForMaster(int n){return events;}
 public java.util.List<JSONObject> listWorldClocks(boolean active,int n){return clocks;}
 public java.util.List<JSONObject> listQuests(String status,int n){return quests;}
 public java.util.List<JSONObject> listFactions(int n){return factions;}
 public JSONObject getSceneState(){return scene;}
}
