package com.braseiro.dnd5e;
import org.json.*;
import org.junit.*;
import static org.junit.Assert.*;
public final class XmmAttackConditionSafetyTest {
 private JSONObject request()throws Exception{return new JSONObject().put("monster","Ankylosaurus")
 .put("action","Tail").put("attack_hit",true).put("target_is_creature",true).put("target_size","Huge");}
 @Test public void strictInputs()throws Exception{
  assertFalse(Xmm2025AttackConditionEngine.resolve(null).getBoolean("resolved"));
  JSONObject missing=request();missing.remove("attack_hit");
  assertFalse(Xmm2025AttackConditionEngine.resolve(missing).getBoolean("resolved"));
  assertFalse(Xmm2025AttackConditionEngine.resolve(request().put("attack_hit","true")).getBoolean("resolved"));
  assertFalse(Xmm2025AttackConditionEngine.resolve(request().put("target_size","huge")).getBoolean("resolved"));
  assertFalse(Xmm2025AttackConditionEngine.resolve(request().put("monster","Unknown")).getBoolean("resolved"));
 }
 @Test public void oversizedOrNonCreatureNeverReceivesCondition()throws Exception{
  JSONObject large=Xmm2025AttackConditionEngine.resolve(request().put("target_size","Gargantuan"));
  assertTrue(large.getBoolean("resolved"));assertEquals(0,large.getJSONArray("proposed_conditions").length());
  JSONObject object=Xmm2025AttackConditionEngine.resolve(request().put("target_is_creature",false));
  assertTrue(object.getBoolean("resolved"));assertEquals(0,object.getJSONArray("proposed_conditions").length());
 }
 @Test public void grappleEscapeAndOtherEffectsRemainManual()throws Exception{
  JSONObject in=new JSONObject().put("monster","Crocodile").put("action","Bite")
   .put("attack_hit",true).put("target_is_creature",true).put("target_size","Medium");
  JSONObject r=Xmm2025AttackConditionEngine.resolve(in);
  assertTrue(r.toString(),r.getBoolean("resolved"));
  assertEquals(12,r.getJSONArray("proposed_conditions").getJSONObject(0).getInt("escape_dc"));
  assertTrue(r.getBoolean("additional_source_effects_manual"));assertFalse(r.getBoolean("condition_applied"));
 }
 @Test public void coverageDoesNotCertifyFullAction()throws Exception{
  JSONObject c=Xmm2025AttackConditionEngine.coverage();
  assertEquals(65,c.getInt("verified"));assertEquals(29,c.getInt("prone"));
  assertEquals(36,c.getInt("grappled"));assertEquals(21,c.getInt("other_riders_manual"));
  assertFalse(c.getBoolean("full_automation_certified"));
 }
 @Test public void everyIndexedRiderMatchesItsCompiledActionAndSizeGate()throws Exception{
  assertTrue(Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"));
  JSONArray catalog=Xmm2025AttackConditionEngine.catalog();assertEquals(65,catalog.length());
  for(int i=0;i<catalog.length();i++){
   JSONObject indexed=catalog.getJSONObject(i);String monster=indexed.getString("id");
   String action=indexed.getString("name"),maximum=indexed.getString("size");
   JSONObject profile=CompiledStatblockEngine.runtimeProfile(monster);
   JSONObject compiled=profile.getJSONArray("actions").getJSONObject(indexed.getInt("slot"));
   assertEquals(monster+" / "+action,action,compiled.optString("name"));
   assertTrue(monster+" / "+action,compiled.optBoolean("source_bound"));
   assertTrue(monster+" / "+action,
    ConditionTokenGuard.has(compiled.optString("condition_on_hit"),indexed.getString("condition")));
   assertEquals(monster+" / "+action,maximum.toLowerCase(java.util.Locale.ROOT),
    compiled.optString("condition_max_target_size"));

   JSONObject hit=Xmm2025AttackConditionEngine.resolve(new JSONObject().put("monster",monster)
    .put("action",action).put("attack_hit",true).put("target_is_creature",true)
    .put("target_size",maximum));
   assertTrue(monster+" / "+action+": "+hit,hit.optBoolean("resolved"));
   JSONArray proposed=hit.getJSONArray("proposed_conditions");assertEquals(monster+" / "+action,1,proposed.length());
   JSONObject condition=proposed.getJSONObject(0);
   assertEquals(monster+" / "+action,indexed.getString("condition"),condition.optString("condition"));
   if("grappled".equals(indexed.getString("condition")))
    assertEquals(monster+" / "+action,indexed.getInt("dc"),condition.optInt("escape_dc"));
   else assertFalse(monster+" / "+action,condition.has("escape_dc"));
   assertEquals(monster+" / "+action,indexed.optBoolean("other"),
    hit.optBoolean("additional_source_effects_manual"));
   assertFalse(monster+" / "+action,hit.optBoolean("condition_applied"));

   JSONObject tooLarge=Xmm2025AttackConditionEngine.resolve(new JSONObject().put("monster",monster)
    .put("action",action).put("attack_hit",true).put("target_is_creature",true)
    .put("target_size","Gargantuan"));
   assertTrue(monster+" / "+action+": "+tooLarge,tooLarge.optBoolean("resolved"));
   if(!"Gargantuan".equalsIgnoreCase(maximum))assertEquals(monster+" / "+action,0,
    tooLarge.getJSONArray("proposed_conditions").length());
   assertFalse(monster+" / "+action,tooLarge.optBoolean("condition_applied"));
  }
 }
}
