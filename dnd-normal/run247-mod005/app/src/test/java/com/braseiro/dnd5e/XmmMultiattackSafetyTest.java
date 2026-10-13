package com.braseiro.dnd5e;
import org.json.*;import org.junit.Test;import static org.junit.Assert.*;
public final class XmmMultiattackSafetyTest {
 @Test public void deterministicFixedAttacks()throws Exception{
  JSONObject dragon=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster","Adult White Dragon"));
  assertTrue(dragon.getBoolean("resolved"));assertEquals(3,dragon.getInt("attack_slots"));
  assertEquals("Rend",dragon.getJSONArray("steps").getJSONObject(0).getString("action"));
  JSONObject beholder=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster","Beholder"));
  assertTrue(beholder.getBoolean("resolved"));assertEquals(3,beholder.getInt("use_slots"));
  assertFalse(beholder.getBoolean("actions_applied"));
 }
 @Test public void explicitChoicesCannotInventAttacks()throws Exception{
  JSONObject in=new JSONObject().put("monster","Assassin");
  assertEquals("explicit_attack_choices_required",Xmm2025MultiattackPlanIndex.resolve(in).getString("status"));
  in.put("selected_actions",new JSONArray().put("Shortsword").put("Light Crossbow").put("Shortsword"));
  assertTrue(Xmm2025MultiattackPlanIndex.resolve(in).getBoolean("resolved"));
  in.put("selected_actions",new JSONArray().put("Shortsword").put("Fireball").put("Shortsword"));
  assertEquals("attack_choice_not_allowed",Xmm2025MultiattackPlanIndex.resolve(in).getString("status"));
 }
 @Test public void complexSpellsAndSubstitutionsFailClosed()throws Exception{
  JSONObject r=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster","Adult Red Dragon"));
  assertFalse(r.getBoolean("resolved"));assertEquals("complex_multiattack_requires_manual_source_resolution",r.getString("status"));
 }
 @Test public void coverageIsNotMisreportedAsFullCombat()throws Exception{
  JSONObject c=Xmm2025MultiattackPlanIndex.coverage();
  assertEquals(306,c.getInt("multiattacks"));assertEquals(224,c.getInt("structured_plans"));assertEquals(82,c.getInt("manual_source_bound"));
  assertFalse(c.getBoolean("full_combat_automation_certified"));
 }
}
