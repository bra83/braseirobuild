package com.braseiro.pathfinder2e;
import java.util.*;import org.json.*;
public class AppDb {
 private final java.util.List<JSONObject> rolls=new java.util.ArrayList<JSONObject>();
 public java.util.List<JSONObject> listDiceHistory(){return rolls;}
 public JSONObject getDiceHistoryById(long id){for(JSONObject r:rolls)if(r.optLong("id",0)==id)return r;return new JSONObject();}
 public void addRoll(JSONObject roll){rolls.add(roll);}
}
