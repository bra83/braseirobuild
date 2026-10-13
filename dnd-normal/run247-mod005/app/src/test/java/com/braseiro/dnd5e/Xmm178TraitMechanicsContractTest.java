package com.braseiro.dnd5e;
import java.util.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/** One verified and executable runtime contract for each of 178 pinned traits. */
@RunWith(Parameterized.class)
public final class Xmm178TraitMechanicsContractTest {
 @Parameterized.Parameters(name="{index}: {0} / {1}")
 public static Collection<Object[]> cases(){
  JSONArray a=Xmm2025TraitMechanicsEngine.entries();
  ArrayList<Object[]> out=new ArrayList<>();
  for(int i=0;i<a.length();i++){
   JSONObject r=a.optJSONObject(i);
   out.add(new Object[]{r.optString("id"),r.optString("trait")});
  }
  return out;
 }
 @BeforeClass public static void install()throws Exception{
  JSONObject r=Xmm2025CompiledBestiaryEngine.install();
  assertEquals(503,r.getInt("statblocks_installed"));assertEquals(0,r.getInt("blocked"));
 }
 private final String id,trait;
 public Xmm178TraitMechanicsContractTest(String id,String trait){this.id=id;this.trait=trait;}
 @Test public void traitMustBindToExactCompiledSourceAndResolveWithoutMutation()throws Exception{
  JSONObject source=Xmm2025TraitMechanicsEngine.profile(id,trait);
  assertEquals(trait,CompiledStatblockEngine.get(id).getJSONArray("traits")
    .getJSONObject(source.getInt("slot")).getString("name"));
  JSONObject in=new JSONObject().put("monster",id).put("trait",trait);
  String kind=source.getString("kind");
  if("Magic Resistance".equals(kind))in.put("roll_kind","saving_throw").put("spell_or_magical_effect",true);
  else if("Legendary Resistance".equals(kind))in.put("in_lair",false).put("save_failed",true).put("choose_to_use",true).put("uses_remaining",1);
  else if("Pack Tactics".equals(kind))in.put("ally_within_5ft",true).put("ally_incapacitated",false);
  else if("Sunlight Sensitivity".equals(kind))in.put("in_sunlight",true).put("roll_kind","attack_roll");
  else if("Flyby".equals(kind))in.put("flight_used",true).put("leaving_enemy_reach",true);
  else if("Siege Monster".equals(kind))in.put("target_kind","object").put("damage_before_trait",7);
  else if("Evasion".equals(kind))in.put("dexterity_save_for_half",true).put("save_succeeded",true).put("incapacitated",false).put("incoming_damage",9);
  else if("Undead Fortitude".equals(kind))in.put("damage_reduces_to_zero",true).put("radiant_damage",false).put("critical_hit",false).put("damage_taken",7).put("constitution_save_total",12);
  else fail("Unrecognized source trait "+kind);
  String before=in.toString();
  JSONObject out=Xmm2025TraitMechanicsEngine.resolve(in);
  assertTrue(id+" / "+trait+": "+out.toString(),out.getBoolean("resolved"));
  assertEquals(before,in.toString());
  assertEquals(source.getString("ptr"),out.getString("source_pointer"));
  assertEquals(Xmm2025TraitMechanicsEngine.SOURCE_BLOB,out.getString("source_blob_sha1"));
  assertFalse(out.getBoolean("auto_rolls"));assertFalse(out.getBoolean("auto_mutation_allowed"));
  assertFalse(out.getBoolean("effect_applied"));assertFalse(out.getBoolean("turn_advanced"));
  assertTrue(out.getBoolean("trigger_satisfied"));
  if("Magic Resistance".equals(kind))assertTrue(out.getBoolean("advantage_on_save"));
  if("Legendary Resistance".equals(kind)){
   assertEquals(0,out.getInt("uses_remaining_proposed"));
   assertTrue(out.getBoolean("save_success_proposed"));assertFalse(out.getBoolean("resource_consumed"));
  }
  if("Pack Tactics".equals(kind))assertTrue(out.getBoolean("attack_advantage"));
  if("Sunlight Sensitivity".equals(kind))assertTrue(out.getBoolean("disadvantage"));
  if("Flyby".equals(kind))assertTrue(out.getBoolean("opportunity_attack_prevented"));
  if("Siege Monster".equals(kind))assertEquals(14,out.getInt("damage_before_defenses"));
  if("Evasion".equals(kind))assertEquals(0,out.getInt("damage_before_defenses"));
  if("Undead Fortitude".equals(kind)){
   assertEquals(12,out.getInt("save_dc"));assertEquals(1,out.getInt("proposed_hp_after"));
   assertFalse(out.getBoolean("hp_applied"));
  }
 }
}
