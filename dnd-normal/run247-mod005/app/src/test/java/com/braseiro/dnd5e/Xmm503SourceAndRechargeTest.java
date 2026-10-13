package com.braseiro.dnd5e;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.util.Arrays;
import java.util.Collection;
import static org.junit.Assert.*;

/** Executable 503-case regression of pinned source stats and explicit recharge. */
@RunWith(Parameterized.class)
public final class Xmm503SourceAndRechargeTest {
 @Parameterized.Parameters(name="{index}: {0}")
 public static Collection<Object[]> cases(){
   JSONArray names=Xmm2025SourceIndex.ids();Object[][] rows=new Object[names.length()][1];
   for(int i=0;i<names.length();i++)rows[i][0]=names.optString(i);
   return Arrays.asList(rows);
 }
 @BeforeClass public static void installOnce() throws Exception { assertEquals(503,Xmm2025CompiledBestiaryEngine.install().getInt("statblocks_installed")); }
 private final String id;
 public Xmm503SourceAndRechargeTest(String id){this.id=id;}
 @Test public void everyCreatureRetainsItsSourceStatsAndManualRecharge() throws Exception {
   JSONObject ref=Xmm2025SourceIndex.get(id),raw=CompiledStatblockEngine.get(id);
   assertEquals(ref.getInt("ac"),raw.getInt("armor_class"));
   assertEquals(ref.getInt("hp"),raw.getInt("hit_points"));
   assertEquals(ref.getString("formula"),raw.getString("hit_points_formula"));
   assertEquals(ref.getString("type"),raw.getString("creature_type"));
   assertEquals(ref.getString("ptr"),raw.getJSONObject("source_reference").getString("ptr"));
   for(String a:new String[]{"str","dex","con","int","wis","cha"})
      assertEquals(ref.getJSONObject("abilities").getInt(a),raw.getJSONObject("abilities").getInt(a));
   assertEquals(ref.getJSONObject("counts").getInt("spells")>0,raw.getBoolean("spellcasting_manual_resolution_required"));
   JSONArray rr=ref.getJSONArray("recharges");
   for(int i=0;i<rr.length();i++){
      JSONObject m=rr.getJSONObject(i);String cat=m.getString("cat");
      JSONArray actions="trait".equals(cat)?raw.getJSONArray("traits"):
        "action".equals(cat)?raw.getJSONArray("actions"):raw.getJSONArray("special_actions");
      int slot=m.getInt("index");
      if(!"trait".equals(cat)&&!"action".equals(cat)){
        int found=-1,j=0;for(int k=0;k<actions.length();k++)
          if(cat.equals(actions.getJSONObject(k).optString("category"))&&j++==slot){found=k;break;}
        slot=found;
      }
      assertTrue(slot>=0);JSONObject action=actions.getJSONObject(slot);
      assertEquals(m.getString("name"),action.getString("name"));
      assertEquals(m.getInt("min"),action.getInt("recharge_min_d6"));
      assertFalse(action.getBoolean("recharge_auto_rolls"));
      assertTrue(action.getBoolean("recharge_roll_required"));
   }
   if("Ankheg".equals(id)){
      JSONObject bite=raw.getJSONArray("actions").getJSONObject(0);
      assertEquals("Bite",bite.getString("name"));
      assertEquals(13,bite.getInt("escape_dc"));
      assertFalse("escape DC must never be represented as a saving throw",bite.has("save_dc"));
      assertEquals("grappled",bite.getString("condition_on_hit"));
      assertEquals("large",bite.getString("condition_max_target_size"));
      assertEquals("unsupported_grapple_escape_rider",
         MonsterActionEconomyGuard.attackRiderBlock(bite));
   }
   assertFalse(raw.getBoolean("auto_rolls"));
   assertFalse(raw.getBoolean("full_statblock_claim"));
 }
}
