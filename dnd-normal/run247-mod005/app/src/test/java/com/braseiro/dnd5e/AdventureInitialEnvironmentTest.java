package com.braseiro.dnd5e;
import org.json.JSONObject;
import org.junit.Test;
import java.util.HashSet;
import static org.junit.Assert.*;
public class AdventureInitialEnvironmentTest {
 @Test public void explicitStartWins()throws Exception{
  JSONObject given=new JSONObject().put("clock_hour",21.5).put("weather","Chuva forte");
  JSONObject actual=AdventureInitialEnvironment.choose(given,new JSONObject(),1L);
  assertEquals(21.5,actual.getDouble("clock_hour"),0.001);
  assertEquals("Noite",actual.getString("time_of_day"));assertEquals("Chuva forte",actual.getString("weather"));
 }
 @Test public void randomStartVariesWithoutRainInDesert()throws Exception{
  HashSet<Double> hours=new HashSet<>();HashSet<String> weather=new HashSet<>();
  for(int i=0;i<100;i++){
   JSONObject actual=AdventureInitialEnvironment.choose(new JSONObject(),new JSONObject().put("terrain_primary","deserto"),i);
   hours.add(actual.getDouble("clock_hour"));weather.add(actual.getString("weather"));
   assertFalse(actual.getString("weather").contains("Chuva"));assertFalse(actual.getString("weather").contains("Neve"));
  }
  assertTrue(hours.size()>3);assertTrue(weather.size()>1);
 }
}