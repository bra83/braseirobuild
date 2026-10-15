package com.braseiro.dnd5e;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** End-to-end, explicit-result bridge from XMM multiattack to source-pinned on-hit riders. */
public final class XmmMultiattackRiderBridgeTest {
 private JSONObject request() throws Exception {return new JSONObject().put("monster","Giant Crocodile");}
 private JSONObject outcome(boolean hit,String size) throws Exception {
  return new JSONObject().put("attack_hit",hit).put("target_is_creature",true).put("target_size",size);
 }
 @Test public void plansExposePinnedRiderContractsWithoutApplyingThem() throws Exception {
  JSONObject r=Xmm2025MultiattackPlanIndex.resolve(request());
  assertTrue(r.toString(),r.getBoolean("resolved"));
  JSONArray steps=r.getJSONArray("steps");assertEquals(2,steps.length());
  JSONObject bite=steps.getJSONObject(0).getJSONObject("on_hit_condition_contract");
  JSONObject tail=steps.getJSONObject(1).getJSONObject("on_hit_condition_contract");
  assertEquals("grappled",bite.getString("condition"));assertEquals(15,bite.getInt("escape_dc"));
  assertEquals("prone",tail.getString("condition"));
  assertTrue(bite.getBoolean("requires_explicit_hit_and_target"));
  assertFalse(bite.getBoolean("condition_applied"));assertFalse(r.getBoolean("actions_applied"));
  assertFalse(r.getBoolean("auto_rolls"));
 }
 @Test public void explicitPerStepHitsProduceUnappliedConditionProposals() throws Exception {
  JSONArray results=new JSONArray().put(outcome(true,"Medium")).put(outcome(true,"Medium"));
  JSONObject r=Xmm2025MultiattackPlanIndex.resolve(request().put("step_outcomes",results));
  assertTrue(r.toString(),r.getBoolean("resolved"));
  JSONArray steps=r.getJSONArray("steps");
  JSONObject bite=steps.getJSONObject(0).getJSONObject("on_hit_condition_result");
  JSONObject tail=steps.getJSONObject(1).getJSONObject("on_hit_condition_result");
  assertTrue(bite.getBoolean("resolved"));assertTrue(tail.getBoolean("resolved"));
  assertEquals("grappled",bite.getJSONArray("proposed_conditions").getJSONObject(0).getString("condition"));
  assertEquals("prone",tail.getJSONArray("proposed_conditions").getJSONObject(0).getString("condition"));
  assertFalse(bite.getBoolean("condition_applied"));assertFalse(tail.getBoolean("condition_applied"));
  assertFalse(r.getBoolean("damage_applied"));assertFalse(r.getBoolean("turn_advanced"));
 }
 @Test public void missesAndOversizedTargetsDoNotApplyEffects() throws Exception {
  JSONArray results=new JSONArray().put(outcome(false,"Medium")).put(outcome(true,"Gargantuan"));
  JSONObject r=Xmm2025MultiattackPlanIndex.resolve(request().put("step_outcomes",results));
  assertTrue(r.toString(),r.getBoolean("resolved"));
  JSONArray steps=r.getJSONArray("steps");
  assertEquals(0,steps.getJSONObject(0).getJSONObject("on_hit_condition_result").getJSONArray("proposed_conditions").length());
  assertEquals(0,steps.getJSONObject(1).getJSONObject("on_hit_condition_result").getJSONArray("proposed_conditions").length());
 }
 @Test public void malformedOutcomesFailClosed() throws Exception {
  JSONObject x=request().put("step_outcomes",new JSONArray().put(outcome(true,"Medium")));
  assertEquals("step_outcome_count_mismatch",Xmm2025MultiattackPlanIndex.resolve(x).getString("status"));
  x.put("step_outcomes",new JSONArray().put(outcome(true,"Medium")).put(new JSONObject().put("attack_hit","true")));
  assertEquals("rider_step_unresolved",Xmm2025MultiattackPlanIndex.resolve(x).getString("status"));
  x=request().put("step_outcomes","not an array");
  assertEquals("step_outcomes_must_be_array",Xmm2025MultiattackPlanIndex.resolve(x).getString("status"));
 }
 @Test public void malformedVariantAndAttackChoicesFailClosed() throws Exception {
  JSONObject x=request().put("variant_index","0");
  assertEquals("explicit_integer_variant_index_required",Xmm2025MultiattackPlanIndex.resolve(x).getString("status"));
  x=new JSONObject().put("monster","Assassin").put("selected_actions",new JSONArray().put("Shortsword").put(5).put("Shortsword"));
  assertEquals("explicit_attack_name_required",Xmm2025MultiattackPlanIndex.resolve(x).getString("status"));
 }
 @Test public void allStructuredPlansStillHaveSourceBoundActions() throws Exception {
  int structured=0,linkedRiderSteps=0;JSONArray ids=Xmm2025MultiattackPlanIndex.ids();for(int i=0;i<ids.length();i++){
   String id=ids.getString(i);
   JSONObject p=Xmm2025MultiattackPlanIndex.plan(id);
   if(!p.getBoolean("structured"))continue;
   structured++;
   JSONArray groups=p.getJSONArray("v").getJSONArray(0),choices=new JSONArray();
   for(int g=0;g<groups.length();g++){JSONObject part=groups.getJSONObject(g);
    for(int j=0;j<part.getInt("c");j++)choices.put(part.getJSONArray("o").getString(0));}
   JSONObject request=new JSONObject().put("monster",id).put("variant_index",0).put("selected_actions",choices);
   if("Clay Golem".equals(id))request.put("hasten_used_this_turn",false);
   JSONObject r=Xmm2025MultiattackPlanIndex.resolve(request);
   assertTrue(id+": "+r.toString(),r.getBoolean("resolved"));
   JSONArray steps=r.getJSONArray("steps");for(int j=0;j<steps.length();j++){
    JSONObject step=steps.getJSONObject(j);JSONObject sourceRider=Xmm2025AttackConditionEngine.profile(id,step.getString("action"));
    if("attack".equals(step.getString("kind"))&&sourceRider.length()>0){
     linkedRiderSteps++;
     JSONObject link=step.getJSONObject("on_hit_condition_contract");
     assertEquals(sourceRider.getString("ptr"),link.getString("source_pointer"));
     assertFalse(link.getBoolean("condition_applied"));
    }else assertFalse(step.has("on_hit_condition_contract"));
   }
  }
  assertEquals(238,structured);assertEquals(48,linkedRiderSteps);
 }
}
