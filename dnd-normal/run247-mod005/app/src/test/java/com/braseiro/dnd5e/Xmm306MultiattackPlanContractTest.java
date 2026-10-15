package com.braseiro.dnd5e;
import java.util.*;import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.junit.runners.Parameterized;import static org.junit.Assert.*;
/** 306 source-bound multiattack contracts, with 64 explicit manual plans. */
@RunWith(Parameterized.class)
public final class Xmm306MultiattackPlanContractTest {
 @Parameterized.Parameters(name="{index}: {0}") public static Collection<Object[]> cases(){
  JSONArray a=Xmm2025MultiattackPlanIndex.ids();ArrayList<Object[]> r=new ArrayList<>();
  for(int i=0;i<a.length();i++)r.add(new Object[]{a.optString(i)});return r;
 }
 @BeforeClass public static void setup()throws Exception{JSONObject r=Xmm2025CompiledBestiaryEngine.install();assertEquals(503,r.getInt("statblocks_installed"));assertEquals(0,r.getInt("blocked"));}
 private final String id;public Xmm306MultiattackPlanContractTest(String id){this.id=id;}
 @Test public void validatePlanOrFailClosed()throws Exception{
  JSONObject plan=Xmm2025MultiattackPlanIndex.plan(id);assertTrue(plan.getBoolean("resolved"));
  assertEquals(Xmm2025MultiattackPlanIndex.SOURCE_BLOB,plan.getString("source_blob_sha1"));
  assertTrue(plan.getString("ptr").startsWith("/monster/"));
  JSONArray actions=CompiledStatblockEngine.get(id).getJSONArray("actions");
  JSONArray variants=plan.getJSONArray("v");
  if(variants.length()==0){
   JSONObject r=Xmm2025MultiattackPlanIndex.resolve(new JSONObject().put("monster",id));
   assertFalse(r.getBoolean("resolved"));assertEquals("complex_multiattack_requires_manual_source_resolution",r.getString("status"));return;
  }
  for(int v=0;v<variants.length();v++){
   JSONArray groups=variants.getJSONArray(v),choices=new JSONArray();
   for(int i=0;i<groups.length();i++){
    JSONObject g=groups.getJSONObject(i);JSONArray opts=g.getJSONArray("o");
    assertTrue(g.getInt("c")>0);assertTrue(opts.length()>0);
    for(int j=0;j<g.getInt("c");j++)choices.put(opts.getString(0));
    for(int j=0;j<opts.length();j++){
     String name=opts.getString(j);boolean found=false;
     for(int k=0;k<actions.length();k++)if(name.equals(actions.getJSONObject(k).optString("name")))found=true;
     assertTrue(id+" missing "+name,found);
    }
   }
   JSONObject request=new JSONObject().put("monster",id).put("variant_index",v).put("selected_actions",choices);
   if(plan.has("context"))request.put(plan.getJSONObject("context").getString("field"),
       v==plan.getJSONObject("context").getInt("variant_index"));
   JSONObject r=Xmm2025MultiattackPlanIndex.resolve(request);
   assertTrue(id+" variant "+v,r.getBoolean("resolved"));
   assertEquals(choices.length(),r.getJSONArray("steps").length());
   assertFalse(r.getBoolean("actions_applied"));assertFalse(r.getBoolean("damage_applied"));
   assertFalse(r.getBoolean("auto_rolls"));assertTrue(r.getBoolean("manual_effect_resolution_required"));
  }
 }
}
