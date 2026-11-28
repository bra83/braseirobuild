package com.braseiro.dnd5e;
import java.util.Random;
import org.json.JSONObject;
/** Select once per new adventure; explicit fields override procedural defaults. */
final class AdventureInitialEnvironment {
 private AdventureInitialEnvironment(){}
 static JSONObject choose(JSONObject opening,JSONObject terrain,long seed)throws Exception{
  if(opening==null)opening=new JSONObject();
  if(terrain==null)terrain=new JSONObject();
  Random random=new Random(seed);
  double hour=opening.optDouble("clock_hour",Double.NaN);
  String label=opening.optString("time_of_day","");
  if(!Double.isFinite(hour)||hour<0||hour>=24){
   if(!label.isEmpty())hour=SceneTemporalResolver.hourForLabel(label);
   else{double[] starts={5.5,7,10.5,12.5,15,17.5,19,21,23};hour=starts[random.nextInt(starts.length)];}
  }
  String weather=opening.optString("weather","").trim();
  String geo=NpcIdentityNormalizer.key(terrain.optString("terrain_primary")+" "+terrain.optString("terrain")+" "+opening.optString("terrain")+" "+opening.optString("season"));
  if(weather.isEmpty()){
   String[] choices;
   if(geo.matches(".*\\b(desert|deserto|arido|duna)\\b.*"))choices=new String[]{"Céu limpo","Tempo seco e vento","Parcialmente nublado"};
   else if(geo.matches(".*\\b(polar|snow|neve|tundra|inverno)\\b.*"))choices=new String[]{"Frio seco","Nublado e frio","Neve leve"};
   else if(geo.matches(".*\\b(swamp|pantano|marsh|brejo)\\b.*"))choices=new String[]{"Névoa localizada","Nublado e úmido","Garoa"};
   else choices=new String[]{"Céu limpo","Parcialmente nublado","Nublado","Vento moderado","Garoa"};
   weather=choices[random.nextInt(choices.length)];
  }
  label=SceneTemporalResolver.labelForHour(hour);
  String lighting=SceneTemporalResolver.resolveLighting(label,"",opening.optString("lighting",""));
  return new JSONObject().put("clock_hour",hour).put("time_of_day",label).put("weather",weather).put("lighting",lighting);
 }
}