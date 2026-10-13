package com.braseiro.dnd5e;

import java.util.ArrayList;
import java.util.Collection;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/** Each source-pinned rule is matched to the compiled action and evaluated
 * at the DC boundary without hidden rolls, mutation or effects.
 */
@RunWith(Parameterized.class)
public final class Xmm59SaveDamageSourceContractTest {
 @Parameterized.Parameters(name="{index} {0} / {1}")
 public static Collection<Object[]> rules()throws Exception{
  JSONArray rows=Xmm2025SaveDamageEngine.index();ArrayList<Object[]> out=new ArrayList<>();
  for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);out.add(new Object[]{r.getString("id"),r.getString("name"),r.getString("category")});}
  return out;
 }
 private final String monster,action,category;
 public Xmm59SaveDamageSourceContractTest(String m,String a,String c){monster=m;action=a;category=c;}
 @Test public void eachVerifiedSaveDamageActionHonorsExplicitSaveAndDamage()throws Exception{
  JSONObject rule=Xmm2025SaveDamageEngine.rule(monster,category,action);
  assertTrue(rule.getBoolean("resolved"));
  assertEquals(Xmm2025SaveDamageEngine.SOURCE_BLOB,rule.getString("source_blob_sha1"));
  JSONObject req=new JSONObject().put("monster",monster).put("category",category)
   .put("action",action).put("target_confirmed",true).put("recharge_ready_confirmed",true)
   .put("damage_mode","average");
  assertEquals("explicit_save_total_required",Xmm2025SaveDamageEngine.resolve(req).getString("status"));
  int dc=rule.getInt("dc"),avg=rule.getInt("average");
  JSONObject failure=Xmm2025SaveDamageEngine.resolve(req.put("save_total",dc-1));
  assertTrue(failure.getBoolean("resolved"));
  assertFalse(failure.getBoolean("save_success"));
  assertEquals(avg,failure.getInt("damage_before_defenses"));
  JSONObject success=Xmm2025SaveDamageEngine.resolve(req.put("save_total",dc));
  assertTrue(success.getBoolean("save_success"));
  assertEquals(avg/2,success.getInt("damage_before_defenses"));
  assertFalse(success.getBoolean("auto_rolls"));
  assertFalse(success.getBoolean("damage_applied"));
  assertFalse(success.getBoolean("auto_mutation_allowed"));
  assertTrue(success.getBoolean("resistance_vulnerability_immunity_pending"));
  assertEquals("source_save_damage_calculated_pending_manual_application",success.getString("status"));
 }
}
