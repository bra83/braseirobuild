package com.braseiro.dnd5e.adventure;
public final class AdventureModule {
 private final String id;
 public AdventureModule(String id){this.id=id;}
 public static AdventureModule createSampleModule(String id,String title){return new AdventureModule(id);}
 public static AdventureModule fromJson(org.json.JSONObject o){return new AdventureModule(o.optString("id"));}
 public String getId(){return id;}
}
