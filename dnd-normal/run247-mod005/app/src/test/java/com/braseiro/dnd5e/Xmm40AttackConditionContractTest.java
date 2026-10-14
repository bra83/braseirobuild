package com.braseiro.dnd5e;
import java.util.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;
@RunWith(Parameterized.class)
public final class Xmm40AttackConditionContractTest {
 @Parameterized.Parameters(name="{index}: {0}/{1}")
 public static Collection<Object[]> cases()throws Exception{
  JSONArray a=Xmm2025AttackConditionEngine.catalog();ArrayList<Object[]> out=new ArrayList<>();
  for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);out.add(new Object[]{r.getString("id"),r.getString("name")});}
  return out;
 }
 @BeforeClass public static void install()throws Exception{assertTrue(Xmm2025CompiledBestiaryEngine.install().getBoolean("complete"));}
 private final String monster,action;
 public Xmm40AttackConditionContractTest(String monster,String action){this.monster=monster;this.action=action;}
 @Test public void sourceBoundOnHitRiderIsPureAndSizeGated()throws Exception{
  JSONObject r=Xmm2025AttackConditionEngine.profile(monster,action);
  assertEquals(action,CompiledStatblockEngine.get(monster).getJSONArray("actions").getJSONObject(r.getInt("slot")).getString("name"));
  JSONObject in=new JSONObject().put("monster",monster).put("action",action)
   .put("attack_hit",true).put("target_is_creature",true).put("target_size",r.getString("size"));
  if(r.optBoolean("choice_required"))in.put("chosen_effect_option","grapple");
  String before=in.toString();JSONObject out=Xmm2025AttackConditionEngine.resolve(in);
  assertTrue(monster+"/"+action+":"+out,out.getBoolean("resolved"));
  assertEquals(before,in.toString());assertEquals(r.getString("ptr"),out.getString("source_pointer"));
  assertEquals(1,out.getJSONArray("proposed_conditions").length());
  JSONObject c=out.getJSONArray("proposed_conditions").getJSONObject(0);
  assertEquals(r.getString("condition"),c.getString("condition"));
  if("grappled".equals(r.getString("condition")))assertEquals(r.getInt("dc"),c.getInt("escape_dc"));
  assertEquals(r.getBoolean("other"),out.getBoolean("additional_source_effects_manual"));
  assertFalse(out.getBoolean("auto_rolls"));assertFalse(out.getBoolean("auto_mutation_allowed"));
  assertFalse(out.getBoolean("condition_applied"));assertFalse(out.getBoolean("damage_applied"));
  assertFalse(out.getBoolean("full_action_effects_certified"));
  JSONObject miss=Xmm2025AttackConditionEngine.resolve(in.put("attack_hit",false));
  assertTrue(miss.getBoolean("resolved"));assertEquals(0,miss.getJSONArray("proposed_conditions").length());
 }
}
