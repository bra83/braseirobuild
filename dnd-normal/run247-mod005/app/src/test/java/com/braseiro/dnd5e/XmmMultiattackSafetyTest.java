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
  JSONObject merrow=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster","Merrow")
      .put("selected_actions",new JSONArray().put("Bite").put("Harpoon")));
  assertTrue(merrow.toString(),merrow.optBoolean("resolved"));assertEquals(2,merrow.optInt("attack_slots"));
  JSONObject pitFiend=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster","Pit Fiend"));
  assertTrue(pitFiend.toString(),pitFiend.optBoolean("resolved"));assertEquals(4,pitFiend.optInt("attack_slots"));
  assertFalse(pitFiend.optBoolean("actions_applied"));
 }
 @Test public void explicitChoicesCannotInventAttacks()throws Exception{
  JSONObject in=new JSONObject().put("monster","Assassin");
  assertEquals("explicit_attack_choices_required",Xmm2025MultiattackPlanIndex.resolve(in).getString("status"));
  in.put("selected_actions",new JSONArray().put("Shortsword").put("Light Crossbow").put("Shortsword"));
  assertTrue(Xmm2025MultiattackPlanIndex.resolve(in).getBoolean("resolved"));
  in.put("selected_actions",new JSONArray().put("Shortsword").put("Fireball").put("Shortsword"));
  assertEquals("attack_choice_not_allowed",Xmm2025MultiattackPlanIndex.resolve(in).getString("status"));
 }
 @Test public void dragonSpellReplacementUsesPinnedSpellAndRemainsUncast()throws Exception{
  JSONObject r=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster","Adult Red Dragon")
      .put("variant_index",1).put("selected_actions",new JSONArray()
          .put("Scorching Ray").put("Rend").put("Rend")));
  assertTrue(r.toString(),r.optBoolean("resolved"));
  JSONObject spell=r.getJSONArray("steps").getJSONObject(0);
  assertEquals("spell",spell.optString("kind"));assertEquals("Scorching Ray",spell.optString("action"));
  assertTrue(spell.getJSONObject("spell_usage_gate").optBoolean("resolved"));
  assertFalse(spell.getJSONObject("spell_usage_gate").optBoolean("spell_effects_applied",true));
  assertTrue(r.optBoolean("manual_effect_resolution_required"));assertFalse(r.optBoolean("actions_applied"));
 }
 @Test public void coverageIsNotMisreportedAsFullCombat()throws Exception{
  JSONObject c=Xmm2025MultiattackPlanIndex.coverage();
  assertEquals(306,c.getInt("multiattacks"));assertEquals(261,c.getInt("structured_plans"));assertEquals(45,c.getInt("manual_source_bound"));
  assertFalse(c.getBoolean("full_combat_automation_certified"));
 }
 @Test public void sphinxOfSecretsCanReplaceAtMostOneClawWithItsRiddleCurse()throws Exception{
  JSONObject plan=Xmm2025MultiattackPlanIndex.plan("Sphinx of Secrets");
  assertTrue(plan.optBoolean("structured"));assertEquals("/monster/407/action/0/entries",plan.optString("ptr"));
  JSONArray claws=new JSONArray().put("Claw").put("Claw").put("Claw");
  JSONObject normal=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster","Sphinx of Secrets")
      .put("variant_index",0).put("selected_actions",claws));
  assertTrue(normal.toString(),normal.optBoolean("resolved"));
  assertEquals(3,normal.optInt("attack_slots"));assertEquals(0,normal.optInt("use_slots"));
  JSONArray replacement=new JSONArray().put("Claw").put("Curse of the Riddle").put("Claw");
  JSONObject alternate=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster","Sphinx of Secrets")
      .put("variant_index",2).put("selected_actions",replacement));
  assertTrue(alternate.toString(),alternate.optBoolean("resolved"));
  assertEquals(2,alternate.optInt("attack_slots"));assertEquals(1,alternate.optInt("use_slots"));
  assertTrue(alternate.optBoolean("manual_effect_resolution_required"));
  assertFalse(alternate.optBoolean("actions_applied",true));assertFalse(alternate.optBoolean("auto_rolls",true));
 }
 @Test public void werebeastsChooseTwoBaseAttacksOrOneBasePlusFormBite()throws Exception{
  for(String id:new String[]{"Werebear","Wereboar","Wererat","Weretiger","Werewolf"}){
   JSONObject plan=Xmm2025MultiattackPlanIndex.plan(id);assertTrue(id,plan.optBoolean("structured"));
   assertEquals(3,plan.getJSONArray("v").length());
   JSONObject base=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster",id)
       .put("variant_index",0).put("selected_actions",new JSONArray()
           .put(plan.getJSONArray("v").getJSONArray(0).getJSONObject(0).getJSONArray("o").getString(0))
           .put(plan.getJSONArray("v").getJSONArray(0).getJSONObject(0).getJSONArray("o").getString(1))));
   assertTrue(id+base,base.optBoolean("resolved"));assertEquals(2,base.optInt("attack_slots"));
   assertFalse(base.optBoolean("actions_applied"));
   JSONObject substitute=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster",id)
       .put("variant_index",1).put("selected_actions",new JSONArray()
           .put(plan.getJSONArray("v").getJSONArray(1).getJSONObject(0).getJSONArray("o").getString(0))
           .put(plan.getJSONArray("v").getJSONArray(1).getJSONObject(1).getJSONArray("o").getString(0))));
   assertTrue(id+substitute,substitute.optBoolean("resolved"));assertEquals(2,substitute.optInt("attack_slots"));
   JSONObject invalid=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster",id)
       .put("variant_index",1).put("selected_actions",new JSONArray().put("Bite").put("Bite")));
   assertFalse(invalid.optBoolean("resolved"));assertEquals("attack_choice_not_allowed",invalid.optString("status"));
  }
 }
 @Test public void compoundAlternativesPreserveSlotsAndRequireHastenContext()throws Exception{
  JSONObject barbed=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster","Barbed Devil")
      .put("variant_index",0).put("selected_actions",new JSONArray().put("Claws").put("Tail")));
  assertTrue(barbed.toString(),barbed.optBoolean("resolved"));assertEquals(2,barbed.optInt("attack_slots"));
  JSONObject medusa=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster","Medusa")
      .put("variant_index",1).put("selected_actions",new JSONArray().put("Poison Ray").put("Poison Ray").put("Poison Ray")));
  assertTrue(medusa.toString(),medusa.optBoolean("resolved"));
  JSONObject dragonTurtle=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster","Dragon Turtle")
      .put("variant_index",2).put("selected_actions",new JSONArray().put("Bite").put("Tail").put("Bite")));
  assertTrue(dragonTurtle.toString(),dragonTurtle.optBoolean("resolved"));assertEquals(3,dragonTurtle.optInt("attack_slots"));
  JSONObject roc=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster","Roc")
      .put("variant_index",1).put("selected_actions",new JSONArray().put("Talons").put("Beak")));
  assertTrue(roc.toString(),roc.optBoolean("resolved"));
  JSONObject youngSilver=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster","Young Silver Dragon")
      .put("variant_index",1).put("selected_actions",new JSONArray().put("Paralyzing Breath").put("Rend").put("Rend")));
  assertTrue(youngSilver.toString(),youngSilver.optBoolean("resolved"));
  assertEquals(2,youngSilver.optInt("attack_slots"));assertEquals(1,youngSilver.optInt("use_slots"));
  JSONObject tarrasque=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster","Tarrasque")
      .put("selected_actions",new JSONArray().put("Bite").put("Claw").put("Tail").put("Claw")));
  assertTrue(tarrasque.toString(),tarrasque.optBoolean("resolved"));assertEquals(4,tarrasque.optInt("attack_slots"));
  assertFalse(barbed.optBoolean("actions_applied"));assertFalse(medusa.optBoolean("auto_rolls"));
 }
 @Test public void clayGolemVariantMustMatchExplicitHastenState()throws Exception{
  JSONObject missing=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster","Clay Golem")
      .put("variant_index",1).put("selected_actions",new JSONArray().put("Slam").put("Slam").put("Slam")));
  assertEquals("explicit_multiattack_context_required",missing.optString("status"));
  JSONObject mismatch=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster","Clay Golem")
      .put("variant_index",1).put("hasten_used_this_turn",false)
      .put("selected_actions",new JSONArray().put("Slam").put("Slam").put("Slam")));
  assertEquals("multiattack_context_variant_mismatch",mismatch.optString("status"));
  JSONObject valid=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster","Clay Golem")
      .put("variant_index",1).put("hasten_used_this_turn",true)
      .put("selected_actions",new JSONArray().put("Slam").put("Slam").put("Slam")));
  assertTrue(valid.toString(),valid.optBoolean("resolved"));assertEquals("hasten_used_this_turn",valid.optString("variant_context_field"));
 }
 @Test public void hybridBatchAcceptsOnlyThePinnedMultiattackPlan()throws Exception{
  JSONObject request=new JSONObject().put("request_id","sphinx-sequence").put("book_id","xmm").put("edition","2025")
      .put("monster","Sphinx of Secrets").put("source_pointer","/monster/407/action/0/entries")
      .put("operation","multiattack_resolve").put("inputs",new JSONObject()
          .put("variant_index",2).put("selected_actions",new JSONArray()
              .put("Claw").put("Curse of the Riddle").put("Claw")));
  JSONObject packet=new JSONObject().put("origin","player_preview")
      .put("source_blob_sha1",Xmm2025SourceIndex.SOURCE_BLOB).put("requests",new JSONArray().put(request));
  JSONObject result=new OfficialBookRuntimeGateway(null).execute("xmm","hybrid_batch_preview",packet)
      .getJSONObject("result");
  assertEquals(1,result.optInt("resolved_count"));
  JSONObject preview=result.getJSONArray("results").getJSONObject(0).getJSONObject("runtime_result");
  assertEquals(1,preview.optInt("use_slots"));assertFalse(preview.optBoolean("actions_applied",true));
  request.put("monster","Clay Golem").put("source_pointer","/monster/95/action/0/entries")
      .put("inputs",new JSONObject().put("variant_index",1).put("hasten_used_this_turn",true)
          .put("selected_actions",new JSONArray().put("Slam").put("Slam").put("Slam")));
  result=new OfficialBookRuntimeGateway(null).execute("xmm","hybrid_batch_preview",packet).getJSONObject("result");
  assertEquals(1,result.optInt("resolved_count"));
  preview=result.getJSONArray("results").getJSONObject(0).getJSONObject("runtime_result");
  assertEquals("hasten_used_this_turn",preview.optString("variant_context_field"));
  request.put("monster","Sphinx of Secrets").put("source_pointer","/monster/407/action/0/entries")
      ;
  request.put("inputs",new JSONObject().put("variant_index",2).put("selected_actions",new JSONArray()
      .put("Claw").put("Curse of the Riddle").put("Claw").put("Curse of the Riddle"))
      .put("untrusted_override",true));
  result=new OfficialBookRuntimeGateway(null).execute("xmm","hybrid_batch_preview",packet).getJSONObject("result");
  assertEquals(0,result.optInt("resolved_count"));
  assertEquals("unrecognized_multiattack_inputs",
      result.getJSONArray("results").getJSONObject(0).optString("reason"));
 }
}
