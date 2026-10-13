package com.braseiro.dnd5e;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** XMM-only routes and failure boundaries for explicit DM dice. */
public final class XmmSaveDamageGatewayTest {
 private JSONObject req()throws Exception{return new JSONObject()
  .put("monster","Adult Black Dragon").put("category","action").put("action","Acid Breath")
  .put("target_confirmed",true).put("recharge_ready_confirmed",true)
  .put("save_total",18).put("damage_mode","rolled").put("damage_total",53);}
 @Test public void allVerifiedRulesAreListedAndNoFullCoverageIsClaimed()throws Exception{
  assertEquals(59,Xmm2025SaveDamageEngine.index().length());
  assertFalse(Xmm2025SaveDamageEngine.coverage().getBoolean("full_combat_automation_certified"));
 }
 @Test public void missingTargetOrRechargeConfirmationBlocks()throws Exception{
  JSONObject r=req();
  assertEquals("manual_target_eligibility_confirmation_required",
    Xmm2025SaveDamageEngine.resolve(r.put("target_confirmed",false)).getString("status"));
  r.put("target_confirmed",true).put("recharge_ready_confirmed",false);
  assertEquals("manual_recharge_readiness_confirmation_required",
    Xmm2025SaveDamageEngine.resolve(r).getString("status"));
 }
 @Test public void rejectsOutOfBoundsOrMissingDiceAndNoOtherMonsterIsAllowed()throws Exception{
  JSONObject r=req();
  assertEquals("damage_roll_out_of_source_bounds",
   Xmm2025SaveDamageEngine.resolve(r.put("damage_total",10000)).getString("status"));
  r.remove("damage_total");
  assertEquals("explicit_damage_total_required",
   Xmm2025SaveDamageEngine.resolve(r).getString("status"));
  assertEquals("source_action_not_in_verified_subset",
   Xmm2025SaveDamageEngine.resolve(r.put("monster","Aboleth")).getString("status"));
 }
 @Test public void gatewayRoutesOnlyXmmAndNeverAppliesDamage()throws Exception{
  OfficialBookRuntimeGateway gw=new OfficialBookRuntimeGateway(null);
  JSONObject result=gw.execute("xmm","save_damage_resolve",req());
  assertTrue(result.getBoolean("executed"));
  assertEquals(26,result.getJSONObject("result").getInt("damage_before_defenses"));
  assertFalse(result.getJSONObject("result").getBoolean("damage_applied"));
  assertFalse(result.getBoolean("auto_mutation_allowed"));
  JSONObject list=gw.execute("xmm","save_damage_catalog",new JSONObject());
  assertTrue(list.getBoolean("executed"));
  assertEquals(59,list.getJSONArray("result").length());
  assertFalse(gw.execute("rhw","save_damage_resolve",req()).getBoolean("executed"));
 }
}
