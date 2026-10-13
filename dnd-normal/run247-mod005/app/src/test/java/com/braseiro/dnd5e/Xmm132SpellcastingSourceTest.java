package com.braseiro.dnd5e;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import static org.junit.Assert.*;

/** Source-bound spellcaster coverage (132 distinct creatures, no automatic effects). */
@RunWith(Parameterized.class)
public final class Xmm132SpellcastingSourceTest {
 @Parameterized.Parameters(name="{index}: {0}")
 public static Collection<Object[]> cases(){
  ArrayList<Object[]> result=new ArrayList<>();
  JSONArray names=Xmm2025CompiledBestiaryEngine.catalog().optJSONArray("names");
  for(int i=0;i<names.length();i++){
   String id=names.optString(i);
   JSONObject source=Xmm2025SourceIndex.get(id);
   if(source.optJSONObject("counts").optInt("spells")>0)result.add(new Object[]{id});
  }
  return result;
 }
 private final String monster;
 public Xmm132SpellcastingSourceTest(String id){monster=id;}
 @Test public void eachSpellcasterExposesSourceUsageAndNeverAutoCasts()throws Exception {
  JSONObject opt=Xmm2025SpellUsageIndex.options(monster);
  assertTrue(monster,opt.getBoolean("resolved"));
  assertFalse(opt.getBoolean("auto_rolls"));
  assertFalse(opt.getBoolean("spell_effects_applied"));
  assertEquals("XMM pinned source pointer",Xmm2025SourceIndex.get(monster).getString("ptr")+"/spellcasting",opt.getString("source_pointer"));
  JSONArray entries=opt.getJSONArray("entries");
  assertEquals(Xmm2025SourceIndex.get(monster).getJSONObject("counts").getInt("spells"),entries.length());
  for(int j=0;j<entries.length();j++){
   JSONObject entry=entries.getJSONObject(j);
   assertNotNull(entry.getJSONArray("will"));
   assertNotNull(entry.getJSONObject("daily"));
   assertNotNull(entry.getJSONObject("restLong"));
   assertNotNull(entry.getJSONObject("legendary"));
   assertNotNull(entry.getJSONObject("recharge"));
  }
 }
}
