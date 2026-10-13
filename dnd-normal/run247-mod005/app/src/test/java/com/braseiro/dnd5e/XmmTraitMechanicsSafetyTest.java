package com.braseiro.dnd5e;
import org.json.*;
import org.junit.*;
import static org.junit.Assert.*;

public final class XmmTraitMechanicsSafetyTest {
 @BeforeClass public static void setup()throws Exception{
  assertTrue(Xmm2025CompiledBestiaryEngine.install().getBoolean("complete"));
 }
 private static JSONObject q(String id,String trait)throws Exception{
  return new JSONObject().put("monster",id).put("trait",trait);
 }
 private static JSONObject r(JSONObject in){return Xmm2025TraitMechanicsEngine.resolve(in);}
 @Test public void unknownTraitAndImplicitInputsFailClosed()throws Exception{
  assertFalse(r(null).getBoolean("resolved"));
  assertFalse(r(q("Unknown","Magic Resistance")).getBoolean("resolved"));
  assertFalse(r(q("Archmage","Magic Resistance")).getBoolean("resolved"));
  assertFalse(r(q("Archmage","Magic Resistance").put("roll_kind","saving_throw")
   .put("spell_or_magical_effect","true")).getBoolean("resolved"));
 }
 @Test public void magicResistanceDoesNotApplyToNonMagicalSaves()throws Exception{
  JSONObject x=r(q("Archmage","Magic Resistance").put("roll_kind","saving_throw")
   .put("spell_or_magical_effect",false));
  assertTrue(x.getBoolean("resolved"));assertFalse(x.getBoolean("advantage_on_save"));
 }
 @Test public void legendaryResourceAndLairCapacity()throws Exception{
  JSONObject in=q("Adult Black Dragon","Legendary Resistance (3/Day, or 4/Day in Lair)")
   .put("in_lair",false).put("save_failed",true).put("choose_to_use",true).put("uses_remaining",3);
  assertEquals(2,r(in).getInt("uses_remaining_proposed"));
  assertFalse(r(in.put("uses_remaining",4)).getBoolean("resolved"));
  assertEquals(4,r(in.put("in_lair",true)).getInt("daily_maximum"));
  assertFalse(r(in.put("uses_remaining",0)).getBoolean("resolved"));
  assertFalse(r(in.put("uses_remaining",1).put("save_failed",false)).getBoolean("resolved"));
 }
 @Test public void packTacticsRejectsIncapacitatedAlly()throws Exception{
  JSONObject x=r(q("Dire Wolf","Pack Tactics").put("ally_within_5ft",true)
   .put("ally_incapacitated",true));
  assertTrue(x.getBoolean("resolved"));assertFalse(x.getBoolean("attack_advantage"));
 }
 @Test public void sunlightDoesNotImposeDisadvantageOnSaves()throws Exception{
  JSONObject x=r(q("Drider","Sunlight Sensitivity").put("in_sunlight",true)
   .put("roll_kind","saving_throw"));
  assertTrue(x.getBoolean("resolved"));assertFalse(x.getBoolean("disadvantage"));
 }
 @Test public void flybyDoesNotProtectWalkingMovement()throws Exception{
  JSONObject x=r(q("Owl","Flyby").put("flight_used",false).put("leaving_enemy_reach",true));
  assertTrue(x.getBoolean("resolved"));assertFalse(x.getBoolean("opportunity_attack_prevented"));
 }
 @Test public void siegeMonsterDoesNotDoubleCreatureDamage()throws Exception{
  JSONObject x=r(q("Treant","Siege Monster").put("target_kind","creature")
   .put("damage_before_trait",7));
  assertEquals(7,x.getInt("damage_before_defenses"));
  assertFalse(x.getBoolean("damage_applied"));
  assertFalse(r(q("Treant","Siege Monster").put("target_kind","object")
   .put("damage_before_trait",-1)).getBoolean("resolved"));
 }
 @Test public void evasionHandlesFailedSaveAndIncapacitation()throws Exception{
  JSONObject in=q("Assassin","Evasion").put("dexterity_save_for_half",true)
   .put("save_succeeded",false).put("incapacitated",false).put("incoming_damage",9);
  assertEquals(4,r(in).getInt("damage_before_defenses"));
  JSONObject disabled=r(in.put("incapacitated",true));
  assertTrue(disabled.getBoolean("resolved"));assertTrue(disabled.getBoolean("normal_save_resolution_required"));
  assertFalse(disabled.has("damage_before_defenses"));
 }
 @Test public void undeadFortitudeRejectsRadiantAndCritical()throws Exception{
  JSONObject in=q("Zombie","Undead Fortitude").put("damage_reduces_to_zero",true)
   .put("radiant_damage",true).put("critical_hit",false).put("damage_taken",9);
  JSONObject radiant=r(in);
  assertTrue(radiant.getBoolean("resolved"));assertFalse(radiant.getBoolean("trigger_satisfied"));
  assertFalse(radiant.has("proposed_hp_after"));
  assertFalse(r(in.put("radiant_damage",false).put("critical_hit",true)).getBoolean("trigger_satisfied"));
  assertFalse(r(in.put("critical_hit",false)).getBoolean("resolved"));
 }
 @Test public void androidGatewayExposesPureTraitContracts()throws Exception{
  OfficialBookRuntimeGateway g=new OfficialBookRuntimeGateway(null);
  JSONObject coverage=g.execute("xmm","trait_coverage",new JSONObject());
  assertTrue(coverage.getBoolean("executed"));
  assertEquals(178,coverage.getJSONObject("result").getInt("verified_trait_contracts"));
  assertFalse(coverage.getJSONObject("result").getBoolean("full_trait_effects_certified"));
  assertTrue(g.execute("xmm","trait_profile",q("Archmage","Magic Resistance")).getBoolean("executed"));
  JSONObject x=g.execute("xmm","trait_resolve",q("Archmage","Magic Resistance")
   .put("roll_kind","saving_throw").put("spell_or_magical_effect",true));
  assertTrue(x.getBoolean("executed"));assertTrue(x.getJSONObject("result").getBoolean("advantage_on_save"));
  assertFalse(x.getBoolean("auto_mutation_allowed"));
 }
}
