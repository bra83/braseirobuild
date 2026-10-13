package com.braseiro.dnd5e;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
/** No hidden casts or consumption: explicit advisory eligibility only. */
public final class XmmSpellUsageGatewayTest {
 @Test public void atWillSpellCanBeIdentifiedWithoutConsumingResources()throws Exception {
  JSONObject in=new JSONObject().put("monster","Aarakocra Aeromancer").put("spell","Mage Hand");
  JSONObject out=Xmm2025SpellUsageIndex.check(in);
  assertTrue(out.getBoolean("resolved"));
  assertTrue(out.getBoolean("may_attempt"));
  assertEquals("will",out.getString("usage"));
  assertEquals(0,out.getInt("spells_cast"));
  assertFalse(out.getBoolean("auto_mutation_allowed"));
 }
 @Test public void dailyLimitedSpellNeedsExplicitRemainingCount()throws Exception {
  JSONObject in=new JSONObject().put("monster","Aarakocra Aeromancer").put("spell","Lightning Bolt");
  JSONObject unknown=Xmm2025SpellUsageIndex.check(in);
  assertEquals("manual_usage_count_required",unknown.getString("status"));
  assertTrue(unknown.isNull("may_attempt"));
  assertFalse(Xmm2025SpellUsageIndex.check(in.put("uses_remaining",0)).getBoolean("may_attempt"));
  assertTrue(Xmm2025SpellUsageIndex.check(in.put("uses_remaining",1)).getBoolean("may_attempt"));
  assertFalse(Xmm2025SpellUsageIndex.check(in).getBoolean("spell_effects_applied"));
 }
 @Test public void eachDailySpellUsesOwnCountRatherThanSharedPool()throws Exception {
  JSONObject in=new JSONObject().put("monster","Adult Black Dragon").put("spell","Speak with Dead");
  JSONObject result=Xmm2025SpellUsageIndex.check(in);
  assertTrue(result.getBoolean("resolved"));
  assertEquals("daily",result.getString("usage"));
  assertTrue("1e = each spell",result.getBoolean("uses_each_per_spell"));
  assertEquals(1,result.getInt("source_usage_limit"));
  assertEquals("manual_usage_count_required",result.getString("status"));
  assertFalse(Xmm2025SpellUsageIndex.check(in.put("uses_remaining",0)).getBoolean("may_attempt"));
  assertTrue(Xmm2025SpellUsageIndex.check(in.put("uses_remaining",1)).getBoolean("may_attempt"));
  assertEquals(0,result.getInt("spells_cast"));
 }
 @Test public void unknownCreatureAndSpellFailClosed()throws Exception {
  assertFalse(Xmm2025SpellUsageIndex.check(new JSONObject().put("monster","unknown").put("spell","Fireball")).getBoolean("resolved"));
  assertFalse(Xmm2025SpellUsageIndex.check(new JSONObject().put("monster","Aboleth").put("spell","nonexistent spell")).getBoolean("resolved"));
 }
 @Test public void androidGatewayRoutesCatalogAndEligibility()throws Exception {
  OfficialBookRuntimeGateway gw=new OfficialBookRuntimeGateway(null);
  JSONObject options=gw.execute("xmm","spell_options",new JSONObject().put("name","Aarakocra Aeromancer"));
  assertTrue(options.getBoolean("executed"));
  assertEquals("Aarakocra Aeromancer", options.getJSONObject("result").getString("id"));
  JSONObject r=gw.execute("xmm","spell_usage_check",new JSONObject().put("monster","Aarakocra Aeromancer").put("spell","Mage Hand"));
  assertTrue(r.getBoolean("executed"));
  assertFalse(r.getBoolean("auto_mutation_allowed"));
  assertFalse(r.getJSONObject("result").getBoolean("spell_effects_applied"));
  JSONObject wrongBook=gw.execute("rhw","spell_usage_check",new JSONObject().put("monster","Aarakocra Aeromancer").put("spell","Mage Hand"));
  assertFalse(wrongBook.getBoolean("executed"));
 }
}
