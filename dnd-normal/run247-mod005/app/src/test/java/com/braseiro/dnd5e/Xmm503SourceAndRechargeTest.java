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
   JSONArray grappleContracts=Xmm2025AttackConditionEngine.catalog();
   for(int i=0;i<grappleContracts.length();i++){
      JSONObject contract=grappleContracts.getJSONObject(i);
      if(!id.equals(contract.optString("id"))||!"grappled".equals(contract.optString("condition")))continue;
      JSONObject action=raw.getJSONArray("actions").getJSONObject(contract.getInt("slot"));
      assertEquals(contract.getString("name"),action.getString("name"));
      assertEquals(contract.getInt("dc"),action.getInt("escape_dc"));
      if(contract.has("save_dc")){
         assertEquals(contract.getInt("save_dc"),action.getInt("save_dc"));
         assertEquals(contract.getString("save_ability"),action.getString("save_ability"));
         assertTrue(action.has("condition_on_fail"));
      }else{
         assertFalse("escape DC must never be represented as a saving throw",action.has("save_dc"));
         assertFalse(action.has("save_ability"));
         assertFalse(action.has("condition_on_fail"));
      }
      assertTrue(action.getString("condition_on_hit").contains("grappled"));
      assertEquals(contract.getString("size").toLowerCase(java.util.Locale.ROOT),
         action.getString("condition_max_target_size"));
      assertTrue(MonsterActionEconomyGuard.attackRiderBlock(action).startsWith("unsupported_"));
      if(action.getString("condition_on_hit").contains(","))
         assertEquals("unsupported_grapple_escape_rider",MonsterActionEconomyGuard.attackRiderBlock(action));
   }
   assertFalse(raw.getBoolean("auto_rolls"));
   assertFalse(raw.getBoolean("full_statblock_claim"));
 }
}
