package com.braseiro.dnd5e;
import org.json.*;
import org.junit.*;
import static org.junit.Assert.*;
public final class XmmAttackConditionGatewayTest {
 @Test public void officialGatewayRoutesArePure()throws Exception{
  OfficialBookRuntimeGateway g=new OfficialBookRuntimeGateway(null);
  JSONObject in=new JSONObject().put("monster","Crocodile").put("action","Bite")
   .put("attack_hit",true).put("target_is_creature",true).put("target_size","Medium");
  assertEquals(40,g.execute("xmm","attack_condition_coverage",new JSONObject()).getJSONObject("result").getInt("verified"));
  assertEquals(40,g.execute("xmm","attack_condition_catalog",new JSONObject()).getJSONArray("result").length());
  assertTrue(g.execute("xmm","attack_condition_profile",in).getBoolean("executed"));
  JSONObject r=g.execute("xmm","attack_condition_resolve",in);
  assertTrue(r.toString(),r.getBoolean("executed"));
  assertEquals(12,r.getJSONObject("result").getJSONArray("proposed_conditions").getJSONObject(0).getInt("escape_dc"));
  assertFalse(r.getBoolean("auto_mutation_allowed"));
  assertFalse(r.getJSONObject("result").getBoolean("condition_applied"));
  assertFalse(g.execute("xmm","attack_condition_missing",in).getBoolean("executed"));
 }
}
