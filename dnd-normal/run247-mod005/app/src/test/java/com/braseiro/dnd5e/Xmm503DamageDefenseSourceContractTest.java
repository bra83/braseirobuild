package com.braseiro.dnd5e;

import java.util.ArrayList;
import java.util.Collection;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/** All 503 source-indexed defenders independently checked against their installed
 * compiled mechanical records. Conditional fields remain explicitly unresolved.
 */
@RunWith(Parameterized.class)
public final class Xmm503DamageDefenseSourceContractTest {
 @Parameterized.Parameters(name="{index}: {0} defense")
 public static Collection<Object[]> monsters(){
  JSONArray names=Xmm2025SourceIndex.ids();ArrayList<Object[]> out=new ArrayList<>();
  for(int i=0;i<names.length();i++)out.add(new Object[]{names.optString(i)});
  return out;
 }
 private final String monster;
 public Xmm503DamageDefenseSourceContractTest(String monster){this.monster=monster;}
 @Test public void compiledDefensesMustMatchPinnedSourceOrRemainConditional()throws Exception{
  JSONObject source=Xmm2025DamageDefenseEngine.profile(monster);
  assertTrue(monster+": "+source.toString(),source.getBoolean("resolved"));
  assertEquals(Xmm2025DamageDefenseEngine.SOURCE_BLOB,source.getString("source_blob_sha1"));
  assertEquals(Xmm2025SourceIndex.get(monster).getString("ptr"),source.getString("source_pointer"));
  assertFalse(source.getBoolean("full_trait_effects_certified"));
  JSONObject req=new JSONObject().put("defender",monster).put("target_confirmed",true)
   .put("incoming_damage",11).put("damage_type","force");
  JSONObject result=Xmm2025DamageDefenseEngine.resolve(req);
  if(source.getBoolean("complex_damage")){
   assertFalse(result.getBoolean("resolved"));
   assertEquals("conditional_or_trait_dependent_defense_requires_manual_resolution",result.getString("status"));
  }else{
   assertTrue(monster+": "+result.toString(),result.getBoolean("resolved"));
   assertEquals(11,result.getInt("damage_after_basic_defenses"));
   assertFalse(result.getBoolean("damage_applied"));
   assertFalse(result.getBoolean("auto_mutation_allowed"));
  }
 }
}
