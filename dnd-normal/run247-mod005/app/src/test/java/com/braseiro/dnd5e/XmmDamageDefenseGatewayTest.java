package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Mechanical boundaries: typed defenses, PHB rounding, condition immunity,
 * fail-closed conditional rules and composition with save-for-half breath attacks.
 */
public final class XmmDamageDefenseGatewayTest {
 private JSONObject incoming(String id,String type,int amount)throws Exception{
  return new JSONObject().put("defender",id).put("damage_type",type)
   .put("incoming_damage",amount).put("target_confirmed",true);
 }
 @Test public void resistanceHalvesAndRoundsDown()throws Exception{
  JSONObject r=Xmm2025DamageDefenseEngine.resolve(incoming("Air Elemental","bludgeoning",9));
  assertTrue(r.getBoolean("resolved"));assertTrue(r.getBoolean("resistant"));
  assertEquals(4,r.getInt("damage_after_basic_defenses"));
  assertFalse(r.getBoolean("damage_applied"));
 }
 @Test public void immunityZerosDamage()throws Exception{
  JSONObject r=Xmm2025DamageDefenseEngine.resolve(incoming("Adult Red Dragon","fire",23));
  assertTrue(r.getBoolean("immune"));assertEquals(0,r.getInt("damage_after_basic_defenses"));
 }
 @Test public void vulnerabilityDoublesDamage()throws Exception{
  JSONObject r=Xmm2025DamageDefenseEngine.resolve(incoming("Awakened Shrub","fire",9));
  assertTrue(r.getBoolean("vulnerable"));assertEquals(18,r.getInt("damage_after_basic_defenses"));
 }
 @Test public void sourceConditionalDefensesFailClosed()throws Exception{
  JSONObject half=Xmm2025DamageDefenseEngine.resolve(incoming("Half-Dragon","fire",9));
  assertFalse(half.getBoolean("resolved"));
  assertEquals("conditional_or_trait_dependent_defense_requires_manual_resolution",half.getString("status"));
  JSONObject rak=Xmm2025DamageDefenseEngine.resolve(incoming("Rakshasa","piercing",9));
  assertFalse(rak.getBoolean("resolved"));
 }
 @Test public void conditionImmunityChecksOnlyUnconditionalSource()throws Exception{
  JSONObject immune=Xmm2025DamageDefenseEngine.conditionImmunity(new JSONObject()
   .put("defender","Air Elemental").put("condition","prone").put("target_confirmed",true));
  assertTrue(immune.getBoolean("resolved"));assertTrue(immune.getBoolean("immune"));
  assertFalse(immune.getBoolean("condition_applied"));
  JSONObject conditional=Xmm2025DamageDefenseEngine.conditionImmunity(new JSONObject()
   .put("defender","Archmage").put("condition","charmed").put("target_confirmed",true));
  assertFalse(conditional.getBoolean("resolved"));
  assertEquals("conditional_condition_immunity_requires_manual_resolution",conditional.getString("status"));
 }
 @Test public void malformedOrUnconfirmedDamageCannotResolve()throws Exception{
  JSONObject r=incoming("Air Elemental","bludgeoning",9);
  assertFalse(Xmm2025DamageDefenseEngine.resolve(r.put("target_confirmed",false)).getBoolean("resolved"));
  r.put("target_confirmed",true).put("incoming_damage",-1);
  assertEquals("invalid_incoming_damage",Xmm2025DamageDefenseEngine.resolve(r).getString("status"));
  r.put("incoming_damage",4.5);
  assertEquals("invalid_incoming_damage",Xmm2025DamageDefenseEngine.resolve(r).getString("status"));
  r.put("incoming_damage",4).put("damage_type","unknown");
  assertEquals("invalid_damage_type",Xmm2025DamageDefenseEngine.resolve(r).getString("status"));
  r.put("damage_type","fire").put("defender","NOT A MONSTER");
  assertEquals("defender_not_in_pinned_xmm",Xmm2025DamageDefenseEngine.resolve(r).getString("status"));
 }
 @Test public void saveForHalfComposesWithTargetVulnerabilityAndImmunity()throws Exception{
  JSONObject req=new JSONObject().put("monster","Young Red Dragon").put("category","action")
   .put("action","Fire Breath").put("target_confirmed",true)
   .put("recharge_ready_confirmed",true).put("save_total",0).put("damage_mode","average")
   .put("defender","Awakened Shrub");
  JSONObject result=Xmm2025DamageDefenseEngine.resolveSaveAgainstDefender(req);
  assertTrue(result.getBoolean("resolved"));
  assertEquals(112,result.getInt("damage_after_basic_defenses"));
  assertFalse(result.getBoolean("damage_applied"));
  JSONObject immune=Xmm2025DamageDefenseEngine.resolveSaveAgainstDefender(req.put("defender","Adult Red Dragon"));
  assertTrue(immune.getBoolean("resolved"));
  assertEquals(0,immune.getInt("damage_after_basic_defenses"));
  assertFalse(Xmm2025DamageDefenseEngine.resolveSaveAgainstDefender(req.remove("defender")!=null?req:req).getBoolean("resolved"));
 }
 @Test public void gatewayRoutesXmmOnlyAndDoesNotMutate()throws Exception{
  OfficialBookRuntimeGateway gateway=new OfficialBookRuntimeGateway(null);
  JSONObject r=gateway.execute("xmm","defense_resolve",incoming("Air Elemental","lightning",11));
  assertTrue(r.getBoolean("executed"));
  assertEquals(5,r.getJSONObject("result").getInt("damage_after_basic_defenses"));
  assertFalse(r.getBoolean("auto_mutation_allowed"));
  assertTrue(gateway.execute("xmm","defense_coverage",new JSONObject()).getBoolean("executed"));
  assertFalse(gateway.execute("nf","defense_resolve",incoming("Air Elemental","lightning",11)).getBoolean("executed"));
 }
 @Test public void coverageNeverClaimsCompleteMonsterTraits()throws Exception{
  JSONObject c=Xmm2025DamageDefenseEngine.coverage();
  assertEquals(503,c.getInt("indexed_monsters"));
  assertEquals(272,c.getInt("explicit_defense_records"));
  assertEquals(6,c.getInt("conditional_source_records"));
  assertFalse(c.getBoolean("full_trait_effects_certified"));
 }
}
